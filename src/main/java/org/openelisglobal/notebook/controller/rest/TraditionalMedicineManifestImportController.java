package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.form.TraditionalMedicineManifestImportForm;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.notebook.service.TraditionalMedicineManifestImportService;
import org.openelisglobal.notebook.service.TraditionalMedicineManifestImportService.ParseError;
import org.openelisglobal.notebook.service.TraditionalMedicineManifestImportService.ParsedManifest;
import org.openelisglobal.notebook.service.TraditionalMedicineManifestImportService.TraditionalMedicineManifestImportResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for Traditional Medicine manifest CSV import operations.
 */
@RestController
@RequestMapping("/rest/notebook/tradmed")
public class TraditionalMedicineManifestImportController extends BaseRestController {

    @Autowired
    private TraditionalMedicineManifestImportService traditionalMedicineManifestImportService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    @PostMapping(value = "/entry/{entryId}/samples/preview-manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> previewManifestForEntry(@PathVariable("entryId") Integer entryId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("mapping") TraditionalMedicineManifestImportForm form) {

        Optional<org.openelisglobal.notebook.valueholder.NotebookEntry> optEntry = notebookEntryService.getMatch("id",
                entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try (InputStream inputStream = file.getInputStream()) {
            ParsedManifest parsed = traditionalMedicineManifestImportService.parseManifestCsv(inputStream, form);
            List<ParseError> validationErrors = traditionalMedicineManifestImportService.validateSampleData(parsed);

            List<ParseError> allErrors = new java.util.ArrayList<>(parsed.errors());
            allErrors.addAll(validationErrors);

            Map<String, Object> response = new HashMap<>();
            response.put("entryId", entryId);
            response.put("totalRows", parsed.rows().size());
            response.put("validRows", parsed.rows().size() - allErrors.size());
            response.put("totalSamplesToCreate",
                    parsed.rows().stream().mapToInt(
                            TraditionalMedicineManifestImportService.TraditionalMedicineManifestRow::numOfSamples)
                            .sum());
            response.put("rows", parsed.rows().stream().map(row -> {
                Map<String, Object> rowMap = new HashMap<>();
                rowMap.put("rowNumber", row.rowNumber());
                rowMap.put("sampleId", row.sampleId());
                rowMap.put("sampleCategory", row.sampleCategory());
                rowMap.put("sourceType", row.sourceType());
                rowMap.put("localName", row.localName());
                rowMap.put("scientificName", row.scientificName());
                rowMap.put("plantPart", row.plantPart());
                rowMap.put("sampleCondition", row.sampleCondition());
                rowMap.put("numOfSamples", row.numOfSamples());
                return rowMap;
            }).collect(Collectors.toList()));
            response.put("errors", allErrors.stream().map(error -> {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("rowNumber", error.rowNumber());
                errorMap.put("column", error.column());
                errorMap.put("message", error.message());
                return errorMap;
            }).collect(Collectors.toList()));
            response.put("valid", allErrors.isEmpty());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to read file: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping(value = "/entry/{entryId}/samples/create-from-manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createSamplesForEntry(@PathVariable("entryId") Integer entryId,
            @RequestPart("file") MultipartFile file, @RequestPart("mapping") TraditionalMedicineManifestImportForm form,
            HttpServletRequest httpRequest) {

        Optional<org.openelisglobal.notebook.valueholder.NotebookEntry> optEntry = notebookEntryService.getMatch("id",
                entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        try (InputStream inputStream = file.getInputStream()) {
            ParsedManifest parsed = traditionalMedicineManifestImportService.parseManifestCsv(inputStream, form);

            if (!parsed.errors().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "CSV parsing errors");
                response.put("errors", parsed.errors());
                return ResponseEntity.badRequest().body(response);
            }

            List<ParseError> validationErrors = traditionalMedicineManifestImportService.validateSampleData(parsed);
            if (!validationErrors.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Sample data validation errors");
                response.put("errors", validationErrors);
                return ResponseEntity.badRequest().body(response);
            }

            TraditionalMedicineManifestImportResult result = traditionalMedicineManifestImportService
                    .createSamplesForEntry(entryId, parsed, sysUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.errors().isEmpty());
            response.put("entryId", entryId);
            response.put("totalRequested", result.totalRequested());
            response.put("totalCreated", result.totalCreated());
            response.put("createdAccessionNumbers", result.createdAccessionNumbers());
            if (!result.errors().isEmpty()) {
                response.put("errors", result.errors());
            }

            return result.errors().isEmpty() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);

        } catch (IOException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to read file: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Override
    protected String getSysUserId(HttpServletRequest request) {
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
        if (usd == null) {
            return null;
        }
        return String.valueOf(usd.getSystemUserId());
    }
}
