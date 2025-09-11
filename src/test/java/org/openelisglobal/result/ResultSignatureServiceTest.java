package org.openelisglobal.result;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.service.ResultSignatureService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultSignature;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;

public class ResultSignatureServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ResultSignatureService resultSignatureService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private SystemUserService systemUserService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/result.xml");
    }

    @Test
    public void getAll_shouldReturnAllResultSignatures() {
        List<ResultSignature> resultSignatures = resultSignatureService.getAll();
        assertEquals(2, resultSignatures.size());
        assertEquals("2", resultSignatures.get(0).getId());
        assertEquals("3", resultSignatures.get(1).getId());

    }

    @Test
    public void getData_shouldReturnSignatureData() {
        ResultSignature resultSignature = resultSignatureService.get("3");
        resultSignatureService.getData(resultSignature);
        assertEquals("External Doctor", resultSignature.getNonUserName());
    }

    @Test
    public void getResultSignatureByResult_shouldReturnResultSignature() {
        Result result = resultService.get("3");
        List<ResultSignature> resultSignatures = resultSignatureService.getResultSignaturesByResult(result);
        assertEquals(1, resultSignatures.size());
        assertEquals("2", resultSignatures.get(0).getId());
    }

    @Test
    public void getResultSignatureById_shouldReturnResultSignature() {
        ResultSignature resultSignature1 = new ResultSignature();
        resultSignature1.setId("3");
        ResultSignature resultSignature = resultSignatureService.getResultSignatureById(resultSignature1);
        assertEquals("External Doctor", resultSignature.getNonUserName());
    }

    @Test
    public void getResultSignaturesByResults_shouldReturnResultSignatures() {
        List<Result> results = resultService.getAll();
        List<ResultSignature> resultSignatures = resultSignatureService.getResultSignaturesByResults(results);
        assertEquals(2, resultSignatures.size());

    }

    @Test
    public void getAllMatching_shouldReturnAllMatchingSignature() {
        List<ResultSignature> resultSignatures = resultSignatureService.getAllMatching("nonUserName",
                "External Doctor");
        assertEquals(1, resultSignatures.size());
        assertEquals("3", resultSignatures.get(0).getId());
    }

    @Test
    public void getAllMatchingGivenMap_shouldReturnAllMatchingResultSignature() {
        Map<String, Object> map = Map.of("nonUserName", "External Doctor");
        List<ResultSignature> resultSignatures = resultSignatureService.getAllMatching(map);
        assertEquals(1, resultSignatures.size());
        assertEquals("3", resultSignatures.get(0).getId());

    }

    @Test
    public void getAllOrdered_shouldReturnOrderedResultSignatures() {
        List<ResultSignature> resultSignatures = resultSignatureService.getAllOrdered("id", false);
        assertEquals(2, resultSignatures.size());
        assertEquals("2", resultSignatures.get(0).getId());
        assertEquals("3", resultSignatures.get(1).getId());
    }

    @Test
    public void getAllOrderedGivenList_shouldReturnOrderedResultSignatures() {
        List<String> orderBy = List.of("id");
        List<ResultSignature> resultSignatures = resultSignatureService.getAllOrdered(orderBy, false);
        assertEquals(2, resultSignatures.size());
        assertEquals("2", resultSignatures.get(0).getId());
        assertEquals("3", resultSignatures.get(1).getId());
    }

    @Test
    public void getAllMatchingOrdered_shouldReturnOrderedResultSignatures() {
        List<ResultSignature> resultSignatures = resultSignatureService.getAllMatchingOrdered("nonUserName",
                "External Doctor", "id", false);
        assertEquals(1, resultSignatures.size());
        assertEquals("3", resultSignatures.get(0).getId());
    }

    @Test
    public void getAllMatchingOrderedGivenMap_shouldReturnOrderedResultSignatures() {
        Map<String, Object> map = Map.of("nonUserName", "External Doctor");
        List<ResultSignature> resultSignatures = resultSignatureService.getAllMatchingOrdered(map, "id", false);
        assertEquals(1, resultSignatures.size());
        assertEquals("3", resultSignatures.get(0).getId());
    }

    @Test
    public void getAllMatchingOrderedGivenList_shouldReturnOrderedResultSignatures() {
        List<String> orderBy = List.of("id");
        List<ResultSignature> resultSignatures = resultSignatureService.getAllMatchingOrdered("nonUserName",
                "External Doctor", orderBy, false);
        assertEquals(1, resultSignatures.size());
        assertEquals("3", resultSignatures.get(0).getId());
    }

    @Test
    public void getAllMatchingOrderedGivenListAndMap_shouldReturnOrderedResultSignatures() {
        List<String> orderBy = List.of("id");
        Map<String, Object> map = Map.of("nonUserName", "External Doctor");
        List<ResultSignature> resultSignatures = resultSignatureService.getAllMatchingOrdered(map, orderBy, false);
        assertEquals(1, resultSignatures.size());
        assertEquals("3", resultSignatures.get(0).getId());
    }

    public void getPage_shouldReturnPageOfResultSignatures() {
        List<ResultSignature> resultSignatures = resultSignatureService.getPage(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(resultSignatures.size() <= expectedPages);
    }

    @Test
    public void getMatchingPage_shouldReturnPageOfResultSignatures() {
        List<ResultSignature> resultSignatures = resultSignatureService.getMatchingPage("nonUserName",
                "External Doctor", 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(resultSignatures.size() <= expectedPages);
    }

    @Test
    public void getMatchingPageGivenMap_shouldReturnPageOfResultSignatures() {
        Map<String, Object> map = Map.of("nonUserName", "External Doctor");
        List<ResultSignature> resultSignatures = resultSignatureService.getMatchingPage(map, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(resultSignatures.size() <= expectedPages);
    }

    @Test
    public void getOrderedPage_shouldReturnOrderedPageOfResultSignatures() {
        List<ResultSignature> resultSignatures = resultSignatureService.getOrderedPage("id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(resultSignatures.size() <= expectedPages);
    }

    @Test
    public void getOrderedPageGivenList_shouldReturnOrderedPageOfResultSignatures() {
        List<String> orderBy = List.of("id");
        List<ResultSignature> resultSignatures = resultSignatureService.getOrderedPage(orderBy, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(resultSignatures.size() <= expectedPages);
    }

    @Test
    public void getMatchingOrderedPage_shouldReturnOrderedPageOfResultSignatures() {
        List<ResultSignature> resultSignatures = resultSignatureService.getMatchingOrderedPage("nonUserName",
                "External Doctor", "id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(resultSignatures.size() <= expectedPages);
    }

    @Test
    public void getMatchingOrderedPageGivenMap_shouldReturnOrderedPageOfResultSignatures() {
        Map<String, Object> map = Map.of("nonUserName", "External Doctor");
        List<ResultSignature> resultSignatures = resultSignatureService.getMatchingOrderedPage(map, "id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(resultSignatures.size() <= expectedPages);
    }

    @Test
    public void getMatchingOrderedPageGivenList_shouldReturnOrderedPageOfResultSignatures() {
        List<String> orderBy = List.of("id");
        List<ResultSignature> resultSignatures = resultSignatureService.getMatchingOrderedPage("nonUserName",
                "External Doctor", orderBy, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(resultSignatures.size() <= expectedPages);
    }

    @Test
    public void getMatchingOrderedPageGivenListAndMap_shouldReturnOrderedPageOfResultSignatures() {
        List<String> orderBy = List.of("id");
        Map<String, Object> map = Map.of("nonUserName", "External Doctor");
        List<ResultSignature> resultSignatures = resultSignatureService.getMatchingOrderedPage(map, orderBy, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(resultSignatures.size() <= expectedPages);
    }

    @Test
    public void getNext_shouldReturnNextResultSignature() {
        ResultSignature resultSignature = resultSignatureService.getNext("2");
        assertEquals("3", resultSignature.getId());
    }

    @Test
    public void getPrevious_shouldReturnPreviousResultSignature() {
        ResultSignature resultSignature = resultSignatureService.getPrevious("3");
        assertEquals("2", resultSignature.getId());
    }

    @Test
    public void save_shouldSaveResultSignature() {
        List<ResultSignature> resultSignatures = resultSignatureService.getAll();
        resultSignatureService.deleteAll(resultSignatures);
        String resultId = resultService.get("3").getId();
        String systemUserId = systemUserService.get("1").getId();

        ResultSignature resultSignature = new ResultSignature();

        resultSignature.setNonUserName("External Doctor");
        resultSignature.setResultId(resultId);
        resultSignature.setSystemUserId(systemUserId);
        ResultSignature savedResultSignature = resultSignatureService.save(resultSignature);
        List<ResultSignature> resultSignaturesAfterSave = resultSignatureService.getAll();
        assertEquals(1, resultSignaturesAfterSave.size());
        assertEquals("External Doctor", resultSignaturesAfterSave.get(0).getNonUserName());
        assertEquals(savedResultSignature.getId(), resultSignaturesAfterSave.get(0).getId());
    }

    @Test
    public void insert_shouldInsertResultSignature() {
        List<ResultSignature> resultSignatures = resultSignatureService.getAll();
        resultSignatureService.deleteAll(resultSignatures);
        String resultId = resultService.get("3").getId();
        String systemUserId = systemUserService.get("1").getId();

        ResultSignature resultSignature = new ResultSignature();

        resultSignature.setNonUserName("External Doctor");
        resultSignature.setResultId(resultId);
        resultSignature.setSystemUserId(systemUserId);
        String insertedId = resultSignatureService.insert(resultSignature);
        List<ResultSignature> resultSignaturesAfterSave = resultSignatureService.getAll();
        assertEquals(1, resultSignaturesAfterSave.size());
        assertEquals("External Doctor", resultSignaturesAfterSave.get(0).getNonUserName());
        assertEquals(insertedId, resultSignaturesAfterSave.get(0).getId());

    }

    @Test
    public void update_shouldUpDateResultSignature() {
        ResultSignature resultSignature = resultSignatureService.get("3");
        String oldNonUserName = resultSignature.getNonUserName();
        resultSignature.setNonUserName("Updated Doctor");
        ResultSignature updatedResultSignature = resultSignatureService.update(resultSignature);
        assertEquals("Updated Doctor", updatedResultSignature.getNonUserName());
        assertTrue(!updatedResultSignature.getNonUserName().equals(oldNonUserName));

    }

    @Test
    public void deleteAll_shouldDeleteAllResultSignatures() {
        List<ResultSignature> resultSignatures = resultSignatureService.getAll();
        assertEquals(2, resultSignatures.size());
        resultSignatureService.deleteAll(resultSignatures);
        List<ResultSignature> resultSignaturesAfterDelete = resultSignatureService.getAll();
        assertEquals(0, resultSignaturesAfterDelete.size());
    }

    @Test
    public void deleteAllGivenList_shouldDeleteAllResultSignatures() {
        List<ResultSignature> resultSignatures = resultSignatureService.getAll();
        assertEquals(2, resultSignatures.size());
        resultSignatureService.deleteAll(resultSignatures);
        List<ResultSignature> resultSignaturesAfterDelete = resultSignatureService.getAll();
        assertEquals(0, resultSignaturesAfterDelete.size());

    }

    @Test
    public void delete_shouldDeleteResultSignature() {
        ResultSignature resultSignature = resultSignatureService.get("3");
        resultSignatureService.delete(resultSignature);
        List<ResultSignature> resultSignaturesAfterDelete = resultSignatureService.getAll();
        assertEquals(1, resultSignaturesAfterDelete.size());
        assertEquals("2", resultSignaturesAfterDelete.get(0).getId());
    }

    @Test
    public void getCount_shouldReturnCountOfResultSignatures() {
        int count = resultSignatureService.getCount();
        assertEquals(2, count);
    }
}
