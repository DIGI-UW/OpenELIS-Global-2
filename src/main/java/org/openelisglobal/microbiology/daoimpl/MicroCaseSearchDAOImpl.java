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
    private static final String CASE_STATUS = "case when (select count(ma.id) from MicroCaseAmendment ma where ma.caseId = c.id and ma.status = 'RELEASED') > 0 then 'AMENDED'"
            + " when c.finalReleaseState in ('FINAL_RELEASED', 'AMENDMENT_IN_PROGRESS') then 'FINAL'"
            + " when c.finalReleaseState = 'PRELIMINARY_RELEASED' then 'PARTIAL' else 'IN_PROGRESS' end";
    private static final String SELECT = "select c, s.accessionNumber, u.testSectionName, p.id, person.lastName, person.firstName, p.birthDate, p.gender, t.description, s.domain, site.name, site.code, s.priority, "
            + CASE_STATUS;
    private static final String FROM = " from MicroCase c left join SampleItem original on original.id = c.sampleItemId"
            + " left join Sample s on s.id = coalesce(c.sampleId, original.sample.id)"
            + " left join TestSection u on u.id = c.labUnitId" + " left join TypeOfSample t on t.id = c.sampleTypeId"
            + " left join VectorSamplingSite site on site.id = c.siteId"
            + " left join SampleHuman h on h.sampleId = s.id and (s.domain is null or upper(s.domain) not in ('ENVIRONMENTAL','E')) left join Patient p on p.id = h.patientId left join p.person person";

    private Session session() {
        return entityManager.unwrap(Session.class);
    }

    private String where(MicroCaseSearchForm q, Set<String> ids) {
        String hql = " where c.labUnitId is not null";
        if (ids != null)
            hql += " and c.labUnitId in (:units)";
        if (q.migrationReview)
            hql += " and exists (select a.id from MicroCaseActivity a where a.caseId = c.id and a.activityType = 'MIGRATION_REVIEW')";
        if (q.labUnitId != null)
            hql += " and c.labUnitId = :unit";
        if (q.status != null)
            hql += " and c.status = :status";
        if (q.caseStatus != null) {
            if (isReleaseStatus(q.caseStatus))
                hql += " and c.status = org.openelisglobal.microbiology.valueholder.MicroCaseStatus.ACTIVE and ("
                        + CASE_STATUS + ") = :caseStatus";
            else if ("AMENDMENT_OPEN".equals(q.caseStatus))
                hql += " and c.finalReleaseState = 'AMENDMENT_IN_PROGRESS'";
            else
                hql += " and c.id in (:referralCases)";
        }
        if (q.from != null)
            hql += " and c.createdAt >= :from";
        if (q.to != null)
            hql += " and c.createdAt < :to";
        if (q.patientId != null)
            hql += " and p.id = :patient";
        if (q.accessionNumber != null)
            hql += " and s.accessionNumber = :accession";
        if (q.q != null)
            hql += " and (lower(s.accessionNumber) like :search or lower(person.lastName) like :search or lower(person.firstName) like :search"
                    + " or lower(concat(coalesce(person.lastName, ''), ' ', coalesce(person.firstName, ''))) like :search"
                    + " or lower(concat(coalesce(person.firstName, ''), ' ', coalesce(person.lastName, ''))) like :search"
                    + " or lower(site.name) like :search or lower(site.code) like :search"
                    + " or exists (select ms.id from MicroCaseSample ms join SampleItem si on si.id = ms.sampleItemId"
                    + " where ms.caseId = c.id and ms.splitOutAt is null and lower(si.locationDetails) like :search))";
        return hql;
    }

    private void bind(Query<?> query, MicroCaseSearchForm q, Set<String> ids) {
        if (ids != null)
            query.setParameterList("units", ids);
        if (q.labUnitId != null)
            query.setParameter("unit", q.labUnitId);
        if (q.status != null)
            query.setParameter("status", org.openelisglobal.microbiology.valueholder.MicroCaseStatus.valueOf(q.status));
        if (q.caseStatus != null) {
            if (isReleaseStatus(q.caseStatus))
                query.setParameter("caseStatus", q.caseStatus);
            else if (!"AMENDMENT_OPEN".equals(q.caseStatus)) {
                var cases = "REFERRED".equals(q.caseStatus) ? openReferralCases() : referralAttentionCases();
                query.setParameterList("referralCases", cases.isEmpty() ? java.util.List.of("__none__") : cases);
            }
        }
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

    private static boolean isReleaseStatus(String status) {
        return java.util.List.of("IN_PROGRESS", "PARTIAL", "FINAL", "AMENDED").contains(status);
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
            List<String> fields, List<String> attentionCaseIds) {
        String hql = where(q, ids);
        if ("AWAITING_INOCULATION".equals(q.cultureFilter)) {
            hql += " and exists (select link.id from MicroCaseAnalysis link, Analysis a"
                    + " where link.caseId = c.id and link.analysisId = a.id and link.cancelledAt is null"
                    + " and (a.sampleItem.rejected is null or a.sampleItem.rejected = false)"
                    + " and (link.caseRole = org.openelisglobal.microbiology.valueholder.MicroCaseRole.CULTURE or a.test.microbiologyCaseRole = 'CULTURE')"
                    + " and not exists (select inoculation.id from MicroCaseInoculation inoculation"
                    + " where inoculation.caseId = c.id and inoculation.sourceSampleItemId = a.sampleItem.id"
                    + " and inoculation.cancelledAt is null and (inoculation.positiveSource is null or inoculation.positiveSource <> 'RECEIVED_ISOLATE')))";
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
            reasons.add("c.programId is null");
            for (String field : fields) {
                if ("culturePurpose".equals(field)) {
                    reasons.add("c.culturePurpose is null");
                    continue;
                }
                // Names come only from the final-requirements service's validated allowlist.
                String missing = java.util.Set.of("admissionDate", "numberOfSets", "antibioticExposure").contains(field)
                        ? "d." + field + " is null"
                        : "(d." + field + " is null or trim(d." + field + ") = '')";
                reasons.add(
                        "(c.siteId is null and not exists (select d.id from MicroCaseOrderDetail d where d.caseId = c.id and not "
                                + missing + "))");
            }
            if (!attentionCaseIds.isEmpty())
                reasons.add("c.id in (:attentionCaseIds)");
            hql += " and (" + String.join(" or ", reasons) + ")";
        }
        return hql;
    }

    private void bindBench(Query<?> query, org.openelisglobal.microbiology.form.MicroBenchQueryForm q, Set<String> ids,
            List<String> attentionCaseIds, Timestamp now) {
        bind(query, q, ids);
        if (cultureGrain(q) || q.needsAttention)
            query.setParameter("benchNow", now);
        if (q.needsAttention && !attentionCaseIds.isEmpty())
            query.setParameterList("attentionCaseIds", attentionCaseIds);
    }

    @Override
    public List<Object[]> benchPage(org.openelisglobal.microbiology.form.MicroBenchQueryForm q, Set<String> ids,
            List<String> fields, List<String> attentionCaseIds, Timestamp now) {
        if (ids != null && ids.isEmpty())
            return List.of();
        String rowId = cultureGrain(q) ? "r.id" : "c.id";
        String group = " group by c.id, c.priority" + (cultureGrain(q) ? ", r.id" : "");
        var query = session().createQuery("select c.id, " + rowId + ", min(" + DUE + ") as dueAt" + FROM + CULTURES
                + benchWhere(q, ids, fields, attentionCaseIds) + group
                + " order by case when c.priority = 'STAT' then 0 else 1 end, dueAt asc nulls last, c.id, " + rowId,
                Object[].class);
        bindBench(query, q, ids, attentionCaseIds, now);
        return query.setFirstResult((q.page - 1) * q.pageSize).setMaxResults(q.pageSize).list();
    }

    @Override
    public long benchCount(org.openelisglobal.microbiology.form.MicroBenchQueryForm q, Set<String> ids,
            List<String> fields, List<String> attentionCaseIds, Timestamp now) {
        if (ids != null && ids.isEmpty())
            return 0;
        var query = session().createQuery("select count(distinct " + (cultureGrain(q) ? "r.id" : "c.id") + ")" + FROM
                + CULTURES + benchWhere(q, ids, fields, attentionCaseIds), Long.class);
        bindBench(query, q, ids, attentionCaseIds, now);
        return query.uniqueResult();
    }

    @Override
    public List<String> referralAttentionCases() {
        return referralCases(false);
    }

    @Override
    public List<String> openReferralCases() {
        return referralCases(true);
    }

    private List<String> referralCases(boolean open) {
        var start = session().createQuery(open
                ? "select distinct ref.analysis.sampleItem.id from Referral ref where ref.status not in (:terminal)"
                : "select distinct ref.analysis.sampleItem.id from Referral ref where ref.status = org.openelisglobal.referral.valueholder.ReferralStatus.COMPLETED and (ref.reconciled is null or ref.reconciled = false)",
                String.class);
        if (open)
            start.setParameterList("terminal",
                    java.util.Arrays.stream(org.openelisglobal.referral.valueholder.ReferralStatus.values())
                            .filter(org.openelisglobal.referral.valueholder.ReferralStatus::isTerminal).toList());
        var frontier = new java.util.LinkedHashSet<>(start.list());
        Set<String> visited = new java.util.HashSet<>();
        Set<String> cases = new java.util.LinkedHashSet<>();
        while (!frontier.isEmpty()) {
            visited.addAll(frontier);
            var members = session().createQuery(
                    "select m.caseId, m.sampleItemId from MicroCaseSample m where m.sampleItemId in (:sources) and m.splitOutAt is null",
                    Object[].class).setParameterList("sources", frontier).list();
            for (var member : members) {
                cases.add((String) member[0]);
            }
            // A descendant assigned to another case is that case's source, not
            // its ancestor's. This mirrors MicroCaseInoculationDAO.getSources.
            members.forEach(member -> frontier.remove((String) member[1]));
            if (frontier.isEmpty())
                break;
            var parents = session().createQuery(
                    "select distinct s.parentSampleItem.id from SampleItem s where s.id in (:sources) and s.parentSampleItem is not null",
                    String.class).setParameterList("sources", frontier).list();
            frontier.clear();
            frontier.addAll(parents);
            frontier.removeAll(visited);
        }
        return List.copyOf(cases);
    }

    @Override
    public List<String> questionnaireCases(MicroCaseSearchForm q, Set<String> ids) {
        if (ids != null && ids.isEmpty())
            return List.of();
        var query = session().createQuery("select distinct c.id" + FROM + where(q, ids)
                + " and c.siteId is null and exists (select program.id from Program program where program.id = c.programId and program.showOnMicroCase = true and program.questionnaireUUID is not null)",
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
