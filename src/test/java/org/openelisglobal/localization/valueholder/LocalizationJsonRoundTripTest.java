package org.openelisglobal.localization.valueholder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.externalconnections.form.ExternalConnectionForm;
import org.openelisglobal.siteinformation.form.SiteInformationForm;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;

/** Settings write the translation map; display aliases cannot override it. */
public class LocalizationJsonRoundTripTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private LocaleContext originalLocale;

    private static final String VALUES = "\"values\":{" + "\"en\":{\"locale\":\"en\",\"value\":\"New English\"},"
            + "\"fr\":{\"locale\":\"fr\",\"value\":\"New French\",\"origin\":\"SITE\",\"systemValue\":\"Shipped French\",\"siteLabel\":false,\"shippedValue\":\"Stale display\"},"
            + "\"es\":{\"locale\":\"es\",\"value\":\"Preserved Spanish\"}}";
    private static final String ALIASES = "\"english\":\"Old English\",\"french\":\"Old French\","
            + "\"localizedValue\":\"Old display text\"";

    @Before
    public void rememberLocale() {
        originalLocale = LocaleContextHolder.getLocaleContext();
    }

    @After
    public void restoreLocale() {
        LocaleContextHolder.setLocaleContext(originalLocale);
    }

    @Test
    public void englishSessionUsesTranslationMapRegardlessOfFieldOrder() throws Exception {
        assertTranslationMapWins(Locale.ENGLISH);
    }

    @Test
    public void frenchSessionUsesTranslationMapRegardlessOfFieldOrder() throws Exception {
        assertTranslationMapWins(Locale.FRENCH);
    }

    private void assertTranslationMapWins(Locale locale) throws Exception {
        LocaleContextHolder.setLocale(locale);
        for (String localization : new String[] { VALUES + "," + ALIASES, ALIASES + "," + VALUES, VALUES }) {
            SiteInformationForm form = mapper.readValue("{\"localization\":{" + localization + "}}",
                    SiteInformationForm.class);
            Localization saved = form.getLocalization();
            assertEquals(localization, "New English", saved.getLocalizedValue("en"));
            assertEquals(localization, "New French", saved.getLocalizedValue("fr"));
            assertEquals(localization, "Preserved Spanish", saved.getLocalizedValue("es"));
            assertEquals(localization, 3, saved.getValues().size());
            assertTrue(saved.getValues().get("fr").isSiteLabel());
            assertEquals("Shipped French", saved.getValues().get("fr").getShippedValue());
        }
    }

    @Test
    public void settingsResponseStillIncludesDisplayAliases() throws Exception {
        LocaleContextHolder.setLocale(Locale.FRENCH);
        SiteInformationForm form = mapper.readValue("{\"localization\":{" + VALUES + "}}", SiteInformationForm.class);
        // Exclude unrelated getters that query the configured language catalog.
        mapper.addMixIn(Localization.class, DisplayFields.class);
        mapper.setFilterProvider(new SimpleFilterProvider().addFilter("displayFields",
                SimpleBeanPropertyFilter.filterOutAllExcept("values", "english", "french", "localizedValue")));
        JsonNode response = mapper.readTree(mapper.writeValueAsString(form)).get("localization");
        assertEquals("New English", response.get("english").asText());
        assertEquals("New French", response.get("french").asText());
        assertEquals("New French", response.get("localizedValue").asText());
        assertEquals("Preserved Spanish", response.at("/values/es/value").asText());
        assertTrue(response.at("/values/fr/siteLabel").asBoolean());
        assertEquals("Shipped French", response.at("/values/fr/shippedValue").asText());
    }

    @Test
    public void connectionEditorStillAcceptsCurrentLanguageInput() throws Exception {
        LocaleContextHolder.setLocale(Locale.FRENCH);
        ExternalConnectionForm form = mapper.readValue(
                "{\"externalConnection\":{" + "\"nameLocalization\":{\"localizedValue\":\"Partner name\"},"
                        + "\"descriptionLocalization\":{\"localizedValue\":\"Partner description\"}}}",
                ExternalConnectionForm.class);
        assertEquals("Partner name", form.getExternalConnection().getNameLocalization().getLocalizedValue("fr"));
        assertEquals("Partner description",
                form.getExternalConnection().getDescriptionLocalization().getLocalizedValue("fr"));
        assertEquals("", form.getExternalConnection().getNameLocalization().getLocalizedValue("en"));
    }

    @JsonFilter("displayFields")
    private abstract static class DisplayFields {
    }
}
