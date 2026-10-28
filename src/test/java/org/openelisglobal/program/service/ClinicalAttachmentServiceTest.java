package org.openelisglobal.program.service;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openelisglobal.program.controller.cytology.CytologySampleForm.CytologyReportForm;
import org.openelisglobal.program.controller.cytology.CytologySampleForm.CytologySlideForm;
import org.openelisglobal.program.controller.immunohistochemistry.ImmunohistochemistrySampleForm.ImmunohistochemistryReportForm;
import org.openelisglobal.program.controller.pathology.PathologySampleForm.PathologyReportForm;
import org.openelisglobal.program.controller.pathology.PathologySampleForm.PathologySlideForm;

@RunWith(Parameterized.class)
public class ClinicalAttachmentServiceTest {
    @Parameterized.Parameters(name = "{0}")
    public static Object[] forms() {
        return new Object[] { PathologySlideForm.class, PathologyReportForm.class, CytologySlideForm.class,
                CytologyReportForm.class, ImmunohistochemistryReportForm.class };
    }

    private final Class<? extends ClinicalAttachmentUpload> form;
    private final ObjectMapper mapper = new ObjectMapper();

    public ClinicalAttachmentServiceTest(Class<? extends ClinicalAttachmentUpload> form) {
        this.form = form;
    }

    @Test
    public void supportedFilesPreserveTheirBytesAndCanonicalType() throws Exception {
        for (String type : List.of("image/png", "image/jpeg", "application/pdf")) {
            byte[] bytes = fixture(type);
            ClinicalAttachmentUpload upload = encoded("data:" + type, bytes);
            assertNull("binding does not decode or decide file policy", upload.getImage());
            ClinicalAttachmentService.validate(upload);
            assertArrayEquals(bytes, upload.getImage());
            assertEquals("data:" + type, upload.getFileType());
            // Existing attachments sent back on a case save use the stored fields.
            ClinicalAttachmentUpload stored = mapper
                    .convertValue(Map.of("fileType", upload.getFileType(), "image", bytes), form);
            ClinicalAttachmentService.validate(stored);
            assertArrayEquals(bytes, stored.getImage());
        }
    }

    @Test
    public void htmlSvgAndMarkupInMetadataAreRejected() throws Exception {
        for (String type : List.of("data:text/html", "data:image/svg+xml", "data:image/png\" onload=\"alert(1)")) {
            ClinicalAttachmentUpload upload = encoded(type, fixture("image/png"));
            assertThrows(InvalidClinicalAttachmentException.class, () -> ClinicalAttachmentService.validate(upload));
        }
    }

    @Test
    public void directStoredFieldsCannotBypassValidation() throws Exception {
        ClinicalAttachmentUpload upload = mapper.convertValue(Map.of("fileType", "data:text/html", "image",
                "<script>not an image</script>".getBytes(StandardCharsets.UTF_8)), form);
        assertThrows(InvalidClinicalAttachmentException.class, () -> ClinicalAttachmentService.validate(upload));
    }

    @Test
    public void mislabeledContentAndMalformedBase64AreRejected() throws Exception {
        ClinicalAttachmentUpload mislabeled = encoded("data:application/pdf", fixture("image/png"));
        assertThrows(InvalidClinicalAttachmentException.class, () -> ClinicalAttachmentService.validate(mislabeled));
        for (String value : List.of("data:image/png;base64,???", "data:image/png", "data:image/png;base64,")) {
            ClinicalAttachmentUpload upload = mapper.convertValue(Map.of("base64Image", value), form);
            assertThrows(InvalidClinicalAttachmentException.class, () -> ClinicalAttachmentService.validate(upload));
        }
    }

    @Test
    public void oversizedFilesAreRejectedBeforeDecoding() {
        ClinicalAttachmentUpload upload = mapper
                .convertValue(Map.of("base64Image", "data:image/png;base64," + "A".repeat(14 * 1024 * 1024)), form);
        assertThrows(InvalidClinicalAttachmentException.class, () -> ClinicalAttachmentService.validate(upload));
    }

    @Test
    public void omittedStoredImageIsNotReplaced() throws Exception {
        ClinicalAttachmentUpload upload = mapper.convertValue(Map.of("fileType", "data:image/png"), form);
        ClinicalAttachmentService.validate(upload);
        assertNull(upload.getImage());
    }

    private ClinicalAttachmentUpload encoded(String type, byte[] bytes) {
        return mapper.convertValue(Map.of("base64Image", type + ";base64," + Base64.getEncoder().encodeToString(bytes)),
                form);
    }

    static byte[] fixture(String type) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if ("application/pdf".equals(type)) {
            try (PDDocument document = new PDDocument()) {
                document.addPage(new PDPage());
                document.save(output);
            }
        } else {
            ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), type.substring(6), output);
        }
        return output.toByteArray();
    }
}
