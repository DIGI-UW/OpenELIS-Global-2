package org.openelisglobal.dataexchange.fhir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IRead;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IReadTyped;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskRestrictionComponent;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceServiceImpl.FhirOperations;
import org.openelisglobal.organization.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.AopTestUtils;

/**
 * An incoming referral names the referring laboratory; the reference lab keeps
 * it as a referring organization, so the order shows who sent it.
 */
public class ReferringOrganizationImportTest extends BaseWebContextSensitiveTest {

    private static final String REFERRING = "1f8c2c3a-5b7d-4e9f-a1b2-c3d4e5f60718";

    @Autowired
    private FhirApiWorkflowService workflowService;

    @Autowired
    private OrganizationService organizationService;

    @Test
    public void theReferringLaboratoryBecomesAReferringOrganizationOnce() {
        FhirApiWorkFlowServiceImpl service = AopTestUtils.getTargetObject(workflowService);
        IGenericClient remote = mock(IGenericClient.class);
        IRead read = mock(IRead.class);
        @SuppressWarnings("unchecked")
        IReadTyped<Organization> typed = mock(IReadTyped.class);
        @SuppressWarnings("unchecked")
        IReadExecutable<Organization> executable = mock(IReadExecutable.class);
        Organization referring = new Organization();
        referring.setId(REFERRING);
        referring.setName("Kampala Central Laboratory");
        when(remote.read()).thenReturn(read);
        when(read.resource(Organization.class)).thenReturn(typed);
        when(typed.withId(REFERRING)).thenReturn(executable);
        when(executable.execute()).thenReturn(referring);
        Task task = new Task();
        task.setRestriction(new TaskRestrictionComponent().addRecipient(new Reference("Organization/" + REFERRING)));

        FhirOperations operations = new FhirOperations();
        service.importReferringOrganization(remote, task, operations);
        service.importReferringOrganization(remote, task, new FhirOperations());

        org.openelisglobal.organization.valueholder.Organization local = organizationService
                .getOrganizationByFhirId(REFERRING);
        assertNotNull(local);
        assertEquals("Kampala Central Laboratory", local.getOrganizationName());
        assertTrue("a copy goes to the local FHIR store", operations.updateResources.containsKey(REFERRING));
        assertEquals(1,
                organizationService.getAll().stream().filter(o -> REFERRING.equals(o.getFhirUuidAsString())).count());
    }

    @Test
    public void aTaskWithoutAReferringOrganizationChangesNothing() {
        FhirApiWorkFlowServiceImpl service = AopTestUtils.getTargetObject(workflowService);
        IGenericClient remote = mock(IGenericClient.class, RETURNS_DEEP_STUBS);
        Task task = new Task();
        task.setRestriction(new TaskRestrictionComponent().addRecipient(new Reference("Practitioner/abc")));
        FhirOperations operations = new FhirOperations();

        service.importReferringOrganization(remote, task, operations);

        assertTrue(operations.updateResources.isEmpty());
    }

    @Test
    public void anOrganizationAlreadyNamedLikeTheReferringLabIsLinkedNotDuplicated() {
        String referringId = "7c1d9e2b-4a6f-4b8e-9d0c-1e2f3a4b5c6d";
        org.openelisglobal.organization.valueholder.Organization existing = new org.openelisglobal.organization.valueholder.Organization();
        existing.setOrganizationName("Mbarara Regional Referral Laboratory");
        existing.setIsActive(IActionConstants.YES);
        existing.setMlsLabFlag(IActionConstants.NO);
        existing.setMlsSentinelLabFlag(IActionConstants.NO);
        String existingId = organizationService.insert(existing);

        FhirApiWorkFlowServiceImpl service = AopTestUtils.getTargetObject(workflowService);
        Organization referring = new Organization();
        referring.setId(referringId);
        referring.setName("Mbarara Regional Referral Laboratory");
        Task task = new Task();
        task.setRestriction(new TaskRestrictionComponent().addRecipient(new Reference("Organization/" + referringId)));

        service.importReferringOrganization(remoteReturning(referringId, referring), task, new FhirOperations());

        org.openelisglobal.organization.valueholder.Organization linked = organizationService
                .getOrganizationByFhirId(referringId);
        assertNotNull(linked);
        assertEquals(existingId, linked.getId());
        assertEquals(1, organizationService.getAll().stream()
                .filter(o -> "Mbarara Regional Referral Laboratory".equals(o.getOrganizationName())).count());
        assertTrue(organizationService.getTypeIdsForOrganizationId(existingId)
                .contains(TableIdService.getInstance().REFERRING_ORG_TYPE_ID));
    }

    private IGenericClient remoteReturning(String id, Organization organization) {
        IGenericClient remote = mock(IGenericClient.class);
        IRead read = mock(IRead.class);
        @SuppressWarnings("unchecked")
        IReadTyped<Organization> typed = mock(IReadTyped.class);
        @SuppressWarnings("unchecked")
        IReadExecutable<Organization> executable = mock(IReadExecutable.class);
        when(remote.read()).thenReturn(read);
        when(read.resource(Organization.class)).thenReturn(typed);
        when(typed.withId(id)).thenReturn(executable);
        when(executable.execute()).thenReturn(organization);
        return remote;
    }
}
