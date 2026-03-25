package org.openelisglobal.analyzer.service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerType;
import org.openelisglobal.analyzerimport.analyzerreaders.ASTMAnalyzerReader;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnalyzerBidirectionalServiceImpl implements AnalyzerBidirectionalService {

    private static final String ORDER_HEADER = "H|\\^&|||OpenELIS^OrderSend^1.0|||||||LIS2-A2\r\n";
    private static final String QUERY_HEADER = "H|\\^&|||OpenELIS^ResultsQuery^1.0|||||||LIS2-A2\r\n";
    private static final String TERMINATOR = "L|1|N\r\n";

    private final AnalyzerService analyzerService;
    private final SampleService sampleService;
    private final SampleHumanService sampleHumanService;
    private final AnalysisService analysisService;
    private final AnalyzerTestMappingService analyzerTestMappingService;
    private final AnalyzerBridgeTransportService bridgeTransportService;

    @Autowired
    public AnalyzerBidirectionalServiceImpl(AnalyzerService analyzerService, SampleService sampleService,
            SampleHumanService sampleHumanService, AnalysisService analysisService,
            AnalyzerTestMappingService analyzerTestMappingService,
            AnalyzerBridgeTransportService bridgeTransportService) {
        this.analyzerService = analyzerService;
        this.sampleService = sampleService;
        this.sampleHumanService = sampleHumanService;
        this.analysisService = analysisService;
        this.analyzerTestMappingService = analyzerTestMappingService;
        this.bridgeTransportService = bridgeTransportService;
    }

    @Override
    public Map<String, Object> sendOrder(String analyzerId, String accessionNumber) {
        String normalizedAccession = normalizeRequired("accessionNumber", accessionNumber);
        Analyzer analyzer = getRequiredAnalyzer(analyzerId);
        AnalyzerType analyzerType = getRequiredAnalyzerType(analyzer);
        Sample sample = getRequiredSample(normalizedAccession);

        List<String> analyzerCodes = resolveMappedAnalyzerCodes(analyzerType.getId(), sample, null);
        if (analyzerCodes.isEmpty()) {
            throw new LIMSRuntimeException("No mapped orders found for accession " + normalizedAccession);
        }

        Patient patient = sampleHumanService.getPatientForSample(sample);
        String message = buildOrderMessage(sample, patient, analyzerCodes);
        bridgeTransportService.sendMessage(analyzer, message);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Order message sent successfully");
        response.put("orderCount", analyzerCodes.size());
        return response;
    }

    @Override
    public Map<String, Object> queryResults(String analyzerId, String accessionNumber, List<String> testCodes,
            String sysUserId) {
        String normalizedAccession = normalizeRequired("accessionNumber", accessionNumber);
        String normalizedUserId = normalizeRequired("sysUserId", sysUserId);
        Analyzer analyzer = getRequiredAnalyzer(analyzerId);
        String queryMessage = buildQueryMessage(normalizedAccession, testCodes);
        String rawResponse = bridgeTransportService.sendMessage(analyzer, queryMessage);

        int importedResultCount = ingestAstmResponse(rawResponse, normalizedUserId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Results queried and imported successfully");
        response.put("importedResultCount", importedResultCount);
        response.put("rawResponse", rawResponse);
        return response;
    }

    private int ingestAstmResponse(String rawResponse, String sysUserId) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return 0;
        }

        ASTMAnalyzerReader reader = new ASTMAnalyzerReader();
        boolean streamRead = reader.readStream(new ByteArrayInputStream(rawResponse.getBytes(StandardCharsets.UTF_8)));
        if (!streamRead) {
            throw new LIMSRuntimeException("Failed to parse analyzer response: " + reader.getError());
        }

        boolean inserted = reader.insertAnalyzerData(sysUserId);
        if (!inserted) {
            throw new LIMSRuntimeException("Failed to import queried results: " + reader.getError());
        }

        String[] lines = rawResponse.split("\\r?\\n");
        int count = 0;
        for (String line : lines) {
            if (line != null && line.startsWith("R|")) {
                count++;
            }
        }
        return count;
    }

    private String buildOrderMessage(Sample sample, Patient patient, List<String> analyzerCodes) {
        String patientId = "";
        String patientName = "";
        if (patient != null) {
            patientId = safe(patient.getNationalId());
            Person person = patient.getPerson();
            if (person != null) {
                patientName = safe(person.getLastName()) + "^" + safe(person.getFirstName());
            }
        }

        String orderTimestamp = sample.getEnteredDate() != null
                ? new SimpleDateFormat("yyyyMMddHHmmss").format(sample.getEnteredDate())
                : new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        String testsField = analyzerCodes.stream().map(code -> "^^^" + code).collect(Collectors.joining("\\"));
        return ORDER_HEADER + "P|1|" + patientId + "||" + patientName + "\r\n" + "O|1|" + sample.getAccessionNumber()
                + "||" + testsField + "|R|" + orderTimestamp + "||||A\r\n" + TERMINATOR;
    }

    private String buildQueryMessage(String accessionNumber, List<String> testCodes) {
        String testsField = "ALL";
        if (testCodes != null && !testCodes.isEmpty()) {
            List<String> normalized = testCodes.stream().filter(v -> v != null && !v.trim().isEmpty()).map(String::trim)
                    .collect(Collectors.toList());
            if (!normalized.isEmpty()) {
                testsField = String.join("\\", normalized);
            }
        }
        return QUERY_HEADER + "Q|1|" + accessionNumber + "||" + testsField + "\r\n" + TERMINATOR;
    }

    private List<String> resolveMappedAnalyzerCodes(String analyzerTypeId, Sample sample,
            List<String> testCodesFilter) {
        List<Analysis> analyses = analysisService.getAnalysesBySampleId(sample.getId());
        if (analyses == null || analyses.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, List<String>> testIdToCodes = buildTestIdToCodesMap(analyzerTypeId);
        Set<String> requestedFilters = new LinkedHashSet<>();
        if (testCodesFilter != null) {
            for (String code : testCodesFilter) {
                if (code != null && !code.trim().isEmpty()) {
                    requestedFilters.add(code.trim());
                }
            }
        }

        Set<String> orderedCodes = new LinkedHashSet<>();
        for (Analysis analysis : analyses) {
            Test test = analysis.getTest();
            if (test == null || test.getId() == null) {
                continue;
            }
            List<String> mappedCodes = testIdToCodes.get(test.getId());
            if (mappedCodes == null) {
                continue;
            }
            for (String mappedCode : mappedCodes) {
                if (requestedFilters.isEmpty() || requestedFilters.contains(mappedCode)) {
                    orderedCodes.add(mappedCode);
                }
            }
        }
        return new ArrayList<>(orderedCodes);
    }

    private Map<String, List<String>> buildTestIdToCodesMap(String analyzerTypeId) {
        Map<String, List<String>> testIdToCodes = new LinkedHashMap<>();
        List<AnalyzerTestMapping> mappings = analyzerTestMappingService.getAll();
        if (mappings == null || mappings.isEmpty()) {
            return testIdToCodes;
        }
        for (AnalyzerTestMapping mapping : mappings) {
            if (!analyzerTypeId.equals(mapping.getAnalyzerTypeId()) || mapping.getTestId() == null
                    || mapping.getAnalyzerTestName() == null) {
                continue;
            }
            String analyzerCode = mapping.getAnalyzerTestName().trim();
            if (analyzerCode.isEmpty()) {
                continue;
            }
            testIdToCodes.computeIfAbsent(mapping.getTestId(), ignored -> new ArrayList<>());
            List<String> codes = testIdToCodes.get(mapping.getTestId());
            if (!codes.contains(analyzerCode)) {
                codes.add(analyzerCode);
            }
        }
        return testIdToCodes;
    }

    private Analyzer getRequiredAnalyzer(String analyzerId) {
        String normalizedId = normalizeRequired("analyzerId", analyzerId);
        Analyzer analyzer = analyzerService.get(normalizedId);
        if (analyzer == null) {
            throw new LIMSRuntimeException("Analyzer not found: " + normalizedId);
        }
        return analyzer;
    }

    private AnalyzerType getRequiredAnalyzerType(Analyzer analyzer) {
        if (analyzer.getAnalyzerType() == null || analyzer.getAnalyzerType().getId() == null) {
            throw new LIMSRuntimeException("Analyzer " + analyzer.getId() + " has no analyzer type configured");
        }
        return analyzer.getAnalyzerType();
    }

    private Sample getRequiredSample(String accessionNumber) {
        Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);
        if (sample == null) {
            throw new LIMSRuntimeException("Sample not found for accession " + accessionNumber);
        }
        return sample;
    }

    private String normalizeRequired(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new LIMSRuntimeException(fieldName + " is required");
        }
        return value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
