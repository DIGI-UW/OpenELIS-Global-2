package org.openelisglobal.analyzer.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import org.springframework.test.web.servlet.MvcResult;

/**
 * Controller tests for AnalyzerProfileRestController.
 *
 * <p>
 * Test cases: list by source, get by id, import (201 + 409 duplicate), update
 * latest designation, delete (204 + 403 built-in).
 */
public class AnalyzerProfileRestControllerTest extends BaseWebContextSensitiveTest {

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
        cleanProfileTestData();
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

    private void cleanProfileTestData() {
        jdbcTemplate.execute("DELETE FROM analyzer_profile_application");
        jdbcTemplate.execute("DELETE FROM analyzer_profile");
    }

    @Test
    public void testListProfiles_NoFilter_ReturnsProfiles() throws Exception {
        // Arrange: Insert a profile via import
        String profileId = importValidProfile();

        // Act & Assert
        mockMvc.perform(withSession(get("/rest/analyzer/profiles")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == '" + profileId + "')]").exists());
    }

    @Test
    public void testListProfiles_BySource_ReturnsFiltered() throws Exception {
        importValidProfile();

        mockMvc.perform(withSession(get("/rest/analyzer/profiles").param("source", "SITE"))
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testGetProfile_ValidId_ReturnsProfileWithJson() throws Exception {
        String profileId = importValidProfile();

        mockMvc.perform(
                withSession(get("/rest/analyzer/profiles/" + profileId)).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(profileId))
                .andExpect(jsonPath("$.profileMetaId").value("test-profile"))
                .andExpect(jsonPath("$.profileJson").exists());
    }

    @Test
    public void testGetProfile_InvalidId_Returns404() throws Exception {
        mockMvc.perform(
                withSession(get("/rest/analyzer/profiles/nonexistent-uuid")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testImportProfile_ValidPayload_Returns201() throws Exception {
        String requestBody = "{\"profile\":{\"profileMeta\":{\"id\":\"import-test\",\"version\":\"1.0.0\",\"displayName\":\"Imported Profile\"},\"analyzer_name\":\"Test Analyzer\"}}";

        mockMvc.perform(withSession(post("/rest/analyzer/profiles")).contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)).andExpect(status().isCreated()).andExpect(jsonPath("$.id").exists());
    }

    @Test
    public void testImportProfile_DuplicateVersion_Returns409() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        Map<String, Object> profile = new LinkedHashMap<>();
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("id", "dup-test");
        meta.put("version", "1.0.0");
        meta.put("displayName", "Duplicate Test");
        profile.put("profileMeta", meta);
        profile.put("analyzer_name", "Test");
        request.put("profile", profile);

        mockMvc.perform(withSession(post("/rest/analyzer/profiles")).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated());

        mockMvc.perform(withSession(post("/rest/analyzer/profiles")).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isConflict());
    }

    @Test
    public void testUpdateProfile_IsLatest_Returns200() throws Exception {
        String profileId = importValidProfile();

        Map<String, Object> request = new HashMap<>();
        request.put("isLatest", true);

        mockMvc.perform(withSession(put("/rest/analyzer/profiles/" + profileId)).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
                .andExpect(jsonPath("$.isLatest").value(true));
    }

    @Test
    public void testDeleteProfile_SiteProfile_Returns204() throws Exception {
        String profileId = importValidProfile();

        mockMvc.perform(withSession(delete("/rest/analyzer/profiles/" + profileId))).andExpect(status().isNoContent());
    }

    @Test
    public void testDeleteProfile_BuiltInProfile_Returns403() throws Exception {
        String profileId = insertBuiltInProfile();

        mockMvc.perform(withSession(delete("/rest/analyzer/profiles/" + profileId))).andExpect(status().isForbidden());
    }

    @Test
    public void testController_HasGlobalAdminPreAuthorizeAnnotation() {
        PreAuthorize annotation = AnalyzerProfileRestController.class.getAnnotation(PreAuthorize.class);
        org.junit.Assert.assertNotNull("Analyzer profile controller should define @PreAuthorize", annotation);
        org.junit.Assert.assertEquals("hasRole('GLOBAL_ADMIN')", annotation.value());
    }

    private String importValidProfile() throws Exception {
        String requestBody = "{\"profile\":{\"profileMeta\":{\"id\":\"test-profile\",\"version\":\"1.0.0\",\"displayName\":\"Test Profile\"},\"analyzer_name\":\"Test Analyzer\"}}";

        MvcResult result = mockMvc.perform(withSession(post("/rest/analyzer/profiles"))
                .contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isCreated())
                .andReturn();

        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return (String) response.get("id");
    }

    private String insertBuiltInProfile() {
        String id = java.util.UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO analyzer_profile (id, profile_meta_id, profile_meta_version, display_name, source, "
                        + "profile_json, checksum_sha256, is_mutable, is_latest, created_by, created_at, updated_at, last_updated) "
                        + "VALUES (?, 'builtin-delete-test', '1.0.0', 'Built-in Test', 'BUILT_IN', '{}'::jsonb, 'abc', false, true, 'system', NOW(), NOW(), NOW())",
                id);
        return id;
    }

    private void setAuthentication(String roleAuthority) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken("test-user", "N/A",
                java.util.List.of(new SimpleGrantedAuthority(roleAuthority)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
