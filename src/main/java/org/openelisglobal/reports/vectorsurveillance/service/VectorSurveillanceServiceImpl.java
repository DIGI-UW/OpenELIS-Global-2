package org.openelisglobal.reports.vectorsurveillance.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.reports.vectorsurveillance.dao.VectorSurveillanceDAO;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SiteOption;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.QcAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.SpeciesAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.SpeciesMirAggregate;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.SporozoiteAggregate;
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

        // Species distribution + per-species specimen totals (the MIR denominator).
        List<SpeciesAggregate> species = dao.getSpeciesDistribution(from, to, siteId);
        long speciesTotal = species.stream().mapToLong(SpeciesAggregate::getSpecimenCount).sum();
        Map<Integer, Long> speciesTotals = new HashMap<>();
        for (SpeciesAggregate s : species) {
            speciesTotals.put(s.getSpeciesId(), s.getSpecimenCount());
        }
        dto.setSpeciesDistribution(
                species.stream().map(s -> new SpeciesRow(s.getSpeciesId(), s.getGenus(), s.getSpecies(),
                        s.getSpecimenCount(), pct(s.getSpecimenCount(), speciesTotal))).collect(Collectors.toList()));

        // MIR per (species, pathogen) — positivity is catalog-driven (significance).
        dto.setMirBySpecies(dao.getMirAggregates(from, to, siteId).stream().map(a -> toMirRow(a, speciesTotals))
                .collect(Collectors.toList()));

        dto.setCollectionDensity(dao
                .getCollectionDensity(from, to, siteId).stream().map(d -> new DensityRow(d.getPeriodLabel(),
                        d.getSiteId(), d.getSiteName(), d.getPoolCount(), d.getSpecimenCount()))
                .collect(Collectors.toList()));

        dto.setPathogenPositivity(dao
                .getPathogenPositivity(from, to, siteId).stream().map(p -> new PositivityRow(p.getPathogen(),
                        p.getPoolsPositive(), p.getPoolsTested(), pct(p.getPoolsPositive(), p.getPoolsTested())))
                .collect(Collectors.toList()));

        QcAggregate qc = dao.getQcPassRate(from, to, siteId);
        if (qc != null) {
            dto.setQcPassRate(new QcPassRate(qc.getAnalysesPassed(), qc.getAnalysesTotal(),
                    pct(qc.getAnalysesPassed(), qc.getAnalysesTotal())));
        }

        // Sporozoite rate (Anopheles CSP-ELISA), MIR-style proportion as a percentage.
        SporozoiteAggregate spo = dao.getSporozoiteAggregate(from, to, siteId);
        if (spo != null && spo.getTotalSpecimens() > 0) {
            dto.setSporozoiteRatePct(pct(spo.getPositivePools(), spo.getTotalSpecimens()));
        }

        // Distinct sites with at least one positive pool (a top-level count).
        dto.setSitesWithPositives(dao.countSitesWithPositives(from, to, siteId));

        // Degradation: when no result carries a significance classification the
        // positivity-dependent panels must show "not configured", not fake zeros.
        dto.setPositivityConfigured(dao.isPositivityClassificationPresent(from, to, siteId));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SiteOption> getSites() {
        return dao.getSites();
    }

    /**
     * MIR math (BR-V04-001), per (species, pathogen). {@code mirClassic} = positive
     * pools ÷ species specimens × 1000; {@code infectionRateObserved} =
     * deconvolution-aware positive organisms ÷ specimens × 1000;
     * {@code positiveResolutionPct} = resolved ÷ positive pools. The sporozoite
     * rate is a top-level figure (Anopheles only), not a per-row column.
     */
    private MirRow toMirRow(SpeciesMirAggregate a, Map<Integer, Long> speciesTotals) {
        MirRow row = new MirRow();
        row.setSpeciesId(a.getSpeciesId());
        row.setSpeciesLabel(label(a.getGenus(), a.getSpecies()));
        row.setPathogen(a.getPathogen());
        long total = speciesTotals.getOrDefault(a.getSpeciesId(), 0L);
        row.setPositivePools(a.getPositivePools());
        row.setTotalSpecimens(total);
        row.setMirClassic(perThousand(a.getPositivePools(), total));
        row.setInfectionRateObserved(perThousand(a.getObservedPositiveOrganisms(), total));
        row.setPositiveResolutionPct(pct(a.getCompletelyResolvedPositivePools(), a.getTotalPositivePools()));
        return row;
    }

    private static String label(String genus, String species) {
        String g = genus == null ? "" : genus.trim();
        String s = species == null ? "" : species.trim();
        return (g + " " + s).trim();
    }

    private static double perThousand(long numerator, long denominator) {
        return denominator > 0 ? ((double) numerator / denominator) * 1000.0 : 0.0;
    }

    private static double pct(long numerator, long denominator) {
        return denominator > 0 ? ((double) numerator / denominator) * 100.0 : 0.0;
    }
}
