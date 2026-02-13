package org.openelisglobal.report.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * Integration test for PatientReportRestController. Verifies that the grounded
 * mapping logic correctly extracts results for a patient.
 */
@RunWith(SpringRunner.class)
public class PatientReportRestControllerTest extends BaseWebContextSensitiveTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Load the consolidated dataset for testing patient results
        executeDataSetWithStateManagement("testdata/patient-results-report.xml");
    }

    @Test
    public void getPatientResults_shouldReturnReportingDataWith13Columns_whenValidPatientIdProvided() throws Exception {
        String patientId = "1";

        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/rest/reports/patient/" + patientId + "/results")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.columns").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.columns.length()").value(13))
                .andExpect(MockMvcResultMatchers.jsonPath("$.rows").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.rows.length()").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.rows[0].dataMap.accessionNumber").value("24-00001"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.rows[0].dataMap.patientName").value(" "))
                .andExpect(MockMvcResultMatchers.jsonPath("$.rows[0].dataMap.patientExternalId").value("EXT-PAT-001"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.rows[0].dataMap.organizationName")
                        .value("Test Health Center"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.rows[0].dataMap.clinicianName").value(""))
                .andExpect(MockMvcResultMatchers.jsonPath("$.rows[0].dataMap.resultValue").value("5.5"));
    }

    @Test
    public void getPatientResults_shouldReturnNotFound_whenPatientDoesNotExist() throws Exception {
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/rest/reports/patient/999/results")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }
}
