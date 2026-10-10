package org.openelisglobal.microbiology.daoimpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.microbiology.dao.MicroCaseSearchDAO;
import org.openelisglobal.microbiology.form.MicroCaseSearchForm;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class MicroCaseSearchDAOImpl implements MicroCaseSearchDAO {
    @PersistenceContext
    private EntityManager entityManager;
    private static final String SELECT = "select c, s.accessionNumber, u.testSectionName, p.id, person.lastName, person.firstName, p.birthDate, p.gender, t.description";
    private static final String FROM = " from MicroCase c left join SampleItem original on original.id = c.sampleItemId"
            + " left join Sample s on s.id = coalesce(c.sampleId, original.sample.id)"
            + " left join TestSection u on u.id = c.labUnitId" + " left join TypeOfSample t on t.id = c.sampleTypeId"
            + " left join SampleHuman h on h.sampleId = s.id left join Patient p on p.id = h.patientId left join p.person person";

    private Session session() {
        return entityManager.unwrap(Session.class);
    }

    private String where(MicroCaseSearchForm q, Set<String> ids) {
        String hql = " where c.labUnitId is not null";
        if (ids != null)
            hql += " and c.labUnitId in (:units)";
        if (q.labUnitId != null)
            hql += " and c.labUnitId = :unit";
        if (q.status != null)
            hql += " and c.status = :status";
        if (q.from != null)
            hql += " and c.createdAt >= :from";
        if (q.to != null)
            hql += " and c.createdAt < :to";
        if (q.patientId != null)
            hql += " and p.id = :patient";
        if (q.accessionNumber != null)
            hql += " and s.accessionNumber = :accession";
        if (q.q != null)
            hql += " and (lower(s.accessionNumber) like :search or lower(person.lastName) like :search or lower(person.firstName) like :search)";
        return hql;
    }

    private void bind(Query<?> query, MicroCaseSearchForm q, Set<String> ids) {
        if (ids != null)
            query.setParameterList("units", ids);
        if (q.labUnitId != null)
            query.setParameter("unit", q.labUnitId);
        if (q.status != null)
            query.setParameter("status", org.openelisglobal.microbiology.valueholder.MicroCaseStatus.valueOf(q.status));
        if (q.from != null)
            query.setParameter("from", Timestamp.valueOf(LocalDate.parse(q.from).atStartOfDay()));
        if (q.to != null)
            query.setParameter("to", Timestamp.valueOf(LocalDate.parse(q.to).plusDays(1).atStartOfDay()));
        if (q.patientId != null)
            query.setParameter("patient", q.patientId);
        if (q.accessionNumber != null)
            query.setParameter("accession", q.accessionNumber);
        if (q.q != null)
            query.setParameter("search", "%" + q.q.toLowerCase(java.util.Locale.ROOT) + "%");
    }

    @Override
    public List<Object[]> search(MicroCaseSearchForm q, Set<String> ids) {
        if (ids != null && ids.isEmpty())
            return List.of();
        String order = "accession".equals(q.sort) ? "s.accessionNumber asc, c.id" : "c.createdAt desc, c.id";
        Query<Object[]> query = session().createQuery(SELECT + FROM + where(q, ids) + " order by " + order,
                Object[].class);
        bind(query, q, ids);
        return query.setFirstResult((q.page - 1) * q.pageSize).setMaxResults(q.pageSize).list();
    }

    @Override
    public long count(MicroCaseSearchForm q, Set<String> ids) {
        if (ids != null && ids.isEmpty())
            return 0;
        Query<Long> query = session().createQuery("select count(c.id)" + FROM + where(q, ids), Long.class);
        bind(query, q, ids);
        return query.uniqueResult();
    }

    @Override
    public Object[] getSummary(String id) {
        return session().createQuery(SELECT + FROM + " where c.id = :id", Object[].class).setParameter("id", id)
                .uniqueResult();
    }

    @Override
    public List<SampleItem> getSamples(String id) {
        return session().createQuery(
                "select si from MicroCaseSample m join SampleItem si on si.id = m.sampleItemId join fetch si.sample left join fetch si.typeOfSample where m.caseId = :id and m.splitOutAt is null order by m.joinedAt, m.id",
                SampleItem.class).setParameter("id", id).list();
    }

    @Override
    public List<IdValuePair> getEligibleLabUnits() {
        return session().createQuery(
                "select distinct u.id, u.testSectionName from TestSection u join Test t on t.testSection.id = u.id where u.isActive = 'Y' and t.isActive = 'Y' and t.opensMicrobiologyCase = true order by u.testSectionName",
                Object[].class).list().stream().map(v -> new IdValuePair((String) v[0], (String) v[1])).toList();
    }
}
