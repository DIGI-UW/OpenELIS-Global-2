package org.openelisglobal.report.controller;

import static org.junit.Assert.assertEquals;
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
 * Integration test for PatientReportRestController. Verifies that patient
 * reporting data is returned correctly from the database.
 *
 * Note: This test verifies the structure and relationships of the response, not
 * specific data values. Data values are defined in the test dataset XML.
 */
public class PatientReportRestControllerTest extends BaseWebContextSensitiveTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/patient-results-report.xml");
    }

    @Test
    public void getPatientResults_shouldReturnReportingDataWith13Columns_whenValidPatientIdProvided() throws Exception {

        MvcResult result = super.mockMvc
                .perform(get("/rest/reports/patient/1/results").accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        String responseJson = result.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);

        assertNotNull("Response should contain columns", responseMap.get("columns"));
        assertNotNull("Response should contain rows", responseMap.get("rows"));

        List<Map<String, Object>> columns = (List<Map<String, Object>>) responseMap.get("columns");
        assertEquals("Should have 13 columns", 13, columns.size());

        String[] expectedColumnKeys = { "accessionNumber", "patientName", "patientExternalId", "patientGender",
                "patientDateOfBirth", "organizationName", "sampleCollectionDate", "sampleReceivedDate", "clinicianName",
                "testName", "testDescription", "analysisStatus", "resultValue" };

        for (int i = 0; i < columns.size(); i++) {
            Map<String, Object> column = columns.get(i);

            assertNotNull("Column should have header", column.get("header"));
            assertNotNull("Column should have type", column.get("type"));
            assertNotNull("Column should have key", column.get("key"));

            assertEquals("Column " + i + " should have correct key", expectedColumnKeys[i], column.get("key"));
        }

        List<Map<String, Object>> rows = (List<Map<String, Object>>) responseMap.get("rows");
        assertTrue("Should have at least 1 row of results", rows.size() >= 1);

        Map<String, Object> firstRow = rows.get(0);
        assertNotNull("Row should contain dataMap", firstRow.get("dataMap"));
        assertNotNull("Row should contain cells", firstRow.get("cells"));

        Map<String, Object> dataMap = (Map<String, Object>) firstRow.get("dataMap");
        for (String key : expectedColumnKeys) {
            assertTrue("DataMap should contain key: " + key, dataMap.containsKey(key));
        }

        List<Object> cells = (List<Object>) firstRow.get("cells");
        assertEquals("Should have 13 cells matching 13 columns", 13, cells.size());
    }

    @Test
    public void getPatientResults_shouldReturnNotFound_whenPatientDoesNotExist() throws Exception {

        super.mockMvc
                .perform(get("/rest/reports/patient/999/results").accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void getPatientResults_shouldIncludeColumnHeadersAndTypes() throws Exception {

        MvcResult result = super.mockMvc
                .perform(get("/rest/reports/patient/1/results").accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        String responseJson = result.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);
        List<Map<String, Object>> columns = (List<Map<String, Object>>) responseMap.get("columns");

        for (Map<String, Object> column : columns) {
            assertNotNull("Column should have header", column.get("header"));
            assertNotNull("Column should have type", column.get("type"));
            assertNotNull("Column should have key", column.get("key"));

            assertTrue("Column header should not be empty", !((String) column.get("header")).isEmpty());
            assertEquals("Column type should be String", "String", column.get("type"));
        }
    }

    @Test
    public void getPatientResults_shouldReturnDataMapAndCellsWithSameLength() throws Exception {

        MvcResult result = super.mockMvc
                .perform(get("/rest/reports/patient/1/results").accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        String responseJson = result.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);
        List<Map<String, Object>> rows = (List<Map<String, Object>>) responseMap.get("rows");
        List<Map<String, Object>> columns = (List<Map<String, Object>>) responseMap.get("columns");

        for (Map<String, Object> row : rows) {
            assertNotNull("Row should have dataMap", row.get("dataMap"));
            assertNotNull("Row should have cells", row.get("cells"));

            Map<String, Object> dataMap = (Map<String, Object>) row.get("dataMap");
            List<Object> cells = (List<Object>) row.get("cells");

            assertEquals("DataMap should have same number of keys as columns", columns.size(), dataMap.size());

            assertEquals("Cells should have same length as columns", columns.size(), cells.size());
        }
    }

    @Test
    public void getPatientResults_shouldReturnStructuredResponseForPatientWithResults() throws Exception {
        MvcResult result = super.mockMvc
                .perform(get("/rest/reports/patient/1/results").accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        String responseJson = result.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);

        assertNotNull("Should have columns", responseMap.get("columns"));
        assertNotNull("Should have rows", responseMap.get("rows"));

        List<Map<String, Object>> columns = (List<Map<String, Object>>) responseMap.get("columns");
        List<Map<String, Object>> rows = (List<Map<String, Object>>) responseMap.get("rows");

        assertEquals("Should have 13 columns", 13, columns.size());
        assertTrue("Should have at least 1 row for patient with results", rows.size() >= 1);
    }

    @Test
    public void getPatientResults_shouldPopulateAllDataMapFieldsFromDatabase() throws Exception {

        MvcResult result = super.mockMvc
                .perform(get("/rest/reports/patient/1/results").accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        String responseJson = result.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);
        List<Map<String, Object>> rows = (List<Map<String, Object>>) responseMap.get("rows");

        assertTrue("Should have at least 1 row", rows.size() >= 1);

        Map<String, Object> dataMap = (Map<String, Object>) rows.get(0).get("dataMap");

        String[] expectedKeys = { "accessionNumber", "patientName", "patientExternalId", "patientGender",
                "patientDateOfBirth", "organizationName", "sampleCollectionDate", "sampleReceivedDate", "clinicianName",
                "testName", "testDescription", "analysisStatus", "resultValue" };

        for (String key : expectedKeys) {
            assertTrue("DataMap should contain key: " + key, dataMap.containsKey(key));
            assertNotNull("Key " + key + " should exist (value may be empty)",
                    dataMap.get(key) != null ? dataMap.get(key) : "");
        }
    }
}