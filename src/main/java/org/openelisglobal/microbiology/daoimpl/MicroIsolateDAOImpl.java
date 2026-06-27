package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroIsolateDAOImpl extends BaseDAOImpl<MicroIsolate, String> implements MicroIsolateDAO {

    public MicroIsolateDAOImpl() {
        super(MicroIsolate.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroIsolate> getByCaseId(String caseId) {
        Query<MicroIsolate> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroIsolate i where i.caseId = :caseId" + " order by i.isolateLabel", MicroIsolate.class);
        query.setParameter("caseId", caseId);
        return query.list();
    }
}
