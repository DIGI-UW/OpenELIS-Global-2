package org.openelisglobal.labelpreset.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Before
    public void setUp() throws Exception {
        super.setUp();
        ensureLabelsDomainAndBarcodeRows();
        executeDataSetWithStateManagement("testdata/system-user.xml");
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
    }

    private void ensureSiteInfoRow(String name, String defaultValue, SiteInformationDomain domain) {
        if (siteInformationService.getSiteInformationByName(name) != null) {
            return;
        }
        SiteInformation row = new SiteInformation();
        row.setName(name);
        row.setValue(defaultValue);
        row.setValueType("text");
        row.setDomain(domain);
        row.setSysUserId(TEST_SYS_USER_ID);
        siteInformationService.insert(row);
    }

    @Test
    public void getSettings_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    public void getSettings_returnsPrePrintFields() throws Exception {
        MvcResult result = mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andReturn();

        SiteBarcodePreprintSettings settings = JSON.readValue(result.getResponse().getContentAsString(),
                SiteBarcodePreprintSettings.class);
        assertNotNull("Response should not be null", settings);
    }

    @Test
    public void postSettings_savesAndReflectsNewValues() throws Exception {
        SiteBarcodePreprintSettings body = new SiteBarcodePreprintSettings();
        body.setPrePrintUseAltAccession(true);
        body.setPrePrintAltAccessionPrefix("TEST");

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)))
                .andExpect(status().isOk());

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
                .andExpect(status().isOk());

        // Second save (update)
        SiteBarcodePreprintSettings body2 = new SiteBarcodePreprintSettings();
        body2.setPrePrintUseAltAccession(true);
        body2.setPrePrintAltAccessionPrefix("SCND");
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body2)))
                .andExpect(status().isOk());

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
            assertEquals(good, readSettings().getPrePrintAltAccessionPrefix());
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

    private SiteBarcodePreprintSettings readSettings() throws Exception {
        ConfigurationProperties.loadDBValuesIntoConfiguration();
        MvcResult result = mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andReturn();
        return JSON.readValue(result.getResponse().getContentAsString(), SiteBarcodePreprintSettings.class);
    }
}
