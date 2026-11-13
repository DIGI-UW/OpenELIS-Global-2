package org.openelisglobal.dashboard.management.surveillance.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.dashboard.management.surveillance.dao.SurveillanceProgramDAO;
import org.openelisglobal.dashboard.management.surveillance.valueholder.SurveillanceProgram;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class SurveillanceProgramDAOImpl extends BaseDAOImpl<SurveillanceProgram, Long>
        implements SurveillanceProgramDAO {

    public SurveillanceProgramDAOImpl() {
        super(SurveillanceProgram.class);
    }
}
