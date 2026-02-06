package org.openelisglobal.fhir.servlets;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import jakarta.servlet.ServletException;
import java.util.Arrays;
import org.openelisglobal.fhir.providers.PractitionerProvider;
import org.springframework.context.ApplicationContext;

public class FhirRestfulServer extends RestfulServer {

    private ApplicationContext applicationContext;

    public FhirRestfulServer(ApplicationContext context) {

        this.applicationContext = context;
    }

    @Override
    protected void initialize() throws ServletException {
        super.initialize();
        setFhirContext(FhirContext.forR4());
        setResourceProviders(Arrays.asList(applicationContext.getBean(PractitionerProvider.class)));
    }
}
