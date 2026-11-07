package org.openelisglobal.common.service.servlet.reports;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
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

    /**
     * Decoding allocates a raster of the declared size, so the size is read first.
     */
    private static final int MAX_LOGO_PIXELS = 2048;

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
            Dimension declared = declaredSize(bytes);
            if (declared != null && (declared.width > MAX_LOGO_PIXELS || declared.height > MAX_LOGO_PIXELS)) {
                throw new IllegalArgumentException(
                        "Logo must be at most " + MAX_LOGO_PIXELS + "\u00d7" + MAX_LOGO_PIXELS + " pixels");
            }
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

    /** The size the image header declares, read without decoding the pixels. */
    private static Dimension declaredSize(byte[] bytes) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                return new Dimension(reader.getWidth(0), reader.getHeight(0));
            } finally {
                reader.dispose();
            }
        }
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
