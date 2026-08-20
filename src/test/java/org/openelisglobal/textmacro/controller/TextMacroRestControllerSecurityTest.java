package org.openelisglobal.textmacro.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.textmacro.controller.rest.TextMacroAdminRestController;
import org.openelisglobal.textmacro.controller.rest.TextMacroRestController;
import org.openelisglobal.textmacro.form.TextMacroAdminForm;
import org.openelisglobal.textmacro.form.TextMacroPageForm;
import org.openelisglobal.textmacro.service.TextMacroService;
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

@WebAppConfiguration
@ContextConfiguration(classes = TextMacroRestControllerSecurityTest.TestConfig.class)
@TestPropertySource("classpath:common.properties")
public class TextMacroRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void runtimeLookupRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/rest/text-macros?context=MICROBIOLOGY_CULTURE_ACTIVITY"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(
                get("/rest/text-macros?context=MICROBIOLOGY_CULTURE_ACTIVITY").with(user("technologist").roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    public void administrationRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/rest/text-macros/admin").with(user("technologist").roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/rest/text-macros/admin").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    public void adminWriteReachesControllerOnlyWithAdminRoleAndServerSessionActor() throws Exception {
        UserSessionData session = new UserSessionData();
        session.setSytemUserId(42);
        mockMvc.perform(post("/rest/text-macros/admin").with(user("admin").roles("ADMIN"))
                .sessionAttr(IActionConstants.USER_SESSION_DATA, session).contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{\"code\":\".gpc\",\"expansionText\":\"Gram-positive cocci\",\"contexts\":[\"MICROBIOLOGY_CULTURE_ACTIVITY\"],\"active\":true}"))
                .andExpect(status().isOk());
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
        TextMacroService textMacroService() {
            TextMacroService service = mock(TextMacroService.class);
            when(service.findActive(any(), any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of());
            when(service.searchAdmin(any())).thenReturn(new TextMacroPageForm());
            when(service.save(any(), any(), any())).thenReturn(new TextMacroAdminForm());
            when(service.exportCsv()).thenReturn("code\r\n");
            when(service.bulk(any(), any()))
                    .thenReturn(new org.openelisglobal.textmacro.form.TextMacroBulkResultForm());
            return service;
        }

        @Bean
        TextMacroRestController textMacroRestController(TextMacroService service) {
            return new TextMacroRestController(service);
        }

        @Bean
        TextMacroAdminRestController textMacroAdminRestController(TextMacroService service) {
            return new TextMacroAdminRestController(service);
        }
    }
}
