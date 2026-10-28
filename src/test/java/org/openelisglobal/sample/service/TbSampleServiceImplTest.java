package org.openelisglobal.sample.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.service.PersonAddressService;
import org.openelisglobal.address.valueholder.AddressPart;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentitytype.service.PatientIdentityTypeService;
import org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap;
import org.openelisglobal.patientidentitytype.valueholder.PatientIdentityType;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.sample.form.SampleTbEntryForm;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.openelisglobal.sampleorganization.valueholder.SampleOrganization;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class TbSampleServiceImplTest {

    private static AutowireCapableBeanFactory savedFactory;

    @Mock
    private PatientService patientService;
    @Mock
    private SampleService sampleService;
    @Mock
    private SampleHumanService sampleHumanService;
    @Mock
    private SampleItemService sampleItemService;
    @Mock
    private AnalysisService analysisService;
    @Mock
    private SampleOrganizationService sampleOrganizationService;
    @Mock
    private ObservationHistoryService observationHistoryService;
    @Mock
    private ObservationHistoryTypeService observationHistoryTypeService;
    @Mock
    private PersonService personService;
    @Mock
    private PersonAddressService personAddressService;
    @Mock
    private AddressPartService addressPartService;
    @Mock
    private PatientIdentityService patientIdentityService;
    @Mock
    private TestService testService;
    @Mock
    private TestSectionService testSectionService;
    @Mock
    private TypeOfSampleService typeOfSampleService;
    @Mock
    private ProviderService providerService;
    @Mock
    private OrganizationService organizationService;

    private TbSampleServiceImpl service;

    @BeforeClass
    public static void setUpSpringContext() throws Exception {
        IStatusService statusService = mock(IStatusService.class);
        when(statusService.getStatusID(any())).thenReturn("1");

        PatientIdentityTypeService identityTypeService = mock(PatientIdentityTypeService.class);
        PatientIdentityType subjectType = new PatientIdentityType();
        subjectType.setId("type-subject");
        subjectType.setIdentityType("SUBJECT");
        when(identityTypeService.getAllPatientIdenityTypes()).thenReturn(List.of(subjectType));

        AutowireCapableBeanFactory stubFactory = mock(AutowireCapableBeanFactory.class);
        when(stubFactory.getBean(IStatusService.class)).thenReturn(statusService);
        when(stubFactory.getBean(PatientIdentityTypeService.class)).thenReturn(identityTypeService);

        Field factoryField = SpringContext.class.getDeclaredField("factory");
        factoryField.setAccessible(true);
        savedFactory = (AutowireCapableBeanFactory) factoryField.get(null);
        factoryField.set(null, stubFactory);
    }

    @AfterClass
    public static void tearDownSpringContext() throws Exception {
        Field factoryField = SpringContext.class.getDeclaredField("factory");
        factoryField.setAccessible(true);
        factoryField.set(null, savedFactory);
    }

    @Before
    public void setUp() {
        PatientIdentityTypeMap.reset();

        service = new TbSampleServiceImpl();
        ReflectionTestUtils.setField(service, "patientService", patientService);
        ReflectionTestUtils.setField(service, "sampleService", sampleService);
        ReflectionTestUtils.setField(service, "sampleHumanService", sampleHumanService);
        ReflectionTestUtils.setField(service, "sampleItemService", sampleItemService);
        ReflectionTestUtils.setField(service, "analysisService", analysisService);
        ReflectionTestUtils.setField(service, "sampleOrganizationService", sampleOrganizationService);
        ReflectionTestUtils.setField(service, "observationHistoryService", observationHistoryService);
        ReflectionTestUtils.setField(service, "observationHistoryTypeService", observationHistoryTypeService);
        ReflectionTestUtils.setField(service, "personService", personService);
        ReflectionTestUtils.setField(service, "personAddressService", personAddressService);
        ReflectionTestUtils.setField(service, "addressPartService", addressPartService);
        ReflectionTestUtils.setField(service, "patientIdentityService", patientIdentityService);
        ReflectionTestUtils.setField(service, "testService", testService);
        ReflectionTestUtils.setField(service, "testSectionService", testSectionService);
        ReflectionTestUtils.setField(service, "typeOfSampleService", typeOfSampleService);
        ReflectionTestUtils.setField(service, "providerService", providerService);
        ReflectionTestUtils.setField(service, "organizationService", organizationService);

        AddressPart phonePart = new AddressPart();
        phonePart.setId("part-phone");
        AddressPart streetPart = new AddressPart();
        streetPart.setId("part-street");
        when(addressPartService.getAddresPartByName("phone")).thenReturn(phonePart);
        when(addressPartService.getAddresPartByName("street")).thenReturn(streetPart);

        TypeOfSample sampleType = new TypeOfSample();
        sampleType.setId("sample-type-1");
        sampleType.setDescription("Sputum");
        when(typeOfSampleService.get(any())).thenReturn(sampleType);

        ObservationHistoryType oht = new ObservationHistoryType();
        oht.setId("oht-1");
        when(observationHistoryTypeService.getByName(any())).thenReturn(oht);

        TestSection testSection = new TestSection();
        testSection.setId("sec-tb");
        when(testSectionService.getTestSectionByName("TB")).thenReturn(testSection);

        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId("test-1");
        test.setIsReportable("Y");
        when(testService.get(any())).thenReturn(test);

        Organization org = new Organization();
        org.setId("org-1");
        when(organizationService.get(any())).thenReturn(org);

        when(personService.insert(any(Person.class))).thenReturn("person-1");
        when(providerService.insert(any(Provider.class))).thenReturn("provider-1");
        when(patientIdentityService.getPatitentIdentityForPatientAndType(any(), any())).thenReturn(null);
        when(patientIdentityService.insert(any())).thenReturn("pid-1");
    }

    @After
    public void tearDown() {
        PatientIdentityTypeMap.reset();
    }

    private SampleTbEntryForm createForm(String subjectNumber, String labNo, String testId) {
        SampleTbEntryForm form = new SampleTbEntryForm();
        form.setTbSubjectNumber(subjectNumber);
        form.setLabNo(labNo);
        form.setPatientFirstName("Jane");
        form.setPatientLastName("Doe");
        form.setPatientBirthDate("1990-01-01");
        form.setPatientGender("F");
        form.setPatientPhone("555-1234");
        form.setPatientAddress("123 Health St");
        form.setProviderFirstName("Dr");
        form.setProviderLastName("Smith");
        form.setRequestDate("2026-10-01");
        form.setReceivedDate("2026-10-02");
        form.setTbSpecimenNature("sputum-nature");
        form.setNewSelectedTests(List.of(testId));
        form.setReferringSiteCode("site-01");
        form.setSysUserId("user-1");
        form.setTbOrderReason("reason-diag");
        form.setTbDiagnosticReason("diag-presumptive");
        form.setTbFollowupReason("follow-none");
        form.setTbAspect("aspect-purulent");
        form.setTbFollowupPeriodLine1("period-1");
        form.setTbFollowupPeriodLine2("period-2");
        form.setSelectedTbMethod("method-smear");
        return form;
    }

    @Test
    public void persistTbData_passesCorrectIdsAcrossAllEntities() {
        when(patientService.insert(any(Patient.class))).thenReturn("patient-100");
        when(sampleService.insert(any(Sample.class))).thenReturn("sample-200");
        when(sampleItemService.insert(any(SampleItem.class))).thenReturn("sample-item-300");

        Sample sample = new Sample();
        sample.setId("sample-200");
        when(sampleService.get("sample-200")).thenReturn(sample);

        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("sample-item-300");
        when(sampleItemService.get("sample-item-300")).thenReturn(sampleItem);

        SampleTbEntryForm form = createForm("SUBJ-001", "LAB-001", "test-1");
        HttpServletRequest request = mock(HttpServletRequest.class);

        boolean result = service.persistTbData(form, request);

        assertTrue("persistTbData should succeed", result);

        // Verify SampleHuman received correct sampleId and patientId
        ArgumentCaptor<SampleHuman> sampleHumanCaptor = ArgumentCaptor.forClass(SampleHuman.class);
        verify(sampleHumanService).insert(sampleHumanCaptor.capture());
        assertEquals("sample-200", sampleHumanCaptor.getValue().getSampleId());
        assertEquals("patient-100", sampleHumanCaptor.getValue().getPatientId());

        // Verify SampleItem received correct sample reference
        ArgumentCaptor<SampleItem> sampleItemCaptor = ArgumentCaptor.forClass(SampleItem.class);
        verify(sampleItemService).insert(sampleItemCaptor.capture());
        assertEquals(sample, sampleItemCaptor.getValue().getSample());

        // Verify Analysis received correct sampleItem reference
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Analysis>> analysisCaptor = ArgumentCaptor.forClass(List.class);
        verify(analysisService).insertAll(analysisCaptor.capture());
        List<Analysis> analyses = analysisCaptor.getValue();
        assertEquals(1, analyses.size());
        assertEquals(sampleItem, analyses.get(0).getSampleItem());

        // Verify ObservationHistory items all received correct sampleId and patientId
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ObservationHistory>> obsCaptor = ArgumentCaptor.forClass(List.class);
        verify(observationHistoryService).insertAll(obsCaptor.capture());
        List<ObservationHistory> observations = obsCaptor.getValue();
        assertEquals(7, observations.size());
        for (ObservationHistory obs : observations) {
            assertEquals("sample-200", obs.getSampleId());
            assertEquals("patient-100", obs.getPatientId());
        }
    }

    @Test
    public void persistTbData_concurrentExecution_isolatesStateBetweenThreads() throws Exception {
        Sample sampleA = new Sample();
        sampleA.setId("sample-A");
        Sample sampleB = new Sample();
        sampleB.setId("sample-B");
        when(sampleService.get("sample-A")).thenReturn(sampleA);
        when(sampleService.get("sample-B")).thenReturn(sampleB);

        SampleItem itemA = new SampleItem();
        itemA.setId("item-A");
        SampleItem itemB = new SampleItem();
        itemB.setId("item-B");
        when(sampleItemService.get("item-A")).thenReturn(itemA);
        when(sampleItemService.get("item-B")).thenReturn(itemB);

        CountDownLatch thread1HoldLatch = new CountDownLatch(1);
        CountDownLatch thread2StartedLatch = new CountDownLatch(1);

        when(patientService.insert(any(Patient.class))).thenAnswer(invocation -> {
            Patient p = invocation.getArgument(0);
            if ("SUBJ-A".equals(p.getExternalId())) {
                thread2StartedLatch.countDown();
                // Pause thread 1 so thread 2 can run and interleave
                thread1HoldLatch.await(2, TimeUnit.SECONDS);
                return "patient-A";
            } else {
                return "patient-B";
            }
        });

        when(sampleService.insert(any(Sample.class))).thenAnswer(invocation -> {
            Sample s = invocation.getArgument(0);
            return "LAB-A".equals(s.getAccessionNumber()) ? "sample-A" : "sample-B";
        });

        when(sampleItemService.insert(any(SampleItem.class))).thenAnswer(invocation -> {
            SampleItem si = invocation.getArgument(0);
            return si.getSample() == sampleA ? "item-A" : "item-B";
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> task1 = () -> {
                SampleTbEntryForm formA = createForm("SUBJ-A", "LAB-A", "test-1");
                return service.persistTbData(formA, mock(HttpServletRequest.class));
            };

            Callable<Boolean> task2 = () -> {
                thread2StartedLatch.await(2, TimeUnit.SECONDS);
                SampleTbEntryForm formB = createForm("SUBJ-B", "LAB-B", "test-1");
                boolean res = service.persistTbData(formB, mock(HttpServletRequest.class));
                thread1HoldLatch.countDown();
                return res;
            };

            Future<Boolean> future1 = executor.submit(task1);
            Future<Boolean> future2 = executor.submit(task2);

            assertTrue("Thread 1 should succeed", future1.get(5, TimeUnit.SECONDS));
            assertTrue("Thread 2 should succeed", future2.get(5, TimeUnit.SECONDS));

            // Verify SampleHuman records: each must strictly pair its own patient and sample
            ArgumentCaptor<SampleHuman> humanCaptor = ArgumentCaptor.forClass(SampleHuman.class);
            verify(sampleHumanService, atLeastOnce()).insert(humanCaptor.capture());
            for (SampleHuman sh : humanCaptor.getAllValues()) {
                if ("sample-A".equals(sh.getSampleId())) {
                    assertEquals("Thread 1 sample-A must only be linked to patient-A", "patient-A", sh.getPatientId());
                } else if ("sample-B".equals(sh.getSampleId())) {
                    assertEquals("Thread 2 sample-B must only be linked to patient-B", "patient-B", sh.getPatientId());
                }
            }

            // Verify ObservationHistory records: each observation must have matching patient and sample
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<ObservationHistory>> obsCaptor = ArgumentCaptor.forClass(List.class);
            verify(observationHistoryService, atLeastOnce()).insertAll(obsCaptor.capture());
            for (List<ObservationHistory> list : obsCaptor.getAllValues()) {
                for (ObservationHistory obs : list) {
                    if ("sample-A".equals(obs.getSampleId())) {
                        assertEquals("Thread 1 observation for sample-A must have patient-A", "patient-A", obs.getPatientId());
                    } else if ("sample-B".equals(obs.getSampleId())) {
                        assertEquals("Thread 2 observation for sample-B must have patient-B", "patient-B", obs.getPatientId());
                    }
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
