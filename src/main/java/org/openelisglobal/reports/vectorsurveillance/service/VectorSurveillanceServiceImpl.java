package org.openelisglobal.reports.vectorsurveillance.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.reports.vectorsurveillance.dao.VectorSurveillanceDAO;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SiteOption;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.QcAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.SpeciesAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.SpeciesMirAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceIndicesDTO;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceIndicesDTO.DensityRow;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceIndicesDTO.MirRow;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceIndicesDTO.PositivityRow;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceIndicesDTO.QcPassRate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceIndicesDTO.SpeciesRow;
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

        dto.setCollectionDensity(dao
                .getCollectionDensity(from, to, siteId).stream().map(d -> new DensityRow(d.getPeriodLabel(),
                        d.getSiteId(), d.getSiteName(), d.getPoolCount(), d.getSpecimenCount()))
                .collect(Collectors.toList()));

        List<SpeciesAggregate> species = dao.getSpeciesDistribution(from, to, siteId);
        long speciesTotal = species.stream().mapToLong(SpeciesAggregate::getSpecimenCount).sum();
        dto.setSpeciesDistribution(
                species.stream().map(s -> new SpeciesRow(s.getSpeciesId(), s.getGenus(), s.getSpecies(),
                        s.getSpecimenCount(), pct(s.getSpecimenCount(), speciesTotal))).collect(Collectors.toList()));

        dto.setPathogenPositivity(dao
                .getPathogenPositivity(from, to, siteId).stream().map(p -> new PositivityRow(p.getPathogen(),
                        p.getPoolsPositive(), p.getPoolsTested(), pct(p.getPoolsPositive(), p.getPoolsTested())))
                .collect(Collectors.toList()));

        QcAggregate qc = dao.getQcPassRate(from, to, siteId);
        if (qc != null) {
            dto.setQcPassRate(new QcPassRate(qc.getAnalysesPassed(), qc.getAnalysesTotal(),
                    pct(qc.getAnalysesPassed(), qc.getAnalysesTotal())));
        }
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteOption> getSites() {
        return dao.getSites();
    }

    /**
     * MIR math (BR-V04-001). {@code sporozoiteRatePct} is left null (deferred —
     * only the &lt; 95% gating is in scope; see spec Clarifications).
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
