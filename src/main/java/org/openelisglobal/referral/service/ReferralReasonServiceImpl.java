package org.openelisglobal.referral.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.referral.dao.ReferralReasonDAO;
import org.openelisglobal.referral.valueholder.ReferralReason;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReferralReasonServiceImpl extends AuditableBaseObjectServiceImpl<ReferralReason, String>
        implements ReferralReasonService {
    @Autowired
    protected ReferralReasonDAO baseObjectDAO;

    ReferralReasonServiceImpl() {
        super(ReferralReason.class);
    }

    @Override
    protected ReferralReasonDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferralReason> getAllReferralReasons() {
        return getBaseObjectDAO().getAllReferralReasons();
    }
}
