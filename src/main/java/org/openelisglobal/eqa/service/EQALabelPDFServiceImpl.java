package org.openelisglobal.eqa.service;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
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
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAPanelSample;
import org.openelisglobal.eqa.valueholder.EQAPanelStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-612 (FR-V2.4-13) — Avery 5160-equivalent label sheets. Layout is plain
 * text placed on a fixed 3×10 grid; the only data read is the blind code, the
 * cycle identifier and the analyte name, so a target value can never reach the
 * text layer (AC-V2.4-14). Output bytes are normalized so regeneration is
 * byte-identical (AC-V2.4-15).
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
    private AnalyteService analyteService;

    @Override
    public byte[] generateLabelSheet(Long panelId) {
        EQAPanel panel = panelService.get(panelId);
        if (!PRINTABLE_STATES.contains(panel.getStatus())) {
            // FR-V2.4-13 scopes the sheet to after sealing and before unblinding:
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
        show(canvas, new Phrase(sample.getBlindCode(), CODE_FONT), left, top - 24);
        show(canvas, new Phrase(cycleIdentifier, META_FONT), left, top - 42);
        show(canvas, new Phrase(analyteName(sample.getAnalyteId()), META_FONT), left, top - 54);
    }

    /**
     * Clipped to the label box: showTextAligned draws a single unwrapped line, so a
     * long blind code would otherwise run into the neighbouring label.
     */
    private void show(PdfContentByte canvas, Phrase phrase, float x, float y) {
        ColumnText column = new ColumnText(canvas);
        column.setSimpleColumn(x, y - 2f, x + LABEL_WIDTH - 2f * PAD, y + phrase.getFont().getSize() + 2f);
        column.addText(phrase);
        try {
            column.go();
        } catch (DocumentException e) {
            throw new IllegalStateException("Label text layout failed", e);
        }
    }

    /**
     * Repeated ids cost nothing: the persistence context caches within the read.
     */
    private String analyteName(Long analyteId) {
        Analyte analyte = analyteService.get(String.valueOf(analyteId));
        return analyte == null || analyte.getAnalyteName() == null ? "" : analyte.getAnalyteName();
    }

    /**
     * iText stamps wall-clock metadata (/CreationDate, /ModDate) and a random
     * trailer /ID on every run. Both are replaced with fixed same-length values —
     * offsets are untouched, so the document stays valid — making regeneration
     * byte-identical (AC-V2.4-15).
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
