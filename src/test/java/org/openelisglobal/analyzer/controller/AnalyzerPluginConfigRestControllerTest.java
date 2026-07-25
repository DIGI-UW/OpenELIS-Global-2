package org.openelisglobal.analyzer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.openelisglobal.analyzer.form.AnalyzerResultValueOption;
import org.openelisglobal.analyzer.service.AnalyzerPendingCodeService;
import org.openelisglobal.analyzer.service.AnalyzerPluginConfigService;
import org.openelisglobal.analyzer.service.AnalyzerResultValueOptionService;
import org.openelisglobal.analyzer.service.AnalyzerSetupVerificationService;
import org.openelisglobal.analyzer.valueholder.AnalyzerPendingCode;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class AnalyzerPluginConfigRestControllerTest extends BaseWebContextSensitiveTest {

    @Mock
    private AnalyzerPluginConfigService analyzerPluginConfigService;

    @Mock
    private AnalyzerPendingCodeService analyzerPendingCodeService;

    @Mock
    private AnalyzerResultValueOptionService analyzerResultValueOptionService;

    @Mock
    private AnalyzerSetupVerificationService analyzerSetupVerificationService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).apply(springSecurity()).build();
        MockitoAnnotations.initMocks(this);
        AnalyzerPluginConfigRestController controller = webApplicationContext
                .getBean(AnalyzerPluginConfigRestController.class);
        ReflectionTestUtils.setField(controller, "analyzerPluginConfigService", analyzerPluginConfigService);
        ReflectionTestUtils.setField(controller, "analyzerPendingCodeService", analyzerPendingCodeService);
        ReflectionTestUtils.setField(controller, "analyzerResultValueOptionService", analyzerResultValueOptionService);
        ReflectionTestUtils.setField(controller, "analyzerSetupVerificationService", analyzerSetupVerificationService);
    }

    @Test
    public void testGetPluginConfig_AsAdmin_Returns200() throws Exception {
        when(analyzerPluginConfigService.getConfigAsMap("101")).thenReturn(Map.of("connectionRole", "SERVER"));

        mockMvc.perform(get("/rest/analyzer/analyzers/101/plugin-config").with(user("admin").roles("GLOBAL_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.connectionRole").value("SERVER"));
    }

    @Test
    public void testUpdatePluginConfig_WithValidPayload_Returns200() throws Exception {
        Map<String, Object> config = Map.of("connectionRole", "SERVER", "serverListenPort", 17001);
        when(analyzerPluginConfigService.getConfigAsMap("101")).thenReturn(config);

        mockMvc.perform(put("/rest/analyzer/analyzers/101/plugin-config").with(user("admin").roles("GLOBAL_ADMIN"))
                .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"connectionRole\":\"SERVER\",\"serverListenPort\":17001}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.serverListenPort").value(17001));
    }

    @Test
    public void testUpdatePluginConfig_WithInvalidAggregation_Returns400() throws Exception {
        when(analyzerPluginConfigService.upsert(eq("101"), any(Map.class), any()))
                .thenThrow(new IllegalArgumentException("aggregationWindowSeconds invalid"));

        mockMvc.perform(put("/rest/analyzer/analyzers/101/plugin-config").with(user("admin").roles("GLOBAL_ADMIN"))
                .with(csrf()).contentType(MediaType.APPLICATION_JSON)
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
        when(analyzerPendingCodeService.getMappedTestIds("101")).thenReturn(Map.of("ABC", "501"));

        mockMvc.perform(get("/rest/analyzer/analyzers/101/pending-codes").with(user("admin").roles("GLOBAL_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("pc-1")).andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].openelisTestId").value("501"));
    }

    @Test
    public void testUpdatePendingCodeStatus_MappedWithoutResolutionReturns400() throws Exception {
        when(analyzerPendingCodeService.updateStatus(eq("101"), eq("pc-1"), eq(AnalyzerPendingCode.Status.MAPPED),
                any())).thenThrow(new IllegalArgumentException("Mapped status requires an OpenELIS test resolution"));

        mockMvc.perform(
                put("/rest/analyzer/analyzers/101/pending-codes/pc-1/status").with(user("admin").roles("GLOBAL_ADMIN"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"MAPPED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetPendingCodeMappingOptions_ReturnsCatalogTests() throws Exception {
        when(analyzerPendingCodeService.getMappingOptions())
                .thenReturn(List.of(Map.of("id", "501", "name", "Xpert MTB/RIF", "loinc", "38379-4")));

        mockMvc.perform(get("/rest/analyzer/analyzers/101/test-mapping-options")
                .with(user("admin").roles("GLOBAL_ADMIN")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value("501"))
                .andExpect(jsonPath("$[0].name").value("Xpert MTB/RIF"));
    }

    @Test
    public void testResolvePendingCode_CreatesMappingAndReturnsMapped() throws Exception {
        AnalyzerPendingCode updated = new AnalyzerPendingCode();
        updated.setId("pc-1");
        updated.setAnalyzerId("101");
        updated.setStatus(AnalyzerPendingCode.Status.MAPPED);
        when(analyzerPendingCodeService.resolve(eq("101"), eq("pc-1"), eq("501"), any())).thenReturn(updated);

        mockMvc.perform(post("/rest/analyzer/analyzers/101/pending-codes/pc-1/resolve")
                .with(user("admin").roles("GLOBAL_ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"openelisTestId\":\"501\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pc-1")).andExpect(jsonPath("$.status").value("MAPPED"));
    }

    @Test
    public void testResolvePendingCode_WithoutCatalogTestReturns400() throws Exception {
        mockMvc.perform(post("/rest/analyzer/analyzers/101/pending-codes/pc-1/resolve")
                .with(user("admin").roles("GLOBAL_ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("openelisTestId is required"));
    }

    @Test
    public void testGetResultValueMappings_Returns200() throws Exception {
        when(analyzerPluginConfigService.getResultValueMappings("101")).thenReturn(List.of(Map.of("analyzerValue",
                "Detected", "openelisValue", "POSITIVE", "testCode", "MTB", "active", true)));

        mockMvc.perform(get("/rest/analyzer/analyzers/101/result-value-mappings")
                .with(user("admin").roles("GLOBAL_ADMIN")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].analyzerValue").value("Detected"))
                .andExpect(jsonPath("$[0].openelisValue").value("POSITIVE"));
    }

    @Test
    public void testGetResultValueOptions_ReturnsCatalogBoundOptions() throws Exception {
        AnalyzerResultValueOption option = new AnalyzerResultValueOption();
        option.setId("result-option-1");
        option.setValue("9001");
        option.setLabel("Detected");
        option.setTestId("501");
        when(analyzerResultValueOptionService.findOptions("101", "MTB")).thenReturn(List.of(option));

        mockMvc.perform(get("/rest/analyzer/analyzers/101/result-value-options").param("testCode", "MTB")
                .with(user("admin").roles("GLOBAL_ADMIN")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value("result-option-1"))
                .andExpect(jsonPath("$[0].label").value("Detected")).andExpect(jsonPath("$[0].testId").value("501"));
    }

    @Test
    public void testGetResultValueOptions_UnmappedLegacyCodeReturnsEmptyList() throws Exception {
        when(analyzerResultValueOptionService.getOptions("101", "LEGACY"))
                .thenThrow(new IllegalArgumentException("No test mapping exists"));
        when(analyzerResultValueOptionService.findOptions("101", "LEGACY")).thenReturn(List.of());

        mockMvc.perform(get("/rest/analyzer/analyzers/101/result-value-options").param("testCode", "LEGACY")
                .with(user("admin").roles("GLOBAL_ADMIN")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    public void testUpdateResultValueMappings_Returns200() throws Exception {
        Map<String, Object> response = Map.of("resultValueMappings", List.of(Map.of("analyzerValue", "Detected",
                "testCode", "MTB", "openelisResultOptionId", "result-option-1", "openelisValue", "9001")));
        when(analyzerPluginConfigService.updateResultValueMappings(eq("101"), any(List.class), any()))
                .thenReturn(response);

        mockMvc.perform(put("/rest/analyzer/analyzers/101/result-value-mappings")
                .with(user("admin").roles("GLOBAL_ADMIN")).contentType(MediaType.APPLICATION_JSON).content(
                        "[{\"analyzerValue\":\"Detected\",\"testCode\":\"MTB\",\"openelisResultOptionId\":\"result-option-1\"}]"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resultValueMappings[0].openelisValue").value("9001"));
    }

    @Test
    public void testUpdateResultValueMappings_FreeTextTargetReturns400() throws Exception {
        when(analyzerPluginConfigService.updateResultValueMappings(eq("101"), any(List.class), any()))
                .thenThrow(new IllegalArgumentException("openelisResultOptionId is required"));

        mockMvc.perform(put("/rest/analyzer/analyzers/101/result-value-mappings")
                .with(user("admin").roles("GLOBAL_ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content("[{\"analyzerValue\":\"Detected\",\"testCode\":\"MTB\",\"openelisValue\":\"POSITIVE\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("openelisResultOptionId is required"));
    }

    @Test
    public void testGetPendingResultValues_Returns200() throws Exception {
        when(analyzerPluginConfigService.getPendingResultValues("101"))
                .thenReturn(List.of(Map.of("id", "rv-1", "analyzerValue", "Trace", "status", "PENDING")));

        mockMvc.perform(get("/rest/analyzer/analyzers/101/pending-result-values")
                .with(user("admin").roles("GLOBAL_ADMIN")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value("rv-1"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    public void testResolvePendingResultValue_Returns200() throws Exception {
        when(analyzerPluginConfigService.resolvePendingResultValue(eq("101"), eq("rv-1"), any(Map.class), any()))
                .thenReturn(Map.of("id", "rv-1", "status", "MAPPED", "openelisResultOptionId", "result-option-1",
                        "openelisValue", "9001"));

        mockMvc.perform(post("/rest/analyzer/analyzers/101/pending-result-values/rv-1/resolve")
                .with(user("admin").roles("GLOBAL_ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"openelisResultOptionId\":\"result-option-1\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MAPPED"))
                .andExpect(jsonPath("$.openelisResultOptionId").value("result-option-1"));
    }

    @Test
    public void testGetSetupVerification_ReturnsCurrentReadiness() throws Exception {
        when(analyzerSetupVerificationService.getVerificationStatus("101"))
                .thenReturn(Map.of("verificationState", "CURRENT", "currentlyVerified", true, "readyForActivation",
                        true));

        mockMvc.perform(get("/rest/analyzer/analyzers/101/setup-verification")
                .with(user("admin").roles("GLOBAL_ADMIN")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.verificationState").value("CURRENT"))
                .andExpect(jsonPath("$.readyForActivation").value(true));
    }

    @Test
    public void testVerifySetup_RecordsConfirmedIds() throws Exception {
        when(analyzerSetupVerificationService.verifySetup(eq("101"), any(Map.class), any()))
                .thenReturn(Map.of("verificationState", "CURRENT", "verifiedBy", "1", "currentlyVerified", true));

        mockMvc.perform(post("/rest/analyzer/analyzers/101/setup-verification")
                .with(user("admin").roles("GLOBAL_ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"mappingIds\":[\"TEST:MTB\"],\"qcIds\":[\"RULE:rule-1\",\"LOT:lot-1\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.verificationState").value("CURRENT"))
                .andExpect(jsonPath("$.currentlyVerified").value(true));
    }
}
