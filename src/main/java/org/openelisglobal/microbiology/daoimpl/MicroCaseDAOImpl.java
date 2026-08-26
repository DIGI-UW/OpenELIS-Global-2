package org.openelisglobal.microbiology.daoimpl;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroWhonetContext;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroCaseDAOImpl extends BaseDAOImpl<MicroCase, String> implements MicroCaseDAO {

    public MicroCaseDAOImpl() {
        super(MicroCase.class);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCase getBySampleItemAndWorkflow(String sampleItemId, String workflowType) {
        Query<MicroCase> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroCase c where c.sampleItemId = :sampleItemId" + " and c.workflowType = :workflowType",
                MicroCase.class);
        query.setParameter("sampleItemId", sampleItemId);
        query.setParameter("workflowType", workflowType);
        return query.uniqueResultOptional().orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCase> getBySampleItem(String sampleItemId) {
        Query<MicroCase> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroCase c where c.sampleItemId = :sampleItemId" + " order by c.workflowType", MicroCase.class);
        query.setParameter("sampleItemId", sampleItemId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCase> getBySampleItemIds(List<String> sampleItemIds) {
        if (sampleItemIds == null || sampleItemIds.isEmpty()) {
            return List.of();
        }
        Query<MicroCase> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroCase c where c.sampleItemId in (:sampleItemIds)"
                        + " order by c.sampleItemId, c.workflowType", MicroCase.class);
        query.setParameterList("sampleItemIds", sampleItemIds);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCase> getOpenCases() {
        Query<MicroCase> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroCase c where c.closedAt is null order by c.createdAt", MicroCase.class);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCase> getFinalizedBacteriologyByClosedAtRange(Timestamp fromInclusive, Timestamp toExclusive) {
        Query<MicroCase> query = entityManager.unwrap(Session.class)
                .createQuery("from MicroCase c where c.workflowType = :workflowType"
                        + " and c.finalReleaseState = :finalReleaseState and c.closedAt >= :fromInclusive"
                        + " and c.closedAt < :toExclusive order by c.closedAt, c.id", MicroCase.class);
        query.setParameter("workflowType", "BACTERIOLOGY");
        query.setParameter("finalReleaseState", "FINAL_RELEASED");
        query.setParameter("fromInclusive", fromInclusive);
        query.setParameter("toExclusive", toExclusive);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroWhonetContext> getWhonetContextsByCaseIds(List<String> caseIds) {
        if (caseIds == null || caseIds.isEmpty()) {
            return List.of();
        }
        Query<Object[]> query = entityManager.unwrap(Session.class).createQuery("select "
                + "c.id, c.sampleItemId, patient.id, patient.nationalId, person.firstName, person.lastName,"
                + " patient.gender, patient.birthDate, sample.accessionNumber, sample.enteredDate,"
                + " item.collectionDate, specimenType.description, sample.gpsLatitude, sample.gpsLongitude"
                + " from MicroCase c join SampleItem item on item.id = c.sampleItemId"
                + " join item.sample sample left join SampleHuman sampleHuman on sampleHuman.sampleId = sample.id"
                + " left join Patient patient on patient.id = sampleHuman.patientId"
                + " left join patient.person person left join item.typeOfSample specimenType"
                + " where c.id in (:caseIds) order by c.id", Object[].class);
        query.setParameterList("caseIds", caseIds);
        return query.list().stream().map(this::toWhonetContext).toList();
    }

    private MicroWhonetContext toWhonetContext(Object[] values) {
        return new MicroWhonetContext((String) values[0], (String) values[1], (String) values[2], (String) values[3],
                (String) values[4], (String) values[5], (String) values[6], toTimestamp(values[7]), (String) values[8],
                toSqlDate(values[9]), toTimestamp(values[10]), (String) values[11], toDouble(values[12]),
                toDouble(values[13]));
    }

    private Timestamp toTimestamp(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof Timestamp timestamp ? timestamp : new Timestamp(((java.util.Date) value).getTime());
    }

    private Date toSqlDate(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof Date date ? date : new Date(((java.util.Date) value).getTime());
    }

    private Double toDouble(Object value) {
        return value == null ? null : ((Number) value).doubleValue();
    }
}
