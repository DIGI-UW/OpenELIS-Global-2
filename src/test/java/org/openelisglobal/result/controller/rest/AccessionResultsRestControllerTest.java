package org.openelisglobal.result.controller.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;

public class AccessionResultsRestControllerTest extends BaseWebContextSensitiveTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/result.xml");
    }

    @Test
    public void getAccessionResults_ShouldReturnSampleResults_WhenAccessionExists() throws Exception {
        mockMvc.perform(get("/rest/accession-results").param("accessionNumber", "13333"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessionNumber").value("13333"))
                .andExpect(jsonPath("$.searchFinished").value(true))
                .andExpect(jsonPath("$.testResult").isArray())
                .andExpect(jsonPath("$.testResult[0].accessionNumber").value("13333"));
    }

    @Test
    public void getAccessionResults_ShouldReturnNotFoundError_WhenAccessionDoesNotExist() throws Exception {
        mockMvc.perform(get("/rest/accession-results").param("accessionNumber", "DOES_NOT_EXIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessionNumber").value("DOES_NOT_EXIST"))
                .andExpect(jsonPath("$.searchFinished").value(false))
                .andExpect(jsonPath("$.error").value("sample.edit.sample.notFound"))
                .andExpect(jsonPath("$.testResult").isArray());
    }
}
