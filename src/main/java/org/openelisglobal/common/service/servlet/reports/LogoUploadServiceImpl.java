package org.openelisglobal.common.service.servlet.reports;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.image.service.ImageService;
import org.openelisglobal.image.valueholder.Image;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LogoUploadServiceImpl implements LogoUploadService {

    private static final long MAX_LOGO_BYTES = 500L * 1024L;

    private static final int MIN_LOGO_PIXELS = 64;

    @Autowired
    private ImageService imageService;
    @Autowired
    private SiteInformationService siteInformationService;

    @Override
    public byte[] validateLogo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A logo file is required");
        }
        if (file.getSize() > MAX_LOGO_BYTES) {
            throw new IllegalArgumentException("File exceeds 500 KB");
        }
        byte[] bytes;
        BufferedImage decoded;
        try {
            bytes = file.getBytes();
            decoded = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            LogEvent.logError(e);
            throw new IllegalArgumentException("Could not read the uploaded file");
        }
        if (decoded == null) {
            throw new IllegalArgumentException("Logo must be a PNG or JPEG image (SVG is not supported on reports)");
        }
        if (decoded.getWidth() < MIN_LOGO_PIXELS || decoded.getHeight() < MIN_LOGO_PIXELS) {
            throw new IllegalArgumentException(
                    "Logo must be at least " + MIN_LOGO_PIXELS + "\u00d7" + MIN_LOGO_PIXELS + " pixels");
        }
        return bytes;
    }

    @Override
    @Transactional
    public void removeImage(Image image, SiteInformation logoInformation) {
        imageService.delete(image);
        logoInformation.setValue("");

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

        siteInformationService.update(logoInformation);
    }
}
