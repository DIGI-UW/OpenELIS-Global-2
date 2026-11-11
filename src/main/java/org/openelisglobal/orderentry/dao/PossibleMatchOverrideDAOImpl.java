package org.openelisglobal.orderentry.dao;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.orderentry.valueholder.PossibleMatchOverride;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class PossibleMatchOverrideDAOImpl extends BaseDAOImpl<PossibleMatchOverride, Integer>
        implements PossibleMatchOverrideDAO {

    public PossibleMatchOverrideDAOImpl() {
        super(PossibleMatchOverride.class);
    }
}
