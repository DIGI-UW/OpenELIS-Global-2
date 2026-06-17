package org.openelisglobal.reports.vectorsurveillance.service;

import java.time.LocalDate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceIndicesDTO;

/**
 * Computes vector surveillance indices on demand from OpenELIS's own recorded
 * data (read model: OLTP-direct, see research D1). Returns fully-compiled DTOs
 * within the read-only transaction (Constitution IV).
 */
public interface VectorSurveillanceService {

    /** All indices for a scope. {@code siteId} null = all sites. */
    SurveillanceIndicesDTO getIndices(LocalDate from, LocalDate to, Integer siteId);
}
