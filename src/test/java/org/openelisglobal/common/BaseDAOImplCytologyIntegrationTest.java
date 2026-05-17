package org.openelisglobal.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.service.cytology.CytologySampleService;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.cytology.CytologySample.CytologyStatus;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * BaseDAOImpl integration tests covering CytologySample (enum-typed status
 * field). Kept separate from {@link BaseDAOImplAnalysisIntegrationTest} because
 * cytology.xml and analysis.xml both reference the sample table — loading both
 * in the same {@code @Before} would cause the second TRUNCATE to cascade-wipe
 * the first.
 */
public class BaseDAOImplCytologyIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private CytologySampleService cytologySampleService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/cytology.xml");
    }

    // --- Enum auto-conversion tests (EQ) ---
    // CytologySample.status is @Enumerated(EnumType.STRING) CytologyStatus.
    // Passing a String like "COMPLETED" must be auto-converted to the enum
    // constant before building criteriaBuilder.equal().

    @Test
    public void addWhere_EQ_enumProperty_shouldConvertStringToEnum() throws Exception {
        List<CytologySample> results = cytologySampleService.getAllMatching("status", "COMPLETED");
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(2, results.get(0).getId().intValue());
    }

    @Test
    public void addWhere_EQ_enumPropertyViaMap_shouldConvertStringToEnum() throws Exception {
        Map<String, Object> props = Map.of("status", "COMPLETED");
        List<CytologySample> results = cytologySampleService.getAllMatching(props);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(2, results.get(0).getId().intValue());
    }

    @Test
    public void addWhere_EQ_enumProperty_differentValue() throws Exception {
        List<CytologySample> results = cytologySampleService.getAllMatching("status", "READY_FOR_CYTOPATHOLOGIST");
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getId().intValue());
    }

    @Test
    public void addWhere_EQ_enumProperty_actualEnumValue_shouldWork() throws Exception {
        List<CytologySample> results = cytologySampleService.getAllMatching("status", CytologyStatus.COMPLETED);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(2, results.get(0).getId().intValue());
    }

    // --- Enum LIKE tests ---
    // For LIKE on enum properties, addWhere uses pathToProperty.as(String.class)
    // to cast the enum column to a string before applying the LIKE pattern.

    @Test
    public void addWhere_LIKE_enumProperty_exactMatch() throws Exception {
        int count = cytologySampleService.getCountLike("status", "COMPLETED");
        assertEquals(1, count);
    }

    @Test
    public void addWhere_LIKE_enumProperty_partialMatch() throws Exception {
        int count = cytologySampleService.getCountLike("status", "PREPARING");
        assertEquals(1, count);
    }

    @Test
    public void addWhere_LIKE_enumPropertyViaMap() throws Exception {
        Map<String, String> props = Map.of("status", "COMPLETED");
        int count = cytologySampleService.getCountLike(props);
        assertEquals(1, count);
    }

    @Test
    public void addWhere_LIKE_enumProperty_caseInsensitive() throws Exception {
        int count = cytologySampleService.getCountLike("status", "completed");
        assertEquals(1, count);
    }

    // --- Combined: enum with ordering ---

    @Test
    public void addWhere_EQ_enumPropertyWithOrdering_shouldWork() throws Exception {
        List<CytologySample> results = cytologySampleService.getAllMatchingOrdered("status",
                "READY_FOR_CYTOPATHOLOGIST", "id", false);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getId().intValue());
    }
}
