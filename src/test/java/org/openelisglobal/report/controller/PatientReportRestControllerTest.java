package org.openelisglobal.report.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.result.action.util.ResultsLoadUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * Integration test for PatientReportRestController. Verifies that patient
 * reporting data is returned correctly.
 */
@RunWith(SpringRunner.class)
public class PatientReportRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        executeDataSetWithStateManagement("testdata/patient-results-report.xml");

        applicationContext.getBean(ResultsLoadUtility.class);
    }

    @Test
    public void getPatientResults_shouldReturnReportingDataWith13Columns_whenValidPatientIdProvided() throws Exception {
        String patientId = "1";

        mockMvc.perform(MockMvcRequestBuilders.get("/rest/reports/patient/" + patientId + "/results")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(MockMvcResultMatchers.status().isOk())
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
        mockMvc.perform(
                MockMvcRequestBuilders.get("/rest/reports/patient/999/results").contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }
}
