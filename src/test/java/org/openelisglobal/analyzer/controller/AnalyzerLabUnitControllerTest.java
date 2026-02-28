package org.openelisglobal.analyzer.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Controller tests for lab unit endpoints.
 *
 * <p>
 * Test cases: GET list, PUT replace, organizational scope.
 */
public class AnalyzerLabUnitControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;
    private ObjectMapper objectMapper;
    private UserSessionData userSessionData;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        objectMapper = new ObjectMapper();
        userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
        executeDataSetWithStateManagement("testdata/analyzer.xml");
        executeDataSetWithStateManagement("testdata/organization.xml");
        cleanLabUnitData();
        setAuthentication("ROLE_GLOBAL_ADMIN");
    }

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder withSession(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) {
        return builder.sessionAttr("userSessionData", userSessionData);
    }

    private void cleanLabUnitData() {
        jdbcTemplate.execute("DELETE FROM analyzer_lab_unit");
    }

    @Test
    public void testGetLabUnits_Empty_ReturnsEmptyArray() throws Exception {
        mockMvc.perform(
                withSession(get("/rest/analyzer/analyzers/1/lab-units")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void testPutLabUnits_ReplacesAndReturnsSuccess() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        List<String> labUnitIds = Arrays.asList("3", "4");
        request.put("labUnitIds", labUnitIds);

        mockMvc.perform(withSession(put("/rest/analyzer/analyzers/1/lab-units")).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(
                withSession(get("/rest/analyzer/analyzers/1/lab-units")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].labUnitId").value("3")).andExpect(jsonPath("$[1].labUnitId").value("4"));
    }

    @Test
    public void testPutLabUnits_EmptyList_ReplacesWithEmpty() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("labUnitIds", Arrays.asList("3"));

        mockMvc.perform(withSession(put("/rest/analyzer/analyzers/1/lab-units")).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk());

        request.put("labUnitIds", Arrays.asList());
        mockMvc.perform(withSession(put("/rest/analyzer/analyzers/1/lab-units")).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk());

        mockMvc.perform(
                withSession(get("/rest/analyzer/analyzers/1/lab-units")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void testController_HasGlobalAdminPreAuthorizeAnnotation() {
        PreAuthorize annotation = AnalyzerLabUnitController.class.getAnnotation(PreAuthorize.class);
        org.junit.Assert.assertNotNull("Analyzer lab unit controller should define @PreAuthorize", annotation);
        org.junit.Assert.assertEquals("hasRole('GLOBAL_ADMIN')", annotation.value());
    }

    private void setAuthentication(String roleAuthority) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken("test-user", "N/A",
                java.util.List.of(new SimpleGrantedAuthority(roleAuthority)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
