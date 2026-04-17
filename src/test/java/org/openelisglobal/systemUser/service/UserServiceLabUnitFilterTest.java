package org.openelisglobal.systemUser.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemuser.service.UserServiceImpl;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.test.service.TestService;

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

    private UserServiceImpl serviceWithHematologyOnly;
    private UserServiceImpl serviceWithBothSections;
    private UserServiceImpl serviceWithNoSections;

    @Before
    public void setup() {
        roleService = org.mockito.Mockito.mock(RoleService.class);
        testService = org.mockito.Mockito.mock(TestService.class);

        Role resultsRole = new Role();
        resultsRole.setId(RESULTS_ROLE_ID);
        resultsRole.setName(ROLE_NAME);
        when(roleService.getRoleByName(ROLE_NAME)).thenReturn(resultsRole);

        org.openelisglobal.test.valueholder.Test hemTest = new org.openelisglobal.test.valueholder.Test();
        hemTest.setId(HEMATOLOGY_TEST_ID);

        org.openelisglobal.test.valueholder.Test chemTest = new org.openelisglobal.test.valueholder.Test();
        chemTest.setId(CHEMISTRY_TEST_ID);

        when(testService.getTestsByTestSectionIds(List.of(Integer.parseInt(HEMATOLOGY_SECTION_ID))))
                .thenReturn(List.of(hemTest));
        when(testService.getTestsByTestSectionIds(
                List.of(Integer.parseInt(HEMATOLOGY_SECTION_ID), Integer.parseInt(CHEMISTRY_SECTION_ID))))
                .thenReturn(List.of(hemTest, chemTest));
        when(testService.getTestsByTestSectionIds(Collections.emptyList())).thenReturn(Collections.emptyList());

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
        };
        injectField(svc, "roleService", roleService);
        injectField(svc, "testService", testService);
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

    @Test
    public void filterResults_unknownTestId_isExcluded() {
        List<TestResultItem> input = List.of(resultItem("999"));

        List<TestResultItem> result = serviceWithBothSections.filterResultsByLabUnitRoles("1", input, ROLE_NAME);

        assertFalse(result.contains(input.get(0)));
    }

    @Test
    public void filterAnalyses_unknownTestId_isExcluded() {
        List<Analysis> input = List.of(analysis("999"));

        List<Analysis> result = serviceWithBothSections.filterAnalysesByLabUnitRoles("1", input, ROLE_NAME);

        assertTrue(result.isEmpty());
    }

    // --- helpers ---

    private TestResultItem resultItem(String testId) {
        TestResultItem item = new TestResultItem();
        item.setTestId(testId);
        return item;
    }

    private Analysis analysis(String testId) {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId(testId);
        Analysis a = new Analysis();
        a.setTest(test);
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
