package org.openelisglobal.reports.vectorsurveillance.manualentry.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder.ManualEntryFieldMap;
import org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder.ManualEntryMetricKeys;
import org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder.ManualEntryViewDTO;
import org.openelisglobal.reports.vectorsurveillance.service.VectorSurveillanceService;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceIndicesDTO;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceIndicesDTO.MirRow;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceIndicesDTO.PositivityRow;
import org.openelisglobal.reports.vectorsurveillance.valueholder.SurveillanceIndicesDTO.SpeciesRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Derives the eight default Manual Entry metrics from the computed
 * {@link SurveillanceIndicesDTO} and projects them through the field map (order
 * + visibility). The sporozoite row is flagged low-confidence when the overall
 * positive resolution is below {@value #SPOROZOITE_RESOLUTION_THRESHOLD_PCT}%
 * (US4-3); its computed rate is still shown, with a caveat, rather than
 * withheld. Each metric reads its source list directly, falling back to a
 * MIR-derived value when a richer source list is unavailable.
 */
@Service
public class ManualEntryViewServiceImpl implements ManualEntryViewService {

    static final double SPOROZOITE_RESOLUTION_THRESHOLD_PCT = 95.0;

    private final ManualEntryFieldMapService fieldMapService;
    private final VectorSurveillanceService surveillanceService;

    @Autowired
    public ManualEntryViewServiceImpl(ManualEntryFieldMapService fieldMapService,
            VectorSurveillanceService surveillanceService) {
        this.fieldMapService = fieldMapService;
        this.surveillanceService = surveillanceService;
    }

    @Override
    @Transactional(readOnly = true)
    public ManualEntryViewDTO getView(LocalDate periodStart, LocalDate periodEnd, Integer siteId) {
        ManualEntryViewDTO dto = new ManualEntryViewDTO(periodStart, periodEnd, siteId);
        SurveillanceIndicesDTO indices = surveillanceService.getIndices(periodStart, periodEnd, siteId);

        boolean sporozoiteLowConfidence = isSporozoiteLowConfidence(indices);

        for (ManualEntryFieldMap field : fieldMapService.getVisibleOrdered()) {
            String metricKey = field.getMetricKey();
            boolean lowConfidence = ManualEntryMetricKeys.SPOROZOITE_RATE.equals(metricKey) && sporozoiteLowConfidence;
            // The value is always derived and shown; below the resolution threshold
            // it is flagged low-confidence for a UI caveat, never withheld.
            String value = deriveValue(metricKey, indices);
            dto.getRows().add(new ManualEntryViewDTO.Row(metricKey, field.getLabel(), field.getPortalTag(), value,
                    lowConfidence));
        }
        return dto;
    }

    /**
     * Sporozoite low-confidence (US4-3): true when the overall positive resolution
     * is below threshold. With no MIR rows there is nothing to flag.
     */
    private boolean isSporozoiteLowConfidence(SurveillanceIndicesDTO indices) {
        List<MirRow> mir = indices.getMirBySpecies();
        if (mir == null || mir.isEmpty()) {
            return false;
        }
        long totalPositive = mir.stream().mapToLong(MirRow::getPositivePools).sum();
        if (totalPositive == 0) {
            return false;
        }
        // Pool-weighted overall resolution across species/pathogens.
        double weighted = mir.stream().mapToDouble(r -> r.getPositiveResolutionPct() * r.getPositivePools()).sum()
                / totalPositive;
        return weighted < SPOROZOITE_RESOLUTION_THRESHOLD_PCT;
    }

    private String deriveValue(String metricKey, SurveillanceIndicesDTO indices) {
        List<MirRow> mir = indices.getMirBySpecies();
        switch (metricKey) {
        case ManualEntryMetricKeys.POOLS_TESTED:
            // Prefer per-pathogen poolsTested; fall back to the positive-pool count
            // when pathogenPositivity is unavailable.
            long poolsTested = sumPoolsTested(indices);
            return Long.toString(poolsTested > 0 ? poolsTested : sumPositivePools(mir));
        case ManualEntryMetricKeys.POOLS_POSITIVE:
            return Long.toString(sumPositivePools(mir));
        case ManualEntryMetricKeys.CONFIRMED_POSITIVE_ORGANISMS:
            // Classical 1-per-positive-pool organism count.
            return Long.toString(sumPositivePools(mir));
        case ManualEntryMetricKeys.TOP_SPECIES:
            return topSpecies(indices);
        case ManualEntryMetricKeys.MIR_CLASSIC:
            return format(overallMirClassic(mir));
        case ManualEntryMetricKeys.MIR_OBSERVED:
            return format(maxObservedRate(mir));
        case ManualEntryMetricKeys.SITES_WITH_POSITIVES:
            return Long.toString(indices.getSitesWithPositives());
        case ManualEntryMetricKeys.SPOROZOITE_RATE:
            // Always shown; below the resolution threshold it is flagged low-confidence.
            Double sporozoite = indices.getSporozoiteRatePct();
            return sporozoite == null ? "—" : format(sporozoite);
        default:
            return "—";
        }
    }

    private static long sumPositivePools(List<MirRow> mir) {
        return mir == null ? 0 : mir.stream().mapToLong(MirRow::getPositivePools).sum();
    }

    private static long sumPoolsTested(SurveillanceIndicesDTO indices) {
        List<PositivityRow> positivity = indices.getPathogenPositivity();
        return positivity == null ? 0 : positivity.stream().mapToLong(PositivityRow::getPoolsTested).sum();
    }

    private static double overallMirClassic(List<MirRow> mir) {
        if (mir == null || mir.isEmpty()) {
            return 0.0;
        }
        long positive = mir.stream().mapToLong(MirRow::getPositivePools).sum();
        long specimens = mir.stream().mapToLong(MirRow::getTotalSpecimens).sum();
        return specimens > 0 ? ((double) positive / specimens) * 1000.0 : 0.0;
    }

    private static double maxObservedRate(List<MirRow> mir) {
        if (mir == null || mir.isEmpty()) {
            return 0.0;
        }
        return mir.stream().mapToDouble(MirRow::getInfectionRateObserved).max().orElse(0.0);
    }

    private static String topSpecies(SurveillanceIndicesDTO indices) {
        List<SpeciesRow> species = indices.getSpeciesDistribution();
        if (species != null && !species.isEmpty()) {
            SpeciesRow top = species.stream().max(Comparator.comparingLong(SpeciesRow::getSpecimenCount)).orElse(null);
            if (top != null) {
                return (top.getGenus() + " " + top.getSpecies()).trim();
            }
        }
        // Fall back to the species with the most positive pools when
        // speciesDistribution is unavailable.
        List<MirRow> mir = indices.getMirBySpecies();
        if (mir != null && !mir.isEmpty()) {
            MirRow top = mir.stream().max(Comparator.comparingLong(MirRow::getPositivePools)).orElse(null);
            if (top != null && top.getSpeciesId() != null) {
                return "species #" + top.getSpeciesId();
            }
        }
        return "—";
    }

    private static String format(double value) {
        // Locale.ROOT → dot-decimal output regardless of the JVM default locale
        // (the frontend parses these as numbers).
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
