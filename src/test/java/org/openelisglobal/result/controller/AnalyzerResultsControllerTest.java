package org.openelisglobal.result.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;

public class AnalyzerResultsControllerTest extends BaseWebContextSensitiveTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/analyzer-results.xml");
    }

    @Test
    public void showRestAnalyzerResults_ShouldReturnResultList_WhenQueriedByAnalyzerId() throws Exception {
        mockMvc.perform(get("/rest/AnalyzerResults").param("id", "2001")).andExpect(status().isOk())
                .andExpect(jsonPath("$.resultList").isArray())
                .andExpect(jsonPath("$.resultList[0].accessionNumber").value("ACC123456"))
                .andExpect(jsonPath("$.resultList[1].importIssueReason").value("UNKNOWN_RESULT_VALUE"))
                .andExpect(jsonPath("$.resultList[1].sourceProfileId").value("genexpert-astm"))
                .andExpect(jsonPath("$.resultList[1].sourceProfileRevision").value(3))
                .andExpect(jsonPath("$.resultList[1].rawTestCode").value("QUAL_RESULT"))
                .andExpect(jsonPath("$.resultList[1].rawResultValue").value("POSITIVE"));
    }
}
