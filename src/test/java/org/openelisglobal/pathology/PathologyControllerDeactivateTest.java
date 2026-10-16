package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.controller.pathology.PathologyController;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * The two endpoints that take a block or a slide out of use, and the case save
 * that refuses a row the case cannot hold.
 *
 * <p>
 * A retirement is a state change on a retained record, so it is reachable only
 * with a principal, it answers with the state the row is now in, and an id
 * nothing carries is a plain not-found rather than a failure. A slide that
 * names a block belonging to no case is the client asking for something
 * untraceable, and is answered with the sentence that says so rather than as a
 * server error.
 */
public class PathologyControllerDeactivateTest extends BaseWebContextSensitiveTest {

    private static final String BLOCK_ENDPOINT = "/rest/pathology/block/101/deactivate";

    private static final String SLIDE_ENDPOINT = "/rest/pathology/slide/201/deactivate";

    private static final String CASE_VIEW_ENDPOINT = "/rest/pathology/caseView/1";

    private static final int LEGACY_BLOCK_ID = 101;

    private static final int LEGACY_SLIDE_ID = 201;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AnnotationConfigWebApplicationContext securityContext;

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfig {

        /**
         * AppTestConfig does not component-scan org.openelisglobal.program.controller,
         * so the controller has no bean in the parent context. It is built here against
         * the parent's own services rather than mocks, because what a retirement has to
         * leave behind is a real row and a real audit entry.
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
    public void init() throws Exception {
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
    public void tearDown() {
        jdbcTemplate.update("DELETE FROM clinlims.history WHERE reference_table IN"
                + " (SELECT id FROM clinlims.reference_tables WHERE name IN ('PATHOLOGY_BLOCK', 'PATHOLOGY_SLIDE'))");
        if (securityContext != null) {
            securityContext.close();
        }
    }

    @Test
    public void deactivate_withoutAPrincipal_isRefused() throws Exception {
        // The base class runs every test as the daemon user (@WithDaemonUser), and the
        // security post-processor hands that principal to each request, so a request
        // has to say it is anonymous or it inherits one.
        int status = mockMvc.perform(post(BLOCK_ENDPOINT).with(anonymous())).andReturn().getResponse().getStatus();

        assertEquals("taking a block out of use is a change to a retained record, so it is not reachable"
                + " without a principal", HttpStatus.UNAUTHORIZED.value(), status);
        assertTrue("and the block is untouched", isActive("pathology_block", LEGACY_BLOCK_ID));
    }

    @Test
    public void deactivateBlock_answers200WithTheRowState() throws Exception {
        MockHttpServletResponse response = postAsTechnician(BLOCK_ENDPOINT,
                "{\"reason\":\"cross-contamination suspected\"}");

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        Map<String, Object> body = bodyOf(response);
        assertEquals("the answer names the block that was retired", Integer.valueOf(LEGACY_BLOCK_ID), body.get("id"));
        assertEquals("and the state it is now in, so the screen does not have to guess", Boolean.FALSE,
                body.get("active"));
    }

    @Test
    public void deactivateBlock_withoutABody_stillRetiresTheRow() throws Exception {
        MockHttpServletResponse response = postAsTechnician(BLOCK_ENDPOINT, null);

        assertEquals("the bench is not obliged to give a reason", HttpStatus.OK.value(), response.getStatus());
        assertEquals(Boolean.FALSE, bodyOf(response).get("active"));
    }

    @Test
    public void deactivateUnknownBlock_answers404() throws Exception {
        MockHttpServletResponse response = postAsTechnician("/rest/pathology/block/424242/deactivate", null);

        assertEquals("an id no block carries is nothing to retire, not a failure", HttpStatus.NOT_FOUND.value(),
                response.getStatus());
        assertTrue("and the answer says which id that was",
                String.valueOf(bodyOf(response).get("error")).contains("424242"));
    }

    @Test
    public void deactivateSlide_answers200WithTheRowState() throws Exception {
        MockHttpServletResponse response = postAsTechnician(SLIDE_ENDPOINT, "{\"reason\":\"section folded\"}");

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        Map<String, Object> body = bodyOf(response);
        assertEquals("the answer names the slide that was retired", Integer.valueOf(LEGACY_SLIDE_ID), body.get("id"));
        assertEquals("and the state it is now in", Boolean.FALSE, body.get("active"));
    }

    @Test
    public void saveWithAForeignSlideBlock_answers400WithASentence() throws Exception {
        MockHttpServletResponse response = postAsTechnician(CASE_VIEW_ENDPOINT,
                "{\"status\":\"GROSSING\"," + "\"blocks\":[],\"reports\":[],\"slides\":[{\"blockId\":424242}]}");

        assertEquals("a slide that cannot be traced to a block on this case is the client asking for"
                + " something the case cannot hold", HttpStatus.BAD_REQUEST.value(), response.getStatus());
        String error = String.valueOf(bodyOf(response).get("error"));
        assertTrue("and the answer is a sentence naming the block, not a stack trace",
                error.contains("block") && error.contains("424242"));
        assertEquals("nothing was stored", 1, slideCountOnCase(1));
    }

    // helpers

    private MockHttpServletResponse postAsTechnician(String endpoint, String body) throws Exception {
        if (body == null) {
            return mockMvc.perform(post(endpoint).with(user("technician1"))).andReturn().getResponse();
        }
        return mockMvc
                .perform(post(endpoint).with(user("technician1")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse();
    }

    private Map<String, Object> bodyOf(MockHttpServletResponse response) throws Exception {
        return objectMapper.readValue(response.getContentAsString(), new TypeReference<Map<String, Object>>() {
        });
    }

    private boolean isActive(String table, int id) {
        List<Boolean> active = jdbcTemplate.queryForList("SELECT active FROM clinlims." + table + " WHERE id = ?",
                Boolean.class, id);
        return !active.isEmpty() && Boolean.TRUE.equals(active.get(0));
    }

    private int slideCountOnCase(int pathologySampleId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinlims.pathology_slide WHERE" + " pathology_sample_id = ?", Integer.class,
                pathologySampleId);
    }
}
