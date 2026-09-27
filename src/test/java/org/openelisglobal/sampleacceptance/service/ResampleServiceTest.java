package org.openelisglobal.sampleacceptance.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.qaevent.service.NCEventService;
import org.openelisglobal.qaevent.service.NceSpecimenService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;
import org.openelisglobal.sample.service.SampleComplianceStandardService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sample.valueholder.SampleComplianceStandard;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;
import org.openelisglobal.sampleproject.service.SampleProjectService;
import org.openelisglobal.sampleproject.valueholder.SampleProject;
import org.openelisglobal.sampletyperequest.service.SampleTypeRequestService;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

/**
 * TDD spec for the Resample workflow (S-09 / OGC-580, FR-10).
 *
 * <p>
 * Each test drives the single {@link ResampleService#resample} entry point and
 * asserts the resulting persisted state — so a test can only pass if a real
 * implementation actually performs that step (no false positives). The original
 * order is the complete fixture order (sample 1: Glucose on a Blood specimen,
 * with patient, requesting provider, and customer organization).
 */
@Rollback
public class ResampleServiceTest extends BaseWebContextSensitiveTest {

    private static final String ORIGINAL_SAMPLE_ID = "1";
    private static final String ORIGINAL_ACCESSION = "24-00001";
    private static final String REPLACEMENT_ACCESSION = "24-00002";
    private static final String REASON = "Sample volume inadequate - tube cracked in transit";
    private static final Integer TEST_USER_ID = 1;

    @Autowired
    private ResampleService resampleService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private NCEventService ncEventService;

    @Autowired
    private NceSpecimenService nceSpecimenService;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private SampleOrganizationService sampleOrganizationService;

    @Autowired
    private AccessionNumberValidatorFactory accessionNumberValidatorFactory;

    @Autowired
    private ObservationHistoryService observationHistoryService;

    @Autowired
    private SampleProjectService sampleProjectService;

    @Autowired
    private SampleComplianceStandardService sampleComplianceStandardService;

