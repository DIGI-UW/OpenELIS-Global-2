package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.notebook.service.BioanalyticalAnalyzerDataAdapter;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.notebook.service.QCTrendingService;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for Bioanalytical analytical test execution and data capture.
 *
 * Handles Stage 3 (Analytical Test Execution) operations: - Upload raw
 * instrument data (chromatograms, spectra, CSV files) - Parse and validate
 * analyzer output - Store calibration curves - Track QC results - Flag
 * out-of-range results - Support bidirectional instrument integration
 *
 * Endpoints: - GET /instruments - List supported instruments - GET
 * /instruments/{type}/formats - Get supported file formats - POST
 * /entry/{entryId}/analyzer-data/upload - Upload raw instrument data - POST
 * /entry/{entryId}/analyzer-data/validate - Validate analyzer data - GET
 * /entry/{entryId}/qc-trending - Get QC trending data - GET
 * /instruments/{id}/quality - Get instrument quality metrics
 */
@RestController
@RequestMapping("/rest/notebook/bioanalytical")
public class BioanalyticalAnalysisController extends BaseRestController {

    @Autowired
    private BioanalyticalAnalyzerDataAdapter analyzerDataAdapter;

    @Autowired(required = false)
    private QCTrendingService qcTrendingService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    /**
     * Get list of supported instruments for automatic data integration.
     *
     * @return List of instrument types (LC-MS/MS, HPLC, Dissolution, etc.)
     */
    @GetMapping(value = "/instruments", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSupportedInstruments() {
        List<String> instruments = analyzerDataAdapter.getSupportedInstrumentTypes();

        Map<String, Object> response = new HashMap<>();
        response.put("instruments", instruments);
        response.put("total", instruments.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get supported file formats for a specific instrument type.
     *
     * @param instrumentType The instrument type (e.g., "LC-MS/MS", "HPLC")
     * @return List of supported file extensions
     */
    @GetMapping(value = "/instruments/{instrumentType}/formats", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSupportedFileFormats(
            @PathVariable("instrumentType") String instrumentType) {
        List<String> formats = analyzerDataAdapter.getSupportedFileFormats(instrumentType);

        Map<String, Object> response = new HashMap<>();
        response.put("instrumentType", instrumentType);
        response.put("supportedFormats", formats);
        response.put("total", formats.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Upload and parse raw instrument data file (chromatogram, spectrum, CSV).
     *
     * @param entryId        The notebook entry ID
     * @param instrumentType The instrument type
     * @param file           The raw data file
     * @param request        HTTP request (for user session)
     * @return Parse result with extracted data
     */
    @PostMapping(value = "/entry/{entryId}/analyzer-data/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadAnalyzerData(@PathVariable("entryId") Integer entryId,
            @RequestParam("instrumentType") String instrumentType, @RequestPart("file") MultipartFile file,
            HttpServletRequest request) {

        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            // Save file to temp location
            java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("bioanalyzer_",
                    "_" + file.getOriginalFilename());
            file.transferTo(tempFile.toFile());

            // Parse analyzer data
            BioanalyticalAnalyzerDataAdapter.ParsedAnalyzerData parsedData = analyzerDataAdapter
                    .parseInstrumentOutput(tempFile.toFile(), instrumentType);

            // Validate data
            BioanalyticalAnalyzerDataAdapter.AnalyzerIntegrationResult validationResult = analyzerDataAdapter
                    .validateInstrumentData(parsedData);

            Map<String, Object> response = new HashMap<>();
            response.put("success", validationResult.success());
            response.put("message", validationResult.message());
            response.put("instrumentType", parsedData.instrumentType());
            response.put("analysisDate", parsedData.analysisDate());
            response.put("totalSamples", parsedData.sampleResults().size());

            if (parsedData.calibrationCurve() != null) {
                Map<String, Object> calibrationMap = new HashMap<>();
                calibrationMap.put("rSquared", parsedData.calibrationCurve().rSquared());
                calibrationMap.put("slope", parsedData.calibrationCurve().slope());
                calibrationMap.put("intercept", parsedData.calibrationCurve().intercept());
                calibrationMap.put("concentrationRangeLow", parsedData.calibrationCurve().concentrationRangeLow());
                calibrationMap.put("concentrationRangeHigh", parsedData.calibrationCurve().concentrationRangeHigh());
                calibrationMap.put("status", parsedData.calibrationCurve().status());
                response.put("calibrationCurve", calibrationMap);
            }

            response.put("totalQCResults", parsedData.qcResults().size());

            if (!validationResult.warnings().isEmpty()) {
                response.put("warnings", validationResult.warnings());
            }

            if (!parsedData.errors().isEmpty()) {
                response.put("partialSuccess", true);
                response.put("errors", parsedData.errors());
            }

            // Clean up temp file
            try {
                java.nio.file.Files.delete(tempFile);
            } catch (IOException e) {
                // Log but don't fail
            }

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to process analyzer data: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Validate parsed analyzer data against acceptance criteria.
     *
     * @param entryId             The notebook entry ID
     * @param calibrationRSquared The calibration curve r² value
     * @param qcPassCount         Number of QC results that passed
     * @param qcTotalCount        Total number of QC results
     * @return Validation result with messages
     */
    @PostMapping(value = "/entry/{entryId}/analyzer-data/validate", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateAnalyzerData(@PathVariable("entryId") Integer entryId,
            @RequestParam("calibrationRSquared") Double calibrationRSquared,
            @RequestParam("qcPassCount") Integer qcPassCount, @RequestParam("qcTotalCount") Integer qcTotalCount) {

        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        List<String> warnings = new java.util.ArrayList<>();

        // Check calibration curve acceptance (r² >= 0.99)
        boolean calibrationPass = calibrationRSquared >= 0.99;
        response.put("calibrationAccepted", calibrationPass);
        if (!calibrationPass) {
            warnings.add("Calibration curve r² (" + calibrationRSquared + ") below 0.99 threshold");
        }

        // Check QC pass rate
        double qcPassRate = (double) qcPassCount / qcTotalCount * 100;
        boolean qcPass = qcPassRate >= 80; // 80% threshold
        response.put("qcAccepted", qcPass);
        response.put("qcPassRate", String.format("%.1f%%", qcPassRate));
        if (!qcPass) {
            warnings.add("QC pass rate (" + String.format("%.1f%%", qcPassRate) + ") below 80%");
        }

        boolean overallPass = calibrationPass && qcPass;
        response.put("overallValidation", overallPass ? "PASS" : "FAIL");
        response.put("warnings", warnings);

        return ResponseEntity.ok(response);
    }

    /**
     * Get QC trending data for an instrument (Levey-Jennings chart).
     *
     * @param entryId      The notebook entry ID
     * @param instrumentId The instrument ID
     * @param qcLevel      QC level (LOW, MEDIUM, HIGH)
     * @param numberOfDays Number of days of historical data
     * @return QC trending data
     */
    @GetMapping(value = "/entry/{entryId}/qc-trending", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getQCTrending(@PathVariable("entryId") Integer entryId,
            @RequestParam("instrumentId") String instrumentId, @RequestParam("qcLevel") String qcLevel,
            @RequestParam(value = "numberOfDays", defaultValue = "30") int numberOfDays) {

        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (qcTrendingService == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "QC Trending service not available");
            return ResponseEntity.internalServerError().body(errorResponse);
        }

        java.time.LocalDate endDate = java.time.LocalDate.now();
        java.time.LocalDate startDate = endDate.minusDays(numberOfDays);

        // TODO: Implement QC trending service method
        // QCTrendingService.QCTrendingData trendingData =
        // qcTrendingService.generateQCTrending(instrumentId, qcLevel, startDate,
        // endDate);

        Map<String, Object> response = new HashMap<>();
        response.put("instrumentId", instrumentId);
        response.put("qcLevel", qcLevel);
        response.put("startDate", startDate);
        response.put("endDate", endDate);
        response.put("message", "QC trending data not yet implemented");

        return ResponseEntity.ok(response);
    }

    /**
     * Get instrument quality metrics (uptime, maintenance, success rate).
     *
     * @param instrumentId The instrument ID
     * @return Instrument quality metrics
     */
    @GetMapping(value = "/instruments/{instrumentId}/quality", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getInstrumentQuality(@PathVariable("instrumentId") String instrumentId) {

        Map<String, Object> response = new HashMap<>();
        response.put("instrumentId", instrumentId);
        response.put("message", "Instrument quality metrics not yet implemented - use ReportingMetricsService");

        // TODO: Query quality metrics from database
        response.put("uptimePercentage", 98.5);
        response.put("maintenanceEventsLastMonth", 2);
        response.put("analyticalSuccessRate", 99.2);

        return ResponseEntity.ok(response);
    }
}
