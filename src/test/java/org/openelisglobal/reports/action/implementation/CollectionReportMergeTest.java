package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.jasperreports.engine.JasperRunManager;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.junit.Test;
import org.openelisglobal.reports.action.implementation.reportBeans.ErrorMessages;
import org.openelisglobal.testsupport.PdfText;

/**
 * The collection reports concatenate PDFs that JasperReports rendered, so the
 * merge reads PDFs written by a different PDF library than its own.
 */
public class CollectionReportMergeTest {

    @Test
    public void merge_keepsEveryPageOfEveryJasperReportInOrder() throws Exception {
        byte[] first = renderNotice("First collected report");
        byte[] second = renderNotice("Second collected report");

        byte[] merged = new CollectionReport() {
            @Override
            protected List<byte[]> generateReports() {
                return List.of();
            }
        }.merge(List.of(first, second));

        assertEquals(PdfText.pageCount(first) + PdfText.pageCount(second), PdfText.pageCount(merged));
        String text = PdfText.of(merged);
        int firstAt = text.indexOf("First collected report");
        int secondAt = text.indexOf("Second collected report");
        assertTrue(text, firstAt >= 0);
        assertTrue(text, secondAt > firstAt);
    }

    private byte[] renderNotice(String message) throws Exception {
        String reportDir = getClass().getClassLoader().getResource("reports/").getPath();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("SUBREPORT_DIR", reportDir);
        ErrorMessages messages = new ErrorMessages();
        messages.setMsgLine1(message);
        return JasperRunManager.runReportToPdf(reportDir + "NoticeOfReportError.jasper", parameters,
                new JRBeanCollectionDataSource(List.of(messages)));
    }
}
