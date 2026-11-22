package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroReportVersionSourceDAO;
import org.openelisglobal.microbiology.valueholder.MicroReportVersionSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroReportVersionSourceDAOImpl extends BaseDAOImpl<MicroReportVersionSource, String>
        implements MicroReportVersionSourceDAO {

    public MicroReportVersionSourceDAOImpl() {
        super(MicroReportVersionSource.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroReportVersionSource> getByReportVersionId(String reportVersionId) {
        Query<MicroReportVersionSource> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroReportVersionSource s where s.reportVersionId = :reportVersionId order by s.id",
                MicroReportVersionSource.class);
        query.setParameter("reportVersionId", reportVersionId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroReportVersionSource> getByCaseId(String caseId) {
        Query<MicroReportVersionSource> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroReportVersionSource s where s.reportVersionId in "
                        + "(select v.id from MicroReportVersion v where v.caseId = :caseId) "
                        + "order by s.reportVersionId, s.id", MicroReportVersionSource.class);
        query.setParameter("caseId", caseId);
        return query.list();
    }
}