    @Autowired
    private SampleTypeRequestService sampleTypeRequestService;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/resample-workflow.xml");
        // status_of_sample was truncated+reseeded by the fixture; reload the cache so
        // SampleRejected / Entered resolve to the new ids.
        statusService.refreshCache();
        // The test context mocks AccessionNumberValidatorFactory; give it a working
        // generator so resample mints a conformant lab number via the production path
        // (and so generateAccessionNumber does not fail loud on a missing generator).
        IAccessionNumberGenerator generator = mock(IAccessionNumberGenerator.class);
        when(generator.getNextAvailableAccessionNumber(any(), anyBoolean())).thenReturn(REPLACEMENT_ACCESSION);
        when(accessionNumberValidatorFactory.getGenerator(AccessionFormat.MAIN)).thenReturn(generator);
    }

    @After
    public void resetAccessionFactory() {
        // Pristine state for other tests sharing the cached Spring context.
        reset(accessionNumberValidatorFactory);
    }

    /** The id of the fixture order's single specimen — the one we resample. */
    private String firstItemId() {
        return sampleItemService.getSampleItemsBySampleId(ORIGINAL_SAMPLE_ID).get(0).getId();
    }

    /**
     * Per-specimen resample cancels the order's matching-type sample_type_request,
     * so the request-based Enter Order / Collect views drop the rejected type (the
     * replacement order carries it). The fixture seeds one REQUESTED request for
     * the Blood specimen's type.
     */
    @Test
    public void resample_cancelsTheMatchingTypeRequest() {
        List<SampleTypeRequest> before = sampleTypeRequestService.getRequestsBySampleId(ORIGINAL_SAMPLE_ID);
        assertEquals("fixture sanity: one seeded request", 1, before.size());
        assertEquals("fixture sanity: request starts REQUESTED", SampleTypeRequest.Status.REQUESTED,
                before.get(0).getStatus());

        resampleService.resample(firstItemId(), REASON, TEST_USER_ID);

        List<SampleTypeRequest> after = sampleTypeRequestService.getRequestsBySampleId(ORIGINAL_SAMPLE_ID);
        assertEquals(1, after.size());
        assertEquals("the matching-type request is cancelled by the resample", SampleTypeRequest.Status.CANCELLED,
                after.get(0).getStatus());
    }

    @Test
    public void resample_recordsNceLinkedToTheOriginalSampleItem_withReason() {
        String originalItemId = sampleItemService.getSampleItemsBySampleId(ORIGINAL_SAMPLE_ID).get(0).getId();

        ResampleResult result = resampleService.resample(firstItemId(), REASON, TEST_USER_ID);

        assertNotNull("an NCE id is returned", result.getNceId());
        List<NceSpecimen> specimens = nceSpecimenService.getSpecimenBySampleItemId(Integer.parseInt(originalItemId));
        assertEquals("NCE is linked to the original sample item", 1, specimens.size());
        assertEquals(result.getNceId(), specimens.get(0).getNceId());

        NcEvent nce = ncEventService.get(result.getNceId());
        assertNotNull(nce);
        assertEquals("NCE description is exactly the resample reason", REASON, nce.getDescription());
        assertEquals("NCE number is the RS- prefixed replacement accession", "RS-" + REPLACEMENT_ACCESSION,
                nce.getNceNumber());
    }

    /**
     * Per-specimen (S-09 decision): a resample rejects only the failed
     * {@code sample_item} and cancels its analyses — the parent order is NOT
     * rejected, so its accepted specimens proceed to Label &amp; Store.
     */
    @Test
    public void resample_rejectsTheFailedSpecimenButNotTheWholeOrder() {
        String itemId = firstItemId();
        String parentStatusBefore = sampleService.get(ORIGINAL_SAMPLE_ID).getStatusId();

        resampleService.resample(itemId, REASON, TEST_USER_ID);

        SampleItem rejectedItem = sampleItemService.get(itemId);
        assertTrue("the failed specimen is flagged rejected", rejectedItem.isRejected());
        List<Analysis> itemAnalyses = analysisService.getAnalysesBySampleItem(rejectedItem);
        assertEquals("the specimen has its one analysis", 1, itemAnalyses.size());
        assertEquals("the rejected specimen's analysis is canceled", statusService.getStatusID(AnalysisStatus.Canceled),
                itemAnalyses.get(0).getStatusId());

        Sample parent = sampleService.get(ORIGINAL_SAMPLE_ID);
        assertEquals("the parent order keeps its status (not rejected)", parentStatusBefore, parent.getStatusId());
        assertNotEquals("the parent order is NOT moved to Sample Rejected",
                statusService.getStatusID(SampleStatus.SampleRejected), parent.getStatusId());
    }

    @Test
    public void resample_createsADistinctDraftOrderWithAFreshAccessionNumber() {
        ResampleResult result = resampleService.resample(firstItemId(), REASON, TEST_USER_ID);

        assertNotNull(result.getNewSampleId());
        assertNotEquals("new order is a different sample", ORIGINAL_SAMPLE_ID, result.getNewSampleId());

        Sample replacement = sampleService.get(result.getNewSampleId());
        assertNotNull(replacement);
        assertNotEquals("fresh lab number, not the original's", ORIGINAL_ACCESSION, replacement.getAccessionNumber());
        assertEquals("replacement carries the freshly generated lab number", REPLACEMENT_ACCESSION,
                replacement.getAccessionNumber());
        // The replacement sample must carry the ORDER-workflow status, not the SAMPLE
        // status, or it never surfaces in reception / result entry (OGC-580
        // regression).
        assertEquals("replacement sample is at OrderStatus.Entered so it enters the result-entry pipeline",
                statusService.getStatusID(OrderStatus.Entered), replacement.getStatusId());

        List<SampleItem> replacementItems = sampleItemService.getSampleItemsBySampleId(result.getNewSampleId());
        assertEquals("replacement has the single cloned specimen", 1, replacementItems.size());
        assertEquals("the cloned specimen carries the sample-level status (SampleStatus.Entered)",
                statusService.getStatusID(SampleStatus.Entered), replacementItems.get(0).getStatusId());
    }

    /**
     * FR-10.3: the replacement must carry the original's compliance standard(s).
     * The link lives in sample_compliance_standards (the normalized table the
     * compliance report reads), so the resample must copy those rows — copying only
     * the observation_history representation would leave the replacement "No
     * standard linked" in the report.
     */
    @Test
    public void resample_copiesTheComplianceStandardLinkToTheReplacement() {
        List<SampleComplianceStandard> originalLinks = sampleComplianceStandardService
                .getAllForSample(ORIGINAL_SAMPLE_ID);
        assertEquals("fixture sanity: the original order has one compliance standard", 1, originalLinks.size());
        String standardId = originalLinks.get(0).getComplianceStandard().getId();

        ResampleResult result = resampleService.resample(firstItemId(), REASON, TEST_USER_ID);

        List<SampleComplianceStandard> replacementLinks = sampleComplianceStandardService
                .getAllForSample(result.getNewSampleId());
        assertEquals("replacement carries exactly one copied compliance standard link", 1, replacementLinks.size());
        assertEquals("replacement is linked to the same compliance standard as the original", standardId,
                replacementLinks.get(0).getComplianceStandard().getId());
        assertNotEquals("the link is a new row for the replacement, not the original's row",
                originalLinks.get(0).getId(), replacementLinks.get(0).getId());
    }

    @Test
    public void resample_setsBidirectionalLinksBetweenOriginalAndReplacement() {
        ResampleResult result = resampleService.resample(firstItemId(), REASON, TEST_USER_ID);

        Sample original = sampleService.get(ORIGINAL_SAMPLE_ID);
        Sample replacement = sampleService.get(result.getNewSampleId());

        assertEquals("original points to the replacement", result.getNewSampleId(), original.getResampledToSampleId());
        assertEquals("replacement points back to the original", ORIGINAL_SAMPLE_ID,
                replacement.getResampledFromSampleId());
    }

    @Test
    public void resample_clonesSampleTypesAndTestsIntoTheReplacement() {
        SampleItem originalItem = sampleItemService.getSampleItemsBySampleId(ORIGINAL_SAMPLE_ID).get(0);
        Analysis originalAnalysis = analysisService.getAnalysesBySampleId(ORIGINAL_SAMPLE_ID).get(0);

        ResampleResult result = resampleService.resample(firstItemId(), REASON, TEST_USER_ID);

        List<SampleItem> newItems = sampleItemService.getSampleItemsBySampleId(result.getNewSampleId());
        assertEquals("same number of specimens", 1, newItems.size());
        assertEquals("same sample type", originalItem.getTypeOfSampleId(), newItems.get(0).getTypeOfSampleId());
        assertNotEquals("specimen is a new row, not the original", originalItem.getId(), newItems.get(0).getId());

        List<Analysis> newAnalyses = analysisService.getAnalysesBySampleId(result.getNewSampleId());
        assertEquals("same number of tests", 1, newAnalyses.size());
        assertEquals("same test", originalAnalysis.getTest().getId(), newAnalyses.get(0).getTest().getId());
        assertNotEquals("analysis is a new row, not the original", originalAnalysis.getId(),
                newAnalyses.get(0).getId());
    }

    /**
     * FR-10: the replacement must carry the same customer/requester so it is a
     * usable order (patient, requesting provider, and customer organization), not
     * an orphaned one. The fixture's original (sample 1) has a patient, provider,
     * and organization.
     */
    @Test
    public void resample_clonesPatientProviderAndOrganizationOntoTheReplacement() {
        Sample original = sampleService.get(ORIGINAL_SAMPLE_ID);
        Patient originalPatient = sampleHumanService.getPatientForSample(original);
        Provider originalProvider = sampleHumanService.getProviderForSample(original);
        SampleOrganization originalOrg = sampleOrganizationService.getDataBySample(original);

        ResampleResult result = resampleService.resample(firstItemId(), REASON, TEST_USER_ID);
        Sample replacement = sampleService.get(result.getNewSampleId());

        Patient clonedPatient = sampleHumanService.getPatientForSample(replacement);
        assertNotNull("replacement carries a patient (not an orphaned order)", clonedPatient);
        assertEquals("same patient as the original", originalPatient.getId(), clonedPatient.getId());

        Provider clonedProvider = sampleHumanService.getProviderForSample(replacement);
        assertNotNull("replacement carries the requesting provider", clonedProvider);
        assertEquals("same provider/requester as the original", originalProvider.getId(), clonedProvider.getId());

        SampleOrganization clonedOrg = sampleOrganizationService.getDataBySample(replacement);
        assertNotNull("replacement carries the customer organization", clonedOrg);
        assertEquals("same organization/customer as the original", originalOrg.getOrganization().getId(),
                clonedOrg.getOrganization().getId());
    }

    /**
     * FR-10: the replacement is a draft awaiting collection, so its cloned specimen
     * and analyses must start fresh (SampleEntered / NotStarted) rather than
     * inheriting the finalized original's already-resulted status.
     */
    @Test
    public void resample_clonedSpecimenAndAnalysisAwaitCollection_notResulted() {
        SampleItem originalItem = sampleItemService.getSampleItemsBySampleId(ORIGINAL_SAMPLE_ID).get(0);
        Analysis originalAnalysis = analysisService.getAnalysesBySampleId(ORIGINAL_SAMPLE_ID).get(0);
        String finalizedItemStatus = originalItem.getStatusId();
        String finalizedAnalysisStatus = originalAnalysis.getStatusId();

        ResampleResult result = resampleService.resample(firstItemId(), REASON, TEST_USER_ID);

        SampleItem newItem = sampleItemService.getSampleItemsBySampleId(result.getNewSampleId()).get(0);
        assertEquals("cloned specimen awaits collection (SampleEntered)",
                statusService.getStatusID(SampleStatus.Entered), newItem.getStatusId());
        assertNotEquals("cloned specimen does not inherit the finalized status", finalizedItemStatus,
                newItem.getStatusId());

        Analysis newAnalysis = analysisService.getAnalysesBySampleId(result.getNewSampleId()).get(0);
        assertEquals("cloned analysis is NotStarted, not already-resulted",
                statusService.getStatusID(AnalysisStatus.NotStarted), newAnalysis.getStatusId());
        assertNotEquals("cloned analysis does not inherit the finalized status", finalizedAnalysisStatus,
                newAnalysis.getStatusId());
    }

    // ---- Gap 1 (FR-10.3 clone completeness) — RED until ResampleServiceImpl also
    // copies the order-context fields beyond patient/provider/org/types/tests.
    // These currently FAIL: the replacement comes out missing this data, which is
    // the
    // gap for environmental/vector (SILNAS) orders. See S09-backend-gap-tasks.md
    // gap 1.

    @Test
    public void resample_clonesOrderContextScalarsOntoTheReplacement() {
        // Fixture sanity: the original genuinely carries these (so a RED here means the
        // clone dropped them, not that the fixture failed to load).
        Sample original = sampleService.get(ORIGINAL_SAMPLE_ID);
        assertEquals("fixture sanity: original requester reference", "ENV-REF-001", original.getClientReference());
        assertEquals("fixture sanity: original priority", OrderPriority.STAT, original.getPriority());
        assertEquals("fixture sanity: original GPS latitude", Double.valueOf(12.34), original.getGpsLatitude());

        ResampleResult result = resampleService.resample(firstItemId(), REASON, TEST_USER_ID);
        Sample replacement = sampleService.get(result.getNewSampleId());

        assertEquals("requester reference carried to the replacement", "ENV-REF-001", replacement.getClientReference());
        assertEquals("order priority carried to the replacement", OrderPriority.STAT, replacement.getPriority());
        assertEquals("GPS latitude carried to the replacement", Double.valueOf(12.34), replacement.getGpsLatitude());
        assertEquals("GPS longitude carried to the replacement", Double.valueOf(56.78), replacement.getGpsLongitude());
    }

    @Test
    public void resample_clonesProgramAndQuestionnaireOntoTheReplacement() {
        SampleProject originalProject = sampleProjectService.getSampleProjectBySampleId(ORIGINAL_SAMPLE_ID);
        assertNotNull("fixture sanity: original has a program/compliance project", originalProject);

        ResampleResult result = resampleService.resample(firstItemId(), REASON, TEST_USER_ID);
        String newSampleId = result.getNewSampleId();

        SampleProject clonedProject = sampleProjectService.getSampleProjectBySampleId(newSampleId);
        assertNotNull("replacement carries the program / compliance standard (site)", clonedProject);
        assertEquals("same program as the original", originalProject.getProjectId(), clonedProject.getProjectId());

        List<ObservationHistory> clonedObs = observationHistoryService.getObservationHistoriesBySampleId(newSampleId);
        assertEquals("replacement carries the questionnaire / default-conditions answers", 1, clonedObs.size());
        assertEquals("same observation value as the original", "River bank A", clonedObs.get(0).getValue());
    }
}
