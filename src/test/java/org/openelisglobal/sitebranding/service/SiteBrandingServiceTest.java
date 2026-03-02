package org.openelisglobal.sitebranding.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.sitebranding.dao.SiteBrandingDAO;
import org.openelisglobal.sitebranding.valueholder.SiteBranding;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Unit tests for SiteBrandingService implementation
 * 
 * References: - Testing Roadmap: .specify/guides/testing-roadmap.md - Backend
 * Best Practices: .specify/guides/backend-testing-best-practices.md - Template:
 * JUnit 4 Service Test
 * 
 * TDD Workflow (MANDATORY for complex logic): - RED: Write failing test first
 * (defines expected behavior) - GREEN: Write minimal code to make test pass -
 * REFACTOR: Improve code quality while keeping tests green
 * 
 * SDD Checkpoint: Unit tests MUST pass after Phase 2 (Services) Test Coverage
 * Goal: >80% (measured via JaCoCo)
 * 
 * Task Reference: T006
 */
@RunWith(MockitoJUnitRunner.class)
public class SiteBrandingServiceTest {

    // ✅ CORRECT: Use @Mock for isolated unit tests (NOT @MockBean)
    @Mock
    private SiteBrandingDAO siteBrandingDAO;

    // ✅ CORRECT: Use @InjectMocks to inject mocks into service under test
    @InjectMocks
    private SiteBrandingServiceImpl siteBrandingService;

    private SiteBranding testBranding;

    @Before
    public void setUp() {
        // Setup test data
        testBranding = new SiteBranding();
        testBranding.setId(1);
        testBranding.setPrimaryColor("#1d4ed8");
        testBranding.setSecondaryColor("#64748b");
        testBranding.setHeaderColor("#112233");
        testBranding.setColorMode("light");
        testBranding.setUseHeaderLogoForLogin(false);
    }

    /**
     * Test: getBranding - creates default if none exists
     * Task Reference: T006
     */
    @Test
    public void testGetBranding_WhenNoneExists_CreatesDefault() {
        // Arrange: No branding exists in database
        when(siteBrandingDAO.getBranding()).thenReturn(null);
        when(siteBrandingDAO.insert(any(SiteBranding.class))).thenAnswer(invocation -> {
            SiteBranding branding = invocation.getArgument(0);
            branding.setId(2);
            return 2;
        });

        // Act: Get branding (should create default)
        SiteBranding result = siteBrandingService.getBranding();

        // Assert: Default branding created with default values
        assertNotNull("Result should not be null", result);
        assertEquals("Primary color should be default", "#0f62fe", result.getPrimaryColor());
        assertEquals("Secondary color should be default", "#393939", result.getSecondaryColor());
        assertEquals("Header color should be default", "#295785", result.getHeaderColor());
        assertEquals("Color mode should be default", "light", result.getColorMode());
        verify(siteBrandingDAO, times(1)).insert(any(SiteBranding.class));
    }

    /**
     * Test: getBranding - returns existing branding if it exists
     * Task Reference: T006
     */
    @Test
    public void testGetBranding_WhenExists_ReturnsExisting() {
        // Arrange: Branding exists in database
        when(siteBrandingDAO.getBranding()).thenReturn(testBranding);

        // Act: Get branding
        SiteBranding result = siteBrandingService.getBranding();

        // Assert: Returns existing branding
        assertNotNull("Result should not be null", result);
        assertEquals("ID should match", Integer.valueOf(1), result.getId());
        verify(siteBrandingDAO, never()).insert(any(SiteBranding.class));
    }

    /**
     * Test: validateColor - valid CSS color values Task Reference: T006
     *
     * Color validation is now permissive - any non-empty string is accepted. This
     * allows hex codes, named colors, rgb(), hsl(), and other CSS color formats.
     */
    @Test
    public void testValidateHexColor_WithValidCssColors_ReturnsTrue() {
        // Test various CSS color formats - all should be valid
        assertTrue("6-digit hex should be valid", siteBrandingService.validateColor("#1d4ed8"));
        assertTrue("3-digit hex should be valid", siteBrandingService.validateColor("#1d4"));
        assertTrue("Uppercase hex should be valid", siteBrandingService.validateColor("#1D4ED8"));
        assertTrue("Named color should be valid", siteBrandingService.validateColor("blue"));
        assertTrue("Named color rebeccapurple should be valid", siteBrandingService.validateColor("rebeccapurple"));
        assertTrue("RGB format should be valid", siteBrandingService.validateColor("rgb(29, 78, 216)"));
        assertTrue("HSL format should be valid", siteBrandingService.validateColor("hsl(217, 91%, 48%)"));
    }

