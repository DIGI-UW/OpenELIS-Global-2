package org.openelisglobal.microbiology.service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.dao.MicroWhonetContext;
import org.openelisglobal.microbiology.form.MicroWhonetExportQueryForm;
import org.openelisglobal.microbiology.form.MicroWhonetPreviewForm;
import org.openelisglobal.microbiology.form.MicroWhonetPreviewRowForm;
import org.openelisglobal.microbiology.form.MicroWhonetWarningForm;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunStatus;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.reports.action.implementation.reportBeans.WHONETCSVRoutineColumnBuilder.WHONetRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroWhonetDatasetServiceImpl implements MicroWhonetDatasetService {

    private static final String ALL = "ALL";
    private static final String NONE = "NONE";
    private static final String FIRST_ISOLATE_7_DAY = "FIRST_ISOLATE_7_DAY";
    private static final long SEVEN_DAYS_MILLIS = Duration.ofDays(7).toMillis();
    private static final Set<String> WHONET_INTERPRETATIONS = Set.of("S", "I", "R");

    private final MicroCaseDAO caseDAO;
    private final MicroIsolateDAO isolateDAO;
    private final MicroAstRunDAO astRunDAO;
    private final MicroAstReadingDAO astReadingDAO;
    private final MicroOrganismDAO organismDAO;
    private final MicroAntibioticDAO antibioticDAO;

    public MicroWhonetDatasetServiceImpl(MicroCaseDAO caseDAO, MicroIsolateDAO isolateDAO, MicroAstRunDAO astRunDAO,
            MicroAstReadingDAO astReadingDAO, MicroOrganismDAO organismDAO, MicroAntibioticDAO antibioticDAO) {
        this.caseDAO = caseDAO;
        this.isolateDAO = isolateDAO;
        this.astRunDAO = astRunDAO;
        this.astReadingDAO = astReadingDAO;
        this.organismDAO = organismDAO;
        this.antibioticDAO = antibioticDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroWhonetDataset compile(MicroWhonetExportQueryForm requestedQuery) {
        NormalizedQuery query = normalize(requestedQuery);
        List<MicroCase> cases = caseDAO.getFinalizedBacteriologyByClosedAtRange(query.fromInclusive, query.toExclusive);
        List<String> caseIds = cases.stream().map(MicroCase::getId).toList();
        Map<String, PatientContext> contextsByCase = caseDAO.getWhonetContextsByCaseIds(caseIds).stream()
                .collect(Collectors.toMap(MicroWhonetContext::caseId, this::patientContext, (first, ignored) -> first,
                        LinkedHashMap::new));
        Map<String, List<MicroIsolate>> isolatesByCase = groupBy(isolateDAO.getByCaseIds(caseIds),
                MicroIsolate::getCaseId);
        List<MicroIsolate> allIsolates = cases.stream()
                .flatMap(value -> valuesFor(isolatesByCase, value.getId()).stream()).toList();

        List<Candidate> candidates = new ArrayList<>();
        for (MicroCase microCase : cases) {
            PatientContext context = contextsByCase.getOrDefault(microCase.getId(), new PatientContext());
            for (MicroIsolate isolate : valuesFor(isolatesByCase, microCase.getId())) {
                if (ALL.equals(query.significance) || query.significance.equals(isolate.getSignificance())) {
                    candidates.add(new Candidate(microCase, isolate, context));
                }
            }
        }
        int afterSignificance = candidates.size();
        candidates = deduplicate(candidates, query.dedup);

        List<String> isolateIds = candidates.stream().map(value -> value.isolate.getId()).toList();
        Map<String, List<MicroAstRun>> runsByIsolate = groupBy(astRunDAO.getByIsolateIds(isolateIds),
                MicroAstRun::getIsolateId);
        List<MicroAstRun> reportableRuns = candidates.stream()
                .flatMap(candidate -> valuesFor(runsByIsolate, candidate.isolate.getId()).stream())
                .filter(this::isReportableReviewed).toList();
        List<String> runIds = reportableRuns.stream().map(MicroAstRun::getId).toList();
        Map<String, List<MicroAstReading>> readingsByRun = groupBy(astReadingDAO.getByRunIds(runIds),
                MicroAstReading::getAstRunId);
        Map<String, List<MicroAstRun>> reportableRunsByIsolate = groupBy(reportableRuns, MicroAstRun::getIsolateId);

        List<WHONetRow> exportRows = new ArrayList<>();
        List<MicroWhonetPreviewRowForm> previewRows = new ArrayList<>();
        Map<String, MicroWhonetWarningForm> warnings = new LinkedHashMap<>();
        int exportableIsolates = 0;
        int excludedRows = 0;
        for (Candidate candidate : candidates) {
            List<MicroAstReading> readings = valuesFor(reportableRunsByIsolate, candidate.isolate.getId()).stream()
                    .flatMap(run -> valuesFor(readingsByRun, run.getId()).stream()).toList();
            if (!hasText(candidate.context.specimenType)) {
                int excluded = Math.max(1, readings.size());
                excludedRows += excluded;
                addWarning(warnings, "SPECIMEN_MAPPING_REQUIRED", "specimen-types", candidate.context.specimenTypeId,
                        candidate.context.specimenTypeLabel, excluded);
                continue;
            }
            Optional<MicroOrganism> organism = optionalReference(candidate.isolate.getOrganismId(), organismDAO::get);
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
        preview.significance = query.significance;
        preview.dedup = query.dedup;
        preview.totalCases = cases.size();
        preview.totalIsolates = allIsolates.size();
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
        return new MicroWhonetDataset(preview, exportRows);
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
        String significance = hasText(query.significance) ? query.significance.trim() : "CLINICALLY_SIGNIFICANT";
        if (!List.of(ALL, MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name()).contains(significance)) {
            throw new IllegalArgumentException("Unsupported significance policy");
        }
        String dedup = hasText(query.dedup) ? query.dedup.trim() : FIRST_ISOLATE_7_DAY;
        if (!List.of(NONE, FIRST_ISOLATE_7_DAY).contains(dedup)) {
            throw new IllegalArgumentException("Unsupported de-duplication policy");
        }
        int page = Math.max(1, query.page);
        int pageSize = List.of(20, 50, 100).contains(query.pageSize) ? query.pageSize : 20;
        ZoneId zone = ZoneId.systemDefault();
        return new NormalizedQuery(from, to, significance, dedup, page, pageSize,
                Timestamp.from(from.atStartOfDay(zone).toInstant()),
                Timestamp.from(to.plusDays(1).atStartOfDay(zone).toInstant()));
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

    private List<Candidate> deduplicate(List<Candidate> candidates, String policy) {
        List<Candidate> sorted = candidates.stream()
                .sorted(Comparator.comparing((Candidate value) -> value.microCase.getClosedAt())
                        .thenComparing(value -> value.microCase.getId()).thenComparing(value -> value.isolate.getId()))
                .toList();
        if (NONE.equals(policy)) {
            return sorted;
        }
        Map<String, Timestamp> firstByPatientOrganism = new HashMap<>();
        List<Candidate> included = new ArrayList<>();
        for (Candidate candidate : sorted) {
            String patientKey = hasText(candidate.context.patientId) ? candidate.context.patientId
                    : "UNKNOWN:" + candidate.isolate.getId();
            String organismKey = hasText(candidate.isolate.getOrganismId()) ? candidate.isolate.getOrganismId()
                    : "UNKNOWN:" + candidate.isolate.getId();
            String key = patientKey + "|" + organismKey;
            Timestamp first = firstByPatientOrganism.get(key);
            if (first == null || candidate.microCase.getClosedAt().getTime() - first.getTime() >= SEVEN_DAYS_MILLIS) {
                included.add(candidate);
                firstByPatientOrganism.put(key, candidate.microCase.getClosedAt());
            }
        }
        return included;
    }

    private PatientContext patientContext(MicroWhonetContext source) {
        PatientContext context = new PatientContext();
        context.patientId = safe(source.patientId());
        context.nationalId = safe(source.nationalId());
        context.firstName = safe(source.firstName());
        context.lastName = safe(source.lastName());
        context.gender = safe(source.gender());
        context.birthDate = source.birthDate() == null ? ""
                : source.birthDate().toLocalDateTime().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        context.accessionNumber = safe(source.accessionNumber());
        context.enteredDate = source.enteredDate() == null ? ""
                : source.enteredDate().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        context.collectionDate = source.collectionDate() == null ? ""
                : source.collectionDate().toLocalDateTime().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        context.specimenTypeId = safe(source.specimenTypeId());
        context.specimenTypeLabel = safe(source.specimenTypeLabel());
        context.specimenType = safe(source.specimenType());
        context.latitude = source.latitude() == null ? "" : source.latitude().toString();
        context.longitude = source.longitude() == null ? "" : source.longitude().toString();
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

    private <T> List<T> valuesFor(Map<String, List<T>> values, String key) {
        return values.getOrDefault(key, List.of());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record NormalizedQuery(LocalDate from, LocalDate to, String significance, String dedup, int page,
            int pageSize, Timestamp fromInclusive, Timestamp toExclusive) {
    }

    private record Candidate(MicroCase microCase, MicroIsolate isolate, PatientContext context) {
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
        private String specimenTypeId;
        private String specimenTypeLabel;
        private String specimenType;
        private String latitude;
        private String longitude;
    }
}
