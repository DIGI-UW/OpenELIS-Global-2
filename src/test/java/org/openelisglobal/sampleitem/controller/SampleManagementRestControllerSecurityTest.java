package org.openelisglobal.sampleitem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.sample.service.SampleLabUnitScopeService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.dto.AddTestsResponse;
import org.openelisglobal.sampleitem.dto.CancelTestResponse;
import org.openelisglobal.sampleitem.dto.CreateAliquotResponse;
import org.openelisglobal.sampleitem.dto.SampleItemDTO;
import org.openelisglobal.sampleitem.dto.SearchSamplesResponse;
import org.openelisglobal.sampleitem.service.SampleManagementService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebAppConfiguration
@ContextConfiguration(classes = { SampleManagementRestControllerSecurityTest.TestConfig.class })
public class SampleManagementRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static MockHttpSession sessionForSysUser(int sysUserId) {
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(sysUserId);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return session;
    }

    // --- searchSamplesByAccessionNumber (GET /rest/sample-management/search) ---

    @Test
    public void searchSamples_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/rest/sample-management/search").param("accessionNumber", "ACC123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void searchSamples_unrelatedRole_returns403() throws Exception {
        mockMvc.perform(get("/rest/sample-management/search").param("accessionNumber", "ACC123")
                .with(user("rep").roles("REPORTS"))).andExpect(status().isForbidden());
    }

    @Test
    public void searchSamples_receptionRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/sample-management/search").param("accessionNumber", "ACC123")
                .session(sessionForSysUser(1)).with(user("rec").roles("RECEPTION"))).andExpect(status().isOk());
    }

    @Test
    public void searchSamples_resultsRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/sample-management/search").param("accessionNumber", "ACC123")
                .session(sessionForSysUser(1)).with(user("res").roles("RESULTS"))).andExpect(status().isOk());
    }

    @Test
    public void searchSamples_adminRole_returns200() throws Exception {
        mockMvc.perform(get("/rest/sample-management/search").param("accessionNumber", "ACC123")
                .session(sessionForSysUser(1)).with(user("adm").roles("ADMIN"))).andExpect(status().isOk());
    }

    // --- createAliquot (POST /rest/sample-management/aliquot) ---

    @Test
    public void createAliquot_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/rest/sample-management/aliquot").contentType(MediaType.APPLICATION_JSON)
                .content("{\"parentSampleItemId\":\"1\",\"quantityToTransfer\":1.0}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void createAliquot_unrelatedRole_returns403() throws Exception {
        mockMvc.perform(post("/rest/sample-management/aliquot").contentType(MediaType.APPLICATION_JSON)
                .content("{\"parentSampleItemId\":\"1\",\"quantityToTransfer\":1.0}")
                .with(user("rep").roles("REPORTS"))).andExpect(status().isForbidden());
    }

    @Test
    public void createAliquot_receptionRole_returns201() throws Exception {
        mockMvc.perform(post("/rest/sample-management/aliquot").contentType(MediaType.APPLICATION_JSON)
                .content("{\"parentSampleItemId\":\"1\",\"quantityToTransfer\":1.0}").session(sessionForSysUser(1))
                .with(user("rec").roles("RECEPTION"))).andExpect(status().isCreated());
    }

    @Test
    public void createAliquot_resultsRole_returns201() throws Exception {
        mockMvc.perform(post("/rest/sample-management/aliquot").contentType(MediaType.APPLICATION_JSON)
                .content("{\"parentSampleItemId\":\"1\",\"quantityToTransfer\":1.0}").session(sessionForSysUser(1))
                .with(user("res").roles("RESULTS"))).andExpect(status().isCreated());
    }

    @Test
    public void createAliquot_adminRole_returns201() throws Exception {
        mockMvc.perform(post("/rest/sample-management/aliquot").contentType(MediaType.APPLICATION_JSON)
                .content("{\"parentSampleItemId\":\"1\",\"quantityToTransfer\":1.0}").session(sessionForSysUser(1))
                .with(user("adm").roles("ADMIN"))).andExpect(status().isCreated());
    }

    // --- addTestsToSamples (POST /rest/sample-management/add-tests) ---

    @Test
    public void addTests_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/rest/sample-management/add-tests").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sampleItemIds\":[\"1\"],\"testIds\":[\"2\"]}")).andExpect(status().isUnauthorized());
    }

    @Test
    public void addTests_unrelatedRole_returns403() throws Exception {
        mockMvc.perform(post("/rest/sample-management/add-tests").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sampleItemIds\":[\"1\"],\"testIds\":[\"2\"]}").with(user("rep").roles("REPORTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void addTests_receptionRole_returns200() throws Exception {
        mockMvc.perform(post("/rest/sample-management/add-tests").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sampleItemIds\":[\"1\"],\"testIds\":[\"2\"]}").session(sessionForSysUser(1))
                .with(user("rec").roles("RECEPTION"))).andExpect(status().isOk());
    }

    @Test
    public void addTests_resultsRole_returns200() throws Exception {
        mockMvc.perform(post("/rest/sample-management/add-tests").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sampleItemIds\":[\"1\"],\"testIds\":[\"2\"]}").session(sessionForSysUser(1))
                .with(user("res").roles("RESULTS"))).andExpect(status().isOk());
    }

    @Test
    public void addTests_adminRole_returns200() throws Exception {
        mockMvc.perform(post("/rest/sample-management/add-tests").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sampleItemIds\":[\"1\"],\"testIds\":[\"2\"]}").session(sessionForSysUser(1))
                .with(user("adm").roles("ADMIN"))).andExpect(status().isOk());
    }

    // --- cancelTest (POST /rest/sample-management/cancel-test) ---

    @Test
    public void cancelTest_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/rest/sample-management/cancel-test").contentType(MediaType.APPLICATION_JSON)
                .content("{\"analysisId\":\"1\",\"sampleItemId\":\"2\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    public void cancelTest_unrelatedRole_returns403() throws Exception {
        mockMvc.perform(post("/rest/sample-management/cancel-test").contentType(MediaType.APPLICATION_JSON)
                .content("{\"analysisId\":\"1\",\"sampleItemId\":\"2\"}").with(user("rep").roles("REPORTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void cancelTest_receptionRole_returns200() throws Exception {
        mockMvc.perform(post("/rest/sample-management/cancel-test").contentType(MediaType.APPLICATION_JSON)
                .content("{\"analysisId\":\"1\",\"sampleItemId\":\"2\"}").session(sessionForSysUser(1))
                .with(user("rec").roles("RECEPTION"))).andExpect(status().isOk());
    }

    @Test
    public void cancelTest_resultsRole_returns200() throws Exception {
        mockMvc.perform(post("/rest/sample-management/cancel-test").contentType(MediaType.APPLICATION_JSON)
                .content("{\"analysisId\":\"1\",\"sampleItemId\":\"2\"}").session(sessionForSysUser(1))
                .with(user("res").roles("RESULTS"))).andExpect(status().isOk());
    }

    @Test
    public void cancelTest_adminRole_returns200() throws Exception {
        mockMvc.perform(post("/rest/sample-management/cancel-test").contentType(MediaType.APPLICATION_JSON)
                .content("{\"analysisId\":\"1\",\"sampleItemId\":\"2\"}").session(sessionForSysUser(1))
                .with(user("adm").roles("ADMIN"))).andExpect(status().isOk());
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
        SampleManagementService sampleManagementService() {
            SampleManagementService service = mock(SampleManagementService.class);
            when(service.searchByAccessionNumber(anyString(), anyBoolean())).thenReturn(new SearchSamplesResponse());
            CreateAliquotResponse aliquotResp = new CreateAliquotResponse();
            aliquotResp.setAliquot(new SampleItemDTO());
            when(service.createAliquot(any(), anyString())).thenReturn(aliquotResp);
            AddTestsResponse addTestsResp = new AddTestsResponse();
            addTestsResp.setSuccessCount(1);
            when(service.addTestsToSamples(any(), anyString())).thenReturn(addTestsResp);
            CancelTestResponse cancelResp = new CancelTestResponse();
            cancelResp.setSuccess(true);
            cancelResp.setTestName("Test1");
            when(service.cancelTest(any(), anyString())).thenReturn(cancelResp);
            return service;
        }

        @Bean
        SampleService sampleService() {
            SampleService service = mock(SampleService.class);
            Sample sample = new Sample();
            sample.setId("1");
            sample.setAccessionNumber("ACC123");
            when(service.getSampleByAccessionNumber(anyString())).thenReturn(sample);
            return service;
        }

        @Bean
        SampleLabUnitScopeService sampleLabUnitScopeService() {
            SampleLabUnitScopeService service = mock(SampleLabUnitScopeService.class);
            when(service.isVisible(anyString(), any())).thenReturn(true);
            when(service.isSampleItemVisible(anyString(), anyString())).thenReturn(true);
            return service;
        }

        @Bean
        SampleManagementRestController sampleManagementRestController(SampleManagementService sampleManagementService,
                SampleService sampleService, SampleLabUnitScopeService sampleLabUnitScopeService) {
            SampleManagementRestController controller = new SampleManagementRestController();
            ReflectionTestUtils.setField(controller, "sampleManagementService", sampleManagementService);
            ReflectionTestUtils.setField(controller, "sampleService", sampleService);
            ReflectionTestUtils.setField(controller, "sampleLabUnitScopeService", sampleLabUnitScopeService);
            return controller;
        }
    }
}
