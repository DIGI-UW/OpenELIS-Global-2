package org.openelisglobal.eqa.service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.analyte.dao.AnalyteDAO;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.eqa.dao.EQAAnalystCompetencyEventDAO;
import org.openelisglobal.eqa.dao.EQAParticipantResultDAO;
import org.openelisglobal.eqa.valueholder.EQAAnalystCompetencyEvent;
import org.openelisglobal.eqa.valueholder.EQACompetencyEventType;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQADismissalCategory;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.openelisglobal.qaevent.service.EqaScoreNceService;
import org.openelisglobal.qaevent.service.NCEventService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EQAAnalystCompetencyServiceImpl implements EQAAnalystCompetencyService {

    /** ISO 15189 §6.2.3 reads competency over a rolling year. */
    private static final int WINDOW_MONTHS = 12;

    /**
     * FR-V2.3-06's evidence floor: fewer than four assessable samples is not
     * evidence of competence, so it bands as Under Review rather than Competent.
     */
    private static final int EVIDENCE_FLOOR = 4;

    private static final String COMPETENT = "COMPETENT";
    private static final String UNDER_REVIEW = "UNDER_REVIEW";
    private static final String NOT_COMPETENT = "NOT_COMPETENT";

    /**
     * Statuses that close a non-conformity, as the Lab Performance rollup reads
     * them.
     */
    private static final List<String> CLOSED_NCE_STATUSES = List.of("Closed", "Completed");

    /**
     * FR-V2.1-22's "counts against the analyst" column, as two sets.
     *
     * <p>
     * DISMISSED_EQUIPMENT and DISMISSED_ACCEPTABLE_ON_REVIEW appear in neither:
     * equipment fault is not the analyst's, and acceptable-on-review means triage
     * found nothing to answer for. They leave the numerator and the denominator.
     *
     * <p>
     * ESCALATED_TO_NCE fails without being evaluable, because the score it
     * escalates is already the evaluable row — the escalation is a second fact
     * about the same sample, which is exactly what the de-duplication below folds
     * back together.
     */
    private static final Set<EQACompetencyEventType> EVALUABLE = Set.of(EQACompetencyEventType.UNACCEPTABLE_SCORE,
            EQACompetencyEventType.QUESTIONABLE_SCORE, EQACompetencyEventType.EXTERNAL_MISSED_DEADLINE,
            EQACompetencyEventType.IN_HOUSE_MISSED_DEADLINE, EQACompetencyEventType.DISMISSED_TRANSCRIPTION,
            EQACompetencyEventType.DISMISSED_OTHER);

    private static final Set<EQACompetencyEventType> FAILING = Set.of(EQACompetencyEventType.UNACCEPTABLE_SCORE,
            EQACompetencyEventType.QUESTIONABLE_SCORE, EQACompetencyEventType.EXTERNAL_MISSED_DEADLINE,
            EQACompetencyEventType.IN_HOUSE_MISSED_DEADLINE, EQACompetencyEventType.DISMISSED_TRANSCRIPTION,
            EQACompetencyEventType.DISMISSED_OTHER, EQACompetencyEventType.ESCALATED_TO_NCE);

    private static final Map<EQAPerformanceStatus, String> VERDICT = Map.of(EQAPerformanceStatus.ACCEPTABLE,
            EQACompetencyRow.ACCEPTABLE, EQAPerformanceStatus.QUESTIONABLE, EQACompetencyRow.QUESTIONABLE,
            EQAPerformanceStatus.UNACCEPTABLE, EQACompetencyRow.UNACCEPTABLE);

    @Autowired
    private EQAAnalystCompetencyEventDAO competencyEventDAO;

    @Autowired
    private EQAParticipantResultDAO participantResultDAO;

    @Autowired
    private AnalyteDAO analyteDAO;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private NCEventService ncEventService;

    @Override
    public EQAAnalystCompetencyEvent record(EQAParticipantResult result, EQACompetencyEventType type, Integer nceId,
            EQADismissalCategory category, String notes, String sysUserId) {
        if (result.getAssignedAnalystId() == null) {
            return null;
        }
        EQAAnalystCompetencyEvent event = new EQAAnalystCompetencyEvent();
        event.setAnalystId(result.getAssignedAnalystId());
        event.setEventType(type);
        event.setEventDate(new Date(System.currentTimeMillis()));
        event.setScheme(result.getCycle() == null ? null : result.getCycle().getScheme());
        event.setCycleId(result.getCycle() == null ? null : result.getCycle().getId());
        event.setParticipantResultId(result.getId());
        event.setAnalyteId(result.getAnalyteId());
        event.setNceId(nceId);
        event.setDismissalCategory(category);
        event.setNotes(notes);
        event.setSysUserId(sysUserId);
        event.setId(competencyEventDAO.insert(event));
        return event;
    }

    @Override
    public void attachNce(Long participantResultId, Integer nceId) {
        List<EQAAnalystCompetencyEvent> events = competencyEventDAO.getAllMatching("participantResultId",
                participantResultId);
        for (EQAAnalystCompetencyEvent event : events) {
            if (event.getNceId() == null && (event.getEventType() == EQACompetencyEventType.UNACCEPTABLE_SCORE
                    || event.getEventType() == EQACompetencyEventType.QUESTIONABLE_SCORE)) {
                event.setNceId(nceId);
                competencyEventDAO.update(event);
            }
        }
    }

    /**
     * ponytail: reads both tables whole, as the Lab Performance rollup does. If
     * either outgrows that, both pages want a windowed query rather than this one.
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getCompetencyRollup() {
        LocalDate windowStart = LocalDate.now().minusMonths(WINDOW_MONTHS);
        List<EQACompetencyRow> rows = new ArrayList<>();
        Set<Long> covered = new LinkedHashSet<>();

        // Both tables are read whole and filtered to the window in Java. That is
        // honest at EQA volumes — a lab runs tens of PT samples a year, not
        // thousands — but it is a full scan of two tables per page load, and this
        // data only grows. When it bites, push windowStart into the DAOs: neither
        // carries a date-bounded finder today, so both need one.

        for (EQAAnalystCompetencyEvent event : competencyEventDAO.getAll()) {
            EQACompetencyRow row = fromEvent(event, windowStart);
            if (row != null) {
                rows.add(row);
                if (event.getParticipantResultId() != null) {
                    covered.add(event.getParticipantResultId());
                }
            }
        }
        for (EQAParticipantResult result : participantResultDAO.getAll()) {
            EQACompetencyRow row = fromResult(result, windowStart, covered);
            if (row != null) {
                rows.add(row);
            }
        }

        nameAnalytes(rows);
        Map<String, Object> page = new LinkedHashMap<>();
        List<Map<String, Object>> analysts = analysts(rows, openEscalatedNces());
        page.put("kpis", kpis(analysts));
        page.put("analysts", analysts);
        return page;
    }

    private EQACompetencyRow fromEvent(EQAAnalystCompetencyEvent event, LocalDate windowStart) {
        LocalDate date = event.getEventDate() == null ? null : event.getEventDate().toLocalDate();
        if (event.getAnalystId() == null || date == null || date.isBefore(windowStart)) {
            return null;
        }
        EQACompetencyRow row = new EQACompetencyRow();
        row.analystId = event.getAnalystId();
        EQAProgram scheme = event.getScheme();
        row.schemeId = scheme == null ? null : scheme.getId();
        row.schemeName = scheme == null ? null : scheme.getName();
        row.analyteId = event.getAnalyteId();
        row.cycleId = event.getCycleId();
        row.participantResultId = event.getParticipantResultId();
        row.date = date;
        row.eventType = event.getEventType();
        row.counted = EVALUABLE.contains(event.getEventType());
        row.failure = FAILING.contains(event.getEventType());
        row.escalation = event.getEventType() == EQACompetencyEventType.ESCALATED_TO_NCE;
        row.nceId = event.getNceId();
        row.outcome = outcomeOf(event.getEventType());
        return row;
    }

    /**
     * The scored results the log does not already speak for — in practice the
     * acceptable ones, since scoring writes an event for every other verdict. A
     * result the log covers is skipped: FR-V2.3-06 makes the event canonical.
     */
    private EQACompetencyRow fromResult(EQAParticipantResult result, LocalDate windowStart, Set<Long> covered) {
        if (result.getAssignedAnalystId() == null || covered.contains(result.getId())) {
            return null;
        }
        boolean scored = result.getSubmissionStatus() == EQASubmissionStatus.SCORED
                && result.getPerformanceStatus() != null;
        boolean missed = result.getSubmissionStatus() == EQASubmissionStatus.MISSED_DEADLINE;
        if (!scored && !missed) {
            return null;
        }
        LocalDate date = anchorDate(result);
        if (date == null || date.isBefore(windowStart)) {
            return null;
        }

        EQACycle cycle = result.getCycle();
        EQAProgram scheme = cycle == null ? null : cycle.getScheme();
        EQACompetencyRow row = new EQACompetencyRow();
        row.analystId = result.getAssignedAnalystId();
        row.schemeId = scheme == null ? null : scheme.getId();
        row.schemeName = scheme == null ? null : scheme.getName();
        row.analyteId = result.getAnalyteId();
        row.cycleId = cycle == null ? null : cycle.getId();
        row.participantResultId = result.getId();
        row.date = date;
        row.counted = true;
        row.failure = missed || result.getPerformanceStatus() != EQAPerformanceStatus.ACCEPTABLE;
        row.outcome = missed ? EQACompetencyRow.MISSED : VERDICT.get(result.getPerformanceStatus());
        return row;
    }

    private static String outcomeOf(EQACompetencyEventType type) {
        return switch (type) {
        case UNACCEPTABLE_SCORE -> EQACompetencyRow.UNACCEPTABLE;
        case QUESTIONABLE_SCORE -> EQACompetencyRow.QUESTIONABLE;
        case EXTERNAL_MISSED_DEADLINE, IN_HOUSE_MISSED_DEADLINE -> EQACompetencyRow.MISSED;
        case ESCALATED_TO_NCE -> EQACompetencyRow.UNACCEPTABLE;
        default -> EQACompetencyRow.DISMISSED;
        };
    }

    /**
     * One row per analyst, each carrying its per-analyte bands and the facts behind
     * them. The analyst's headline band is the worst of their analytes: competence
     * is claimed per analyte, so one analyte under review is not a competent
     * analyst.
     */
    private List<Map<String, Object>> analysts(List<EQACompetencyRow> rows, Set<Integer> openNces) {
        Map<Long, List<EQACompetencyRow>> byAnalyst = new LinkedHashMap<>();
        for (EQACompetencyRow row : rows) {
            byAnalyst.computeIfAbsent(row.analystId, id -> new ArrayList<>()).add(row);
        }

        List<Map<String, Object>> out = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();
        for (Map.Entry<Long, List<EQACompetencyRow>> entry : byAnalyst.entrySet()) {
            List<EQACompetencyRow> owned = entry.getValue();
            owned.sort(Comparator.comparing((EQACompetencyRow row) -> row.date).reversed());

            Map<Long, List<EQACompetencyRow>> byAnalyte = new LinkedHashMap<>();
            for (EQACompetencyRow row : owned) {
                byAnalyte.computeIfAbsent(row.analyteId, id -> new ArrayList<>()).add(row);
            }

            List<Map<String, Object>> analytes = new ArrayList<>();
            String headline = COMPETENT;
            for (Map.Entry<Long, List<EQACompetencyRow>> analyte : byAnalyte.entrySet()) {
                Map<String, Object> banded = band(analyte.getValue(), openNces);
                banded.put("analyteId", analyte.getKey());
                banded.put("analyteName", analyteName(analyte.getValue()));
                analytes.add(banded);
                headline = worst(headline, (String) banded.get("status"));
            }
            analytes.sort(Comparator.comparing(row -> String.valueOf(row.get("analyteName"))));

            List<EQACompetencyRow> facts = facts(owned);
            EQACompetencyRow latest = facts.stream().filter(row -> VERDICT.containsValue(row.outcome))
                    .max(Comparator.comparing(row -> row.date)).orElse(null);

            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("analystId", entry.getKey());
            dto.put("analystName", analystName(entry.getKey()));
            dto.put("status", headline);
            dto.put("sampleCount", facts.size());
            dto.put("sampleCountThisYear",
                    (int) facts.stream().filter(row -> row.date.getYear() == currentYear).count());
            dto.put("evaluableCount", (int) facts.stream().filter(row -> row.counted).count());
            dto.put("failureCount", (int) facts.stream().filter(row -> row.failure).count());
            dto.put("mostRecentPerformance", latest == null ? null : latest.outcome);
            dto.put("mostRecentDate", latest == null ? null : latest.date.toString());
            dto.put("analytes", analytes);
            dto.put("history", history(owned));
            out.add(dto);
        }
        out.sort(Comparator.comparing(row -> String.valueOf(row.get("analystName"))));
        return out;
    }

    /**
     * FR-V2.3-06's band table, read in precedence order because its rows overlap —
     * an analyst with an open escalation also satisfies "failure_n ≥ 2", and its
     * final "otherwise Competent" would claim anyone the earlier rows skipped.
     * Severest first is the only ordering that cannot assert competence over an
     * unanswered failure.
     *
     * <p>
     * The table's "2+ consecutive questionable_score" clause is deliberately not
     * implemented: every questionable sample is already a failure, so two of them
     * satisfy "failure_n ≥ 2" on the same line. Coding it would add a branch that
     * can never decide anything. See docs/eqa/analyst-competency-rules.md.
     */
    private Map<String, Object> band(List<EQACompetencyRow> analyteRows, Set<Integer> openNces) {
        List<EQACompetencyRow> facts = facts(analyteRows);
        int evaluable = (int) facts.stream().filter(row -> row.counted).count();
        int failures = (int) facts.stream().filter(row -> row.failure).count();
        boolean openEscalation = analyteRows.stream()
                .anyMatch(row -> row.escalation && (row.nceId == null || openNces.contains(row.nceId)));

        String status;
        if (openEscalation) {
            status = NOT_COMPETENT;
        } else if (failures >= 2) {
            status = UNDER_REVIEW;
        } else if (evaluable < EVIDENCE_FLOOR) {
            status = UNDER_REVIEW;
        } else {
            status = COMPETENT;
        }

        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("status", status);
        dto.put("evaluableCount", evaluable);
        dto.put("failureCount", failures);
        dto.put("openEscalation", openEscalation);
        EQACompetencyRow latest = facts.stream().filter(row -> VERDICT.containsValue(row.outcome))
                .max(Comparator.comparing(row -> row.date)).orElse(null);
        dto.put("latestPerformance", latest == null ? null : latest.outcome);
        dto.put("latestDate", latest == null ? null : latest.date.toString());
        return dto;
    }

    /**
     * The de-duplication FR-V2.3-06 asks for: rows about one sample collapse to one
     * fact. An escalated unacceptable score is a single failed sample, and a
     * dismissal that does not count against the analyst clears the sample it
     * dismisses rather than sitting beside it.
     */
    private List<EQACompetencyRow> facts(List<EQACompetencyRow> rows) {
        Map<String, EQACompetencyRow> byFact = new LinkedHashMap<>();
        int index = 0;
        for (EQACompetencyRow row : rows) {
            String key = row.factKey(index++);
            EQACompetencyRow known = byFact.get(key);
            if (known == null) {
                byFact.put(key, copy(row));
                continue;
            }
            known.counted |= row.counted;
            known.failure |= row.failure;
            known.escalation |= row.escalation;
            if (row.date.isAfter(known.date)) {
                known.date = row.date;
            }
            if (severity(row.outcome) > severity(known.outcome)) {
                known.outcome = row.outcome;
            }
        }

        List<EQACompetencyRow> facts = new ArrayList<>();
        for (EQACompetencyRow row : byFact.values()) {
            // A sample only excused by a non-counting dismissal leaves both totals.
            if (row.counted || row.failure) {
                facts.add(row);
            }
        }
        return facts;
    }

    /**
     * The merge below mutates its accumulator, and the caller's list is read again
     * afterwards by the per-analyte bands and the evidence table. Copying keeps the
     * de-duplication from rewriting the rows those two still need.
     */
    private static EQACompetencyRow copy(EQACompetencyRow row) {
        EQACompetencyRow clone = new EQACompetencyRow();
        clone.analystId = row.analystId;
        clone.schemeId = row.schemeId;
        clone.schemeName = row.schemeName;
        clone.analyteId = row.analyteId;
        clone.analyteName = row.analyteName;
        clone.cycleId = row.cycleId;
        clone.participantResultId = row.participantResultId;
        clone.date = row.date;
        clone.eventType = row.eventType;
        clone.outcome = row.outcome;
        clone.counted = row.counted;
        clone.failure = row.failure;
        clone.escalation = row.escalation;
        clone.nceId = row.nceId;
        return clone;
    }

    private static int severity(String outcome) {
        return List.of(EQACompetencyRow.DISMISSED, EQACompetencyRow.ACCEPTABLE, EQACompetencyRow.QUESTIONABLE,
                EQACompetencyRow.MISSED, EQACompetencyRow.UNACCEPTABLE).indexOf(outcome);
    }

    private static String worst(String left, String right) {
        List<String> order = List.of(COMPETENT, UNDER_REVIEW, NOT_COMPETENT);
        return order.indexOf(right) > order.indexOf(left) ? right : left;
    }

    private List<Map<String, Object>> history(List<EQACompetencyRow> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (EQACompetencyRow row : rows) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("date", row.date.toString());
            dto.put("schemeName", row.schemeName);
            dto.put("analyteId", row.analyteId);
            dto.put("analyteName", row.analyteName);
            dto.put("cycleId", row.cycleId);
            dto.put("eventType", row.eventType == null ? null : row.eventType.name());
            dto.put("outcome", row.outcome);
            dto.put("counted", row.counted);
            dto.put("failure", row.failure);
            dto.put("nceId", row.nceId);
            out.add(dto);
        }
        return out;
    }

    /** Non-conformities raised from EQA that are still open, by id. */
    private Set<Integer> openEscalatedNces() {
        Set<Integer> open = new LinkedHashSet<>();
        for (NcEvent event : ncEventService.getAll()) {
            String source = event.getTriggerSourceType();
            if (source == null || !source.startsWith(EqaScoreNceService.TRIGGER_SOURCE_PREFIX)) {
                continue;
            }
            if (!CLOSED_NCE_STATUSES.contains(event.getStatus()) && event.getId() != null) {
                open.add(Integer.valueOf(event.getId()));
            }
        }
        return open;
    }

    private void nameAnalytes(List<EQACompetencyRow> rows) {
        List<String> ids = rows.stream().map(row -> row.analyteId).filter(id -> id != null).map(String::valueOf)
                .distinct().toList();
        if (ids.isEmpty()) {
            return;
        }
        Map<String, String> names = new LinkedHashMap<>();
        for (Analyte analyte : analyteDAO.get(ids)) {
            names.put(analyte.getId(), analyte.getAnalyteName());
        }
        for (EQACompetencyRow row : rows) {
            row.analyteName = row.analyteId == null ? null : names.get(String.valueOf(row.analyteId));
        }
    }

    private static String analyteName(List<EQACompetencyRow> rows) {
        return rows.stream().map(row -> row.analyteName).filter(name -> name != null).findFirst().orElse(null);
    }

    private String analystName(Long analystId) {
        SystemUser user = analystId == null ? null : systemUserService.get(String.valueOf(analystId));
        if (user == null) {
            return String.valueOf(analystId);
        }
        String name = ((user.getFirstName() == null ? "" : user.getFirstName() + " ")
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return name.isEmpty() ? user.getLoginName() : name;
    }

    private Map<String, Object> kpis(List<Map<String, Object>> analysts) {
        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("analystCount", analysts.size());
        kpis.put("competentCount", countBand(analysts, COMPETENT));
        kpis.put("underReviewCount", countBand(analysts, UNDER_REVIEW));
        kpis.put("notCompetentCount", countBand(analysts, NOT_COMPETENT));
        kpis.put("assessedSampleCount", analysts.stream().mapToInt(row -> (int) row.get("sampleCount")).sum());
        return kpis;
    }

    private static int countBand(List<Map<String, Object>> analysts, String band) {
        return (int) analysts.stream().filter(row -> band.equals(row.get("status"))).count();
    }

    private static LocalDate anchorDate(EQAParticipantResult result) {
        if (result.getScoreReceivedAt() != null) {
            return result.getScoreReceivedAt().toLocalDateTime().toLocalDate();
        }
        if (result.getSubmittedAt() != null) {
            return result.getSubmittedAt().toLocalDateTime().toLocalDate();
        }
        EQACycle cycle = result.getCycle();
        if (cycle == null) {
            return null;
        }
        java.util.Date end = cycle.getPlannedEndDate() == null ? cycle.getPlannedStartDate()
                : cycle.getPlannedEndDate();
        return end == null ? null : new Date(end.getTime()).toLocalDate();
    }
}
