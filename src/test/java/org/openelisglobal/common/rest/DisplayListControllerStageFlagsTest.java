package org.openelisglobal.common.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.program.util.PathologyStages;
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
 * The pathology case view's stage rail has to know which of the seven optional
 * bench stages this deployment has switched off, so it can render one as not
 * applicable instead of asking the operator to work a stage the lab does not
 * use (FR-2.3, AC-7). The switches reach the browser only through the
 * authenticated {@code /rest/configuration-properties} endpoint, and before
 * this nothing there named them, so this drives that endpoint the way a browser
 * does.
 *
 * <p>
 * AppTestConfig does not component-scan {@code org.openelisglobal.common.rest},
 * so {@link DisplayListController} has no bean in the parent context; it is
 * built here against the parent's real services, following the same pattern
 * {@code PathologyControllerCountsTest} uses for a controller outside the scan.
 */
public class DisplayListControllerStageFlagsTest extends BaseWebContextSensitiveTest {

    private static final String ENDPOINT = "/rest/configuration-properties";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AnnotationConfigWebApplicationContext securityContext;

    private String coverslippingBefore;

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfig {

        /**
         * AppTestConfig does not component-scan org.openelisglobal.common.rest, so the
         * controller has no bean in the parent context. It is built here against the
         * parent's own services rather than mocks, because what the endpoint reports is
         * the result of reading real configuration.
         */
        @Bean
        DisplayListController displayListController(ApplicationContext context) {
            return context.getParent().getAutowireCapableBeanFactory().createBean(DisplayListController.class);
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
        // The base class already authenticates as admin; this class loads no fixture
        // that would replace system_user, so it keeps that principal.
        coverslippingBefore = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.PATHOLOGY_STAGE_COVERSLIPPING_ENABLED);

        securityContext = new AnnotationConfigWebApplicationContext();
        securityContext.setParent(webApplicationContext);
        securityContext.setServletContext(webApplicationContext.getServletContext());
        securityContext.register(TestConfig.class);
        securityContext.refresh();
        mockMvc = MockMvcBuilders.webAppContextSetup(securityContext).apply(springSecurity()).build();
    }

    @After
    public void tearDown() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_STAGE_COVERSLIPPING_ENABLED,
                coverslippingBefore == null ? "true" : coverslippingBefore);
        if (securityContext != null) {
            securityContext.close();
        }
    }

    @Test
    public void configurationProperties_withoutAPrincipal_isRefused() throws Exception {
        // The base class runs every test as the daemon user (@WithDaemonUser), and the
        // security post-processor hands that principal to each request, so a request
        // has to say it is anonymous or it inherits one.
        int status = mockMvc.perform(get(ENDPOINT).with(anonymous())).andReturn().getResponse().getStatus();

        assertEquals(
                "the stage switches only tell a signed-in browser how to draw the case view, so the"
                        + " endpoint requires a principal like every other /rest/** route",
                HttpStatus.UNAUTHORIZED.value(), status);
    }

    @Test
    public void configurationProperties_withAPrincipal_isServed() throws Exception {
        int status = callEndpoint().getStatus();

        assertEquals("the refusal above is about the missing principal and not about the endpoint being"
                + " unreachable in this test", HttpStatus.OK.value(), status);
    }

    @Test
    public void configurationProperties_exposeEverySwitchableStageAsEnabled() throws Exception {
        Map<String, Object> configs = configs();

        int checked = 0;
        for (PathologyStatus status : PathologyStages.ordered()) {
            Optional<Property> property = PathologyStages.enablementProperty(status);
            if (property.isEmpty()) {
                continue;
            }
            String key = property.get().toString();
            assertTrue("the case view's stage rail cannot render " + status + " without its switch, key " + key,
                    configs.containsKey(key));
            assertEquals(status + "'s switch ships enabled, so a fresh deployment renders it like any other"
                    + " bench stage", "true", configs.get(key));
            checked++;
        }
        assertEquals("all seven switches were checked, so a mapping answering empty for every stage could not"
                + " pass this test", 7, checked);
    }

    @Test
    public void configurationProperties_carryNoSwitchForAMandatoryStage() throws Exception {
        Map<String, Object> configs = configs();

        for (PathologyStatus status : List.of(PathologyStatus.ACCESSIONED, PathologyStatus.GROSSING,
                PathologyStatus.READY_PATHOLOGIST, PathologyStatus.COMPLETED)) {
            String key = "PATHOLOGY_STAGE_" + status.name() + "_ENABLED";

            assertFalse(status + " is mandatory and nothing may disable it, so the endpoint must carry no"
                    + " switch a deployment could mistake for one", configs.containsKey(key));
        }
    }

    @Test
    public void configurationProperties_followTheLiveSettingNotABakedInValue() throws Exception {
        ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_STAGE_COVERSLIPPING_ENABLED, "false");

        Map<String, Object> configs = configs();

        assertEquals("the endpoint must answer with whatever the admin page last set, not a value fixed at startup",
                "false", configs.get(Property.PATHOLOGY_STAGE_COVERSLIPPING_ENABLED.toString()));
    }

    // helpers

    private MockHttpServletResponse callEndpoint() throws Exception {
        return mockMvc.perform(get(ENDPOINT).with(user("admin"))).andReturn().getResponse();
    }

    private Map<String, Object> configs() throws Exception {
        MockHttpServletResponse response = callEndpoint();
        assertEquals("the switches cannot be read if the endpoint did not answer", HttpStatus.OK.value(),
                response.getStatus());
        return objectMapper.readValue(response.getContentAsString(), new TypeReference<Map<String, Object>>() {
        });
    }
}
