package org.openelisglobal.program.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.program.valueholder.pathology.PathologyConsultation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class PathologyConsultationDAOImpl extends BaseDAOImpl<PathologyConsultation, Integer>
        implements PathologyConsultationDAO {

    PathologyConsultationDAOImpl() {
        super(PathologyConsultation.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PathologyConsultation> getForCase(Integer pathologySampleId) {
        Query<PathologyConsultation> query = entityManager.unwrap(Session.class).createQuery(
                "from PathologyConsultation c where c.pathologySampleId = :caseId order by c.requestedAt desc, c.id desc",
                PathologyConsultation.class);
        query.setParameter("caseId", pathologySampleId);
        return query.list();
    }
}
