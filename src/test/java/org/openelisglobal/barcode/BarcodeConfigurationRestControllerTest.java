package org.openelisglobal.barcode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.barcode.form.BarcodeConfigurationForm;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.siteinformation.service.SiteInformationDomainService;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.siteinformation.valueholder.SiteInformationDomain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class BarcodeConfigurationRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SiteInformationService siteInformationService;
    @Autowired
    private SiteInformationDomainService siteInformationDomainService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        ensureBarcodeLabelDomainExists();
        ensureBarcodeLabelQuantityRowsExist();
        executeDataSetWithStateManagement("testdata/system-user.xml");
    }

    private void ensureBarcodeLabelDomainExists() {
        SiteInformationDomain labelsDomain = siteInformationDomainService.getByName("labels");
        if (labelsDomain != null) {
            return;
        }

        SiteInformationDomain domain = new SiteInformationDomain();
        domain.setName("labels");
        domain.setDescription("items that pertain to barcodes/labels");
        siteInformationDomainService.insert(domain);
    }

    // Liquibase changeset 2.5.x.x/barcode_additional_info.xml installs the
    // numMax*/numDefault* site_information rows on context startup, but sibling
    // tests (BarcodeConfigServiceTest, SiteInformationServiceTest) load fixtures
    // whose <site_information> elements cause cleanRowsInCurrentConnection to
    // TRUNCATE site_information RESTART IDENTITY CASCADE, wiping those rows.
    // Re-seed defensively here so this test class is order-independent.
    private void ensureBarcodeLabelQuantityRowsExist() {
        SiteInformationDomain labelsDomain = siteInformationDomainService.getByName("labels");
        ensureSiteInformationRow("numMaxOrderLabels", "5000", labelsDomain);
        ensureSiteInformationRow("numMaxSpecimenLabels", "5000", labelsDomain);
        ensureSiteInformationRow("numMaxAliquotLabels", "5000", labelsDomain);
        ensureSiteInformationRow("numDefaultOrderLabels", "1", labelsDomain);
        ensureSiteInformationRow("numDefaultSpecimenLabels", "1", labelsDomain);
        ensureSiteInformationRow("numDefaultAliquotLabels", "1", labelsDomain);
    }

    private void ensureSiteInformationRow(String name, String defaultValue, SiteInformationDomain domain) {
        if (siteInformationService.getSiteInformationByName(name) != null) {
            return;
        }
        SiteInformation row = new SiteInformation();
        row.setName(name);
        row.setValue(defaultValue);
        row.setValueType("text");
        row.setDomain(domain);
        row.setSysUserId("1");
        siteInformationService.insert(row);
    }

    private void applyValidDimensions(BarcodeConfigurationForm form) {
        form.setHeightOrderLabels(10.0f);
        form.setWidthOrderLabels(5.0f);
        form.setHeightSpecimenLabels(10.0f);
        form.setWidthSpecimenLabels(5.0f);
        form.setHeightBlockLabels(10.0f);
        form.setWidthBlockLabels(5.0f);
        form.setHeightSlideLabels(10.0f);
        form.setWidthSlideLabels(5.0f);
        form.setHeightFreezerLabels(10.0f);
        form.setWidthFreezerLabels(5.0f);
    }

    @Test
    public void showBarcodeConfiguration() throws Exception {
        MvcResult urlResult = super.mockMvc.perform(get("/rest/BarcodeConfiguration")
                .accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        String formJson = urlResult.getResponse().getContentAsString();

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> formMap = objectMapper.readValue(formJson, new TypeReference<Map<String, Object>>() {
        });
        assertEquals("BarcodeConfigurationForm", formMap.get("formName"));
        assertEquals("MasterListsPage", formMap.get("cancelAction"));
        assertEquals("POST", formMap.get("cancelMethod"));
    }

    @Test
    public void barcodeConfigurationPartialUpdate() throws Exception {

        BarcodeConfigurationForm initialForm = new BarcodeConfigurationForm();
        initialForm.setNumMaxOrderLabels(100);
        initialForm.setNumMaxSpecimenLabels(200);
        initialForm.setPrePrintAltAccessionPrefix("INITIAL");
        initialForm.setPrePrintDontUseAltAccession(true);
        applyValidDimensions(initialForm);

        String initialJson = new ObjectMapper().writeValueAsString(initialForm);
        super.mockMvc.perform(
                post("/rest/BarcodeConfiguration").contentType(MediaType.APPLICATION_JSON_VALUE).content(initialJson));

        BarcodeConfigurationForm updateForm = new BarcodeConfigurationForm();
        updateForm.setNumMaxOrderLabels(150);
        updateForm.setNumMaxSpecimenLabels(200);
        updateForm.setPrePrintAltAccessionPrefix("INITIAL");
        updateForm.setPrePrintDontUseAltAccession(true);
        applyValidDimensions(updateForm);

        String updateJson = new ObjectMapper().writeValueAsString(updateForm);
        super.mockMvc.perform(
                post("/rest/BarcodeConfiguration").contentType(MediaType.APPLICATION_JSON_VALUE).content(updateJson));

        ConfigurationProperties.loadDBValuesIntoConfiguration();

        MvcResult result = super.mockMvc.perform(get("/rest/BarcodeConfiguration")).andReturn();
        BarcodeConfigurationForm retrievedForm = new ObjectMapper().readValue(result.getResponse().getContentAsString(),
                BarcodeConfigurationForm.class);

        assertEquals(150, retrievedForm.getNumMaxOrderLabels());
        assertEquals(200, retrievedForm.getNumMaxSpecimenLabels());
        assertEquals("INITIAL", retrievedForm.getPrePrintAltAccessionPrefix());
    }

    @Test
    public void BarcodeConfigurationSave() throws Exception {
        BarcodeConfigurationForm form = new BarcodeConfigurationForm();

        form.setNumMaxOrderLabels(100);
        form.setNumMaxSpecimenLabels(200);
        form.setNumMaxAliquotLabels(300);
        form.setNumDefaultOrderLabels(50);
        form.setNumDefaultSpecimenLabels(100);
        form.setNumDefaultAliquotLabels(150);
        form.setHeightOrderLabels(10.5f);
        form.setWidthOrderLabels(5.5f);
        form.setHeightSpecimenLabels(12.0f);
        form.setWidthSpecimenLabels(6.0f);
        form.setHeightBlockLabels(8.0f);
        form.setWidthBlockLabels(4.0f);
        form.setHeightSlideLabels(7.0f);
        form.setWidthSlideLabels(3.5f);
        form.setHeightFreezerLabels(7.0f);
        form.setWidthFreezerLabels(3.5f);
        form.setSpecimenCollectionDateCheck(true);
        form.setSpecimenCollectedByCheck(false);
        form.setSpecimenTestsCheck(true);
        form.setSpecimenPatientSexCheck(false);
        form.setPrePrintDontUseAltAccession(true);
        form.setPrePrintAltAccessionPrefix("ABCD");

        String configurationJson = new ObjectMapper().writeValueAsString(form);

        super.mockMvc.perform(post("/rest/BarcodeConfiguration").contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE).content(configurationJson)).andReturn();

        ConfigurationProperties.loadDBValuesIntoConfiguration();

        MvcResult urlResults = super.mockMvc.perform(get("/rest/BarcodeConfiguration")
                .accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        String formJson = urlResults.getResponse().getContentAsString();

        ObjectMapper objectMapper = new ObjectMapper();
        BarcodeConfigurationForm retrievedForm = objectMapper.readValue(formJson, BarcodeConfigurationForm.class);

        assertEquals(100, retrievedForm.getNumMaxOrderLabels());
        assertEquals(200, retrievedForm.getNumMaxSpecimenLabels());
        assertEquals("ABCD", retrievedForm.getPrePrintAltAccessionPrefix());
    }

    @Test
    public void saveBarcodeConfiguration_ShouldRejectEmptyAltAccessionWhenRequired() throws Exception {
        BarcodeConfigurationForm form = new BarcodeConfigurationForm();
        form.setPrePrintDontUseAltAccession(false);
        form.setPrePrintAltAccessionPrefix("");

        mockMvc.perform(post("/rest/BarcodeConfiguration").contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(form))).andExpect(status().isOk()) // this is now correct
                .andExpect(model().attributeHasFieldErrorCode("barcodeConfigurationForm", "prePrintAltAccessionPrefix",
                        "error.altaccession.required"));
    }

    @Test
    public void saveBarcodeConfiguration_ShouldRedirectOnSuccess() throws Exception {
        BarcodeConfigurationForm form = new BarcodeConfigurationForm();
        applyValidDimensions(form);
        form.setPrePrintDontUseAltAccession(true);

        mockMvc.perform(post("/rest/BarcodeConfiguration").contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(form))).andExpect(status().isFound())
                .andExpect(redirectedUrl("/rest/BarcodeConfiguration"));
    }

    @Test
    public void saveBarcodeConfiguration_ShouldRejectNegativeLabelCounts() throws Exception {
        BarcodeConfigurationForm form = new BarcodeConfigurationForm();
        form.setNumMaxOrderLabels(-1);
        form.setPrePrintDontUseAltAccession(true);

        mockMvc.perform(post("/rest/BarcodeConfiguration").contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(form))).andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("barcodeConfigurationForm", "numMaxOrderLabels"));
    }

    @Test
    public void saveBarcodeConfiguration_ShouldRejectOversizedLabelCounts() throws Exception {
        BarcodeConfigurationForm form = new BarcodeConfigurationForm();
        form.setNumMaxSpecimenLabels(5001);
        form.setPrePrintDontUseAltAccession(true);

        mockMvc.perform(post("/rest/BarcodeConfiguration").contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(form))).andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("barcodeConfigurationForm", "numMaxSpecimenLabels"));
    }

    @Test
    public void showBarcodeConfiguration_ShouldFallbackForMalformedStoredQuantityValues() throws Exception {
        SiteInformation maxOrder = siteInformationService.getSiteInformationByName("numMaxOrderLabels");
        SiteInformation defaultOrder = siteInformationService.getSiteInformationByName("numDefaultOrderLabels");
        assertNotNull("max order site information should exist", maxOrder);
        assertNotNull("default order site information should exist", defaultOrder);

        maxOrder.setValue("not-a-number");
        maxOrder.setSysUserId(TEST_SYS_USER_ID);
        defaultOrder.setValue("NaN");
        defaultOrder.setSysUserId(TEST_SYS_USER_ID);
        siteInformationService.update(maxOrder);
        siteInformationService.update(defaultOrder);
        ConfigurationProperties.loadDBValuesIntoConfiguration();

        BarcodeConfigurationForm saved = new ObjectMapper().readValue(
                mockMvc.perform(get("/rest/BarcodeConfiguration")).andReturn().getResponse().getContentAsString(),
                BarcodeConfigurationForm.class);

        assertEquals("Malformed max order should fallback to 10", 10, saved.getNumMaxOrderLabels());
        assertEquals("Malformed default order should fallback to 1", 1, saved.getNumDefaultOrderLabels());
    }
}