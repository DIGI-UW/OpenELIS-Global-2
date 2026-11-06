package org.openelisglobal.resultvalidation;

import static org.junit.Assert.assertEquals;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.resultvalidation.util.ResultsValidationUtility;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AccessionValidationComponentSelectionsTest extends BaseWebContextSensitiveTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private ResultsValidationUtility validationUtility;

    @Autowired
    private UserService userService;

    @Autowired
    private SystemUserService systemUserService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/component-selections.xml");
        authenticateAs("testUser");
        userService.saveUserLabUnitRoles(systemUserService.get("1"), Map.of("AllLabUnits", Set.of("9400")), "1");
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    public void eachSelectionPartIsItsOwnRowWithItsOwnSelections() {
        Map<String, String> selectionsByRow = validationUtility
                .getValidationAnalysisBySample(sampleService.getSampleByAccessionNumber("VAL-MS-001")).stream()
                .collect(Collectors.toMap(AnalysisItem::getTestName, AnalysisItem::getMultiSelectResultValues));

        assertEquals(Map.of("Appearance(Swab) — Colour", "{\"1\":\"9301,9303\"}", "Appearance(Swab) — Size",
                "{\"1\":\"9302\"}"), selectionsByRow);
    }
}
