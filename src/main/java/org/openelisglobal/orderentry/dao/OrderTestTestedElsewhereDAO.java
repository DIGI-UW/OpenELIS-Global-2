package org.openelisglobal.orderentry.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.orderentry.valueholder.OrderTestTestedElsewhere;

public interface OrderTestTestedElsewhereDAO extends BaseDAO<OrderTestTestedElsewhere, Integer> {

    List<OrderTestTestedElsewhere> forSample(Integer sampleId);

    Optional<OrderTestTestedElsewhere> forSampleAndTest(Integer sampleId, Integer testId);
}
