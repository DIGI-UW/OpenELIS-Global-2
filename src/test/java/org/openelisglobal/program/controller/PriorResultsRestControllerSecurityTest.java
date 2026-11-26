package org.openelisglobal.program.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.program.service.PriorResultsService;
import org.openelisglobal.program.service.ProgramSampleLabUnitScope;
import org.openelisglobal.program.valueholder.AnatomicPathologyBench;
import org.openelisglobal.program.valueholder.PriorResults;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.sample.service.SampleLabUnitScopeService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.testsupport.SliceSecurityConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

/**
 * The prior results of a case are read only by a signed-in user who may read
 * the case itself: anyone else is refused before the patient's other cases are
 * looked at.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { SliceSecurityConfig.class, PriorResultsRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class PriorResultsRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final Map<AnatomicPathologyBench, String> URLS = Map.of(AnatomicPathologyBench.PATHOLOGY,
            "/rest/pathology/caseView/7/priorResults", AnatomicPathologyBench.IMMUNOHISTOCHEMISTRY,
            "/rest/immunohistochemistry/caseView/7/priorResults", AnatomicPathologyBench.CYTOLOGY,
            "/rest/cytology/caseView/7/priorResults");

    private PriorResultsService service;
    private ProgramSampleLabUnitScope scope;
    private ProgramSample theCase;

    @Before
    public void resetStubs() {
        service = webApplicationContext.getBean(PriorResultsService.class);
        scope = webApplicationContext.getBean(ProgramSampleLabUnitScope.class);
        reset(service, scope);
        theCase = new ProgramSample();
        theCase.setId(7);
        when(service.caseOf(any(), any())).thenReturn(theCase);
        when(service.priorResultsOf(any(), any(), anyBoolean(), any())).thenReturn(PriorResults.none(false));
    }

    @Test
    public void anonymousIs401_onEachBench() throws Exception {
        for (String url : URLS.values()) {
            mockMvc.perform(get(url).with(anonymous())).andExpect(status().isUnauthorized());
        }

        verify(service, never()).caseOf(any(), any());
        verify(service, never()).priorResultsOf(any(), any(), anyBoolean(), any());
    }

    @Test
    public void aCaseTheUserMayNotReadIs404_andPriorResultsAreNeverRead() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)).when(scope).requireVisible(any(), any());

        for (String url : URLS.values()) {
            mockMvc.perform(signedIn(url)).andExpect(status().isNotFound());
        }

        verify(service, never()).priorResultsOf(any(), any(), anyBoolean(), any());
    }

    @Test
    public void aVisibleCaseAnswers200() throws Exception {
        for (Map.Entry<AnatomicPathologyBench, String> bench : URLS.entrySet()) {
            mockMvc.perform(signedIn(bench.getValue())).andExpect(status().isOk());

            verify(service).caseOf(bench.getKey(), 7);
            verify(service).priorResultsOf(eq(bench.getKey()), eq(7), eq(false), eq("1"));
        }
        verify(scope, times(3)).requireVisible(eq("1"), same(theCase));
    }

    // helpers

    private static MockHttpServletRequestBuilder signedIn(String url) {
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
        return get(url).with(user("tech")).sessionAttr(IActionConstants.USER_SESSION_DATA, userSessionData);
    }

    @Configuration
    static class TestConfig {

        @Bean
        PriorResultsService priorResultsService() {
            return mock(PriorResultsService.class);
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
        PriorResultsRestController priorResultsRestController(PriorResultsService priorResultsService,
                ProgramSampleLabUnitScope programSampleLabUnitScope) {
            return new PriorResultsRestController(priorResultsService, programSampleLabUnitScope);
        }
    }
}
