package org.openelisglobal.systemuser.controller.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.common.util.UserContextHolder;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.systemuser.validator.UnifiedSystemUserFormValidator;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.view.PageBuilderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Security slice test verifying that user management endpoints
 * (/rest/UnifiedSystemUser) require ADMIN role, while utility lookups
 * (/rest/users) are accessible to all authenticated users.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { UnifiedSystemUserRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class UnifiedSystemUserRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private final AutowireCapableBeanFactory beanFactory = mock(AutowireCapableBeanFactory.class);
    private final DefaultConfigurationProperties configurationProperties = mock(DefaultConfigurationProperties.class);
    private AutowireCapableBeanFactory previousFactory;
    private DisplayListService displayListService;
    private DisplayListService previousDisplayListService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private UserModuleService userModuleService;

    @Before
    public void setUpMocks() {
        previousFactory = (AutowireCapableBeanFactory) ReflectionTestUtils.getField(SpringContext.class, "factory");
        ReflectionTestUtils.setField(SpringContext.class, "factory", beanFactory);
        when(beanFactory.getBean(DefaultConfigurationProperties.class)).thenReturn(configurationProperties);
        when(configurationProperties.getPropertyValue(any(ConfigurationProperties.Property.class))).thenReturn("en");
        when(configurationProperties.getPropertyValue(ConfigurationProperties.Property.AmbiguousDateHolder))
                .thenReturn("/");
        when(configurationProperties.getPropertyValue(ConfigurationProperties.Property.DEFAULT_DATE_LOCALE))
                .thenReturn("en");

        UserContextHolder userContextHolder = mock(UserContextHolder.class);
        when(userContextHolder.getCurrentSysUserId()).thenReturn("1");
        when(beanFactory.getBean(UserContextHolder.class)).thenReturn(userContextHolder);

        previousDisplayListService = (DisplayListService) ReflectionTestUtils.getField(DisplayListService.class,
                "instance");
        displayListService = mock(DisplayListService.class);
        ReflectionTestUtils.setField(DisplayListService.class, "instance", displayListService);
        when(displayListService.getList(any())).thenReturn(Collections.emptyList());

        reset(roleService, systemUserService, userRoleService, userModuleService);
        when(userModuleService.isUserAdmin(any())).thenReturn(true);

        Role adminRole = new Role();
        adminRole.setId("1");
        adminRole.setName(Constants.ROLE_GLOBAL_ADMIN);
        when(roleService.getRoleByName(any())).thenReturn(adminRole);
        when(roleService.getAllActiveRoles()).thenReturn(Collections.emptyList());

        SystemUser user = new SystemUser();
        user.setId("5");
        user.setFirstName("Bob");
        user.setLastName("White");
        user.setLoginName("bwhite");
        user.setIsActive("Y");
        when(systemUserService.getAll()).thenReturn(List.of(user));
        when(userRoleService.userInRole("5", "techRole")).thenReturn(true);
    }

    private static MockHttpSession adminSession() {
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return session;
    }

    @After
    public void restoreSpringContextFactory() {
        ReflectionTestUtils.setField(SpringContext.class, "factory", previousFactory);
        ReflectionTestUtils.setField(DisplayListService.class, "instance", previousDisplayListService);
    }

    @Test
    public void unifiedSystemUser_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/rest/UnifiedSystemUser")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/rest/UnifiedSystemUser").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void usersList_WithoutAuthentication_Returns401() throws Exception {
        mockMvc.perform(get("/rest/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/rest/users/techRole")).andExpect(status().isUnauthorized());
    }

    @Test
    public void unifiedSystemUser_NonAdminRole_Returns403() throws Exception {
        mockMvc.perform(get("/rest/UnifiedSystemUser").with(user("technician").roles("RESULTS")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/rest/UnifiedSystemUser").with(user("technician").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());

        mockMvc.perform(get("/rest/UnifiedSystemUser").with(user("reception").roles("RECEPTION")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/rest/UnifiedSystemUser").with(user("reception").roles("RECEPTION"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test
    public void unifiedSystemUser_GlobalAdminRole_PassesAuthorization() throws Exception {
        mockMvc.perform(
                get("/rest/UnifiedSystemUser").session(adminSession()).with(user("admin").roles("GLOBAL_ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.formAction").value("UnifiedSystemUser"));

        mockMvc.perform(post("/rest/UnifiedSystemUser").session(adminSession())
                .with(user("admin").roles("GLOBAL_ADMIN")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void unifiedSystemUser_AdminRole_PassesAuthorization() throws Exception {
        mockMvc.perform(get("/rest/UnifiedSystemUser").session(adminSession()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.formAction").value("UnifiedSystemUser"));

        mockMvc.perform(post("/rest/UnifiedSystemUser").session(adminSession()).with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
    }

    @Test
    public void usersList_NonAdminRole_Returns200() throws Exception {
        mockMvc.perform(get("/rest/users").with(user("technician").roles("RESULTS"))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("5")).andExpect(jsonPath("$[0].value").value("White,Bob"));

        mockMvc.perform(get("/rest/users/techRole").with(user("technician").roles("RESULTS")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value("5"))
                .andExpect(jsonPath("$[0].value").value("White,Bob"));
    }

    @Test
    public void usersList_AdminRole_Returns200() throws Exception {
        mockMvc.perform(get("/rest/users").with(user("admin").roles("GLOBAL_ADMIN"))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("5")).andExpect(jsonPath("$[0].value").value("White,Bob"));

        mockMvc.perform(get("/rest/users/techRole").with(user("admin").roles("GLOBAL_ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value("5"))
                .andExpect(jsonPath("$[0].value").value("White,Bob"));
    }

    @Configuration
    @EnableWebMvc
    @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity(prePostEnabled = true)
    static class TestConfig {

        @Bean
        org.springframework.security.web.SecurityFilterChain securityFilterChain(
                org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .httpBasic(org.springframework.security.config.Customizer.withDefaults())
                    .csrf(csrf -> csrf.disable());
            return http.build();
        }

        @Bean
        UnifiedSystemUserFormValidator unifiedSystemUserFormValidator() {
            return mock(UnifiedSystemUserFormValidator.class);
        }

        @Bean
        LoginUserService loginUserService() {
            return mock(LoginUserService.class);
        }

        @Bean
        RoleService roleService() {
            RoleService mock = mock(RoleService.class);
            Role adminRole = new Role();
            adminRole.setId("1");
            adminRole.setName(Constants.ROLE_GLOBAL_ADMIN);
            when(mock.getRoleByName(any())).thenReturn(adminRole);
            return mock;
        }

        @Bean
        UserRoleService userRoleService() {
            return mock(UserRoleService.class);
        }

        @Bean
        SystemUserService systemUserService() {
            return mock(SystemUserService.class);
        }

        @Bean
        UserModuleService userModuleService() {
            return mock(UserModuleService.class);
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
        PageBuilderService pageBuilderService() {
            return mock(PageBuilderService.class);
        }

        @Bean
        MessageSource messageSource() {
            ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
            messageSource.setBasename("classpath:languages/message");
            messageSource.setDefaultEncoding("UTF-8");
            MessageUtil.setMessageSource(messageSource);
            return messageSource;
        }

        @Bean
        UnifiedSystemUserRestController unifiedSystemUserRestController(UnifiedSystemUserFormValidator formValidator,
                LoginUserService loginService, RoleService roleService, UserRoleService userRoleService,
                SystemUserService systemUserService, UserModuleService userModuleService, UserService userService,
                TestSectionService testSectionService, PageBuilderService pageBuilderService) {
            UnifiedSystemUserRestController controller = new UnifiedSystemUserRestController();
            ReflectionTestUtils.setField(controller, "formValidator", formValidator);
            ReflectionTestUtils.setField(controller, "loginService", loginService);
            ReflectionTestUtils.setField(controller, "roleService", roleService);
            ReflectionTestUtils.setField(controller, "userRoleService", userRoleService);
            ReflectionTestUtils.setField(controller, "systemUserService", systemUserService);
            ReflectionTestUtils.setField(controller, "userModuleService", userModuleService);
            ReflectionTestUtils.setField(controller, "userService", userService);
            ReflectionTestUtils.setField(controller, "testSectionService", testSectionService);
            ReflectionTestUtils.setField(controller, "pageBuilderService", pageBuilderService);
            return controller;
        }
    }
}
