package org.openelisglobal.module.manifest;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.openelisglobal.module.api.ModuleKind;
import org.openelisglobal.module.api.manifest.BackendSection;
import org.openelisglobal.module.api.manifest.ConfigurationSection;
import org.openelisglobal.module.api.manifest.FrontendSection;
import org.openelisglobal.module.api.manifest.MenuEntry;
import org.openelisglobal.module.api.manifest.ModuleManifest;
import org.openelisglobal.module.api.manifest.PermissionDeclaration;
import org.openelisglobal.module.api.manifest.Requires;
import org.openelisglobal.module.manifest.ManifestValidation.Problem;
import org.openelisglobal.module.version.VersionRanges;

/**
 * Validates a manifest in two steps: the JSON schema shipped with
 * {@code openelis-module-api} ({@code META-INF/openelis-module.schema.json}),
 * then the rules the schema cannot express: the kind matching the declarations,
 * the version ranges, the namespace rules and the cross references between
 * sections (a menu entry's permission must be declared, a route must live under
 * the module's prefix, and so on).
 *
 * <p>
 * Pure: no Spring, no file system beyond the schema resource. The registry
 * calls it at discovery and the module test kit calls it at build time.
 */
public final class ManifestValidator {

    /**
     * The package every module class lives under, as
     * {@code org.openelisglobal.module.<name>}: the segment after the root names
     * the module and must not be one of the sub-packages the core owns.
     */
    public static final String MODULE_PACKAGE_ROOT = "org.openelisglobal.module";

    /** Sub-packages of the module package root that belong to the core. */
    public static final Set<String> RESERVED_MODULE_PACKAGES = Set.of("api", "manifest", "runtime", "web", "testkit");

    /** Extension points the SPI publishes, by manifest name. */
    public static final Set<String> KNOWN_EXTENSION_POINTS = Set.of("report");

    private static final Pattern ID = Pattern.compile(ModuleManifest.ID_PATTERN);
    private static final Pattern PERMISSION_KEY = Pattern.compile("^[a-z][a-zA-Z0-9.-]*$");
    private static final Pattern CLASS_NAME = Pattern
            .compile("^[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*$");
    private static final Pattern PROPERTY_KEY = Pattern.compile("^[a-zA-Z][a-zA-Z0-9.-]*$");

    private final ManifestReader reader;
    private final JsonSchema schema;

    public ManifestValidator() {
        this(new ManifestReader());
    }

    public ManifestValidator(ManifestReader reader) {
        this.reader = reader;
        this.schema = loadSchema();
    }

