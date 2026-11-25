package org.openelisglobal.sample.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.sample.form.SampleTbEntryForm;
import org.openelisglobal.sample.valueholder.OrderPriority;
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
import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for TbSampleServiceImpl verifying persistSampleData,
 * persistSampleHumanData, persistSampleItemData, and persistAnalysisData
 * without relying on a full Spring web context or Docker container.
 *
 * Covers GitHub issue #4850: persistSampleData() instantiates new Sample
 * without copying form.sampleId on update.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class TbSampleServicePersistSampleDataTest {

    @Mock
    private SampleService sampleService;
    @Mock
    private SampleHumanService sampleHumanService;
    @Mock
    private SampleItemService sampleItemService;
    @Mock
    private AnalysisService analysisService;
    @Mock
    private TestService testService;
    @Mock
    private TestSectionService testSectionService;
    @Mock
    private TypeOfSampleService typeOfSampleService;
    @Mock
    private OrganizationService organizationService;
    @Mock
    private SampleOrganizationService sampleOrganizationService;
    @Mock
    private ObservationHistoryService observationHistoryService;
    @Mock
    private ObservationHistoryTypeService observationHistoryTypeService;
    @Mock
    private PersonService personService;
    @Mock
    private ProviderService providerService;
    @Mock
    private MessageSource messageSource;
    @Mock
    private IStatusService statusService;

    @InjectMocks
    private TbSampleServiceImpl tbSampleService;

    @Captor
    private ArgumentCaptor<Sample> sampleCaptor;
    @Captor
    private ArgumentCaptor<SampleHuman> sampleHumanCaptor;
    @Captor
    private ArgumentCaptor<SampleItem> sampleItemCaptor;
    @Captor
    private ArgumentCaptor<SampleOrganization> sampleOrganizationCaptor;
    @Captor
    private ArgumentCaptor<List<Analysis>> analysisListCaptor;

    private AutowireCapableBeanFactory previousSpringFactory;
    private Object previousMessageUtilInstance;

    @Before
    public void setUp() throws Exception {
        AutowireCapableBeanFactory mockFactory = mock(AutowireCapableBeanFactory.class);
        DefaultConfigurationProperties config = mock(DefaultConfigurationProperties.class);
        when(config.getPropertyValue(ConfigurationProperties.Property.AmbiguousDateHolder)).thenReturn("X");
        when(config.getPropertyValue(ConfigurationProperties.Property.DEFAULT_DATE_LOCALE)).thenReturn("en-US");
        when(config.getPropertyValue(ConfigurationProperties.Property.DEFAULT_LANG_LOCALE)).thenReturn("en-US");
        when(config.getPropertyValue(any(ConfigurationProperties.Property.class))).thenReturn("X");
        when(config.getPropertyValue(anyString())).thenReturn("human");
        when(mockFactory.getBean(DefaultConfigurationProperties.class)).thenReturn(config);
        when(mockFactory.getBean(IStatusService.class)).thenReturn(statusService);
        previousSpringFactory = swapSpringFactory(mockFactory);

        previousMessageUtilInstance = ReflectionTestUtils.getField(MessageUtil.class, "instance");
        when(messageSource.getMessage(anyString(), any(), anyString(), any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if ("date.format".equals(key) || "date.format.formatKey".equals(key)) {
                return "MM/dd/yyyy";
            }
            if ("dateTime.format".equals(key) || "timestamp.format.formatKey".equals(key)) {
                return "MM/dd/yyyy HH:mm";
            }
            if ("time.format".equals(key) || "time.format.formatKey".equals(key)) {
                return "HH:mm";
            }
            if ("timestamp.format.formatKey.12".equals(key)) {
                return "MM/dd/yyyy hh:mm a";
            }
            return invocation.getArgument(2);
        });
        MessageUtil.setMessageSource(messageSource);

        when(personService.insert(any(Person.class))).thenReturn("person-1");
        when(providerService.insert(any(Provider.class))).thenReturn("provider-1");

        ReflectionTestUtils.setField(tbSampleService, "statusService", statusService);
    }

    @After
    public void tearDown() throws Exception {
        swapSpringFactory(previousSpringFactory);
        ReflectionTestUtils.setField(MessageUtil.class, "instance", previousMessageUtilInstance);
    }

    private static AutowireCapableBeanFactory swapSpringFactory(AutowireCapableBeanFactory replacement)
            throws Exception {
        Field factoryField = SpringContext.class.getDeclaredField("factory");
        factoryField.setAccessible(true);
        AutowireCapableBeanFactory previous = (AutowireCapableBeanFactory) factoryField.get(null);
        factoryField.set(null, replacement);
        return previous;
    }

    @Test
    public void persistSampleData_NewSample_InsertsAndSetsFields() {
        SampleTbEntryForm form = new SampleTbEntryForm();
        form.setLabNo("LAB-001");
        form.setRequestDate("11/19/2026");
        form.setReceivedDate("11/19/2026");
        form.setSysUserId("1");

        when(statusService.getStatusID(OrderStatus.Entered)).thenReturn("1");
        when(sampleService.insert(any(Sample.class))).thenReturn("101");

        String resultSampleId = tbSampleService.persistSampleData(form);

        assertEquals("101", resultSampleId);
        verify(sampleService).insert(sampleCaptor.capture());
        Sample inserted = sampleCaptor.getValue();
        assertEquals("LAB-001", inserted.getAccessionNumber());
        assertEquals("H", inserted.getDomain());
        assertEquals(OrderPriority.ROUTINE, inserted.getPriority());
        assertEquals("1", inserted.getStatusId());
        assertEquals("1", inserted.getSysUserId());
        verify(sampleService, never()).update(any(Sample.class));
    }

    @Test
    public void persistSampleData_ExistingSample_RetrievesAndUpdatesSample() {
        SampleTbEntryForm form = new SampleTbEntryForm();
        form.setSampleId("202");
        form.setRequestDate("11/19/2026");
        form.setReceivedDate("11/19/2026");
        form.setSysUserId("1");

        Sample existingSample = new Sample();
        existingSample.setId("202");
        existingSample.setAccessionNumber("ACC-EXISTING");
        existingSample.setDomain("H");
        existingSample.setStatusId("5");

        when(sampleService.get("202")).thenReturn(existingSample);
        when(sampleService.update(any(Sample.class))).thenReturn(existingSample);

        String resultSampleId = tbSampleService.persistSampleData(form);

        assertEquals("202", resultSampleId);
        verify(sampleService).get("202");
        verify(sampleService).update(sampleCaptor.capture());
        Sample updated = sampleCaptor.getValue();
        assertEquals("202", updated.getId());
        assertEquals("ACC-EXISTING", updated.getAccessionNumber());
        assertEquals("H", updated.getDomain());
        assertEquals("5", updated.getStatusId());
        assertEquals(OrderPriority.ROUTINE, updated.getPriority());
        verify(sampleService, never()).insert(any(Sample.class));
    }

    @Test(expected = LIMSRuntimeException.class)
    public void persistSampleData_ExistingSample_ThrowsWhenSampleNotFound() {
        SampleTbEntryForm form = new SampleTbEntryForm();
        form.setSampleId("303");
        form.setRequestDate("11/19/2026");
        form.setReceivedDate("11/19/2026");
        form.setSysUserId("1");

        when(sampleService.get("303")).thenReturn(null);

        tbSampleService.persistSampleData(form);
    }

    @Test
    public void persistSampleHumanData_ExistingSample_UpdatesExistingSampleHuman() {
        SampleTbEntryForm form = new SampleTbEntryForm();
        form.setSampleId("202");
        form.setSysUserId("1");

        ReflectionTestUtils.setField(tbSampleService, "sampleId", "202");
        ReflectionTestUtils.setField(tbSampleService, "patientId", "500");

        SampleHuman existingSh = new SampleHuman();
        existingSh.setId("sh-1");
        existingSh.setSampleId("202");

        when(sampleHumanService.getDataBySample(any(SampleHuman.class))).thenReturn(existingSh);
        when(sampleHumanService.update(any(SampleHuman.class))).thenReturn(existingSh);

        String resultId = tbSampleService.persistSampleHumanData(form);

        assertEquals("sh-1", resultId);
        verify(sampleHumanService).getDataBySample(any(SampleHuman.class));
        verify(sampleHumanService).update(existingSh);
        verify(sampleHumanService, never()).insert(any(SampleHuman.class));
        assertEquals("500", existingSh.getPatientId());
        assertEquals("1", existingSh.getSysUserId());
    }

    @Test
    public void persistSampleHumanData_NewSample_InsertsSampleHuman() {
        SampleTbEntryForm form = new SampleTbEntryForm();
        form.setSysUserId("1");

        ReflectionTestUtils.setField(tbSampleService, "sampleId", "101");
        ReflectionTestUtils.setField(tbSampleService, "patientId", "500");

        when(sampleHumanService.insert(any(SampleHuman.class))).thenReturn("sh-new");

        String resultId = tbSampleService.persistSampleHumanData(form);

        assertEquals("sh-new", resultId);
        verify(sampleHumanService).insert(sampleHumanCaptor.capture());
        SampleHuman inserted = sampleHumanCaptor.getValue();
        assertEquals("101", inserted.getSampleId());
        assertEquals("500", inserted.getPatientId());
        assertEquals("1", inserted.getSysUserId());
        verify(sampleHumanService, never()).update(any(SampleHuman.class));
    }

    @Test
    public void persistSampleItemData_ExistingItem_UpdatesSampleItem() {
        SampleTbEntryForm form = new SampleTbEntryForm();
        form.setTbSpecimenNature("spec-1");
        form.setSysUserId("1");

        ReflectionTestUtils.setField(tbSampleService, "sampleId", "202");

        SampleItem existingItem = new SampleItem();
        existingItem.setId("si-1");

        TypeOfSample mockTypeOfSample = new TypeOfSample();
        mockTypeOfSample.setId("spec-1");

        when(sampleItemService.getSampleItemsBySampleId("202")).thenReturn(Collections.singletonList(existingItem));
        when(sampleService.get("202")).thenReturn(new Sample());
        when(typeOfSampleService.get("spec-1")).thenReturn(mockTypeOfSample);
        when(sampleItemService.update(any(SampleItem.class))).thenReturn(existingItem);

        String resultId = tbSampleService.persistSampleItemData(form);

        assertEquals("si-1", resultId);
        verify(sampleItemService).update(existingItem);
        verify(sampleItemService, never()).insert(any(SampleItem.class));
    }

    @Test
    public void persistSampleItemData_NewItem_InsertsSampleItem() {
        SampleTbEntryForm form = new SampleTbEntryForm();
        form.setTbSpecimenNature("spec-1");
        form.setSysUserId("1");

        ReflectionTestUtils.setField(tbSampleService, "sampleId", "101");

        TypeOfSample mockTypeOfSample = new TypeOfSample();
        mockTypeOfSample.setId("spec-1");

        when(sampleItemService.getSampleItemsBySampleId("101")).thenReturn(Collections.emptyList());
        when(sampleService.get("101")).thenReturn(new Sample());
        when(typeOfSampleService.get("spec-1")).thenReturn(mockTypeOfSample);
        when(statusService.getStatusID(SampleStatus.Entered)).thenReturn("status-entered");
        when(sampleItemService.insert(any(SampleItem.class))).thenReturn("si-new");

        String resultId = tbSampleService.persistSampleItemData(form);

        assertEquals("si-new", resultId);
        verify(sampleItemService).insert(sampleItemCaptor.capture());
        SampleItem inserted = sampleItemCaptor.getValue();
        assertEquals("status-entered", inserted.getStatusId());
        assertEquals("1", inserted.getSortOrder());
        verify(sampleItemService, never()).update(any(SampleItem.class));
    }

    @Test
    public void persistAnalysisData_NullSelectedTests_DoesNotThrowNpe() {
        SampleTbEntryForm form = new SampleTbEntryForm();
        form.setNewSelectedTests(null);

        when(analysisService.insertAll(anyList())).thenReturn(Collections.emptyList());

        List<String> result = tbSampleService.persistAnalysisData(form);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(analysisService).insertAll(analysisListCaptor.capture());
        assertTrue(analysisListCaptor.getValue().isEmpty());
    }

    @Test
    public void persistAnalysisData_WithSelectedTests_InsertsAnalyses() {
        SampleTbEntryForm form = new SampleTbEntryForm();
        form.setNewSelectedTests(Arrays.asList("test-1"));
        form.setTbSpecimenNature("spec-1");
        form.setSysUserId("1");

        ReflectionTestUtils.setField(tbSampleService, "sampleItemId", "si-1");

        org.openelisglobal.test.valueholder.Test mockTest = mock(org.openelisglobal.test.valueholder.Test.class);
        when(mockTest.getIsReportable()).thenReturn("Y");

        SampleItem mockSampleItem = new SampleItem();
        mockSampleItem.setId("si-1");

        TestSection mockSection = new TestSection();
        mockSection.setName("TB");

        TypeOfSample mockTypeOfSample = new TypeOfSample();
        mockTypeOfSample.setDescription("Sputum");

        when(testService.get("test-1")).thenReturn(mockTest);
        when(sampleItemService.get("si-1")).thenReturn(mockSampleItem);
        when(testSectionService.getTestSectionByName("TB")).thenReturn(mockSection);
        when(statusService.getStatusID(AnalysisStatus.NotStarted)).thenReturn("status-ns");
        when(typeOfSampleService.get("spec-1")).thenReturn(mockTypeOfSample);
        when(analysisService.insertAll(anyList())).thenReturn(Arrays.asList("analysis-1"));

        List<String> result = tbSampleService.persistAnalysisData(form);

        assertEquals(1, result.size());
        assertEquals("analysis-1", result.get(0));
        verify(analysisService).insertAll(analysisListCaptor.capture());
        List<Analysis> analyses = analysisListCaptor.getValue();
        assertEquals(1, analyses.size());
        Analysis analysis = analyses.get(0);
        assertEquals(mockSampleItem, analysis.getSampleItem());
        assertEquals(mockTest, analysis.getTest());
        assertEquals("0", analysis.getRevision());
        assertEquals(mockSection, analysis.getTestSection());
        assertEquals("Y", analysis.getIsReportable());
        assertEquals("MANUAL", analysis.getAnalysisType());
        assertEquals("status-ns", analysis.getStatusId());
        assertEquals("1", analysis.getSysUserId());
        assertEquals("Sputum", analysis.getSampleTypeName());
        assertNotNull(analysis.getFhirUuid());
    }

    @Test
    public void persistSampleOrganizationData_ExistingSampleWithOrganization_UpdatesSampleOrganization() {
        SampleTbEntryForm form = new SampleTbEntryForm();
        form.setSampleId("202");
        form.setReferringSiteCode("org-1");
        form.setSysUserId("1");

        ReflectionTestUtils.setField(tbSampleService, "sampleId", "202");

        Sample mockSample = new Sample();
        mockSample.setId("202");

        SampleOrganization existingSo = new SampleOrganization();
        existingSo.setId("so-1");

        Organization mockOrg = new Organization();
        mockOrg.setId("org-1");

        when(sampleService.get("202")).thenReturn(mockSample);
        when(sampleOrganizationService.getDataBySample(mockSample)).thenReturn(existingSo);
        when(organizationService.get("org-1")).thenReturn(mockOrg);
        when(sampleOrganizationService.update(any(SampleOrganization.class))).thenReturn(existingSo);

        String resultId = tbSampleService.persistSampleOrganizationData(form);

        assertEquals("so-1", resultId);
        verify(sampleOrganizationService).update(existingSo);
        verify(sampleOrganizationService, never()).insert(any(SampleOrganization.class));
        assertEquals(mockSample, existingSo.getSample());
        assertEquals(mockOrg, existingSo.getOrganization());
        assertEquals("1", existingSo.getSysUserId());
    }

    @Test
    public void persistSampleOrganizationData_ExistingSampleWithoutOrganization_InsertsSampleOrganization() {
        SampleTbEntryForm form = new SampleTbEntryForm();
        form.setSampleId("202");
        form.setReferringSiteCode("org-1");
        form.setSysUserId("1");

        ReflectionTestUtils.setField(tbSampleService, "sampleId", "202");

        Sample mockSample = new Sample();
        mockSample.setId("202");

        Organization mockOrg = new Organization();
        mockOrg.setId("org-1");

        when(sampleService.get("202")).thenReturn(mockSample);
        when(sampleOrganizationService.getDataBySample(mockSample)).thenReturn(null);
        when(organizationService.get("org-1")).thenReturn(mockOrg);
        when(sampleOrganizationService.insert(any(SampleOrganization.class))).thenReturn("so-new");

        String resultId = tbSampleService.persistSampleOrganizationData(form);

        assertEquals("so-new", resultId);
        verify(sampleOrganizationService).insert(sampleOrganizationCaptor.capture());
        verify(sampleOrganizationService, never()).update(any(SampleOrganization.class));
        SampleOrganization inserted = sampleOrganizationCaptor.getValue();
        assertEquals(mockSample, inserted.getSample());
        assertEquals(mockOrg, inserted.getOrganization());
        assertEquals("1", inserted.getSysUserId());
        assertNotNull(inserted.getLastupdated());
    }

    @Test
    public void persistSampleOrganizationData_NewSample_InsertsSampleOrganization() {
        SampleTbEntryForm form = new SampleTbEntryForm();
        form.setSampleId(null);
        form.setReferringSiteCode("org-1");
        form.setSysUserId("1");

        ReflectionTestUtils.setField(tbSampleService, "sampleId", "101");

        Sample mockSample = new Sample();
        mockSample.setId("101");

        Organization mockOrg = new Organization();
        mockOrg.setId("org-1");

        when(sampleService.get("101")).thenReturn(mockSample);
        when(organizationService.get("org-1")).thenReturn(mockOrg);
        when(sampleOrganizationService.insert(any(SampleOrganization.class))).thenReturn("so-new");

        String resultId = tbSampleService.persistSampleOrganizationData(form);

        assertEquals("so-new", resultId);
        verify(sampleOrganizationService).insert(sampleOrganizationCaptor.capture());
        verify(sampleOrganizationService, never()).update(any(SampleOrganization.class));
        SampleOrganization inserted = sampleOrganizationCaptor.getValue();
        assertEquals(mockSample, inserted.getSample());
        assertEquals(mockOrg, inserted.getOrganization());
        assertEquals("1", inserted.getSysUserId());
    }

    @Test
    public void persistObservations_ExistingObservations_UpdatesObservations() {
        SampleTbEntryForm form = new SampleTbEntryForm();
        form.setTbOrderReason("Reason1");
        form.setTbDiagnosticReason("Diag1");
        form.setTbFollowupReason("Follow1");
        form.setTbAspect("Aspect1");
        form.setTbFollowupPeriodLine1("P1");
        form.setTbFollowupPeriodLine2("P2");
        form.setSelectedTbMethod("M1");
        form.setSysUserId("1");

        ReflectionTestUtils.setField(tbSampleService, "sampleId", "101");
        ReflectionTestUtils.setField(tbSampleService, "patientId", "500");

        ObservationHistoryType oht = new ObservationHistoryType();
        oht.setId("oht-1");
        when(observationHistoryTypeService.getByName(anyString())).thenReturn(oht);

        ObservationHistory existingObs = new ObservationHistory();
        existingObs.setId("obs-1");
        existingObs.setObservationHistoryTypeId("oht-1");

        when(observationHistoryService.getObservationHistoriesBySampleId("101"))
                .thenReturn(Collections.singletonList(existingObs));
        when(observationHistoryService.update(any(ObservationHistory.class))).thenAnswer(i -> i.getArgument(0));

        List<String> result = tbSampleService.persistObservations(form);

        assertEquals(7, result.size());
        verify(observationHistoryService, atLeastOnce()).update(existingObs);
        assertEquals("500", existingObs.getPatientId());
        assertEquals("1", existingObs.getSysUserId());
    }

    @Test
    public void persistObservations_NewObservations_InsertsObservations() {
        SampleTbEntryForm form = new SampleTbEntryForm();
        form.setTbOrderReason("Reason1");
        form.setTbDiagnosticReason("Diag1");
        form.setTbFollowupReason("Follow1");
        form.setTbAspect("Aspect1");
        form.setTbFollowupPeriodLine1("P1");
        form.setTbFollowupPeriodLine2("P2");
        form.setSelectedTbMethod("M1");
        form.setSysUserId("1");

        ReflectionTestUtils.setField(tbSampleService, "sampleId", "101");
        ReflectionTestUtils.setField(tbSampleService, "patientId", "500");

        ObservationHistoryType oht = new ObservationHistoryType();
        oht.setId("oht-1");
        when(observationHistoryTypeService.getByName(anyString())).thenReturn(oht);

        when(observationHistoryService.getObservationHistoriesBySampleId("101")).thenReturn(Collections.emptyList());
        when(observationHistoryService.insert(any(ObservationHistory.class))).thenReturn("obs-new");

        List<String> result = tbSampleService.persistObservations(form);

        assertEquals(7, result.size());
        verify(observationHistoryService, times(7)).insert(any(ObservationHistory.class));
        verify(observationHistoryService, never()).update(any(ObservationHistory.class));
    }
}
