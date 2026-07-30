package org.openelisglobal.sample.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.sample.bean.SampleEditItem;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.form.SampleEditForm;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

public class SampleEditServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String DATASET_XML = "testdata/sample-edit-service.xml";
    private static final String SYS_USER_ID = "1";
    private static final String ACCESSION_NUMBER = "24-00001";
    private static final String EXISTING_ANALYSIS_ID = "1";
    private static final String EXISTING_SAMPLE_ITEM_ID = "1";
    private static final String TEST_ID = "1";

    @Autowired
    private SampleEditService sampleEditService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private SampleItemService sampleItemService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement(DATASET_XML);
    }

    private SampleEditForm createBaseForm() {
        SampleEditForm form = new SampleEditForm();
        form.setAccessionNumber(ACCESSION_NUMBER);

        SampleOrderItem sampleOrderItem = new SampleOrderItem();
        sampleOrderItem.setPriority(OrderPriority.ROUTINE);
        form.setSampleOrderItems(sampleOrderItem);

        form.setExistingTests(new ArrayList<>());
        form.setPossibleTests(new ArrayList<>());

        return form;
    }

    @Test
    public void editSample_withValidUpdates_shouldModifySampleProperties() {
        SampleEditForm form = createBaseForm();
        form.getSampleOrderItems().setPriority(OrderPriority.STAT);
        form.getSampleOrderItems().setConsentGiven(true);
        form.getSampleOrderItems().setConsentRecordedBy("TestRecorder");
        form.getSampleOrderItems().setConsentRecordedAt("15/02/2024");
        form.getSampleOrderItems().setConsentFormReference("REF-123");

        MockHttpServletRequest request = new MockHttpServletRequest();
        Sample sample = sampleService.getSampleByAccessionNumber(ACCESSION_NUMBER);

        sampleEditService.editSample(form, request, sample, true, SYS_USER_ID);

        Sample updatedSample = sampleService.getSampleByAccessionNumber(ACCESSION_NUMBER);

        assertEquals("Priority should be STAT", OrderPriority.STAT, updatedSample.getPriority());
        assertEquals("Consent should be true", true, updatedSample.getConsentGiven());
        assertEquals("Recorder should match", "TestRecorder", updatedSample.getConsentRecordedBy());
        assertEquals("Reference should match", "REF-123", updatedSample.getConsentFormReference());
        assertEquals("Recorded At should match exactly", "2024-02-15 00:00:00.0",
                updatedSample.getConsentRecordedAt().toString());
    }

    @Test
    public void editSample_withAddedTests_shouldCreateNewAnalyses() {
        SampleEditForm form = createBaseForm();

        SampleEditItem addItem = new SampleEditItem();
        addItem.setAdd(true);
        addItem.setTestId(TEST_ID);
        addItem.setSampleItemId(EXISTING_SAMPLE_ITEM_ID);
        form.getPossibleTests().add(addItem);

        MockHttpServletRequest request = new MockHttpServletRequest();
        Sample sample = sampleService.getSampleByAccessionNumber(ACCESSION_NUMBER);

        sampleEditService.editSample(form, request, sample, true, SYS_USER_ID);

        SampleItem sampleItem = sampleItemService.get(EXISTING_SAMPLE_ITEM_ID);
        List<Analysis> analyses = analysisService.getAnalysesBySampleItem(sampleItem);

        assertEquals("Should have exactly 2 analyses after adding one", 2, analyses.size());

        String notStartedStatus = SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted);

        boolean foundNewAnalysis = false;
        for (Analysis analysis : analyses) {
            if (!analysis.getId().equals(EXISTING_ANALYSIS_ID)) {
                foundNewAnalysis = true;
                Analysis freshAnalysis = analysisService.get(analysis.getId());

                assertEquals("Status should be NotStarted", notStartedStatus, freshAnalysis.getStatusId());
                assertEquals("Test ID should match", TEST_ID, freshAnalysis.getTest().getId());
                assertEquals("AnalysisType should be MANUAL", "MANUAL", freshAnalysis.getAnalysisType());
                assertEquals("Revision should be 0", "0", freshAnalysis.getRevision());
                assertEquals("Sample item ID should match", EXISTING_SAMPLE_ITEM_ID,
                        freshAnalysis.getSampleItem().getId());
                assertEquals("IsReportable should match the test definition", "Y", freshAnalysis.getIsReportable());
            }
        }

        assertTrue("Newly added analysis should be found in DB", foundNewAnalysis);
    }

    @Test
    public void editSample_withCanceledTests_shouldUpdateAnalysisStatus() {
        SampleEditForm form = createBaseForm();

        SampleEditItem cancelItem = new SampleEditItem();
        cancelItem.setCanceled(true);
        cancelItem.setAnalysisId(EXISTING_ANALYSIS_ID);
        cancelItem.setSampleItemId(EXISTING_SAMPLE_ITEM_ID);
        form.getExistingTests().add(cancelItem);

        MockHttpServletRequest request = new MockHttpServletRequest();
        Sample sample = sampleService.getSampleByAccessionNumber(ACCESSION_NUMBER);

        sampleEditService.editSample(form, request, sample, true, SYS_USER_ID);

        Analysis freshCanceled = analysisService.get(EXISTING_ANALYSIS_ID);

        String canceledStatus = SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled);
        assertEquals("Analysis status should be exactly Canceled", canceledStatus, freshCanceled.getStatusId());
        assertEquals("Analysis should remain linked to sample item", EXISTING_SAMPLE_ITEM_ID,
                freshCanceled.getSampleItem().getId());
        assertEquals("Test ID should remain the same", TEST_ID, freshCanceled.getTest().getId());
    }

    @Test
    public void editSample_withModifiedSampleItem_shouldUpdateCollectionDate() {
        SampleEditForm form = createBaseForm();

        SampleEditItem editItem = new SampleEditItem();
        editItem.setSampleItemChanged(true);
        editItem.setSampleItemId(EXISTING_SAMPLE_ITEM_ID);
        editItem.setCollectionDate("15/02/2024");
        editItem.setCollectionTime("10:30");
        form.getExistingTests().add(editItem);

        MockHttpServletRequest request = new MockHttpServletRequest();
        Sample sample = sampleService.getSampleByAccessionNumber(ACCESSION_NUMBER);

        sampleEditService.editSample(form, request, sample, true, SYS_USER_ID);

        SampleItem freshItem = sampleItemService.get(EXISTING_SAMPLE_ITEM_ID);
        assertEquals("Collection date should exactly match 2024-02-15 10:30", "2024-02-15 10:30:00.0",
                freshItem.getCollectionDate().toString());
    }

    @Test
    public void editSample_withRemovedSampleItem_shouldCancelSampleItemAndAnalysis() {
        SampleEditForm form = createBaseForm();

        SampleEditItem removeItem = new SampleEditItem();
        removeItem.setRemoveSample(true);
        removeItem.setSampleItemId(EXISTING_SAMPLE_ITEM_ID);
        removeItem.setAnalysisId(EXISTING_ANALYSIS_ID);
        form.getExistingTests().add(removeItem);

        MockHttpServletRequest request = new MockHttpServletRequest();
        Sample sample = sampleService.getSampleByAccessionNumber(ACCESSION_NUMBER);

        sampleEditService.editSample(form, request, sample, true, SYS_USER_ID);

        SampleItem freshItem = sampleItemService.get(EXISTING_SAMPLE_ITEM_ID);
        Analysis freshAnalysis = analysisService.get(EXISTING_ANALYSIS_ID);

        String canceledSampleStatus = SpringContext.getBean(IStatusService.class)
                .getStatusID(org.openelisglobal.common.services.StatusService.SampleStatus.Canceled);
        String canceledAnalysisStatus = SpringContext.getBean(IStatusService.class)
                .getStatusID(AnalysisStatus.Canceled);

        assertEquals("Sample item status should be Canceled", canceledSampleStatus, freshItem.getStatusId());
        assertEquals("Associated analysis status should be Canceled", canceledAnalysisStatus,
                freshAnalysis.getStatusId());
    }

    @Test
    public void getUpdatedAnalysisList_shouldReturnModifiedAnalyses() {
        SampleEditForm form = createBaseForm();

        SampleEditItem addItem = new SampleEditItem();
        addItem.setAdd(true);
        addItem.setTestId(TEST_ID);
        addItem.setSampleItemId(EXISTING_SAMPLE_ITEM_ID);
        form.getPossibleTests().add(addItem);

        SampleEditItem cancelItem = new SampleEditItem();
        cancelItem.setCanceled(true);
        cancelItem.setAnalysisId(EXISTING_ANALYSIS_ID);
        cancelItem.setSampleItemId(EXISTING_SAMPLE_ITEM_ID);
        form.getExistingTests().add(cancelItem);

        MockHttpServletRequest request = new MockHttpServletRequest();
        Sample sample = sampleService.getSampleByAccessionNumber(ACCESSION_NUMBER);

        sampleEditService.editSample(form, request, sample, true, SYS_USER_ID);

        List<String> updatedAnalyses = sampleEditService.getUpdatedAnalysisList();

        assertEquals("List should contain exactly the 2 modified analysis IDs", 2, updatedAnalyses.size());
        assertTrue("List should contain the explicitly canceled analysis ID",
                updatedAnalyses.contains(EXISTING_ANALYSIS_ID));
    }
}
