package org.openelisglobal.compliance.daoimpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;
import org.openelisglobal.compliance.valueholder.ThresholdType;

/**
 * Unit test suite for {@link ComplianceThresholdDAOImpl}. Mocks the
 * EntityManager so the suite stays free of a real database while still
 * exercising HQL bindings and the DAO's exception-translation contract.
 */
@RunWith(MockitoJUnitRunner.class)
public class ComplianceThresholdDAOImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<ComplianceThreshold> typedQuery;

    @Mock
    private TypedQuery<Long> longTypedQuery;

    @Mock
    private TypedQuery<Object[]> objectArrayTypedQuery;

    @InjectMocks
    private ComplianceThresholdDAOImpl complianceThresholdDAO;

    private ComplianceThreshold testThreshold;
    private final String testGroupId = "GROUP_001";
    private final String testThresholdId = "THRESHOLD_001";

    @Before
    public void setUp() {
        testThreshold = createTestThreshold();
        testThreshold.setId(testThresholdId);
    }

    @Test
    public void testGetThresholdsByGroupId_shouldReturnOrderedThresholds() throws LIMSRuntimeException {
        List<ComplianceThreshold> expectedThresholds = Arrays.asList(createThreshold("PH", "pH Level", 1),
                createThreshold("TEMP", "Temperature", 2), createThreshold("TURB", "Turbidity", 3));

        when(entityManager.createQuery(
                "SELECT DISTINCT ct FROM ComplianceThreshold ct "
                        + "LEFT JOIN FETCH ct.group g LEFT JOIN FETCH g.standard "
                        + "LEFT JOIN FETCH ct.valueMappings WHERE ct.group.id = :groupId ORDER BY ct.sortOrder",
                ComplianceThreshold.class)).thenReturn(typedQuery);
        when(typedQuery.setParameter("groupId", testGroupId)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(expectedThresholds);

        List<ComplianceThreshold> result = complianceThresholdDAO.getThresholdsByGroupId(testGroupId);

        assertNotNull("Results should not be null", result);
        assertEquals("Should return 3 thresholds", 3, result.size());
        assertEquals("First threshold should be PH", "PH", result.get(0).getParameterCode());
        assertEquals("Second threshold should be TEMP", "TEMP", result.get(1).getParameterCode());
        assertEquals("Third threshold should be TURB", "TURB", result.get(2).getParameterCode());
        verify(typedQuery).setParameter("groupId", testGroupId);
    }

    @Test
    public void testGetThresholdsByTestId_shouldReturnTestSpecificThresholds() throws LIMSRuntimeException {
        String testId = "TEST_001";
        List<ComplianceThreshold> expectedThresholds = Arrays.asList(createTestSpecificThreshold("PH", testId),
                createTestSpecificThreshold("TEMP", testId));

        when(entityManager.createQuery(
                "SELECT DISTINCT ct FROM ComplianceThreshold ct "
                        + "LEFT JOIN FETCH ct.group g LEFT JOIN FETCH g.standard "
                        + "LEFT JOIN FETCH ct.valueMappings WHERE ct.test.id = :testId ORDER BY ct.sortOrder",
                ComplianceThreshold.class)).thenReturn(typedQuery);
        when(typedQuery.setParameter("testId", testId)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(expectedThresholds);

        List<ComplianceThreshold> result = complianceThresholdDAO.getThresholdsByTestId(testId);

        assertNotNull("Results should not be null", result);
        assertEquals("Should return 2 test-specific thresholds", 2, result.size());
        for (ComplianceThreshold threshold : result) {
            assertEquals("All thresholds should have test ID " + testId, testId, threshold.getTestId());
        }
    }

    @Test
    public void testGetThresholdsByTestAndStandard_shouldReturnFilteredThresholds() throws LIMSRuntimeException {
        String testId = "TEST_001";
        String standardId = "STD_001";
        List<ComplianceThreshold> expectedThresholds = Arrays.asList(createTestSpecificThreshold("PH", testId),
                createTestSpecificThreshold("TEMP", testId));

        when(entityManager.createQuery("SELECT DISTINCT ct FROM ComplianceThreshold ct "
                + "JOIN FETCH ct.group pg LEFT JOIN FETCH ct.valueMappings " + "WHERE pg.standard.id = :standardId "
                + "AND (ct.test.id = :testId " + "     OR (ct.test IS NULL AND LOWER(ct.parameterCode) = ("
                + "         SELECT LOWER(t.name) FROM org.openelisglobal.test.valueholder.Test t"
                + "         WHERE t.id = :testId))) " + "ORDER BY pg.sortOrder, ct.sortOrder",
                ComplianceThreshold.class)).thenReturn(typedQuery);
        when(typedQuery.setParameter("testId", testId)).thenReturn(typedQuery);
        when(typedQuery.setParameter("standardId", standardId)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(expectedThresholds);

        List<ComplianceThreshold> result = complianceThresholdDAO.getThresholdsByTestAndStandard(testId, standardId);

        assertNotNull("Results should not be null", result);
        assertEquals("Should return 2 filtered thresholds", 2, result.size());
        verify(typedQuery).setParameter("testId", testId);
        verify(typedQuery).setParameter("standardId", standardId);
    }

    @Test
    public void testParameterExistsInGroupForType_shouldReturnTrueWhenExists() throws LIMSRuntimeException {
        String parameterCode = "PH";
        when(entityManager.createQuery(
                "SELECT COUNT(ct) FROM ComplianceThreshold ct" + " WHERE ct.group.id = :groupId"
                        + " AND ct.parameterCode = :parameterCode" + " AND ct.thresholdType = :thresholdType",
                Long.class)).thenReturn(longTypedQuery);
        when(longTypedQuery.setParameter("groupId", testGroupId)).thenReturn(longTypedQuery);
        when(longTypedQuery.setParameter("parameterCode", parameterCode)).thenReturn(longTypedQuery);
        when(longTypedQuery.setParameter("thresholdType", ThresholdType.RANGE)).thenReturn(longTypedQuery);
        when(longTypedQuery.getSingleResult()).thenReturn(1L);

        boolean exists = complianceThresholdDAO.parameterExistsInGroupForType(testGroupId, parameterCode,
                ThresholdType.RANGE);

        assertTrue("Parameter+type combo should exist in group", exists);
    }

    @Test
    public void testGetTestThresholdSummary_shouldGroupByTest() throws LIMSRuntimeException {
        List<Object[]> expectedRows = Arrays.asList(new Object[] { "TEST_001", 2L, 1L },
                new Object[] { "TEST_002", 5L, 2L });
        when(entityManager.createQuery(
                "SELECT t.id, COUNT(DISTINCT ct.id), COUNT(DISTINCT s.id) " + "FROM ComplianceThreshold ct "
                        + "JOIN ct.test t " + "JOIN ct.group pg " + "JOIN pg.standard s " + "GROUP BY t.id",
                Object[].class)).thenReturn(objectArrayTypedQuery);
        when(objectArrayTypedQuery.getResultList()).thenReturn(expectedRows);

        List<Object[]> result = complianceThresholdDAO.getTestThresholdSummary();

        assertNotNull("Results should not be null", result);
        assertEquals("Should return 2 rows", 2, result.size());
    }

    @Test
    public void testGroupHasThresholds_shouldReturnTrueWhenAtLeastOne() throws LIMSRuntimeException {
        when(entityManager.createQuery("SELECT COUNT(ct) FROM ComplianceThreshold ct WHERE ct.group.id = :groupId",
                Long.class)).thenReturn(longTypedQuery);
        when(longTypedQuery.setParameter("groupId", testGroupId)).thenReturn(longTypedQuery);
        when(longTypedQuery.getSingleResult()).thenReturn(3L);

        assertTrue("Group with thresholds should report true",
                complianceThresholdDAO.groupHasThresholds(testGroupId));
    }

    @Test
    public void testStandardHasThresholds_shouldReturnTrueWhenAtLeastOne() throws LIMSRuntimeException {
        String standardId = "STD_001";
        when(entityManager.createQuery(
                "SELECT COUNT(ct) FROM ComplianceThreshold ct WHERE ct.group.standard.id = :standardId", Long.class))
                .thenReturn(longTypedQuery);
        when(longTypedQuery.setParameter("standardId", standardId)).thenReturn(longTypedQuery);
        when(longTypedQuery.getSingleResult()).thenReturn(7L);

        assertTrue("Standard with thresholds should report true",
                complianceThresholdDAO.standardHasThresholds(standardId));
    }

    @Test
    public void testGetThresholdsByGroupId_shouldHandleRuntimeException() {
        when(entityManager.createQuery(any(String.class), eq(ComplianceThreshold.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        try {
            complianceThresholdDAO.getThresholdsByGroupId(testGroupId);
            fail("Should throw LIMSRuntimeException");
        } catch (LIMSRuntimeException e) {
            assertTrue("Should contain original error message",
                    e.getMessage().contains("ComplianceThreshold getThresholdsByGroupId"));
            assertTrue("Should contain cause", e.getCause() instanceof RuntimeException);
        }
    }

    private ComplianceThreshold createTestThreshold() {
        ComplianceThreshold threshold = new ComplianceThreshold();
        threshold.setParameterCode("TEST_PARAM");
        threshold.setDisplayName("Test Parameter");
        threshold.setThresholdType(ThresholdType.RANGE);
        threshold.setMinValue(new BigDecimal("5.0"));
        threshold.setMaxValue(new BigDecimal("10.0"));
        threshold.setUnits("mg/L");
        threshold.setSortOrder(1);
        threshold.setSysUserId("1");
        return threshold;
    }

    private ComplianceThreshold createThreshold(String parameterCode, String displayName, int sortOrder) {
        ComplianceThreshold threshold = createTestThreshold();
        threshold.setParameterCode(parameterCode);
        threshold.setDisplayName(displayName);
        threshold.setSortOrder(sortOrder);
        return threshold;
    }

    private ComplianceThreshold createTestSpecificThreshold(String parameterCode, String testId) {
        ComplianceThreshold threshold = createTestThreshold();
        threshold.setParameterCode(parameterCode);
        threshold.setTestId(testId);
        return threshold;
    }
}
