package org.openelisglobal.orderentry.dao;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.orderentry.valueholder.OrderTestTestedElsewhere;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class OrderTestTestedElsewhereDAOImpl extends BaseDAOImpl<OrderTestTestedElsewhere, Integer>
        implements OrderTestTestedElsewhereDAO {

    public OrderTestTestedElsewhereDAOImpl() {
        super(OrderTestTestedElsewhere.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderTestTestedElsewhere> forSample(Integer sampleId) {
        return getAllMatching("sampleId", sampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderTestTestedElsewhere> forSampleAndTest(Integer sampleId, Integer testId) {
        return getAllMatching(Map.of("sampleId", sampleId, "testId", testId)).stream().findFirst();
    }
}
