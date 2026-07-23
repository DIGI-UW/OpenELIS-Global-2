package org.openelisglobal.analyzer.controller;

import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Base class for analyzer controller tests that exercise ADMIN-protected REST
 * endpoints.
 */
public abstract class AuthenticatedAnalyzerControllerTest extends BaseWebContextSensitiveTest {

    @Before
    public void setAdminAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @After
    public void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }
}
