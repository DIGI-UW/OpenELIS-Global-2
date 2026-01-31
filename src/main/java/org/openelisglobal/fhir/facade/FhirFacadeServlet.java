package org.openelisglobal.fhir.facade;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.springframework.stereotype.Component;

@Component
public class FhirFacadeServlet {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();

    public static FhirContext getFhirContext() {
        return FHIR_CONTEXT;
    }

    public static IParser getJsonParser() {
        return FHIR_CONTEXT.newJsonParser().setPrettyPrint(true);
    }

    public static IParser getXmlParser() {
        return FHIR_CONTEXT.newXmlParser().setPrettyPrint(true);
    }
}
