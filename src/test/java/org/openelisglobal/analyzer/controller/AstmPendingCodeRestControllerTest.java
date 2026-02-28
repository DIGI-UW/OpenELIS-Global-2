package org.openelisglobal.analyzer.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.service.AstmConfigService;
import org.openelisglobal.analyzer.service.AstmPendingCodeService;
import org.openelisglobal.analyzer.service.AstmQcRuleService;
import org.openelisglobal.analyzer.service.AstmTestMappingConfigService;
import org.openelisglobal.analyzer.valueholder.AstmPendingCode;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.class)
public class AstmPendingCodeRestControllerTest {

    @Mock
    private AstmConfigService astmConfigService;

    @Mock
    private AstmQcRuleService astmQcRuleService;

    @Mock
    private AstmTestMappingConfigService astmTestMappingConfigService;

    @Mock
    private AstmPendingCodeService astmPendingCodeService;

    @Mock
    private UserRoleService userRoleService;

    private MockMvc mockMvc;
    private UserSessionData userSessionData;

    @Before
    public void setUp() {
        AstmConfigRestController controller = new AstmConfigRestController();
        ReflectionTestUtils.setField(controller, "astmConfigService", astmConfigService);
        ReflectionTestUtils.setField(controller, "astmQcRuleService", astmQcRuleService);
        ReflectionTestUtils.setField(controller, "astmTestMappingConfigService", astmTestMappingConfigService);
        ReflectionTestUtils.setField(controller, "astmPendingCodeService", astmPendingCodeService);
        ReflectionTestUtils.setField(controller, "userRoleService", userRoleService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
        when(userRoleService.userInRole(anyString(), anyString())).thenReturn(true);
    }

    @Test
    public void pendingCodeEndpoints_GetMapAndStatus_WorkAsExpected() throws Exception {
        AstmPendingCode pending = new AstmPendingCode();
        pending.setId("pc-1");
        pending.setAnalyzerTestName("UNMAPPED1");
        pending.setFirstSeenAt(new Date());
        pending.setLastSeenAt(new Date());
        pending.setSeenCount(2);
        pending.setStatus(AstmPendingCode.Status.PENDING.name());
        pending.setSamplePayload("R|1||^^^UNMAPPED1|12|");

        when(astmPendingCodeService.findPendingByAnalyzerId("1")).thenReturn(List.of(pending));

        mockMvc.perform(get("/rest/analyzer/analyzers/1/pending-codes").contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("userSessionData", userSessionData)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("pc-1"))
                .andExpect(jsonPath("$[0].analyzerTestName").value("UNMAPPED1"));

        mockMvc.perform(
                post("/rest/analyzer/analyzers/1/pending-codes/pc-1/map").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"openelisTestId\":\"test-123\"}").sessionAttr("userSessionData", userSessionData))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("ASTM_PENDING_CODE_RESOLVED"));
        verify(astmPendingCodeService).resolveByMapping("pc-1", "test-123");

        mockMvc.perform(
                put("/rest/analyzer/analyzers/1/pending-codes/pc-1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IGNORED\"}").sessionAttr("userSessionData", userSessionData))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("ASTM_PENDING_CODE_STATUS_UPDATED"));
        verify(astmPendingCodeService).updateStatus("pc-1", AstmPendingCode.Status.IGNORED);
    }

    @Test
    public void mapPendingCode_WithoutOpenElisTestId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/rest/analyzer/analyzers/1/pending-codes/pc-1/map")
                .contentType(MediaType.APPLICATION_JSON).content("{}").sessionAttr("userSessionData", userSessionData))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ASTM_PENDING_CODE_REQUIRED_FIELD"));
    }

    @Test
    public void updatePendingCodeStatus_InvalidStatus_ReturnsBadRequest() throws Exception {
        mockMvc.perform(
                put("/rest/analyzer/analyzers/1/pending-codes/pc-1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BAD_STATUS\"}").sessionAttr("userSessionData", userSessionData))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ASTM_PENDING_CODE_INVALID_STATUS"));
    }
}
