package org.openelisglobal.report.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * Integration test for PatientReportRestController.
 *
 * Tests the REST endpoint that retrieves patient lab results in a report
 * format. Uses minimal but complete test data to avoid triggering complex code
 * paths.
 */
public class PatientReportRestControllerTest extends BaseWebContextSensitiveTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Load the complete but minimal test dataset
        executeDataSetWithStateManagement("testdata/patient-results-report.xml");
    }

    /**
     * Verify endpoint returns 200 OK and valid JSON structure. Tests the basic
     * happy path.
     */
    @Test
    public void getPatientResults_shouldReturnOkStatus_whenValidPatientIdProvided() throws Exception {

        MvcResult result = super.mockMvc
                .perform(get("/rest/reports/patient/1/results").accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        String responseJson = result.getResponse().getContentAsString();
        assertNotNull("Response should not be empty", responseJson);
        assertTrue("Response should be valid JSON", responseJson.startsWith("{"));
    }

    /**
     * Verify response contains required top-level structure.
     */
    @Test
    public void getPatientResults_shouldReturnJsonWithRequiredStructure() throws Exception {

        MvcResult result = super.mockMvc
                .perform(get("/rest/reports/patient/1/results").accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        String responseJson = result.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);

        // Verify structure
        assertNotNull("Response should have 'columns' field", responseMap.get("columns"));
        assertNotNull("Response should have 'rows' field", responseMap.get("rows"));
    }

    /**
     * Verify columns array has valid structure.
     */
    @Test
    public void getPatientResults_shouldReturnValidColumnStructure() throws Exception {

        MvcResult result = super.mockMvc
                .perform(get("/rest/reports/patient/1/results").accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        String responseJson = result.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);

        List<Map<String, Object>> columns = (List<Map<String, Object>>) responseMap.get("columns");

        assertNotNull("Columns should not be null", columns);
        assertTrue("Should have at least one column", columns.size() > 0);

        // Verify each column has required fields
        for (int i = 0; i < columns.size(); i++) {
            Map<String, Object> column = columns.get(i);
            assertNotNull("Column " + i + " should have 'header' field", column.get("header"));
            assertNotNull("Column " + i + " should have 'type' field", column.get("type"));
            assertNotNull("Column " + i + " should have 'key' field", column.get("key"));
        }
    }

    /**
     * Verify rows array has valid structure.
     */
    @Test
    public void getPatientResults_shouldReturnValidRowStructure() throws Exception {

        MvcResult result = super.mockMvc
                .perform(get("/rest/reports/patient/1/results").accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        String responseJson = result.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);

        List<Map<String, Object>> rows = (List<Map<String, Object>>) responseMap.get("rows");

        assertNotNull("Rows should not be null", rows);

        // If rows exist, verify each has required fields
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            assertNotNull("Row " + i + " should have 'dataMap' field", row.get("dataMap"));
            assertNotNull("Row " + i + " should have 'cells' field", row.get("cells"));
        }
    }

    /**
     * Verify dataMap and cells array lengths match columns count.
     */
    @Test
    public void getPatientResults_shouldHaveConsistentDataStructure() throws Exception {

        MvcResult result = super.mockMvc
                .perform(get("/rest/reports/patient/1/results").accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        String responseJson = result.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);

        List<Map<String, Object>> columns = (List<Map<String, Object>>) responseMap.get("columns");
        List<Map<String, Object>> rows = (List<Map<String, Object>>) responseMap.get("rows");

        // For each row, verify dataMap and cells sizes match column count
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            Map<String, Object> dataMap = (Map<String, Object>) row.get("dataMap");
            List<Object> cells = (List<Object>) row.get("cells");

            assertTrue("Row " + i + " dataMap size should match columns count: " + dataMap.size() + " vs "
                    + columns.size(), dataMap.size() == columns.size());
            assertTrue("Row " + i + " cells size should match columns count: " + cells.size() + " vs " + columns.size(),
                    cells.size() == columns.size());
        }
    }

    /**
     * Verify endpoint returns 404 for non-existent patient.
     */
    @Test
    public void getPatientResults_shouldReturnNotFound_whenPatientDoesNotExist() throws Exception {

        super.mockMvc
                .perform(get("/rest/reports/patient/999/results").accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }
}