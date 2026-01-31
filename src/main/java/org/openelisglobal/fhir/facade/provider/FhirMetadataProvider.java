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
import org.openelisglobal.fhir.facade.FhirFacadeConfig;
import org.openelisglobal.fhir.facade.FhirFacadeServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fhir")
public class FhirMetadataProvider {

    private static final String FHIR_JSON = "application/fhir+json";

    @Autowired
    private FhirFacadeConfig fhirFacadeConfig;

    @GetMapping(value = "/metadata", produces = { FHIR_JSON, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<String> metadata() {
        try {
            CapabilityStatement cs = buildCapabilityStatement();
            String json = FhirFacadeServlet.getJsonParser().encodeResourceToString(cs);
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(FHIR_JSON)).body(json);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private CapabilityStatement buildCapabilityStatement() {
        CapabilityStatement cs = new CapabilityStatement();

        cs.setUrl("https://openelis-global.org/fhir/metadata");
        cs.setVersion(fhirFacadeConfig.getServerVersion());
        cs.setName("OpenELISFhirFacade");
        cs.setTitle(fhirFacadeConfig.getServerName());
        cs.setStatus(PublicationStatus.ACTIVE);
        cs.setExperimental(false);
        cs.setDate(new Date());
        cs.setPublisher("OpenELIS Global");
        cs.setDescription("OpenELIS Global 2 - Native FHIR R4 Facade");
        cs.setKind(CapabilityStatementKind.INSTANCE);

        CapabilityStatementSoftwareComponent software = cs.getSoftware();
        software.setName("OpenELIS Global");
        software.setVersion("3.0");

        cs.setFhirVersion(FHIRVersion._4_0_1);
        cs.addFormat("application/fhir+json");
        cs.addFormat("application/fhir+xml");

        CapabilityStatementRestComponent rest = cs.addRest();
        rest.setMode(RestfulCapabilityMode.SERVER);

        addPatientResourceCapabilities(rest);

        return cs;
    }

    private void addPatientResourceCapabilities(CapabilityStatementRestComponent rest) {
        CapabilityStatementRestResourceComponent patient = rest.addResource();
        patient.setType("Patient");
        patient.setProfile("http://hl7.org/fhir/StructureDefinition/Patient");

        patient.addInteraction().setCode(TypeRestfulInteraction.READ);
        patient.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);

        addSearchParam(patient, "identifier", SearchParamType.TOKEN, "Search by patient identifier");
        addSearchParam(patient, "family", SearchParamType.STRING, "Search by family name");
        addSearchParam(patient, "given", SearchParamType.STRING, "Search by given name");
        addSearchParam(patient, "name", SearchParamType.STRING, "Search by any name part");
        addSearchParam(patient, "_count", SearchParamType.NUMBER, "Maximum results to return");
    }

    private void addSearchParam(CapabilityStatementRestResourceComponent resource, String name, SearchParamType type,
            String documentation) {
        CapabilityStatementRestResourceSearchParamComponent param = resource.addSearchParam();
        param.setName(name);
        param.setType(type);
        param.setDocumentation(documentation);
    }
}
