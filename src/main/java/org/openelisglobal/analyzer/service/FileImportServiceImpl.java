package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.openelisglobal.analyzer.dao.AnalyzerFileUploadDAO;
import org.openelisglobal.analyzer.dao.AnalyzerRunDAO;
import org.openelisglobal.analyzer.dao.FileImportConfigurationDAO;
import org.openelisglobal.analyzer.form.AnalyzerRunPreviewForm;
import org.openelisglobal.analyzer.form.PreviewRecordForm;
import org.openelisglobal.analyzer.form.SubmitRequestForm;
import org.openelisglobal.analyzer.valueholder.AnalyzerFileUpload;
import org.openelisglobal.analyzer.valueholder.AnalyzerRun;
import org.openelisglobal.analyzer.valueholder.FileImportConfiguration;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerReader;
import org.openelisglobal.analyzerimport.analyzerreaders.ExcelAnalyzerReader;
import org.openelisglobal.analyzerimport.analyzerreaders.FileAnalyzerReader;
import org.openelisglobal.analyzerresults.dao.AnalyzerResultsDAO;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FileImportServiceImpl extends BaseObjectServiceImpl<FileImportConfiguration, String>
        implements FileImportService {

    @Value("${file.import.base.directory:/data/analyzer-imports}")
    private String baseImportDir;

    @Autowired
    private FileImportConfigurationDAO fileImportConfigurationDAO;

    @Autowired
    private AnalyzerResultsDAO analyzerResultsDAO;

    @Autowired
    private AnalyzerFileUploadDAO analyzerFileUploadDAO;

    @Autowired
    private AnalyzerRunDAO analyzerRunDAO;

    /** Optional: set in tests to avoid SpringContext. */
    private PluginAnalyzerService pluginAnalyzerService;

    @Autowired(required = false)
    public void setPluginAnalyzerService(PluginAnalyzerService pluginAnalyzerService) {
        this.pluginAnalyzerService = pluginAnalyzerService;
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public FileImportServiceImpl() {
        super(FileImportConfiguration.class);
    }

    @Override
    protected FileImportConfigurationDAO getBaseObjectDAO() {
        return fileImportConfigurationDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FileImportConfiguration> getByAnalyzerId(Integer analyzerId) {
        return fileImportConfigurationDAO.findByAnalyzerId(analyzerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileImportConfiguration> getAllActive() {
        return fileImportConfigurationDAO.findAllActive();
    }

    @Override
    public boolean processFile(Path filePath, FileImportConfiguration configuration, String systemUserId) {
        try (InputStream fileStream = Files.newInputStream(filePath)) {
            AnalyzerReader reader = getReaderForFormat(configuration);

            boolean readSuccess = reader.readStream(fileStream);
            if (!readSuccess) {
                String error = reader.getError();
                LogEvent.logError(this.getClass().getSimpleName(), "processFile",
                        "Failed to read file " + filePath + ": " + error);
                return false;
            }

            boolean insertSuccess = reader.insertAnalyzerData(systemUserId);
            if (!insertSuccess) {
                String error = reader.getError();
                LogEvent.logError(this.getClass().getSimpleName(), "processFile",
                        "Failed to insert analyzer data from file " + filePath + ": " + error);
                return false;
            }

            LogEvent.logInfo(this.getClass().getSimpleName(), "processFile",
                    "Successfully processed file: " + filePath + " for analyzer: " + configuration.getAnalyzerId());
            return true;
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "processFile",
                    "IO error processing file " + filePath + ": " + e.getMessage());
            return false;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "processFile",
                    "Unexpected error processing file " + filePath + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public AnalyzerReader getReaderForFormat(FileImportConfiguration configuration) {
        if (configuration == null) {
            return new FileAnalyzerReader();
        }

        String fileFormat = configuration.getFileFormat() == null ? "CSV"
                : configuration.getFileFormat().trim().toUpperCase();

        switch (fileFormat) {
        case "CSV":
        case "TSV":
            return new FileAnalyzerReader(configuration);
        case "EXCEL":
            return new ExcelAnalyzerReader(configuration);
        case "XML":
            return new org.openelisglobal.analyzerimport.analyzerreaders.XmlAnalyzerReader(configuration);
        default:
            LogEvent.logWarn(this.getClass().getSimpleName(), "getReaderForFormat",
                    "Unknown file format '" + fileFormat + "', defaulting to CSV reader");
            return new FileAnalyzerReader(configuration);
        }
    }

    @Override
    public boolean archiveFile(Path filePath, FileImportConfiguration configuration) {
        try {
            if (configuration.getArchiveDirectory() == null || configuration.getArchiveDirectory().isEmpty()) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "archiveFile",
                        "Archive directory not configured for analyzer: " + configuration.getAnalyzerId());
                return false;
            }

            Path archiveDir = Paths.get(configuration.getArchiveDirectory());

            // Defense-in-depth: verify archive path is within base import directory
            try {
                Path basePath = Paths.get(baseImportDir).normalize().toAbsolutePath();
                if (!archiveDir.normalize().toAbsolutePath().startsWith(basePath)) {
                    LogEvent.logError(this.getClass().getSimpleName(), "archiveFile",
                            "Archive directory outside allowed base: " + configuration.getArchiveDirectory());
                    return false;
                }
            } catch (InvalidPathException e) {
                LogEvent.logError(this.getClass().getSimpleName(), "archiveFile",
                        "Invalid archive directory path: " + configuration.getArchiveDirectory());
                return false;
            }

            if (!Files.exists(archiveDir)) {
                Files.createDirectories(archiveDir);
            }

            Path targetPath = archiveDir.resolve(filePath.getFileName());
            Files.move(filePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            LogEvent.logInfo(this.getClass().getSimpleName(), "archiveFile",
                    "Archived file: " + filePath + " to: " + targetPath);
            return true;
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "archiveFile",
                    "Error archiving file: " + filePath + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean moveToErrorDirectory(Path filePath, FileImportConfiguration configuration, String errorMessage) {
        try {
            if (configuration.getErrorDirectory() == null || configuration.getErrorDirectory().isEmpty()) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "moveToErrorDirectory",
                        "Error directory not configured for analyzer: " + configuration.getAnalyzerId());
                return false;
            }

            Path errorDir = Paths.get(configuration.getErrorDirectory());

            // Defense-in-depth: verify error path is within base import directory
            try {
                Path basePath = Paths.get(baseImportDir).normalize().toAbsolutePath();
                if (!errorDir.normalize().toAbsolutePath().startsWith(basePath)) {
                    LogEvent.logError(this.getClass().getSimpleName(), "moveToErrorDirectory",
                            "Error directory outside allowed base: " + configuration.getErrorDirectory());
                    return false;
                }
            } catch (InvalidPathException e) {
                LogEvent.logError(this.getClass().getSimpleName(), "moveToErrorDirectory",
                        "Invalid error directory path: " + configuration.getErrorDirectory());
                return false;
            }

            if (!Files.exists(errorDir)) {
                Files.createDirectories(errorDir);
            }

            Path targetPath = errorDir.resolve(filePath.getFileName());
            Files.move(filePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            LogEvent.logError(this.getClass().getSimpleName(), "moveToErrorDirectory",
                    "Moved failed file: " + filePath + " to: " + targetPath + " - Error: " + errorMessage);
            return true;
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "moveToErrorDirectory",
                    "Error moving file to error directory: " + filePath + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDuplicate(Integer analyzerId, String sampleId, String testCode, String testDate, String testTime) {
        try {
            AnalyzerResults tempResult = new AnalyzerResults();
            tempResult.setAnalyzerId(String.valueOf(analyzerId));
            tempResult.setAccessionNumber(sampleId);
            tempResult.setTestName(testCode);

            Timestamp completeDate = null;
            if (testDate != null && !testDate.isEmpty()) {
                try {
                    String dateTimeString = testDate;
                    if (testTime != null && !testTime.isEmpty()) {
                        dateTimeString += " " + testTime;
                    } else {
                        dateTimeString += " 00:00:00";
                    }
                    SimpleDateFormat[] formats = { new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
                            new SimpleDateFormat("yyyy-MM-dd HH:mm"), new SimpleDateFormat("MM/dd/yyyy HH:mm:ss"),
                            new SimpleDateFormat("dd-MM-yyyy HH:mm:ss") };
                    for (SimpleDateFormat format : formats) {
                        try {
                            completeDate = new Timestamp(format.parse(dateTimeString).getTime());
                            break;
                        } catch (ParseException e) {
                            // Try next format
                        }
                    }
                    if (completeDate == null) {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "isDuplicate",
                                "Could not parse date/time: " + dateTimeString);
                    }
                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "isDuplicate",
                            "Error parsing date/time: " + e.getMessage());
                }
            }
            tempResult.setCompleteDate(completeDate);

            List<AnalyzerResults> duplicates = analyzerResultsDAO.getDuplicateResultByAccessionAndTest(tempResult);

            if (duplicates != null && !duplicates.isEmpty()) {
                if (completeDate != null) {
                    for (AnalyzerResults duplicate : duplicates) {
                        if (duplicate.getCompleteDate() != null && duplicate.getCompleteDate().equals(completeDate)) {
                            LogEvent.logDebug(this.getClass().getSimpleName(), "isDuplicate",
                                    "Found exact duplicate: analyzer=" + analyzerId + ", sample=" + sampleId + ", test="
                                            + testCode + ", date=" + completeDate);
                            return true;
                        }
                    }
                    // If we have a date but no exact match, it's not a duplicate
                    return false;
                } else {
                    // No date provided, consider it a duplicate if analyzerId, accessionNumber and
                    // testName match
                    LogEvent.logDebug(this.getClass().getSimpleName(), "isDuplicate",
                            "Found duplicate (no date check): analyzer=" + analyzerId + ", sample=" + sampleId
                                    + ", test=" + testCode);
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "isDuplicate", "Error checking duplicate for analyzer: "
                    + analyzerId + ", sample: " + sampleId + ", test: " + testCode + ": " + e.getMessage());
            return false; // On error, don't block processing
        }
    }

    @Override
    public AnalyzerRunPreviewForm parseAndPreview(Integer analyzerId, InputStream fileStream, String filename,
            String systemUserId) {
        AnalyzerRunPreviewForm form = new AnalyzerRunPreviewForm();
        form.setTotalRecords(0);
        form.setValidRecords(0);
        form.setWarningRecords(0);
        form.setErrorRecords(0);
        form.setRecords(Collections.emptyList());
        form.setDuplicateWarning(false);

        Optional<FileImportConfiguration> configOpt = getByAnalyzerId(analyzerId);
        if (configOpt.isEmpty()) {
            return form;
        }
        FileImportConfiguration config = configOpt.get();

        byte[] bytes;
        try {
            bytes = fileStream.readAllBytes();
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "parseAndPreview",
                    "Failed to read stream: " + e.getMessage());
            return form;
        }

        String fileHashSha256 = sha256Hex(bytes);
        int uploadedByInt = parseUserId(systemUserId);
        boolean duplicateFile = analyzerFileUploadDAO.findByAnalyzerIdAndFileHash(analyzerId, fileHashSha256)
                .isPresent();

        AnalyzerFileUpload upload = new AnalyzerFileUpload();
        upload.setAnalyzerId(analyzerId);
        upload.setFilename(filename != null ? filename : "upload");
        upload.setFileHashSha256(fileHashSha256);
        upload.setFileSize((long) bytes.length);
        upload.setStatus("PENDING");
        upload.setUploadedBy(uploadedByInt);
        upload.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        analyzerFileUploadDAO.insert(upload);

        if (duplicateFile) {
            form.setDuplicateWarning(true);
        }

        AnalyzerReader reader = getReaderForFormat(config);
        if (!reader.readStream(new ByteArrayInputStream(bytes))) {
            upload.setStatus("ERROR");
            upload.setErrorMessage(reader.getError());
            analyzerFileUploadDAO.update(upload);
            return form;
        }

        List<Map<String, String>> parsed = reader.getParsedRecords();
        Map<String, String> columnMappings = config.getColumnMappings() != null ? config.getColumnMappings()
                : Collections.emptyMap();
        List<PreviewRecordForm> records = new ArrayList<>();
        int valid = 0, warning = 0, error = 0;
        for (int i = 0; i < parsed.size(); i++) {
            PreviewRecordForm rec = toPreviewRecord(i + 1, parsed.get(i), columnMappings, analyzerId);
            records.add(rec);
            switch (rec.getStatus()) {
            case "VALID":
                valid++;
                break;
            case "WARNING":
                warning++;
                break;
            default:
                error++;
                break;
            }
        }

        form.setTotalRecords(parsed.size());
        form.setValidRecords(valid);
        form.setWarningRecords(warning);
        form.setErrorRecords(error);
        form.setRecords(records);
        form.setUploadId(upload.getId());

        List<String> lines = reader.getLines();
        try {
            String linesJson = objectMapper.writeValueAsString(lines);
            AnalyzerRun run = new AnalyzerRun();
            run.setAnalyzerFileUploadId(upload.getId());
            // Note: pluginId stores the analyzer ID (used for plugin lookup by analyzer)
            run.setPluginId(config.getAnalyzerId() != null ? config.getAnalyzerId().toString() : null);
            run.setCustomPreviewData(linesJson);
            run.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            analyzerRunDAO.insert(run);
        } catch (JsonProcessingException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "parseAndPreview",
                    "Could not store preview lines: " + e.getMessage());
        }

        return form;
    }

    @Override
    public void submitResults(Integer analyzerId, SubmitRequestForm request, String systemUserId) {
        if (request == null || request.getPreviewSessionId() == null) {
            throw new IllegalArgumentException("previewSessionId is required");
        }
        Long uploadId = request.getPreviewSessionId();
        Optional<AnalyzerFileUpload> uploadOpt = analyzerFileUploadDAO.get(uploadId);
        if (uploadOpt.isEmpty()) {
            throw new IllegalArgumentException("Upload not found: " + uploadId);
        }
        if (!uploadOpt.get().getAnalyzerId().equals(analyzerId)) {
            throw new IllegalStateException("Upload does not belong to analyzer " + analyzerId);
        }
        AnalyzerFileUpload upload = uploadOpt.get();
        upload.setStatus("PROCESSING");
        analyzerFileUploadDAO.update(upload);

        Optional<AnalyzerRun> runOpt = analyzerRunDAO.findByAnalyzerFileUploadId(uploadId);
        if (runOpt.isEmpty() || runOpt.get().getCustomPreviewData() == null) {
            upload.setStatus("ERROR");
            upload.setErrorMessage("No preview data for submit");
            analyzerFileUploadDAO.update(upload);
            return;
        }

        List<String> lines;
        try {
            lines = objectMapper.readValue(runOpt.get().getCustomPreviewData(), new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            upload.setStatus("ERROR");
            upload.setErrorMessage("Invalid preview data: " + e.getMessage());
            analyzerFileUploadDAO.update(upload);
            return;
        }

        Set<Integer> excluded = new HashSet<>(
                request.getExcludedRows() != null ? request.getExcludedRows() : Collections.emptyList());
        List<String> filtered = new ArrayList<>();
        // Always preserve header (index 0); exclusions use 1-based row numbers
        // matching the rowNumber assigned during preview (see toPreviewRecord)
        if (!lines.isEmpty()) {
            filtered.add(lines.get(0));
        }
        for (int i = 1; i < lines.size(); i++) {
            if (!excluded.contains(i)) {
                filtered.add(lines.get(i));
            }
        }

        PluginAnalyzerService pluginService = pluginAnalyzerService != null ? pluginAnalyzerService
                : SpringContext.getBean(PluginAnalyzerService.class);
        AnalyzerImporterPlugin plugin = pluginService != null
                ? pluginService.getPluginByAnalyzerId(String.valueOf(analyzerId))
                : null;
        if (plugin == null) {
            upload.setStatus("ERROR");
            upload.setErrorMessage("No plugin for analyzer " + analyzerId);
            analyzerFileUploadDAO.update(upload);
            return;
        }
        AnalyzerLineInserter inserter = plugin.getAnalyzerLineInserter();
        if (inserter == null) {
            upload.setStatus("ERROR");
            upload.setErrorMessage("Plugin has no line inserter");
            analyzerFileUploadDAO.update(upload);
            return;
        }
        inserter.setContextAnalyzerId(String.valueOf(analyzerId));
        boolean success = inserter.insert(filtered, systemUserId);
        if (!success) {
            upload.setStatus("ERROR");
            upload.setErrorMessage(inserter.getError());
            analyzerFileUploadDAO.update(upload);
            return;
        }

        upload.setStatus("COMPLETED");
        // Count data rows only (exclude header row)
        upload.setResultCount(Math.max(0, filtered.size() - 1));
        upload.setCompletedAt(new Timestamp(System.currentTimeMillis()));
        analyzerFileUploadDAO.update(upload);
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private static int parseUserId(String systemUserId) {
        if (systemUserId == null || systemUserId.isBlank()) {
            return 1;
        }
        try {
            return Integer.parseInt(systemUserId.trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private PreviewRecordForm toPreviewRecord(int rowNumber, Map<String, String> record,
            Map<String, String> columnMappings, Integer analyzerId) {
        String sampleId = extractField(record, columnMappings, "sampleId", "Sample_ID", "sample_id");
        String testCode = extractField(record, columnMappings, "testCode", "Test_Code", "test_code");
        String result = extractField(record, columnMappings, "result", "Result", "result");
        String testDate = extractField(record, columnMappings, "testDate", "Date", "date");
        String testTime = extractField(record, columnMappings, "testTime", "Time", "time");

        PreviewRecordForm rec = new PreviewRecordForm();
        rec.setRowNumber(rowNumber);
        rec.setSampleId(sampleId);
        rec.setTestCode(testCode);
        rec.setResult(result);
        rec.setValidationMessages(new ArrayList<>());

        if (sampleId == null || sampleId.isBlank() || testCode == null || testCode.isBlank()) {
            rec.setStatus("ERROR");
            if (sampleId == null || sampleId.isBlank()) {
                rec.getValidationMessages().add(msg("MISSING_SAMPLE_ID", "Sample ID is required"));
            }
            if (testCode == null || testCode.isBlank()) {
                rec.getValidationMessages().add(msg("MISSING_TEST_CODE", "Test code is required"));
            }
            return rec;
        }
        if (isDuplicate(analyzerId, sampleId, testCode, testDate, testTime)) {
            rec.setStatus("WARNING");
            rec.getValidationMessages().add(msg("DUPLICATE_RESULT", "Duplicate result already exists"));
            return rec;
        }
        rec.setStatus("VALID");
        return rec;
    }

    private static String extractField(Map<String, String> record, Map<String, String> columnMappings,
            String internalName, String... possibleNames) {
        if (record.containsKey(internalName)) {
            return record.get(internalName);
        }
        for (String name : possibleNames) {
            if (record.containsKey(name)) {
                return record.get(name);
            }
            for (String key : record.keySet()) {
                if (key != null && key.equalsIgnoreCase(name)) {
                    return record.get(key);
                }
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void autoCreateFromProfile(String analyzerId, Map<String, Object> configData, String analyzerName) {
        if (analyzerId == null || configData == null) {
            return;
        }

        Integer analyzerIdInt;
        try {
            analyzerIdInt = Integer.parseInt(analyzerId);
        } catch (NumberFormatException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "autoCreateFromProfile",
                    "Invalid analyzer ID: " + analyzerId);
            return;
        }

        // Don't create if one already exists
        if (fileImportConfigurationDAO.findByAnalyzerId(analyzerIdInt).isPresent()) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "autoCreateFromProfile",
                    "FileImportConfiguration already exists for analyzer " + analyzerId);
            return;
        }

        // Extract file format from configDefaults or protocol
        String fileFormat = "CSV";
        Object configDefaults = configData.get("configDefaults");
        if (configDefaults instanceof Map) {
            Object fmt = ((Map<String, Object>) configDefaults).get("fileFormat");
            if (fmt instanceof String) {
                fileFormat = ((String) fmt).trim().toUpperCase();
            }
        }
        Object protocol = configData.get("protocol");
        if (protocol instanceof Map) {
            Object fmt = ((Map<String, Object>) protocol).get("format");
            if (fmt instanceof String && fileFormat.equals("CSV")) {
                fileFormat = ((String) fmt).trim().toUpperCase();
            }
        }

        // Extract hasHeader from configDefaults
        boolean hasHeader = true;
        if (configDefaults instanceof Map) {
            Object hh = ((Map<String, Object>) configDefaults).get("hasHeader");
            if (hh instanceof Boolean) {
                hasHeader = (Boolean) hh;
            }
        }

        // Extract column mappings
        Map<String, String> columnMappings = new HashMap<>();
        Object colMapping = configData.get("column_mapping");
        if (colMapping instanceof Map) {
            ((Map<?, ?>) colMapping).forEach((k, v) -> {
                if (k instanceof String && v instanceof String) {
                    columnMappings.put((String) k, (String) v);
                }
            });
        }

        // Derive file pattern from supported_extensions or fileFormat
        String filePattern = deriveFilePattern(configData, fileFormat);

        // Build default directory paths using sanitized analyzer name
        String safeName = analyzerName != null ? analyzerName.replaceAll("[^a-zA-Z0-9_-]", "-").toLowerCase()
                : "analyzer-" + analyzerId;
        String importDir = baseImportDir + "/" + safeName + "/incoming";
        String archiveDir = baseImportDir + "/" + safeName + "/archive";
        String errorDir = baseImportDir + "/" + safeName + "/error";

        FileImportConfiguration config = new FileImportConfiguration();
        config.setAnalyzerId(analyzerIdInt);
        config.setFileFormat(fileFormat);
        config.setFilePattern(filePattern);
        config.setHasHeader(hasHeader);
        config.setColumnMappings(columnMappings);
        config.setImportDirectory(importDir);
        config.setArchiveDirectory(archiveDir);
        config.setErrorDirectory(errorDir);
        config.setDelimiter(fileFormat.equals("TSV") ? "\t" : ",");
        config.setActive(true);

        fileImportConfigurationDAO.insert(config);

        LogEvent.logInfo(this.getClass().getSimpleName(), "autoCreateFromProfile",
                "Auto-created FileImportConfiguration for analyzer " + analyzerId + " (format=" + fileFormat
                        + ", importDir=" + importDir + ")");
    }

    @SuppressWarnings("unchecked")
    private String deriveFilePattern(Map<String, Object> configData, String fileFormat) {
        Object extensions = configData.get("supported_extensions");
        if (extensions instanceof List && !((List<?>) extensions).isEmpty()) {
            List<String> exts = (List<String>) extensions;
            if (exts.size() == 1) {
                return "*" + exts.get(0);
            }
            // Multiple extensions: use first one as primary pattern
            return "*" + exts.get(0);
        }
        // Fall back based on format
        switch (fileFormat) {
        case "EXCEL":
            return "*.xls";
        case "TSV":
            return "*.tsv";
        case "XML":
            return "*.xml";
        default:
            return "*.csv";
        }
    }

    private static PreviewRecordForm.ValidationMessageForm msg(String code, String message) {
        PreviewRecordForm.ValidationMessageForm m = new PreviewRecordForm.ValidationMessageForm();
        m.setCode(code);
        m.setMessage(message);
        return m;
    }
}
