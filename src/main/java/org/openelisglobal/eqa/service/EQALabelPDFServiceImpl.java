package org.openelisglobal.eqa.service;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAPanelSample;
import org.openelisglobal.eqa.valueholder.EQAPanelStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-612 — Avery 5160-equivalent label sheets. Layout is plain text placed on
 * a fixed 3×10 grid; the only data read is the blind code, the cycle identifier
 * and the analyte name, so a target value can never reach the text layer.
 * Output bytes are normalized so regeneration is byte-identical.
 */
@Service
@Transactional(readOnly = true)
public class EQALabelPDFServiceImpl implements EQALabelPDFService {

    // Avery 5160 geometry in points: 3 columns × 10 rows on US Letter.
    private static final float LABEL_WIDTH = 189f; // 2.625"
    private static final float LABEL_HEIGHT = 72f; // 1"
    private static final float LEFT_MARGIN = 13.5f; // 0.1875"
    private static final float TOP_MARGIN = 36f; // 0.5"
    private static final float COLUMN_PITCH = 198f; // 2.75"
    private static final int COLUMNS = 3;
    private static final int ROWS = 10;
    private static final float PAD = 8f;

    private static final String ELLIPSIS = "...";

    /** Sealed and distributed only — see generateLabelSheet. */
    private static final Set<EQAPanelStatus> PRINTABLE_STATES = EnumSet.of(EQAPanelStatus.SEALED,
            EQAPanelStatus.DISTRIBUTED);

