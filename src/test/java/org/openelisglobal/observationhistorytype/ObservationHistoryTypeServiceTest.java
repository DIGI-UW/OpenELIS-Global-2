package org.openelisglobal.observationhistorytype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.springframework.beans.factory.annotation.Autowired;

public class ObservationHistoryTypeServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ObservationHistoryTypeService observationHistoryTypeService;

    private final List<ObservationHistoryType> createdTypes = new ArrayList<>();
    private String typeNamePrefix;
    private ObservationHistoryType educationType;
    private ObservationHistoryType maritalType;
    private ObservationHistoryType occupationType;

    @Before
    public void setUp() {
        typeNamePrefix = "T" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() + "_";
        educationType = createType("EDUCATION_LEVEL", "Patient's education level");
        maritalType = createType("MARITAL_STATUS", "Patient's marital status");
        occupationType = createType("OCCUPATION", "Patient's occupation");
    }

    @After
    public void tearDown() {
        for (int index = createdTypes.size() - 1; index >= 0; index--) {
            ObservationHistoryType type = createdTypes.get(index);
            if (observationHistoryTypeService.get(type.getId()) != null) {
                observationHistoryTypeService.delete(type);
            }
        }
        createdTypes.clear();
    }

    @Test
    public void testDataInDataBase() {
        List<ObservationHistoryType> observationHistoryTypes = observationHistoryTypeService.getAll();

        assertNotNull("Observation history type list should not be null", observationHistoryTypes);
        assertFalse("Observation history type list should not be empty", observationHistoryTypes.isEmpty());
        assertTrue(observationHistoryTypes.stream().anyMatch(type -> educationType.getId().equals(type.getId())));
    }

    @Test
    public void getAll_shouldContainServiceCreatedObservationHistoryTypes() {
        List<ObservationHistoryType> observationHistoryTypes = observationHistoryTypeService.getAll();

        assertTrue(observationHistoryTypes.stream().anyMatch(type -> educationType.getId().equals(type.getId())));
        assertTrue(observationHistoryTypes.stream().anyMatch(type -> maritalType.getId().equals(type.getId())));
        assertTrue(observationHistoryTypes.stream().anyMatch(type -> occupationType.getId().equals(type.getId())));
    }

    @Test
    public void get_shouldReturnObservationHistoryTypeByGeneratedId() {
        ObservationHistoryType observationType = observationHistoryTypeService.get(educationType.getId());

        assertEquals(typeName("EDUCATION_LEVEL"), observationType.getTypeName());
        assertEquals("Patient's education level", observationType.getDescription());
    }

    @Test
    public void getByName_shouldReturnObservationHistoryTypeByStableName() {
        ObservationHistoryType observationType = observationHistoryTypeService.getByName(typeName("EDUCATION_LEVEL"));

        assertEquals(educationType.getId(), observationType.getId());
        assertEquals("Patient's education level", observationType.getDescription());
    }

    @Test
    public void insert_shouldInsertObservationHistoryType() {
        ObservationHistoryType insertedType = createType("EXERCISE_FREQUENCY", "Patient's exercise frequency");

        ObservationHistoryType found = observationHistoryTypeService.getByName(insertedType.getTypeName());
        assertNotNull(found);
        assertEquals("Patient's exercise frequency", found.getDescription());
    }

    @Test
    public void save_shouldSaveObservationHistoryType() {
        ObservationHistoryType observationType = new ObservationHistoryType();
        observationType.setTypeName(typeName("ALCOHOL_CONSUMPTION"));
        observationType.setDescription("Patient's alcohol consumption habits");
        ObservationHistoryType savedType = observationHistoryTypeService.save(observationType);
        createdTypes.add(savedType);

        ObservationHistoryType found = observationHistoryTypeService.getByName(savedType.getTypeName());
        assertNotNull(found);
        assertEquals("Patient's alcohol consumption habits", found.getDescription());
    }

    @Test
    public void update_shouldUpdateObservationHistoryType() {
        maritalType.setTypeName(typeName("RELATIONSHIP_STATUS"));
        maritalType.setDescription("Updated marital status description");

        observationHistoryTypeService.update(maritalType);

        ObservationHistoryType updatedType = observationHistoryTypeService.get(maritalType.getId());
        assertEquals(typeName("RELATIONSHIP_STATUS"), updatedType.getTypeName());
        assertEquals("Updated marital status description", updatedType.getDescription());
    }

    @Test
    public void delete_shouldDeleteOnlyTheServiceCreatedObservationHistoryType() {
        observationHistoryTypeService.delete(occupationType);
        createdTypes.remove(occupationType);

        assertNull(observationHistoryTypeService.getByName(typeName("OCCUPATION")));
        assertNotNull(observationHistoryTypeService.getByName("SampleRecordStatus"));
    }

    private ObservationHistoryType createType(String suffix, String description) {
        ObservationHistoryType type = new ObservationHistoryType();
        type.setTypeName(typeName(suffix));
        type.setDescription(description);
        type.setId(observationHistoryTypeService.insert(type));
        createdTypes.add(type);
        return type;
    }

    private String typeName(String suffix) {
        return typeNamePrefix + suffix;
    }
}
