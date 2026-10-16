package org.openelisglobal.labelpreset.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.labelpreset.controller.rest.SiteWideBarcodeSettingsRestController.SiteBarcodePreprintSettings;
import org.openelisglobal.siteinformation.service.SiteInformationDomainService;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.siteinformation.valueholder.SiteInformationDomain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for
 * {@link org.openelisglobal.labelpreset.controller.rest.SiteWideBarcodeSettingsRestController}
 * (OGC-285 M3).
 *
 * <p>
 * Real service + real DB; no @MockBean of code-under-test.
 */
public class SiteWideBarcodeSettingsRestControllerTest extends BaseWebContextSensitiveTest {

    private static final String BASE_URL = "/api/siteSettings/barcode";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private SiteInformationService siteInformationService;

    @Autowired
    private SiteInformationDomainService siteInformationDomainService;

    private String capturedAltAccessionPrefix;
    private String capturedUseAltAccession;
    private boolean capturedAltAccessionPrefixExisted;
    private boolean capturedUseAltAccessionExisted;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        captureCurrentSettings();
        resetSettingsToDefaults();
        executeDataSetWithStateManagement("testdata/system-user.xml");
        ConfigurationProperties.loadDBValuesIntoConfiguration();
    }

    @After
    public void tearDown() throws Exception {
        restoreCapturedSettings();
        ConfigurationProperties.loadDBValuesIntoConfiguration();
    }

    private void captureCurrentSettings() {
        SiteInformation prefixSi = siteInformationService.getSiteInformationByName("prePrintAltAccessionPrefix");
        if (prefixSi != null) {
            capturedAltAccessionPrefixExisted = true;
            capturedAltAccessionPrefix = prefixSi.getValue();
        } else {
            capturedAltAccessionPrefixExisted = false;
            capturedAltAccessionPrefix = null;
        }

        SiteInformation useAltSi = siteInformationService.getSiteInformationByName("prePrintUseAltAccession");
        if (useAltSi != null) {
            capturedUseAltAccessionExisted = true;
            capturedUseAltAccession = useAltSi.getValue();
        } else {
            capturedUseAltAccessionExisted = false;
            capturedUseAltAccession = null;
        }
    }

    private void resetSettingsToDefaults() {
        setSiteInfoRow("prePrintAltAccessionPrefix", "");
        setSiteInfoRow("prePrintUseAltAccession", "false");
    }

    private void restoreCapturedSettings() {
        restoreSetting("prePrintAltAccessionPrefix", capturedAltAccessionPrefixExisted, capturedAltAccessionPrefix);
        restoreSetting("prePrintUseAltAccession", capturedUseAltAccessionExisted, capturedUseAltAccession);
    }

    private void restoreSetting(String name, boolean existed, String originalValue) {
        SiteInformation si = siteInformationService.getSiteInformationByName(name);
        if (existed) {
            setSiteInfoRow(name, originalValue);
        } else if (si != null) {
            siteInformationService.delete(si);
        }
    }

    private void setSiteInfoRow(String name, String value) {
        SiteInformationDomain labelsDomain = siteInformationDomainService.getByName("labels");
        if (labelsDomain == null) {
            labelsDomain = new SiteInformationDomain();
            labelsDomain.setName("labels");
            labelsDomain.setDescription("Barcode/label configuration");
            siteInformationDomainService.insert(labelsDomain);
        }
        SiteInformation row = siteInformationService.getSiteInformationByName(name);
        boolean isNew = (row == null);
        if (isNew) {
            row = new SiteInformation();
            row.setName(name);
            row.setValueType("text");
            row.setDomain(labelsDomain);
        }
        row.setValue(value);
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

    private SiteBarcodePreprintSettings readSettings() throws Exception {
        ConfigurationProperties.loadDBValuesIntoConfiguration();
        MvcResult result = mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andReturn();
        return JSON.readValue(result.getResponse().getContentAsString(), SiteBarcodePreprintSettings.class);
    }
}
