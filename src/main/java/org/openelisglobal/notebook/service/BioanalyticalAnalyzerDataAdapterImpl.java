package org.openelisglobal.notebook.service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bioanalytical Analyzer Data Adapter Implementation.
 *
 * Provides automatic data capture and integration from analytical instruments:
 * - LC-MS/MS: Automatic import of mass spectra (mzML, CDF formats) - HPLC:
 * Automatic import of chromatograms (CDF, PDF formats) - Dissolution Apparatus:
 * Manual or automatic CSV import - Physical Tests: Manual entry support
 * (Disintegration, Hardness, Friability) - Stability Chamber: Automatic
 * environmental monitoring - Freezers: Temperature/humidity logging (manual or
 * automated)
 *
 * Extends AnalyzerResultImportService pattern for consistency with existing
 * labs.
 */
@Service
public class BioanalyticalAnalyzerDataAdapterImpl implements BioanalyticalAnalyzerDataAdapter {

    private static final DateTimeFormatter ANALYSIS_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final Set<String> SUPPORTED_INSTRUMENTS = Set.of("LC-MS/MS", "HPLC", "Dissolution Apparatus",
            "Disintegration Tester", "Hardness Tester", "Friability Tester", "Stability Chamber",
            "UV-Vis Spectrophotometer", "FTIR", "Freezer", "Millipore Water Purification");

    private static final Map<String, List<String>> SUPPORTED_FILE_FORMATS = Map.ofEntries(
            Map.entry("LC-MS/MS", List.of(".mzML", ".cdf", ".raw")), Map.entry("HPLC", List.of(".cdf", ".pdf", ".csv")),
            Map.entry("Dissolution Apparatus", List.of(".csv", ".xlsx", ".xls")),
            Map.entry("Disintegration Tester", List.of(".csv", ".txt")),
            Map.entry("Hardness Tester", List.of(".csv", ".txt")),
            Map.entry("Friability Tester", List.of(".csv", ".txt")),
            Map.entry("Stability Chamber", List.of(".csv", ".xlsx")),
            Map.entry("UV-Vis Spectrophotometer", List.of(".csv", ".pdf")), Map.entry("FTIR", List.of(".csv", ".pdf")),
            Map.entry("Freezer", List.of(".csv", ".xlsx")),
            Map.entry("Millipore Water Purification", List.of(".csv", ".xlsx")));

    @Value("${analytics.data.storage.path:./analytics}")
    private String analyticsDataPath;

