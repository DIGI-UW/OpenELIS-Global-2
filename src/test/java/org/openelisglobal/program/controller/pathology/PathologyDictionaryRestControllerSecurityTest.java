package org.openelisglobal.program.controller.pathology;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.program.service.PathologyConclusionSearchService;
import org.openelisglobal.program.valueholder.pathology.ConclusionEntries;
import org.openelisglobal.program.valueholder.pathology.ConclusionEntry;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.testsupport.SliceSecurityConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * The diagnosis search carries no permission key of its own: any signed-in user
 * may read it, and nobody else reaches the service.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { SliceSecurityConfig.class,
        PathologyDictionaryRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class PathologyDictionaryRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final String SEARCH = "/rest/pathology/conclusionEntries";

    private PathologyConclusionSearchService service;

    @Before
    public void resetStubs() {
        service = webApplicationContext.getBean(PathologyConclusionSearchService.class);
        reset(service);
        when(service.search(any())).thenReturn(
                new ConclusionEntries(List.of(new ConclusionEntry("9101", "Adenocarcinoma", "C80.1", false)), 1));
    }

    @Test
    public void anonymousIs401() throws Exception {
        mockMvc.perform(get(SEARCH).param("q", "aden").with(anonymous())).andExpect(status().isUnauthorized());

        verify(service, never()).search(any());
    }

    @Test
    public void aSignedInSearch_reachesTheServiceWithTheText() throws Exception {
        mockMvc.perform(get(SEARCH).param("q", "aden").with(user("tech"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].code").value("C80.1"));

        verify(service).search(eq("aden"));
    }

    @Configuration
    static class TestConfig {

        @Bean
        PathologyConclusionSearchService pathologyConclusionSearchService() {
            return mock(PathologyConclusionSearchService.class);
        }

        @Bean
        PathologyDictionaryRestController pathologyDictionaryRestController(
                PathologyConclusionSearchService pathologyConclusionSearchService) {
            return new PathologyDictionaryRestController(pathologyConclusionSearchService);
        }
    }
}
