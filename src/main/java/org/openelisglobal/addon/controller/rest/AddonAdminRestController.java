package org.openelisglobal.addon.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.addon.AddonCatalog;
import org.openelisglobal.addon.packaging.AddonPackageException;
import org.openelisglobal.addon.packaging.AddonPackageReader;
import org.openelisglobal.addon.service.AddonRegistryService;
import org.openelisglobal.addon.service.AddonRegistryService.AddonView;
import org.openelisglobal.addon.service.AddonRegistryService.Credentials;
import org.openelisglobal.addon.service.AddonRegistryService.InstallResult;
import org.openelisglobal.addon.service.AddonSettings;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Administration of add-ons (the Extensions page): install a package, list,
 * enable or disable, configure, rotate credentials, remove. Installing
 * registers the add-on; the operator runs its container (see
 * docs/addons/design.md).
 */
@RestController
@RequestMapping("/rest/addons")
public class AddonAdminRestController extends BaseRestController {

    @Autowired
    private AddonRegistryService registry;
    @Autowired
    private AddonSettings settings;

    /**
     * Scopes, events and slots; read by the administration page and the UI slots.
     */
    @GetMapping("/catalog")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> catalog() {
        return AddonCatalog.describe();
    }

    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> list() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("addons", registry.list());
        out.put("trustedPublishers", List.copyOf(settings.trustedPublishers().keySet()));
        return out;
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> install(@RequestParam("package") MultipartFile file, HttpServletRequest request)
            throws IOException {
        if (file.getSize() > AddonPackageReader.MAX_PACKAGE_BYTES) {
            return refused(new AddonPackageException("package.tooLarge", "The package is larger than 10 MB"));
        }
        try {
            InstallResult result = registry.install(file.getBytes(), getSysUserId(request));
            return ResponseEntity.status(result.upgraded() ? HttpStatus.OK : HttpStatus.CREATED).body(result);
        } catch (AddonPackageException e) {
            return refused(e);
        }
    }

    @PutMapping("/{id}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> setEnabled(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Object enabled = body.get("enabled");
        if (!(enabled instanceof Boolean on)) {
            return ResponseEntity.badRequest().body(Map.of("code", "enabled"));
        }
        if (!registry.setEnabled(id, on)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(registry.get(id).orElseThrow());
    }

    @PutMapping("/{id}/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> saveConfig(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Object values = body.get("values");
        @SuppressWarnings("unchecked")
        Map<String, Object> map = values instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
        try {
            return registry.saveConfig(id, map).<ResponseEntity<?>>map(c -> ResponseEntity.ok(Map.of("config", c)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("code", "config", "field", String.valueOf(e.getMessage())));
        }
    }

    @PutMapping("/{id}/endpoint")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> setEndpoint(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            if (!registry.setEndpoint(id, stringField(body, "endpoint"))) {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("code", "endpoint"));
        }
        return ResponseEntity.ok(registry.get(id).orElseThrow());
    }

    /** New API key and webhook secret; the old ones stop working at once. */
    @PostMapping("/{id}/credentials")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Credentials> rotateCredentials(@PathVariable String id) {
        return registry.rotateCredentials(id).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> uninstall(@PathVariable String id) {
        return registry.uninstall(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/deliveries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> deliveries(@PathVariable String id) {
        if (registry.get(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(registry.deliveries(id, 50));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AddonView> get(@PathVariable String id) {
        return registry.get(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static ResponseEntity<Map<String, Object>> refused(AddonPackageException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", e.getCode());
        body.put("message", e.getMessage());
        body.put("problems", e.getProblems());
        return ResponseEntity.badRequest().body(body);
    }
}
