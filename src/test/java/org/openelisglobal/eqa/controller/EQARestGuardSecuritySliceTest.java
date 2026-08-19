package org.openelisglobal.eqa.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.config.ControllerSetup;
import org.openelisglobal.eqa.controller.rest.EQAMyProgramsRestController;
import org.openelisglobal.eqa.controller.rest.EQAPanelRestController;
import org.openelisglobal.eqa.controller.rest.EQAProgramRestController;
import org.openelisglobal.eqa.service.EQALabProgramEnrollmentService;
import org.openelisglobal.eqa.service.EQAPanelService;
import org.openelisglobal.eqa.service.EQAProgramEnrollmentService;
import org.openelisglobal.eqa.service.EQAProgramService;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.login.valueholder.UserSessionData;
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
 * OGC-609 — behavioral proof of the four EQA guard shapes through the real
 * filter chain and method security, one representative endpoint per shape:
 * reads on the {@code qa.view.eqa} umbrella, participant-lane writes on the
 * participant tier, provider-lane writes on the provider tier, and unblinding
 * on its dedicated tier. The exhaustive per-endpoint matrix is covered by
 * {@link EQARestGuardMatrixTest}; this slice proves the expressions those
 * annotations use actually admit and refuse the right callers, in the right
 * order (authorization before handler logic).
 */
