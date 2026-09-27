package org.openelisglobal.common.service.servlet.reports;

import org.openelisglobal.image.valueholder.Image;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.web.multipart.MultipartFile;

public interface LogoUploadService {

    void removeImage(Image image, SiteInformation logoInformation);

    void saveImage(Image image, boolean newImage, String imageId, SiteInformation logoInformation);

    /**
     * The uploaded logo's bytes, once it is known to be usable on a report: a
     * raster image that actually decodes, at most 500 KB, and at least 64x64.
     *
     * <p>
     * Decoding is what rejects an SVG. JasperReports has no SVG renderer on this
     * classpath (no Batik), so an SVG would upload cleanly and then render as an
     * empty logo slot on every report — this is the only place that failure is
     * visible.
     *
     * @throws IllegalArgumentException with a message meant for the uploader
     */
    byte[] validateLogo(MultipartFile file);
}
