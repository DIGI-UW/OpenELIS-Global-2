package org.openelisglobal.fhir.facade.provider;

import java.util.Date;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementKind;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceSearchParamComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementSoftwareComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.RestfulCapabilityMode;
import org.hl7.fhir.r4.model.CapabilityStatement.TypeRestfulInteraction;
import org.hl7.fhir.r4.model.Enumerations.FHIRVersion;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.fhir.facade.FhirFacadeConfig;
import org.openelisglobal.fhir.facade.FhirFacadeServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FHIR Metadata/CapabilityStatement endpoint.
 *
 * <p>
 * This controller provides the FHIR CapabilityStatement (metadata) endpoint:
 * <ul>
 * <li>GET /fhir/metadata - Returns server capabilities</li>
 * </ul>
 *
 * <p>
 * The CapabilityStatement describes the FHIR server's capabilities including:
 * <ul>
 * <li>Supported FHIR version</li>
 * <li>Supported resource types</li>
 * <li>Supported operations (read, search, etc.)</li>
 * <li>Supported search parameters</li>
 * </ul>
 *
 * @author OpenELIS Global Team
 * @since 3.0
 */
@RestController
@RequestMapping("/fhir")
public class FhirMetadataProvider {

    private static final String FHIR_JSON = "application/fhir+json";

    @Autowired
    private FhirFacadeConfig fhirFacadeConfig;

    /**
     * FHIR Metadata operation - returns server CapabilityStatement.
     *
     * <p>
     * Endpoint: GET /fhir/metadata
     *
     * @return the CapabilityStatement resource as FHIR JSON
     */
    @GetMapping(value = "/metadata", produces = { FHIR_JSON, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<String> metadata() {
        LogEvent.logInfo(this.getClass().getSimpleName(), "metadata", "Returning CapabilityStatement");

        try {
            CapabilityStatement cs = buildCapabilityStatement();
            String json = FhirFacadeServlet.getJsonParser().encodeResourceToString(cs);

            return ResponseEntity.ok().contentType(MediaType.parseMediaType(FHIR_JSON)).body(json);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "metadata",
                    "Error building CapabilityStatement: " + e.getMessage());
            return ResponseEntity.internalServerError().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Build the CapabilityStatement describing server capabilities.
     *
     * @return the CapabilityStatement resource
     */
    private CapabilityStatement buildCapabilityStatement() {
        CapabilityStatement cs = new CapabilityStatement();

        // Basic metadata
        cs.setUrl("https://openelis-global.org/fhir/metadata");
        cs.setVersion(fhirFacadeConfig.getServerVersion());
        cs.setName("OpenELISFhirFacade");
        cs.setTitle(fhirFacadeConfig.getServerName());
        cs.setStatus(PublicationStatus.ACTIVE);
        cs.setExperimental(false);
        cs.setDate(new Date());
        cs.setPublisher("OpenELIS Global");
        cs.setDescription("OpenELIS Global 2 - Native FHIR R4 Facade. "
                + "This server provides direct FHIR access to OpenELIS laboratory data "
                + "without requiring an external HAPI JPA Server.");
        cs.setKind(CapabilityStatementKind.INSTANCE);

        // Software information
        CapabilityStatementSoftwareComponent software = cs.getSoftware();
        software.setName("OpenELIS Global");
        software.setVersion("3.0");

        // FHIR version
        cs.setFhirVersion(FHIRVersion._4_0_1);
        cs.addFormat("application/fhir+json");
        cs.addFormat("application/fhir+xml");

        // REST capabilities
        CapabilityStatementRestComponent rest = cs.addRest();
        rest.setMode(RestfulCapabilityMode.SERVER);
        rest.setDocumentation("RESTful FHIR R4 Server for OpenELIS Global Laboratory Data");

        addPatientResourceCapabilities(rest);

        return cs;
    }

    /**
     * Add Patient resource capabilities to the REST component.
     *
     * @param rest the REST component
     */
    private void addPatientResourceCapabilities(CapabilityStatementRestComponent rest) {
        CapabilityStatementRestResourceComponent patient = rest.addResource();
        patient.setType("Patient");
        patient.setProfile("http://hl7.org/fhir/StructureDefinition/Patient");
        patient.setDocumentation("Patient demographics and identifiers");

        // Supported interactions
        patient.addInteraction().setCode(TypeRestfulInteraction.READ);
        patient.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);

        // Search parameters
        addSearchParam(patient, "identifier", SearchParamType.TOKEN,
                "Search by patient identifier (subject number, national ID, GUID)");
        addSearchParam(patient, "family", SearchParamType.STRING, "Search by family name (partial match supported)");
        addSearchParam(patient, "given", SearchParamType.STRING, "Search by given name (partial match supported)");
        addSearchParam(patient, "name", SearchParamType.STRING, "Search by any name part");
        addSearchParam(patient, "_count", SearchParamType.NUMBER, "Maximum number of results to return");
    }

    /**
     * Add a search parameter to a resource.
     *
     * @param resource      the resource component
     * @param name          parameter name
     * @param type          parameter type
     * @param documentation parameter documentation
     */
    private void addSearchParam(CapabilityStatementRestResourceComponent resource, String name, SearchParamType type,
            String documentation) {
        CapabilityStatementRestResourceSearchParamComponent param = resource.addSearchParam();
        param.setName(name);
        param.setType(type);
        param.setDocumentation(documentation);
    }
}
