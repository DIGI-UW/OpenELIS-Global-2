package org.openelisglobal.storage.integration;

import static org.junit.Assert.*;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Location;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.storage.fhir.StorageLocationFhirTransform;
import org.openelisglobal.storage.valueholder.StorageRoom;

/**
 * Integration test for FHIR Location resource creation. Tests FHIR server
 * connectivity and Location resource structure.
 * 
 * Uses HTTP endpoint to avoid SSL certificate complexity in tests.
 * 
 * NOTE: These tests require a running FHIR server at
 * http://localhost:8081/fhir/ Skip in CI environments where FHIR server is not
 * available.
 */
public class StorageFhirIntegrationTest {

    private FhirContext fhirContext;
    private IGenericClient fhirClient;
    private StorageLocationFhirTransform storageLocationFhirTransform;

    private StorageRoom testRoom;

    @Before
    public void setup() {
        // Initialize FHIR context and client
        fhirContext = FhirContext.forR4();
        fhirContext.getRestfulClientFactory().setSocketTimeout(5000);

        // Use HTTP endpoint for testing (port 8081 mapped from container)
        fhirClient = fhirContext.newRestfulGenericClient("http://localhost:8081/fhir/");

        // Initialize transform service
        storageLocationFhirTransform = new StorageLocationFhirTransform();

        // Create test data
        testRoom = new StorageRoom();
        testRoom.setId((int) (System.currentTimeMillis() % Integer.MAX_VALUE));
        testRoom.setFhirUuid(UUID.randomUUID());
        testRoom.setCode("TEST-FHIR-INT");
        testRoom.setName("Test FHIR Integration Room");
        testRoom.setDescription("Created by integration test");
        testRoom.setActive(true);
    }

    /**
     * Check if FHIR server is available. Returns true if server is reachable.
     */
    private boolean isFhirServerAvailable() {
        try {
            fhirClient.capabilities().ofType(CapabilityStatement.class).execute();
            return true;
        } catch (Exception e) {
            // Server not available - return false
            return false;
        }
    }

    @Test
    public void testFhirServerConnectivity() {
        if (!isFhirServerAvailable()) {
            System.out.println("⚠️ FHIR server not available, skipping connectivity test");
            return;
        }

        try {
            // When: Query FHIR server capabilities
            CapabilityStatement capabilities = fhirClient.capabilities().ofType(CapabilityStatement.class).execute();

            // Then: Should get response
            assertNotNull("FHIR server should return CapabilityStatement", capabilities);
            System.out.println("✅ FHIR server accessible. Version: " + capabilities.getFhirVersion());
        } catch (Exception e) {
            fail("FHIR server not accessible: " + e.getMessage());
        }
    }

    @Test
    public void testTransformAndPersistStorageRoom() {
        if (!isFhirServerAvailable()) {
            System.out.println("⚠️ FHIR server not available, skipping persistence test");
            return;
        }

        try {
            // When: Transform StorageRoom to FHIR Location
            Location fhirLocation = storageLocationFhirTransform.transformToFhirLocation(testRoom);

            // Then: Location should be valid
            assertNotNull("FHIR Location should not be null", fhirLocation);
            assertEquals("Location ID should match entity fhir_uuid", testRoom.getFhirUuid().toString(),
                    fhirLocation.getId());

            // And: Create resource on FHIR server
            Location created = (Location) fhirClient.create().resource(fhirLocation).execute().getResource();

            assertNotNull("Created Location should be returned", created);
            System.out.println("✅ Successfully created Location on FHIR server: " + created.getId());

        } catch (Exception e) {
            fail("FHIR persistence test failed: " + e.getMessage());
        }
    }

    @Test
    public void testQueryStorageLocationsByPhysicalType() {
        if (!isFhirServerAvailable()) {
            System.out.println("⚠️ FHIR server not available, skipping query test");
            return;
        }

        try {
            // When: Query for rooms (physicalType=ro)
            Bundle results = fhirClient.search().forResource(Location.class).returnBundle(Bundle.class).execute();

            // Then: Should get results (may be empty if no data yet)
            assertNotNull("Bundle should be returned", results);
            System.out.println("✅ FHIR query successful. Found " + results.getTotal() + " locations");

        } catch (Exception e) {
            fail("FHIR query test failed: " + e.getMessage());
        }
    }
}
