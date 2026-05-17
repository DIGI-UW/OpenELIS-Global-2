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
import org.springframework.beans.factory.annotation.Autowired;

/**
 * BaseDAOImpl integration tests covering Analysis (LIMSStringNumberUserType
 * String-mapped statusId field). Kept separate from
 * {@link BaseDAOImplCytologyIntegrationTest} because analysis.xml and
 * cytology.xml both reference the sample table — loading both in the same
 * {@code @Before} would cause the second TRUNCATE to cascade-wipe the first.
 */
public class BaseDAOImplAnalysisIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private AnalysisDAO analysisDAO;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/analysis.xml");
    }

    // --- Integer type guard tests ---
    // Analysis.statusId is String in Java (via LIMSStringNumberUserType), NUMERIC
    // in DB. The old code would convert "1" -> Integer(1) because the property
    // name ends in "Id", which caused a type mismatch. The new code checks
    // pathToProperty.getJavaType() and only converts when the Java type is
    // Integer/int.

    @Test
    public void addWhere_EQ_stringIdProperty_shouldNotConvertToInteger() throws Exception {
        List<Analysis> results = analysisService.getAllMatching("statusId", "1");
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("1", results.get(0).getStatusId());
    }

    @Test
    public void addWhere_EQ_stringIdPropertyViaMap_shouldNotConvertToInteger() throws Exception {
        Map<String, Object> props = Map.of("statusId", "1");
        List<Analysis> results = analysisService.getAllMatching(props);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("1", results.get(0).getStatusId());
    }

    // --- IN clause ---
    // The IN clause was changed from In<String> to raw CriteriaBuilder.In to
    // accept any type. Verify it works with String PKs (Analysis uses String PK
    // via LIMSStringNumberUserType).

    @Test
    public void addWhere_IN_stringPKList_shouldReturnMatchingEntities() throws Exception {
        List<Analysis> results = analysisDAO.get(List.of("1", "2"));
        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(a -> "1".equals(a.getId())));
        assertTrue(results.stream().anyMatch(a -> "2".equals(a.getId())));
    }
}
