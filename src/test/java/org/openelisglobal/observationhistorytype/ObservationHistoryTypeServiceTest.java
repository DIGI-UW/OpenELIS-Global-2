package org.openelisglobal.observationhistorytype;

import static org.junit.Assert.*;

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

    @Test
    public void testDataInDataBase() {
        List<ObservationHistoryType> observationHistoryTypes = observationHistoryTypeService.getAll();

        assertNotNull("Observation history type list should not be null", observationHistoryTypes);
        assertFalse("Observation history type list should not be empty", observationHistoryTypes.isEmpty());

        for (ObservationHistoryType observationType : observationHistoryTypes) {
            assertNotNull("ObservationHistoryType ID should not be null", observationType.getId());
            assertNotNull("ObservationHistoryType typeName should not be null", observationType.getTypeName());
        }
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

    @Test
    public void insert_shouldInsertObservationHistoryType() {
        ObservationHistoryType observationType = new ObservationHistoryType();
        observationType.setTypeName("EXERCISE_FREQUENCY");
        observationType.setDescription("Patient's exercise frequency");
        observationHistoryTypeService.insert(observationType);

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
        observationHistoryTypeService.save(observationType);

        ObservationHistoryType savedType = observationHistoryTypeService.getByName("ALCOHOL_CONSUMPTION");
        assertNotNull(savedType);
        assertEquals("ALCOHOL_CONSUMPTION", savedType.getTypeName());
        assertEquals("Patient's alcohol consumption habits", savedType.getDescription());
    }

    @Test
    public void update_shouldUpdateObservationHistoryType() {
        ObservationHistoryType observationType = observationHistoryTypeService.get("2");

        observationType.setTypeName("RELATIONSHIP_STATUS");
        observationType.setDescription("Updated marital status description");

        observationHistoryTypeService.update(observationType);

        ObservationHistoryType updatedType = observationHistoryTypeService.get("2");
        assertEquals("RELATIONSHIP_STATUS", updatedType.getTypeName());
        assertEquals("Updated marital status description", updatedType.getDescription());
    }

    @Test
    public void delete_shouldInactivateObservationHistoryType() {
        ObservationHistoryType observationType = observationHistoryTypeService.get("3");

        observationHistoryTypeService.delete(observationType);
        assertEquals(3, observationHistoryTypeService.getAll().size());
    }
}