package org.openelisglobal.analyzer.controller;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AstmConfigService;
import org.openelisglobal.analyzer.service.AstmPendingCodeService;
import org.openelisglobal.analyzer.service.AstmQcRuleService;
import org.openelisglobal.analyzer.service.AstmTestMappingConfigService;
import org.openelisglobal.analyzer.valueholder.AstmAnalyzerConfig;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

public class AstmConfigAuthorizationIntegrationTest extends BaseWebContextSensitiveTest {

    private AstmConfigService astmConfigServiceMock;
    private AstmQcRuleService astmQcRuleServiceMock;
    private AstmTestMappingConfigService astmTestMappingConfigServiceMock;
    private AstmPendingCodeService astmPendingCodeServiceMock;
    private UserRoleService userRoleServiceMock;
    private UserSessionData userSessionData;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        astmConfigServiceMock = mock(AstmConfigService.class);
        astmQcRuleServiceMock = mock(AstmQcRuleService.class);
        astmTestMappingConfigServiceMock = mock(AstmTestMappingConfigService.class);
        astmPendingCodeServiceMock = mock(AstmPendingCodeService.class);
        userRoleServiceMock = mock(UserRoleService.class);

        AstmConfigRestController controller = webApplicationContext.getBean(AstmConfigRestController.class);
        ReflectionTestUtils.setField(controller, "astmConfigService", astmConfigServiceMock);
        ReflectionTestUtils.setField(controller, "astmQcRuleService", astmQcRuleServiceMock);
        ReflectionTestUtils.setField(controller, "astmTestMappingConfigService", astmTestMappingConfigServiceMock);
        ReflectionTestUtils.setField(controller, "astmPendingCodeService", astmPendingCodeServiceMock);
        ReflectionTestUtils.setField(controller, "userRoleService", userRoleServiceMock);

        userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
    }

    @Test
    public void astmEndpoints_WithNonAdminRole_ReturnForbidden() throws Exception {
        when(userRoleServiceMock.userInRole(anyString(), anyString())).thenReturn(false);

        mockMvc.perform(get("/rest/analyzer/analyzers/1/astm-config").sessionAttr("userSessionData", userSessionData))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));

        mockMvc.perform(post("/rest/analyzer/analyzers/1/qc-rules").sessionAttr("userSessionData", userSessionData)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ruleType\":\"SPECIMEN_ID_PREFIX\",\"targetField\":\"SPECIMEN_ID_FIELD\",\"operand\":\"QC-\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));

        mockMvc.perform(put("/rest/analyzer/analyzers/1/pending-codes/pc-1/status")
                .sessionAttr("userSessionData", userSessionData).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IGNORED\"}")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    public void astmEndpoints_WithAdminRole_AllowAccess() throws Exception {
        when(userRoleServiceMock.userInRole(anyString(), anyString())).thenReturn(true);

        AstmAnalyzerConfig cfg = new AstmAnalyzerConfig();
        cfg.setId("cfg-1");
        cfg.setConnectionRole("SERVER");
        cfg.setServerListenPort(6001);
        cfg.setAggregationMode("PER_MESSAGE");
        when(astmConfigServiceMock.getConfig("1")).thenReturn(cfg);
        when(astmConfigServiceMock.getExtractionConfigs("1")).thenReturn(List.of());
        when(astmConfigServiceMock.getFlagMappings("1")).thenReturn(List.of());
        when(astmConfigServiceMock.updateConfig(anyString(), anyMap())).thenReturn(cfg);

        mockMvc.perform(get("/rest/analyzer/analyzers/1/astm-config").sessionAttr("userSessionData", userSessionData))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("cfg-1"));

        mockMvc.perform(put("/rest/analyzer/analyzers/1/astm-config").sessionAttr("userSessionData", userSessionData)
                .contentType(MediaType.APPLICATION_JSON).content("{\"connectionRole\":\"SERVER\",\"serverListenPort\":6001}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("cfg-1"));

        mockMvc.perform(delete("/rest/analyzer/analyzers/1/test-mapping-configs/cfg-1")
                .sessionAttr("userSessionData", userSessionData)).andExpect(status().isNoContent());
        verify(astmTestMappingConfigServiceMock).delete("cfg-1");
    }
}
