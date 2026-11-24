package org.openelisglobal.resultrun.controller.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.qc.form.BenchQCCaptureForm;
import org.openelisglobal.resultrun.form.ResultRunChip;
import org.openelisglobal.resultrun.form.ResultRunHeader;
import org.openelisglobal.resultrun.form.ResultRunListResponse;
import org.openelisglobal.resultrun.form.ResultRunRowsPage;
import org.openelisglobal.resultrun.form.ResultRunSelection;
import org.openelisglobal.resultrun.form.ResultRunSelectionSummary;
import org.openelisglobal.resultrun.form.RunOrderMatch;
import org.openelisglobal.resultrun.form.RunViewer;
import org.openelisglobal.resultrun.service.ResultRunService;
import org.openelisglobal.resultrun.service.RunExceptionService;
import org.openelisglobal.resultrun.service.RunQcService;
import org.openelisglobal.resultrun.valueholder.RunTestAction;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.systemuser.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OGC-1200: the runs a user may work, gated on Results rights per lab unit
 * (D-043, D-044), never on Analyser Import.
 */
@RestController
@RequestMapping("/rest/result-runs")
@PreAuthorize("hasRole('RESULTS')")
public class ResultRunRestController extends BaseRestController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ResultRunService resultRunService;
    private final RunQcService runQcService;
    private final RunExceptionService runExceptionService;
    private final UserService userService;
    private final RoleService roleService;

    public ResultRunRestController(ResultRunService resultRunService, RunQcService runQcService,
            RunExceptionService runExceptionService, UserService userService, RoleService roleService) {
        this.resultRunService = resultRunService;
        this.runQcService = runQcService;
        this.runExceptionService = runExceptionService;
        this.userService = userService;
        this.roleService = roleService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResultRunListResponse list(HttpServletRequest request,
            @RequestParam(required = false) String testSectionId) {
        return resultRunService.listActiveRuns(viewer(request), testSectionId);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultRunHeader> header(HttpServletRequest request, @PathVariable Long id) {
        return ResponseEntity.of(resultRunService.getHeader(id, viewer(request)));
    }

    @GetMapping(value = "/{id}/rows", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultRunRowsPage> rows(HttpServletRequest request, @PathVariable Long id,
            @RequestParam(required = false) String chip, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        return ResponseEntity
                .of(resultRunService.getRows(id, viewer(request), ResultRunChip.fromRequest(chip), page, pageSize));
    }

    /** What a bulk action on this selection would cover (FR-E11, FR-E12). */
    @PostMapping(value = "/{id}/selection", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultRunSelectionSummary> selection(HttpServletRequest request, @PathVariable Long id,
            @RequestBody ResultRunSelection selection) {
        return ResponseEntity.of(resultRunService.summarizeSelection(id, viewer(request), selection));
    }

    /**
     * The accepted message as a JSON document. Returned as a tree, not a String:
     * the JSON converter would otherwise encode the text a second time.
     */
    @GetMapping(value = "/{id}/raw", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> raw(HttpServletRequest request, @PathVariable Long id)
            throws JsonProcessingException {
        Optional<String> json = resultRunService.getRawMessage(id, viewer(request));
        if (json.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(OBJECT_MAPPER.readTree(json.get()));
    }

    /**
     * Records a control on the run (FR-E2 Record control) and answers with the
     * run's QC block as it now stands. The control's test must be one the user
     * holds Results rights for.
     */
    @PostMapping(value = "/{id}/controls", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> recordControl(HttpServletRequest request, @PathVariable Long id,
            @RequestBody BenchQCCaptureForm capture) {
        RunViewer viewer = viewer(request);
        if (resultRunService.getHeader(id, viewer).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (capture == null || capture.testId() == null || !viewer.testIds().contains(capture.testId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            runQcService.recordControl(id, capture, Integer.parseInt(getSysUserId(request)));
        } catch (IllegalArgumentException e) {
            String message = e.getMessage() == null ? "Invalid control result" : e.getMessage();
            return ResponseEntity.badRequest().body(Map.of("message", message));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(runQcService.qcBlock(id));
    }

    /** What the page sends to decide a held test (FR-C5). */
    public record TestDecision(String action, String reason, boolean fileNce) {
    }

    /**
     * Decides a held test of the run: Retest, Reject, or Accept despite the QC
     * failure (FR-C5), and answers with the QC block as it now stands.
     */
    @PostMapping(value = "/{id}/tests/{testId}/decision", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> decide(HttpServletRequest request, @PathVariable Long id, @PathVariable String testId,
            @RequestBody TestDecision decision) {
        RunViewer viewer = viewer(request);
        if (resultRunService.getHeader(id, viewer).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!viewer.testIds().contains(testId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        RunTestAction action;
        try {
            action = RunTestAction.valueOf(decision.action());
        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Unknown decision"));
        }
        try {
            runQcService.decide(id, testId, action, decision.reason(), decision.fileNce(), getSysUserId(request));
        } catch (IllegalArgumentException e) {
            String message = e.getMessage() == null ? "Invalid decision" : e.getMessage();
            return ResponseEntity.badRequest().body(Map.of("message", message));
        }
        return ResponseEntity.ok(runQcService.qcBlock(id));
    }

    /** The earlier run the page confirms this run retests (FR-C5). */
    public record RetestOf(Long runId) {
    }

    /**
     * Confirms this run as the retest of an earlier run's test (FR-C5), which
     * supersedes it there (FR-A3), and answers with the QC block as it now stands.
     */
    @PostMapping(value = "/{id}/tests/{testId}/retest-of", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> confirmRetest(HttpServletRequest request, @PathVariable Long id,
            @PathVariable String testId, @RequestBody RetestOf retestOf) {
        RunViewer viewer = viewer(request);
        if (resultRunService.getHeader(id, viewer).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!viewer.testIds().contains(testId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            runQcService.confirmRetest(id, testId, retestOf == null ? null : retestOf.runId(), getSysUserId(request));
        } catch (IllegalArgumentException e) {
            String message = e.getMessage() == null ? "Invalid retest link" : e.getMessage();
            return ResponseEntity.badRequest().body(Map.of("message", message));
        }
        return ResponseEntity.ok(runQcService.qcBlock(id));
    }

    /**
     * Orders a run exception can be matched to, by the start of their lab number
     * (FR-E4 type-ahead), saying whether the value's test is on each.
     */
    @GetMapping(value = "/{id}/exceptions/orders", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RunOrderMatch>> exceptionOrders(HttpServletRequest request, @PathVariable Long id,
            @RequestParam(required = false) String q, @RequestParam(required = false) String testId) {
        if (resultRunService.getHeader(id, viewer(request)).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(runExceptionService.ordersMatching(q, testId));
    }

    /** What the page sends to resolve an exception's values (FR-E4). */
    public record ExceptionDecision(List<String> rowIds, String key, String reason, String accessionNumber) {
    }

    /** Places an exception's values on the order the technician found (FR-E4). */
    @PostMapping(value = "/{id}/exceptions/match", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> matchException(HttpServletRequest request, @PathVariable Long id,
            @RequestBody ExceptionDecision decision) {
        return resolveException(request, id, decision, (viewer, sysUserId) -> runExceptionService.match(id, viewer,
                decision.rowIds(), decision.key(), decision.accessionNumber(), decision.reason(), sysUserId));
    }

    /** Keeps an exception's values blocked with a reason (FR-E4). */
    @PostMapping(value = "/{id}/exceptions/hold", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> holdException(HttpServletRequest request, @PathVariable Long id,
            @RequestBody ExceptionDecision decision) {
        return resolveException(request, id, decision, (viewer, sysUserId) -> runExceptionService.hold(id, viewer,
                decision.rowIds(), decision.key(), decision.reason(), sysUserId));
    }

    /** Takes an exception's values off the run with a reason (FR-E4). */
    @PostMapping(value = "/{id}/exceptions/ignore", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> ignoreException(HttpServletRequest request, @PathVariable Long id,
            @RequestBody ExceptionDecision decision) {
        return resolveException(request, id, decision, (viewer, sysUserId) -> runExceptionService.ignore(id, viewer,
                decision.rowIds(), decision.key(), decision.reason(), sysUserId));
    }

    private interface ExceptionAction {
        void apply(RunViewer viewer, String sysUserId);
    }

    /**
     * Applies an exception decision and answers with the run header as it now
     * stands, or no exceptions once the viewer has resolved every value they could
     * see: 400 for a missing reason or order, 409 (marked stale) when a value
     * changed under the page.
     */
    private ResponseEntity<?> resolveException(HttpServletRequest request, Long id, ExceptionDecision decision,
            ExceptionAction action) {
        RunViewer viewer = viewer(request);
        if (resultRunService.getHeader(id, viewer).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (decision == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Choose the records to resolve"));
        }
        try {
            action.apply(viewer, getSysUserId(request));
        } catch (IllegalArgumentException e) {
            String message = e.getMessage() == null ? "Invalid decision" : e.getMessage();
            return ResponseEntity.badRequest().body(Map.of("message", message));
        } catch (IllegalStateException e) {
            String message = e.getMessage() == null ? "The run has changed" : e.getMessage();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", message, "stale", true));
        }
        return ResponseEntity.ok(resultRunService.getHeader(id, viewer).<Object>map(header -> header)
                .orElse(Map.of("exceptions", List.of())));
    }

    private RunViewer viewer(HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        String roleId = roleService.getRoleByName(Constants.ROLE_RESULTS).getId();
        List<IdValuePair> sections = userService.getUserViewerTestSections(sysUserId, roleId);
        Set<String> labUnitIds = new HashSet<>();
        if (sections != null) {
            sections.forEach(section -> labUnitIds.add(section.getId()));
        }
        return new RunViewer(userService.getTestIdsInUserLabUnits(sysUserId, Constants.ROLE_RESULTS), labUnitIds);
    }
}
