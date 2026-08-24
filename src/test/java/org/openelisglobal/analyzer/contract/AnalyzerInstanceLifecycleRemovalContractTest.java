package org.openelisglobal.analyzer.contract;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class AnalyzerInstanceLifecycleRemovalContractTest {

    private static final Path JAVA_ROOT = Path.of("src", "main", "java", "org", "openelisglobal", "analyzer");
    private static final Path FRONTEND_ROOT = Path.of("frontend", "src");

    @Test
    public void hardDeleteHasNoAnalyzerInstanceRuntimeOrUiPath() throws Exception {
        assertDoesNotContain(JAVA_ROOT.resolve("controller/AnalyzerActivationRestController.java"), "/{id}/delete");
        assertDoesNotContain(JAVA_ROOT.resolve("service/AnalyzerService.java"), "deleteWithDependents");
        assertDoesNotContain(FRONTEND_ROOT.resolve("services/analyzerService.ts"), "deleteAnalyzer");
        assertDoesNotContain(FRONTEND_ROOT.resolve("components/analyzers/AnalyzersList/AnalyzersList.tsx"),
                "analyzer-action-delete");
        assertDoesNotContain(Path.of("frontend", "playwright", "helpers", "create-analyzer-from-profile.ts"),
                "deleteAnalyzerFromDashboard");
        Path oldModal = FRONTEND_ROOT.resolve("components/analyzers/DeleteAnalyzerModal");
        assertFalse(Files.exists(oldModal.resolve("DeleteAnalyzerModal.tsx")));
        assertFalse(Files.exists(oldModal.resolve("DeleteAnalyzerModal.test.jsx")));
    }

    @Test
    public void supersededDeletedStatusCannotReturn() throws Exception {
        assertDoesNotContain(JAVA_ROOT.resolve("valueholder/Analyzer.java"), "DELETED");
        Path normalization = Path.of("src", "main", "resources", "liquibase", "3.5.x.x",
                "092-normalize-analyzer-deleted-status.xml");
        assertTrue("missing lifecycle normalization migration", Files.isRegularFile(normalization));
        String migration = Files.readString(normalization);
        assertTrue(migration.contains("<column name=\"status\" value=\"INACTIVE\"/>"));
        assertTrue(migration.contains("<where>status = 'DELETED'</where>"));
    }

    @Test
    public void supersededAnalyzerFormHasNoRuntimeCompatibilityPath() throws Exception {
        Path formRoot = JAVA_ROOT.resolve("form");
        assertFalse("superseded AnalyzerForm remains", Files.exists(formRoot.resolve("AnalyzerForm.java")));

        Path request = formRoot.resolve("AnalyzerInstanceRequest.java");
        assertTrue("missing analyzer instance request", Files.isRegularFile(request));
        String source = Files.readString(request);
        assertFalse("instance request ignores unknown legacy fields", source.contains("JsonIgnoreProperties"));
        for (String removed : new String[] { "status", "analyzerType", "protocolVersion", "identifierPattern",
                "pluginTypeId", "filePattern", "fileFormat", "columnMappings", "delimiter", "hasHeader", "skipRows" }) {
            assertFalse("instance request retains superseded field " + removed, source.contains(removed));
        }
    }

    private static void assertDoesNotContain(Path path, String text) throws Exception {
        assertTrue("missing source file: " + path, Files.isRegularFile(path));
        assertFalse(path + " contains " + text, Files.readString(path).contains(text));
    }
}
