package org.openelisglobal.coldstorage.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.common.dao.BaseDAO;

public interface FreezerDAO extends BaseDAO<Freezer, Long> {

    Optional<Freezer> findByName(String name);

    List<Freezer> findActiveFreezers();

    List<Freezer> getAllFreezers();

    List<Freezer> searchFreezers(String search);
}
