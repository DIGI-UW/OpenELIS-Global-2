package org.openelisglobal.lesotho.clinicalinfo.daoimpl;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.lesotho.clinicalinfo.dao.OrderClinicalInfoDAO;
import org.openelisglobal.lesotho.clinicalinfo.valueholder.OrderClinicalInfo;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class OrderClinicalInfoDAOImpl extends BaseDAOImpl<OrderClinicalInfo, Integer> implements OrderClinicalInfoDAO {

    public OrderClinicalInfoDAOImpl() {
        super(OrderClinicalInfo.class);
    }

    @Override
    public Optional<OrderClinicalInfo> getLatestBySampleId(String sampleId) {
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<OrderClinicalInfo> query = session.createQuery(
                    "FROM OrderClinicalInfo c WHERE c.sampleId = :sampleId ORDER BY c.id DESC",
                    OrderClinicalInfo.class);
            query.setParameter("sampleId", sampleId);
            query.setMaxResults(1);
            List<OrderClinicalInfo> rows = query.getResultList();
            return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
        } catch (Exception e) {
            handleException(e, "getLatestBySampleId");
            return Optional.empty();
        }
    }
}
