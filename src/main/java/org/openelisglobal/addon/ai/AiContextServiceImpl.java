package org.openelisglobal.addon.ai;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.openelisglobal.addon.security.AddonToken;
import org.openelisglobal.addon.service.AddonTokenService;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the de-identified context from the orders of a UI token. Only these
 * fields leave the server: sex, age in years (90 and over grouped), test code,
 * LOINC, name, value, unit, reference range and status, and day differences
 * (received N days ago, an earlier result N days before). Free text (test,
 * section and unit names, text results) goes through {@link Deidentifier} with
 * the identifiers of the patients involved.
 */
@Service
@Transactional(readOnly = true)
public class AiContextServiceImpl implements AiContextService {

    /** Earlier results kept per test: in the patient view, and per case. */
    static final int PREVIOUS_PATIENT = 3;
    static final int PREVIOUS_CASE = 1;
    /** Older orders of a patient read for earlier results. */
    static final int HISTORY_ORDERS = 40;
    static final int AGE_CAP = 90;
    static final int TEXT_MAX = 4000;
    private static final Set<AnalysisStatus> EXCLUDED = Set.of(AnalysisStatus.Canceled, AnalysisStatus.SampleRejected);

    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private PatientIdentityService patientIdentityService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private AddonTokenService tokenService;

    private Clock clock = Clock.systemDefaultZone();

    void setClock(Clock clock) {
        this.clock = clock;
    }

    private record Order(Sample sample, Patient patient, LocalDate day) {
    }

    @Override
    public Map<String, Object> context(AddonToken.Claims claims, String view) {
        List<Order> orders = orders(claims);
        Map<String, Patient> patients = new LinkedHashMap<>();
        for (Order o : orders) {
            patients.putIfAbsent(o.patient().getId(), o.patient());
        }
        Deidentifier deid = new Deidentifier(identifiers(patients.values()));
        LocalDate today = LocalDate.now(clock);
        List<Map<String, Object>> cases = new ArrayList<>();
        String effectiveView = VIEW_PATIENT.equals(view) && patients.size() == 1 ? VIEW_PATIENT : VIEW_CASES;
        if (VIEW_PATIENT.equals(effectiveView)) {
            LocalDate newest = orders.stream().map(Order::day).filter(Objects::nonNull).max(Comparator.naturalOrder())
                    .orElse(null);
            List<Order> current = orders.stream().filter(o -> Objects.equals(o.day(), newest)).toList();
            Patient patient = current.get(0).patient();
            List<Map<String, Object>> results = new ArrayList<>();
            Set<String> currentIds = new HashSet<>();
            Set<String> testIds = new HashSet<>();
            for (Order o : current) {
                currentIds.add(o.sample().getId());
                for (Analysis a : analyses(o.sample())) {
                    results.add(row(a, deid));
                    if (a.getTest() != null) {
                        testIds.add(a.getTest().getId());
                    }
                }
            }
            cases.add(caseOf(1, patient, newest, today, results,
                    previous(patient, newest, currentIds, testIds, PREVIOUS_PATIENT, deid)));
        } else {
            int label = 0;
            for (Order o : orders) {
                if (label >= MAX_CASES) {
                    break;
                }
                label++;
                List<Map<String, Object>> results = new ArrayList<>();
                Set<String> testIds = new HashSet<>();
                for (Analysis a : analyses(o.sample())) {
                    results.add(row(a, deid));
                    if (a.getTest() != null) {
                        testIds.add(a.getTest().getId());
                    }
                }
                cases.add(caseOf(label, o.patient(), o.day(), today, results,
                        previous(o.patient(), o.day(), Set.of(o.sample().getId()), testIds, PREVIOUS_CASE, deid)));
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("user", claims.userId());
        out.put("subject",
                patients.isEmpty() ? "none"
                        : patients.size() > 1 ? "mixed"
                                : "p_" + tokenService.opaqueId(claims.addonId(), patients.keySet().iterator().next()));
        out.put("view", effectiveView);
        out.put("cases", cases);
        return out;
    }

    @Override
    public String deidentify(AddonToken.Claims claims, String text) {
        String t = text == null ? "" : text.length() > TEXT_MAX ? text.substring(0, TEXT_MAX) : text;
        Map<String, Patient> patients = new LinkedHashMap<>();
        for (Order o : orders(claims)) {
            patients.putIfAbsent(o.patient().getId(), o.patient());
        }
        return new Deidentifier(identifiers(patients.values())).scrub(t);
    }

    // ----- loading

    private List<Order> orders(AddonToken.Claims claims) {
        List<Order> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String accession : claims.accessions() == null ? List.<String>of() : claims.accessions()) {
            Sample sample = sampleService.getSampleByAccessionNumber(accession);
            if (sample == null || !seen.add(sample.getId())) {
                continue;
            }
            Patient patient = sampleHumanService.getPatientForSample(sample);
            if (patient == null) {
                continue;
            }
            out.add(new Order(sample, patient, day(sample)));
        }
        return out;
    }

    private List<Analysis> analyses(Sample sample) {
        List<Analysis> out = new ArrayList<>();
        for (Analysis a : analysisService.getAnalysesBySampleId(sample.getId())) {
            AnalysisStatus status = statusService.getAnalysisStatusForID(a.getStatusId());
            if (status == null || !EXCLUDED.contains(status)) {
                out.add(a);
            }
        }
        return out;
    }

    /**
     * Earlier results of the same tests for the patient: orders received before
     * {@code day}, newest first, at most {@code perTest} per test, only results
     * with a value.
     */
    private List<Map<String, Object>> previous(Patient patient, LocalDate day, Set<String> currentSampleIds,
            Set<String> testIds, int perTest, Deidentifier deid) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (day == null || testIds.isEmpty()) {
            return out;
        }
        List<Sample> older = new ArrayList<>(sampleService.getSamplesForPatient(patient.getId()));
        older.removeIf(s -> currentSampleIds.contains(s.getId()) || day(s) == null || !day(s).isBefore(day));
        older.sort(Comparator.comparing(Sample::getReceivedTimestamp, Comparator.nullsLast(Comparator.reverseOrder())));
        Map<String, Integer> count = new HashMap<>();
        for (Sample s : older.subList(0, Math.min(older.size(), HISTORY_ORDERS))) {
            for (Analysis a : analyses(s)) {
                Test test = a.getTest();
                if (test == null || !testIds.contains(test.getId()) || count.getOrDefault(test.getId(), 0) >= perTest) {
                    continue;
                }
                Map<String, Object> r = row(a, deid);
                if (r.get("value") == null) {
                    continue;
                }
                r.remove("status");
                r.remove("section");
                r.put("daysBefore", ChronoUnit.DAYS.between(day(s), day));
                out.add(r);
                count.merge(test.getId(), 1, Integer::sum);
            }
        }
        return out;
    }

