package org.openelisglobal.configuration.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.configuration.service.ConfigurationInitializationService;
import org.openelisglobal.configuration.service.ConfigurationReloadFileResult;
import org.openelisglobal.configuration.service.ConfigurationReloadOptions;
import org.openelisglobal.configuration.service.ConfigurationReloadResult;
import org.springframework.test.util.ReflectionTestUtils;

public class ConfigurationReloadRestControllerTest {

    private ConfigurationInitializationService configurationInitializationService;
    private ConfigurationReloadRefreshService refreshService;
    private ConfigurationReloadRestController controller;

    @Before
    public void setUp() {
        configurationInitializationService = mock(ConfigurationInitializationService.class);
        refreshService = mock(ConfigurationReloadRefreshService.class);
        controller = new ConfigurationReloadRestController();
        ReflectionTestUtils.setField(controller, "configurationInitializationService",
                configurationInitializationService);
        ReflectionTestUtils.setField(controller, "refreshService", refreshService);
    }

    @Test
    public void reloadDomains_passesDomainFilterAndForceOptionThenRefreshesCaches() {
        ConfigurationReloadResult result = new ConfigurationReloadResult(
                List.of(ConfigurationReloadFileResult.processed("roles", "roles.csv")));
        when(configurationInitializationService.reload(any(ConfigurationReloadOptions.class))).thenReturn(result);

        ConfigurationReloadResult response = controller
                .reloadDomains(new ConfigurationReloadRestController.ConfigurationReloadRequest(Set.of("roles"), true));

        ArgumentCaptor<ConfigurationReloadOptions> optionsCaptor = ArgumentCaptor
                .forClass(ConfigurationReloadOptions.class);
        verify(configurationInitializationService).reload(optionsCaptor.capture());
        assertEquals(Set.of("roles"), optionsCaptor.getValue().domains());
        assertEquals(true, optionsCaptor.getValue().force());
        verify(refreshService).refreshAfterDomainReload();
        assertEquals(result, response);
    }

    @Test
    public void reloadDomains_skipsCacheRefreshWhenReloadReportsErrors() {
        ConfigurationReloadResult result = new ConfigurationReloadResult(
                List.of(ConfigurationReloadFileResult.error("roles", "roles.csv", "boom")));
        when(configurationInitializationService.reload(any(ConfigurationReloadOptions.class))).thenReturn(result);

        controller
                .reloadDomains(new ConfigurationReloadRestController.ConfigurationReloadRequest(Set.of("roles"), true));

        verify(refreshService, never()).refreshAfterDomainReload();
    }
}
