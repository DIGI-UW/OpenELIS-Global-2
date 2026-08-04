package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroWhonetExportRunDAO;
import org.openelisglobal.microbiology.valueholder.MicroWhonetExportRun;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroWhonetExportRunDAOImpl extends BaseDAOImpl<MicroWhonetExportRun, String>
        implements MicroWhonetExportRunDAO {

    public MicroWhonetExportRunDAOImpl() {
        super(MicroWhonetExportRun.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroWhonetExportRun> getRecent(int limit) {
        Query<MicroWhonetExportRun> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroWhonetExportRun r order by r.generatedAt desc, r.id desc", MicroWhonetExportRun.class);
        query.setMaxResults(Math.max(1, Math.min(100, limit)));
        return query.list();
    }
}
