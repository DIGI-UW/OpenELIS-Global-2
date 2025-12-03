package org.openelisglobal.sitebranding.controller.rest;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.sitebranding.form.SiteBrandingForm;
import org.openelisglobal.sitebranding.service.SiteBrandingService;
import org.openelisglobal.sitebranding.valueholder.SiteBranding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Integration tests for SiteBrandingRestController
 * 
 * Following TDD approach: Write tests BEFORE implementation
 * Tests based on contracts/site-branding-api.json specification
 * 
 * Uses BaseWebContextSensitiveTest (legacy pattern) since project doesn't use
 * Spring Boot. Reference: Testing Roadmap > BaseWebContextSensitiveTest (Legacy
 * Integration)
 * 
 * Task Reference: T008
 */
public class SiteBrandingRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SiteBrandingService siteBrandingService;

    @Autowired
    private DataSource dataSource;

    private ObjectMapper objectMapper;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
        jdbcTemplate = new JdbcTemplate(dataSource);
        cleanTestData();
    }

    @After
    public void tearDown() throws Exception {
        cleanTestData();
    }

    private void cleanTestData() {
        try {
            // Clean up test branding data
            jdbcTemplate.execute("DELETE FROM site_branding WHERE id LIKE 'TEST-%'");
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    /**
     * Test: GET /rest/site-branding/ - returns branding configuration
     * Task Reference: T008
     */
    @Test
    public void testGetBranding_WithAdminRole_ReturnsBranding() throws Exception {
        // Arrange: Create test branding
        SiteBranding branding = new SiteBranding();
        branding.setId("TEST-001");
        branding.setPrimaryColor("#1d4ed8");
        branding.setSecondaryColor("#64748b");
        branding.setAccentColor("#0891b2");
        branding.setColorMode("light");
        branding.setUseHeaderLogoForLogin(false);
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Act: GET /rest/site-branding/
        // Then: Expect 200 OK with branding configuration
        mockMvc.perform(get("/rest/site-branding/").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryColor").value("#1d4ed8"))
                .andExpect(jsonPath("$.secondaryColor").value("#64748b"))
                .andExpect(jsonPath("$.accentColor").value("#0891b2"));
    }

    /**
     * Test: GET /rest/site-branding/ - returns default branding if none exists
     * Task Reference: T008
     */
    @Test
    public void testGetBranding_WhenNoneExists_ReturnsDefaults() throws Exception {
        // Act: GET /rest/site-branding/ when no branding exists
        // Then: Expect 200 OK with default values
        mockMvc.perform(get("/rest/site-branding/").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryColor").value("#1d4ed8"))
                .andExpect(jsonPath("$.secondaryColor").value("#64748b"))
                .andExpect(jsonPath("$.accentColor").value("#0891b2"));
    }

    /**
     * Test: GET /rest/site-branding/ - requires admin role
     * Task Reference: T008
     */
    @Test
    public void testGetBranding_WithNonAdminRole_Returns403() throws Exception {
        // Act: GET /rest/site-branding/ with non-admin role
        // Then: Expect 403 Forbidden
        mockMvc.perform(get("/rest/site-branding/").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    /**
     * Test: PUT /rest/site-branding/ - updates branding configuration
     * Task Reference: T008
     */
    @Test
    public void testUpdateBranding_WithValidData_UpdatesConfiguration() throws Exception {
        // Arrange: Create test branding
        SiteBranding branding = new SiteBranding();
        branding.setPrimaryColor("#1d4ed8");
        branding.setSecondaryColor("#64748b");
        branding.setAccentColor("#0891b2");
        branding.setColorMode("light");
        branding.setUseHeaderLogoForLogin(false);
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Arrange: Update form
        SiteBrandingForm form = new SiteBrandingForm();
        form.setPrimaryColor("#ff0000");
        form.setSecondaryColor("#00ff00");
        form.setAccentColor("#0000ff");
        form.setColorMode("light");
        form.setUseHeaderLogoForLogin(false);

        String requestBody = objectMapper.writeValueAsString(form);

        // Act: PUT /rest/site-branding/
        // Then: Expect 200 OK with updated configuration
        mockMvc.perform(put("/rest/site-branding/").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryColor").value("#ff0000"))
                .andExpect(jsonPath("$.secondaryColor").value("#00ff00"))
                .andExpect(jsonPath("$.accentColor").value("#0000ff"));
    }

    /**
     * Test: PUT /rest/site-branding/ - validates color format
     * Task Reference: T008
     */
    @Test
    public void testUpdateBranding_WithInvalidColor_Returns400() throws Exception {
        // Arrange: Form with invalid color
        SiteBrandingForm form = new SiteBrandingForm();
        form.setPrimaryColor("invalid-color");
        form.setSecondaryColor("#64748b");
        form.setAccentColor("#0891b2");
        form.setColorMode("light");

        String requestBody = objectMapper.writeValueAsString(form);

        // Act: PUT /rest/site-branding/ with invalid color
        // Then: Expect 400 Bad Request
        mockMvc.perform(put("/rest/site-branding/").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test: PUT /rest/site-branding/ - requires admin role
     * Task Reference: T008
     */
    @Test
    public void testUpdateBranding_WithNonAdminRole_Returns403() throws Exception {
        // Arrange: Valid form
        SiteBrandingForm form = new SiteBrandingForm();
        form.setPrimaryColor("#ff0000");
        form.setSecondaryColor("#00ff00");
        form.setAccentColor("#0000ff");
        form.setColorMode("light");

        String requestBody = objectMapper.writeValueAsString(form);

        // Act: PUT /rest/site-branding/ with non-admin role
        // Then: Expect 403 Forbidden
        mockMvc.perform(put("/rest/site-branding/").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isForbidden());
    }

    /**
     * Test: POST /rest/site-branding/logo/header - upload header logo
     * Task Reference: T027
     */
    @Test
    public void testUploadHeaderLogo_WithValidFile_Returns200() throws Exception {
        // Arrange: Create test branding
        SiteBranding branding = new SiteBranding();
        branding.setPrimaryColor("#1d4ed8");
        branding.setSecondaryColor("#64748b");
        branding.setAccentColor("#0891b2");
        branding.setColorMode("light");
        branding.setUseHeaderLogoForLogin(false);
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Arrange: Create mock MultipartFile (valid PNG, < 2MB)
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-header-logo.png",
                "image/png",
                "fake png content".getBytes()
        );

        // Act: POST /rest/site-branding/logo/header with file
        // Then: Expect 200 OK with logo URL
        mockMvc.perform(multipart("/rest/site-branding/logo/header")
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value("/rest/site-branding/logo/header"))
                .andExpect(jsonPath("$.fileName").value("test-header-logo.png"));
    }

    /**
     * Test: POST /rest/site-branding/logo/header - validates file format
     * Task Reference: T027
     */
    @Test
    public void testUploadHeaderLogo_WithInvalidFormat_Returns400() throws Exception {
        // Arrange: Create test branding
        SiteBranding branding = new SiteBranding();
        branding.setPrimaryColor("#1d4ed8");
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Arrange: Create mock MultipartFile with invalid format (TXT instead of image)
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "invalid file content".getBytes()
        );

        // Act: POST /rest/site-branding/logo/header with invalid file
        // Then: Expect 400 Bad Request
        mockMvc.perform(multipart("/rest/site-branding/logo/header")
                .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    /**
     * Test: POST /rest/site-branding/logo/header - validates file size
     * Task Reference: T027
     */
    @Test
    public void testUploadHeaderLogo_WithExcessiveSize_Returns400() throws Exception {
        // Arrange: Create test branding
        SiteBranding branding = new SiteBranding();
        branding.setPrimaryColor("#1d4ed8");
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Arrange: Create mock MultipartFile exceeding 2MB limit
        byte[] largeContent = new byte[3 * 1024 * 1024]; // 3MB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large-image.png",
                "image/png",
                largeContent
        );

        // Act: POST /rest/site-branding/logo/header with oversized file
        // Then: Expect 400 Bad Request
        mockMvc.perform(multipart("/rest/site-branding/logo/header")
                .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    /**
     * Test: GET /rest/site-branding/logo/header - serves logo file
     * Task Reference: T034
     */
    @Test
    public void testGetHeaderLogo_WithExistingLogo_ReturnsFile() throws Exception {
        // Arrange: Create test branding with header logo
        String logoPath = "/var/lib/openelis-global/branding/header-test.png";
        Files.createDirectories(Paths.get("/var/lib/openelis-global/branding/"));
        Files.write(Paths.get(logoPath), "test image content".getBytes());

        SiteBranding branding = new SiteBranding();
        branding.setId("test-id");
        branding.setHeaderLogoPath(logoPath);
        branding.setPrimaryColor("#1d4ed8");
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Act: GET /rest/site-branding/logo/header
        // Then: Expect 200 OK with file content and caching headers
        mockMvc.perform(get("/rest/site-branding/logo/header"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=3600"))
                .andExpect(content().bytes("test image content".getBytes()));

        // Cleanup
        Files.deleteIfExists(Paths.get(logoPath));
    }

    /**
     * Test: POST /rest/site-branding/logo/login - upload login logo
     * Task Reference: T036
     */
    @Test
    public void testUploadLoginLogo_WithValidFile_Returns200() throws Exception {
        // Arrange: Create test branding
        SiteBranding branding = new SiteBranding();
        branding.setPrimaryColor("#1d4ed8");
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Arrange: Create mock MultipartFile (valid PNG, < 2MB)
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-login-logo.png",
                "image/png",
                "fake png content".getBytes()
        );

        // Act: POST /rest/site-branding/logo/login with file
        // Then: Expect 200 OK with logo URL
        mockMvc.perform(multipart("/rest/site-branding/logo/login")
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value("/rest/site-branding/logo/login"))
                .andExpect(jsonPath("$.fileName").value("test-login-logo.png"));
    }

    /**
     * Test: GET /rest/site-branding/logo/login - serves login logo file
     * Task Reference: T036
     */
    @Test
    public void testGetLoginLogo_WithExistingLogo_ReturnsFile() throws Exception {
        // Arrange: Create test branding with login logo
        String logoPath = "/var/lib/openelis-global/branding/login-test.png";
        Files.createDirectories(Paths.get("/var/lib/openelis-global/branding/"));
        Files.write(Paths.get(logoPath), "test login image content".getBytes());

        SiteBranding branding = new SiteBranding();
        branding.setId("test-id");
        branding.setLoginLogoPath(logoPath);
        branding.setPrimaryColor("#1d4ed8");
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Act: GET /rest/site-branding/logo/login
        // Then: Expect 200 OK with file content
        mockMvc.perform(get("/rest/site-branding/logo/login"))
                .andExpect(status().isOk())
                .andExpect(content().bytes("test login image content".getBytes()));

        // Cleanup
        Files.deleteIfExists(Paths.get(logoPath));
    }

    /**
     * Test: POST /rest/site-branding/logo/favicon - upload favicon
     * Task Reference: T042
     */
    @Test
    public void testUploadFavicon_WithValidFile_Returns200() throws Exception {
        // Arrange: Create test branding
        SiteBranding branding = new SiteBranding();
        branding.setPrimaryColor("#1d4ed8");
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Arrange: Create mock MultipartFile (valid ICO, < 2MB)
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-favicon.ico",
                "image/x-icon",
                "fake ico content".getBytes()
        );

        // Act: POST /rest/site-branding/logo/favicon with file
        // Then: Expect 200 OK with logo URL
        mockMvc.perform(multipart("/rest/site-branding/logo/favicon")
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value("/rest/site-branding/logo/favicon"))
                .andExpect(jsonPath("$.fileName").value("test-favicon.ico"));
    }

    /**
     * Test: GET /rest/site-branding/logo/favicon - serves favicon file
     * Task Reference: T042
     */
    @Test
    public void testGetFavicon_WithExistingFavicon_ReturnsFile() throws Exception {
        // Arrange: Create test branding with favicon
        String faviconPath = "/var/lib/openelis-global/branding/favicon-test.ico";
        Files.createDirectories(Paths.get("/var/lib/openelis-global/branding/"));
        Files.write(Paths.get(faviconPath), "test favicon content".getBytes());

        SiteBranding branding = new SiteBranding();
        branding.setId("test-id");
        branding.setFaviconPath(faviconPath);
        branding.setPrimaryColor("#1d4ed8");
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Act: GET /rest/site-branding/logo/favicon
        // Then: Expect 200 OK with file content
        mockMvc.perform(get("/rest/site-branding/logo/favicon"))
                .andExpect(status().isOk())
                .andExpect(content().bytes("test favicon content".getBytes()));

        // Cleanup
        Files.deleteIfExists(Paths.get(faviconPath));
    }

    /**
     * Test: DELETE /rest/site-branding/logo/{type} - remove logo
     * Task Reference: T060
     */
    @Test
    public void testDeleteLogo_WithExistingLogo_Returns200() throws Exception {
        // Arrange: Create branding with header logo file
        String logoPath = "/var/lib/openelis-global/branding/header-1234567890.png";
        Files.createDirectories(Paths.get("/var/lib/openelis-global/branding/"));
        Files.write(Paths.get(logoPath), "test logo content".getBytes());

        SiteBranding branding = new SiteBranding();
        branding.setHeaderLogoPath(logoPath);
        branding.setPrimaryColor("#1d4ed8");
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Act: DELETE /rest/site-branding/logo/header
        // Then: Expect 200 OK, logo path set to null
        mockMvc.perform(delete("/rest/site-branding/logo/header"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerLogoUrl").isEmpty());

        // Verify file was deleted
        assertFalse("Logo file should be deleted", Files.exists(Paths.get(logoPath)));
    }

    /**
     * Test: DELETE /rest/site-branding/logo/{type} - validates logo type
     * Task Reference: T060
     */
    @Test
    public void testDeleteLogo_WithInvalidType_Returns400() throws Exception {
        // Arrange: Create test branding
        SiteBranding branding = new SiteBranding();
        branding.setPrimaryColor("#1d4ed8");
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Act: DELETE /rest/site-branding/logo/invalid
        // Then: Expect 400 Bad Request
        mockMvc.perform(delete("/rest/site-branding/logo/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    /**
     * Test: POST /rest/site-branding/reset - reset all branding to defaults
     * Task Reference: T065
     */
    @Test
    public void testResetBranding_ResetsAllToDefaults() throws Exception {
        // Arrange: Create branding with custom values and logo files
        String headerPath = "/var/lib/openelis-global/branding/header-123.png";
        String loginPath = "/var/lib/openelis-global/branding/login-123.png";
        String faviconPath = "/var/lib/openelis-global/branding/favicon-123.ico";
        Files.createDirectories(Paths.get("/var/lib/openelis-global/branding/"));
        Files.write(Paths.get(headerPath), "header content".getBytes());
        Files.write(Paths.get(loginPath), "login content".getBytes());
        Files.write(Paths.get(faviconPath), "favicon content".getBytes());

        SiteBranding branding = new SiteBranding();
        branding.setHeaderLogoPath(headerPath);
        branding.setLoginLogoPath(loginPath);
        branding.setFaviconPath(faviconPath);
        branding.setPrimaryColor("#ff0000");
        branding.setSecondaryColor("#00ff00");
        branding.setAccentColor("#0000ff");
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Act: POST /rest/site-branding/reset
        // Then: Expect 200 OK, all logo paths set to null, colors reset to defaults
        mockMvc.perform(post("/rest/site-branding/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerLogoUrl").isEmpty())
                .andExpect(jsonPath("$.loginLogoUrl").isEmpty())
                .andExpect(jsonPath("$.faviconUrl").isEmpty())
                .andExpect(jsonPath("$.primaryColor").value("#1d4ed8")) // Default primary color
                .andExpect(jsonPath("$.secondaryColor").value("#64748b")) // Default secondary color
                .andExpect(jsonPath("$.accentColor").value("#0891b2")); // Default accent color

        // Verify files were deleted
        assertFalse("Header logo file should be deleted", Files.exists(Paths.get(headerPath)));
        assertFalse("Login logo file should be deleted", Files.exists(Paths.get(loginPath)));
        assertFalse("Favicon file should be deleted", Files.exists(Paths.get(faviconPath)));
    }
}

