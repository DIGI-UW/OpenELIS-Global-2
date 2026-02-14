package org.openelisglobal.barcode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.barcode.form.BarcodeConfigurationForm;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

public class BarcodeConfigurationRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SiteInformationService siteInformationService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
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

        String initialJson = new ObjectMapper().writeValueAsString(initialForm);
        super.mockMvc.perform(
                post("/rest/BarcodeConfiguration").contentType(MediaType.APPLICATION_JSON_VALUE).content(initialJson));

        BarcodeConfigurationForm updateForm = new BarcodeConfigurationForm();
        updateForm.setNumMaxOrderLabels(150);
        updateForm.setNumMaxSpecimenLabels(200);
        updateForm.setPrePrintAltAccessionPrefix("INITIAL");

        String updateJson = new ObjectMapper().writeValueAsString(updateForm);
        super.mockMvc.perform(
                post("/rest/BarcodeConfiguration").contentType(MediaType.APPLICATION_JSON_VALUE).content(updateJson));

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
        form.setSpecimenCollectionDateCheck(true);
        form.setSpecimenCollectedByCheck(false);
        form.setSpecimenTestsCheck(true);
        form.setSpecimenPatientSexCheck(false);
        form.setPrePrintDontUseAltAccession(true);
        form.setPrePrintAltAccessionPrefix("ABCD");

        String configurationJson = new ObjectMapper().writeValueAsString(form);

        super.mockMvc.perform(post("/rest/BarcodeConfiguration").contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE).content(configurationJson)).andReturn();

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
        form.setHeightOrderLabels(10.0f);
        form.setPrePrintDontUseAltAccession(true);

        mockMvc.perform(post("/rest/BarcodeConfiguration").contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(form))).andExpect(status().isFound())
                .andExpect(redirectedUrl("/rest/BarcodeConfiguration"));
    }

    @Test
    public void saveBarcodeConfiguration_ShouldNormalizeNegativeAndOversizedValues() throws Exception {
        BarcodeConfigurationForm form = new BarcodeConfigurationForm();
        form.setNumMaxOrderLabels(-1);
        form.setNumMaxSpecimenLabels(5001);
        form.setNumDefaultOrderLabels(0);
        form.setPrePrintDontUseAltAccession(true);

        mockMvc.perform(post("/rest/BarcodeConfiguration").contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(form))).andExpect(status().isFound())
                .andExpect(redirectedUrl("/rest/BarcodeConfiguration"));

        BarcodeConfigurationForm saved = new ObjectMapper().readValue(
                mockMvc.perform(get("/rest/BarcodeConfiguration")).andReturn().getResponse().getContentAsString(),
                BarcodeConfigurationForm.class);

        assertEquals("Invalid max order should normalize to default max order", 10, saved.getNumMaxOrderLabels());
        assertEquals("Oversized max specimen should normalize to default max value", 10, saved.getNumMaxSpecimenLabels());
        assertEquals("Zero default order should normalize to default quantity", 1, saved.getNumDefaultOrderLabels());
    }

    @Test
    public void showBarcodeConfiguration_ShouldFallbackForMalformedStoredQuantityValues() throws Exception {
        SiteInformation maxOrder = siteInformationService.getSiteInformationByName("numMaxOrderLabels");
        SiteInformation defaultOrder = siteInformationService.getSiteInformationByName("numDefaultOrderLabels");
        assertNotNull("max order site information should exist", maxOrder);
        assertNotNull("default order site information should exist", defaultOrder);

        maxOrder.setValue("not-a-number");
        defaultOrder.setValue("NaN");
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