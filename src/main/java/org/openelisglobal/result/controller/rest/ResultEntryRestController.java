package org.openelisglobal.result.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.StaleObjectStateException;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.service.AnalysisServiceImpl;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.audittrail.dao.HistoryDAO;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.formfields.FormFields;
import org.openelisglobal.common.formfields.FormFields.Field;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.registration.ResultUpdateRegister;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dataexchange.fhir.exception.FhirPersistanceException;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.result.action.util.ResultUtil;
import org.openelisglobal.result.action.util.ResultsUpdateDataSet;
import org.openelisglobal.result.controller.LogbookResultsBaseController;
import org.openelisglobal.result.form.LogbookResultsForm;
import org.openelisglobal.result.form.SingleResultEntryForm;
import org.openelisglobal.result.service.AnalysisTimelineService;
import org.openelisglobal.result.service.LogbookResultsPersistService;
import org.openelisglobal.result.service.ResultEntryPresenceService;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testalertrule.service.TestAlertEvaluationService;
import org.openelisglobal.testreagentlink.service.TestReagentLinkService;
import org.openelisglobal.testreagentlink.valueholder.TestReagentLink;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.testresultinterpretation.service.TestResultInterpretationService;
import org.openelisglobal.testresultinterpretation.valueholder.TestResultInterpretation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * REST surface for the unified Results worklist (OGC-1020, slice R1 of
 * OGC-811).
 *
 * <p>
 * Concurrency model (multi-component FRS §O — optimistic, never locking):
 * <ul>
 * <li><b>FR-O1</b> — the save payload is one analysis ({@code
 * SingleResultEntryForm}); saving a row can never write another row.</li>
 * <li><b>FR-O2</b> — the client round-trips {@code analysisLastupdated} (loaded
 * via {@code ResultsLoadUtility}); a mismatch is rejected 409 naming who saved
 * and when. Hibernate's {@code @Version} on {@code
 * Analysis.lastupdated} remains the transactional backstop.</li>
 * <li><b>FR-O3</b> — session-bound, in-memory presence; advisory only.</li>
 * </ul>
 *
 * Audit: result/analysis writes are recorded automatically in the {@code
 * history} table by the audited services (activity 'I' = RESULT_SAVED, 'U' =
 * RESULT_MODIFIED semantics; the single-char activity column predates named
 * events).
 */
@Controller
@RequestMapping(value = "/rest/results-entry")
public class ResultEntryRestController extends LogbookResultsBaseController {

    @Autowired
    private AnalysisService analysisService;

    @Autowired(required = false)
    private org.openelisglobal.notifications.service.HeaderNotificationService headerNotificationService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private LogbookResultsPersistService logbookPersistService;
    @Autowired
    private FhirTransformService fhirTransformService;
    @Autowired
    private ResultEntryPresenceService presenceService;
    @Autowired
    private HistoryDAO historyDAO;
    @Autowired(required = false)
    private TestAlertEvaluationService testAlertEvaluationService;
    @Autowired
    private AnalysisTimelineService analysisTimelineService;
    @Autowired
    private TestResultComponentService testResultComponentService;
    @Autowired
    private TestResultInterpretationService interpretationService;
    @Autowired
    private TestReagentLinkService testReagentLinkService;
    @Autowired
    private org.openelisglobal.result.service.ResultService resultService;
    @Autowired
    private org.openelisglobal.inventory.service.InventoryItemService inventoryItemService;

