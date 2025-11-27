package org.openelisglobal.coldstorage.dao;

import java.util.Optional;
import org.openelisglobal.coldstorage.valueholder.ThresholdProfile;
import org.openelisglobal.common.dao.BaseDAO;

public interface ThresholdProfileDAO extends BaseDAO<ThresholdProfile, Long> {

    Optional<ThresholdProfile> findByName(String name);
}
