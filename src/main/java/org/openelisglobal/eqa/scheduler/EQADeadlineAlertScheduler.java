package org.openelisglobal.eqa.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.eqa.dao.EQARoundDAO;
import org.openelisglobal.eqa.dao.SampleEQADAO;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQARound;
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
    private static final String ENTITY_TYPE_EQA_ROUND = "EQARound";
    private static final ObjectMapper CONTEXT_MAPPER = new ObjectMapper();
    private static final long HOURS_72 = 72;
    private static final long HOURS_24 = 24;
    private static final long HOURS_4 = 4;
    private static final long ESCALATION_HOURS = 4;

    /**
     * FR-V2.2-14: digest thresholds, in days before a round's submission deadline.
     */
    private static final long[] DIGEST_DAYS = { 7, 3, 1 };

    /**
     * Cycle states in which a submission is still expected — a reminder for a cycle
     * already submitted, scored or closed is noise.
     */
    private static final Set<EQACycleStatus> AWAITING_SUBMISSION = EnumSet.of(EQACycleStatus.PLANNED,
            EQACycleStatus.PANEL_RECEIVED, EQACycleStatus.TESTING, EQACycleStatus.READY_TO_SUBMIT,
            EQACycleStatus.PREP_IN_PROGRESS, EQACycleStatus.READY_TO_SHIP, EQACycleStatus.SHIPPED,
            EQACycleStatus.DELIVERED, EQACycleStatus.SUBMISSIONS_OPEN);

    /**
     * Only the digest reads this; the older jobs keep their inline Instant.now().
     * Package-private setter exists for tests to pin the day the digest sees.
     */
    private Clock clock = Clock.systemDefaultZone();

    @Autowired
    private AlertService alertService;

    @Autowired
    private SampleEQADAO sampleEQADAO;

    @Autowired
    private EQARoundDAO eqaRoundDAO;

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
        // Push the OPEN/CRITICAL/unacknowledged/age filter into HQL so we only
        // fetch the rows that actually need escalation. The previous version
        // pulled every alert of type SampleEQA and filtered in Java, which OOMed
        // as the alert table grew.
        OffsetDateTime cutoff = OffsetDateTime.now().minus(ESCALATION_HOURS, ChronoUnit.HOURS);
        List<Alert> candidates = alertService.getUnacknowledgedAlertsOlderThan(ENTITY_TYPE_SAMPLE_EQA, AlertStatus.OPEN,
                AlertSeverity.CRITICAL, cutoff);
        for (Alert alert : candidates) {
            alertService.createAlert(AlertType.CRITICAL_UNACKNOWLEDGED, ENTITY_TYPE_SAMPLE_EQA, alert.getId(),
                    AlertSeverity.CRITICAL,
                    "Critical alert unacknowledged for " + ESCALATION_HOURS + "+ hours: " + alert.getMessage(),
                    "{\"originalAlertId\":" + alert.getId() + "}");
        }
    }

    /**
     * FR-V2.2-14: 7/3/1-day submission-deadline digest. Alert rows are keyed to the
     * ROUND, not the cycle: createAlert dedupes blindly on (type, entityType,
     * entityId) within 30 minutes, so cycle-keyed rows would fold two different
     * thresholds fired in one tick — a cycle whose round 1 is due tomorrow and
     * round 2 in three days would permanently lose the second reminder. The
     * business dedupe the FRS asks for — one alert per (cycle, threshold), ever —
     * is the explicit alreadyAlerted check, which also outlives createAlert's
     * 30-minute window across 5-minute reschedules.
     */
    @Scheduled(fixedDelay = 300000)
    public void sendCycleDeadlineDigest() {
        logger.debug("Running EQA cycle deadline digest...");
        LocalDate today = LocalDate.now(clock);
        Timestamp from = Timestamp.valueOf(today.atStartOfDay());
        Timestamp to = Timestamp.valueOf(today.plusDays(DIGEST_DAYS[0] + 1).atStartOfDay());

        for (EQARound round : eqaRoundDAO.findWithSubmissionDeadlineBetween(from, to)) {
            EQACycle cycle = round.getCycle();
            if (!AWAITING_SUBMISSION.contains(cycle.getStatus())) {
                continue;
            }
            long daysOut = ChronoUnit.DAYS.between(today,
                    round.getSubmissionDeadline().toLocalDateTime().toLocalDate());
            if (!isDigestThreshold(daysOut) || alreadyAlerted(cycle.getId(), daysOut)) {
                continue;
            }
            String schemeName = cycle.getScheme() != null ? cycle.getScheme().getName() : "?";
            String message = "EQA cycle '" + schemeName + "' #" + cycle.getCycleNumber() + " round "
                    + round.getRoundNumber() + ": submission deadline in " + daysOut + " day(s)";
            String contextJson = String.format(
                    "{\"cycleId\":%d,\"roundId\":%d,\"thresholdDays\":%d,\"submissionDeadline\":\"%s\"}", cycle.getId(),
                    round.getId(), daysOut, round.getSubmissionDeadline());
            alertService.createAlert(AlertType.EQA_DEADLINE, ENTITY_TYPE_EQA_ROUND, round.getId(),
                    daysOut <= 1 ? AlertSeverity.CRITICAL : AlertSeverity.WARNING, message, contextJson);
        }
    }

    private boolean isDigestThreshold(long daysOut) {
        for (long d : DIGEST_DAYS) {
            if (daysOut == d) {
                return true;
            }
        }
        return false;
    }

    /**
     * A threshold fires once per cycle (rows are round-keyed). The context is
     * PARSED, never substring-matched: context_data is jsonb, and Postgres
     * canonicalizes it on storage — "thresholdDays":7 comes back as
     * "thresholdDays": 7, so a marker string that includes the exact punctuation
     * silently never matches and the dedupe evaporates.
     */
    private boolean alreadyAlerted(Long cycleId, long daysOut) {
        return alertService
                .getAllMatching(Map.of("alertType", AlertType.EQA_DEADLINE, "alertEntityType", ENTITY_TYPE_EQA_ROUND))
                .stream().anyMatch(a -> {
                    if (a.getContextData() == null) {
                        return false;
                    }
                    try {
                        JsonNode ctx = CONTEXT_MAPPER.readTree(a.getContextData());
                        return ctx.path("cycleId").asLong() == cycleId && ctx.path("thresholdDays").asLong() == daysOut;
                    } catch (Exception e) {
                        return false;
                    }
                });
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    private void generateDeadlineAlert(SampleEQA sample, AlertSeverity severity, String message) {
        String contextJson = String.format("{\"sampleId\":%d,\"deadline\":\"%s\",\"priority\":\"%s\"}",
                sample.getSampleId(), sample.getEqaDeadline(),
                sample.getEqaPriority() != null ? sample.getEqaPriority().name() : "STANDARD");

        alertService.createAlert(AlertType.EQA_DEADLINE, ENTITY_TYPE_SAMPLE_EQA, sample.getId(), severity, message,
                contextJson);
    }
}
