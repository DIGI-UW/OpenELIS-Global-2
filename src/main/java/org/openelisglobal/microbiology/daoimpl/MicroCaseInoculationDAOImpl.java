package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroCaseInoculationDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseInoculation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroCaseInoculationDAOImpl extends BaseDAOImpl<MicroCaseInoculation, String>
        implements MicroCaseInoculationDAO {

    public MicroCaseInoculationDAOImpl() {
        super(MicroCaseInoculation.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseInoculation> getByCaseId(String caseId) {
        Query<MicroCaseInoculation> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroCaseInoculation i where i.caseId = :caseId order by i.occurredAt, i.id",
                MicroCaseInoculation.class);
        query.setParameter("caseId", caseId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseInoculation> getByContainerIdentifier(String containerIdentifier) {
        Query<MicroCaseInoculation> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroCaseInoculation i where i.containerIdentifier = :containerIdentifier order by i.id",
                MicroCaseInoculation.class);
        query.setParameter("containerIdentifier", containerIdentifier);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseInoculation> getRows(String caseId) {
        // Separate collection fetches avoid a Cartesian product and multiple-bag fetch
        // errors.
        var rows = entityManager.createQuery(
                "select distinct i from MicroCaseInoculation i left join fetch i.readings where i.caseId = :caseId order by i.occurredAt, i.id",
                MicroCaseInoculation.class).setParameter("caseId", caseId).getResultList();
        entityManager.createQuery(
                "select distinct i from MicroCaseInoculation i left join fetch i.extensions where i.caseId = :caseId",
                MicroCaseInoculation.class).setParameter("caseId", caseId).getResultList();
        entityManager.createQuery(
                "select distinct i from MicroCaseInoculation i left join fetch i.proposals where i.caseId = :caseId",
                MicroCaseInoculation.class).setParameter("caseId", caseId).getResultList();
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<org.openelisglobal.sampleitem.valueholder.SampleItem> getSources(String caseId) {
        var sources = entityManager.createQuery(
                "select si from SampleItem si join fetch si.sample left join fetch si.typeOfSample left join fetch si.parentSampleItem where si.id in (select m.sampleItemId from MicroCaseSample m where m.caseId = :caseId and m.splitOutAt is null)",
                org.openelisglobal.sampleitem.valueholder.SampleItem.class).setParameter("caseId", caseId)
                .getResultList();
        java.util.Set<String> visited = new java.util.HashSet<>();
        var frontier = sources.stream().map(s -> s.getId()).toList();
        while (!frontier.isEmpty()) {
            visited.addAll(frontier);
            var children = entityManager.createQuery(
                    "select si from SampleItem si join fetch si.sample left join fetch si.typeOfSample join fetch si.parentSampleItem p where p.id in (:parents)",
                    org.openelisglobal.sampleitem.valueholder.SampleItem.class).setParameter("parents", frontier)
                    .getResultList().stream().filter(s -> !visited.contains(s.getId())).toList();
            sources.addAll(children);
            frontier = children.stream().map(s -> s.getId()).toList();
        }
        return sources;
    }
}
