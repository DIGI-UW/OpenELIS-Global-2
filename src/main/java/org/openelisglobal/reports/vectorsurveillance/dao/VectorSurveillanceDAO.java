package org.openelisglobal.reports.vectorsurveillance.dao;

import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.SpeciesMirAggregate;

/**
 * Read-only aggregation queries (HQL) over the vector OLTP model. Pure
 * data-retrieval (Constitution IV) — all index math lives in the service. Grows
 * one method per index as that index's test/impl lands (incremental TDD).
 */
public interface VectorSurveillanceDAO {

    /**
     * Per species × pathogen raw MIR counts for the scope. CONFIRMED-only positive
     * results; QC samples excluded. {@code siteId} null = all sites.
     */
    List<SpeciesMirAggregate> getMirAggregates(LocalDate from, LocalDate to, Integer siteId);
}
