package org.openelisglobal.genericsample.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.genericsample.form.GenericSampleImportResult;
import org.openelisglobal.genericsample.form.GenericSampleOrderForm;
import org.openelisglobal.genericsample.service.GenericSampleOrderService;
import org.openelisglobal.internationalization.MessageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/rest")
public class GenericSampleOrderRestController extends BaseController {

    @Autowired
    private GenericSampleOrderService genericSampleOrderService;

    @Override
    protected String getPageTitleKey() {
        return MessageUtil.getContextualKey("sample.entry.title");
    }

    @Override
    protected String getPageSubtitleKey() {
        return MessageUtil.getContextualKey("sample.entry.title");
    }

    @Override
    protected String findLocalForward(String forward) {
        // REST controllers don't typically use forwards, but we need to implement this
        // abstract method
        return "PageNotFound";
    }

    @PostMapping(value = "/GenericSampleOrder", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> saveGenericSampleOrder(HttpServletRequest request,
            @RequestBody GenericSampleOrderForm form) {
        try {
            String sysUserId = getSysUserId(request);
            Map<String, Object> result = genericSampleOrderService.saveGenericSampleOrder(form, sysUserId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to save generic sample order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping(value = "/GenericSampleOrder", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getGenericSampleOrderByAccessionNumber(
            @RequestParam(required = true) String accessionNumber) {
        try {
            GenericSampleOrderForm form = genericSampleOrderService
                    .getGenericSampleOrderByAccessionNumber(accessionNumber);
            if (form.getDefaultFields() == null || org.apache.commons.validator.GenericValidator
                    .isBlankOrNull(form.getDefaultFields().getLabNo())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "No sample found with accession number: " + accessionNumber);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            return ResponseEntity.ok(form);
        } catch (Exception e) {
            LogEvent.logError(e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve generic sample order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping(value = "/GenericSampleOrder/{accessionNumber}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateGenericSampleOrder(HttpServletRequest request,
            @PathVariable("accessionNumber") String accessionNumber, @RequestBody GenericSampleOrderForm form) {
        try {
            String sysUserId = getSysUserId(request);
            Map<String, Object> result = genericSampleOrderService.updateGenericSampleOrder(accessionNumber, form,
                    sysUserId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to update generic sample order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping(value = "/GenericSampleOrder/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> validateImportFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "File is empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            InputStream inputStream = file.getInputStream();
            GenericSampleImportResult result = genericSampleOrderService.validateImportFile(inputStream,
                    file.getOriginalFilename(), file.getContentType());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to validate import file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping(value = "/GenericSampleOrder/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> importSamplesFromFile(HttpServletRequest request,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "File is empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            String sysUserId = getSysUserId(request);
            InputStream inputStream = file.getInputStream();
            Map<String, Object> result = genericSampleOrderService.importSamplesFromFile(inputStream,
                    file.getOriginalFilename(), file.getContentType(), sysUserId);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to import samples from file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
