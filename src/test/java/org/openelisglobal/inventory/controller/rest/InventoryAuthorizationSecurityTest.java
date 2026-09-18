package org.openelisglobal.inventory.controller.rest;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.openelisglobal.inventory.projection.InventoryProjectionService;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryManagementService;
import org.openelisglobal.inventory.service.InventoryTagService;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
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
 * The inventory API is gated on a role, and on nothing else.
 *
 * <p>
 * The URL interceptor cannot gate it: it matches
 * {@code system_module_url.url_path} by exact string, so no seeded row reaches
 * a path carrying an id, and an unmatched {@code /rest} path is allowed rather
 * than refused. So {@code @PreAuthorize} is the whole of this module's
 * server-side authorization.
 *
 * <p>
 * This runs as a security slice rather than through
 * {@code BaseWebContextSensitiveTest}, because that context excludes
 * {@code SecurityConfig} from its component scan — method security is not
 * switched on there, so a guard could be deleted and every one of those tests
 * would still pass.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { InventoryAuthorizationSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class InventoryAuthorizationSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void theBoardRefusesAnUnauthenticatedCaller() throws Exception {
        mockMvc.perform(get("/rest/inventory/board").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Reception reaches neither the screen nor the API. The React route has always
     * excluded it; until now the API admitted it.
     */
    @Test
    public void theBoardRefusesARoleThatCannotOpenTheScreen() throws Exception {
        mockMvc.perform(get("/rest/inventory/board").with(user("reception").roles("RECEPTION"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    public void theBoardAdmitsExactlyTheRolesTheRouteDoes() throws Exception {
        mockMvc.perform(get("/rest/inventory/board").with(user("results").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
        mockMvc.perform(
                get("/rest/inventory/board").with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /**
     * The write that matters most: consuming decrements a lot and writes the usage
     * row the board projects from. A 403 here means the guard ran before the body
     * was even parsed.
     */
    @Test
    public void consumingStockRefusesAnUnrelatedRole() throws Exception {
        mockMvc.perform(post("/rest/inventory/management/consume").with(user("reception").roles("RECEPTION"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test
    public void receivingStockRefusesAnUnrelatedRole() throws Exception {
        mockMvc.perform(post("/rest/inventory/management/receive").with(user("reception").roles("RECEPTION"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test
    public void definingAnItemRefusesAnUnrelatedRole() throws Exception {
        mockMvc.perform(post("/rest/inventory/items").with(user("reception").roles("RECEPTION"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test
    public void listingItemsRefusesAnUnauthenticatedCaller() throws Exception {
        mockMvc.perform(get("/rest/inventory/items").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
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
        InventoryProjectionService inventoryProjectionService() {
            return mock(InventoryProjectionService.class);
        }

        @Bean
        InventoryItemService inventoryItemService() {
            return mock(InventoryItemService.class);
        }

        @Bean
        InventoryTagService inventoryTagService() {
            return mock(InventoryTagService.class);
        }

        @Bean
        InventoryManagementService inventoryManagementService() {
            return mock(InventoryManagementService.class);
        }

        @Bean
        UserModuleService userModuleService() {
            return mock(UserModuleService.class);
        }

        @Bean
        InventoryBoardRestController inventoryBoardRestController() {
            return new InventoryBoardRestController();
        }

        @Bean
        InventoryItemRestController inventoryItemRestController() {
            return new InventoryItemRestController();
        }

        @Bean
        InventoryManagementRestController inventoryManagementRestController() {
            return new InventoryManagementRestController();
        }
    }
}
