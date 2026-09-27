package org.openelisglobal.reports.vectorsurveillance.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.reports.vectorsurveillance.dao.VectorSurveillanceDAO;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SiteOption;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceAggregates.EffortAggregate;
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

        // Collection density is per-site/period; the density rows carry only the site
        // id, so resolve
        // the name (the chart groups/labels each site's series by it).
        Map<Integer, String> siteNames = new HashMap<>();
        for (SiteOption s : dao.getSites()) {
            if (s.getId() != null) {
                siteNames.put(s.getId(), s.getName());
            }
        }
        // Sampling effort per (period, site): sum of (traps x nights) over the pools
        // that recorded trap effort. Density = abundance / effort. When no effort was
        // recorded, density is left null and the dashboard shows abundance only — never
        // a fabricated rate (the WHO/CDC abundance-vs-density distinction).
        Map<String, Long> effortByKey = new HashMap<>();
        for (EffortAggregate e : dao.getCollectionEffort(from, to, siteId)) {
            Integer traps = parsePositiveInt(e.getTrapCount());
            Integer nights = parsePositiveInt(e.getTrapNights());
            if (traps == null || nights == null) {
                continue;
            }
            effortByKey.merge(densityKey(e.getPeriodLabel(), e.getSiteId()), (long) traps * nights, Long::sum);
        }

        dto.setCollectionDensity(dao.getCollectionDensity(from, to, siteId).stream().map(d -> {
            DensityRow row = new DensityRow(d.getPeriodLabel(), d.getSiteId(), siteNames.get(d.getSiteId()),
                    d.getPoolCount(), d.getSpecimenCount());
            Long effort = effortByKey.get(densityKey(d.getPeriodLabel(), d.getSiteId()));
            if (effort != null && effort > 0) {
                row.setTrapNights(effort);
                row.setDensity((double) d.getSpecimenCount() / effort);
            }
            return row;
        }).collect(Collectors.toList()));

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
        // Data-quality guard: surface a warning when a result carries a significance
        // value outside the recognized set (never silently trust a mixed catalog).
        dto.setPositivityClassificationUnrecognized(dao.hasUnrecognizedPositivityClassification(from, to, siteId));
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

    private static String densityKey(String periodLabel, Integer siteId) {
        return periodLabel + "|" + siteId;
    }

    /**
     * Parses a positive integer observation value; null if blank, non-numeric, or
     * <= 0.
     */
    private static Integer parsePositiveInt(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
