package org.openelisglobal.patient.service;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.patient.dao.PatientPhotoDAO;
import org.openelisglobal.patient.valueholder.PatientPhoto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientPhotoServiceImpl extends AuditableBaseObjectServiceImpl<PatientPhoto, Integer>
        implements PatientPhotoService {

    @Autowired
    protected PatientPhotoDAO baseObjectDAO;

    private static final int THUMBNAIL_SIZE = 100;

    public PatientPhotoServiceImpl() {
        super(PatientPhoto.class);
    }

    @Override
    protected PatientPhotoDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Transactional
    @Override
    public PatientPhoto savePhoto(String patientId, String photoBase64) throws LIMSRuntimeException {

        if (!photoBase64.isEmpty()) {
            String photoType = extractPhotoType(photoBase64);
            String cleanBase64 = cleanBase64Data(photoBase64);
            String thumbnail = createThumbnail(cleanBase64);

            PatientPhoto existingPhoto = baseObjectDAO.getByPatientId(patientId);

            PatientPhoto patientPhoto;
            if (existingPhoto != null) {
                patientPhoto = existingPhoto;
                patientPhoto.setPhotoData(cleanBase64);
                patientPhoto.setThumbnailData(thumbnail);
                patientPhoto.setPhotoType(photoType);
                update(patientPhoto);
            } else {
                patientPhoto = new PatientPhoto();
                patientPhoto.setPatientId(patientId);
                patientPhoto.setPhotoData(cleanBase64);
                patientPhoto.setThumbnailData(thumbnail);
                patientPhoto.setPhotoType(photoType);
                insert(patientPhoto);
            }

            return patientPhoto;
        }
        return null;
    }

    private String extractPhotoType(String photoBase64) {
        if (photoBase64.startsWith("data:image/")) {
            String[] parts = photoBase64.split(";");
            return parts[0].replace("data:", "");
        }
        return "image/jpeg";
    }

    private String cleanBase64Data(String photoBase64) {
        if (photoBase64.contains(",")) {
            return photoBase64.split(",")[1];
        }
        return photoBase64;
    }

    private String createThumbnail(String base64Data) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
            BufferedImage originalImage = ImageIO.read(bais);

            if (originalImage == null) {
                return null;
            }

            BufferedImage thumbnail = resizeImage(originalImage, THUMBNAIL_SIZE, THUMBNAIL_SIZE);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "jpg", baos);
            byte[] thumbnailBytes = baos.toByteArray();

            return Base64.getEncoder().encodeToString(thumbnailBytes);

        } catch (Exception e) {
            return base64Data;
        }
    }

    @Override
    public String getPhotoByPatientId(String patientId, boolean isThumbnail) {
        if (patientId == null) {
            return null;
        }
        PatientPhoto photo = baseObjectDAO.getByPatientId(patientId);
        if (!isThumbnail) {
            return photo != null ? "data:" + photo.getPhotoType() + ";base64," + photo.getPhotoData() : null;
        }
        return photo != null ? photo.getThumbnailData() : null;
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int maxWidth, int maxHeight) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        double ratio = Math.min((double) maxWidth / originalWidth, (double) maxHeight / originalHeight);

        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.drawImage(originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();

        return resizedImage;
    }

}
