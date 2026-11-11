package org.openelisglobal.module.manifest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.junit.Test;
import org.openelisglobal.module.api.manifest.ModuleManifest;
import org.openelisglobal.module.manifest.ManifestValidation.Problem;

public class ManifestValidatorTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private final ManifestValidator validator = new ManifestValidator();

    @Test
    public void fullModuleManifestIsValidAndBound() {
        ManifestValidation validation = validator.validate(fixture("valid-module.json"));

        assertTrue(validation.describe(), validation.isValid());
        ModuleManifest manifest = validation.manifest();
        assertEquals("example-workflow", manifest.getId());
        assertEquals("example_workflow", manifest.getIdAsIdentifier());
        assertEquals("mod.example-workflow.", manifest.getPermissionPrefix());
        assertEquals("/rest/modules/example-workflow/", manifest.getRestPrefix());
        assertEquals("/m/example-workflow/", manifest.getRoutePrefix());
        assertEquals("mod_example_workflow_", manifest.getTablePrefix());
        assertEquals("org.openelisglobal.module.workflow.report.CaseSummaryReportProvider",
                manifest.getBackend().getExtensions().get(0).getClassName());
        assertEquals(10, manifest.getBackend().getExtensions().get(0).getOrder());
        assertEquals("5", manifest.getConfiguration().getProperties().get(0).getDefaultValue());
        assertNull(manifest.getConfiguration().getProperties().get(1).getDefaultValue());
        assertEquals(2, manifest.getFrontend().getRoutes().size());
        assertEquals("Results", manifest.getPermissions().get(0).getDefaultRoles().get(0));
    }

    @Test
    public void providerManifestIsValid() {
        ManifestValidation validation = validator.validate(fixture("valid-provider.json"));

        assertTrue(validation.describe(), validation.isValid());
        assertEquals("provider", validation.manifest().getKind());
        assertNull(validation.manifest().getFrontend());
    }

    @Test
    public void manifestThatIsNotJsonIsOneProblem() {
        ManifestValidation validation = validator.validate("{ not json");

        assertFalse(validation.isValid());
        assertNull(validation.manifest());
        assertEquals(1, validation.problems().size());
        assertTrue(validation.describe(), validation.describe().contains("not valid JSON"));
    }

    @Test
    public void schemaRejectsMissingRequiredFieldsAndUnknownKeys() {
        ManifestValidation validation = validator.validate(edited("valid-module.json", root -> {
            root.remove("name");
            root.put("surprise", true);
        }));

        assertFalse(validation.isValid());
        assertNull(validation.manifest());
        assertTrue(validation.describe(), hasProblemMentioning(validation, "name"));
        assertTrue(validation.describe(), hasProblemMentioning(validation, "surprise"));
    }

    @Test
    public void openElisMinimumVersionIsMandatoryWithALowerBound() {
        ManifestValidation missing = validator.validate(
                edited("valid-provider.json", root -> ((ObjectNode) root.get("requires")).remove("openelis")));
        assertFalse(missing.isValid());
        assertTrue(missing.describe(), hasProblemMentioning(missing, "openelis"));

        ManifestValidation unbounded = validator.validate(
                edited("valid-provider.json", root -> ((ObjectNode) root.get("requires")).put("openelis", "<4")));
        assertFalse(unbounded.isValid());
        assertTrue(unbounded.describe(), hasProblemAt(unbounded, "/requires/openelis"));

        ManifestValidation garbage = validator.validate(
                edited("valid-provider.json", root -> ((ObjectNode) root.get("requires")).put("openelis", "soon")));
        assertFalse(garbage.isValid());
        assertTrue(garbage.describe(), hasProblemAt(garbage, "/requires/openelis"));
    }

    @Test
    public void spiRangeIsMandatoryWhenExtensionsAreDeclared() {
        ManifestValidation validation = validator
                .validate(edited("valid-provider.json", root -> ((ObjectNode) root.get("requires")).remove("spi")));

        assertFalse(validation.isValid());
        assertTrue(validation.describe(), hasProblemAt(validation, "/requires/spi"));
    }

    @Test
    public void frontendContractRangeIsMandatoryWhenAFrontendIsDeclared() {
        ManifestValidation validation = validator.validate(
                edited("valid-module.json", root -> ((ObjectNode) root.get("requires")).remove("frontendContract")));

        assertFalse(validation.isValid());
        assertTrue(validation.describe(), hasProblemAt(validation, "/requires/frontendContract"));
    }

    @Test
    public void providerMayNotDeclareEntitiesChangelogMenuOrFrontend() {
        ManifestValidation validation = validator
                .validate(edited("valid-module.json", root -> root.put("kind", "provider")));

        assertFalse(validation.isValid());
        assertTrue(validation.describe(), hasProblemAt(validation, "/backend/entities"));
        assertTrue(validation.describe(), hasProblemAt(validation, "/backend/liquibase"));
        assertTrue(validation.describe(), hasProblemAt(validation, "/menu"));
        assertTrue(validation.describe(), hasProblemAt(validation, "/frontend"));
    }

    @Test
    public void providerMustImplementAnExtensionPoint() {
        ManifestValidation validation = validator.validate(
                edited("valid-provider.json", root -> ((ObjectNode) root.get("backend")).remove("extensions")));

        assertFalse(validation.isValid());
        assertTrue(validation.describe(), hasProblemAt(validation, "/backend/extensions"));
    }

    @Test
    public void unknownExtensionPointIsReported() {
        ManifestValidation validation = validator.validate(edited("valid-provider.json", root -> {
            ArrayNode extensions = (ArrayNode) root.get("backend").get("extensions");
            ((ObjectNode) extensions.get(0)).put("spi", "billing-but-not-yet");
        }));

        assertFalse(validation.isValid());
        assertTrue(validation.describe(), hasProblemAt(validation, "/backend/extensions/0/spi"));
    }

    @Test
    public void basePackagesOutsideTheModuleRootAreRefused() {
        for (String pkg : new String[] { "org.example.openelis.reports", "org.openelisglobal.sample",
                "org.openelisglobal.module", "org.openelisglobal.modules.sitereports" }) {
            ManifestValidation validation = validator.validate(edited("valid-provider.json", root -> {
                ArrayNode packages = (ArrayNode) root.get("backend").get("basePackages");
                packages.removeAll();
                packages.add(pkg);
            }));

            assertFalse(pkg, validation.isValid());
            assertTrue(validation.describe(), hasProblemAt(validation, "/backend/basePackages/0"));
        }
    }

    @Test
    public void packagesTheCoreOwnsUnderTheModuleRootAreRefused() {
        ManifestValidation validation = validator.validate(edited("valid-provider.json", root -> {
            ArrayNode packages = (ArrayNode) root.get("backend").get("basePackages");
            packages.removeAll();
            packages.add("org.openelisglobal.module.runtime");
            ((ObjectNode) root.get("backend")).put("activator", "org.openelisglobal.module.web.SiteReportsModule");
            ((ObjectNode) root.get("backend").get("extensions").get(0)).put("class",
                    "org.openelisglobal.module.RootProvider");
        }));

        assertFalse(validation.isValid());
        assertTrue(validation.describe(), hasProblemAt(validation, "/backend/basePackages/0"));
        assertTrue(validation.describe(), hasProblemAt(validation, "/backend/activator"));
        assertTrue(validation.describe(), hasProblemAt(validation, "/backend/extensions/0/class"));
        assertTrue(validation.describe(), validation.describe().contains("belongs to the core"));
    }

    @Test
    public void moduleClassesUnderTheirOwnPackageAreAccepted() {
        ManifestValidation validation = validator.validate(edited("valid-module.json", root -> {
            ArrayNode packages = (ArrayNode) root.get("backend").get("basePackages");
            packages.removeAll();
            packages.add("org.openelisglobal.module.workflow");
            packages.add("org.openelisglobal.module.workflow.extra");
        }));

        assertTrue(validation.describe(), validation.isValid());
    }

    @Test
    public void badIdIsRefusedBySchemaAndRules() {
        ManifestValidation validation = validator
                .validate(edited("valid-provider.json", root -> root.put("id", "Site Reports")));

        assertFalse(validation.isValid());
        assertTrue(validation.describe(), hasProblemMentioning(validation, "id"));
    }

    @Test
    public void menuEntriesMustUseTheModuleNamespaces() {
        ManifestValidation validation = validator.validate(edited("valid-module.json", root -> {
            ObjectNode entry = (ObjectNode) root.get("menu").get(0);
            entry.put("elementId", "menu_mod_other_module");
            entry.put("displayKey", "banner.menu.sample");
            entry.put("actionURL", "/SamplePatientEntry");
            entry.put("permission", "admin");
        }));

        assertFalse(validation.isValid());
        assertTrue(validation.describe(), hasProblemAt(validation, "/menu/0/elementId"));
        assertTrue(validation.describe(), hasProblemAt(validation, "/menu/0/displayKey"));
        assertTrue(validation.describe(), hasProblemAt(validation, "/menu/0/actionURL"));
        assertTrue(validation.describe(), hasProblemAt(validation, "/menu/0/permission"));
    }

    @Test
    public void routesLiveUnderTheModulePrefixAndAreGated() {
        ManifestValidation topLevel = validator.validate(edited("valid-module.json", root -> {
            ObjectNode route = (ObjectNode) root.get("frontend").get("routes").get(0);
            route.put("path", "/cases");
        }));
        assertFalse(topLevel.isValid());
        assertTrue(topLevel.describe(), hasProblemMentioning(topLevel, "path"));

        ManifestValidation anonymous = validator.validate(edited("valid-module.json", root -> {
            ObjectNode route = (ObjectNode) root.get("frontend").get("routes").get(0);
            route.remove("permission");
        }));
        assertFalse(anonymous.isValid());
        assertTrue(anonymous.describe(), hasProblemAt(anonymous, "/frontend/routes/0"));

        ManifestValidation undeclared = validator.validate(edited("valid-module.json", root -> {
            ObjectNode route = (ObjectNode) root.get("frontend").get("routes").get(0);
            route.put("permission", "approve");
        }));
        assertFalse(undeclared.isValid());
        assertTrue(undeclared.describe(), hasProblemAt(undeclared, "/frontend/routes/0/permission"));

        ManifestValidation duplicate = validator.validate(edited("valid-module.json", root -> {
            ObjectNode route = (ObjectNode) root.get("frontend").get("routes").get(1);
            route.put("path", "/m/example-workflow/cases");
        }));
        assertFalse(duplicate.isValid());
        assertTrue(duplicate.describe(), hasProblemAt(duplicate, "/frontend/routes/1/path"));
    }

    @Test
    public void configurationDefaultsMustMatchTheirTypes() {
        ManifestValidation validation = validator.validate(edited("valid-module.json", root -> {
            ObjectNode property = (ObjectNode) root.get("configuration").get("properties").get(0);
            property.put("default", "five");
        }));

        assertFalse(validation.isValid());
        assertTrue(validation.describe(), hasProblemAt(validation, "/configuration/properties/0/default"));
    }

    @Test
    public void duplicatePermissionKeysAreReported() {
        ManifestValidation validation = validator.validate(edited("valid-module.json", root -> {
            ObjectNode second = (ObjectNode) root.get("permissions").get(1);
            second.put("key", "view");
        }));

        assertFalse(validation.isValid());
        assertTrue(validation.describe(), hasProblemAt(validation, "/permissions/1/key"));
    }

    @Test
    public void changelogOutsideTheLiquibaseDirectoryIsRefused() {
        for (String location : new String[] { "db/changelog.xml", "liquibase/", "liquibase/../x.xml" }) {
            ManifestValidation validation = validator.validate(
                    edited("valid-module.json", root -> ((ObjectNode) root.get("backend")).put("liquibase", location)));

            assertFalse(location, validation.isValid());
            assertTrue(validation.describe(), hasProblemAt(validation, "/backend/liquibase"));
            assertTrue(validation.describe(), validation.describe().contains("liquibase/base.xml"));
        }
    }

    @Test
    public void baseChangelogUnderLiquibaseIsAccepted() {
        for (String location : new String[] { "liquibase/base.xml", "liquibase/example-workflow/changelog.xml" }) {
            ManifestValidation validation = validator.validate(
                    edited("valid-module.json", root -> ((ObjectNode) root.get("backend")).put("liquibase", location)));

            assertTrue(location + ": " + validation.describe(), validation.isValid());
        }
    }

    @Test
    public void manifestRoundTripsThroughTheReader() {
        ManifestReader reader = new ManifestReader();
        ModuleManifest manifest = reader.read(fixture("valid-module.json"));
        ModuleManifest again = reader.read(reader.write(manifest));

        assertNotNull(again.getBackend());
        assertEquals(manifest.getBackend().getExtensions().get(0).getClassName(),
                again.getBackend().getExtensions().get(0).getClassName());
        assertEquals("5", again.getConfiguration().getProperties().get(0).getDefaultValue());
        assertTrue(validator.checkRules(again).isEmpty());
    }

    private static boolean hasProblemAt(ManifestValidation validation, String path) {
        return validation.problems().stream().map(Problem::path).anyMatch(path::equals);
    }

    private static boolean hasProblemMentioning(ManifestValidation validation, String text) {
        return validation.problems().stream().anyMatch(p -> p.toString().contains(text));
    }

    static String fixture(String name) {
        try (InputStream in = ManifestValidatorTest.class.getResourceAsStream("/module-fixtures/" + name)) {
            if (in == null) {
                throw new IllegalStateException("fixture " + name + " is missing");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String edited(String name, Consumer<ObjectNode> edit) {
        try {
            ObjectNode root = (ObjectNode) JSON.readTree(fixture(name));
            edit.accept(root);
            return JSON.writeValueAsString(root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
