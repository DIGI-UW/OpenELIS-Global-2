package org.openelisglobal.addon.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.addon.AddonCatalog;
import org.openelisglobal.addon.dao.AddonAccessLogDAO;
import org.openelisglobal.addon.packaging.AddonPackageException;
import org.openelisglobal.addon.packaging.AddonPackageReader;
import org.openelisglobal.addon.service.AddonAccessLogService;
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
    @Autowired
    private AddonAccessLogService accessLog;

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
    /**
     * The access log of one add-on: every authenticated request it made to the
     * add-on API, newest first. {@code actor} and {@code purpose} are what the
     * add-on reported. {@code format=csv} downloads it.
     */
    @GetMapping("/{id}/access-log")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> accessLog(@PathVariable String id, @RequestParam(required = false) String from,
            @RequestParam(required = false) String to, @RequestParam(required = false) String actor,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false, defaultValue = "200") int limit,
            @RequestParam(required = false, defaultValue = "json") String format) {
        Instant f;
        Instant t;
        try {
            f = from == null || from.isBlank() ? null
                    : LocalDate.parse(from).atStartOfDay(ZoneId.systemDefault()).toInstant();
            t = to == null || to.isBlank() ? null
                    : LocalDate.parse(to).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("code", "range"));
        }
        if (limit < 1 || limit > 5000) {
            return ResponseEntity.badRequest().body(Map.of("code", "param", "param", "limit"));
        }
        List<AddonAccessLogDAO.Entry> entries = accessLog.find(id, f, t, blank(actor), blank(subject), limit);
        if ("csv".equals(format)) {
            StringBuilder csv = new StringBuilder(
                    "at,addon,credential,user,actor,purpose,method,endpoint,subjectType,subject,status,rows\n");
            for (AddonAccessLogDAO.Entry e : entries) {
                csv.append(String.join(",", csvCell(String.valueOf(e.at())), csvCell(e.addonId()),
                        csvCell(e.credential()), csvCell(e.userId()), csvCell(e.actor()), csvCell(e.purpose()),
                        csvCell(e.method()), csvCell(e.endpoint()), csvCell(e.subjectType()), csvCell(e.subject()),
                        String.valueOf(e.status()), e.rows() == null ? "" : String.valueOf(e.rows()))).append('\n');
            }
            return ResponseEntity.ok().header("Content-Type", "text/csv; charset=UTF-8")
                    .header("Content-Disposition", "attachment; filename=\"" + id + "-access-log.csv\"")
                    .body(csv.toString());
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("addon", id);
        out.put("retentionDays", settings.accessLogRetentionDays());
        out.put("entries", entries.stream().map(AddonAdminRestController::entryView).toList());
        return ResponseEntity.ok(out);
    }

    /**
     * An entry for the page: the time as an ISO-8601 instant, absent values as
     * null.
     */
    static Map<String, Object> entryView(AddonAccessLogDAO.Entry e) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("at", e.at() == null ? null : e.at().toString());
        v.put("addonId", e.addonId());
        v.put("credential", e.credential());
        v.put("userId", e.userId());
        v.put("actor", e.actor());
        v.put("purpose", e.purpose());
        v.put("method", e.method());
        v.put("endpoint", e.endpoint());
        v.put("subjectType", e.subjectType());
        v.put("subject", e.subject());
        v.put("status", e.status());
        v.put("rows", e.rows());
        return v;
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** A CSV cell; a leading formula character is neutralised for spreadsheets. */
    static String csvCell(String value) {
        if (value == null) {
            return "";
        }
        String v = value;
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

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
