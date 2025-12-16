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
import org.openelisglobal.notebook.form.PharmaManifestImportForm;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.notebook.service.PharmaManifestImportService;
import org.openelisglobal.notebook.service.PharmaManifestImportService.ParseError;
import org.openelisglobal.notebook.service.PharmaManifestImportService.ParsedManifest;
import org.openelisglobal.notebook.service.PharmaManifestImportService.PharmaManifestImportResult;
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
 * REST controller for Pharmaceuticals manifest CSV import operations.
 */
@RestController
@RequestMapping("/rest/notebook/pharma")
public class PharmaManifestImportController extends BaseRestController {

    @Autowired
    private PharmaManifestImportService pharmaManifestImportService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    @PostMapping(value = "/entry/{entryId}/samples/preview-manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> previewManifestForEntry(@PathVariable("entryId") Integer entryId,
            @RequestPart("file") MultipartFile file, @RequestPart("mapping") PharmaManifestImportForm form) {

        Optional<org.openelisglobal.notebook.valueholder.NotebookEntry> optEntry = notebookEntryService
                .getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try (InputStream inputStream = file.getInputStream()) {
            ParsedManifest parsed = pharmaManifestImportService.parseManifestCsv(inputStream, form);
            List<ParseError> validationErrors = pharmaManifestImportService.validateSampleTypes(parsed);

            List<ParseError> allErrors = new java.util.ArrayList<>(parsed.errors());
            allErrors.addAll(validationErrors);

            Map<String, Object> response = new HashMap<>();
            response.put("entryId", entryId);
            response.put("totalRows", parsed.rows().size());
            response.put("validRows", parsed.rows().size() - allErrors.size());
            response.put("totalSamplesToCreate",
                    parsed.rows().stream().mapToInt(PharmaManifestImportService.PharmaManifestRow::numOfSamples).sum());
            response.put("rows", parsed.rows().stream().map(row -> {
                Map<String, Object> rowMap = new HashMap<>();
                rowMap.put("rowNumber", row.rowNumber());
                rowMap.put("groupId", row.groupId());
                rowMap.put("sampleType", row.sampleType());
                rowMap.put("numOfSamples", row.numOfSamples());
                rowMap.put("chemicalName", row.chemicalName());
                rowMap.put("grade", row.grade());
                rowMap.put("lotNumber", row.lotNumber());
                rowMap.put("expiryOrRetestDate", row.expiryOrRetestDate());
                rowMap.put("storageCondition", row.storageCondition());
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
            @RequestPart("file") MultipartFile file, @RequestPart("mapping") PharmaManifestImportForm form,
            HttpServletRequest httpRequest) {

        Optional<org.openelisglobal.notebook.valueholder.NotebookEntry> optEntry = notebookEntryService
                .getMatch("id", entryId);
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
            ParsedManifest parsed = pharmaManifestImportService.parseManifestCsv(inputStream, form);

            if (!parsed.errors().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "CSV parsing errors");
                response.put("errors", parsed.errors());
                return ResponseEntity.badRequest().body(response);
            }

            List<ParseError> validationErrors = pharmaManifestImportService.validateSampleTypes(parsed);
            if (!validationErrors.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Sample type validation errors");
                response.put("errors", validationErrors);
                return ResponseEntity.badRequest().body(response);
            }

            PharmaManifestImportResult result = pharmaManifestImportService.createSamplesForEntry(entryId, parsed,
                    sysUserId);

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

