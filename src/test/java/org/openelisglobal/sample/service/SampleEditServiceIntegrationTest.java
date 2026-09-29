package org.openelisglobal.sample.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sample.bean.SampleEditItem;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.form.SampleEditForm;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

public class SampleEditServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String DATASET_XML = "testdata/sample-edit-service.xml";
    private static final String REMOVE_SAMPLE_DATASET_XML = "testdata/sample-edit-service-remove-sample.xml";
    private static final String SYS_USER_ID = "1";
    private static final String ACCESSION_NUMBER = "24-00001";
    private static final String EXISTING_ANALYSIS_ID = "1";
    private static final String EXISTING_SAMPLE_ITEM_ID = "1";
    private static final String TEST_ID = "1";
    private static final String SECOND_ANALYSIS_ON_SAME_ITEM_ID = "2";
    private static final String OTHER_SAMPLE_ITEM_ID = "2";
    private static final String ANALYSIS_ON_OTHER_ITEM_ID = "3";

    @Autowired
    private SampleEditService sampleEditService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement(DATASET_XML);
        jdbcTemplate.update("DELETE FROM clinlims.sample_requester WHERE sample_id = 1");
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

    /**
     * OGC-1366: Modify Order passes sampleChanged=false unless the accession number
     * changes, so a priority-only edit reached the database through the dirty flush
     * and never went through the audited update: STAT to ROUTINE left no trace in
     * the order's history.
     */
    @Test
    public void editSample_changingOnlyThePriority_isRecordedInTheAuditTrail() {
        sampleEditService.editSample(createBaseForm(), new MockHttpServletRequest(), null, false, SYS_USER_ID);
        SampleEditForm toStat = createBaseForm();
        toStat.getSampleOrderItems().setPriority(OrderPriority.STAT);
        Mockito.clearInvocations(auditTrailService);

        sampleEditService.editSample(toStat, new MockHttpServletRequest(), null, false, SYS_USER_ID);

        assertEquals(OrderPriority.STAT, sampleService.getSampleByAccessionNumber(ACCESSION_NUMBER).getPriority());
        ArgumentCaptor<BaseObject> changed = ArgumentCaptor.forClass(BaseObject.class);
        ArgumentCaptor<BaseObject> stored = ArgumentCaptor.forClass(BaseObject.class);
        verify(auditTrailService).saveHistory(changed.capture(), stored.capture(), eq(SYS_USER_ID),
                eq(IActionConstants.AUDIT_TRAIL_UPDATE), argThat("sample"::equalsIgnoreCase));
        assertEquals(OrderPriority.STAT, ((Sample) changed.getValue()).getPriority());
        assertEquals(OrderPriority.ROUTINE, ((Sample) stored.getValue()).getPriority());
    }

    @Test
    public void editSample_withAnUnchangedPriority_writesNoSampleAuditRow() {
        sampleEditService.editSample(createBaseForm(), new MockHttpServletRequest(), null, false, SYS_USER_ID);
        Mockito.clearInvocations(auditTrailService);

        sampleEditService.editSample(createBaseForm(), new MockHttpServletRequest(), null, false, SYS_USER_ID);

        verify(auditTrailService, never()).saveHistory(any(), any(), any(), eq(IActionConstants.AUDIT_TRAIL_UPDATE),
                argThat("sample"::equalsIgnoreCase));
    }

    /**
     * OGC-1366 walk: with requester names optional, a Modify Order save that left
     * the requester blank inserted an empty Person and Provider and linked them as
     * the order's requester. Order entry treats a blank requester as none.
     */
    @Test
    public void editSample_withABlankRequester_createsNoProviderAndNoRequesterLink() {
        int people = count("clinlims.person");
        int providers = count("clinlims.provider");

        sampleEditService.editSample(modifiedForm(), new MockHttpServletRequest(), null, false, SYS_USER_ID);

        assertEquals(people, count("clinlims.person"));
        assertEquals(providers, count("clinlims.provider"));
        assertEquals(0, personRequesterIds().size());
    }

    /**
     * Review of #4469: picking an existing requester on an order with none linked
     * created a copy of that person and provider, so the requester search then
     * listed them twice.
     */
    @Test
    public void editSample_pickingAnExistingRequester_linksItWithoutACopy() {
        SampleEditForm picked = modifiedForm();
        picked.getSampleOrderItems().setProviderPersonId("2");
        picked.getSampleOrderItems().setProviderFirstName("Test");
        picked.getSampleOrderItems().setProviderLastName("Clinician");
        int people = count("clinlims.person");
        int providers = count("clinlims.provider");

        sampleEditService.editSample(picked, new MockHttpServletRequest(), null, false, SYS_USER_ID);

        assertEquals(people, count("clinlims.person"));
        assertEquals(providers, count("clinlims.provider"));
        assertEquals(List.of("2"), personRequesterIds());
    }

    @Test
    public void editSample_clearingTheRequester_unlinksItFromTheOrder() {
        SampleEditForm withRequester = modifiedForm();
        withRequester.getSampleOrderItems().setProviderFirstName("Grace");
        withRequester.getSampleOrderItems().setProviderLastName("Nansubuga");
        sampleEditService.editSample(withRequester, new MockHttpServletRequest(), null, false, SYS_USER_ID);
        assertEquals(1, personRequesterIds().size());
        SampleEditForm cleared = modifiedForm();
        cleared.getSampleOrderItems().setProviderPersonId(personRequesterIds().get(0));
        int people = count("clinlims.person");

        sampleEditService.editSample(cleared, new MockHttpServletRequest(), null, false, SYS_USER_ID);

        assertEquals(people, count("clinlims.person"));

        assertEquals(0, personRequesterIds().size());
        assertNull(jdbcTemplate.queryForObject("SELECT provider_id::text FROM clinlims.sample_human WHERE samp_id = 1",
                String.class));
    }

    /**
     * OGC-1366 walk: Modify Order sends the loaded site's name with its id
     * (OGC-1191 keeps the name so the required field stays filled), and every save
     * cloned the site into a new organization. The id wins, as in order entry; a
     * name alone is a new site.
     */
    @Test
    public void editSample_withAnExistingSite_linksItAndCreatesNoOrganization() {
        SampleEditForm form = modifiedForm();
        form.getSampleOrderItems().setReferringSiteId("1");
        form.getSampleOrderItems().setReferringSiteName("Test Health Center");
        jdbcTemplate.update("INSERT INTO clinlims.organization_organization_type (org_id, org_type_id) VALUES (1, ?)"
                + " ON CONFLICT DO NOTHING", Long.valueOf(referringClinicTypeId()));
        int organizations = count("clinlims.organization");

        sampleEditService.editSample(form, new MockHttpServletRequest(), null, false, SYS_USER_ID);
        sampleEditService.editSample(form, new MockHttpServletRequest(), null, false, SYS_USER_ID);

        assertEquals(organizations, count("clinlims.organization"));
        assertEquals(List.of("1"), siteRequesterIds());
    }

    @Test
    public void editSample_withANewSiteName_createsThatSite() {
        SampleEditForm form = modifiedForm();
        form.getSampleOrderItems().setReferringSiteName("QA_AUTO New Referring Clinic");
        referringClinicTypeId();
        int organizations = count("clinlims.organization");

        sampleEditService.editSample(form, new MockHttpServletRequest(), null, false, SYS_USER_ID);

        assertEquals(organizations + 1, count("clinlims.organization"));
        String newSiteId = jdbcTemplate.queryForObject(
                "SELECT id::text FROM clinlims.organization WHERE name = 'QA_AUTO New Referring Clinic'", String.class);
        assertEquals(List.of(newSiteId), siteRequesterIds());
    }

    /**
     * The referring-clinic organization type, under the id TableIdService read at
     * startup. Other fixtures truncate organization_type, which would leave that id
     * dangling for the rest of the run.
     */
    private String referringClinicTypeId() {
        String id = TableIdService.getInstance().REFERRING_ORG_TYPE_ID;
        jdbcTemplate.update("INSERT INTO clinlims.organization_type (id, short_name, description, lastupdated)"
                + " VALUES (?, 'referring clinic', 'Name of org who can order lab tests', now())"
                + " ON CONFLICT (id) DO NOTHING", Long.valueOf(id));
        return id;
    }

    private List<String> siteRequesterIds() {
        return jdbcTemplate.queryForList("SELECT sr.requester_id::text FROM clinlims.sample_requester sr"
                + " JOIN clinlims.requester_type rt ON rt.id = sr.requester_type_id"
                + " WHERE sr.sample_id = 1 AND rt.requester_type = 'organization'", String.class);
    }

    private SampleEditForm modifiedForm() {
        SampleEditForm form = createBaseForm();
        form.getSampleOrderItems().setModified(true);
        form.getSampleOrderItems().setSampleId("1");
        form.getSampleOrderItems().setReceivedDateForDisplay("12/02/2024");
        form.getSampleOrderItems().setReceivedTime("10:00");
        return form;
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private List<String> personRequesterIds() {
        return jdbcTemplate.queryForList("SELECT sr.requester_id::text FROM clinlims.sample_requester sr"
                + " JOIN clinlims.requester_type rt ON rt.id = sr.requester_type_id"
                + " WHERE sr.sample_id = 1 AND rt.requester_type = 'provider'", String.class);
    }

    /**
     * OGC-1266: an order saved without a patient (EQA, environmental, a declared
     * no-patient order) could not be modified at all; the save dereferenced the
     * missing patient.
     */
    @Test
    public void editSample_onAnOrderWithoutAPatient_shouldSaveTheChange() {
        Sample sample = sampleService.getSampleByAccessionNumber(ACCESSION_NUMBER);
        SampleHuman lookup = new SampleHuman();
        lookup.setSampleId(sample.getId());
        SampleHuman link = sampleHumanService.getDataBySample(lookup);
        link.setPatientId(null);
        link.setSysUserId(SYS_USER_ID);
        sampleHumanService.update(link);
        SampleEditForm form = createBaseForm();
        form.getSampleOrderItems().setPriority(OrderPriority.STAT);

        sampleEditService.editSample(form, new MockHttpServletRequest(), sample, true, SYS_USER_ID);

        assertEquals(OrderPriority.STAT, sampleService.getSampleByAccessionNumber(ACCESSION_NUMBER).getPriority());
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
    public void editSample_withRemovedSampleItem_shouldCancelEveryAnalysisOnThatItemOnly() throws Exception {
        executeDataSetWithStateManagement(REMOVE_SAMPLE_DATASET_XML);
        SampleEditForm form = createBaseForm();

        SampleEditItem firstRow = new SampleEditItem();
        firstRow.setAccessionNumber(ACCESSION_NUMBER + "-1");
        firstRow.setRemoveSample(true);
        firstRow.setSampleItemId(EXISTING_SAMPLE_ITEM_ID);
        firstRow.setAnalysisId(EXISTING_ANALYSIS_ID);
        form.getExistingTests().add(firstRow);

        SampleEditItem secondRow = new SampleEditItem();
        secondRow.setAccessionNumber("");
        secondRow.setSampleItemId(EXISTING_SAMPLE_ITEM_ID);
        secondRow.setAnalysisId(SECOND_ANALYSIS_ON_SAME_ITEM_ID);
        form.getExistingTests().add(secondRow);

        SampleEditItem otherItemRow = new SampleEditItem();
        otherItemRow.setAccessionNumber(ACCESSION_NUMBER + "-2");
        otherItemRow.setSampleItemId(OTHER_SAMPLE_ITEM_ID);
        otherItemRow.setAnalysisId(ANALYSIS_ON_OTHER_ITEM_ID);
        form.getExistingTests().add(otherItemRow);

        MockHttpServletRequest request = new MockHttpServletRequest();
        Sample sample = sampleService.getSampleByAccessionNumber(ACCESSION_NUMBER);
        String otherItemStatusBefore = sampleItemService.get(OTHER_SAMPLE_ITEM_ID).getStatusId();

        sampleEditService.editSample(form, request, sample, true, SYS_USER_ID);

        String canceledSampleStatus = SpringContext.getBean(IStatusService.class)
                .getStatusID(org.openelisglobal.common.services.StatusService.SampleStatus.Canceled);
        String canceledAnalysisStatus = SpringContext.getBean(IStatusService.class)
                .getStatusID(AnalysisStatus.Canceled);
        String notStartedAnalysisStatus = SpringContext.getBean(IStatusService.class)
                .getStatusID(AnalysisStatus.NotStarted);

        assertEquals("Removed sample item should be Canceled", canceledSampleStatus,
                sampleItemService.get(EXISTING_SAMPLE_ITEM_ID).getStatusId());
        assertEquals("Analysis on the ticked row should be Canceled", canceledAnalysisStatus,
                analysisService.get(EXISTING_ANALYSIS_ID).getStatusId());
        assertEquals("Second analysis on the removed sample item should be Canceled too", canceledAnalysisStatus,
                analysisService.get(SECOND_ANALYSIS_ON_SAME_ITEM_ID).getStatusId());
        assertEquals("Analysis on the other sample item must stay untouched", notStartedAnalysisStatus,
                analysisService.get(ANALYSIS_ON_OTHER_ITEM_ID).getStatusId());
        assertEquals("Other sample item must stay untouched", otherItemStatusBefore,
                sampleItemService.get(OTHER_SAMPLE_ITEM_ID).getStatusId());

        List<String> updatedAnalyses = sampleEditService.getUpdatedAnalysisList();
        assertTrue("Updated list should carry the ticked row's analysis",
                updatedAnalyses.contains(EXISTING_ANALYSIS_ID));
        assertTrue("Updated list should carry the second analysis on the removed item",
                updatedAnalyses.contains(SECOND_ANALYSIS_ON_SAME_ITEM_ID));
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
