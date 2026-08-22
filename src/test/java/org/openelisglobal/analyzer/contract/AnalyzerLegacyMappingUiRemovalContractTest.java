package org.openelisglobal.analyzer.contract;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Test;

public class AnalyzerLegacyMappingUiRemovalContractTest {

    private static final Path FRONTEND_ROOT = Path.of("frontend", "src");
    private static final Path PLAYWRIGHT_ROOT = Path.of("frontend", "playwright");
    private static final Path WEBAPP_ROOT = Path.of("src", "main", "webapp");
    private static final Path RESOURCES_ROOT = Path.of("src", "main", "resources");
    private static final Path BACKEND_ROOT = Path.of("src", "main", "java", "org", "openelisglobal", "analyzer");
    private static final Path ANALYZER_IMPORT_ROOT = Path.of("src", "main", "java", "org", "openelisglobal",
            "analyzerimport");
    private static final Path COMMON_ROOT = Path.of("src", "main", "java", "org", "openelisglobal", "common");
    private static final Path MENU_ROOT = Path.of("src", "main", "java", "org", "openelisglobal", "menu");

    @Test
    public void perAnalyzerMappingEditorAndQueueAreAbsent() throws Exception {
        assertFalse(Files.exists(FRONTEND_ROOT.resolve("components/analyzers/FieldMapping")));

        assertDoesNotContain(FRONTEND_ROOT.resolve("App.jsx"), "FieldMapping");
        assertDoesNotContain(FRONTEND_ROOT.resolve("App.jsx"), "/analyzers/:id/mappings");
        assertDoesNotContain(FRONTEND_ROOT.resolve("components/analyzers/AnalyzersList/AnalyzersList.tsx"),
                "CopyMappingsModal");
        assertDoesNotContain(FRONTEND_ROOT.resolve("components/analyzers/ErrorDashboard/ErrorDetailsModal.jsx"),
                "/mappings");
        assertDoesNotContain(FRONTEND_ROOT.resolve("services/analyzerService.ts"), "copyMappings");
        assertDoesNotContain(FRONTEND_ROOT.resolve("services/analyzerService.ts"), "pending-codes");
        assertDoesNotContain(FRONTEND_ROOT.resolve("services/analyzerService.ts"), "preview-mapping");
        assertFalse(Files.exists(PLAYWRIGHT_ROOT.resolve("tests/foundational/harness/analyzer-simulator.spec.ts")));
    }

    @Test
    public void supersededMappingCopyIsAbsent() throws Exception {
        String messages = Files.readString(FRONTEND_ROOT.resolve("languages/en.json"));
        assertFalse(messages.contains("analyzer.action.copyMappings"));
        assertFalse(messages.contains("analyzer.action.fieldMappings"));
        assertFalse(messages.contains("analyzer.copyMappings."));
        assertFalse(messages.contains("analyzer.fieldMapping."));
        assertFalse(messages.contains("analyzer.errorDetails.createMapping"));
        assertFalse(messages.contains("analyzer.errorDetails.recommendedActions.createMapping"));
    }

    @Test
    public void supersededPerAnalyzerMappingRestSurfaceIsAbsent() {
        assertFalse(Files.exists(BACKEND_ROOT.resolve("controller/AnalyzerFieldMappingRestController.java")));
        for (String path : List.of("form/AnalyzerFieldMappingForm.java", "service/AnalyzerFieldMappingHydrator.java",
                "service/AnalyzerFieldMappingService.java", "service/AnalyzerFieldMappingServiceImpl.java",
                "service/AnalyzerMappingCopyService.java", "service/AnalyzerMappingCopyServiceImpl.java",
                "service/AnalyzerMappingPreviewService.java", "service/AnalyzerMappingPreviewServiceImpl.java",
                "service/MappingValidationService.java", "service/MappingValidationServiceImpl.java",
                "service/CopyMappingsResult.java", "service/CopyOptions.java", "service/MappingPreviewResult.java",
                "service/ParsedField.java", "service/AppliedMapping.java", "service/EntityPreview.java",
                "service/PreviewOptions.java")) {
            assertFalse("superseded per-analyzer mapping helper remains: " + path,
                    Files.exists(BACKEND_ROOT.resolve(path)));
        }
    }

