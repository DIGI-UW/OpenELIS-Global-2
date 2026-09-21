package org.openelisglobal.storage.controller;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.storage.service.SampleStorageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Moving and listing reagent lots is gated on a role.
 *
 * <p>
 * It was not: the controller carried no {@code @PreAuthorize} at all, so any
 * authenticated user could enumerate every lot in the lab and move one to
 * another box. The URL interceptor cannot stand in for the annotation — it
 * matches {@code system_module_url.url_path} by exact string, so no seeded row
 * reaches a path carrying an id, and an unmatched {@code /rest} path is allowed
 * rather than refused.
 *
 * <p>
 * A security slice rather than {@code BaseWebContextSensitiveTest}, because
 * that context excludes {@code SecurityConfig} from its component scan: method
 * security is off there, so the guard could be deleted again and every one of
 * those tests would still pass.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { InventoryLotStorageAuthorizationSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class InventoryLotStorageAuthorizationSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void listingLotsRefusesAnUnauthenticatedCaller() throws Exception {
        mockMvc.perform(get("/rest/storage/inventory-lots").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    /**
     * A logged-in user with no business in storage. This is the case that was open:
     * authentication alone was the whole check.
     */
    @Test
    public void listingLotsRefusesARoleThatCannotOpenTheScreen() throws Exception {
        mockMvc.perform(get("/rest/storage/inventory-lots").with(user("validator").roles("VALIDATION"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void movingALotRefusesARoleThatCannotOpenTheScreen() throws Exception {
        mockMvc.perform(post("/rest/storage/inventory-lots/move").with(user("validator").roles("VALIDATION"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    /** Exactly the roles the Storage route admits, and no more. */
    @Test
    public void listingLotsAdmitsTheRolesTheRouteDoes() throws Exception {
        for (String role : new String[] { "RECEPTION", "RESULTS", "ADMIN" }) {
            mockMvc.perform(get("/rest/storage/inventory-lots").with(user(role.toLowerCase()).roles(role))
                    .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
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
        SampleStorageService sampleStorageService() {
            return mock(SampleStorageService.class);
        }

        @Bean
        UserModuleService userModuleService() {
            return mock(UserModuleService.class);
        }

        @Bean
        InventoryLotStorageRestController inventoryLotStorageRestController() {
            return new InventoryLotStorageRestController();
        }
    }
}
