package org.openelisglobal.sitebranding.service;

import java.io.IOException;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.sitebranding.valueholder.SiteBranding;

/**
 * Service interface for SiteBranding entity
 * 
 * Task Reference: T014
 */
public interface SiteBrandingService extends BaseObjectService<SiteBranding, Integer> {

    /**
     * Get the current branding configuration Creates a default record if none
     * exists
     * 
     * @return SiteBranding entity with current or default values
     */
    SiteBranding getBranding();

    /**
     * Save branding configuration (insert or update)
     * 
     * @param branding SiteBranding entity to save
     * @return Saved SiteBranding entity
     */
    SiteBranding saveBranding(SiteBranding branding);

    /**
     * Validate color value format. Accepts any non-empty string since CSS color
     * formats include hex codes, named colors (e.g., "rebeccapurple"), rgb(),
     * hsl(), and other formats. CSS will handle invalid colors gracefully by
     * ignoring them.
     *
     * @param color CSS color value to validate
     * @return true if non-empty, false otherwise
     * @deprecated Validation is now permissive; this method returns true for any
     *             non-empty string
     */
    boolean validateColor(String color);

    /**
     * Validate logo file format and size
     * 
     * @param file MultipartFile to validate
     * @return true if valid, false otherwise
     */
    boolean validateLogoFile(org.springframework.web.multipart.MultipartFile file);

    /**
     * Upload logo file and update branding configuration
     * 
     * @param file MultipartFile to upload
     * @param type LogoType (HEADER, LOGIN, or FAVICON)
     * @return Full filesystem path to uploaded file
     * @throws IOException if file cannot be saved
     */
    String uploadLogo(org.springframework.web.multipart.MultipartFile file, LogoType type) throws IOException;

    /**
     * Get logo URL for serving
     * 
     * @param type LogoType
     * @return URL path to logo or null if default should be used
     */
    String getLogoUrl(LogoType type);

    /**
     * Remove logo file and update branding configuration
     * 
     * @param type LogoType (HEADER, LOGIN, or FAVICON)
     * @throws IOException if file cannot be deleted
     */
    void removeLogo(LogoType type) throws IOException;

    /**
     * Reset all branding to default values Deletes all logo files and resets colors
     * to defaults
     * 
     * @throws IOException if files cannot be deleted
     */
    void resetToDefaults() throws IOException;
}