@WebAppConfiguration
@ContextConfiguration(classes = { EQARestGuardSecuritySliceTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class EQARestGuardSecuritySliceTest extends SecuritySliceMockMvcTest {

    /** Session principal for handlers that resolve the acting user. */
    private UserSessionData sessionUser() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(7); // sic — the setter is misspelled in UserSessionData
        return usd;
    }

    // ---- read umbrella ----

    @Test
    public void read_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/eqa/my-programs")).andExpect(status().isUnauthorized());
    }

    @Test
    public void read_legacyRoleAloneNoLongerAdmits() throws Exception {
        // Pre-T-05 the RECEPTION/RESULTS role name was the whole gate. Now the
        // qa.view.eqa authority (derived from the grant matrix at login) is
        // what admits a read.
        mockMvc.perform(get("/rest/eqa/my-programs").with(user("reception").roles("RECEPTION")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void read_viewEqaAuthorityReturns200() throws Exception {
        mockMvc.perform(get("/rest/eqa/my-programs")
                .with(user("qaofficer").authorities(new SimpleGrantedAuthority("qa.view.eqa"))))
                .andExpect(status().isOk());
    }

    @Test
    public void read_globalAdminFallbackReturns200() throws Exception {
        mockMvc.perform(get("/rest/eqa/my-programs").with(user("admin").roles("GLOBAL_ADMIN")))
                .andExpect(status().isOk());
    }

    // ---- participant-lane writes ----

    @Test
    public void participantWrite_readUmbrellaCannotWrite() throws Exception {
        // Auth ordering: qa.view.eqa admits reads everywhere but must stop at
        // the first write.
        mockMvc.perform(delete("/rest/eqa/my-programs/9")
                .with(user("viewer").authorities(new SimpleGrantedAuthority("qa.view.eqa"))))
                .andExpect(status().isForbidden());
    }

    @Test
    public void participantWrite_participantTierReturns204() throws Exception {
        mockMvc.perform(delete("/rest/eqa/my-programs/9")
                .with(user("qaofficer").authorities(new SimpleGrantedAuthority("qa.eqa.participant"))))
                .andExpect(status().isNoContent());
    }

    @Test
    public void participantWrite_legacyRoleAloneNoLongerAdmits() throws Exception {
        // Participant-lane actions stay open to the bench, but as a grant
        // (qa/026 gives Reception and Results the tier) rather than a role clause
        // in nine annotations. A principal holding only the role name — an SSO
        // login whose grants have not been applied — is refused, which is the
        // same rule the read umbrella follows.
        mockMvc.perform(delete("/rest/eqa/my-programs/9").with(user("bench").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    // ---- provider-lane writes ----

    @Test
    public void providerWrite_benchAndParticipantTierRefused() throws Exception {
        mockMvc.perform(post("/rest/eqa/programs").contentType(MediaType.APPLICATION_JSON).content("{}")
                .with(user("bench").authorities(new SimpleGrantedAuthority("qa.eqa.participant"),
                        new SimpleGrantedAuthority("qa.view.eqa"), new SimpleGrantedAuthority("ROLE_RECEPTION"),
                        new SimpleGrantedAuthority("ROLE_RESULTS"))))
                .andExpect(status().isForbidden());
    }

    @Test
    public void providerWrite_providerTierReturns200() throws Exception {
        mockMvc.perform(post("/rest/eqa/programs").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"HIV VL PT\"}").sessionAttr(IActionConstants.USER_SESSION_DATA, sessionUser())
                .with(user("provider").authorities(new SimpleGrantedAuthority("qa.eqa.provider"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("HIV VL PT"));
    }

    // ---- unblind tier ----

    @Test
    public void unblind_manageGrantIsNotEnough() throws Exception {
        // Lifecycle control and target visibility are separate privileges: the
        // qa.manage.eqa holder can seal and distribute but must not unblind.
        // The principal also holds the read umbrella, so the only thing that can
        // produce the 403 is the unblind tier guard itself — without it the class
        // guard would refuse this caller anyway and the test would pass on a
        // deleted annotation.
        mockMvc.perform(post("/rest/eqa/panels/5/unblind").with(user("coordinator")
                .authorities(new SimpleGrantedAuthority("qa.view.eqa"), new SimpleGrantedAuthority("qa.manage.eqa"))))
                .andExpect(status().isForbidden());
    }

    @Test
    public void unblind_readUmbrellaAloneIsNotEnough() throws Exception {
        mockMvc.perform(post("/rest/eqa/panels/5/unblind")
                .with(user("viewer").authorities(new SimpleGrantedAuthority("qa.view.eqa"))))
                .andExpect(status().isForbidden());
    }

    @Test
    public void unblind_dedicatedTierReturns200() throws Exception {
        mockMvc.perform(
                post("/rest/eqa/panels/5/unblind").sessionAttr(IActionConstants.USER_SESSION_DATA, sessionUser())
                        .with(user("unblinder").authorities(new SimpleGrantedAuthority("qa.eqa.inhouse.unblind"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(5));
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
        EQALabProgramEnrollmentService labProgramEnrollmentService() {
            EQALabProgramEnrollmentService service = Mockito.mock(EQALabProgramEnrollmentService.class);
            Mockito.when(service.findAll()).thenReturn(List.of());
            return service;
        }

        @Bean
        EQAProgramService programService() {
            EQAProgramService service = Mockito.mock(EQAProgramService.class);
            EQAProgram program = new EQAProgram();
            program.setName("HIV VL PT");
            program.setIsActive(true);
            Mockito.when(service.insert(any(EQAProgram.class))).thenReturn(1L);
            Mockito.when(service.get(eq(1L))).thenReturn(program);
            return service;
        }

        @Bean
        EQAProgramEnrollmentService programEnrollmentService() {
            return Mockito.mock(EQAProgramEnrollmentService.class);
        }

        @Bean
        EQAPanelService panelService() {
            EQAPanelService service = Mockito.mock(EQAPanelService.class);
            Mockito.when(service.unblind(anyLong(), any())).thenReturn(new EQAPanel());
            Mockito.when(service.toPanelDto(any())).thenReturn(Map.of("id", 5));
            return service;
        }

        @Bean
        EQAMyProgramsRestController myProgramsRestController(EQALabProgramEnrollmentService enrollmentService) {
            EQAMyProgramsRestController controller = new EQAMyProgramsRestController();
            ReflectionTestUtils.setField(controller, "enrollmentService", enrollmentService);
            return controller;
        }

        @Bean
        EQAProgramRestController programRestController(EQAProgramService programService,
                EQAProgramEnrollmentService programEnrollmentService) {
            EQAProgramRestController controller = new EQAProgramRestController();
            ReflectionTestUtils.setField(controller, "programService", programService);
            ReflectionTestUtils.setField(controller, "enrollmentService", programEnrollmentService);
            return controller;
        }

        @Bean
        EQAPanelRestController panelRestController(EQAPanelService panelService) {
            return new EQAPanelRestController(panelService);
        }

        @Bean
        ControllerSetup controllerSetup() {
            // The real @ControllerAdvice sits in the slice so @PreAuthorize
            // denials route through it exactly as in production (it used to
            // turn them into 500s — the 403 assertions guard that ordering).
            return new ControllerSetup();
        }
    }
}
