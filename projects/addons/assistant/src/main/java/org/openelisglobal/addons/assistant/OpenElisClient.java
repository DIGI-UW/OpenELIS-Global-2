package org.openelisglobal.addons.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * The OpenELIS add-on API ({@code OPENELIS_API_URL}), reached from inside the
 * compose network. With the frame's UI token: {@code ai/context} and
 * {@code ai/deidentify}; with the service key ({@code OPENELIS_API_KEY}): the
 * configuration. OpenELIS's backend usually has a self-signed certificate: give
 * its CA in {@code OPENELIS_CA_FILE} (PEM); without it the certificate is not
 * checked (a warning is printed), as in the example add-on. The host name is
 * always checked, so {@code OPENELIS_API_URL} uses the name in the certificate
 * ({@code oe.openelis.org} in the standard compose file).
 */
public class OpenElisClient implements AssistantService.ContextSource {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final long CONFIG_TTL_MS = 30_000;

    private final String base;
    private final String apiKey;
    private final HttpClient http;
    private volatile Map<String, Object> config;
    private volatile long configAt;

    public OpenElisClient(String base, String apiKey, String caFile) {
        this.base = base.replaceAll("/+$", "");
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.http = client(caFile);
    }

    // ------------------------------------------------------------------ the frame's token

    @Override
    public ContextText.Built build(Request req, Lang lang) throws AssistantService.ContextException {
        String view = ContextText.LOOKUP.equals(req.page()) ? "patient" : "cases";
        JsonNode ctx = get("/ai/context?view=" + view, req.token());
        return ContextText.build(ctx, req.page(), req.pageName(), lang);
    }

    @Override
    public String deidentify(Request req, String text) throws AssistantService.ContextException {
        try {
            String body = JSON.writeValueAsString(Map.of("text", text));
            JsonNode r = send(HttpRequest.newBuilder(URI.create(base + "/ai/deidentify"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)), req.token());
            return r.path("text").asText(text);
        } catch (IOException e) {
            throw new AssistantService.ContextException("openelis", e.getMessage());
        }
    }

    private JsonNode get(String path, String token) throws AssistantService.ContextException {
        return send(HttpRequest.newBuilder(URI.create(base + path)).GET(), token);
    }

    private JsonNode send(HttpRequest.Builder b, String credential) throws AssistantService.ContextException {
        if (credential == null || credential.isBlank() || credential.length() > 4096) {
            throw new AssistantService.ContextException("token", "no token");
        }
        HttpResponse<InputStream> res;
        try {
            res = http.send(b.timeout(Duration.ofSeconds(15)).header("Authorization", "Bearer " + credential)
                    .header("Accept", "application/json").build(), HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            throw new AssistantService.ContextException("openelis", "OpenELIS unreachable");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssistantService.ContextException("openelis", "interrupted");
        }
        try (InputStream in = res.body()) {
            if (res.statusCode() == 401 || res.statusCode() == 403) {
                throw new AssistantService.ContextException("token", "OpenELIS refused the token");
            }
            if (res.statusCode() / 100 != 2) {
                throw new AssistantService.ContextException("openelis", "OpenELIS HTTP " + res.statusCode());
            }
            return JSON.readTree(in);
        } catch (IOException e) {
            throw new AssistantService.ContextException("openelis", "bad answer from OpenELIS");
        }
    }

    // ------------------------------------------------------------------ configuration (service key)

    /** The configuration from the Extensions page, cached {@value #CONFIG_TTL_MS} ms; the last one if unreachable. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> config() {
        long now = System.currentTimeMillis();
        Map<String, Object> c = config;
        if (c != null && now - configAt < CONFIG_TTL_MS) {
            return c;
        }
        try {
            c = JSON.convertValue(get("/config", apiKey), Map.class);
            config = c;
            configAt = now;
            return c;
        } catch (AssistantService.ContextException | IllegalArgumentException e) {
            return c == null ? Map.of() : c;
        }
    }

    /** "ok", or why the service key does not work (shown by /health). */
    public String check() {
        try {
            JsonNode w = get("/whoami", apiKey);
            return "service".equals(w.path("kind").asText()) ? "ok" : "unexpected credential";
        } catch (AssistantService.ContextException e) {
            return e.code + ": " + e.getMessage();
        }
    }

    static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ TLS

    private static HttpClient client(String caFile) {
        HttpClient.Builder b = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .proxy(HttpClient.Builder.NO_PROXY);
        try {
            if (caFile != null && !caFile.isBlank()) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                ks.load(null, null);
                int i = 0;
                try (InputStream in = Files.newInputStream(Path.of(caFile))) {
                    for (var cert : cf.generateCertificates(in)) {
                        ks.setCertificateEntry("ca" + i++, cert);
                    }
                }
                TrustManagerFactory tmf = TrustManagerFactory
                        .getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ks);
                SSLContext ssl = SSLContext.getInstance("TLS");
                ssl.init(null, tmf.getTrustManagers(), new SecureRandom());
                b.sslContext(ssl);
            } else {
                // the host name is still checked: use the name in the backend's certificate (oe.openelis.org)
                System.err.println("WARNING: OPENELIS_CA_FILE not set, not verifying who signed the OpenELIS certificate");
                SSLContext ssl = SSLContext.getInstance("TLS");
                ssl.init(null, new TrustManager[] { new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] c, String a) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] c, String a) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                } }, new SecureRandom());
                b.sslContext(ssl);
            }
        } catch (Exception e) {
            throw new IllegalStateException("cannot set up TLS for OpenELIS: " + e.getMessage(), e);
        }
        return b.build();
    }
}
