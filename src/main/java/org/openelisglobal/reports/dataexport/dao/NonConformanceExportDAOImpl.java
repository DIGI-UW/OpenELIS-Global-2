package org.openelisglobal.reports.dataexport.dao;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.stream.Stream;
import org.hibernate.jpa.QueryHints;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.reports.dataexport.form.ExportSnapshot;
import org.openelisglobal.reports.dataexport.service.ExportDateRange;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class NonConformanceExportDAOImpl extends BaseDAOImpl<NcEvent, Integer> implements NonConformanceExportDAO {
    public NonConformanceExportDAOImpl() {
        super(NcEvent.class);
    }

    @Override
    public Stream<EventRow> events(ExportSnapshot request) {
        var filter = request.filterSpec();
        String hql = "select e.id, link.id, si.id, coalesce(s.accessionNumber, e.labOrderNumber), "
                + "case when si.rejected = true then reason.qaEventName else kind.name end, "
                + "section.testSectionName, e.nameOfReporter, e.dateOfEvent, e.reportDate, e.status "
                + "from NcEvent e left join NceSpecimen link on link.nceId = e.id "
                + "left join SampleItem si on cast(si.id as integer) = link.sampleItemId "
                + "left join si.sample s left join QaEvent reason on reason.id = si.rejectReasonId "
                + "left join NceType kind on kind.id = e.nceTypeId "
                + "left join TestSection section on cast(section.id as integer) = e.reportingUnitId "
                + "where coalesce(e.dateOfEvent, e.reportDate) >= :from "
                + "and coalesce(e.dateOfEvent, e.reportDate) <= :to "
                // Scope precedence matters. An explicit reporting unit is
                // authoritative, so an event assigned to section A must not become
                // visible to section B merely because the same specimen also carries a
                // B analysis. Otherwise the event's accession, reason, reporter and
                // status cross the lab-section RBAC boundary. Where no unit is
                // recorded, the linked analysis decides; only an unlinked event falls
                // back to any analysis on the specimen.
                + "and ((e.reportingUnitId is not null and e.reportingUnitId in :units) "
                + "or (e.reportingUnitId is null and link.analysisId is not null and exists "
                + "(select a.id from Analysis a where cast(a.id as integer) = link.analysisId "
                + "and a.testSection.id in :sections)) "
                + "or (e.reportingUnitId is null and link.analysisId is null and exists "
                + "(select a.id from Analysis a where a.sampleItem.id = si.id and a.testSection.id in :sections))) "
                + "order by e.id, link.id";
        return entityManager.createQuery(hql, Object[].class)
                .setParameter("from", Date.valueOf(LocalDate.parse(filter.dateFrom())))
                .setParameter("to", Date.valueOf(LocalDate.parse(filter.dateTo())))
                .setParameter("sections", filter.labSectionIds())
                .setParameter("units", filter.labSectionIds().stream().map(Integer::valueOf).toList())
                .setHint(QueryHints.HINT_FETCH_SIZE, 250).setHint(QueryHints.HINT_READONLY, true).getResultStream()
                .map(r -> new EventRow((Integer) r[0], (Integer) r[1], (String) r[2], (String) r[3], (String) r[4],
                        (String) r[5], (String) r[6], (Date) r[7], (Date) r[8], (String) r[9]));
    }

    @Override
    public Stream<RejectionRow> rejections(ExportSnapshot request) {
        var filter = request.filterSpec();
        var dates = ExportDateRange.of(filter.dateFrom(), filter.dateTo(), ZoneId.of(request.timezone()),
                Integer.MAX_VALUE);
        String hql = "select occurrence.id, si.id, s.accessionNumber, reason.qaEventName, occurrence.enteredDate "
                + "from SampleQaEvent occurrence join occurrence.sample s "
                + "left join occurrence.sampleItem si join occurrence.qaEvent reason "
                + "where occurrence.enteredDate >= :from and occurrence.enteredDate < :to "
                // NonConformityUpdateWorker.addSampleQaEvent records the occurrence's
                // own section as a SECTION QaObservation, so that is the scope. Using
                // any related analysis instead exports an occurrence recorded against
                // section A to section B whenever the specimen or order happens to
                // carry a B analysis. Occurrences predating the observation keep the
                // analysis-based fallback so they remain reachable by someone.
                + "and (exists (select o.id from QaObservation o where o.observedType = 'SAMPLE' "
                + "and o.observedId = occurrence.id and o.observationType.name = 'SECTION' "
                + "and o.value in :sectionKeys) "
                + "or (not exists (select o2.id from QaObservation o2 where o2.observedType = 'SAMPLE' "
                + "and o2.observedId = occurrence.id and o2.observationType.name = 'SECTION') "
                + "and exists (select a.id from Analysis a where a.testSection.id in :sections "
                + "and (a.sampleItem.id = si.id or (si.id is null and a.sampleItem.sample.id = s.id))))) "
                + "order by occurrence.id";
        return entityManager.createQuery(hql, Object[].class).setParameter("from", Timestamp.from(dates.start()))
                .setParameter("to", Timestamp.from(dates.endExclusive()))
                .setParameter("sections", filter.labSectionIds()).setParameter("sectionKeys", filter.labSectionIds())
                .setHint(QueryHints.HINT_FETCH_SIZE, 250).setHint(QueryHints.HINT_READONLY, true).getResultStream()
                .map(r -> new RejectionRow((String) r[0], (String) r[1], (String) r[2], (String) r[3],
                        (Timestamp) r[4]));
    }
}