    /**
     * Lab Units the user may enter results for, each carrying its domain so the
     * page can derive {@code currentDomain} (FR-M1).
     */
    @GetMapping(value = "lab-units", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasRole('RESULTS')")
    public List<Map<String, String>> getUserLabUnits(HttpServletRequest request) {
        Role resultsRole = roleService.getRoleByName(Constants.ROLE_RESULTS);
        if (resultsRole == null) {
            return Collections.emptyList();
        }
        List<IdValuePair> sections = userService.getUserTestSections(getSysUserId(request), resultsRole.getId());
        List<Map<String, String>> labUnits = new ArrayList<>();
        for (IdValuePair pair : sections) {
            Map<String, String> unit = new HashMap<>();
            unit.put("id", pair.getId());
            unit.put("value", pair.getValue());
            TestSection section = testSectionService.get(pair.getId());
            unit.put("domain",
                    section != null && !GenericValidator.isBlankOrNull(section.getDomain()) ? section.getDomain()
                            : "CLINICAL");
            labUnits.add(unit);
        }
        return labUnits;
    }

    /**
     * OGC-1022 (R3, FR-H1/H2) — this analysis's own event timeline, newest first,
     * paginated 25/50/100. Creation, status transitions, result value changes,
     * bound notes, retest revisions and reflex children — never patient trends or
     * Westgard statistics (D7).
     */
    @GetMapping(value = "analysis/{analysisId}/history", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasRole('RESULTS')")
    public ResponseEntity<Map<String, Object>> getAnalysisHistory(@PathVariable String analysisId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "25") int pageSize,
            @RequestParam(required = false) String componentId) {
        Analysis analysis;
        try {
            analysis = analysisService.get(analysisId);
        } catch (RuntimeException e) {
            analysis = null;
        }
        if (analysis == null) {
            return ResponseEntity.notFound().build();
        }
        int boundedPageSize = pageSize == 50 || pageSize == 100 ? pageSize : 25;
        int boundedPage = Math.max(1, page);

        List<AnalysisTimelineService.AnalysisTimelineEvent> all = analysisTimelineService.getTimeline(analysis);
        // OGC-811 — component-aware presentation: a component row sees its own
        // events plus every analysis-level event (componentId null: creation,
        // status, referral, NCE, legacy records). No componentId param = the
        // unchanged analysis-level contract.
        if (!GenericValidator.isBlankOrNull(componentId)) {
            all = all.stream()
                    .filter(event -> event.getComponentId() == null || componentId.equals(event.getComponentId()))
                    .collect(java.util.stream.Collectors.toList());
        }
        int from = Math.min(all.size(), (boundedPage - 1) * boundedPageSize);
        int to = Math.min(all.size(), from + boundedPageSize);

        Map<String, Object> body = new HashMap<>();
        body.put("events", all.subList(from, to));
        body.put("total", all.size());
        body.put("page", boundedPage);
        body.put("pageSize", boundedPageSize);
        return ResponseEntity.ok(body);
    }

    /**
     * OGC-1026 (R7, FR-G1) — the interpretation rule buckets configured on a test's
     * components (Test Catalog Editor, OGC-949 M7), readable by the Results role.
     * The catalog editor's own endpoint is ADMIN-only, which would silently hide
     * the buckets from bench techs.
     */
    @GetMapping(value = "test/{testId}/interpretations", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasRole('RESULTS')")
    public ResponseEntity<List<Map<String, Object>>> getTestInterpretations(@PathVariable String testId) {
        List<Map<String, Object>> buckets = new ArrayList<>();
        for (TestResultComponent component : testResultComponentService.getActiveComponentsByTestId(testId)) {
            for (TestResultInterpretation interpretation : interpretationService
                    .getActiveByComponentId(component.getId())) {
                Map<String, Object> bucket = new HashMap<>();
                bucket.put("componentId", component.getId());
                bucket.put("id", interpretation.getId());
                bucket.put("valueMatch", interpretation.getValueMatch());
                bucket.put("text", interpretation.getInterpretationText());
                bucket.put("severity", interpretation.getSeverity());
                bucket.put("color", interpretation.getColor());
                bucket.put("displayOrder", interpretation.getDisplayOrder());
                buckets.add(bucket);
            }
        }
        return ResponseEntity.ok(buckets);
    }

    /**
     * OGC-1024 (R5) — the reagents linked to a test (Test Catalog Editor), readable
     * by the Results role so the bench Reagents &amp; QC section can surface lots
     * and record consumption. The catalog's own endpoint is ADMIN-only.
     */
    @GetMapping(value = "test/{testId}/reagents", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasRole('RESULTS')")
    public ResponseEntity<List<Map<String, Object>>> getTestReagentLinks(@PathVariable String testId) {
        List<Map<String, Object>> reagents = new ArrayList<>();
        for (TestReagentLink link : testReagentLinkService.getByTestId(testId)) {
            Map<String, Object> reagent = new HashMap<>();
            reagent.put("reagentId", link.getReagentId());
            reagent.put("usageType", link.getUsageType());
            reagent.put("quantityPerTest", link.getQuantityPerTest());
            reagent.put("quantityUnit", link.getQuantityUnit());
            if (link.getReagentId() != null) {
                org.openelisglobal.inventory.valueholder.InventoryItem item = inventoryItemService
                        .get(link.getReagentId());
                if (item != null) {
                    reagent.put("name", item.getName());
                    reagent.put("units", item.getUnits());
                }
            }
            reagents.add(reagent);
        }
        return ResponseEntity.ok(reagents);
    }

    /**
     * Per-analysis result save (FR-O1/FR-O2). The path names the one analysis this
     * request may write; the body carries its edited values and the version token
     * it was loaded with.
     */
    @PostMapping(value = "analysis/{analysisId}/result", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasRole('RESULTS')")
    public ResponseEntity<Map<String, Object>> saveSingleAnalysisResult(HttpServletRequest request,
            @PathVariable String analysisId,
            @Validated(LogbookResultsForm.LogbookResults.class) @RequestBody SingleResultEntryForm form) {

        TestResultItem item = form.getTestResult();
        Map<String, Object> body = new HashMap<>();

        if (item == null || !analysisId.equals(item.getAnalysisId())) {
            body.put("error", "Payload analysisId does not match the path analysisId — a save may only write the"
                    + " analysis it names (FR-O1).");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }

        Analysis analysis = analysisService.get(analysisId);
        if (analysis == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        ResponseEntity<Map<String, Object>> staleResponse = rejectIfStale(item, analysis, body);
        if (staleResponse != null) {
            return staleResponse;
        }

        item.setModified(true);
        reuseExistingResultForComponent(item, analysis);

        boolean useTechnicianName = ConfigurationProperties.getInstance()
                .isPropertyValueEqual(Property.resultTechnicianName, "true");
        boolean alwaysValidate = ConfigurationProperties.getInstance()
                .isPropertyValueEqual(Property.ALWAYS_VALIDATE_RESULTS, "true");
        boolean supportReferrals = FormFields.getInstance().useField(Field.ResultsReferral);
        String statusRuleSet = ConfigurationProperties.getInstance().getPropertyValueUpperCase(Property.StatusRules);

        ResultsUpdateDataSet dataSet = new ResultsUpdateDataSet(getSysUserId(request));
        dataSet.filterModifiedItems(Collections.singletonList(item));

        Errors errors = dataSet.validateModifiedItems();
        if (errors.hasErrors()) {
            body.put("error", errors.getAllErrors().stream().map(e -> MessageUtil.getMessage(e.getCode()))
                    .collect(Collectors.joining("; ")));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }

        ResultUtil.createResultsFromItems(dataSet, supportReferrals, alwaysValidate, useTechnicianName, statusRuleSet,
                request);
        ResultUtil.createAnalysisOnlyUpdates(dataSet, request);

        List<IResultUpdate> updaters = ResultUpdateRegister.getRegisteredUpdaters();
        try {
            List<Analysis> reflexAnalyses = logbookPersistService.persistDataSet(dataSet, updaters,
                    getSysUserId(request));
            body.put("reflex", reflexAnalyses.stream().filter(e -> !e.getResultCalculated())
                    .map(e -> analysisService.getOrderAccessionNumber(e)).collect(Collectors.toList()));
            body.put("calculated", reflexAnalyses.stream().filter(e -> e.getResultCalculated())
                    .map(e -> analysisService.getOrderAccessionNumber(e)).collect(Collectors.toList()));

            // A generated test outlives the page that generated it, so saying so
            // has to outlive it too: the logbook page has always recorded these
            // through the notification system, and a toast that disappears on
            // navigation is not a record. Same service the alerts use.
            notifyGenerated(currentUserId, reflexAnalyses.stream().filter(e -> !e.getResultCalculated()),
                    "notification.reflex.created");
            notifyGenerated(currentUserId, reflexAnalyses.stream().filter(e -> e.getResultCalculated()),
                    "notification.calculated.created");

            try {
                fhirTransformService.transformPersistResultsEntryFhirObjects(dataSet);
            } catch (FhirTransformationException | FhirPersistanceException e) {
                LogEvent.logError(e);
            }
            if (testAlertEvaluationService != null) {
                String currentUser = getSysUserId(request);
                // corrections matter as much as first entries — a modified value
                // can cross a critical bound (OGC-1022 R3)
                Stream.concat(dataSet.getNewResults().stream(), dataSet.getModifiedResults().stream()).forEach(rs -> {
                    try {
                        testAlertEvaluationService.evaluateAndDispatch(rs.result, currentUser);
                    } catch (RuntimeException ex) {
                        LogEvent.logError(ex);
                    }
                });
            }
        } catch (LIMSRuntimeException e) {
            if (e.getCause() instanceof StaleObjectStateException) {
                return rejectStale(analysis, body);
            }
            LogEvent.logError(e);
            body.put("error", "errors.UpdateException");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }

        for (IResultUpdate updater : updaters) {
            try {
                updater.postTransactionalCommitUpdate(dataSet);
            } catch (RuntimeException e) {
                LogEvent.logError(e);
            }
        }

        Analysis persisted = analysisService.get(analysisId);
        if (persisted != null && persisted.getLastupdated() != null) {
            body.put("analysisLastupdated", String.valueOf(persisted.getLastupdated().getTime()));
        }
        // The persisted result's id — the client's row must adopt it, or a row
        // saved from the blank placeholder state keeps a null resultId and every
        // subsequent save INSERTS another result (duplicate component rows).
        Stream.concat(dataSet.getNewResults().stream(), dataSet.getModifiedResults().stream()).map(rs -> rs.result)
                .filter(r -> r != null && r.getId() != null).findFirst()
                .ifPresent(r -> body.put("resultId", r.getId()));
        return ResponseEntity.ok(body);
    }

    /**
     * Idempotency guard for the per-analysis save: a payload with a BLANK resultId
     * means "new result" to the legacy save service — but if this analysis already
     * has a persisted result for the row's component (the client's row state was
     * stale, e.g. saved from a placeholder row that never learned its persisted
     * id), inserting again duplicates the component. Bind the item to the existing
     * result so the save UPDATES it. Multiselect rows legitimately hold several
     * results per component and manage their own lifecycle — they are left alone.
     */
    private void reuseExistingResultForComponent(TestResultItem item, Analysis analysis) {
        if (!GenericValidator.isBlankOrNull(item.getResultId())
                || org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType
                        .isMultiSelectVariant(item.getResultType())) {
            return;
        }
        String itemComponentId = item.getTestResultComponentId();
        String primaryComponentId = null;
        if (itemComponentId != null && analysis.getTest() != null) {
            // ensureSinglePrimary guarantees exactly one active component
            // carries the flag (TestResultComponentServiceImpl)
            primaryComponentId = testResultComponentService.getActiveComponentsByTestId(analysis.getTest().getId())
                    .stream().filter(c -> Boolean.TRUE.equals(c.getIsPrimary())).map(TestResultComponent::getId)
                    .findFirst().orElse(null);
        }
        org.openelisglobal.result.valueholder.Result latestMatch = null;
        for (org.openelisglobal.result.valueholder.Result existing : resultService.getResultsByAnalysis(analysis)) {
            String existingComponentId = existing.getTestResult() != null ? existing.getTestResult().getComponentId()
                    : null;
            boolean matches = java.util.Objects.equals(existingComponentId, itemComponentId)
                    // legacy results carry no component id; the loader buckets
                    // them onto the PRIMARY component's row
                    || (existingComponentId == null && itemComponentId != null
                            && itemComponentId.equals(primaryComponentId));
            if (matches && (latestMatch == null
                    || Long.parseLong(existing.getId()) > Long.parseLong(latestMatch.getId()))) {
                latestMatch = existing;
            }
        }
        if (latestMatch != null) {
            item.setResultId(latestMatch.getId());
        }
    }

    /**
     * FR-O3 presence heartbeat. Body: {@code analysisId} the caller currently has
     * open in Edit (null/blank = none) and the {@code visibleAnalysisIds} on their
     * screen. Returns analysisId → display name of the OTHER user editing it.
     */
    /** Records generated tests under the header bell. */
    private void notifyGenerated(String currentUser, Stream<Analysis> analyses, String messageKey) {
        if (headerNotificationService == null) {
            return;
        }
        List<String> accessions = analyses.map(e -> analysisService.getOrderAccessionNumber(e))
                .collect(Collectors.toList());
        if (accessions.isEmpty()) {
            return;
        }
        try {
            headerNotificationService.notifyUser(currentUser,
                    MessageUtil.getMessage(messageKey) + " " + String.join(", ", accessions));
        } catch (RuntimeException ex) {
            // Recording the notification must never fail the save it describes.
            LogEvent.logError(ex);
        }
    }

    @PostMapping(value = "presence", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasRole('RESULTS')")
    public Map<String, String> presenceHeartbeat(HttpServletRequest request, @RequestBody PresenceHeartbeatForm form) {
        String sessionId = request.getSession().getId();
        presenceService.heartbeat(sessionId, getUserDisplayName(getSysUserId(request)), form.getAnalysisId());
        return presenceService.getPresence(
                form.getVisibleAnalysisIds() == null ? Collections.emptyList() : form.getVisibleAnalysisIds(),
                sessionId);
    }

    public static class PresenceHeartbeatForm {
        private String analysisId;
        private List<String> visibleAnalysisIds;

        public String getAnalysisId() {
            return analysisId;
        }

        public void setAnalysisId(String analysisId) {
            this.analysisId = analysisId;
        }

        public List<String> getVisibleAnalysisIds() {
            return visibleAnalysisIds;
        }

        public void setVisibleAnalysisIds(List<String> visibleAnalysisIds) {
            this.visibleAnalysisIds = visibleAnalysisIds;
        }
    }

    private ResponseEntity<Map<String, Object>> rejectIfStale(TestResultItem item, Analysis analysis,
            Map<String, Object> body) {
        Timestamp current = analysis.getLastupdated();
        String clientToken = item.getAnalysisLastupdated();
        if (current != null && !GenericValidator.isBlankOrNull(clientToken)
                && current.getTime() != Long.parseLong(clientToken)) {
            return rejectStale(analysis, body);
        }
        return null;
    }

    /**
     * 409 body for FR-O2: names who last saved this analysis and when, so the stale
     * editor gets "updated by {0} at {1} — refresh" rather than a silent merge. The
     * stale editor always loses; the active user's save is never overwritten.
     */
    private ResponseEntity<Map<String, Object>> rejectStale(Analysis analysis, Map<String, Object> body) {
        body.put("error", "error.results.staleSave");
        if (analysis.getLastupdated() != null) {
            body.put("modifiedAt", analysis.getLastupdated().toString());
            body.put("analysisLastupdated", String.valueOf(analysis.getLastupdated().getTime()));
        }
        body.put("modifiedBy", resolveLastModifier(analysis));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    private String resolveLastModifier(Analysis analysis) {
        try {
            List<History> rows = historyDAO.getHistoryByRefIdAndRefTableId(analysis.getId(),
                    AnalysisServiceImpl.getTableReferenceId());
            History latest = null;
            for (History row : rows) {
                if (latest == null || (row.getTimestamp() != null && latest.getTimestamp() != null
                        && row.getTimestamp().after(latest.getTimestamp()))) {
                    latest = row;
                }
            }
            if (latest != null) {
                return getUserDisplayName(latest.getSysUserId());
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
        }
        return MessageUtil.getMessage("label.results.anotherUser");
    }

    private String getUserDisplayName(String sysUserId) {
        try {
            SystemUser user = systemUserService.getUserById(sysUserId);
            if (user != null) {
                return user.getDisplayName();
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
        }
        return MessageUtil.getMessage("label.results.anotherUser");
    }

    @Override
    protected String findLocalForward(String forward) {
        return "PageNotFound";
    }
}
