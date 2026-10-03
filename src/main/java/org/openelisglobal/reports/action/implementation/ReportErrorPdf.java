package org.openelisglobal.reports.action.implementation;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.action.implementation.reportBeans.ErrorMessages;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfWriter;

/** The page a report prints instead of itself when it cannot be produced. */
final class ReportErrorPdf {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD);
    private static final Font MESSAGE_FONT = new Font(Font.HELVETICA, 10);

    private ReportErrorPdf() {
    }

    static byte[] render(List<ErrorMessages> messages) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, out);
        document.open();
        Paragraph title = new Paragraph(MessageUtil.getMessage("report.noReportMessage"), TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(12);
        document.add(title);
        for (ErrorMessages message : messages) {
            for (String line : Arrays.asList(message.getMsgLine1(), message.getMsgLine2(), message.getMsgLine3())) {
                if (!GenericValidator.isBlankOrNull(line)) {
                    document.add(new Paragraph(line, MESSAGE_FONT));
                }
            }
        }
        document.close();
        return out.toByteArray();
    }
}
