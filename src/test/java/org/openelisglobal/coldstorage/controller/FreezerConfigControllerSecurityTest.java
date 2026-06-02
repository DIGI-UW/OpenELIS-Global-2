package org.openelisglobal.coldstorage.controller;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.coldstorage.service.SystemConfigService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.siteinformation.service.SiteInformationDomainService;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebAppConfiguration
@ContextConfiguration(classes = { FreezerConfigControllerSecurityTest.TestConfig.class })
public class FreezerConfigControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void testGetSystemConfig_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/rest/coldstorage/system-config")).andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetSystemConfig_WithPrivilege_Returns200() throws Exception {
        mockMvc.perform(get("/rest/coldstorage/system-config").with(user("admin").authorities(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("PRIV_SYSTEM_CONFIGURE"))))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetSystemConfig_WithoutPrivilege_Returns403() throws Exception {
        mockMvc.perform(get("/rest/coldstorage/system-config").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testSaveSystemConfig_WithPrivilege_Returns200() throws Exception {
        mockMvc.perform(post("/rest/coldstorage/system-config")
                .with(user("admin").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        "PRIV_SYSTEM_CONFIGURE")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"modbusTcpPort\":502,\"bacnetUdpPort\":47808}"))
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveSystemConfig_WithoutPrivilege_Returns403() throws Exception {
        mockMvc.perform(post("/rest/coldstorage/system-config").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"modbusTcpPort\":502,\"bacnetUdpPort\":47808}"))
                .andExpect(status().isForbidden());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity(prePostEnabled = true)
    static class TestConfig {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).httpBasic(Customizer.withDefaults())
                    .csrf(csrf -> csrf.disable());
            return http.build();
        }

        @Bean
        SystemConfigService systemConfigService() {
            return new SystemConfigService(mock(SiteInformationService.class),
                    mock(SiteInformationDomainService.class));
        }

        @Bean
        FreezerConfigController freezerConfigController(SystemConfigService systemConfigService) {
            return new FreezerConfigController(systemConfigService);
        }
    }
}
