package org.openelisglobal.workplan.reports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.testsupport.PdfText;

public class WorkplanPdfTest extends BaseWebContextSensitiveTest {

    private static final String ORDER_ONE = "DEV0126000000000101";
    private static final String ORDER_TWO = "DEV0126000000000102";
    private static final Property[] WORKPLAN_OPTIONS = { Property.RESULTS_ON_WORKPLAN, Property.SUBJECT_ON_WORKPLAN,
            Property.NEXT_VISIT_DATE_ON_WORKPLAN };

    private final Map<Property, String> originalOptions = new EnumMap<>(Property.class);

    @Before
    public void setUp() throws Exception {
        for (Property option : WORKPLAN_OPTIONS) {
            originalOptions.put(option, ConfigurationProperties.getInstance().getPropertyValue(option));
        }
        IAccessionNumberGenerator generator = Mockito.mock(IAccessionNumberGenerator.class);
        Mockito.when(generator.getPrefix()).thenReturn("DEV");
        Mockito.when(generator.getInvarientLength()).thenReturn(7);
        AccessionNumberValidatorFactory factory = SpringContext.getBean(AccessionNumberValidatorFactory.class);
        Mockito.when(factory.getGenerator(Mockito.any())).thenReturn(generator);
    }

    @After
    public void tearDown() {
        Mockito.reset(SpringContext.getBean(AccessionNumberValidatorFactory.class));
        originalOptions.forEach(ConfigurationProperties.getInstance()::setPropertyValue);
    }

    @Test
    public void repeatsWorkplanIdentityAndPrintDateAcrossPages() throws Exception {
        java.util.ArrayList<org.openelisglobal.test.beanItems.TestResultItem> items = new java.util.ArrayList<>();
        for (int i = 0; i < 150; i++) {
            org.openelisglobal.test.beanItems.TestResultItem item = new org.openelisglobal.test.beanItems.TestResultItem();
            item.setAccessionNumber("DEV0126000000000101");
            item.setTestName("Long Test");
            items.add(item);
        }
        for (String results : List.of("false", "true")) {
            setWorkplanOptions(results, "false", "false");
            for (boolean bySection : List.of(false, true)) {
                byte[] pdf = WorkplanPdf.render("Long Test", items, bySection, bySection ? "DEV" : null);
                org.openelisglobal.testsupport.PdfRegression.everyPage(pdf,
                        "workplan-continuation-" + results + "-" + bySection, "Long Test",
                        org.openelisglobal.common.util.DateUtil.getCurrentDateAsText());
            }
        }
    }

    @Test
    public void testWorkplan_listsEveryTestWithItsLabNumberAndReceptionDate() throws Exception {
        setWorkplanOptions("false", "false", "false");

        String text = PdfText.of(new TestWorkplanReport("Glucose").renderPdf(rows()));

        assertTrue(text, text.contains("Work plan: Glucose"));
        assertTrue(text, text.contains("Lab No Date of reception"));
        assertEquals(text, 2, occurrences(text, ORDER_ONE + " 01/10/2026"));
        assertTrue(text, text.contains(ORDER_TWO + " 02/10/2026"));
        assertFalse("subject numbers are off by default: " + text, text.contains("SUBJ-1"));
    }

    @Test
    public void testWorkplan_addsTheSubjectNumberAndNextVisitWhenConfigured() throws Exception {
        setWorkplanOptions("false", "true", "true");

        String text = PdfText.of(new TestWorkplanReport("Glucose").renderPdf(rows()));

        assertTrue(text, text.contains("Date of reception Date of next visit"));
        assertTrue(text, text.contains(ORDER_ONE + " SUBJ-1 01/10/2026 15/10/2026"));
        assertTrue(text, text.contains(ORDER_TWO + " SUBJ-2 02/10/2026"));
    }

    @Test
    public void sectionWorkplanWithResults_groupsAnOrdersTestsAndLeavesSpaceForResults() throws Exception {
        setWorkplanOptions("true", "false", "false");

        String text = PdfText.of(new TestSectionWorkplanReport("Biochemistry").renderPdf(rows()));

        assertTrue(text, text.contains("Work plan: Biochemistry"));
        assertTrue("the fixed accession prefix moves to the header: " + text,
                text.contains("Lab No (DEV) Date of reception Name of test Results Tech ID"));
        assertFalse("the lab number prints without its fixed prefix: " + text, text.contains(ORDER_ONE));
        assertTrue(text, text.contains("000000000101 01/10/2026 Glucose"));
        assertEquals("an order's lab number prints once for all its tests: " + text, 1,
                occurrences(text, "000000000101"));
        assertTrue(text, text.contains("Creatinine"));
        assertTrue(text, text.contains("000000000102 02/10/2026 Glucose"));
    }

    private void setWorkplanOptions(String resultsOnWorkplan, String subjectNumber, String nextVisit) {
        ConfigurationProperties.getInstance().setPropertyValue(Property.RESULTS_ON_WORKPLAN, resultsOnWorkplan);
        ConfigurationProperties.getInstance().setPropertyValue(Property.SUBJECT_ON_WORKPLAN, subjectNumber);
        ConfigurationProperties.getInstance().setPropertyValue(Property.NEXT_VISIT_DATE_ON_WORKPLAN, nextVisit);
    }

    private List<TestResultItem> rows() {
        return List.of(item(ORDER_ONE, "SUBJ-1", "01/10/2026", "15/10/2026", "Glucose"),
                item(ORDER_ONE, "SUBJ-1", "01/10/2026", "15/10/2026", "Creatinine"),
                item(ORDER_TWO, "SUBJ-2", "02/10/2026", "", "Glucose"));
    }

    private TestResultItem item(String accession, String subject, String received, String nextVisit, String testName) {
        TestResultItem item = new TestResultItem();
        item.setAccessionNumber(accession);
        item.setPatientInfo(subject);
        item.setReceivedDate(received);
        item.setNextVisitDate(nextVisit);
        item.setTestName(testName);
        return item;
    }

    private int occurrences(String text, String value) {
        return text.split(java.util.regex.Pattern.quote(value), -1).length - 1;
    }
}
