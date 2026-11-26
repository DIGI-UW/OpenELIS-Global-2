package org.openelisglobal.program.controller.immunohistochemistry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.config.ControllerSetup;
import org.openelisglobal.program.service.ProgramSampleLabUnitScope;
import org.openelisglobal.sample.service.SampleLabUnitScopeService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * The immunohistochemistry case view's read and save for a case id nothing
 * carries, for a case outside the user's lab units, and for a missing row that
 * is not the case.
 *
 * <p>
 * A case id nothing carries is not found, as is a case the user's units do not
 * cover; a missing row the save only refers to stays a server error, so the
 * screen is never told the case is gone.
 */
public class ImmunohistochemistryControllerCaseNotFoundTest extends BaseWebContextSensitiveTest {

    /**
     * A case this test writes over the fixture's first sample, which carries a
     * patient so the case view can be built. Outside the ids any dataset uses.
     */
    private static final int CASE_ID = 9601;

    private static final String CASE_VIEW = "/rest/immunohistochemistry/caseView/" + CASE_ID;

    private static final String UNKNOWN_CASE_VIEW = "/rest/immunohistochemistry/caseView/424242";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AnnotationConfigWebApplicationContext securityContext;

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    static class TestConfig {

        /**
         * AppTestConfig does not component-scan org.openelisglobal.program.controller,
         * so the controller is built here over the parent's own services.
         */
        @Bean
        ImmunohistochemistryController immunohistochemistryController(ApplicationContext context) {
            return context.getParent().getAutowireCapableBeanFactory().createBean(ImmunohistochemistryController.class);
        }

        /**
         * AppTestConfig leaves out org.openelisglobal.config, so the application's
         * answer to any unexpected failure is registered here, as production has it.
         */
        @Bean
        ControllerSetup controllerSetup() {
            return new ControllerSetup();
        }

        /** The rule the application's chain applies to /rest/**. */
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults());
            return http.build();
        }
    }

    @Before
    public void loadCaseAndSecurity() throws Exception {
        executeDataSetWithStateManagement("testdata/cytology-display-service.xml");
        // The fixture replaces system_user with its own users, so authenticate as one
        // of them.
        authenticateAs("technician1");
        removeOwnCase();
        jdbcTemplate.update("INSERT INTO clinlims.immunohistochemistry_sample"
                + " (id, technician_id, pathologist_id, program_id, sample_id, status, reffered, last_updated)"
                + " VALUES (?, 1, 2, 1, 1, 'IN_PROGRESS', false, now())", CASE_ID);

        securityContext = new AnnotationConfigWebApplicationContext();
        securityContext.setParent(webApplicationContext);
        securityContext.setServletContext(webApplicationContext.getServletContext());
        securityContext.register(TestConfig.class);
        securityContext.refresh();
        mockMvc = MockMvcBuilders.webAppContextSetup(securityContext).apply(springSecurity()).build();
    }

    @After
    public void removeCaseAndCloseSecurity() {
        removeOwnCase();
        if (securityContext != null) {
            securityContext.close();
        }
    }

    /**
     * A case id nothing carries is not found, whether the case view is read or
     * saved or the case is taken by a technician or a pathologist, rather than a
     * server error.
     */
    @Test
    public void anUnknownCase_answers404OnReadAndSave() throws Exception {
        MockHttpServletResponse read = read(UNKNOWN_CASE_VIEW);
        MockHttpServletResponse save = save(UNKNOWN_CASE_VIEW, "{}");

        assertEquals("reading a case nobody has is not found", HttpStatus.NOT_FOUND.value(), read.getStatus());
        assertTrue("and the answer names the id", String.valueOf(bodyOf(read).get("error")).contains("424242"));
        assertEquals("saving one is not found either", HttpStatus.NOT_FOUND.value(), save.getStatus());
        assertTrue("and that answer names the id too", String.valueOf(bodyOf(save).get("error")).contains("424242"));
        assertEquals("taking it as technician is not found", HttpStatus.NOT_FOUND.value(),
                assign("/rest/immunohistochemistry/assignTechnician").getStatus());
        assertEquals("taking it as pathologist is not found", HttpStatus.NOT_FOUND.value(),
                assign("/rest/immunohistochemistry/assignPathologist").getStatus());
    }

    /** The fixture's case is read as the screen reads it. */
    @Test
    public void aKnownCase_answers200OnRead() throws Exception {
        assertEquals("a case the fixture carries is read", HttpStatus.OK.value(), read(CASE_VIEW).getStatus());
    }

    /**
     * A case the user's lab units do not cover is not found. This 404 comes from
     * the lab-unit scope, not the not-found handler; the test pins that the handler
     * claims only ObjectNotFoundException.
     */
    @Test
    public void aCaseOutsideTheUsersUnits_answers404() throws Throwable {
        asAUserWhoseUnits(false, () -> assertEquals("a case outside the user's units is not found",
                HttpStatus.NOT_FOUND.value(), read(CASE_VIEW).getStatus()));
    }

    /**
     * The same read through the same scope switch, answered visible, is read, so
     * the refusal above comes from the units and not from the switch. The answer
     * comes from the lab-unit scope, not the not-found handler; the test pins that
     * the handler claims only ObjectNotFoundException.
     */
    @Test
    public void aCaseInsideTheUsersUnits_answers200() throws Throwable {
        asAUserWhoseUnits(true, () -> assertEquals("a case inside the user's units is read", HttpStatus.OK.value(),
                read(CASE_VIEW).getStatus()));
    }

    /**
     * A pathologist id no user carries is a failure of the save, not a missing
     * case, so it stays the server error any unexpected failure is.
     */
    @Test
    public void aMissingRowThatIsNotTheCase_staysAServerError() throws Exception {
        MockHttpServletResponse response = save(CASE_VIEW, "{\"assignedPathologistId\":\"424242\"}");

        assertEquals("the case exists, so the answer is a server error", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                response.getStatus());
    }

    private void removeOwnCase() {
        jdbcTemplate.update("DELETE FROM clinlims.immunohistochemistry_sample WHERE id = ?", CASE_ID);
    }

    private MockHttpServletResponse read(String endpoint) throws Exception {
        return mockMvc.perform(get(endpoint).with(user("technician1"))).andReturn().getResponse();
    }

    /** Takes the unknown case on one of the bench's assign endpoints. */
    private MockHttpServletResponse assign(String endpoint) throws Exception {
        return mockMvc.perform(post(endpoint).param("immunohistochemistrySampleId", "424242").with(user("technician1")))
                .andReturn().getResponse();
    }

    private MockHttpServletResponse save(String endpoint, String body) throws Exception {
        return mockMvc
                .perform(post(endpoint).with(user("technician1")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse();
    }

    private Map<String, Object> bodyOf(MockHttpServletResponse response) throws Exception {
        return objectMapper.readValue(response.getContentAsString(), new TypeReference<Map<String, Object>>() {
        });
    }

    /**
     * Runs the check with the lab-unit scope answering every case as visible or
     * not, then restores the real scope.
     */
    private void asAUserWhoseUnits(boolean coverTheCase, ThrowingRunnable check) throws Throwable {
        Object scope = AopTestUtils
                .getUltimateTargetObject(webApplicationContext.getBean(ProgramSampleLabUnitScope.class));
        Object realService = ReflectionTestUtils.getField(scope, "sampleLabUnitScopeService");
        SampleLabUnitScopeService units = Mockito.mock(SampleLabUnitScopeService.class);
        Mockito.when(units.isVisible(Mockito.any(), Mockito.any())).thenReturn(coverTheCase);
        ReflectionTestUtils.setField(scope, "sampleLabUnitScopeService", units);
        try {
            check.run();
        } finally {
            ReflectionTestUtils.setField(scope, "sampleLabUnitScopeService", realService);
        }
    }
}
