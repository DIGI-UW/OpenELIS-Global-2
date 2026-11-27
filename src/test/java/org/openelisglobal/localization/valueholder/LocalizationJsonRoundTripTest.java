package org.openelisglobal.localization.valueholder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

/**
 * OGC-1609: a screen posts back the localization it was served (Site
 * Information, any translated setting). The derived {@code siteLabel} and
 * {@code shippedValue} it carries must not make the body unreadable, so the
 * save is not refused with 400.
 */
public class LocalizationJsonRoundTripTest {

    private final ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(Include.NON_NULL);

    @Test
    public void aLocalizationReadsBackWhatItWasServed() throws Exception {
        Localization localization = new Localization();
        localization.setDescription("Site information banner test");
        localization.setLocalizedValue("en", "Test LIMS");
        localization.setSystemValue("fr", "Test LIMS", true);
        localization.getValues().get("fr").writeSiteLabel("Banniere");

        String values = mapper.writeValueAsString(localization.getValues());
        assertTrue("the response still carries the derived flags", values.contains("\"siteLabel\":true"));
        assertTrue(values.contains("\"shippedValue\":\"Test LIMS\""));
        String served = "{\"description\":\"Site information banner test\",\"english\":\"Test LIMS QA\",\"values\":"
                + values.replaceFirst("\"value\":\"Test LIMS\"", "\"value\":\"Test LIMS QA\"") + "}";

        Localization posted = mapper.readValue(served, Localization.class);

        assertEquals("Test LIMS QA", posted.getValues().get("en").getValue());
        assertEquals("Banniere", posted.getValues().get("fr").getValue());
    }
}
