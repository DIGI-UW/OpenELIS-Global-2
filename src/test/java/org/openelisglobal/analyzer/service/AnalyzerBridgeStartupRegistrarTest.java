package org.openelisglobal.analyzer.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.FileImportConfiguration;
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

    @InjectMocks
    private AnalyzerBridgeStartupRegistrar registrar;

    private Analyzer analyzer;

    @Before
    public void setUp() {
        analyzer = new Analyzer();
        analyzer.setId("2009");
        analyzer.setName("QuantStudio 7 Flex");
        analyzer.setStatus(Analyzer.AnalyzerStatus.ACTIVE);
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
        FileImportConfiguration cfg = new FileImportConfiguration();
        cfg.setImportDirectory("/data/analyzer-imports/quantstudio");
        cfg.setFilePattern("*.csv");

        when(analyzerService.getAllWithTypes()).thenReturn(List.of(analyzer));
        when(fileImportService.getByAnalyzerId(2009)).thenReturn(Optional.of(cfg));
        when(bridgeRegistrationService.registerFile(any(), any(), any(), any(), any())).thenReturn(true);

        // onStartup dispatches async — use timeout to wait for completion
        registrar.onStartup(rootContextRefreshedEvent());

        verify(bridgeRegistrationService, timeout(ASYNC_TIMEOUT_MS)).registerFile(eq("2009"), eq("QuantStudio 7 Flex"),
                eq("/data/analyzer-imports/quantstudio"), eq("*.csv"), eq(Map.of()));
    }

    @Test
    public void shouldRegisterFileAnalyzerWithColumnMappings() {
        FileImportConfiguration cfg = new FileImportConfiguration();
        cfg.setImportDirectory("/data/analyzer-imports/quantstudio");
        cfg.setFilePattern("*.xlsx");
        cfg.setColumnMappings(Map.of("Sample Name", "sampleId", "CT", "result"));

        when(analyzerService.getAllWithTypes()).thenReturn(List.of(analyzer));
        when(fileImportService.getByAnalyzerId(2009)).thenReturn(Optional.of(cfg));
        when(bridgeRegistrationService.registerFile(any(), any(), any(), any(), any())).thenReturn(true);

        registrar.onStartup(rootContextRefreshedEvent());

        verify(bridgeRegistrationService, timeout(ASYNC_TIMEOUT_MS)).registerFile(eq("2009"), eq("QuantStudio 7 Flex"),
                eq("/data/analyzer-imports/quantstudio"), eq("*.xlsx"),
                eq(Map.of("Sample Name", "sampleId", "CT", "result")));
    }

    @Test
    public void shouldSkipDeletedAnalyzerOnStartup() {
        analyzer.setStatus(Analyzer.AnalyzerStatus.DELETED);

        registrar.onStartup(rootContextRefreshedEvent());

        // Allow async work to complete, then verify no registration happened
        verify(bridgeRegistrationService, timeout(ASYNC_TIMEOUT_MS).times(0)).registerFile(any(), any(), any(), any(),
                any());
        verify(bridgeRegistrationService, timeout(ASYNC_TIMEOUT_MS).times(0)).registerTcp(any(), any(), any(), any(),
                any());
    }
}
