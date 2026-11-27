package org.openelisglobal.program.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.controller.cytology.CytologyController;
import org.openelisglobal.program.controller.immunohistochemistry.ImmunohistochemistryController;
import org.openelisglobal.program.controller.pathology.PathologyController;
import org.openelisglobal.sample.service.SampleLabUnitScopeService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * The dashboards' search stays inside the user's lab units. The term matches
 * both cases in the status (part of each patient's last name); a user whose
 * units cover only the first order is shown that one, an unrestricted user
 * both.
 */
public class ApDashboardSearchScopeTest extends BaseWebContextSensitiveTest {

    /** Part of both patients' last names, and the start of no lab number. */
    private static final String PART_OF_BOTH_NAMES = "ke";

    private static final String VISIBLE_SAMPLE_ID = "1";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AnnotationConfigWebApplicationContext securityContext;

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfig {

        /**
         * AppTestConfig does not component-scan org.openelisglobal.program.controller,
         * so the controllers are built here against the parent's own services.
         */
        @Bean
        PathologyController pathologyController(ApplicationContext context) {
            return context.getParent().getAutowireCapableBeanFactory().createBean(PathologyController.class);
        }

        @Bean
        ImmunohistochemistryController immunohistochemistryController(ApplicationContext context) {
            return context.getParent().getAutowireCapableBeanFactory().createBean(ImmunohistochemistryController.class);
        }

        @Bean
        CytologyController cytologyController(ApplicationContext context) {
            return context.getParent().getAutowireCapableBeanFactory().createBean(CytologyController.class);
        }

        /** The rule the application's chain applies to /rest/**. */
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).httpBasic(Customizer.withDefaults());
            return http.build();
        }
    }

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/ap-dashboard-search.xml");
        securityContext = new AnnotationConfigWebApplicationContext();
        securityContext.setParent(webApplicationContext);
        securityContext.setServletContext(webApplicationContext.getServletContext());
        securityContext.register(TestConfig.class);
        securityContext.refresh();
        mockMvc = MockMvcBuilders.webAppContextSetup(securityContext).apply(springSecurity()).build();
    }

    @After
    public void tearDown() {
        if (securityContext != null) {
            securityContext.close();
        }
    }

    @Test
    public void pathologySearch_listsOnlyTheCasesInTheUsersUnits() throws Exception {
        assertScoped("pathology", "GROSSING");
    }

    @Test
    public void immunohistochemistrySearch_listsOnlyTheCasesInTheUsersUnits() throws Exception {
        assertScoped("immunohistochemistry", "IN_PROGRESS");
    }

    @Test
    public void cytologySearch_listsOnlyTheCasesInTheUsersUnits() throws Exception {
        assertScoped("cytology", "PREPARING_SLIDES");
    }

    private void assertScoped(String bench, String status) throws Exception {
        assertEquals("an unrestricted user is shown both matching cases", List.of("APS26000001", "APS26000002"),
                labNumbers(bench, status));
        assertEquals("a user whose units cover one order is shown only that case", List.of("APS26000001"),
                asAUserSeeingOnlyTheFirstOrder(() -> labNumbers(bench, status)));
    }

    private List<String> labNumbers(String bench, String status) throws Exception {
        MockHttpServletResponse response = mockMvc
                .perform(get("/rest/" + bench + "/dashboard").param("statuses", status)
                        .param("searchTerm", PART_OF_BOTH_NAMES).session(new MockHttpSession()).with(user("admin")))
                .andReturn().getResponse();
        assertEquals("the dashboard answered", 200, response.getStatus());
        JsonNode items = objectMapper.readTree(response.getContentAsString()).get("items");
        List<String> labNumbers = new ArrayList<>();
        items.forEach(item -> labNumbers.add(item.get("labNumber").asText()));
        labNumbers.sort(null);
        return labNumbers;
    }

    private interface Check<T> {
        T run() throws Exception;
    }

    /**
     * Runs the check as a user whose lab units cover only the first order, then
     * restores the real scope.
     */
    private <T> T asAUserSeeingOnlyTheFirstOrder(Check<T> check) throws Exception {
        Object scope = AopTestUtils
                .getUltimateTargetObject(webApplicationContext.getBean(ProgramSampleLabUnitScope.class));
        Object realService = ReflectionTestUtils.getField(scope, "sampleLabUnitScopeService");
        SampleLabUnitScopeService restricted = Mockito.mock(SampleLabUnitScopeService.class);
        Mockito.when(restricted.restrictedSectionIds(any())).thenReturn(Set.of("1"));
        Mockito.when(restricted.isVisible(any(),
                argThat((Sample sample) -> sample != null && VISIBLE_SAMPLE_ID.equals(sample.getId()))))
                .thenReturn(true);
        ReflectionTestUtils.setField(scope, "sampleLabUnitScopeService", restricted);
        try {
            return check.run();
        } finally {
            ReflectionTestUtils.setField(scope, "sampleLabUnitScopeService", realService);
        }
    }
}
