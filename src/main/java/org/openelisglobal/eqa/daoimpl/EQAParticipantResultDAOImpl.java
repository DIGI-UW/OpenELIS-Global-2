package org.openelisglobal.eqa.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.eqa.dao.EQAParticipantResultDAO;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQAParticipantResultDAOImpl extends BaseDAOImpl<EQAParticipantResult, Long>
        implements EQAParticipantResultDAO {

    public EQAParticipantResultDAOImpl() {
        super(EQAParticipantResult.class);
    }
}
