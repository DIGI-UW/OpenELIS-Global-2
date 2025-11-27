package org.openelisglobal.coldstorage.dao;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.coldstorage.valueholder.FreezerReading;
import org.openelisglobal.common.dao.BaseDAO;

public interface FreezerReadingDAO extends BaseDAO<FreezerReading, Long> {

    Optional<FreezerReading> findLatestByFreezer(Long freezerId);

    List<FreezerReading> findRecentByFreezer(Long freezerId, int limit);

    List<FreezerReading> findByFreezerWithin(Long freezerId, OffsetDateTime start, OffsetDateTime end);
}
