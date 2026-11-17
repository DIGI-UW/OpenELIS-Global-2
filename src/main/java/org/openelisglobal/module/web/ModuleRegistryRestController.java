package org.openelisglobal.module.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.module.api.ModuleState;
import org.openelisglobal.module.api.manifest.ModuleManifest;
import org.openelisglobal.module.runtime.ExtensionPoints;
import org.openelisglobal.module.runtime.ExtensionRegistry;
import org.openelisglobal.module.runtime.ModuleMessages;
import org.openelisglobal.module.runtime.ModuleRecord;
import org.openelisglobal.module.runtime.ModuleRegistry;
import org.openelisglobal.module.runtime.spring.ModuleConfigurationImpl;
import org.openelisglobal.module.runtime.spring.ModuleContextFactory;
import org.openelisglobal.module.runtime.spring.ModuleContextImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The registry as the application shell and the administrator see it. Lives
 * under {@code /rest/module-registry} (not {@code /rest/modules}) so that no
 * module mapping can shadow it.
 *
 * <ul>
 * <li>{@code GET /rest/module-registry}: every discovered module with id, kind,
 * version, state, reason, and for a started module its frontend declaration,
 * menu, permissions and exposed configuration; any authenticated user, because
 * the shell needs it after login.</li>
 * <li>{@code GET /rest/module-registry/{id}}: one module with the operator's
 * details (JAR, digest, bundled and shadowed libraries, warnings);
 * administrators.</li>
 * <li>{@code GET /rest/module-registry/{id}/config}: the configuration values
 * the manifest exposes to the module's frontend; any authenticated user.</li>
 * </ul>
 */
@RestController
@RequestMapping("/rest/module-registry")
public class ModuleRegistryRestController {

    private final ModuleRegistry registry;
    private final ModuleContextFactory factory;
    private final ModuleMessages messages = new ModuleMessages();

    public ModuleRegistryRestController(ModuleRegistry registry, ModuleContextFactory factory) {
        this.registry = registry;
        this.factory = factory;
    }

    /**
     * The registry for one locale: each started module carries the message bundles
     * of {@code locale} (the shell's active locale), else of the request's
     * {@code Accept-Language}, and of their fallbacks (the base language, then
     * English), not every bundle the module ships; {@code locales} names those.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> list(@RequestParam(value = "locale", required = false) String locale,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        List<String> wanted = localeChain(locale, acceptLanguage);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("openelisVersion", registry.versions().openelis());
        body.put("coreApiVersion", registry.versions().coreApi());
        body.put("spiVersion", registry.versions().spi());
        body.put("frontendContractVersion", ModuleFrontendContract.VERSION);
        body.put("locale", wanted.get(0));
        List<Map<String, Object>> modules = new ArrayList<>();
        for (ModuleRecord record : registry.all()) {
            modules.add(summary(record, wanted));
        }
        body.put("modules", modules);
        body.put("extensionPoints", extensionPoints());
        return body;
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('GLOBAL_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable("id") String id,
            @RequestParam(value = "locale", required = false) String locale,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        return registry.find(id).map(record -> {
            Map<String, Object> body = summary(record, localeChain(locale, acceptLanguage));
            body.put("jar", record.jarPath().getFileName().toString());
            body.put("sha256", record.sha256());
            body.put("bundledLibraries", record.bundledLibraries());
            List<Map<String, String>> shadowed = new ArrayList<>();
            for (ModuleRecord.ShadowedLibrary library : record.shadowedLibraries()) {
                shadowed.add(Map.of("bundled", library.bundled(), "coreCopy", library.coreCopy()));
            }
            body.put("shadowedLibraries", shadowed);
            body.put("warnings", record.warnings());
            body.put("recordedVersion", record.recordedVersion());
            body.put("manifest", record.manifest());
            return ResponseEntity.ok(body);
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping(value = "/{id}/config", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> config(@PathVariable("id") String id) {
        ModuleContextImpl context = factory.moduleContext(id);
        if (context == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(((ModuleConfigurationImpl) context.configuration()).exposedToFrontend());
    }

    /**
     * The locales whose bundles a response carries, most specific first: the
     * requested tag (both its {@code -} and {@code _} spellings), its base
     * language, then English. The request's parameter wins over the
     * {@code Accept-Language} header's first language; neither means English.
     */
    static List<String> localeChain(String locale, String acceptLanguage) {
        String chosen = locale == null ? "" : locale.trim();
        if (chosen.isEmpty() && acceptLanguage != null) {
            chosen = acceptLanguage.split(",")[0].split(";")[0].trim();
        }
        if (chosen.isEmpty() || "*".equals(chosen)) {
            chosen = "en";
        }
        // Bundle files are named like the locales the shell sends, "fr" and "fr-MG":
        // a hand-written "FR" or "fr-mg" finds the same files
        String[] parts = chosen.replace('_', '-').split("-");
        parts[0] = parts[0].toLowerCase();
        for (int i = 1; i < parts.length; i++) {
            parts[i] = parts[i].toUpperCase();
        }
        String tag = String.join("-", parts);
        Set<String> chain = new LinkedHashSet<>();
        chain.add(tag);
        chain.add(tag.replace('-', '_'));
        chain.add(tag.split("-")[0]);
        chain.add("en");
        return new ArrayList<>(chain);
    }

    private Map<String, Object> summary(ModuleRecord record, List<String> wanted) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", record.id());
        body.put("name", record.name());
        body.put("version", record.version());
        body.put("kind", record.kind().manifestValue());
        body.put("state", record.state().name());
        body.put("reason", record.reason().orElse(null));
        ModuleManifest manifest = record.manifest();
        if (manifest != null) {
            body.put("description", manifest.getDescription());
            body.put("vendor", manifest.getVendor());
            body.put("requires", manifest.getRequires());
            if (record.state() == ModuleState.STARTED) {
                body.put("permissions", manifest.getPermissions());
                body.put("menu", manifest.getMenu());
                body.put("frontend", manifest.getFrontend());
                // The running JAR's digest in the path: every file under it is immutable, a new
                // JAR is a new URL
                body.put("assetBase", "/modules/" + record.id() + "/" + record.sha256().substring(0, 16) + "/");
                body.put("restPrefix", manifest.getRestPrefix());
                body.put("routePrefix", manifest.getRoutePrefix());
                body.put("remoteName", manifest.getIdAsIdentifier());
                Map<String, Map<String, String>> bundles = messages.bundles(record);
                Map<String, Map<String, String>> carried = new LinkedHashMap<>();
                for (String tag : wanted) {
                    if (bundles.containsKey(tag)) {
                        carried.put(tag, bundles.get(tag));
                    }
                }
                body.put("messages", carried);
                body.put("locales", new ArrayList<>(bundles.keySet()));
            }
        }
        return body;
    }

    private List<Map<String, Object>> extensionPoints() {
        List<Map<String, Object>> points = new ArrayList<>();
        ExtensionRegistry extensions = registry.extensions();
        for (String name : ExtensionPoints.names()) {
            ExtensionPoints.Definition definition = ExtensionPoints.definition(name);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("name", name);
            point.put("cardinality", definition.cardinality().name().toLowerCase());
            List<String> providers = new ArrayList<>();
            for (ExtensionRegistry.Registration<?> registration : extensions.registrations(definition.type())) {
                providers.add(registration.moduleId());
            }
            point.put("providers", providers);
            points.add(point);
        }
        return points;
    }
}
