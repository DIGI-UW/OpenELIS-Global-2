package org.openelisglobal.reports.vectorsurveillance.dao;

import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SiteOption;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.DensityAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.PositivityAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.QcAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.SpeciesAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.SpeciesMirAggregate;

/**
 * Read-only aggregation queries (HQL) over the vector OLTP model. Pure
 * data-retrieval (Constitution IV) — all index math lives in the service. Each
 * method is defensive: a query failure yields an empty result rather than
 * breaking the whole dashboard. {@code siteId} null = all sites; dates bound
 * {@code Sample.collectionDate}.
 */
public interface VectorSurveillanceDAO {

    List<DensityAggregate> getCollectionDensity(LocalDate from, LocalDate to, Integer siteId);

    List<SpeciesAggregate> getSpeciesDistribution(LocalDate from, LocalDate to, Integer siteId);

    List<SpeciesMirAggregate> getMirAggregates(LocalDate from, LocalDate to, Integer siteId);

    List<PositivityAggregate> getPathogenPositivity(LocalDate from, LocalDate to, Integer siteId);

    QcAggregate getQcPassRate(LocalDate from, LocalDate to, Integer siteId);

    List<SiteOption> getSites();
}
