package org.openelisglobal.typeofsample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.springframework.beans.factory.annotation.Autowired;

public class TypeOfSampleTestServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;

    private List<TypeOfSampleTest> typeOfSampleTests;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/sample-type-test.xml");
    }

    @Test
    public void getData_ShouldReturnDataForASpecificSampleTypeTest() {
        TypeOfSampleTest typeOfSampleTest = new TypeOfSampleTest();
        typeOfSampleTest.setId("3");
        typeOfSampleTestService.getData(typeOfSampleTest);
        assertEquals("1002", typeOfSampleTest.getTypeOfSampleId());
        assertEquals("2002", typeOfSampleTest.getTestId());
    }

    @Test
    public void getTypeOfSampleTestsForTest_ShouldReturnAllSampleTypeTestsWithATestIdPassedAsParameter() {
        typeOfSampleTests = typeOfSampleTestService.getTypeOfSampleTestsForTest("2002");
        assertNotNull(typeOfSampleTests);
        assertEquals(2, typeOfSampleTests.size());
        assertEquals("3", typeOfSampleTests.get(1).getId());
    }

    @Test
    public void getPageOfTypeOfSampleTests_ShouldReturnAPageOfResults_UsingAPageNumber() {
        int NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        typeOfSampleTests = typeOfSampleTestService.getPageOfTypeOfSampleTests(1);
        assertTrue(NUMBER_OF_PAGES >= typeOfSampleTests.size());
    }

    @Test
    public void getAllTypeOfSampleTests_ShouldReturnAllSampleTypeTestsInTheDB() {
        typeOfSampleTests = typeOfSampleTestService.getAllTypeOfSampleTests();
        assertNotNull(typeOfSampleTests);
        assertEquals(3, typeOfSampleTests.size());
        assertEquals("3", typeOfSampleTests.get(2).getId());
    }

    @Test
    public void getTotalTypeOfSampleTestCount_ShouldReturnTheNumberOfSampleTypeTestsInTheDB() {
        Integer numberOfSampleTypeTests = typeOfSampleTestService.getTotalTypeOfSampleTestCount();
        assertNotNull(numberOfSampleTypeTests);
        assertEquals(Integer.valueOf("3"), numberOfSampleTypeTests);
    }

    @Test
    public void getTypeOfSampleTestsForSampleType_ShouldReturnAllSampleTypeTestsWithASampleIdPassedAsParameter() {
        typeOfSampleTests = typeOfSampleTestService.getTypeOfSampleTestsForSampleType("1002");
        assertNotNull(typeOfSampleTests);
        assertEquals(2, typeOfSampleTests.size());
        assertEquals("3", typeOfSampleTests.get(1).getId());
    }

    @Test
    public void delete_ShouldDeleteASampleTypeTestPassedAsParameter() {
        List<TypeOfSampleTest> typeOfSampleTests = typeOfSampleTestService.getAll();
        assertEquals(3, typeOfSampleTests.size());
        TypeOfSampleTest TypeOfSampleTest = typeOfSampleTestService.getAll().get(0);
        typeOfSampleTestService.delete(TypeOfSampleTest);
        List<TypeOfSampleTest> deletedTypeOfSampleTest = typeOfSampleTestService.getAll();
        assertEquals(2, deletedTypeOfSampleTest.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllSampleTypeTests() {
        List<TypeOfSampleTest> typeOfSampleTests = typeOfSampleTestService.getAll();
        assertEquals(3, typeOfSampleTests.size());
        typeOfSampleTestService.deleteAll(typeOfSampleTestService.getAll());
        List<TypeOfSampleTest> delectedTypeOfSampleTest = typeOfSampleTestService.getAll();
        assertTrue(delectedTypeOfSampleTest.isEmpty());
    }

}
