package org.openelisglobal.addon.daoimpl;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.addon.dao.AddonDAO;
import org.openelisglobal.addon.valueholder.Addon;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AddonDAOImpl extends BaseDAOImpl<Addon, String> implements AddonDAO {

    public AddonDAOImpl() {
        super(Addon.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Addon> findAllOrdered() {
        try {
            return entityManager.createQuery("FROM Addon a ORDER BY a.name, a.id", Addon.class).getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Addon findAllOrdered()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Addon> findByApiKeyHash(String apiKeyHash) {
        try {
            List<Addon> found = entityManager.createQuery("FROM Addon a WHERE a.apiKeyHash = :hash", Addon.class)
                    .setParameter("hash", apiKeyHash).setMaxResults(1).getResultList();
            return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Addon findByApiKeyHash()", e);
        }
    }

    @Override
    public Addon save(Addon addon) {
        try {
            if (entityManager.find(Addon.class, addon.getId()) == null) {
                entityManager.persist(addon);
                return addon;
            }
            return entityManager.merge(addon);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Addon save()", e);
        }
    }

    @Override
    public void remove(Addon addon) {
        try {
            Addon managed = entityManager.find(Addon.class, addon.getId());
            if (managed != null) {
                entityManager.remove(managed);
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Addon remove()", e);
        }
    }
}
