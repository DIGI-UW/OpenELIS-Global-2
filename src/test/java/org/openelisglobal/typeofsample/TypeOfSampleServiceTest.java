package org.openelisglobal.typeofsample;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.typeofsample.dao.TypeOfSampleDAO.SampleDomain;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;

public class TypeOfSampleServiceTest extends BaseWebContextSensitiveTest {
    @Autowired
    TypeOfSampleService tosSample;

    @Autowired
    TestService testSample;

    @Before
    public void init() throws Exception {
        tosSample.clearCache();
        executeDataSetWithStateManagement("testdata/typeofsample.xml");
    }

    @Test
    public void getData_shouldCopyPropertiesFromDatabase() {
        TypeOfSample typeOfSample = new TypeOfSample();
        typeOfSample.setId("1");
        tosSample.getData(typeOfSample);

        Assert.assertEquals("whole blood", typeOfSample.getDescription());
    }

    @Test
    public void getNameForTypeOfSampleId_shouldReturnNameForTypeOfSampleId() {
        Assert.assertEquals("Urine", tosSample.getNameForTypeOfSampleId("2"));
    }

    @Test
    public void getAllTypeOfSample_shouldReturnAllTypeOfSample() {
        List<TypeOfSample> typeOfSamples = tosSample.getAllTypeOfSamples();
        Assert.assertEquals(6, typeOfSamples.size());
    }

    @Test
    public void getAllTypeOfSamplesSortOrdered_shouldReturnTypeOfSamplesSortOrdered() {
        List<TypeOfSample> typeOfSamples = tosSample.getAllTypeOfSamplesSortOrdered();
        Assert.assertEquals(6, typeOfSamples.size());
        // Optionally, update the order assertions if needed

    }

    @Test
    public void getTypesForDomain_shouldReturnTypesForDomain() {
        SampleDomain domain = SampleDomain.HUMAN;
        List<TypeOfSample> typeOfSamples = tosSample.getTypesForDomainBySortOrder(domain);
        Assert.assertEquals(6, typeOfSamples.size());
    }

    @Test
    public void getTypesForDomainBySortOrder_shouldReturngTypesForDomainBySortOrder() {
        SampleDomain domain = SampleDomain.HUMAN;
        List<TypeOfSample> typeOfSamples = tosSample.getTypesForDomain(domain);
        Assert.assertEquals(6, typeOfSamples.size());
    }

    @Test
    public void getTypeOfSampleByLocalAbbrevAndDomain_shouldReturnTypeOfSampleByLocalAbbrevAndDomain() {
        TypeOfSample typeOfSample = tosSample.getTypeOfSampleByLocalAbbrevAndDomain("tissue", "H");
        Assert.assertEquals("Skin Tissue", typeOfSample.getDescription());
    }

    @Test
    public void getTypeOfSampleByDescriptionAndDomain_shouldReturnTypeOfSampleByDescriptionAndDomain() {
        TypeOfSample tos = tosSample.get("5");
        TypeOfSample typeOfSample = tosSample.getTypeOfSampleByDescriptionAndDomain(tos, true);
        Assert.assertEquals("Skin Tissue", typeOfSample.getDescription());
    }

    @Test
    public void getTypes_shouldReturnTypes() {
        List<TypeOfSample> typeOfSamples = tosSample.getTypes("whole", "H");
        Assert.assertEquals("whole blood", typeOfSamples.get(0).getDescription());
    }

    @Test
    public void getTotalTypeOfSampleCount_shouldReturnTotalTypeOfSampleCount() {
        Assert.assertEquals("6", tosSample.getTotalTypeOfSampleCount().toString());
    }

    @Test
    public void getTypeOfSampleById_shouldReturnTypeOfSampleById() {
        Assert.assertEquals("Urine", tosSample.getTypeOfSampleById("2").getDescription());
    }

    @Test
    public void getTypeOfSampleById_shouldReturnSampleTypeById() {
        Assert.assertEquals("Urine", tosSample.getTypeOfSampleById("2").getDescription());
    }

    @Test
    public void getPageOfTypeOfSamples_shouldReturnPageOfTypeOfSamples() {

        List<TypeOfSample> typeOfSamples = tosSample.getPageOfTypeOfSamples(1);

        int expectedPageSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));

        Assert.assertTrue(typeOfSamples.size() <= expectedPageSize);
        if (expectedPageSize >= 5) {
            Assert.assertTrue(typeOfSamples.stream().anyMatch(t -> t.getDescription().equals("whole blood")));
            Assert.assertTrue(typeOfSamples.stream().anyMatch(t -> t.getDescription().equals("Urine")));
        }
    }

    @Test
    public void getTransientTypeOfSampleById_shouldReturnSampleType() {
        Assert.assertEquals("Urine", tosSample.getTransientTypeOfSampleById("2").getDescription());
    }

    @Test
    public void getSampleTypeFromTest_shouldReturnSampleTypeFromTest() {
        org.openelisglobal.test.valueholder.Test test = testSample.get("2");
        Assert.assertEquals("Urine", tosSample.getSampleTypeFromTest(test).getDescription());
    }

    @Test
    public void getTypeOfSampleForTest_shouldReturnTypeOfSampleForTest() {
        List<TypeOfSample> typeOfSamples = tosSample.getTypeOfSampleForTest("5");
        Assert.assertEquals(1, typeOfSamples.size());
        Assert.assertEquals("5", typeOfSamples.get(0).getId());

    }

    @Test
    public void getTypeOfSampleNameForId_shouldReturnTypeOfSampleNameForId() {
        Assert.assertEquals("Fluids", tosSample.getTypeOfSampleNameForId("4"));
    }

    @Test
    public void getTypeOfSampleIdForLocalAbbreviation_shouldReturnTypeOfSampleIdForLocalAbbreviation() {
        Assert.assertEquals("5", tosSample.getTypeOfSampleIdForLocalAbbreviation("tissue"));
    }

    @Test
    public void getLocalizationForSampleType_shouldReturnLocalizationForSampleType() {
        Assert.assertEquals("Tissu cutané", tosSample.getLocalizationForSampleType("5").getFrench());
    }

    @Test
    public void getAllTestsBySampleTypeId_shouldReturnAllTestsBySampleTypeId() {
        List<org.openelisglobal.test.valueholder.Test> tests = tosSample.getAllTestsBySampleTypeId("4");
        Assert.assertEquals(1, tests.size());
        Assert.assertEquals("Fluids Test", tests.get(0).getDescription());

    }

    @Test
    public void getActiveTestsBySampleTypeId_shouldReturnActiveTestsBySampleTypeId() {
        Assert.assertEquals("Skin Tissue Test",
                tosSample.getActiveTestsBySampleTypeId("5", true).get(0).getDescription());
    }

    @Test
    public void getActiveTestsBySampleTypeIdAndTestUnit_shouldReturnActiveTestsBySampleTypeIdAndTestUnit() {
        List<String> testUnitIds = Arrays.asList("1");
        Assert.assertEquals("Blood Test",
                tosSample.getActiveTestsBySampleTypeIdAndTestUnit("1", true, testUnitIds).get(0).getDescription());
    }
}