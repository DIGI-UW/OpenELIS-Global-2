package org.openelisglobal.sitebranding.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.sitebranding.valueholder.SiteBranding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.yaml.snakeyaml.Yaml;

/**
 * Configuration handler for loading site branding settings from YAML files.
 * Enables pre-configuration of branding via initializer packages.
 *
 * Configuration files should be placed in the site-branding subdirectory of the
 * backend configuration directory.
 *
 * Example YAML format:
 *
 * <pre>
 * siteBranding:
 *   headerLogo: "branding/custom-header-logo.png"
 *   loginLogo: "branding/custom-login-logo.png"
 *   useHeaderLogoForLogin: false
 *   favicon: "branding/custom-favicon.png"
 *   colorMode: "light"
 *   colors:
 *     header: "#295785"
 *     primary: "#0f62fe"
 *     secondary: "#393939"
 * </pre>
 *
 * Logo file paths are relative to the configuration directory. Files are copied
 * to the branding directory.
 */
@Component
public class SiteBrandingConfigurationHandler implements DomainConfigurationHandler {

    @Value("${org.openelisglobal.branding.dir:/var/lib/openelis-global/branding/}")
    private String brandingDir;

    @Value("${org.openelisglobal.branding.config.dir:/var/lib/openelis-global/configuration/backend/site-branding/}")
    private String configDir;

    @Autowired
    private SiteBrandingService siteBrandingService;

    @Override
    public String getDomainName() {
        return "site-branding";
    }

    @Override
    public String getFileExtension() {
        return "yml";
    }

