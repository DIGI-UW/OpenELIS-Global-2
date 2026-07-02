package org.openelisglobal.batchworkplan.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplan;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanStatus;
import org.openelisglobal.common.dao.BaseDAO;

public interface BatchWorkplanDAO extends BaseDAO<BatchWorkplan, Long> {

    List<BatchWorkplan> getAllWithItems();

    Optional<BatchWorkplan> getWithItems(Long id);

    List<BatchWorkplan> getByStatuses(List<BatchWorkplanStatus> statuses);
}
