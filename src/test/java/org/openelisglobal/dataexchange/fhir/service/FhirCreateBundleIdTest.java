package org.openelisglobal.dataexchange.fhir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Resource;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The create path used to stamp a fresh {@code UUID.randomUUID()} on every
 * resource it PUT, discarding the id the caller had set (OGC-1188).
 *
 * <p>
 * That is invisible locally and fatal across sites: a peer lab addresses this
 * installation's resources by the uuid it already knows — an Organization by
 * its {@code fhir_uuid}, which the referral Task carries as {@code Task.owner}
 * and which the receiving lab filters its inbound poll on. A random id there
 * means the peer's poll never matches and the referral never arrives.
 */
public class FhirCreateBundleIdTest extends BaseWebContextSensitiveTest {

    @Autowired
    private FhirPersistanceService fhirPersistanceService;

    @Test
    public void createKeepsTheIdTheCallerPutOnTheResource() {
        String organizationFhirUuid = "4175d853-e703-4d2c-aa95-9803cb355215";
        Organization organization = new Organization();
        organization.setId(organizationFhirUuid);
        organization.setName("Reference Laboratory");

        Bundle bundle = makeCreateBundleFor(organizationFhirUuid, organization);

        assertEquals("a peer can only find the resource under the uuid this site published",
                "Organization/" + organizationFhirUuid, requestUrlOfOnlyEntry(bundle));
    }

    @Test
    public void createMintsAnIdOnlyWhenTheResourceHasNone() {
        Organization organization = new Organization();
        organization.setName("Reference Laboratory");

        Bundle bundle = makeCreateBundleFor("", organization);

        String requestUrl = requestUrlOfOnlyEntry(bundle);
        assertNotNull(requestUrl);
        assertNotEquals("an id-less resource still needs one to be PUT", "Organization/", requestUrl);
        UUID.fromString(requestUrl.substring("Organization/".length()));
    }

    /**
     * Callers with no uuid to hand fall back to the local database id, and a FHIR
     * server refuses a client-assigned id that is all digits (HAPI-0960), which
     * failed the whole transaction and took the order's FHIR push down with it.
     */
    @Test
    public void createMintsAnIdWhenTheResourceCarriesALocalDatabaseId() {
        Organization organization = new Organization();
        organization.setId("1");
        organization.setName("Reference Laboratory");

        Bundle bundle = makeCreateBundleFor("1", organization);

        String requestUrl = requestUrlOfOnlyEntry(bundle);
        assertNotEquals("an all-digits id is not client-assignable and must not be sent as one", "Organization/1",
                requestUrl);
        UUID.fromString(requestUrl.substring("Organization/".length()));
    }

    private Bundle makeCreateBundleFor(String key, Resource resource) {
        Map<String, Resource> resources = new HashMap<>();
        resources.put(key, resource);
        return fhirPersistanceService.makeTransactionBundleForCreate(resources);
    }

    private String requestUrlOfOnlyEntry(Bundle bundle) {
        assertEquals(1, bundle.getEntry().size());
        return bundle.getEntryFirstRep().getRequest().getUrl();
    }
}
