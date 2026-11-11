package org.openelisglobal.testsupport;

import org.openelisglobal.config.ControllerSetup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * The web + security scaffolding every {@link SecuritySliceMockMvcTest} slice
 * needs, so a slice test declares only its controller and that controller's
 * collaborators. List it alongside the test's own configuration:
 * {@code @ContextConfiguration(classes = { SliceSecurityConfig.class,
 * MyTest.TestConfig.class })}.
 *
 * <p>
 * The filter chain authenticates every request so that an unauthenticated call
 * is rejected before reaching a handler. That is this configuration's rule
 * rather than production's, so it is never itself worth asserting — what a
 * slice test is here to pin is the {@code @PreAuthorize} expression on the
 * controller, which decides 200 against 403 for an authenticated caller.
 */
/**
 * Lives outside {@code org.openelisglobal.security} on purpose: the shared test
 * context component-scans that package, so a configuration placed there is
 * picked up by every test, and its extra security setup changes how beans
 * across the application are proxied.
 */
@Configuration
@EnableWebMvc
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SliceSecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    /**
     * The real {@code @ControllerAdvice}, because live traffic routes
     * {@code @PreAuthorize} denials through it and its generic RuntimeException
     * handler used to turn them into 500s. With it registered, every 403 assertion
     * guards the advice ordering too.
     */
    @Bean
    ControllerSetup controllerSetup() {
        return new ControllerSetup();
    }
}
