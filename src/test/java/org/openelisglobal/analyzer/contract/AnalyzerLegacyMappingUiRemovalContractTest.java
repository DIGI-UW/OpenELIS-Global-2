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
    private static final Path BACKEND_ROOT = Path.of("src", "main", "java", "org", "openelisglobal", "analyzer");

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

    private static void assertDoesNotContain(Path path, String text) throws Exception {
        assertTrue("missing source file: " + path, Files.isRegularFile(path));
        assertFalse(path + " contains " + text, Files.readString(path).contains(text));
    }
}
