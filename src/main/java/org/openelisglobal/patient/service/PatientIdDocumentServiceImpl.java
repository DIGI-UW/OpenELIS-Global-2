package org.openelisglobal.patient.service;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.patient.dao.PatientIdDocumentDAO;
import org.openelisglobal.patient.valueholder.PatientIdDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientIdDocumentServiceImpl extends AuditableBaseObjectServiceImpl<PatientIdDocument, Integer>
        implements PatientIdDocumentService {

    @Autowired
    protected PatientIdDocumentDAO baseObjectDAO;

    private static final int THUMBNAIL_SIZE = 150;

    public PatientIdDocumentServiceImpl() {
        super(PatientIdDocument.class);
        this.auditTrailLog = true;
    }

    @Override
    protected PatientIdDocumentDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Transactional
    @Override
    public PatientIdDocument saveDocument(String patientId, String documentBase64, String documentCategory,
            String description, String sysUserId) throws LIMSRuntimeException {

        if (documentBase64 == null || documentBase64.isEmpty()) {
            return null;
        }

        String documentType = extractDocumentType(documentBase64);
        String cleanBase64 = cleanBase64Data(documentBase64);
        String thumbnail = createThumbnail(cleanBase64);

        PatientIdDocument document = new PatientIdDocument();
        document.setPatientId(patientId);
        document.setDocumentData(cleanBase64);
        document.setThumbnailData(thumbnail != null ? thumbnail : cleanBase64);
        document.setDocumentType(documentType);
        document.setDocumentCategory(documentCategory != null ? documentCategory : "OTHER");
        document.setDescription(description);
        document.setDeleted(false);
        document.setSysUserId(sysUserId);

        insert(document);
        return document;
    }

    @Override
    public List<PatientIdDocument> getDocumentsByPatientId(String patientId) throws LIMSRuntimeException {
        if (patientId == null) {
            return Collections.emptyList();
        }
        return baseObjectDAO.getByPatientId(patientId);
    }

    @Transactional
    @Override
    public void softDeleteDocument(Integer documentId, String sysUserId) throws LIMSRuntimeException {
        Optional<PatientIdDocument> optDoc = getMatch("id", documentId);
        if (optDoc.isPresent()) {
            PatientIdDocument doc = optDoc.get();
            doc.setDeleted(true);
            doc.setSysUserId(sysUserId);
            update(doc);
        }
    }

    @Transactional
    @Override
    public PatientIdDocument updateDocumentCategory(Integer documentId, String documentCategory, String description,
            String sysUserId) throws LIMSRuntimeException {
        return updateDocument(documentId, null, documentCategory, description, sysUserId);
    }

    @Transactional
    @Override
    public PatientIdDocument updateDocument(Integer documentId, String documentBase64, String documentCategory,
            String description, String sysUserId) throws LIMSRuntimeException {
        Optional<PatientIdDocument> optDoc = getMatch("id", documentId);
        if (optDoc.isEmpty()) {
            return null;
        }
        PatientIdDocument doc = optDoc.get();
        doc.setDocumentCategory(documentCategory);
        doc.setDescription(description);
        if (documentBase64 != null && !documentBase64.isEmpty()) {
            String documentType = extractDocumentType(documentBase64);
            String cleanBase64 = cleanBase64Data(documentBase64);
            String thumbnail = createThumbnail(cleanBase64);
            doc.setDocumentData(cleanBase64);
            doc.setThumbnailData(thumbnail != null ? thumbnail : cleanBase64);
            doc.setDocumentType(documentType);
        }
        doc.setSysUserId(sysUserId);
        update(doc);
        return doc;
    }

    private String extractDocumentType(String base64Data) {
        if (base64Data.startsWith("data:image/")) {
            String[] parts = base64Data.split(";");
            return parts[0].replace("data:", "");
        }
        if (base64Data.startsWith("data:application/pdf")) {
            return "application/pdf";
        }
        return "image/jpeg";
    }

    private String cleanBase64Data(String base64Data) {
        if (base64Data.contains(",")) {
            return base64Data.split(",")[1];
        }
        return base64Data;
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
            return null;
        }
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
