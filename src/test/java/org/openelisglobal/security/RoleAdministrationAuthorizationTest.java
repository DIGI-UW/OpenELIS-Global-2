package org.openelisglobal.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.privilege.service.PrivilegeService;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * The role editor's endpoints are authorization-sensitive: a caller who can
 * reach them can grant any privilege to any role and therefore to themselves.
 * These tests pin who reaches them.
 *
 * <p>
 * Gating lives on the service interfaces, not the controller (S011c), so the
 * services here are stubbed: what is under test is that the endpoints exist,
 * bind their payloads, and are reachable only through an authenticated session.
 * That the underlying services carry PRIV_ROLE_MANAGE / PRIV_ROLE_VIEW is
 * asserted by ServicePrivilegeCoverageTest and by the annotations themselves.
 */
@WebAppConfiguration
@ContextConfiguration(classes = { RoleAdministrationAuthorizationTest.TestConfig.class })
@TestPropertySource("classpath:common.properties")
public class RoleAdministrationAuthorizationTest extends SecuritySliceMockMvcTest {

    @Autowired
    private RoleService roleService;

    @Autowired
    private PrivilegeService privilegeService;

    private static final String ADMIN_AUTHORITIES = "PRIV_ROLE_MANAGE";

    @org.junit.Before
    public void resetSharedServiceMocks() {
        org.mockito.Mockito.reset(mockBehind(roleService), mockBehind(privilegeService));
    }

    /**
     * createRole resolves the acting user from the OE session before persisting, so
     * a request without one fails in the actor lookup rather than in the endpoint.
     */
    private static org.openelisglobal.login.valueholder.UserSessionData actorSession() {
        org.openelisglobal.login.valueholder.UserSessionData session = new org.openelisglobal.login.valueholder.UserSessionData();
        session.setSytemUserId(42);
        return session;
    }

    @Test
    public void listRoles_withoutAuthentication_isRefused() throws Exception {
        mockMvc.perform(get("/rest/roles").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void privilegeCatalogue_withoutAuthentication_isRefused() throws Exception {
        mockMvc.perform(get("/rest/roles/privileges").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void createRole_withoutAuthentication_isRefused() throws Exception {
        mockMvc.perform(post("/rest/roles").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"X\",\"groupingParentName\":\"Global Roles\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void replacePrivileges_withoutAuthentication_isRefused() throws Exception {
        mockMvc.perform(put("/rest/roles/5/privileges").contentType(MediaType.APPLICATION_JSON)
                .content("{\"privilegeIds\":[1,2]}")).andExpect(status().isUnauthorized());
    }

    @Test
    public void createRole_bindsEveryFieldTheEditorSends() throws Exception {
        Role created = new Role();
        created.setId(90);
        created.setName("Reception Plus");
        when(mockBehind(roleService).createAssignableRole(anyString(), any(), any(), anyString(), any(), any()))
                .thenReturn(created);

        mockMvc.perform(post("/rest/roles")
                .with(user("admin").authorities(AuthorityUtils.createAuthorityList(ADMIN_AUTHORITIES)))
                .sessionAttr(org.openelisglobal.common.action.IActionConstants.USER_SESSION_DATA, actorSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Reception Plus\",\"description\":\"d\",\"groupingParentName\":\"Lab Unit Roles\","
                        + "\"parentRoleName\":\"Reception\",\"privilegeIds\":[]}"))
                .andExpect(status().isCreated());

        org.mockito.ArgumentCaptor<String> name = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<String> container = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<String> parent = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(mockBehind(roleService)).createAssignableRole(name.capture(), any(), any(),
                container.capture(), parent.capture(), any());
        assertEquals("Reception Plus", name.getValue());
        // The two parents are distinct concepts and must not be conflated.
        assertEquals("Lab Unit Roles", container.getValue());
        assertEquals("Reception", parent.getValue());
    }

    @Test
    public void replacePrivileges_passesTheIdsThrough() throws Exception {
        when(mockBehind(privilegeService).replaceDirectPrivilegesForRole(anyString(), any())).thenReturn(List.of());

        mockMvc.perform(put("/rest/roles/5/privileges")
                .with(user("admin").authorities(AuthorityUtils.createAuthorityList(ADMIN_AUTHORITIES)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"privilegeIds\":[3,7]}"))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<java.util.Collection<Integer>> ids = org.mockito.ArgumentCaptor
                .forClass(java.util.Collection.class);
        org.mockito.Mockito.verify(mockBehind(privilegeService)).replaceDirectPrivilegesForRole(
                org.mockito.ArgumentMatchers.eq("5"), ids.capture());
        assertTrue(ids.getValue().containsAll(List.of(3, 7)));
        assertEquals(2, ids.getValue().size());
    }

    /** Clearing every box must send an empty list, not skip the call. */
    @Test
    public void replacePrivileges_withAnEmptyListRevokesEverything() throws Exception {
        when(mockBehind(privilegeService).replaceDirectPrivilegesForRole(anyString(), any())).thenReturn(List.of());

        mockMvc.perform(put("/rest/roles/5/privileges")
                .with(user("admin").authorities(AuthorityUtils.createAuthorityList(ADMIN_AUTHORITIES)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"privilegeIds\":[]}"))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<java.util.Collection<Integer>> ids = org.mockito.ArgumentCaptor
                .forClass(java.util.Collection.class);
        org.mockito.Mockito.verify(mockBehind(privilegeService)).replaceDirectPrivilegesForRole(
                org.mockito.ArgumentMatchers.eq("5"), ids.capture());
        assertTrue(ids.getValue().isEmpty());
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
        RoleService roleService() {
            return GatedServiceMocks.stubbableMock(RoleService.class);
        }

        @Bean
        PrivilegeService privilegeService() {
            return GatedServiceMocks.stubbableMock(PrivilegeService.class);
        }

        @Bean
        org.openelisglobal.role.controller.rest.RoleRestController roleRestController() {
            return new org.openelisglobal.role.controller.rest.RoleRestController();
        }

        @Bean
        org.openelisglobal.login.dao.UserModuleService userModuleService() {
            return nullStub(org.openelisglobal.login.dao.UserModuleService.class);
        }

        @Bean
        org.openelisglobal.view.PageBuilderService pageBuilderService() {
            return nullStub(org.openelisglobal.view.PageBuilderService.class);
        }
    }
}
