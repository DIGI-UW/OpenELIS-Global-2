package org.openelisglobal.eqa.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.eqa.service.EQAPanelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    public EQAPanelRestController(EQAPanelService panelService) {
        this.panelService = panelService;
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

    @PostMapping(value = "/panels/{id}/unblind", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(EQAGuards.UNBLIND)
    public Map<String, Object> unblind(HttpServletRequest request, @PathVariable Long id) {
        return panelService.toPanelDto(panelService.unblind(id, getSysUserId(request)));
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
