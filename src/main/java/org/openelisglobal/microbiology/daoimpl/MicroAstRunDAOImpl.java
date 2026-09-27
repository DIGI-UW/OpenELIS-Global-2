package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroReviewedAstWorklistQuery;
import org.openelisglobal.microbiology.dao.MicroReviewedAstWorklistRow;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunStatus;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroAstRunDAOImpl extends BaseDAOImpl<MicroAstRun, String> implements MicroAstRunDAO {

    static final String REVIEWED_WORKLIST_FROM_HQL = " from MicroAstRun run"
            + " join MicroIsolate isolate on isolate.id = run.isolateId"
            + " join MicroCase microCase on microCase.id = isolate.caseId"
            + " left join MicroAstPanel panel on panel.id = run.panelId"
            + " left join SampleItem sampleItem on sampleItem.id = microCase.sampleItemId"
            + " left join sampleItem.sample sample"
            + " left join SampleHuman sampleHuman on sampleHuman.sampleId = sample.id"
            + " left join Patient patient on patient.id = sampleHuman.patientId" + " left join patient.person person"
            + " left join sampleItem.typeOfSample sampleType"
            + " left join MicroCaseActivity activity on activity.caseId = microCase.id and not exists ("
            + "select newer.id from MicroCaseActivity newer where newer.caseId = activity.caseId and ("
            + "newer.occurredAt > activity.occurredAt or (newer.occurredAt = activity.occurredAt"
            + " and newer.id > activity.id)))"
            + " left join SystemUser activityUser on activityUser.id = activity.performedBy"
            + " where run.status = :reviewedStatus" + " and (:workflow = '' or microCase.workflowType = :workflow)"
            + " and (:stage = '' or microCase.stage = :stage)" + " and (:urgency = ''"
            + " or (:urgency = 'HIGH' and microCase.priority in ('STAT', 'URGENT'))"
            + " or (:urgency = 'ROUTINE' and microCase.priority not in ('STAT', 'URGENT')))"
            + " and (:due = '' or :due = 'VIEW')" + " and (:search = '' or lower(run.id) like :searchLike"
            + " or lower(microCase.id) like :searchLike"
            + " or lower(cast(microCase.sampleItemId as string)) like :searchLike"
            + " or lower(microCase.workflowType) like :searchLike" + " or lower(microCase.stage) like :searchLike"
            + " or lower(isolate.isolateLabel) like :searchLike"
            + " or lower(coalesce(isolate.preliminaryOrganismText, '')) like :searchLike"
            + " or lower(coalesce(isolate.organismId, '')) like :searchLike"
            + " or lower(coalesce(run.panelId, '')) like :searchLike" + " or lower(run.status) like :searchLike"
            + " or lower(coalesce(sample.accessionNumber, '')) like :searchLike"
            + " or lower(coalesce(person.lastName, '')) like :searchLike"
            + " or lower(coalesce(person.firstName, '')) like :searchLike"
            + " or lower(coalesce(sampleType.description, '')) like :searchLike"
            + " or lower(coalesce(panel.name, '')) like :searchLike"
            + " or lower(coalesce(activityUser.firstName, '')) like :searchLike"
            + " or lower(coalesce(activityUser.lastName, '')) like :searchLike"
            + " or (:searchMatchesHigh = true and microCase.priority in ('STAT', 'URGENT'))"
            + " or (:searchMatchesRoutine = true and microCase.priority not in ('STAT', 'URGENT'))"
            + " or :searchMatchesView = true)";

    static final String REVIEWED_WORKLIST_SELECT_HQL = "select run, isolate, microCase" + REVIEWED_WORKLIST_FROM_HQL;
    static final String REVIEWED_WORKLIST_COUNT_HQL = "select count(distinct run.id)" + REVIEWED_WORKLIST_FROM_HQL;

    public MicroAstRunDAOImpl() {
        super(MicroAstRun.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstRun> getByIsolateId(String isolateId) {
        Query<MicroAstRun> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroAstRun r where r.isolateId = :isolateId order by r.startedAt, r.id", MicroAstRun.class);
        query.setParameter("isolateId", isolateId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstRun> getByIsolateIds(List<String> isolateIds) {
        if (isolateIds == null || isolateIds.isEmpty()) {
            return List.of();
        }
        Query<MicroAstRun> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroAstRun r where r.isolateId in (:isolateIds) order by r.isolateId, r.startedAt, r.id",
                MicroAstRun.class);
        query.setParameterList("isolateIds", isolateIds);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAstRun> getByAmendmentId(String amendmentId) {
        Query<MicroAstRun> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroAstRun r where r.amendmentId = :amendmentId order by r.startedAt", MicroAstRun.class);
        query.setParameter("amendmentId", amendmentId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroReviewedAstWorklistRow> getReviewedWorklistPage(MicroReviewedAstWorklistQuery worklistQuery) {
        Query<Object[]> query = entityManager.unwrap(Session.class).createQuery(
                REVIEWED_WORKLIST_SELECT_HQL + reviewedWorklistOrder(worklistQuery.sort()), Object[].class);
        setReviewedWorklistParameters(query, worklistQuery);
        query.setFirstResult(worklistQuery.offset());
        query.setMaxResults(worklistQuery.limit());
        return query.list().stream().map(values -> new MicroReviewedAstWorklistRow((MicroCase) values[2],
                (MicroIsolate) values[1], (MicroAstRun) values[0])).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countReviewedWorklist(MicroReviewedAstWorklistQuery worklistQuery) {
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(REVIEWED_WORKLIST_COUNT_HQL, Long.class);
        setReviewedWorklistParameters(query, worklistQuery);
        return query.uniqueResult();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MicroAstRun> getByAnalyzerAndCard(String analyzerId, String cardId) {
        Query<MicroAstRun> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroAstRun r where r.analyzerInstrumentId = :analyzerId and r.analyzerCardId = :cardId "
                        + "and r.status in ('AWAITING_RESULTS', 'RESULTS_IN', 'QC_FAILED') order by r.startedAt desc",
                MicroAstRun.class);
        query.setParameter("analyzerId", analyzerId);
        query.setParameter("cardId", cardId);
        query.setMaxResults(1);
        return query.uniqueResultOptional();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnresolvedByBreakpointStandardId(String breakpointStandardId) {
        Query<Long> query = entityManager.unwrap(Session.class)
                .createQuery("select count(r.id) from MicroAstRun r where r.breakpointStandardId = :standardId"
                        + " and r.status = 'IN_PROGRESS'", Long.class);
        query.setParameter("standardId", breakpointStandardId);
        return query.uniqueResult();
    }

    private String reviewedWorklistOrder(String sort) {
        String priorityOrder = "case when microCase.priority in ('STAT', 'URGENT') then 0 else 1 end";
        if ("newest".equals(sort)) {
            return " order by run.startedAt desc, " + priorityOrder + ", run.id";
        }
        if ("workflow".equals(sort)) {
            return " order by microCase.workflowType, " + priorityOrder + ", run.startedAt, run.id";
        }
        return " order by " + priorityOrder + ", run.startedAt, run.id";
    }

    private void setReviewedWorklistParameters(Query<?> query, MicroReviewedAstWorklistQuery worklistQuery) {
        String search = worklistQuery.search() == null ? "" : worklistQuery.search().trim().toLowerCase(Locale.ROOT);
        query.setParameter("reviewedStatus", MicroAstRunStatus.REVIEWED.name());
        query.setParameter("workflow", text(worklistQuery.workflow()));
        query.setParameter("stage", text(worklistQuery.stage()));
        query.setParameter("urgency", text(worklistQuery.urgency()));
        query.setParameter("due", text(worklistQuery.due()));
        query.setParameter("search", search);
        query.setParameter("searchLike", "%" + search + "%");
        query.setParameter("searchMatchesHigh", "high".contains(search));
        query.setParameter("searchMatchesRoutine", "routine".contains(search));
        query.setParameter("searchMatchesView", "view".contains(search));
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }
}
