package org.openelisglobal.accreditation.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.openelisglobal.accreditation.service.AccreditingBodyService;
import org.openelisglobal.accreditation.valueholder.AccreditingBody;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ControllerUtills;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN') and hasAuthority('TEST_CATALOG_MANAGE')")
public class AccreditingBodyRestController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "code", "name", "logoPath", "logoVisibilityMode",
            "thresholdPct", "displayOrder", "active" };

    @Autowired
    private AccreditingBodyService accreditingBodyService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "/accrediting-bodies", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllAccreditingBodies(
            @RequestParam(value = "activeOnly", required = false, defaultValue = "false") boolean activeOnly) {
        List<AccreditingBody> bodies = activeOnly ? accreditingBodyService.getAllActive()
                : accreditingBodyService.getAllOrderedByDisplayOrder();
        return ResponseEntity.ok(bodies);
    }

    @PostMapping(value = "/accrediting-bodies", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createAccreditingBody(HttpServletRequest request,
            @RequestBody @Valid AccreditingBody accreditingBody, BindingResult result) {
        if (result.hasErrors()) {
            saveErrors(result);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getAllErrors());
        }
        try {
            accreditingBody.setSysUserId(ControllerUtills.getSysUserId(request));
            Long id = accreditingBodyService.insert(accreditingBody);
            AccreditingBody saved = accreditingBodyService.get(id);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved != null ? saved : accreditingBody);
        } catch (LIMSDuplicateRecordException e) {
            LogEvent.logDebug(e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            errorMap.put("status", 409);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMap);
        } catch (LIMSRuntimeException e) {
            LogEvent.logDebug(e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PatchMapping(value = "/accrediting-bodies/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateAccreditingBody(HttpServletRequest request, @PathVariable Long id,
            @RequestBody @Valid AccreditingBody accreditingBody, BindingResult result) {
        if (result.hasErrors()) {
            saveErrors(result);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getAllErrors());
        }
        try {
            accreditingBody.setId(id);
            accreditingBody.setSysUserId(ControllerUtills.getSysUserId(request));
            AccreditingBody updated = accreditingBodyService.update(accreditingBody);
            return ResponseEntity.ok(updated);
        } catch (LIMSRuntimeException e) {
            LogEvent.logDebug(e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping(value = "/accrediting-bodies/{id}")
    public ResponseEntity<?> deleteAccreditingBody(HttpServletRequest request, @PathVariable Long id) {
        AccreditingBody existing = accreditingBodyService.get(id);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Accrediting body not found with id: " + id);
        }
        try {
            existing.setSysUserId(ControllerUtills.getSysUserId(request));
            accreditingBodyService.delete(existing);
            return ResponseEntity.noContent().build();
        } catch (LIMSRuntimeException e) {
            LogEvent.logDebug(e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            errorMap.put("status", 409);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMap);
        }
    }

    @PostMapping(value = "/accrediting-bodies/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadLogo(HttpServletRequest request, @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        AccreditingBody existing = accreditingBodyService.get(id);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Accrediting body not found with id: " + id);
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/png") && !contentType.equals("image/svg+xml"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Only PNG or SVG files are allowed");
        }
        if (file.getSize() > 500 * 1024) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File size must not exceed 500 KB");
        }
        if ("image/png".equals(contentType)) {
            try {
                BufferedImage image = ImageIO.read(file.getInputStream());
                if (image != null && (image.getWidth() < 64 || image.getHeight() < 64)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Logo dimensions must be at least 64x64 pixels");
                }
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Could not read image file");
            }
        }
        try {
            String sysUserIdStr = ControllerUtills.getSysUserId(request);
            AccreditingBody updated = accreditingBodyService.uploadLogo(id, file, sysUserIdStr);
            return ResponseEntity.ok(updated);
        } catch (LIMSRuntimeException e) {
            LogEvent.logDebug(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping(value = "/accrediting-bodies/{id}/logo")
    public ResponseEntity<?> deleteLogo(HttpServletRequest request, @PathVariable Long id) {
        AccreditingBody existing = accreditingBodyService.get(id);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Accrediting body not found with id: " + id);
        }
        try {
            String sysUserIdStr = ControllerUtills.getSysUserId(request);
            AccreditingBody updated = accreditingBodyService.removeLogo(id, sysUserIdStr);
            return ResponseEntity.ok(updated);
        } catch (LIMSRuntimeException e) {
            LogEvent.logDebug(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @Override
    protected String findLocalForward(String forward) {
        return "PageNotFound";
    }

    @Override
    protected String getPageTitleKey() {
        return null;
    }

    @Override
    protected String getPageSubtitleKey() {
        return null;
    }
}
