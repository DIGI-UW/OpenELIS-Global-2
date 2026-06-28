package org.openelisglobal.analyzer.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.service.AnalyzerBridgeSyncService;
import org.openelisglobal.analyzer.service.AnalyzerQcRuleService;
import org.openelisglobal.analyzer.valueholder.AnalyzerQcRule;
import org.openelisglobal.analyzer.valueholder.AnalyzerQcRule.RuleType;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerQcRuleRestControllerTest {

    @Mock
    private AnalyzerQcRuleService analyzerQcRuleService;

    @Mock
    private AnalyzerBridgeSyncService analyzerBridgeSyncService;

    @InjectMocks
    private AnalyzerQcRuleRestController controller;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void createQcRule_pushesUpdatedBridgeRegistration() throws Exception {
        AnalyzerQcRule rule = rule("rule-1");
        when(analyzerQcRuleService.createRule(eq("7"), any(AnalyzerQcRule.class), eq("1"))).thenReturn(rule);

        MvcResult result = mockMvc.perform(post("/rest/analyzer/analyzers/7/qc-rules")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, userSession()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"ruleType\":\"SPECIMEN_ID_PREFIX\",\"operand\":\"QC\",\"isActive\":true}"))
                .andExpect(status().isCreated()).andReturn();

        assertEquals(201, result.getResponse().getStatus());
        verify(analyzerBridgeSyncService).pushAnalyzer("7");
    }

    @Test
    public void updateQcRule_pushesUpdatedBridgeRegistration() throws Exception {
        AnalyzerQcRule rule = rule("rule-1");
        when(analyzerQcRuleService.updateRule(eq("7"), eq("rule-1"), any(AnalyzerQcRule.class), eq("1")))
                .thenReturn(rule);

        MvcResult result = mockMvc.perform(put("/rest/analyzer/analyzers/7/qc-rules/rule-1")
                .sessionAttr(IActionConstants.USER_SESSION_DATA, userSession()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"ruleType\":\"SPECIMEN_ID_PREFIX\",\"operand\":\"Q\",\"isActive\":true}"))
                .andExpect(status().isOk()).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        verify(analyzerBridgeSyncService).pushAnalyzer("7");
    }

    @Test
    public void deleteQcRule_pushesUpdatedBridgeRegistration() throws Exception {
        MvcResult result = mockMvc.perform(delete("/rest/analyzer/analyzers/7/qc-rules/rule-1"))
                .andExpect(status().isNoContent()).andReturn();

        assertEquals(204, result.getResponse().getStatus());
        verify(analyzerQcRuleService).deleteRule("7", "rule-1");
        verify(analyzerBridgeSyncService).pushAnalyzer("7");
    }

    private AnalyzerQcRule rule(String id) {
        AnalyzerQcRule rule = new AnalyzerQcRule();
        rule.setId(id);
        rule.setAnalyzerId("7");
        rule.setRuleType(RuleType.SPECIMEN_ID_PREFIX);
        rule.setOperand("QC");
        rule.setActive(true);
        return rule;
    }

    private UserSessionData userSession() {
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(1);
        return sessionData;
    }
}
