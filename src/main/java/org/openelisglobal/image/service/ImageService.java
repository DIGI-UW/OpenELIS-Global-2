package org.openelisglobal.image.service;

import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.image.valueholder.Image;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ImageService extends BaseObjectService<Image, String> {

    @PreAuthorize("hasAuthority('PRIV_SITEINFO_VIEW')")
    String getFullPreviewPath();

    @PreAuthorize("hasAuthority('PRIV_SITEINFO_VIEW')")
    String getImageNameFilePath(String imageName);

    @PreAuthorize("hasAuthority('PRIV_SITEINFO_VIEW')")
    Image getImageByDescription(String imageDescription);

    @PreAuthorize("hasAuthority('PRIV_SITEINFO_VIEW')")
    Optional<Image> getImageBySiteInfoName(String imageName);
}
