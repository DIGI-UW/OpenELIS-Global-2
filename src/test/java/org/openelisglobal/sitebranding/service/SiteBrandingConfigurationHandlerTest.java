package org.openelisglobal.sitebranding.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.sitebranding.valueholder.SiteBranding;

/**
 * Unit tests for SiteBrandingConfigurationHandler
 *
 * Tests the YAML configuration loading functionality for site branding via the
 * initializer package mechanism.
 */
@RunWith(MockitoJUnitRunner.class)
public class SiteBrandingConfigurationHandlerTest {

    @Mock
    private SiteBrandingService siteBrandingService;

    @InjectMocks
    private SiteBrandingConfigurationHandler handler;

    private SiteBranding testBranding;

    @Before
    public void setUp() {
        testBranding = new SiteBranding();
        testBranding.setId(1);
        testBranding.setPrimaryColor("#1d4ed8");
        testBranding.setSecondaryColor("#64748b");
        testBranding.setHeaderColor("#112233");
        testBranding.setColorMode("light");
        testBranding.setUseHeaderLogoForLogin(false);

        when(siteBrandingService.getBranding()).thenReturn(testBranding);
        when(siteBrandingService.saveBranding(any(SiteBranding.class))).thenReturn(testBranding);
    }

    @Test
    public void testGetDomainName() {
        assertEquals("site-branding", handler.getDomainName());
    }

    @Test
    public void testGetFileExtension() {
        assertEquals("yml", handler.getFileExtension());
    }

    @Test
    public void testGetLoadOrder() {
        assertEquals(50, handler.getLoadOrder());
    }

    @Test
    public void testProcessConfiguration_WithColors() throws Exception {
        String yaml = "siteBranding:\n" + "  colors:\n" + "    header: \"rgb(56, 178, 172)\"\n"
                + "    primary: \"#1a365d\"\n" + "    secondary: \"slate\"\n";

        InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

        handler.processConfiguration(inputStream, "test-branding.yml");

        // Verify branding was retrieved and saved
        verify(siteBrandingService, times(1)).getBranding();
        verify(siteBrandingService, times(1)).saveBranding(any(SiteBranding.class));

        // Verify colors were updated on the branding object
        assertEquals("rgb(56, 178, 172)", testBranding.getHeaderColor());
        assertEquals("#1a365d", testBranding.getPrimaryColor());
        assertEquals("slate", testBranding.getSecondaryColor());
    }

    @Test
    public void testProcessConfiguration_WithUseHeaderLogoForLogin() throws Exception {
        String yaml = "siteBranding:\n  useHeaderLogoForLogin: true\n";

        InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

        handler.processConfiguration(inputStream, "test-branding.yml");

        // Verify useHeaderLogoForLogin was set
        assertTrue(testBranding.getUseHeaderLogoForLogin());
    }

    @Test
    public void testProcessConfiguration_WithColorMode() throws Exception {
        String yaml = "siteBranding:\n  colorMode: dark\n";

        InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

        handler.processConfiguration(inputStream, "test-branding.yml");

        assertEquals("dark", testBranding.getColorMode());
    }

    @Test
    public void testProcessConfiguration_WithEmptyFile() throws Exception {
        String yaml = "";

        InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

        handler.processConfiguration(inputStream, "empty.yml");

        // Should not attempt to save when config is empty
        verify(siteBrandingService, never()).saveBranding(any(SiteBranding.class));
    }

    @Test
    public void testProcessConfiguration_WithMissingSiteBrandingKey() throws Exception {
        String yaml = "otherConfig:\n  someKey: someValue\n";

        InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

        handler.processConfiguration(inputStream, "no-branding.yml");

        // Should not attempt to save when siteBranding key is missing
        verify(siteBrandingService, never()).saveBranding(any(SiteBranding.class));
    }

    @Test
    public void testProcessConfiguration_WithPartialColors() throws Exception {
        // Only specifying primary color - other colors should remain unchanged
        String yaml = "siteBranding:\n  colors:\n    primary: \"navy\"\n";

        InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

        handler.processConfiguration(inputStream, "partial-colors.yml");

        // Verify only primary was updated
        assertEquals("navy", testBranding.getPrimaryColor());
        // Other colors should remain at their original values from setUp()
        assertEquals("#64748b", testBranding.getSecondaryColor());
        assertEquals("#112233", testBranding.getHeaderColor());
    }

}
