package org.openelisglobal.compliance.controller.rest;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import org.openelisglobal.common.log.LogEvent;
import org.openpdf.text.Font;
import org.openpdf.text.pdf.BaseFont;

/**
 * Fonts for the compliance certificate. iText's built-in Helvetica only encodes
 * WinAnsi, so the threshold signs (≤, ≥) printed as nothing: "Lead 0.050 mg/L
 * 0.03 mg/L". The certificate embeds DejaVu Sans (shipped by
 * jasperreports-fonts) with Identity-H encoding, and falls back to Helvetica
 * only when the font cannot be loaded.
 */
final class CertificateFonts {

    static final String REGULAR = "net/sf/jasperreports/fonts/dejavu/DejaVuSans.ttf";
    static final String BOLD = "net/sf/jasperreports/fonts/dejavu/DejaVuSans-Bold.ttf";

    private static final BaseFont REGULAR_FONT = load(REGULAR);
    private static final BaseFont BOLD_FONT = load(BOLD);

    private CertificateFonts() {
    }

    static Font regular(float size) {
        return font(REGULAR_FONT, size, Font.NORMAL, null);
    }

    static Font bold(float size) {
        return bold(size, null);
    }

    static Font bold(float size, Color color) {
        return font(BOLD_FONT, size, Font.BOLD, color);
    }

    /** Whether the certificate prints with the embedded Unicode font. */
    static boolean unicode() {
        return REGULAR_FONT != null && BOLD_FONT != null;
    }

    private static Font font(BaseFont base, float size, int helveticaStyle, Color color) {
        if (base == null) {
            return color == null ? new Font(Font.HELVETICA, size, helveticaStyle)
                    : new Font(Font.HELVETICA, size, helveticaStyle, color);
        }
        return color == null ? new Font(base, size) : new Font(base, size, Font.NORMAL, color);
    }

    private static BaseFont load(String resource) {
        try (InputStream in = CertificateFonts.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                LogEvent.logError(CertificateFonts.class.getSimpleName(), "load",
                        "Font " + resource + " is not on the classpath; certificates fall back to Helvetica");
                return null;
            }
            return BaseFont.createFont(resource, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, in.readAllBytes(), null);
        } catch (IOException | org.openpdf.text.DocumentException e) {
            LogEvent.logError(CertificateFonts.class.getSimpleName(), "load",
                    "Font " + resource + " could not be read; certificates fall back to Helvetica: " + e);
            return null;
        }
    }
}
