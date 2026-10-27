package org.openelisglobal.addon.dao;

import java.util.Optional;
import org.openelisglobal.addon.valueholder.AddonAsset;
import org.openelisglobal.common.dao.BaseDAO;

public interface AddonAssetDAO extends BaseDAO<AddonAsset, Long> {

    Optional<AddonAsset> find(String addonId, String path);

    void deleteAll(String addonId);

    void save(AddonAsset asset);
}
