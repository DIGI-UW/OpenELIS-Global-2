package org.openelisglobal.analyzer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analyzer.service.AnalyzerBridgePairingService;
import org.openelisglobal.analyzer.service.BridgePairingException;
import org.openelisglobal.common.util.UserContextHolder;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.testsupport.SliceSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

@WebAppConfiguration
@ContextConfiguration(classes = { SliceSecurityConfig.class, AnalyzerBridgePairingRestControllerTest.TestConfig.class })
public class AnalyzerBridgePairingRestControllerTest extends SecuritySliceMockMvcTest {

    private static final String PAIRING = "/rest/analyzer/bridge-pairing";

    @Autowired
    private AnalyzerBridgePairingService pairing;

    @Before
    public void stubStatus() {
        org.mockito.Mockito.reset(pairing);
        when(pairing.getStatus()).thenReturn(new AnalyzerBridgePairingService.Status("https://bridge:8443", true,
                "ab".repeat(32), "cd".repeat(32), Instant.parse("2026-10-08T12:00:00Z"), false));
    }

    @Test
    public void globalAdminSeesThePairing() throws Exception {
        mockMvc.perform(get(PAIRING).with(user("admin").roles("GLOBAL_ADMIN", "ADMIN"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.paired").value(true))
                .andExpect(jsonPath("$.bridgeCertificateSha256").value("ab".repeat(32)));
    }

    @Test
    public void theAnalyzerImportRoleCannotSeeOrPair() throws Exception {
        mockMvc.perform(get(PAIRING).with(user("analyzer").roles("ANALYSER_IMPORT"))).andExpect(status().isForbidden());
        mockMvc.perform(post(PAIRING).with(user("analyzer").roles("ANALYSER_IMPORT"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"ABCD\"}"))
                .andExpect(status().isForbidden());
        verify(pairing, never()).pair(any(), any());
    }

    @Test
    public void globalAdminPairsWithTheCode() throws Exception {
        AnalyzerBridgePairingService.Status paired = pairing.getStatus();
        when(pairing.pair(eq("ABCD-EFGH"), eq("1"))).thenReturn(paired);

        mockMvc.perform(post(PAIRING).with(user("admin").roles("GLOBAL_ADMIN", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"ABCD-EFGH\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paired").value(true));
    }

    @Test
    public void aRefusedPairingNamesTheReason() throws Exception {
        when(pairing.pair(any(), any()))
                .thenThrow(new BridgePairingException("analyzer.bridgePairing.error.wrongCode"));

        mockMvc.perform(post(PAIRING).with(user("admin").roles("GLOBAL_ADMIN", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"WRONG\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorKey").value("analyzer.bridgePairing.error.wrongCode"));
    }

    @Test
    public void anUnreachableBridgeIsAGatewayError() throws Exception {
        when(pairing.pair(any(), any()))
                .thenThrow(new BridgePairingException("analyzer.bridgePairing.error.unreachable"));

        mockMvc.perform(post(PAIRING).with(user("admin").roles("GLOBAL_ADMIN", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"ABCD\"}"))
                .andExpect(status().isBadGateway());
    }

    @Configuration
    static class TestConfig {
        @Bean
        AnalyzerBridgePairingService pairing() {
            return mock(AnalyzerBridgePairingService.class);
        }

        @Bean
        AnalyzerBridgePairingRestController controller(AnalyzerBridgePairingService pairing) {
            return new AnalyzerBridgePairingRestController(pairing);
        }

        @Bean
        SpringContext springContext() {
            return new SpringContext();
        }

        @Bean
        SystemUserService systemUserService() {
            return mock(SystemUserService.class);
        }

        @Bean
        SystemUser daemonSystemUser() {
            return new SystemUser();
        }

        @Bean
        UserContextHolder userContextHolder() {
            UserContextHolder holder = mock(UserContextHolder.class);
            when(holder.getCurrentSysUserId()).thenReturn("1");
            return holder;
        }
    }
}
