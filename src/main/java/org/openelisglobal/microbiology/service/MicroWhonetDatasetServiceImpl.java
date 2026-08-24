package org.openelisglobal.microbiology.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstReadingDAO;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCaseOrderDetailDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.dao.MicroPatientOriginDAO;
import org.openelisglobal.microbiology.dao.MicroWorklistContextDAO;
import org.openelisglobal.microbiology.form.MicroWhonetExportQueryForm;
import org.openelisglobal.microbiology.form.MicroWhonetFilterOptionForm;
import org.openelisglobal.microbiology.form.MicroWhonetFilterOptionsForm;
import org.openelisglobal.microbiology.form.MicroWhonetPatientContext;
import org.openelisglobal.microbiology.form.MicroWhonetPreviewForm;
import org.openelisglobal.microbiology.form.MicroWhonetPreviewRowForm;
import org.openelisglobal.microbiology.form.MicroWhonetWarningForm;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunStatus;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseOrderDetail;
import org.openelisglobal.microbiology.valueholder.MicroCulturePurpose;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.microbiology.valueholder.MicroPatientOrigin;
import org.openelisglobal.microbiology.valueholder.MicroWhonetExportSelection;
import org.openelisglobal.reports.action.implementation.reportBeans.WHONETCSVRoutineColumnBuilder.WHONetRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroWhonetDatasetServiceImpl implements MicroWhonetDatasetService {

    private static final String ALL = "ALL";
    private static final String NONE = "NONE";
    private static final String FIRST_ISOLATE_7_DAY = "FIRST_ISOLATE_7_DAY";
    private static final String FIRST_ISOLATE_14_DAY = "FIRST_ISOLATE_14_DAY";
    private static final String FIRST_ISOLATE_30_DAY = "FIRST_ISOLATE_30_DAY";
    private static final String COLLECTION_DATE = "COLLECTION_DATE";
    private static final String RELEASE_DATE = "RELEASE_DATE";
    private static final String ANY_SOURCE = "ANY_SOURCE";
    private static final String SAME_SOURCE = "SAME_SOURCE";
    private static final String INSENSITIVE = "INSENSITIVE";
    private static final String SENSITIVE = "SENSITIVE";
    private static final Map<String, Integer> FIRST_ISOLATE_WINDOWS = Map.of(FIRST_ISOLATE_7_DAY, 7,
            FIRST_ISOLATE_14_DAY, 14, FIRST_ISOLATE_30_DAY, 30);
    private static final Set<String> WHONET_INTERPRETATIONS = Set.of("S", "I", "R");

    private final MicroCaseDAO caseDAO;
    private final MicroCaseOrderDetailDAO caseOrderDetailDAO;
    private final MicroIsolateDAO isolateDAO;
    private final MicroAstRunDAO astRunDAO;
    private final MicroAstReadingDAO astReadingDAO;
    private final MicroOrganismDAO organismDAO;
    private final MicroPatientOriginDAO patientOriginDAO;
    private final MicroAntibioticDAO antibioticDAO;
    private final MicroWorklistContextDAO worklistContextDAO;

    public MicroWhonetDatasetServiceImpl(MicroCaseDAO caseDAO, MicroCaseOrderDetailDAO caseOrderDetailDAO,
            MicroIsolateDAO isolateDAO, MicroAstRunDAO astRunDAO, MicroAstReadingDAO astReadingDAO,
            MicroOrganismDAO organismDAO, MicroPatientOriginDAO patientOriginDAO, MicroAntibioticDAO antibioticDAO,
            MicroWorklistContextDAO worklistContextDAO) {
        this.caseDAO = caseDAO;
        this.caseOrderDetailDAO = caseOrderDetailDAO;
        this.isolateDAO = isolateDAO;
        this.astRunDAO = astRunDAO;
        this.astReadingDAO = astReadingDAO;
        this.organismDAO = organismDAO;
        this.patientOriginDAO = patientOriginDAO;
        this.antibioticDAO = antibioticDAO;
        this.worklistContextDAO = worklistContextDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroWhonetDataset compile(MicroWhonetExportQueryForm requestedQuery) {
        NormalizedQuery query = normalize(requestedQuery);
        Population population = loadPopulation(query);

        List<Candidate> candidates = new ArrayList<>();
        for (MicroCase microCase : population.cases) {
            PatientContext context = population.contextsByCase.getOrDefault(microCase.getId(), new PatientContext());
            for (MicroIsolate isolate : valuesFor(population.isolatesByCase, microCase.getId())) {
                candidates.add(new Candidate(microCase, isolate, context));
            }
        }
        candidates = candidates.stream().filter(
                candidate -> query.specimen.isEmpty() || query.specimen.contains(candidate.context.specimenTypeId))
                .toList();
        int afterSpecimen = candidates.size();
        candidates = candidates.stream().filter(
                candidate -> query.organism.isEmpty() || query.organism.contains(candidate.isolate.getOrganismId()))
                .toList();
        int afterOrganism = candidates.size();
        candidates = candidates.stream()
                .filter(candidate -> query.origin.isEmpty() || query.origin.contains(candidate.context.patientOrigin))
                .toList();
        int afterPatientOrigin = candidates.size();
        int clinicalPurposeCases = countCasesWithPurpose(population.cases, population.contextsByCase,
                MicroCulturePurpose.CLINICAL_DIAGNOSTIC.name());
        int screeningPurposeCases = countCasesWithPurpose(population.cases, population.contextsByCase,
                MicroCulturePurpose.ACTIVE_SCREENING.name());
        int unspecifiedPurposeCases = population.cases.size() - clinicalPurposeCases - screeningPurposeCases;
        candidates = candidates.stream().filter(candidate -> isPurposeIncluded(candidate.context.culturePurpose,
                query.includeScreening, query.includeUnspecified)).toList();
        int afterCulturePurpose = candidates.size();
        candidates = candidates.stream()
                .filter(candidate -> query.significance.contains(candidate.isolate.getSignificance())).toList();
        int afterSignificance = candidates.size();
        if (!NONE.equals(query.dedup) && query.excludeContaminants) {
            candidates = candidates.stream().filter(candidate -> !MicroIsolateSignificance.CONTAMINANT.name()
                    .equals(candidate.isolate.getSignificance())).toList();
        }
        AstData astData = !NONE.equals(query.dedup) && SENSITIVE.equals(query.profileSensitivity)
                ? loadReportableAstData(candidates)
                : null;
        candidates = deduplicate(candidates, query, astData);
        if (astData == null) {
            astData = loadReportableAstData(candidates);
        }

        List<WHONetRow> exportRows = new ArrayList<>();
        List<MicroWhonetPreviewRowForm> previewRows = new ArrayList<>();
        Map<String, MicroWhonetWarningForm> warnings = new LinkedHashMap<>();
        int exportableIsolates = 0;
        int excludedRows = 0;
        for (Candidate candidate : candidates) {
            List<MicroAstReading> readings = readingsFor(candidate, astData);
            if (!hasText(candidate.context.specimenType)) {
                int excluded = Math.max(1, readings.size());
                excludedRows += excluded;
                addWarning(warnings, "SPECIMEN_MAPPING_REQUIRED", "specimen-types", candidate.context.specimenTypeId,
                        candidate.context.specimenTypeLabel, excluded);
                continue;
            }
            Optional<MicroOrganism> organism = Optional
                    .ofNullable(mappedReference(population.organismsById, candidate.isolate.getOrganismId()));
            if (organism.isEmpty()) {
                organism = optionalReference(candidate.isolate.getOrganismId(), organismDAO::get);
            }
            if (organism.isEmpty() || !hasText(organism.get().getWhonetCode())) {
                int excluded = Math.max(1, readings.size());
                excludedRows += excluded;
                addWarning(warnings, "ORGANISM_MAPPING_REQUIRED", "organisms", candidate.isolate.getOrganismId(),
                        organism.map(MicroOrganism::getDisplayName).orElse(candidate.isolate.getIsolateLabel()),
                        excluded);
                continue;
            }
            if (readings.isEmpty()) {
                excludedRows++;
                addWarning(warnings, "AST_RESULT_REQUIRED", null, null, candidate.isolate.getIsolateLabel(), 1);
                continue;
            }
            int rowsBefore = exportRows.size();
            for (MicroAstReading reading : readings) {
                Optional<MicroAntibiotic> antibiotic = optionalReference(reading.getAntibioticId(), antibioticDAO::get);
                if (antibiotic.isEmpty() || !hasText(antibiotic.get().getWhonetCode())) {
                    excludedRows++;
                    addWarning(warnings, "ANTIBIOTIC_MAPPING_REQUIRED", "antibiotics", reading.getAntibioticId(),
                            antibiotic.map(MicroAntibiotic::getDisplayName).orElse(reading.getAntibioticId()), 1);
                    continue;
                }
                String interpretation = toWhonetInterpretation(
                        hasText(reading.getOverrideInterpretation()) ? reading.getOverrideInterpretation()
                                : reading.getInterpretation());
                if (!WHONET_INTERPRETATIONS.contains(interpretation)) {
                    excludedRows++;
                    addWarning(warnings, "AST_INTERPRETATION_REQUIRED", null, reading.getId(),
                            antibiotic.get().getDisplayName(), 1);
                    continue;
                }
                exportRows.add(toWhonetRow(candidate, organism.get(), antibiotic.get(), reading, interpretation));
                previewRows.add(toPreviewRow(candidate, organism.get(), antibiotic.get(), reading, interpretation));
            }
            if (exportRows.size() > rowsBefore) {
                exportableIsolates++;
            }
        }

        MicroWhonetPreviewForm preview = new MicroWhonetPreviewForm();
        preview.from = query.from.toString();
        preview.to = query.to.toString();
        preview.significance = significancePolicy(query.significance);
        preview.dedup = query.dedup;
        preview.dedupBasis = query.dedupBasis;
        preview.dedupScope = query.dedupScope;
        preview.excludeContaminants = query.excludeContaminants;
        preview.profileSensitivity = query.profileSensitivity;
        preview.totalCases = population.cases.size();
        preview.totalIsolates = population.allIsolates.size();
        preview.afterSpecimen = afterSpecimen;
        preview.afterOrganism = afterOrganism;
        preview.afterPatientOrigin = afterPatientOrigin;
        preview.clinicalPurposeCases = clinicalPurposeCases;
        preview.screeningPurposeCases = screeningPurposeCases;
        preview.unspecifiedPurposeCases = unspecifiedPurposeCases;
        preview.afterCulturePurpose = afterCulturePurpose;
        preview.afterSignificance = afterSignificance;
        preview.afterDeduplication = candidates.size();
        preview.exportableIsolates = exportableIsolates;
        preview.exportedRows = exportRows.size();
        preview.excludedRows = excludedRows;
        preview.canGenerate = !exportRows.isEmpty();
        preview.warnings.addAll(warnings.values());
        int first = Math.min((query.page - 1) * query.pageSize, previewRows.size());
        int last = Math.min(first + query.pageSize, previewRows.size());
        preview.rows.addAll(previewRows.subList(first, last));
        return new MicroWhonetDataset(preview, exportRows,
                new MicroWhonetExportSelection(query.specimen, query.organism, query.origin, query.significance,
                        query.includeScreening, query.includeUnspecified, query.dedupBasis, query.dedupScope,
                        query.excludeContaminants, query.profileSensitivity));
    }

    @Override
    @Transactional(readOnly = true)
    public MicroWhonetFilterOptionsForm getFilterOptions(MicroWhonetExportQueryForm requestedQuery) {
        Population population = loadPopulation(normalize(requestedQuery));
        MicroWhonetFilterOptionsForm options = new MicroWhonetFilterOptionsForm();

        Map<String, String> specimenLabels = new HashMap<>();
        for (PatientContext context : population.contextsByCase.values()) {
            if (hasText(context.specimenTypeId)) {
                specimenLabels.putIfAbsent(context.specimenTypeId,
                        hasText(context.specimenTypeLabel) ? context.specimenTypeLabel : context.specimenTypeId);
            }
        }
        options.specimenTypes.addAll(toOptions(specimenLabels));

        Map<String, String> organismLabels = new HashMap<>();
        for (MicroIsolate isolate : population.allIsolates) {
            MicroOrganism organism = mappedReference(population.organismsById, isolate.getOrganismId());
            if (organism != null) {
                organismLabels.putIfAbsent(organism.getId(), organism.getDisplayName());
            }
        }
        options.organisms.addAll(toOptions(organismLabels));

        List<String> originCodes = population.contextsByCase.values().stream().map(context -> context.patientOrigin)
                .filter(this::hasText).distinct().sorted().toList();
        Map<String, String> originLabels = indexBy(patientOriginDAO.getByCodes(originCodes),
                MicroPatientOrigin::getCode).values().stream()
                .collect(Collectors.toMap(MicroPatientOrigin::getCode,
                        origin -> hasText(origin.getDisplayName()) ? origin.getDisplayName() : origin.getCode()));
        for (String code : originCodes) {
            originLabels.putIfAbsent(code, code);
        }
        options.patientOrigins.addAll(toOptions(originLabels));

        Map<String, String> significanceLabels = population.allIsolates.stream().map(MicroIsolate::getSignificance)
                .filter(this::hasText).distinct().collect(Collectors.toMap(Function.identity(), Function.identity()));
        options.significance.addAll(toOptions(significanceLabels));
        return options;
    }

    private Population loadPopulation(NormalizedQuery query) {
        List<MicroCase> cases = caseDAO.getFinalizedBacteriologyByCollectionDateRange(query.fromInclusive,
                query.toExclusive);
        if (cases.isEmpty()) {
            return new Population(List.of(), Map.of(), List.of(), Map.of(), Map.of());
        }
        List<String> caseIds = cases.stream().map(MicroCase::getId).toList();
        List<String> sampleItemIds = cases.stream().map(MicroCase::getSampleItemId).filter(this::hasText).distinct()
                .toList();
        Map<String, MicroWhonetPatientContext> patientContextsBySampleItem = indexBy(
                worklistContextDAO.getWhonetPatientContexts(sampleItemIds), MicroWhonetPatientContext::sampleItemId);
        Map<String, MicroCaseOrderDetail> detailsByCase = indexBy(caseOrderDetailDAO.getByCaseIds(caseIds),
                MicroCaseOrderDetail::getCaseId);
        Map<String, PatientContext> contextsByCase = new LinkedHashMap<>();
        for (MicroCase microCase : cases) {
            contextsByCase.put(microCase.getId(),
                    patientContext(patientContextsBySampleItem.get(microCase.getSampleItemId()),
                            detailsByCase.get(microCase.getId())));
        }
        Map<String, List<MicroIsolate>> isolatesByCase = groupBy(isolateDAO.getByCaseIds(caseIds),
                MicroIsolate::getCaseId);
        List<MicroIsolate> allIsolates = cases.stream()
                .flatMap(value -> valuesFor(isolatesByCase, value.getId()).stream()).toList();
        List<String> organismIds = allIsolates.stream().map(MicroIsolate::getOrganismId).filter(this::hasText)
                .distinct().sorted().toList();
        List<MicroOrganism> organisms = organismIds.isEmpty() ? List.of() : organismDAO.getByIds(organismIds);
        Map<String, MicroOrganism> organismsById = indexBy(organisms, MicroOrganism::getId);
        return new Population(cases, isolatesByCase, allIsolates, contextsByCase, organismsById);
    }

    private NormalizedQuery normalize(MicroWhonetExportQueryForm query) {
        if (query == null) {
            throw new IllegalArgumentException("Export query is required");
        }
        LocalDate from = parseDate(query.from, "from");
        LocalDate to = parseDate(query.to, "to");
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("to must be on or after from");
        }
        List<String> specimen = normalizeValues(query.specimen);
        List<String> organism = normalizeValues(query.organism);
        List<String> origin = normalizeValues(query.origin);
        List<String> significance = normalizeSignificance(query.significance);
        String dedup = hasText(query.dedup) ? query.dedup.trim() : FIRST_ISOLATE_7_DAY;
        if (!FIRST_ISOLATE_WINDOWS.containsKey(dedup) && !NONE.equals(dedup)) {
            throw new IllegalArgumentException("Unsupported de-duplication policy");
        }
        String dedupBasis = hasText(query.dedupBasis) ? query.dedupBasis.trim() : COLLECTION_DATE;
        if (!List.of(COLLECTION_DATE, RELEASE_DATE).contains(dedupBasis)) {
            throw new IllegalArgumentException("Unsupported de-duplication date basis");
        }
        String dedupScope = hasText(query.dedupScope) ? query.dedupScope.trim() : ANY_SOURCE;
        if (!List.of(ANY_SOURCE, SAME_SOURCE).contains(dedupScope)) {
            throw new IllegalArgumentException("Unsupported de-duplication source scope");
        }
        String profileSensitivity = hasText(query.profileSensitivity) ? query.profileSensitivity.trim() : INSENSITIVE;
        if (!List.of(INSENSITIVE, SENSITIVE).contains(profileSensitivity)) {
            throw new IllegalArgumentException("Unsupported susceptibility-profile sensitivity");
        }
        int page = Math.max(1, query.page);
        int pageSize = List.of(20, 50, 100).contains(query.pageSize) ? query.pageSize : 20;
        ZoneId zone = ZoneId.systemDefault();
        return new NormalizedQuery(from, to, specimen, organism, origin, significance, query.includeScreening,
                query.includeUnspecified, dedup, dedupBasis, dedupScope, query.excludeContaminants, profileSensitivity,
                page, pageSize, Timestamp.from(from.atStartOfDay(zone).toInstant()),
                Timestamp.from(to.plusDays(1).atStartOfDay(zone).toInstant()));
    }

    private List<String> normalizeValues(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().filter(this::hasText).map(String::trim).distinct().sorted().toList();
    }

    private List<String> normalizeSignificance(List<String> values) {
        List<String> normalized = normalizeValues(values);
        if (normalized.isEmpty()) {
            return List.of(MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name());
        }
        List<String> allowed = List.of(MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name(),
                MicroIsolateSignificance.CONTAMINANT.name(), MicroIsolateSignificance.NORMAL_FLORA.name(),
                MicroIsolateSignificance.UNKNOWN.name());
        if (normalized.contains(ALL)) {
            return allowed.stream().sorted().toList();
        }
        if (!allowed.containsAll(normalized)) {
            throw new IllegalArgumentException("Unsupported significance value");
        }
        return normalized;
    }

    private String significancePolicy(List<String> significance) {
        if (significance.size() == 1) {
            return significance.get(0);
        }
        if (significance.size() == MicroIsolateSignificance.values().length) {
            return ALL;
        }
        return significance.stream().map(value -> switch (value) {
        case "CLINICALLY_SIGNIFICANT" -> "CLINICAL";
        case "NORMAL_FLORA" -> "FLORA";
        default -> value;
        }).collect(Collectors.joining("|"));
    }

    private LocalDate parseDate(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(field + " must use YYYY-MM-DD", exception);
        }
    }

    private List<Candidate> deduplicate(List<Candidate> candidates, NormalizedQuery query, AstData astData) {
        String basis = NONE.equals(query.dedup) ? COLLECTION_DATE : query.dedupBasis;
        List<Candidate> sorted = candidates.stream()
                .sorted(Comparator.comparing((Candidate value) -> dedupTimestamp(value, basis))
                        .thenComparing(value -> value.microCase.getId()).thenComparing(value -> value.isolate.getId()))
                .toList();
        if (NONE.equals(query.dedup)) {
            return sorted;
        }
        int windowDays = FIRST_ISOLATE_WINDOWS.get(query.dedup);
        Map<String, LocalDate> firstByPatientOrganism = new HashMap<>();
        List<Candidate> included = new ArrayList<>();
        for (Candidate candidate : sorted) {
            String patientKey = hasText(candidate.context.patientId) ? candidate.context.patientId
                    : "UNKNOWN:" + candidate.isolate.getId();
            String organismKey = hasText(candidate.isolate.getOrganismId()) ? candidate.isolate.getOrganismId()
                    : "UNKNOWN:" + candidate.isolate.getId();
            String key = patientKey + "|" + organismKey;
            if (SAME_SOURCE.equals(query.dedupScope)) {
                String sourceKey = hasText(candidate.context.specimenTypeId) ? candidate.context.specimenTypeId
                        : "UNKNOWN:" + candidate.isolate.getId();
                key += "|SOURCE:" + sourceKey;
            }
            if (SENSITIVE.equals(query.profileSensitivity)) {
                key += "|PROFILE:" + susceptibilityProfile(candidate, astData);
            }
            LocalDate candidateDate = dedupDate(candidate, basis);
            LocalDate first = firstByPatientOrganism.get(key);
            if (first == null || ChronoUnit.DAYS.between(first, candidateDate) >= windowDays) {
                included.add(candidate);
                firstByPatientOrganism.put(key, candidateDate);
            }
        }
        return included;
    }

    private Timestamp dedupTimestamp(Candidate candidate, String basis) {
        Timestamp timestamp = RELEASE_DATE.equals(basis) ? candidate.microCase.getClosedAt()
                : candidate.context.collectionTimestamp;
        if (timestamp == null) {
            throw new IllegalArgumentException("Selected de-duplication date is unavailable");
        }
        return timestamp;
    }

    private LocalDate dedupDate(Candidate candidate, String basis) {
        return dedupTimestamp(candidate, basis).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private String susceptibilityProfile(Candidate candidate, AstData astData) {
        return readingsFor(candidate, astData).stream().filter(reading -> hasText(reading.getAntibioticId()))
                .map(reading -> Map.entry(reading.getAntibioticId(),
                        toWhonetInterpretation(
                                hasText(reading.getOverrideInterpretation()) ? reading.getOverrideInterpretation()
                                        : reading.getInterpretation())))
                .filter(entry -> WHONET_INTERPRETATIONS.contains(entry.getValue()))
                .map(entry -> entry.getKey() + "=" + entry.getValue()).distinct().sorted()
                .collect(Collectors.joining(";"));
    }

    private AstData loadReportableAstData(List<Candidate> candidates) {
        List<String> isolateIds = candidates.stream().map(value -> value.isolate.getId()).distinct().toList();
        if (isolateIds.isEmpty()) {
            return new AstData(Map.of(), Map.of());
        }
        Map<String, List<MicroAstRun>> runsByIsolate = groupBy(astRunDAO.getByIsolateIds(isolateIds),
                MicroAstRun::getIsolateId);
        List<MicroAstRun> reportableRuns = candidates.stream()
                .flatMap(candidate -> valuesFor(runsByIsolate, candidate.isolate.getId()).stream())
                .filter(this::isReportableReviewed).toList();
        List<String> runIds = reportableRuns.stream().map(MicroAstRun::getId).toList();
        Map<String, List<MicroAstReading>> readingsByRun = runIds.isEmpty() ? Map.of()
                : groupBy(astReadingDAO.getByRunIds(runIds), MicroAstReading::getAstRunId);
        return new AstData(groupBy(reportableRuns, MicroAstRun::getIsolateId), readingsByRun);
    }

    private List<MicroAstReading> readingsFor(Candidate candidate, AstData astData) {
        return valuesFor(astData.reportableRunsByIsolate, candidate.isolate.getId()).stream()
                .flatMap(run -> valuesFor(astData.readingsByRun, run.getId()).stream()).toList();
    }

    private PatientContext patientContext(MicroWhonetPatientContext source, MicroCaseOrderDetail orderDetail) {
        PatientContext context = new PatientContext();
        context.patientId = source == null ? "" : safe(source.patientId());
        context.nationalId = source == null ? "" : safe(source.nationalId());
        context.firstName = source == null ? "" : safe(source.firstName());
        context.lastName = source == null ? "" : safe(source.lastName());
        context.gender = source == null ? "" : safe(source.gender());
        context.birthDate = source == null || source.birthDate() == null ? ""
                : source.birthDate().toLocalDateTime().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        context.accessionNumber = source == null ? "" : safe(source.accessionNumber());
        context.enteredDate = source == null || source.enteredDate() == null ? ""
                : source.enteredDate().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        context.collectionDate = source == null || source.collectionDate() == null ? ""
                : source.collectionDate().toLocalDateTime().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        context.collectionTimestamp = source == null ? null : source.collectionDate();
        context.specimenTypeId = source == null ? "" : safe(source.specimenTypeId());
        context.specimenTypeLabel = source == null ? "" : safe(source.specimenTypeLabel());
        context.specimenType = source == null ? "" : safe(source.specimenTypeCode());
        context.patientOrigin = orderDetail == null ? "" : safe(orderDetail.getPatientOrigin());
        context.culturePurpose = orderDetail == null ? "" : safe(orderDetail.getCulturePurpose());
        context.latitude = source == null || source.latitude() == null ? "" : source.latitude().toString();
        context.longitude = source == null || source.longitude() == null ? "" : source.longitude().toString();
        return context;
    }

    private WHONetRow toWhonetRow(Candidate candidate, MicroOrganism organism, MicroAntibiotic antibiotic,
            MicroAstReading reading, String interpretation) {
        PatientContext context = candidate.context;
        return new WHONetRow(context.nationalId, context.firstName, context.lastName, context.gender, context.birthDate,
                context.enteredDate, context.accessionNumber, context.collectionDate, context.specimenType,
                antibiotic.getWhonetCode(), organism.getWhonetCode(), interpretation, safe(reading.getMethod()),
                context.latitude, context.longitude);
    }

    private MicroWhonetPreviewRowForm toPreviewRow(Candidate candidate, MicroOrganism organism,
            MicroAntibiotic antibiotic, MicroAstReading reading, String interpretation) {
        MicroWhonetPreviewRowForm row = new MicroWhonetPreviewRowForm();
        row.caseId = candidate.microCase.getId();
        row.isolateId = candidate.isolate.getId();
        row.accessionNumber = candidate.context.accessionNumber;
        row.specimenType = candidate.context.specimenType;
        row.organismCode = organism.getWhonetCode();
        row.antibioticCode = antibiotic.getWhonetCode();
        row.interpretation = interpretation;
        row.method = reading.getMethod();
        return row;
    }

    private boolean isReportableReviewed(MicroAstRun run) {
        return run.isReportable() && MicroAstRunStatus.REVIEWED.name().equals(run.getStatus());
    }

    private int countCasesWithPurpose(List<MicroCase> cases, Map<String, PatientContext> contextsByCase,
            String purpose) {
        return (int) cases.stream().filter(microCase -> {
            PatientContext context = contextsByCase.get(microCase.getId());
            return context != null && purpose.equals(context.culturePurpose);
        }).count();
    }

    private boolean isPurposeIncluded(String purpose, boolean includeScreening, boolean includeUnspecified) {
        if (MicroCulturePurpose.CLINICAL_DIAGNOSTIC.name().equals(purpose)) {
            return true;
        }
        if (MicroCulturePurpose.ACTIVE_SCREENING.name().equals(purpose)) {
            return includeScreening;
        }
        return includeUnspecified;
    }

    private String toWhonetInterpretation(String interpretation) {
        if (interpretation == null) {
            return "";
        }
        return switch (interpretation) {
        case "SUSCEPTIBLE" -> "S";
        case "INTERMEDIATE" -> "I";
        case "RESISTANT" -> "R";
        default -> interpretation;
        };
    }

    private <T> Optional<T> optionalReference(String id, Function<String, Optional<T>> loader) {
        return hasText(id) ? loader.apply(id) : Optional.empty();
    }

    private <T> T mappedReference(Map<String, T> values, String id) {
        return hasText(id) ? values.get(id) : null;
    }

    private void addWarning(Map<String, MicroWhonetWarningForm> warnings, String code, String resource,
            String resourceId, String itemLabel, int excludedRows) {
        String key = code + "|" + safe(resourceId);
        MicroWhonetWarningForm warning = warnings.computeIfAbsent(key, ignored -> {
            MicroWhonetWarningForm created = new MicroWhonetWarningForm();
            created.code = code;
            created.resource = resource;
            created.resourceId = resourceId;
            created.itemLabel = itemLabel;
            return created;
        });
        warning.excludedRows += excludedRows;
    }

    private <T> Map<String, List<T>> groupBy(List<T> values, Function<T, String> key) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return values.stream().collect(Collectors.groupingBy(key, LinkedHashMap::new, Collectors.toList()));
    }

    private <T> Map<String, T> indexBy(List<T> values, Function<T, String> key) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return values.stream().filter(value -> hasText(key.apply(value)))
                .collect(Collectors.toMap(key, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
    }

    private List<MicroWhonetFilterOptionForm> toOptions(Map<String, String> labels) {
        return labels.entrySet().stream()
                .map(entry -> new MicroWhonetFilterOptionForm(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing((MicroWhonetFilterOptionForm option) -> safe(option.label),
                        String.CASE_INSENSITIVE_ORDER).thenComparing(option -> option.id))
                .toList();
    }

    private <T> List<T> valuesFor(Map<String, List<T>> values, String key) {
        return values.getOrDefault(key, List.of());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record NormalizedQuery(LocalDate from, LocalDate to, List<String> specimen, List<String> organism,
            List<String> origin, List<String> significance, boolean includeScreening, boolean includeUnspecified,
            String dedup, String dedupBasis, String dedupScope, boolean excludeContaminants, String profileSensitivity,
            int page, int pageSize, Timestamp fromInclusive, Timestamp toExclusive) {
    }

    private record Population(List<MicroCase> cases, Map<String, List<MicroIsolate>> isolatesByCase,
            List<MicroIsolate> allIsolates, Map<String, PatientContext> contextsByCase,
            Map<String, MicroOrganism> organismsById) {
    }

    private record Candidate(MicroCase microCase, MicroIsolate isolate, PatientContext context) {
    }

    private record AstData(Map<String, List<MicroAstRun>> reportableRunsByIsolate,
            Map<String, List<MicroAstReading>> readingsByRun) {
    }

    private static class PatientContext {
        private String patientId;
        private String nationalId;
        private String firstName;
        private String lastName;
        private String gender;
        private String birthDate;
        private String accessionNumber;
        private String enteredDate;
        private String collectionDate;
        private Timestamp collectionTimestamp;
        private String specimenTypeId;
        private String specimenTypeLabel;
        private String specimenType;
        private String patientOrigin;
        private String culturePurpose;
        private String latitude;
        private String longitude;
    }
}
