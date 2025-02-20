package org.openelisglobal.gender;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
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
    public void verifyTestData() {
        List<Gender> genders = genderService.getAll();
        System.out.println("Genders we have in db: " + genders.size());
        genders.forEach(gender -> System.out
                .println(gender.getId() + " - " + gender.getGenderType() + " - " + gender.getDescription()));

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

}
