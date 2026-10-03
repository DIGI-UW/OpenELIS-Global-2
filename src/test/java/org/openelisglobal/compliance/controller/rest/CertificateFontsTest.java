package org.openelisglobal.compliance.controller.rest;

import static org.junit.Assert.assertTrue;

import com.itextpdf.text.Document;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import org.junit.Test;
import org.openelisglobal.testsupport.PdfText;

/**
 * OGC-1266: the compliance certificate printed "Lead (Pb) 0.050 mg/L 0.03 mg/L"
 * because Helvetica cannot encode the threshold signs.
 */
public class CertificateFontsTest {

    @Test
    public void thresholdSignsPrint() throws Exception {
        assertTrue("the embedded Unicode font is on the classpath", CertificateFonts.unicode());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Phrase("Lead (Pb) ≤ 0.03 mg/L, Dissolved Oxygen ≥ 6 mg/L", CertificateFonts.regular(9)));
        document.add(new Phrase(" — ≤ bold", CertificateFonts.bold(9)));
        document.close();

        String text = PdfText.ofPage(out.toByteArray(), 1);
        assertTrue(text, text.contains("≤ 0.03 mg/L"));
        assertTrue(text, text.contains("≥ 6 mg/L"));
        assertTrue(text, text.contains("≤ bold"));
    }
}
