package org.openelisglobal.eqa.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.eqa.controller.rest.EQAPanelRestController;
import org.openelisglobal.eqa.service.EQAPanelService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * OGC-609 [EQA V2.1] — the unblind privilege the controller hands the DTO
 * mapper comes from the caller's authorities, nothing else. The mapping itself
 * (null targets vs values) is covered by EQAPanelLifecycleIntegrationTest
 * against the real converter.
 */
@RunWith(MockitoJUnitRunner.class)
public class EQAPanelRestControllerTest {

    @Mock
    private EQAPanelService panelService;

    @InjectMocks
    private EQAPanelRestController controller;

    @After
    public void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateWith(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("tester", "n/a",
                List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList()));
    }

    @Test
    public void unprivilegedCaller_readsSamplesBlinded() {
        authenticateWith("ROLE_RESULTS");

        controller.getSamples(5L);

        verify(panelService).getSampleDtos(eq(5L), eq(false));
    }

    @Test
    public void manageGrant_unlocksTargets() {
        authenticateWith("ROLE_RESULTS", "qa.manage.eqa");

        controller.getSamples(5L);

        verify(panelService).getSampleDtos(eq(5L), eq(true));
    }

    @Test
    public void globalAdmin_unlocksTargets() {
        authenticateWith("ROLE_GLOBAL_ADMIN");

        controller.getSamples(5L);

        verify(panelService).getSampleDtos(eq(5L), eq(true));
    }

    @Test
    public void noAuthentication_readsBlinded() {
        SecurityContextHolder.clearContext();

        controller.getSamples(5L);

        verify(panelService).getSampleDtos(eq(5L), eq(false));
    }
}
