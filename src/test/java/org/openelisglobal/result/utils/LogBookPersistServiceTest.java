package org.openelisglobal.result.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.ResultSaveService;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.common.services.serviceBeans.ResultSaveBean;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.action.util.ResultsUpdateDataSet;
import org.openelisglobal.result.service.LogbookResultsPersistService;
import org.openelisglobal.result.service.ResultInventoryService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.service.ResultSignatureService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultInventory;
import org.openelisglobal.result.valueholder.ResultSignature;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.springframework.beans.factory.annotation.Autowired;

public class LogBookPersistServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    AnalysisService analysisService;
    @Autowired
    ResultService resultService;
    @Autowired
    PatientService patientService;
    @Autowired
    ResultInventoryService resultInventoryService;
    @Autowired
    ResultSignatureService resultSigService;
    @Autowired
    LogbookResultsPersistService logbookPersistService;
    @Autowired
    SystemUserService systemUserService;
    @Autowired
    AnalyteService analyteService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/inventory.xml");
    }

    private TestResultItem getTestResultItem() {
        DemoData demoData = new DemoData();
        TestResultItem item = new TestResultItem();
        item.setAccessionNumber(demoData.getAnalyses().get(0).getSampleItem().getSample().getAccessionNumber());
        item.setSequenceNumber("1");
        item.setShowSampleDetails(true);
        item.setResultValue(demoData.getResults().get(0).getValue());
        item.setResultType(demoData.getResults().get(0).getResultType());
        item.setValid(true);
        item.setShadowRejected(false);
        item.setRefer(false);
        item.setTestDate(demoData.getAnalyses().get(0).getTest().getActiveBeginDateForDisplay());
        item.setTestMethod(demoData.getAnalyses().get(0).getTest().getMethod().getMethodName());
        item.setAnalysisId(demoData.getAnalyses().get(0).getId());
        item.setTechnician("John Doe");
        item.setTechnicianSignatureId(null);
        return item;
    }

    @Test
    public void saveNewResult_shouldSaveNewResultsFromResultSet() throws Exception {
        Map<String, List<String>> reflexMap = new HashMap<>();

        SystemUser systemUser = systemUserService.getAll().get(0);
        Analysis analysis = analysisService.getAll().get(0);

        TestResultItem testResultItem = getTestResultItem();

        ResultSaveBean saveBean = new ResultSaveBean();
        saveBean.setResultType(testResultItem.getResultType());
        saveBean.setResultValue(testResultItem.getResultValue());
        saveBean.setTestId(analysis.getTest().getId());
        saveBean.setReportable("Y");
        saveBean.setHasQualifiedResult(false);

        ResultSaveService resultSaveService = SpringContext.getBean(ResultSaveService.class);
        resultSaveService.setAnalysis(analysis);
        resultSaveService.setCurrentUserId(systemUser.getId());

        List<Result> deletableResults = new ArrayList<>();
        List<Result> results = resultSaveService.createResultsFromTestResultItem(saveBean, deletableResults);

        Patient patient = patientService.getAll().get(0);
        String sampleTestingStartedId = SpringContext.getBean(IStatusService.class)
                .getStatusID(org.openelisglobal.common.services.StatusService.OrderStatus.Started);
        Sample sample = analysis.getSampleItem().getSample();
        sample.setStatusId(sampleTestingStartedId);
        List<ResultInventory> invResults = resultInventoryService.getAll();
        resultInventoryService.deleteAll(invResults);
        List<ResultSignature> sigs = resultSigService.getAll();
        resultSigService.deleteAll(sigs);
        List<Result> existingResults = resultService.getAll();
        resultService.deleteAll(existingResults);

        ResultsUpdateDataSet dataSet = new ResultsUpdateDataSet("");
        for (Result result : results) {
            ResultSet rs = new ResultSet(result, null, null, patient, sample, reflexMap, false);
            dataSet.getNewResults().add(rs);
        }

        List<IResultUpdate> resultUpdates = new ArrayList<>();
        logbookPersistService.persistDataSet(dataSet, resultUpdates, systemUser.getId());

        List<Result> savedResults = resultService.getAll();
        assertEquals("6.8", savedResults.get(0).getValue());
        assertFalse("Results should be persisted", savedResults.isEmpty());
    }
}
