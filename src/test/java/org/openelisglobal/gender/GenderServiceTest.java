package org.openelisglobal.gender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.List;
import org.hibernate.ObjectNotFoundException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.gender.service.GenderService;
import org.openelisglobal.gender.valueholder.Gender;
import org.springframework.beans.factory.annotation.Autowired;

public class GenderServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    GenderService genderService;

    @PersistenceContext
    private EntityManager entityManager;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/gender.xml");
    }

    @Test
    public void createGender_shouldCreateNewGender() throws Exception {
        Gender gender = createGender("X", "Unknown");
        Integer savedGenderId = genderService.insert(gender);

        Gender savedGender = genderService.get(savedGenderId);

        Assert.assertNotNull(savedGender);
        assertEquals("X", savedGender.getGenderType());
        assertEquals("Unknown", savedGender.getDescription());
        Assert.assertNotNull(savedGender.getId());
    }

    @Test
    public void getAllGenders_shouldReturnAllGenders() throws Exception {
        List<Gender> genders = genderService.getAll();
        assertEquals(4, genders.size());
    }

    @Test
    public void getGenderByType_shouldReturnGenderByType() throws Exception {
        Gender genderByType = genderService.getGenderByType("M");

        Assert.assertNotNull(genderByType);
        assertEquals("M", genderByType.getGenderType());
        assertEquals("Male", genderByType.getDescription());
        assertEquals("gender.male", genderByType.getNameKey());
    }

    private Gender createGender(String genderType, String description) {
        Gender gender = new Gender();
        gender.setGenderType(genderType);
        gender.setDescription(description);
        gender.setLastupdated(new Timestamp(System.currentTimeMillis()));
        return gender;
    }

    @Test
    public void GenderWithNullDescription_should_return_GenderWithNullDescription() {

        Gender retrievedGender = genderService.get(4);
        Assert.assertTrue(retrievedGender.getDescription() == null || retrievedGender.getDescription().isEmpty());

        assertEquals("W", retrievedGender.getGenderType());
        assertEquals("gender.untold", retrievedGender.getNameKey());
    }

    @Test(expected = LIMSDuplicateRecordException.class)
    public void InsertDuplicateGender_shouldThrowDuplicateGenderException() {

        Gender gender2 = new Gender();
        gender2.setGenderType("M");
        gender2.setDescription("Male");
        gender2.setNameKey("gender.male");

        genderService.insert(gender2);
    }

    @Test
    public void getNonexistentGender_shouldReturnNull() throws Exception {
        try {
            Gender gender = genderService.get(999);
            assertNull(gender);
        } catch (ObjectNotFoundException e) {

            assertTrue(true);
        }
    }

    @Test
    public void getGenderByNonexistentType_shouldReturnNull() throws Exception {
        Gender gender = genderService.getGenderByType("NONEXISTENT");
        assertNull(gender);
    }

    @Test(expected = LIMSDuplicateRecordException.class)
    public void updateGenderToDuplicateType_shouldThrowException() {
        Gender maleGender = genderService.get(1);
        Gender femaleGender = genderService.get(2);

        femaleGender.setGenderType("M");
        genderService.update(femaleGender);
    }

    @Test
    public void saveNewGender_shouldPersistCorrectly() {
        int initialCount = genderService.getAll().size();
        Gender newGender = createGender("N", "Non-binary");

        Gender savedGender = genderService.save(newGender);

        assertEquals(initialCount + 1, genderService.getAll().size());
        assertEquals("N", savedGender.getGenderType());
        assertEquals("Non-binary", savedGender.getDescription());
    }

    @Test(expected = LIMSDuplicateRecordException.class)
    public void saveDuplicateGender_shouldThrowException() {
        Gender duplicateGender = createGender("M", "Duplicate Male");
        genderService.save(duplicateGender);
    }

    @Test
    public void getGenderWithEmptyDescription_shouldReturnCorrectly() {
        Gender gender = genderService.get(4);
        assertTrue(gender.getDescription() == null || gender.getDescription().isEmpty());
    }

    @Test
    public void getAllGenders_shouldReturnCorrectOrder() {
        List<Gender> genders = genderService.getAll();
        assertEquals("M", genders.get(0).getGenderType());
        assertEquals("F", genders.get(1).getGenderType());
        assertEquals("O", genders.get(2).getGenderType());
        assertEquals("W", genders.get(3).getGenderType());
    }

    @Test
    public void createGenderWithMinimumFields_shouldSucceed() {
        Gender minimalGender = new Gender();
        minimalGender.setGenderType("X");
        minimalGender.setLastupdated(new Timestamp(System.currentTimeMillis()));

        Integer id = genderService.insert(minimalGender);
        Gender savedGender = genderService.get(id);

        assertEquals("X", savedGender.getGenderType());
        assertNull(savedGender.getDescription());
        assertNull(savedGender.getNameKey());
    }

    @Test(expected = LIMSRuntimeException.class)
    public void createGenderWithNullType_shouldThrowException() {
        Gender invalidGender = createGender(null, "Invalid");
        genderService.insert(invalidGender);
    }

    @Test
    public void createGender_withMaxLengthFields_shouldSucceed() {
        Gender gender = new Gender();
        gender.setGenderType("X");
        gender.setDescription("20-char description");
        gender.setNameKey("60-char name key ".repeat(3).trim());

        Integer id = genderService.insert(gender);
        Gender saved = genderService.get(id);

        assertEquals("20-char description", saved.getDescription());
        assertEquals("60-char name key 60-char name key 60-char name key", saved.getNameKey());
    }

    @Test
    public void createMultipleGenders_shouldGenerateSequentialIds() {
        Gender gender1 = createGender("A", "Gender A");
        Gender gender2 = createGender("B", "Gender B");

        Integer id1 = genderService.insert(gender1);
        Integer id2 = genderService.insert(gender2);

        assertEquals(1, id2 - id1);
    }

    @Test
    public void genderAsBaseObject_shouldImplementMethodsCorrectly() {
        Gender gender = new Gender();
        gender.setDescription("Test Description");

        assertEquals("Test Description", gender.getDefaultLocalizedName());
        assertTrue(gender.toString().contains("Test Description"));
    }

    @Test
    public void getNameKey_shouldReturnCorrectKey() {
        Gender male = genderService.getGenderByType("M");
        assertEquals("gender.male", male.getNameKey());

        Gender female = genderService.getGenderByType("F");
        assertEquals("gender.female", female.getNameKey());
    }

}
