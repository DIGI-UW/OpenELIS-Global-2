package org.openelisglobal.addon.packaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.addon.AddonCatalog;

/**
 * {@code addon.json}, schema version 1. Parsed once from the package, validated
 * against {@link AddonCatalog}, and kept verbatim in the registry.
 *
 * <pre>
 * {
 *   "schemaVersion": 1,
 *   "id": "hello", "name": "Hello", "version": "1.0.0", "publisher": "example",
 *   "description": "…",
 *   "image": { "ref": "ghcr.io/example/hello@sha256:&lt;64 hex&gt;" },
 *   "runtime": { "port": 8080, "eventsPath": "/events" },
 *   "scopes": ["orders.read", "results.read"],
 *   "events": ["result.validated"],
 *   "network": { "egress": ["api.example.org"] },
 *   "extensions": [ { "name": "hello-panel", "slot": "result-lookup-side-panel-slot",
 *                     "page": "panel.html", "order": 10, "meta": { "title": "Hello", "height": 240 } } ],
 *   "config": [ { "key": "greeting", "type": "string", "label": "Greeting", "default": "Hi" } ]
 * }
 * </pre>
 *
 * Extensions follow the OpenMRS 3 {@code ExtensionDefinition} (name, slot,
 * order, meta); {@code page} replaces OpenMRS' JavaScript component with a page
 * from the package's {@code ui/} folder, shown in a sandboxed iframe.
 */
public final class AddonManifest {

    public static final String FILE = "addon.json";
    public static final String UI_DIR = "ui/";

