package org.openelisglobal.systemuser.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * "All Lab Units" is exclusive with lab-unit-specific grants (OGC-1231). A
 * submission that holds both used to be written and then normalized on read,
 * which silently dropped the scoped grants while reporting success.
 */
public class UnifiedSystemUserLabUnitRolesRestControllerTest extends BaseWebContextSensitiveTest {

    private static final String HEMATOLOGY = "36";
    private static final String BIOCHEMISTRY = "56";
    private static final String RECEPTION = "4";
    private static final String VALIDATION = "10";

    @Autowired
    private UserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockHttpSession session;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/unified-system-user-lab-units.xml");
        session = buildAdminSession();
    }

    @Test
    public void save_shouldRefuseAllLabUnitsCombinedWithSpecificLabUnits_andWriteNothing() throws Exception {
        Map<String, Object> form = readForm("3-6");
        form.put("selectedTestSectionLabUnits", Map.of(HEMATOLOGY, List.of(VALIDATION),
                UnifiedSystemUserRestController.ALL_LAB_UNITS, List.of(RECEPTION)));

        mockMvc.perform(post("/rest/UnifiedSystemUser").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(UnifiedSystemUserRestController.ERROR_ALL_LAB_UNITS_EXCLUSIVE));

        assertNull(userService.getUserLabUnitRoles("3"));
    }

    @Test
    public void save_shouldStoreAllLabUnitsAlone() throws Exception {
        Map<String, Object> form = readForm("4-7");
        form.put("selectedTestSectionLabUnits",
                Map.of(UnifiedSystemUserRestController.ALL_LAB_UNITS, List.of(RECEPTION)));

        mockMvc.perform(post("/rest/UnifiedSystemUser").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form))).andExpect(status().isOk())
                .andExpect(jsonPath("$.forward").value("redirect:/UnifiedSystemUser"));

        assertEquals(Set.of(UnifiedSystemUserRestController.ALL_LAB_UNITS), storedLabUnits("4"));
    }

    @Test
    public void save_shouldStoreSeveralSpecificLabUnitsTogether() throws Exception {
        Map<String, Object> form = readForm("5-8");
        form.put("selectedTestSectionLabUnits",
                Map.of(HEMATOLOGY, List.of(VALIDATION), BIOCHEMISTRY, List.of(RECEPTION)));

        mockMvc.perform(post("/rest/UnifiedSystemUser").session(session).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form))).andExpect(status().isOk())
                .andExpect(jsonPath("$.forward").value("redirect:/UnifiedSystemUser"));

        assertEquals(Set.of(HEMATOLOGY, BIOCHEMISTRY), storedLabUnits("5"));
    }

    private Map<String, Object> readForm(String combinedId) throws Exception {
        String json = mockMvc
                .perform(get("/rest/UnifiedSystemUser").param("ID", combinedId).session(session)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Map<String, Object> form = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });
        form.keySet().removeIf(key -> List.of("globalRoles", "labUnitRoles", "testSections").contains(key));
        return form;
    }

    private Set<String> storedLabUnits(String systemUserId) {
        UserLabUnitRoles roles = userService.getUserLabUnitRoles(systemUserId);
        return roles.getLabUnitRoleMap().stream().map(map -> map.getLabUnit()).collect(Collectors.toSet());
    }

    private MockHttpSession buildAdminSession() {
        UserDetails userDetails = User.withUsername("admin").password("N/A").authorities("ROLE_ADMIN").build();
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "N/A", userDetails.getAuthorities()));

        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(1);
        sessionData.setAdmin(true);

        MockHttpSession httpSession = new MockHttpSession();
        httpSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        httpSession.setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return httpSession;
    }
}
