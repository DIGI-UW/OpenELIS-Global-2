package org.openelisglobal.labelpreset.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.labelpreset.controller.rest.SiteWideBarcodeSettingsRestController.SiteBarcodePreprintSettings;
import org.openelisglobal.siteinformation.service.SiteInformationDomainService;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.siteinformation.valueholder.SiteInformationDomain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for
 * {@link org.openelisglobal.labelpreset.controller.rest.SiteWideBarcodeSettingsRestController}
 * (OGC-285 M3).
 *
 * <p>
 * Real service + real DB; no @MockBean of code-under-test.
 */
@Transactional
public class SiteWideBarcodeSettingsRestControllerTest extends BaseWebContextSensitiveTest {

    private static final String BASE_URL = "/api/siteSettings/barcode";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private SiteInformationService siteInformationService;

    @Autowired
    private SiteInformationDomainService siteInformationDomainService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/system-user.xml");
        ensureLabelsDomainAndBarcodeRows();
        ConfigurationProperties.loadDBValuesIntoConfiguration();
    }

    private void ensureLabelsDomainAndBarcodeRows() {
        SiteInformationDomain labelsDomain = siteInformationDomainService.getByName("labels");
        if (labelsDomain == null) {
            labelsDomain = new SiteInformationDomain();
            labelsDomain.setName("labels");
            labelsDomain.setDescription("Barcode/label configuration");
            siteInformationDomainService.insert(labelsDomain);
        }
        ensureSiteInfoRow("prePrintAltAccessionPrefix", "", labelsDomain);
        ensureSiteInfoRow("prePrintUseAltAccession", "false", labelsDomain);
        // Saving settings reloads all configuration, including these localized values.
        // Own their rows inside the test transaction instead of relying on suite seeds.
        localizedSetting(Property.BANNER_TEXT, 993410L, "Barcode test laboratory", labelsDomain);
        localizedSetting(Property.BILLING_REFERENCE_NUMBER_LABEL, 993411L, "Barcode test billing", labelsDomain);
    }

    private void localizedSetting(Property property, long id, String label, SiteInformationDomain domain) {
        jdbcTemplate.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, NOW())", id,
                label);
        jdbcTemplate.update("INSERT INTO clinlims.localization_value"
                + " (id, localization_id, locale, value) VALUES (?, ?, 'en', ?)", id, id, label);
        ensureSiteInfoRow(property.getDBName(), Long.toString(id), domain);
        SiteInformation row = siteInformationService.getSiteInformationByName(property.getDBName());
        row.setValue(Long.toString(id));
        row.setValueType("text");
        row.setTag("localization");
        row.setSysUserId(TEST_SYS_USER_ID);
        siteInformationService.save(row);
    }

    private void ensureSiteInfoRow(String name, String defaultValue, SiteInformationDomain domain) {
        SiteInformation row = siteInformationService.getSiteInformationByName(name);
        boolean isNew = row == null;
        if (isNew) {
            row = new SiteInformation();
            row.setName(name);
            row.setValueType("text");
            row.setDomain(domain);
        }
        row.setValue(defaultValue);
        row.setSysUserId(TEST_SYS_USER_ID);
        siteInformationService.persistData(row, isNew);
    }

    @Test
    public void getSettings_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    public void getSettings_returnsPrePrintFields() throws Exception {
        mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.prePrintUseAltAccession").value(false));
    }

    @Test
    public void postSettings_savesAndReflectsNewValues() throws Exception {
        SiteBarcodePreprintSettings body = new SiteBarcodePreprintSettings();
        body.setPrePrintUseAltAccession(true);
        body.setPrePrintAltAccessionPrefix("TEST");

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.prePrintUseAltAccession").value(true))
                .andExpect(jsonPath("$.prePrintAltAccessionPrefix").value("TEST"));

        ConfigurationProperties.loadDBValuesIntoConfiguration();

        mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.prePrintUseAltAccession").value(true))
                .andExpect(jsonPath("$.prePrintAltAccessionPrefix").value("TEST"));
        SiteBarcodePreprintSettings saved = readSettings();
        assertEquals("Alt accession prefix should be saved", "TEST", saved.getPrePrintAltAccessionPrefix());
        assertEquals("UseAltAccession flag should be saved", Boolean.TRUE, saved.getPrePrintUseAltAccession());
    }

    @Test
    public void postSettings_updatesExistingRows() throws Exception {
        // First save
        SiteBarcodePreprintSettings body1 = new SiteBarcodePreprintSettings();
        body1.setPrePrintUseAltAccession(false);
        body1.setPrePrintAltAccessionPrefix("FRST");
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.prePrintUseAltAccession").value(false))
                .andExpect(jsonPath("$.prePrintAltAccessionPrefix").value("FRST"));

        // Second save (update)
        SiteBarcodePreprintSettings body2 = new SiteBarcodePreprintSettings();
        body2.setPrePrintUseAltAccession(true);
        body2.setPrePrintAltAccessionPrefix("SCND");
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.prePrintUseAltAccession").value(true))
                .andExpect(jsonPath("$.prePrintAltAccessionPrefix").value("SCND"));

        ConfigurationProperties.loadDBValuesIntoConfiguration();

        mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.prePrintUseAltAccession").value(true))
                .andExpect(jsonPath("$.prePrintAltAccessionPrefix").value("SCND"));

        assertEquals("SCND", siteInformationService.getSiteInformationByName("prePrintAltAccessionPrefix").getValue());
        assertEquals("true", siteInformationService.getSiteInformationByName("prePrintUseAltAccession").getValue());

        SiteBarcodePreprintSettings saved = readSettings();
        assertEquals("Should reflect updated prefix", "SCND", saved.getPrePrintAltAccessionPrefix());
    }

    // ── OGC-1217: the prefix is validated only for the separate pre-printed series
    // ──

    @Test
    public void postSettings_separateSeriesWithShortPrefix_isRejectedAndNothingChanges() throws Exception {
        SiteBarcodePreprintSettings stored = new SiteBarcodePreprintSettings();
        stored.setPrePrintUseAltAccession(false);
        stored.setPrePrintAltAccessionPrefix("KEEP");
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(stored)))
                .andExpect(status().isOk());

        SiteBarcodePreprintSettings body = new SiteBarcodePreprintSettings();
        body.setPrePrintUseAltAccession(true);
        body.setPrePrintAltAccessionPrefix("ABC");
        MvcResult rejected = mockMvc
                .perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity()).andReturn();
        String reason = rejected.getResponse().getContentAsString();
        assertTrue("the field is named: " + reason, reason.contains("prePrintAltAccessionPrefix"));
        assertTrue("the message is a text tag: " + reason,
                reason.contains("{error.sitesettings.barcode.prefix.format}"));

        SiteBarcodePreprintSettings current = readSettings();
        assertEquals(Boolean.FALSE, current.getPrePrintUseAltAccession());
        assertEquals("KEEP", current.getPrePrintAltAccessionPrefix());
    }

    @Test
    public void postSettings_separateSeriesRejectsLongBlankAndSymbolPrefixes() throws Exception {
        for (String bad : new String[] { "ABCDE", "AB-1", "", "    ", "AB C" }) {
            SiteBarcodePreprintSettings body = new SiteBarcodePreprintSettings();
            body.setPrePrintUseAltAccession(true);
            body.setPrePrintAltAccessionPrefix(bad);
            mockMvc.perform(
                    post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)))
                    .andExpect(status().isUnprocessableEntity());
        }
        SiteBarcodePreprintSettings body = new SiteBarcodePreprintSettings();
        body.setPrePrintUseAltAccession(true);
        body.setPrePrintAltAccessionPrefix(null);
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void postSettings_separateSeriesAcceptsFourLettersOrDigits() throws Exception {
        for (String good : new String[] { "AB12", "zz99", "0000" }) {
            SiteBarcodePreprintSettings body = new SiteBarcodePreprintSettings();
            body.setPrePrintUseAltAccession(true);
            body.setPrePrintAltAccessionPrefix(good);
            mockMvc.perform(
                    post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)))
                    .andExpect(status().isOk());
            assertEquals(good.toUpperCase(), readSettings().getPrePrintAltAccessionPrefix());
        }
    }

    @Test
    public void postSettings_orderEntryPoolKeepsWhateverPrefixIsSent() throws Exception {
        SiteBarcodePreprintSettings body = new SiteBarcodePreprintSettings();
        body.setPrePrintUseAltAccession(false);
        body.setPrePrintAltAccessionPrefix("AB");
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)))
                .andExpect(status().isOk());

        SiteBarcodePreprintSettings current = readSettings();
        assertEquals(Boolean.FALSE, current.getPrePrintUseAltAccession());
        assertEquals("AB", current.getPrePrintAltAccessionPrefix());
    }

    @Test
    public void postSettings_trimsThePrefixBeforeValidatingAndStoring() throws Exception {
        SiteBarcodePreprintSettings body = new SiteBarcodePreprintSettings();
        body.setPrePrintUseAltAccession(true);
        body.setPrePrintAltAccessionPrefix(" AB12 ");
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)))
                .andExpect(status().isOk());
        assertEquals("AB12", readSettings().getPrePrintAltAccessionPrefix());
    }

    @Test
    public void postSettings_storesThePrefixInCapitalsLikeTheCardShowsIt() throws Exception {
        SiteBarcodePreprintSettings body = new SiteBarcodePreprintSettings();
        body.setPrePrintUseAltAccession(true);
        body.setPrePrintAltAccessionPrefix("abcd");
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)))
                .andExpect(status().isOk());
        assertEquals("lab numbers are compared exactly, so the stored prefix is what the card produces", "ABCD",
                readSettings().getPrePrintAltAccessionPrefix());

        SiteBarcodePreprintSettings kept = new SiteBarcodePreprintSettings();
        kept.setPrePrintUseAltAccession(false);
        kept.setPrePrintAltAccessionPrefix("ab");
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(kept)))
                .andExpect(status().isOk());
        assertEquals("AB", readSettings().getPrePrintAltAccessionPrefix());
    }

    /**
     * Regression guard for the initially-absent-row case.
     *
     * <p>
     * If neither DB row exists at test startup, setUp() inserts rows (via
     * resetSettingsToDefaults()) and teardown deletes them. Without explicit cache
     * restoration the in-memory cache still holds the values written by the test
     * after teardown, corrupting the next test's baseline.
     *
     * <p>
     * This test verifies that after a POST the GET reflects the posted values, and
     * — by relying on setUp()/tearDown() — that the fixture properly cleans up even
     * when both rows were absent before the suite began.
     */
    @Test
    public void postSettings_whenRowsInitiallyAbsent_savesAndCacheIsRestoredOnTeardown() throws Exception {
        // Delete both rows so they are absent for this logical scenario
        SiteInformation prefixSi = siteInformationService.getSiteInformationByName("prePrintAltAccessionPrefix");
        if (prefixSi != null) {
            siteInformationService.delete(prefixSi);
        }
        SiteInformation useAltSi = siteInformationService.getSiteInformationByName("prePrintUseAltAccession");
        if (useAltSi != null) {
            siteInformationService.delete(useAltSi);
        }
        // After deletion reload so the cache reflects the absent state
        ConfigurationProperties.loadDBValuesIntoConfiguration();

        // GET must still succeed (controller must handle missing rows gracefully)
        mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

        // POST creates the rows from scratch
        SiteBarcodePreprintSettings body = new SiteBarcodePreprintSettings();
        body.setPrePrintUseAltAccession(true);
        body.setPrePrintAltAccessionPrefix("AB12");
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.prePrintUseAltAccession").value(true))
                .andExpect(jsonPath("$.prePrintAltAccessionPrefix").value("AB12"));

        SiteBarcodePreprintSettings saved = readSettings();
        assertEquals("prefix persisted after creation from absent state", "AB12",
                saved.getPrePrintAltAccessionPrefix());
        assertEquals("useAltAccession persisted after creation from absent state", Boolean.TRUE,
                saved.getPrePrintUseAltAccession());

        // Verify the cache values reflect what was posted
        ConfigurationProperties cfg = ConfigurationProperties.getInstance();
        assertEquals("AB12", cfg.getPropertyValue(Property.ALT_ACCESSION_PREFIX));
        assertEquals("true", cfg.getPropertyValue(Property.USE_ALT_ACCESSION_PREFIX));
    }

    private SiteBarcodePreprintSettings readSettings() throws Exception {
        ConfigurationProperties.loadDBValuesIntoConfiguration();
        MvcResult result = mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andReturn();
        return JSON.readValue(result.getResponse().getContentAsString(), SiteBarcodePreprintSettings.class);
    }
}
