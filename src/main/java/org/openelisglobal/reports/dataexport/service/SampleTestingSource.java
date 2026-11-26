package org.openelisglobal.reports.dataexport.service;

import java.io.IOException;
import java.io.Writer;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.reports.dataexport.dao.SampleTestingExportDAO;
import org.openelisglobal.reports.dataexport.form.ExportRecord;
import org.openelisglobal.reports.dataexport.form.ExportSnapshot;
import org.openelisglobal.reports.dataexport.form.ReportSourceConfig;
import org.openelisglobal.reports.dataexport.form.ReportingVariable;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SampleTestingSource implements ReportingSource {
    private static final List<String> BOTH = List.of("SPREADSHEET", "RESULT_LIST");
    private static final List<String> DETAIL = List.of("RESULT_LIST");
    private static final List<String> SPREADSHEET = List.of("SPREADSHEET");
    private static final Set<String> ORDER_FIELDS = Set.of("enteredDate", "program", "labUnit");
    private static final List<Interval> RESULT_INTERVALS = List.of(
            new Interval("orderToResultMinutes", "Order to Result (min)"),
            new Interval("receivedToValidatedMinutes", "Received to Validated (min)"),
            new Interval("resultedToValidatedMinutes", "Resulted to Validated (min)"));
    @Autowired
    private SampleTestingExportDAO dao;
    @Autowired
    private ReportingCsvWriter csv;
    @Autowired
    private ReportingResultValues resultValues;
    @Autowired
    private IStatusService statuses;
    @Autowired
    private ExportLocations locations;
    @Autowired
    private ExportTestNames testNames;

    @Override
    public String id() {
        return "SAMPLE_TESTING";
    }

    @Override
    public List<String> defaultResultStatuses() {
        return List.of("FINALIZED");
    }

    /**
     * Collection is the definition's own date; received and order entry are the
     * alternatives (OGC-1655).
     */
    @Override
    public List<String> dateBases() {
        return List.of("collectionDate", "receivedDate", "orderEnteredDate");
    }

    @Override
    public void validateConfiguration(ReportSourceConfig configuration) {
        if (!"collectionDate".equals(configuration.dateAnchor())
                || !List.of("tests", "components", "observations", "testTurnaround", "componentTurnaround",
                        ExportLocations.GROUP).containsAll(configuration.catalogs())
                || !List.of("labSectionIds", "testIds", "resultStatuses", "dateBasis")
                        .containsAll(configuration.filters())) {
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
        common(fields, "orderStatus", "Order Status", "text", "sample", BOTH);
        common(fields, "priority", "Priority", "text", "sample", BOTH);
        common(fields, "numberOfTests", "Number of Tests Ordered", "number", "sample", BOTH);
        common(fields, "enteredDate", "Date Entered", "datetime", "sample", BOTH);
        common(fields, "program", "Program", "text", "sample", BOTH);
        common(fields, "labUnit", "Lab Unit", "text", "sample", BOTH);
        common(fields, "ageYears", "Age at collection (years)", "number", "sample", BOTH);
        common(fields, "ageMonths", "Age at collection (months)", "number", "sample", BOTH);
        common(fields, "ageWeeks", "Age at collection (weeks)", "number", "sample", BOTH);
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
        var names = testNames.labels(tests, LocaleContextHolder.getLocale());
        tests.forEach(t -> fields
                .add(new ReportingVariable("test:" + t.getId(), names.get(t.getId()), "result", "tests", true, BOTH)));
        tests.forEach(t -> turnaroundFields(fields, "test:" + t.getId(), names.get(t.getId()), "testTurnaround"));
        dao.components().stream().filter(c -> names.containsKey(c.getTestId())).forEach(c -> {
            String id = "component:" + c.getId();
            String label = names.get(c.getTestId()) + " — "
                    + ExportTestNames.componentLabel(c, LocaleContextHolder.getLocale());
            fields.add(new ReportingVariable(id, label, c.getResultType(), "components", true, BOTH));
            turnaroundFields(fields, id, label, "componentTurnaround");
        });
        dao.observationTypes().forEach(t -> fields.add(new ReportingVariable("observation:" + t.getId(),
                t.getDescription(), "text", "observations", false, BOTH)));
        fields.addAll(locations.catalog(BOTH));
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
        // allComponents(), not components(): a component removed from test
        // configuration is soft-deactivated while historical Result rows keep
        // referencing it. Resolving against the active-only list would leave those
        // results with a null component, dropping their label and folding their
        // value into the overall test measurement.
        var components = dao.allComponents().stream()
                .collect(Collectors.toMap(TestResultComponent::getId, Function.identity()));
        Set<String> multiComponentTests = components.values().stream()
                .collect(Collectors.groupingBy(TestResultComponent::getTestId, Collectors.counting())).entrySet()
                .stream().filter(entry -> entry.getValue() > 1).map(Map.Entry::getKey).collect(Collectors.toSet());
        Map<String, String> dictionaryCache = new HashMap<>();
        Map<String, Map<String, String>> locationCache = new HashMap<>();
        var units = dao.unitsOfMeasure().stream().collect(
                Collectors.toMap(UnitOfMeasure::getId, UnitOfMeasure::getUnitOfMeasureName, (first, second) -> first));
        ZoneId zone = ZoneId.of(request.timezone());
        boolean orderFieldsSelected = request.variables().stream().map(ReportingVariable::id)
                .anyMatch(ORDER_FIELDS::contains);
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
                        if (resultValues.isQualifier(result))
                            continue;
                        var specimen = result.getAnalysis().getSampleItem();
                        if (!Objects.equals(specimenId, specimen.getId())) {
                            specimenId = specimen.getId();
                            Patient patient = dao.patient(specimen.getSample().getId());
                            patientFields = new LinkedHashMap<>(patientFields(patient, zone));
                            patientFields.putAll(ages(patient, specimen.getCollectionDate()));
                            observationFields = new LinkedHashMap<>(
                                    observationFields(dao.observations(specimen.getSample().getId(), specimenId)));
                            observationFields.putAll(locations.forSample(specimen.getSample(), locationCache));
                            if (orderFieldsSelected) {
                                observationFields.putAll(orderFields(specimen.getSample().getId(), specimenId, zone,
                                        request.userLocale(), request.filterSpec().labSectionIds()));
                            }
                        }
                        if (!Objects.equals(sampleId, specimen.getSample().getId())) {
                            sampleId = specimen.getSample().getId();
                            analysisCount = dao.analysisCount(sampleId);
                        }
                        Normalized value = normalize(result, request, components, multiComponentTests, units,
                                dictionaryCache, patientFields, observationFields, analysisCount, zone);
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

    /**
     * The sample type as the application shows it (its localized name), not the
     * catalog description: an environmental sample exported as "Air Minum" while
     * every screen said "Drinking Water". It reads in the language of the user who
     * asked for the export (OGC-1680), not the language the export worker runs in.
     */
    static String sampleTypeName(TypeOfSample type, Locale locale) {
        if (type == null) {
            return null;
        }
        Localization localization = type.getLocalization();
        String localized = localization == null ? null : localization.getLocalizedValue(locale);
        return localized == null || localized.isBlank() ? type.getDescription() : localized;
    }

    /**
     * The unit configured on the matched component, falling back to the test's.
     *
     * <p>
     * The test-level unit is only a legacy mirror of the primary component's unit,
     * so a multi-component test whose secondary component has its own {@code uomId}
     * would otherwise pair that component's value with the primary's unit. Legacy
     * results carry no component and keep the test unit.
     */
    private static String resultUnit(TestResultComponent component, Test test, Map<String, String> units) {
        if (component != null && component.getUomId() != null) {
            String configured = units.get(component.getUomId());
            if (configured != null) {
                return configured;
            }
        }
        return test.getUnitOfMeasure() == null ? null : test.getUnitOfMeasure().getUnitOfMeasureName();
    }

    private Normalized normalize(Result r, ExportSnapshot request, Map<String, TestResultComponent> components,
            Set<String> multiComponentTests, Map<String, String> units, Map<String, String> dictionaryCache,
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
        Timestamp receivedAt = received(specimen, sample);
        a.put("receivedDate", date(receivedAt, zone));
        a.put("receivedTime", time(receivedAt, zone));
        a.put("orderDate", sample.getEnteredDate() == null ? null : sample.getEnteredDate().toLocalDate().toString());
        a.put("sampleType", sampleTypeName(specimen.getTypeOfSample(), request.userLocale()));
        a.put("sampleStatus", statuses.getStatusNameFromId(specimen.getStatusId()));
        a.put("orderStatus", statuses.getStatusNameFromId(sample.getStatusId()));
        a.put("priority", sample.getPriority() == null ? null : sample.getPriority().toString());
        a.put("numberOfTests", Long.toString(analysisCount));
        a.put("resultId", r.getId());
        a.put("testName", ExportTestNames.name(test, request.userLocale()));
        a.put("component", component == null || !multiComponentTests.contains(component.getTestId()) ? null
                : ExportTestNames.componentLabel(component, request.userLocale()));
        a.put("loincCode", test.getLoinc());
        a.put("resultUnit", resultUnit(component, test, units));
        a.put("resultStatus", analysis.isCorrectedSincePatientReport() ? "Corrected"
                : statuses.getStatusNameFromId(analysis.getStatusId()));
        a.put("dateResulted", timestamp(analysis.getCompletedDate(), zone));
        a.put("validationDate", timestamp(analysis.getReleasedDate(), zone));
        a.put("labSection", ExportTestNames.sectionName(analysis.getTestSection(), request.userLocale()));
        Instant order = sample.getEnteredDate() == null ? null
                : sample.getEnteredDate().toLocalDate().atStartOfDay(zone).toInstant();
        Instant collected = instant(specimen.getCollectionDate());
        Instant received = instant(receivedAt);
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
        String value = resultValues.format(r, dictionaryCache);
        String multiKey = ResultType.isMultiSelectVariant(r.getResultType())
                ? analysis.getId() + ":" + componentId + ":" + r.getGrouping() + ":" + r.getResultType()
                : null;
        return new Normalized(r.getId(), specimen.getId(), a, fields, value, multiKey);
    }

    /**
     * The patient's age when the specimen was collected, in whole years, months and
     * weeks, each counted on its own as the Routine CSV did (OGC-1655).
     */
    static Map<String, String> ages(Patient patient, Timestamp collected) {
        Map<String, String> ages = new LinkedHashMap<>();
        if (patient == null || patient.getBirthDate() == null || collected == null) {
            return ages;
        }
        ages.put("ageYears", Integer.toString(DateUtil.getAgeInYears(patient.getBirthDate(), collected)));
        ages.put("ageMonths", Integer.toString(DateUtil.getAgeInMonths(patient.getBirthDate(), collected)));
        ages.put("ageWeeks", Integer.toString(DateUtil.getAgeInWeeks(patient.getBirthDate(), collected)));
        return ages;
    }

    private Map<String, String> orderFields(String sampleId, String specimenId, ZoneId zone, Locale locale,
            List<String> labSectionIds) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("enteredDate", timestamp(dao.enteredAt(sampleId), zone));
        fields.put("program", dao.program(sampleId));
        fields.put("labUnit", dao.labUnits(specimenId, labSectionIds).stream()
                .map(unit -> ExportTestNames.sectionName(unit, locale)).sorted().collect(Collectors.joining("; ")));
        return fields;
    }

    /**
     * When the specimen reached the lab: its own receipt, or the order's when the
     * specimen was not received on its own, as order entry records it (OGC-1655).
     */
    static Timestamp received(SampleItem specimen, Sample sample) {
        return specimen.getReceivedDate() != null ? specimen.getReceivedDate()
                : sample == null ? null : sample.getReceivedTimestamp();
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
