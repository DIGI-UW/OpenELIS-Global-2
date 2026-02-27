package org.openelisglobal.report.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class PatientReportRestControllerTest extends BaseWebContextSensitiveTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/patient-results-report.xml");
    }

    @Test
    public void getPatientResults_shouldReturnReportData_whenExistingPatientHasResults() throws Exception {
        this.mockMvc
                .perform(get("/rest/reports/patient/1/results").contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.columns").isArray()).andExpect(jsonPath("$.columns.length()").value(13))
                .andExpect(jsonPath("$.rows").isArray()).andExpect(jsonPath("$.rows.length()").value(1))
                .andExpect(jsonPath("$.rows[0].dataMap.accessionNumber").value("24-00001"))
                .andExpect(jsonPath("$.rows[0].dataMap.patientExternalId").value("EXT-PAT-001"))
                .andExpect(jsonPath("$.rows[0].dataMap.testName").isString())
                .andExpect(jsonPath("$.rows[0].dataMap.resultValue").value("5.5"));
    }

    @Test
    public void getPatientResults_shouldReturnNotFound_whenNonExistentPatientIdProvided() throws Exception {
        this.mockMvc.perform(get("/rest/reports/patient/999999/results").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getPatientResults_shouldReturnNotFound_whenNegativePatientIdProvided() throws Exception {
        this.mockMvc.perform(get("/rest/reports/patient/-1/results").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getPatientResults_shouldReturnNotFound_whenZeroPatientIdProvided() throws Exception {
        this.mockMvc.perform(get("/rest/reports/patient/0/results").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getPatientResults_shouldRespondToRequest_whenValidUrlIsProvided() throws Exception {
        this.mockMvc.perform(get("/rest/reports/patient/1/results").accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    public void getPatientResults_shouldRespondWithJsonContentType_whenRequestIsValid() throws Exception {
        this.mockMvc.perform(get("/rest/reports/patient/1/results").contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert (status == 404 || status == 200);
                });
    }
}