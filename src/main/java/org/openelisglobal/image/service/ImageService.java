package org.openelisglobal.image.service;

import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.image.valueholder.Image;

public interface ImageService extends BaseObjectService<Image, String> {

    String getFullPreviewPath();

    String getImageNameFilePath(String imageName);

    Image getImageByDescription(String imageDescription);

    Optional<Image> getImageBySiteInfoName(String imageName);
}
