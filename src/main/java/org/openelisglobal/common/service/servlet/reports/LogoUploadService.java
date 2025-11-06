package org.openelisglobal.common.service.servlet.reports;

import org.openelisglobal.image.valueholder.Image;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;

public interface LogoUploadService {

    void removeImage(Image image, SiteInformation logoInformation);

    void saveImage(Image image, boolean newImage, String imageId, SiteInformation logoInformation);
}
