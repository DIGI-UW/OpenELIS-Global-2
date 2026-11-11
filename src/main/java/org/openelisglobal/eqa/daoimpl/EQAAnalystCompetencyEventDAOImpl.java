package org.openelisglobal.eqa.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.eqa.dao.EQAAnalystCompetencyEventDAO;
import org.openelisglobal.eqa.valueholder.EQAAnalystCompetencyEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQAAnalystCompetencyEventDAOImpl extends BaseDAOImpl<EQAAnalystCompetencyEvent, Long>
        implements EQAAnalystCompetencyEventDAO {

    public EQAAnalystCompetencyEventDAOImpl() {
        super(EQAAnalystCompetencyEvent.class);
    }
}
