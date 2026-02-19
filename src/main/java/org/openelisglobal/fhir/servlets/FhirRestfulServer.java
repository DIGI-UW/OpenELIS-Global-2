package org.openelisglobal.fhir.servlets;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import jakarta.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationContext;

public class FhirRestfulServer extends RestfulServer {

    private final ApplicationContext applicationContext;

    public FhirRestfulServer(ApplicationContext context) {
        this.applicationContext = context;
    }

    @Override
    protected void initialize() throws ServletException {

        super.initialize();

        setFhirContext(FhirContext.forR4());

        Map<String, IResourceProvider> providerMap = applicationContext.getBeansOfType(IResourceProvider.class);

        List<IResourceProvider> providers = new ArrayList<>(providerMap.values());

        setResourceProviders(providers);
    }
}
