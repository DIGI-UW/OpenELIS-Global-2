package org.openelisglobal.analyzer.controller;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.service.AstmConfigService;
import org.openelisglobal.analyzer.service.AstmPendingCodeService;
import org.openelisglobal.analyzer.service.AstmQcRuleService;
import org.openelisglobal.analyzer.service.AstmTestMappingConfigService;
import org.openelisglobal.analyzer.valueholder.AstmTestMappingConfig;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.class)
public class AstmTestMappingConfigRestControllerTest {

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
    public void testMappingConfigEndpoints_GetPostPutDelete_WorkAsExpected() throws Exception {
        AstmTestMappingConfig cfg = new AstmTestMappingConfig();
        cfg.setId("cfg-1");
        cfg.setAnalyzerTestName("TBIL");
        cfg.setTransformType("GREATER_LESS_FLAG");
        cfg.setTransformConfigJson("{\"operators\":[\">\",\"<\"]}");
        cfg.setIsActive(true);

        when(astmTestMappingConfigService.findByAnalyzerId("1")).thenReturn(List.of(cfg));
        when(astmTestMappingConfigService.create(org.mockito.ArgumentMatchers.eq("1"), anyMap())).thenReturn(cfg);
        when(astmTestMappingConfigService.update(org.mockito.ArgumentMatchers.eq("cfg-1"), anyMap())).thenReturn(cfg);

        mockMvc.perform(get("/rest/analyzer/analyzers/1/test-mapping-configs").contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("userSessionData", userSessionData)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("cfg-1"))
                .andExpect(jsonPath("$[0].transformType").value("GREATER_LESS_FLAG"));

        mockMvc.perform(post("/rest/analyzer/analyzers/1/test-mapping-configs").contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{\"analyzerTestName\":\"TBIL\",\"transformType\":\"GREATER_LESS_FLAG\",\"transformConfig\":{\"operators\":[\">\",\"<\"]}}")
                .sessionAttr("userSessionData", userSessionData)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("cfg-1"));

        mockMvc.perform(
                put("/rest/analyzer/analyzers/1/test-mapping-configs/cfg-1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\":true}").sessionAttr("userSessionData", userSessionData))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("cfg-1"));

        mockMvc.perform(delete("/rest/analyzer/analyzers/1/test-mapping-configs/cfg-1")
                .contentType(MediaType.APPLICATION_JSON).sessionAttr("userSessionData", userSessionData))
                .andExpect(status().isNoContent());
    }
}
