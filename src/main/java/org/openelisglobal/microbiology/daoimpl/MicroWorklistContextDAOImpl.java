package org.openelisglobal.microbiology.daoimpl;

import java.sql.Timestamp;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroWorklistContextDAO;
import org.openelisglobal.microbiology.form.MicroWorklistActivityContext;
import org.openelisglobal.microbiology.form.MicroWorklistSpecimenContext;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroWorklistContextDAOImpl extends BaseDAOImpl<MicroCase, String> implements MicroWorklistContextDAO {

    static final String SPECIMEN_CONTEXT_HQL = "select sampleItem.id, sample.accessionNumber, person.lastName, person.firstName, type.description "
            + "from SampleItem sampleItem join sampleItem.sample sample "
            + "left join SampleHuman sampleHuman on sampleHuman.sampleId = sample.id "
            + "left join Patient patient on patient.id = sampleHuman.patientId "
            + "left join patient.person person left join sampleItem.typeOfSample type "
            + "where sampleItem.id in (:sampleItemIds)";

    static final String LATEST_ACTIVITY_CONTEXT_HQL = "select activity.caseId, activity.occurredAt, activity.performedBy, user.firstName, user.lastName "
            + "from MicroCaseActivity activity left join SystemUser user on user.id = activity.performedBy "
            + "where activity.caseId in (:caseIds) and not exists ("
            + "select newer.id from MicroCaseActivity newer where newer.caseId = activity.caseId and ("
            + "newer.occurredAt > activity.occurredAt or (newer.occurredAt = activity.occurredAt "
            + "and newer.id > activity.id)))";

    public MicroWorklistContextDAOImpl() {
        super(MicroCase.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroWorklistSpecimenContext> getSpecimenContexts(List<String> sampleItemIds) {
        if (sampleItemIds.isEmpty()) {
            return List.of();
        }
        Query<Object[]> query = entityManager.unwrap(Session.class).createQuery(SPECIMEN_CONTEXT_HQL, Object[].class);
        query.setParameterList("sampleItemIds", sampleItemIds);
        return query.list().stream().map(values -> new MicroWorklistSpecimenContext(text(values[0]), text(values[1]),
                patientDisplay(values[2], values[3]), text(values[4]))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroWorklistActivityContext> getLatestActivityContexts(List<String> caseIds) {
        if (caseIds.isEmpty()) {
            return List.of();
        }
        Query<Object[]> query = entityManager.unwrap(Session.class).createQuery(LATEST_ACTIVITY_CONTEXT_HQL,
                Object[].class);
        query.setParameterList("caseIds", caseIds);
        return query.list().stream().map(values -> new MicroWorklistActivityContext(text(values[0]),
                (Timestamp) values[1], text(values[2]), text(values[3]), text(values[4]))).toList();
    }

    private String patientDisplay(Object lastName, Object firstName) {
        String last = text(lastName);
        String first = text(firstName);
        if (last.isEmpty()) {
            return first;
        }
        return first.isEmpty() ? last : last + ", " + first;
    }

    private String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
