package org.openelisglobal.addon.controller.rest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.openelisglobal.addon.AddonCatalog;
import org.openelisglobal.addon.ai.AiContextService;
import org.openelisglobal.addon.security.AddonAuthenticationToken;
import org.openelisglobal.addon.security.AddonToken;
import org.openelisglobal.addon.service.AddonDataService;
import org.openelisglobal.addon.service.AddonRegistryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The add-on API, {@code /rest/addon-api/v1}, called by add-on containers with
 * {@code Authorization: Bearer <token or service key>}. No user session is
 * involved; SecurityConfig's add-on chain authenticates the caller. Each read
 * needs a scope (403 without it) and an order the credentials may read (403
 * otherwise).
 */
@RestController
@RequestMapping("/rest/addon-api/v1")
public class AddonApiRestController {

    @Autowired
    private AddonDataService data;
    @Autowired
    private AddonRegistryService registry;
    @Autowired
    private AiContextService aiContext;

    @GetMapping("/whoami")
    public Map<String, Object> whoami() {
        AddonToken.Claims claims = claims();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("addon", claims.addonId());
        out.put("kind", claims.isService() ? "service" : "user");
        out.put("scopes", claims.scopes());
        if (!claims.isService() && claims.scopes().contains(AddonCatalog.ORDERS_READ)) {
            out.put("accessionNumbers", claims.accessions());
        }
        return out;
    }

    /** The add-on's configuration values, as set on the Extensions page. */
    @GetMapping("/config")
    public Map<String, Object> config() {
        return registry.configOf(claims().addonId());
    }

    @GetMapping("/orders/{accession}")
    public ResponseEntity<Map<String, Object>> order(@PathVariable String accession) {
        return read(accession, AddonCatalog.ORDERS_READ, () -> data.order(accession));
    }

    @GetMapping("/orders/{accession}/results")
    public ResponseEntity<Map<String, Object>> results(@PathVariable String accession) {
        return read(accession, AddonCatalog.RESULTS_READ, () -> data.results(accession));
    }

    @GetMapping("/orders/{accession}/patient")
    public ResponseEntity<Map<String, Object>> patient(@PathVariable String accession) {
        return read(accession, AddonCatalog.PATIENT_READ, () -> data.patient(accession));
    }

    /**
     * The de-identified context of the orders on the user's screen
     * ({@code ai.context}). Only for a UI token: a service key has no screen.
     *
     * @param view {@code cases} (one case per order) or {@code patient} (one
     *             patient: the newest day is current, older orders are history)
     */
    @GetMapping("/ai/context")
    public ResponseEntity<Map<String, Object>> aiContext(
            @RequestParam(name = "view", required = false, defaultValue = "cases") String view) {
        AddonToken.Claims claims = claims();
        ResponseEntity<Map<String, Object>> refused = aiRefusal(claims);
        return refused != null ? refused : ResponseEntity.ok(aiContext.context(claims, view));
    }

    /**
     * Body {@code {"text": "..."}}: the text without the identifiers of the
     * patients of the token's orders (and without identifier-like patterns), for
     * example a question the user typed before it goes to a language model.
     */
    @PostMapping("/ai/deidentify")
    public ResponseEntity<Map<String, Object>> deidentify(@RequestBody Map<String, Object> body) {
        AddonToken.Claims claims = claims();
        ResponseEntity<Map<String, Object>> refused = aiRefusal(claims);
        if (refused != null) {
            return refused;
        }
        Object text = body == null ? null : body.get("text");
        if (!(text instanceof String s)) {
            return ResponseEntity.badRequest().body(Map.of("code", "text"));
        }
        String clean = aiContext.deidentify(claims, s);
        return ResponseEntity.ok(Map.of("text", clean, "changed", !clean.equals(s)));
    }

    private static ResponseEntity<Map<String, Object>> aiRefusal(AddonToken.Claims claims) {
        if (!claims.scopes().contains(AddonCatalog.AI_CONTEXT)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "scope", "scope", AddonCatalog.AI_CONTEXT));
        }
        if (claims.isService()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("code", "context"));
        }
        return null;
    }

    private ResponseEntity<Map<String, Object>> read(String accession, String scope,
            Supplier<Optional<Map<String, Object>>> load) {
        AddonToken.Claims claims = claims();
        if (!claims.scopes().contains(scope)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("code", "scope", "scope", scope));
        }
        if (!data.mayRead(claims, accession)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("code", "context"));
        }
        return load.get().map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("code", "order")));
    }

    private static AddonToken.Claims claims() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof AddonAuthenticationToken token) {
            return token.getClaims();
        }
        // unreachable behind the add-on security chain
        throw new IllegalStateException("not an add-on caller");
    }
}
