package org.openelisglobal.dataexchange.fhir.controller;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;
import org.hl7.fhir.r4.model.Questionnaire;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.questionnaire.service.QuestionnaireStorageService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
@ContextConfiguration(classes = FhirQuestionnaireBoundaryTest.TestConfig.class)
public class FhirQuestionnaireBoundaryTest extends SecuritySliceMockMvcTest {
    private static final UUID FORM = UUID.fromString("00000000-0000-0000-0000-000000000001");
    @Autowired
    private QuestionnaireStorageService storage;

    @Before
    public void resetStorage() {
        reset(storage);
    }

    @Test
    public void anonymousCannotReadDefinitions() throws Exception {
        mockMvc.perform(get("/rest/fhir/Questionnaire/" + FORM)).andExpect(status().isUnauthorized());
        verifyZeroInteractions(storage);
    }

    @Test
    public void orderEntryCanReadItsDefinitionWithoutClinicalExportPermission() throws Exception {
        when(storage.getQuestionnaire(FORM)).thenReturn(Optional.of(new Questionnaire()));
        mockMvc.perform(get("/rest/fhir/Questionnaire/" + FORM).with(user("reception").roles("RECEPTION")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.resourceType").value("Questionnaire"));
        verify(storage).getQuestionnaire(FORM);
        verifyNoMoreInteractions(storage);
    }

    @Test
    public void missingDefinitionReturnsNotFound() throws Exception {
        when(storage.getQuestionnaire(FORM)).thenReturn(Optional.empty());
        mockMvc.perform(get("/rest/fhir/Questionnaire/" + FORM).with(user("reception").roles("RECEPTION")))
                .andExpect(status().isNotFound());
        verify(storage).getQuestionnaire(FORM);
        verifyNoMoreInteractions(storage);
    }

    @Test
    public void malformedDefinitionIdentifierNeverReachesStorage() throws Exception {
        mockMvc.perform(get("/rest/fhir/Questionnaire/not-a-uuid").with(user("reception").roles("RECEPTION")))
                .andExpect(status().isBadRequest());
        verifyZeroInteractions(storage);
    }

    @Test
    public void genericStoreAndPatientResponsesHaveNoRouteEvenForAdministrator() throws Exception {
        for (String path : new String[] { "Patient", "Patient/patient-1", "QuestionnaireResponse/response-1",
                "metadata" }) {
            mockMvc.perform(get("/rest/fhir/" + path).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isNotFound());
        }
        verifyZeroInteractions(storage);
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfig implements org.springframework.web.servlet.config.annotation.WebMvcConfigurer {
        @Override
        public void extendMessageConverters(
                java.util.List<org.springframework.http.converter.HttpMessageConverter<?>> converters) {
            converters.add(0, new org.openelisglobal.dataexchange.fhir.FhirMediaTypeMessageConverter(
                    ca.uhn.fhir.context.FhirContext.forR4Cached()));
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults()).build();
        }

        @Bean
        QuestionnaireStorageService storage() {
            return mock(QuestionnaireStorageService.class);
        }

        @Bean
        FhirQueryRestController controller(QuestionnaireStorageService storage) {
            FhirQueryRestController controller = new FhirQueryRestController();
            ReflectionTestUtils.setField(controller, "questionnaireStorageService", storage);
            return controller;
        }
    }
}
