package org.openelisglobal.sample.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.sample.dao.SampleComplianceStandardDAO;
import org.openelisglobal.sample.valueholder.SampleComplianceStandard;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class SampleComplianceStandardDAOImpl extends BaseDAOImpl<SampleComplianceStandard, Long>
        implements SampleComplianceStandardDAO {

    public SampleComplianceStandardDAOImpl() {
        super(SampleComplianceStandard.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleComplianceStandard> getAllForSample(String sampleId) {
        String sql = "from SampleComplianceStandard s join fetch s.complianceStandard where s.sample.id = :sampleId order by s.priority asc";
        Query<SampleComplianceStandard> query = entityManager.unwrap(Session.class).createQuery(sql,
                SampleComplianceStandard.class);
        query.setParameter("sampleId", sampleId);
        return query.list();
    }

    @Override
    public void deleteAllForSample(String sampleId) {
        String hql = "delete from SampleComplianceStandard s where s.sample.id = :sampleId";
        entityManager.unwrap(Session.class).createQuery(hql).setParameter("sampleId", sampleId).executeUpdate();
    }
}
