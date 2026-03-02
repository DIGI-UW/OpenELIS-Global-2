package org.openelisglobal.report.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.report.ReportingData;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class PatientReportRestControllerTest extends BaseWebContextSensitiveTest {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/patient-results-report.xml");
    }

    @Test
    public void getPatientResults_shouldReturnPatientReport_whenPatientHasResults() throws Exception {

        MvcResult mvcResult = super.mockMvc.perform(get("/rest/reports/patient/1/results")
                .accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(200, status);

        String content = mvcResult.getResponse().getContentAsString();
        ReportingData data = super.mapFromJson(content, ReportingData.class);

        assertThat(data, notNullValue());
        assertThat(data.getColumns().size(), is(13));
        assertThat(data.getRows().size(), is(1));

        assertThat(data.getRows().get(0).getDataMap().get("accessionNumber"), is("24-00001"));

        assertThat(data.getRows().get(0).getDataMap().get("patientExternalId"), is("EXT-PAT-001"));

        assertThat(data.getRows().get(0).getDataMap().get("resultValue"), is("5.5"));
    }

    @Test
    public void getPatientResults_shouldReturnNotFound_whenPatientDoesNotExist() throws Exception {

        MvcResult mvcResult = super.mockMvc
                .perform(get("/rest/reports/patient/999999/results").accept(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        assertEquals(404, mvcResult.getResponse().getStatus());
    }

    @Test
    public void getPatientResults_shouldReturnNotFound_whenInvalidPatientIdProvided() throws Exception {

        assertEquals(404,
                super.mockMvc.perform(get("/rest/reports/patient/-1/results")).andReturn().getResponse().getStatus());

        assertEquals(404,
                super.mockMvc.perform(get("/rest/reports/patient/0/results")).andReturn().getResponse().getStatus());
    }

    @Test
    public void getPatientResults_shouldReturnJsonContentType() throws Exception {

        MvcResult mvcResult = super.mockMvc
                .perform(get("/rest/reports/patient/1/results").accept(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        assertEquals(MediaType.APPLICATION_JSON_VALUE, mvcResult.getResponse().getContentType());
    }
}