package org.openelisglobal.addon.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.addon.valueholder.Addon;
import org.openelisglobal.common.dao.BaseDAO;

public interface AddonDAO extends BaseDAO<Addon, String> {

    List<Addon> findAllOrdered();

    Optional<Addon> findByApiKeyHash(String apiKeyHash);

    Addon save(Addon addon);

    void remove(Addon addon);
}
