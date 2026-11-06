package org.openelisglobal.microbiology.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroCultureProposalDAO;
import org.openelisglobal.microbiology.valueholder.MicroCultureProposal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroCultureProposalDAOImpl extends BaseDAOImpl<MicroCultureProposal, String>
        implements MicroCultureProposalDAO {
    public MicroCultureProposalDAOImpl() {
        super(MicroCultureProposal.class);
    }
}
