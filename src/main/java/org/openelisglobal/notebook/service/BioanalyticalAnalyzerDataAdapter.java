package org.openelisglobal.notebook.service;

import java.io.File;
import java.util.List;
import java.util.Map;
import org.openelisglobal.notebook.valueholder.NotebookEntry;

/**
 * Interface for Bioanalytical analyzer data integration.
 *
 * Handles automatic data capture from analytical instruments (LC-MS/MS, HPLC,
 * dissolution apparatus, stability chambers) and integration into the notebook
 * workflow. Supports: - Automatic data import from instrument output files
 * (CDF, mzML, CSV) - Bidirectional integration (read from and write to
 * instruments) - Calibration curve parsing and storage - QC result extraction -
 * Raw data file archiving
 *
 * Extends the existing AnalyzerResultImportService pattern for
 * bioanalytical-specific instrument types and data formats.
 */
public interface BioanalyticalAnalyzerDataAdapter {

    /**
     * Parsed analyzer data structure containing all extracted information from
     * instrument output.
     */
    record ParsedAnalyzerData(String instrumentType, String analysisDate, String analystId,
            List<SampleResult> sampleResults, CalibrationCurveData calibrationCurve, List<QCResultData> qcResults,
            List<String> systemSuitabilityResults, List<String> errors) {
    }

    record SampleResult(String sampleId, String retentionTime, Double peakArea, Double calculatedConcentration,
            String concentrationUnit, Map<String, Object> additionalData) {
    }

    record CalibrationCurveData(String equation, Double rSquared, Double slope, Double intercept,
            Double concentrationRangeLow, Double concentrationRangeHigh, String acceptanceCriteria, String status) {
    }

    record QCResultData(String qcLevel, // LOW, MEDIUM, HIGH
            Double expectedConcentration, Double measuredConcentration, Double percentRecovery, String passFail) {
    }

    record AnalyzerIntegrationResult(boolean success, String message, List<String> warnings) {
    }

    /**
     * Parse instrument output file (CDF, mzML, CSV) based on instrument type.
     *
     * @param dataFile       The instrument output file
     * @param instrumentType Type of instrument (LC-MS/MS, HPLC, dissolution, etc.)
     * @return Parsed analyzer data with results, calibration, QC
     */
    ParsedAnalyzerData parseInstrumentOutput(File dataFile, String instrumentType);

    /**
     * Validate parsed instrument data against acceptance criteria.
     *
     * @param data The parsed analyzer data
     * @return Integration result with success status and messages
     */
    AnalyzerIntegrationResult validateInstrumentData(ParsedAnalyzerData data);

    /**
     * Import HPLC chromatogram files into notebook entry.
     *
     * @param entry             The notebook entry
     * @param chromatogramFiles List of chromatogram files (CDF, PDF formats)
     * @return List of stored raw data file IDs
     */
    List<String> importHplcChromatograms(NotebookEntry entry, List<File> chromatogramFiles);

    /**
     * Import LC-MS/MS mass spectra files into notebook entry.
     *
     * @param entry        The notebook entry
     * @param spectraFiles List of spectra files (mzML, CDF formats)
     * @return List of stored raw data file IDs
     */
    List<String> importLcMsmsSpectra(NotebookEntry entry, List<File> spectraFiles);

    /**
     * Import dissolution apparatus data (typically CSV format).
     *
     * @param entry    The notebook entry
     * @param dataFile Dissolution data file
     * @return Import result
     */
    AnalyzerIntegrationResult importDissolutionData(NotebookEntry entry, File dataFile);

    /**
     * Write calibration curve parameters to instrument (bidirectional integration).
     *
     * @param instrumentId    The instrument ID
     * @param calibrationData Calibration curve to write
     * @return Integration result
     */
    AnalyzerIntegrationResult writeCalibrationToInstrument(String instrumentId, CalibrationCurveData calibrationData);

    /**
     * Get list of supported instrument types for automatic data integration.
     *
     * @return List of supported instrument types
     */
    List<String> getSupportedInstrumentTypes();

    /**
     * Get list of supported file formats for a given instrument type.
     *
     * @param instrumentType The instrument type
     * @return List of supported file extensions (e.g., ".cdf", ".mzML", ".csv")
     */
    List<String> getSupportedFileFormats(String instrumentType);
}
