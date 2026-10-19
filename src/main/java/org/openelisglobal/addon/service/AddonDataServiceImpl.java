package org.openelisglobal.addon.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.addon.AddonCatalog;
import org.openelisglobal.addon.dao.AddonOutboxDAO;
import org.openelisglobal.addon.security.AddonToken;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AddonDataServiceImpl implements AddonDataService {

    @Autowired
    private AddonOutboxDAO outboxDAO;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private IStatusService statusService;

    @Override
    public boolean mayRead(AddonToken.Claims claims, String accessionNumber) {
        if (!AddonCatalog.isAccessionNumber(accessionNumber)) {
            return false;
        }
        if (claims.isService()) {
            return outboxDAO.wasNotified(claims.addonId(), accessionNumber);
        }
        return claims.accessions().contains(accessionNumber);
    }

    @Override
    public Optional<Map<String, Object>> order(String accessionNumber) {
        Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);
        if (sample == null) {
            return Optional.empty();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("accessionNumber", sample.getAccessionNumber());
        out.put("receivedDate",
                sample.getReceivedTimestamp() == null ? null : sample.getReceivedTimestamp().toInstant().toString());
        out.put("testCount", analysisService.getAnalysesBySampleId(sample.getId()).size());
        return Optional.of(out);
    }

    @Override
    public Optional<Map<String, Object>> results(String accessionNumber) {
        Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);
        if (sample == null) {
            return Optional.empty();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Analysis analysis : analysisService.getAnalysesBySampleId(sample.getId())) {
            Test test = analysis.getTest();
            List<Result> results = resultService.getResultsByAnalysis(analysis);
            Result result = results.isEmpty() ? null : results.get(0);
            AnalysisStatus status = statusService.getAnalysisStatusForID(analysis.getStatusId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("analysisId", analysis.getId());
            row.put("testCode", test == null ? null : test.getLocalCode());
            row.put("loinc", test == null ? null : test.getLoinc());
            row.put("testName", test == null ? null : test.getName());
            row.put("value", result == null ? null : resultService.getResultValue(result, ", ", true, false));
            row.put("unit", test == null || test.getUnitOfMeasure() == null ? null
                    : test.getUnitOfMeasure().getUnitOfMeasureName());
            row.put("low", result == null ? null : result.getMinNormal());
            row.put("high", result == null ? null : result.getMaxNormal());
            row.put("status", status == null ? null : status.name());
            rows.add(row);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("accessionNumber", sample.getAccessionNumber());
        out.put("results", rows);
        return Optional.of(out);
    }

    @Override
    public Optional<Map<String, Object>> patient(String accessionNumber) {
        Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);
        if (sample == null) {
            return Optional.empty();
        }
        Patient patient = sampleHumanService.getPatientForSample(sample);
        if (patient == null) {
            return Optional.empty();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("accessionNumber", sample.getAccessionNumber());
        out.put("sex", patient.getGender());
        out.put("birthDate", patient.getBirthDate() == null ? null
                : patient.getBirthDate().toLocalDateTime().toLocalDate().toString());
        return Optional.of(out);
    }
}
