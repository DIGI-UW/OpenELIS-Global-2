package org.openelisglobal.module.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.module.api.manifest.ModuleManifest;

/**
 * The message bundles a module ships for its frontend, read once from the
 * module JAR: every file that matches the manifest's {@code frontend.messages}
 * pattern under the module's asset root, keyed by the locale the file name
 * carries and holding only the module's own keys ({@code mod.<id>.*}). The
 * shell receives them with the registry and merges them into its messages, so
 * menu labels, pages and slot contributions translate on their first render
 * without a request per module and locale.
 */
public final class ModuleMessages {

    private static final String LOG = "ModuleMessages";
    private static final String ASSET_ROOT = "META-INF/resources/modules/";
    private static final String LOCALE = "{locale}";

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Map<String, Map<String, String>>> cache = new ConcurrentHashMap<>();

    /** Locale code to messages; empty when the module ships no bundle. */
    public Map<String, Map<String, String>> bundles(ModuleRecord record) {
        return cache.computeIfAbsent(record.id() + "@" + record.sha256(), key -> read(record));
    }

    private Map<String, Map<String, String>> read(ModuleRecord record) {
        ModuleManifest manifest = record.manifest();
        if (manifest == null || manifest.getFrontend() == null || record.jarPath() == null) {
            return Map.of();
        }
        String pattern = manifest.getFrontend().getMessages();
        int at = pattern == null ? -1 : pattern.indexOf(LOCALE);
        if (at < 0) {
            return Map.of();
        }
        String prefix = ASSET_ROOT + manifest.getId() + "/" + pattern.substring(0, at);
        String suffix = pattern.substring(at + LOCALE.length());
        String keyPrefix = manifest.getPermissionPrefix();
        Map<String, Map<String, String>> bundles = new TreeMap<>();
        try (JarFile jar = new JarFile(record.jarPath().toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith(prefix) || !name.endsWith(suffix)) {
                    continue;
                }
                String locale = name.substring(prefix.length(), name.length() - suffix.length());
                if (locale.isEmpty() || locale.contains("/")) {
                    continue;
                }
                try (InputStream in = jar.getInputStream(entry)) {
                    bundles.put(locale, ownKeys(mapper.readTree(in), keyPrefix));
                }
            }
        } catch (IOException e) {
            LogEvent.logWarn(LOG, "read",
                    "[module:" + record.id() + "] message bundles could not be read: " + e.getMessage());
            return Map.of();
        }
        return bundles;
    }

    private static Map<String, String> ownKeys(JsonNode tree, String keyPrefix) {
        Map<String, String> messages = new TreeMap<>();
        tree.fields().forEachRemaining(field -> {
            if (field.getKey().startsWith(keyPrefix) && field.getValue().isTextual()) {
                messages.put(field.getKey(), field.getValue().asText());
            }
        });
        return messages;
    }
}
