package org.openelisglobal.program.controller.pathology;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.program.service.ConsultationAnswer;
import org.openelisglobal.program.service.ConsultationDraft;
import org.openelisglobal.program.service.PathologyConsultationService;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.ProgramSampleLabUnitScope;
import org.openelisglobal.program.valueholder.pathology.ConsultationStatus;
import org.openelisglobal.program.valueholder.pathology.PathologyConsultation;
import org.openelisglobal.sample.service.SampleLabUnitScopeService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.testsupport.SliceSecurityConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.server.ResponseStatusException;

/**
 * Second opinions carry no permission key of their own: any signed-in user who
 * may read the case reaches the service, which holds the rules on who may
 * answer, and nobody else reaches it at all. The case is looked up for the
 * user's lab units before anything is written.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { SliceSecurityConfig.class,
        PathologyConsultationRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class PathologyConsultationRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final String REQUEST = "/rest/pathology/caseView/7/consultations";
    private static final String ANSWER = "/rest/pathology/caseView/7/consultations/12/answer";
    private static final String WITHDRAW = "/rest/pathology/caseView/7/consultations/12/withdraw";

    private static final String REQUEST_BODY = "{\"consultedUserId\":\"1003\",\"reasonId\":\"9401\","
            + "\"preTreatment\":true}";
    private static final String ANSWER_BODY = "{\"outcome\":\"DISAGREE\",\"note\":\"Looks reactive\"}";

    private static final Map<String, String> BODIES = Map.of(REQUEST, REQUEST_BODY, ANSWER, ANSWER_BODY, WITHDRAW,
            "{}");

    private PathologySampleService sampleService;
    private PathologyConsultationService service;
    private ProgramSampleLabUnitScope scope;

    @Before
    public void resetStubs() {
        sampleService = webApplicationContext.getBean(PathologySampleService.class);
        service = webApplicationContext.getBean(PathologyConsultationService.class);
        scope = webApplicationContext.getBean(ProgramSampleLabUnitScope.class);
        reset(sampleService, service, scope);
        when(service.request(any(), any(), any())).thenReturn(consultationAt(ConsultationStatus.REQUESTED));
        when(service.answer(any(), any(), any(), any())).thenReturn(consultationAt(ConsultationStatus.ANSWERED));
        when(service.withdraw(any(), any(), any())).thenReturn(consultationAt(ConsultationStatus.WITHDRAWN));
    }

    @Test
    public void anonymousIs401_onEachEndpoint_beforeTheService() throws Exception {
        for (Map.Entry<String, String> endpoint : BODIES.entrySet()) {
            mockMvc.perform(post(endpoint.getKey()).with(anonymous()).contentType(MediaType.APPLICATION_JSON)
                    .content(endpoint.getValue())).andExpect(status().isUnauthorized());
        }

        verify(sampleService, never()).get(any());
        verifyNothingReachedTheService();
    }

    @Test
    public void aCaseOutsideTheUsersUnits_is404_onEachEndpoint_beforeTheService() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)).when(scope).requireVisible(any(), any());

        for (Map.Entry<String, String> endpoint : BODIES.entrySet()) {
            signedIn(endpoint.getKey(), endpoint.getValue()).andExpect(status().isNotFound());
        }

        verifyNothingReachedTheService();
    }

    @Test
    public void theSignedInUser_andTheBody_reachTheService() throws Exception {
        signedIn(REQUEST, REQUEST_BODY).andExpect(status().isOk());
        signedIn(ANSWER, ANSWER_BODY).andExpect(status().isOk());
        signedIn(WITHDRAW, "{}").andExpect(status().isOk());

        verify(service).request(eq(7), eq(new ConsultationDraft("1003", "9401", true)), eq("1"));
        verify(service).answer(eq(7), eq(12), eq(new ConsultationAnswer("DISAGREE", "Looks reactive")), eq("1"));
        verify(service).withdraw(eq(7), eq(12), eq("1"));
    }

    // helpers

    private void verifyNothingReachedTheService() {
        verify(service, never()).request(any(), any(), any());
        verify(service, never()).answer(any(), any(), any(), any());
        verify(service, never()).withdraw(any(), any(), any());
    }

    private ResultActions signedIn(String endpoint, String body) throws Exception {
        return mockMvc.perform(
                post(endpoint).with(user("pathologist")).sessionAttr(IActionConstants.USER_SESSION_DATA, sessionUser())
                        .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static UserSessionData sessionUser() {
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
        return userSessionData;
    }

    private static PathologyConsultation consultationAt(ConsultationStatus status) {
        PathologyConsultation consultation = new PathologyConsultation();
        consultation.setId(12);
        consultation.setStatus(status);
        return consultation;
    }

    @Configuration
    static class TestConfig {

        @Bean
        PathologySampleService pathologySampleService() {
            return mock(PathologySampleService.class);
        }

        @Bean
        PathologyConsultationService pathologyConsultationService() {
            return mock(PathologyConsultationService.class);
        }

        @Bean
        ProgramSampleLabUnitScope programSampleLabUnitScope() {
            return mock(ProgramSampleLabUnitScope.class);
        }

        /**
         * Spring still injects the scope's own field on the mock it is handed, so that
         * field needs a candidate even though no call reaches it.
         */
        @Bean
        SampleLabUnitScopeService sampleLabUnitScopeService() {
            return mock(SampleLabUnitScopeService.class);
        }

        @Bean
        PathologyConsultationRestController pathologyConsultationRestController(
                PathologyConsultationService pathologyConsultationService,
                PathologySampleService pathologySampleService, ProgramSampleLabUnitScope programSampleLabUnitScope) {
            return new PathologyConsultationRestController(pathologyConsultationService, pathologySampleService,
                    programSampleLabUnitScope);
        }
    }
}
