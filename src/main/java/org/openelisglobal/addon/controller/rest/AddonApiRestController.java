package org.openelisglobal.addon.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.openelisglobal.addon.AddonCatalog;
import org.openelisglobal.addon.ai.AiContextService;
import org.openelisglobal.addon.security.AddonAuthenticationToken;
import org.openelisglobal.addon.security.AddonToken;
import org.openelisglobal.addon.service.AddonAccessLogService;
import org.openelisglobal.addon.service.AddonDataService;
import org.openelisglobal.addon.service.AddonQueryService;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * The add-on API, {@code /rest/addon-api/v1}, called by add-on containers with
 * {@code Authorization: Bearer <token or service key>}. No user session is
 * involved; SecurityConfig's add-on chain authenticates the caller. Each read
 * needs a scope (403 without it) and an order the credentials may read (403
 * otherwise).
 *
 * <p>
 * Laboratory-wide reads (order lists, worklists, patients, statistics, the test
 * catalog) need their own scopes and are bounded: dates span at most
 * {@link AddonCatalog#MAX_RANGE_DAYS} days, lists hold at most
 * {@link AddonCatalog#MAX_LIST_ROWS} rows. Every request is written to the
 * access log ({@link AddonAccessLogService}) with the order, patient or query
 * it was about.
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
    @Autowired
    private AddonQueryService query;

    @GetMapping("/whoami")
    public Map<String, Object> whoami() {
        AddonToken.Claims claims = claims();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("addon", claims.addonId());
        out.put("kind", claims.isService() ? "service" : "user");
        out.put("scopes", claims.scopes());
        if (!claims.isService()) {
            out.put("userId", claims.userId());
            if (claims.slot() != null) {
                out.put("slot", claims.slot());
            }
            if (claims.scopes().contains(AddonCatalog.ORDERS_READ)) {
                out.put("accessionNumbers", claims.accessions());
            }
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
        return read(accession, AddonCatalog.ORDERS_READ, () -> withIdentity(accession, data.order(accession)));
    }

    @GetMapping("/orders/{accession}/results")
    public ResponseEntity<Map<String, Object>> results(@PathVariable String accession) {
        return read(accession, AddonCatalog.RESULTS_READ, () -> data.results(accession).map(out -> {
            rows(out.get("results"));
            return out;
        }));
    }

    @GetMapping("/orders/{accession}/patient")
    public ResponseEntity<Map<String, Object>> patient(@PathVariable String accession) {
        return read(accession, AddonCatalog.PATIENT_READ, () -> withIdentity(accession, data.patient(accession)));
    }

    /** With {@code patient.identity}, an order also names its patient. */
    private Optional<Map<String, Object>> withIdentity(String accession, Optional<Map<String, Object>> found) {
        if (found.isEmpty() || !claims().scopes().contains(AddonCatalog.PATIENT_IDENTITY)) {
            return found;
        }
        Map<String, Object> out = new LinkedHashMap<>(found.get());
        query.patientOfOrder(accession).ifPresent(p -> out.put("patient", p));
        return Optional.of(out);
    }

    private static final Set<String> ORDER_STATUSES = Set.of("any", "pending", "awaiting-validation", "finalized");

    /**
     * Orders received between two days (inclusive), newest first, with the count of
     * their tests in each state. Needs {@code orders.search} and
     * {@code orders.read}; with {@code patient.identity} each order names its
     * patient.
     */
    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> orders(@RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false, defaultValue = "any") String status,
            @RequestParam(required = false) String section,
            @RequestParam(required = false, defaultValue = "50") int limit,
            @RequestParam(required = false, defaultValue = "0") int offset) {
        note(AddonAccessLogService.QUERY,
                "orders " + from + ".." + to + " " + status + (section == null ? "" : " " + section));
        ResponseEntity<Map<String, Object>> refused = needs(AddonCatalog.ORDERS_SEARCH, AddonCatalog.ORDERS_READ);
        if (refused != null) {
            return refused;
        }
        LocalDate[] range = range(from, to);
        if (range == null) {
            return bad("range");
        }
        if (!ORDER_STATUSES.contains(status)) {
            return bad("param", "status");
        }
        if (limit < 1 || limit > AddonCatalog.MAX_LIST_ROWS || offset < 0 || offset > 100_000) {
            return bad("param", "limit");
        }
        Map<String, Object> out = query.orders(range[0], range[1], status, blank(section), limit, offset,
                claims().scopes().contains(AddonCatalog.PATIENT_IDENTITY));
        rows(out.get("orders"));
        return ResponseEntity.ok(out);
    }

    /**
     * Tests waiting for a result ({@code result-entry}) or for validation
     * ({@code validation}), oldest order first. Needs {@code orders.search}.
     */
    @GetMapping("/worklist")
    public ResponseEntity<Map<String, Object>> worklist(@RequestParam(required = false) String stage,
            @RequestParam(required = false) String section,
            @RequestParam(required = false, defaultValue = "50") int limit) {
        note(AddonAccessLogService.QUERY, "worklist " + stage + (section == null ? "" : " " + section));
        ResponseEntity<Map<String, Object>> refused = needs(AddonCatalog.ORDERS_SEARCH);
        if (refused != null) {
            return refused;
        }
        if (!"result-entry".equals(stage) && !"validation".equals(stage)) {
            return bad("param", "stage");
        }
        if (limit < 1 || limit > AddonCatalog.MAX_LIST_ROWS) {
            return bad("param", "limit");
        }
        Map<String, Object> out = query.worklist(stage, blank(section), limit);
        rows(out.get("items"));
        return ResponseEntity.ok(out);
    }

    /**
     * Patients whose name matches (as on the patient search page) or with exactly
     * this identifier; at most {@link AddonCatalog#MAX_PATIENT_MATCHES}. Needs
     * {@code patient.identity}.
     */
    @GetMapping("/patients")
    public ResponseEntity<Map<String, Object>> patients(@RequestParam(required = false) String q) {
        note(AddonAccessLogService.QUERY, "patients " + (q == null ? "" : q.trim()));
        ResponseEntity<Map<String, Object>> refused = needs(AddonCatalog.PATIENT_IDENTITY);
        if (refused != null) {
            return refused;
        }
        if (q == null || q.trim().length() < 2 || q.length() > 100) {
            return bad("param", "q");
        }
        Map<String, Object> out = query.findPatients(q);
        rows(out.get("patients"));
        return ResponseEntity.ok(out);
    }

    @GetMapping("/patients/{patientId}")
    public ResponseEntity<Map<String, Object>> patientById(@PathVariable String patientId) {
        note(AddonAccessLogService.PATIENT, patientId);
        ResponseEntity<Map<String, Object>> refused = needs(AddonCatalog.PATIENT_IDENTITY);
        if (refused != null) {
            return refused;
        }
        return query.patient(patientId).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("code", "patient")));
    }

    /**
     * A patient's orders, newest first (at most 20). Needs
     * {@code patient.identity}.
     */
    @GetMapping("/patients/{patientId}/orders")
    public ResponseEntity<Map<String, Object>> patientOrders(@PathVariable String patientId,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        note(AddonAccessLogService.PATIENT, patientId);
        ResponseEntity<Map<String, Object>> refused = needs(AddonCatalog.PATIENT_IDENTITY);
        if (refused != null) {
            return refused;
        }
        if (limit < 1 || limit > AddonCatalog.MAX_PATIENT_MATCHES) {
            return bad("param", "limit");
        }
        return query.patientOrders(patientId, limit).map(out -> {
            rows(out.get("orders"));
            return ResponseEntity.ok(out);
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("code", "patient")));
    }

    private static final Set<String> COUNT_GROUPS = Set.of("day", "section", "status");
    private static final Set<String> TAT_GROUPS = Set.of("section", "test");

    /**
     * Counts of orders and tests received between two days. Needs
     * {@code stats.read}.
     */
    @GetMapping("/stats/orders")
    public ResponseEntity<Map<String, Object>> orderStats(@RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false, defaultValue = "day") String groupBy) {
        return stats(from, to, groupBy, COUNT_GROUPS, "orders", query::orderCounts);
    }

    /**
     * Turnaround time, sample received to result released, of the results released
     * between two days: count, median and 90th percentile in minutes. Needs
     * {@code stats.read}.
     */
    @GetMapping("/stats/tat")
    public ResponseEntity<Map<String, Object>> tatStats(@RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false, defaultValue = "section") String groupBy) {
        return stats(from, to, groupBy, TAT_GROUPS, "tat", query::turnaround);
    }

    private interface StatsQuery {
        Map<String, Object> run(LocalDate from, LocalDate to, String groupBy);
    }

    private ResponseEntity<Map<String, Object>> stats(String from, String to, String groupBy, Set<String> groups,
            String name, StatsQuery run) {
        note(AddonAccessLogService.QUERY, "stats " + name + " " + from + ".." + to + " by " + groupBy);
        ResponseEntity<Map<String, Object>> refused = needs(AddonCatalog.STATS_READ);
        if (refused != null) {
            return refused;
        }
        LocalDate[] range = range(from, to);
        if (range == null) {
            return bad("range");
        }
        if (!groups.contains(groupBy)) {
            return bad("param", "groupBy");
        }
        Map<String, Object> out = run.run(range[0], range[1], groupBy);
        rows(out.get("rows"));
        return ResponseEntity.ok(out);
    }

    /** The test catalog. Needs {@code catalog.read}. */
    @GetMapping("/catalog/tests")
    public ResponseEntity<Map<String, Object>> tests(@RequestParam(required = false) String section,
            @RequestParam(required = false, defaultValue = "true") String active) {
        note(AddonAccessLogService.QUERY, "catalog tests" + (section == null ? "" : " " + section));
        ResponseEntity<Map<String, Object>> refused = needs(AddonCatalog.CATALOG_READ);
        if (refused != null) {
            return refused;
        }
        if (!Set.of("true", "false", "any").contains(active)) {
            return bad("param", "active");
        }
        Boolean onlyActive = "any".equals(active) ? null : Boolean.valueOf(active);
        Map<String, Object> out = query.tests(blank(section), onlyActive);
        rows(out.get("tests"));
        return ResponseEntity.ok(out);
    }

    /** 403 naming the first missing scope, or null if all are granted. */
    private static ResponseEntity<Map<String, Object>> needs(String... scopes) {
        List<String> granted = claims().scopes();
        for (String scope : scopes) {
            if (!granted.contains(scope)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("code", "scope", "scope", scope));
            }
        }
        return null;
    }

    /**
     * {@code from}..{@code to} as days, both given, in order, at most
     * {@link AddonCatalog#MAX_RANGE_DAYS} days apart; null otherwise.
     */
    static LocalDate[] range(String from, String to) {
        try {
            LocalDate f = LocalDate.parse(from);
            LocalDate t = LocalDate.parse(to);
            if (t.isBefore(f) || ChronoUnit.DAYS.between(f, t) >= AddonCatalog.MAX_RANGE_DAYS) {
                return null;
            }
            return new LocalDate[] { f, t };
        } catch (DateTimeParseException | NullPointerException e) {
            return null;
        }
    }

    private static ResponseEntity<Map<String, Object>> bad(String code) {
        return ResponseEntity.badRequest().body(Map.of("code", code, "maxDays", AddonCatalog.MAX_RANGE_DAYS));
    }

    private static ResponseEntity<Map<String, Object>> bad(String code, String param) {
        return ResponseEntity.badRequest().body(Map.of("code", code, "param", param));
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static void note(String type, String subject) {
        HttpServletRequest request = request();
        if (request != null) {
            AddonAccessLogService.subject(request, type, subject);
        }
    }

    private static void rows(Object list) {
        HttpServletRequest request = request();
        if (request != null && list instanceof List<?> l) {
            AddonAccessLogService.rows(request, l.size());
        }
    }

    private static HttpServletRequest request() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a ? a.getRequest()
                : null;
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
        note(AddonAccessLogService.ORDER, accession);
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
