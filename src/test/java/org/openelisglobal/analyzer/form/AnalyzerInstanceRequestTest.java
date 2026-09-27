package org.openelisglobal.analyzer.form;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.Test;

public class AnalyzerInstanceRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void readsProfilePinLabUnitsAndGenericBridgeConnectionValues() throws Exception {
        AnalyzerInstanceRequest request = objectMapper.readValue("""
                {
                  "name":"Synthetic bench 1",
                  "profileId":"fixture.synthetic-connection",
                  "profileRevision":1,
                  "testUnitIds":["7"],
                  "connectionValues":{
                    "futureTextField":"configured by the lab",
                    "futureNumberField":4317
                  }
                }
                """, AnalyzerInstanceRequest.class);

        assertEquals("Synthetic bench 1", request.getName());
        assertEquals("fixture.synthetic-connection", request.getProfileId());
        assertEquals(Integer.valueOf(1), request.getProfileRevision());
        assertEquals(List.of("7"), request.getTestUnitIds());
        assertEquals("configured by the lab", request.getConnectionValues().path("futureTextField").asText());
        assertEquals(4317, request.getConnectionValues().path("futureNumberField").asInt());
    }
}
