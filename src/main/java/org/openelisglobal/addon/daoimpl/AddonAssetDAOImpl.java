package org.openelisglobal.addon.daoimpl;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.addon.dao.AddonAssetDAO;
import org.openelisglobal.addon.valueholder.AddonAsset;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AddonAssetDAOImpl extends BaseDAOImpl<AddonAsset, Long> implements AddonAssetDAO {

    public AddonAssetDAOImpl() {
        super(AddonAsset.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AddonAsset> find(String addonId, String path) {
        try {
            List<AddonAsset> found = entityManager
                    .createQuery("FROM AddonAsset a WHERE a.addonId = :addonId AND a.path = :path", AddonAsset.class)
                    .setParameter("addonId", addonId).setParameter("path", path).setMaxResults(1).getResultList();
            return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AddonAsset find()", e);
        }
    }

    @Override
    public void deleteAll(String addonId) {
        try {
            entityManager.createQuery("DELETE FROM AddonAsset a WHERE a.addonId = :addonId")
                    .setParameter("addonId", addonId).executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AddonAsset deleteAll()", e);
        }
    }

    @Override
    public void save(AddonAsset asset) {
        try {
            entityManager.persist(asset);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AddonAsset save()", e);
        }
    }
}
