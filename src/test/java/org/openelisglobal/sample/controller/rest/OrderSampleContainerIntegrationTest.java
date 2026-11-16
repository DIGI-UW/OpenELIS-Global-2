package org.openelisglobal.sample.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.services.SampleAddService;
import org.openelisglobal.containertype.form.ContainerTypeForm;
import org.openelisglobal.containertype.form.ContainerTypeView;
import org.openelisglobal.containertype.service.ContainerTypeService;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.service.PatientManagementUpdate;
import org.openelisglobal.sample.service.SamplePatientEntryService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1611 P4 (FR-G3, G8): a sample is saved with the container type it was
 * drawn in, the order reads it back, and a container that does not yield the
 * sample's type, or is no longer in use, is refused unless the sample already
 * had it.
 */
@Transactional
public class OrderSampleContainerIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private SamplePatientEntryService samplePatientEntryService;
    @Autowired
    private OrderSearchRestController orderSearchRestController;
    @Autowired
    private ContainerTypeService containerTypeService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private SampleService sampleService;
    @PersistenceContext
    private EntityManager entityManager;

    private String userId;
    private Patient patient;
    private TypeOfSample serum;
    private ContainerTypeView sst;
    private ContainerTypeView redTop;
    private ContainerTypeView pst;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        userId = fixtures.defaultUserId();
        patient = fixtures.createPatient("CNTR");
        serum = fixtures.createTypeOfSample();
        TypeOfSample plasma = fixtures.createTypeOfSample();
        sst = container("QA1611S-SST", "QA1611S SST", serum.getId());
        redTop = container("QA1611S-RED", "QA1611S Red top", serum.getId());
        pst = container("QA1611S-PST", "QA1611S PST", plasma.getId());
    }

    @After
    public void forgetTheSampleTypeLinks() {
        typeOfSampleService.clearCache();
    }

    @Test
    public void theContainerTypeIsSavedAndReadBackAndCanBeChanged() {
        Sample sample = newSample();
        persist(sample, sampleXml("", String.valueOf(sst.id())));

        Map<String, Object> saved = onlySample(sample.getAccessionNumber());
        assertEquals(String.valueOf(sst.id()), saved.get("containerTypeId"));
        assertEquals("QA1611S SST", saved.get("containerTypeName"));

        String sampleItemId = String.valueOf(saved.get("sampleItemId"));
        persist(reread(sample), sampleXml(sampleItemId, String.valueOf(redTop.id())));
        assertEquals(String.valueOf(redTop.id()), onlySample(sample.getAccessionNumber()).get("containerTypeId"));

        persist(reread(sample), sampleXml(sampleItemId, null));
        assertEquals("a save that does not send it keeps it", String.valueOf(redTop.id()),
                onlySample(sample.getAccessionNumber()).get("containerTypeId"));
    }

    @Test
    public void aSampleWithoutAContainerTypeReadsBackEmpty() {
        Sample sample = newSample();
        persist(sample, sampleXml("", ""));

        Map<String, Object> saved = onlySample(sample.getAccessionNumber());
        assertEquals("", saved.get("containerTypeId"));
        assertEquals("", saved.get("containerTypeName"));
    }

    @Test
    public void aContainerThatDoesNotYieldTheSampleTypeIsRefused() {
        Sample sample = newSample();

        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> persist(sample, sampleXml("", String.valueOf(pst.id()))));
        assertEquals("order.sample.container.refused", refused.getMessage());
    }

    @Test
    public void aRetiredContainerIsRefusedForANewSampleButKeptByOneThatHadIt() {
        Sample sample = newSample();
        persist(sample, sampleXml("", String.valueOf(sst.id())));
        String sampleItemId = String.valueOf(onlySample(sample.getAccessionNumber()).get("sampleItemId"));
        containerTypeService.setActive(sst.id(), false, "1");

        persist(reread(sample), sampleXml(sampleItemId, String.valueOf(sst.id())));
        Map<String, Object> kept = onlySample(sample.getAccessionNumber());
        assertEquals(String.valueOf(sst.id()), kept.get("containerTypeId"));
        assertEquals("a retired container still reads back by name", "QA1611S SST", kept.get("containerTypeName"));

        Sample another = newSample();
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> persist(another, sampleXml("", String.valueOf(sst.id()))));
        assertEquals("order.sample.container.refused", refused.getMessage());
    }

    @Test
    public void aContainerTypeThatIsNotANumberIsRefused() {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> persist(newSample(), sampleXml("", "tube")));
        assertEquals("order.sample.container.refused", refused.getMessage());
    }

    @Test
    public void anOrdersSamplesReadBackInTheirSortOrder() {
        Sample sample = newSample();
        persist(sample, "<samples>" + sampleTag(1) + sampleTag(2) + sampleTag(3) + "</samples>");
        List<String> ids = samples(sample.getAccessionNumber()).stream()
                .map(row -> String.valueOf(row.get("sampleItemId"))).toList();

        entityManager.flush();
        jdbcTemplate.update("UPDATE clinlims.sample_item SET sort_order = 3 WHERE id = ?::numeric", ids.get(0));
        jdbcTemplate.update("UPDATE clinlims.sample_item SET sort_order = 1 WHERE id = ?::numeric", ids.get(2));
        entityManager.clear();

        List<Map<String, Object>> reread = samples(sample.getAccessionNumber());
        assertEquals(List.of("1", "2", "3"), reread.stream().map(row -> String.valueOf(row.get("sortOrder"))).toList());
        assertEquals(List.of(ids.get(2), ids.get(1), ids.get(0)),
                reread.stream().map(row -> String.valueOf(row.get("sampleItemId"))).toList());
    }

    private String sampleTag(int index) {
        return "<sample sampleID='" + index + "' typeId='" + serum.getId() + "' sampleItemId=''"
                + " clientKey='' tests='' panels='' testSectionMap='' testSampleTypeMap='' rejected='false'/>";
    }

    private String sampleXml(String sampleItemId, String containerTypeId) {
        return "<samples><sample sampleID='1' typeId='" + serum.getId() + "' sampleItemId='" + sampleItemId
                + "' clientKey='' tests='' panels='' testSectionMap='' testSampleTypeMap='' rejected='false'"
                + (containerTypeId == null ? "" : " containerTypeId='" + containerTypeId + "'") + "/></samples>";
    }

    private ContainerTypeView container(String code, String name, String sampleTypeId) {
        return containerTypeService.save(null, new ContainerTypeForm(code, name, null, null, null, null, null, null,
                null, null, List.of("CLINICAL"), List.of(sampleTypeId)), "1");
    }

    private Map<String, Object> onlySample(String labNumber) {
        List<Map<String, Object>> samples = samples(labNumber);
        assertEquals(1, samples.size());
        return samples.get(0);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> samples(String labNumber) {
        ResponseEntity<Map<String, Object>> response = orderSearchRestController.searchOrder(labNumber,
                new MockHttpServletRequest());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return (List<Map<String, Object>>) response.getBody().get("samples");
    }

    private Sample reread(Sample sample) {
        return sampleService.getSampleByAccessionNumber(sample.getAccessionNumber());
    }

    private Sample newSample() {
        Sample sample = new Sample();
        sample.setAccessionNumber("CNT" + UUID.randomUUID().toString().replace("-", "").substring(0, 9));
        sample.setEnteredDate(new Date(System.currentTimeMillis()));
        sample.setReceivedTimestamp(Timestamp.from(Instant.now()));
        sample.setStatusId(fixtures.ensureSampleEnteredStatus());
        sample.setSysUserId(userId);
        return sample;
    }

    private void persist(Sample sample, String sampleXml) {
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        SampleAddService sampleAddService = new SampleAddService(sampleXml, userId, sample, "");
        SamplePatientUpdateData updateData = new SamplePatientUpdateData(userId);
        updateData.setSample(sample);
        updateData.setSampleAddService(sampleAddService);
        updateData.setSampleItemsTests(sampleAddService.createSampleTestCollection());

        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setPatientPK(patient.getId());
        form.setPatientProperties(patientInfo);

        PatientManagementUpdate patientUpdate = SpringContext.getBean(PatientManagementUpdate.class);
        samplePatientEntryService.persistData(updateData, patientUpdate, patientInfo, form,
                new MockHttpServletRequest());
    }
}
