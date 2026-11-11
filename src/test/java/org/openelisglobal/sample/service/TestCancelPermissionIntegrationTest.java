package org.openelisglobal.sample.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1613 (R-34): removing a test that has started from an order is allowed to
 * an administrator or a user holding a validation role, and refused to a user
 * who can only enter orders and results.
 */
@Transactional
public class TestCancelPermissionIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestCancelPermission testCancelPermission;

    private String userId;
    private MockHttpServletRequest request;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        for (String role : List.of("Reception", "Results", "Validator")) {
            ensureSystemRole(role);
        }
        Integer id = jdbcTemplate.queryForObject("SELECT nextval('clinlims.system_user_seq')", Integer.class);
        String loginName = "cancelperm" + id;
        jdbcTemplate.update(
                "INSERT INTO clinlims.system_user (id, login_name, last_name, first_name, is_active,"
                        + " is_employee, lastupdated) VALUES (?, ?, 'Permission', 'Tech', 'Y', 'Y', now())",
                id, loginName);
        userId = String.valueOf(id);
        grant("Reception");
        grant("Results");

        request = new MockHttpServletRequest();
        SecurityContextImpl context = new SecurityContextImpl(new UsernamePasswordAuthenticationToken(
                User.withUsername(loginName).password("unused").authorities(List.of()).build(), null));
        request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    @Test
    public void aUserWhoCanOnlyEnterOrdersAndResultsIsRefused() {
        assertFalse(testCancelPermission.mayCancelStartedTests(request, userId));
    }

    @Test
    public void aValidatorMayCancelAStartedTest() {
        grant("Validator");

        assertTrue(testCancelPermission.mayCancelStartedTests(request, userId));
    }

    private void grant(String role) {
        jdbcTemplate.update("INSERT INTO clinlims.system_user_role (system_user_id, role_id)"
                + " SELECT ?, id FROM clinlims.system_role WHERE name = ?", Integer.valueOf(userId), role);
    }
}
