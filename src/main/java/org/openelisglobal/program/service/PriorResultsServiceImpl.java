package org.openelisglobal.program.service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.program.dao.ProgramSampleDAO;
import org.openelisglobal.program.util.DictionaryEntryNames;
import org.openelisglobal.program.valueholder.AnatomicPathologyBench;
import org.openelisglobal.program.valueholder.PriorCase;
import org.openelisglobal.program.valueholder.PriorResultRow;
import org.openelisglobal.program.valueholder.PriorResults;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.program.valueholder.cytology.CytologyDiagnosis;
import org.openelisglobal.program.valueholder.cytology.CytologyDiagnosis.CytologyDiagnosisResultType;
import org.openelisglobal.program.valueholder.cytology.CytologyDiagnosis.DiagnosisCategory;
import org.openelisglobal.program.valueholder.cytology.CytologyDiagnosisCategoryResultsMap;
import org.openelisglobal.program.valueholder.cytology.CytologySpecimenAdequacy.SpecimenAdequancySatisfaction;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion.ConclusionType;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PriorResultsServiceImpl implements PriorResultsService {

    static final String REPORTED = "REPORTED";
    static final String NOT_REPORTED = "NOT_REPORTED";
    static final String NONE_RECORDED = "NONE_RECORDED";
    static final String IN_REPORT = "IN_REPORT";

    /** The stage name a signed-out case carries on all three benches. */
    private static final String SIGNED_OUT = "COMPLETED";

    private static final int DEFAULT_ROWS = 5;
    private static final int MOST_ROWS = 50;

    /** The order the cytology report prints its categories in. */
    private static final List<DiagnosisCategory> CYTOLOGY_CATEGORIES = List.of(
            DiagnosisCategory.NON_NEOPLASTIC_CELLULAR_VARIATIONS, DiagnosisCategory.REACTIVE_CELLULAR_CHANGES,
            DiagnosisCategory.ORGANISMS, DiagnosisCategory.EPITHELIAL_CELL_ABNORMALITY, DiagnosisCategory.OTHER);

    /**
     * Newest request first, then the larger order number, then pathology before
     * immunohistochemistry before cytology, then the larger case id.
     */
    private static final Comparator<PriorCase> NEWEST_FIRST = Comparator
            .comparing((PriorCase c) -> requestDate(c.sample()), Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(c -> orderId(c.sample()), Comparator.reverseOrder()).thenComparing(PriorCase::bench)
            .thenComparing(PriorCase::caseId, Comparator.reverseOrder());

    private final ProgramSampleDAO programSampleDAO;
    private final ProgramSampleLabUnitScope programSampleLabUnitScope;
    private final SampleItemService sampleItemService;
    private final DictionaryService dictionaryService;

    public PriorResultsServiceImpl(ProgramSampleDAO programSampleDAO,
            ProgramSampleLabUnitScope programSampleLabUnitScope, SampleItemService sampleItemService,
            DictionaryService dictionaryService) {
        this.programSampleDAO = programSampleDAO;
        this.programSampleLabUnitScope = programSampleLabUnitScope;
        this.sampleItemService = sampleItemService;
        this.dictionaryService = dictionaryService;
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramSample caseOf(AnatomicPathologyBench bench, Integer id) {
        return programSampleDAO.getCase(bench, id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public PriorResults priorResultsOf(AnatomicPathologyBench bench, Integer id, boolean all, String sysUserId) {
        boolean restricted = programSampleLabUnitScope.isRestricted(sysUserId);
        ProgramSample onScreen = caseOf(bench, id);
        if (onScreen == null || onScreen.getSample() == null) {
            return PriorResults.none(restricted);
        }
        Sample order = onScreen.getSample();

        List<PriorCase> prior = programSampleDAO.getCasesSharingPatientWith(order.getId()).stream()
                .filter(c -> !(c.bench() == bench && c.caseId().equals(id)) && isPrior(c.sample(), order))
                .collect(Collectors.toCollection(ArrayList::new));

        if (restricted) {
            Map<String, Boolean> visibleOrders = new HashMap<>();
            prior.removeIf(c -> !visibleOrders.computeIfAbsent(c.sample().getId(),
                    orderId -> programSampleLabUnitScope.isOrderVisible(sysUserId, c.sample())));
        }

        prior.sort(NEWEST_FIRST);
        int total = prior.size();
        List<PriorCase> shown = all ? prior : prior.subList(0, Math.min(rowsSetting(), total));
        return new PriorResults(rowsOf(shown), total, restricted);
    }

    /**
     * A case is prior when it shares the order on screen, or its order was
     * requested earlier, or on the same day with a smaller order number.
     */
    private static boolean isPrior(Sample other, Sample onScreen) {
        if (Objects.equals(other.getId(), onScreen.getId())) {
            return true;
        }
        LocalDate otherDate = requestDate(other);
        LocalDate onScreenDate = requestDate(onScreen);
        if (otherDate == null || onScreenDate == null) {
            return false;
        }
        int byDate = otherDate.compareTo(onScreenDate);
        return byDate < 0 || (byDate == 0 && orderId(other) < orderId(onScreen));
    }

    private List<PriorResultRow> rowsOf(List<PriorCase> shown) {
        Map<String, String> specimens = new HashMap<>();
        Map<Integer, List<Object[]>> conclusions = pathologyConclusions(shown);
        Map<Integer, CytologyResult> diagnoses = cytologyResults(shown);
        List<PriorResultRow> rows = new ArrayList<>();
        for (PriorCase c : shown) {
            Sample sample = c.sample();
            String specimen = specimens.computeIfAbsent(sample.getId(), this::specimenOf);
            Conclusion conclusion = conclusionOf(c, conclusions, diagnoses);
            LocalDate requestDate = requestDate(sample);
            rows.add(new PriorResultRow(c.bench().name(), c.caseId(), sample.getAccessionNumber(),
                    requestDate == null ? null : requestDate.toString(), specimen, conclusion.state(),
                    conclusion.text()));
        }
        return rows;
    }

    /** The conclusion cell: its state, and the text only when REPORTED. */
    private record Conclusion(String state, String text) {
    }

    /** A signed-out cytology case's diagnosis and specimen adequacy. */
    private record CytologyResult(CytologyDiagnosis diagnosis, SpecimenAdequancySatisfaction satisfaction) {
    }

    private Conclusion conclusionOf(PriorCase c, Map<Integer, List<Object[]>> conclusions,
            Map<Integer, CytologyResult> diagnoses) {
        if (!SIGNED_OUT.equals(c.status())) {
            return new Conclusion(NOT_REPORTED, null);
        }
        if (c.bench() == AnatomicPathologyBench.IMMUNOHISTOCHEMISTRY) {
            // The IHC case stores no conclusion; it is written into its report only.
            return new Conclusion(IN_REPORT, null);
        }
        Optional<String> text = c.bench() == AnatomicPathologyBench.PATHOLOGY
                ? pathologyConclusion(conclusions.getOrDefault(c.caseId(), List.of()))
                : cytologyConclusion(diagnoses.get(c.caseId()));
        return text.map(t -> new Conclusion(REPORTED, t)).orElseGet(() -> new Conclusion(NONE_RECORDED, null));
    }

    /**
     * The coded conclusions' names in stored order, retired entries included and
     * unknown ids skipped; when none resolves, the free text if it is not blank.
     */
    private Optional<String> pathologyConclusion(List<Object[]> rows) {
        List<String> names = new ArrayList<>();
        String text = null;
        for (Object[] row : rows) {
            String value = (String) row[2];
            if (row[1] == ConclusionType.DICTIONARY) {
                DictionaryEntryNames.localizedNameOf(dictionaryService, value).ifPresent(names::add);
            } else if (row[1] == ConclusionType.TEXT && StringUtils.isNotBlank(value)) {
                text = value;
            }
        }
        if (!names.isEmpty()) {
            return Optional.of(String.join(", ", names));
        }
        return Optional.ofNullable(text);
    }

    /**
     * What the cytology report prints: an unsatisfactory specimen's adequacy line
     * first, then what it prints for the diagnosis, joined.
     */
    private Optional<String> cytologyConclusion(CytologyResult result) {
        if (result == null) {
            return Optional.empty();
        }
        List<String> printed = new ArrayList<>();
        if (result.satisfaction() == SpecimenAdequancySatisfaction.UN_SATISFACTORY_FOR_EVALUATION) {
            // The English label ends in a space, which the joined cell would show.
            printed.add(MessageUtil.getMessage("cytology.label.unsatisfactory").strip());
        }
        cytologyDiagnosis(result.diagnosis()).ifPresent(printed::add);
        return printed.isEmpty() ? Optional.empty() : Optional.of(String.join(", ", printed));
    }

    /**
     * What the cytology report prints for the diagnosis: the negative sentence, or
     * each category's dictionary names or first text, in the report's category
     * order.
     */
    private Optional<String> cytologyDiagnosis(CytologyDiagnosis diagnosis) {
        if (diagnosis == null) {
            return Optional.empty();
        }
        if (Boolean.TRUE.equals(diagnosis.getNegativeDiagnosis())) {
            return Optional.of(MessageUtil.getMessage("cytology.label.negative"));
        }
        Map<DiagnosisCategory, List<String>> byCategory = new EnumMap<>(DiagnosisCategory.class);
        List<CytologyDiagnosisCategoryResultsMap> maps = diagnosis.getDiagnosisResultsMaps() == null ? List.of()
                : diagnosis.getDiagnosisResultsMaps().stream()
                        .sorted(Comparator.comparing(CytologyDiagnosisCategoryResultsMap::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .collect(Collectors.toList());
        for (CytologyDiagnosisCategoryResultsMap map : maps) {
            if (map.getCategory() == null) {
                continue;
            }
            List<String> results = map.getResults() == null ? List.of() : map.getResults();
            List<String> names = new ArrayList<>();
            if (map.getResultType() == CytologyDiagnosisResultType.DICTIONARY) {
                results.forEach(
                        id -> DictionaryEntryNames.localizedNameOf(dictionaryService, id).ifPresent(names::add));
            } else if (!results.isEmpty() && StringUtils.isNotBlank(results.get(0))) {
                names.add(results.get(0));
            }
            // As the report does: epithelial findings add up, every other category
            // shows its last entry.
            if (map.getCategory() == DiagnosisCategory.EPITHELIAL_CELL_ABNORMALITY) {
                byCategory.computeIfAbsent(map.getCategory(), k -> new ArrayList<>()).addAll(names);
            } else {
                byCategory.put(map.getCategory(), names);
            }
        }
        List<String> printed = CYTOLOGY_CATEGORIES.stream()
                .flatMap(category -> byCategory.getOrDefault(category, List.of()).stream())
                .collect(Collectors.toList());
        return printed.isEmpty() ? Optional.empty() : Optional.of(String.join(", ", printed));
    }

    private Map<Integer, List<Object[]>> pathologyConclusions(List<PriorCase> shown) {
        List<Integer> ids = signedOut(shown, AnatomicPathologyBench.PATHOLOGY);
        Map<Integer, List<Object[]>> byCase = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : programSampleDAO.getPathologyConclusions(ids)) {
                byCase.computeIfAbsent((Integer) row[0], k -> new ArrayList<>()).add(row);
            }
        }
        return byCase;
    }

    private Map<Integer, CytologyResult> cytologyResults(List<PriorCase> shown) {
        List<Integer> ids = signedOut(shown, AnatomicPathologyBench.CYTOLOGY);
        Map<Integer, CytologyResult> byCase = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : programSampleDAO.getCytologyDiagnoses(ids)) {
                byCase.put((Integer) row[0],
                        new CytologyResult((CytologyDiagnosis) row[1], (SpecimenAdequancySatisfaction) row[2]));
            }
        }
        return byCase;
    }

    private static List<Integer> signedOut(List<PriorCase> shown, AnatomicPathologyBench bench) {
        return shown.stream().filter(c -> c.bench() == bench && SIGNED_OUT.equals(c.status())).map(PriorCase::caseId)
                .collect(Collectors.toList());
    }

    /**
     * The order's specimen types, one per sample item in item order, joined; blank
     * when the order has no items. The same rule as the private specimen list of
     * {@code PathologyDisplayServiceImpl}, which the pathology case view prints.
     */
    private String specimenOf(String sampleId) {
        return sampleItemService.getSampleItemsBySampleId(sampleId).stream().map(SampleItem::getTypeOfSample)
                .filter(Objects::nonNull).map(TypeOfSample::getLocalizedName).collect(Collectors.joining(", "));
    }

    /**
     * The site's row setting: a whole number from 1 to 50, anything else, or no
     * value, means five.
     */
    private static int rowsSetting() {
        String value = ConfigurationProperties.getInstance().getPropertyValue(Property.PATHOLOGY_PRIOR_RESULTS_ROWS);
        if (value == null || !value.matches("\\d{1,2}")) {
            return DEFAULT_ROWS;
        }
        int rows = Integer.parseInt(value);
        return rows >= 1 && rows <= MOST_ROWS ? rows : DEFAULT_ROWS;
    }

    /** The calendar day the order was requested; the column is a date. */
    private static LocalDate requestDate(Sample sample) {
        java.util.Date entered = sample.getEnteredDate();
        // Not toInstant(): java.sql.Date throws from it.
        return entered == null ? null : new Date(entered.getTime()).toLocalDate();
    }

    private static long orderId(Sample sample) {
        return Long.parseLong(sample.getId());
    }
}
