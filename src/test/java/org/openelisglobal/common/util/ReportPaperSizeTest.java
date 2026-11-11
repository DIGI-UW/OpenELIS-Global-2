package org.openelisglobal.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openpdf.text.PageSize;
import org.openpdf.text.Rectangle;

public class ReportPaperSizeTest extends BaseWebContextSensitiveTest {

    private String originalPaperSize;

    @Before
    public void setUp() {
        originalPaperSize = ConfigurationProperties.getInstance().getPropertyValue(Property.REPORT_PAPER_SIZE);
    }

    @After
    public void tearDown() {
        setPaperSize(originalPaperSize);
    }

    @Test
    public void reportsPrintOnA4UnlessTheSiteChoosesLetter() {
        setPaperSize("A4");
        assertSize(PageSize.A4, PdfExportSupport.pageSize());
        assertSize(PageSize.A3, PdfExportSupport.largePageSize());

        setPaperSize("Letter");
        assertSize(PageSize.LETTER, PdfExportSupport.pageSize());
        assertSize(PageSize.TABLOID, PdfExportSupport.largePageSize());
    }

    @Test
    public void anUnknownPaperSizePrintsOnA4() {
        setPaperSize("Legal");

        assertSize(PageSize.A4, PdfExportSupport.pageSize());
    }

    @Test
    public void theLargeSizeTurnedSidewaysIsLandscapeOnEitherPaper() {
        for (String paper : List.of("A4", "Letter")) {
            setPaperSize(paper);
            Rectangle landscape = PdfExportSupport.largePageSize().rotate();
            assertTrue(paper + ": " + landscape, landscape.getWidth() > landscape.getHeight());
        }
    }

    private static void setPaperSize(String paper) {
        ConfigurationProperties.getInstance().setPropertyValue(Property.REPORT_PAPER_SIZE, paper);
    }

    private static void assertSize(Rectangle expected, Rectangle actual) {
        assertEquals("width", expected.getWidth(), actual.getWidth(), 0.5f);
        assertEquals("height", expected.getHeight(), actual.getHeight(), 0.5f);
    }
}
