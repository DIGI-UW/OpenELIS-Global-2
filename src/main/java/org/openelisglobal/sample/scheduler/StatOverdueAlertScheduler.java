package org.openelisglobal.sample.scheduler;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class StatOverdueAlertScheduler {

    private static final String TURNAROUND_CONFIG = "statTurnaroundMinutes";
    private static final int DEFAULT_TURNAROUND_MINUTES = 60;
    private static final String ENTITY_TYPE = "Sample";

    @Autowired
    private AlertService alertService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SiteInformationService siteInformationService;

    @Autowired
    private IStatusService statusService;

    @Scheduled(fixedDelay = 300000)
    public void checkStatTurnaround() {
        int minutes = turnaroundMinutes();
        Timestamp cutoff = Timestamp.from(Instant.now().minus(minutes, ChronoUnit.MINUTES));
        List<String> awaitingResult = Stream
                .of(AnalysisStatus.NotStarted, AnalysisStatus.TechnicalRejected, AnalysisStatus.BiologistRejected)
                .map(statusService::getStatusID).toList();

        for (Sample sample : sampleService.getStatSamplesReceivedBeforeWithAnalysisIn(cutoff, awaitingResult)) {
            String accession = sample.getAccessionNumber();
            alertService.createAlert(AlertType.STAT_OVERDUE, ENTITY_TYPE, Long.parseLong(sample.getId()),
                    AlertSeverity.CRITICAL,
                    "STAT order " + accession + " has no result " + minutes + " minutes after receipt",
                    String.format("{\"sampleId\":\"%s\",\"accessionNumber\":\"%s\",\"turnaroundMinutes\":%d}",
                            sample.getId(), accession, minutes));
        }
    }

    private int turnaroundMinutes() {
        SiteInformation config = siteInformationService.getSiteInformationByName(TURNAROUND_CONFIG);
        if (config == null || config.getValue() == null) {
            return DEFAULT_TURNAROUND_MINUTES;
        }
        try {
            return Integer.parseInt(config.getValue().trim());
        } catch (NumberFormatException e) {
            return DEFAULT_TURNAROUND_MINUTES;
        }
    }
}
