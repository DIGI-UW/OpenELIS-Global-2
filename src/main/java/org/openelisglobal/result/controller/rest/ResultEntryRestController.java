package org.openelisglobal.result.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

            try {
                fhirTransformService.transformPersistResultsEntryFhirObjects(dataSet);
            } catch (FhirTransformationException | FhirPersistanceException e) {
                LogEvent.logError(e);
            }
            if (testAlertEvaluationService != null) {
                String currentUser = getSysUserId(request);
                dataSet.getNewResults().forEach(rs -> {
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
        return ResponseEntity.ok(body);
    }

    /**
     * FR-O3 presence heartbeat. Body: {@code analysisId} the caller currently has
     * open in Edit (null/blank = none) and the {@code visibleAnalysisIds} on their
     * screen. Returns analysisId → display name of the OTHER user editing it.
     */
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
