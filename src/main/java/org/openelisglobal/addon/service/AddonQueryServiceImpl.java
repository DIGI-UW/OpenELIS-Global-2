package org.openelisglobal.addon.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.addon.AddonCatalog;
import org.openelisglobal.addon.dao.AddonQueryDAO;
import org.openelisglobal.addon.dao.AddonQueryDAO.StatusIds;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.search.service.SearchResultsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AddonQueryServiceImpl implements AddonQueryService {

    public static final String TAT_DEFINITION = "sample received to result released (validated), in minutes";

    @Autowired
    private AddonQueryDAO dao;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private SearchResultsService searchResultsService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private AnalysisService analysisService;

    StatusIds statusIds() {
        return new StatusIds(
                List.of(id(AnalysisStatus.NotStarted), id(AnalysisStatus.TechnicalRejected),
                        id(AnalysisStatus.BiologistRejected)),
                id(AnalysisStatus.TechnicalAcceptance), id(AnalysisStatus.Finalized),
                List.of(id(AnalysisStatus.Canceled), id(AnalysisStatus.SampleRejected)));
    }

    private int id(AnalysisStatus status) {
        String id = statusService.getStatusID(status);
        // a status missing from this database matches nothing
        return id == null || id.isBlank() ? -1 : Integer.parseInt(id);
    }

    @Override
    public Map<String, Object> orders(LocalDate from, LocalDate to, String status, String section, int limit,
            int offset, boolean withPatients) {
        AddonQueryDAO.Page<AddonQueryDAO.OrderRow> page = dao.orders(from, to, status, section, statusIds(), limit,
                offset);
        List<Map<String, Object>> orders = new ArrayList<>();
        for (AddonQueryDAO.OrderRow r : page.rows()) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("accessionNumber", r.accessionNumber());
            o.put("receivedDate", instant(r.received()));
            o.put("testCount", r.tests());
            o.put("pending", r.pending());
            o.put("awaitingValidation", r.awaitingValidation());
            o.put("finalized", r.finalized());
            o.put("sections", r.sections());
            if (withPatients) {
                patientOfOrder(r.accessionNumber()).ifPresent(p -> o.put("patient", p));
            }
            orders.add(o);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("from", from.toString());
        out.put("to", to.toString());
        out.put("total", page.total() < 0 ? offset : page.total());
        out.put("offset", offset);
        out.put("orders", orders);
        return out;
    }

    @Override
    public Map<String, Object> worklist(String stage, String section, int limit) {
        StatusIds ids = statusIds();
        List<Integer> statuses = "validation".equals(stage) ? List.of(ids.awaitingValidation())
                : List.copyOf(ids.pending());
        AddonQueryDAO.Page<AddonQueryDAO.WorkItem> page = dao.worklist(statuses, section, limit);
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> items = new ArrayList<>();
        for (AddonQueryDAO.WorkItem w : page.rows()) {
            Map<String, Object> i = new LinkedHashMap<>();
            i.put("accessionNumber", w.accessionNumber());
            i.put("analysisId", w.analysisId());
            i.put("testCode", w.testCode());
            i.put("testName", w.testName());
            i.put("section", w.section());
            i.put("receivedDate", instant(w.received()));
            i.put("waitingMinutes", w.received() == null ? null : Duration.between(w.received(), now).toMinutes());
            items.add(i);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("stage", stage);
        out.put("total", page.total());
        out.put("items", items);
        return out;
    }

    @Override
    public Map<String, Object> orderCounts(LocalDate from, LocalDate to, String groupBy) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AddonQueryDAO.CountRow r : dao.counts(from, to, groupBy, statusIds())) {
            Map<String, Object> row = new LinkedHashMap<>();
            String key = r.key();
            if ("status".equals(groupBy)) {
                AnalysisStatus status = statusService.getAnalysisStatusForID(key);
                key = status == null ? key : status.name();
            }
            row.put("key", key);
            row.put("orders", r.orders());
            row.put("tests", r.tests());
            row.put("finalized", r.finalized());
            rows.add(row);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("from", from.toString());
        out.put("to", to.toString());
        out.put("groupBy", groupBy);
        out.put("rows", rows);
        return out;
    }

    @Override
    public Map<String, Object> turnaround(LocalDate from, LocalDate to, String groupBy) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AddonQueryDAO.TatRow r : dao.turnaround(from, to, groupBy, statusIds().finalized())) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", r.key());
            row.put("count", r.count());
            row.put("medianMinutes", Math.round(r.medianMinutes()));
            row.put("p90Minutes", Math.round(r.p90Minutes()));
            rows.add(row);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("from", from.toString());
        out.put("to", to.toString());
        out.put("groupBy", groupBy);
        out.put("definition", TAT_DEFINITION);
        out.put("rows", rows);
        return out;
    }

    @Override
    public Map<String, Object> tests(String section, Boolean active) {
        List<Map<String, Object>> tests = new ArrayList<>();
        for (AddonQueryDAO.TestRow t : dao.tests(section, active)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", t.code());
            row.put("name", t.name());
            row.put("loinc", t.loinc());
            row.put("section", t.section());
            row.put("sampleType", t.sampleType() == null ? null : t.sampleType().replace("|", ", "));
            row.put("unit", t.unit());
            row.put("active", t.active());
            tests.add(row);
        }
        return Map.of("tests", tests);
    }

    @Override
    public Map<String, Object> findPatients(String query) {
        String q = query.trim().replaceAll("\\s+", " ");
        Map<String, PatientSearchResults> found = new LinkedHashMap<>();
        if (q.matches(".*\\d.*")) {
            // an identifier: exact match on each kind
            add(found, searchResultsService.getSearchResultsExact(null, null, null, null, q, null, null, null, null,
                    null));
            add(found, searchResultsService.getSearchResultsExact(null, null, q, null, null, null, null, null, null,
                    null));
            add(found, searchResultsService.getSearchResultsExact(null, null, null, q, null, null, null, null, null,
                    null));
            add(found, searchResultsService.getSearchResultsExact(null, null, null, null, null, q, null, null, null,
                    null));
        } else {
            // a name: the same search as the patient search page, the words as last
            // name, first name, or split between them in both orders
            add(found, searchResultsService.getSearchResults(q, null, null, null, null, null, null, null, null, null));
            add(found, searchResultsService.getSearchResults(null, q, null, null, null, null, null, null, null, null));
            String[] words = q.split(" ");
            if (words.length > 1) {
                String first = words[0];
                String rest = q.substring(first.length() + 1);
                String last = words[words.length - 1];
                String head = q.substring(0, q.length() - last.length() - 1);
                add(found, searchResultsService.getSearchResults(first, rest, null, null, null, null, null, null, null,
                        null));
                add(found, searchResultsService.getSearchResults(last, head, null, null, null, null, null, null, null,
                        null));
            }
        }
        List<Map<String, Object>> patients = new ArrayList<>();
        for (String id : found.keySet()) {
            if (patients.size() == AddonCatalog.MAX_PATIENT_MATCHES) {
                break;
            }
            patient(id).ifPresent(patients::add);
        }
        return Map.of("patients", patients);
    }

    private static void add(Map<String, PatientSearchResults> found, List<PatientSearchResults> results) {
        if (results == null) {
            return;
        }
        for (PatientSearchResults r : results) {
            if (r.getPatientID() != null && found.size() < AddonCatalog.MAX_PATIENT_MATCHES * 2) {
                found.putIfAbsent(r.getPatientID(), r);
            }
        }
    }

    @Override
    public Optional<Map<String, Object>> patient(String patientId) {
        if (patientId == null || !patientId.matches("\\d{1,18}")) {
            return Optional.empty();
        }
        Patient patient = patientService.getData(patientId);
        return patient == null ? Optional.empty() : Optional.of(describe(patient));
    }

    @Override
    public Optional<Map<String, Object>> patientOrders(String patientId, int limit) {
        if (patient(patientId).isEmpty()) {
            return Optional.empty();
        }
        List<Map<String, Object>> orders = new ArrayList<>();
        sampleService.getSamplesForPatient(patientId).stream()
                .sorted(Comparator.comparing(Sample::getReceivedTimestamp,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .filter(s -> AddonCatalog.isAccessionNumber(s.getAccessionNumber())).limit(limit).forEach(s -> {
                    Map<String, Object> o = new LinkedHashMap<>();
                    o.put("accessionNumber", s.getAccessionNumber());
                    o.put("receivedDate",
                            s.getReceivedTimestamp() == null ? null : s.getReceivedTimestamp().toInstant().toString());
                    o.put("testCount", analysisService.getAnalysesBySampleId(s.getId()).size());
                    orders.add(o);
                });
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("patientId", patientId);
        out.put("orders", orders);
        return Optional.of(out);
    }

    @Override
    public Optional<Map<String, Object>> patientOfOrder(String accessionNumber) {
        Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);
        if (sample == null) {
            return Optional.empty();
        }
        Patient patient = sampleHumanService.getPatientForSample(sample);
        return patient == null || patient.getId() == null ? Optional.empty() : Optional.of(describe(patient));
    }

    Map<String, Object> describe(Patient patient) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("patientId", patient.getId());
        String first = blankToNull(patientService.getFirstName(patient));
        String last = blankToNull(patientService.getLastName(patient));
        p.put("firstName", first);
        p.put("lastName", last);
        p.put("fullName", blankToNull(String.join(" ", last == null ? "" : last, first == null ? "" : first).trim()));
        p.put("sex", patient.getGender());
        p.put("birthDate", patient.getBirthDate() == null ? null
                : patient.getBirthDate().toLocalDateTime().toLocalDate().toString());
        Map<String, Object> ids = new LinkedHashMap<>();
        putIfSet(ids, "nationalId", patientService.getNationalId(patient));
        putIfSet(ids, "subjectNumber", patientService.getSubjectNumber(patient));
        putIfSet(ids, "stNumber", patientService.getSTNumber(patient));
        putIfSet(ids, "externalId", patientService.getExternalId(patient));
        p.put("identifiers", ids);
        return p;
    }

    private static void putIfSet(Map<String, Object> map, String key, String value) {
        String v = blankToNull(value);
        if (v != null) {
            map.put(key, v);
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String instant(LocalDateTime t) {
        return t == null ? null : t.atZone(ZoneId.systemDefault()).toInstant().toString();
    }
}
