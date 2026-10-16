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

    public static float pageWidth(byte[] pdf, int page) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getPage(page - 1).getMediaBox().getWidth();
        }
    }

    public static float pageHeight(byte[] pdf, int page) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getPage(page - 1).getMediaBox().getHeight();
        }
    }

    /** The right edge of the right-most glyph on a page, in points. */
    public static float rightmostGlyph(byte[] pdf, int page) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            float[] right = { 0 };
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void processTextPosition(TextPosition text) {
                    right[0] = Math.max(right[0], text.getXDirAdj() + text.getWidthDirAdj());
                }
            };
            stripper.setStartPage(page);
            stripper.setEndPage(page);
            stripper.getText(document);
            return right[0];
        }
    }

    /**
     * The baseline of the first line on a page that starts with the given text,
     * measured down from the top of the page, in points; -1 when absent.
     */
    public static float baselineOf(byte[] pdf, int page, String lineStart) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            float[] baseline = { -1 };
            StringBuilder line = new StringBuilder();
            float[] lineY = { Float.NaN };
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void processTextPosition(TextPosition text) {
                    if (lineY[0] != text.getYDirAdj()) {
                        line.setLength(0);
                        lineY[0] = text.getYDirAdj();
                    }
                    line.append(text.getUnicode());
                    if (baseline[0] < 0 && line.toString().replace(" ", "").startsWith(lineStart.replace(" ", ""))) {
                        baseline[0] = text.getYDirAdj();
                    }
                }
            };
            stripper.setStartPage(page);
            stripper.setEndPage(page);
            stripper.getText(document);
            return baseline[0];
        }
    }

    /** The page's raw content stream, for checks on how something is drawn. */
    public static String content(byte[] pdf, int page) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf);
                java.io.InputStream in = document.getPage(page - 1).getContents()) {
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
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
