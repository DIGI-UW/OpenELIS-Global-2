package org.openelisglobal.analyzermigration;

import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.nio.file.Path;
import java.util.Set;

final class MigrationContractAssertions {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path SCHEMA = Path.of("..", "..", "specs", "015-ogc-1054-analyzer-contract-migration",
            "contracts", "analyzer-migration-manifest.schema.json");

    private MigrationContractAssertions() {
    }

    static void assertManifestConforms(JsonNode manifest) throws Exception {
        JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                .getSchema(JSON.readTree(SCHEMA.toFile()));
        Set<ValidationMessage> messages = schema.validate(manifest);
        assertTrue("migration manifest contract violations: " + messages, messages.isEmpty());
    }
}
