package org.openelisglobal.common.management.controller.rest;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.sampletypeterminology.service.SampleTypeTerminologyMappingService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
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
@ContextConfiguration(classes = { SampleTypeManagementRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class SampleTypeManagementRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void testSampleTypeManagement_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/rest/sample-types").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testSampleTypeManagement_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(get("/rest/sample-types").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void testSampleTypeManagement_AdminRole_Returns200() throws Exception {
        mockMvc.perform(
                get("/rest/sample-types").with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // Mutating + terminology endpoints must carry the same ADMIN gate. A
    // non-admin PUT returning 403 also pins the hasRole('ADMIN') convention —
    // hasRole('ROLE_ADMIN') would have double-prefixed and locked admins out.

    @Test
    public void updateSampleType_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(put("/rest/sample-types/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void updateSampleType_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(put("/rest/sample-types/1").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test
    public void updateSampleType_AdminRole_PassesAuth() throws Exception {
        // the mocked service returns null → 404: the request cleared the auth gate
        mockMvc.perform(put("/rest/sample-types/1").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isNotFound());
    }

    @Test
    public void getSampleTypeById_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(get("/rest/sample-types/1").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void getSampleTypeById_AdminRole_PassesAuth() throws Exception {
        // the mocked service returns null → 404: the request cleared the auth gate
        mockMvc.perform(
                get("/rest/sample-types/1").with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void updateDisplayOrder_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(put("/rest/sample-types/1/display-order").contentType(MediaType.APPLICATION_JSON)
                .content("{\"position\":1}")).andExpect(status().isUnauthorized());
    }

    @Test
    public void updateDisplayOrder_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(put("/rest/sample-types/1/display-order").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"position\":1}")).andExpect(status().isForbidden());
    }

    @Test
    public void updateDisplayOrder_AdminRole_PassesAuth() throws Exception {
        // the mocked service returns null → 404: the request cleared the auth gate
        mockMvc.perform(put("/rest/sample-types/1/display-order").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"position\":1}")).andExpect(status().isNotFound());
    }

    @Test
    public void updateDisplayOrder_AdminRole_InvalidPosition_Returns422() throws Exception {
        mockMvc.perform(put("/rest/sample-types/1/display-order").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"position\":0}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void terminology_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(get("/rest/sample-types/1/terminology").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
        mockMvc.perform(put("/rest/sample-types/1/terminology").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test
    public void terminology_AdminRole_PassesAuth() throws Exception {
        mockMvc.perform(get("/rest/sample-types/1/terminology").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    // Bidirectional associated-tests endpoints carry the same ADMIN gate.

    @Test
    public void associatedTests_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(get("/rest/sample-types/1/associable-tests").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
        mockMvc.perform(put("/rest/sample-types/1/tests/2").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/rest/sample-types/1/tests/2").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void associatedTests_AdminRole_PassesAuth() throws Exception {
        // mocked service returns null → 404: the request cleared the auth gate
        mockMvc.perform(get("/rest/sample-types/1/associable-tests").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
        mockMvc.perform(put("/rest/sample-types/1/tests/2").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isNotFound());
        mockMvc.perform(delete("/rest/sample-types/1/tests/2").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
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
        TypeOfSampleService typeOfSampleService() {
            return mock(TypeOfSampleService.class);
        }

        @Bean
        SampleTypeTerminologyMappingService sampleTypeTerminologyMappingService() {
            return mock(SampleTypeTerminologyMappingService.class);
        }

        @Bean
        org.openelisglobal.test.service.TestService testService() {
            return mock(org.openelisglobal.test.service.TestService.class);
        }

        @Bean
        org.openelisglobal.typeofsample.service.TypeOfSampleTestService typeOfSampleTestService() {
            return mock(org.openelisglobal.typeofsample.service.TypeOfSampleTestService.class);
        }

        @Bean
        SampleTypeManagementRestController sampleTypeManagementRestController(TypeOfSampleService typeOfSampleService,
                SampleTypeTerminologyMappingService sampleTypeTerminologyMappingService,
                org.openelisglobal.test.service.TestService testService,
                org.openelisglobal.typeofsample.service.TypeOfSampleTestService typeOfSampleTestService) {
            SampleTypeManagementRestController controller = new SampleTypeManagementRestController();
            ReflectionTestUtils.setField(controller, "typeOfSampleService", typeOfSampleService);
            ReflectionTestUtils.setField(controller, "terminologyService", sampleTypeTerminologyMappingService);
            ReflectionTestUtils.setField(controller, "testService", testService);
            ReflectionTestUtils.setField(controller, "typeOfSampleTestService", typeOfSampleTestService);
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
