package org.openelisglobal.systemuser.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.TestSection;

/**
 * T031 - Verifies that filterResultsByLabUnitRoles and
 * filterAnalysesByLabUnitRoles correctly scope results to the test sections
 * assigned to a user for a given role.
 *
 * getUserTestSections() is deeply coupled to RequestContextHolder and
 * HttpSession. A subclass overrides it to return a controlled test-section
 * list, letting these tests focus purely on the filtering logic.
 *
 * Scenario: two lab units - Hematology (section id="10") and Chemistry (section
 * id="20"). A user assigned the Results role in Hematology only sees Hematology
 * tests; a user with both sections sees everything.
 */
@RunWith(MockitoJUnitRunner.class)
public class UserServiceLabUnitFilterTest {

    private static final String HEMATOLOGY_SECTION_ID = "10";
    private static final String CHEMISTRY_SECTION_ID = "20";
    private static final String HEMATOLOGY_TEST_ID = "100";
    private static final String CHEMISTRY_TEST_ID = "200";
    private static final String ROLE_NAME = Constants.ROLE_RESULTS;
    private static final int RESULTS_ROLE_ID = 2;

    private RoleService roleService;
    private TestService testService;
    private AnalysisService analysisService;

    private UserServiceImpl serviceWithHematologyOnly;
    private UserServiceImpl serviceWithBothSections;
    private UserServiceImpl serviceWithNoSections;

    @Before
    public void setup() {
        roleService = org.mockito.Mockito.mock(RoleService.class);
        testService = org.mockito.Mockito.mock(TestService.class);
        analysisService = org.mockito.Mockito.mock(AnalysisService.class);
        // lenient: the buildService override keeps these fixtures off the viewer
        // path, but the mock must still answer if a future test does reach it.
        org.mockito.Mockito.lenient().when(analysisService.getTestSectionIdsWithAnyAnalyses())
                .thenReturn(java.util.Set.of());

        Role resultsRole = new Role();
        resultsRole.setId(RESULTS_ROLE_ID);
        resultsRole.setName(ROLE_NAME);
        when(roleService.getRoleByName(ROLE_NAME)).thenReturn(resultsRole);

        // develop's analysisIsInAllowedUnit resolves a row's unit from
        // testService.getTestById(testId).getTestSection(), not from the
        // section->tests lookup these fixtures used to rely on. Without a
        // TestSection on each Test the helper falls through to "allow", and
        // every one of these filters kept rows it should have dropped.
        TestSection hemSection = new TestSection();
        hemSection.setId(HEMATOLOGY_SECTION_ID);
        TestSection chemSection = new TestSection();
        chemSection.setId(CHEMISTRY_SECTION_ID);

        org.openelisglobal.test.valueholder.Test hemTest = new org.openelisglobal.test.valueholder.Test();
        hemTest.setId(HEMATOLOGY_TEST_ID);
        hemTest.setTestSection(hemSection);

        org.openelisglobal.test.valueholder.Test chemTest = new org.openelisglobal.test.valueholder.Test();
        chemTest.setId(CHEMISTRY_TEST_ID);
        chemTest.setTestSection(chemSection);

        when(testService.getTestById(HEMATOLOGY_TEST_ID)).thenReturn(hemTest);
        when(testService.getTestById(CHEMISTRY_TEST_ID)).thenReturn(chemTest);

        serviceWithHematologyOnly = buildService(List.of(new IdValuePair(HEMATOLOGY_SECTION_ID, "Hematology")));
        serviceWithBothSections = buildService(List.of(new IdValuePair(HEMATOLOGY_SECTION_ID, "Hematology"),
                new IdValuePair(CHEMISTRY_SECTION_ID, "Chemistry")));
        serviceWithNoSections = buildService(Collections.emptyList());
    }

    private UserServiceImpl buildService(List<IdValuePair> fixedSections) {
        UserServiceImpl svc = new UserServiceImpl() {
            @Override
            public List<IdValuePair> getUserTestSections(String systemUserId, String roleId) {
                return fixedSections;
            }

            // develop routed the filters through getUserViewerTestSections, which
            // ADDS deactivated units that still hold analyses. This fixture is
            // about which units the user may see at all, so pin it to the same
            // fixed set; the viewer widening has its own coverage.
            @Override
            public List<IdValuePair> getUserViewerTestSections(String systemUserId, String roleId) {
                return fixedSections;
            }
        };
        injectField(svc, "roleService", roleService);
        injectField(svc, "testService", testService);
        // Injected so any path that does reach the real viewer method has its
        // dependency; the override above keeps these fixtures off that path.
        injectField(svc, "analysisService", analysisService);
        return svc;
    }

    // --- filterResultsByLabUnitRoles ---

    @Test
    public void filterResults_hematologyUser_keepsHematologyItems() {
        List<TestResultItem> input = Arrays.asList(resultItem(HEMATOLOGY_TEST_ID), resultItem(CHEMISTRY_TEST_ID));

        List<TestResultItem> result = serviceWithHematologyOnly.filterResultsByLabUnitRoles("1", input, ROLE_NAME);

        assertEquals(1, result.size());
        assertEquals(HEMATOLOGY_TEST_ID, result.get(0).getTestId());
    }

    @Test
    public void filterResults_hematologyUser_excludesChemistryItems() {
        List<TestResultItem> input = List.of(resultItem(CHEMISTRY_TEST_ID));

        List<TestResultItem> result = serviceWithHematologyOnly.filterResultsByLabUnitRoles("1", input, ROLE_NAME);

        assertTrue(result.isEmpty());
    }

