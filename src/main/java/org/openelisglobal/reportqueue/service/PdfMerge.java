package org.openelisglobal.reportqueue.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.pdf.PdfCopy;
import org.openpdf.text.pdf.PdfReader;

/**
 * Joins several PDFs into one, each keeping its own pages as rendered, so a
 * batch prints every report in turn with each one numbered from its own page 1.
 */
final class PdfMerge {

    private PdfMerge() {
    }

    static byte[] concat(List<byte[]> pdfs) throws IOException {
        if (pdfs.size() == 1) {
            return pdfs.get(0);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        try {
            PdfCopy copy = new PdfCopy(document, out);
            document.open();
            for (byte[] pdf : pdfs) {
                PdfReader reader = new PdfReader(pdf);
                for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                    copy.addPage(copy.getImportedPage(reader, page));
                }
                copy.freeReader(reader);
                reader.close();
            }
        } catch (DocumentException e) {
            throw new IOException("Could not join the reports into one PDF", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return out.toByteArray();
    }
}
