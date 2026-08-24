package org.openelisglobal.analyzer.form;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import java.util.List;
import org.junit.Test;

public class AnalyzerInstanceRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void readsOnlyProfilePinLabUnitsAndRoleApplicableInstanceSettings() throws Exception {
        AnalyzerInstanceRequest request = objectMapper.readValue("""
                {
                  "name":"Fluoro bench 1",
                  "profileId":"fluorocycler-xt",
                  "profileRevision":1,
                  "testUnitIds":["7"],
                  "transportMode":"FILE",
                  "connectionRole":"RECEIVER",
                  "importDirectory":"/data/analyzers/fluoro/incoming"
                }
                """, AnalyzerInstanceRequest.class);

        assertEquals("Fluoro bench 1", request.getName());
        assertEquals("fluorocycler-xt", request.getProfileId());
        assertEquals(Integer.valueOf(1), request.getProfileRevision());
        assertEquals(List.of("7"), request.getTestUnitIds());
        assertEquals("FILE", request.getTransportMode());
        assertEquals("RECEIVER", request.getConnectionRole());
        assertEquals("/data/analyzers/fluoro/incoming", request.getImportDirectory());
    }

    @Test
    public void rejectsEverySupersededAnalyzerFormField() throws Exception {
        for (String field : List.of("status", "analyzerType", "protocolVersion", "identifierPattern", "pluginTypeId",
                "filePattern", "fileFormat", "columnMappings", "delimiter", "hasHeader", "skipRows")) {
            try {
                objectMapper.readValue("{\"name\":\"Bench 1\",\"" + field + "\":null}", AnalyzerInstanceRequest.class);
                fail("accepted superseded analyzer instance field " + field);
            } catch (UnrecognizedPropertyException expected) {
                assertEquals(field, expected.getPropertyName());
            }
        }
    }
}
