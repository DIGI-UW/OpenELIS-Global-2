package org.openelisglobal.module.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

/**
 * The frontend contract version has one source, the module SDK package: the
 * shell's registry reads it from there, and the backend constant that
 * {@code /rest/module-registry} publishes is kept in step by this test.
 */
public class ModuleFrontendContractTest {

    private static final Path SDK_PACKAGE = Path.of("frontend/packages/module-sdk/package.json");

    @Test
    public void versionIsTheModuleSdkPackageVersion() throws IOException {
        assertTrue(SDK_PACKAGE + " is missing", Files.isRegularFile(SDK_PACKAGE));
        JsonNode manifest = new ObjectMapper().readTree(Files.readString(SDK_PACKAGE));
        assertEquals("@openelis/module-sdk", manifest.path("name").asText());
        assertEquals(manifest.path("version").asText(), ModuleFrontendContract.VERSION);
    }

    @Test
    public void versionIsASemanticVersion() {
        assertTrue(ModuleFrontendContract.VERSION, ModuleFrontendContract.VERSION.matches("\\d+\\.\\d+\\.\\d+"));
    }
}
