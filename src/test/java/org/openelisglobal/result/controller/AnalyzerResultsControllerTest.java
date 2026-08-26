package org.openelisglobal.result.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class AnalyzerResultsControllerTest extends BaseWebContextSensitiveTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        executeDataSetWithStateManagement("testdata/analyzer-results.xml");
    }

    @Test
    public void showRestAnalyzerResults_ShouldReturnResultList_WhenQueriedByAnalyzerId() throws Exception {
        mockMvc.perform(get("/rest/AnalyzerResults").with(user("admin").roles("ADMIN")).param("id", "2001"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resultList").isArray())
                .andExpect(jsonPath("$.resultList[0].accessionNumber").value("ACC123456"))
                .andExpect(jsonPath("$.resultList[1].importIssueReason").value("UNKNOWN_RESULT_VALUE"))
                .andExpect(jsonPath("$.resultList[1].sourceProfileId").value("genexpert-astm"))
                .andExpect(jsonPath("$.resultList[1].sourceProfileRevision").value(3))
                .andExpect(jsonPath("$.resultList[1].rawTestCode").value("QUAL_RESULT"))
                .andExpect(jsonPath("$.resultList[1].rawResultValue").value("POSITIVE"));
    }

    @Test
    public void showRestAnalyzerResults_RejectsUnrelatedAuthenticatedRole() throws Exception {
        mockMvc.perform(get("/rest/AnalyzerResults").with(user("admin").roles("RESULTS")).param("id", "2001"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void showRestAnalyzerResults_AllowsEstablishedAnalyzerRole() throws Exception {
        mockMvc.perform(get("/rest/AnalyzerResults").with(user("admin").roles("ANALYSER_IMPORT")).param("id", "2001"))
                .andExpect(status().isOk());
    }
}
