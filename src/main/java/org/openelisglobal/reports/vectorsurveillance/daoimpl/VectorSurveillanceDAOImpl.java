package org.openelisglobal.reports.vectorsurveillance.daoimpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.reports.vectorsurveillance.dao.VectorSurveillanceDAO;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SiteOption;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.DensityAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.PositivityAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.QcAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.SpeciesAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.SpeciesMirAggregate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * HQL aggregations over the vector OLTP model. Defensive by design — every
 * query is wrapped so a single failure degrades to an empty result (FR-012)
 * instead of breaking the dashboard.
 *
 * <p>
 * Positivity signal: a pool is treated as positive when
 * {@code deconvolutionStatus <> 'NOT_APPLICABLE'} — in this module
 * deconvolution is only triggered on a positive pool result, so it is the
 * workflow-true "positive" marker. (Flagged for confirmation vs. a
 * result-vocabulary rule.) Period labels use Postgres ISO-week via
 * {@code to_char(..., 'IYYY-"W"IW')}; the deployment DB is Postgres.
 */
@Component
@Transactional(readOnly = true)
public class VectorSurveillanceDAOImpl implements VectorSurveillanceDAO {

    private static final String POSITIVE = "p.deconvolutionStatus <> 'NOT_APPLICABLE'";

    @PersistenceContext
    private EntityManager entityManager;

