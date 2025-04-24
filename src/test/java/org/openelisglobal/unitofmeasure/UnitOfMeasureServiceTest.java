package org.openelisglobal.unitofmeasure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;

public class UnitOfMeasureServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private UnitOfMeasureService unitOfMeasureService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/unit-of-measure.xml");
    }

    @Test
    public void testDataInDatabase() {
        List<UnitOfMeasure> unitOfMeasures = unitOfMeasureService.getAll();
        assertNotNull(unitOfMeasures);
        assertTrue(unitOfMeasures.size() >= 10);

    }

    @Test
    public void getUnitOfMeasureById_shouldReturnCorrectUnitOfMeasure() {
        UnitOfMeasure unitOfMeasure = unitOfMeasureService.getUnitOfMeasureById("1");
        assertNotNull(unitOfMeasure);
        assertEquals("mg/dL", unitOfMeasure.getUnitOfMeasureName());
        assertEquals("Milligrams per deciliter", unitOfMeasure.getDescription());
    }

    @Test
    public void getUnitOfMeasureByName_shouldReturnCorrectUnitOfMeasure() {
        UnitOfMeasure searchUnit = new UnitOfMeasure();
        searchUnit.setUnitOfMeasureName("mmol/L");

        UnitOfMeasure unitOfMeasure = unitOfMeasureService.getUnitOfMeasureByName(searchUnit);
        assertNotNull(unitOfMeasure);
        assertEquals("mmol/L", unitOfMeasure.getUnitOfMeasureName());
        assertEquals("3", unitOfMeasure.getId());
        assertEquals("Millimoles per liter", unitOfMeasure.getDescription());
    }

    @Test
    public void insert_shouldInsertNewUnitOfMeasure() {
        UnitOfMeasure unitOfMeasure = new UnitOfMeasure();
        unitOfMeasure.setUnitOfMeasureName("pg/mL");
        unitOfMeasure.setDescription("Picograms per milliliter");

        String id = unitOfMeasureService.insert(unitOfMeasure);
        assertNotNull(id);

        UnitOfMeasure savedUnit = unitOfMeasureService.getUnitOfMeasureById(id);
        assertNotNull(savedUnit);
        assertEquals("pg/mL", savedUnit.getUnitOfMeasureName());
        assertEquals("Picograms per milliliter", savedUnit.getDescription());
    }

    @Test(expected = LIMSDuplicateRecordException.class)
    public void insert_shouldThrowExceptionForDuplicateUnitOfMeasure() {
        UnitOfMeasure unitOfMeasure = new UnitOfMeasure();
        unitOfMeasure.setUnitOfMeasureName("mg/dL");
        unitOfMeasure.setDescription("Duplicate unit test");

        unitOfMeasureService.insert(unitOfMeasure);
    }

    @Test
    public void save_shouldSaveNewUnitOfMeasure() {

        UnitOfMeasure unitOfMeasure = new UnitOfMeasure();
        unitOfMeasure.setUnitOfMeasureName("fmol/L");
        unitOfMeasure.setDescription("Femtomoles per liter");

        UnitOfMeasure savedUnit = unitOfMeasureService.save(unitOfMeasure);
        assertNotNull(savedUnit);
        assertNotNull(savedUnit.getId());

        UnitOfMeasure retrievedUnit = unitOfMeasureService.getUnitOfMeasureById(savedUnit.getId());
        assertEquals("fmol/L", retrievedUnit.getUnitOfMeasureName());
        assertEquals("Femtomoles per liter", retrievedUnit.getDescription());
    }

    @Test(expected = LIMSDuplicateRecordException.class)
    public void update_shouldThrowExceptionForDuplicateUnitName() {
        UnitOfMeasure unitOfMeasure = unitOfMeasureService.getUnitOfMeasureById("4");
        assertNotNull(unitOfMeasure);

        unitOfMeasure.setUnitOfMeasureName("mg/dL");

        unitOfMeasureService.update(unitOfMeasure);
    }

    @Test
    public void getAll_shouldReturnAllUnitsOfMeasure() {
        List<UnitOfMeasure> allUnits = unitOfMeasureService.getAll();

        assertTrue(allUnits.size() >= 10);

        boolean foundMgdL = false;
        boolean foundPercent = false;

        for (UnitOfMeasure unit : allUnits) {
            if ("mg/dL".equals(unit.getUnitOfMeasureName())) {
                foundMgdL = true;
            }
            if ("%".equals(unit.getUnitOfMeasureName())) {
                foundPercent = true;
            }
        }

        assertTrue("Should find mg/dL unit", foundMgdL);
        assertTrue("Should find % unit", foundPercent);
    }
}