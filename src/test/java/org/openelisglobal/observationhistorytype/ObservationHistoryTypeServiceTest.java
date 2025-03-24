package org.openelisglobal.observationhistorytype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.springframework.beans.factory.annotation.Autowired;

public class ObservationHistoryTypeServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ObservationHistoryTypeService observationHistoryTypeService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/observation-history-type.xml");
    }

    // @Test
    public void testDataInDataBase() {
        List<ObservationHistoryType> observationHistoryTypes = observationHistoryTypeService.getAll();
        observationHistoryTypes.forEach(observationType -> {
            System.out.print(observationType.getTypeName() + " ");
        });
    }

    @Test
    public void getAll_shouldReturnAllObservationHistoryTypes() {
        List<ObservationHistoryType> observationHistoryTypes = observationHistoryTypeService.getAll();
        assertTrue(observationHistoryTypes.size() == 4);
    }

    @Test
    public void get_shouldReturnObservationHistoryTypeById() {
        ObservationHistoryType observationType = observationHistoryTypeService.get("1");
        assertEquals("EDUCATION_LEVEL", observationType.getTypeName());
        assertEquals("Patient's education level", observationType.getDescription());
    }

    @Test
    public void getByName_shouldReturnObservationHistoryTypeByName() {
        ObservationHistoryType observationType = observationHistoryTypeService.getByName("EDUCATION_LEVEL");
        assertEquals("1", observationType.getId());
        assertEquals("Patient's education level", observationType.getDescription());
    }

    // CRUD Tests

    @Test
    public void insert_shouldInsertObservationHistoryType() {
        ObservationHistoryType observationType = new ObservationHistoryType();
        observationType.setTypeName("EXERCISE_FREQUENCY");
        observationType.setDescription("Patient's exercise frequency");
        // observationType.setIsActive("Y");
        observationHistoryTypeService.insert(observationType);

        // Verify insertion
        ObservationHistoryType insertedType = observationHistoryTypeService.getByName("EXERCISE_FREQUENCY");
        assertNotNull(insertedType);
        assertEquals("EXERCISE_FREQUENCY", insertedType.getTypeName());
        assertEquals("Patient's exercise frequency", insertedType.getDescription());
    }

    @Test
    public void save_shouldSaveObservationHistoryType() {
        ObservationHistoryType observationType = new ObservationHistoryType();
        observationType.setTypeName("ALCOHOL_CONSUMPTION");
        observationType.setDescription("Patient's alcohol consumption habits");
        // observationType.setIsActive("Y");
        observationHistoryTypeService.save(observationType);

        // Verify save
        ObservationHistoryType savedType = observationHistoryTypeService.getByName("ALCOHOL_CONSUMPTION");
        assertNotNull(savedType);
        assertEquals("ALCOHOL_CONSUMPTION", savedType.getTypeName());
        assertEquals("Patient's alcohol consumption habits", savedType.getDescription());
    }

    @Test
    public void update_shouldUpdateObservationHistoryType() {
        // Get existing observation history type
        ObservationHistoryType observationType = observationHistoryTypeService.get("2");

        // Update fields
        observationType.setTypeName("RELATIONSHIP_STATUS");
        observationType.setDescription("Updated marital status description");

        // Perform update
        observationHistoryTypeService.update(observationType);

        // Verify update
        ObservationHistoryType updatedType = observationHistoryTypeService.get("2");
        assertEquals("RELATIONSHIP_STATUS", updatedType.getTypeName());
        assertEquals("Updated marital status description", updatedType.getDescription());
    }

    @Test
    public void delete_shouldInactivateObservationHistoryType() {
        // Get existing observation history type
        ObservationHistoryType observationType = observationHistoryTypeService.get("3");

        // Delete/inactivate
        observationHistoryTypeService.delete(observationType);

        // Verify deletion (should be marked as inactive)
        // ObservationHistoryType deletedType = observationHistoryTypeService.get("3");
        assertEquals(3, observationHistoryTypeService.getAll().size());
    }
}