package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.IdType;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * FHIR R4 Resource Provider for DiagnosticReport resources.
 *
 * <p>
 * Exposes lab analyses from OpenELIS directly via the native FHIR facade. A
 * DiagnosticReport groups the Observation results produced by an Analysis into
 * a structured, patient-facing report.
 *
 * <p>
 * Provider is auto-discovered by {@code FhirRestfulServer} via Spring's
 * {@code applicationContext.getBeansOfType(IResourceProvider.class)} — no
 * manual registration required.
 *
 * <p>
 * Supported operations:
 * <ul>
 * <li>READ: GET /fhir/DiagnosticReport/{uuid}</li>
 * </ul>
 */
@Component
public class DiagnosticReportProvider implements IResourceProvider {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private FhirTransformService fhirTransformService;

    @Override
    public Class<DiagnosticReport> getResourceType() {
        return DiagnosticReport.class;
    }

    @Read
    public DiagnosticReport read(@IdParam IdType id) {
        String method = "read";
        try {
            FhirProviderUtils.validateIdParam(id, "DiagnosticReport", this.getClass().getSimpleName(), method);

            String uuid = id.getIdPart();
            UUID parsedUuid;
            try {
                parsedUuid = UUID.fromString(uuid);
            } catch (IllegalArgumentException e) {
                throw new ResourceNotFoundException("DiagnosticReport not found: " + uuid);
            }

            List<Analysis> matches = analysisService.getAllMatching("fhirUuid", parsedUuid);
            if (matches == null || matches.isEmpty()) {
                throw new ResourceNotFoundException("DiagnosticReport not found: " + uuid);
            }

            DiagnosticReport report = fhirTransformService.transformAnalysisToDiagnosticReport(matches.get(0));
            if (report == null) {
                throw new ResourceNotFoundException("Failed to transform Analysis to DiagnosticReport: " + uuid);
            }

            return report;

        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error reading DiagnosticReport: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error retrieving DiagnosticReport");
        }
    }
}
