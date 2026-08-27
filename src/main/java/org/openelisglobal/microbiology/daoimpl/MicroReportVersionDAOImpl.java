package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroReportVersionDAO;
import org.openelisglobal.microbiology.valueholder.MicroReportVersion;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroReportVersionDAOImpl extends BaseDAOImpl<MicroReportVersion, String>
        implements MicroReportVersionDAO {

    public MicroReportVersionDAOImpl() {
        super(MicroReportVersion.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroReportVersion> getByCaseId(String caseId) {
        Query<MicroReportVersion> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroReportVersion v where v.caseId = :caseId order by v.versionNumber",
                MicroReportVersion.class);
        query.setParameter("caseId", caseId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public MicroReportVersion getLatestByCaseId(String caseId) {
        Query<MicroReportVersion> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroReportVersion v where v.caseId = :caseId order by v.versionNumber desc",
                MicroReportVersion.class);
        query.setParameter("caseId", caseId);
        query.setMaxResults(1);
        return query.uniqueResultOptional().orElse(null);
    }
}
