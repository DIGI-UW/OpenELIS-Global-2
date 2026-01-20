package org.openelisglobal.resultvalidation.service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultvalidation.bean.ValidationDetailsDTO;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ValidationDetailsServiceImpl implements ValidationDetailsService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private TestService testService;

    @Override
    @Transactional(readOnly = true)
    public ValidationDetailsDTO getValidationDetails(String analysisId) {
        ValidationDetailsDTO details = new ValidationDetailsDTO();

        Analysis analysis = analysisService.get(analysisId);
        if (analysis == null) {
            return details;
        }

        Sample sample = analysis.getSampleItem().getSample();
        Patient patient = sampleHumanService.getPatientForSample(sample);

        if (patient != null) {
            String testId = analysis.getTest().getId();
            details.setPreviousResults(getPreviousResults(testId, patient.getId()));
        }

        details.setQcData(new ArrayList<>());
        details.setReagentLots(new ArrayList<>());
        details.setOrderInfo(getOrderInfo(analysisId));
        details.setAttachments(new ArrayList<>());

        List<Result> results = resultService.getResultsByAnalysis(analysis);
        if (!results.isEmpty()) {
            Result currentResult = results.get(0);
            details.setDeltaCheck(calculateDeltaCheck(analysisId, currentResult.getValue()));
        }

        return details;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ValidationDetailsDTO.PreviousResult> getPreviousResults(String testId, String patientId) {
        List<ValidationDetailsDTO.PreviousResult> previousResults = new ArrayList<>();

        Test test = testService.get(testId);
        Patient patient = patientService.get(patientId);

        if (test == null || patient == null) {
            return previousResults;
        }

        List<Sample> patientSamples = sampleService.getSamplesForPatient(patientId);

        String finalizedStatusId = org.openelisglobal.common.services.StatusService.getInstance()
                .getStatusID(org.openelisglobal.common.services.StatusService.AnalysisStatus.Finalized);

        for (Sample sample : patientSamples) {
            List<Analysis> analyses = analysisService.getAnalysesBySampleId(sample.getId());

            for (Analysis analysis : analyses) {
                if (!testId.equals(analysis.getTest().getId())) {
                    continue;
                }
                if (analysis.getStatusId() == null || !analysis.getStatusId().equals(finalizedStatusId)) {
                    continue;
                }

                List<Result> results = resultService.getResultsByAnalysis(analysis);
                if (!results.isEmpty()) {
                    Result result = results.get(0);
                    String value = result.getValue();

                    if (value != null && !value.isEmpty()) {
                        ValidationDetailsDTO.PreviousResult prevResult = new ValidationDetailsDTO.PreviousResult();

                        if (analysis.getCompletedDate() != null) {
                            prevResult.setDate(analysis.getCompletedDate().toInstant().atZone(ZoneId.systemDefault())
                                    .toLocalDate().format(DATE_FORMAT));
                        }
                        prevResult.setValue(value);
                        prevResult.setStatus("normal");

                        previousResults.add(prevResult);
                    }
                }
            }
        }

        previousResults.sort((a, b) -> {
            if (a.getDate() == null || b.getDate() == null) {
                return 0;
            }
            return b.getDate().compareTo(a.getDate());
        });

        if (previousResults.size() > 10) {
            previousResults = new ArrayList<>(previousResults.subList(0, 10));
        }

        return previousResults;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ValidationDetailsDTO.QCResult> getQCData(String analyzerId, String testId) {
        return new ArrayList<>();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ValidationDetailsDTO.ReagentLot> getReagentLots(String analysisId) {
        return new ArrayList<>();
    }

    @Override
    @Transactional(readOnly = true)
    public ValidationDetailsDTO.OrderInfo getOrderInfo(String analysisId) {
        ValidationDetailsDTO.OrderInfo orderInfo = new ValidationDetailsDTO.OrderInfo();

        Analysis analysis = analysisService.get(analysisId);
        if (analysis == null) {
            return orderInfo;
        }

        Sample sample = analysis.getSampleItem().getSample();

        if (sample != null) {
            if (sample.getCollectionDate() != null) {
                orderInfo.setCollectionDate(sample.getCollectionDate().toInstant().atZone(ZoneId.systemDefault())
                        .toLocalDateTime().format(DATE_TIME_FORMAT));
            }
            if (sample.getReceivedDate() != null) {
                orderInfo.setReceivedDate(sample.getReceivedDate().toInstant().atZone(ZoneId.systemDefault())
                        .toLocalDateTime().format(DATE_TIME_FORMAT));
            }
            orderInfo.setPriority("Routine");
        }

        return orderInfo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ValidationDetailsDTO.Attachment> getAttachments(String analysisId) {
        return new ArrayList<>();
    }

    @Override
    @Transactional(readOnly = true)
    public ValidationDetailsDTO.DeltaCheck calculateDeltaCheck(String analysisId, String currentValue) {
        return null;
    }
}
