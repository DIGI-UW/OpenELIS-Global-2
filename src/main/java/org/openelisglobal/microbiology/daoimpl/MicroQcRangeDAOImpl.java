package org.openelisglobal.microbiology.daoimpl;

import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroQcRangeDAO;
import org.openelisglobal.microbiology.valueholder.MicroQcRange;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroQcRangeDAOImpl extends BaseDAOImpl<MicroQcRange, String> implements MicroQcRangeDAO {

    public MicroQcRangeDAOImpl() {
        super(MicroQcRange.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MicroQcRange> find(String qcStrain, String antibioticId, String method, String diskContent,
            String standardId) {
        String diskContentMatch = diskContent == null ? "r.diskContent is null" : "r.diskContent = :diskContent";
        Query<MicroQcRange> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroQcRange r where lower(r.qcStrain) = lower(:qcStrain)"
                        + " and r.antibioticId = :antibioticId and r.method = :method and " + diskContentMatch
                        + " and r.standardId = :standardId", MicroQcRange.class);
        query.setParameter("qcStrain", qcStrain);
        query.setParameter("antibioticId", antibioticId);
        query.setParameter("method", method);
        query.setParameter("standardId", standardId);
        if (diskContent != null) {
            query.setParameter("diskContent", diskContent);
        }
        query.setMaxResults(1);
        return query.uniqueResultOptional();
    }
}
