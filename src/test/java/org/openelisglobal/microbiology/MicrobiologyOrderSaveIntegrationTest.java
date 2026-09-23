package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.SampleAddService;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
import org.openelisglobal.microbiology.service.MicroCaseAnalysisService;
import org.openelisglobal.microbiology.service.MicroCaseOrderDetailService;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseOrderDetail;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.service.PatientManagementUpdate;
import org.openelisglobal.sample.service.SamplePatientEntryService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MicrobiologyOrderSaveIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MicrobiologyTestFixtures fixtures;

    @Autowired
    private SamplePatientEntryService samplePatientEntryService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private MicroCaseService caseService;

    @Autowired
    private MicroCaseAnalysisService caseAnalysisService;

    @Autowired
    private MicroCaseOrderDetailService orderDetailService;

    private String userId;
    private org.openelisglobal.test.valueholder.Test cultureTest;
    private Patient patient;
    private TypeOfSample sampleType;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        userId = fixtures.defaultUserId();
        String methodId = fixtures.createMethodId();
        fixtures.createReferenceData(methodId);
        cultureTest = fixtures.createCatalogCultureTest(methodId, MicroWorkflowType.BACTERIOLOGY);
        patient = fixtures.createPatient("OGC782M4");
        sampleType = fixtures.getOrCreateActiveSampleType();
    }

    /**
     * Covers the real transactional save orchestration; the supported browser
     * interaction remains covered by the registered M-03 Playwright journey.
     */
    @Test
    public void supportedOrderSaveCreatesOneCaseAndRemainsIdempotent() {
        Sample sample = newSample();
        MicroCaseOrderDetailRequestForm orderDetail = orderDetail();

        SamplePatientUpdateData firstSave = orderUpdate(sample, null);
        persist(firstSave, orderDetail);

        SampleItem savedItem = firstSave.getSampleItemsTests().getFirst().item;
        Analysis savedAnalysis = analysisService.getAnalysisBySampleItemAndTest(savedItem.getId(), cultureTest.getId());
        List<MicroCase> firstCases = caseService.getSiblingCases(savedItem.getId());

        assertNotNull(sample.getId());
        assertNotNull(savedItem.getId());
        assertNotNull(savedAnalysis);
        assertEquals(1, firstCases.size());
        assertOrderDetail(firstCases.getFirst(), orderDetail);
        assertCaseAnalysisLink(firstCases.getFirst(), savedAnalysis);

        SamplePatientUpdateData repeatedSave = orderUpdate(sample, savedItem.getId());
        persist(repeatedSave, orderDetail);

        SampleItem repeatedItem = repeatedSave.getSampleItemsTests().getFirst().item;
        Analysis repeatedAnalysis = analysisService.getAnalysisBySampleItemAndTest(repeatedItem.getId(),
                cultureTest.getId());
        List<MicroCase> repeatedCases = caseService.getSiblingCases(repeatedItem.getId());

        assertEquals(savedItem.getId(), repeatedItem.getId());
        assertEquals(savedAnalysis.getId(), repeatedAnalysis.getId());
        assertEquals(1, repeatedCases.size());
        assertEquals(firstCases.getFirst().getId(), repeatedCases.getFirst().getId());
        assertOrderDetail(repeatedCases.getFirst(), orderDetail);
        assertCaseAnalysisLink(repeatedCases.getFirst(), repeatedAnalysis);
    }

    private Sample newSample() {
        Sample sample = new Sample();
        sample.setAccessionNumber("M4" + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
        sample.setEnteredDate(new Date(System.currentTimeMillis()));
        sample.setReceivedTimestamp(Timestamp.from(Instant.now()));
        sample.setStatusId(fixtures.ensureSampleEnteredStatus());
        sample.setSysUserId(userId);
        return sample;
    }

    private SamplePatientUpdateData orderUpdate(Sample sample, String existingSampleItemId) {
        String itemIdAttribute = existingSampleItemId == null ? "" : " sampleItemId='" + existingSampleItemId + "'";
        String sampleXml = "<samples><sample sampleID='" + sampleType.getId() + "' tests='" + cultureTest.getId()
                + "' testSectionMap='' testSampleTypeMap='' panels='' date='' time='' initialConditionIds=''"
                + itemIdAttribute + "/></samples>";
        SampleAddService sampleAddService = new SampleAddService(sampleXml, userId, sample, "");

        SamplePatientUpdateData updateData = new SamplePatientUpdateData(userId);
        updateData.setSample(sample);
        updateData.setSampleAddService(sampleAddService);
        updateData.setSampleItemsTests(sampleAddService.createSampleTestCollection());
        return updateData;
    }

    private void persist(SamplePatientUpdateData updateData, MicroCaseOrderDetailRequestForm orderDetail) {
        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setPatientPK(patient.getId());
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        form.setPatientProperties(patientInfo);
        form.setMicrobiologyOrderDetail(orderDetail);

        PatientManagementUpdate patientUpdate = SpringContext.getBean(PatientManagementUpdate.class);
        samplePatientEntryService.persistData(updateData, patientUpdate, patientInfo, form,
                new MockHttpServletRequest());
    }

    private MicroCaseOrderDetailRequestForm orderDetail() {
        MicroCaseOrderDetailRequestForm detail = new MicroCaseOrderDetailRequestForm();
        detail.cultureMethodId = cultureTest.getMethod().getId();
        detail.culturePurpose = "CLINICAL_DIAGNOSTIC";
        detail.patientOrigin = "INPATIENT";
        detail.admissionDate = "2026-08-17";
        detail.numberOfSets = 2;
        detail.clinicalHistory = "Persistent fever after antibiotics";
        detail.antibioticExposure = true;
        return detail;
    }

    private void assertOrderDetail(MicroCase microCase, MicroCaseOrderDetailRequestForm expected) {
        MicroCaseOrderDetail actual = orderDetailService.getOrderDetail(microCase.getId());
        assertNotNull(actual);
        assertEquals(expected.cultureMethodId, actual.getCultureMethodId());
        assertEquals(expected.patientOrigin, actual.getPatientOrigin());
        assertEquals(expected.culturePurpose, actual.getCulturePurpose());
        assertEquals(LocalDate.parse(expected.admissionDate), actual.getAdmissionDate());
        assertEquals(expected.numberOfSets, actual.getNumberOfSets());
        assertEquals(expected.clinicalHistory, actual.getClinicalHistory());
        assertEquals(expected.antibioticExposure, actual.getAntibioticExposure());
    }

    private void assertCaseAnalysisLink(MicroCase microCase, Analysis analysis) {
        var links = caseAnalysisService.getCaseAnalyses(microCase.getId());
        assertEquals(1, links.size());
        assertEquals(microCase.getId(), links.getFirst().getCaseId());
        assertEquals(analysis.getId(), links.getFirst().getAnalysisId());
    }
}