    /** Files a package's ui/ folder may contain, by extension. */
    public static final Map<String, String> UI_CONTENT_TYPES = Map.ofEntries(
            Map.entry("html", "text/html; charset=utf-8"), Map.entry("js", "text/javascript; charset=utf-8"),
            Map.entry("mjs", "text/javascript; charset=utf-8"), Map.entry("css", "text/css; charset=utf-8"),
            Map.entry("json", "application/json"), Map.entry("svg", "image/svg+xml"), Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"), Map.entry("jpeg", "image/jpeg"), Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"), Map.entry("woff2", "font/woff2"),
            Map.entry("txt", "text/plain; charset=utf-8"));

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String ID_PATTERN = "[a-z][a-z0-9-]{1,30}";
    private static final String EXTENSION_NAME_PATTERN = "[a-z][a-z0-9-]{1,40}";
    private static final String LOCALE_PATTERN = "[a-z]{2,3}(?:[-_][A-Za-z]{2})?";
    private static final String CONFIG_KEY_PATTERN = "[A-Za-z][A-Za-z0-9_]{0,40}";
    private static final String SEMVER_PATTERN = "\\d+\\.\\d+\\.\\d+([-+][0-9A-Za-z.-]+)?";
    private static final String IMAGE_PATTERN = "[a-z0-9][a-z0-9._/:-]{0,180}@sha256:[0-9a-f]{64}";
    private static final Set<String> CONFIG_TYPES = Set.of("string", "number", "boolean", "select", "secret");
    /** Config type whose values are stored encrypted and never shown again. */
    public static final String SECRET = "secret";

    /**
     * {@code icon}: for a button slot ({@code top-nav-actions-slot}), one of
     * {@link #ICONS}; anything else falls back to "application". {@code meta.title}
     * is a string or a map by locale ({@code {"en": "AI assistant", "vi": "Trợ lý
     * AI"}}); {@code title} is then the English one (or the first), and the UI
     * picks from {@code titles} by the user's locale.
     */
    public record Extension(String name, String slot, String page, int order, String title, int height, String icon,
            Map<String, String> titles) {
    }

    /**
     * Icons a header button may use (named after Carbon icons the UI maps them to).
     */
    public static final Set<String> ICONS = Set.of("application", "magic-wand", "chat", "help", "idea", "bot");

    public record ConfigField(String key, String type, String label, boolean required, Object defaultValue,
            List<String> options, Double min, Double max) {
    }

    private final JsonNode root;
    private final String raw;

    private AddonManifest(JsonNode root, String raw) {
        this.root = root;
        this.raw = raw;
    }

    /**
     * Parses without validating (for a manifest already stored in the registry).
     */
    public static AddonManifest parse(String json) {
        try {
            JsonNode root = JSON.readTree(json);
            if (root == null || !root.isObject()) {
                throw new AddonPackageException("manifest.invalid", "addon.json is not a JSON object");
            }
            return new AddonManifest(root, json);
        } catch (AddonPackageException e) {
            throw e;
        } catch (Exception e) {
            throw new AddonPackageException("manifest.invalid", "addon.json is not valid JSON");
        }
    }

    /** Parses and validates the manifest of a package. */
    public static AddonManifest fromPackage(Map<String, byte[]> files) {
        byte[] bytes = files.get(FILE);
        if (bytes == null) {
            throw new AddonPackageException("manifest.missing", "The package has no addon.json");
        }
        AddonManifest manifest = parse(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        List<String> problems = manifest.problems(files);
        if (!problems.isEmpty()) {
            throw new AddonPackageException("manifest.invalid", "addon.json is not valid", problems);
        }
        return manifest;
    }

    public String raw() {
        return raw;
    }

    public String id() {
        return root.path("id").asText();
    }

    public String name() {
        return root.path("name").asText();
    }

    public String version() {
        return root.path("version").asText();
    }

    public String publisher() {
        return root.path("publisher").asText();
    }

    public String description() {
        return root.hasNonNull("description") ? root.get("description").asText() : null;
    }

    public String image() {
        return root.path("image").path("ref").asText();
    }

    public int port() {
        return root.path("runtime").path("port").asInt(8080);
    }

    public String eventsPath() {
        return root.path("runtime").path("eventsPath").asText("/events");
    }

    public List<String> scopes() {
        return strings(root.path("scopes"));
    }

    public List<String> events() {
        return strings(root.path("events"));
    }

    /**
     * Hosts outside OpenELIS the add-on's service says it connects to
     * ({@code network.egress}). Shown for consent; enforcing it is the operator's
     * network policy.
     */
    public List<String> egress() {
        return strings(root.path("network").path("egress"));
    }

    /** Keys of the {@code secret} configuration fields. */
    public Set<String> secretKeys() {
        Set<String> out = new LinkedHashSet<>();
        config().stream().filter(f -> SECRET.equals(f.type())).forEach(f -> out.add(f.key()));
        return out;
    }

    /** UI files of the package (paths relative to {@code ui/}). */
    public static Map<String, byte[]> uiFiles(Map<String, byte[]> files) {
        Map<String, byte[]> out = new LinkedHashMap<>();
        files.forEach((name, content) -> {
            if (name.startsWith(UI_DIR) && name.length() > UI_DIR.length()) {
                out.put(name.substring(UI_DIR.length()), content);
            }
        });
        return out;
    }

    public static String contentType(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? null : UI_CONTENT_TYPES.get(path.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    public List<Extension> extensions() {
        List<Extension> out = new ArrayList<>();
        for (JsonNode e : root.path("extensions")) {
            JsonNode meta = e.path("meta");
            String icon = meta.path("icon").asText("application");
            Map<String, String> titles = new LinkedHashMap<>();
            JsonNode t = meta.path("title");
            if (t.isObject()) {
                t.fields().forEachRemaining(f -> {
                    if (f.getKey().matches(LOCALE_PATTERN) && f.getValue().isTextual()) {
                        titles.put(f.getKey(), f.getValue().asText());
                    }
                });
            }
            String title = t.isTextual() ? t.asText()
                    : titles.getOrDefault("en", titles.isEmpty() ? name() : titles.values().iterator().next());
            out.add(new Extension(e.path("name").asText(), e.path("slot").asText(), e.path("page").asText(),
                    e.path("order").asInt(100), title, Math.max(80, Math.min(900, meta.path("height").asInt(240))),
                    ICONS.contains(icon) ? icon : "application", titles));
        }
        return out;
    }

    public List<ConfigField> config() {
        List<ConfigField> out = new ArrayList<>();
        for (JsonNode c : root.path("config")) {
            JsonNode d = c.get("default");
            Object def = d == null || d.isNull() ? null
                    : d.isBoolean() ? (Object) d.asBoolean() : d.isNumber() ? (Object) d.asDouble() : d.asText();
            out.add(new ConfigField(c.path("key").asText(), c.path("type").asText("string"),
                    c.path("label").asText(c.path("key").asText()), c.path("required").asBoolean(false), def,
                    strings(c.path("options")), c.has("min") ? c.get("min").asDouble() : null,
                    c.has("max") ? c.get("max").asDouble() : null));
        }
        return out;
    }

    /** Everything wrong with this manifest, in plain English. Empty = valid. */
    public List<String> problems(Map<String, byte[]> files) {
        List<String> errs = new ArrayList<>();
        if (root.path("schemaVersion").asInt(0) != 1) {
            errs.add("schemaVersion must be 1");
        }
        for (String field : List.of("id", "name", "version", "publisher")) {
            if (!root.path(field).isTextual() || root.path(field).asText().isBlank()) {
                errs.add(field + " is required");
            }
        }
        if (!id().matches(ID_PATTERN)) {
            errs.add("id: lowercase letters, digits and dashes, 2 to 31 characters, starting with a letter");
        }
        if (name().length() > 120) {
            errs.add("name: at most 120 characters");
        }
        if (!version().matches(SEMVER_PATTERN) || version().length() > 40) {
            errs.add("version must be a semantic version (1.2.3)");
        }
        if (description() != null && description().length() > 1000) {
            errs.add("description: at most 1000 characters");
        }
        if (root.path("image").has("archive")) {
            errs.add("image.archive is not supported: publish the image to a registry and give image.ref");
        }
        if (!image().matches(IMAGE_PATTERN)) {
            errs.add("image.ref must be an image pinned by digest (registry/name@sha256:<64 hex digits>)");
        }
        JsonNode runtime = root.path("runtime");
        if (runtime.has("port") && (!runtime.get("port").canConvertToInt() || port() < 1 || port() > 65535)) {
            errs.add("runtime.port must be a port number");
        }
        if (!eventsPath().matches("/[A-Za-z0-9._/-]{0,100}")) {
            errs.add("runtime.eventsPath must be a path starting with /");
        }
        checkList(root, "scopes", errs);
        checkList(root, "events", errs);
        Set<String> scopes = new LinkedHashSet<>(scopes());
        for (String s : scopes) {
            if (!AddonCatalog.isScope(s)) {
                errs.add("unknown scope " + s);
            }
        }
        for (String e : events()) {
            String needed = AddonCatalog.scopeForEvent(e);
            if (needed == null) {
                errs.add("unknown event " + e);
            } else if (!scopes.contains(needed)) {
                errs.add("event " + e + " needs scope " + needed);
            }
        }
        JsonNode network = root.path("network");
        if (!network.isMissingNode() && !network.isObject()) {
            errs.add("network must be an object");
        }
        checkList(network, "egress", errs);
        List<String> egress = egress();
        if (egress.size() > AddonCatalog.MAX_EGRESS_HOSTS) {
            errs.add("network.egress: at most " + AddonCatalog.MAX_EGRESS_HOSTS + " hosts");
        }
        if (new LinkedHashSet<>(egress).size() != egress.size()) {
            errs.add("network.egress: a host is repeated");
        }
        for (String host : egress) {
            if (!AddonCatalog.isEgressHost(host)) {
                errs.add("network.egress: " + host
                        + " is not a host name (lowercase DNS name, optional :port; no IP address, no wildcard)");
            }
        }
        Set<String> names = new LinkedHashSet<>();
        for (JsonNode x : root.path("extensions")) {
            String name = x.path("name").asText();
            if (!name.matches(EXTENSION_NAME_PATTERN) || !names.add(name)) {
                errs.add("extension name is invalid or repeated: " + name);
            }
            if (AddonCatalog.slot(x.path("slot").asText()) == null) {
                errs.add("extension " + name + ": unknown slot " + x.path("slot").asText());
            }
            JsonNode title = x.path("meta").path("title");
            if (title.isObject()) {
                title.fields().forEachRemaining(f -> {
                    if (!f.getKey().matches(LOCALE_PATTERN) || !f.getValue().isTextual()
                            || f.getValue().asText().length() > 120) {
                        errs.add("extension " + name + ": meta.title." + f.getKey()
                                + " must be a locale (en, vi, fr-CA…) with a text of at most 120 characters");
                    }
                });
            } else if (!title.isMissingNode() && !title.isTextual()) {
                errs.add("extension " + name + ": meta.title must be a text or a map by locale");
            }
            String page = x.path("page").asText();
            if (page.isEmpty() || !files.containsKey(UI_DIR + page)) {
                errs.add("extension " + name + ": page ui/" + page + " is not in the package");
            }
        }
        Set<String> keys = new LinkedHashSet<>();
        for (JsonNode c : root.path("config")) {
            String key = c.path("key").asText();
            if (!key.matches(CONFIG_KEY_PATTERN) || !keys.add(key)) {
                errs.add("config key is invalid or repeated: " + key);
            }
            String type = c.path("type").asText("string");
            if (!CONFIG_TYPES.contains(type)) {
                errs.add("config " + key + ": type " + type + " is not supported");
            }
            if ("select".equals(type) && c.path("options").size() == 0) {
                errs.add("config " + key + ": a select needs options");
            }
            if (SECRET.equals(type) && c.has("default")) {
                errs.add("config " + key + ": a secret cannot have a default");
            }
        }
        for (String path : uiFiles(files).keySet()) {
            if (contentType(path) == null) {
                errs.add("ui/" + path + ": file type not allowed in the UI folder");
            }
        }
        return errs;
    }

    private static void checkList(JsonNode root, String field, List<String> errs) {
        JsonNode node = root.path(field);
        if (!node.isMissingNode() && !node.isArray()) {
            errs.add(field + " must be a list");
        }
    }

    /**
     * Checks and normalises configuration values against the manifest. Unknown keys
     * are refused; missing values take the default.
     *
     * @throws IllegalArgumentException naming the first bad key
     */
    public Map<String, Object> validateConfig(Map<String, ?> values) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<ConfigField> fields = config();
        if (values != null) {
            for (String k : values.keySet()) {
                if (fields.stream().noneMatch(f -> f.key().equals(k))) {
                    throw new IllegalArgumentException(k);
                }
            }
        }
        for (ConfigField f : fields) {
            if (SECRET.equals(f.type())) {
                continue; // see validateSecrets
            }
            Object v = values == null ? null : values.get(f.key());
            if (v == null || (v instanceof String s && s.isBlank() && !"string".equals(f.type()))) {
                if (f.required() && f.defaultValue() == null) {
                    throw new IllegalArgumentException(f.key());
                }
                if (f.defaultValue() != null) {
                    out.put(f.key(), f.defaultValue());
                }
                continue;
            }
            out.put(f.key(), configValue(f, v));
        }
        return out;
    }

    /**
     * The {@code secret} values among {@code values}: a non-empty string sets the
     * secret, an empty string clears it, an absent key leaves it unchanged (the
     * administration page never receives secrets back, so it only sends changed
     * ones).
     *
     * @return key → new value, or key → "" to clear
     * @throws IllegalArgumentException naming the first bad key
     */
    public Map<String, String> validateSecrets(Map<String, ?> values) {
        Map<String, String> out = new LinkedHashMap<>();
        if (values == null) {
            return out;
        }
        for (String key : secretKeys()) {
            if (!values.containsKey(key) || values.get(key) == null) {
                continue;
            }
            String s = String.valueOf(values.get(key));
            if (s.length() > 500) {
                throw new IllegalArgumentException(key);
            }
            out.put(key, s.strip());
        }
        return out;
    }

    private static Object configValue(ConfigField f, Object v) {
        String s = String.valueOf(v);
        switch (f.type()) {
        case "boolean":
            if (v instanceof Boolean b) {
                return b;
            }
            if ("true".equals(s) || "false".equals(s)) {
                return Boolean.parseBoolean(s);
            }
            throw new IllegalArgumentException(f.key());
        case "number":
            double d;
            try {
                d = v instanceof Number n ? n.doubleValue() : Double.parseDouble(s);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(f.key());
            }
            if (Double.isNaN(d) || Double.isInfinite(d) || (f.min() != null && d < f.min())
                    || (f.max() != null && d > f.max())) {
                throw new IllegalArgumentException(f.key());
            }
            return d;
        case "select":
            if (!f.options().contains(s)) {
                throw new IllegalArgumentException(f.key());
            }
            return s;
        default:
            if (s.length() > 500 || (f.required() && s.isBlank())) {
                throw new IllegalArgumentException(f.key());
            }
            return s;
        }
    }

    private static List<String> strings(JsonNode array) {
        List<String> out = new ArrayList<>();
        if (array.isArray()) {
            array.forEach(n -> out.add(n.asText()));
        }
        return out;
    }
}
