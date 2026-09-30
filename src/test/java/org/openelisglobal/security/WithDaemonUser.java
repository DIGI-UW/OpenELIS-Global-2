package org.openelisglobal.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.test.context.support.WithSecurityContext;

/**
 * Test annotation that populates the SecurityContext with a
 * {@link DaemonAuthenticationToken} carrying {@code ROLE_SYSTEM}. Use on test
 * methods or classes that need to run as the daemon user.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@WithSecurityContext(factory = WithDaemonUserSecurityContextFactory.class)
public @interface WithDaemonUser {
}
