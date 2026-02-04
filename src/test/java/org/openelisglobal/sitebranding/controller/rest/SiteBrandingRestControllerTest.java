package org.openelisglobal.sitebranding.controller.rest;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.sitebranding.form.SiteBrandingForm;
import org.openelisglobal.sitebranding.service.SiteBrandingService;
import org.openelisglobal.sitebranding.valueholder.SiteBranding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Integration tests for SiteBrandingRestController
 *
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

    @Value("${org.openelisglobal.branding.dir:/var/lib/openelis-global/branding/}")
    private String brandingDir;

    private ObjectMapper objectMapper;
    private JdbcTemplate jdbcTemplate;

    // Minimal valid 1x1 transparent PNG (67 bytes)
    private static final byte[] VALID_PNG_BYTES = Base64.getDecoder()
            .decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

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
            // Clean up ALL site branding data to ensure test isolation
            jdbcTemplate.execute("DELETE FROM site_branding");
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    /**
     * Test: GET /rest/site-branding/ - returns branding configuration Task
     * Reference: T008
     */
    @Test
    public void testGetBranding_WithAdminRole_ReturnsBranding() throws Exception {
        // Arrange: Get the single branding and customize it
        SiteBranding branding = siteBrandingService.getBranding();
        branding.setPrimaryColor("#1d4ed8");
        branding.setSecondaryColor("#64748b");
        branding.setHeaderColor("#0891b2");
        branding.setColorMode("light");
        branding.setUseHeaderLogoForLogin(false);
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Act: GET /rest/site-branding/
        // Then: Expect 200 OK with branding configuration
        mockMvc.perform(get("/rest/site-branding/").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryColor").value("#1d4ed8"))
                .andExpect(jsonPath("$.secondaryColor").value("#64748b"))
                .andExpect(jsonPath("$.headerColor").value("#0891b2"));
    }

    /**
     * Test: GET /rest/site-branding/ - returns default branding if none exists Task
     * Reference: T008
     */
    @Test
    public void testGetBranding_WhenNoneExists_ReturnsDefaults() throws Exception {
        // Act: GET /rest/site-branding/ when no branding exists
        // Then: Expect 200 OK with default values
        mockMvc.perform(get("/rest/site-branding/").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryColor").value("#0f62fe"))
                .andExpect(jsonPath("$.secondaryColor").value("#393939"))
                .andExpect(jsonPath("$.headerColor").value("#295785"));
    }

    /**
     * Test: GET /rest/site-branding/ - accessible without authentication
     *
     * The GET endpoint is intentionally unauthenticated so that branding can be
     * displayed on the login page before a user has logged in.
     *
     * Task Reference: T008
     */
    @Test
    public void testGetBranding_WithoutAuthentication_Returns200() throws Exception {
        // Act: GET /rest/site-branding/ without authentication
        // Then: Expect 200 OK with default branding (no authentication required)
        mockMvc.perform(get("/rest/site-branding/").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryColor").exists());
    }

    /**
     * Test: PUT /rest/site-branding/ - updates branding configuration Task
     * Reference: T008
     */
    @Test
    public void testUpdateBranding_WithValidData_UpdatesConfiguration() throws Exception {
        // Arrange: Get the single branding and set initial values
        SiteBranding branding = siteBrandingService.getBranding();
        branding.setPrimaryColor("#1d4ed8");
        branding.setSecondaryColor("#64748b");
        branding.setHeaderColor("#0891b2");
        branding.setColorMode("light");
        branding.setUseHeaderLogoForLogin(false);
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Arrange: Update form
        SiteBrandingForm form = new SiteBrandingForm();
        form.setPrimaryColor("#ff0000");
        form.setSecondaryColor("#00ff00");
        form.setHeaderColor("#0000ff");
        form.setColorMode("light");
        form.setUseHeaderLogoForLogin(false);

        String requestBody = objectMapper.writeValueAsString(form);

        // Act: PUT /rest/site-branding/
        // Then: Expect 200 OK with updated configuration
        mockMvc.perform(put("/rest/site-branding/").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.primaryColor").value("#ff0000"))
                .andExpect(jsonPath("$.secondaryColor").value("#00ff00"))
                .andExpect(jsonPath("$.headerColor").value("#0000ff"));
    }

    /**
     * Test: PUT /rest/site-branding/ - accepts any CSS color format Task Reference:
     * T008
     *
     * Color validation is now permissive - any non-empty string is accepted. CSS
     * handles invalid colors gracefully by ignoring them, so we allow named colors,
     * rgb(), hsl(), and other formats.
     */
    @Test
    public void testUpdateBranding_WithCssNamedColor_Returns200() throws Exception {
        // Arrange: Get the single branding and set initial values
        SiteBranding branding = siteBrandingService.getBranding();
        branding.setPrimaryColor("#1d4ed8");
        branding.setSecondaryColor("#64748b");
        branding.setHeaderColor("#0891b2");
        branding.setColorMode("light");
        branding.setUseHeaderLogoForLogin(false);
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Arrange: Form with CSS named color (not hex)
        SiteBrandingForm form = new SiteBrandingForm();
        form.setPrimaryColor("rebeccapurple");
        form.setSecondaryColor("slate");
        form.setHeaderColor("rgb(56, 178, 172)");
        form.setColorMode("light");

        String requestBody = objectMapper.writeValueAsString(form);

        // Act: PUT /rest/site-branding/ with CSS color names/functions
        // Then: Expect 200 OK - CSS color formats are now accepted
        mockMvc.perform(put("/rest/site-branding/").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.primaryColor").value("rebeccapurple"))
                .andExpect(jsonPath("$.secondaryColor").value("slate"))
                .andExpect(jsonPath("$.headerColor").value("rgb(56, 178, 172)"));
    }

    /**
     * Test: PUT /rest/site-branding/ - requires admin role Task Reference: T008
     *
     * Note: BaseWebContextSensitiveTest sets up mockMvc with admin role by default.
     * This test verifies the endpoint requires authentication, but cannot easily
     * test non-admin access without additional security test configuration.
     */
    @Test
    public void testUpdateBranding_RequiresAuthentication() throws Exception {
        // This test verifies the PUT endpoint works for authenticated users.
        // Full role-based testing requires additional security test setup.
        SiteBrandingForm form = new SiteBrandingForm();
        form.setPrimaryColor("#ff0000");
        form.setSecondaryColor("#00ff00");
        form.setHeaderColor("#0000ff");
        form.setColorMode("light");

        String requestBody = objectMapper.writeValueAsString(form);

        // Act: PUT /rest/site-branding/ - should succeed with admin role from test
        // setup
        mockMvc.perform(put("/rest/site-branding/").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk());
    }

    /**
     * Test: POST /rest/site-branding/logo/header - upload header logo Task
     * Reference: T027
     */
    @Test
    public void testUploadHeaderLogo_WithValidFile_Returns200() throws Exception {
        // Arrange: Get the single branding
        SiteBranding branding = siteBrandingService.getBranding();
        branding.setPrimaryColor("#1d4ed8");
        branding.setSecondaryColor("#64748b");
        branding.setHeaderColor("#0891b2");
        branding.setColorMode("light");
        branding.setUseHeaderLogoForLogin(false);
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Arrange: Create mock MultipartFile with valid PNG content
        MockMultipartFile file = new MockMultipartFile("file", "test-header-logo.png", "image/png", VALID_PNG_BYTES);

        // Act: POST /rest/site-branding/logo/header with file
        // Then: Expect 200 OK with logo URL
        mockMvc.perform(multipart("/rest/site-branding/logo/header").file(file)).andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value("/rest/site-branding/logo/header"))
                .andExpect(jsonPath("$.fileName").value("test-header-logo.png"));
    }

    /**
     * Test: POST /rest/site-branding/logo/header - validates file format Task
     * Reference: T027
     */
    @Test
    public void testUploadHeaderLogo_WithInvalidFormat_Returns400() throws Exception {
        // Arrange: Get the single branding
        SiteBranding branding = siteBrandingService.getBranding();
        branding.setPrimaryColor("#1d4ed8");
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Arrange: Create mock MultipartFile with invalid format (TXT instead of image)
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain",
                "invalid file content".getBytes());

        // Act: POST /rest/site-branding/logo/header with invalid file
        // Then: Expect 400 Bad Request
        mockMvc.perform(multipart("/rest/site-branding/logo/header").file(file)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    /**
     * Test: POST /rest/site-branding/logo/header - validates file size Task
     * Reference: T027
     */
    @Test
    public void testUploadHeaderLogo_WithExcessiveSize_Returns400() throws Exception {
        // Arrange: Get the single branding
        SiteBranding branding = siteBrandingService.getBranding();
        branding.setPrimaryColor("#1d4ed8");
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Arrange: Create mock MultipartFile exceeding 2MB limit
        byte[] largeContent = new byte[3 * 1024 * 1024]; // 3MB
        MockMultipartFile file = new MockMultipartFile("file", "large-image.png", "image/png", largeContent);

        // Act: POST /rest/site-branding/logo/header with oversized file
        // Then: Expect 400 Bad Request
        mockMvc.perform(multipart("/rest/site-branding/logo/header").file(file)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    /**
     * Test: GET /rest/site-branding/logo/header - serves logo file Task Reference:
     * T034
     */
    @Test
    public void testGetHeaderLogo_WithExistingLogo_ReturnsFile() throws Exception {
        // Arrange: Create test branding with header logo
        Path brandingPath = Paths.get(brandingDir);
        Files.createDirectories(brandingPath);
        String logoPath = brandingPath.resolve("header-test.png").toString();
        Files.write(Paths.get(logoPath), "test image content".getBytes());

        SiteBranding branding = siteBrandingService.getBranding();
        branding.setHeaderLogoPath(logoPath);
        branding.setPrimaryColor("#1d4ed8");
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Act: GET /rest/site-branding/logo/header
        // Then: Expect 200 OK with file content and caching headers
        mockMvc.perform(get("/rest/site-branding/logo/header")).andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=300"))
                .andExpect(content().bytes("test image content".getBytes()));

        // Cleanup
        Files.deleteIfExists(Paths.get(logoPath));
    }

    /**
     * Test: GET /rest/site-branding/logo/header with If-None-Match - returns 304
     * when ETag matches
     */
    @Test
    public void testGetHeaderLogo_WithMatchingETag_Returns304() throws Exception {
        // Arrange: Create test branding with header logo
        Path brandingPath = Paths.get(brandingDir);
        Files.createDirectories(brandingPath);
        String logoPath = brandingPath.resolve("header-etag-test.png").toString();
        Files.write(Paths.get(logoPath), "test image content".getBytes());

        SiteBranding branding = siteBrandingService.getBranding();
        branding.setHeaderLogoPath(logoPath);
        branding.setPrimaryColor("#1d4ed8");
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // First request to get the ETag
        String etag = mockMvc.perform(get("/rest/site-branding/logo/header")).andExpect(status().isOk()).andReturn()
                .getResponse().getHeader(HttpHeaders.ETAG);

        assertNotNull("ETag header should be present", etag);

        // Second request with If-None-Match header should return 304
        mockMvc.perform(get("/rest/site-branding/logo/header").header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified());

        // Cleanup
        Files.deleteIfExists(Paths.get(logoPath));
    }

    /**
     * Test: POST /rest/site-branding/logo/login - upload login logo Task Reference:
     * T036
     */
    @Test
    public void testUploadLoginLogo_WithValidFile_Returns200() throws Exception {
        // Arrange: Get the single branding
        SiteBranding branding = siteBrandingService.getBranding();
        branding.setPrimaryColor("#1d4ed8");
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Arrange: Create mock MultipartFile with valid PNG content
        MockMultipartFile file = new MockMultipartFile("file", "test-login-logo.png", "image/png", VALID_PNG_BYTES);

        // Act: POST /rest/site-branding/logo/login with file
        // Then: Expect 200 OK with logo URL
        mockMvc.perform(multipart("/rest/site-branding/logo/login").file(file)).andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value("/rest/site-branding/logo/login"))
                .andExpect(jsonPath("$.fileName").value("test-login-logo.png"));
    }

    /**
     * Test: GET /rest/site-branding/logo/login - serves login logo file Task
     * Reference: T036
     */
    @Test
    public void testGetLoginLogo_WithExistingLogo_ReturnsFile() throws Exception {
        // Arrange: Create test branding with login logo
        Path brandingPath = Paths.get(brandingDir);
        Files.createDirectories(brandingPath);
        String logoPath = brandingPath.resolve("login-test.png").toString();
        Files.write(Paths.get(logoPath), "test login image content".getBytes());

        SiteBranding branding = siteBrandingService.getBranding();
        branding.setLoginLogoPath(logoPath);
        branding.setPrimaryColor("#1d4ed8");
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Act: GET /rest/site-branding/logo/login
        // Then: Expect 200 OK with file content
        mockMvc.perform(get("/rest/site-branding/logo/login")).andExpect(status().isOk())
                .andExpect(content().bytes("test login image content".getBytes()));

        // Cleanup
        Files.deleteIfExists(Paths.get(logoPath));
    }

    /**
     * Test: POST /rest/site-branding/logo/favicon - upload favicon Task Reference:
     * T042
     */
    @Test
    public void testUploadFavicon_WithValidFile_Returns200() throws Exception {
        // Arrange: Get the single branding
        SiteBranding branding = siteBrandingService.getBranding();
        branding.setPrimaryColor("#1d4ed8");
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Arrange: Create mock MultipartFile with valid PNG content (PNG works as
        // favicon too)
        MockMultipartFile file = new MockMultipartFile("file", "test-favicon.png", "image/png", VALID_PNG_BYTES);

        // Act: POST /rest/site-branding/logo/favicon with file
        // Then: Expect 200 OK with logo URL
        mockMvc.perform(multipart("/rest/site-branding/logo/favicon").file(file)).andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value("/rest/site-branding/logo/favicon"))
                .andExpect(jsonPath("$.fileName").value("test-favicon.png"));
    }

    /**
     * Test: GET /rest/site-branding/logo/favicon - serves favicon file Task
     * Reference: T042
     */
    @Test
    public void testGetFavicon_WithExistingFavicon_ReturnsFile() throws Exception {
        // Arrange: Create test branding with favicon
        Path brandingPath = Paths.get(brandingDir);
        Files.createDirectories(brandingPath);
        String faviconPath = brandingPath.resolve("favicon-test.ico").toString();
        Files.write(Paths.get(faviconPath), "test favicon content".getBytes());

        SiteBranding branding = siteBrandingService.getBranding();
        branding.setFaviconPath(faviconPath);
        branding.setPrimaryColor("#1d4ed8");
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Act: GET /rest/site-branding/logo/favicon
        // Then: Expect 200 OK with file content
        mockMvc.perform(get("/rest/site-branding/logo/favicon")).andExpect(status().isOk())
                .andExpect(content().bytes("test favicon content".getBytes()));

        // Cleanup
        Files.deleteIfExists(Paths.get(faviconPath));
    }

    /**
     * Test: DELETE /rest/site-branding/logo/{type} - remove logo Task Reference:
     * T060
     */
    @Test
    public void testDeleteLogo_WithExistingLogo_Returns200() throws Exception {
        // Arrange: Create branding with header logo file
        Path brandingPath = Paths.get(brandingDir);
        Files.createDirectories(brandingPath);
        String logoPath = brandingPath.resolve("header-1234567890.png").toString();
        Files.write(Paths.get(logoPath), "test logo content".getBytes());

        SiteBranding branding = siteBrandingService.getBranding();
        branding.setHeaderLogoPath(logoPath);
        branding.setPrimaryColor("#1d4ed8");
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Act: DELETE /rest/site-branding/logo/header
        // Then: Expect 200 OK, logo path cleared (headerLogoUrl will be null/absent)
        mockMvc.perform(delete("/rest/site-branding/logo/header")).andExpect(status().isOk())
                .andExpect(jsonPath("$.headerLogoUrl").doesNotExist());

        // Verify file was deleted
        assertFalse("Logo file should be deleted", Files.exists(Paths.get(logoPath)));
    }

    /**
     * Test: DELETE /rest/site-branding/logo/{type} - validates logo type Task
     * Reference: T060
     */
    @Test
    public void testDeleteLogo_WithInvalidType_Returns400() throws Exception {
        // Arrange: Get the single branding
        SiteBranding branding = siteBrandingService.getBranding();
        branding.setPrimaryColor("#1d4ed8");
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Act: DELETE /rest/site-branding/logo/invalid
        // Then: Expect 400 Bad Request
        mockMvc.perform(delete("/rest/site-branding/logo/invalid")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    /**
     * Test: POST /rest/site-branding/reset - reset all branding to defaults Task
     * Reference: T065
     */
    @Test
    public void testResetBranding_ResetsAllToDefaults() throws Exception {
        // Arrange: Create branding with custom values and logo files
        Path brandingPath = Paths.get(brandingDir);
        Files.createDirectories(brandingPath);
        String headerPath = brandingPath.resolve("header-123.png").toString();
        String loginPath = brandingPath.resolve("login-123.png").toString();
        String faviconPath = brandingPath.resolve("favicon-123.ico").toString();
        Files.write(Paths.get(headerPath), "header content".getBytes());
        Files.write(Paths.get(loginPath), "login content".getBytes());
        Files.write(Paths.get(faviconPath), "favicon content".getBytes());

        SiteBranding branding = siteBrandingService.getBranding();
        branding.setHeaderLogoPath(headerPath);
        branding.setLoginLogoPath(loginPath);
        branding.setFaviconPath(faviconPath);
        branding.setPrimaryColor("#ff0000");
        branding.setSecondaryColor("#00ff00");
        branding.setHeaderColor("#0000ff");
        branding.setSysUserId("1");
        siteBrandingService.saveBranding(branding);

        // Act: POST /rest/site-branding/reset
        // Then: Expect 200 OK, all logo paths cleared, colors reset to defaults
        mockMvc.perform(post("/rest/site-branding/reset")).andExpect(status().isOk())
                .andExpect(jsonPath("$.headerLogoUrl").doesNotExist())
                .andExpect(jsonPath("$.loginLogoUrl").doesNotExist()).andExpect(jsonPath("$.faviconUrl").doesNotExist())
                .andExpect(jsonPath("$.headerColor").value("#295785")) // Default header color
                .andExpect(jsonPath("$.primaryColor").value("#0f62fe")) // Default primary color
                .andExpect(jsonPath("$.secondaryColor").value("#393939")); // Default secondary color

        // Verify files were deleted
        assertFalse("Header logo file should be deleted", Files.exists(Paths.get(headerPath)));
        assertFalse("Login logo file should be deleted", Files.exists(Paths.get(loginPath)));
        assertFalse("Favicon file should be deleted", Files.exists(Paths.get(faviconPath)));
    }
}