    @Override
    public int getLoadOrder() {
        // Load early since branding has no dependencies on other entities
        return 50;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Processing site branding configuration from: " + fileName);

        Yaml yaml = new Yaml();
        Map<String, Object> config = yaml.load(inputStream);

        if (config == null || !config.containsKey("siteBranding")) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processConfiguration",
                    "Configuration file " + fileName + " does not contain 'siteBranding' key");
            return;
        }

        Map<String, Object> brandingConfig = (Map<String, Object>) config.get("siteBranding");
        if (brandingConfig == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processConfiguration",
                    "siteBranding configuration is null in file: " + fileName);
            return;
        }

        // Get or create branding entity
        SiteBranding branding = siteBrandingService.getBranding();
        branding.setSysUserId("1"); // System user for configuration loading

        // Process logo files
        processLogoFile(brandingConfig, "headerLogo", branding, LogoType.HEADER);
        processLogoFile(brandingConfig, "loginLogo", branding, LogoType.LOGIN);
        processLogoFile(brandingConfig, "favicon", branding, LogoType.FAVICON);

        // Process useHeaderLogoForLogin flag
        if (brandingConfig.containsKey("useHeaderLogoForLogin")) {
            Object value = brandingConfig.get("useHeaderLogoForLogin");
            branding.setUseHeaderLogoForLogin(Boolean.TRUE.equals(value));
        }

        // Process colors
        if (brandingConfig.containsKey("colors")) {
            Map<String, Object> colors = (Map<String, Object>) brandingConfig.get("colors");
            if (colors != null) {
                if (colors.containsKey("primary") && colors.get("primary") != null) {
                    branding.setPrimaryColor(colors.get("primary").toString().trim());
                }
                if (colors.containsKey("secondary") && colors.get("secondary") != null) {
                    branding.setSecondaryColor(colors.get("secondary").toString().trim());
                }
                if (colors.containsKey("header") && colors.get("header") != null) {
                    branding.setHeaderColor(colors.get("header").toString().trim());
                }
            }
        }

        // Process color mode
        if (brandingConfig.containsKey("colorMode")) {
            Object colorMode = brandingConfig.get("colorMode");
            if (colorMode != null) {
                branding.setColorMode(colorMode.toString().trim());
            }
        }

        // Save updated branding
        siteBrandingService.saveBranding(branding);

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Successfully applied site branding configuration from: " + fileName);
    }

    /**
     * Process a logo file from the configuration. Validates that the file is a
     * legitimate image and copies it from the configuration directory to the
     * branding directory.
     */
    private void processLogoFile(Map<String, Object> config, String key, SiteBranding branding, LogoType type) {
        if (!config.containsKey(key) || config.get(key) == null) {
            return;
        }

        String relativePath = config.get(key).toString().trim();
        if (relativePath.isEmpty()) {
            return;
        }

        // Source file is relative to the configuration directory
        Path sourcePath = Paths.get(configDir, relativePath);
        if (!Files.exists(sourcePath)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processLogoFile",
                    "Logo file not found: " + sourcePath + " for " + key);
            return;
        }

        // Validate the file is a legitimate image before copying
        if (!isValidImageFile(sourcePath)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processLogoFile",
                    "Invalid image file: " + sourcePath + " for " + key + " - file is not a valid image");
            return;
        }

        try {
            // Ensure branding directory exists before copying
            Path brandingPath = Paths.get(brandingDir);
            if (!Files.exists(brandingPath)) {
                Files.createDirectories(brandingPath);
            }

            // Generate destination filename with timestamp to avoid caching issues
            String originalFilename = sourcePath.getFileName().toString();
            String extension = getFileExtension(originalFilename);
            String timestamp = String.valueOf(System.currentTimeMillis());
            String destFilename = type.getValue() + "-" + timestamp + "." + extension;
            Path destPath = brandingPath.resolve(destFilename);

            // Copy file to branding directory
            Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);

            // Update branding entity with new path
            String fullPath = destPath.toAbsolutePath().toString();
            switch (type) {
            case HEADER:
                branding.setHeaderLogoPath(fullPath);
                break;
            case LOGIN:
                branding.setLoginLogoPath(fullPath);
                break;
            case FAVICON:
                branding.setFaviconPath(fullPath);
                break;
            }

            LogEvent.logInfo(this.getClass().getSimpleName(), "processLogoFile",
                    "Copied logo file: " + sourcePath + " -> " + destPath);

        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "processLogoFile",
                    "Failed to copy logo file: " + sourcePath + " - " + e.getMessage());
        }
    }

    /**
     * Validates that a file at the given path is a legitimate image file by
     * checking both the extension and the actual file content.
     */
    private boolean isValidImageFile(Path filePath) {
        String extension = getFileExtension(filePath.getFileName().toString()).toLowerCase();
        List<String> allowedExtensions = Arrays.asList("png", "svg", "jpg", "jpeg");

        // Check extension is allowed
        if (!allowedExtensions.contains(extension)) {
            return false;
        }

        // For SVG files, check XML structure
        if (extension.equals("svg")) {
            return isValidSvgFile(filePath);
        }

        // For raster images, validate using ImageIO
        return isValidRasterImageFile(filePath);
    }

    /**
     * Validates that a file contains valid raster image data by checking if ImageIO
     * can read it.
     */
    private boolean isValidRasterImageFile(Path filePath) {
        try (InputStream is = Files.newInputStream(filePath)) {
            ImageInputStream iis = ImageIO.createImageInputStream(is);
            if (iis == null) {
                return false;
            }
            try {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
                return readers.hasNext();
            } finally {
                iis.close();
            }
        } catch (IOException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "isValidRasterImageFile",
                    "Failed to validate raster image: " + e.getMessage());
            return false;
        }
    }

    /**
     * Validates that a file contains valid SVG content by parsing it as XML and
     * checking for an svg root element. Prevents XXE attacks by disabling external
     * entities.
     */
    private boolean isValidSvgFile(Path filePath) {
        try (InputStream is = Files.newInputStream(filePath)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Disable external entities to prevent XXE attacks
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            return "svg".equalsIgnoreCase(doc.getDocumentElement().getTagName());
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "isValidSvgFile",
                    "SVG validation failed: " + e.getMessage());
            return false;
        }
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }
}
