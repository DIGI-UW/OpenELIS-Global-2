package org.openelisglobal.referral.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.referral.dao.ReferralTypeDAO;
import org.openelisglobal.referral.valueholder.ReferralType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReferralTypeServiceImpl extends AuditableBaseObjectServiceImpl<ReferralType, String>
        implements ReferralTypeService {
    @Autowired
    protected ReferralTypeDAO baseObjectDAO;

    ReferralTypeServiceImpl() {
        super(ReferralType.class);
    }

    @Override
    protected ReferralTypeDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public ReferralType getReferralTypeByName(String name) {
        return getMatch("name", name).orElse(null);
    }
}
