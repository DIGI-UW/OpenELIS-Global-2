package org.openelisglobal.analyzer.controller;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.openelisglobal.analyzer.valueholder.AstmAnalyzerConfig;
import org.openelisglobal.analyzer.valueholder.AstmFieldExtractionConfig;
import org.openelisglobal.analyzer.valueholder.AstmFlagMapping;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.class)
public class AstmConfigRestControllerTest {

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
    public void getAstmConfig_ReturnsConfigWithOverridesAndFlags() throws Exception {
        AstmAnalyzerConfig cfg = new AstmAnalyzerConfig();
        cfg.setId("cfg-1");
        cfg.setConnectionRole("SERVER");
        cfg.setServerListenPort(6001);
        cfg.setAggregationMode("PER_MESSAGE");

        AstmFieldExtractionConfig extraction = new AstmFieldExtractionConfig();
        extraction.setKey("SPECIMEN_ID_FIELD");
        extraction.setFieldIndex(3);

        AstmFlagMapping flag = new AstmFlagMapping();
        flag.setId("fm-1");
        flag.setAnalyzerFlag("H");
        flag.setOpenelisFlag("HIGH");
        flag.setIsCustom(true);

        when(astmConfigService.getConfig("1")).thenReturn(cfg);
        when(astmConfigService.getExtractionConfigs("1")).thenReturn(List.of(extraction));
        when(astmConfigService.getFlagMappings("1")).thenReturn(List.of(flag));

        mockMvc.perform(get("/rest/analyzer/analyzers/1/astm-config").contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("userSessionData", userSessionData)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("cfg-1")).andExpect(jsonPath("$.connectionRole").value("SERVER"))
                .andExpect(jsonPath("$.extractionOverrides[0].key").value("SPECIMEN_ID_FIELD"))
                .andExpect(jsonPath("$.flagMappings[0].analyzerFlag").value("H"))
                .andExpect(jsonPath("$.flagMappings[0].isCustom").value(true));
    }

    @Test
    public void updateAstmConfig_WithValidationFailure_ReturnsBadRequest() throws Exception {
        when(astmConfigService.updateConfig(org.mockito.ArgumentMatchers.eq("1"), anyMap()))
                .thenThrow(new LIMSRuntimeException("SERVER role requires serverListenPort"));

        mockMvc.perform(put("/rest/analyzer/analyzers/1/astm-config").contentType(MediaType.APPLICATION_JSON)
                .content("{\"connectionRole\":\"SERVER\"}").sessionAttr("userSessionData", userSessionData))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("ASTM_CONFIG_VALIDATION_ERROR"));
    }

    @Test
    public void updateAstmConfig_FlagMappingRoundTrip_ReturnsCustomFlags() throws Exception {
        AstmAnalyzerConfig cfg = new AstmAnalyzerConfig();
        cfg.setId("cfg-1");
        cfg.setConnectionRole("SERVER");
        cfg.setServerListenPort(6001);
        cfg.setAggregationMode("PER_MESSAGE");

        AstmFlagMapping flag = new AstmFlagMapping();
        flag.setId("fm-2");
        flag.setAnalyzerFlag("X");
        flag.setOpenelisFlag("ABNORMAL");
        flag.setIsCustom(true);

        when(astmConfigService.updateConfig(org.mockito.ArgumentMatchers.eq("1"), anyMap())).thenReturn(cfg);
        when(astmConfigService.getExtractionConfigs("1")).thenReturn(List.of());
        when(astmConfigService.getFlagMappings("1")).thenReturn(List.of(flag));

        mockMvc.perform(put("/rest/analyzer/analyzers/1/astm-config").contentType(MediaType.APPLICATION_JSON).content(
                "{\"connectionRole\":\"SERVER\",\"serverListenPort\":6001,\"flagMappings\":[{\"analyzerFlag\":\"X\",\"openelisFlag\":\"ABNORMAL\",\"isCustom\":true}]}")
                .sessionAttr("userSessionData", userSessionData)).andExpect(status().isOk())
                .andExpect(jsonPath("$.flagMappings[0].analyzerFlag").value("X"))
                .andExpect(jsonPath("$.flagMappings[0].openelisFlag").value("ABNORMAL"))
                .andExpect(jsonPath("$.flagMappings[0].isCustom").value(true));
    }
}
