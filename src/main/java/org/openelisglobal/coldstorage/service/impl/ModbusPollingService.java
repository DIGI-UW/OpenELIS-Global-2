package org.openelisglobal.coldstorage.service.impl;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.openelisglobal.coldstorage.config.FreezerMonitoringProperties;
import org.openelisglobal.coldstorage.service.FreezerReadingService;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.service.ModbusClientService;
import org.openelisglobal.coldstorage.service.ReadingIngestionService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.config.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Polls active freezer devices via Modbus on a scheduled interval. Only created
 * when org.openelisglobal.freezermonitoring.enabled=true.
 */
@Service
@ConditionalOnProperty(property = "org.openelisglobal.freezermonitoring.enabled", havingValue = "true")
@SuppressWarnings("unused")
public class ModbusPollingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModbusPollingService.class);

    private final FreezerMonitoringProperties config;
    private final FreezerService freezerService;
    private final ModbusClientService modbusClientService;
    private final ReadingIngestionService readingIngestionService;
    private final FreezerReadingService freezerReadingService;
    private final ExecutorService pollingExecutor;

    public ModbusPollingService(FreezerMonitoringProperties config, FreezerService freezerService,
            ModbusClientService modbusClientService, ReadingIngestionService readingIngestionService,
            FreezerReadingService freezerReadingService,
            @Qualifier("freezerPollingExecutor") ExecutorService pollingExecutor) {
        this.config = config;
        this.freezerService = freezerService;
        this.modbusClientService = modbusClientService;
        this.readingIngestionService = readingIngestionService;
        this.freezerReadingService = freezerReadingService;
        this.pollingExecutor = pollingExecutor;
        config.validateConfig();
        LOGGER.info("Freezer Modbus polling service ENABLED");
    }

    /**
     * Polls every active freezer concurrently on a small dedicated pool (see
     * {@link org.openelisglobal.coldstorage.config.FreezerPollingExecutorConfig}).
     *
     * <p>
     * Previously this loop was sequential: one slow/unreachable device (up to
     * timeout x retries) delayed polling of every other device in the same cycle,
     * and - since scheduling uses {@code fixedDelay} - pushed back the start of the
     * next cycle for every freezer. Each device is now polled on its own thread and
     * fully isolated in its own try/catch so a single failing device can never
     * abort or delay polling of the others. The poll cycle itself is still
     * triggered by the existing {@code @Scheduled} mechanism; this method blocks
     * (via {@link CompletableFuture#join()}) only until every in-flight device poll
     * for this cycle completes, which keeps {@code fixedDelay} semantics intact (no
     * overlapping cycles) while making the cycle duration bounded by the slowest
     * single device instead of the sum of all devices.
     */
    @Scheduled(initialDelayString = "#{T(java.time.Duration).parse('${org.openelisglobal.freezermonitoring.modbus.initial-delay:PT15S}').toMillis()}", fixedDelayString = "#{T(java.time.Duration).parse('${org.openelisglobal.freezermonitoring.modbus.poll-interval:PT5M}').toMillis()}")
    public void pollDevices() {
        List<Freezer> freezers = freezerService.getActiveFreezers();
        if (freezers.isEmpty()) {
            LOGGER.debug("Skipping freezer polling run - no active freezers configured");
            return;
        }

        List<CompletableFuture<Void>> futures = freezers.stream()
                .map(freezer -> CompletableFuture.runAsync(() -> pollSingleDevice(freezer), pollingExecutor)).toList();

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception ex) {
            // CompletableFuture.runAsync already isolates exceptions per-device (see
            // pollSingleDevice's own try/catch), so this should not normally trigger.
            // Guard it anyway so a truly unexpected failure in the join itself cannot
            // propagate out of a @Scheduled method and silently disable future runs.
            LOGGER.error("Unexpected error while waiting for freezer poll cycle to complete", ex);
        }
    }

    /**
     * Polls a single freezer and records the result. Fully isolated: any exception
     * here is caught and logged so it cannot abort polling of other devices in the
     * same cycle (each device runs on its own future).
     */
    private void pollSingleDevice(Freezer freezer) {
        try {
            OffsetDateTime timestamp = OffsetDateTime.now();
            modbusClientService.readCurrentValues(freezer).ifPresentOrElse(result -> {
                readingIngestionService.ingest(freezer, timestamp, BigDecimal.valueOf(result.temperatureCelsius()),
                        result.humidityPercentage() != null ? BigDecimal.valueOf(result.humidityPercentage()) : null,
                        result.temperatureCelsius2() != null ? BigDecimal.valueOf(result.temperatureCelsius2()) : null,
                        true, null);
                LOGGER.debug("Recorded freezer reading for {} at {} °C", freezer.getName(),
                        result.temperatureCelsius());
            }, () -> {
                LOGGER.warn("Failed to poll freezer '{}'", freezer.getName());
                readingIngestionService.ingest(freezer, timestamp, null, null, false,
                        "Modbus read failure - see logs for details");
            });
        } catch (Exception ex) {
            // Belt-and-braces: readCurrentValues/ingest should not throw, but a single
            // misbehaving device must never be able to abort the rest of the poll cycle.
            LOGGER.error("Unexpected error polling freezer '{}'", freezer.getName(), ex);
        }
    }

    /**
     * Deletes freezer_reading rows older than the configured retention window
     * (default 400 days - generous so nobody's data silently vanishes on upgrade).
     * Runs once a day by default (see
     * {@code org.openelisglobal.freezermonitoring.retention-cron}). This is a
     * straightforward age-based batch delete, not a partitioning system: alerts and
     * corrective actions reference {@code freezer_id}, not individual
     * {@code freezer_reading} rows, so there is no foreign key to violate by
     * deleting old readings.
     */
    @Scheduled(cron = "${org.openelisglobal.freezermonitoring.retention-cron:0 30 2 * * ?}")
    public void cleanupOldReadings() {
        int retentionDays = config.getRetentionDays();
        if (retentionDays <= 0) {
            LOGGER.debug("Freezer reading retention cleanup disabled (retention-days={})", retentionDays);
            return;
        }
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(retentionDays);
        try {
            int deleted = freezerReadingService.deleteReadingsOlderThan(cutoff);
            if (deleted > 0) {
                LOGGER.info("Freezer reading retention cleanup deleted {} reading(s) older than {} ({} day(s))",
                        deleted, cutoff, retentionDays);
            } else {
                LOGGER.debug("Freezer reading retention cleanup found nothing older than {} ({} day(s))", cutoff,
                        retentionDays);
            }
        } catch (Exception ex) {
            LOGGER.error("Freezer reading retention cleanup failed", ex);
        }
    }
}
