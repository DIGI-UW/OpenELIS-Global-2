package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.servlet.http.HttpServletRequest;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;

/**
 * Shared utility methods for FHIR resource providers. Eliminates boilerplate
 * around {@link MethodOutcome} construction, FHIR store synchronization, and
 * common validation that is identical across all
 * {@link ca.uhn.fhir.rest.server.IResourceProvider} implementations.
 */
public final class FhirProviderUtils {

    private FhirProviderUtils() {
        // utility class — not instantiable
    }

    /**
     * Builds a {@link MethodOutcome} for a successful Create operation (HTTP 201).
     */
    public static MethodOutcome buildCreateOutcome(IBaseResource resource) {
        MethodOutcome outcome = new MethodOutcome();
        outcome.setId(resource.getIdElement());
        outcome.setResource(resource);
        outcome.setCreated(true);
        outcome.setResponseStatusCode(201);
        return outcome;
    }

    /**
     * Builds a {@link MethodOutcome} for a successful Update operation (HTTP 200).
     */
    public static MethodOutcome buildUpdateOutcome(IBaseResource resource) {
        MethodOutcome outcome = new MethodOutcome();
        outcome.setId(resource.getIdElement());
        outcome.setResource(resource);
        outcome.setCreated(false);
        outcome.setResponseStatusCode(200);
        return outcome;
    }

    /**
     * Builds a {@link MethodOutcome} for a successful Delete (soft-delete)
     * operation (HTTP 204).
     *
     * @param theId        the FHIR resource ID that was deleted
     * @param resourceType the FHIR resource type name (e.g. "Practitioner",
     *                     "Organization")
     */
    public static MethodOutcome buildDeleteOutcome(IdType theId, String resourceType) {
        MethodOutcome outcome = new MethodOutcome();
        outcome.setId(theId);
        outcome.setResponseStatusCode(204);

        OperationOutcome operationOutcome = new OperationOutcome();
        operationOutcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.INFORMATION)
                .setDiagnostics(resourceType + " " + theId.getIdPart() + " has been deleted");
        outcome.setOperationOutcome(operationOutcome);

        return outcome;
    }

    /**
     * Syncs a FHIR resource to the external FHIR store. Failures are logged but do
     * not propagate — the local operation is considered successful even if the sync
     * fails.
     */
    public static void syncToFhirStore(FhirPersistanceService fhirPersistenceService, Resource resource,
            String callerClassName, String callingMethod) {
        try {
            fhirPersistenceService.updateFhirResourceInFhirStore(resource);
        } catch (Exception syncEx) {
            LogEvent.logError(callerClassName, callingMethod,
                    "FHIR store sync failed (continuing anyway): " + syncEx.getMessage());
        }
    }

    /**
     * Extracts the sysUserId from the HTTP request for audit trail purposes.
     */
    public static String getSysUserId(HttpServletRequest request) {
        return ControllerUtills.getSysUserId(request);
    }

    /**
     * Validates that the given {@link IdType} is present and has an ID part. Throws
     * {@link InvalidRequestException} if not.
     *
     * @param theId           the ID to validate
     * @param resourceType    the FHIR resource type name for the error message
     * @param callerClassName the caller's class name for logging
     * @param method          the calling method name for logging
     */
    public static void validateIdParam(IdType theId, String resourceType, String callerClassName, String method) {
        if (theId == null || !theId.hasIdPart()) {
            LogEvent.logError(callerClassName, method, "Missing " + resourceType + " ID for " + method);
            throw new InvalidRequestException(resourceType + " ID must be provided for " + method);
        }
    }
}