    /**
     * Test: validateColor - empty/null values Task Reference: T006
     */
    @Test
    public void testValidateHexColor_WithEmptyOrNullValues_ReturnsFalse() {
        // Only empty/null values should be invalid
        assertFalse("Empty string should be invalid", siteBrandingService.validateColor(""));
        assertFalse("Whitespace-only string should be invalid", siteBrandingService.validateColor("   "));
        assertFalse("Null should be invalid", siteBrandingService.validateColor(null));
    }

    /**
     * Test: saveBranding - insert new branding when none exists Task Reference:
     * T006
     */
    @Test
    public void testSaveBranding_WithNewBranding_WhenNoneExists_Inserts() {
        // Arrange: New branding (no ID) and no existing branding in database
        SiteBranding newBranding = new SiteBranding();
        newBranding.setPrimaryColor("#ff0000");
        when(siteBrandingDAO.getBranding()).thenReturn(null);
        when(siteBrandingDAO.insert(any(SiteBranding.class))).thenAnswer(invocation -> {
            SiteBranding branding = invocation.getArgument(0);
            branding.setId(3);
            return 3;
        });

        // Act: Save branding
        SiteBranding result = siteBrandingService.saveBranding(newBranding);

        // Assert: Branding inserted
        assertNotNull("Result should not be null", result);
        assertEquals("ID should be set", Integer.valueOf(3), result.getId());
        verify(siteBrandingDAO, times(1)).insert(any(SiteBranding.class));
        verify(siteBrandingDAO, never()).update(any(SiteBranding.class));
    }

    /**
     * Test: saveBranding - prevents duplicate records by updating existing when
     * saving with null ID
     */
    @Test
    public void testSaveBranding_WithNewBranding_WhenOneExists_UpdatesExisting() {
        // Arrange: New branding (no ID) but existing branding already in database
        SiteBranding newBranding = new SiteBranding();
        newBranding.setPrimaryColor("#ff0000");
        newBranding.setSecondaryColor("#00ff00");
        newBranding.setHeaderColor("#0000ff");
        newBranding.setColorMode("dark");

        // Existing branding in database
        when(siteBrandingDAO.getBranding()).thenReturn(testBranding);
        when(siteBrandingDAO.get(testBranding.getId())).thenReturn(java.util.Optional.of(testBranding));
        when(siteBrandingDAO.update(any(SiteBranding.class))).thenReturn(testBranding);

        // Act: Save new branding (should update existing instead of inserting)
        SiteBranding result = siteBrandingService.saveBranding(newBranding);

        // Assert: Existing branding was updated, not a new one inserted
        assertNotNull("Result should not be null", result);
        verify(siteBrandingDAO, never()).insert(any(SiteBranding.class));
        verify(siteBrandingDAO, times(1)).update(any(SiteBranding.class));
    }

    /**
     * Test: saveBranding - update existing branding Task Reference: T006
     */
    @Test
    public void testSaveBranding_WithExistingBranding_Updates() {
        // Arrange: Existing branding (has ID)
        testBranding.setPrimaryColor("#ff0000");
        // The service fetches a fresh managed entity by ID before updating
        when(siteBrandingDAO.get(testBranding.getId())).thenReturn(java.util.Optional.of(testBranding));
        when(siteBrandingDAO.update(any(SiteBranding.class))).thenReturn(testBranding);

        // Act: Save branding
        SiteBranding result = siteBrandingService.saveBranding(testBranding);

        // Assert: Branding updated
        assertNotNull("Result should not be null", result);
        assertEquals("Primary color should be updated", "#ff0000", result.getPrimaryColor());
        verify(siteBrandingDAO, times(1)).update(any(SiteBranding.class));
        verify(siteBrandingDAO, never()).insert(any(SiteBranding.class));
    }

    /**
     * Test: saveBranding - accepts CSS color formats Task Reference: T006
     *
     * Color validation is now permissive - any non-empty string is accepted. CSS
     * handles invalid colors gracefully by ignoring them, so we allow named colors,
     * rgb(), hsl(), and other formats.
     */
    @Test
    public void testSaveBranding_WithCssNamedColor_Succeeds() {
        // Arrange: Branding with CSS named color
        testBranding.setPrimaryColor("rebeccapurple");
        when(siteBrandingDAO.get(testBranding.getId())).thenReturn(java.util.Optional.of(testBranding));
        when(siteBrandingDAO.update(any(SiteBranding.class))).thenReturn(testBranding);

        // Act: Save branding (should succeed)
        SiteBranding result = siteBrandingService.saveBranding(testBranding);

        // Assert: Color was saved
        assertNotNull("Result should not be null", result);
        assertEquals("rebeccapurple", result.getPrimaryColor());
    }

