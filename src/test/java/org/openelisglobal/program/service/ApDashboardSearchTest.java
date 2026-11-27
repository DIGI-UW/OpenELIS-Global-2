package org.openelisglobal.program.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.service.cytology.CytologySampleService;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.program.valueholder.cytology.CytologySample.CytologyStatus;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample.ImmunohistochemistryStatus;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The Pathology, Immunohistochemistry and Cytology dashboards' search. A term
 * that names one order (its lab number, a dotted label, or on the Pathology
 * bench a block or slide barcode) lists that order's cases. Any other term
 * matches a lab number from its start, or any part of the patient's first or
 * last name in any letter case, always inside the requested statuses.
 *
 * <p>
 * Per bench the fixture holds two cases in one status, lab numbers APS26000001
 * (Achieng OKELLO) and APS26000002 (Wanjiru Kering), and a third case in
 * another status, BPS26000003, also Achieng OKELLO's. Pathology also holds
 * CPS26000004 at Processing, an order with no patient.
 */
public class ApDashboardSearchTest extends BaseWebContextSensitiveTest {

    private static final int P_OKELLO = 701;
    private static final int P_KERING = 702;
    private static final int P_OTHER_STATUS = 703;
    private static final int P_NO_PATIENT = 704;
    private static final int I_OKELLO = 711;
    private static final int I_KERING = 712;
    private static final int I_OTHER_STATUS = 713;
    private static final int C_OKELLO = 721;
    private static final int C_KERING = 722;
    private static final int C_OTHER_STATUS = 723;

    private static final List<PathologyStatus> P_STATUS = List.of(PathologyStatus.GROSSING);
    private static final List<PathologyStatus> P_BOTH = List.of(PathologyStatus.GROSSING, PathologyStatus.COMPLETED);
    private static final List<ImmunohistochemistryStatus> I_STATUS = List.of(ImmunohistochemistryStatus.IN_PROGRESS);
    private static final List<ImmunohistochemistryStatus> I_BOTH = List.of(ImmunohistochemistryStatus.IN_PROGRESS,
            ImmunohistochemistryStatus.COMPLETED);
    private static final List<CytologyStatus> C_STATUS = List.of(CytologyStatus.PREPARING_SLIDES);
    private static final List<CytologyStatus> C_BOTH = List.of(CytologyStatus.PREPARING_SLIDES,
            CytologyStatus.COMPLETED);

    @Autowired
    private PathologySampleService pathologySampleService;

    @Autowired
    private ImmunohistochemistrySampleService immunohistochemistrySampleService;

