package org.openelisglobal.common.management.controller.rest;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.SupportedLocaleService;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testconfiguration.service.TestSectionCreateService;
import org.openelisglobal.testconfiguration.service.TestSectionTestAssignService;
import org.openelisglobal.view.PageBuilderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebAppConfiguration
@ContextConfiguration(classes = { LabUnitManagementRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class LabUnitManagementRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void getLabUnits_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/rest/lab-units-management").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getLabUnits_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(get("/rest/lab-units-management").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void getLabUnits_AdminRole_Returns200() throws Exception {
        mockMvc.perform(get("/rest/lab-units-management").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    // Mutating endpoints must carry the same ADMIN gate. A non-admin PUT
    // returning 403 also pins the hasRole('ADMIN') convention.

    @Test
    public void updateLabUnit_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(put("/rest/lab-units-management/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void updateLabUnit_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(put("/rest/lab-units-management/1").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test
    public void updateLabUnit_AdminRole_PassesAuth() throws Exception {
        // the mocked service returns null → 404: the request cleared the auth gate
        mockMvc.perform(put("/rest/lab-units-management/1").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isNotFound());
    }

    @Test
    public void getLabUnitById_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(get("/rest/lab-units-management/1").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void getLabUnitById_AdminRole_PassesAuth() throws Exception {
        // the mocked service returns null → 404: the request cleared the auth gate
        mockMvc.perform(get("/rest/lab-units-management/1").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    public void updateDisplayOrder_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(put("/rest/lab-units-management/1/display-order").contentType(MediaType.APPLICATION_JSON)
                .content("{\"position\":1}")).andExpect(status().isUnauthorized());
    }

    @Test
    public void updateDisplayOrder_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(put("/rest/lab-units-management/1/display-order").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"position\":1}")).andExpect(status().isForbidden());
    }

    @Test
    public void updateDisplayOrder_AdminRole_PassesAuth() throws Exception {
        // the mocked service returns null → 404: the request cleared the auth gate
        mockMvc.perform(put("/rest/lab-units-management/1/display-order").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"position\":1}")).andExpect(status().isNotFound());
    }

    @Test
    public void updateDisplayOrder_AdminRole_InvalidPosition_Returns422() throws Exception {
        mockMvc.perform(put("/rest/lab-units-management/1/display-order").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"position\":0}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void assignedTests_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(get("/rest/lab-units-management/1/tests").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void assignedTests_AdminRole_PassesAuth() throws Exception {
        // mocked service returns null → 404: the request cleared the auth gate
        mockMvc.perform(get("/rest/lab-units-management/1/tests").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    // Create + bulk assignment endpoints carry the same ADMIN gate.

    @Test
    public void createLabUnit_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(post("/rest/lab-units-management").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void createLabUnit_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(post("/rest/lab-units-management").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test
    public void createLabUnit_AdminRole_MissingFallbackName_Returns422() throws Exception {
        // empty body clears auth but fails validation (fallback-locale name required)
        mockMvc.perform(post("/rest/lab-units-management").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void assignableTests_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(get("/rest/lab-units-management/1/assignable-tests").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void assignableTests_AdminRole_PassesAuth() throws Exception {
        // mocked service returns null → 404: the request cleared the auth gate
        mockMvc.perform(get("/rest/lab-units-management/1/assignable-tests").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    public void bulkAssign_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(post("/rest/lab-units-management/1/tests/assign").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mockMvc.perform(post("/rest/lab-units-management/1/tests/reassign").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test
    public void bulkAssign_AdminRole_EmptyBody_Returns422() throws Exception {
        // empty body clears auth but fails validation (testIds required)
        mockMvc.perform(post("/rest/lab-units-management/1/tests/assign").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnprocessableEntity());
        mockMvc.perform(post("/rest/lab-units-management/1/tests/reassign").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void bulkAssign_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(
                post("/rest/lab-units-management/1/tests/assign").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
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
        TestSectionService testSectionService() {
            return mock(TestSectionService.class);
        }

        @Bean
        LocalizationService localizationService() {
            return mock(LocalizationService.class);
        }

        @Bean
        SupportedLocaleService supportedLocaleService() {
            return mock(SupportedLocaleService.class);
        }

        @Bean
        TestService testService() {
            return mock(TestService.class);
        }

        @Bean
        TestSectionCreateService testSectionCreateService() {
            return mock(TestSectionCreateService.class);
        }

        @Bean
        TestSectionTestAssignService testSectionTestAssignService() {
            return mock(TestSectionTestAssignService.class);
        }

        @Bean
        RoleService roleService() {
            return mock(RoleService.class);
        }

        @Bean
        LabUnitManagementRestController labUnitManagementRestController(TestSectionService testSectionService,
                LocalizationService localizationService, SupportedLocaleService supportedLocaleService,
                TestService testService, TestSectionCreateService testSectionCreateService,
                TestSectionTestAssignService testSectionTestAssignService, RoleService roleService) {
            LabUnitManagementRestController controller = new LabUnitManagementRestController();
            ReflectionTestUtils.setField(controller, "testSectionService", testSectionService);
            ReflectionTestUtils.setField(controller, "localizationService", localizationService);
            ReflectionTestUtils.setField(controller, "supportedLocaleService", supportedLocaleService);
            ReflectionTestUtils.setField(controller, "testService", testService);
            ReflectionTestUtils.setField(controller, "testSectionCreateService", testSectionCreateService);
            ReflectionTestUtils.setField(controller, "testSectionTestAssignService", testSectionTestAssignService);
            ReflectionTestUtils.setField(controller, "roleService", roleService);
            return controller;
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
