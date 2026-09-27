package org.openelisglobal.microbiology.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.microbiology.form.MicroWorklistPageForm;
import org.openelisglobal.microbiology.form.MicroWorklistQueryForm;
import org.openelisglobal.microbiology.service.MicroWorklistService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebAppConfiguration
@ContextConfiguration(classes = { MicroWorklistRestControllerSecurityTest.TestConfig.class })
public class MicroWorklistRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Autowired
    private MicroWorklistService worklistService;

    @Test
    public void getWorklistWithoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/microbiology/worklist")).andExpect(status().isUnauthorized());
    }

    @Test
    public void getWorklistWithUnrelatedRoleReturns403() throws Exception {
        mockMvc.perform(get("/rest/microbiology/worklist").with(user("reception").roles("RECEPTION")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getWorklistWithResultsRoleReturns200() throws Exception {
        mockMvc.perform(get("/rest/microbiology/worklist").with(user("analyst").roles("RESULTS")))
                .andExpect(status().isOk());
    }

    @Test
    public void getWorklistWithValidationRoleReturns200() throws Exception {
        mockMvc.perform(get("/rest/microbiology/worklist").with(user("validator").roles("VALIDATION")))
                .andExpect(status().isOk());
    }

    @Test
    public void getWorklistWithAdminRoleReturns200() throws Exception {
        mockMvc.perform(get("/rest/microbiology/worklist").with(user("manager").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    public void getWorklistBindsCanonicalSurveillanceScope() throws Exception {
        clearInvocations(worklistService);
        mockMvc.perform(get("/rest/microbiology/worklist").with(user("analyst").roles("RESULTS")).param("grain", "ast")
                .param("from", "2026-07-01").param("to", "2026-07-31").param("specimen", "blood", "urine")
                .param("organism", "ecoli").param("origin", "icu").param("significance", "pathogen", "screening")
                .param("page", "3").param("pageSize", "50")).andExpect(status().isOk());

        ArgumentCaptor<MicroWorklistQueryForm> queryCaptor = ArgumentCaptor.forClass(MicroWorklistQueryForm.class);
        verify(worklistService).getWorklistPage(queryCaptor.capture());
        MicroWorklistQueryForm query = queryCaptor.getValue();
        assertEquals("ast", query.grain);
        assertEquals("2026-07-01", query.from);
        assertEquals("2026-07-31", query.to);
        assertEquals(Arrays.asList("blood", "urine"), query.specimen);
        assertEquals(Arrays.asList("ecoli"), query.organism);
        assertEquals(Arrays.asList("icu"), query.origin);
        assertEquals(Arrays.asList("pathogen", "screening"), query.significance);
        assertEquals(3, query.page);
        assertEquals(50, query.pageSize);
    }

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
        MicroWorklistService microWorklistService() {
            MicroWorklistService service = mock(MicroWorklistService.class);
            when(service.getWorklistPage(any(MicroWorklistQueryForm.class))).thenReturn(new MicroWorklistPageForm());
            return service;
        }

        @Bean
        MicroWorklistRestController microWorklistRestController(MicroWorklistService service) {
            return new MicroWorklistRestController(service);
        }
    }
}
