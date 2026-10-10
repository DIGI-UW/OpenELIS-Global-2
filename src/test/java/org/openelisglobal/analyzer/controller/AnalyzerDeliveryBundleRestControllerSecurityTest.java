package org.openelisglobal.analyzer.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analyzerimport.service.AnalyzerDeliveryBundleService;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebAppConfiguration
@ContextConfiguration(classes = { AnalyzerDeliveryBundleRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class AnalyzerDeliveryBundleRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final String BUNDLE = "/rest/analyzer/deliveries/receipt-1/bundle";
    private static final String JSON = "{\"resourceType\":\"Bundle\",\"type\":\"collection\"}";

    @Autowired
    private AnalyzerDeliveryBundleService bundles;

    @Before
    public void resetService() {
        reset(bundles);
        when(bundles.getBundle("receipt-1")).thenReturn(Optional.of(JSON));
        when(bundles.getBundle("missing")).thenReturn(Optional.empty());
    }

    @Test
    public void withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get(BUNDLE)).andExpect(status().isUnauthorized());
    }

    @Test
    public void withResultsRole_returns403BeforeReadingTheBundle() throws Exception {
        mockMvc.perform(get(BUNDLE).with(user("results").roles("RESULTS"))).andExpect(status().isForbidden());
        verify(bundles, never()).getBundle("receipt-1");
    }

    @Test
    public void withAnalyserImportRole_returnsTheBundleAsJson() throws Exception {
        mockMvc.perform(get(BUNDLE).with(user("operator").roles("ANALYSER_IMPORT"))).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json")).andExpect(content().string(JSON));
    }

    @Test
    public void forAnUnknownReceipt_returns404() throws Exception {
        mockMvc.perform(get("/rest/analyzer/deliveries/missing/bundle").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
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
        AnalyzerDeliveryBundleService bundles() {
            return mock(AnalyzerDeliveryBundleService.class);
        }

        @Bean
        AnalyzerDeliveryBundleRestController analyzerDeliveryBundleRestController(
                AnalyzerDeliveryBundleService bundles) {
            return new AnalyzerDeliveryBundleRestController(bundles);
        }
    }
}
