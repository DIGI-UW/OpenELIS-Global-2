package org.openelisglobal.sitebranding.form;

import jakarta.validation.constraints.Size;

/**
 * Form object for SiteBranding entity - used for REST API input/output
 * 
 * Task Reference: T016
 */
public class SiteBrandingForm {

    private Integer id;

    private String headerLogoUrl;

    private String loginLogoUrl;

    private Boolean useHeaderLogoForLogin = false;

    private String faviconUrl;

    @Size(max = 50, message = "Header color must not exceed 50 characters")
    private String headerColor;

    @Size(max = 50, message = "Primary color must not exceed 50 characters")
    private String primaryColor;

    @Size(max = 50, message = "Secondary color must not exceed 50 characters")
    private String secondaryColor;

    @Size(max = 10, message = "Color mode must not exceed 10 characters")
    private String colorMode;

    private String lastModified;

    private String lastModifiedBy;

    // Getters and Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getHeaderLogoUrl() {
        return headerLogoUrl;
    }

    public void setHeaderLogoUrl(String headerLogoUrl) {
        this.headerLogoUrl = headerLogoUrl;
    }

    public String getLoginLogoUrl() {
        return loginLogoUrl;
    }

    public void setLoginLogoUrl(String loginLogoUrl) {
        this.loginLogoUrl = loginLogoUrl;
    }

    public Boolean getUseHeaderLogoForLogin() {
        return useHeaderLogoForLogin;
    }

    public void setUseHeaderLogoForLogin(Boolean useHeaderLogoForLogin) {
        this.useHeaderLogoForLogin = useHeaderLogoForLogin;
    }

    public String getFaviconUrl() {
        return faviconUrl;
    }

    public void setFaviconUrl(String faviconUrl) {
        this.faviconUrl = faviconUrl;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(String secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public String getHeaderColor() {
        return headerColor;
    }

    public void setHeaderColor(String headerColor) {
        this.headerColor = headerColor;
    }

    public String getColorMode() {
        return colorMode;
    }

    public void setColorMode(String colorMode) {
        this.colorMode = colorMode;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }
}
