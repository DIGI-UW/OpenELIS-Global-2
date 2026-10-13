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

    private static final String CULTURES = " left join MicroCaseInoculation r on r.caseId = c.id and r.cancelledAt is null"
            + " and (r.positiveSource is null or r.positiveSource <> 'RECEIVED_ISOLATE')";
    private static final String ACTIVE = "(r.duration is not null and r.durationUnit is not null and (r.outcome is null or r.outcome = 'GROWTH'))";

    private static String seconds(String value, String unit) {
        return "(" + value + " * case " + unit
                + " when 'DAYS' then 86400.0 when 'WEEKS' then 604800.0 else 3600.0 end)";
    }

    private static String epoch(String timestamp) {
        return "cast(function('date_part', 'epoch', " + timestamp + ") as double)";
    }

    private static final String END = "(" + epoch("r.occurredAt") + " + " + seconds("r.duration", "r.durationUnit")
            + " + coalesce((select sum(" + seconds("e.extendBy", "e.unit")
            + ") from MicroCultureExtension e where e.inoculation.id = r.id and e.undoneAt is null), 0.0))";
    private static final String CHECK = "(" + epoch("r.occurredAt") + " + (floor((coalesce((select max("
            + epoch("reading.recordedAt") + ") from MicroCultureReading reading where reading.inoculation.id = r.id), "
            + epoch("r.occurredAt") + ") - " + epoch("r.occurredAt")
            + ") / nullif(r.checkIntervalHours * 3600.0, 0.0)) + 1.0) * r.checkIntervalHours * 3600.0)";
    private static final String DUE = "case when " + ACTIVE + " then case when r.checkIntervalHours is null or " + END
            + " <= " + CHECK + " then " + END + " else " + CHECK + " end else null end";

    private boolean cultureGrain(org.openelisglobal.microbiology.form.MicroBenchQueryForm q) {
        return "CHECK_DUE".equals(q.cultureFilter) || "FINAL_READ_DUE".equals(q.cultureFilter);
    }

    private String benchWhere(org.openelisglobal.microbiology.form.MicroBenchQueryForm q, Set<String> ids,
            List<String> fields, List<String> questionnaireIds) {
        String hql = where(q, ids);
        if ("AWAITING_INOCULATION".equals(q.cultureFilter)) {
            hql += " and r.id is null and exists (select link.id from MicroCaseAnalysis link, Analysis a"
                    + " where link.caseId = c.id and link.analysisId = a.id and link.cancelledAt is null"
                    + " and (link.caseRole = org.openelisglobal.microbiology.valueholder.MicroCaseRole.CULTURE or a.test.microbiologyCaseRole = 'CULTURE'))";
        } else if (cultureGrain(q)) {
            hql += " and " + ACTIVE + " and " + ("CHECK_DUE".equals(q.cultureFilter) ? CHECK : END) + " <= "
                    + epoch("cast(:benchNow as timestamp)");
        }
        if (q.needsAttention) {
            var reasons = new java.util.ArrayList<String>();
            reasons.add("(" + ACTIVE + " and (" + CHECK + " <= " + epoch("cast(:benchNow as timestamp)") + " or " + END
                    + " <= " + epoch("cast(:benchNow as timestamp)") + "))");
            reasons.add(
                    "exists (select i.id from MicroIncomingItem i where i.microCase.id = c.id and i.state in ('WAITING','FAILED','NEEDS_AMENDMENT'))");
            reasons.add(
                    "exists (select proposal.id from MicroCultureProposal proposal where proposal.inoculation.id = r.id and proposal.confirmedAt is null)");
            reasons.add(
                    "exists (select ref.id from Referral ref, MicroCaseSample ms where ms.caseId = c.id and ms.splitOutAt is null and ref.analysis.sampleItem.id = ms.sampleItemId and ref.status = org.openelisglobal.referral.valueholder.ReferralStatus.COMPLETED and (ref.reconciled is null or ref.reconciled = false))");
            reasons.add(
                    "not exists (select ps.id from ProgramSample ps where ps.sample.id = s.id and ps.program.showOnMicroCase = true)");
            for (String field : fields) {
                // Names come only from the final-requirements service's validated allowlist.
                String missing = java.util.Set.of("admissionDate", "numberOfSets", "antibioticExposure").contains(field)
                        ? "d." + field + " is null"
                        : "(d." + field + " is null or trim(d." + field + ") = '')";
                reasons.add("not exists (select d.id from MicroCaseOrderDetail d where d.caseId = c.id and not "
                        + missing + ")");
            }
            if (!questionnaireIds.isEmpty())
                reasons.add("c.id in (:questionnaireIds)");
            hql += " and (" + String.join(" or ", reasons) + ")";
        }
        return hql;
    }

    private void bindBench(Query<?> query, org.openelisglobal.microbiology.form.MicroBenchQueryForm q, Set<String> ids,
            List<String> questionnaireIds, Timestamp now) {
        bind(query, q, ids);
        if (cultureGrain(q) || q.needsAttention)
            query.setParameter("benchNow", now);
        if (q.needsAttention && !questionnaireIds.isEmpty())
            query.setParameterList("questionnaireIds", questionnaireIds);
    }

    @Override
    public List<Object[]> benchPage(org.openelisglobal.microbiology.form.MicroBenchQueryForm q, Set<String> ids,
            List<String> fields, List<String> questionnaireIds, Timestamp now) {
        if (ids != null && ids.isEmpty())
            return List.of();
        String rowId = cultureGrain(q) ? "r.id" : "c.id";
        String group = " group by c.id, c.priority" + (cultureGrain(q) ? ", r.id" : "");
        var query = session().createQuery("select c.id, " + rowId + ", min(" + DUE + ") as dueAt" + FROM + CULTURES
                + benchWhere(q, ids, fields, questionnaireIds) + group
                + " order by case when c.priority = 'STAT' then 0 else 1 end, dueAt asc nulls last, c.id, " + rowId,
                Object[].class);
        bindBench(query, q, ids, questionnaireIds, now);
        return query.setFirstResult((q.page - 1) * q.pageSize).setMaxResults(q.pageSize).list();
    }

    @Override
    public long benchCount(org.openelisglobal.microbiology.form.MicroBenchQueryForm q, Set<String> ids,
            List<String> fields, List<String> questionnaireIds, Timestamp now) {
        if (ids != null && ids.isEmpty())
            return 0;
        var query = session().createQuery("select count(distinct " + (cultureGrain(q) ? "r.id" : "c.id") + ")" + FROM
                + CULTURES + benchWhere(q, ids, fields, questionnaireIds), Long.class);
        bindBench(query, q, ids, questionnaireIds, now);
        return query.uniqueResult();
    }

    @Override
    public List<String> questionnaireCases(MicroCaseSearchForm q, Set<String> ids) {
        if (ids != null && ids.isEmpty())
            return List.of();
        var query = session().createQuery("select distinct c.id" + FROM + where(q, ids)
                + " and exists (select ps.id from ProgramSample ps where ps.sample.id = s.id and ps.program.showOnMicroCase = true and ps.program.questionnaireUUID is not null)",
                String.class);
        bind(query, q, ids);
        return query.list();
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
