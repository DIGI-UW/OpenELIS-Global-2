package org.openelisglobal.microbiology;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.SampleAddService;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.service.MicroCaseService;
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
        cultureTest = fixtures.createCatalogCultureTest(methodId);
        patient = fixtures.createPatient("OGC782M4");
        sampleType = fixtures.getOrCreateActiveSampleType();
    }

    /**
     * Ordering a culture-flagged test persists the order but opens no microbiology
     * case; V1 order routing is retired.
     */
    @Test
    public void orderSaveWithCultureTestCreatesNoCase() {
        Sample sample = newSample();

        SamplePatientUpdateData firstSave = orderUpdate(sample, null);
        persist(firstSave);

        SampleItem savedItem = firstSave.getSampleItemsTests().getFirst().item;
        Analysis savedAnalysis = analysisService.getAnalysisBySampleItemAndTest(savedItem.getId(), cultureTest.getId());

        assertNotNull(sample.getId());
        assertNotNull(savedItem.getId());
        assertNotNull(savedAnalysis);
        assertTrue(caseService.getSiblingCases(savedItem.getId()).isEmpty());

        SamplePatientUpdateData repeatedSave = orderUpdate(sample, savedItem.getId());
        persist(repeatedSave);

        SampleItem repeatedItem = repeatedSave.getSampleItemsTests().getFirst().item;
        Analysis repeatedAnalysis = analysisService.getAnalysisBySampleItemAndTest(repeatedItem.getId(),
                cultureTest.getId());

        assertNotNull(repeatedAnalysis);
        assertTrue(caseService.getSiblingCases(repeatedItem.getId()).isEmpty());
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

    private void persist(SamplePatientUpdateData updateData) {
        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setPatientPK(patient.getId());
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        form.setPatientProperties(patientInfo);

        PatientManagementUpdate patientUpdate = SpringContext.getBean(PatientManagementUpdate.class);
        samplePatientEntryService.persistData(updateData, patientUpdate, patientInfo, form,
                new MockHttpServletRequest());
    }
}
