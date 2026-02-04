package org.openelisglobal.sitebranding.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.sitebranding.dao.SiteBrandingDAO;
import org.openelisglobal.sitebranding.valueholder.SiteBranding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;

/**
 * Service implementation for SiteBranding entity
 *
 * Task Reference: T015
 */
@Service
@Transactional
public class SiteBrandingServiceImpl extends BaseObjectServiceImpl<SiteBranding, Integer>
        implements SiteBrandingService {

    private static final Logger logger = LoggerFactory.getLogger(SiteBrandingServiceImpl.class);
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final String[] ALLOWED_FORMATS = { "png", "svg", "jpg", "jpeg" };

    @Value("${org.openelisglobal.branding.dir:/var/lib/openelis-global/branding/}")
    private String brandingDir;

    @Autowired
    private SiteBrandingDAO siteBrandingDAO;

    public SiteBrandingServiceImpl() {
        super(SiteBranding.class);
    }

    @Override
    protected SiteBrandingDAO getBaseObjectDAO() {
        return siteBrandingDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public SiteBranding getBranding() {
        try {
            SiteBranding branding = siteBrandingDAO.getBranding();
            if (branding == null) {
                logger.info("No branding found in database, creating default branding");
                // Create default branding if none exists
                branding = createDefaultBranding();
                logger.info("Default branding created: id={}", branding.getId());
            }
            return branding;
        } catch (Exception e) {
            logger.error("Error getting SiteBranding", e);
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error getting SiteBranding", e);
        }
    }

    @Override
    public SiteBranding saveBranding(SiteBranding branding) {
        logger.debug(
                "saveBranding() called with branding id={}, headerColor={}, primaryColor={}, secondaryColor={}, colorMode={}, useHeaderLogoForLogin={}",
                branding.getId(), branding.getHeaderColor(), branding.getPrimaryColor(), branding.getSecondaryColor(),
                branding.getColorMode(), branding.getUseHeaderLogoForLogin());

        try {

            // Task Reference: T094 - Ensure sysUserId and lastupdated are set for audit
            // trail
            branding.setLastupdatedFields();
            logger.debug("Set lastupdatedFields, sysUserId={}", branding.getSysUserId());
            // sysUserId should be set by controller before calling this method

            // Task Reference: T099 - Validate all logo paths exist (if not null)
            validateLogoPaths(branding);
            logger.debug("Logo path validation passed");

            if (branding.getId() == null) {
                // Check if a branding record already exists to prevent duplicates
                SiteBranding existing = siteBrandingDAO.getBranding();
                if (existing != null) {
                    // Use existing record's ID - fall through to update logic
                    logger.debug("Branding record already exists with id={}, will update instead of insert",
                            existing.getId());
                    branding.setId(existing.getId());
                } else {
                    // Insert new record (first branding for this installation)
                    logger.debug("Inserting new branding record");
                    Integer id = siteBrandingDAO.insert(branding);
                    branding.setId(id);
                    logger.info("Branding configuration created: id={}, user={}", id, branding.getSysUserId());
                    LogEvent.logInfo("SiteBrandingService", "saveBranding",
                            "Branding configuration created by user: " + branding.getSysUserId());
                    return branding;
                }
            }

            // Get a fresh managed entity from the database to avoid OptimisticLockException
            // The entity passed in may be detached with a stale version field
            SiteBranding existingBranding = siteBrandingDAO.get(branding.getId())
                    .orElseThrow(() -> new LIMSRuntimeException("Branding not found with id: " + branding.getId()));

            logger.debug("Retrieved managed branding entity for update: id={}", existingBranding.getId());

            // Task Reference: T093 - Log color changes for audit trail
            logger.debug("Comparing existing vs new branding for changes");
            logColorChanges(existingBranding, branding);

            // Copy changes from detached entity to managed entity
            if (branding.getPrimaryColor() != null) {
                existingBranding.setPrimaryColor(branding.getPrimaryColor());
            }
            if (branding.getSecondaryColor() != null) {
                existingBranding.setSecondaryColor(branding.getSecondaryColor());
            }
            if (branding.getHeaderColor() != null) {
                existingBranding.setHeaderColor(branding.getHeaderColor());
            }
            if (branding.getColorMode() != null) {
                existingBranding.setColorMode(branding.getColorMode());
            }
            if (branding.getUseHeaderLogoForLogin() != null) {
                existingBranding.setUseHeaderLogoForLogin(branding.getUseHeaderLogoForLogin());
            }
            if (branding.getHeaderLogoPath() != null) {
                existingBranding.setHeaderLogoPath(branding.getHeaderLogoPath());
            }
            if (branding.getLoginLogoPath() != null) {
                existingBranding.setLoginLogoPath(branding.getLoginLogoPath());
            }
            if (branding.getFaviconPath() != null) {
                existingBranding.setFaviconPath(branding.getFaviconPath());
            }
            if (branding.getSysUserId() != null) {
                existingBranding.setSysUserId(branding.getSysUserId());
            }

            // Set lastupdated fields on the managed entity
            existingBranding.setLastupdatedFields();

            logger.debug("Updating existing branding record: id={}", existingBranding.getId());
            SiteBranding updated = siteBrandingDAO.update(existingBranding);
            logger.info("Branding configuration updated: id={}, user={}", updated.getId(), updated.getSysUserId());
            LogEvent.logInfo("SiteBrandingService", "saveBranding",
                    "Branding configuration updated by user: " + existingBranding.getSysUserId());
            return updated;
        } catch (LIMSRuntimeException e) {
            logger.error("LIMSRuntimeException in saveBranding: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error in saveBranding", e);
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error saving SiteBranding", e);
        }
    }

    @Override
    public boolean validateColor(String color) {
        return color != null && !color.trim().isEmpty();
    }

    /**
     * Validate all logo paths exist (if not null) Task Reference: T099
     */
    private void validateLogoPaths(SiteBranding branding) {
        if (branding.getHeaderLogoPath() != null && !Files.exists(Paths.get(branding.getHeaderLogoPath()))) {
            LogEvent.logWarn("SiteBrandingService", "validateLogoPaths",
                    "Header logo path does not exist: " + branding.getHeaderLogoPath());
            // Don't throw exception - just log warning, allow save to proceed
        }
        if (branding.getLoginLogoPath() != null && !Files.exists(Paths.get(branding.getLoginLogoPath()))) {
            LogEvent.logWarn("SiteBrandingService", "validateLogoPaths",
                    "Login logo path does not exist: " + branding.getLoginLogoPath());
        }
        if (branding.getFaviconPath() != null && !Files.exists(Paths.get(branding.getFaviconPath()))) {
            LogEvent.logWarn("SiteBrandingService", "validateLogoPaths",
                    "Favicon path does not exist: " + branding.getFaviconPath());
        }
    }

    /**
     * Log color changes for audit trail Task Reference: T093
     */
    private void logColorChanges(SiteBranding existing, SiteBranding updated) {
        if (existing.getPrimaryColor() != null && updated.getPrimaryColor() != null
                && !existing.getPrimaryColor().equals(updated.getPrimaryColor())) {
            LogEvent.logInfo("SiteBrandingService", "saveBranding",
                    String.format("Primary color changed: %s -> %s by user: %s", existing.getPrimaryColor(),
                            updated.getPrimaryColor(), updated.getSysUserId()));
        }
        if (existing.getSecondaryColor() != null && updated.getSecondaryColor() != null
                && !existing.getSecondaryColor().equals(updated.getSecondaryColor())) {
            LogEvent.logInfo("SiteBrandingService", "saveBranding",
                    String.format("Secondary color changed: %s -> %s by user: %s", existing.getSecondaryColor(),
                            updated.getSecondaryColor(), updated.getSysUserId()));
        }
        if (existing.getHeaderColor() != null && updated.getHeaderColor() != null
                && !existing.getHeaderColor().equals(updated.getHeaderColor())) {
            LogEvent.logInfo("SiteBrandingService", "saveBranding",
                    String.format("Header color changed: %s -> %s by user: %s", existing.getHeaderColor(),
                            updated.getHeaderColor(), updated.getSysUserId()));
        }
    }

    /**
     * Create default branding configuration
     *
     * @return SiteBranding entity with default values
     */
    private SiteBranding createDefaultBranding() {
        SiteBranding branding = new SiteBranding();
        branding.setHeaderColor("#295785");
        branding.setPrimaryColor("#0f62fe");
        branding.setSecondaryColor("#393939");
        branding.setColorMode("light");
        branding.setUseHeaderLogoForLogin(false);
        branding.setLastupdatedFields();
        // Set default sysUserId for initial creation (will be updated on first save)
        branding.setSysUserId("system");

        // Insert default record
        Integer id = siteBrandingDAO.insert(branding);
        branding.setId(id);
        return branding;
    }

    @Override
    public boolean validateLogoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            return false;
        }

        // Validate file format by extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return false;
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        List<String> allowedList = Arrays.asList(ALLOWED_FORMATS);
        if (!allowedList.contains(extension)) {
            return false;
        }

        // Validate actual image content to prevent malicious files with fake extensions
        try {
            if (extension.equals("svg")) {
                return isValidSvgContent(file.getInputStream());
            } else {
                return isValidRasterImage(file.getInputStream());
            }
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "validateLogoFile",
                    "Error validating image content: " + e.getMessage());
            return false;
        }
    }

    /**
     * Validates that the input stream contains a valid raster image (PNG, JPG,
     * JPEG) by checking if ImageIO can find a reader for it.
     */
    private boolean isValidRasterImage(InputStream inputStream) throws IOException {
        ImageInputStream iis = ImageIO.createImageInputStream(inputStream);
        if (iis == null) {
            return false;
        }
        try {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            return readers.hasNext();
        } finally {
            iis.close();
        }
    }

    /**
     * Validates that the input stream contains a valid SVG file by parsing it as
     * XML and checking for an svg root element. Also prevents XXE attacks by
     * disabling external entities.
     */
    private boolean isValidSvgContent(InputStream inputStream) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Disable external entities to prevent XXE attacks
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputStream);
            return "svg".equalsIgnoreCase(doc.getDocumentElement().getTagName());
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "isValidSvgContent",
                    "SVG validation failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public String uploadLogo(MultipartFile file, LogoType type) throws IOException {
        // Validate file
        if (!validateLogoFile(file)) {
            throw new LIMSRuntimeException(
                    "Invalid logo file: format must be PNG, SVG, or JPG/JPEG, size must be <= 2MB");
        }

        // Ensure branding directory exists
        Path brandingPath = Paths.get(brandingDir);
        if (!Files.exists(brandingPath)) {
            Files.createDirectories(brandingPath);
        }

        // Get current branding
        SiteBranding branding = getBranding();

        // Task Reference: T040 - Handle useHeaderLogoForLogin flag for login logo
        if (type == LogoType.LOGIN && branding.getUseHeaderLogoForLogin()) {
            // If using header logo for login, don't store separate login logo
            // Clear login logo path if it exists
            if (branding.getLoginLogoPath() != null) {
                try {
                    Files.deleteIfExists(Paths.get(branding.getLoginLogoPath()));
                } catch (IOException e) {
                    LogEvent.logError("Error deleting old login logo file", e);
                }
                branding.setLoginLogoPath(null);
                saveBranding(branding);
            }
            throw new LIMSRuntimeException("Cannot upload login logo when 'Use header logo for login' is enabled");
        }

        // Delete old logo file if exists
        String oldPath = getLogoPath(branding, type);
        if (oldPath != null) {
            try {
                Files.deleteIfExists(Paths.get(oldPath));
            } catch (IOException e) {
                LogEvent.logError("Error deleting old logo file: " + oldPath, e);
            }
        }

        // Generate new filename: {type}-{timestamp}.{ext}
        String extension = getFileExtension(file.getOriginalFilename());
        String timestamp = String.valueOf(System.currentTimeMillis());
        String filename = type.getValue() + "-" + timestamp + "." + extension;
        Path filePath = brandingPath.resolve(filename);

        // Save file
        file.transferTo(filePath.toFile());

        // Update branding entity with file path
        String fullPath = filePath.toAbsolutePath().toString();
        setLogoPath(branding, type, fullPath);

        // Save branding
        saveBranding(branding);

        // Task Reference: T093 - Log logo upload for audit trail
        LogEvent.logInfo("SiteBrandingService", "uploadLogo",
                String.format("Logo uploaded - Type: %s, File: %s, Size: %d bytes, User: %s", type.getValue(),
                        file.getOriginalFilename(), file.getSize(), branding.getSysUserId()));

        return fullPath;
    }

    @Override
    public String getLogoUrl(LogoType type) {
        SiteBranding branding = getBranding();
        String path = getLogoPath(branding, type);
        if (path != null && Files.exists(Paths.get(path))) {
            return "/rest/site-branding/logo/" + type.getValue();
        }
        return null; // Return null to indicate default logo should be used
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }

    /**
     * Get logo path from branding entity based on type
     */
    private String getLogoPath(SiteBranding branding, LogoType type) {
        switch (type) {
        case HEADER:
            return branding.getHeaderLogoPath();
        case LOGIN:
            return branding.getLoginLogoPath();
        case FAVICON:
            return branding.getFaviconPath();
        default:
            return null;
        }
    }

    /**
     * Set logo path in branding entity based on type
     */
    private void setLogoPath(SiteBranding branding, LogoType type, String path) {
        switch (type) {
        case HEADER:
            branding.setHeaderLogoPath(path);
            break;
        case LOGIN:
            branding.setLoginLogoPath(path);
            break;
        case FAVICON:
            branding.setFaviconPath(path);
            break;
        }
    }

    /**
     * Remove logo file and update branding configuration Task Reference: T063
     */
    @Override
    @Transactional
    public void removeLogo(LogoType type) throws IOException {
        // Get current branding
        SiteBranding branding = getBranding();

        // Get logo path
        String logoPath = getLogoPath(branding, type);
        if (logoPath == null) {
            // No logo to remove
            return;
        }

        // Delete file from filesystem
        try {
            Path filePath = Paths.get(logoPath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            LogEvent.logError("Error deleting logo file: " + logoPath, e);
            throw new LIMSRuntimeException("Failed to delete logo file", e);
        }

        // Set logo path to null in branding entity
        setLogoPath(branding, type, null);

        // Save branding
        saveBranding(branding);

        // Task Reference: T093 - Log logo removal for audit trail
        LogEvent.logInfo("SiteBrandingService", "removeLogo", String.format(
                "Logo removed - Type: %s, File: %s, User: %s", type.getValue(), logoPath, branding.getSysUserId()));
    }

    /**
     * Reset all branding to default values Task Reference: T066
     */
    @Override
    @Transactional
    public void resetToDefaults() throws IOException {
        // Get current branding
        SiteBranding branding = getBranding();

        // Delete all logo files
        String[] logoPaths = { branding.getHeaderLogoPath(), branding.getLoginLogoPath(), branding.getFaviconPath() };

        for (String logoPath : logoPaths) {
            if (logoPath != null) {
                try {
                    Path filePath = Paths.get(logoPath);
                    if (Files.exists(filePath)) {
                        Files.delete(filePath);
                        logger.info("Deleted logo file: {}", logoPath);
                    }
                } catch (IOException e) {
                    LogEvent.logError("Error deleting logo file during reset: " + logoPath, e);
                    // Continue with other files even if one fails
                }
            }
        }

        // Get fresh managed entity to update directly (bypassing saveBranding's
        // null-check logic)
        SiteBranding managedBranding = siteBrandingDAO.get(branding.getId())
                .orElseThrow(() -> new LIMSRuntimeException("Branding not found with id: " + branding.getId()));

        // Reset all fields to defaults - including explicitly setting logo paths to
        // null
        managedBranding.setHeaderLogoPath(null);
        managedBranding.setLoginLogoPath(null);
        managedBranding.setFaviconPath(null);
        managedBranding.setUseHeaderLogoForLogin(false);
        managedBranding.setHeaderColor("#295785");
        managedBranding.setPrimaryColor("#0f62fe");
        managedBranding.setSecondaryColor("#393939");
        managedBranding.setColorMode("light");
        managedBranding.setSysUserId(branding.getSysUserId());
        managedBranding.setLastupdatedFields();

        // Update directly
        siteBrandingDAO.update(managedBranding);

        // Task Reference: T093 - Log reset action for audit trail
        LogEvent.logInfo("SiteBrandingService", "resetToDefaults",
                "All branding reset to defaults by user: " + branding.getSysUserId());
    }
}
