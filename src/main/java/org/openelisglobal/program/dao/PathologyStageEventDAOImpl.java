package org.openelisglobal.program.dao;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.program.valueholder.pathology.PathologyStageEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class PathologyStageEventDAOImpl extends BaseDAOImpl<PathologyStageEvent, Integer>
        implements PathologyStageEventDAO {

    PathologyStageEventDAOImpl() {
        super(PathologyStageEvent.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PathologyStageEvent> getForCase(Integer pathologySampleId) {
        Query<PathologyStageEvent> query = entityManager.unwrap(Session.class).createQuery(
                "from PathologyStageEvent e where e.pathologySampleId = :caseId order by e.enteredAt, e.id",
                PathologyStageEvent.class);
        query.setParameter("caseId", pathologySampleId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PathologyStageEvent> getOpenEvent(Integer pathologySampleId, PathologyStatus stage) {
        Query<PathologyStageEvent> query = entityManager.unwrap(Session.class)
                .createQuery(
                        "from PathologyStageEvent e where e.pathologySampleId = :caseId and e.stage = :stage"
                                + " and e.completedAt is null order by e.enteredAt desc, e.id desc",
                        PathologyStageEvent.class);
        query.setParameter("caseId", pathologySampleId);
        query.setParameter("stage", stage);
        query.setMaxResults(1);
        return query.list().stream().findFirst();
    }
}
