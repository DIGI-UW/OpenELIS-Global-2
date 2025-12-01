package org.openelisglobal.sitebranding.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.sitebranding.form.SiteBrandingForm;
import org.openelisglobal.sitebranding.service.LogoType;
import org.openelisglobal.sitebranding.service.SiteBrandingService;
import org.openelisglobal.sitebranding.valueholder.SiteBranding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller for Site Branding configuration
 * 
 * Task Reference: T017
 */
@RestController
@RequestMapping("/rest/site-branding")
public class SiteBrandingRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(SiteBrandingRestController.class);

    @Autowired
    private SiteBrandingService siteBrandingService;

    @Autowired
    private UserModuleService userModuleService;

    /**
     * Helper method to check admin status
     * 
     * @param request HTTP request containing session information
     * @return true if user is admin, false otherwise
     */
    private boolean checkAdminStatus(HttpServletRequest request) {
        try {
            return userModuleService.isUserAdmin(request);
        } catch (Exception e) {
            logger.debug("Could not determine admin status, treating as non-admin: " + e.getMessage());
            return false;
        }
    }

    /**
     * GET /rest/site-branding/ - Get current branding configuration
     * Returns default values if no custom branding is configured
     */
    @GetMapping("/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SiteBrandingForm> getBranding() {
        try {
            SiteBranding branding = siteBrandingService.getBranding();
            SiteBrandingForm form = entityToForm(branding);
            return ResponseEntity.ok(form);
        } catch (Exception e) {
            logger.error("Error getting branding configuration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PUT /rest/site-branding/ - Update branding configuration
     */
    @PutMapping("/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateBranding(@Valid @RequestBody SiteBrandingForm form, HttpServletRequest request) {
        try {
            // Get existing branding or create new
            SiteBranding branding = siteBrandingService.getBranding();
            
            // Update fields from form
            if (form.getPrimaryColor() != null) {
                branding.setPrimaryColor(form.getPrimaryColor());
            }
            if (form.getSecondaryColor() != null) {
                branding.setSecondaryColor(form.getSecondaryColor());
            }
            if (form.getAccentColor() != null) {
                branding.setAccentColor(form.getAccentColor());
            }
            if (form.getColorMode() != null) {
                branding.setColorMode(form.getColorMode());
            }
            if (form.getUseHeaderLogoForLogin() != null) {
                branding.setUseHeaderLogoForLogin(form.getUseHeaderLogoForLogin());
            }

            // Set sysUserId from request
            String sysUserId = getSysUserId(request);
            if (sysUserId != null) {
                branding.setSysUserId(sysUserId);
            }

            // Save branding
            SiteBranding saved = siteBrandingService.saveBranding(branding);
            SiteBrandingForm response = entityToForm(saved);
            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error updating branding configuration: " + e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error updating branding configuration", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Convert entity to form DTO
     */
    private SiteBrandingForm entityToForm(SiteBranding branding) {
        SiteBrandingForm form = new SiteBrandingForm();
        form.setId(branding.getId());
        form.setHeaderLogoUrl(branding.getHeaderLogoPath() != null ? "/rest/site-branding/logo/header" : null);
        form.setLoginLogoUrl(branding.getLoginLogoPath() != null ? "/rest/site-branding/logo/login" : null);
        form.setUseHeaderLogoForLogin(branding.getUseHeaderLogoForLogin());
        form.setFaviconUrl(branding.getFaviconPath() != null ? "/rest/site-branding/logo/favicon" : null);
        form.setPrimaryColor(branding.getPrimaryColor());
        form.setSecondaryColor(branding.getSecondaryColor());
        form.setAccentColor(branding.getAccentColor());
        form.setColorMode(branding.getColorMode());
        
        if (branding.getLastupdated() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            form.setLastModified(sdf.format(branding.getLastupdated()));
        }
        form.setLastModifiedBy(branding.getSysUserId());
        
        return form;
    }

    /**
     * POST /rest/site-branding/logo/{type} - Upload logo file
     * Task Reference: T031
     */
    @PostMapping(value = "/logo/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadLogo(
            @PathVariable String type,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        try {
            // Validate logo type
            LogoType logoType;
            try {
                logoType = LogoType.fromString(type);
            } catch (IllegalArgumentException e) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Invalid logo type: " + type);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Validate file
            if (!siteBrandingService.validateLogoFile(file)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Invalid file: format must be PNG, SVG, or JPG/JPEG, size must be <= 2MB");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Get sysUserId for audit
            String sysUserId = getSysUserId(request);
            SiteBranding branding = siteBrandingService.getBranding();
            if (sysUserId != null) {
                branding.setSysUserId(sysUserId);
            }

            // Upload logo
            String filePath = siteBrandingService.uploadLogo(file, logoType);

            // Return response with logo URL
            Map<String, Object> response = new HashMap<>();
            response.put("logoUrl", "/rest/site-branding/logo/" + type);
            response.put("fileName", file.getOriginalFilename());
            response.put("fileSize", file.getSize());

            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error uploading logo: " + e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (IOException e) {
            logger.error("Error saving logo file", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to save logo file");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        } catch (Exception e) {
            logger.error("Error uploading logo", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /rest/site-branding/logo/{type} - Serve logo file
     * Task Reference: T034
     */
    @GetMapping("/logo/{type}")
    public ResponseEntity<Resource> getLogo(@PathVariable String type) {
        try {
            // Validate logo type
            LogoType logoType;
            try {
                logoType = LogoType.fromString(type);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Get logo path
            SiteBranding branding = siteBrandingService.getBranding();
            String logoPath = getLogoPath(branding, logoType);

            if (logoPath == null || !java.nio.file.Files.exists(java.nio.file.Paths.get(logoPath))) {
                // Return 404 - frontend should fall back to default logo
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Serve file
            Resource resource = new FileSystemResource(logoPath);
            String contentType = getContentType(logoPath);

            // Task Reference: T102 - Optimize logo file serving with caching headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("inline", resource.getFilename());
            headers.setCacheControl("public, max-age=3600"); // Cache for 1 hour
            headers.setETag("\"" + logoPath.hashCode() + "\""); // Simple ETag based on path

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error serving logo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
     * Get content type based on file extension
     */
    private String getContentType(String filePath) {
        String lowerPath = filePath.toLowerCase();
        if (lowerPath.endsWith(".png")) {
            return "image/png";
        } else if (lowerPath.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerPath.endsWith(".ico")) {
            return "image/x-icon";
        }
        return "application/octet-stream";
    }

    /**
     * DELETE /rest/site-branding/logo/{type} - Remove logo file
     * Task Reference: T062
     */
    @DeleteMapping("/logo/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> removeLogo(@PathVariable String type, HttpServletRequest request) {
        try {
            // Validate logo type
            LogoType logoType;
            try {
                logoType = LogoType.fromString(type);
            } catch (IllegalArgumentException e) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Invalid logo type: " + type);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Get sysUserId for audit
            String sysUserId = getSysUserId(request);
            SiteBranding branding = siteBrandingService.getBranding();
            if (sysUserId != null) {
                branding.setSysUserId(sysUserId);
            }

            // Remove logo
            siteBrandingService.removeLogo(logoType);

            // Return updated branding
            SiteBranding updatedBranding = siteBrandingService.getBranding();
            SiteBrandingForm response = entityToForm(updatedBranding);
            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error removing logo: " + e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (IOException e) {
            logger.error("Error deleting logo file", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to delete logo file");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        } catch (Exception e) {
            logger.error("Error removing logo", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /rest/site-branding/reset - Reset all branding to defaults
     * Task Reference: T067
     */
    @PostMapping("/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resetBranding(HttpServletRequest request) {
        try {
            // Get sysUserId for audit
            String sysUserId = getSysUserId(request);
            SiteBranding branding = siteBrandingService.getBranding();
            if (sysUserId != null) {
                branding.setSysUserId(sysUserId);
            }

            // Reset to defaults
            siteBrandingService.resetToDefaults();

            // Return updated branding
            SiteBranding resetBranding = siteBrandingService.getBranding();
            SiteBrandingForm response = entityToForm(resetBranding);
            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error resetting branding: " + e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (IOException e) {
            logger.error("Error deleting logo files during reset", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to delete logo files");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        } catch (Exception e) {
            logger.error("Error resetting branding", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

