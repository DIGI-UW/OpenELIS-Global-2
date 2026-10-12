package org.openelisglobal.microbiology.service;

import java.util.*;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.MicroPatientReportForm;
import org.openelisglobal.microbiology.valueholder.*;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.test.service.TestSectionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MicroPatientReportService {
    @org.springframework.beans.factory.annotation.Autowired
    private org.openelisglobal.testresultcomponent.service.TestResultComponentService components;
    @org.springframework.beans.factory.annotation.Autowired
    private MicrobiologyCaseAccessService access;
    private final MicroCaseDAO cases;
    private final MicroCaseAnalysisDAO links;
    private final MicroCaseInoculationDAO cultures;
    private final MicroIsolateDAO isolates;
    private final MicroOrganismDAO organisms;
    private final MicroAntibioticDAO antibiotics;
    private final MicroAstRunDAO runs;
    private final MicroAstReportChoiceService choices;
    private final MicroBreakpointStandardDAO standards;
    private final MicroCaseTimelineService timeline;
    private final MicroDstService dst;
    private final ResultService results;
    private final IStatusService statuses;
    private final TestSectionService units;
    private final MicroCaseAuthorizationService authorization;
    private final MicroCaseActivityDAO activities;
    private final MicroReportVersionDAO versions;
    private final com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();

    public MicroPatientReportService(MicroCaseDAO cases, MicroCaseAnalysisDAO links, MicroCaseInoculationDAO cultures,
            MicroIsolateDAO isolates, MicroOrganismDAO organisms, MicroAntibioticDAO antibiotics, MicroAstRunDAO runs,
            MicroAstReportChoiceService choices, MicroBreakpointStandardDAO standards,
            MicroCaseTimelineService timeline, MicroDstService dst, ResultService results, IStatusService statuses,
            TestSectionService units, MicroCaseAuthorizationService authorization, MicroCaseActivityDAO activities,
            MicroReportVersionDAO versions) {
        this.cases = cases;
        this.links = links;
        this.cultures = cultures;
        this.isolates = isolates;
        this.organisms = organisms;
        this.antibiotics = antibiotics;
        this.runs = runs;
        this.choices = choices;
        this.standards = standards;
        this.timeline = timeline;
        this.dst = dst;
        this.results = results;
        this.statuses = statuses;
        this.units = units;
        this.authorization = authorization;
        this.activities = activities;
        this.versions = versions;
    }

    public MicroPatientReportForm display(String caseId) {
        var c = cases.get(caseId).orElseThrow();
        if ("FINAL_RELEASED".equals(c.getFinalReleaseState())) {
            var latest = versions.getLatestByCaseId(caseId);
            if (latest != null && latest.getStructuredContent() != null)
                return issued(latest);
        }
        return preview(caseId);
    }

    public MicroPatientReportForm preview(String caseId) {
        var c = cases.get(caseId).orElseThrow();
        var form = new MicroPatientReportForm();
        form.caseId = caseId;
        form.labUnitId = c.getLabUnitId();
        form.labUnitName = units.get(c.getLabUnitId()).getLocalizedName();
        for (var a : links.getAnalyses(caseId)) {
            var link = links.getByCaseAndAnalysis(caseId, a.getId());
            if (!selected(link) || a.getTest().isInLabOnly() || "N".equals(a.getIsReportable())
                    || !"Finalized".equals(statuses.getAnalysisStatusForID(a.getStatusId()).name()))
                continue;
            var definitions = components.getComponentsByTestId(a.getTest().getId());
            var values = new ArrayList<MicroPatientReportForm.Value>();
            var analysisResults = results.getResultsByAnalysis(a);
            for (var result : analysisResults) {
                if ("N".equals(result.getIsReportable()) || result.getParentResult() != null)
                    continue;
                var componentId = result.getTestResult() == null ? null : result.getTestResult().getComponentId();
                var component = definitions.stream()
                        .filter(d -> componentId == null ? d.getIsPrimary() : d.getId().equals(componentId)).findFirst()
                        .orElse(null);
                if (component != null && !component.getIsPrimary() && !component.getShowOnReport())
                    continue;
                if (componentId != null && component == null)
                    throw new IllegalStateException("REPORT_COMPONENT_UNAVAILABLE");
                String label = component != null ? component.getLabel()
                        : result.getAnalyte() == null ? "" : result.getAnalyte().getAnalyteName();
                String value = results.getSimpleResultValue(result);
                if ("D".equals(result.getResultType())) {
                    var quantified = analysisResults.stream()
                            .filter(child -> child.getParentResult() != null
                                    && result.getId().equals(child.getParentResult().getId())
                                    && "A".equals(child.getResultType()) && !"N".equals(child.getIsReportable()))
                            .map(results::getSimpleResultValue).filter(v -> v != null && !v.isBlank()).toList();
                    if (!quantified.isEmpty())
                        value += " " + String.join(" ", quantified);
                }
                values.add(new MicroPatientReportForm.Value(result.getId(), componentId, label, value));
            }
            if (values.isEmpty())
                continue;
            var target = "ADDITIONAL".equals(link.getPlacement()) ? form.additional : form.initial;
            target.add(new MicroPatientReportForm.Test(a.getId(), a.getTest().getName(), values));
            form.validatedResults += values.size();
        }
        for (var row : cultures.getRows(caseId))
            if (row.getOutcome() != null && row.getOutcomeAt() != null)
                form.cultures.add(new MicroPatientReportForm.Culture(row.getId(), row.getContainerIdentifier(),
                        row.getOutcome(), row.getOccurredAt(), row.getOutcomeAt(),
                        MicroCultureTiming.hours(row.getOccurredAt(), row.getOutcomeAt())));
        for (var isolate : isolates.getByCaseId(caseId)) {
            if (isolate.getCancelledAt() != null || !"CONFIRMED".equals(isolate.getIdentificationStatus())
                    || isolate.getOrganismId() == null)
                continue;
            var rows = new ArrayList<MicroPatientReportForm.Susceptibility>();
            for (var reading : choices.getSelectedReadings(isolate.getId())) {
                var run = runs.get(reading.getAstRunId()).orElseThrow();
                var standard = standards.get(run.getBreakpointStandardId()).orElseThrow();
                var antibiotic = antibiotics.get(reading.getAntibioticId()).orElseThrow();
                rows.add(new MicroPatientReportForm.Susceptibility(antibiotic.getId(), antibiotic.getDisplayName(),
                        reading.getId(), run.getId(), reading.getRawText(), reading.getUnits(),
                        MicroAstReportChoiceService.interpretation(reading), standard.getAuthority(),
                        run.getBreakpointVersion(), reading.getMatchedBy()));
            }
            rows.sort(Comparator.comparing(MicroPatientReportForm.Susceptibility::agent));
            form.validatedResults += rows.size();
            form.isolates.add(new MicroPatientReportForm.Isolate(isolate.getId(), isolate.getIsolateLabel(),
                    organisms.get(isolate.getOrganismId()).orElseThrow().getDisplayName(), rows));
        }
        form.classification = dst.evaluate(caseId).classification;
        form.notes = timeline.getReportNotes(caseId).stream()
                .map(n -> new MicroPatientReportForm.Note(n.note, n.performedByDisplay, n.occurredAt)).toList();
        return form;
    }

    public static boolean selected(MicroCaseAnalysis link) {
        return link.getCancelledAt() == null && (link.getSelectedForReport() != null ? link.getSelectedForReport()
                : !"ADDITIONAL".equals(link.getPlacement()));
    }

    @Transactional
    public void selectAnalysis(String caseId, String analysisId, boolean selected, String actor) {
        authorization.requireWrite(caseId, actor, Constants.ROLE_VALIDATION);
        var link = links.getByCaseAndAnalysis(caseId, analysisId);
        if (link == null || link.getCancelledAt() != null)
            throw new IllegalArgumentException("Case analysis not found");
        var analysis = links.getAnalyses(caseId).stream().filter(a -> a.getId().equals(analysisId)).findFirst()
                .orElseThrow();
        if (selected && (analysis.getTest().isInLabOnly() || "N".equals(analysis.getIsReportable())))
            throw new IllegalArgumentException("RESULT_NOT_REPORTABLE");
        boolean before = selected(link);
        link.setSelectedForReport(selected);
        links.update(link);
        var event = new MicroCaseActivity();
        event.setCaseId(caseId);
        event.setActivityType("REPORT_RESULT_SELECTED");
        event.setPerformedBy(actor);
        event.setOccurredAt(MicroCaseServiceImpl.now());
        event.setStructuredData(encode(Map.of("analysisId", analysisId, "before", before, "after", selected)));
        activities.insert(event);
    }

    public List<MicroPatientReportForm.Version> versions(String caseId) {
        return versions.getByCaseId(caseId).stream().filter(v -> v.getStructuredContent() != null)
                .map(v -> new MicroPatientReportForm.Version(v.getId(), v.getVersionNumber(), v.getReleaseType(),
                        v.getReleasedAt(), v.getReleasedBy(), decode(v.getStructuredContent())))
                .toList();
    }

    public List<org.openelisglobal.analysis.valueholder.Analysis> issuedAnalysesForSample(String sampleId) {
        return links.getIssuedAnalysesForSample(sampleId);
    }

    public Boolean mayReportAnalysis(String analysisId, String actor) {
        var link = links.getActiveByAnalysisId(analysisId);
        if (link == null)
            return null;
        var c = cases.get(link.getCaseId()).orElseThrow();
        return access.hasLabUnitRole(actor, c.getLabUnitId(), Constants.ROLE_REPORTS);
    }

    public MicroPatientReportForm issuedForAnalysis(String analysisId) {
        var link = links.getActiveByAnalysisId(analysisId);
        if (link == null)
            return null;
        var latest = versions.getLatestByCaseId(link.getCaseId());
        return latest == null || latest.getStructuredContent() == null ? null : issued(latest);
    }

    private MicroPatientReportForm issued(MicroReportVersion version) {
        var report = decode(version.getStructuredContent());
        report.releaseType = version.getReleaseType();
        report.reportVersion = version.getVersionNumber();
        report.releasedAt = version.getReleasedAt();
        return report;
    }

    public String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public MicroPatientReportForm decode(String value) {
        try {
            return json.readValue(value, MicroPatientReportForm.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("REPORT_SNAPSHOT_INVALID", e);
        }
    }
}
