package org.openelisglobal.reports.dataexport.dao;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;
import org.hibernate.jpa.QueryHints;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.reports.dataexport.form.ExportSnapshot;
import org.openelisglobal.reports.dataexport.service.ExportDateRange;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class SampleTestingExportDAOImpl extends BaseDAOImpl<Result, String> implements SampleTestingExportDAO {
    public SampleTestingExportDAOImpl() {
        super(Result.class);
    }

    @Override
    public Stream<Result> stream(ExportSnapshot request) {
        var filter = request.filterSpec();
        var dates = ExportDateRange.of(filter.dateFrom(), filter.dateTo(), ZoneId.of(request.timezone()),
                Integer.MAX_VALUE);
        String hql = "select r from Result r join fetch r.analysis a join fetch a.sampleItem si "
                + "join fetch si.sample s join fetch a.test t left join fetch t.unitOfMeasure "
                + "left join fetch a.testSection left join fetch si.typeOfSample "
                + "left join fetch r.testResult tr left join fetch r.parentResult "
                + "where si.collectionDate >= :from and si.collectionDate < :to "
                + "and a.testSection.id in :sections and a.statusId in :statuses "
                + (filter.testIds().isEmpty() ? "" : "and t.id in :tests ")
                + "order by si.id, a.id, coalesce(tr.componentId, ''), coalesce(r.grouping, 0), r.resultType, r.id";
        var query = entityManager.createQuery(hql, Result.class).setParameter("from", Timestamp.from(dates.start()))
                .setParameter("to", Timestamp.from(dates.endExclusive()))
                .setParameter("sections", filter.labSectionIds()).setParameter("statuses", request.statusIds())
                .setHint(QueryHints.HINT_FETCH_SIZE, 250).setHint(QueryHints.HINT_READONLY, true);
        if (!filter.testIds().isEmpty())
            query.setParameter("tests", filter.testIds());
        return query.getResultStream();
    }

    @Override
    public List<Test> tests() {
        return entityManager.createQuery("select t from Test t left join fetch t.testSection "
                + "where t.isActive = 'Y' order by t.description, t.id", Test.class).getResultList();
    }

    @Override
    public List<TestResultComponent> components() {
        return entityManager.createQuery(
                "from TestResultComponent c where c.isActive = 'Y' " + "order by c.testId, c.displayOrder, c.id",
                TestResultComponent.class).getResultList();
    }

    @Override
    public Patient patient(String sampleId) {
        return entityManager
                .createQuery(
                        "select p from Patient p left join fetch p.person "
                                + "where p.id in (select sh.patientId from SampleHuman sh where sh.sampleId = :sample)",
                        Patient.class)
                .setParameter("sample", sampleId).setMaxResults(1).getResultStream().findFirst().orElse(null);
    }

    @Override
    public long analysisCount(String sampleId) {
        return entityManager
                .createQuery("select count(a) from Analysis a where a.sampleItem.sample.id = :sample", Long.class)
                .setParameter("sample", sampleId).getSingleResult();
    }

    @Override
    public List<ObservationHistoryType> observationTypes() {
        return entityManager
                .createQuery("from ObservationHistoryType t where t.description is not null and t.description <> '' "
                        + "order by t.description, t.id", ObservationHistoryType.class)
                .getResultList();
    }

    @Override
    public List<ObservationHistory> observations(String sampleId, String specimenId) {
        return entityManager
                .createQuery("from ObservationHistory o where o.sampleId = :sample "
                        + "and (o.sampleItemId is null or o.sampleItemId = :specimen) "
                        + "order by o.observationHistoryTypeId, o.id", ObservationHistory.class)
                .setParameter("sample", sampleId).setParameter("specimen", specimenId).getResultList();
    }

    @Override
    public String dictionary(String id) {
        return entityManager.createQuery("select d.dictEntry from Dictionary d where d.id = :id", String.class)
                .setParameter("id", id).getResultStream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("reporting.value.dictionaryMissing"));
    }

    @Override
    public List<String> qualifiers(String resultId) {
        return entityManager
                .createQuery("select r.value from Result r where r.parentResult.id = :id "
                        + "and r.resultType = 'A' and r.testResult is null order by r.id", String.class)
                .setParameter("id", resultId).getResultList();
    }

    @Override
    public void clearReadBatch() {
        entityManager.clear();
    }
}
