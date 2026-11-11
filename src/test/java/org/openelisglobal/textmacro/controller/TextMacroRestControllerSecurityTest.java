package org.openelisglobal.textmacro.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebAppConfiguration
@ContextConfiguration(classes = TextMacroRestControllerSecurityTest.TestConfig.class)
@TestPropertySource("classpath:common.properties")
public class TextMacroRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Autowired
    private TextMacroService service;

    @Test
    public void runtimeLookupRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/rest/text-macros?context=MICROBIOLOGY_CULTURE_READING"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(
                get("/rest/text-macros?context=MICROBIOLOGY_CULTURE_READING").with(user("technologist").roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    public void administrationRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/rest/text-macros/admin").with(user("technologist").roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/rest/text-macros/admin").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    public void nonAdminCannotWriteBulkChangeOrExportTheLibrary() throws Exception {
        clearInvocations(service);
        UserSessionData session = new UserSessionData();
        session.setSytemUserId(7);
        mockMvc.perform(post("/rest/text-macros/admin").with(user("technologist").roles("USER"))
                .sessionAttr(IActionConstants.USER_SESSION_DATA, session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\".gpc\",\"expansionText\":\"Text\",\"category\":\"CULTURE\","
                        + "\"contexts\":[\"MICROBIOLOGY_CASE_NOTE\"],\"active\":true}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/rest/text-macros/admin/1").with(user("technologist").roles("USER"))
                .sessionAttr(IActionConstants.USER_SESSION_DATA, session).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isForbidden());
        mockMvc.perform(post("/rest/text-macros/admin/bulk").with(user("technologist").roles("USER"))
                .sessionAttr(IActionConstants.USER_SESSION_DATA, session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\":[\"1\"],\"action\":\"DELETE_LOCAL\"}")).andExpect(status().isForbidden());
        mockMvc.perform(get("/rest/text-macros/admin/export").with(user("technologist").roles("USER")))
                .andExpect(status().isForbidden());

        verify(service, never()).save(any(), any(), any());
        verify(service, never()).bulk(any(), any());
        verify(service, never()).exportCsv();
    }

    @Test
    public void adminWriteReachesControllerOnlyWithAdminRoleAndServerSessionActor() throws Exception {
        clearInvocations(service);
        UserSessionData session = new UserSessionData();
        session.setSytemUserId(42);
        mockMvc.perform(post("/rest/text-macros/admin").with(user("admin").roles("ADMIN"))
                .sessionAttr(IActionConstants.USER_SESSION_DATA, session).contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{\"code\":\".gpc\",\"expansionText\":\"Gram-positive cocci\",\"contexts\":[\"MICROBIOLOGY_CULTURE_READING\"],\"active\":true}"))
                .andExpect(status().isOk());
        verify(service).save(isNull(), any(), eq("42"));
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
