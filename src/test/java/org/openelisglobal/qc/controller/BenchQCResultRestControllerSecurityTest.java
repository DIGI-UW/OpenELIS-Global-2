package org.openelisglobal.qc.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.config.ControllerSetup;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.qc.service.QCResultService;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * OGC-1025 — {@code POST /rest/qc/results} is a results-entry <em>write</em>,
 * so it takes the RESULTS role like the rest of results entry, not the
 * {@code qa.view.qc} viewing permission it launched with under OGC-1147. The
 * inversion case (a QC-view-only user is refused) is the point of the change.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { BenchQCResultRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class BenchQCResultRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final String URL = "/rest/qc/results";
    private static final String RDT_INVALID_BODY = "{\"source\":\"RDT\",\"qualitativeOutcome\":\"INVALID\","
            + "\"testId\":\"13\",\"testSectionId\":\"36\",\"controlLabel\":\"UAT kit\"}";

    @Before
    public void resetStub() {
        QCResultService service = webApplicationContext.getBean(QCResultService.class);
        org.mockito.Mockito.reset(service);
        when(service.createBenchQCResult(any(), anyInt())).thenReturn(new QCResult());
    }

    private UserSessionData sessionUser() {
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
        return userSessionData;
    }

    @Test
    public void record_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(RDT_INVALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void record_qcViewAuthorityAloneReturns403() throws Exception {
        // the OGC-1147 launch gate — viewing QC must no longer permit writing it
        mockMvc.perform(post(URL).with(user("qc").authorities(new SimpleGrantedAuthority("qa.view.qc")))
                .contentType(MediaType.APPLICATION_JSON).content(RDT_INVALID_BODY)).andExpect(status().isForbidden());
    }

    @Test
    public void record_nonResultsRoleReturns403() throws Exception {
        mockMvc.perform(post(URL).with(user("front").roles("RECEPTION")).contentType(MediaType.APPLICATION_JSON)
                .content(RDT_INVALID_BODY)).andExpect(status().isForbidden());
    }

    @Test
    public void record_resultsRoleReturns201() throws Exception {
        mockMvc.perform(post(URL).with(user("tech").roles("RESULTS"))
                .sessionAttr(IActionConstants.USER_SESSION_DATA, sessionUser()).contentType(MediaType.APPLICATION_JSON)
                .content(RDT_INVALID_BODY)).andExpect(status().isCreated());
    }

    @Test
    public void record_globalAdminFallbackReturns201() throws Exception {
        mockMvc.perform(post(URL).with(user("admin").roles("GLOBAL_ADMIN"))
                .sessionAttr(IActionConstants.USER_SESSION_DATA, sessionUser()).contentType(MediaType.APPLICATION_JSON)
                .content(RDT_INVALID_BODY)).andExpect(status().isCreated());
    }

    @Test
    public void record_rejectedCaptureReturns400WithJsonMessage() throws Exception {
        QCResultService service = webApplicationContext.getBean(QCResultService.class);
        when(service.createBenchQCResult(any(), anyInt()))
                .thenThrow(new IllegalArgumentException("An RDT control cannot carry a numeric value"));

        // a JSON-object body, not a bare string — the capture form's fetch
        // helper parses every response as JSON and surfaces `message` verbatim
        mockMvc.perform(post(URL).with(user("tech").roles("RESULTS"))
                .sessionAttr(IActionConstants.USER_SESSION_DATA, sessionUser()).contentType(MediaType.APPLICATION_JSON)
                .content(RDT_INVALID_BODY)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("An RDT control cannot carry a numeric value"));
    }

    @Test
    public void record_rejectionWithNoMessageStillReturns400() throws Exception {
        QCResultService service = webApplicationContext.getBean(QCResultService.class);
        when(service.createBenchQCResult(any(), anyInt())).thenThrow(new IllegalArgumentException());

        // Map.of rejects a null value — an exception with no message must not
        // turn the intended 400 into a 500
        mockMvc.perform(post(URL).with(user("tech").roles("RESULTS"))
                .sessionAttr(IActionConstants.USER_SESSION_DATA, sessionUser()).contentType(MediaType.APPLICATION_JSON)
                .content(RDT_INVALID_BODY)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid control result"));
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
        QCResultService qcResultService() {
            QCResultService service = mock(QCResultService.class);
            when(service.createBenchQCResult(any(), anyInt())).thenReturn(new QCResult());
            return service;
        }

        @Bean
        BenchQCResultRestController benchQCResultRestController(QCResultService qcResultService) {
            BenchQCResultRestController controller = new BenchQCResultRestController();
            ReflectionTestUtils.setField(controller, "qcResultService", qcResultService);
            return controller;
        }

        @Bean
        ControllerSetup controllerSetup() {
            // Real @ControllerAdvice so @PreAuthorize denials surface as 403s, not 500s.
            return new ControllerSetup();
        }
    }
}
