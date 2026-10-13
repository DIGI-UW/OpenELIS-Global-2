package org.openelisglobal.microbiology.service;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.registration.ResultUpdateRegister;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.result.action.util.*;
import org.openelisglobal.result.service.LogbookResultsPersistService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.resultvalidation.service.ResultSelfValidationPolicy;
import org.openelisglobal.resultvalidation.service.ResultValidationService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class MicroCaseResultServiceImpl implements MicroCaseResultService {
    @org.springframework.beans.factory.annotation.Autowired
    private org.openelisglobal.dataexchange.fhir.service.FhirTransformService fhir;
    @org.springframework.beans.factory.annotation.Autowired
    private org.openelisglobal.fhir.service.ObservationTransformService observationTransform;
    @org.springframework.beans.factory.annotation.Autowired
    private org.openelisglobal.samplehuman.service.SampleHumanService sampleHumans;
    @org.springframework.beans.factory.annotation.Autowired
    private org.openelisglobal.panel.service.PanelService panelCatalog;
    @org.springframework.beans.factory.annotation.Autowired
    private org.openelisglobal.typeofsample.service.TypeOfSamplePanelService samplePanels;
    @org.springframework.beans.factory.annotation.Autowired
    private MicroCaseInoculationDAO cultureRows;
    private final MicroCaseDAO cases;
    private final MicroCaseAnalysisDAO links;
    private final MicroCaseSearchDAO search;
    private final MicrobiologyCaseAccessService access;
    private final AnalysisService analyses;
    private final TestService tests;
    private final TypeOfSampleTestService sampleTests;
    private final PanelItemService panels;
    private final ObjectProvider<ResultsLoadUtility> loaders;
    private final LogbookResultsPersistService persist;
    private final ResultService results;
    private final org.openelisglobal.result.service.ResultEntryAcknowledgementService acknowledgements;
    private final IStatusService statuses;
    private final SystemUserService users;
    private final OrganizationService organizations;
    private final ResultSelfValidationPolicy selfValidation;
    private final ResultValidationService validation;
    private final MicroCaseActivityDAO activities;

    public MicroCaseResultServiceImpl(MicroCaseDAO cases, MicroCaseAnalysisDAO links, MicroCaseSearchDAO search,
            MicrobiologyCaseAccessService access, AnalysisService analyses, TestService tests,
            TypeOfSampleTestService sampleTests, PanelItemService panels, ObjectProvider<ResultsLoadUtility> loaders,
            LogbookResultsPersistService persist, ResultService results, IStatusService statuses,
            SystemUserService users, OrganizationService organizations, ResultSelfValidationPolicy selfValidation,
            ResultValidationService validation, MicroCaseActivityDAO activities,
            org.openelisglobal.result.service.ResultEntryAcknowledgementService acknowledgements) {
        this.cases = cases;
        this.links = links;
        this.search = search;
        this.access = access;
        this.analyses = analyses;
        this.tests = tests;
        this.sampleTests = sampleTests;
        this.panels = panels;
        this.loaders = loaders;
        this.persist = persist;
        this.results = results;
        this.statuses = statuses;
        this.users = users;
        this.organizations = organizations;
        this.selfValidation = selfValidation;
        this.validation = validation;
        this.activities = activities;
        this.acknowledgements = acknowledgements;
    }

    @Override
    public List<MicroCaseTestForm> getTests(String caseId, String actor) {
        if (actor == null || actor.isBlank())
            throw new AccessDeniedException("Authenticated system user required");
        var c = cases.get(caseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var list = links.getAnalyses(caseId);
        var loader = loaders.getObject();
        loader.setSysUser(actor);
        var rows = loader.getGroupedTestsForAnalysisList(list, true);
        List<MicroCaseTestForm> forms = new ArrayList<>();
        for (var a : list) {
            var link = links.getByCaseAndAnalysis(caseId, a.getId());
            MicroCaseTestForm f = new MicroCaseTestForm();
            f.analysisId = a.getId();
            f.reportable = !a.getTest().isInLabOnly() && !"N".equals(a.getIsReportable());
            f.selectedForReport = f.reportable && MicroPatientReportService.selected(link);
            f.testId = a.getTest().getId();
            f.sampleItemId = a.getSampleItem().getId();
            f.testName = a.getTest().getName();
            f.placement = link.getCultureId() != null ? "CULTURE"
                    : "ADDITIONAL".equals(link.getPlacement()) ? "ADDITIONAL" : "INITIAL";
            f.cultureId = link.getCultureId();
            f.status = state(a);
            f.version = version(a, link);
            f.enteredBy = link.getEnteredBy();
            f.testedElsewhere = link.getTestedElsewhere();
            f.performingLabId = link.getPerformingLabId();
            f.performingUserId = link.getPerformingUserId();
            f.performedAt = link.getPerformedAt() == null ? null
                    : link.getPerformedAt().toLocalDateTime().toLocalDate().toString();
            f.performedByDisplay = display(link);
            boolean mutable = mutable(c) && !List.of("Finalized", "Canceled", "SampleRejected").contains(f.status);
            f.canEdit = mutable && access.hasLabUnitRole(actor, c.getLabUnitId(), Constants.ROLE_RESULTS);
            f.selfValidationBlocked = selfValidation.blocked(a, actor);
            f.canValidate = mutable && "TechnicalAcceptance".equals(f.status) && !f.selfValidationBlocked
                    && access.hasLabUnitRole(actor, c.getLabUnitId(), Constants.ROLE_VALIDATION);
            f.components = rows.stream().filter(row -> a.getId().equals(row.getAnalysisId())).toList();
            forms.add(f);
        }
        return forms;
    }

    @Override
    public MicroCaseTestCatalogForm getCatalog(String caseId, String sampleTypeId, String actor) {
        var c = cases.get(caseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!access.hasLabUnitRole(actor, c.getLabUnitId(), Constants.ROLE_RESULTS))
            throw new AccessDeniedException("Case lab unit access required");
        MicroCaseMutationGuard.requireMutable(c);
        if (cultureRows.getSources(caseId).stream()
                .noneMatch(s -> !s.isRejected() && sampleTypeId.equals(s.getTypeOfSampleId())))
            bad("MICROBIOLOGY_SAMPLE_CASE_MISMATCH");
        var catalog = new MicroCaseTestCatalogForm();
        catalog.tests = sampleTests.getTypeOfSampleTestsForSampleType(sampleTypeId).stream()
                .map(mapping -> tests.get(mapping.getTestId())).filter(Objects::nonNull)
                .filter(t -> t.isActive() && Boolean.TRUE.equals(t.getOrderable()))
                .map(t -> new MicroCaseTestCatalogForm.Option(t.getId(), t.getName())).toList();
        var allowed = catalog.tests.stream().map(MicroCaseTestCatalogForm.Option::id).toList();
        catalog.panels = samplePanels.getTypeOfSamplePanelsForSampleType(sampleTypeId).stream()
                .map(mapping -> panelCatalog.getPanelById(mapping.getPanelId())).filter(Objects::nonNull)
                .filter(p -> "Y".equals(p.getIsActive()) && panels.getPanelItemsForPanel(p.getId()).stream()
                        .anyMatch(item -> allowed.contains(item.getTest().getId())))
                .map(p -> new MicroCaseTestCatalogForm.Option(p.getId(), p.getPanelName())).toList();
        return catalog;
    }

    @Override
    @Transactional
    public List<MicroCaseTestForm> addTests(String caseId, MicroCaseAddTestsForm request, String actor) {
        var c = writeCase(caseId, actor, Constants.ROLE_RESULTS);
        if (request == null || !List.of("INITIAL", "ADDITIONAL", "CULTURE").contains(request.placement))
            bad("MICROBIOLOGY_PLACEMENT_REQUIRED");
        var specimen = ("CULTURE".equals(request.placement) ? cultureRows.getSources(caseId)
                : search.getSamples(caseId)).stream().filter(s -> s.getId().equals(request.sampleItemId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("MICROBIOLOGY_SAMPLE_CASE_MISMATCH"));
        if ("CULTURE".equals(request.placement)) {
            var culture = cultureRows.get(request.cultureId)
                    .orElseThrow(() -> new IllegalArgumentException("MICROBIOLOGY_CULTURE_REQUIRED"));
            if (!caseId.equals(culture.getCaseId()) || !request.sampleItemId.equals(culture.getSourceSampleItemId()))
                bad("MICROBIOLOGY_CULTURE_SOURCE");
            if ("NO_GROWTH".equals(culture.getOutcome()) || "CONTAMINATED".equals(culture.getOutcome()))
                bad("MICROBIOLOGY_CULTURE_CONCLUDED");
        } else if (request.cultureId != null)
            bad("MICROBIOLOGY_CULTURE_PLACEMENT");
        if (specimen.isRejected())
            bad("MICROBIOLOGY_SAMPLE_REJECTED");
        Set<String> ids = new LinkedHashSet<>(request.testIds == null ? List.of() : request.testIds);
        Set<String> allowed = new HashSet<>();
        sampleTests.getTypeOfSampleTestsForSampleType(specimen.getTypeOfSample().getId())
                .forEach(t -> allowed.add(t.getTestId()));
        Map<String, org.openelisglobal.panel.valueholder.Panel> selectedPanels = new LinkedHashMap<>();
        var allowedPanels = samplePanels.getTypeOfSamplePanelsForSampleType(specimen.getTypeOfSampleId()).stream()
                .map(org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel::getPanelId).toList();
        for (String panel : request.panelIds == null ? List.<String>of() : request.panelIds) {
            var requestedPanel = panelCatalog.getPanelById(panel);
            if (requestedPanel == null || !"Y".equals(requestedPanel.getIsActive()) || !allowedPanels.contains(panel))
                bad("MICROBIOLOGY_INVALID_PANEL");
            var items = panels
                    .getPanelItemsForPanel(panel).stream().filter(item -> allowed.contains(item.getTest().getId())
                            && item.getTest().isActive() && Boolean.TRUE.equals(item.getTest().getOrderable()))
                    .toList();
            if (items.isEmpty())
                bad("MICROBIOLOGY_INVALID_PANEL");
            items.forEach(item -> {
                ids.add(item.getTest().getId());
                selectedPanels.putIfAbsent(item.getTest().getId(), item.getPanel());
            });
        }
        if (ids.isEmpty() || ids.size() > 100)
            bad("MICROBIOLOGY_TEST_SELECTION_REQUIRED");
        var selected = ids.stream().map(tests::get).toList();
        if (selected.stream().anyMatch(t -> t == null || !t.isActive() || !Boolean.TRUE.equals(t.getOrderable())
                || !allowed.contains(t.getId())))
            bad("MICROBIOLOGY_TEST_SAMPLE_MISMATCH");
        var existing = links.getAnalyses(caseId);
        List<String> changedAnalysisIds = new ArrayList<>();
        for (var t : selected) {
            var prior = existing.stream()
                    .filter(a -> t.getId().equals(a.getTest().getId())
                            && specimen.getId().equals(a.getSampleItem().getId())
                            && Objects.equals(request.cultureId,
                                    links.getByCaseAndAnalysis(caseId, a.getId()).getCultureId()))
                    .findFirst().orElse(null);
            if (prior != null) {
                var panel = selectedPanels.get(t.getId());
                if (panel != null && prior.getPanel() == null) {
                    prior.setPanel(panel);
                    prior.setSysUserId(actor);
                    analyses.update(prior);
                    changedAnalysisIds.add(prior.getId());
                    record(caseId, "TEST_PANEL_ASSIGNED", prior.getId(), actor);
                } else if (panel != null && !panel.getId().equals(prior.getPanel().getId())) {
                    bad("MICROBIOLOGY_INVALID_PANEL");
                }
                continue;
            }
            Analysis a = new Analysis();
            a.setTest(t);
            a.setPanel(selectedPanels.get(t.getId()));
            a.setTestSection(t.getTestSection());
            a.setSampleItem(specimen);
            a.setIsReportable(t.getIsReportable());
            a.setAnalysisType("MANUAL");
            a.setRevision("1");
            a.setStatusId(statuses.getStatusID(AnalysisStatus.NotStarted));
            a.setStartedDate(MicroCaseServiceImpl.now());
            a.setSysUserId(actor);
            a.setFhirUuid(UUID.randomUUID());
            a.setId(analyses.insert(a));
            changedAnalysisIds.add(a.getId());
            var link = new MicroCaseAnalysis();
            link.setCaseId(caseId);
            link.setAnalysisId(a.getId());
            link.setPlacement(request.placement);
            link.setCultureId(request.cultureId);
            link.setSysUserId(actor);
            links.insert(link);
            record(caseId, "TEST_ADDED", a.getId(), actor);
        }
        if (!changedAnalysisIds.isEmpty()) {
            var idsToPublish = List.copyOf(changedAnalysisIds);
            afterCommit(() -> {
                try {
                    fhir.transformAnalysisByIds(idsToPublish);
                } catch (org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException
                        | org.openelisglobal.dataexchange.fhir.exception.FhirPersistanceException failure) {
                    org.openelisglobal.common.log.LogEvent.logError(failure);
                }
            });
        }
        return getTests(caseId, actor);
    }

    @Override
    @Transactional
    public List<MicroCaseTestForm> saveResults(String caseId, String analysisId, MicroCaseResultRequestForm request,
            String actor, HttpServletRequest httpRequest) {
        writeCase(caseId, actor, Constants.ROLE_RESULTS);
        persistResult(caseId, analysisId, request, actor, httpRequest, true);
        return getTests(caseId, actor);
    }

    /**
     * Trusted ingestion keeps the normal component validation and review-pending
     * status.
     */
    @Override
    @Transactional
    public void receiveResult(String caseId, String analysisId, MicroCaseResultRequestForm request, String actor) {
        var c = cases.getForUpdate(caseId);
        if (c == null || actor == null || actor.isBlank())
            throw new IllegalArgumentException("MICROBIOLOGY_INCOMING_REQUIRED");
        MicroCaseMutationGuard.requireMutable(c);
        persistResult(caseId, analysisId, request, actor, null, false);
    }

    private void persistResult(String caseId, String analysisId, MicroCaseResultRequestForm request, String actor,
            HttpServletRequest httpRequest, boolean checkToken) {
        var a = requireAnalysis(caseId, analysisId);
        if (checkToken)
        checkVersion(a, links.getByCaseAndAnalysis(caseId, analysisId), request == null ? null : request.version);
        requireEditable(a);
        if (!checkToken && !"NotStarted".equals(state(a)))
            bad("MICROBIOLOGY_INCOMING_EXISTING_RESULT");
        if (request.note != null && request.note.length() > 4000)
            bad("MICROBIOLOGY_NOTE_TOO_LONG");
        if (request.components == null || request.components.isEmpty())
            bad("MICROBIOLOGY_RESULT_REQUIRED");
        var loader = loaders.getObject();
        loader.setSysUser(actor);
        var rows = loader.getGroupedTestsForAnalysisList(List.of(a), true);
        Map<String, TestResultItem> byComponent = new HashMap<>();
        rows.forEach(row -> byComponent.put(row.getTestResultComponentId(), row));
        Set<String> supplied = new HashSet<>();
        List<TestResultItem> changed = new ArrayList<>();
        for (var input : request.components) {
            if (input == null || !supplied.add(input.componentId) || !byComponent.containsKey(input.componentId))
                bad("MICROBIOLOGY_COMPONENT_MISMATCH");
            var row = byComponent.get(input.componentId);
            row.setResultValue(input.value == null ? "" : input.value);
            row.setMultiSelectResultValues(input.multiSelectResultValues);
            validateComponent(row, input);
            row.setNote(request.note);
            row.setCriticalAcknowledged(input.criticalAcknowledged);
            row.setInvalidResultConfirmed(input.invalidResultConfirmed);
            row.setModified(true);
            row.setReadOnly(false);
            row.setTechnician(actor);
            changed.add(row);
        }
        if (changed.stream().noneMatch(ResultUtil::areResults))
            bad("MICROBIOLOGY_RESULT_REQUIRED");
        var data = new ResultsUpdateDataSet(actor);
        data.filterModifiedItems(changed);
        // Clear only explicitly submitted components, preserving their siblings.
        for (var row : changed) {
            if (!ResultUtil.areResults(row)) {
                results.getResultsByAnalysis(a).stream()
                        .filter(result -> result.getTestResult() != null && Objects
                                .equals(row.getTestResultComponentId(), result.getTestResult().getComponentId()))
                        .forEach(data.getDeletableResults()::add);
            }
        }
        var errors = data.validateModifiedItems();
        if (errors.hasErrors())
            bad("MICROBIOLOGY_INVALID_RESULT");
        var alerts = acknowledgements.alertsForItems(changed);
        if (alerts.stream().anyMatch(alert -> !alert.isAcknowledged()))
            throw new MicroCaseResultAcknowledgementException(
                    acknowledgements.refusalBody(alerts.stream().filter(alert -> !alert.isAcknowledged()).toList()));
        ResultUtil.createResultsFromItems(data, false, false, httpRequest != null, "", httpRequest);
        // The case editor submits one analysis note, not one copy per component.
        // ResultUtil compiles a note from each component carrying that text.
        data.getNoteList().forEach(note -> note.setTestResultComponentId(null));
        Set<List<String>> noteKeys = new HashSet<>();
        data.getNoteList().removeIf(note -> !noteKeys.add(Arrays.asList(note.getReferenceId(),
                note.getReferenceTableId(), note.getNoteType(), note.getSubject(), note.getText())));
        // Notes can retain a blank component in modifiedItems. Never reload a result
        // that this same save deletes; its cancellation is published separately.
        var clearedIds = data.getDeletableResults().stream().map(r -> r.getId()).toList();
        data.getModifiedResults().removeIf(result -> clearedIds.contains(result.result.getId()));
        // Case entry always awaits expert review, including sites that auto-validate
        // ordinary results.
        for (var modified : data.getModifiedAnalysis())
            modified.setStatusId(statuses.getStatusID(AnalysisStatus.TechnicalAcceptance));
        var updaters = new ArrayList<>(ResultUpdateRegister.getRegisteredUpdaters());
        updaters.add(acknowledgements.acknowledgementRecorder(alerts,
                org.openelisglobal.result.valueholder.ResultEntryAcknowledgement.SOURCE_RESULTS_ENTRY, actor));
        // Compile cancellations before deletion; the async publisher cannot reload
        // removed results or traverse their detached relationships after commit.
        var clearedObservations = data.getDeletableResults().stream()
                .map(observationTransform::transformResultToObservation).toList();
        persist.persistDataSet(data, updaters, actor);
        afterCommit(() -> {
            try {
                fhir.transformPersistResultsEntryFhirObjects(data, clearedObservations);
            } catch (org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException
                    | org.openelisglobal.dataexchange.fhir.exception.FhirPersistanceException failure) {
                org.openelisglobal.common.log.LogEvent.logError(failure);
            }
        });
        var link = links.getByCaseAndAnalysis(caseId, analysisId);
        link.setEnteredBy(actor);
        if (!link.getTestedElsewhere()) {
            link.setPerformingUserId(actor);
            link.setPerformedAt(MicroCaseServiceImpl.now());
        }
        link.setSysUserId(actor);
        links.update(link);
        record(caseId, "RESULT_ENTERED", analysisId, actor);
    }

    @Override
    @Transactional
    public List<MicroCaseTestForm> setTestedElsewhere(String caseId, String analysisId,
            MicroCaseTestedElsewhereRequestForm request, String actor) {
        writeCase(caseId, actor, Constants.ROLE_RESULTS);
        var a = requireAnalysis(caseId, analysisId);
        checkVersion(a, links.getByCaseAndAnalysis(caseId, analysisId), request == null ? null : request.version);
        requireEditable(a);
        Timestamp when = null;
        if (request.testedElsewhere) {
            boolean lab = request.performingLabId != null && !request.performingLabId.isBlank();
            boolean user = request.performingUserId != null && !request.performingUserId.isBlank();
            if (lab == user)
                bad("MICROBIOLOGY_PERFORMER_REQUIRED");
            if (lab && organizations.getOrganizationById(request.performingLabId) == null
                    || user && users.get(request.performingUserId) == null)
                bad("MICROBIOLOGY_PERFORMER_UNKNOWN");
            try {
                var performed = LocalDate.parse(request.performedAt);
                if (performed.isAfter(LocalDate.now()))
                    bad("MICROBIOLOGY_PERFORMED_DATE_REQUIRED");
                when = Timestamp.valueOf(performed.atStartOfDay());
            } catch (RuntimeException invalid) {
                bad("MICROBIOLOGY_PERFORMED_DATE_REQUIRED");
            }
        }
        var link = links.getByCaseAndAnalysis(caseId, analysisId);
        link.setTestedElsewhere(request.testedElsewhere);
        link.setPerformingLabId(request.testedElsewhere ? request.performingLabId : null);
        link.setPerformingUserId(request.testedElsewhere ? request.performingUserId : null);
        link.setPerformedAt(when);
        link.setSysUserId(actor);
        links.update(link);
        a.setSysUserId(actor);
        analyses.update(a);
        record(caseId, "TESTED_ELSEWHERE", analysisId, actor);
        return getTests(caseId, actor);
    }

    @Override
    @Transactional
    public List<MicroCaseTestForm> validateResult(String caseId, String analysisId, String token, String actor) {
        writeCase(caseId, actor, Constants.ROLE_VALIDATION);
        var a = requireAnalysis(caseId, analysisId);
        checkVersion(a, links.getByCaseAndAnalysis(caseId, analysisId), token);
        if (!"TechnicalAcceptance".equals(state(a)))
            bad("MICROBIOLOGY_RESULT_NOT_AWAITING_VALIDATION");
        selfValidation.requireAnotherValidator(a, actor);
        a.setStatusId(statuses.getStatusID(AnalysisStatus.Finalized));
        a.setReleasedDate(MicroCaseServiceImpl.now());
        a.setSysUserId(actor);
        var saved = new ArrayList<>(results.getResultsByAnalysis(a));
        var data = new ResultsUpdateDataSet(actor);
        var sample = a.getSampleItem().getSample();
        var patient = sampleHumans.getPatientForSample(sample);
        for (var result : saved) {
            result.setResultEvent(org.openelisglobal.dataexchange.orderresult.OrderResponseWorker.Event.FINAL_RESULT);
            data.getNewResults().add(new ResultSet(result, null, null, patient, sample, null, false));
        }
        var context = new org.openelisglobal.resultvalidation.bean.AnalysisItem();
        context.setAccessionNumber(sample.getAccessionNumber());
        var sampleUpdates = new ArrayList<org.openelisglobal.sample.valueholder.Sample>();
        validation.persistdata(List.of(), List.of(a), saved, List.of(context), sampleUpdates, new ArrayList<>(), data,
                org.openelisglobal.common.services.registration.ValidationUpdateRegister.getRegisteredUpdaters(),
                actor);
        afterCommit(() -> {
            try {
                fhir.transformPersistResultValidationFhirObjects(List.of(), List.of(a), saved, List.of(context),
                        sampleUpdates, new ArrayList<>());
            } catch (org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException failure) {
                org.openelisglobal.common.log.LogEvent.logError(failure);
            }
        });
        record(caseId, "RESULT_VALIDATED", analysisId, actor);
        return getTests(caseId, actor);
    }

    private void validateComponent(TestResultItem row, MicroCaseResultRequestForm.ResultComponent input) {
        String value = input.value == null ? "" : input.value.trim();
        Set<String> options = (row.getDictionaryResults() == null
                ? List.<org.openelisglobal.common.util.IdValuePair>of()
                : row.getDictionaryResults()).stream().map(org.openelisglobal.common.util.IdValuePair::getId)
                .collect(java.util.stream.Collectors.toSet());
        if ("D".equals(row.getResultType()) && !value.isEmpty() && !options.contains(value))
            bad("MICROBIOLOGY_INVALID_RESULT_OPTION");
        if ("M".equals(row.getResultType()) || "C".equals(row.getResultType())) {
            try {
                var groups = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(input.multiSelectResultValues == null ? "{}" : input.multiSelectResultValues);
                if (groups == null || !groups.isObject())
                    bad("MICROBIOLOGY_INVALID_RESULT_OPTION");
                boolean selected = false;
                var values = groups.fields();
                while (values.hasNext()) {
                    var entry = values.next();
                    Integer.parseInt(entry.getKey());
                    var group = entry.getValue();
                    if (!group.isTextual())
                        bad("MICROBIOLOGY_INVALID_RESULT_OPTION");
                    for (String id : group.asText().split(",")) {
                        if (!id.isBlank()) {
                            if (!options.contains(id))
                                bad("MICROBIOLOGY_INVALID_RESULT_OPTION");
                            selected = true;
                        }
                    }
                }
                // The empty selector emits {} (or blank groups), which is not a
                // result. Normalize it for the shared presence/deletion checks.
                if (!selected) {
                    row.setResultValue("");
                    row.setMultiSelectResultValues(null);
                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException | NumberFormatException invalid) {
                bad("MICROBIOLOGY_INVALID_RESULT_OPTION");
            }
        }
        if ("N".equals(row.getResultType()) && !value.isEmpty()) {
            try {
                String normalized = org.openelisglobal.common.util.StringUtil
                        .normalizeScientificNotation(value.replaceFirst("^[<>]=?\\s*", ""));
                new java.math.BigDecimal(normalized); // Validate the entire number, including its exponent.
                int exponent = Math.max(normalized.indexOf('e'), normalized.indexOf('E'));
                String mantissa = exponent < 0 ? normalized : normalized.substring(0, exponent);
                int dot = mantissa.indexOf('.');
                int places = dot < 0 ? 0 : mantissa.length() - dot - 1;
                int configured = row.getSignificantDigits();
                if (!Objects.equals(value, row.getRawResultValue()) && configured >= 0
                        && (exponent < 0 || configured > 0) && places > configured)
                    bad("MICROBIOLOGY_INVALID_PRECISION");
            } catch (NumberFormatException invalid) {
                bad("MICROBIOLOGY_INVALID_RESULT");
            }
        }
    }

    private MicroCase writeCase(String id, String actor, String role) {
        var c = cases.getForUpdate(id);
        if (c == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (!access.hasLabUnitRole(actor, c.getLabUnitId(), role))
            throw new AccessDeniedException("Case lab unit access required");
        MicroCaseMutationGuard.requireMutable(c);
        return c;
    }

    private Analysis requireAnalysis(String caseId, String id) {
        if (links.getByCaseAndAnalysis(caseId, id) == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return links.getAnalyses(caseId).stream().filter(a -> a.getId().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private String state(Analysis a) {
        return statuses.getAnalysisStatusForID(a.getStatusId()).name();
    }

    static String version(Analysis a, MicroCaseAnalysis link) {
        String analysisVersion = a.getLastupdated() == null ? "" : a.getLastupdated().toInstant().toString();
        String provenanceVersion = link.getLastupdated() == null ? "" : link.getLastupdated().toInstant().toString();
        return analysisVersion + "/" + provenanceVersion;
    }

    private void checkVersion(Analysis a, MicroCaseAnalysis link, String token) {
        if (token == null || !token.equals(version(a, link)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MICROBIOLOGY_STALE_RESULT");
    }

    private void requireEditable(Analysis a) {
        if (List.of("Finalized", "Canceled", "SampleRejected").contains(state(a)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MICROBIOLOGY_RESULT_LOCKED");
    }

    private boolean mutable(MicroCase c) {
        try {
            MicroCaseMutationGuard.requireMutable(c);
            return true;
        } catch (MicroCaseLockedException ex) {
            return false;
        }
    }

    private String display(MicroCaseAnalysis link) {
        if (link.getPerformingLabId() != null) {
            var org = organizations.getOrganizationById(link.getPerformingLabId());
            return org == null ? null : org.getOrganizationName();
        }
        if (link.getPerformingUserId() != null) {
            var user = users.get(link.getPerformingUserId());
            return user == null ? null : user.getFirstName() + " " + user.getLastName();
        }
        return null;
    }

    private void record(String caseId, String type, String analysisId, String actor) {
        var activity = new MicroCaseActivity();
        activity.setCaseId(caseId);
        activity.setActivityType(type);
        activity.setOccurredAt(MicroCaseServiceImpl.now());
        activity.setPerformedBy(actor);
        activity.setStructuredData("{\"analysisId\":\"" + analysisId + "\"}");
        activity.setSysUserId(actor);
        activities.insert(activity);
    }

    private void afterCommit(Runnable work) {
        org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            work.run();
                        } catch (RuntimeException failure) {
                            org.openelisglobal.common.log.LogEvent.logError(failure);
                        }
                    }
                });
    }

    private static void bad(String code) {
        throw new IllegalArgumentException(code);
    }
}
