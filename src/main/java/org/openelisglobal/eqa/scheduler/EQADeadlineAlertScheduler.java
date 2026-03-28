package org.openelisglobal.eqa.scheduler;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.eqa.dao.SampleEQADAO;
import org.openelisglobal.eqa.valueholder.SampleEQA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class EQADeadlineAlertScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EQADeadlineAlertScheduler.class);
    private static final String ENTITY_TYPE_SAMPLE_EQA = "SampleEQA";
    private static final long HOURS_72 = 72;
    private static final long HOURS_24 = 24;
    private static final long HOURS_4 = 4;
    private static final long ESCALATION_HOURS = 4;

    @Autowired
    private AlertService alertService;

    @Autowired
    private SampleEQADAO sampleEQADAO;

    @Scheduled(fixedDelay = 300000)
    public void checkEQADeadlines() {
        logger.debug("Running EQA deadline check...");
        Timestamp now = Timestamp.from(Instant.now());
        Timestamp horizon72h = Timestamp.from(Instant.now().plus(HOURS_72, ChronoUnit.HOURS));

        List<SampleEQA> approachingDeadlines = sampleEQADAO.findByDeadlineBefore(horizon72h);

        for (SampleEQA sample : approachingDeadlines) {
            if (sample.getEqaDeadline() == null || !Boolean.TRUE.equals(sample.getIsEqaSample())) {
                continue;
            }

            long hoursRemaining = ChronoUnit.HOURS.between(now.toInstant(), sample.getEqaDeadline().toInstant());

            if (hoursRemaining <= 0) {
                generateDeadlineAlert(sample, AlertSeverity.CRITICAL,
                        "EQA sample OVERDUE — deadline was " + sample.getEqaDeadline());
            } else if (hoursRemaining <= HOURS_4) {
                generateDeadlineAlert(sample, AlertSeverity.CRITICAL,
                        "EQA sample deadline in " + hoursRemaining + " hours");
            } else if (hoursRemaining <= HOURS_24) {
                generateDeadlineAlert(sample, AlertSeverity.WARNING,
                        "EQA sample deadline in " + hoursRemaining + " hours");
            } else if (hoursRemaining <= HOURS_72) {
                generateDeadlineAlert(sample, AlertSeverity.WARNING,
                        "EQA sample deadline approaching — " + hoursRemaining + " hours remaining");
            }
        }
    }

    @Scheduled(fixedDelay = 300000)
    public void checkSampleExpirations() {
        logger.debug("Running sample expiration check...");
        Timestamp horizon7d = Timestamp.from(Instant.now().plus(7, ChronoUnit.DAYS));

        List<SampleEQA> expiringSamples = sampleEQADAO.findByDeadlineBefore(horizon7d);

        for (SampleEQA sample : expiringSamples) {
            if (sample.getEqaDeadline() == null) {
                continue;
            }

            long daysRemaining = ChronoUnit.DAYS.between(Instant.now(), sample.getEqaDeadline().toInstant());

            if (daysRemaining <= 1) {
                alertService.createAlert(AlertType.SAMPLE_EXPIRATION, ENTITY_TYPE_SAMPLE_EQA, sample.getId(),
                        AlertSeverity.CRITICAL, "Sample expiring within 1 day",
                        "{\"daysRemaining\":" + daysRemaining + "}");
            } else if (daysRemaining <= 2) {
                alertService.createAlert(AlertType.SAMPLE_EXPIRATION, ENTITY_TYPE_SAMPLE_EQA, sample.getId(),
                        AlertSeverity.WARNING, "Sample expiring in " + daysRemaining + " days",
                        "{\"daysRemaining\":" + daysRemaining + "}");
            }
        }
    }

    @Scheduled(fixedDelay = 300000)
    public void escalateUnacknowledgedAlerts() {
        logger.debug("Running alert escalation check...");
        List<Alert> openAlerts = alertService.getAlertsByEntity(ENTITY_TYPE_SAMPLE_EQA, null);

        if (openAlerts == null) {
            return;
        }

        for (Alert alert : openAlerts) {
            if (alert.getStatus() != AlertStatus.OPEN) {
                continue;
            }
            if (alert.getSeverity() == AlertSeverity.CRITICAL && alert.getStartTime() != null && ChronoUnit.HOURS
                    .between(alert.getStartTime(), java.time.OffsetDateTime.now()) >= ESCALATION_HOURS
                    && alert.getAcknowledgedAt() == null) {
                alertService.createAlert(AlertType.CRITICAL_UNACKNOWLEDGED, ENTITY_TYPE_SAMPLE_EQA, alert.getId(),
                        AlertSeverity.CRITICAL,
                        "Critical alert unacknowledged for " + ESCALATION_HOURS + "+ hours: " + alert.getMessage(),
                        "{\"originalAlertId\":" + alert.getId() + "}");
            }
        }
    }

    private void generateDeadlineAlert(SampleEQA sample, AlertSeverity severity, String message) {
        String contextJson = String.format("{\"sampleId\":%d,\"deadline\":\"%s\",\"priority\":\"%s\"}",
                sample.getSampleId(), sample.getEqaDeadline(),
                sample.getEqaPriority() != null ? sample.getEqaPriority().name() : "STANDARD");

        alertService.createAlert(AlertType.EQA_DEADLINE, ENTITY_TYPE_SAMPLE_EQA, sample.getId(), severity, message,
                contextJson);
    }
}
