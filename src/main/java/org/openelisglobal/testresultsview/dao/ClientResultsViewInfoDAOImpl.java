package org.openelisglobal.testresultsview.dao;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.testresultsview.valueholder.ClientResultsViewBean;
import org.springframework.stereotype.Component;

@Component
public class ClientResultsViewInfoDAOImpl extends BaseDAOImpl<ClientResultsViewBean, Integer>
        implements ClientResultsViewInfoDAO {

    public ClientResultsViewInfoDAOImpl() {
        super(ClientResultsViewBean.class);
    }
}
