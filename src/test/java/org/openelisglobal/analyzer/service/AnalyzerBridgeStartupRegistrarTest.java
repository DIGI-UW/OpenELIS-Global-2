package org.openelisglobal.analyzer.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerBridgeStartupRegistrarTest {

    private static final int ASYNC_TIMEOUT_MS = 5_000;

    @Mock
    private AnalyzerService analyzerService;

    @Mock
    private FileImportService fileImportService;

    @Mock
    private BridgeRegistrationService bridgeRegistrationService;

    @Mock
    private AnalyzerTestMappingService analyzerTestMappingService;

    @InjectMocks
    private AnalyzerBridgeStartupRegistrar registrar;

    private Analyzer analyzer;

    @Before
    public void setUp() {
        analyzer = new Analyzer();
        analyzer.setId("2009");
        analyzer.setName("QuantStudio 7 Flex");
        analyzer.setStatus(Analyzer.AnalyzerStatus.ACTIVE);
        when(analyzerTestMappingService.getAllForAnalyzer(any())).thenReturn(List.of());
    }

    private static ContextRefreshedEvent rootContextRefreshedEvent() {
        ContextRefreshedEvent event = mock(ContextRefreshedEvent.class);
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(event.getApplicationContext()).thenReturn(ctx);
        when(ctx.getParent()).thenReturn(null);
        return event;
    }

    @Test
    public void shouldRegisterFileAnalyzerOnStartup() {
        // Set unified FILE fields directly on Analyzer entity
        analyzer.setImportDirectory("/data/analyzer-imports/quantstudio");
        analyzer.setFilePattern("*.csv");
        analyzer.setFileFormat("CSV");
        analyzer.setDelimiter(",");
        analyzer.setSkipRows(0);

        when(analyzerService.getAllWithTypes()).thenReturn(List.of(analyzer));
        when(bridgeRegistrationService.registerFile(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(true);

        registrar.onStartup(rootContextRefreshedEvent());

        verify(bridgeRegistrationService, timeout(ASYNC_TIMEOUT_MS)).registerFile(eq("2009"), eq("QuantStudio 7 Flex"),
                eq("/data/analyzer-imports/quantstudio"), eq("*.csv"), eq(Map.of()), eq("CSV"), eq(","), eq(0), any());
    }

    @Test
    public void shouldRegisterFileAnalyzerWithColumnMappings() {
        analyzer.setImportDirectory("/data/analyzer-imports/quantstudio");
        analyzer.setFilePattern("*.xlsx");
        analyzer.setFileFormat("EXCEL");
        analyzer.setColumnMappings(Map.of("Sample Name", "sampleId", "CT", "result"));

        when(analyzerService.getAllWithTypes()).thenReturn(List.of(analyzer));
        when(bridgeRegistrationService.registerFile(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(true);

        registrar.onStartup(rootContextRefreshedEvent());

        verify(bridgeRegistrationService, timeout(ASYNC_TIMEOUT_MS)).registerFile(eq("2009"), eq("QuantStudio 7 Flex"),
                eq("/data/analyzer-imports/quantstudio"), eq("*.xlsx"),
                eq(Map.of("Sample Name", "sampleId", "CT", "result")), eq("EXCEL"), any(), any(), any());
    }

    @Test
    public void shouldSkipDeletedAnalyzerOnStartup() {
        analyzer.setStatus(Analyzer.AnalyzerStatus.DELETED);

        registrar.onStartup(rootContextRefreshedEvent());

        verify(bridgeRegistrationService, timeout(ASYNC_TIMEOUT_MS).times(0)).registerFile(any(), any(), any(), any(),
                any(), any(), any(), any(), any());
        verify(bridgeRegistrationService, timeout(ASYNC_TIMEOUT_MS).times(0)).registerTcp(any(), any(), any(), any(),
                any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void fullStateSyncIncludesAllBridgeCollectionsForTcpAndFileAnalyzers() {
        Analyzer tcpAnalyzer = new Analyzer();
        tcpAnalyzer.setId("tcp-1");
        tcpAnalyzer.setName("GeneXpert");
        tcpAnalyzer.setStatus(Analyzer.AnalyzerStatus.ACTIVE);
        tcpAnalyzer.setIpAddress("10.0.0.10");
        tcpAnalyzer.setPort(5380);
        tcpAnalyzer.setIdentifierPattern("GENEXPERT|CEPHEID.*GX");

        Analyzer fileAnalyzer = new Analyzer();
        fileAnalyzer.setId("file-1");
        fileAnalyzer.setName("QuantStudio");
        fileAnalyzer.setStatus(Analyzer.AnalyzerStatus.ACTIVE);
        fileAnalyzer.setImportDirectory("/data/analyzer-imports/quantstudio");
        fileAnalyzer.setFilePattern("*.csv");

        when(analyzerService.getAllWithTypes()).thenReturn(List.of(tcpAnalyzer, fileAnalyzer));
        when(bridgeRegistrationService.registerTcp(any(), any(), any(), any(), any(), any())).thenReturn(true);
        when(bridgeRegistrationService.registerFile(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(true);
        when(bridgeRegistrationService.syncAll(any())).thenReturn(true);

        doAnswer(invocation -> {
            ((Map<String, Object>) invocation.getArgument(0)).put("qcRules", List.of());
            return null;
        }).when(bridgeRegistrationService).attachQcRules(anyMap(), anyString());
        doAnswer(invocation -> {
            ((Map<String, Object>) invocation.getArgument(0)).put("controlLots", List.of());
            return null;
        }).when(bridgeRegistrationService).attachControlLots(anyMap(), anyString());
        doAnswer(invocation -> {
            ((Map<String, Object>) invocation.getArgument(0)).put("testCodeLoinc", Map.of());
            return null;
        }).when(bridgeRegistrationService).attachTestCodeLoinc(anyMap(), anyString());

        registrar.onStartup(rootContextRefreshedEvent());

        ArgumentCaptor<List<Map<String, Object>>> payloadCaptor = ArgumentCaptor.forClass(List.class);
        verify(bridgeRegistrationService, timeout(ASYNC_TIMEOUT_MS)).syncAll(payloadCaptor.capture());
        List<Map<String, Object>> payloads = payloadCaptor.getValue();

        org.junit.Assert.assertEquals(2, payloads.size());
        for (Map<String, Object> payload : payloads) {
            org.junit.Assert.assertTrue(payload.containsKey("qcRules"));
            org.junit.Assert.assertTrue(payload.containsKey("controlLots"));
            org.junit.Assert.assertTrue(payload.containsKey("testCodeLoinc"));
        }
        Map<String, Object> tcpPayload = payloads.stream()
                .filter(payload -> "tcp-1".equals(payload.get("oeAnalyzerId"))).findFirst().orElseThrow();
        org.junit.Assert.assertEquals("GENEXPERT|CEPHEID.*GX", tcpPayload.get("identifierPattern"));
    }
}
