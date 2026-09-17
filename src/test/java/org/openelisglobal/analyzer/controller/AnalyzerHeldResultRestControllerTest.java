package org.openelisglobal.analyzer.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analyzerimport.service.AnalyzerNormalizedResultImportService;
import org.openelisglobal.analyzerimport.service.AnalyzerNormalizedResultImportSummary;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class AnalyzerHeldResultRestControllerTest {
    private AnalyzerNormalizedResultImportService service;
    private MockMvc mockMvc;
    private MockHttpSession session;

    @Before
    public void setUp() {
        service = mock(AnalyzerNormalizedResultImportService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AnalyzerHeldResultRestController(service)).build();
        UserSessionData user = new UserSessionData();
        user.setSytemUserId(17);
        session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, user);
    }

    @Test
    public void usesPathIdentitiesAndTheAuthenticatedActor() throws Exception {
        when(service.reprocessHeldResult("77", "12", "17"))
                .thenReturn(new AnalyzerNormalizedResultImportSummary("77", 1, 0, 0));
        mockMvc.perform(post("/rest/analyzer/analyzers/77/held-results/12/reprocess").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.analyzerId").value("77"))
                .andExpect(jsonPath("$.resultsHeld").value(0));
        verify(service).reprocessHeldResult("77", "12", "17");
    }

    @Test
    public void rejectsAResultFromAnotherAnalyzer() throws Exception {
        when(service.reprocessHeldResult("77", "12", "17")).thenThrow(new IllegalArgumentException("Wrong owner"));
        mockMvc.perform(post("/rest/analyzer/analyzers/77/held-results/12/reprocess").session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void reportsAnAlreadyProcessedRowAsAConflict() throws Exception {
        when(service.reprocessHeldResult("77", "12", "17")).thenThrow(new IllegalStateException("Not held"));
        mockMvc.perform(post("/rest/analyzer/analyzers/77/held-results/12/reprocess").session(session))
                .andExpect(status().isConflict());
    }
}
