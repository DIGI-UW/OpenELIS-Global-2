package org.openelisglobal.fhir.facade;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;

/**
 * FHIR Facade Core - Provides FHIR context and parsing utilities.
 *
 * <p>
 * This component provides the core FHIR functionality for the facade layer.
 * Instead of using HAPI FHIR's servlet-based approach (which has javax/jakarta
 * compatibility issues), we use Spring MVC controllers with HAPI FHIR for
 * parsing/serialization only.
 *
 * <p>
 * The facade provides:
 * <ul>
 * <li>FHIR R4 context for resource handling</li>
 * <li>JSON and XML parsers/serializers</li>
 * <li>Resource transformation utilities</li>
 * </ul>
 *
 * <p>
 * URL Mapping: /fhir/* (handled by FhirFacadeController)
 *
 * @author OpenELIS Global Team
 * @since 3.0
 */
@Component
public class FhirFacadeServlet {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();

    /**
     * Get the shared FHIR R4 context.
     *
     * @return the FHIR context
     */
    public static FhirContext getFhirContext() {
        return FHIR_CONTEXT;
    }

    /**
     * Get a JSON parser for FHIR resources.
     *
     * @return a JSON parser
     */
    public static IParser getJsonParser() {
        return FHIR_CONTEXT.newJsonParser().setPrettyPrint(true);
    }

    /**
     * Get an XML parser for FHIR resources.
     *
     * @return an XML parser
     */
    public static IParser getXmlParser() {
        return FHIR_CONTEXT.newXmlParser().setPrettyPrint(true);
    }

    /**
     * Initialize the FHIR facade.
     */
    public FhirFacadeServlet() {
        LogEvent.logInfo(this.getClass().getSimpleName(), "constructor",
                "FHIR Facade Core initialized with R4 context");
    }
}
