package org.openelisglobal.provider.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.dao.ProviderDAO;
import org.openelisglobal.provider.valueholder.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderServiceImpl extends AuditableBaseObjectServiceImpl<Provider, String> implements ProviderService {
    @Autowired
    protected ProviderDAO baseObjectDAO;
    @Autowired
    protected PersonService personService;

    ProviderServiceImpl() {
        super(Provider.class);
    }

    @Override
    protected ProviderDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(Provider provider) {
        getBaseObjectDAO().getData(provider);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Provider> getPageOfProviders(int startingRecNo) {
        return getBaseObjectDAO().getPageOfProviders(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Provider> getAllProviders() {
        return getBaseObjectDAO().getAllProviders();
    }

    @Override
    @Transactional(readOnly = true)
    public Provider getProviderByPerson(Person person) {
        return getBaseObjectDAO().getProviderByPerson(person);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Provider> getAllActiveProviders() {
        return getBaseObjectDAO().getAllMatching("active", Boolean.TRUE);
    }

    @Override
    @Transactional
    public void deactivateAllProviders() {
        for (Provider provider : getBaseObjectDAO().getAll()) {
            provider.setActive(false);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Provider getProviderByFhirId(UUID fhirUuid) {
        List<Provider> providers = getBaseObjectDAO().getAllMatching("fhirUuid", fhirUuid);
        if (providers.size() <= 0) {
            return null;
        } else {
            return providers.get(0);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getProviderIdByFhirId(UUID fhirUuid) {
        List<Provider> providers = getBaseObjectDAO().getAllMatching("fhirUuid", fhirUuid);
        if (providers.size() <= 0) {
            return null;
        } else {
            return providers.get(0).getId();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Provider> getPagesOfSearchedProviders(int startingRecNo, String parameter) {
        return baseObjectDAO.getPagesOfSearchedProviders(startingRecNo, parameter);
    }

    @Override
    @Transactional(readOnly = true)
    public int getTotalSearchedProviderCount(String parameter) {
        return baseObjectDAO.getTotalSearchedProviderCount(parameter);
    }

    @Override
    @Transactional
    public void deactivateProviders(List<Provider> providers) {
        for (Provider deactivateProvider : providers) {
            Optional<Provider> dbProvider = baseObjectDAO.get(deactivateProvider.getId());
            if (dbProvider.isPresent()) {
                dbProvider.get().setActive(false);
            } else {
                LogEvent.logWarn(this.getClass().getSimpleName(), "deactivateProviders",
                        "could not deactivate Provider with id '" + deactivateProvider.getId()
                                + "' as it could not be found");
            }
        }
    }

    @Override
    @Transactional
    public Provider insertOrUpdateProviderByFhirUuid(UUID fhirUuid, Provider provider) {
        Provider dbProvider = getProviderByFhirId(fhirUuid);

        if (dbProvider != null) {
            dbProvider.setActive(provider.getActive());
            dbProvider.getPerson().setLastName(provider.getPerson().getLastName());
            dbProvider.getPerson().setMiddleName(provider.getPerson().getMiddleName());
            dbProvider.getPerson().setFirstName(provider.getPerson().getFirstName());
            dbProvider.getPerson().setEmail(provider.getPerson().getEmail());
            dbProvider.getPerson().setPrimaryPhone(provider.getPerson().getPrimaryPhone());
            dbProvider.getPerson().setWorkPhone(provider.getPerson().getWorkPhone());
            dbProvider.getPerson().setFax(provider.getPerson().getFax());
            dbProvider.getPerson().setCellPhone(provider.getPerson().getCellPhone());
            dbProvider = save(dbProvider);
        } else {
            if (fhirUuid == null) {
                fhirUuid = UUID.randomUUID();
            }
            provider.setFhirUuid(fhirUuid);
            provider.getPerson().setSysUserId("1");
            provider.setPerson(personService.save(provider.getPerson()));
            provider.setSysUserId("1");
            dbProvider = save(provider);
        }
        return dbProvider;
    }
}
