package org.openelisglobal.addons.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * The add-on's HTTP service.
 *
 * <pre>
 * GET  /health                          state, for the operator
 * GET  /api/status?page=&amp;name=           enabled, cap, suggestions, current conversation
 * POST /api/ask                         {page, name, question, suggestionId, conversationId, newConversation}
 *                                       text/event-stream (meta, delta, reset, done, error); ?stream=0 → one JSON
 * POST /api/continue                    {page, name, previousId}
 * GET  /api/conversations[/{id}]        the user's own conversations
 * </pre>
 *
 * Every /api call carries the frame's OpenELIS token
 * ({@code Authorization: Bearer}); the service sends it on to OpenELIS for the
 * de-identified context and the user id, so a disabled add-on or an expired
 * token stops everything. The frame has an opaque origin: /api answers CORS for
 * any origin, without credentials.
 */
public final class Main {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> PAGES = Set.of(ContextText.LOOKUP, ContextText.VALIDATION, ContextText.OTHER);
    private static final int BODY_MAX = 16 * 1024;

    private final AssistantService service;
    private final OpenElisClient openelis;
    private final java.util.function.Supplier<Settings> settings;
    private final Semaphore asking;

    Main(AssistantService service, OpenElisClient openelis, java.util.function.Supplier<Settings> settings,
            int concurrency) {
        this.service = service;
        this.openelis = openelis;
        this.settings = settings;
        this.asking = new Semaphore(Math.max(1, concurrency));
    }

