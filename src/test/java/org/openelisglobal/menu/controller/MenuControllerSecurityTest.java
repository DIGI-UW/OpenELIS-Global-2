package org.openelisglobal.menu.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.menu.service.MenuService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@WebAppConfiguration
@ContextConfiguration(classes = MenuControllerSecurityTest.TestConfig.class)
public class MenuControllerSecurityTest extends SecuritySliceMockMvcTest {

    @Test
    public void adminMenuWithoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/admin/menu")).andExpect(status().isUnauthorized());
    }

    @Test
    public void nonAdminCannotReadOrChangeAdminMenu() throws Exception {
        var resultsUser = user("results").roles("RESULTS");
        mockMvc.perform(get("/rest/admin/menu").with(resultsUser)).andExpect(status().isForbidden());
        mockMvc.perform(get("/rest/admin/menu/missing").with(resultsUser)).andExpect(status().isForbidden());
        mockMvc.perform(
                post("/rest/admin/menu").with(resultsUser).contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void adminCanReadAndChangeAdminMenu() throws Exception {
        var adminUser = user("admin").roles("ADMIN");
        mockMvc.perform(get("/rest/admin/menu").with(adminUser)).andExpect(status().isOk());
        mockMvc.perform(get("/rest/admin/menu/missing").with(adminUser)).andExpect(status().isOk());
        mockMvc.perform(post("/rest/admin/menu").with(adminUser).contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isOk());
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
        MenuController menuController() {
            return new MenuController();
        }

        @Bean
        MenuService menuService() {
            MenuService service = mock(MenuService.class);
            when(service.getAll()).thenReturn(List.of());
            return service;
        }

        @Bean
        SpringContext springContext() {
            return new SpringContext();
        }

        @Bean
        UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        TestSectionService testSectionService() {
            return mock(TestSectionService.class);
        }

        @Bean
        UserRoleService userRoleService() {
            return mock(UserRoleService.class);
        }
    }
}
