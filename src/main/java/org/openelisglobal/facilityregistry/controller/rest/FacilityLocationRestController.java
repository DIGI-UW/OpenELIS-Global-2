package org.openelisglobal.facilityregistry.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.facilityregistry.form.FacilityLocationForm;
import org.openelisglobal.facilityregistry.form.FacilityLocationStatus;
import org.openelisglobal.facilityregistry.service.FacilityLocationModeException;
import org.openelisglobal.facilityregistry.service.FacilityRegistryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The facility's coordinates and its registration in the facility registry.
 * POST stores the position an administrator's browser reports, in automatic
 * location mode only.
 */
@RestController
@RequestMapping("/rest/facility-location")
@PreAuthorize("hasRole('ADMIN')")
public class FacilityLocationRestController extends BaseRestController {

    @Autowired
    private FacilityRegistryService facilityRegistryService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public FacilityLocationStatus getFacilityLocation() {
        return facilityRegistryService.getStatus();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public FacilityLocationStatus recordBrowserLocation(HttpServletRequest request,
            @RequestBody FacilityLocationForm form) {
        return facilityRegistryService.recordBrowserLocation(form.getLatitude(), form.getLongitude(),
                getSysUserId(request));
    }

    @ExceptionHandler(FacilityLocationModeException.class)
    public ResponseEntity<Map<String, String>> manualMode(FacilityLocationModeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> notReady(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}
