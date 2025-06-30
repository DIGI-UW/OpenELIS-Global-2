package org.openelisglobal.sampleitem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;

public class SampleItemServiceTest extends BaseWebContextSensitiveTest {
    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    SampleItemService sampleItemService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/typeofsample.xml");
    }

    @Test
    public void getData_shouldReturnCopiedPropertiesFromDatabase() {
        SampleItem item = new SampleItem();
        item.setId("1");
        sampleItemService.getData(item);

        Assert.assertEquals("1", item.getSortOrder());
    }

    @Test
    public void getData_shouldReturnCopiedPropertiesFromDatabaseById() {
        SampleItem item = sampleItemService.getData("2");

        Assert.assertEquals("12", item.getSortOrder());
        Assert.assertEquals("13333", item.getSample().getAccessionNumber());
    }

    @Test
    public void getSampleItemsBySampleIdAndType_shouldReturnSampleItemsBySampleIdAndType() {
        TypeOfSample tos = typeOfSampleService.get("2");
        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleIdAndType("2", tos);

        Assert.assertEquals(1, sampleItems.size());
        Assert.assertEquals("2", sampleItems.get(0).getTypeOfSampleId());
    }

    @Test
    public void getPageOfSampleItems_shouldReturnPageOfSampleItems() {
        List<SampleItem> sampleItems = sampleItemService.getPageOfSampleItems(1);

        int expectedPageSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));

        Assert.assertTrue(sampleItems.size() <= expectedPageSize);
        if (expectedPageSize >= 5) {
            Assert.assertTrue(sampleItems.stream().anyMatch(s -> s.getSample().getAccessionNumber().equals("12345")));
            Assert.assertTrue(sampleItems.stream().anyMatch(s -> s.getTypeOfSample().getDescription().equals("Urine")));
        }
    }

    @Test
    public void getAllSampleItem_shouldReturnAllSampleItems() {
        List<SampleItem> sampleItems = sampleItemService.getAllSampleItems();

        Assert.assertEquals(4, sampleItems.size());
    }

    @Test
    public void getSampleItemsBySampleId_shouldReturnSampleItemsBySampleId() {
        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId("3");

        Assert.assertEquals(1, sampleItems.size());
        Assert.assertEquals("plasma", sampleItems.get(0).getTypeOfSample().getDescription());
    }

    @Test
    public void getTypeOfSampleId_shouldReturnTypeOfSampleId() {
        SampleItem item = sampleItemService.get("3");

        Assert.assertEquals("3", sampleItemService.getTypeOfSampleId(item));
    }

    @Test
    public void getDataBySample_shouldReturnCopiedPropertiesFromDatabase() {
        SampleItem item = new SampleItem();
        item.setId("4");
        sampleItemService.getData(item);

        Assert.assertEquals("2", item.getSortOrder());
        Assert.assertEquals("Fluids", item.getTypeOfSample().getDescription());
    }

    @Test
    public void getSampleItemsBySampleIdAndStatus_shouldReturnSampleItemsBySampleIdAndStatus() {
        Set<Integer> includedStatusList = new HashSet<>();
        includedStatusList.add(3);

        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleIdAndStatus("3", includedStatusList);

        Assert.assertEquals(1, sampleItems.size());
        Assert.assertEquals("plasma", sampleItems.get(0).getTypeOfSample().getDescription());
    }
}