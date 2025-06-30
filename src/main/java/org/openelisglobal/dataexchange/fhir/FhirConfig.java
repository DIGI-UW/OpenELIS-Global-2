
package org.openelisglobal.dataexchange.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IRestfulClientFactory;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.apache.commons.validator.GenericValidator;
import org.apache.http.impl.client.CloseableHttpClient;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FhirConfig {

    @Getter
    @Value("${org.openelisglobal.oe.fhir.system:http://openelis-global.org}")
    private String oeFhirSystem;

    @Getter
    @Value("${org.openelisglobal.fhirstore.uri:}")
    private String localFhirStorePath;

    @Getter
    @Value("${org.openelisglobal.remote.source.uri:}")
    private String[] remoteStorePaths;

    @Getter
    @Value("${org.openelisglobal.fhirstore.username:}")
    private String username;

    @Getter
    @Value("${org.openelisglobal.fhirstore.password:}")
    private String password;

    @Getter
    @Value("${org.openelisglobal.crserver.uri:}")
    private String clientRegistryServerUrl;

    @Getter
    @Value("${org.openelisglobal.crserver.username:}")
    private String clientRegistryUserName;

    @Getter
    @Value("${org.openelisglobal.crserver.password:}")
    private String clientRegistryPassword;

    @Value("${org.openelisglobal.remote.source.identifier:}#{T(java.util.Collections).emptyList()}")
    private List<String> remoteStoreIdentifier;

    @Autowired
    CloseableHttpClient httpClient;

    @Bean
    public FhirContext fhirContext() {
        FhirContext fhirContext = new FhirContext(FhirVersionEnum.R4);
        configureFhirHttpClient(fhirContext);
        return fhirContext;
    }

    public void configureFhirHttpClient(FhirContext fhirContext) {
        IRestfulClientFactory clientFactory = new ApacheRestfulClientFactory(fhirContext);

        clientFactory.setHttpClient(httpClient);
        fhirContext.setRestfulClientFactory(clientFactory);
    }

    public List<String> getRemoteStoreIdentifier() {

        if (remoteStoreIdentifier.contains(ResourceType.Practitioner + "/*")) {
            List<String> fetchedIdentifiers = new ArrayList<>();
            for (String remoteStorePath : getRemoteStorePaths()) {
                IGenericClient fhirClient = fhirContext().newRestfulGenericClient(remoteStorePath);
                if (!GenericValidator.isBlankOrNull(getUsername())
                        && !getLocalFhirStorePath().equals(remoteStorePath)) {
                    IClientInterceptor authInterceptor = new BasicAuthInterceptor(getUsername(), getPassword());
                    fhirClient.registerInterceptor(authInterceptor);
                }
                List<Bundle> allBundles = new ArrayList<>();
                Bundle practitionerBundle = fhirClient.search().forResource(Practitioner.class)
                        .returnBundle(Bundle.class).execute();
                allBundles.add(practitionerBundle);

                while (practitionerBundle.getLink(IBaseBundle.LINK_NEXT) != null) {
                    practitionerBundle = fhirClient.loadPage().next(practitionerBundle).execute();
                    allBundles.add(practitionerBundle);
                }

                for (Bundle bundle : allBundles) {
                    for (BundleEntryComponent bundleComponent : bundle.getEntry()) {
                        if (bundleComponent.hasResource()
                                && ResourceType.Practitioner.equals(bundleComponent.getResource().getResourceType())) {

                            fetchedIdentifiers.add(ResourceType.Practitioner + "/"
                                    + bundleComponent.getResource().getIdElement().getIdPart());
                        }
                    }
                }
            }
            return fetchedIdentifiers;
        } else {
            return remoteStoreIdentifier;
        }

    }

}
