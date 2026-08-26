package org.openelisglobal.shipment.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.junit.Test;

/**
 * T-44: the fhirNonConformityCodes SiteInformation config must be parsed as
 * real JSON, not scanned by indexOf — whitespace or formatting variations must
 * not silently drop custom SNOMED codes.
 */
public class ShippingBoxFhirTransformNonConformityCodesTest {

    @Test
    public void compactJson_parsesAllEntries() {
        Map<String, String> codes = ShippingBoxFhirTransform
                .parseNonConformityCodes("{\"RECEIVED_DAMAGED\":\"12345\",\"MISSING\":\"67890\"}");
        assertEquals("12345", codes.get("RECEIVED_DAMAGED"));
        assertEquals("67890", codes.get("MISSING"));
    }

    @Test
    public void spacedJson_parsesCorrectly() {
        Map<String, String> codes = ShippingBoxFhirTransform
                .parseNonConformityCodes("{ \"RECEIVED_DAMAGED\" : \"12345\" }");
        assertEquals("12345", codes.get("RECEIVED_DAMAGED"));
    }

    @Test
    public void malformedJson_yieldsEmptyMapSoDefaultsApply() {
        Map<String, String> codes = ShippingBoxFhirTransform.parseNonConformityCodes("{'RECEIVED_DAMAGED': not json");
        assertTrue(codes.isEmpty());
    }

    @Test
    public void blankOrNull_yieldsEmptyMap() {
        assertTrue(ShippingBoxFhirTransform.parseNonConformityCodes("").isEmpty());
        assertTrue(ShippingBoxFhirTransform.parseNonConformityCodes(null).isEmpty());
    }
}
