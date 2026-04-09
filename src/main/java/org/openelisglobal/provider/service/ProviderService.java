package org.openelisglobal.provider.service;

import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.valueholder.Provider;

public interface ProviderService extends BaseObjectService<Provider, String> {
    void getData(Provider provider);

    List<Provider> getPageOfProviders(int startingRecNo);

    List<Provider> getAllProviders();

    List<Provider> getAllActiveProviders();

    Provider getProviderByPerson(Person person);

    void deactivateAllProviders();

    Provider getProviderByFhirId(UUID fhirUuid);

    String getProviderIdByFhirId(UUID fhirUuid);

    List<Provider> getPagesOfSearchedProviders(int startingRecNo, String parameter);

    int getTotalSearchedProviderCount(String parameter);

    /**
     * Search providers by phone number.
     *
     * @param startingRecNo starting record number for pagination
     * @param phone         phone number to search for
     * @return list of providers matching the phone number
     */
    List<Provider> getPagesOfSearchedProvidersByPhone(int startingRecNo, String phone);

    /**
     * Get total count of providers matching the phone number.
     *
     * @param phone phone number to search for
     * @return count of matching providers
     */
    int getTotalSearchedProviderCountByPhone(String phone);

    void deactivateProviders(List<Provider> providers);

    Provider insertOrUpdateProviderByFhirUuid(UUID fhirUuid, Provider provider);
}