    private static final Font CODE_FONT = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
    private static final Font META_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);

    @Autowired
    private EQAPanelService panelService;
    @Autowired
    private EQAPanelSampleDAO panelSampleDAO;
    @Autowired
    private EQAPanelService eqaPanelService;

    @Override
    public byte[] generateLabelSheet(Long panelId) {
        EQAPanel panel = panelService.get(panelId);
        if (!PRINTABLE_STATES.contains(panel.getStatus())) {
            // The sheet is scoped to after sealing and before unblinding:
            // before, the panel is still being built; after, the targets are out
            // and a blind code is no longer blind.
            throw new IllegalStateException(
                    "Labels are printable between sealing and unblinding; this panel is " + panel.getStatus());
        }
        List<EQAPanelSample> samples = panelSampleDAO.getAllMatchingOrdered("panel.id", panelId, "sampleCode", false);
        if (samples.isEmpty()) {
            throw new IllegalArgumentException("The panel has no samples to label");
        }
        // A blank code would render an empty label, which is worse than no sheet:
        // the tube gets an anonymous sticker and the aliquot becomes untraceable.
        for (EQAPanelSample sample : samples) {
            if (GenericValidator.isBlankOrNull(sample.getBlindCode())) {
                throw new IllegalArgumentException(
                        "Panel sample " + sample.getSampleCode() + " has no blind code to print");
            }
        }

        String cycleIdentifier = panel.getCycle() != null
                && !GenericValidator.isBlankOrNull(panel.getCycle().getCycleName()) ? panel.getCycle().getCycleName()
                        : panel.getPanelName();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.LETTER, 0, 0, 0, 0);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            PdfContentByte canvas = writer.getDirectContent();
            int slot = 0;
            for (EQAPanelSample sample : samples) {
                if (slot == COLUMNS * ROWS) {
                    document.newPage();
                    slot = 0;
                }
                drawLabel(canvas, slot, sample, cycleIdentifier);
                slot++;
            }
            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Label sheet generation failed", e);
        }
        return normalize(out.toByteArray());
    }

    @Override
    public int countLabels(Long panelId) {
        return panelSampleDAO.getAllMatching("panel.id", panelId).size();
    }

    private void drawLabel(PdfContentByte canvas, int slot, EQAPanelSample sample, String cycleIdentifier) {
        int column = slot % COLUMNS;
        int row = slot / COLUMNS;
        float left = LEFT_MARGIN + column * COLUMN_PITCH + PAD;
        float top = PageSize.LETTER.getHeight() - TOP_MARGIN - row * LABEL_HEIGHT;

        // Blind code large and first; cycle + analyte small underneath. Target
        // values and acceptance ranges are deliberately never read here.
        show(canvas, oneLine(sample.getBlindCode(), CODE_FONT), left, top - 24);
        show(canvas, oneLine(cycleIdentifier, META_FONT), left, top - 42);
        show(canvas, oneLine(analyteName(sample.getAnalyteId()), META_FONT), left, top - 54);
    }

    /** The width one line of label text has to live in. */
    private static final float TEXT_WIDTH = LABEL_WIDTH - 2f * PAD;

    /**
     * One line, shortened with an ellipsis if it does not fit the label's width. A
     * label box holds a single line, and a value long enough to wrap used to lose
     * every line of itself rather than the tail — so the cycle identifier and the
     * analyte name printed on nothing but the shortest fixtures. A truncated
     * analyte name on a tube is still useful; a missing one is not.
     */
    private static Phrase oneLine(String text, Font font) {
        String fitted = text == null ? "" : text.trim();
        BaseFont base = font.getCalculatedBaseFont(false);
        if (base.getWidthPoint(fitted, font.getSize()) <= TEXT_WIDTH) {
            return new Phrase(fitted, font);
        }
        while (!fitted.isEmpty() && base.getWidthPoint(fitted + ELLIPSIS, font.getSize()) > TEXT_WIDTH) {
            fitted = fitted.substring(0, fitted.length() - 1);
        }
        return new Phrase(fitted.trim() + ELLIPSIS, font);
    }

    /**
     * Clipped to the label box: showTextAligned draws a single unwrapped line, so a
     * long blind code would otherwise run into the neighbouring label.
     *
     * <p>
     * The box has to hold a whole line of leading, not just the glyphs: sized to
     * the font alone it was a fraction too short and iText dropped the text without
     * a word. The baseline still lands on {@code y}, because that is where a column
     * places its first line.
     */
    private void show(PdfContentByte canvas, Phrase phrase, float x, float y) {
        float leading = phrase.getFont().getSize() + 1f;
        ColumnText column = new ColumnText(canvas);
        column.setLeading(leading);
        column.setSimpleColumn(x, y - 3f, x + TEXT_WIDTH, y + leading);
        column.addText(phrase);
        try {
            // go() reports what it could not fit rather than throwing, so an
            // unchecked call is how a line goes missing silently. Everything drawn
            // here is one line wide by construction, so this cannot fire without a
            // bug above it.
            if ((column.go() & ColumnText.NO_MORE_TEXT) == 0) {
                throw new IllegalStateException("Label text did not fit its box: " + phrase.getContent());
            }
        } catch (DocumentException e) {
            throw new IllegalStateException("Label text layout failed", e);
        }
    }

    /**
     * Repeated ids cost nothing: the persistence context caches within the read.
     */
    private String analyteName(Long analyteId) {
        return StringUtils.defaultString(eqaPanelService.analyteName(analyteId));
    }

    /**
     * iText stamps wall-clock metadata (/CreationDate, /ModDate) and a random
     * trailer /ID on every run. Both are replaced with fixed same-length values —
     * offsets are untouched, so the document stays valid — making regeneration
     * byte-identical.
     */
    private byte[] normalize(byte[] pdf) {
        String raw = new String(pdf, StandardCharsets.ISO_8859_1);
        raw = replaceSameLength(raw, Pattern.compile("/CreationDate\\(([^)]*)\\)"), 'D');
        raw = replaceSameLength(raw, Pattern.compile("/ModDate\\(([^)]*)\\)"), 'D');
        raw = replaceSameLength(raw, Pattern.compile("/ID *\\[<([0-9a-fA-F]+)><([0-9a-fA-F]+)>\\]"), '0');
        return raw.getBytes(StandardCharsets.ISO_8859_1);
    }

    private String replaceSameLength(String raw, Pattern pattern, char filler) {
        Matcher matcher = pattern.matcher(raw);
        StringBuilder result = new StringBuilder(raw);
        while (matcher.find()) {
            for (int group = 1; group <= matcher.groupCount(); group++) {
                for (int i = matcher.start(group); i < matcher.end(group); i++) {
                    result.setCharAt(i, filler);
                }
            }
        }
        return result.toString();
    }
}
