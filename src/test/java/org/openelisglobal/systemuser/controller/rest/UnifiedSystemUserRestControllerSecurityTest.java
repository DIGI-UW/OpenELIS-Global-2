package org.openelisglobal.systemuser.controller.rest;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.ServletException;
import org.junit.After;
import org.junit.Test;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.security.SecuritySliceMockMvcTest;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.systemuser.validator.UnifiedSystemUserFormValidator;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.view.PageBuilderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Issue #4938: ModuleAuthenticationInterceptor skips the legacy permission
 * check for every /rest path, so /rest/UnifiedSystemUser relies on
 * {@code @PreAuthorize}. Reading the user editor (every account and the role
 * configuration) and saving it (create or edit an account, set a password,
 * grant roles including admin) need the admin role. A receptionist or a lab
 * technician is refused and nothing is read or written. The user pickers
 * (/rest/users, /rest/users/{roleName}) that Cytology, Pathology, Notebooks and
 * Audit Logs use stay open to any signed-in user.
 *
 * To prove an admin gets past the role check and into the handler, the first
 * service each handler calls answers 418; the tests look for that status.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { UnifiedSystemUserRestControllerSecurityTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class UnifiedSystemUserRestControllerSecurityTest extends SecuritySliceMockMvcTest {

    private static final String USER_FORM_JSON = "{\"userLoginName\":\"newadmin\",\"userFirstName\":\"New\","
            + "\"userLastName\":\"Admin\",\"userPassword\":\"Secret#2026\",\"confirmPassword\":\"Secret#2026\","
            + "\"selectedRoles\":[\"1\"]}";

    @Autowired
    private RoleService roleService;

    @Autowired
    private UnifiedSystemUserFormValidator formValidator;

    @Autowired
    private LoginUserService loginUserService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private UserService userService;

    @After
    public void resetMocks() {
        reset(roleService, formValidator, loginUserService, systemUserService, userService);
    }

    // GET /rest/UnifiedSystemUser: the user editor

    @Test
    public void readUserEditor_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/UnifiedSystemUser")).andExpect(status().isUnauthorized());
    }

    @Test
    public void readUserEditor_labTechnicianIsRefusedAndNothingIsRead() throws Exception {
        mockMvc.perform(get("/rest/UnifiedSystemUser").with(user("qa_labtech").roles("RESULTS")))
                .andExpect(status().isForbidden());
        verify(roleService, never()).getAllActiveRoles();
        verify(systemUserService, never()).getAll();
    }

    @Test
    public void readUserEditor_receptionistIsRefused() throws Exception {
        mockMvc.perform(get("/rest/UnifiedSystemUser").with(user("qa_recept").roles("RECEPTION")))
                .andExpect(status().isForbidden());
        verify(roleService, never()).getAllActiveRoles();
    }

    @Test
    public void readUserEditor_adminReachesTheController() throws Exception {
        // The editor's first step reads date configuration that this slice does not
        // load, so the handler fails once it runs. A refused request would stop at
        // the role check with 403 and never enter the handler; the failure coming
        // from inside showUnifiedSystemUser proves the admin got past the check.
        try {
            int status = mockMvc.perform(get("/rest/UnifiedSystemUser").with(user("admin").roles("ADMIN"))).andReturn()
                    .getResponse().getStatus();
            assertNotEquals("the admin passes the role check", 401, status);
            assertNotEquals("the admin passes the role check", 403, status);
        } catch (ServletException handlerFailure) {
            assertTrue("the admin request ran the editor handler", ranHandler(handlerFailure, "showUnifiedSystemUser"));
        }
    }

    private static boolean ranHandler(Throwable failure, String handlerMethod) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            for (StackTraceElement frame : t.getStackTrace()) {
                if (UnifiedSystemUserRestController.class.getName().equals(frame.getClassName())
                        && handlerMethod.equals(frame.getMethodName())) {
                    return true;
                }
            }
        }
        return false;
    }

    // POST /rest/UnifiedSystemUser: create or edit an account

    @Test
    public void saveUser_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(post("/rest/UnifiedSystemUser").contentType(MediaType.APPLICATION_JSON).content(USER_FORM_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void saveUser_labTechnicianIsRefusedAndNothingIsWritten() throws Exception {
        mockMvc.perform(post("/rest/UnifiedSystemUser").with(user("qa_labtech").roles("RESULTS"))
                .contentType(MediaType.APPLICATION_JSON).content(USER_FORM_JSON)).andExpect(status().isForbidden());
        verify(formValidator, never()).validate(any(), any());
        verifyZeroInteractions(userService, loginUserService);
    }

    @Test
    public void saveUser_receptionistIsRefusedAndNothingIsWritten() throws Exception {
        mockMvc.perform(post("/rest/UnifiedSystemUser").with(user("qa_recept").roles("RECEPTION"))
                .contentType(MediaType.APPLICATION_JSON).content(USER_FORM_JSON)).andExpect(status().isForbidden());
        verify(formValidator, never()).validate(any(), any());
    }

    @Test
    public void saveUser_adminReachesTheController() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT)).when(formValidator).validate(any(), any());
        mockMvc.perform(post("/rest/UnifiedSystemUser").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(USER_FORM_JSON)).andExpect(status().isIAmATeapot());
    }

    // User pickers stay open to signed-in users

    @Test
    public void userPicker_staysOpenToSignedInUsers() throws Exception {
        mockMvc.perform(get("/rest/users").with(user("qa_labtech").roles("RESULTS"))).andExpect(status().isOk());
    }

    @Test
    public void userPickerByRole_staysOpenToSignedInUsers() throws Exception {
        mockMvc.perform(get("/rest/users/Results").with(user("qa_labtech").roles("RESULTS")))
                .andExpect(status().isOk());
    }

    @Test
    public void userPicker_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/rest/users")).andExpect(status().isUnauthorized());
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
        UnifiedSystemUserFormValidator unifiedSystemUserFormValidator() {
            return mock(UnifiedSystemUserFormValidator.class);
        }

        @Bean
        LoginUserService loginUserService() {
            return mock(LoginUserService.class);
        }

        @Bean
        RoleService roleService() {
            return mock(RoleService.class);
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
        UnifiedSystemUserRestController unifiedSystemUserRestController() {
            return new UnifiedSystemUserRestController();
        }
    }
}
