package org.openelisglobal.eqa.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.eqa.dao.EQAParticipantFollowupDAO;
import org.openelisglobal.eqa.valueholder.EQAParticipantFollowup;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQAParticipantFollowupDAOImpl extends BaseDAOImpl<EQAParticipantFollowup, Long>
        implements EQAParticipantFollowupDAO {

    public EQAParticipantFollowupDAOImpl() {
        super(EQAParticipantFollowup.class);
    }
}
