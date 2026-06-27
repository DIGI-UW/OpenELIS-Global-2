package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroAstRunDAOImpl extends BaseDAOImpl<MicroAstRun, String> implements MicroAstRunDAO {

    public MicroAstRunDAOImpl() {
        super(MicroAstRun.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstRun> getByIsolateId(String isolateId) {
        Query<MicroAstRun> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroAstRun r where r.isolateId = :isolateId order by r.startedAt", MicroAstRun.class);
        query.setParameter("isolateId", isolateId);
        return query.list();
    }
}
