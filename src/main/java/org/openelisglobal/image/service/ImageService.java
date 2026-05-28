package org.openelisglobal.image.service;

import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.image.valueholder.Image;

@CrossDomainService(callers = "pre-login branding (login page, all users), SiteBrandingService — all methods are public reads; writes go through BaseObjectService guarded at caller")
public interface ImageService extends BaseObjectService<Image, String> {

    String getFullPreviewPath();

    String getImageNameFilePath(String imageName);

    Image getImageByDescription(String imageDescription);

    Optional<Image> getImageBySiteInfoName(String imageName);
}
