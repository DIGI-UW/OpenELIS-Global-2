package org.openelisglobal.testReflex.controller.rest;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.testreflex.controller.rest.TestReflexRuleRestController;
import org.openelisglobal.testreflex.service.TestReflexService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Reflex Tests and the Test Editor's Reflex &amp; Calc section use this
 * controller. All are in the Global Administrator-only admin area (issue
 * #3966).
 */
@WebAppConfiguration
@ContextConfiguration(classes = { TestReflexRuleRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class TestReflexRuleRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final String REFLEX_RULE_JSON = "{\"ruleName\":\"Security test\",\"overall\":\"ALL\","
            + "\"conditions\":[],\"actions\":[]}";

    @Test
    public void saveReflexRule_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(post("/rest/reflexrule").contentType(MediaType.APPLICATION_JSON).content(REFLEX_RULE_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void saveReflexRule_nonAdminReturns403() throws Exception {
        mockMvc.perform(post("/rest/reflexrule").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content(REFLEX_RULE_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void saveReflexRule_adminPassesAuthorization() throws Exception {
        mockMvc.perform(post("/rest/reflexrule").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(REFLEX_RULE_JSON))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void saveReflexRule_receptionReturns403() throws Exception {
        mockMvc.perform(post("/rest/reflexrule").with(user("reception").roles("RECEPTION"))
                .contentType(MediaType.APPLICATION_JSON).content(REFLEX_RULE_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void deactivateReflexRule_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(post("/rest/deactivate-reflexrule/1")).andExpect(status().isUnauthorized());
    }

    @Test
    public void deactivateReflexRule_nonAdminReturns403() throws Exception {
        mockMvc.perform(post("/rest/deactivate-reflexrule/1").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void deactivateReflexRule_adminPassesAuthorization() throws Exception {
        mockMvc.perform(post("/rest/deactivate-reflexrule/1").with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void activateReflexRule_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(post("/rest/activate-reflexrule/1")).andExpect(status().isUnauthorized());
    }

    @Test
    public void activateReflexRule_nonAdminReturns403() throws Exception {
        mockMvc.perform(post("/rest/activate-reflexrule/1").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void activateReflexRule_adminPassesAuthorization() throws Exception {
        mockMvc.perform(post("/rest/activate-reflexrule/1").with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void getReflexRules_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/reflexrules")).andExpect(status().isUnauthorized());
    }

    @Test
    public void getReflexRules_nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/rest/reflexrules").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getReflexRules_adminPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/reflexrules").with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    public void getReflexRuleOptions_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/reflexrule-options")).andExpect(status().isUnauthorized());
    }

    @Test
    public void getReflexRuleOptions_nonAdminReturns403() throws Exception {
        mockMvc.perform(get("/rest/reflexrule-options").with(user("results").roles("RESULTS")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getReflexRuleOptions_adminPassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/reflexrule-options").with(user("admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
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
        TestReflexService testReflexService() {
            return mock(TestReflexService.class);
        }

        @Bean
        DictionaryService dictionaryService() {
            return mock(DictionaryService.class);
        }

        @Bean
        TypeOfSampleService typeOfSampleService() {
            return mock(TypeOfSampleService.class);
        }

        @Bean
        TestReflexRuleRestController testReflexRuleRestController() {
            return new TestReflexRuleRestController();
        }
    }
}
