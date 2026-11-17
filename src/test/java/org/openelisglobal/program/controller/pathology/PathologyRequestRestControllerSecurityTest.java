package org.openelisglobal.program.controller.pathology;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.OptimisticLockException;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.program.service.PathologyRequestConflictException;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.ProgramSampleLabUnitScope;
import org.openelisglobal.program.service.RequestDraft;
import org.openelisglobal.program.service.RequestStatusChange;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest.RequestStatus;
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
 * Bench requests carry no permission key of their own: any signed-in user who
 * may read the case may raise one or change its status, and nobody else reaches
 * the service at all. The case is looked up for the user's lab units before
 * anything is written.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { SliceSecurityConfig.class,
        PathologyRequestRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class PathologyRequestRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final String RAISE = "/rest/pathology/caseView/7/requests";
    private static final String STATUS = "/rest/pathology/caseView/7/requests/31/status";

    private static final String RAISE_BODY = "{\"kind\":\"STAIN\",\"targetBlockId\":null,\"targetSlideId\":92,"
            + "\"stainId\":\"9752\",\"priority\":\"URGENT\",\"instruction\":\"PAS on A1.1\"}";
    private static final String CANCEL_BODY = "{\"from\":\"OPENED\",\"status\":\"CANCELLED\","
            + "\"cancelReason\":\"asked twice\",\"resultBlockId\":null,\"resultSlideId\":null}";

    private PathologySampleService service;
    private ProgramSampleLabUnitScope scope;

    @Before
    public void resetStubs() {
        service = webApplicationContext.getBean(PathologySampleService.class);
        scope = webApplicationContext.getBean(ProgramSampleLabUnitScope.class);
        reset(service, scope);
        when(service.raiseRequest(any(), any(), any())).thenReturn(requestAt(RequestStatus.OPENED));
        when(service.changeRequestStatus(any(), any(), any(), any())).thenReturn(requestAt(RequestStatus.CANCELLED));
    }

    /** An anonymous request is refused before the case is even looked up. */
    @Test
    public void anAnonymousRaise_isRefusedBeforeTheService() throws Exception {
        mockMvc.perform(post(RAISE).with(anonymous()).contentType(MediaType.APPLICATION_JSON).content(RAISE_BODY))
                .andExpect(status().isUnauthorized());

        verify(service, never()).get(any());
        verify(service, never()).raiseRequest(any(), any(), any());
    }

    @Test
    public void anAnonymousStatusChange_isRefusedBeforeTheService() throws Exception {
        mockMvc.perform(post(STATUS).with(anonymous()).contentType(MediaType.APPLICATION_JSON).content(CANCEL_BODY))
                .andExpect(status().isUnauthorized());

        verify(service, never()).get(any());
        verify(service, never()).changeRequestStatus(any(), any(), any(), any());
    }

    /**
     * A case outside the user's lab units is not found, and the request never
     * reaches the service that would write it.
     */
    @Test
    public void aRaiseOnACaseOutsideTheUsersUnits_isNotFoundBeforeTheService() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)).when(scope).requireVisible(any(), any());

        signedIn(RAISE, RAISE_BODY).andExpect(status().isNotFound());

        verify(service, never()).raiseRequest(any(), any(), any());
    }

    @Test
    public void aStatusChangeOnACaseOutsideTheUsersUnits_isNotFoundBeforeTheService() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)).when(scope).requireVisible(any(), any());

        signedIn(STATUS, CANCEL_BODY).andExpect(status().isNotFound());

        verify(service, never()).changeRequestStatus(any(), any(), any(), any());
    }

    /**
     * A signed-in user reaches the service with the request as posted, and the
     * answer names only the request's id and status: the screen reads the case
     * again for the rest.
     */
    @Test
    public void aSignedInRaise_reachesTheServiceAsThatUserWithTheDraft() throws Exception {
        signedIn(RAISE, RAISE_BODY).andExpect(status().isOk())
                .andExpect(content().string("{\"id\":31,\"status\":\"OPENED\"}"));

        verify(service).raiseRequest(eq(7), eq(new RequestDraft("STAIN", null, 92, "9752", "URGENT", "PAS on A1.1")),
                eq("1"));
    }

    @Test
    public void aSignedInStatusChange_reachesTheServiceWithTheStatusThePageShowed() throws Exception {
        signedIn(STATUS, CANCEL_BODY).andExpect(status().isOk())
                .andExpect(content().string("{\"id\":31,\"status\":\"CANCELLED\"}"));

        verify(service).changeRequestStatus(eq(7), eq(31),
                eq(new RequestStatusChange("OPENED", "CANCELLED", "asked twice", null, null)), eq("1"));
    }

    /**
     * A post with no body still reaches the service, which refuses it with a
     * message the screen translates rather than the request failing to bind.
     */
    @Test
    public void aRaiseWithNoBody_reachesTheServiceWithNoDraft() throws Exception {
        mockMvc.perform(post(RAISE).with(user("tech")).sessionAttr(IActionConstants.USER_SESSION_DATA, sessionUser()))
                .andExpect(status().isOk());

        verify(service).raiseRequest(eq(7), isNull(), eq("1"));
    }

    /** A page that showed a status the request has left is told so. */
    @Test
    public void aStatusChangeFromAStaleStatus_answers409Request() throws Exception {
        when(service.changeRequestStatus(any(), any(), any(), any())).thenThrow(
                new PathologyRequestConflictException(31, RequestStatus.COMPLETED, RequestStatus.OPENED));

        signedIn(STATUS, CANCEL_BODY).andExpect(status().isConflict())
                .andExpect(jsonPath("$.conflict").value("request"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    /** A raise that loses to another change on the case is a conflict, not an error. */
    @Test
    public void aRaiseThatLosesTheVersionCheck_answers409() throws Exception {
        when(service.raiseRequest(any(), any(), any())).thenThrow(new OptimisticLockException());

        signedIn(RAISE, RAISE_BODY).andExpect(status().isConflict()).andExpect(jsonPath("$.conflict").value("version"));
    }

    @Test
    public void aStatusChangeThatLosesTheVersionCheck_answers409() throws Exception {
        when(service.changeRequestStatus(any(), any(), any(), any())).thenThrow(new OptimisticLockException());

        signedIn(STATUS, CANCEL_BODY).andExpect(status().isConflict())
                .andExpect(jsonPath("$.conflict").value("version"));
    }

    // helpers

    private ResultActions signedIn(String endpoint, String body) throws Exception {
        return mockMvc.perform(
                post(endpoint).with(user("tech")).sessionAttr(IActionConstants.USER_SESSION_DATA, sessionUser())
                        .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static UserSessionData sessionUser() {
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
        return userSessionData;
    }

    private static PathologyRequest requestAt(RequestStatus status) {
        PathologyRequest request = new PathologyRequest();
        request.setId(31);
        request.setStatus(status);
        return request;
    }

    @Configuration
    static class TestConfig {

        @Bean
        PathologySampleService pathologySampleService() {
            return mock(PathologySampleService.class);
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
        PathologyRequestRestController pathologyRequestRestController(PathologySampleService pathologySampleService,
                ProgramSampleLabUnitScope programSampleLabUnitScope) {
            return new PathologyRequestRestController(pathologySampleService, programSampleLabUnitScope);
        }
    }
}
