package org.openelisglobal.dataexchange.externalorders.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.dataexchange.externalorders.valueholder.IncomingOrder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class IncomingOrderDAOImpl extends BaseDAOImpl<IncomingOrder, Integer> implements IncomingOrderDAO {

    public IncomingOrderDAOImpl() {
        super(IncomingOrder.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IncomingOrder> getByExternalOrderNumber(String externalOrderNumber) {
        if (externalOrderNumber == null || externalOrderNumber.trim().isEmpty()) {
            return Optional.empty();
        }

        List<IncomingOrder> result = entityManager
                .createQuery(
                        "from IncomingOrder e where e.externalOrderNumber = :externalOrderNumber",
                        IncomingOrder.class)
                .setParameter("externalOrderNumber", externalOrderNumber)
                .setMaxResults(1)
                .getResultList();

        return result == null || result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }
}
