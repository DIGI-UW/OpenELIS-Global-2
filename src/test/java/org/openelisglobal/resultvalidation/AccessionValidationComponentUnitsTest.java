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

/**
 * Each part of a multi-part result reaches the validator with its own unit, as
 * Results Entry shows it. Fixture: {@code testdata/component-units.xml} —
 * accession VAL-CU-001, a viral load in copies/ml and its Ct, which has no
 * unit.
 */
@Transactional
public class AccessionValidationComponentUnitsTest extends BaseWebContextSensitiveTest {

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
        executeDataSetWithStateManagement("testdata/component-units.xml");
        authenticateAs("testUser");
        userService.saveUserLabUnitRoles(systemUserService.get("1"), Map.of("AllLabUnits", Set.of("9400")), "1");
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    public void eachPartOfTheResultCarriesItsOwnUnit() {
        Map<String, String> unitsByRow = validationUtility
                .getValidationAnalysisBySample(sampleService.getSampleByAccessionNumber("VAL-CU-001")).stream()
                .collect(Collectors.toMap(AnalysisItem::getTestName, AnalysisItem::getUnits));

        assertEquals(Map.of("Viral Load(Plasma) — Viral load", "cp/ml VCU", "Viral Load(Plasma) — Ct", ""), unitsByRow);
    }
}
