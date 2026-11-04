package org.openelisglobal.addon.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.addon.service.AddonRegistryService;
import org.openelisglobal.addon.service.AddonRegistryService.ExtensionView;
import org.openelisglobal.addon.service.AddonTokenService;
import org.openelisglobal.addon.service.AddonTokenService.IssuedToken;
import org.openelisglobal.addon.service.AddonTokenService.TokenRefusedException;
import org.openelisglobal.addon.valueholder.AddonAsset;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * What the frontend's extension slots use: the extensions of enabled add-ons, a
 * scoped token for one add-on frame, and the files of the frames.
 */
@RestController
@RequestMapping("/rest/addons")
public class AddonUiRestController extends BaseRestController {

    /**
     * Header for add-on UI files. {@code sandbox} gives the document an opaque
     * origin even when it is opened directly, not only inside the iframe: no access
     * to OpenELIS cookies, storage or same-origin APIs.
     */
    static final String FRAME_CSP = "sandbox allow-scripts allow-forms; frame-ancestors 'self'";

    @Autowired
    private AddonRegistryService registry;
    @Autowired
    private AddonTokenService tokens;

    @GetMapping("/extensions")
    @PreAuthorize("isAuthenticated()")
    public Map<String, List<ExtensionView>> extensions() {
        return Map.of("extensions", registry.activeExtensions());
    }

    /**
     * Body: {@code {"slot": "...", "context": {"page": "...", "accessionNumbers":
     * [...]}}}. Answers the token and the context as the add-on will see it.
     */
    @PostMapping("/{id}/token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> token(@PathVariable String id, @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Object context = body.get("context");
        @SuppressWarnings("unchecked")
        Map<String, Object> ctx = context instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
        try {
            IssuedToken issued = tokens.issue(id, stringField(body, "slot"), ctx, getSysUserId(request),
                    currentRoles());
            return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(issued);
        } catch (TokenRefusedException e) {
            return switch (e.getRefusal()) {
            case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("code", "addon"));
            case SLOT -> ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("code", "slot"));
            case ROLE -> ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("code", "role"));
            };
        }
    }

    /** A file of an enabled add-on's UI. Open (see AddonSecurity.UI_ASSET_PATH). */
    @GetMapping("/{id}/ui/**")
    public ResponseEntity<byte[]> asset(@PathVariable String id, HttpServletRequest request) {
        String marker = "/rest/addons/" + id + "/ui/";
        String uri = request.getRequestURI();
        int at = uri.indexOf(marker);
        String path = at < 0 ? "" : uri.substring(at + marker.length());
        if (!path.matches("[A-Za-z0-9._-]+(/[A-Za-z0-9._-]+)*") || path.contains("..")) {
            return ResponseEntity.notFound().build();
        }
        return registry.asset(id, path).map(AddonUiRestController::serve)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static ResponseEntity<byte[]> serve(AddonAsset asset) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, asset.getContentType())
                .header("Content-Security-Policy", FRAME_CSP).header("X-Content-Type-Options", "nosniff")
                .header("Referrer-Policy", "no-referrer").header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(asset.getContent());
    }

    private static Set<String> currentRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Set.of();
        }
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5)).collect(Collectors.toSet());
    }
}
