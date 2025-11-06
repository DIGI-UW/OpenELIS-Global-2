package org.openelisglobal.common.service.servlet.reports;

import org.openelisglobal.image.service.ImageService;
import org.openelisglobal.image.valueholder.Image;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoUploadServiceImpl implements LogoUploadService {

    @Autowired
    private ImageService imageService;
    @Autowired
    private SiteInformationService siteInformationService;

    @Override
    @Transactional
    public void removeImage(Image image, SiteInformation logoInformation) {
        imageService.delete(image);
        logoInformation.setValue("");
        logoInformation.setSysUserId("1");
        siteInformationService.update(logoInformation);
    }

    @Override
    @Transactional
    public void saveImage(Image image, boolean newImage, String imageId, SiteInformation logoInformation) {
        if (!newImage) {
            // The reason the old image is deleted and a new one added is because updating
            // the image doesn't work.
            image.setId(imageId);
            imageService.delete(image);
        }
        Image savedImage = imageService.save(image);

        logoInformation.setValue(savedImage.getId());
        logoInformation.setSysUserId("1");
        siteInformationService.update(logoInformation);
    }
}
