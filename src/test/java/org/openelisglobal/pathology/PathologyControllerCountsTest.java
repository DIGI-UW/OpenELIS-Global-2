package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.bean.PathologyDashBoardCount;
import org.openelisglobal.program.controller.pathology.PathologyController;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
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
 * The Pathology Dashboard's four count tiles (AC-4, AC-6). The tiles used to
 * name four statuses of the old eight-value enum, so once the bench stages
 * replaced them a case at any of the five new stages was counted on no tile at
 * all, and a case with an outstanding pathologist request was only counted
 * because it had been parked in a status that was not a bench stage.
 *
 * <p>
 * The fixture supplies two cases at GROSSING, one of them holding an open
 * request; this test adds one case at every stage on top, so every stage is
 * represented exactly once and each tile's grouping is countable.
 */
public class PathologyControllerCountsTest extends BaseWebContextSensitiveTest {

    private static final String COUNT_ENDPOINT = "/rest/pathology/dashboard/count";

    /** One seeded case per stage: {@code FIRST_SEEDED_ID + status.ordinal()}. */
    private static final int FIRST_SEEDED_ID = 9201;

    private static final int GROSSING_CASES_IN_THE_FIXTURE = 2;

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
         * the parent's own services rather than mocks, because what the tiles count is
         * the result of real queries over real rows.
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
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).httpBasic(Customizer.withDefaults());
            return http.build();
        }
    }

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/pathology-sample.xml");
        // The fixture replaces system_user with its own users, so authenticate as one
        // of them.
        authenticateAs("technician1");
        cleanup();
        for (PathologyStatus status : PathologyStatus.values()) {
            insertPathologySample(idFor(status), status.name());
        }

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
        cleanup();
        if (securityContext != null) {
            securityContext.close();
        }
    }

    @Test
    public void count_withoutAPrincipal_isRefused() throws Exception {
        // The base class runs every test as the daemon user (@WithDaemonUser), and the
        // security post-processor hands that principal to each request, so a request
        // has to say it is anonymous or it inherits one.
        int status = mockMvc.perform(get(COUNT_ENDPOINT).with(anonymous())).andReturn().getResponse().getStatus();

        assertEquals("the dashboard tiles say how many cases the lab is holding, so the endpoint is not"
                + " readable without a principal", HttpStatus.UNAUTHORIZED.value(), status);
    }

    @Test
    public void count_withAPrincipal_isServed() throws Exception {
        int status = callCountEndpoint().getStatus();

        assertEquals("the refusal above is about the missing principal and not about the endpoint being"
                + " unreachable in this test", HttpStatus.OK.value(), status);
    }

    @Test
    public void count_inProgress_countsEveryStageThatIsStillBenchWork() throws Exception {
        Long inProgress = counts().getInProgress();

        assertEquals(
                "every stage but the review queue and the finished cases is work in progress, and a"
                        + " case at one of them belongs on this tile",
                Long.valueOf(9L + GROSSING_CASES_IN_THE_FIXTURE), inProgress);
    }

    @Test
    public void count_awaitingReview_countsOnlyTheReviewQueue() throws Exception {
        Long awaitingReview = counts().getAwaitingReview();

        assertEquals("only a case waiting for a pathologist to pick it up is awaiting review", Long.valueOf(1L),
                awaitingReview);
    }

    @Test
    public void count_additionalRequests_countsCasesHoldingAnOpenRequest() throws Exception {
        Long additionalRequests = counts().getAdditionalRequests();

        assertEquals("an outstanding request is read from the case's request rows, so only the fixture's"
                + " case holding one is counted", Long.valueOf(1L), additionalRequests);
    }

    @Test
    public void count_complete_countsTheCasesFinishedInTheLastWeek() throws Exception {
        Long complete = counts().getComplete();

        assertEquals("only the seeded COMPLETED case counts; the fixture's cases are at GROSSING and are"
                + " excluded by stage, not by date", Long.valueOf(1L), complete);
    }

    /**
     * Taking a case out of the review queue must move it between two tiles, not
     * drop it from both: a pathologist reviewing a case is working it.
     */
    @Test
    public void count_aCaseTakenOutOfTheReviewQueue_movesOntoTheInProgressTile() throws Exception {
        jdbcTemplate.update("UPDATE clinlims.pathology_sample SET status = ? WHERE id = ?",
                PathologyStatus.UNDER_REVIEW.name(), idFor(PathologyStatus.READY_PATHOLOGIST));

        PathologyDashBoardCount counts = counts();

        assertEquals("nothing is left in the queue once the only case in it is picked up", Long.valueOf(0L),
                counts.getAwaitingReview());
        assertEquals("the case a pathologist is reviewing is counted as work in progress",
                Long.valueOf(10L + GROSSING_CASES_IN_THE_FIXTURE), counts.getInProgress());
    }

    // helpers

    private MockHttpServletResponse callCountEndpoint() throws Exception {
        // The endpoint asks only for an authenticated principal, so no role is
        // granted here on purpose.
        return mockMvc.perform(get(COUNT_ENDPOINT).with(user("technician1"))).andReturn().getResponse();
    }

    private PathologyDashBoardCount counts() throws Exception {
        MockHttpServletResponse response = callCountEndpoint();
        assertEquals("the tiles cannot be read if the endpoint did not answer", HttpStatus.OK.value(),
                response.getStatus());
        return objectMapper.readValue(response.getContentAsString(), PathologyDashBoardCount.class);
    }

    private static int idFor(PathologyStatus status) {
        return FIRST_SEEDED_ID + status.ordinal();
    }

    /**
     * The id is the table's only NOT NULL column, but the entity reads the status
     * as an enum name, so it is supplied as a name the enum declares. The row is
     * stamped an hour ago rather than now() so that a database clock running a
     * little ahead of the JVM's cannot push it past the "complete" tile's upper
     * bound, which the controller takes from the JVM.
     */
    private void insertPathologySample(int id, String status) {
        jdbcTemplate.update("INSERT INTO clinlims.pathology_sample (id, program_id, sample_id, status,"
                + " last_updated) VALUES (?, 1, 1, ?, now() - interval '1 hour')", id, status);
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE FROM clinlims.pathology_sample WHERE id BETWEEN ? AND ?", FIRST_SEEDED_ID,
                FIRST_SEEDED_ID + PathologyStatus.values().length - 1);
    }
}
