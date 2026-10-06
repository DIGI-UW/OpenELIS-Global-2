package org.openelisglobal.sample.controller.rest;

import static org.junit.Assert.assertEquals;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.services.SampleAddService;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.service.PatientManagementUpdate;
import org.openelisglobal.sample.service.SamplePatientEntryService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.dto.TestSelectionDTO;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-back lists only panels the order was placed under (OGC-1268): a
 * panel-member test picked alone reads back with no panel.
 */
@Transactional
public class OrderSearchPanelReadbackIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MicrobiologyTestFixtures fixtures;

    @Autowired
    private SamplePatientEntryService samplePatientEntryService;

    @Autowired
    private PanelService panelService;

    @Autowired
    private PanelItemService panelItemService;

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private TestService testService;

    @Autowired
    private OrderSearchRestController orderSearchRestController;

    private String userId;
    private Patient patient;
    private TypeOfSample sampleType;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        userId = fixtures.defaultUserId();
        patient = fixtures.createPatient("PNLRB");
        sampleType = fixtures.createTypeOfSample();
    }

    @Test
    public void aPanelMemberTestOrderedAloneReadsBackWithNoPanels() {
        org.openelisglobal.test.valueholder.Test test = catalogTest();
        panelContaining(test);
        Sample sample = newSample();
        persist(sample, samplesXml(sampleXml(test, "")));

        Map<String, Object> sampleData = onlySample(search(sample.getAccessionNumber()));

        @SuppressWarnings("unchecked")
        List<TestSelectionDTO> tests = (List<TestSelectionDTO>) sampleData.get("tests");
        assertEquals(1, tests.size());
        assertEquals(test.getId(), tests.get(0).getId());
        assertEquals("a panel that was never ordered must not be inferred from panel membership", List.of(),
                sampleData.get("panels"));
    }

    @Test
    public void anOrderPlacedThroughAPanelReadsBackExactlyThatPanel() {
        org.openelisglobal.test.valueholder.Test test = catalogTest();
        Panel panel = panelContaining(test);
        Sample sample = newSample();
        persist(sample, samplesXml(sampleXml(test, panel.getId())));

        Map<String, Object> sampleData = onlySample(search(sample.getAccessionNumber()));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> panels = (List<Map<String, Object>>) sampleData.get("panels");
        assertEquals(1, panels.size());
        assertEquals(panel.getId(), panels.get(0).get("id"));
        assertEquals(panel.getPanelName(), panels.get(0).get("name"));
        assertEquals(test.getId(), panels.get(0).get("testIds"));
    }

    private Map<String, Object> search(String labNumber) {
        ResponseEntity<Map<String, Object>> response = orderSearchRestController.searchOrder(labNumber);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> onlySample(Map<String, Object> body) {
        List<Map<String, Object>> samples = (List<Map<String, Object>>) body.get("samples");
        assertEquals(1, samples.size());
        return samples.get(0);
    }

    private org.openelisglobal.test.valueholder.Test catalogTest() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setName("PanelReadbackIT " + suffix);
        test.setDescription("PanelReadbackIT " + suffix);
        test.setIsActive("Y");
        test.setGuid(UUID.randomUUID().toString());
        test.setDomain("CLINICAL");
        test.setOrderable(true);
        test.setSysUserId(userId);
        testService.insert(test);
        return test;
    }

    private Panel panelContaining(org.openelisglobal.test.valueholder.Test test) {
        String name = "Readback panel " + UUID.randomUUID().toString().substring(0, 8);
        Localization localization = new Localization();
        localization.setDescription("Panel read-back test panel");
        localization.setEnglish(name);
        localization.setFrench(name);
        localization.setSysUserId(userId);
        localization.setId(localizationService.insert(localization));

        Panel panel = new Panel();
        panel.setPanelName(name);
        panel.setLocalization(localization);
        panel.setDescription("Panel read-back test panel");
        panel.setIsActive("Y");
        panel.setSysUserId(userId);
        panel.setId(panelService.insert(panel));

        PanelItem item = new PanelItem();
        item.setPanel(panel);
        item.setTest(test);
        item.setSortOrder("1");
        item.setSysUserId(userId);
        panelItemService.insert(item);
        return panel;
    }

    private String samplesXml(String... samples) {
        return "<samples>" + String.join("", samples) + "</samples>";
    }

    private String sampleXml(org.openelisglobal.test.valueholder.Test test, String panelIds) {
        return "<sample sampleID='1' typeId='" + sampleType.getId() + "' sampleItemId='' clientKey='' tests='"
                + test.getId() + "' panels='" + panelIds
                + "' testSectionMap='' testSampleTypeMap='' rejected='false'/>";
    }

    private Sample newSample() {
        Sample sample = new Sample();
        sample.setAccessionNumber("PRB" + UUID.randomUUID().toString().replace("-", "").substring(0, 9));
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
