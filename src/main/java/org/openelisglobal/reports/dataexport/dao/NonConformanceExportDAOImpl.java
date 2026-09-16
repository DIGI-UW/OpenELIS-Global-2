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
                + "and coalesce(e.dateOfEvent, e.reportDate) <= :to " + "and (e.reportingUnitId in :units or exists "
                + "(select a.id from Analysis a where a.sampleItem.id = si.id and a.testSection.id in :sections)) "
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
                + "and exists (select a.id from Analysis a where a.testSection.id in :sections "
                + "and (a.sampleItem.id = si.id or (si.id is null and a.sampleItem.sample.id = s.id))) "
                + "order by occurrence.id";
        return entityManager.createQuery(hql, Object[].class).setParameter("from", Timestamp.from(dates.start()))
                .setParameter("to", Timestamp.from(dates.endExclusive()))
                .setParameter("sections", filter.labSectionIds()).setHint(QueryHints.HINT_FETCH_SIZE, 250)
                .setHint(QueryHints.HINT_READONLY, true).getResultStream().map(r -> new RejectionRow((String) r[0],
                        (String) r[1], (String) r[2], (String) r[3], (Timestamp) r[4]));
    }
}
