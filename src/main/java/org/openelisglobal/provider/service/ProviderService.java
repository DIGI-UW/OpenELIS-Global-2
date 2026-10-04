package org.openelisglobal.provider.service;

import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.valueholder.Provider;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ProviderService extends BaseObjectService<Provider, String> {
    @PreAuthorize("hasAuthority('PRIV_PROVIDER_VIEW')")
    void getData(Provider provider);

    @PreAuthorize("hasAuthority('PRIV_PROVIDER_VIEW')")
    List<Provider> getPageOfProviders(int startingRecNo);

    @PreAuthorize("hasAuthority('PRIV_PROVIDER_VIEW')")
    List<Provider> getAllProviders();

    @PreAuthorize("hasAuthority('PRIV_PROVIDER_VIEW')")
    List<Provider> getAllActiveProviders();

    @PreAuthorize("hasAuthority('PRIV_PROVIDER_VIEW')")
    Provider getProviderByPerson(Person person);

    @PreAuthorize("hasAuthority('PRIV_PROVIDER_MANAGE')")
    void deactivateAllProviders();

    @PreAuthorize("hasAuthority('PRIV_PROVIDER_VIEW')")
    Provider getProviderByFhirId(UUID fhirUuid);

    @PreAuthorize("hasAuthority('PRIV_PROVIDER_VIEW')")
    String getProviderIdByFhirId(UUID fhirUuid);

    @PreAuthorize("hasAuthority('PRIV_PROVIDER_VIEW')")
    List<Provider> getPagesOfSearchedProviders(int startingRecNo, String parameter);

    /** The same page, narrowed to one provider title (OGC-1223). */
    @PreAuthorize("hasAuthority('PRIV_PROVIDER_VIEW')")
    List<Provider> getPagesOfSearchedProviders(int startingRecNo, String parameter, String titleCode);

    @PreAuthorize("hasAuthority('PRIV_PROVIDER_VIEW')")
    int getTotalSearchedProviderCount(String parameter);

    /** The same count, narrowed to one provider title (OGC-1223). */
    @PreAuthorize("hasAuthority('PRIV_PROVIDER_VIEW')")
    int getTotalSearchedProviderCount(String parameter, String titleCode);

    /**
     * Search providers by phone number.
     *
     * @param startingRecNo starting record number for pagination
     * @param phone         phone number to search for
     * @return list of providers matching the phone number
     */
    @PreAuthorize("hasAuthority('PRIV_PROVIDER_VIEW')")
    List<Provider> getPagesOfSearchedProvidersByPhone(int startingRecNo, String phone);

    @PreAuthorize("hasAuthority('PRIV_PROVIDER_VIEW')")
    int getTotalSearchedProviderCountByPhone(String phone);

    @PreAuthorize("hasAuthority('PRIV_PROVIDER_MANAGE')")
    void deactivateProviders(List<Provider> providers);

    @PreAuthorize("hasAuthority('PRIV_PROVIDER_MANAGE')")
    Provider insertOrUpdateProviderByFhirUuid(UUID fhirUuid, Provider provider);
}
