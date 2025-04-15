package org.openelisglobal.barcode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.barcode.service.BarcodeLabelInfoService;
import org.openelisglobal.barcode.valueholder.BarcodeLabelInfo;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;

public class BarcodeLableInforServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private BarcodeLabelInfoService barcodeLabelInfoService;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/barcode-lable-info.xml");

    }

    @Test
    public void getDataByCode() {
        BarcodeLabelInfo info = barcodeLabelInfoService.getDataByCode("ABC123");
        assertNotNull(info);
        assertEquals("1", info.getId());
        assertEquals(100, info.getNumPrinted());
        assertEquals("Standard", info.getType());

        BarcodeLabelInfo info2 = barcodeLabelInfoService.getDataByCode("XYZ456");
        assertNotNull(info2);
        assertEquals("2", info2.getId());
        assertEquals(150, info2.getNumPrinted());
        assertEquals("Custom", info2.getType());
        BarcodeLabelInfo info3 = barcodeLabelInfoService.getDataByCode("LMN789");
        assertNotNull(info3);
        assertEquals("3", info3.getId());
        assertEquals(200, info3.getNumPrinted());
        assertEquals("Standard", info3.getType());

    }

    @Test
    public void get_shouldReturnBarcodeLabelInfoGivenId() {
        BarcodeLabelInfo barcode = barcodeLabelInfoService.get("1");
        assertEquals(100, barcode.getNumPrinted());
        assertEquals("Standard", barcode.getType());

    }

    @Test
    public void getNext_shouldReturnNextBarcodeLabelInfo() {
        BarcodeLabelInfo bLabelInfo = barcodeLabelInfoService.getNext("1");
        assertEquals("2", bLabelInfo.getId());
    }

    @Test
    public void getPrevious_shouldReturnPreviousItem() {
        BarcodeLabelInfo bLabelInfo = barcodeLabelInfoService.getPrevious("2");
        assertEquals("1", bLabelInfo.getId());
    }

    @Test
    public void save_sholdSaveBarcodeLabelInfo() {
        barcodeLabelInfoService.deleteAll(barcodeLabelInfoService.getAll());
        BarcodeLabelInfo barcodeLabelInfo = new BarcodeLabelInfo();
        barcodeLabelInfo.setNumPrinted(300);
        barcodeLabelInfo.setCode("75");
        barcodeLabelInfo.setType("Standard");
        BarcodeLabelInfo barcodeLabelInfo2 = barcodeLabelInfoService.save(barcodeLabelInfo);
        assertNotNull(barcodeLabelInfo2);
        assertEquals(300, barcodeLabelInfo2.getNumPrinted());
    }

    @Test
    public void getAll_sholdReturnAll() {
        List<BarcodeLabelInfo> barcodes = barcodeLabelInfoService.getAll();
        assertEquals(3, barcodes.size());

    }

    @Test
    public void update_shouldUpdateBarcodeLabelInfo() {
        BarcodeLabelInfo bLabelInfo = barcodeLabelInfoService.get("1");
        bLabelInfo.setCode("15");
        BarcodeLabelInfo barcodeLabelInfo2 = barcodeLabelInfoService.update(bLabelInfo);
        assertEquals("1", barcodeLabelInfo2.getId());
        assertEquals("15", barcodeLabelInfo2.getCode());
    }

    @Test
    public void updateAll_shouldUpdateBarcodeLabelInfoInTheList() {
        BarcodeLabelInfo bLabelInfo = barcodeLabelInfoService.get("1");
        bLabelInfo.setCode("15");
        BarcodeLabelInfo bLabelInfo2 = barcodeLabelInfoService.get("2");
        bLabelInfo.setCode("45");
        List<BarcodeLabelInfo> barcodeLabelInfos = new ArrayList<>();
        barcodeLabelInfos.add(bLabelInfo2);
        barcodeLabelInfos.add(bLabelInfo);

        List<BarcodeLabelInfo> barcodesUpdated = barcodeLabelInfoService.updateAll(barcodeLabelInfos);
        assertEquals(2, barcodesUpdated.size());
        assertEquals("2", barcodesUpdated.get(0).getId());
        assertEquals("1", barcodesUpdated.get(1).getId());

    }

    @Test
    public void getAllMatching() {
        List<BarcodeLabelInfo> barcodeLabelInfos = barcodeLabelInfoService.getAllMatching("type", "Standard");
        assertEquals(2, barcodeLabelInfos.size());
        assertEquals("1", barcodeLabelInfos.get(0).getId());
        assertEquals("3", barcodeLabelInfos.get(1).getId());

    }

    @Test
    public void insert_shouldInsertBarcodeLabelInfo() {
        barcodeLabelInfoService.deleteAll(barcodeLabelInfoService.getAll());
        BarcodeLabelInfo barcodeLabelInfo = new BarcodeLabelInfo();
        barcodeLabelInfo.setNumPrinted(300);
        barcodeLabelInfo.setCode("75");
        barcodeLabelInfo.setType("Standard");
        barcodeLabelInfoService.insert(barcodeLabelInfo);
        List<BarcodeLabelInfo> barcodeLabelInfos = barcodeLabelInfoService.getAll();
        assertEquals(1, barcodeLabelInfos.size());

    }

    @Test
    public void delete_shouldDeleteBarcodeLabelInfo() {
        BarcodeLabelInfo barcodeLabelInfo2 = barcodeLabelInfoService.get("1");
        barcodeLabelInfoService.delete(barcodeLabelInfo2);
        List<BarcodeLabelInfo> barcodeLabelInfos = barcodeLabelInfoService.getAll();
        assertEquals(2, barcodeLabelInfos.size());

    }

    @Test
    public void deleteAll_shouldDeleteAllBarcodeLabelInfo() {
        List<BarcodeLabelInfo> barcodeLabelInfos = barcodeLabelInfoService.getAll();
        assertEquals(3, barcodeLabelInfos.size());
        barcodeLabelInfoService.deleteAll(barcodeLabelInfos);
        List<BarcodeLabelInfo> barcodeLabelInfos2 = barcodeLabelInfoService.getAll();
        assertEquals(0, barcodeLabelInfos2.size());

    }

    @Test
    public void getAllOrdered_shouldReturnAllOrdered() {
        List<BarcodeLabelInfo> orded = barcodeLabelInfoService.getAllOrdered("id", true);
        assertEquals(3, orded.size());
        assertEquals("3", orded.get(0).getId());
        assertEquals("2", orded.get(1).getId());
        assertEquals("1", orded.get(2).getId());
    }

    @Test
    public void getAllOrdered_shouldReturnAllOrderedDesc() {
        List<BarcodeLabelInfo> orded = barcodeLabelInfoService.getAllOrdered("id", false);
        assertEquals(3, orded.size());
        assertEquals("1", orded.get(0).getId());
        assertEquals("2", orded.get(1).getId());
        assertEquals("3", orded.get(2).getId());

    }

    @Test
    public void getAllMatchingOrdered_shouldReturnAllMatchingOrdered() {
        List<BarcodeLabelInfo> orded = barcodeLabelInfoService.getAllMatchingOrdered("type", "Standard", "id", true);
        assertEquals(2, orded.size());
        assertEquals("3", orded.get(0).getId());
        assertEquals("1", orded.get(1).getId());

    }

    @Test
    public void hasPrevious_shouldReturnTrue() {
        boolean hasPrevious = barcodeLabelInfoService.hasPrevious("2");
        assertEquals(true, hasPrevious);
    }

    @Test
    public void hasNext_shouldReturnTrue() {
        boolean hasNext = barcodeLabelInfoService.hasNext("2");
        assertEquals(true, hasNext);
    }

    @Test
    public void getPaged_shouldReturnPagedBarcodeLabelInfo() {
        List<BarcodeLabelInfo> pages = barcodeLabelInfoService.getPage(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(pages.size() <= expectedPages);

    }

    @Test
    public void getMatchingOrderdPaged_shouldReturnPagedBarcodeLabelInfo() {
        List<BarcodeLabelInfo> pages = barcodeLabelInfoService.getMatchingOrderedPage("type", "Standard", "id", true,
                1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(pages.size() <= expectedPages);

    }

    @Test
    public void getAllOrderedPage_shouldReturnPagedBarcodeLabelInfo() {
        List<BarcodeLabelInfo> pages = barcodeLabelInfoService.getOrderedPage("id", true, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(pages.size() <= expectedPages);

    }

    @Test
    public void getCount_shouldReturnCount() {
        int count = barcodeLabelInfoService.getCount();
        assertEquals(3, count);
    }

    @Test
    public void getCountLike_shouldReturnCount() {
        int count = barcodeLabelInfoService.getCountLike("type", "Standard");
        assertEquals(2, count);
    }

    @Test
    public void getCountMatching_shouldReturnCount() {
        int count = barcodeLabelInfoService.getCountMatching("type", "Standard");
        assertEquals(2, count);
    }

    @Test
    public void getMatching_givenMaps() {
        Map<String, Object> map = Map.of("type", "Standard");
        List<BarcodeLabelInfo> barcodeLabelInfos = barcodeLabelInfoService.getAllMatching(map);
        assertEquals(2, barcodeLabelInfos.size());
        assertEquals("1", barcodeLabelInfos.get(0).getId());
        assertEquals("3", barcodeLabelInfos.get(1).getId());
    }

    @Test
    public void getAllMatchingOrdered_givenMaps() {
        Map<String, Object> map = Map.of("type", "Standard");
        List<BarcodeLabelInfo> barcodeLabelInfos = barcodeLabelInfoService.getAllMatchingOrdered(map, "id", true);
        assertEquals(2, barcodeLabelInfos.size());
        assertEquals("3", barcodeLabelInfos.get(0).getId());
        assertEquals("1", barcodeLabelInfos.get(1).getId());
    }

    @Test
    public void getAllMatchingOrderedPage_givenMaps() {
        Map<String, Object> map = Map.of("type", "Standard");
        List<BarcodeLabelInfo> barcodeLabelInfos = barcodeLabelInfoService.getMatchingOrderedPage(map, "id", true, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(barcodeLabelInfos.size() <= expectedPages);
    }

    @Test
    public void getAllOrdered_givenListOfProperties() {
        List<String> properties = new ArrayList<>();
        properties.add("id");
        properties.add("type");
        List<BarcodeLabelInfo> barcodeLabelInfos = barcodeLabelInfoService.getAllOrdered(properties, true);
        assertEquals(3, barcodeLabelInfos.size());
        assertEquals("3", barcodeLabelInfos.get(0).getId());
        assertEquals("2", barcodeLabelInfos.get(1).getId());
        assertEquals("1", barcodeLabelInfos.get(2).getId());
    }

}
