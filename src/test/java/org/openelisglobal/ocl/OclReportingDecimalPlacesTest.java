package org.openelisglobal.ocl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class OclReportingDecimalPlacesTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    public void missingMetadataOrPermissionToEnterDecimalsDoesNotInventAReportingCount() throws Exception {
        for (String extras : new String[] { "{}", "{\"allow_decimal\":true}", "{\"allow_decimal\":false}",
                "{\"allow_decimal\":\"true\"}", "{\"allow_decimal\":\"false\"}" }) {
            assertEquals("", map(extras).path("reportingDecimalPlaces").asText());
        }
    }

    @Test
    public void anExplicitDecimalPlaceCountIsRetainedIncludingZero() throws Exception {
        assertEquals("0", map("{\"decimal_places\":0}").path("reportingDecimalPlaces").asText());
        assertEquals("3", map("{\"decimal_places\":3}").path("reportingDecimalPlaces").asText());
    }

    private ObjectNode map(String extras) throws Exception {
        OclToOpenElisMapper mapper = mock(OclToOpenElisMapper.class, CALLS_REAL_METHODS);
        ObjectNode output = json.createObjectNode();
        ReflectionTestUtils.invokeMethod(mapper, "mapNumericValidation",
                json.readTree("{\"datatype\":\"Numeric\",\"extras\":" + extras + "}"), output);
        return output;
    }
}
