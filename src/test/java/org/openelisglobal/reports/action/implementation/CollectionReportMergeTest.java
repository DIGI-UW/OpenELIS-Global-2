package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperRunManager;
import org.junit.Test;
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

    private static final String ONE_PAGE_REPORT = """
            <jasperReport xmlns="http://jasperreports.sourceforge.net/jasperreports"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://jasperreports.sourceforge.net/jasperreports http://jasperreports.sourceforge.net/xsd/jasperreport.xsd"
                name="OnePage" pageWidth="595" pageHeight="842" columnWidth="555"
                leftMargin="20" rightMargin="20" topMargin="20" bottomMargin="20">
              <parameter name="message" class="java.lang.String"/>
              <title>
                <band height="30">
                  <textField>
                    <reportElement x="0" y="0" width="555" height="30"/>
                    <textFieldExpression><![CDATA[$P{message}]]></textFieldExpression>
                  </textField>
                </band>
              </title>
            </jasperReport>
            """;

    private byte[] renderNotice(String message) throws Exception {
        JasperReport report = JasperCompileManager
                .compileReport(new ByteArrayInputStream(ONE_PAGE_REPORT.getBytes(StandardCharsets.UTF_8)));
        return JasperRunManager.runReportToPdf(report, new HashMap<>(Map.of("message", message)),
                new JREmptyDataSource());
    }
}