    private static JsonSchema loadSchema() {
        try (InputStream in = ModuleManifest.class.getResourceAsStream("/META-INF/openelis-module.schema.json")) {
            if (in == null) {
                throw new IllegalStateException(
                        "META-INF/openelis-module.schema.json is missing from openelis-module-api");
            }
            String schemaText = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
            SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
            return factory.getSchema(schemaText, InputFormat.JSON, config);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Validates the manifest text: schema first, then the rules, then returns the
     * bound model and every problem.
     */
    public ManifestValidation validate(String json) {
        List<Problem> problems = new ArrayList<>();
        JsonNode tree;
        try {
            tree = reader.readTree(json);
        } catch (ManifestException e) {
            problems.add(new Problem("", e.getMessage()));
            return new ManifestValidation(null, problems);
        }
        Set<ValidationMessage> messages = schema.validate(tree);
        for (ValidationMessage message : messages) {
            problems.add(new Problem(pointer(message), message.getMessage()));
        }
        if (!problems.isEmpty()) {
            return new ManifestValidation(null, problems);
        }
        ModuleManifest manifest;
        try {
            manifest = reader.bind(tree);
        } catch (ManifestException e) {
            problems.add(new Problem("", e.getMessage()));
            return new ManifestValidation(null, problems);
        }
        problems.addAll(checkRules(manifest));
        return new ManifestValidation(manifest, problems);
    }

    /** The rule checks alone, for a manifest that was bound elsewhere. */
    public List<Problem> checkRules(ModuleManifest manifest) {
        List<Problem> problems = new ArrayList<>();
        checkIdentity(manifest, problems);
        ModuleKind kind = kindOf(manifest);
        checkRequires(manifest, problems);
        Set<String> permissionKeys = checkPermissions(manifest, problems);
        checkBackend(manifest, kind, problems);
        checkMenu(manifest, kind, permissionKeys, problems);
        checkFrontend(manifest, kind, permissionKeys, problems);
        checkConfiguration(manifest, problems);
        return problems;
    }

    private static ModuleKind kindOf(ModuleManifest manifest) {
        try {
            return ModuleKind.fromManifestValue(manifest.getKind());
        } catch (IllegalArgumentException e) {
            return ModuleKind.MODULE;
        }
    }

    private static void checkIdentity(ModuleManifest manifest, List<Problem> problems) {
        if (manifest.getId() == null || !ID.matcher(manifest.getId()).matches()) {
            problems.add(new Problem("/id", "must match " + ModuleManifest.ID_PATTERN));
        }
        if (manifest.getVersion() == null || VersionRanges.coreVersion(manifest.getVersion()).isEmpty()) {
            problems.add(new Problem("/version", "must be a semantic version"));
        }
        if (manifest.getConflicts().contains(manifest.getId())) {
            problems.add(new Problem("/conflicts", "a module cannot conflict with itself"));
        }
    }

    private static void checkRequires(ModuleManifest manifest, List<Problem> problems) {
        Requires requires = manifest.getRequires();
        String openelis = requires.getOpenelis();
        if (openelis == null || openelis.isBlank()) {
            problems.add(new Problem("/requires/openelis",
                    "is mandatory: the minimum OpenELIS version the module was built against, for example '>=3.4.0'"));
        } else if (!VersionRanges.isValidRange(openelis)) {
            problems.add(new Problem("/requires/openelis", "is not a version range: '" + openelis + "'"));
        } else if (!VersionRanges.hasLowerBound(openelis)) {
            problems.add(new Problem("/requires/openelis", "must carry a lower bound, for example '>=3.4.0'"));
        }
        checkOptionalRange("/requires/coreApi", requires.getCoreApi(), problems);
        checkOptionalRange("/requires/spi", requires.getSpi(), problems);
        checkOptionalRange("/requires/frontendContract", requires.getFrontendContract(), problems);
        boolean declaresExtensions = manifest.getBackend() != null && !manifest.getBackend().getExtensions().isEmpty();
        if (declaresExtensions && isBlank(requires.getSpi())) {
            problems.add(new Problem("/requires/spi", "is mandatory when backend.extensions is declared"));
        }
        if (manifest.getFrontend() != null && isBlank(requires.getFrontendContract())) {
            problems.add(new Problem("/requires/frontendContract", "is mandatory when frontend is declared"));
        }
        Set<String> dependencyIds = new HashSet<>();
        for (int i = 0; i < requires.getModules().size(); i++) {
            Requires.ModuleDependency dependency = requires.getModules().get(i);
            String path = "/requires/modules/" + i;
            if (dependency.getId() == null || !ID.matcher(dependency.getId()).matches()) {
                problems.add(new Problem(path + "/id", "must match " + ModuleManifest.ID_PATTERN));
            } else if (dependency.getId().equals(manifest.getId())) {
                problems.add(new Problem(path + "/id", "a module cannot depend on itself"));
            } else if (!dependencyIds.add(dependency.getId())) {
                problems.add(new Problem(path + "/id", "is listed twice"));
            }
            if (!VersionRanges.isValidRange(dependency.getVersion())) {
                problems.add(new Problem(path + "/version", "is not a version range"));
            }
        }
        for (String conflict : manifest.getConflicts()) {
            if (dependencyIds.contains(conflict)) {
                problems.add(new Problem("/conflicts", "'" + conflict + "' is both required and in conflict"));
            }
        }
    }

    private static void checkOptionalRange(String path, String range, List<Problem> problems) {
        if (!isBlank(range) && !VersionRanges.isValidRange(range)) {
            problems.add(new Problem(path, "is not a version range: '" + range + "'"));
        }
    }

    private static Set<String> checkPermissions(ModuleManifest manifest, List<Problem> problems) {
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < manifest.getPermissions().size(); i++) {
            PermissionDeclaration permission = manifest.getPermissions().get(i);
            String path = "/permissions/" + i + "/key";
            String key = permission.getKey();
            if (key == null || !PERMISSION_KEY.matcher(key).matches()) {
                problems.add(new Problem(path, "must match " + PERMISSION_KEY.pattern()));
            } else if (!keys.add(key)) {
                problems.add(new Problem(path, "'" + key + "' is declared twice"));
            }
        }
        return keys;
    }

    /**
     * Null when {@code name} is a package under
     * {@code org.openelisglobal.module.<name>}, otherwise the problem to report.
     */
    public static String namespaceProblem(String name) {
        String prefix = MODULE_PACKAGE_ROOT + ".";
        if (name == null || !name.startsWith(prefix) || name.length() == prefix.length()) {
            return "'" + name + "' is not under " + prefix + "<name>; every module package lives under "
                    + MODULE_PACKAGE_ROOT;
        }
        String segment = name.substring(prefix.length()).split("\\.", 2)[0];
        if (RESERVED_MODULE_PACKAGES.contains(segment)) {
            return "'" + name + "' falls under " + prefix + segment + ", which belongs to the core";
        }
        return null;
    }

    private static void checkClassName(String path, String className, List<Problem> problems) {
        if (className == null || !CLASS_NAME.matcher(className).matches()) {
            problems.add(new Problem(path, "is not a class name"));
            return;
        }
        String problem = namespaceProblem(className);
        if (problem == null && className.lastIndexOf('.') < MODULE_PACKAGE_ROOT.length() + 1) {
            problem = "'" + className + "' sits directly in " + MODULE_PACKAGE_ROOT + "; a module class lives in "
                    + MODULE_PACKAGE_ROOT + ".<name>";
        }
        if (problem != null) {
            problems.add(new Problem(path, problem));
        }
    }

    private static void checkBackend(ModuleManifest manifest, ModuleKind kind, List<Problem> problems) {
        BackendSection backend = manifest.getBackend();
        if (backend == null) {
            return;
        }
        for (int i = 0; i < backend.getBasePackages().size(); i++) {
            String problem = namespaceProblem(backend.getBasePackages().get(i));
            if (problem != null) {
                problems.add(new Problem("/backend/basePackages/" + i, problem));
            }
        }
        if (backend.getActivator() != null) {
            checkClassName("/backend/activator", backend.getActivator(), problems);
        }
        for (int i = 0; i < backend.getEntities().size(); i++) {
            checkClassName("/backend/entities/" + i, backend.getEntities().get(i), problems);
        }
        Set<String> extensionClasses = new HashSet<>();
        for (int i = 0; i < backend.getExtensions().size(); i++) {
            BackendSection.ExtensionDeclaration extension = backend.getExtensions().get(i);
            String path = "/backend/extensions/" + i;
            if (extension.getSpi() == null || !KNOWN_EXTENSION_POINTS.contains(extension.getSpi())) {
                problems.add(new Problem(path + "/spi", "unknown extension point '" + extension.getSpi() + "'; known: "
                        + String.join(", ", new java.util.TreeSet<>(KNOWN_EXTENSION_POINTS))));
            }
            int before = problems.size();
            checkClassName(path + "/class", extension.getClassName(), problems);
            if (problems.size() == before
                    && !extensionClasses.add(extension.getSpi() + ":" + extension.getClassName())) {
                problems.add(new Problem(path + "/class", "is declared twice for the same extension point"));
            }
        }
        if (backend.getLiquibase() != null && kind == ModuleKind.MODULE) {
            String expected = "liquibase/" + manifest.getId() + "/";
            if (!backend.getLiquibase().startsWith(expected)) {
                problems.add(new Problem("/backend/liquibase", "must live under '" + expected
                        + "' (the module class loader is parent-first, so any other path could shadow a core changelog)"));
            }
        }
        if (kind == ModuleKind.PROVIDER) {
            if (!backend.getEntities().isEmpty()) {
                problems.add(new Problem("/backend/entities", "a provider declares no entities; use kind 'module'"));
            }
            if (backend.getLiquibase() != null) {
                problems.add(new Problem("/backend/liquibase", "a provider ships no changelog; use kind 'module'"));
            }
            if (backend.getExtensions().isEmpty()) {
                problems.add(
                        new Problem("/backend/extensions", "a provider must implement at least one extension point"));
            }
        }
    }

    private static void checkMenu(ModuleManifest manifest, ModuleKind kind, Set<String> permissionKeys,
            List<Problem> problems) {
        if (manifest.getMenu().isEmpty()) {
            return;
        }
        if (kind == ModuleKind.PROVIDER) {
            problems.add(new Problem("/menu", "a provider contributes no menu entries; use kind 'module'"));
        }
        String elementPrefix = "menu_mod_" + manifest.getIdAsIdentifier();
        Set<String> elementIds = new HashSet<>();
        for (int i = 0; i < manifest.getMenu().size(); i++) {
            MenuEntry entry = manifest.getMenu().get(i);
            String path = "/menu/" + i;
            String elementId = entry.getElementId();
            if (elementId == null || !(elementId.equals(elementPrefix) || elementId.startsWith(elementPrefix + "_"))) {
                problems.add(new Problem(path + "/elementId", "must start with '" + elementPrefix + "'"));
            } else if (!elementIds.add(elementId)) {
                problems.add(new Problem(path + "/elementId", "'" + elementId + "' is declared twice"));
            }
            if (entry.getDisplayKey() == null || !entry.getDisplayKey().startsWith(manifest.getPermissionPrefix())) {
                problems.add(new Problem(path + "/displayKey",
                        "must be a module message key starting with '" + manifest.getPermissionPrefix() + "'"));
            }
            if (entry.getActionURL() != null && !entry.getActionURL().isEmpty()
                    && !entry.getActionURL().startsWith(manifest.getRoutePrefix())) {
                problems.add(new Problem(path + "/actionURL",
                        "must be one of the module's routes under '" + manifest.getRoutePrefix() + "'"));
            }
            if (entry.getPermission() != null && !permissionKeys.contains(entry.getPermission())) {
                problems.add(new Problem(path + "/permission",
                        "'" + entry.getPermission() + "' is not one of the module's declared permission keys"));
            }
        }
    }

    private static void checkFrontend(ModuleManifest manifest, ModuleKind kind, Set<String> permissionKeys,
            List<Problem> problems) {
        FrontendSection frontend = manifest.getFrontend();
        if (frontend == null) {
            return;
        }
        if (kind == ModuleKind.PROVIDER) {
            problems.add(new Problem("/frontend", "a provider ships no frontend; use kind 'module'"));
        }
        if (isBlank(frontend.getEntry()) || frontend.getEntry().contains("://")
                || frontend.getEntry().startsWith("/")) {
            problems.add(
                    new Problem("/frontend/entry", "must be a file name relative to the module's asset directory"));
        }
        if (isBlank(frontend.getMessages()) || !frontend.getMessages().contains("{locale}")) {
            problems.add(new Problem("/frontend/messages", "must contain the '{locale}' placeholder"));
        }
        Set<String> paths = new HashSet<>();
        for (int i = 0; i < frontend.getRoutes().size(); i++) {
            FrontendSection.RouteDeclaration route = frontend.getRoutes().get(i);
            String path = "/frontend/routes/" + i;
            if (route.getPath() == null || !route.getPath().startsWith(manifest.getRoutePrefix())) {
                problems.add(new Problem(path + "/path", "must start with '" + manifest.getRoutePrefix() + "'"));
            } else if (!paths.add(route.getPath())) {
                problems.add(new Problem(path + "/path", "'" + route.getPath() + "' is declared twice"));
            }
            if (isBlank(route.getPermission()) && isBlank(route.getRole())) {
                problems.add(new Problem(path, "needs a permission or a role; there are no anonymous module pages"));
            }
            if (!isBlank(route.getPermission()) && !permissionKeys.contains(route.getPermission())) {
                problems.add(new Problem(path + "/permission",
                        "'" + route.getPermission() + "' is not one of the module's declared permission keys"));
            }
        }
        for (int i = 0; i < frontend.getSlots().size(); i++) {
            FrontendSection.SlotDeclaration slot = frontend.getSlots().get(i);
            if (!isBlank(slot.getPermission()) && !permissionKeys.contains(slot.getPermission())) {
                problems.add(new Problem("/frontend/slots/" + i + "/permission",
                        "'" + slot.getPermission() + "' is not one of the module's declared permission keys"));
            }
        }
    }

    private static void checkConfiguration(ModuleManifest manifest, List<Problem> problems) {
        ConfigurationSection configuration = manifest.getConfiguration();
        if (configuration == null) {
            return;
        }
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < configuration.getProperties().size(); i++) {
            ConfigurationSection.PropertyDeclaration property = configuration.getProperties().get(i);
            String path = "/configuration/properties/" + i;
            if (property.getKey() == null || !PROPERTY_KEY.matcher(property.getKey()).matches()) {
                problems.add(new Problem(path + "/key", "must match " + PROPERTY_KEY.pattern()));
            } else if (!keys.add(property.getKey())) {
                problems.add(new Problem(path + "/key", "'" + property.getKey() + "' is declared twice"));
            }
            String defaultValue = property.getDefaultValue();
            if (defaultValue != null && !defaultMatchesType(property.getType(), defaultValue)) {
                problems.add(
                        new Problem(path + "/default", "'" + defaultValue + "' is not a valid " + property.getType()));
            }
        }
    }

    static boolean defaultMatchesType(String type, String value) {
        switch (type == null ? "string" : type) {
        case "integer":
            try {
                Integer.parseInt(value);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        case "boolean":
            return value.equals("true") || value.equals("false");
        case "url":
            return value.startsWith("http://") || value.startsWith("https://");
        default:
            return true;
        }
    }

    private static String pointer(ValidationMessage message) {
        String location = message.getInstanceLocation() == null ? "" : message.getInstanceLocation().toString();
        return location.equals("$") ? "" : location.replace("$.", "/").replace('.', '/');
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
