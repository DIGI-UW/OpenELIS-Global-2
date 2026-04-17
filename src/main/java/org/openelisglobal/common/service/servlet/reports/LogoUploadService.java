package org.openelisglobal.common.service.servlet.reports;

import org.openelisglobal.image.valueholder.Image;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.security.access.prepost.PreAuthorize;

public interface LogoUploadService {

    @PreAuthorize("hasAuthority('PRIV_BRANDING_MANAGE')")
    void removeImage(Image image, SiteInformation logoInformation);

    @PreAuthorize("hasAuthority('PRIV_BRANDING_MANAGE')")
    void saveImage(Image image, boolean newImage, String imageId, SiteInformation logoInformation);
}
