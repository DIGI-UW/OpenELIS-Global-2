package org.openelisglobal.dataexchange.fhir.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
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
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@WebAppConfiguration
@ContextConfiguration(classes = FhirReplayRestControllerTest.TestConfig.class)
public class FhirReplayRestControllerTest extends SecuritySliceMockMvcTest {

    private static final String REQUEST = "{\"sampleIds\":[\"1154\",\"1157\"]}";
    private static final List<String> SAMPLE_IDS = List.of("1154", "1157");

    @Autowired
    private FhirTransformService transformService;

    @Before
    public void resetService() {
        reset(transformService);
    }

    @Test
    public void anonymousRequestDoesNotEmit() throws Exception {
        mockMvc.perform(post("/rest/fhir/replay").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isUnauthorized());
        verifyZeroInteractions(transformService);
    }

    @Test
    public void nonAdministratorDoesNotEmit() throws Exception {
        mockMvc.perform(post("/rest/fhir/replay").with(csrf()).with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content(REQUEST)).andExpect(status().isForbidden());
        verifyZeroInteractions(transformService);
    }

    @Test
    public void malformedOrEmptySelectionsDoNotEmit() throws Exception {
        for (String body : List.of("{}", "{\"sampleIds\":null}", "{\"sampleIds\":[]}", "{\"sampleIds\":[null]}",
                "{\"sampleIds\":[\"\"]}", "{\"sampleIds\":[\"bad\"]}", "{\"sampleIds\":[\"0\"]}",
                "{\"sampleIds\":[\"-1\"]}", "{\"sampleIds\":[\"2147483648\"]}")) {
            replay(body).andExpect(status().isBadRequest());
        }
        verifyZeroInteractions(transformService);
    }

    @Test
    public void oversizedSelectionDoesNotEmit() throws Exception {
        replay(new ObjectMapper().writeValueAsString(Map.of("sampleIds", Collections.nCopies(101, "1154"))))
                .andExpect(status().isBadRequest());
        verifyZeroInteractions(transformService);
    }

    @Test
    public void successfulReplayUsesOnlySelectedIdsAndReportsConfirmedEntries() throws Exception {
        when(transformService.transformPersistObjectsUnderSamples(SAMPLE_IDS))
                .thenReturn(CompletableFuture.completedFuture(response("200 OK", "201 Created")));

        replay(REQUEST).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.sampleIds[0]").value("1154"))
                .andExpect(jsonPath("$.sampleIds[1]").value("1157"))
                .andExpect(jsonPath("$.sampleIds.length()").value(2))
                .andExpect(jsonPath("$.resourceCount").value(2));
        verify(transformService).transformPersistObjectsUnderSamples(SAMPLE_IDS);
        verifyNoMoreInteractions(transformService);
    }

    @Test
    public void missingSampleIsNotReportedAsCompletion() throws Exception {
        when(transformService.transformPersistObjectsUnderSamples(SAMPLE_IDS)).thenReturn(
                CompletableFuture.failedFuture(new ObjectNotFoundException("1157", "Sample")));
        replay(REQUEST).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("FHIR_REPLAY_SAMPLE_NOT_FOUND"));
    }

    @Test
    public void failedTransformIsNotReportedAsCompletion() throws Exception {
        when(transformService.transformPersistObjectsUnderSamples(SAMPLE_IDS)).thenReturn(
                CompletableFuture.failedFuture(new FhirLocalPersistingException("store unavailable")));
        replay(REQUEST).andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("FHIR_REPLAY_FAILED"));
    }

    @Test
    public void failedOrMissingTransactionEntryIsNotReportedAsCompletion() throws Exception {
        for (Bundle bundle : List.of(response("200 OK", "422 Unprocessable Entity"), response("200 OK", null),
                response(), new Bundle().setType(Bundle.BundleType.COLLECTION))) {
            when(transformService.transformPersistObjectsUnderSamples(SAMPLE_IDS))
                    .thenReturn(CompletableFuture.completedFuture(bundle));
            replay(REQUEST).andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.error").value("FHIR_REPLAY_INVALID_RESPONSE"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void timeoutReportsUnknownCompletionWithoutCancellingTheWorker() throws Exception {
        Future<Bundle> future = mock(Future.class);
        when(future.get(120, TimeUnit.SECONDS)).thenThrow(new TimeoutException());
        when(transformService.transformPersistObjectsUnderSamples(SAMPLE_IDS)).thenReturn(future);
        replay(REQUEST).andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.error").value("FHIR_REPLAY_COMPLETION_UNKNOWN"));
        verify(future).get(120, TimeUnit.SECONDS);
        verifyNoMoreInteractions(future);
    }

    private ResultActions replay(String body) throws Exception {
        return mockMvc.perform(post("/rest/fhir/replay").with(csrf()).with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static Bundle response(String... statuses) {
        Bundle response = new Bundle().setType(Bundle.BundleType.TRANSACTIONRESPONSE);
        for (String status : statuses) {
            response.addEntry().getResponse().setStatus(status);
        }
        return response;
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfig implements WebMvcConfigurer {
        @Bean
        @Override
        public LocalValidatorFactoryBean getValidator() {
            // Tomcat provides EL at runtime; focused JVM tests use the existing EL-free
            // pattern.
            LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
            validator.setMessageInterpolator(new ParameterMessageInterpolator());
            return validator;
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults()).build();
        }

        @Bean
        FhirTransformService fhirTransformService() {
            return mock(FhirTransformService.class);
        }

        @Bean
        FhirReplayRestController fhirReplayRestController(FhirTransformService service) {
            return new FhirReplayRestController(service);
        }
    }
}
