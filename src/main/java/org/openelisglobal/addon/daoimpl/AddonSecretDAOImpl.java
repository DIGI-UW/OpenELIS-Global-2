package org.openelisglobal.addon.daoimpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.openelisglobal.addon.dao.AddonSecretDAO;
import org.openelisglobal.addon.valueholder.AddonSecret;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AddonSecretDAOImpl implements AddonSecretDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public String getOrCreate(String name, String candidate) {
        try {
            AddonSecret existing = entityManager.find(AddonSecret.class, name);
            if (existing != null) {
                return existing.getValue();
            }
            AddonSecret secret = new AddonSecret();
            secret.setId(name);
            secret.setValue(candidate);
            entityManager.persist(secret);
            entityManager.flush();
            return candidate;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AddonSecret getOrCreate()", e);
        }
    }
}
