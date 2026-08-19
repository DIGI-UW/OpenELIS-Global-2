package org.openelisglobal.eqa.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.eqa.service.EQABlindingService;
import org.openelisglobal.eqa.service.EQALabelPDFService;
import org.openelisglobal.eqa.service.EQAPanelService;
import org.openelisglobal.eqa.valueholder.EQAUnblindMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Panel lifecycle + sealed-sample reads (OGC-609). The sealed-target rule is
 * enforced in the service's DTO mapping; this controller only decides whether
 * the caller holds the unblind privilege.
 *
 * <p>
 * Reads sit under the {@code qa.view.eqa} umbrella; lifecycle writes stay on
 * {@code qa.manage.eqa}; unblinding — the endpoint and the sealed-target reveal
 * alike — requires the dedicated {@code qa.eqa.inhouse.unblind} tier (OGC-609
 * permission model).
 */
@RestController
@RequestMapping("/rest/eqa")
@PreAuthorize(EQAGuards.READ)
public class EQAPanelRestController extends BaseRestController {

    private final EQAPanelService panelService;
    private final EQABlindingService blindingService;
    private final EQALabelPDFService labelPDFService;

    public EQAPanelRestController(EQAPanelService panelService, EQABlindingService blindingService,
            EQALabelPDFService labelPDFService) {
        this.panelService = panelService;
        this.blindingService = blindingService;
        this.labelPDFService = labelPDFService;
    }

    @GetMapping(value = "/panels", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> panelsByCycle(@RequestParam Long cycleId) {
        return panelService.getPanelDtos(cycleId);
    }

    @GetMapping(value = "/panels/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getPanel(@PathVariable Long id) {
        return panelService.toPanelDto(panelService.get(id));
    }

    /** Sample DTOs carry null targets unless revealed — see EQAPanelService. */
    @GetMapping(value = "/panels/{id}/samples", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> getSamples(@PathVariable Long id) {
        return panelService.getSampleDtos(id, callerCanUnblind());
    }

    @PostMapping(value = "/panels/{id}/seal", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(EQAGuards.MANAGE)
    public Map<String, Object> seal(HttpServletRequest request, @PathVariable Long id) {
        return panelService.toPanelDto(panelService.seal(id, getSysUserId(request)));
    }

    @PostMapping(value = "/panels/{id}/distribute", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(EQAGuards.MANAGE)
    public Map<String, Object> distribute(HttpServletRequest request, @PathVariable Long id) {
        return panelService.toPanelDto(panelService.distribute(id, getSysUserId(request)));
    }

    /**
     * FR-V2.4-06 manual unblind: unblinds AND scores — the endpoint's caller
     * expects the panel to come back resolved, same as the scheduled path.
     */
    @PostMapping(value = "/panels/{id}/unblind", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(EQAGuards.UNBLIND)
    public Map<String, Object> unblind(HttpServletRequest request, @PathVariable Long id) {
        return panelService
                .toPanelDto(blindingService.unblindAndScore(id, getSysUserId(request), EQAUnblindMethod.MANUAL));
    }

    /**
     * FR-V2.4-04 "Seal panel &amp; distribute": body carries one order spec per
     * panel sample — {@code {"orders":[{"panelSampleId":1,"testId":"7",
     * "analystId":12}]}} — analystId optional (round-robin fills it). Values are
     * read via String.valueOf so a numeric-vs-string JSON mismatch cannot 500.
     */
    @PostMapping(value = "/panels/{id}/seal-and-distribute", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(EQAGuards.MANAGE)
    public Map<String, Object> sealAndDistribute(HttpServletRequest request, @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        List<EQABlindingService.BlindOrderSpec> specs = new ArrayList<>();
        Object orders = body.get("orders");
        if (orders instanceof List<?> rows) {
            for (Object row : rows) {
                if (row instanceof Map<?, ?> spec) {
                    specs.add(new EQABlindingService.BlindOrderSpec(longOrNull(spec.get("panelSampleId")),
                            stringOrNull(spec.get("testId")), longOrNull(spec.get("analystId"))));
                }
            }
        }
        return blindingService.sealAndDistribute(id, specs, getSysUserId(request));
    }

    /** FR-V2.4-13: blind-code label sheet, printable any time after sealing. */
    /**
     * FR-V2.4-13: reprint is allowed to anyone who can see the panel, so this stays
     * on the read umbrella rather than the lifecycle grant — the sheet carries
     * blind codes only, never a target.
     */
    @GetMapping(value = "/panels/{id}/labels", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> labelSheet(HttpServletRequest request, @PathVariable Long id) {
        byte[] pdf = labelPDFService.generateLabelSheet(id);
        LogEvent.logInfo(getClass().getSimpleName(), "labelSheet", "EQA label sheet printed: panel=" + id + " user="
                + getSysUserId(request) + " labelCount=" + labelPDFService.countLabels(id));
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=eqa-panel-" + id + "-labels.pdf")
                .body(pdf);
    }

    private static Long longOrNull(Object value) {
        if (value == null || GenericValidator.isBlankOrNull(String.valueOf(value))) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a numeric id: " + value);
        }
    }

    private static String stringOrNull(Object value) {
        return value == null || GenericValidator.isBlankOrNull(String.valueOf(value)) ? null : String.valueOf(value);
    }

    /**
     * The unblind privilege, evaluated per call: sealed targets are revealed only
     * to holders of the dedicated unblind tier (qa.eqa.inhouse.unblind) or a global
     * admin.
     */
    private boolean callerCanUnblind() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (EQAGuards.UNBLIND_AUTHORITY.equals(authority.getAuthority())
                    || "ROLE_GLOBAL_ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    @ExceptionHandler(ObjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(ObjectNotFoundException e) {
        return Map.of("error", "EQA panel not found");
    }

    /** An illegal lifecycle move is a conflict with current state. */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleIllegalMove(IllegalStateException e) {
        return Map.of("error", e.getMessage());
    }

    /** A failed seal precondition is unprocessable input, not a conflict. */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, String> handleBadInput(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }
}