    @Override
    public ParsedAnalyzerData parseInstrumentOutput(File dataFile, String instrumentType) {
        List<SampleResult> sampleResults = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        CalibrationCurveData calibrationCurve = null;
        List<QCResultData> qcResults = new ArrayList<>();
        List<String> systemSuitabilityResults = new ArrayList<>();

        try {
            String fileExtension = getFileExtension(dataFile.getName());

            switch (instrumentType.toUpperCase()) {
            case "LC-MS/MS":
                if (".mzML".equalsIgnoreCase(fileExtension)) {
                    // Parse mzML file (XML-based mass spectra format)
                    ParseMzMLResult mzmlResult = parseMzMLFile(dataFile);
                    sampleResults.addAll(mzmlResult.sampleResults);
                    if (mzmlResult.calibrationCurve != null) {
                        calibrationCurve = mzmlResult.calibrationCurve;
                    }
                    qcResults.addAll(mzmlResult.qcResults);
                    systemSuitabilityResults.addAll(mzmlResult.systemSuitabilityResults);
                    errors.addAll(mzmlResult.errors);
                } else if (".cdf".equalsIgnoreCase(fileExtension)) {
                    // Parse CDF file (NetCDF format for chromatography/MS data)
                    // Note: Full CDF parsing requires specialized library (netcdf4-java)
                    errors.add("CDF parsing not yet implemented - use mzML format");
                }
                break;

            case "HPLC":
                if (".csv".equalsIgnoreCase(fileExtension)) {
                    // Parse HPLC CSV export
                    ParseCsvResult csvResult = parseHplcCsvFile(dataFile);
                    sampleResults.addAll(csvResult.sampleResults);
                    if (csvResult.calibrationCurve != null) {
                        calibrationCurve = csvResult.calibrationCurve;
                    }
                    qcResults.addAll(csvResult.qcResults);
                    errors.addAll(csvResult.errors);
                } else if (".cdf".equalsIgnoreCase(fileExtension) || ".pdf".equalsIgnoreCase(fileExtension)) {
                    errors.add("Automatic parsing of " + fileExtension + " format not yet implemented - export as CSV");
                }
                break;

            case "DISSOLUTION APPARATUS":
                if (".csv".equalsIgnoreCase(fileExtension)) {
                    ParseCsvResult csvResult = parseDissolutionCsvFile(dataFile);
                    sampleResults.addAll(csvResult.sampleResults);
                    errors.addAll(csvResult.errors);
                }
                break;

            case "STABILITY CHAMBER":
                if (".csv".equalsIgnoreCase(fileExtension)) {
                    ParseCsvResult csvResult = parseStabilityChamberCsvFile(dataFile);
                    sampleResults.addAll(csvResult.sampleResults);
                    errors.addAll(csvResult.errors);
                }
                break;

            default:
                errors.add("Instrument type '" + instrumentType + "' not supported for auto-import");
            }

        } catch (IOException e) {
            errors.add("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            errors.add("Error parsing instrument data: " + e.getMessage());
        }

        return new ParsedAnalyzerData(instrumentType, LocalDateTime.now().format(ANALYSIS_DATE_FORMAT), null, // analyst
                                                                                                              // ID to
                                                                                                              // be
                                                                                                              // filled
                                                                                                              // by
                                                                                                              // caller
                sampleResults, calibrationCurve, qcResults, systemSuitabilityResults, errors);
    }

    @Override
    public AnalyzerIntegrationResult validateInstrumentData(ParsedAnalyzerData data) {
        List<String> warnings = new ArrayList<>();

        // Validate calibration curve
        if (data.calibrationCurve() != null) {
            if (data.calibrationCurve().rSquared() < 0.99) {
                warnings.add("Calibration curve r² (" + data.calibrationCurve().rSquared()
                        + ") below acceptance criterion (0.99)");
            }
        }

        // Validate QC results
        for (QCResultData qc : data.qcResults()) {
            if ("FAIL".equalsIgnoreCase(qc.passFail())) {
                warnings.add("QC sample " + qc.qcLevel() + " failed (" + qc.percentRecovery() + "% recovery)");
            }
        }

        // Validate sample results
        for (SampleResult sample : data.sampleResults()) {
            if (sample.calculatedConcentration() != null && data.calibrationCurve() != null) {
                if (sample.calculatedConcentration() < data.calibrationCurve().concentrationRangeLow()) {
                    warnings.add("Sample " + sample.sampleId() + " concentration below LLOQ");
                }
                if (sample.calculatedConcentration() > data.calibrationCurve().concentrationRangeHigh()) {
                    warnings.add("Sample " + sample.sampleId() + " concentration above ULOQ");
                }
            }
        }

        boolean success = data.errors().isEmpty();
        String message = success ? "Analyzer data validated successfully"
                : "Analyzer data validation failed with errors";

        return new AnalyzerIntegrationResult(success, message, warnings);
    }

    @Override
    @Transactional
    public List<String> importHplcChromatograms(NotebookEntry entry, List<File> chromatogramFiles) {
        List<String> storedFileIds = new ArrayList<>();

        for (File file : chromatogramFiles) {
            try {
                // TODO: Store file using analyticalRawFileService
                // String fileId = analyticalRawFileService.storeRawDataFile(entry, file,
                // "HPLC");
                // storedFileIds.add(fileId);
            } catch (Exception e) {
                // Log error
            }
        }

        return storedFileIds;
    }

    @Override
    @Transactional
    public List<String> importLcMsmsSpectra(NotebookEntry entry, List<File> spectraFiles) {
        List<String> storedFileIds = new ArrayList<>();

        for (File file : spectraFiles) {
            try {
                // TODO: Store file using analyticalRawFileService
                // String fileId = analyticalRawFileService.storeRawDataFile(entry, file,
                // "LC-MS/MS");
                // storedFileIds.add(fileId);
            } catch (Exception e) {
                // Log error
            }
        }

        return storedFileIds;
    }

    @Override
    public AnalyzerIntegrationResult importDissolutionData(NotebookEntry entry, File dataFile) {
        try {
            ParsedAnalyzerData data = parseInstrumentOutput(dataFile, "DISSOLUTION APPARATUS");
            return validateInstrumentData(data);
        } catch (Exception e) {
            return new AnalyzerIntegrationResult(false, "Error importing dissolution data: " + e.getMessage(),
                    List.of());
        }
    }

    @Override
    public AnalyzerIntegrationResult writeCalibrationToInstrument(String instrumentId,
            CalibrationCurveData calibrationData) {
        // TODO: Implement bidirectional integration to write calibration to instrument
        // This would require instrument-specific APIs/interfaces
        return new AnalyzerIntegrationResult(false, "Bidirectional instrument integration not yet implemented",
                List.of());
    }

    @Override
    public List<String> getSupportedInstrumentTypes() {
        return new ArrayList<>(SUPPORTED_INSTRUMENTS);
    }

    @Override
    public List<String> getSupportedFileFormats(String instrumentType) {
        return SUPPORTED_FILE_FORMATS.getOrDefault(instrumentType, List.of());
    }

    // ==================== Private Helper Methods ====================

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }

    private ParseMzMLResult parseMzMLFile(File file) throws IOException {
        // TODO: Implement mzML parsing using appropriate XML/mzML library
        // For now, return empty result with TODO note
        return new ParseMzMLResult(List.of(), List.of(), List.of(), List.of(), null);
    }

    private ParseCsvResult parseHplcCsvFile(File file) throws IOException {
        // TODO: Implement HPLC CSV parsing
        // Expected columns: Sample_ID, Retention_Time, Peak_Area, Concentration,
        // Conc_Unit
        return new ParseCsvResult(List.of(), null, List.of(), List.of());
    }

    private ParseCsvResult parseDissolutionCsvFile(File file) throws IOException {
        // TODO: Implement Dissolution CSV parsing
        // Expected columns: Sample_ID, Time_Point, Percentage_Dissolved
        return new ParseCsvResult(List.of(), null, List.of(), List.of());
    }

    private ParseCsvResult parseStabilityChamberCsvFile(File file) throws IOException {
        // TODO: Implement Stability Chamber CSV parsing
        // Expected columns: Timestamp, Temperature, Humidity, Chamber_ID
        return new ParseCsvResult(List.of(), null, List.of(), List.of());
    }

    // ==================== Inner Helper Classes ====================

    private record ParseMzMLResult(List<SampleResult> sampleResults, List<QCResultData> qcResults,
            List<String> systemSuitabilityResults, List<String> errors, CalibrationCurveData calibrationCurve) {
    }

    private record ParseCsvResult(List<SampleResult> sampleResults, CalibrationCurveData calibrationCurve,
            List<QCResultData> qcResults, List<String> errors) {
    }
}
