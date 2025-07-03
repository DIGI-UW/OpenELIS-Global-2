package org.openelisglobal.samplepdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.samplepdf.service.SamplePdfService;
import org.openelisglobal.samplepdf.valueholder.SamplePdf;
import org.springframework.beans.factory.annotation.Autowired;

public class SamplePdfServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SamplePdfService samplePdfService;

    private List<SamplePdf> samplePdfList;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int NUMBER_OF_PAGES = 1;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/sample-pdf.xml");
        propertyValues = new HashMap<>();
        propertyValues.put("barcode", "ABC123XYZ");
        orderProperties = new ArrayList<>();
        orderProperties.add("accessionNumber");
    }

    @Test
    public void isAccessionNumberFound_ShouldReturnTrueIfTheAccessionNumberIsFound() {
        boolean accessionNumberFound = samplePdfService.isAccessionNumberFound(100001);
        assertTrue(accessionNumberFound);
    }

    @Test
    public void getSamplePdfByAccessionNumber_ShouldReturnASamplePdfUsingAssessionNumber() {
        SamplePdf samplePdf = samplePdfService.get("2");
        SamplePdf returnedSamplePdf = samplePdfService.getSamplePdfByAccessionNumber(samplePdf);
        assertNotNull(returnedSamplePdf);
        assertEquals("DEF456UVW", returnedSamplePdf.getBarcode());
        assertEquals("N", returnedSamplePdf.getAllowView());
    }

    @Test
    public void getAll_ShouldReturnAllSamplePdfs() {
        samplePdfList = samplePdfService.getAll();
        assertNotNull(samplePdfList);
        assertEquals(4, samplePdfList.size());
        assertEquals("GHI789RST", samplePdfList.get(2).getBarcode());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingSamplePdfs_UsingPropertyNameAndValue() {
        samplePdfList = samplePdfService.getAllMatching("barcode", "GHI789RST");
        assertNotNull(samplePdfList);
        assertEquals(1, samplePdfList.size());
        assertEquals("100003", samplePdfList.get(0).getAccessionNumber());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingSamplePdfs_UsingAMap() {
        samplePdfList = samplePdfService.getAllMatching(propertyValues);
        assertNotNull(samplePdfList);
        assertEquals(2, samplePdfList.size());
        assertEquals("100002", samplePdfList.get(1).getAccessionNumber());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedSamplePdfs_usingAnOrderProperty() {
        samplePdfList = samplePdfService.getAllOrdered("accessionNumber", true);
        assertNotNull(samplePdfList);
        assertEquals(4, samplePdfList.size());
        assertEquals("100002", samplePdfList.get(2).getAccessionNumber());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedSamplePdfs_UsingAList() {
        samplePdfList = samplePdfService.getAllOrdered(orderProperties, false);
        assertNotNull(samplePdfList);
        assertEquals(4, samplePdfList.size());
        assertEquals("100003", samplePdfList.get(3).getAccessionNumber());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSamplePdfs_UsingPropertyNameAndValueAndAnOrderProperty() {
        samplePdfList = samplePdfService.getAllMatchingOrdered("allowView", "Y", "accessionNumber", true);
        assertNotNull(samplePdfList);
        assertEquals(3, samplePdfList.size());
        assertEquals("100001", samplePdfList.get(2).getAccessionNumber());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSamplePdfs_UsingPropertyNameAndValueAndAList() {
        samplePdfList = samplePdfService.getAllMatchingOrdered("allowView", "Y", orderProperties, true);
        assertNotNull(samplePdfList);
        assertEquals(3, samplePdfList.size());
        assertEquals("100001", samplePdfList.get(2).getAccessionNumber());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllMatchingOrderedSamplePdfs_UsingAMapAndAnOrderProperty() {
        samplePdfList = samplePdfService.getAllMatchingOrdered(propertyValues, "allowView", false);
        assertNotNull(samplePdfList);
        assertEquals(2, samplePdfList.size());
        assertEquals("100002", samplePdfList.get(1).getAccessionNumber());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedSamplePdfs_UsingAMapAndAList() {
        samplePdfList = samplePdfService.getAllMatchingOrdered(propertyValues, orderProperties, false);
        assertNotNull(samplePdfList);
        assertEquals(2, samplePdfList.size());
        assertEquals("100001", samplePdfList.get(0).getAccessionNumber());
    }

    @Test
    public void getPage_ShouldReturnAPageOfResults() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        samplePdfList = samplePdfService.getPage(1);
        assertTrue(NUMBER_OF_PAGES >= samplePdfList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfResults_UsingAMap() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        samplePdfList = samplePdfService.getMatchingPage(propertyValues, 1);
        assertTrue(NUMBER_OF_PAGES >= samplePdfList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAPageOfResults_UsingAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        samplePdfList = samplePdfService.getOrderedPage("accessionNumber", true, 1);
        assertTrue(NUMBER_OF_PAGES >= samplePdfList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAPageOfResults_UsingAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        samplePdfList = samplePdfService.getOrderedPage(orderProperties, false, 1);
        assertTrue(NUMBER_OF_PAGES >= samplePdfList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAPageOfResults_UsingAPropertyNameAndValueAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        samplePdfList = samplePdfService.getMatchingOrderedPage("allowView", "Y", "accessionNumber", false, 1);
        assertTrue(NUMBER_OF_PAGES >= samplePdfList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAPageOfResults_UsingAPropertyNameAndValueAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        samplePdfList = samplePdfService.getMatchingOrderedPage("allowView", "Y", orderProperties, true, 1);
        assertTrue(NUMBER_OF_PAGES >= samplePdfList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAPageOfResults_UsingAMapAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        samplePdfList = samplePdfService.getMatchingOrderedPage(propertyValues, "accessionNumber", true, 1);
        assertTrue(NUMBER_OF_PAGES >= samplePdfList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAPageOfResults_UsingAMapAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        samplePdfList = samplePdfService.getMatchingOrderedPage(propertyValues, orderProperties, false, 1);
        assertTrue(NUMBER_OF_PAGES >= samplePdfList.size());
    }

    @Test
    public void update_ShouldModifyASamplePdf() {
        SamplePdf samplePdf = samplePdfService.get("2");
        samplePdf.setAccessionNumber("200003");
        samplePdf.setAllowView("Y");
        samplePdf.setBarcode("DEF456UVZX");
        SamplePdf updatedSamplePdf = samplePdfService.update(samplePdf);
        assertEquals("200003", updatedSamplePdf.getAccessionNumber());
    }

    @Test
    public void delete_ShouldDeleteASamplePdfPassedAsParameter() {
        SamplePdf samplePdf = samplePdfService.getAll().get(3);
        samplePdfService.delete(samplePdf);
        List<SamplePdf> updatedPdfList = samplePdfService.getAll();
        assertEquals(3, updatedPdfList.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllSamplePdfs() {
        samplePdfList = samplePdfService.getAll();
        samplePdfService.deleteAll(samplePdfList);
        List<SamplePdf> updatedPdfList = samplePdfService.getAll();
        assertTrue(updatedPdfList.isEmpty());
    }
}
