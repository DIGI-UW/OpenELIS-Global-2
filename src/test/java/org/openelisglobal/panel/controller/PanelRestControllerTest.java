package org.openelisglobal.panel.controller;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.panel.form.PanelCreateForm;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;

@Rollback
public class PanelRestControllerTest extends BaseWebContextSensitiveTest {

    private static final String TEST_PANEL_ID = "1";

    @Autowired
    private PanelService panelService;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/panel.xml");
    }

    @Test
    public void list_shouldReturnActivePanels() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/panel?active=true").accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(200, result.getResponse().getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> panels = mapFromJson(result.getResponse().getContentAsString(), List.class);
        assertThat(panels, notNullValue());
        assertThat(panels.isEmpty(), is(false));
    }

    @Test
    public void get_shouldReturnPanelFromDataset() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/panel/" + TEST_PANEL_ID).accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(200, result.getResponse().getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> panel = mapFromJson(result.getResponse().getContentAsString(), Map.class);
        assertThat(panel.get("id"), is(TEST_PANEL_ID));
        assertThat(panel.get("name"), is("Test Panel"));
        assertThat(panel.get("description"), is("This is a test panel."));
    }

    @Test
    public void getWithIncludeTests_shouldReturnEmptyTestsArray() throws Exception {
        MvcResult result = mockMvc
                .perform(get("/rest/panel/" + TEST_PANEL_ID + "?includeTests=true")
                        .accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> panel = mapFromJson(result.getResponse().getContentAsString(), Map.class);
        assertThat(panel.get("tests"), notNullValue());
        assertThat(((List<?>) panel.get("tests")).isEmpty(), is(true));
    }

    @Test
    public void update_shouldUpdateDescription() throws Exception {
        Panel existing = panelService.getPanelByName("Test Panel");
        assertThat(existing, notNullValue());

        PanelCreateForm form = new PanelCreateForm();
        form.setName(existing.getPanelName());
        form.setCode(existing.getCode());
        form.setDescription("Updated description");
        form.setLoincCode(existing.getLoinc());
        form.setActive(true);
        form.setLabUnitIds(List.of("1"));
        form.setSampleTypeIds(new ArrayList<>());

        MvcResult result = mockMvc.perform(put("/rest/panel/{id}", existing.getId())
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(mapToJson(form))).andReturn();

        assertEquals(200, result.getResponse().getStatus());

        Panel updated = panelService.getPanelById(existing.getId());
        assertThat(updated, notNullValue());
        assertThat(updated.getDescription(), is("Updated description"));
    }

    @Test
    public void getLabUnits_shouldReturnEmptyArray() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/panel/" + TEST_PANEL_ID + "/lab-units")
                .accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(200, result.getResponse().getStatus());

        @SuppressWarnings("unchecked")
        List<?> labUnits = mapFromJson(result.getResponse().getContentAsString(), List.class);
        assertThat(labUnits.isEmpty(), is(true));
    }

    @Test
    public void updateLabUnits_shouldAcceptEmptyArray() throws Exception {
        MvcResult putResult = mockMvc
                .perform(put("/rest/panel/" + TEST_PANEL_ID + "/lab-units")
                        .contentType(MediaType.APPLICATION_JSON_VALUE).content("[]"))
                .andExpect(status().isOk()).andReturn();

        assertEquals(200, putResult.getResponse().getStatus());

        MvcResult getResult = mockMvc
                .perform(get("/rest/panel/" + TEST_PANEL_ID + "/lab-units").accept(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<?> labUnits = mapFromJson(getResult.getResponse().getContentAsString(), List.class);
        assertThat(labUnits.isEmpty(), is(true));
    }

    @Test
    public void getSampleTypes_shouldReturnEmptyArray() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/panel/" + TEST_PANEL_ID + "/sample-types")
                .accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(200, result.getResponse().getStatus());

        @SuppressWarnings("unchecked")
        List<?> sampleTypes = mapFromJson(result.getResponse().getContentAsString(), List.class);
        assertThat(sampleTypes.isEmpty(), is(true));
    }

    @Test
    public void updateSampleTypes_shouldAcceptEmptyArray() throws Exception {
        MvcResult putResult = mockMvc
                .perform(put("/rest/panel/" + TEST_PANEL_ID + "/sample-types")
                        .contentType(MediaType.APPLICATION_JSON_VALUE).content("[]"))
                .andExpect(status().isOk()).andReturn();

        assertEquals(200, putResult.getResponse().getStatus());

        MvcResult getResult = mockMvc
                .perform(get("/rest/panel/" + TEST_PANEL_ID + "/sample-types").accept(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<?> sampleTypes = mapFromJson(getResult.getResponse().getContentAsString(), List.class);
        assertThat(sampleTypes.isEmpty(), is(true));
    }

    @Test
    public void getPanelTests_shouldReturnEmptyArray() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/panel/" + TEST_PANEL_ID + "/tests")
                .accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(200, result.getResponse().getStatus());

        @SuppressWarnings("unchecked")
        List<?> tests = mapFromJson(result.getResponse().getContentAsString(), List.class);
        assertThat(tests.isEmpty(), is(true));
    }

    @Test
    public void addPanelTest_withValidBody_shouldReturn200() throws Exception {
        executeDataSetWithStateManagement("testdata/panel-item.xml");

        String body = """
                {
                  "testId": "1",
                  "displayOrder": 1,
                  "panelLoincCode": "PL-CTRL-001"
                }
                """;

        MvcResult result = mockMvc
                .perform(post("/rest/panel/3/tests").contentType(MediaType.APPLICATION_JSON_VALUE).content(body))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());
    }

    @Test
    public void list_withSearchParam_shouldReturnMatchingPanels() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/panel?search=Test").accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(200, result.getResponse().getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> panels = mapFromJson(result.getResponse().getContentAsString(), List.class);
        assertThat(panels.isEmpty(), is(false));
    }

    @Test
    public void list_withNonMatchingSearch_shouldReturnEmptyArray() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/panel?search=ZZZNOMATCH99999")
                .accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(200, result.getResponse().getStatus());

        @SuppressWarnings("unchecked")
        List<?> panels = mapFromJson(result.getResponse().getContentAsString(), List.class);
        assertThat(panels.isEmpty(), is(true));
    }

    @Test
    public void duplicate_shouldReturn201WithCopyInName() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/rest/panel/" + TEST_PANEL_ID + "/duplicate").contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        assertEquals(201, result.getResponse().getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> created = mapFromJson(result.getResponse().getContentAsString(), Map.class);
        assertThat((String) created.get("name"), containsString("(Copy)"));
        assertThat(created.get("active"), is(false));
    }

    @Test
    public void duplicate_nonExistentPanel_shouldReturn404() throws Exception {
        MvcResult result = mockMvc
                .perform(post("/rest/panel/99999999/duplicate").contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        assertEquals(404, result.getResponse().getStatus());
    }

    @Test
    public void create_withEmptyLabUnitIds_shouldReturn400() throws Exception {
        PanelCreateForm form = new PanelCreateForm();
        form.setName("No Lab Unit Panel");
        form.setCode("NLU-001");
        form.setActive(true);
        form.setLabUnitIds(new ArrayList<>());
        form.setSampleTypeIds(new ArrayList<>());

        MvcResult result = mockMvc
                .perform(post("/rest/panel").contentType(MediaType.APPLICATION_JSON_VALUE).content(mapToJson(form)))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
    }

    @Test
    public void exportPanels_json_shouldReturnPanelsKey() throws Exception {
        String body = """
                {
                  "panelIds": ["1"],
                  "format": "json",
                  "includeTests": true
                }
                """;

        MvcResult result = mockMvc
                .perform(post("/rest/panel/export").contentType(MediaType.APPLICATION_JSON_VALUE).content(body))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> response = mapFromJson(result.getResponse().getContentAsString(), Map.class);
        assertThat(response.get("panels"), notNullValue());
        assertThat(response.get("count"), is(1));
        assertThat(response.get("exportedAt"), notNullValue());
    }

    @Test
    public void exportPanels_csv_shouldReturnStringContent() throws Exception {
        String body = """
                {
                  "panelIds": ["1"],
                  "format": "csv",
                  "includeTests": false
                }
                """;

        MvcResult result = mockMvc
                .perform(post("/rest/panel/export").contentType(MediaType.APPLICATION_JSON_VALUE).content(body))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());
        assertTrue("CSV should start with header",
                result.getResponse().getContentAsString().startsWith("panelId,panelName,panelCode"));
    }

    @Test
    public void validateImport_shouldReturnOk() throws Exception {
        MvcResult result = mockMvc.perform(post("/rest/panel/import/validate")
                .contentType(MediaType.APPLICATION_JSON_VALUE).content("{\"mode\":\"create\",\"data\":{}}"))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());
    }

    @Test
    public void validateImport_withPanelList_shouldReturnPreviewArray() throws Exception {
        String body = """
                {
                  "mode": "create",
                  "data": [
                    {"code": "CTRL-VLD-001", "name": "Controller Validate Panel"}
                  ]
                }
                """;

        MvcResult result = mockMvc
                .perform(
                        post("/rest/panel/import/validate").contentType(MediaType.APPLICATION_JSON_VALUE).content(body))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> response = mapFromJson(result.getResponse().getContentAsString(), Map.class);
        assertThat(response.get("preview"), notNullValue());
        assertThat(((List<?>) response.get("preview")).isEmpty(), is(false));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> preview = (List<Map<String, Object>>) response.get("preview");
        assertThat(preview.get(0).get("action"), is("create"));

        @SuppressWarnings("unchecked")
        Map<String, Object> counts = (Map<String, Object>) response.get("counts");
        assertThat(counts.get("create"), is(1));
    }

    @Test
    public void executeImport_shouldReturnOk() throws Exception {
        MvcResult result = mockMvc.perform(post("/rest/panel/import").contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"mode\":\"create\",\"data\":{}}")).andReturn();

        assertEquals(200, result.getResponse().getStatus());
    }

    @Test
    public void executeImport_withNewPanel_shouldReturnCreatedCount() throws Exception {
        String body = """
                {
                  "mode": "create",
                  "data": [
                    {"code": "CTRL-EXEC-001", "name": "Controller Exec Panel 1"}
                  ]
                }
                """;

        MvcResult result = mockMvc
                .perform(post("/rest/panel/import").contentType(MediaType.APPLICATION_JSON_VALUE).content(body))
                .andReturn();

        assertEquals(200, result.getResponse().getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> response = mapFromJson(result.getResponse().getContentAsString(), Map.class);
        assertThat(response.get("panelsCreated"), is(1));
        assertThat(response.get("panelsUpdated"), is(0));
    }
}
