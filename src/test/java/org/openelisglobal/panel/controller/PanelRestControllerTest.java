package org.openelisglobal.panel.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.panel.form.PanelCreateForm;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

public class PanelRestControllerTest extends BaseWebContextSensitiveTest {

    private static final String TEST_PANEL_ID = "1";

    @Autowired
    private PanelService panelService;

    private ObjectMapper objectMapper;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/panel.xml");
        objectMapper = new ObjectMapper();
        assertNotNull(TEST_PANEL_ID);
    }

    @Test
    public void list_shouldReturnActivePanels() throws Exception {
        mockMvc.perform(get("/rest/panel?active=true").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    public void get_shouldReturnPanelFromDataset() throws Exception {
        mockMvc.perform(get("/rest/panel/" + TEST_PANEL_ID).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(TEST_PANEL_ID))
                .andExpect(jsonPath("$.name").value("Test Panel"))
                .andExpect(jsonPath("$.description").value("This is a test panel."));
    }

    @Test
    public void getWithIncludeTests_shouldReturnEmptyTestsArray() throws Exception {
        mockMvc.perform(
                get("/rest/panel/" + TEST_PANEL_ID + "?includeTests=true").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tests").isArray())
                .andExpect(jsonPath("$.tests.length()").value(0));
    }

    @Test
    public void update_shouldUpdateDescription() throws Exception {
        Panel existing = panelService.getPanelByName("Test Panel");
        assertNotNull("Panel must exist in dataset", existing);

        PanelCreateForm form = new PanelCreateForm();
        form.setName(existing.getPanelName());
        form.setCode(existing.getCode());
        form.setDescription("Updated description");
        form.setLoincCode(existing.getLoinc());
        form.setActive(true);
        form.setLabUnitIds(List.of());
        form.setSampleTypeIds(List.of());

        mockMvc.perform(put("/rest/panel/{id}", existing.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form))).andExpect(status().isOk());

        Panel updated = panelService.getPanelById(existing.getId());
        assertNotNull(updated);
        assertEquals("Updated description", updated.getDescription());
    }

    @Test
    public void getLabUnits_shouldReturnEmptyArray() throws Exception {
        mockMvc.perform(get("/rest/panel/" + TEST_PANEL_ID + "/lab-units").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void updateLabUnits_shouldAcceptEmptyArray() throws Exception {
        mockMvc.perform(put("/rest/panel/" + TEST_PANEL_ID + "/lab-units").contentType(MediaType.APPLICATION_JSON)
                .content("[]")).andExpect(status().isOk());

        mockMvc.perform(get("/rest/panel/" + TEST_PANEL_ID + "/lab-units").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void getSampleTypes_shouldReturnEmptyArray() throws Exception {
        mockMvc.perform(get("/rest/panel/" + TEST_PANEL_ID + "/sample-types").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void updateSampleTypes_shouldAcceptEmptyArray() throws Exception {
        mockMvc.perform(put("/rest/panel/" + TEST_PANEL_ID + "/sample-types").contentType(MediaType.APPLICATION_JSON)
                .content("[]")).andExpect(status().isOk());

        mockMvc.perform(get("/rest/panel/" + TEST_PANEL_ID + "/sample-types").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void getPanelTests_shouldReturnEmptyArray() throws Exception {
        mockMvc.perform(get("/rest/panel/" + TEST_PANEL_ID + "/tests").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void exportPanels_shouldSucceed() throws Exception {
        String body = """
                {
                  "panelIds": ["1"],
                  "format": "json",
                  "includeTests": true
                }
                """;

        mockMvc.perform(post("/rest/panel/export").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    public void validateImport_shouldReturnOk() throws Exception {
        mockMvc.perform(post("/rest/panel/import/validate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"create\",\"data\":{}}")).andExpect(status().isOk());
    }

    @Test
    public void executeImport_shouldReturnOk() throws Exception {
        mockMvc.perform(post("/rest/panel/import").contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"create\",\"data\":{}}")).andExpect(status().isOk());
    }
}
