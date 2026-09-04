package org.openelisglobal.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Parameterized access-control integration test.
 *
 * <p>
 * For each endpoint asserts two things:
 * <ol>
 * <li>A user with the WRONG privilege receives HTTP 403.</li>
 * <li>A user with the CORRECT privilege is NOT rejected (not 403).</li>
 * </ol>
 *
 * <p>
 * Uses lightweight stub controllers that carry only the {@code @PreAuthorize}
 * annotation under test — no real service dependencies, no deep autowiring.
 * This keeps the Spring context minimal and avoids cascading
 * PermissionModuleService / UserModuleService dependency chains.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { EndpointAccessControlTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class EndpointAccessControlTest extends SecuritySliceMockMvcTest {

    private static final List<EndpointCase> ENDPOINT_CASES = List.of(
            new EndpointCase("orders", "/rest/stub/TestSectionOrder", "PRIV_SYSTEM_CONFIGURE"),
            new EndpointCase("results", "/rest/stub/accession-results", "PRIV_RESULT_VIEW"),
            new EndpointCase("nce", "/rest/stub/nce-number", "PRIV_NCE_VIEW"),
            new EndpointCase("admin", "/rest/stub/SampleTypeManagement", "PRIV_SYSTEM_CONFIGURE"));

    @Test
    public void endpoints_shouldReturn403_whenRequiredPrivilegeIsMissing() throws Exception {
        for (EndpointCase endpointCase : ENDPOINT_CASES) {
            int status = mockMvc.perform(get(endpointCase.path())
                    .with(user("limited-user").authorities(new SimpleGrantedAuthority("PRIV_UNRELATED")))
                    .contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse().getStatus();

            assertEquals(String.format("Expected 403 for %s [%s] when missing %s", endpointCase.category(),
                    endpointCase.path(), endpointCase.requiredPrivilege()), 403, status);
        }
    }

    @Test
    public void endpoint_orders_shouldNotReturn403_withAdminPrivilege() throws Exception {
        assertAuthPasses("/rest/stub/TestSectionOrder", "PRIV_SYSTEM_CONFIGURE");
    }

    @Test
    public void endpoint_results_shouldNotReturn403_withResultViewPrivilege() throws Exception {
        assertAuthPasses("/rest/stub/accession-results", "PRIV_RESULT_VIEW");
    }

    @Test
    public void endpoint_nce_shouldNotReturn403_withNceViewPrivilege() throws Exception {
        assertAuthPasses("/rest/stub/nce-number", "PRIV_NCE_VIEW");
    }

    @Test
    public void endpoint_admin_shouldNotReturn403_withAdminPrivilege() throws Exception {
        assertAuthPasses("/rest/stub/SampleTypeManagement", "PRIV_SYSTEM_CONFIGURE");
    }

    private void assertAuthPasses(String path, String privilege) throws Exception {
        int status = mockMvc
                .perform(get(path).with(user("authorized-user").authorities(new SimpleGrantedAuthority(privilege)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getStatus();

        assertNotEquals("Expected non-403 for [" + path + "] with privilege " + privilege, 403, status);
    }

    private static class EndpointCase {
        private final String category;
        private final String path;
        private final String requiredPrivilege;

        EndpointCase(String category, String path, String requiredPrivilege) {
            this.category = category;
            this.path = path;
            this.requiredPrivilege = requiredPrivilege;
        }

        String category() {
            return category;
        }

        String path() {
            return path;
        }

        String requiredPrivilege() {
            return requiredPrivilege;
        }
    }

    // -----------------------------------------------------------------------
    // Minimal stub controllers — no @Autowired deps, only @PreAuthorize.
    // These mirror the privilege requirements of the real controllers without
    // pulling in BaseController / UserModuleService / PermissionModuleService.
    // -----------------------------------------------------------------------

    @RestController
    @RequestMapping("/rest/stub")
    static class StubControllers {

        @GetMapping("/TestSectionOrder")
        @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
        public ResponseEntity<String> testSectionOrder() {
            return ResponseEntity.ok("ok");
        }

        @GetMapping("/accession-results")
        @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
        public ResponseEntity<String> accessionResults() {
            return ResponseEntity.ok("ok");
        }

        @GetMapping("/nce-number")
        @PreAuthorize("hasAuthority('PRIV_NCE_VIEW')")
        public ResponseEntity<String> nceNumber() {
            return ResponseEntity.ok("ok");
        }

        @GetMapping("/SampleTypeManagement")
        @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
        public ResponseEntity<String> sampleTypeManagement() {
            return ResponseEntity.ok("ok");
        }
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity(prePostEnabled = true)
    static class TestConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).httpBasic(Customizer.withDefaults())
                    .csrf(csrf -> csrf.disable());
            return http.build();
        }

        @Bean
        StubControllers stubControllers() {
            return new StubControllers();
        }
    }
}
