package org.openelisglobal.reports.vectorsurveillance.daoimpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.openelisglobal.reports.vectorsurveillance.dao.VectorSurveillanceDAO;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.SpeciesMirAggregate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Minimal {@link VectorSurveillanceDAO} bean so the application context can be
 * built. M1 shipped the {@code VectorSurveillanceService}/{@code Impl} and the
 * {@code VectorSurveillanceDAO} interface, but no implementing bean — which
 * left {@code vectorSurveillanceServiceImpl} unsatisfiable and broke the entire
 * Spring context (and all integration tests, and app boot). This placeholder
 * resolves that wiring.
 *
 * <p>
 * <b>Ownership:</b> the real MIR-aggregation HQL belongs to M1 (it spans pools,
 * members, sample items, identifications, results and the QC catalog and is
 * unit-tested there against the OLTP model). M1 lands those queries one index
 * at a time (see {@code VectorSurveillanceServiceImpl} — "getIndices grows one
 * index at a time"). Until then this returns an empty aggregate set, which is
 * the correct "no indices computed yet" state. <em>This file is provided only
 * to unblock context startup and must be replaced by M1's real
 * implementation.</em>
 */
@Repository
@Transactional
public class VectorSurveillanceDAOImpl implements VectorSurveillanceDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<SpeciesMirAggregate> getMirAggregates(LocalDate from, LocalDate to, Integer siteId) {
        // Placeholder pending M1's real HQL aggregation (see class javadoc).
        return Collections.emptyList();
    }
}
