package org.openelisglobal.sitebranding.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.sitebranding.form.SiteBrandingForm;
import org.openelisglobal.sitebranding.service.LogoType;
import org.openelisglobal.sitebranding.service.SiteBrandingService;
import org.openelisglobal.sitebranding.valueholder.SiteBranding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
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

    /**
     * GET /rest/site-branding - Get current branding configuration Returns default
     * values if no custom branding is configured.
     *
     * No authentication required - branding must be visible to all users including
     * the login page.
     */
    @GetMapping(value = { "/", "" })
    public ResponseEntity<SiteBrandingForm> getBranding() {
        logger.debug("GET /rest/site-branding/ - Request received");
        try {
            SiteBranding branding = siteBrandingService.getBranding();
            logger.debug(
                    "Retrieved branding: id={}, primaryColor={}, secondaryColor={}, headerColor={}, colorMode={}, useHeaderLogoForLogin={}",
                    branding.getId(), branding.getPrimaryColor(), branding.getSecondaryColor(),
                    branding.getHeaderColor(), branding.getColorMode(), branding.getUseHeaderLogoForLogin());
            SiteBrandingForm form = entityToForm(branding);
            logger.debug(
                    "Returning branding form: id={}, primaryColor={}, secondaryColor={}, headerColor={}, colorMode={}, useHeaderLogoForLogin={}, headerLogoUrl={}, loginLogoUrl={}, faviconUrl={}",
                    form.getId(), form.getPrimaryColor(), form.getSecondaryColor(), form.getHeaderColor(),
                    form.getColorMode(), form.getUseHeaderLogoForLogin(), form.getHeaderLogoUrl(),
                    form.getLoginLogoUrl(), form.getFaviconUrl());
            return ResponseEntity.ok(form);
        } catch (Exception e) {
            logger.error("Error getting branding configuration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PUT /rest/site-branding - Update branding configuration
     */
    @PutMapping(value = { "/", "" })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateBranding(@Valid @RequestBody SiteBrandingForm form, HttpServletRequest request) {
        logger.info("PUT /rest/site-branding/ - Update request received");
        logger.debug(
                "Incoming form data: id={}, primaryColor={}, secondaryColor={}, headerColor={}, colorMode={}, useHeaderLogoForLogin={}",
                form.getId(), form.getPrimaryColor(), form.getSecondaryColor(), form.getHeaderColor(),
                form.getColorMode(), form.getUseHeaderLogoForLogin());

        try {
            // Get existing branding or create new
            SiteBranding branding = siteBrandingService.getBranding();
            logger.debug(
                    "Existing branding before update: id={}, primaryColor={}, secondaryColor={}, headerColor={}, colorMode={}, useHeaderLogoForLogin={}",
                    branding.getId(), branding.getPrimaryColor(), branding.getSecondaryColor(),
                    branding.getHeaderColor(), branding.getColorMode(), branding.getUseHeaderLogoForLogin());

            // Update fields from form (only if not null and not empty)
            boolean changed = false;
            if (form.getPrimaryColor() != null && !form.getPrimaryColor().trim().isEmpty()) {
                String newColor = form.getPrimaryColor().trim();
                if (!newColor.equals(branding.getPrimaryColor())) {
                    logger.debug("Updating primaryColor: {} -> {}", branding.getPrimaryColor(), newColor);
                    branding.setPrimaryColor(newColor);
                    changed = true;
                }
            }
            if (form.getSecondaryColor() != null && !form.getSecondaryColor().trim().isEmpty()) {
                String newColor = form.getSecondaryColor().trim();
                if (!newColor.equals(branding.getSecondaryColor())) {
                    logger.debug("Updating secondaryColor: {} -> {}", branding.getSecondaryColor(), newColor);
                    branding.setSecondaryColor(newColor);
                    changed = true;
                }
            }
            if (form.getHeaderColor() != null && !form.getHeaderColor().trim().isEmpty()) {
                String newColor = form.getHeaderColor().trim();
                if (!newColor.equals(branding.getHeaderColor())) {
                    logger.debug("Updating headerColor: {} -> {}", branding.getHeaderColor(), newColor);
                    branding.setHeaderColor(newColor);
                    changed = true;
                }
            }
            if (form.getColorMode() != null && !form.getColorMode().trim().isEmpty()) {
                String newMode = form.getColorMode().trim();
                if (!newMode.equals(branding.getColorMode())) {
                    logger.debug("Updating colorMode: {} -> {}", branding.getColorMode(), newMode);
                    branding.setColorMode(newMode);
                    changed = true;
                }
            }
            if (form.getUseHeaderLogoForLogin() != null) {
                Boolean newValue = form.getUseHeaderLogoForLogin();
                if (!newValue.equals(branding.getUseHeaderLogoForLogin())) {
                    logger.debug("Updating useHeaderLogoForLogin: {} -> {}", branding.getUseHeaderLogoForLogin(),
                            newValue);
                    branding.setUseHeaderLogoForLogin(newValue);
                    changed = true;
                }
            }

            if (!changed) {
                logger.debug("No changes detected in update request");
            }

            // Set sysUserId from request
            String sysUserId = getSysUserId(request);
            logger.debug("sysUserId from request: {}", sysUserId);
            if (sysUserId != null) {
                branding.setSysUserId(sysUserId);
            }

            // Save branding
            logger.debug("Calling siteBrandingService.saveBranding() with branding id={}", branding.getId());
            SiteBranding saved = siteBrandingService.saveBranding(branding);
            logger.info(
                    "Branding saved successfully: id={}, primaryColor={}, secondaryColor={}, headerColor={}, colorMode={}, useHeaderLogoForLogin={}",
                    saved.getId(), saved.getPrimaryColor(), saved.getSecondaryColor(), saved.getHeaderColor(),
                    saved.getColorMode(), saved.getUseHeaderLogoForLogin());

            SiteBrandingForm response = entityToForm(saved);
            logger.debug(
                    "Returning response: id={}, primaryColor={}, secondaryColor={}, headerColor={}, colorMode={}, useHeaderLogoForLogin={}",
                    response.getId(), response.getPrimaryColor(), response.getSecondaryColor(),
                    response.getHeaderColor(), response.getColorMode(), response.getUseHeaderLogoForLogin());
            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            logger.error("LIMSRuntimeException updating branding configuration: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Unexpected error updating branding configuration", e);
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
        form.setHeaderColor(branding.getHeaderColor());
        form.setColorMode(branding.getColorMode());

        if (branding.getLastupdated() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            form.setLastModified(sdf.format(branding.getLastupdated()));
        }
        form.setLastModifiedBy(branding.getSysUserId());

        return form;
    }

    /**
     * POST /rest/site-branding/logo/{type} - Upload logo file Task Reference: T031
     */
    @PostMapping(value = "/logo/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadLogo(@PathVariable String type, @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        logger.info("POST /rest/site-branding/logo/{} - Logo upload request received", type);
        logger.debug("File details: name={}, size={} bytes, contentType={}", file.getOriginalFilename(), file.getSize(),
                file.getContentType());

        try {
            // Validate logo type
            LogoType logoType;
            try {
                logoType = LogoType.fromString(type);
                logger.debug("Logo type validated: {}", logoType);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid logo type requested: {}", type);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Invalid logo type: " + type);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Validate file
            if (!siteBrandingService.validateLogoFile(file)) {
                logger.warn("File validation failed: name={}, size={}, contentType={}", file.getOriginalFilename(),
                        file.getSize(), file.getContentType());
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Invalid file: format must be PNG, SVG, or JPG/JPEG, size must be <= 2MB");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            logger.debug("File validation passed");

            // Get sysUserId for audit
            String sysUserId = getSysUserId(request);
            logger.debug("sysUserId from request: {}", sysUserId);
            SiteBranding branding = siteBrandingService.getBranding();
            if (sysUserId != null) {
                branding.setSysUserId(sysUserId);
            }

            // Upload logo
            logger.debug("Calling siteBrandingService.uploadLogo() for type: {}", logoType);
            String filePath = siteBrandingService.uploadLogo(file, logoType);
            logger.info("Logo uploaded successfully: type={}, filePath={}, fileName={}, fileSize={}", logoType,
                    filePath, file.getOriginalFilename(), file.getSize());

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
     * GET /rest/site-branding/logo/{type} - Serve logo file Task Reference: T034
     */
    @GetMapping("/logo/{type}")
    public ResponseEntity<Resource> getLogo(@PathVariable String type, WebRequest request) {
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

            // Calculate ETag and Last-Modified from file modification time
            long lastModified = java.nio.file.Files.getLastModifiedTime(java.nio.file.Paths.get(logoPath)).toMillis();
            String etag = "\"" + lastModified + "\"";

            // Check If-None-Match and If-Modified-Since headers - return 304 if cached
            // version is current
            if (request.checkNotModified(etag, lastModified)) {
                return null;
            }

            // Serve file
            Resource resource = new FileSystemResource(logoPath);
            MediaType contentType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(contentType);
            headers.setContentDispositionFormData("inline", resource.getFilename());
            headers.setCacheControl("public, max-age=300");
            headers.setETag(etag);
            headers.setLastModified(lastModified);
            headers.set("X-Content-Type-Options", "nosniff");

            return ResponseEntity.ok().headers(headers).body(resource);
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
     * DELETE /rest/site-branding/logo/{type} - Remove logo file Task Reference:
     * T062
     */
    @DeleteMapping("/logo/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> removeLogo(@PathVariable String type, HttpServletRequest request) {
        logger.info("DELETE /rest/site-branding/logo/{} - Logo removal request received", type);

        try {
            // Validate logo type
            LogoType logoType;
            try {
                logoType = LogoType.fromString(type);
                logger.debug("Logo type validated: {}", logoType);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid logo type requested for removal: {}", type);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Invalid logo type: " + type);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Get sysUserId for audit
            String sysUserId = getSysUserId(request);
            logger.debug("sysUserId from request: {}", sysUserId);
            SiteBranding branding = siteBrandingService.getBranding();
            logger.debug("Current branding before removal: headerLogoPath={}, loginLogoPath={}, faviconPath={}",
                    branding.getHeaderLogoPath(), branding.getLoginLogoPath(), branding.getFaviconPath());

            if (sysUserId != null) {
                branding.setSysUserId(sysUserId);
            }

            // Remove logo
            logger.debug("Calling siteBrandingService.removeLogo() for type: {}", logoType);
            siteBrandingService.removeLogo(logoType);
            logger.info("Logo removed successfully: type={}", logoType);

            // Return updated branding
            SiteBranding updatedBranding = siteBrandingService.getBranding();
            SiteBrandingForm response = entityToForm(updatedBranding);
            logger.debug("Returning updated branding after logo removal");
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
     * POST /rest/site-branding/reset - Reset all branding to defaults Task
     * Reference: T067
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