    @Test
    public void legacyAnalyzerTestNameAdminSurfaceIsAbsent() throws Exception {
        assertFalse(Files.exists(FRONTEND_ROOT.resolve("components/admin/analyzerTestName/AnalyzerTestName.jsx")));
        assertDoesNotContain(FRONTEND_ROOT.resolve("components/admin/Admin.jsx"), "AnalyzerTestName");
        assertDoesNotContain(FRONTEND_ROOT.resolve("components/admin/AdminSideNav.jsx"), "AnalyzerTestName");
        assertFalse(Files.exists(WEBAPP_ROOT.resolve("pages/analyzertestname/analyzerTestName.jsp")));
        assertFalse(Files.exists(WEBAPP_ROOT.resolve("pages/analyzertestname/analyzerTestNameMenu.jsp")));
        assertDoesNotContain(RESOURCES_ROOT.resolve("tiles/tiles-defs.xml"), "analyzerTestNameDefinition");
        assertDoesNotContain(RESOURCES_ROOT.resolve("tiles/tiles-defs.xml"), "analyzerTestNameMenuDefinition");
        assertDoesNotContain(MENU_ROOT.resolve("service/AdminMenuItemServiceImpl.java"), "/AnalyzerTestNameMenu");
        assertDoesNotContain(COMMON_ROOT.resolve("formfields/AdminFormFields.java"), "AnalyzerTestNameMenu");
        assertDoesNotContain(COMMON_ROOT.resolve("formfields/DefaultAdminFormFields.java"), "AnalyzerTestNameMenu");

        for (String path : List.of("controller/AnalyzerTestNameController.java",
                "controller/AnalyzerTestNameMenuController.java", "controller/rest/AnalyzerTestNameRestController.java",
                "controller/rest/AnalyzerTestNameMenuRestController.java", "form/AnalyzerTestNameForm.java",
                "form/AnalyzerTestNameMenuForm.java", "validator/AnalyzerTestMappingValidator.java",
                "action/beans/NamedAnalyzerTestMapping.java")) {
            assertFalse("legacy Analyzer Test Names admin helper remains: " + path,
                    Files.exists(ANALYZER_IMPORT_ROOT.resolve(path)));
        }
    }

    @Test
    public void legacyCustomFieldTypeAdminSurfaceIsAbsent() throws Exception {
        assertFalse(Files.exists(FRONTEND_ROOT.resolve("pages/CustomFieldTypeManagementPage.jsx")));
        assertFalse(Files.exists(FRONTEND_ROOT.resolve("components/analyzers/admin/CustomFieldTypeManagement.jsx")));
        assertFalse(
                Files.exists(FRONTEND_ROOT.resolve("components/analyzers/CustomFieldTypes/ValidationRuleEditor.jsx")));
        assertDoesNotContain(FRONTEND_ROOT.resolve("App.jsx"), "CustomFieldTypeManagementPage");
        assertDoesNotContain(FRONTEND_ROOT.resolve("App.jsx"), "/analyzers/custom-field-types");
        assertDoesNotContain(FRONTEND_ROOT.resolve("services/analyzerService.ts"), "custom-field-types");
        assertDoesNotContain(FRONTEND_ROOT.resolve("languages/en.json"), "\"customFieldType.");
        assertDoesNotContain(FRONTEND_ROOT.resolve("languages/en.json"), "\"validationRule.");

        for (String path : List.of("controller/CustomFieldTypeRestController.java", "form/CustomFieldTypeForm.java",
                "form/ValidationRuleConfigurationForm.java")) {
            assertFalse("legacy custom-field mapping writer remains: " + path,
                    Files.exists(BACKEND_ROOT.resolve(path)));
        }
    }

    @Test
    public void legacyPerAnalyzerMappingWritersAreAbsent() throws Exception {
        Path service = BACKEND_ROOT.resolve("service/AnalyzerService.java");
        Path implementation = BACKEND_ROOT.resolve("service/AnalyzerServiceImpl.java");
        Path pluginService = COMMON_ROOT.resolve("services/PluginAnalyzerService.java");

        assertDoesNotContain(service, "persistTestMappings");
        assertDoesNotContain(service, "void persistData(Analyzer analyzer, List<AnalyzerTestMapping>");
        assertDoesNotContain(service, "autoCreateTestMappings");
        assertDoesNotContain(implementation, "persistTestMappings");
        assertDoesNotContain(implementation, "newMapping(AnalyzerTestMapping");
        assertDoesNotContain(implementation, "autoCreateTestMappings");
        assertDoesNotContain(pluginService, "findOrCreateAnalyzerForType");
        assertDoesNotContain(pluginService, "createTestMappings");
        assertDoesNotContain(pluginService, "analyzerMappingService");
        assertDoesNotContain(BACKEND_ROOT.resolve("controller/AnalyzerRestController.java"),
                "analyzerTestMappingService");
    }

    private static void assertDoesNotContain(Path path, String text) throws Exception {
        assertTrue("missing source file: " + path, Files.isRegularFile(path));
        assertFalse(path + " contains " + text, Files.readString(path).contains(text));
    }
}
