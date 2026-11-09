package org.openelisglobal.module.web;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Optional;
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
 * Serves a started module's prebuilt frontend from its JAR: {@code GET
 * /modules/{id}/<path>} reads {@code META-INF/resources/modules/<id>/<path>}
 * through the module's class loader. Hashed chunks are immutable for a year;
 * the remote entry and the message files are revalidated on every load.
 * Requires the authenticated session like every other page; excluded from the
 * URL interceptor like the core's static files.
 */
@RestController
public class ModuleAssetsController {

    private static final Map<String, MediaType> TYPES = Map.of("js", MediaType.valueOf("text/javascript"), "mjs",
            MediaType.valueOf("text/javascript"), "css", MediaType.valueOf("text/css"), "json",
            MediaType.APPLICATION_JSON, "map", MediaType.APPLICATION_JSON, "svg", MediaType.valueOf("image/svg+xml"),
            "png", MediaType.IMAGE_PNG, "woff2", MediaType.valueOf("font/woff2"), "woff",
            MediaType.valueOf("font/woff"), "txt", MediaType.TEXT_PLAIN);

    private final ModuleRegistry registry;

    public ModuleAssetsController(ModuleRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/modules/{id}/**")
    public ResponseEntity<byte[]> asset(@PathVariable("id") String id, HttpServletRequest request) throws IOException {
        Optional<ModuleRecord> module = registry.find(id).filter(ModuleRecord::isActive);
        if (module.isEmpty() || module.get().manifest().getFrontend() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        String path = assetPath(request, id);
        if (path == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        URL resource = module.get().classLoader().findResource("META-INF/resources/modules/" + id + "/" + path);
        if (resource == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        URLConnection connection = resource.openConnection();
        connection.setUseCaches(false);
        byte[] bytes;
        try (InputStream in = connection.getInputStream()) {
            bytes = in.readAllBytes();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType(path));
        headers.setCacheControl(isImmutable(path) ? CacheControl.maxAge(java.time.Duration.ofDays(365)).immutable()
                : CacheControl.noCache());
        headers.setETag(
                "\"" + module.get().sha256().substring(0, 16) + "-" + Integer.toHexString(path.hashCode()) + "\"");
        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
        if (ifNoneMatch != null && ifNoneMatch.equals(headers.getETag())) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).headers(headers).build();
        }
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    /**
     * The path below {@code /modules/{id}/}, refused when it tries to leave the
     * asset directory.
     */
    static String assetPath(HttpServletRequest request, String id) {
        String full = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        if (full == null) {
            full = request.getRequestURI().substring(request.getContextPath().length());
        }
        String prefix = "/modules/" + id + "/";
        if (!full.startsWith(prefix)) {
            return null;
        }
        String path = full.substring(prefix.length());
        if (path.isEmpty() || path.contains("..") || path.startsWith("/") || path.contains("//")) {
            return null;
        }
        return path;
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
}
