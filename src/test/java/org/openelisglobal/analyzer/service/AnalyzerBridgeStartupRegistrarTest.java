package org.openelisglobal.analyzer.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
}
