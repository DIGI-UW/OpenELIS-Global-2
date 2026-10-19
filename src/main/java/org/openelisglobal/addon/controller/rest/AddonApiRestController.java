package org.openelisglobal.addon.controller.rest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.openelisglobal.addon.AddonCatalog;
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
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping("/whoami")
    public Map<String, Object> whoami() {
        AddonToken.Claims claims = claims();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("addon", claims.addonId());
        out.put("kind", claims.isService() ? "service" : "user");
        out.put("scopes", claims.scopes());
        if (!claims.isService()) {
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
