package org.openelisglobal.microbiology.daoimpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.openelisglobal.microbiology.dao.MicroCultureDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseInoculation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroCultureDAOImpl implements MicroCultureDAO {
    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseInoculation> getRows(String caseId) {
        // Separate collection fetches avoid a Cartesian product and multiple-bag fetch
        // errors.
        var rows = em.createQuery(
                "select distinct i from MicroCaseInoculation i left join fetch i.readings where i.caseId = :caseId order by i.occurredAt, i.id",
                MicroCaseInoculation.class).setParameter("caseId", caseId).getResultList();
        em.createQuery(
                "select distinct i from MicroCaseInoculation i left join fetch i.extensions where i.caseId = :caseId",
                MicroCaseInoculation.class).setParameter("caseId", caseId).getResultList();
        em.createQuery(
                "select distinct i from MicroCaseInoculation i left join fetch i.proposals where i.caseId = :caseId",
                MicroCaseInoculation.class).setParameter("caseId", caseId).getResultList();
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<org.openelisglobal.sampleitem.valueholder.SampleItem> getSources(String caseId) {
        var sources = em.createQuery(
                "select si from SampleItem si join fetch si.sample left join fetch si.typeOfSample left join fetch si.parentSampleItem where si.id in (select m.sampleItemId from MicroCaseSample m where m.caseId = :caseId and m.splitOutAt is null)",
                org.openelisglobal.sampleitem.valueholder.SampleItem.class).setParameter("caseId", caseId)
                .getResultList();
        java.util.Set<String> visited = new java.util.HashSet<>();
        var frontier = sources.stream().map(s -> s.getId()).toList();
        while (!frontier.isEmpty()) {
            visited.addAll(frontier);
            var children = em.createQuery(
                    "select si from SampleItem si join fetch si.sample left join fetch si.typeOfSample join fetch si.parentSampleItem p where p.id in (:parents)",
                    org.openelisglobal.sampleitem.valueholder.SampleItem.class).setParameter("parents", frontier)
                    .getResultList().stream().filter(s -> !visited.contains(s.getId())).toList();
            sources.addAll(children);
            frontier = children.stream().map(s -> s.getId()).toList();
        }
        return sources;
    }

    @Override
    public void insert(Object entity) {
        em.persist(entity);
        em.flush();
    }
}
