package org.openelisglobal.pathology;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.function.ThrowingRunnable;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.controller.pathology.PathologyController;
import org.openelisglobal.program.service.ProgramSampleLabUnitScope;
import org.openelisglobal.sample.service.SampleLabUnitScopeService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
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
 * Drives the pathology controller through MockMvc behind a security chain, over
 * the fixture's pathology case, so each test states only the rule it pins.
 *
 * <p>
 * Audit entries on blocks and slides survive the fixture reload, so they are
 * cleared after each test.
 */
abstract class PathologyControllerTestSupport extends BaseWebContextSensitiveTest {

    /**
     * The block the fixture seeds, which carries a legacy number and no state.
     */
    protected static final int LEGACY_BLOCK_ID = 101;

    private final ObjectMapper objectMapper = new ObjectMapper();

    protected AnnotationConfigWebApplicationContext securityContext;

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfig {

        /**
         * AppTestConfig does not component-scan org.openelisglobal.program.controller,
         * so the controller has no bean in the parent context. It is built here against
         * the parent's own services rather than mocks, because what a state change has
         * to leave behind is a real row and a real audit entry.
         */
        @Bean
        PathologyController pathologyController(ApplicationContext context) {
            return context.getParent().getAutowireCapableBeanFactory().createBean(PathologyController.class);
        }

        /**
         * AppTestConfig also excludes SecurityConfig, so the application's own chains
         * are unreachable from a database-backed test. This reproduces the rule that
         * chain applies to /rest/**, which is anyRequest().authenticated(), and asks
         * for the refusal as a status rather than the redirect to /LoginPage the
         * production chain's form login answers a browser with.
         */
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults());
            return http.build();
        }
    }

    @Before
    public void loadCaseAndSecurity() throws Exception {
        executeDataSetWithStateManagement("testdata/pathology-sample-with-patient.xml");
        // The fixture replaces system_user with its own users, so authenticate as one
        // of them.
        authenticateAs("technician1");

        // Add the security interceptors without bootstrapping a second
        // database-backed application.
        securityContext = new AnnotationConfigWebApplicationContext();
        securityContext.setParent(webApplicationContext);
        securityContext.setServletContext(webApplicationContext.getServletContext());
        securityContext.register(TestConfig.class);
        securityContext.refresh();
        mockMvc = MockMvcBuilders.webAppContextSetup(securityContext).apply(springSecurity()).build();
    }

    @After
    public void clearAuditAndSecurity() {
        jdbcTemplate.update("DELETE FROM clinlims.history WHERE reference_table IN"
                + " (SELECT id FROM clinlims.reference_tables WHERE name IN ('PATHOLOGY_BLOCK', 'PATHOLOGY_SLIDE'))");
        if (securityContext != null) {
            securityContext.close();
        }
    }

    protected MockHttpServletResponse postAsTechnician(String endpoint, String body) throws Exception {
        if (body == null) {
            return mockMvc.perform(post(endpoint).with(user("technician1"))).andReturn().getResponse();
        }
        return mockMvc
                .perform(post(endpoint).with(user("technician1")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse();
    }

    protected Map<String, Object> bodyOf(MockHttpServletResponse response) throws Exception {
        return objectMapper.readValue(response.getContentAsString(), new TypeReference<Map<String, Object>>() {
        });
    }

    protected boolean isActive(String table, int id) {
        List<Boolean> active = jdbcTemplate.queryForList("SELECT active FROM clinlims." + table + " WHERE id = ?",
                Boolean.class, id);
        return !active.isEmpty() && Boolean.TRUE.equals(active.get(0));
    }

    /**
     * Runs the check as a user whose lab units cover no order, then restores the
     * real scope.
     */
    protected void asAUserOutsideTheCaseUnits(ThrowingRunnable check) throws Throwable {
        Object scope = AopTestUtils
                .getUltimateTargetObject(webApplicationContext.getBean(ProgramSampleLabUnitScope.class));
        Object realService = ReflectionTestUtils.getField(scope, "sampleLabUnitScopeService");
        ReflectionTestUtils.setField(scope, "sampleLabUnitScopeService", Mockito.mock(SampleLabUnitScopeService.class));
        try {
            check.run();
        } finally {
            ReflectionTestUtils.setField(scope, "sampleLabUnitScopeService", realService);
        }
    }
}
