package org.openelisglobal.sitebranding.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.GenericGenerator;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * SiteBranding entity - Represents the organization's branding configuration
 * Single record per OpenELIS deployment
 * 
 * Task Reference: T011
 */
@Entity
@Table(name = "site_branding")
public class SiteBranding extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "uuid-generator")
    @GenericGenerator(name = "uuid-generator", strategy = "uuid2")
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "header_logo_path", length = 500)
    private String headerLogoPath;

    @Column(name = "login_logo_path", length = 500)
    private String loginLogoPath;

    @Column(name = "use_header_logo_for_login", nullable = false)
    private Boolean useHeaderLogoForLogin = false;

    @Column(name = "favicon_path", length = 500)
    private String faviconPath;

    @Column(name = "primary_color", length = 7, nullable = false)
    private String primaryColor = "#1d4ed8";

    @Column(name = "secondary_color", length = 7, nullable = false)
    private String secondaryColor = "#64748b";

    @Column(name = "accent_color", length = 7, nullable = false)
    private String accentColor = "#0891b2";

    @Column(name = "color_mode", length = 10, nullable = false)
    private String colorMode = "light";

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getHeaderLogoPath() {
        return headerLogoPath;
    }

    public void setHeaderLogoPath(String headerLogoPath) {
        this.headerLogoPath = headerLogoPath;
    }

    public String getLoginLogoPath() {
        return loginLogoPath;
    }

    public void setLoginLogoPath(String loginLogoPath) {
        this.loginLogoPath = loginLogoPath;
    }

    public Boolean getUseHeaderLogoForLogin() {
        return useHeaderLogoForLogin;
    }

    public void setUseHeaderLogoForLogin(Boolean useHeaderLogoForLogin) {
        this.useHeaderLogoForLogin = useHeaderLogoForLogin;
    }

    public String getFaviconPath() {
        return faviconPath;
    }

    public void setFaviconPath(String faviconPath) {
        this.faviconPath = faviconPath;
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

    public String getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(String accentColor) {
        this.accentColor = accentColor;
    }

    public String getColorMode() {
        return colorMode;
    }

    public void setColorMode(String colorMode) {
        this.colorMode = colorMode;
    }
}

