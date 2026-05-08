package org.openelisglobal.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.dao.AnalysisDAO;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.program.service.cytology.CytologySampleService;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.cytology.CytologySample.CytologyStatus;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for BaseDAOImpl.addWhere() changes introduced when removing
 * ClassicQueryTranslatorFactory. These test the three behavioral changes in the
 * criteria-building logic:
 *
 * 1. Integer type guard: only convert String->Integer when the entity
 * property's Java type is Integer/int (not for LIMSStringNumberUserType String
 * fields) 2. Enum auto-conversion: convert String values to enum when the
 * entity property is an enum type 3. Enum LIKE: use .as(String.class) cast for
 * LIKE comparisons on enum properties
 *
 * Uses CytologySample (enum-typed 'status' field) and Analysis
 * (LIMSStringNumberUserType-mapped 'statusId' field) as concrete test subjects.
 * Datasets are loaded per-test to avoid table conflicts between cytology.xml
 * and analysis.xml (both reference the sample table).
 */
public class BaseDAOImplIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private CytologySampleService cytologySampleService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private AnalysisDAO analysisDAO;

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    // --- Integer type guard tests ---
    // Analysis.statusId is String in Java (via LIMSStringNumberUserType), NUMERIC
    // in DB.
    // The old code would convert "1" -> Integer(1) because the property name ends
    // in "Id",
    // which caused a type mismatch. The new code checks
    // pathToProperty.getJavaType() and
    // only converts when the Java type is Integer/int.

    @Test
    public void addWhere_EQ_stringIdProperty_shouldNotConvertToInteger() throws Exception {
        executeDataSetWithStateManagement("testdata/analysis.xml");
        List<Analysis> results = analysisService.getAllMatching("statusId", "1");
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("1", results.get(0).getStatusId());
    }

    @Test
    public void addWhere_EQ_stringIdPropertyViaMap_shouldNotConvertToInteger() throws Exception {
        executeDataSetWithStateManagement("testdata/analysis.xml");
        Map<String, Object> props = Map.of("statusId", "1");
        List<Analysis> results = analysisService.getAllMatching(props);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("1", results.get(0).getStatusId());
    }

    // --- Enum auto-conversion tests (EQ) ---
    // CytologySample.status is @Enumerated(EnumType.STRING) CytologyStatus.
    // Passing a String like "COMPLETED" must be auto-converted to the enum constant
    // before building criteriaBuilder.equal().

    @Test
    public void addWhere_EQ_enumProperty_shouldConvertStringToEnum() throws Exception {
        executeDataSetWithStateManagement("testdata/cytology.xml");
        List<CytologySample> results = cytologySampleService.getAllMatching("status", "COMPLETED");
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(2, results.get(0).getId().intValue());
    }

    @Test
    public void addWhere_EQ_enumPropertyViaMap_shouldConvertStringToEnum() throws Exception {
        executeDataSetWithStateManagement("testdata/cytology.xml");
        Map<String, Object> props = Map.of("status", "COMPLETED");
        List<CytologySample> results = cytologySampleService.getAllMatching(props);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(2, results.get(0).getId().intValue());
    }

    @Test
    public void addWhere_EQ_enumProperty_differentValue() throws Exception {
        executeDataSetWithStateManagement("testdata/cytology.xml");
        List<CytologySample> results = cytologySampleService.getAllMatching("status", "READY_FOR_CYTOPATHOLOGIST");
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getId().intValue());
    }

    @Test
    public void addWhere_EQ_enumProperty_actualEnumValue_shouldWork() throws Exception {
        executeDataSetWithStateManagement("testdata/cytology.xml");
        List<CytologySample> results = cytologySampleService.getAllMatching("status", CytologyStatus.COMPLETED);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(2, results.get(0).getId().intValue());
    }

    // --- Enum LIKE tests ---
    // For LIKE on enum properties, addWhere uses pathToProperty.as(String.class) to
    // cast
    // the enum column to a string before applying the LIKE pattern.

    @Test
    public void addWhere_LIKE_enumProperty_exactMatch() throws Exception {
        executeDataSetWithStateManagement("testdata/cytology.xml");
        int count = cytologySampleService.getCountLike("status", "COMPLETED");
        assertEquals(1, count);
    }

    @Test
    public void addWhere_LIKE_enumProperty_partialMatch() throws Exception {
        executeDataSetWithStateManagement("testdata/cytology.xml");
        // "PREPARING_SLIDES" should match LIKE %PREPARING%
        int count = cytologySampleService.getCountLike("status", "PREPARING");
        assertEquals(1, count);
    }

    @Test
    public void addWhere_LIKE_enumPropertyViaMap() throws Exception {
        executeDataSetWithStateManagement("testdata/cytology.xml");
        Map<String, String> props = Map.of("status", "COMPLETED");
        int count = cytologySampleService.getCountLike(props);
        assertEquals(1, count);
    }

    @Test
    public void addWhere_LIKE_enumProperty_caseInsensitive() throws Exception {
        executeDataSetWithStateManagement("testdata/cytology.xml");
        // LIKE uses criteriaBuilder.lower(), so "completed" should match "COMPLETED"
        int count = cytologySampleService.getCountLike("status", "completed");
        assertEquals(1, count);
    }

    // --- Combined: enum with ordering ---

    @Test
    public void addWhere_EQ_enumPropertyWithOrdering_shouldWork() throws Exception {
        executeDataSetWithStateManagement("testdata/cytology.xml");
        List<CytologySample> results = cytologySampleService.getAllMatchingOrdered("status",
                "READY_FOR_CYTOPATHOLOGIST", "id", false);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getId().intValue());
    }

    // --- IN clause ---
    // The IN clause was changed from In<String> to raw CriteriaBuilder.In to accept
    // any type. Verify it works with String PKs (Analysis uses String PK via
    // LIMSStringNumberUserType).

    @Test
    public void addWhere_IN_stringPKList_shouldReturnMatchingEntities() throws Exception {
        executeDataSetWithStateManagement("testdata/analysis.xml");
        List<Analysis> results = analysisDAO.get(List.of("1", "2"));
        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(a -> "1".equals(a.getId())));
        assertTrue(results.stream().anyMatch(a -> "2".equals(a.getId())));
    }
}
