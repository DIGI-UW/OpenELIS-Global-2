package org.openelisglobal.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import jakarta.annotation.PreDestroy;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.AppTestConfig;
import org.openelisglobal.BaseTestConfig;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.IsolatedDatabase;
import org.openelisglobal.common.action.IActionConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * The module authorization lookup used the raw request URI, so percent-encoding
 * a character in a mapped path skipped the lookup while Spring still routed the
 * request to the controller, and an unmapped REST path is let through
 * (SCAN-049, GHSA-2crq-rhhh-xv9j). Fixture
 * {@code testdata/module-url-probe.xml} maps {@code /PrivilegeProbe} to one
 * module; the REST form of that page shares the mapping.
 */
@ContextConfiguration(inheritLocations = false, classes = { AppTestConfig.class,
        ModuleAuthorizationEncodedPathTest.TestConfig.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@IsolatedDatabase
public class ModuleAuthorizationEncodedPathTest extends BaseWebContextSensitiveTest {

    /**
     * The test context leaves the interceptor package unscanned. The context starts
     * against its own freshly seeded database.
     */
    @Configuration
    public static class TestConfig extends BaseTestConfig {
        private final PostgreSQLContainer<?> database = new PostgreSQLContainer<>("postgres:14.4");

        @Override
        protected PostgreSQLContainer<?> databaseContainer() {
            return database;
        }

        @PreDestroy
        public void stopDatabase() {
            database.stop();
        }

        @Bean
        public ModuleAuthenticationInterceptor moduleAuthenticationInterceptor() {
            return new ModuleAuthenticationInterceptor();
        }
    }

    private static final String PLAIN = "/rest/PrivilegeProbe";
    private static final String ENCODED = "/rest/Privilege%50robe";
    private static final String MODULE = "Probe Module";

    @Autowired
    private ModuleAuthenticationInterceptor interceptor;

    @Before
    public void loadMapping() throws Exception {
        executeDataSetWithStateManagement("testdata/module-url-probe.xml");
    }

    @Test
    public void withoutTheModule_theEncodedPathIsRefusedLikeThePlainOne() throws Exception {
        assertFalse(interceptor.preHandle(request(PLAIN, Set.of()), new MockHttpServletResponse(), null));

        MockHttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request(ENCODED, Set.of()), response, null));
        assertEquals(401, response.getStatus());
    }

    @Test
    public void withTheModule_bothFormsAreAllowed() throws Exception {
        assertTrue(interceptor.preHandle(request(PLAIN, Set.of(MODULE)), new MockHttpServletResponse(), null));
        assertTrue(interceptor.preHandle(request(ENCODED, Set.of(MODULE)), new MockHttpServletResponse(), null));
    }

    /** A signed-in non-admin, probe, with the given modules. */
    private static MockHttpServletRequest request(String uri, Set<String> permittedModules) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setContextPath("");
        UserDetails probe = User.withUsername("probe").password("N/A").authorities("ROLE_USER").build();
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext
                .setAuthentication(new UsernamePasswordAuthenticationToken(probe, "N/A", probe.getAuthorities()));
        request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext);
        request.getSession().setAttribute(IActionConstants.PERMITTED_ACTIONS_MAP, new HashSet<>(permittedModules));
        return request;
    }
}
