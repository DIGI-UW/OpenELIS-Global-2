package org.openelisglobal.addons.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Outbound connections to model providers. The service only connects to hosts
 * its own manifest declares in {@code network.egress} (the list the
 * administrator consented to), even if the configuration names another
 * endpoint; the operator's network policy enforces the same list from outside.
 * {@code HTTPS_PROXY} (e.g. an allowlisting proxy) is honoured.
 */
public final class Egress {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static volatile List<String> allowed = load();

    private Egress() {
    }

    private static List<String> load() {
        List<String> out = new ArrayList<>();
        try (InputStream in = Egress.class.getResourceAsStream("/addon.json")) {
            if (in != null) {
                JsonNode m = JSON.readTree(in);
                m.path("network").path("egress").forEach(h -> out.add(h.asText().toLowerCase(Locale.ROOT)));
            }
        } catch (Exception e) {
            // no manifest: nothing allowed
        }
        return out;
    }

    /** Tests. */
    static void allow(List<String> hosts) {
        allowed = List.copyOf(hosts);
    }

    public static List<String> allowed() {
        return allowed;
    }

    /**
     * True if {@code url} is http(s) to a declared host: "host" allows any port,
     * "host:port" only that port.
     */
    public static boolean permits(String url) {
        URI u;
        try {
            u = URI.create(url);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (u.getHost() == null || !("https".equals(u.getScheme()) || "http".equals(u.getScheme()))) {
            return false;
        }
        String host = u.getHost().toLowerCase(Locale.ROOT);
        int port = u.getPort() > 0 ? u.getPort() : "https".equals(u.getScheme()) ? 443 : 80;
        for (String a : allowed) {
            if (a.equals(host) || a.equals(host + ":" + port)) {
                return true;
            }
        }
        return false;
    }

    /** A client for model providers: through HTTPS_PROXY/HTTP_PROXY when set. */
    public static HttpClient client(int connectSeconds) {
        HttpClient.Builder b = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(connectSeconds));
        String proxy = firstNonBlank(System.getenv("HTTPS_PROXY"), System.getenv("https_proxy"),
                System.getenv("HTTP_PROXY"), System.getenv("http_proxy"));
        if (proxy != null) {
            URI p = URI.create(proxy.contains("://") ? proxy : "http://" + proxy);
            b.proxy(ProxySelector.of(new InetSocketAddress(p.getHost(), p.getPort() > 0 ? p.getPort() : 3128)));
        }
        return b.build();
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }
}
