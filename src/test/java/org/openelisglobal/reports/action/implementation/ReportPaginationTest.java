package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.*;

import java.util.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.*;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.reports.action.implementation.reportBeans.NonConformityReportData;
import org.openelisglobal.testsupport.*;

@org.springframework.transaction.annotation.Transactional
public class ReportPaginationTest extends BaseWebContextSensitiveTest {
    private final Map<Property, String> original = new EnumMap<>(Property.class);

    @Before
    public void setUp() throws Exception {
        for (Property setting : List.of(Property.USE_PAGE_NUMBERS_ON_REPORTS,
                Property.SIGNATURES_ON_NONCONFORMITY_REPORTS, Property.REPORT_PAPER_SIZE)) {
            original.put(setting, ConfigurationProperties.getInstance().getPropertyValue(setting));
        }
        executeDataSetWithStateManagement("testdata/reporting-r1-logo.xml");
    }

    @After
    public void restoreSettings() {
        original.forEach((key, value) -> ConfigurationProperties.getInstance().setPropertyValue(key, value));
    }

    @Test
    public void managementReportsRepeatLogosScopeAndReadableMarkup() throws Exception {
        for (String size : List.of("A4", "Letter")) {
            ConfigurationProperties.getInstance().setPropertyValue(Property.REPORT_PAPER_SIZE, size);
            List<List<String>> rows = new ArrayList<>();
            for (int i = 0; i < 140; i++) {
                rows.add(List.of("ROW-" + i, "A &amp; B<br/>second line"));
            }
            for (String title : List.of("Activity Report", "Rejection Report")) {
                byte[] pdf = ManagementReportPdf.render(org.openelisglobal.common.util.PdfExportSupport.pageSize(),
                        title, "Unit: R1", new HaitiNonConformityByDate().new DateRange("01/10/2026", "02/10/2026"),
                        List.of("Order", "Value"), new float[] { 1, 2 }, rows);
                PdfRegression.everyPage(pdf, title.replace(' ', '-') + "-" + size, title, "Unit: R1", "01/10/2026",
                        "02/10/2026");
                String text = PdfText.of(pdf);
                assertTrue(text, text.contains("A & B\nsecond line"));
                assertFalse(text, text.contains("<br") || text.contains("&amp;"));
                try (PDDocument document = Loader.loadPDF(pdf)) {
                    for (org.apache.pdfbox.pdmodel.PDPage page : document.getPages()) {
                        int images = 0;
                        for (org.apache.pdfbox.cos.COSName name : page.getResources().getXObjectNames()) {
                            if (page.getResources().getXObject(name) instanceof PDImageXObject) {
                                images++;
                            }
                        }
                        assertEquals("configured logo on each page", 1, images);
                    }
                }
            }
        }
    }

    @Test
    public void nonConformityFootersRetainIdentityCountsAndConfiguredSignature() throws Exception {
        List<NonConformityReportData> rows = new ArrayList<>();
        for (int i = 0; i < 180; i++) {
            NonConformityReportData row = new NonConformityReportData();
            row.setAccessionNumber("LAB-" + i);
            row.setSection("Chemistry");
            row.setNonConformityReason("Hemolysis");
            row.setSampleNote("ORDER-NOTE-" + i);
            rows.add(row);
        }
        for (boolean pageNumbers : List.of(false, true)) {
            for (boolean signature : List.of(false, true)) {
                ConfigurationProperties config = ConfigurationProperties.getInstance();
                config.setPropertyValue(Property.USE_PAGE_NUMBERS_ON_REPORTS, String.valueOf(pageNumbers));
                config.setPropertyValue(Property.SIGNATURES_ON_NONCONFORMITY_REPORTS, String.valueOf(signature));
                byte[] pdf = NonConformityReportPdf.byDate("R1 Non-conformities", "October 2026", rows, false);
                PdfRegression.everyPage(pdf, "non-conformity-" + pageNumbers + "-" + signature, "R1 Non-conformities",
                        "October 2026");
                java.util.List<String> pageTexts = org.openelisglobal.testsupport.PdfRegression.pages(pdf);
                for (int i = 0; i < rows.size(); i++) {
                    PdfRegression.samePage(pageTexts, "ORDER-NOTE-" + i, "LAB-" + i);
                }
                int pages = PdfText.pageCount(pdf);
                for (int page = 1; page <= pages; page++) {
                    String text = PdfText.ofPage(pdf, page);
                    assertEquals(text, signature, text.contains(
                            org.openelisglobal.internationalization.MessageUtil.getMessage("report.supervisorSign")));
                    assertEquals(text, pageNumbers, text.contains("Page " + page));
                    if (pageNumbers) {
                        assertTrue(text, text.contains("Page " + page + " of " + pages));
                    }
                }
            }
        }
    }
}
