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
     * The evidence floor: fewer than four assessable samples is not evidence of
     * competence, so it bands as Under Review rather than Competent.
     */
    private static final int EVIDENCE_FLOOR = 4;

    private static final String COMPETENT = "COMPETENT";
    private static final String UNDER_REVIEW = "UNDER_REVIEW";
    private static final String NOT_COMPETENT = "NOT_COMPETENT";

    /**
     * Which rule produced a band. UNDER_REVIEW is reached by two rules that mean
     * opposite things — an analyst who has failed twice, and an analyst nobody has
     * given enough work to judge — and they call for opposite actions. The stored
     * status stays one value; this says which branch of {@link #band} set it, so
     * the page can tell the reader what to do about it.
     */
    private static final String MEETS_EVIDENCE = "MEETS_EVIDENCE";
    private static final String REPEATED_FAILURE = "REPEATED_FAILURE";
    private static final String INSUFFICIENT_EVIDENCE = "INSUFFICIENT_EVIDENCE";
    private static final String OPEN_ESCALATION = "OPEN_ESCALATION";

    /**
     * Statuses that close a non-conformity, as the Lab Performance rollup reads
     * them.
     */
    private static final List<String> CLOSED_NCE_STATUSES = List.of("Closed", "Completed");

    /**
     * The "counts against the analyst" column, as two sets.
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

    /**
     * The triage verdicts: a supervisor's finding <i>about</i> a score, recorded
     * after it. Whichever of these lands last decides how the sample counts,
     * because triage is the later and better-informed statement -- which is what
     * lets the two excusing categories excuse anything at all.
     *
     * <p>
     * ESCALATED_TO_NCE is deliberately not one of them. An escalation is an extra
     * fact about a sample the score already made evaluable, not a decision about
     * whether it counts.
     */
    private static final Set<EQACompetencyEventType> TRIAGE_VERDICT = Set.of(EQACompetencyEventType.DISMISSED_EQUIPMENT,
            EQACompetencyEventType.DISMISSED_TRANSCRIPTION, EQACompetencyEventType.DISMISSED_ACCEPTABLE_ON_REVIEW,
            EQACompetencyEventType.DISMISSED_OTHER);

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
     * Reads both tables whole, as the Lab Performance rollup does. If either
     * outgrows that, both pages want a windowed query rather than this one.
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
        // The rule's own parameters travel with its verdicts, so the page quotes the
        // window and the floor it was actually judged against.
        page.put("windowStart", windowStart.toString());
        page.put("windowEnd", LocalDate.now().toString());
        page.put("windowMonths", WINDOW_MONTHS);
        page.put("evidenceFloor", EVIDENCE_FLOOR);
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
        row.eventId = event.getId();
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
     * result the log covers is skipped: the event is canonical.
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
            // The headline is the worst band across the analytes, so the rules that
            // produced it are those of the analytes sitting at that band. Each one
            // carries its own counts: a rule reads over one analyte, and the
            // analyst's totals across every analyte are not the denominator it
            // fired on.
            List<Map<String, Object>> headlineReasons = new ArrayList<>();
            for (Map<String, Object> banded : analytes) {
                if (headline.equals(banded.get("status"))) {
                    Map<String, Object> why = new LinkedHashMap<>();
                    why.put("reason", banded.get("reason"));
                    why.put("analyteName", banded.get("analyteName"));
                    why.put("evaluableCount", banded.get("evaluableCount"));
                    why.put("failureCount", banded.get("failureCount"));
                    headlineReasons.add(why);
                }
            }

            List<EQACompetencyRow> facts = facts(owned);
            EQACompetencyRow latest = facts.stream().filter(row -> VERDICT.containsValue(row.outcome))
                    .max(Comparator.comparing(row -> row.date)).orElse(null);

            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("analystId", entry.getKey());
            dto.put("analystName", analystName(entry.getKey()));
            dto.put("status", headline);
            dto.put("statusReasons", headlineReasons);
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
     * The band table, read in precedence order because its rows overlap — an
     * analyst with an open escalation also satisfies "failure_n ≥ 2", and its final
     * "otherwise Competent" would claim anyone the earlier rows skipped. Severest
     * first is the only ordering that cannot assert competence over an unanswered
     * failure.
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
        String reason;
        if (openEscalation) {
            status = NOT_COMPETENT;
            reason = OPEN_ESCALATION;
        } else if (failures >= 2) {
            status = UNDER_REVIEW;
            reason = REPEATED_FAILURE;
        } else if (evaluable < EVIDENCE_FLOOR) {
            status = UNDER_REVIEW;
            reason = INSUFFICIENT_EVIDENCE;
        } else {
            status = COMPETENT;
            reason = MEETS_EVIDENCE;
        }

        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("status", status);
        dto.put("reason", reason);
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
     * The de-duplication: rows about one sample collapse to one fact, and the
     * specification names the winner -- "the event is the canonical row".
     */
    private List<EQACompetencyRow> facts(List<EQACompetencyRow> rows) {
        Map<String, List<EQACompetencyRow>> byFact = new LinkedHashMap<>();
        int index = 0;
        for (EQACompetencyRow row : rows) {
            byFact.computeIfAbsent(row.factKey(index++), key -> new ArrayList<>()).add(row);
        }

        List<EQACompetencyRow> facts = new ArrayList<>();
        for (List<EQACompetencyRow> group : byFact.values()) {
            EQACompetencyRow fact = collapse(group);
            // A sample excused by a non-counting dismissal leaves both totals.
            if (fact.counted || fact.failure) {
                facts.add(fact);
            }
        }
        return facts;
    }

    /**
     * One sample is one fact, and the latest statement about it decides how it
     * counts. Scoring is the instrument's verdict; a triage verdict is the
     * supervisor's finding about that verdict and is recorded after it, so it
     * replaces the score's counting decision rather than being OR-ed with it.
     *
     * <p>
     * OR-ing them was the defect: the score event always lands first, so its flags
     * survived whatever triage decided, and the two categories the specification
     * says do not count against the analyst could only ever excuse a sample that
     * had not failed. An equipment fault was never lifted off the analyst who
     * happened to run the sample.
     *
     * <p>
     * Nothing leaves the record. The evidence table under each analyst still lists
     * every event with its date and category, so an assessor sees that the sample
     * failed and sees why it was excused; only the counts move. Excusing pulls the
     * denominator down, which can band an analyst Under Review for thin evidence
     * rather than Competent -- a broken analyser means less evidence about the
     * person, not more, and the specification's own worked example is exactly that
     * case.
     */
    private EQACompetencyRow collapse(List<EQACompetencyRow> group) {
        EQACompetencyRow fact = copy(group.get(0));
        for (EQACompetencyRow row : group) {
            fact.escalation |= row.escalation;
            if (row.date.isAfter(fact.date)) {
                fact.date = row.date;
            }
            if (severity(row.outcome) > severity(fact.outcome)) {
                fact.outcome = row.outcome;
            }
        }

        EQACompetencyRow verdict = latestTriageVerdict(group);
        if (verdict != null) {
            fact.counted = verdict.counted;
            fact.failure = verdict.failure;
        } else {
            // No triage: the score rows speak for the sample, and an escalation
            // beside one of them adds the failure without a second denominator.
            fact.counted = group.stream().anyMatch(row -> row.counted);
            fact.failure = group.stream().anyMatch(row -> row.failure);
        }
        return fact;
    }

    /**
     * The last triage verdict about a sample, ordered by date and then by event id
     * -- a bad score and the triage answering it usually land on the same day, so
     * the date alone cannot order them.
     */
    private static EQACompetencyRow latestTriageVerdict(List<EQACompetencyRow> group) {
        // Set.of throws on a null probe, and a row derived from a result has no
        // event type at all.
        return group.stream().filter(row -> row.eventType != null && TRIAGE_VERDICT.contains(row.eventType))
                .max(Comparator.<EQACompetencyRow, LocalDate>comparing(row -> row.date)
                        .thenComparing(row -> row.eventId == null ? 0L : row.eventId))
                .orElse(null);
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
        clone.eventId = row.eventId;
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
