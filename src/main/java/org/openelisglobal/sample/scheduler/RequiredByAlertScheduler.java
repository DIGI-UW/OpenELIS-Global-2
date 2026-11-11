package org.openelisglobal.sample.scheduler;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class RequiredByAlertScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RequiredByAlertScheduler.class);
    private static final String ENTITY_TYPE = "Sample";
    private static final long HOURS_24 = 24;
    private static final long HOURS_4 = 4;

    @Autowired
    private AlertService alertService;

    @Autowired
    private SampleService sampleService;

    @Scheduled(fixedDelay = 300000)
    public void checkRequiredByDeadlines() {
        logger.debug("Running Required-By deadline check...");

        Timestamp horizon = Timestamp.from(Instant.now().plus(HOURS_24, ChronoUnit.HOURS));
        List<Sample> approaching = sampleService.findSamplesWithRequiredByBefore(horizon);

        Set<Long> approachingIds = new HashSet<>();
        for (Sample sample : approaching) {
            if (sample.getRequiredBy() == null)
                continue;
            approachingIds.add(Long.parseLong(sample.getId()));

            long hoursRemaining = ChronoUnit.HOURS.between(Instant.now(), sample.getRequiredBy().toInstant());
            String accession = sample.getAccessionNumber();
            String contextJson = String.format("{\"sampleId\":\"%s\",\"accessionNumber\":\"%s\",\"requiredBy\":\"%s\"}",
                    sample.getId(), accession, sample.getRequiredBy());

            if (hoursRemaining <= 0) {
                alertService.createAlert(AlertType.REQUIRED_BY_DEADLINE, ENTITY_TYPE, Long.parseLong(sample.getId()),
                        AlertSeverity.CRITICAL, "Order " + accession + " is past its required-by deadline",
                        contextJson);
            } else if (hoursRemaining <= HOURS_4) {
                alertService.createAlert(AlertType.REQUIRED_BY_DEADLINE, ENTITY_TYPE, Long.parseLong(sample.getId()),
                        AlertSeverity.CRITICAL, "Order " + accession + " required by in " + hoursRemaining + " hour(s)",
                        contextJson);
            } else {
                alertService.createAlert(AlertType.REQUIRED_BY_DEADLINE, ENTITY_TYPE, Long.parseLong(sample.getId()),
                        AlertSeverity.WARNING, "Order " + accession + " required by in " + hoursRemaining + " hour(s)",
                        contextJson);
            }
        }

        // Resolve open alerts for samples whose deadline has been extended beyond 24h
        resolveExtendedDeadlineAlerts(approachingIds);
    }

    private void resolveExtendedDeadlineAlerts(Set<Long> approachingIds) {
        // Find all samples that have open REQUIRED_BY_DEADLINE alerts
        // but whose required_by is now > 24h away (not in approaching list)
        List<Sample> allWithRequiredBy = sampleService
                .findSamplesWithRequiredByBefore(Timestamp.from(Instant.now().plus(365 * 24, ChronoUnit.HOURS)));

        for (Sample sample : allWithRequiredBy) {
            Long sampleId = Long.parseLong(sample.getId());
            if (approachingIds.contains(sampleId))
                continue;

            List<Alert> alerts = alertService.getAlertsByEntity(ENTITY_TYPE, sampleId);
            for (Alert alert : alerts) {
                if (alert.getAlertType() != AlertType.REQUIRED_BY_DEADLINE)
                    continue;
                if (alert.getStatus() != AlertStatus.OPEN && alert.getStatus() != AlertStatus.ACKNOWLEDGED)
                    continue;
                alertService.resolveAlert(alert.getId(), 1, "Required-by date extended beyond alert threshold");
            }
        }
    }
}
