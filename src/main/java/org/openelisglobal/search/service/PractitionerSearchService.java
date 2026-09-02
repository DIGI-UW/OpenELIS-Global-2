package org.openelisglobal.search.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.fhir.search.searchparams.PractitionerSearchParams;
import org.openelisglobal.fhir.util.FhirSearchHelper;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PractitionerSearchService {

    @Autowired
    private ProviderService providerService;

   
    public List<Provider> searchProviders(PractitionerSearchParams params) {

        int pageStart = params.getPageStart() != null ? params.getPageStart() : 1;
        int pageSize = params.getPageSize() != null ? params.getPageSize() : 50;
        boolean activeOnly = params.isActiveOnly();

       
        if (params.hasId()) {
            String resourceId = FhirSearchHelper.extractToken(params.getId());
            if (resourceId != null) {
                Provider provider = providerService.getProviderByFhirId(UUID.fromString(resourceId));
                List<Provider> result = new ArrayList<>();
                if (provider != null && (!activeOnly || provider.getActive())) {
                    result.add(provider);
                }
                return result;
            }
        }

        if (params.hasLastUpdated()) {
            Date fromDate = FhirSearchHelper.extractFromDate(params.getLastUpdated());
            Date toDate = FhirSearchHelper.extractToDate(params.getLastUpdated());

            
            return providerService.getProvidersByLastUpdated(fromDate, toDate, pageStart, pageSize);
        }

      
        if (params.hasIdentifier()) {
            String identifierValue = FhirSearchHelper.extractToken(params.getIdentifier());

            if (identifierValue != null) {
                // Try externalId first
                List<Provider> providers = providerService.getAllMatching("externalId", identifierValue);
                if (providers.isEmpty()) {
                    providers = providerService.getAllMatching("npi", identifierValue);
                }
                if (providers.isEmpty()) {
                    providers = providerService.getAllMatching("id", identifierValue);
                }

                
                if (activeOnly) {
                    providers = filterActiveOnly(providers);
                }
                return providers;
            }
        }

        
        if (params.hasName()) {
            String givenName = FhirSearchHelper.extractString(params.getGiven());
            String familyName = FhirSearchHelper.extractString(params.getFamily());
            String searchTerm = FhirSearchHelper.buildSearchTerm(givenName, familyName);

          
            return providerService.getPagesOfSearchedProviders(pageStart, searchTerm);
        }

        
        List<Provider> allProviders = providerService.getAllMatching("active", true);
        return FhirSearchHelper.applyPagination(allProviders, pageStart, pageSize);
    }

   
    private List<Provider> filterActiveOnly(List<Provider> providers) {
        List<Provider> activeProviders = new ArrayList<>();
        for (Provider provider : providers) {
            if (provider.getActive()) {
                activeProviders.add(provider);
            }
        }
        return activeProviders;
    }
}