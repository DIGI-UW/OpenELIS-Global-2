package org.openelisglobal.reports.dataexport.service;

import java.io.IOException;
import java.io.Writer;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.reports.dataexport.dao.SampleTestingExportDAO;
import org.openelisglobal.reports.dataexport.form.ExportRecord;
import org.openelisglobal.reports.dataexport.form.ExportSnapshot;
import org.openelisglobal.reports.dataexport.form.ReportSourceConfig;
import org.openelisglobal.reports.dataexport.form.ReportingVariable;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SampleTestingSource implements ReportingSource {
    private static final List<String> BOTH = List.of("SPREADSHEET", "RESULT_LIST");
    private static final List<String> DETAIL = List.of("RESULT_LIST");
    private static final List<String> SPREADSHEET = List.of("SPREADSHEET");
    private static final List<Interval> RESULT_INTERVALS = List.of(
            new Interval("orderToResultMinutes", "Order to Result (min)"),
            new Interval("receivedToValidatedMinutes", "Received to Validated (min)"),
            new Interval("resultedToValidatedMinutes", "Resulted to Validated (min)"));
    @Autowired
    private SampleTestingExportDAO dao;
    @Autowired
    private ReportingCsvWriter csv;
    @Autowired
    private IStatusService statuses;

    @Override
    public String id() {
        return "SAMPLE_TESTING";
    }

    @Override
    public void validateConfiguration(ReportSourceConfig configuration) {
        if (!"collectionDate".equals(configuration.dateAnchor())
                || !List.of("tests", "components", "observations", "testTurnaround", "componentTurnaround")
                        .containsAll(configuration.catalogs())
                || !List.of("labSectionIds", "testIds", "resultStatuses").containsAll(configuration.filters())) {
            throw new IllegalArgumentException("reporting.definition.unsupportedMapping");
        }
        var available = catalog().stream().map(ReportingVariable::id).collect(Collectors.toSet());
        if (!available.containsAll(configuration.attributes())) {
            throw new IllegalArgumentException("reporting.definition.attributesInvalid");
        }
    }

    @Override
    public List<ReportingVariable> catalog() {
        List<ReportingVariable> fields = new ArrayList<>();
        common(fields, "accessionNumber", "Accession Number", "text", "sample", BOTH);
        common(fields, "specimenId", "Specimen ID", "text", "sample", BOTH);
        common(fields, "collectionDate", "Collection Date", "date", "sample", BOTH);
        common(fields, "collectionTime", "Collection Time", "time", "sample", BOTH);
        common(fields, "receivedDate", "Received Date", "date", "sample", BOTH);
        common(fields, "receivedTime", "Received Time", "time", "sample", BOTH);
        common(fields, "orderDate", "Order Date", "date", "sample", BOTH);
        common(fields, "sampleType", "Sample Type", "text", "sample", BOTH);
        common(fields, "sampleStatus", "Sample Status", "text", "sample", BOTH);
        common(fields, "priority", "Priority", "text", "sample", BOTH);
        common(fields, "numberOfTests", "Number of Tests Ordered", "number", "sample", BOTH);
        common(fields, "patientName", "Patient Name", "text", "patient", BOTH);
        common(fields, "dateOfBirth", "Date of Birth", "date", "patient", BOTH);
        common(fields, "sex", "Sex", "text", "patient", BOTH);
        common(fields, "nationalId", "National ID", "text", "patient", BOTH);
        common(fields, "phoneNumber", "Phone Number", "text", "patient", BOTH);
        common(fields, "address", "Address", "text", "patient", BOTH);
        common(fields, "resultId", "Result ID", "text", "result", DETAIL);
        common(fields, "testName", "Test Name", "text", "result", DETAIL);
        common(fields, "component", "Component", "text", "result", DETAIL);
        common(fields, "loincCode", "LOINC Code", "text", "result", DETAIL);
        common(fields, "resultValue", "Result Value", "text", "result", DETAIL);
        common(fields, "resultUnit", "Result Unit", "text", "result", DETAIL);
        common(fields, "resultStatus", "Result Status", "text", "result", DETAIL);
        common(fields, "dateResulted", "Date Resulted", "datetime", "result", DETAIL);
        common(fields, "validationDate", "Validation Date", "datetime", "result", DETAIL);
        common(fields, "labSection", "Lab Section", "text", "result", DETAIL);
        common(fields, "orderToResultMinutes", "Order to Result (min)", "number", "turnaround", DETAIL);
        common(fields, "receivedToValidatedMinutes", "Received to Validated (min)", "number", "turnaround", DETAIL);
        common(fields, "orderToCollectionMinutes", "Order to Collection (min)", "number", "turnaround", BOTH);
        common(fields, "collectionToReceivedMinutes", "Collection to Received (min)", "number", "turnaround", BOTH);
        common(fields, "resultedToValidatedMinutes", "Resulted to Validated (min)", "number", "turnaround", DETAIL);
        var tests = dao.tests();
        var names = tests.stream().collect(Collectors.toMap(t -> t.getId(), t -> t.getDescription()));
        tests.forEach(t -> fields
                .add(new ReportingVariable("test:" + t.getId(), t.getDescription(), "result", "tests", true, BOTH)));
        tests.forEach(t -> turnaroundFields(fields, "test:" + t.getId(), t.getDescription(), "testTurnaround"));
        dao.components().stream().filter(c -> names.containsKey(c.getTestId())).forEach(c -> {
            String id = "component:" + c.getId();
            String label = names.get(c.getTestId()) + " — " + c.getLabel();
            fields.add(new ReportingVariable(id, label, c.getResultType(), "components", true, BOTH));
            turnaroundFields(fields, id, label, "componentTurnaround");
        });
        dao.observationTypes().forEach(t -> fields.add(new ReportingVariable("observation:" + t.getId(),
                t.getDescription(), "text", "observations", false, BOTH)));
        return fields;
    }

    private static void turnaroundFields(List<ReportingVariable> fields, String measurement, String label,
            String group) {
        RESULT_INTERVALS.forEach(interval -> fields.add(new ReportingVariable(measurement + ":" + interval.id(),
                label + " — " + interval.label(), "number", group, true, SPREADSHEET)));
    }

    private static void common(List<ReportingVariable> fields, String id, String label, String type, String group,
            List<String> layouts) {
        fields.add(new ReportingVariable(id, label, type, group, false, layouts));
    }

    @Override
    public long write(Writer output, ExportSnapshot request) throws IOException {
        var components = dao.components().stream()
                .collect(Collectors.toMap(TestResultComponent::getId, Function.identity()));
        ZoneId zone = ZoneId.of(request.timezone());
        try (var stream = dao.stream(request)) {
            Iterator<Result> input = stream.iterator();
            Iterator<ExportRecord> records = new Iterator<>() {
                private Normalized pending;
                private int count;
                private String specimenId;
                private String sampleId;
                private Map<String, String> patientFields = Map.of();
                private Map<String, String> observationFields = Map.of();
                private long analysisCount;

                private Normalized read() {
                    while (input.hasNext()) {
                        Result result = input.next();
                        if (isQualifier(result))
                            continue;
                        var specimen = result.getAnalysis().getSampleItem();
                        if (!Objects.equals(specimenId, specimen.getId())) {
                            specimenId = specimen.getId();
                            Patient patient = dao.patient(specimen.getSample().getId());
                            patientFields = patientFields(patient, zone);
                            observationFields = observationFields(
                                    dao.observations(specimen.getSample().getId(), specimenId));
                        }
                        if (!Objects.equals(sampleId, specimen.getSample().getId())) {
                            sampleId = specimen.getSample().getId();
                            analysisCount = dao.analysisCount(sampleId);
                        }
                        Normalized value = normalize(result, request, components, patientFields, observationFields,
                                analysisCount, zone);
                        if (++count % 250 == 0)
                            dao.clearReadBatch();
                        return value;
                    }
                    return null;
                }

                @Override
                public boolean hasNext() {
                    if (pending == null)
                        pending = read();
                    return pending != null;
                }

                @Override
                public ExportRecord next() {
                    if (!hasNext())
                        throw new NoSuchElementException();
                    Normalized first = pending;
                    pending = null;
                    StringBuilder value = new StringBuilder(first.value() == null ? "" : first.value());
                    if (first.multiKey() != null) {
                        while ((pending = read()) != null && first.multiKey().equals(pending.multiKey())) {
                            value.append("; ").append(pending.value() == null ? "" : pending.value());
                        }
                    }
                    Map<String, String> attributes = new LinkedHashMap<>(first.attributes());
                    attributes.put("resultValue", value.toString());
                    Map<String, String> measurements = new LinkedHashMap<>();
                    for (String id : first.measurementIds()) {
                        measurements.put(id, value.toString());
                        // Keep null intervals present: a result with missing timestamps
                        // must still survive a duration-only spreadsheet selection.
                        for (Interval interval : RESULT_INTERVALS)
                            measurements.put(id + ":" + interval.id(), first.attributes().get(interval.id()));
                    }
                    return new ExportRecord(first.id(), first.specimen(), null, attributes, measurements);
                }
            };
            return csv.write(output, ReportingCsvWriter.Layout.valueOf(request.layout()),
                    request.variables().stream().map(ReportingVariable::exportField).toList(), records);
        }
    }

    private boolean isQualifier(Result result) {
        return "A".equals(result.getResultType()) && result.getTestResult() == null && result.getParentResult() != null
                && ResultType.isDictionaryVariant(result.getParentResult().getResultType());
    }

    private Normalized normalize(Result r, ExportSnapshot request, Map<String, TestResultComponent> components,
            Map<String, String> patientFields, Map<String, String> observationFields, long analysisCount, ZoneId zone) {
        var analysis = r.getAnalysis();
        var specimen = analysis.getSampleItem();
        var sample = specimen.getSample();
        var test = analysis.getTest();
        String componentId = r.getTestResult() == null ? null : r.getTestResult().getComponentId();
        TestResultComponent component = components.get(componentId);
        Map<String, String> a = new LinkedHashMap<>(patientFields);
        a.putAll(observationFields);
        a.put("accessionNumber", sample.getAccessionNumber());
        a.put("specimenId", specimen.getId());
        a.put("collectionDate", date(specimen.getCollectionDate(), zone));
        a.put("collectionTime", specimen.getCollectionDate() == null ? null
                : specimen.getCollectionDate().toInstant().atZone(zone).toLocalTime().toString());
        a.put("receivedDate", date(specimen.getReceivedDate(), zone));
        a.put("receivedTime", time(specimen.getReceivedDate(), zone));
        a.put("orderDate", sample.getEnteredDate() == null ? null : sample.getEnteredDate().toLocalDate().toString());
        a.put("sampleType", specimen.getTypeOfSample() == null ? null : specimen.getTypeOfSample().getDescription());
        a.put("sampleStatus", statuses.getStatusNameFromId(specimen.getStatusId()));
        a.put("priority", sample.getPriority() == null ? null : sample.getPriority().toString());
        a.put("numberOfTests", Long.toString(analysisCount));
        a.put("resultId", r.getId());
        a.put("testName", test.getDescription());
        a.put("component", component == null ? null : component.getLabel());
        a.put("loincCode", test.getLoinc());
        a.put("resultUnit", test.getUnitOfMeasure() == null ? null : test.getUnitOfMeasure().getUnitOfMeasureName());
        a.put("resultStatus", analysis.isCorrectedSincePatientReport() ? "Corrected"
                : statuses.getStatusNameFromId(analysis.getStatusId()));
        a.put("dateResulted", timestamp(analysis.getCompletedDate(), zone));
        a.put("validationDate", timestamp(analysis.getReleasedDate(), zone));
        a.put("labSection", analysis.getTestSection() == null ? null : analysis.getTestSection().getTestSectionName());
        Instant order = sample.getEnteredDate() == null ? null
                : sample.getEnteredDate().toLocalDate().atStartOfDay(zone).toInstant();
        Instant collected = instant(specimen.getCollectionDate());
        Instant received = instant(specimen.getReceivedDate());
        Instant resulted = instant(analysis.getCompletedDate());
        Instant validated = instant(analysis.getReleasedDate());
        a.put("orderToResultMinutes", minutes(order, resulted));
        a.put("receivedToValidatedMinutes", minutes(received, validated));
        a.put("orderToCollectionMinutes", minutes(order, collected));
        a.put("collectionToReceivedMinutes", minutes(collected, received));
        a.put("resultedToValidatedMinutes", minutes(resulted, validated));
        List<String> fields = new ArrayList<>();
        if (componentId == null || component == null || component.getIsPrimary())
            fields.add("test:" + test.getId());
        if (componentId != null)
            fields.add("component:" + componentId);
        String value = formatValue(r);
        String multiKey = ResultType.isMultiSelectVariant(r.getResultType())
                ? analysis.getId() + ":" + componentId + ":" + r.getGrouping() + ":" + r.getResultType()
                : null;
        return new Normalized(r.getId(), specimen.getId(), a, fields, value, multiKey);
    }

    private String formatValue(Result result) {
        String value = result.getValue();
        if (value == null || value.isBlank())
            return value;
        if (ResultType.isDictionaryVariant(result.getResultType())) {
            value = dao.dictionary(value);
            var qualifiers = dao.qualifiers(result.getId());
            if (!qualifiers.isEmpty())
                value += " (" + String.join("; ", qualifiers) + ")";
        } else if ("N".equals(result.getResultType()) && result.getSignificantDigits() >= 0) {
            int digits = result.getSignificantDigits();
            // Match stored reporting precision without rounding or HTML formatting.
            if (digits == 0)
                return value.split("\\.")[0];
            int places = value.contains(".") ? value.length() - value.lastIndexOf('.') - 1 : 0;
            if (!value.contains("."))
                value += ".";
            if (places < digits)
                value += "0".repeat(digits - places);
        }
        return value;
    }

    private static Map<String, String> patientFields(Patient patient, ZoneId zone) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (patient == null)
            return fields;
        fields.put("dateOfBirth", date(patient.getBirthDate(), zone));
        fields.put("sex", patient.getGender());
        fields.put("nationalId", patient.getNationalId());
        var person = patient.getPerson();
        if (person != null) {
            fields.put("patientName", join(" ", person.getFirstName(), person.getMiddleName(), person.getLastName()));
            fields.put("phoneNumber", person.getPrimaryPhone());
            fields.put("address",
                    join(", ", person.getStreetAddress(), person.getCity(), person.getState(), person.getCountry()));
        }
        return fields;
    }

    private Map<String, String> observationFields(List<ObservationHistory> observations) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (ObservationHistory observation : observations) {
            String value = observation.getValue();
            if (ObservationHistory.ValueType.DICTIONARY.getCode().equals(observation.getValueType()))
                value = dao.dictionary(value);
            else if (ObservationHistory.ValueType.KEY.getCode().equals(observation.getValueType()))
                value = MessageUtil.getMessage(value);
            grouped.computeIfAbsent("observation:" + observation.getObservationHistoryTypeId(),
                    key -> new ArrayList<>()).add(value == null ? "" : value);
        }
        Map<String, String> fields = new LinkedHashMap<>();
        grouped.forEach((key, values) -> fields.put(key, String.join("; ", values)));
        return fields;
    }

    private static String join(String separator, String... values) {
        return java.util.Arrays.stream(values).filter(v -> v != null && !v.isBlank())
                .collect(Collectors.joining(separator));
    }

    private static String date(Timestamp value, ZoneId zone) {
        return value == null ? null : value.toInstant().atZone(zone).toLocalDate().toString();
    }

    private static String timestamp(Timestamp value, ZoneId zone) {
        return value == null ? null : value.toInstant().atZone(zone).toOffsetDateTime().toString();
    }

    private static String time(Timestamp value, ZoneId zone) {
        return value == null ? null : value.toInstant().atZone(zone).toLocalTime().toString();
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private static String minutes(Instant start, Instant end) {
        return start == null || end == null ? null : Long.toString(ChronoUnit.MINUTES.between(start, end));
    }

    private record Normalized(String id, String specimen, Map<String, String> attributes, List<String> measurementIds,
            String value, String multiKey) {
    }

    private record Interval(String id, String label) {
    }
}