    /**
     * Test: validateLogoFile - valid PNG file Task Reference: T026
     */
    @Test
    public void testValidateLogoFile_WithValidPng_ReturnsTrue() throws IOException {
        // Create a real PNG image
        byte[] pngBytes = createValidPngImage();
        MockMultipartFile file = new MockMultipartFile("file", "test-logo.png", "image/png", pngBytes);

        assertTrue("Valid PNG file should pass validation", siteBrandingService.validateLogoFile(file));
    }

    /**
     * Test: validateLogoFile - valid JPG file Task Reference: T026
     */
    @Test
    public void testValidateLogoFile_WithValidJpg_ReturnsTrue() throws IOException {
        // Create a real JPG image
        byte[] jpgBytes = createValidJpgImage();
        MockMultipartFile file = new MockMultipartFile("file", "test-logo.jpg", "image/jpeg", jpgBytes);

        assertTrue("Valid JPG file should pass validation", siteBrandingService.validateLogoFile(file));
    }

    /**
     * Test: validateLogoFile - valid SVG file Task Reference: T026
     */
    @Test
    public void testValidateLogoFile_WithValidSvg_ReturnsTrue() {
        String svgContent = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"100\">"
                + "<circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"blue\"/></svg>";
        MockMultipartFile file = new MockMultipartFile("file", "test-logo.svg", "image/svg+xml", svgContent.getBytes());

        assertTrue("Valid SVG file should pass validation", siteBrandingService.validateLogoFile(file));
    }

    /**
     * Test: validateLogoFile - invalid file format (text file with .txt extension)
     * Task Reference: T026
     */
    @Test
    public void testValidateLogoFile_WithInvalidExtension_ReturnsFalse() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "not an image".getBytes());

        assertFalse("File with invalid extension should fail validation", siteBrandingService.validateLogoFile(file));
    }

    /**
     * Test: validateLogoFile - fake PNG (text content with .png extension) Task
     * Reference: T026
     */
    @Test
    public void testValidateLogoFile_WithFakePng_ReturnsFalse() {
        // A text file disguised as PNG - content validation should catch this
        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "image/png",
                "this is not actually a PNG image".getBytes());

        assertFalse("Fake PNG file should fail content validation", siteBrandingService.validateLogoFile(file));
    }

    /**
     * Test: validateLogoFile - file exceeds size limit Task Reference: T026
     */
    @Test
    public void testValidateLogoFile_WithExcessiveSize_ReturnsFalse() throws IOException {
        // Create a file larger than 2MB
        byte[] largeContent = new byte[3 * 1024 * 1024]; // 3MB
        MockMultipartFile file = new MockMultipartFile("file", "large.png", "image/png", largeContent);

        assertFalse("File exceeding size limit should fail validation", siteBrandingService.validateLogoFile(file));
    }

    /**
     * Test: validateLogoFile - null file Task Reference: T026
     */
    @Test
    public void testValidateLogoFile_WithNullFile_ReturnsFalse() {
        assertFalse("Null file should fail validation", siteBrandingService.validateLogoFile(null));
    }

    /**
     * Test: validateLogoFile - empty file Task Reference: T026
     */
    @Test
    public void testValidateLogoFile_WithEmptyFile_ReturnsFalse() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertFalse("Empty file should fail validation", siteBrandingService.validateLogoFile(file));
    }

    /**
     * Test: validateLogoFile - SVG with XXE attack attempt Task Reference: T026
     *
     * Ensures that SVG files with external entity declarations (potential XXE
     * attacks) are rejected.
     */
    @Test
    public void testValidateLogoFile_WithXxeSvg_ReturnsFalse() {
        // SVG with DOCTYPE and external entity - potential XXE attack
        String xxeSvg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<!DOCTYPE svg [ <!ENTITY xxe SYSTEM \"file:///etc/passwd\"> ]>"
                + "<svg xmlns=\"http://www.w3.org/2000/svg\">" + "<text>&xxe;</text>" + "</svg>";
        MockMultipartFile file = new MockMultipartFile("file", "xxe.svg", "image/svg+xml", xxeSvg.getBytes());

        assertFalse("SVG with XXE attack should fail validation", siteBrandingService.validateLogoFile(file));
    }

    /**
     * Test: validateLogoFile - invalid SVG (not XML) Task Reference: T026
     */
    @Test
    public void testValidateLogoFile_WithInvalidSvg_ReturnsFalse() {
        String invalidSvg = "this is not valid XML or SVG content";
        MockMultipartFile file = new MockMultipartFile("file", "invalid.svg", "image/svg+xml", invalidSvg.getBytes());

        assertFalse("Invalid SVG content should fail validation", siteBrandingService.validateLogoFile(file));
    }

    /**
     * Helper method to create a valid PNG image for testing.
     */
    private byte[] createValidPngImage() throws IOException {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    /**
     * Helper method to create a valid JPG image for testing.
     */
    private byte[] createValidJpgImage() throws IOException {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }
}
