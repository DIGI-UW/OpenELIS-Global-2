package org.openelisglobal.eqa.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.eqa.dao.EQAPanelReceiptDAO;
import org.openelisglobal.eqa.valueholder.EQAPanelReceipt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EQAPanelReceiptDAOImpl extends BaseDAOImpl<EQAPanelReceipt, Long> implements EQAPanelReceiptDAO {

    public EQAPanelReceiptDAOImpl() {
        super(EQAPanelReceipt.class);
    }
}
