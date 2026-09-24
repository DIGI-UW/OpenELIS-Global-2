package org.openelisglobal.sampleacceptance.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleacceptance.service.AdminChecklistView;
import org.openelisglobal.sampleacceptance.service.ResampleResult;
import org.openelisglobal.sampleacceptance.service.ResampleService;
import org.openelisglobal.sampleacceptance.service.SampleAcceptanceBlockedException;
import org.openelisglobal.sampleacceptance.service.SampleAcceptanceChecklistService;
import org.openelisglobal.sampleacceptance.service.SampleAcceptanceEvaluation;
import org.openelisglobal.sampleacceptance.service.SampleAcceptanceRecordService;
import org.openelisglobal.sampleacceptance.valueholder.SampleAcceptanceRecord;
import org.openelisglobal.sampleacceptance.valueholder.SampleAcceptanceRecord.Answer;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for the S-09 (OGC-580) Sample Acceptance Checklist — the
 * pre-analytical specimen-acceptance gate. Exposes the resolved per-domain
 * checklist + enforcement, and per-sample acceptance recording / evaluation /
 * NCE pre-fill. Distinct from /rest/qa-checklist (OGC-356 QA verification).
 */
@RestController
@RequestMapping("/rest/sample-acceptance-checklist")
public class SampleAcceptanceRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(SampleAcceptanceRestController.class);

    /**
     * The only answer values the gate recognizes; anything else is rejected (400).
     */
    private static final Set<String> ALLOWED_ANSWERS = Set.of(SampleAcceptanceRecord.ANSWER_PASS,
            SampleAcceptanceRecord.ANSWER_FAIL, SampleAcceptanceRecord.ANSWER_NA);

    private static final int MAX_LABEL_LENGTH = 255;

    private static final int MAX_NOTE_LENGTH = 2000;

    @Autowired
    private SampleAcceptanceChecklistService checklistService;

    @Autowired
    private SampleAcceptanceRecordService recordService;

    @Autowired
    private ResampleService resampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private HttpServletRequest httpRequest;

    /**
     * Resolved checklist config. With {@code ?domain=CLINICAL|ENVIRONMENTAL|VECTOR}
     * returns that domain's effective list (its own items, else the lab-wide
     * fallback); without a domain returns the lab-wide list.
     */
    @GetMapping("/config")
    public ResponseEntity<?> getConfig(@RequestParam(required = false) String domain) {
        try {
            List<Dictionary> items = (domain == null || domain.isBlank()) ? checklistService.listLabWide()
                    : checklistService.listForDomain(domain);
            return ResponseEntity.ok(toItemMaps(items));
        } catch (Exception e) {
            logger.error("Error getting sample acceptance config", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get checklist config");
        }
    }

    /** Per-domain enforcement modes (MANDATORY / OPTIONAL / OFF). */
    @GetMapping("/enforcement")
    public ResponseEntity<?> getEnforcement() {
        try {
            Map<String, String> response = new LinkedHashMap<>();
            response.put("clinical", checklistService.getEnforcement("CLINICAL"));
            response.put("environmental", checklistService.getEnforcement("ENVIRONMENTAL"));
            response.put("vector", checklistService.getEnforcement("VECTOR"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting enforcement settings", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get enforcement settings");
        }
    }

    /**
     * Acceptance evaluation for a specimen (resolved checklist + latest decision +
     * blocked + resample links).
     */
    @GetMapping("/sample-item/{sampleItemId}")
    public ResponseEntity<?> getForSampleItem(@PathVariable String sampleItemId) {
        try {
            if (!isNumeric(sampleItemId)) {
                return error(HttpStatus.BAD_REQUEST, "Invalid sampleItemId: must be numeric");
            }
            return ResponseEntity.ok(evaluationResponse(sampleItemId));
        } catch (Exception e) {
            logger.error("Error evaluating sample acceptance for sample item: {}", sampleItemId, e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to evaluate sample acceptance");
        }
    }

    /**
     * Per-specimen acceptance evaluations for a whole order — one entry per
     * non-voided {@code sample_item} ({@code sampleItemId}, eligibility
     * {@code overallStatus}, {@code blocked}, resolved {@code domain}). Backs the
     * QA-step intake-acceptance table (master list); each row's detail is loaded
     * via {@link #getForSampleItem}.
     */
    @GetMapping("/order/{sampleId}/items")
    public ResponseEntity<?> getForOrder(@PathVariable String sampleId) {
        try {
            if (!isNumeric(sampleId)) {
                return error(HttpStatus.BAD_REQUEST, "Invalid sampleId: must be numeric");
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            for (SampleAcceptanceEvaluation eval : recordService.evaluateOrder(sampleId)) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("sampleItemId", eval.getSampleItemId());
                row.put("overallStatus", eval.getOverallStatus());
                row.put("blocked", eval.isBlocked());
                row.put("domain", eval.getDomain());
                rows.add(row);
            }
            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            logger.error("Error evaluating order acceptance for order: {}", sampleId, e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to evaluate order acceptance");
        }
    }

    /**
     * FR-08 server-side enforcement gate for a whole order. {@code 200
     * {blocked:false}} when every specimen may proceed; {@code 409 {blocked:true}}
     * when any non-voided, non-rejected specimen's domain enforcement is MANDATORY
     * and its checklist is not yet satisfied. The Collect → Label &amp; Store
     * transition calls this as a precondition so the mandatory gate cannot be
     * bypassed client-side.
     */
    @GetMapping("/sample/{sampleId}/gate")
    public ResponseEntity<?> gate(@PathVariable String sampleId) {
        try {
            if (!isNumeric(sampleId)) {
                return error(HttpStatus.BAD_REQUEST, "Invalid sampleId: must be numeric");
            }
            recordService.enforceAcceptanceGateForOrder(sampleId);
            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("blocked", false);
            return ResponseEntity.ok(ok);
        } catch (SampleAcceptanceBlockedException e) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("blocked", true);
            body.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        } catch (Exception e) {
            logger.error("Error evaluating acceptance gate for order: {}", sampleId, e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to evaluate acceptance gate");
        }
    }

    /**
     * Record an acceptance assessment for a specimen (append-only). Body: {@code {
     * "answers": [ { "itemKey", "label", "answer": PASS|FAIL|NA, "note" } ] } }
     * Returns the refreshed evaluation.
     */
    @PostMapping("/sample-item/{sampleItemId}")
    @PreAuthorize("hasAnyRole('RECEPTION', 'RESULTS', 'ADMIN')")
    public ResponseEntity<?> record(@PathVariable String sampleItemId, @RequestBody Map<String, Object> body) {
        try {
            if (!isNumeric(sampleItemId)) {
                return error(HttpStatus.BAD_REQUEST, "Invalid sampleItemId: must be numeric");
            }
            recordService.recordAssessment(sampleItemId, parseAnswers(body), currentUserId());
            return ResponseEntity.ok(evaluationResponse(sampleItemId));
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error recording sample acceptance for sample item: {}", sampleItemId, e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to record sample acceptance");
        }
    }

    /**
     * Accept/record an assessment for an entire vector pool — the pool is the unit
     * of acceptance for vector. Cascades the same answers to every live member
     * {@code sample_item} (see
     * {@link SampleAcceptanceRecordService#recordAssessmentForPool}). Body
     * identical to {@link #record}. Returns a representative member's refreshed
     * evaluation.
     */
    @PostMapping("/pool/{vectorPoolId}")
    @PreAuthorize("hasAnyRole('RECEPTION', 'RESULTS', 'ADMIN')")
    public ResponseEntity<?> recordForPool(@PathVariable String vectorPoolId, @RequestBody Map<String, Object> body) {
        try {
            if (!isNumeric(vectorPoolId)) {
                return error(HttpStatus.BAD_REQUEST, "Invalid vectorPoolId: must be numeric");
            }
            List<SampleAcceptanceRecord> records = recordService.recordAssessmentForPool(
                    Integer.parseInt(vectorPoolId.trim()), parseAnswers(body), currentUserId());
            if (records.isEmpty()) {
                return error(HttpStatus.BAD_REQUEST, "Vector pool has no live members to accept");
            }
            return ResponseEntity.ok(evaluationResponse(String.valueOf(records.get(0).getSampleItemId())));
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error recording pool acceptance for vector pool: {}", vectorPoolId, e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to record pool acceptance");
        }
    }

    /**
     * Resample one failed specimen (FR-10): in one atomic transaction record an NCE
     * against the specimen, reject just that {@code sample_item} (the order's
     * accepted specimens proceed), and create a draft replacement order cloned from
     * the parent order carrying only this specimen. Body: {@code { "reason": "..."
     * }}. A resample is distinguished by the replacement's resampled_from link (see
     * {@link #getForSampleItem}). Returns the parent order, new draft order, and
     * NCE identifiers.
     */
    @PostMapping("/sample-item/{sampleItemId}/resample")
    @PreAuthorize("hasAnyRole('RECEPTION', 'RESULTS', 'ADMIN')")
    public ResponseEntity<?> resample(@PathVariable String sampleItemId,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            if (!isNumeric(sampleItemId)) {
                return error(HttpStatus.BAD_REQUEST, "Invalid sampleItemId: must be numeric");
            }
            String reason = body == null ? null : asString(body.get("reason"));
            if (reason == null || reason.isBlank()) {
                return error(HttpStatus.BAD_REQUEST, "A resample reason is required");
            }
            ResampleResult result = resampleService.resample(sampleItemId, reason.trim(), currentUserId());
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("originalSampleId", result.getOriginalSampleId());
            response.put("newSampleId", result.getNewSampleId());
            response.put("newAccessionNumber", result.getNewAccessionNumber());
            response.put("nceId", result.getNceId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error resampling specimen: {}", sampleItemId, e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to resample");
        }
    }

    /**
     * Reject an entire vector pool — cascade a plain reject (no replacement order,
     * unlike {@link #resample}) to every live member {@code sample_item}. Body:
     * {@code { "reason": "..." }} (optional). Returns {@code {rejected:true}}.
     */
    @PostMapping("/pool/{vectorPoolId}/reject")
    @PreAuthorize("hasAnyRole('RECEPTION', 'RESULTS', 'ADMIN')")
    public ResponseEntity<?> rejectPool(@PathVariable String vectorPoolId,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            if (!isNumeric(vectorPoolId)) {
                return error(HttpStatus.BAD_REQUEST, "Invalid vectorPoolId: must be numeric");
            }
            String reason = body == null ? null : asString(body.get("reason"));
            resampleService.rejectPool(Integer.parseInt(vectorPoolId.trim()), reason == null ? null : reason.trim(),
                    currentUserId());
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("rejected", true);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error rejecting vector pool: {}", vectorPoolId, e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to reject pool");
        }
    }

    /**
     * Plainly reject a single (non-pooled vector) specimen — no replacement order.
     * Body: {@code { "reason": "..." }} (optional). Returns
     * {@code {rejected:true}}.
     */
    @PostMapping("/sample-item/{sampleItemId}/reject")
    @PreAuthorize("hasAnyRole('RECEPTION', 'RESULTS', 'ADMIN')")
    public ResponseEntity<?> rejectSampleItem(@PathVariable String sampleItemId,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            if (!isNumeric(sampleItemId)) {
                return error(HttpStatus.BAD_REQUEST, "Invalid sampleItemId: must be numeric");
            }
            String reason = body == null ? null : asString(body.get("reason"));
            resampleService.reject(sampleItemId, reason == null ? null : reason.trim(), currentUserId());
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("rejected", true);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error rejecting specimen: {}", sampleItemId, e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to reject specimen");
        }
    }

    /** Suggested NCE reason pre-filled from the specimen's latest failed items. */
    @GetMapping("/sample-item/{sampleItemId}/nce-prefill")
    public ResponseEntity<?> ncePrefill(@PathVariable String sampleItemId) {
        try {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("reason", recordService.buildNcePrefillReason(sampleItemId));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error building NCE prefill for sample item: {}", sampleItemId, e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to build NCE prefill");
        }
    }

    // ---- admin config (Order Entry Config → Sample Acceptance Checklist)
    // ----------------------------------------------------

    /**
     * Everything the admin screen needs for one navigation target: its own items
     * (active and inactive), the lab-wide items, whether the domain overrides the
     * lab-wide list, and the current enforcement mode. {@code ?domain=} is
     * {@code ALL} (or blank) for the lab-wide list, else CLINICAL / ENVIRONMENTAL /
     * VECTOR.
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAdminView(@RequestParam(required = false) String domain) {
        try {
            return ResponseEntity.ok(toAdminView(checklistService.getAdminView(domain)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error building admin checklist view for domain: {}", domain, e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load checklist config");
        }
    }

    /** Create a checklist item. Body: {@code { "domain", "label" }}. */
    @PostMapping("/admin/items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createItem(@RequestBody Map<String, Object> body) {
        try {
            String domain = asString(body.get("domain"));
            String label = asString(body.get("label"));
            String validationError = validateLabel(label);
            if (validationError != null) {
                return error(HttpStatus.BAD_REQUEST, validationError);
            }
            Dictionary created = checklistService.createItem(domain, label, currentSysUserId());
            return ResponseEntity.ok(toAdminItemMap(created));
        } catch (LIMSDuplicateRecordException e) {
            return error(HttpStatus.BAD_REQUEST,
                    "An item with that label already exists in this list (it may be inactive — edit that item to reactivate it)");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating checklist item", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create checklist item");
        }
    }

    /**
     * Rename / activate-deactivate an item. Body: {@code { "label", "active" }}.
     */
    @PutMapping("/admin/items/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateItem(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            if (!isNumeric(id)) {
                return error(HttpStatus.BAD_REQUEST, "Invalid id: must be numeric");
            }
            String label = asString(body.get("label"));
            String validationError = validateLabel(label);
            if (validationError != null) {
                return error(HttpStatus.BAD_REQUEST, validationError);
            }
            boolean active = asBoolean(body.get("active"), true);
            Dictionary updated = checklistService.updateItem(id, label, active, currentSysUserId());
            return ResponseEntity.ok(toAdminItemMap(updated));
        } catch (LIMSDuplicateRecordException e) {
            return error(HttpStatus.BAD_REQUEST,
                    "An item with that label already exists in this list (it may be inactive — edit that item to reactivate it)");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating checklist item: {}", id, e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update checklist item");
        }
    }

    /**
     * Persist item order. Body: {@code { "domain", "orderedIds": [..] }}. Returns
     * the refreshed admin view.
     */
    @PostMapping("/admin/items/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> reorderItems(@RequestBody Map<String, Object> body) {
        try {
            String domain = asString(body.get("domain"));
            List<String> orderedIds = new ArrayList<>();
            Object idsObj = body.get("orderedIds");
            if (idsObj instanceof List) {
                for (Object o : (List<Object>) idsObj) {
                    String id = asString(o);
                    if (!isNumeric(id)) {
                        return error(HttpStatus.BAD_REQUEST, "Invalid id in orderedIds: " + id);
                    }
                    orderedIds.add(id);
                }
            }
            checklistService.reorder(domain, orderedIds, currentSysUserId());
            return ResponseEntity.ok(toAdminView(checklistService.getAdminView(domain)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error reordering checklist items", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to reorder checklist items");
        }
    }

    /**
     * Set the per-domain enforcement mode. Body: {@code { "mode":
     * MANDATORY|OPTIONAL|OFF }}.
     */
    @PutMapping("/admin/enforcement/{domain}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> setEnforcement(@PathVariable String domain, @RequestBody Map<String, Object> body) {
        try {
            String mode = asString(body.get("mode"));
            checklistService.setEnforcement(domain, mode, currentSysUserId());
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("domain", domain == null ? null : domain.toUpperCase());
            response.put("mode", mode == null ? null : mode.toUpperCase());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error setting enforcement for domain: {}", domain, e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to set enforcement");
        }
    }

    // ---- response mapping helpers
    // ----------------------------------------------------

    private List<Map<String, Object>> toItemMaps(List<Dictionary> items) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Dictionary item : items) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("itemKey", item.getDictEntry());
            m.put("label", item.getLocalAbbreviation());
            m.put("localizedName", item.getLocalizedName());
            m.put("displayOrder", item.getSortOrder());
            out.add(m);
        }
        return out;
    }

    /** Shapes the admin view (own + lab-wide items, enforcement, override flag). */
    private Map<String, Object> toAdminView(AdminChecklistView view) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("domain", view.getDomain());
        response.put("enforcement", view.getEnforcement());
        response.put("domainOverrides", view.isDomainOverrides());
        response.put("ownItems", toAdminItemMaps(view.getOwnItems()));
        response.put("labWideItems", toAdminItemMaps(view.getLabWideItems()));
        return response;
    }

    private List<Map<String, Object>> toAdminItemMaps(List<Dictionary> items) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (items != null) {
            for (Dictionary item : items) {
                out.add(toAdminItemMap(item));
            }
        }
        return out;
    }

    /**
     * Like {@link #toItemMaps} but for the admin screen: carries the row id and the
     * active flag (so the screen can render the toggle and reorder), and prefers
     * the localized name for the editable label.
     */
    private Map<String, Object> toAdminItemMap(Dictionary item) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", item.getId());
        m.put("itemKey", item.getDictEntry());
        m.put("label", item.getLocalizedName());
        m.put("displayOrder", item.getSortOrder());
        m.put("active", IActionConstants.YES.equals(item.getIsActive()));
        return m;
    }

    /**
     * The acceptance evaluation for a specimen plus its parent order's resample
     * cross-references, so a consumer (e.g. an order-view banner) can tell that the
     * order was resampled ({@code resampledToSampleId}) or is itself a replacement
     * ({@code resampledFromSampleId}).
     */
    private Map<String, Object> evaluationResponse(String sampleItemId) {
        Map<String, Object> response = toEvaluationMap(recordService.evaluate(sampleItemId));
        SampleItem item = sampleItemService.get(sampleItemId);
        Sample sample = item == null ? null : item.getSample();
        Map<String, Object> resample = new LinkedHashMap<>();
        resample.put("resampledToSampleId", sample == null ? null : sample.getResampledToSampleId());
        resample.put("resampledFromSampleId", sample == null ? null : sample.getResampledFromSampleId());
        response.put("resample", resample);
        return response;
    }

    private Map<String, Object> toEvaluationMap(SampleAcceptanceEvaluation eval) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sampleItemId", eval.getSampleItemId());
        response.put("domain", eval.getDomain());
        response.put("enforcement", eval.getEnforcement());
        response.put("overallStatus", eval.getOverallStatus());
        response.put("blocked", eval.isBlocked());
        response.put("items", toItemMaps(eval.getItems()));

        SampleAcceptanceRecord latest = eval.getLatest();
        if (latest != null) {
            Map<String, Object> latestMap = new LinkedHashMap<>();
            latestMap.put("id", latest.getId());
            latestMap.put("overallStatus", latest.getOverallStatus());
            latestMap.put("assessedByUserId", latest.getAssessedByUserId());
            latestMap.put("assessedAt", latest.getAssessedAt());
            List<Map<String, Object>> answerMaps = new ArrayList<>();
            for (Answer a : latest.getAnswers()) {
                Map<String, Object> am = new LinkedHashMap<>();
                am.put("itemKey", a.getItemKey());
                am.put("label", a.getLabel());
                am.put("answer", a.getAnswer());
                am.put("note", a.getNote());
                answerMaps.add(am);
            }
            latestMap.put("answers", answerMaps);
            response.put("latest", latestMap);
        } else {
            response.put("latest", null);
        }
        return response;
    }

    private Integer currentUserId() {
        try {
            String sysUserId = getSysUserId(httpRequest);
            return sysUserId != null ? Integer.parseInt(sysUserId) : null;
        } catch (Exception e) {
            logger.warn("Could not resolve current user id: {}", e.getMessage());
            return null;
        }
    }

    /** The acting user's id as a String (the form the config services expect). */
    private String currentSysUserId() {
        return getSysUserId(httpRequest);
    }

    private String asString(Object o) {
        return o == null ? null : o.toString();
    }

    /**
     * Parse + validate the {@code {answers:[...]}} body shared by the per-specimen
     * ({@link #record}) and per-pool ({@link #recordForPool}) record endpoints.
     * Throws {@link IllegalArgumentException} (→ 400) on an out-of-vocabulary
     * answer.
     */
    @SuppressWarnings("unchecked")
    private List<Answer> parseAnswers(Map<String, Object> body) {
        List<Answer> answers = new ArrayList<>();
        Object answersObj = body == null ? null : body.get("answers");
        if (answersObj instanceof List) {
            for (Object o : (List<Object>) answersObj) {
                if (o instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) o;
                    String itemKey = asString(m.get("itemKey"));
                    String label = asString(m.get("label"));
                    String answer = asString(m.get("answer"));
                    String note = asString(m.get("note"));
                    String validationError = validateAnswer(answer, label, note);
                    if (validationError != null) {
                        throw new IllegalArgumentException(validationError);
                    }
                    answers.add(new Answer(itemKey, label, answer, note));
                }
            }
        }
        return answers;
    }

    private boolean asBoolean(Object o, boolean defaultValue) {
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        if (o == null) {
            return defaultValue;
        }
        String s = o.toString().trim();
        return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("Y") || s.equals("1");
    }

    /**
     * A checklist item label is required (early 400). The maximum length is
     * enforced by the service against the backing column width and surfaced as a
     * 400 via the {@link IllegalArgumentException} handler.
     */
    private String validateLabel(String label) {
        if (label == null || label.isBlank()) {
            return "A label is required";
        }
        return null;
    }

    /**
     * Validates a single submitted answer. A blank/absent answer is allowed (the
     * item is simply unanswered → PENDING); any non-blank value must be one of
     * PASS/FAIL/NA, otherwise the gate could be defeated by an out-of-vocabulary
     * value that {@code computeStatus} would treat as passing. Also bounds
     * label/note length.
     *
     * @return an error message if invalid, or {@code null} if the answer is
     *         acceptable
     */
    private String validateAnswer(String answer, String label, String note) {
        if (answer != null && !answer.isBlank() && !ALLOWED_ANSWERS.contains(answer)) {
            return "Invalid answer value '" + answer + "': must be one of PASS, FAIL, NA";
        }
        if (label != null && label.length() > MAX_LABEL_LENGTH) {
            return "label exceeds " + MAX_LABEL_LENGTH + " characters";
        }
        if (note != null && note.length() > MAX_NOTE_LENGTH) {
            return "note exceeds " + MAX_NOTE_LENGTH + " characters";
        }
        return null;
    }

    private boolean isNumeric(String value) {
        if (value == null) {
            return false;
        }
        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }
}
