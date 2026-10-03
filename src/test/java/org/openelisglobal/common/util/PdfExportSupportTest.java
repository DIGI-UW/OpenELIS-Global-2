package org.openelisglobal.common.util;

import static org.junit.Assert.assertTrue;

import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.PdfPTable;
import java.io.ByteArrayOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.testsupport.PdfText;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

public class PdfExportSupportTest {

    private Object previousMessageUtil;

    @Before
    public void setUp() {
        previousMessageUtil = ReflectionTestUtils.getField(MessageUtil.class, "instance");
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("test.export.page", LocaleContextHolder.getLocale(), "Folio");
        MessageUtil.setMessageSource(messages);
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(MessageUtil.class, "instance", previousMessageUtil);
    }

    @Test
    public void everyPageOfAMultiPageExportCarriesItsPageNumber() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfExportSupport.openWithPageNumbers(document, out, "test.export.page");
        PdfExportSupport.addHeading(document, "Export Title", new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD),
                new Font(Font.FontFamily.HELVETICA, 9), "Generated for the test lab\n");
        PdfPTable table = new PdfPTable(2);
        PdfExportSupport.addHeaderRow(table, new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD), 4, "Sample", "Result");
        for (int row = 1; row <= 120; row++) {
            table.addCell("SAMPLE-" + row);
            table.addCell("RESULT-" + row);
        }
        document.add(table);
        document.close();
        byte[] pdf = out.toByteArray();

        int pages = PdfText.pageCount(pdf);
        assertTrue("the export spans several pages: " + pages, pages >= 3);
        String firstPage = PdfText.ofPage(pdf, 1);
        assertTrue(firstPage, firstPage.contains("Export Title"));
        assertTrue(firstPage, firstPage.contains("Generated for the test lab"));
        assertTrue(firstPage, firstPage.contains("Sample Result"));
        for (int page = 1; page <= pages; page++) {
            String text = PdfText.ofPage(pdf, page);
            assertTrue("page " + page + ": " + text, text.contains("Folio " + page));
        }
        assertTrue(PdfText.of(pdf).contains("SAMPLE-120 RESULT-120"));
    }
}
