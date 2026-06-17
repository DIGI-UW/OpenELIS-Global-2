package org.openelisglobal.reports.vectorsurveillance.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.stream.Collectors;
import org.openelisglobal.reports.vectorsurveillance.dao.VectorSurveillanceDAO;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.SpeciesMirAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceIndicesDTO;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceIndicesDTO.MirRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VectorSurveillanceServiceImpl implements VectorSurveillanceService {

    private final VectorSurveillanceDAO dao;

    @Autowired
    public VectorSurveillanceServiceImpl(VectorSurveillanceDAO dao) {
        this.dao = dao;
    }

    @Override
    @Transactional(readOnly = true)
    public SurveillanceIndicesDTO getIndices(LocalDate from, LocalDate to, Integer siteId) {
        SurveillanceIndicesDTO dto = new SurveillanceIndicesDTO();
        dto.setFreshness(new Timestamp(System.currentTimeMillis()));
        dto.setMirBySpecies(
                dao.getMirAggregates(from, to, siteId).stream().map(this::toMirRow).collect(Collectors.toList()));
        // density / species distribution / positivity / QC indices land with their
        // own tests (T005/T006) — getIndices grows one index at a time (incremental
        // TDD).
        return dto;
    }

    /**
     * MIR math (BR-V04-001): classical MIR and the deconvolution-aware observed
     * rate are both per-1,000 specimens; positive-resolution is the share of
     * positive pools fully deconvoluted. {@code sporozoiteRatePct} is left null
     * (deferred — only the &lt; 95% gating is in scope; see spec Clarifications).
     */
    private MirRow toMirRow(SpeciesMirAggregate a) {
        MirRow row = new MirRow();
        row.setSpeciesId(a.getSpeciesId());
        row.setPathogen(a.getPathogen());
        row.setPositivePools(a.getPositivePools());
        row.setTotalSpecimens(a.getTotalSpecimens());
        row.setMirClassic(perThousand(a.getPositivePools(), a.getTotalSpecimens()));
        row.setInfectionRateObserved(perThousand(a.getObservedPositiveOrganisms(), a.getTotalSpecimens()));
        row.setPositiveResolutionPct(pct(a.getCompletelyResolvedPositivePools(), a.getTotalPositivePools()));
        row.setSporozoiteRatePct(null);
        return row;
    }

    private static double perThousand(long numerator, long denominator) {
        return denominator > 0 ? ((double) numerator / denominator) * 1000.0 : 0.0;
    }

    private static double pct(long numerator, long denominator) {
        return denominator > 0 ? ((double) numerator / denominator) * 100.0 : 0.0;
    }
}
