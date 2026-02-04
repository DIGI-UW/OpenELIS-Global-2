package org.openelisglobal.result.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
import org.openelisglobal.dataexchange.orderresult.OrderResponseWorker.Event;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.action.util.ResultsUpdateDataSet;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultInventory;
import org.openelisglobal.result.valueholder.ResultSignature;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.scriptlet.service.ScriptletService;
import org.openelisglobal.scriptlet.valueholder.Scriptlet;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.testreflex.action.util.TestReflexUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

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
    @Autowired
    ScriptletService scriptletService;
    @Autowired
    MethodService methodService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/logbook-db.xml");
        Scriptlet cd4Scriptlet = new Scriptlet();
        cd4Scriptlet.setScriptletName("Calculate CD4");
        cd4Scriptlet = scriptletService.getScriptletByName(cd4Scriptlet);

        if (cd4Scriptlet != null && cd4Scriptlet.getId() != null) {
            ReflectionTestUtils.setField(TestReflexUtil.class, "CD4_SCRIPTLET_ID", cd4Scriptlet.getId());
        }
    }

    private TestResultItem getTestResultItem() {
        Result result = resultService.getAll().get(0);
        Method method = methodService.getAll().get(0);

        TestResultItem item = new TestResultItem();
        item.setAccessionNumber("S-TEST-001");
        item.setSequenceNumber("1");
        item.setShowSampleDetails(true);
        item.setResultValue("6.8");
        item.setResultType("N");
        item.setValid(true);
        item.setShadowRejected(false);
        item.setRefer(false);
        item.setResult(result);
        item.setResultId(result.getId());
        item.setTestDate("2024-01-15");
        item.setTestMethod(method.getMethodName());
        item.setAnalysisId("1");
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
        assertTrue(savedResults.size() > 0);
        assertEquals("6.8", savedResults.get(0).getValue());
        assertEquals("N", savedResults.get(0).getResultType());

    }

    @Test
    public void modifyResult_shouldUpdateResultIntheDatabase() {
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
        saveBean.setResultId(testResultItem.getResultId());

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

        ResultsUpdateDataSet dataSet = new ResultsUpdateDataSet("");
        for (Result result : results) {
            ResultSet rs = new ResultSet(result, null, null, patient, sample, reflexMap, false);
            rs.result.setResultEvent(Event.RESULT);

            dataSet.getModifiedResults().add(rs);
        }

        List<IResultUpdate> resultUpdates = new ArrayList<>();
        logbookPersistService.persistDataSet(dataSet, resultUpdates, systemUser.getId());

        List<Result> savedResults = resultService.getAll();
        assertEquals("6.8", savedResults.get(0).getValue());
        assertEquals("N", savedResults.get(0).getResultType());
        assertFalse("Results should be persisted", savedResults.isEmpty());

    }
}
