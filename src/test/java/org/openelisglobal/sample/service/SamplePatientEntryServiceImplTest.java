package org.openelisglobal.sample.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.barcode.service.BarcodeInfoService;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
import org.openelisglobal.microbiology.service.MicroCaseOrderDetailService;
import org.openelisglobal.organization.service.OrganizationContactService;
import org.openelisglobal.organization.valueholder.OrganizationContact;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.requester.service.SampleRequesterService;
import org.openelisglobal.requester.valueholder.SampleRequester;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class SamplePatientEntryServiceImplTest {

    @Mock
    private BarcodeInfoService barcodeInfoService;
    @Mock
    private PersonService personService;
    @Mock
    private SampleRequesterService sampleRequesterService;
    @Mock
    private OrganizationContactService organizationContactService;

    @Mock
    private MicroCaseOrderDetailService microCaseOrderDetailService;

    private SamplePatientEntryServiceImpl service;

    // TableIdService.INSTANCE is a process-wide static field, not scoped to a
    // Spring context — overwriting it here without restoring leaks into every
    // other test class that shares this Surefire fork. Confirmed live (2026-07-08):
    // this leaked a bare TableIdService (ORGANIZATION_REQUESTER_TYPE_ID left at
    // the Java default 0, since this test never sets it) that permanently
    // replaced the real Spring-initialized singleton, causing
    // ServiceRequestFacadeTest's createServiceRequest_* tests to fail with
    // "insert or update on table sample_requester violates foreign key
    // constraint requester_type_fk ... Key (requester_type_id)=(0)" whenever
    // this test class ran earlier in the same fork.
    private TableIdService originalTableIdServiceInstance;

    @Before
    public void setUp() {
        service = new SamplePatientEntryServiceImpl();
        ReflectionTestUtils.setField(service, "barcodeInfoService", barcodeInfoService);
        ReflectionTestUtils.setField(service, "microCaseOrderDetailService", microCaseOrderDetailService);
        ReflectionTestUtils.setField(service, "personService", personService);
        ReflectionTestUtils.setField(service, "sampleRequesterService", sampleRequesterService);
        ReflectionTestUtils.setField(service, "organizationContactService", organizationContactService);

        // TableIdService.getInstance() is a Spring-managed singleton normally
        // populated by @PostConstruct; this is a pure Mockito test with no
        // Spring context, so it would otherwise be null (NPE on any
        // REQUESTOR_CONTACT_REQUESTER_TYPE_ID/etc. access) unless another
        // test class in the same JVM happened to initialize it first — which
        // made this file order-dependent rather than actually passing on its
        // own. Wire a real instance with the one id persistRequesterData
        // reads directly, saving whatever was there before so it can be
        // restored afterward.
        originalTableIdServiceInstance = TableIdService.getInstance();
        TableIdService tableIdService = new TableIdService();
        tableIdService.REQUESTOR_CONTACT_REQUESTER_TYPE_ID = 4L;
        tableIdService.PROVIDER_REQUESTER_TYPE_ID = 2L;
        ReflectionTestUtils.setField(tableIdService, "INSTANCE", tableIdService);
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(new TableIdService(), "INSTANCE", originalTableIdServiceInstance);
    }

    /**
     * A Requestor contact bound to an order (no Requesting Organization present)
     * must be saved as a standalone SampleRequester row and must NOT attempt any
     * OrganizationContact linking, since there is no organization to link it to.
     */
    @Test
    public void persistRequesterData_requestorOnly_savesRequesterContactWithoutOrgLink() {
        Sample sample = new Sample();
        sample.setId("100");

        Person requestorPerson = new Person();
        requestorPerson.setId("55");

        SampleRequester requesterContact = new SampleRequester();
        requesterContact.setRequesterTypeId(3L);

        SamplePatientUpdateData updateData = mock(SamplePatientUpdateData.class);
        when(updateData.getSample()).thenReturn(sample);
        when(updateData.getRequestorPerson()).thenReturn(requestorPerson);
        when(updateData.getRequesterContact()).thenReturn(requesterContact);
        // No existing requestor_contact row for this sample yet -> insert path.
        when(sampleRequesterService.getRequestersForSampleId("100")).thenReturn(Collections.emptyList());

        service.persistRequesterData(updateData);

        ArgumentCaptor<SampleRequester> captor = ArgumentCaptor.forClass(SampleRequester.class);
        verify(sampleRequesterService).insert(captor.capture());
        verify(sampleRequesterService, never()).update(any());
        org.junit.Assert.assertEquals(55L, captor.getValue().getRequesterId());
        org.junit.Assert.assertEquals(100L, captor.getValue().getSampleId());
        verify(organizationContactService, never()).insert(any());
    }

    /**
     * Bug (2026-07-06): editing an already-selected Requestor's info and saving
     * silently re-persisted the unchanged original AND duplicated the
     * sample_requester link row on every save (6 duplicates observed for one real
     * sample). This covers the second half — persistRequesterData must reuse the
     * existing requestor_contact row for this sample, not insert a new one, when
     * one already exists.
     */
    @Test
    public void persistRequesterData_requestorAlreadyLinkedToSample_updatesExistingRowInsteadOfInserting() {
        Sample sample = new Sample();
        sample.setId("100");

        Person requestorPerson = new Person();
        requestorPerson.setId("55");

        SampleRequester newRequesterContact = new SampleRequester();
        newRequesterContact.setRequesterTypeId(4L);

        SampleRequester existingRow = new SampleRequester();
        existingRow.setId("9");
        existingRow.setSampleId(100L);
        existingRow.setRequesterId(55L);
        existingRow.setRequesterTypeId(4L);

        SamplePatientUpdateData updateData = mock(SamplePatientUpdateData.class);
        when(updateData.getSample()).thenReturn(sample);
        when(updateData.getRequestorPerson()).thenReturn(requestorPerson);
        when(updateData.getRequesterContact()).thenReturn(newRequesterContact);
        when(updateData.getCurrentUserId()).thenReturn("user-1");
        when(sampleRequesterService.getRequestersForSampleId("100")).thenReturn(List.of(existingRow));

        service.persistRequesterData(updateData);

        verify(sampleRequesterService, never()).insert(any());
        ArgumentCaptor<SampleRequester> captor = ArgumentCaptor.forClass(SampleRequester.class);
        verify(sampleRequesterService).update(captor.capture());
        org.junit.Assert.assertEquals("9", captor.getValue().getId());
        org.junit.Assert.assertEquals(55L, captor.getValue().getRequesterId());
    }

    /**
     * Same bug class as the Requestor one above, confirmed present in the parallel
     * Provider code path (Clinical workflow) — persistRequesterData must insert a
     * new provider-typed SampleRequester row only when this sample has none yet.
     */
    @Test
    public void persistRequesterData_providerOnly_insertsWhenNoExistingRow() {
        Sample sample = new Sample();
        sample.setId("200");

        Person providerPerson = new Person();
        providerPerson.setId("77");

        SamplePatientUpdateData updateData = mock(SamplePatientUpdateData.class);
        when(updateData.getSample()).thenReturn(sample);
        when(updateData.getProviderPerson()).thenReturn(providerPerson);
        when(updateData.getCurrentUserId()).thenReturn("user-1");
        when(sampleRequesterService.getRequestersForSampleId("200")).thenReturn(Collections.emptyList());

        service.persistRequesterData(updateData);

        ArgumentCaptor<SampleRequester> captor = ArgumentCaptor.forClass(SampleRequester.class);
        verify(sampleRequesterService).insert(captor.capture());
        verify(sampleRequesterService, never()).update(any());
        org.junit.Assert.assertEquals(77L, captor.getValue().getRequesterId());
        org.junit.Assert.assertEquals(200L, captor.getValue().getSampleId());
        org.junit.Assert.assertEquals(2L, captor.getValue().getRequesterTypeId());
    }

    /**
     * Bug (2026-07-06): same as the Requestor duplicate-insert bug, confirmed
     * present in the parallel Provider path — re-saving a Clinical order with an
     * already-linked Provider must update the existing provider-typed
     * SampleRequester row, not insert a duplicate.
     */
    @Test
    public void persistRequesterData_providerAlreadyLinkedToSample_updatesExistingRowInsteadOfInserting() {
        Sample sample = new Sample();
        sample.setId("200");

        Person providerPerson = new Person();
        providerPerson.setId("77");

        SampleRequester existingRow = new SampleRequester();
        existingRow.setId("15");
        existingRow.setSampleId(200L);
        existingRow.setRequesterId(77L);
        existingRow.setRequesterTypeId(2L);

        SamplePatientUpdateData updateData = mock(SamplePatientUpdateData.class);
        when(updateData.getSample()).thenReturn(sample);
        when(updateData.getProviderPerson()).thenReturn(providerPerson);
        when(updateData.getCurrentUserId()).thenReturn("user-1");
        when(sampleRequesterService.getRequestersForSampleId("200")).thenReturn(List.of(existingRow));

        service.persistRequesterData(updateData);

        verify(sampleRequesterService, never()).insert(any());
        ArgumentCaptor<SampleRequester> captor = ArgumentCaptor.forClass(SampleRequester.class);
        verify(sampleRequesterService).update(captor.capture());
        org.junit.Assert.assertEquals("15", captor.getValue().getId());
        org.junit.Assert.assertEquals(77L, captor.getValue().getRequesterId());
    }

    /**
     * When both a Requesting Organization and a Requestor contact are bound on the
     * same order, an OrganizationContact link must be created for future "suggested
     * contacts for this org" reuse.
     */
    @Test
    public void persistRequesterData_orgAndRequestorBoth_linksOrganizationContact() {
        Sample sample = new Sample();
        sample.setId("100");

        Person requestorPerson = new Person();
        requestorPerson.setId("55");

        SampleRequester requesterContact = new SampleRequester();
        requesterContact.setRequesterTypeId(3L);

        SampleRequester requesterSite = new SampleRequester();
        requesterSite.setRequesterId("42");

        SamplePatientUpdateData updateData = mock(SamplePatientUpdateData.class);
        when(updateData.getSample()).thenReturn(sample);
        when(updateData.getRequestorPerson()).thenReturn(requestorPerson);
        when(updateData.getRequesterContact()).thenReturn(requesterContact);
        when(updateData.getRequesterSite()).thenReturn(requesterSite);
        when(updateData.getCurrentUserId()).thenReturn("user-1");

        when(sampleRequesterService.getRequestersForSampleId("100")).thenReturn(Collections.emptyList());
        when(organizationContactService.getListForOrganizationId("42")).thenReturn(Collections.emptyList());
        // linkOrganizationContactIfNeeded loads the managed Person via
        // personService.get(id) rather than fabricating a transient one
        // (Hibernate rejects cascading a bare `new Person()` with just an id).
        when(personService.get("55")).thenReturn(requestorPerson);

        service.persistRequesterData(updateData);

        ArgumentCaptor<OrganizationContact> captor = ArgumentCaptor.forClass(OrganizationContact.class);
        verify(organizationContactService).insert(captor.capture());
        org.junit.Assert.assertEquals("42", captor.getValue().getOrganizationId());
        org.junit.Assert.assertEquals("55", captor.getValue().getPerson().getId());
    }

    /**
     * The OrganizationContact link must be idempotent — if this org+person pairing
     * was already linked (e.g. a prior order for the same organization and
     * contact), do not insert a duplicate row.
     */
    @Test
    public void persistRequesterData_orgAndRequestorAlreadyLinked_doesNotDuplicateLink() {
        Sample sample = new Sample();
        sample.setId("100");

        Person requestorPerson = new Person();
        requestorPerson.setId("55");

        SampleRequester requesterContact = new SampleRequester();
        requesterContact.setRequesterTypeId(3L);

        SampleRequester requesterSite = new SampleRequester();
        requesterSite.setRequesterId("42");

        SamplePatientUpdateData updateData = mock(SamplePatientUpdateData.class);
        when(updateData.getSample()).thenReturn(sample);
        when(updateData.getRequestorPerson()).thenReturn(requestorPerson);
        when(updateData.getRequesterContact()).thenReturn(requesterContact);
        when(updateData.getRequesterSite()).thenReturn(requesterSite);
        when(updateData.getCurrentUserId()).thenReturn("user-1");

        when(sampleRequesterService.getRequestersForSampleId("100")).thenReturn(Collections.emptyList());
        Person existingLinkedPerson = new Person();
        existingLinkedPerson.setId("55");
        OrganizationContact existingContact = new OrganizationContact();
        existingContact.setOrganizationId("42");
        existingContact.setPerson(existingLinkedPerson);
        when(organizationContactService.getListForOrganizationId("42")).thenReturn(List.of(existingContact));

        service.persistRequesterData(updateData);

        verify(organizationContactService, never()).insert(any());
    }

    @Test
    public void persistOrderSpecimenBarcodeCounts_persistsProvidedValues() {
        Sample sample = new Sample();
        sample.setId("sample-1");
        SampleItem firstItem = new SampleItem();
        firstItem.setId("item-1");
        SampleItem secondItem = new SampleItem();
        secondItem.setId("item-2");
        Map<SampleItem, Integer> specimenLabelQuantities = new LinkedHashMap<>();
        specimenLabelQuantities.put(firstItem, 4);
        specimenLabelQuantities.put(secondItem, 3);

        service.persistOrderSpecimenBarcodeCounts(sample, 4, specimenLabelQuantities);

        ArgumentCaptor<Map<SampleItem, Integer>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(barcodeInfoService).saveBarcodeInfoForSampleAndSampleItems(eq(sample), eq(4), mapCaptor.capture());
        org.junit.Assert.assertEquals(Integer.valueOf(4), mapCaptor.getValue().get(firstItem));
        org.junit.Assert.assertEquals(Integer.valueOf(3), mapCaptor.getValue().get(secondItem));
    }

    @Test
    public void persistOrderSpecimenBarcodeCounts_defaultsInvalidValuesToOne() {
        Sample sample = new Sample();
        sample.setId("sample-1");
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("item-1");
        Map<SampleItem, Integer> specimenLabelQuantities = new LinkedHashMap<>();
        specimenLabelQuantities.put(sampleItem, -2);

        service.persistOrderSpecimenBarcodeCounts(sample, 0, specimenLabelQuantities);

        ArgumentCaptor<Map<SampleItem, Integer>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(barcodeInfoService).saveBarcodeInfoForSampleAndSampleItems(eq(sample), eq(1), mapCaptor.capture());
        org.junit.Assert.assertEquals(Integer.valueOf(1), mapCaptor.getValue().get(sampleItem));
    }

    @Test
    public void persistOrderSpecimenBarcodeCounts_skipsNullSample() {
        service.persistOrderSpecimenBarcodeCounts(null, 2, new LinkedHashMap<>());

        verify(barcodeInfoService, never()).saveBarcodeInfoForSampleAndSampleItems(isNull(), anyInt(),
                org.mockito.ArgumentMatchers.anyMap());
    }

    @Test
    public void persistMicrobiologyOrderDraftUsesTheAuthenticatedActorAndSavedSample() {
        Sample sample = new Sample();
        sample.setId("99");
        MicroCaseOrderDetailRequestForm detail = new MicroCaseOrderDetailRequestForm();
        detail.clinicalHistory = "Fever";

        service.persistMicrobiologyOrderDraft(sample, detail, "7");

        verify(microCaseOrderDetailService).saveOrderDraft(sample, detail, "7");
    }

    @Test
    public void persistMicrobiologyOrderDraftSkipsAbsentDetail() {
        Sample sample = new Sample();
        sample.setId("99");

        service.persistMicrobiologyOrderDraft(sample, null, "7");

        verify(microCaseOrderDetailService, never()).saveOrderDraft(eq(sample), isNull(), eq("7"));
    }
}
