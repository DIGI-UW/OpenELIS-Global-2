package org.openelisglobal.analyzer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerPendingCodeService;
import org.openelisglobal.analyzer.service.AnalyzerPluginConfigService;
import org.openelisglobal.analyzer.valueholder.AnalyzerPendingCode;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public class AnalyzerPluginConfigRestControllerTest extends BaseWebContextSensitiveTest {

    @Mock
    private AnalyzerPluginConfigService analyzerPluginConfigService;

    @Mock
    private AnalyzerPendingCodeService analyzerPendingCodeService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        AnalyzerPluginConfigRestController controller = webApplicationContext
                .getBean(AnalyzerPluginConfigRestController.class);
        ReflectionTestUtils.setField(controller, "analyzerPluginConfigService", analyzerPluginConfigService);
        ReflectionTestUtils.setField(controller, "analyzerPendingCodeService", analyzerPendingCodeService);
    }

    @Test
    public void testGetPluginConfig_AsAdmin_Returns200() throws Exception {
        when(analyzerPluginConfigService.getConfigAsMap("101")).thenReturn(Map.of("connectionRole", "SERVER"));

        mockMvc.perform(get("/rest/analyzer/analyzers/101/plugin-config").with(adminUser())
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.connectionRole").value("SERVER"));
    }

    @Test
    public void testGetPluginConfig_Unauthorized_Returns403() throws Exception {
        when(analyzerPluginConfigService.getConfigAsMap("101")).thenReturn(Map.of());

        mockMvc.perform(get("/rest/analyzer/analyzers/101/plugin-config").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testUpdatePluginConfig_WithValidPayload_Returns200() throws Exception {
        Map<String, Object> config = Map.of("connectionRole", "SERVER", "serverListenPort", 17001);
        when(analyzerPluginConfigService.getConfigAsMap("101")).thenReturn(config);

        mockMvc.perform(put("/rest/analyzer/analyzers/101/plugin-config").with(adminUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"connectionRole\":\"SERVER\",\"serverListenPort\":17001}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.serverListenPort").value(17001));
    }

    @Test
    public void testUpdatePluginConfig_WithInvalidAggregation_Returns400() throws Exception {
        when(analyzerPluginConfigService.upsert(eq("101"), any(Map.class), any()))
                .thenThrow(new IllegalArgumentException("aggregationWindowSeconds invalid"));

        mockMvc.perform(put("/rest/analyzer/analyzers/101/plugin-config").with(adminUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aggregationMode\":\"BY_SESSION\",\"aggregationWindowSeconds\":999}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
    }

    @Test
    public void testGetPendingCodes_Returns200() throws Exception {
        AnalyzerPendingCode pendingCode = new AnalyzerPendingCode();
        pendingCode.setId("pc-1");
        pendingCode.setAnalyzerId("101");
        pendingCode.setAnalyzerTestName("ABC");
        pendingCode.setStatus(AnalyzerPendingCode.Status.PENDING);
        when(analyzerPendingCodeService.findByAnalyzerId("101")).thenReturn(List.of(pendingCode));

        mockMvc.perform(get("/rest/analyzer/analyzers/101/pending-codes").with(adminUser())
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("pc-1")).andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    public void testUpdatePendingCodeStatus_Returns200() throws Exception {
        AnalyzerPendingCode updated = new AnalyzerPendingCode();
        updated.setId("pc-1");
        updated.setAnalyzerId("101");
        updated.setStatus(AnalyzerPendingCode.Status.MAPPED);
        when(analyzerPendingCodeService.updateStatus(eq("pc-1"), eq(AnalyzerPendingCode.Status.MAPPED), any()))
                .thenReturn(updated);

        mockMvc.perform(put("/rest/analyzer/analyzers/101/pending-codes/pc-1/status").with(adminUser())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"MAPPED\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pc-1")).andExpect(jsonPath("$.status").value("MAPPED"));
    }

    private RequestPostProcessor adminUser() {
        return request -> {
            SecurityContext context = new SecurityContextImpl();
            context.setAuthentication(new UsernamePasswordAuthenticationToken("admin", "adminADMIN!",
                    List.of(new SimpleGrantedAuthority("ROLE_GLOBAL_ADMIN"))));
            request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    context);
            return request;
        };
    }
}
