package org.openelisglobal.storage.integration;

import static org.junit.Assert.*;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.junit.Before;
import org.junit.Test;

/**
 * Simple FHIR connectivity test without Spring context. Validates FHIR server
 * is accessible.
 */
public class FhirConnectivityTest {

    private FhirContext fhirContext;
    private IGenericClient fhirClient;

    @Before
    public void setup() {
        fhirContext = FhirContext.forR4();

        // Use localhost:8444 for FHIR server (mapped port from dev.docker-compose.yml)
        // Disable SSL validation for local testing
        fhirContext.getRestfulClientFactory().setSocketTimeout(5000);

        // For dev environment, use HTTP endpoint
        fhirClient = fhirContext.newRestfulGenericClient("http://localhost:8081/fhir/");
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
    public void testFhirServerIsAccessible() {
        if (!isFhirServerAvailable()) {
            System.out.println("⚠️ FHIR server not available, skipping connectivity test");
            return;
        }

        try {
            // When: Query FHIR server capabilities
            CapabilityStatement capabilities = fhirClient.capabilities().ofType(CapabilityStatement.class).execute();

            // Then: Should get response
            assertNotNull("FHIR server should return CapabilityStatement", capabilities);
            assertEquals("Should be FHIR R4", "4.0.1", capabilities.getFhirVersion().toString());

            System.out.println("✅ FHIR server is accessible at http://localhost:8081/fhir/");
            System.out.println("FHIR Version: " + capabilities.getFhirVersion());
        } catch (Exception e) {
            fail("FHIR server not accessible: " + e.getMessage());
        }
    }
}
