package org.openelisglobal.eqa.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.openelisglobal.accreditation.dto.EqaCoverageView;
import org.openelisglobal.accreditation.service.TestAccreditationService;
import org.openelisglobal.eqa.dao.EQAParticipantResultDAO;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.openelisglobal.qaevent.service.EqaScoreNceService;
import org.openelisglobal.qaevent.service.NCEventService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EQALabPerformanceServiceImpl implements EQALabPerformanceService {

    /** The accreditation window every KPI on the page is read against. */
    private static final int WINDOW_MONTHS = 12;

    /** FR-V2.3-07's matrix is the last four cycles of each scheme. */
    private static final int MATRIX_CYCLES = 4;

    private static final String UNASSIGNED_SECTION = "unassigned";

    /** Worst verdict wins a cell; a cell with no result of its own is missing. */
    private static final Map<EQAPerformanceStatus, String> VERDICT = Map.of(EQAPerformanceStatus.ACCEPTABLE,
            "acceptable", EQAPerformanceStatus.QUESTIONABLE, "questionable", EQAPerformanceStatus.UNACCEPTABLE,
            "unacceptable");

    private static final List<String> VERDICT_ORDER = List.of("acceptable", "questionable", "unacceptable");

    private static final List<String> CLOSED_NCE_STATUSES = List.of("Closed", "Completed");

    @Autowired
    private EQAParticipantResultDAO participantResultDAO;

    @Autowired
    private EQAParticipantResultService participantResultService;

    @Autowired
    private TestAccreditationService testAccreditationService;

    @Autowired
    private NCEventService ncEventService;

    @Override
    public Map<String, Object> getLabPerformance() {
        // A DRAFT is a working value the provider has never seen; counting it would
        // move every rate on a page the lab shows an assessor.
        List<EQAParticipantResult> results = participantResultDAO.getAll().stream()
                .filter(result -> result.getSubmissionStatus() != EQASubmissionStatus.DRAFT).toList();
        Map<Long, String> sections = participantResultService.sectionNamesByResultId(results);
        LocalDate windowStart = LocalDate.now().minusMonths(WINDOW_MONTHS);

        List<EqaCoverageView> accreditation = testAccreditationService.getEqaCoverage();
        List<Map<String, Object>> gaps = gaps(accreditation);

        Map<String, Object> page = new LinkedHashMap<>();
        page.put("kpis", kpis(results, windowStart, gaps.size()));
        page.put("coverage", coverage(results, sections));
        page.put("gaps", gaps);
        page.put("recentCycles", recentCycles(results, windowStart));
        return page;
    }

    private Map<String, Object> kpis(List<EQAParticipantResult> results, LocalDate windowStart, int uncoveredTests) {
        LocalDate priorStart = windowStart.minusMonths(WINDOW_MONTHS);

        int scored = 0;
        int acceptable = 0;
        int priorScored = 0;
        int priorAcceptable = 0;
        int submitted = 0;
        int late = 0;
        for (EQAParticipantResult result : results) {
            LocalDate scoredOn = anchorDate(result);
            if (result.getPerformanceStatus() != null && scoredOn != null) {
                boolean isAcceptable = result.getPerformanceStatus() == EQAPerformanceStatus.ACCEPTABLE;
                if (!scoredOn.isBefore(windowStart)) {
                    scored++;
                    acceptable += isAcceptable ? 1 : 0;
                } else if (!scoredOn.isBefore(priorStart)) {
                    priorScored++;
                    priorAcceptable += isAcceptable ? 1 : 0;
                }
            }
            LocalDate submittedOn = toLocalDate(result.getSubmittedAt());
            if (submittedOn != null && !submittedOn.isBefore(windowStart)) {
                submitted++;
                LocalDate deadline = cycleDeadline(result.getCycle());
                // No planned end is not a late submission — the cycle simply never
                // stated one, and inventing a deadline would invent the breach.
                late += deadline != null && submittedOn.isAfter(deadline) ? 1 : 0;
            }
        }

        Integer acceptanceRate = rate(acceptable, scored);
        Integer priorRate = rate(priorAcceptable, priorScored);

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("acceptanceRate", acceptanceRate);
        kpis.put("priorAcceptanceRate", priorRate);
        kpis.put("acceptanceDelta", acceptanceRate == null || priorRate == null ? null : acceptanceRate - priorRate);
        kpis.put("scoredCount", scored);
        kpis.put("acceptableCount", acceptable);
        kpis.put("onTimeRate", rate(submitted - late, submitted));
        kpis.put("submittedCount", submitted);
        kpis.put("lateCount", late);
        kpis.putAll(nceCounts(windowStart));
        kpis.put("uncoveredTestCount", uncoveredTests);
        return kpis;
    }

    /**
     * The register is the owner of EQA-triggered non-conformities; this counts them
     * for the tile that deep-links there. Both EQA trigger sources share the
     * {@code EQA_} prefix the {@code ?source=eqa} contract filters on.
     *
     * <p>
     * ponytail: reads the whole table like the NCE dashboard does. If nc_event ever
     * outgrows that, both callers want a counting query, not this one.
     */
    private Map<String, Object> nceCounts(LocalDate windowStart) {
        int total = 0;
        int open = 0;
        for (NcEvent event : ncEventService.getAll()) {
            String source = event.getTriggerSourceType();
            if (source == null || !source.startsWith(EqaScoreNceService.TRIGGER_SOURCE_PREFIX)) {
                continue;
            }
            LocalDate raised = toLocalDate(
                    event.getReportDate() == null ? event.getDateOfEvent() : event.getReportDate());
            if (raised != null && raised.isBefore(windowStart)) {
                continue;
            }
            total++;
            open += CLOSED_NCE_STATUSES.contains(event.getStatus()) ? 0 : 1;
        }
        return Map.of("eqaNceCount", total, "eqaNceOpenCount", open);
    }

    /**
     * One row per section and scheme, its cells the scheme's last four cycles
     * (FR-V2.3-07). A section that reported nothing in one of those cycles reads as
     * missing rather than being dropped: the gap is the finding.
     */
    private List<Map<String, Object>> coverage(List<EQAParticipantResult> results, Map<Long, String> sections) {
        Map<Long, List<EQACycle>> cyclesByScheme = new LinkedHashMap<>();
        Map<String, EQAPerformanceStatus> worstByCell = new LinkedHashMap<>();
        Map<String, String> sectionsBySchemeRow = new LinkedHashMap<>();
        Map<Long, String> schemeNames = new LinkedHashMap<>();

        for (EQAParticipantResult result : results) {
            EQACycle cycle = result.getCycle();
            EQAProgram scheme = cycle == null ? null : cycle.getScheme();
            if (scheme == null || result.getPerformanceStatus() == null) {
                continue;
            }
            schemeNames.put(scheme.getId(), scheme.getName());
            List<EQACycle> cycles = cyclesByScheme.computeIfAbsent(scheme.getId(), id -> new ArrayList<>());
            if (cycles.stream().noneMatch(known -> known.getId().equals(cycle.getId()))) {
                cycles.add(cycle);
            }
            String section = sections.getOrDefault(result.getId(), UNASSIGNED_SECTION);
            sectionsBySchemeRow.put(scheme.getId() + "|" + section, section);
            String cellKey = scheme.getId() + "|" + section + "|" + cycle.getId();
            EQAPerformanceStatus known = worstByCell.get(cellKey);
            if (known == null || severity(result.getPerformanceStatus()) > severity(known)) {
                worstByCell.put(cellKey, result.getPerformanceStatus());
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<Long, List<EQACycle>> entry : cyclesByScheme.entrySet()) {
            Long schemeId = entry.getKey();
            List<EQACycle> ordered = entry.getValue().stream().sorted(Comparator
                    .comparing((EQACycle cycle) -> cycle.getPlannedStartDate(),
                            Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(cycle -> cycle.getCycleNumber(), Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(EQACycle::getId)).toList();
            List<EQACycle> lastFour = ordered.size() <= MATRIX_CYCLES ? ordered
                    : ordered.subList(ordered.size() - MATRIX_CYCLES, ordered.size());

            for (String sectionKey : new LinkedHashSet<>(sectionsBySchemeRow.keySet())) {
                if (!sectionKey.startsWith(schemeId + "|")) {
                    continue;
                }
                String section = sectionsBySchemeRow.get(sectionKey);
                List<Map<String, Object>> cells = new ArrayList<>();
                int acceptable = 0;
                int evaluated = 0;
                for (EQACycle cycle : lastFour) {
                    EQAPerformanceStatus status = worstByCell.get(schemeId + "|" + section + "|" + cycle.getId());
                    String verdict = status == null ? "missing" : VERDICT.get(status);
                    evaluated += status == null ? 0 : 1;
                    acceptable += status == EQAPerformanceStatus.ACCEPTABLE ? 1 : 0;
                    Map<String, Object> cell = new LinkedHashMap<>();
                    cell.put("cycleId", cycle.getId());
                    cell.put("cycleLabel", cycleLabel(cycle));
                    cell.put("verdict", verdict);
                    cells.add(cell);
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("section", UNASSIGNED_SECTION.equals(section) ? null : section);
                row.put("schemeId", schemeId);
                row.put("schemeName", schemeNames.get(schemeId));
                row.put("cells", cells);
                row.put("acceptanceRate", rate(acceptable, evaluated));
                rows.add(row);
            }
        }
        rows.sort(Comparator.comparing((Map<String, Object> row) -> String.valueOf(row.get("section")))
                .thenComparing(row -> String.valueOf(row.get("schemeName"))));
        return rows;
    }

    /**
     * The accredited tests with no live EQA enrollment — ISO 15189 §7.7.2's
     * alternative-assessment candidates. Read from the accreditation module rather
     * than recomputed here, so both pages answer the coverage question the same
     * way.
     */
    private List<Map<String, Object>> gaps(List<EqaCoverageView> accreditation) {
        Map<String, Map<String, Object>> byTest = new LinkedHashMap<>();
        for (EqaCoverageView body : accreditation) {
            for (EqaCoverageView.GapTest gap : body.gaps) {
                Map<String, Object> row = byTest.computeIfAbsent(gap.testId, id -> {
                    Map<String, Object> fresh = new LinkedHashMap<>();
                    fresh.put("testId", gap.testId);
                    fresh.put("testName", gap.testName);
                    fresh.put("bodyCodes", new ArrayList<String>());
                    return fresh;
                });
                @SuppressWarnings("unchecked")
                List<String> bodies = (List<String>) row.get("bodyCodes");
                if (body.bodyCode != null && !bodies.contains(body.bodyCode)) {
                    bodies.add(body.bodyCode);
                }
            }
        }
        return new ArrayList<>(byTest.values());
    }

    /** Cycles this lab has results for in the window, newest first (AC-E07). */
    private List<Map<String, Object>> recentCycles(List<EQAParticipantResult> results, LocalDate windowStart) {
        Map<Long, Map<String, Object>> byCycle = new LinkedHashMap<>();
        Map<Long, EQACycle> cycles = new LinkedHashMap<>();
        for (EQAParticipantResult result : results) {
            EQACycle cycle = result.getCycle();
            LocalDate anchor = anchorDate(result);
            if (cycle == null || anchor == null || anchor.isBefore(windowStart)) {
                continue;
            }
            cycles.put(cycle.getId(), cycle);
            Map<String, Object> row = byCycle.computeIfAbsent(cycle.getId(), id -> {
                Map<String, Object> fresh = new LinkedHashMap<>();
                fresh.put("cycleId", cycle.getId());
                fresh.put("cycleLabel", cycleLabel(cycle));
                fresh.put("schemeName", cycle.getScheme() == null ? null : cycle.getScheme().getName());
                fresh.put("schemeType", cycle.getScheme() == null || cycle.getScheme().getSchemeType() == null ? null
                        : cycle.getScheme().getSchemeType().name());
                fresh.put("status", cycle.getStatus() == null ? null : cycle.getStatus().name());
                fresh.put("scoredCount", 0);
                fresh.put("acceptableCount", 0);
                fresh.put("performance", null);
                fresh.put("submittedAt", null);
                return fresh;
            });

            if (result.getPerformanceStatus() != null) {
                row.put("scoredCount", (int) row.get("scoredCount") + 1);
                if (result.getPerformanceStatus() == EQAPerformanceStatus.ACCEPTABLE) {
                    row.put("acceptableCount", (int) row.get("acceptableCount") + 1);
                }
                String worst = (String) row.get("performance");
                String verdict = VERDICT.get(result.getPerformanceStatus());
                if (worst == null || VERDICT_ORDER.indexOf(verdict) > VERDICT_ORDER.indexOf(worst)) {
                    row.put("performance", verdict);
                }
            }
            LocalDate submitted = toLocalDate(result.getSubmittedAt());
            String known = (String) row.get("submittedAt");
            if (submitted != null && (known == null || submitted.isAfter(LocalDate.parse(known)))) {
                row.put("submittedAt", submitted.toString());
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>(byCycle.values());
        rows.sort(Comparator.comparing((Map<String, Object> row) -> {
            EQACycle cycle = cycles.get((Long) row.get("cycleId"));
            return toLocalDate(cycle.getPlannedStartDate());
        }, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
        return rows;
    }

    private static int severity(EQAPerformanceStatus status) {
        return VERDICT_ORDER.indexOf(VERDICT.get(status));
    }

    private static String cycleLabel(EQACycle cycle) {
        if (cycle.getCycleName() != null && !cycle.getCycleName().isBlank()) {
            return cycle.getCycleName();
        }
        return cycle.getCycleNumber() == null ? String.valueOf(cycle.getId()) : "Cycle " + cycle.getCycleNumber();
    }

    /**
     * When the result counts as scored: the provider's score date if it has one,
     * else the submission, else the cycle's own period. A scored row with no date
     * anywhere would otherwise fall out of every window.
     */
    private static LocalDate anchorDate(EQAParticipantResult result) {
        LocalDate scored = toLocalDate(result.getScoreReceivedAt());
        if (scored != null) {
            return scored;
        }
        LocalDate submitted = toLocalDate(result.getSubmittedAt());
        if (submitted != null) {
            return submitted;
        }
        EQACycle cycle = result.getCycle();
        if (cycle == null) {
            return null;
        }
        LocalDate end = toLocalDate(cycle.getPlannedEndDate());
        return end != null ? end : toLocalDate(cycle.getPlannedStartDate());
    }

    private static LocalDate cycleDeadline(EQACycle cycle) {
        return cycle == null ? null : toLocalDate(cycle.getPlannedEndDate());
    }

    private static LocalDate toLocalDate(Date date) {
        return date == null ? null : new java.sql.Date(date.getTime()).toLocalDate();
    }

    private static Integer rate(int part, int total) {
        return total == 0 ? null : Math.round(part * 100f / total);
    }
}