    @Autowired
    private CytologySampleService cytologySampleService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/ap-dashboard-search.xml");
    }

    // ---- Pathology ----

    @Test
    public void pWholeLabNumber_findsThatCaseOnly() {
        List<Integer> found = p("APS26000001", P_STATUS);

        assertEquals(List.of(P_OKELLO), found);
        assertFalse("the case whose lab number shares the start is not listed", found.contains(P_KERING));
    }

    @Test
    public void pStartOfLabNumber_inLowerCase_findsBothCasesUnderThatStart() {
        List<Integer> found = p("aps2600000", P_BOTH);

        assertEquals(List.of(P_OKELLO, P_KERING), found);
        assertFalse("a lab number under another start is not listed", found.contains(P_OTHER_STATUS));
    }

    @Test
    public void pNameInAnotherLetterCase_findsThatPatientsCase() {
        assertEquals("a last name typed in lower case", List.of(P_OKELLO), p("okello", P_STATUS));
        assertEquals("a first name typed in upper case", List.of(P_OKELLO), p("ACHIENG", P_STATUS));
        assertFalse("the other patient's case is not listed", p("okello", P_STATUS).contains(P_KERING));
    }

    @Test
    public void pPartOfName_findsThatPatientsCase() {
        assertEquals("part of a first name", List.of(P_OKELLO), p("chien", P_STATUS));
        assertEquals("part of a last name, spaces around it", List.of(P_OKELLO), p(" kel ", P_STATUS));
        assertFalse("the other patient's case is not listed", p("chien", P_STATUS).contains(P_KERING));
    }

    @Test
    public void pBlockOrSlideBarcode_findsItsCase() {
        assertEquals("a block barcode", List.of(P_OKELLO), p("APS26000001-A1", P_STATUS));
        assertEquals("a slide barcode", List.of(P_OKELLO), p("APS26000001-A1-1", P_STATUS));
        assertFalse("the sibling case is not listed", p("APS26000001-A1-1", P_STATUS).contains(P_KERING));
    }

    @Test
    public void pNoMatch_findsNothing() {
        assertEquals("setup: two cases stand in the status", 2, p("", P_STATUS).size());
        assertTrue(p("zzq", P_STATUS).isEmpty());
    }

    @Test
    public void pNameMatchInAnotherStatus_isNotListed() {
        assertEquals(List.of(P_OKELLO), p("okello", P_STATUS));
        assertEquals("with both statuses the same term lists it", List.of(P_OKELLO, P_OTHER_STATUS),
                p("okello", P_BOTH));
    }

    @Test
    public void pOrderWithNoPatient_isFoundByTheStartOfItsLabNumberOnly() {
        List<PathologyStatus> processing = List.of(PathologyStatus.PROCESSING);

        assertEquals("the start of its lab number", List.of(P_NO_PATIENT), p("cps26", processing));
        assertTrue("a name term finds nothing and does not fail", p("okello", processing).isEmpty());
    }

    @Test
    public void pOneCharacter_matchesLabNumbersFromTheirStartOnly() {
        List<Integer> found = p("a", P_BOTH);

        assertEquals(List.of(P_OKELLO, P_KERING), found);
        assertFalse("a case whose patient's name holds the letter is not listed for it",
                found.contains(P_OTHER_STATUS));
    }

    @Test
    public void pWildcardCharacters_matchOnlyThemselves() {
        assertTrue("a percent sign", p("%", P_STATUS).isEmpty());
        assertTrue("an underscore", p("_", P_STATUS).isEmpty());
        assertTrue("a percent sign after a real start", p("APS%", P_STATUS).isEmpty());
        assertEquals("the real start alone", List.of(P_OKELLO, P_KERING), p("APS", P_STATUS));
    }

    // ---- Immunohistochemistry ----

    @Test
    public void iWholeLabNumber_findsThatCaseOnly() {
        List<Integer> found = i("APS26000001", I_STATUS);

        assertEquals(List.of(I_OKELLO), found);
        assertFalse("the case whose lab number shares the start is not listed", found.contains(I_KERING));
    }

    @Test
    public void iStartOfLabNumber_inLowerCase_findsBothCasesUnderThatStart() {
        List<Integer> found = i("aps2600000", I_BOTH);

        assertEquals(List.of(I_OKELLO, I_KERING), found);
        assertFalse("a lab number under another start is not listed", found.contains(I_OTHER_STATUS));
    }

    @Test
    public void iNameInAnotherLetterCase_findsThatPatientsCase() {
        assertEquals("a last name typed in lower case", List.of(I_OKELLO), i("okello", I_STATUS));
        assertEquals("a first name typed in upper case", List.of(I_OKELLO), i("ACHIENG", I_STATUS));
        assertFalse("the other patient's case is not listed", i("okello", I_STATUS).contains(I_KERING));
    }

    @Test
    public void iPartOfName_findsThatPatientsCase() {
        assertEquals("part of a first name", List.of(I_OKELLO), i("chien", I_STATUS));
        assertEquals("part of a last name", List.of(I_OKELLO), i("kel", I_STATUS));
        assertFalse("the other patient's case is not listed", i("chien", I_STATUS).contains(I_KERING));
    }

    @Test
    public void iDottedLabel_findsItsCase() {
        List<Integer> found = i("APS26000001.1", I_STATUS);

        assertEquals(List.of(I_OKELLO), found);
        assertFalse("the sibling case is not listed", found.contains(I_KERING));
    }

    @Test
    public void iNoMatch_findsNothing() {
        assertEquals("setup: two cases stand in the status", 2, i("", I_STATUS).size());
        assertTrue(i("zzq", I_STATUS).isEmpty());
    }

    @Test
    public void iNameMatchInAnotherStatus_isNotListed() {
        assertEquals(List.of(I_OKELLO), i("okello", I_STATUS));
        assertEquals("with both statuses the same term lists it", List.of(I_OKELLO, I_OTHER_STATUS),
                i("okello", I_BOTH));
    }

    // ---- Cytology ----

    @Test
    public void cWholeLabNumber_findsThatCaseOnly() {
        List<Integer> found = c("APS26000001", C_STATUS);

        assertEquals(List.of(C_OKELLO), found);
        assertFalse("the case whose lab number shares the start is not listed", found.contains(C_KERING));
    }

    @Test
    public void cStartOfLabNumber_inLowerCase_findsBothCasesUnderThatStart() {
        List<Integer> found = c("aps2600000", C_BOTH);

        assertEquals(List.of(C_OKELLO, C_KERING), found);
        assertFalse("a lab number under another start is not listed", found.contains(C_OTHER_STATUS));
    }

    @Test
    public void cNameInAnotherLetterCase_findsThatPatientsCase() {
        assertEquals("a last name typed in lower case", List.of(C_OKELLO), c("okello", C_STATUS));
        assertEquals("a first name typed in upper case", List.of(C_OKELLO), c("ACHIENG", C_STATUS));
        assertFalse("the other patient's case is not listed", c("okello", C_STATUS).contains(C_KERING));
    }

    @Test
    public void cPartOfName_findsThatPatientsCase() {
        assertEquals("part of a first name", List.of(C_OKELLO), c("chien", C_STATUS));
        assertEquals("part of a last name", List.of(C_OKELLO), c("kel", C_STATUS));
        assertFalse("the other patient's case is not listed", c("chien", C_STATUS).contains(C_KERING));
    }

    @Test
    public void cDottedLabel_findsItsCase() {
        List<Integer> found = c("APS26000001.1", C_STATUS);

        assertEquals(List.of(C_OKELLO), found);
        assertFalse("the sibling case is not listed", found.contains(C_KERING));
    }

    @Test
    public void cNoMatch_findsNothing() {
        assertEquals("setup: two cases stand in the status", 2, c("", C_STATUS).size());
        assertTrue(c("zzq", C_STATUS).isEmpty());
    }

    @Test
    public void cNameMatchInAnotherStatus_isNotListed() {
        assertEquals(List.of(C_OKELLO), c("okello", C_STATUS));
        assertEquals("with both statuses the same term lists it", List.of(C_OKELLO, C_OTHER_STATUS),
                c("okello", C_BOTH));
    }

    // helpers

    private List<Integer> p(String term, List<PathologyStatus> statuses) {
        return ids(pathologySampleService.searchWithStatusAndTerm(statuses, term));
    }

    private List<Integer> i(String term, List<ImmunohistochemistryStatus> statuses) {
        return ids(immunohistochemistrySampleService.searchWithStatusAndTerm(statuses, term));
    }

    private List<Integer> c(String term, List<CytologyStatus> statuses) {
        return ids(cytologySampleService.searchWithStatusAndTerm(statuses, term));
    }

    /** The ids of the cases found, in id order. */
    private static List<Integer> ids(List<? extends ProgramSample> cases) {
        return cases.stream().map(ProgramSample::getId).sorted().toList();
    }
}
