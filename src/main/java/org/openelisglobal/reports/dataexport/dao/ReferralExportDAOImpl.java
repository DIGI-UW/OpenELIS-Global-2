package org.openelisglobal.reports.dataexport.dao;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.stream.Stream;
import org.hibernate.jpa.QueryHints;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralResult;
import org.openelisglobal.reports.dataexport.form.ExportSnapshot;
import org.openelisglobal.reports.dataexport.service.ExportDateRange;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class ReferralExportDAOImpl extends BaseDAOImpl<Referral, String> implements ReferralExportDAO {
    public ReferralExportDAOImpl() {
        super(Referral.class);
    }

    @Override
    public Stream<Row> stream(ExportSnapshot request) {
        String anchor = switch (request.definition().dateAnchor()) {
        case "requestDate" -> "ref.requestDate";
        case "sentDate" -> "ref.sentDate";
        default -> throw new IllegalArgumentException("reporting.definition.unsupportedMapping");
        };
        var filter = request.filterSpec();
        var dates = ExportDateRange.of(filter.dateFrom(), filter.dateTo(), ZoneId.of(request.timezone()),
                Integer.MAX_VALUE);
        String hql = "select ref, returned, returnedTest from Referral ref "
                + "join fetch ref.analysis a join fetch a.sampleItem si join fetch si.sample "
                + "join fetch a.test t left join fetch a.testSection left join fetch si.typeOfSample "
                + "left join fetch ref.organization "
                + "left join ReferralResult returned on returned.referralId = ref.id "
                + "left join fetch returned.result r left join fetch r.testResult tr left join fetch r.parentResult "
                + "left join Test returnedTest on returnedTest.id = returned.testId "
                + "left join fetch returnedTest.unitOfMeasure " + "where " + anchor + " >= :from and " + anchor
                + " < :to " + "and a.testSection.id in :sections "
                + (filter.testIds().isEmpty() ? "" : "and t.id in :tests ")
                + "order by ref.id, returned.testId, coalesce(tr.componentId, ''), "
                + "coalesce(r.grouping, 0), r.resultType, returned.referralReportDate, returned.id";
        var query = entityManager.createQuery(hql, Object[].class).setParameter("from", Timestamp.from(dates.start()))
                .setParameter("to", Timestamp.from(dates.endExclusive()))
                .setParameter("sections", filter.labSectionIds()).setHint(QueryHints.HINT_FETCH_SIZE, 250)
                .setHint(QueryHints.HINT_READONLY, true);
        if (!filter.testIds().isEmpty())
            query.setParameter("tests", filter.testIds());
        return query.getResultStream().map(row -> new Row((Referral) row[0], (ReferralResult) row[1], (Test) row[2]));
    }

    @Override
    public void clearReadBatch() {
        entityManager.clear();
    }
}
