package org.openelisglobal.sitebranding.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.sitebranding.dao.SiteBrandingDAO;
import org.openelisglobal.sitebranding.valueholder.SiteBranding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service implementation for SiteBranding entity
 * 
 * Task Reference: T015
 */
@Service
@Transactional
public class SiteBrandingServiceImpl extends BaseObjectServiceImpl<SiteBranding, String>
        implements SiteBrandingService {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{3,6}$");
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final String BRANDING_DIR = "/var/lib/openelis-global/branding/";
    private static final String[] ALLOWED_FORMATS = { "png", "svg", "jpg", "jpeg" };

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
                // Create default branding if none exists
                branding = createDefaultBranding();
            }
            return branding;
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error getting SiteBranding", e);
        }
    }

    @Override
    public SiteBranding saveBranding(SiteBranding branding) {
        try {
            // Validate colors before saving
            if (branding.getPrimaryColor() != null && !validateHexColor(branding.getPrimaryColor())) {
                throw new LIMSRuntimeException("Invalid primary color format: " + branding.getPrimaryColor());
            }
            if (branding.getSecondaryColor() != null && !validateHexColor(branding.getSecondaryColor())) {
                throw new LIMSRuntimeException("Invalid secondary color format: " + branding.getSecondaryColor());
            }
            if (branding.getAccentColor() != null && !validateHexColor(branding.getAccentColor())) {
                throw new LIMSRuntimeException("Invalid accent color format: " + branding.getAccentColor());
            }

            // Task Reference: T094 - Ensure sysUserId and lastupdated are set for audit trail
            branding.setLastupdatedFields();
            // sysUserId should be set by controller before calling this method

            // Task Reference: T099 - Validate all logo paths exist (if not null)
            validateLogoPaths(branding);

            SiteBranding existingBranding = null;
            if (branding.getId() != null) {
                existingBranding = siteBrandingDAO.get(branding.getId()).orElse(null);
            }

            if (branding.getId() == null) {
                // Insert new record
                String id = siteBrandingDAO.insert(branding);
                branding.setId(id);
                // Task Reference: T093 - Log branding creation
                LogEvent.logInfo("SiteBrandingService", "saveBranding", 
                    "Branding configuration created by user: " + branding.getSysUserId());
                return branding;
            } else {
                // Task Reference: T093 - Log color changes for audit trail
                if (existingBranding != null) {
                    logColorChanges(existingBranding, branding);
                }
                SiteBranding updated = siteBrandingDAO.update(branding);
                LogEvent.logInfo("SiteBrandingService", "saveBranding", 
                    "Branding configuration updated by user: " + branding.getSysUserId());
                return updated;
            }
        } catch (LIMSRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error saving SiteBranding", e);
        }
    }

    @Override
    public boolean validateHexColor(String color) {
        if (color == null || color.trim().isEmpty()) {
            return false;
        }
        return HEX_COLOR_PATTERN.matcher(color.trim()).matches();
    }

    /**
     * Validate all logo paths exist (if not null)
     * Task Reference: T099
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
     * Log color changes for audit trail
     * Task Reference: T093
     */
    private void logColorChanges(SiteBranding existing, SiteBranding updated) {
        if (existing.getPrimaryColor() != null && updated.getPrimaryColor() != null 
            && !existing.getPrimaryColor().equals(updated.getPrimaryColor())) {
            LogEvent.logInfo("SiteBrandingService", "saveBranding", 
                String.format("Primary color changed: %s -> %s by user: %s", 
                    existing.getPrimaryColor(), updated.getPrimaryColor(), updated.getSysUserId()));
        }
        if (existing.getSecondaryColor() != null && updated.getSecondaryColor() != null 
            && !existing.getSecondaryColor().equals(updated.getSecondaryColor())) {
            LogEvent.logInfo("SiteBrandingService", "saveBranding", 
                String.format("Secondary color changed: %s -> %s by user: %s", 
                    existing.getSecondaryColor(), updated.getSecondaryColor(), updated.getSysUserId()));
        }
        if (existing.getAccentColor() != null && updated.getAccentColor() != null 
            && !existing.getAccentColor().equals(updated.getAccentColor())) {
            LogEvent.logInfo("SiteBrandingService", "saveBranding", 
                String.format("Accent color changed: %s -> %s by user: %s", 
                    existing.getAccentColor(), updated.getAccentColor(), updated.getSysUserId()));
        }
    }

    /**
     * Create default branding configuration
     * 
     * @return SiteBranding entity with default values
     */
    private SiteBranding createDefaultBranding() {
        SiteBranding branding = new SiteBranding();
        branding.setPrimaryColor("#1d4ed8");
        branding.setSecondaryColor("#64748b");
        branding.setAccentColor("#0891b2");
        branding.setColorMode("light");
        branding.setUseHeaderLogoForLogin(false);
        branding.setLastupdatedFields();
        // sysUserId will be set when branding is first saved
        
        // Insert default record
        String id = siteBrandingDAO.insert(branding);
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

        // Validate file format
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return false;
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        for (String allowedFormat : ALLOWED_FORMATS) {
            if (extension.equals(allowedFormat)) {
                return true;
            }
        }

        return false;
    }

    @Override
    @Transactional
    public String uploadLogo(MultipartFile file, LogoType type) throws IOException {
        // Validate file
        if (!validateLogoFile(file)) {
            throw new LIMSRuntimeException("Invalid logo file: format must be PNG, SVG, or JPG/JPEG, size must be <= 2MB");
        }

        // Ensure branding directory exists
        Path brandingDir = Paths.get(BRANDING_DIR);
        if (!Files.exists(brandingDir)) {
            Files.createDirectories(brandingDir);
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
        Path filePath = brandingDir.resolve(filename);

        // Save file
        file.transferTo(filePath.toFile());

        // Update branding entity with file path
        String fullPath = filePath.toAbsolutePath().toString();
        setLogoPath(branding, type, fullPath);

        // Save branding
        saveBranding(branding);

        // Task Reference: T093 - Log logo upload for audit trail
        LogEvent.logInfo("SiteBrandingService", "uploadLogo", 
            String.format("Logo uploaded - Type: %s, File: %s, Size: %d bytes, User: %s", 
                type.getValue(), file.getOriginalFilename(), file.getSize(), branding.getSysUserId()));

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
     * Remove logo file and update branding configuration
     * Task Reference: T063
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
        LogEvent.logInfo("SiteBrandingService", "removeLogo", 
            String.format("Logo removed - Type: %s, File: %s, User: %s", 
                type.getValue(), logoPath, branding.getSysUserId()));
    }

    /**
     * Reset all branding to default values
     * Task Reference: T066
     */
    @Override
    @Transactional
    public void resetToDefaults() throws IOException {
        // Get current branding
        SiteBranding branding = getBranding();

        // Delete all logo files
        String[] logoPaths = {
            branding.getHeaderLogoPath(),
            branding.getLoginLogoPath(),
            branding.getFaviconPath()
        };

        for (String logoPath : logoPaths) {
            if (logoPath != null) {
                try {
                    Path filePath = Paths.get(logoPath);
                    if (Files.exists(filePath)) {
                        Files.delete(filePath);
                    }
                } catch (IOException e) {
                    LogEvent.logError("Error deleting logo file during reset: " + logoPath, e);
                    // Continue with other files even if one fails
                }
            }
        }

        // Reset all fields to defaults
        branding.setHeaderLogoPath(null);
        branding.setLoginLogoPath(null);
        branding.setFaviconPath(null);
        branding.setUseHeaderLogoForLogin(false);
        branding.setPrimaryColor("#1d4ed8");
        branding.setSecondaryColor("#64748b");
        branding.setAccentColor("#0891b2");
        branding.setColorMode("light");

        // Save branding
        saveBranding(branding);
        
        // Task Reference: T093 - Log reset action for audit trail
        LogEvent.logInfo("SiteBrandingService", "resetToDefaults", 
            "All branding reset to defaults by user: " + branding.getSysUserId());
    }
}

