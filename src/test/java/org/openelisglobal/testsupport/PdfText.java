package org.openelisglobal.testsupport;

import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

/**
 * Text of a generated PDF in reading order. Text is sorted by its position on
 * the page, so a table row reads as one line even when the writer emitted the
 * table after the content that follows it on the page. Whitespace glyphs are
 * skipped and word breaks come from the gaps between glyphs: iText draws a lone
 * space just above each paragraph, and sorting would otherwise merge it into
 * the paragraph's first line ("O verall summary").
 */
public final class PdfText {

    private PdfText() {
    }

    public static String of(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return stripper(1, document.getNumberOfPages()).getText(document);
        }
    }

    public static String ofPage(byte[] pdf, int page) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return stripper(page, page).getText(document);
        }
    }

    public static int pageCount(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getNumberOfPages();
        }
    }

    public static float pageHeight(byte[] pdf, int page) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getPage(page - 1).getMediaBox().getHeight();
        }
    }

    private static PDFTextStripper stripper(int startPage, int endPage) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper() {
            @Override
            protected void processTextPosition(TextPosition text) {
                if (!text.getUnicode().isBlank()) {
                    super.processTextPosition(text);
                }
            }
        };
        stripper.setSortByPosition(true);
        stripper.setStartPage(startPage);
        stripper.setEndPage(endPage);
        return stripper;
    }
}
