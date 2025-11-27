package org.openelisglobal.coldstorage.dao;

import java.time.OffsetDateTime;
import java.util.List;
import org.openelisglobal.coldstorage.valueholder.FreezerThresholdProfile;
import org.openelisglobal.common.dao.BaseDAO;

public interface FreezerThresholdProfileDAO extends BaseDAO<FreezerThresholdProfile, Long> {

    List<FreezerThresholdProfile> findActiveAssignments(Long freezerId, OffsetDateTime at);
}
