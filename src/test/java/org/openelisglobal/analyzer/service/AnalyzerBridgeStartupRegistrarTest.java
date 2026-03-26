package org.openelisglobal.analyzer.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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
        when(bridgeRegistrationService.registerFile(any(), any(), any(), any())).thenReturn(true);

        registrar.reRegisterActiveAnalyzers(rootContextRefreshedEvent());

        verify(bridgeRegistrationService).registerFile(eq("2009"), eq("QuantStudio 7 Flex"),
                eq("/data/analyzer-imports/quantstudio"), eq("*.csv"));
    }

    @Test
    public void shouldSkipDeletedAnalyzerOnStartup() {
        analyzer.setStatus(Analyzer.AnalyzerStatus.DELETED);
        when(analyzerService.getAllWithTypes()).thenReturn(List.of(analyzer));

        registrar.reRegisterActiveAnalyzers(rootContextRefreshedEvent());

        verify(bridgeRegistrationService, never()).registerFile(any(), any(), any(), any());
        verify(bridgeRegistrationService, never()).registerTcp(any(), any(), any(), any(), any());
    }
}
