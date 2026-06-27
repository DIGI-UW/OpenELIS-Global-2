package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroCriticalCommunicationDAO;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroCriticalCommunicationDAOImpl extends BaseDAOImpl<MicroCriticalCommunication, String>
        implements MicroCriticalCommunicationDAO {

    public MicroCriticalCommunicationDAOImpl() {
        super(MicroCriticalCommunication.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCriticalCommunication> getByCaseId(String caseId) {
        Query<MicroCriticalCommunication> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroCriticalCommunication c where c.caseId = :caseId order by c.communicatedAt",
                MicroCriticalCommunication.class);
        query.setParameter("caseId", caseId);
        return query.list();
    }
}
