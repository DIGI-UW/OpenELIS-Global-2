package org.openelisglobal.analyzer.contract;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class AnalyzerActivationBoundaryContractTest {

    private static final Path ANALYZER_ROOT = Path.of("src", "main", "java", "org", "openelisglobal", "analyzer");

    @Test
    public void activationServiceIsTheOnlyAnalyzerInstanceActivationWriter() throws Exception {
        assertDoesNotContain(ANALYZER_ROOT.resolve("controller/AnalyzerRestController.java"), "setActive(true)");
        assertDoesNotContain(ANALYZER_ROOT.resolve("service/AnalyzerTypeService.java"), "getOrCreateDefaultInstance");
        assertDoesNotContain(ANALYZER_ROOT.resolve("service/AnalyzerTypeServiceImpl.java"),
                "getOrCreateDefaultInstance");
        assertDoesNotContain(ANALYZER_ROOT.resolve("service/AnalyzerTypeServiceImpl.java"), "setActive(true)");

        String activationService = Files
                .readString(ANALYZER_ROOT.resolve("service/AnalyzerActivationServiceImpl.java"));
        assertTrue(activationService.contains("setStatus(Analyzer.AnalyzerStatus.ACTIVE)"));
        assertTrue(activationService.contains("setActive(true)"));
    }

    private static void assertDoesNotContain(Path path, String text) throws Exception {
        assertTrue("missing source file: " + path, Files.isRegularFile(path));
        assertFalse(path + " contains " + text, Files.readString(path).contains(text));
    }
}