    @Test
    public void filterResults_bothSectionsUser_keepsAll() {
        List<TestResultItem> input = Arrays.asList(resultItem(HEMATOLOGY_TEST_ID), resultItem(CHEMISTRY_TEST_ID));

        List<TestResultItem> result = serviceWithBothSections.filterResultsByLabUnitRoles("1", input, ROLE_NAME);

        assertEquals(2, result.size());
    }

    @Test
    public void filterResults_noSections_returnsEmpty() {
        List<TestResultItem> input = List.of(resultItem(HEMATOLOGY_TEST_ID));

        List<TestResultItem> result = serviceWithNoSections.filterResultsByLabUnitRoles("1", input, ROLE_NAME);

        assertTrue(result.isEmpty());
    }

    @Test
    public void filterResults_emptyInput_returnsEmpty() {
        List<TestResultItem> result = serviceWithHematologyOnly.filterResultsByLabUnitRoles("1",
                Collections.emptyList(), ROLE_NAME);

        assertTrue(result.isEmpty());
    }

    // --- filterAnalysesByLabUnitRoles ---

    @Test
    public void filterAnalyses_hematologyUser_keepsHematologyAnalyses() {
        List<Analysis> input = Arrays.asList(analysis(HEMATOLOGY_TEST_ID), analysis(CHEMISTRY_TEST_ID));

        List<Analysis> result = serviceWithHematologyOnly.filterAnalysesByLabUnitRoles("1", input, ROLE_NAME);

        assertEquals(1, result.size());
        assertEquals(HEMATOLOGY_TEST_ID, result.get(0).getTest().getId());
    }

    @Test
    public void filterAnalyses_hematologyUser_excludesChemistryAnalyses() {
        List<Analysis> input = List.of(analysis(CHEMISTRY_TEST_ID));

        List<Analysis> result = serviceWithHematologyOnly.filterAnalysesByLabUnitRoles("1", input, ROLE_NAME);

        assertTrue(result.isEmpty());
    }

    @Test
    public void filterAnalyses_bothSectionsUser_keepsAll() {
        List<Analysis> input = Arrays.asList(analysis(HEMATOLOGY_TEST_ID), analysis(CHEMISTRY_TEST_ID));

        List<Analysis> result = serviceWithBothSections.filterAnalysesByLabUnitRoles("1", input, ROLE_NAME);

        assertEquals(2, result.size());
    }

    @Test
    public void filterAnalyses_noSections_returnsEmpty() {
        List<Analysis> input = List.of(analysis(HEMATOLOGY_TEST_ID));

        List<Analysis> result = serviceWithNoSections.filterAnalysesByLabUnitRoles("1", input, ROLE_NAME);

        assertTrue(result.isEmpty());
    }

    @Test
    public void filterAnalyses_emptyInput_returnsEmpty() {
        List<Analysis> result = serviceWithHematologyOnly.filterAnalysesByLabUnitRoles("1", Collections.emptyList(),
                ROLE_NAME);

        assertTrue(result.isEmpty());
    }

    // --- boundary ---

    /**
     * A row whose test cannot be resolved to a lab unit is KEPT, not dropped.
     *
     * <p>
     * This assertion used to be the opposite. develop's
     * {@code analysisIsInAllowedUnit} (OGC-189) deliberately falls open when it
     * cannot place a row: the closed version made pending work vanish from every
     * worklist the moment its lab unit was deactivated, and it could then never be
     * completed. An unplaceable row is a data problem, not an authorization one,
     * and the unit filter is a view scope rather than a security boundary — the
     * endpoint's own @PreAuthorize is what gates access.
     */
    @Test
    public void filterResults_unknownTestId_isKeptBecauseTheFilterFallsOpen() {
        List<TestResultItem> input = List.of(resultItem("999"));

        List<TestResultItem> result = serviceWithBothSections.filterResultsByLabUnitRoles("1", input, ROLE_NAME);

        assertTrue(result.contains(input.get(0)));
    }

    /** Same fall-open contract as the TestResultItem variant above. */
    @Test
    public void filterAnalyses_unknownTestId_isKeptBecauseTheFilterFallsOpen() {
        List<Analysis> input = List.of(analysis("999"));

        List<Analysis> result = serviceWithBothSections.filterAnalysesByLabUnitRoles("1", input, ROLE_NAME);

        assertEquals(1, result.size());
    }

    // --- helpers ---

    private TestResultItem resultItem(String testId) {
        TestResultItem item = new TestResultItem();
        item.setTestId(testId);
        return item;
    }

    private Analysis analysis(String testId) {
        // develop's filter judges an analysis by its OWN lab unit first, falling
        // back to the test's configured unit. Both need a TestSection here or the
        // helper allows the row through and the filter looks broken.
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId(testId);
        Analysis a = new Analysis();
        a.setTest(test);
        // Only the two known tests belong to a lab unit. An id outside them stays
        // unplaceable on purpose, which is what exercises the fall-open branch.
        if (HEMATOLOGY_TEST_ID.equals(testId) || CHEMISTRY_TEST_ID.equals(testId)) {
            TestSection section = new TestSection();
            section.setId(HEMATOLOGY_TEST_ID.equals(testId) ? HEMATOLOGY_SECTION_ID : CHEMISTRY_SECTION_ID);
            test.setTestSection(section);
            a.setTestSection(section);
        }
        return a;
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = findField(target.getClass(), fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject field " + fieldName, e);
        }
    }

    private java.lang.reflect.Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        while (cls != null) {
            try {
                return cls.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