    // ----- shaping

    private Map<String, Object> caseOf(int label, Patient patient, LocalDate day, LocalDate today,
            List<Map<String, Object>> results, List<Map<String, Object>> previous) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("label", label);
        c.put("patient", patientOf(patient, day == null ? today : day));
        c.put("receivedDaysAgo", day == null ? null : ChronoUnit.DAYS.between(day, today));
        c.put("results", results);
        c.put("previous", previous);
        return c;
    }

    static Map<String, Object> patientOf(Patient patient, LocalDate at) {
        Map<String, Object> p = new LinkedHashMap<>();
        String g = patient.getGender();
        p.put("sex", "M".equalsIgnoreCase(g) ? "M" : "F".equalsIgnoreCase(g) ? "F" : null);
        LocalDate birth = patient.getBirthDate() == null ? null
                : patient.getBirthDate().toLocalDateTime().toLocalDate();
        if (birth != null && !birth.isAfter(at)) {
            Period age = Period.between(birth, at);
            if (age.getYears() >= AGE_CAP) {
                p.put("ageYears", AGE_CAP);
                p.put("ageCapped", true);
            } else if (age.getYears() < 1) {
                p.put("ageMonths", age.getMonths());
            } else {
                p.put("ageYears", age.getYears());
            }
        }
        return p;
    }

    private Map<String, Object> row(Analysis a, Deidentifier deid) {
        Test test = a.getTest();
        List<Result> results = resultService.getResultsByAnalysis(a);
        Result result = results.isEmpty() ? null : results.get(0);
        String value = result == null ? null : resultService.getResultValue(result, ", ", true, false);
        if (value != null && value.isBlank()) {
            value = null;
        }
        if (value != null && !isNumeric(value)) {
            value = deid.scrub(value.length() > 120 ? value.substring(0, 120) : value);
        }
        AnalysisStatus status = statusService.getAnalysisStatusForID(a.getStatusId());
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("section", a.getTestSection() == null ? null : deid.scrub(a.getTestSection().getTestSectionName()));
        r.put("code", test == null ? null : test.getLocalCode());
        r.put("loinc", test == null ? null : test.getLoinc());
        r.put("name", test == null ? null : deid.scrub(test.getName()));
        r.put("value", value);
        r.put("unit", test == null || test.getUnitOfMeasure() == null ? null
                : deid.scrub(test.getUnitOfMeasure().getUnitOfMeasureName()));
        r.put("low", result == null ? null : result.getMinNormal());
        r.put("high", result == null ? null : result.getMaxNormal());
        r.put("status", status == null ? null : status.name());
        return r;
    }

    static boolean isNumeric(String value) {
        return value.trim().replace(',', '.').matches("[<>]?=?\\s*-?\\d+(\\.\\d+)?");
    }

    private static LocalDate day(Sample s) {
        return s.getReceivedTimestamp() == null ? null : s.getReceivedTimestamp().toLocalDateTime().toLocalDate();
    }

    // ----- identifiers to remove from free text

    private List<String> identifiers(Iterable<Patient> patients) {
        List<String> ids = new ArrayList<>();
        for (Patient p : patients) {
            Person person = p.getPerson();
            if (person != null) {
                String last = nz(person.getLastName());
                String middle = nz(person.getMiddleName());
                String first = nz(person.getFirstName());
                ids.add((last + " " + middle + " " + first).replaceAll("\\s+", " ").trim());
                ids.add((first + " " + middle + " " + last).replaceAll("\\s+", " ").trim());
                ids.add((last + " " + first).trim());
                ids.add((first + " " + last).trim());
                for (String s : new String[] { last, middle, first, person.getStreetAddress(), person.getCity(),
                        person.getState(), person.getMultipleUnit(), person.getPrimaryPhone(), person.getCellPhone(),
                        person.getHomePhone(), person.getWorkPhone(), person.getFax(), person.getEmail() }) {
                    ids.add(s);
                }
            }
            for (String s : new String[] { p.getNationalId(), p.getExternalId(), p.getChartNumber(), p.getUpidCode(),
                    p.getMedicareId(), p.getMedicaidId(), p.getBirthPlace() }) {
                ids.add(s);
            }
            for (PatientIdentity identity : patientIdentityService.getPatientIdentitiesForPatient(p.getId())) {
                ids.add(identity.getIdentityData());
            }
            for (Sample s : sampleService.getSamplesForPatient(p.getId())) {
                ids.add(s.getAccessionNumber());
            }
        }
        ids.removeIf(s -> s == null || s.isBlank());
        return ids;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