    public static void main(String[] args) throws IOException {
        String api = env("OPENELIS_API_URL", "https://oe.openelis.org:8443/api/OpenELIS-Global/rest/addon-api/v1");
        OpenElisClient openelis = new OpenElisClient(api, env("OPENELIS_API_KEY", ""), env("OPENELIS_CA_FILE", ""));
        java.util.function.Supplier<Settings> settings = () -> Settings.from(openelis.config());
        Clock clock = Clock.systemUTC();
        AiStore store;
        Path data = Path.of(env("ASSISTANT_DATA_DIR", "/data"));
        try {
            store = new FileAiStore(clock, data);
        } catch (IOException | RuntimeException e) {
            System.err.println("WARNING: " + data + " is not writable: the usage log (daily cap) stays in memory");
            store = new MemoryAiStore(clock);
        }
        AssistantService service = new AssistantService(settings, openelis, store,
                AssistantService::configuredProviders, clock);
        int port = Integer.parseInt(env("PORT", "8080"));
        Main main = new Main(service, openelis, settings, Integer.parseInt(env("ASSISTANT_CONCURRENCY", "4")));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 64);
        server.createContext("/", main::handle);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        System.out.println("assistant add-on listening on :" + port + ", OpenELIS " + api + ", store "
                + store.getClass().getSimpleName() + ", egress " + Egress.allowed());
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return v == null || v.isBlank() ? fallback : v.trim();
    }

    // ------------------------------------------------------------------ routing

    void handle(HttpExchange ex) throws IOException {
        try (ex) {
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            if (path.equals("/health") && method.equals("GET")) {
                health(ex);
                return;
            }
            if (path.equals("/events") && method.equals("POST")) {
                ex.sendResponseHeaders(204, -1); // this add-on subscribes to no event
                return;
            }
            if (!path.startsWith("/api/")) {
                json(ex, 404, Map.of("code", "not-found"));
                return;
            }
            cors(ex);
            if (method.equals("OPTIONS")) {
                ex.sendResponseHeaders(204, -1);
                return;
            }
            String token = bearer(ex);
            Map<String, String> q = query(ex.getRequestURI().getRawQuery());
            try {
                switch (method + " " + path) {
                case "GET /api/status" -> json(ex, 200, service.status(ask(token, q.get("page"), q.get("name"), null,
                        null, q.get("conversationId"), false)));
                case "POST /api/ask" -> ask(ex, token, q);
                case "POST /api/continue" -> {
                    JsonNode b = body(ex);
                    Map<String, Object> c = service.continuePrevious(
                            ask(token, text(b, "page"), text(b, "name"), null, null, null, true),
                            text(b, "previousId"));
                    json(ex, c == null ? 404 : 200, c == null ? Map.of("code", "not-found") : c);
                }
                case "GET /api/conversations" -> json(ex, 200, service.history(
                        ask(token, q.get("page"), q.get("name"), null, null, null, false), parse(q.get("limit"), 15)));
                default -> {
                    if (method.equals("GET") && path.startsWith("/api/conversations/")) {
                        Map<String, Object> c = service.conversation(
                                ask(token, q.get("page"), q.get("name"), null, null, null, false),
                                path.substring("/api/conversations/".length()));
                        json(ex, c == null ? 404 : 200, c == null ? Map.of("code", "not-found") : c);
                    } else {
                        json(ex, 404, Map.of("code", "not-found"));
                    }
                }
                }
            } catch (AssistantService.ContextException e) {
                json(ex, statusOf(e.code), Map.of("code", e.code));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (UncheckedIOException e) {
                // the browser went away
            } catch (RuntimeException e) {
                System.err.println("error on " + method + " " + path + ": " + e);
                json(ex, 500, Map.of("code", "error"));
            }
        }
    }

    private void ask(HttpExchange ex, String token, Map<String, String> q) throws IOException, InterruptedException {
        JsonNode b = body(ex);
        AssistantService.Ask ask = ask(token, text(b, "page"), text(b, "name"), text(b, "question"),
                text(b, "suggestionId"), text(b, "conversationId"), b.path("newConversation").asBoolean(false));
        if (!asking.tryAcquire(60, TimeUnit.SECONDS)) {
            json(ex, 503, Map.of("code", "busy"));
            return;
        }
        try {
            String accept = String.valueOf(ex.getRequestHeaders().getFirst("Accept"));
            boolean sse = !"0".equals(q.get("stream")) && accept.contains("text/event-stream");
            if (sse) {
                ex.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
                ex.getResponseHeaders().set("Cache-Control", "no-cache, no-transform");
                ex.getResponseHeaders().set("X-Accel-Buffering", "no"); // nginx: do not buffer
                ex.sendResponseHeaders(200, 0);
                OutputStream out = ex.getResponseBody();
                service.ask(ask, (name, data) -> {
                    try {
                        out.write(("event: " + name + "\ndata: " + JSON.writeValueAsString(data) + "\n\n")
                                .getBytes(StandardCharsets.UTF_8));
                        out.flush();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            } else {
                Map<String, Object> result = new LinkedHashMap<>();
                service.ask(ask, (name, data) -> {
                    if (!"delta".equals(name) && !"reset".equals(name)) {
                        result.put("event", name);
                        result.putAll(data);
                    }
                });
                json(ex, "error".equals(result.get("event")) ? statusOf(String.valueOf(result.get("code"))) : 200,
                        result);
            }
        } finally {
            asking.release();
        }
    }

    private AssistantService.Ask ask(String token, String page, String name, String question, String suggestionId,
            String conversationId, boolean newConversation) {
        String p = page != null && PAGES.contains(page) ? page : ContextText.OTHER;
        String n = name == null ? null : name.replaceAll("[^a-z0-9-]", "");
        return new AssistantService.Ask(token, p, n == null || n.length() > 40 ? null : n, question, suggestionId,
                conversationId, newConversation);
    }

    private void health(HttpExchange ex) throws IOException {
        Settings s = settings.get();
        Map<String, Object> h = new LinkedHashMap<>();
        h.put("openelis", openelis.check());
        h.put("language", s.lang.code);
        h.put("providers", AssistantService.configuredProviders(s).stream()
                .map(p -> Map.of("name", p.name(), "available", p.available())).toList());
        h.put("egress", Egress.allowed());
        json(ex, 200, h);
    }

    // ------------------------------------------------------------------ helpers

    static int statusOf(String code) {
        return switch (code) {
        case "token" -> 401;
        case "quota" -> 429;
        case "not-configured", "busy" -> 503;
        case "empty" -> 400;
        default -> 502;
        };
    }

    private static void cors(HttpExchange ex) {
        var h = ex.getResponseHeaders();
        h.set("Access-Control-Allow-Origin", "*");
        h.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        String asked = ex.getRequestHeaders().getFirst("Access-Control-Request-Headers");
        h.set("Access-Control-Allow-Headers", asked == null ? "Authorization, Content-Type" : asked);
        h.set("Access-Control-Max-Age", "600");
        h.set("Cache-Control", "no-store");
    }

    private static String bearer(HttpExchange ex) {
        String a = ex.getRequestHeaders().getFirst("Authorization");
        return a != null && a.startsWith("Bearer ") ? a.substring(7).trim() : null;
    }

    private static JsonNode body(HttpExchange ex) throws IOException {
        byte[] b = ex.getRequestBody().readNBytes(BODY_MAX + 1);
        if (b.length > BODY_MAX) {
            throw new IllegalArgumentException("body too large");
        }
        return b.length == 0 ? JSON.createObjectNode() : JSON.readTree(b);
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static Map<String, String> query(String raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null) {
            return out;
        }
        for (String part : raw.split("&")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                out.put(URLDecoder.decode(part.substring(0, eq), StandardCharsets.UTF_8),
                        URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }
        return out;
    }

    private static int parse(String s, int fallback) {
        try {
            return s == null ? fallback : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    static void json(HttpExchange ex, int status, Object body) throws IOException {
        byte[] b = JSON.writeValueAsBytes(body);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, b.length);
        ex.getResponseBody().write(b);
    }

    static List<String> pages() {
        return List.copyOf(PAGES);
    }
}
