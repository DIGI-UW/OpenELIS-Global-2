package org.openelisglobal.dashboard.management.quality;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class QiWeightsDAOImpl extends BaseDAOImpl<QiWeights, Long> implements QiWeightsDAO {

    public QiWeightsDAOImpl() {
        super(QiWeights.class);
    }
}
