package org.openelisglobal.microbiology.controller.rest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.form.MicroCaseActivityForm;
import org.openelisglobal.microbiology.service.MicroCaseTimelineService;
import org.openelisglobal.microbiology.service.MicroReportProjectionService;
import org.openelisglobal.microbiology.service.MicroReportReleaseService;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { MicrobiologyBenchRestControllerSecurityTest.TestConfig.class })
public class MicrobiologyBenchRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Autowired
    private MicroCaseTimelineService timelineService;

    @Autowired
    private MicroReportReleaseService releaseService;

    @Test
    public void unrelatedAuthenticatedRoleCannotReadBenchCaseData() throws Exception {
        mockMvc.perform(get("/rest/microbiology/cases/case-1/timeline").with(user("reception").roles("RECEPTION")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void noteUsesSessionActorAndIgnoresSubmittedActorField() throws Exception {
        MicroCaseActivityForm activity = new MicroCaseActivityForm();
        activity.id = "activity-1";
        when(timelineService.addNote("case-1", "Bench note", "42")).thenReturn(activity);

        mockMvc.perform(post("/rest/microbiology/cases/case-1/notes").with(user("analyst").roles("RESULTS"))
                .sessionAttr(IActionConstants.USER_SESSION_DATA, sessionFor(42)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"Bench note\",\"performedBy\":\"999\"}")).andExpect(status().isOk());

        verify(timelineService).addNote("case-1", "Bench note", "42");
    }

    @Test
    public void resultsRoleCannotReleaseFinalReport() throws Exception {
        mockMvc.perform(post("/rest/microbiology/cases/case-1/release/final").with(user("analyst").roles("RESULTS"))
                .sessionAttr(IActionConstants.USER_SESSION_DATA, sessionFor(42)).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isForbidden());
    }

    @Test
    public void validationRoleReleasesFinalWithSessionActor() throws Exception {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        when(releaseService.releaseFinal(eq("case-1"), eq("42"))).thenReturn(microCase);

        mockMvc.perform(
                post("/rest/microbiology/cases/case-1/release/final").with(user("validator").roles("VALIDATION"))
                        .sessionAttr(IActionConstants.USER_SESSION_DATA, sessionFor(42))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"performedBy\":\"999\"}"))
                .andExpect(status().isOk());

        verify(releaseService).releaseFinal("case-1", "42");
    }

    private UserSessionData sessionFor(int userId) {
        UserSessionData session = new UserSessionData();
        session.setSytemUserId(userId);
        return session;
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
        MicroCaseTimelineService microCaseTimelineService() {
            return mock(MicroCaseTimelineService.class);
        }

        @Bean
        MicroReportReleaseService microReportReleaseService() {
            return mock(MicroReportReleaseService.class);
        }

        @Bean
        MicroReportProjectionService microReportProjectionService() {
            return mock(MicroReportProjectionService.class);
        }

        @Bean
        MicroCaseTimelineRestController microCaseTimelineRestController(MicroCaseTimelineService service) {
            return new MicroCaseTimelineRestController(service);
        }

        @Bean
        MicroReportReleaseRestController microReportReleaseRestController(MicroReportReleaseService releaseService,
                MicroReportProjectionService projectionService) {
            return new MicroReportReleaseRestController(releaseService, projectionService);
        }
    }
}