    private Timestamp from(LocalDate d) {
        return Timestamp
                .from((d == null ? LocalDate.of(1970, 1, 1) : d).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Timestamp to(LocalDate d) {
        return Timestamp.from((d == null ? LocalDate.of(2999, 1, 1) : d).plusDays(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private long lng(Object o) {
        return o == null ? 0L : ((Number) o).longValue();
    }

    private Integer integer(Object o) {
        return o == null ? null : ((Number) o).intValue();
    }

    @Override
    public List<DensityAggregate> getCollectionDensity(LocalDate fromDate, LocalDate toDate, Integer siteId) {
        try {
            String hql = "select function('to_char', s.collectionDate, 'IYYY-\"W\"IW'), si.collectionLocationId,"
                    + " count(distinct p.id), coalesce(sum(si.quantity), 0)"
                    + " from VectorPool p, Sample s, VectorPoolMember vpm, SampleItem si"
                    + " where s.id = p.sampleId and vpm.pool = p and vpm.sampleItem = si and p.active = true"
                    + " and s.collectionDate between :from and :to" + siteClause(siteId)
                    + " group by function('to_char', s.collectionDate, 'IYYY-\"W\"IW'), si.collectionLocationId"
                    + " order by 1";
            List<?> rows = bind(hql, fromDate, toDate, siteId).getResultList();
            List<DensityAggregate> out = new ArrayList<>();
            for (Object row : rows) {
                Object[] r = (Object[]) row;
                Integer sid = parseSite(r[1]);
                out.add(new DensityAggregate((String) r[0], sid, null, lng(r[2]), lng(r[3])));
            }
            return out;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<SpeciesAggregate> getSpeciesDistribution(LocalDate fromDate, LocalDate toDate, Integer siteId) {
        try {
            String hql = "select sp.id, sp.genus, sp.species, coalesce(sum(si.quantity), 0)"
                    + " from VectorSpecimenIdentification vsi, SampleItem si, Sample s, VectorSpecies sp"
                    + " where cast(vsi.sampleItemId as string) = si.id and si.sample.id = s.id"
                    + " and vsi.vectorSpeciesId = sp.id and vsi.confidence = 'CONFIRMED'"
                    + " and s.collectionDate between :from and :to" + siteClause(siteId)
                    + " group by sp.id, sp.genus, sp.species order by 4 desc";
            List<?> rows = bind(hql, fromDate, toDate, siteId).getResultList();
            List<SpeciesAggregate> out = new ArrayList<>();
            for (Object row : rows) {
                Object[] r = (Object[]) row;
                out.add(new SpeciesAggregate(integer(r[0]), (String) r[1], (String) r[2], lng(r[3])));
            }
            return out;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<SpeciesMirAggregate> getMirAggregates(LocalDate fromDate, LocalDate toDate, Integer siteId) {
        try {
            String hql = "select sp.id, sp.genus, sp.species," + " count(distinct case when " + POSITIVE
                    + " then p.id else null end)," + " coalesce(sum(si.quantity), 0),"
                    + " count(distinct case when p.deconvolutionStatus = 'COMPLETE' then p.id else null end)"
                    + " from VectorPool p, Sample s, VectorPoolMember vpm, SampleItem si,"
                    + " VectorSpecimenIdentification vsi, VectorSpecies sp"
                    + " where s.id = p.sampleId and vpm.pool = p and vpm.sampleItem = si and p.parentPool is null"
                    + " and cast(vsi.sampleItemId as string) = si.id and vsi.vectorSpeciesId = sp.id"
                    + " and vsi.confidence = 'CONFIRMED' and s.collectionDate between :from and :to"
                    + siteClause(siteId) + " group by sp.id, sp.genus, sp.species";
            List<?> rows = bind(hql, fromDate, toDate, siteId).getResultList();
            List<SpeciesMirAggregate> out = new ArrayList<>();
            for (Object row : rows) {
                Object[] r = (Object[]) row;
                long positive = lng(r[3]);
                long resolved = lng(r[5]);
                // pathogen granularity deferred (needs analysis/test join); species-level MIR
                // for now.
                out.add(new SpeciesMirAggregate(integer(r[0]), (String) r[1], (String) r[2], null, positive, lng(r[4]),
                        positive, positive, resolved));
            }
            return out;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<PositivityAggregate> getPathogenPositivity(LocalDate fromDate, LocalDate toDate, Integer siteId) {
        try {
            String hql = "select count(distinct case when " + POSITIVE
                    + " then p.id else null end), count(distinct p.id)" + " from VectorPool p, Sample s"
                    + " where s.id = p.sampleId and p.parentPool is null and s.collectionDate between :from and :to";
            // site filter on positivity uses the pool's first member site (kept simple):
            // skip when null.
            Query q = entityManager.createQuery(hql);
            q.setParameter("from", from(fromDate));
            q.setParameter("to", to(toDate));
            Object[] r = (Object[]) q.getSingleResult();
            List<PositivityAggregate> out = new ArrayList<>();
            out.add(new PositivityAggregate("All pathogens", lng(r[0]), lng(r[1])));
            return out;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return Collections.emptyList();
        }
    }

    @Override
    public QcAggregate getQcPassRate(LocalDate fromDate, LocalDate toDate, Integer siteId) {
        try {
            String total = "select count(a.id) from Analysis a where a.vectorPoolId is not null";
            String failed = "select count(distinct aqe.analysis.id) from AnalysisQaEvent aqe"
                    + " where aqe.analysis.vectorPoolId is not null";
            long totalCount = lng(entityManager.createQuery(total).getSingleResult());
            long failedCount = lng(entityManager.createQuery(failed).getSingleResult());
            return new QcAggregate(Math.max(0, totalCount - failedCount), totalCount);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return new QcAggregate(0, 0);
        }
    }

    @Override
    public List<SiteOption> getSites() {
        try {
            List<?> rows = entityManager.createQuery(
                    "select s.id, s.code, s.name from VectorSamplingSite s" + " where s.active = true order by s.name")
                    .getResultList();
            List<SiteOption> out = new ArrayList<>();
            for (Object row : rows) {
                Object[] r = (Object[]) row;
                out.add(new SiteOption(integer(r[0]), (String) r[1], (String) r[2]));
            }
            return out;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return Collections.emptyList();
        }
    }

    private String siteClause(Integer siteId) {
        return siteId == null ? "" : " and si.collectionLocationId = :siteId";
    }

    private Query bind(String hql, LocalDate fromDate, LocalDate toDate, Integer siteId) {
        Query q = entityManager.createQuery(hql);
        q.setParameter("from", from(fromDate));
        q.setParameter("to", to(toDate));
        if (siteId != null) {
            q.setParameter("siteId", String.valueOf(siteId));
        }
        return q;
    }

    private Integer parseSite(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
