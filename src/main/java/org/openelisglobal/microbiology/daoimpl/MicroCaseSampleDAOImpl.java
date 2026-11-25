package org.openelisglobal.microbiology.daoimpl;

import jakarta.persistence.LockModeType;
import java.util.List;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroCaseSampleDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseSample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroCaseSampleDAOImpl extends BaseDAOImpl<MicroCaseSample, String> implements MicroCaseSampleDAO {
    public MicroCaseSampleDAOImpl() {
        super(MicroCaseSample.class);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCaseSample getActiveByCaseAndSample(String caseId, String sampleItemId) {
        Query<MicroCaseSample> query = entityManager.unwrap(org.hibernate.Session.class).createQuery(
                "from MicroCaseSample m where m.caseId = :caseId and m.sampleItemId = :sampleItemId and m.splitOutAt is null",
                MicroCaseSample.class);
        query.setParameter("caseId", caseId);
        query.setParameter("sampleItemId", sampleItemId);
        return query.uniqueResultOptional().orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseSample> getByCaseId(String caseId) {
        Query<MicroCaseSample> query = entityManager.unwrap(org.hibernate.Session.class).createQuery(
                "from MicroCaseSample m where m.caseId = :caseId order by m.joinedAt, m.id", MicroCaseSample.class);
        query.setParameter("caseId", caseId);
        return query.list();
    }

    @Override
    public SampleItem lockSampleItem(String sampleItemId) {
        return entityManager.find(SampleItem.class, sampleItemId, LockModeType.PESSIMISTIC_WRITE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getRelatedCaseIds(String caseId) {
        return entityManager.createQuery(
                "select distinct other.caseId from MicroCaseSample own join MicroCaseSample other on own.sampleItemId = other.sampleItemId where own.caseId = :caseId and other.caseId <> :caseId and own.splitOutAt is null and other.splitOutAt is null",
                String.class).setParameter("caseId", caseId).getResultList();
    }
}
