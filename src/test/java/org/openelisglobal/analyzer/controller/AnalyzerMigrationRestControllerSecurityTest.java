package org.openelisglobal.analyzer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.openelisglobal.analyzer.service.AnalyzerMigrationReferenceService;
import org.openelisglobal.analyzer.service.AnalyzerMigrationReferenceView;
import org.openelisglobal.analyzer.service.AnalyzerMigrationSourceService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebAppConfiguration
@ContextConfiguration(classes = { AnalyzerMigrationRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class AnalyzerMigrationRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Autowired
    private AnalyzerMigrationReferenceService references;

    @Test
    public void sourceSnapshotRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/rest/analyzer/migration/source").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void sourceSnapshotRejectsNonGlobalAdministrators() throws Exception {
        mockMvc.perform(get("/rest/analyzer/migration/source").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void sourceSnapshotReturnsTheFrozenExportForGlobalAdministrators() throws Exception {
        mockMvc.perform(get("/rest/analyzer/migration/source").with(user("global").roles("GLOBAL_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion").value("1.0"))
                .andExpect(jsonPath("$.analyzers[0].sourceAnalyzerId").value("42"));
    }

    @Test
    public void globalAdministratorCanAttachAndReadTheExactMigrationReference() throws Exception {
        AnalyzerMigrationReferenceView expected = new AnalyzerMigrationReferenceView("42", "bridge-42",
                new AnalyzerMigrationReferenceView.ProfileReference("fluorocycler-xt", 1, "sha256:" + "a".repeat(64)));
        when(references.attach(eq("42"), any(), eq("17"))).thenReturn(expected);
        when(references.get("42")).thenReturn(expected);
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(17);
        String request = "{\"profileId\":\"fluorocycler-xt\",\"profileRevision\":1,"
                + "\"profileFingerprint\":\"sha256:" + "a".repeat(64) + "\",\"bridgeConnectionId\":\"bridge-42\"}";

        mockMvc.perform(put("/rest/analyzer/migration/analyzers/42/reference")
                .with(user("global").roles("GLOBAL_ADMIN")).sessionAttr(IActionConstants.USER_SESSION_DATA, sessionData)
                .contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isOk())
                .andExpect(jsonPath("$.bridgeConnectionId").value("bridge-42"))
                .andExpect(jsonPath("$.profileRef.profileId").value("fluorocycler-xt"));
        mockMvc.perform(
                get("/rest/analyzer/migration/analyzers/42/reference").with(user("global").roles("GLOBAL_ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.profileRef.revision").value(1));

        verify(references).attach(eq("42"), any(), eq("17"));
        verify(references).get("42");
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
        AnalyzerMigrationSourceService analyzerMigrationSourceService() {
            AnalyzerMigrationSourceService service = mock(AnalyzerMigrationSourceService.class);
            ObjectNode snapshot = new ObjectMapper().createObjectNode();
            snapshot.put("schemaVersion", "1.0");
            snapshot.putArray("analyzers").addObject().put("sourceAnalyzerId", "42");
            when(service.snapshot()).thenReturn(snapshot);
            return service;
        }

        @Bean
        AnalyzerMigrationReferenceService analyzerMigrationReferenceService() {
            return mock(AnalyzerMigrationReferenceService.class);
        }

        @Bean
        AnalyzerMigrationRestController analyzerMigrationRestController(AnalyzerMigrationSourceService sourceService,
                AnalyzerMigrationReferenceService referenceService) {
            return new AnalyzerMigrationRestController(sourceService, referenceService);
        }
    }
}
