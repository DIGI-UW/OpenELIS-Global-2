package org.openelisglobal.coldstorage.service.impl;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.openelisglobal.coldstorage.config.FreezerMonitoringProperties;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.service.ModbusClientService;
import org.openelisglobal.coldstorage.service.ReadingIngestionService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.config.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public ModbusPollingService(FreezerMonitoringProperties config, FreezerService freezerService,
            ModbusClientService modbusClientService, ReadingIngestionService readingIngestionService) {
        this.config = config;
        this.freezerService = freezerService;
        this.modbusClientService = modbusClientService;
        this.readingIngestionService = readingIngestionService;
        config.validateConfig();
        LOGGER.info("Freezer Modbus polling service ENABLED");
    }

    @Scheduled(initialDelayString = "#{T(java.time.Duration).parse('${org.openelisglobal.freezermonitoring.modbus.initial-delay:PT15S}').toMillis()}", fixedDelayString = "#{T(java.time.Duration).parse('${org.openelisglobal.freezermonitoring.modbus.poll-interval:PT5M}').toMillis()}")
    public void pollDevices() {
        List<Freezer> freezers = freezerService.getActiveFreezers();
        if (freezers.isEmpty()) {
            LOGGER.debug("Skipping freezer polling run - no active freezers configured");
            return;
        }

        for (Freezer freezer : freezers) {
            OffsetDateTime timestamp = OffsetDateTime.now();
            modbusClientService.readCurrentValues(freezer).ifPresentOrElse(result -> {
                readingIngestionService.ingest(freezer, timestamp, BigDecimal.valueOf(result.temperatureCelsius()),
                        result.humidityPercentage() != null ? BigDecimal.valueOf(result.humidityPercentage()) : null,
                        true, null);
                LOGGER.debug("Recorded freezer reading for {} at {} °C", freezer.getName(),
                        result.temperatureCelsius());
            }, () -> {
                LOGGER.warn("Failed to poll freezer '{}'", freezer.getName());
                readingIngestionService.ingest(freezer, timestamp, null, null, false,
                        "Modbus read failure - see logs for details");
            });
        }
    }
}
