package org.openelisglobal.analyzer.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerBridgeStartupRegistrarTest {

    @Mock
    private BridgeRegistrationService bridgeRegistrationService;

    private AnalyzerBridgeStartupRegistrar registrar;

    @Before
    public void setUp() {
        registrar = new AnalyzerBridgeStartupRegistrar(bridgeRegistrationService, Runnable::run);
        when(bridgeRegistrationService.synchronize()).thenReturn(new BridgeRegistrationResult(true, Set.of(), null));
    }

    @Test
    public void rootStartupPerformsOneFullStateSynchronization() {
        registrar.onStartup(contextRefreshedEvent(null));

        verify(bridgeRegistrationService).synchronize();
    }

    @Test
    public void childContextDoesNotDuplicateStartupSynchronization() {
        registrar.onStartup(contextRefreshedEvent(mock(ApplicationContext.class)));

        verify(bridgeRegistrationService, never()).synchronize();
    }

    @Test
    public void scheduledReconciliationUsesTheSameFullStateWriter() {
        registrar.reconcile();

        verify(bridgeRegistrationService).synchronize();
    }

    private static ContextRefreshedEvent contextRefreshedEvent(ApplicationContext parent) {
        ContextRefreshedEvent event = mock(ContextRefreshedEvent.class);
        ApplicationContext context = mock(ApplicationContext.class);
        when(event.getApplicationContext()).thenReturn(context);
        when(context.getParent()).thenReturn(parent);
        return event;
    }
}
