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
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.SporozoiteAggregate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * HQL aggregations over the vector OLTP model. Defensive by design — every
 * query is wrapped so a single failure degrades to an empty result (FR-012)
 * instead of breaking the dashboard.
 *
 * <p>
 * Positivity is read from the test catalog, NOT from
 * {@code deconvolutionStatus}. A pool result is positive when its
 * {@code Result} resolves to a {@code TestResult} whose
 * {@code significance = 'POSITIVE'} — the catalog-configured classification
 * (SILNAS distro). Deconvolution status is a value-agnostic workflow field
 * ({@code COMPLETE} covers both a fully-split positive and a confirmed-all
 * negative), so it cannot mark positivity. When no result carries a
 * significance tag the positivity-dependent indices report nothing (the service
 * surfaces a "not configured" state) rather than guessing.
 *
 * <p>
 * Pathogen granularity = the pathogen-detection {@code Test} (e.g. "Malaria
 * Parasite Detection"). Period labels use Postgres ISO-week.
 */
@Component
@Transactional(readOnly = true)
public class VectorSurveillanceDAOImpl implements VectorSurveillanceDAO {

    /** Catalog classification marking a positive surveillance result. */
    private static final String POSITIVE = "POSITIVE";

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
        if (o == null) {
            return null;
        }
        // Some ids (e.g. Test.id) are mapped via LIMSStringNumberUserType and come
        // back as String from HQL; others (VectorSpecies.id, site ids) are Number.
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            return Integer.valueOf(o.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
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
                out.add(new DensityAggregate((String) r[0], parseSite(r[1]), null, lng(r[2]), lng(r[3])));
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
                    + " where vsi.sampleItemId = cast(si.id as long) and si.sample.id = s.id"
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

    /**
     * Per species × pathogen-test, the catalog-driven positive-pool counts.
     * {@code positivePools} = distinct intake pools with a POSITIVE catalog result
     * for the test that also contain a CONFIRMED specimen of the species;
     * {@code completelyResolvedPositivePools} = those with deconvolution COMPLETE;
     * {@code observedPositiveOrganisms} = exact positive individual specimens
     * (deconvolution-aware) plus a classical 1-per-pool fallback for unresolved
     * positive pools. {@code totalSpecimens} is filled by the service from the
     * species distribution.
     */
    @Override
    public List<SpeciesMirAggregate> getMirAggregates(LocalDate fromDate, LocalDate toDate, Integer siteId) {
        try {
            // Positive intake pools per (species, pathogen test) + resolved subset.
            String hql = "select sp.id, sp.genus, sp.species, t.id, t.description," + " count(distinct p.id),"
                    + " count(distinct case when p.deconvolutionStatus = 'COMPLETE' then p.id else null end)"
                    + " from VectorPool p, Sample s, VectorPoolMember vpm, SampleItem si,"
                    + " VectorSpecimenIdentification vsi, VectorSpecies sp,"
                    + " Analysis a, Result r, TestResult tr, Test t"
                    + " where s.id = p.sampleId and p.parentPool is null" + " and vpm.pool = p and vpm.sampleItem = si"
                    + " and vsi.sampleItemId = cast(si.id as long) and vsi.vectorSpeciesId = sp.id"
                    + " and vsi.confidence = 'CONFIRMED'"
                    + " and cast(a.vectorPoolId as integer) = p.id and r.analysis = a"
                    + " and r.testResult = tr and tr.test = t and tr.significance = :positive"
                    + " and s.collectionDate between :from and :to" + siteClause(siteId)
                    + " group by sp.id, sp.genus, sp.species, t.id, t.description";
            List<?> rows = bind(hql, fromDate, toDate, siteId).getResultList();

            List<SpeciesMirAggregate> out = new ArrayList<>();
            for (Object row : rows) {
                Object[] r = (Object[]) row;
                Integer speciesId = integer(r[0]);
                String pathogen = (String) r[4];
                Integer testId = integer(r[3]);
                long positivePools = lng(r[5]);
                long resolved = lng(r[6]);
                long observed = observedPositiveOrganisms(speciesId, testId, fromDate, toDate, siteId, positivePools,
                        resolved);
                out.add(new SpeciesMirAggregate(speciesId, (String) r[1], (String) r[2], pathogen, positivePools,
                        0L /* totalSpecimens filled by service */, observed, positivePools, resolved));
            }
            return out;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return Collections.emptyList();
        }
    }

    /**
     * Deconvolution-aware positive-organism count for a (species, test): the exact
     * number of individual specimens (deconvoluted leaves, sampleItem-level
     * analyses) with a POSITIVE catalog result, plus a classical 1-per-pool
     * fallback for positive pools that have not been resolved to individuals.
     */
    private long observedPositiveOrganisms(Integer speciesId, Integer testId, LocalDate fromDate, LocalDate toDate,
            Integer siteId, long positivePools, long resolvedPools) {
        long unresolved = Math.max(0, positivePools - resolvedPools);
        if (speciesId == null || testId == null) {
            return positivePools; // classical fallback
        }
        try {
            String hql = "select count(distinct si.id)"
                    + " from Analysis a, Result r, TestResult tr, SampleItem si, Sample s,"
                    + " VectorSpecimenIdentification vsi"
                    + " where a.sampleItem = si and a.vectorPoolId is null and r.analysis = a"
                    + " and r.testResult = tr and tr.test.id = :testId and tr.significance = :positive"
                    + " and vsi.sampleItemId = cast(si.id as long) and vsi.vectorSpeciesId = :speciesId"
                    + " and vsi.confidence = 'CONFIRMED' and si.sample.id = s.id"
                    + " and s.collectionDate between :from and :to" + siteClause(siteId);
            Query q = entityManager.createQuery(hql);
            q.setParameter("positive", POSITIVE);
            // Test.id is mapped as a String (LIMSStringNumberUserType), so bind the
            // catalog test id as a String — a numeric bind throws a parameter-type
            // mismatch and silently collapses every MIR row to the classical fallback.
            q.setParameter("testId", String.valueOf(testId));
            q.setParameter("speciesId", speciesId.longValue());
            q.setParameter("from", from(fromDate));
            q.setParameter("to", to(toDate));
            if (siteId != null) {
                q.setParameter("siteId", siteId.longValue());
            }
            long individualPositives = lng(q.getSingleResult());
            return individualPositives + unresolved;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return positivePools; // classical fallback on any failure
        }
    }

    @Override
    public List<PositivityAggregate> getPathogenPositivity(LocalDate fromDate, LocalDate toDate, Integer siteId) {
        try {
            // Pools tested per pathogen (have an analysis for the test) and the positive
            // subset (POSITIVE catalog result). Site filter applied via the pool members.
            String hql = "select t.description,"
                    + " count(distinct case when tr.significance = :positive then p.id else null end),"
                    + " count(distinct p.id)"
                    + " from VectorPool p, Sample s, Analysis a, Result r, TestResult tr, Test t"
                    + " where s.id = p.sampleId and p.parentPool is null"
                    + " and cast(a.vectorPoolId as integer) = p.id and r.analysis = a"
                    + " and r.testResult = tr and tr.test = t" + " and s.collectionDate between :from and :to"
                    + sitePoolClause(siteId) + " group by t.description order by t.description";
            Query q = entityManager.createQuery(hql);
            q.setParameter("positive", POSITIVE);
            q.setParameter("from", from(fromDate));
            q.setParameter("to", to(toDate));
            if (siteId != null) {
                q.setParameter("siteId", siteId.longValue());
            }
            List<?> rows = q.getResultList();
            List<PositivityAggregate> out = new ArrayList<>();
            for (Object row : rows) {
                Object[] r = (Object[]) row;
                out.add(new PositivityAggregate((String) r[0], lng(r[1]), lng(r[2])));
            }
            return out;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return Collections.emptyList();
        }
    }

    /**
     * Sporozoite rate inputs: Anopheles pools POSITIVE for the CSP-ELISA assay over
     * total Anopheles specimens tested. The assay is identified by LOINC 71712-2,
     * with a description fallback when LOINC is absent.
     */
    @Override
    public SporozoiteAggregate getSporozoiteAggregate(LocalDate fromDate, LocalDate toDate, Integer siteId) {
        try {
            // (1) Anopheles intake pools POSITIVE for the CSP-ELISA assay. LOINC
            // 71712-2 identifies it precisely; the description fallback excludes
            // Plasmodium PCR assays so confirmatory PCR positives do not inflate it.
            String testMatch = " and (t.loinc = '71712-2'"
                    + " or lower(t.description) like '%sporozoite%' or lower(t.description) like '%csp%')";
            String posHql = "select count(distinct p.id)"
                    + " from VectorPool p, Sample s, VectorPoolMember vpm, SampleItem si,"
                    + " VectorSpecimenIdentification vsi, VectorSpecies sp,"
                    + " Analysis a, Result r, TestResult tr, Test t"
                    + " where s.id = p.sampleId and p.parentPool is null" + " and vpm.pool = p and vpm.sampleItem = si"
                    + " and vsi.sampleItemId = cast(si.id as long) and vsi.vectorSpeciesId = sp.id"
                    + " and vsi.confidence = 'CONFIRMED' and lower(sp.genus) = 'anopheles'"
                    + " and cast(a.vectorPoolId as integer) = p.id and r.analysis = a"
                    + " and r.testResult = tr and tr.test = t and tr.significance = :positive" + testMatch
                    + " and s.collectionDate between :from and :to" + siteClause(siteId);
            Query posQ = entityManager.createQuery(posHql);
            posQ.setParameter("positive", POSITIVE);
            posQ.setParameter("from", from(fromDate));
            posQ.setParameter("to", to(toDate));
            if (siteId != null) {
                posQ.setParameter("siteId", siteId.longValue());
            }
            long positivePools = lng(posQ.getSingleResult());

            // (2) Total Anopheles specimens tested in scope (one row per specimen ID).
            String totHql = "select coalesce(sum(si.quantity), 0)"
                    + " from VectorSpecimenIdentification vsi, SampleItem si, Sample s, VectorSpecies sp"
                    + " where vsi.sampleItemId = cast(si.id as long) and vsi.vectorSpeciesId = sp.id"
                    + " and vsi.confidence = 'CONFIRMED' and lower(sp.genus) = 'anopheles'"
                    + " and si.sample.id = s.id and s.collectionDate between :from and :to" + siteClause(siteId);
            Query totQ = entityManager.createQuery(totHql);
            totQ.setParameter("from", from(fromDate));
            totQ.setParameter("to", to(toDate));
            if (siteId != null) {
                totQ.setParameter("siteId", siteId.longValue());
            }
            long totalSpecimens = lng(totQ.getSingleResult());

            return new SporozoiteAggregate(positivePools, totalSpecimens);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return new SporozoiteAggregate(0, 0);
        }
    }

    /**
     * True when at least one vector pool result in scope carries a catalog
     * significance classification. When false (results exist but none are
     * classified) the service surfaces a "positivity not configured" state instead
     * of fabricating zeros.
     */
    @Override
    public boolean isPositivityClassificationPresent(LocalDate fromDate, LocalDate toDate, Integer siteId) {
        try {
            String hql = "select count(r.id) from VectorPool p, Sample s, Analysis a, Result r, TestResult tr"
                    + " where s.id = p.sampleId and p.parentPool is null"
                    + " and cast(a.vectorPoolId as integer) = p.id and r.analysis = a"
                    + " and r.testResult = tr and tr.significance is not null"
                    + " and s.collectionDate between :from and :to" + sitePoolClause(siteId);
            Query q = entityManager.createQuery(hql);
            q.setParameter("from", from(fromDate));
            q.setParameter("to", to(toDate));
            if (siteId != null) {
                q.setParameter("siteId", siteId.longValue());
            }
            return lng(q.getSingleResult()) > 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            return false;
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

    /** Site filter on the specimen's collection location (density/species/MIR). */
    private String siteClause(Integer siteId) {
        return siteId == null ? "" : " and cast(si.collectionLocationId as long) = :siteId";
    }

    /**
     * Site filter for pool-level queries with no SampleItem alias — scope via the
     * pool's member specimens.
     */
    private String sitePoolClause(Integer siteId) {
        return siteId == null ? ""
                : " and exists (select 1 from VectorPoolMember vpm2, SampleItem si2 where vpm2.pool = p"
                        + " and vpm2.sampleItem = si2 and cast(si2.collectionLocationId as long) = :siteId)";
    }

    private Query bind(String hql, LocalDate fromDate, LocalDate toDate, Integer siteId) {
        Query q = entityManager.createQuery(hql);
        if (hql.contains(":positive")) {
            q.setParameter("positive", POSITIVE);
        }
        q.setParameter("from", from(fromDate));
        q.setParameter("to", to(toDate));
        if (siteId != null) {
            q.setParameter("siteId", siteId.longValue());
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
