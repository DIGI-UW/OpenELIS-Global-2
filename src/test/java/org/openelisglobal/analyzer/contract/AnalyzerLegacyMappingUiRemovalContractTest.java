package org.openelisglobal.analyzer.contract;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class AnalyzerLegacyMappingUiRemovalContractTest {

    private static final Path FRONTEND_ROOT = Path.of("frontend", "src");

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
    }

    @Test
    public void supersededMappingCopyIsAbsent() throws Exception {
        String messages = Files.readString(FRONTEND_ROOT.resolve("languages/en.json"));
        assertFalse(messages.contains("analyzer.action.copyMappings"));
        assertFalse(messages.contains("analyzer.action.fieldMappings"));
        assertFalse(messages.contains("analyzer.fieldMapping."));
        assertFalse(messages.contains("analyzer.errorDetails.createMapping"));
        assertFalse(messages.contains("analyzer.errorDetails.recommendedActions.createMapping"));
    }

    private static void assertDoesNotContain(Path path, String text) throws Exception {
        assertTrue("missing source file: " + path, Files.isRegularFile(path));
        assertFalse(path + " contains " + text, Files.readString(path).contains(text));
    }
}
