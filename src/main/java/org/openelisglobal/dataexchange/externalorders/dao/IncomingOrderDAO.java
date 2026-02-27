package org.openelisglobal.dataexchange.externalorders.dao;

import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.dataexchange.externalorders.valueholder.IncomingOrder;

public interface IncomingOrderDAO extends BaseDAO<IncomingOrder, Integer> {

    Optional<IncomingOrder> getByExternalOrderNumber(String externalOrderNumber);
}
