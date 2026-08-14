package org.openelisglobal.analyzer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.analyzer.service.AnalyzerProfileCatalogException;
import org.openelisglobal.analyzer.service.AnalyzerProfileCatalogFilter;
import org.openelisglobal.analyzer.service.AnalyzerTypeAttentionCode;
import org.openelisglobal.analyzer.service.AnalyzerTypeCatalogService;
import org.openelisglobal.analyzer.service.AnalyzerTypeCatalogSummary;
import org.openelisglobal.analyzer.service.AnalyzerTypeMappingProgress;
import org.openelisglobal.analyzer.service.AnalyzerTypeSiteBindingSummary;
import org.openelisglobal.analyzer.service.BridgeProfileAudit;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class AnalyzerTypeCatalogRestControllerTest {

    private AnalyzerTypeCatalogService catalogService;
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        catalogService = mock(AnalyzerTypeCatalogService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AnalyzerTypeCatalogRestController(catalogService)).build();
    }

    @Test
    public void listsComposedTypesUsingUrlBackedFilters() throws Exception {
        when(catalogService.list(any(AnalyzerProfileCatalogFilter.class))).thenReturn(List.of(summary()));

        mockMvc.perform(get("/rest/analyzer/types").param("q", "mock analyzer").param("source", "SHIPPED")
                .param("status", "ACTIVE").param("protocol", "ASTM")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].profileId").value("shipped.mock-hematology"))
                .andExpect(jsonPath("$[0].testMappings.total").value(2))
                .andExpect(jsonPath("$[0].testMappings.bound").value(2))
                .andExpect(jsonPath("$[0].analyzerCount").value(3))
                .andExpect(jsonPath("$[0].attentionCodes[0]").value("RESULT_VALUE_BINDING_REQUIRED"));

        ArgumentCaptor<AnalyzerProfileCatalogFilter> filter = ArgumentCaptor
                .forClass(AnalyzerProfileCatalogFilter.class);
        verify(catalogService).list(filter.capture());
        org.junit.Assert.assertEquals(new AnalyzerProfileCatalogFilter("mock analyzer", "SHIPPED", "ACTIVE", "ASTM"),
                filter.getValue());
    }

    @Test
    public void exposesBridgeFailureInsteadOfReturningAnEmptyCatalog() throws Exception {
        when(catalogService.list(any(AnalyzerProfileCatalogFilter.class)))
                .thenThrow(new AnalyzerProfileCatalogException("Analyzer Bridge profile catalog returned HTTP 503"));

        mockMvc.perform(get("/rest/analyzer/types")).andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Analyzer Bridge profile catalog returned HTTP 503"));
    }

    private static AnalyzerTypeCatalogSummary summary() {
        AnalyzerTypeMappingProgress tests = new AnalyzerTypeMappingProgress(2, 2, 0, 0, 0, 0);
        AnalyzerTypeMappingProgress results = new AnalyzerTypeMappingProgress(2, 0, 2, 0, 0, 0);
        AnalyzerTypeSiteBindingSummary binding = new AnalyzerTypeSiteBindingSummary("binding-1", "revision-1", 1, 3,
                "sha256:binding", "oe-user", Instant.parse("2026-08-14T03:00:00Z"));
        return new AnalyzerTypeCatalogSummary("shipped.mock-hematology", 3, "Mock Hematology", "HEMATOLOGY", "ASTM",
                "SHIPPED", "ACTIVE", "OpenELIS", "Mock Heme", "1.0", null, null, true, "sha256:bridge",
                new BridgeProfileAudit("ACTIVATED", "bridge-user", Instant.parse("2026-08-14T02:00:00Z")), tests,
                results, 1, 3L, binding, List.of(AnalyzerTypeAttentionCode.RESULT_VALUE_BINDING_REQUIRED));
    }
}
