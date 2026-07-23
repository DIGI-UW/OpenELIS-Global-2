package org.openelisglobal.common.management.controller.rest;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.view.PageBuilderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebAppConfiguration
@ContextConfiguration(classes = { SampleTypeManagementRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class SampleTypeManagementRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void testSampleTypeManagement_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/rest/SampleTypeManagement").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testSampleTypeManagement_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(get("/rest/SampleTypeManagement").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void testSampleTypeManagement_AdminRole_Returns200() throws Exception {
        mockMvc.perform(get("/rest/SampleTypeManagement").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Configuration
    @EnableWebMvc
    @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(prePostEnabled = true)
    static class TestConfig {
        @Bean
        org.springframework.security.web.SecurityFilterChain securityFilterChain(
                org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .httpBasic(org.springframework.security.config.Customizer.withDefaults())
                    .csrf(csrf -> csrf.disable());
            return http.build();
        }

        @Bean
        SampleTypeManagementRestController sampleTypeManagementRestController() {
            return new SampleTypeManagementRestController();
        }

        @Bean
        UserModuleService userModuleService() {
            return mock(UserModuleService.class);
        }

        @Bean
        PageBuilderService pageBuilderService() {
            return mock(PageBuilderService.class);
        }
    }
}
