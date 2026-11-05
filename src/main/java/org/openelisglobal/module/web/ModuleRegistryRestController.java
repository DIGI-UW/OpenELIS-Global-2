package org.openelisglobal.module.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> list() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("openelisVersion", registry.versions().openelis());
        body.put("coreApiVersion", registry.versions().coreApi());
        body.put("spiVersion", registry.versions().spi());
        body.put("frontendContractVersion", ModuleFrontendContract.VERSION);
        List<Map<String, Object>> modules = new ArrayList<>();
        for (ModuleRecord record : registry.all()) {
            modules.add(summary(record));
        }
        body.put("modules", modules);
        body.put("extensionPoints", extensionPoints());
        return body;
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('GLOBAL_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable("id") String id) {
        return registry.find(id).map(record -> {
            Map<String, Object> body = summary(record);
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

    private Map<String, Object> summary(ModuleRecord record) {
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
                body.put("assetBase", "/modules/" + record.id() + "/");
                body.put("restPrefix", manifest.getRestPrefix());
                body.put("routePrefix", manifest.getRoutePrefix());
                body.put("remoteName", manifest.getIdAsIdentifier());
                body.put("messages", messages.bundles(record));
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
