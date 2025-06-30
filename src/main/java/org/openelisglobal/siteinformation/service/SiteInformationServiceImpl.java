package org.openelisglobal.siteinformation.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.apache.commons.validator.GenericValidator;
import org.jasypt.util.text.TextEncryptor;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.util.ConfigurationSideEffects;
import org.openelisglobal.siteinformation.dao.SiteInformationDAO;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiteInformationServiceImpl extends AuditableBaseObjectServiceImpl<SiteInformation, String>
        implements SiteInformationService {

    // this approach of getting the entity manager is not recommended in the service
    // layer. We are doing it here so we can decrypt
    // values without persisting those decrypted values, by evicting the objecct
    // from the entity manage when it is retrieved. It is recommended to integrate
    // jasyp with hibernate directly, but this cannot be done until we move
    // "encrypted" values into another table or column
    @PersistenceContext
    protected EntityManager entityManager;

    @Autowired
    private SiteInformationDAO siteInformationDAO;
    @Autowired
    private ConfigurationSideEffects configurationSideEffects;
    @Autowired
    private TextEncryptor encryptor;

    public SiteInformationServiceImpl() {
        super(SiteInformation.class);
    }

    @Override
    protected SiteInformationDAO getBaseObjectDAO() {
        return siteInformationDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteInformation> getPageOfSiteInformationByDomainName(int startingRecNo, String dbDomainName) {
        return getMatchingOrderedPage("domain.name", dbDomainName, "name", false, startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public int getCountForDomainName(String dbDomainName) {
        return getCountMatching("domain.name", dbDomainName);
    }

    @Override
    @Transactional(readOnly = true)
    public SiteInformation getSiteInformationByName(String name) {
        return getMatch("name", name).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(SiteInformation siteInformation) {
        getBaseObjectDAO().getData(siteInformation);
        decryptSiteInformation(siteInformation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteInformation> getAllSiteInformation() {
        return getAll();
    }

    @Override
    @Transactional(readOnly = true)
    public SiteInformation getSiteInformationById(String urlId) {
        return get(urlId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteInformation> getSiteInformationByDomainName(String domainName) {
        List<SiteInformation> siteInformations = getBaseObjectDAO().getSiteInformationByDomainName(domainName);
        for (SiteInformation siteInformation : siteInformations) {
            decryptSiteInformation(siteInformation);
        }
        return siteInformations;
    }

    @Override
    @Transactional
    public void persistData(SiteInformation siteInformation, boolean newSiteInformation) {
        if (newSiteInformation) {
            insert(siteInformation);
            // siteInformationDAO.insertData(siteInformation);
        } else {
            update(siteInformation);
            // siteInformationDAO.updateData(siteInformation);
        }

        configurationSideEffects.siteInformationChanged(siteInformation);
    }

    @Override
    @Transactional(readOnly = true)
    public SiteInformation get(String id) {
        SiteInformation siteInformation = super.get(id);
        decryptSiteInformation(siteInformation);
        return siteInformation;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SiteInformation> getMatch(String propertyName, Object propertyValue) {
        Optional<SiteInformation> siteInformation = super.getMatch(propertyName, propertyValue);
        if (siteInformation.isPresent()) {
            decryptSiteInformation(siteInformation.get());
        }
        return siteInformation;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SiteInformation> getMatch(Map<String, Object> propertyValues) {
        Optional<SiteInformation> siteInformation = super.getMatch(propertyValues);
        if (siteInformation.isPresent()) {
            decryptSiteInformation(siteInformation.get());
        }
        return siteInformation;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteInformation> getAll() {
        List<SiteInformation> siteInformations = super.getAll();
        for (SiteInformation siteInformation : siteInformations) {
            decryptSiteInformation(siteInformation);
        }
        return siteInformations;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteInformation> getAllMatching(String propertyName, Object propertyValue) {
        List<SiteInformation> siteInformations = super.getAllMatching(propertyName, propertyValue);
        for (SiteInformation siteInformation : siteInformations) {
            decryptSiteInformation(siteInformation);
        }
        return siteInformations;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteInformation> getAllMatching(Map<String, Object> propertyValues) {
        List<SiteInformation> siteInformations = super.getAllMatching(propertyValues);
        for (SiteInformation siteInformation : siteInformations) {
            decryptSiteInformation(siteInformation);
        }
        return siteInformations;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteInformation> getAllOrdered(String orderProperty, boolean descending) {
        List<SiteInformation> siteInformations = super.getAllOrdered(orderProperty, descending);
        for (SiteInformation siteInformation : siteInformations) {
            decryptSiteInformation(siteInformation);
        }
        return siteInformations;
    }

    @Override
    public List<SiteInformation> getAllOrdered(List<String> orderProperties, boolean descending) {
        List<SiteInformation> siteInformations = super.getAllOrdered(orderProperties, descending);
        for (SiteInformation siteInformation : siteInformations) {
            decryptSiteInformation(siteInformation);
        }
        return siteInformations;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteInformation> getAllMatchingOrdered(String propertyName, Object propertyValue, String orderProperty,
            boolean descending) {
        List<SiteInformation> siteInformations = super.getAllMatchingOrdered(propertyName, propertyValue, orderProperty,
                descending);
        for (SiteInformation siteInformation : siteInformations) {
            decryptSiteInformation(siteInformation);
        }
        return siteInformations;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteInformation> getAllMatchingOrdered(String propertyName, Object propertyValue,
            List<String> orderProperties, boolean descending) {
        List<SiteInformation> siteInformations = super.getAllMatchingOrdered(propertyName, propertyValue,
                orderProperties, descending);
        for (SiteInformation siteInformation : siteInformations) {
            decryptSiteInformation(siteInformation);
        }
        return siteInformations;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteInformation> getAllMatchingOrdered(Map<String, Object> propertyValues, String orderProperty,
            boolean descending) {
        List<SiteInformation> siteInformations = super.getAllMatchingOrdered(propertyValues, orderProperty, descending);
        for (SiteInformation siteInformation : siteInformations) {
            decryptSiteInformation(siteInformation);
        }
        return siteInformations;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteInformation> getAllMatchingOrdered(Map<String, Object> propertyValues, List<String> orderProperties,
            boolean descending) {
        List<SiteInformation> siteInformations = super.getAllMatchingOrdered(propertyValues, orderProperties,
                descending);
        for (SiteInformation siteInformation : siteInformations) {
            decryptSiteInformation(siteInformation);
        }
        return siteInformations;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteInformation> getPage(int startingRecNo) {
        List<SiteInformation> siteInformations = super.getPage(startingRecNo);
        for (SiteInformation siteInformation : siteInformations) {
            decryptSiteInformation(siteInformation);
        }
        return siteInformations;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteInformation> getMatchingPage(String propertyName, Object propertyValue, int startingRecNo) {
        List<SiteInformation> siteInformations = super.getMatchingPage(propertyName, propertyValue, startingRecNo);
        for (SiteInformation siteInformation : siteInformations) {
            decryptSiteInformation(siteInformation);
        }
        return siteInformations;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteInformation> getMatchingPage(Map<String, Object> propertyValues, int startingRecNo) {
        List<SiteInformation> siteInformations = super.getMatchingPage(propertyValues, startingRecNo);
        for (SiteInformation siteInformation : siteInformations) {
            decryptSiteInformation(siteInformation);
        }
        return siteInformations;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteInformation> getOrderedPage(String orderProperty, boolean descending, int startingRecNo) {
        List<SiteInformation> siteInformations = super.getOrderedPage(orderProperty, descending, startingRecNo);
        for (SiteInformation siteInformation : siteInformations) {
            decryptSiteInformation(siteInformation);
        }
        return siteInformations;
    }

    @Override
    public List<SiteInformation> getOrderedPage(List<String> orderProperties, boolean descending, int startingRecNo) {
        List<SiteInformation> siteInformations = super.getOrderedPage(orderProperties, descending, startingRecNo);
        for (SiteInformation siteInformation : siteInformations) {
            decryptSiteInformation(siteInformation);
        }
        return siteInformations;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteInformation> getMatchingOrderedPage(String propertyName, Object propertyValue, String orderProperty,
            boolean descending, int startingRecNo) {
        List<SiteInformation> siteInformations = super.getMatchingOrderedPage(propertyName, propertyValue,
                orderProperty, descending, startingRecNo);
        for (SiteInformation siteInformation : siteInformations) {
            decryptSiteInformation(siteInformation);
        }
        return siteInformations;
    }

    @Override
    public List<SiteInformation> getMatchingOrderedPage(String propertyName, Object propertyValue,
            List<String> orderProperties, boolean descending, int startingRecNo) {
        List<SiteInformation> siteInformations = super.getMatchingOrderedPage(propertyName, propertyValue,
                orderProperties, descending, startingRecNo);
        for (SiteInformation siteInformation : siteInformations) {
            decryptSiteInformation(siteInformation);
        }
        return siteInformations;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteInformation> getMatchingOrderedPage(Map<String, Object> propertyValues, String orderProperty,
            boolean descending, int startingRecNo) {
        List<SiteInformation> siteInformations = super.getMatchingOrderedPage(propertyValues, orderProperty, descending,
                startingRecNo);
        for (SiteInformation siteInformation : siteInformations) {
            decryptSiteInformation(siteInformation);
        }
        return siteInformations;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteInformation> getMatchingOrderedPage(Map<String, Object> propertyValues,
            List<String> orderProperties, boolean descending, int startingRecNo) {
        List<SiteInformation> siteInformations = super.getMatchingOrderedPage(propertyValues, orderProperties,
                descending, startingRecNo);
        for (SiteInformation siteInformation : siteInformations) {
            decryptSiteInformation(siteInformation);
        }
        return siteInformations;
    }

    @Override
    @Transactional
    public String insert(SiteInformation siteInformation) {
        encryptSiteInformation(siteInformation);
        return super.insert(siteInformation);
    }

    @Override
    @Transactional
    public List<String> insertAll(List<SiteInformation> siteInformations) {
        List<String> ids = new ArrayList<>();
        for (SiteInformation siteInformation : siteInformations) {
            ids.add(insert(siteInformation));
        }
        return ids;
    }

    @Override
    @Transactional
    public SiteInformation save(SiteInformation siteInformation) {
        if ((siteInformation.getId() instanceof String && GenericValidator.isBlankOrNull(siteInformation.getId()))
                || null == siteInformation.getId()) {
            return get(insert(siteInformation));
        } else {
            return update(siteInformation);
        }
    }

    @Override
    @Transactional
    public List<SiteInformation> saveAll(List<SiteInformation> siteInformations) {
        List<SiteInformation> resultingSiteInformations = new ArrayList<>();
        for (SiteInformation siteInformation : siteInformations) {
            resultingSiteInformations.add(this.save(siteInformation));
        }
        return resultingSiteInformations;
    }

    @Override
    @Transactional
    public SiteInformation update(SiteInformation siteInformation) {
        encryptSiteInformation(siteInformation);
        return super.update(siteInformation);
    }

    @Override
    @Transactional
    public List<SiteInformation> updateAll(List<SiteInformation> siteInformations) {
        List<SiteInformation> resultsiteInformations = new ArrayList<>();
        for (SiteInformation siteInformation : siteInformations) {
            resultsiteInformations.add(update(siteInformation));
        }
        return resultsiteInformations;
    }

    @Override
    @Transactional
    public SiteInformation getNext(String id) {
        SiteInformation siteInformation = super.getNext(id);
        decryptSiteInformation(siteInformation);
        return siteInformation;
    }

    @Override
    @Transactional
    public SiteInformation getPrevious(String id) {
        SiteInformation siteInformation = super.getPrevious(id);
        decryptSiteInformation(siteInformation);
        return siteInformation;
    }

    private void decryptSiteInformation(SiteInformation siteInformation) {
        // detaching object so the decryption does not get written to the database
        entityManager.detach(siteInformation);
        if (siteInformation.isEncrypted() && !GenericValidator.isBlankOrNull(siteInformation.getValue())) {
            siteInformation.setValue(encryptor.decrypt(siteInformation.getValue()));
        }
    }

    private void encryptSiteInformation(SiteInformation siteInformation) {
        if (siteInformation.isEncrypted()) {
            siteInformation.setValue(encryptor.encrypt(siteInformation.getValue()));
        }
    }

    @Override
    @Transactional
    public List<SiteInformation> updateSiteInformationByName(Map<String, String> map) {
        List<SiteInformation> updatedSiteInfomration = new ArrayList<>();
        for (Entry<String, String> entry : map.entrySet()) {
            SiteInformation si = getSiteInformationByName(entry.getKey());
            si.setValue(entry.getValue());
            updatedSiteInfomration.add(siteInformationDAO.update(si));
        }
        return updatedSiteInfomration;
    }
}
