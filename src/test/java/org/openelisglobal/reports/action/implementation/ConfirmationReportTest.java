package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reports.action.implementation.reportBeans.ConfirmationData;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.testsupport.PdfText;

public class ConfirmationReportTest extends BaseWebContextSensitiveTest {

    @Test
    public void eachSiteStartsOnItsOwnPageAndLongOrdersRetainTheirIdentity() throws Exception {
        ConfirmationReport report = new ConfirmationReport();
        ReportForm form = new ReportForm();
        form.setLowerDateRange("01/01/2026");
        form.setUpperDateRange("12/12/2026");
        report.initializeReport(form);
        ConfirmationData first = confirmation();
        ConfirmationData second = confirmation();
        second.setOrganizationName("Clinic B");
        byte[] twoSites = report.render(List.of(first, second));
        org.junit.Assert.assertEquals(2, PdfText.pageCount(twoSites));
        org.junit.Assert.assertFalse(PdfText.ofPage(twoSites, 1).contains("Clinic B"));
        org.junit.Assert.assertFalse(PdfText.ofPage(twoSites, 2).contains("Clinic A"));
        second.setRequesterTest(java.util.Collections.nCopies(100, "Long initial test"));
        second.setRequesterResult(java.util.Collections.nCopies(100, "Positive"));
        byte[] longOrder = report.render(List.of(second));
        org.openelisglobal.testsupport.PdfRegression.everyPage(longOrder, "confirmation-long-order", "REQ-1",
                "DEV0126000000000951", "Confirmation Test Report");
    }

    @Test
    public void ordinaryOrderNotesAndResultsStayWithTheirIdentityAtPageBoundaries() throws Exception {
        ConfirmationReport report = new ConfirmationReport();
        ReportForm form = new ReportForm();
        form.setLowerDateRange("01/01/2026");
        form.setUpperDateRange("12/12/2026");
        report.initializeReport(form);
        java.util.ArrayList<ConfirmationData> orders = new java.util.ArrayList<>();
        for (int i = 0; i < 24; i++) {
            ConfirmationData order = confirmation();
            order.setRequesterAccession("REQUEST-" + i);
            order.setLabAccession("LAB-" + i);
            order.setNote("ORDER-NOTE-" + i);
            orders.add(order);
        }
        byte[] pdf = report.render(orders);
        java.util.List<String> pageTexts = org.openelisglobal.testsupport.PdfRegression.pages(pdf);
        org.openelisglobal.testsupport.PdfRegression.everyPage(pdf, "confirmation-order-boundaries",
                "Confirmation Test Report");
        for (int i = 0; i < orders.size(); i++) {
            org.openelisglobal.testsupport.PdfRegression.samePage(pageTexts, "ORDER-NOTE-" + i, "REQUEST-" + i);
            org.openelisglobal.testsupport.PdfRegression.samePage(pageTexts, "ORDER-NOTE-" + i, "LAB-" + i);
        }
    }

    @Test
    public void listsEachOrdersInitialAndConfirmationResultsUnderItsSite() throws Exception {
        ConfirmationReport report = new ConfirmationReport();
        ReportForm form = new ReportForm();
        form.setLowerDateRange("01/01/2026");
        form.setUpperDateRange("12/12/2026");
        report.initializeReport(form);

        List<String> lines = Arrays.asList(PdfText.of(report.render(List.of(confirmation()))).split("\n"));

        assertLine(lines, "Confirmation Test Report");
        assertLine(lines, "01/01/2026 - 12/12/2026 Site ID: DEV01");
        assertLine(lines, "Site: Clinic A");
        assertLine(lines, "Requester Lab Number Confirmation Order Number Sample Type Reception");
        assertLine(lines, "REQ-1 DEV0126000000000951 Serum 01/06/2026");
        assertLine(lines, "Requester contact: Dr Requester");
        assertLine(lines, "Tel: 555-0101 Fax: 555-0102 Email: req@example.org");
        assertLine(lines, "Test Result Completion Date");
        assertLine(lines, "Initial Results HIV rapid Positive");
        assertLine(lines, "Confirmation Results Western blot Positive 05/06/2026");
        assertLine(lines, "PCR In progress");
        assertLine(lines, "Note: Repeat sample requested");
    }

    private static ConfirmationData confirmation() {
        ConfirmationData data = new ConfirmationData();
        data.setOrganizationName("Clinic A");
        data.setRequesterAccession("REQ-1");
        data.setLabAccession("DEV0126000000000951");
        data.setSampleType("Serum");
        data.setRequesterTest(List.of("HIV rapid"));
        data.setRequesterResult(List.of("Positive"));
        data.setLabTest(List.of("Western blot", "PCR"));
        data.setLabResult(List.of("Positive", ""));
        data.setCompleationDate(List.of("05/06/2026", ""));
        data.setRequesterName("Dr Requester");
        data.setRequesterPhone("555-0101");
        data.setRequesterFax("555-0102");
        data.setRequesterEMail("req@example.org");
        data.setNote("Repeat sample requested");
        data.setReceptionDate("01/06/2026");
        return data;
    }

    private void assertLine(List<String> lines, String expected) {
        assertTrue(expected + " in " + lines, lines.contains(expected));
    }
}
