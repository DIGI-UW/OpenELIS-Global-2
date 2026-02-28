package org.openelisglobal.analyzer.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * Controller tests for the profile-apply endpoint.
 */
public class AnalyzerProfileApplyControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;
    private UserSessionData userSessionData;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
        executeDataSetWithStateManagement("testdata/analyzer.xml");
        cleanProfileData();
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

    private void cleanProfileData() {
        jdbcTemplate.execute("DELETE FROM analyzer_profile_application");
        jdbcTemplate.execute("DELETE FROM analyzer_profile");
    }

    @Test
    public void testApplyProfile_CreatesProvenance() throws Exception {
        String profileId = insertTestProfile();
        String analyzerId = "1";

        mockMvc.perform(withSession(post("/rest/analyzer/analyzers/" + analyzerId + "/profile-apply"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"profileId\":\"" + profileId + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM analyzer_profile_application WHERE analyzer_id = ? AND source_profile_id = ?",
                Integer.class, Integer.valueOf(analyzerId), profileId);
        org.junit.Assert.assertEquals("Provenance record should be created", Integer.valueOf(1), count);
    }

    @Test
    public void testApplyProfile_MissingProfileId_Returns400() throws Exception {
        mockMvc.perform(withSession(post("/rest/analyzer/analyzers/1/profile-apply"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
    }

    @Test
    public void testController_HasGlobalAdminPreAuthorizeAnnotation() {
        PreAuthorize annotation = AnalyzerProfileRestController.class.getAnnotation(PreAuthorize.class);
        org.junit.Assert.assertNotNull("Analyzer profile controller should define @PreAuthorize", annotation);
        org.junit.Assert.assertEquals("hasRole('GLOBAL_ADMIN')", annotation.value());
    }

    private String insertTestProfile() {
        String id = java.util.UUID.randomUUID().toString();
        String json = "{\"profileMeta\":{\"id\":\"apply-test\",\"version\":\"1.0.0\",\"displayName\":\"Apply Test\"},\"analyzer_name\":\"Test\"}";
        jdbcTemplate.update(
                "INSERT INTO analyzer_profile (id, profile_meta_id, profile_meta_version, display_name, source, "
                        + "profile_json, checksum_sha256, is_mutable, is_latest, created_by, created_at, updated_at, last_updated) "
                        + "VALUES (?, 'apply-test', '1.0.0', 'Apply Test', 'SITE', ?::jsonb, 'abc123', true, true, 'system', NOW(), NOW(), NOW())",
                id, json);
        return id;
    }

    private void setAuthentication(String roleAuthority) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken("test-user", "N/A",
                java.util.List.of(new SimpleGrantedAuthority(roleAuthority)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
