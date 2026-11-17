package org.openelisglobal.module.web;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.openelisglobal.module.runtime.ModuleRecord;
import org.openelisglobal.module.runtime.ModuleRegistry;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Serves a started module's prebuilt frontend from its JAR, read through the
 * module's class loader from {@code META-INF/resources/modules/<id>/<path>}.
 *
 * <p>
 * The registry publishes the module's files under
 * {@code /modules/<id>/<sha16>/}, the first 16 hex digits of the running JAR's
 * digest: every file there, the remote entry and the message files included, is
 * immutable for a year, a new JAR is a new URL, and a request for any other
 * digest is a 404 so an old URL can never cache new bytes. The plain form
 * {@code /modules/<id>/<path>} stays for one release, revalidated on every load
 * for the entry and the message files. Both forms carry a weak ETag that names
 * the digest (weak so the connector's compression applies), a revalidation is
 * answered from it before the JAR is read, and a file is read once per digest
 * and kept in memory. Requires the authenticated session like every other page;
 * excluded from the URL interceptor like the core's static files.
 */
@RestController
public class ModuleAssetsController {

    /**
     * One file of one module as requested: the digest segment, when the URL carries
     * one, and the path below it.
     */
    record AssetRequest(String digest, String path) {
    }

    private static final java.util.regex.Pattern DIGEST_SEGMENT = java.util.regex.Pattern.compile("[0-9a-f]{16}");

    private static final Map<String, MediaType> TYPES = Map.of("js", MediaType.valueOf("text/javascript"), "mjs",
            MediaType.valueOf("text/javascript"), "css", MediaType.valueOf("text/css"), "json",
            MediaType.APPLICATION_JSON, "map", MediaType.APPLICATION_JSON, "svg", MediaType.valueOf("image/svg+xml"),
            "png", MediaType.IMAGE_PNG, "woff2", MediaType.valueOf("font/woff2"), "woff",
            MediaType.valueOf("font/woff"), "txt", MediaType.TEXT_PLAIN);

    private final ModuleRegistry registry;
    private final Map<String, byte[]> bytesByDigestAndPath = new ConcurrentHashMap<>();

    public ModuleAssetsController(ModuleRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/modules/{id}/**")
    public ResponseEntity<byte[]> asset(@PathVariable("id") String id, HttpServletRequest request) throws IOException {
        Optional<ModuleRecord> module = registry.find(id).filter(ModuleRecord::isActive);
        if (module.isEmpty() || module.get().manifest().getFrontend() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        AssetRequest asset = parse(requestedPath(request), id);
        if (asset == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        String digest = module.get().sha256().substring(0, 16);
        if (asset.digest() != null && !asset.digest().equals(digest)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        String path = asset.path();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType(path));
        CacheControl forAYear = CacheControl.maxAge(java.time.Duration.ofDays(365)).cachePublic().immutable();
        headers.setCacheControl(asset.digest() != null ? forAYear
                : isImmutable(path) ? CacheControl.maxAge(java.time.Duration.ofDays(365)).immutable()
                        : CacheControl.noCache());
        headers.setETag(etag(digest, path));
        if (matches(request.getHeader(HttpHeaders.IF_NONE_MATCH), headers.getETag())) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).headers(headers).build();
        }
        byte[] bytes = bytesByDigestAndPath.get(digest + "/" + path);
        if (bytes == null) {
            URL resource = module.get().classLoader().findResource("META-INF/resources/modules/" + id + "/" + path);
            if (resource == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            URLConnection connection = resource.openConnection();
            connection.setUseCaches(false);
            try (InputStream in = connection.getInputStream()) {
                bytes = in.readAllBytes();
            }
            bytesByDigestAndPath.put(digest + "/" + path, bytes);
        }
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    static String requestedPath(HttpServletRequest request) {
        String full = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        return full != null ? full : request.getRequestURI().substring(request.getContextPath().length());
    }

    /**
     * The path below {@code /modules/{id}/} (digest segment stripped), or null when
     * the URL is refused.
     */
    static String assetPath(HttpServletRequest request, String id) {
        AssetRequest asset = parse(requestedPath(request), id);
        return asset == null ? null : asset.path();
    }

    /**
     * Splits {@code /modules/<id>/[<sha16>/]<path>} into the digest segment, when
     * the first element below the module is one, and the file path, refused when
     * the path is empty or tries to leave the asset directory.
     */
    static AssetRequest parse(String full, String id) {
        String prefix = "/modules/" + id + "/";
        if (full == null || !full.startsWith(prefix)) {
            return null;
        }
        String path = full.substring(prefix.length());
        String digest = null;
        int slash = path.indexOf('/');
        if (slash == 16 && DIGEST_SEGMENT.matcher(path.substring(0, 16)).matches()) {
            digest = path.substring(0, 16);
            path = path.substring(17);
        } else if (slash < 0 && DIGEST_SEGMENT.matcher(path).matches()) {
            return null;
        }
        if (path.isEmpty() || path.contains("..") || path.startsWith("/") || path.contains("//")) {
            return null;
        }
        return new AssetRequest(digest, path);
    }

    static MediaType mediaType(String path) {
        int dot = path.lastIndexOf('.');
        String extension = dot < 0 ? "" : path.substring(dot + 1).toLowerCase();
        return TYPES.getOrDefault(extension, MediaType.APPLICATION_OCTET_STREAM);
    }

    /**
     * Vite names chunks {@code name-<hash>.js}; the entry and the message files
     * keep stable names.
     */
    static boolean isImmutable(String path) {
        String file = path.substring(path.lastIndexOf('/') + 1);
        return (path.startsWith("assets/") || path.startsWith("__federation"))
                && file.matches(".*-[0-9A-Za-z_]{6,}\\.[a-z0-9]+$");
    }

    /** A weak validator for one file of one JAR build. */
    static String etag(String digest, String path) {
        return "W/\"" + digest + "-" + Integer.toHexString(path.hashCode()) + "\"";
    }

    /**
     * Whether an {@code If-None-Match} header names this ETag: the weak form the
     * controller sends, or the strong form a proxy may have reduced it to.
     */
    static boolean matches(String ifNoneMatch, String etag) {
        if (ifNoneMatch == null) {
            return false;
        }
        for (String candidate : ifNoneMatch.split(",")) {
            String value = candidate.trim();
            if (value.equals("*") || value.equals(etag) || ("W/" + value).equals(etag)) {
                return true;
            }
        }
        return false;
    }
}
