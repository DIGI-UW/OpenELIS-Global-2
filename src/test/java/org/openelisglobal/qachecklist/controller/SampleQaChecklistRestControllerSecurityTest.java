package org.openelisglobal.qachecklist.controller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.qachecklist.service.SampleQaChecklistService;
import org.openelisglobal.qachecklist.valueholder.SampleQaChecklist;
import org.openelisglobal.sample.service.OrderProgressService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebAppConfiguration
@ContextConfiguration(classes = { SampleQaChecklistRestControllerSecurityTest.TestConfig.class })
public class SampleQaChecklistRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static MockHttpSession sessionForSysUser(int sysUserId) {
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(sysUserId);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return session;
    }

    // --- getChecklistConfig (GET /rest/qa-checklist/config) ---

    @Test
    public void getChecklistConfig_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/rest/qa-checklist/config")).andExpect(status().isUnauthorized());
    }

    @Test
    public void getChecklistConfig_unrelatedRole_returns403() throws Exception {
        mockMvc.perform(get("/rest/qa-checklist/config").with(user("res").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getChecklistConfig_receptionRole_passesSecurity() throws Exception {
        assertPassesSecurity(get("/rest/qa-checklist/config").with(user("rec").roles("RECEPTION")));
    }

    @Test
    public void getChecklistConfig_adminRole_passesSecurity() throws Exception {
        assertPassesSecurity(get("/rest/qa-checklist/config").with(user("adm").roles("ADMIN")));
    }

    // --- getQaChecklist (GET /rest/qa-checklist/{sampleId}) ---

    @Test
    public void getQaChecklist_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/rest/qa-checklist/1")).andExpect(status().isUnauthorized());
    }

    @Test
    public void getQaChecklist_unrelatedRole_returns403() throws Exception {
        mockMvc.perform(get("/rest/qa-checklist/1").with(user("res").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getQaChecklist_receptionRole_passesSecurity() throws Exception {
        assertPassesSecurity(get("/rest/qa-checklist/1").with(user("rec").roles("RECEPTION")));
    }

    @Test
    public void getQaChecklist_adminRole_passesSecurity() throws Exception {
        assertPassesSecurity(get("/rest/qa-checklist/1").with(user("adm").roles("ADMIN")));
    }

    // --- getQaChecklistByLabNumber (GET
    // /rest/qa-checklist/by-lab-number/{labNumber}) ---

    @Test
    public void getQaChecklistByLabNumber_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/rest/qa-checklist/by-lab-number/LAB123")).andExpect(status().isUnauthorized());
    }

    @Test
    public void getQaChecklistByLabNumber_unrelatedRole_returns403() throws Exception {
        mockMvc.perform(get("/rest/qa-checklist/by-lab-number/LAB123").with(user("res").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getQaChecklistByLabNumber_receptionRole_passesSecurity() throws Exception {
        assertPassesSecurity(get("/rest/qa-checklist/by-lab-number/LAB123").with(user("rec").roles("RECEPTION")));
    }

    @Test
    public void getQaChecklistByLabNumber_adminRole_passesSecurity() throws Exception {
        assertPassesSecurity(get("/rest/qa-checklist/by-lab-number/LAB123").with(user("adm").roles("ADMIN")));
    }

    // --- saveQaChecklist (POST /rest/qa-checklist) ---

    @Test
    public void saveQaChecklist_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/rest/qa-checklist").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sampleId\":1,\"verifiedItems\":{}}")).andExpect(status().isUnauthorized());
    }

    @Test
    public void saveQaChecklist_unrelatedRole_returns403() throws Exception {
        mockMvc.perform(post("/rest/qa-checklist").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sampleId\":1,\"verifiedItems\":{}}").with(user("res").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void saveQaChecklist_receptionRole_passesSecurity() throws Exception {
        assertPassesSecurity(post("/rest/qa-checklist").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sampleId\":1,\"verifiedItems\":{}}").session(sessionForSysUser(1))
                .with(user("rec").roles("RECEPTION")));
    }

    @Test
    public void saveQaChecklist_adminRole_passesSecurity() throws Exception {
        assertPassesSecurity(post("/rest/qa-checklist").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sampleId\":1,\"verifiedItems\":{}}").session(sessionForSysUser(1))
                .with(user("adm").roles("ADMIN")));
    }

    /**
     * The handler may fail on what the slice does not mock; getting that far still
     * means the request passed the role check.
     */
    private void assertPassesSecurity(MockHttpServletRequestBuilder request) throws Exception {
        try {
            int status = mockMvc.perform(request).andReturn().getResponse().getStatus();
            assertNotEquals(401, status);
            assertNotEquals(403, status);
        } catch (ServletException e) {
            assertFalse(e.getCause() instanceof AccessDeniedException);
        }
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
        SampleQaChecklistService sampleQaChecklistService() {
            SampleQaChecklistService service = mock(SampleQaChecklistService.class);
            when(service.getActiveChecklistItems()).thenReturn(Collections.emptyList());
            when(service.findBySampleId(anyString())).thenReturn(null);
            SampleQaChecklist checklist = new SampleQaChecklist();
            checklist.setId(1);
            checklist.setSampleId(1);
            checklist.setVerifiedItems(Collections.emptyMap());
            checklist.setAllRequiredVerified(true);
            when(service.saveOrUpdateChecklist(any(), any(), any())).thenReturn(checklist);
            return service;
        }

        @Bean
        SampleService sampleService() {
            SampleService service = mock(SampleService.class);
            Sample sample = new Sample();
            sample.setId("1");
            sample.setAccessionNumber("LAB123");
            when(service.getSampleByAccessionNumber(anyString())).thenReturn(sample);
            return service;
        }

        @Bean
        OrderProgressService orderProgressService() {
            return mock(OrderProgressService.class);
        }

        @Bean
        SampleQaChecklistRestController sampleQaChecklistRestController(
                SampleQaChecklistService sampleQaChecklistService, SampleService sampleService,
                OrderProgressService orderProgressService, HttpServletRequest httpServletRequest) {
            SampleQaChecklistRestController controller = new SampleQaChecklistRestController();
            ReflectionTestUtils.setField(controller, "sampleQaChecklistService", sampleQaChecklistService);
            ReflectionTestUtils.setField(controller, "sampleService", sampleService);
            ReflectionTestUtils.setField(controller, "orderProgressService", orderProgressService);
            ReflectionTestUtils.setField(controller, "httpRequest", httpServletRequest);
            return controller;
        }
    }
}
