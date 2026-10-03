package org.openelisglobal.organization.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.organization.locations.LocationsApi;
import org.openelisglobal.organization.locations.LocationsApi.ActiveRequest;
import org.openelisglobal.organization.locations.LocationsApi.ActiveResult;
import org.openelisglobal.organization.locations.LocationsApi.Area;
import org.openelisglobal.organization.locations.LocationsApi.AreaLevel;
import org.openelisglobal.organization.locations.LocationsApi.AreaRequest;
import org.openelisglobal.organization.locations.LocationsApi.Detail;
import org.openelisglobal.organization.locations.LocationsApi.HistoryEntry;
import org.openelisglobal.organization.locations.LocationsApi.IdentifierCollision;
import org.openelisglobal.organization.locations.LocationsApi.Lists;
import org.openelisglobal.organization.locations.LocationsApi.Page;
import org.openelisglobal.organization.locations.LocationsApi.Query;
import org.openelisglobal.organization.locations.LocationsApi.SaveRequest;
import org.openelisglobal.organization.locations.LocationsApi.SaveResult;
import org.openelisglobal.organization.locations.LocationsApi.UsageDetail;
import org.openelisglobal.organization.locations.LocationsApi.Ward;
import org.openelisglobal.organization.locations.LocationsApi.WardRequest;
import org.openelisglobal.organization.locations.LocationsConflictException;
import org.openelisglobal.organization.locations.LocationsNotFoundException;
import org.openelisglobal.organization.locations.LocationsService;
import org.openelisglobal.organization.locations.LocationsValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OGC-1363: the Locations and Organizations admin menu. Admin only, like the
 * Organization Management and Sampling Sites pages it replaces. A refused save
 * answers 422 with the message per field, a stale save or a blocked activation
 * change answers 409, a missing record 404.
 */
@RestController
@RequestMapping("/rest/locations")
// No class-level @PreAuthorize (S011c): authorization moved to
// LocationsService, where every read now requires PRIV_ORGANIZATION_VIEW and
// every write PRIV_ORGANIZATION_MANAGE. develop added hasRole('ADMIN') here
// while the service carried no gate at all, so removing the annotation
// without gating the service would have opened this surface to any
// authenticated user.
public class LocationsRestController extends BaseRestController {

    @Autowired
    private LocationsService locationsService;

    @GetMapping(value = "/organizations", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page list(@RequestParam(defaultValue = LocationsApi.VIEW_ORGANIZATIONS) String view,
            @RequestParam(required = false) String q, @RequestParam(required = false) List<String> type,
            @RequestParam(required = false) String location, @RequestParam(required = false) List<String> category,
            @RequestParam(required = false) List<String> ownership,
            @RequestParam(defaultValue = LocationsApi.STATUS_ACTIVE) String status,
            @RequestParam(defaultValue = "false") boolean reviewOverdue,
            @RequestParam(defaultValue = "name") String sort, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int pageSize) {
        return locationsService.list(
                new Query(view, q, type, location, category, ownership, status, reviewOverdue, sort, page, pageSize));
    }

    @GetMapping(value = "/organizations/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Detail get(@PathVariable String id) {
        return locationsService.get(id);
    }

    @PostMapping(value = "/organizations", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SaveResult> create(@RequestBody SaveRequest body, HttpServletRequest request) {
        SaveResult result = locationsService.save(new SaveRequest(null, body.kind(), body.name(), body.shortName(),
                body.typeIds(), body.categoryId(), body.ownershipId(), body.description(), body.parentId(),
                body.streetAddress(), body.city(), body.state(), body.zipCode(), body.gpsLatitude(),
                body.gpsLongitude(), body.contactName(), body.phone(), body.fax(), body.email(), body.internetAddress(),
                body.identifiers(), body.referral(), body.site(), body.serviceType(), null), getSysUserId(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping(value = "/organizations/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public SaveResult update(@PathVariable String id, @RequestBody SaveRequest body, HttpServletRequest request) {
        return locationsService.save(new SaveRequest(id, body.kind(), body.name(), body.shortName(), body.typeIds(),
                body.categoryId(), body.ownershipId(), body.description(), body.parentId(), body.streetAddress(),
                body.city(), body.state(), body.zipCode(), body.gpsLatitude(), body.gpsLongitude(), body.contactName(),
                body.phone(), body.fax(), body.email(), body.internetAddress(), body.identifiers(), body.referral(),
                body.site(), body.serviceType(), body.lastupdated()), getSysUserId(request));
    }

    @GetMapping(value = "/organizations/{id}/usage", produces = MediaType.APPLICATION_JSON_VALUE)
    public UsageDetail usage(@PathVariable String id) {
        return locationsService.usage(id);
    }

    @PostMapping(value = "/organizations/active", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ActiveResult setActive(@RequestBody ActiveRequest body, HttpServletRequest request) {
        return locationsService.setActive(body, getSysUserId(request));
    }

    @GetMapping(value = "/organizations/{id}/wards", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Ward> wards(@PathVariable String id, @RequestParam(defaultValue = "false") boolean includeInactive) {
        return locationsService.wards(id, includeInactive);
    }

    @PostMapping(value = "/organizations/{id}/wards", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Ward> createWard(@PathVariable String id, @RequestBody WardRequest body,
            HttpServletRequest request) {
        Ward ward = locationsService.saveWard(
                id, new WardRequest(null, body.name(), body.code(), body.serviceType(), body.contactName(),
                        body.phone(), body.email(), body.gpsLatitude(), body.gpsLongitude(), null),
                getSysUserId(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ward);
    }

    @PutMapping(value = "/organizations/{id}/wards/{wardId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Ward updateWard(@PathVariable String id, @PathVariable String wardId, @RequestBody WardRequest body,
            HttpServletRequest request) {
        return locationsService.saveWard(id,
                new WardRequest(wardId, body.name(), body.code(), body.serviceType(), body.contactName(), body.phone(),
                        body.email(), body.gpsLatitude(), body.gpsLongitude(), body.lastupdated()),
                getSysUserId(request));
    }

    @PostMapping(value = "/wards/{wardId}/move", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Ward moveWard(@PathVariable String wardId, @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        return locationsService.moveWard(wardId, body.get("parentId"), getSysUserId(request));
    }

    @GetMapping(value = "/organizations/{id}/history", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<HistoryEntry> history(@PathVariable String id) {
        return locationsService.history(id);
    }

    @GetMapping(value = "/identifier-collisions", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<IdentifierCollision> identifierCollisions() {
        return locationsService.identifierCollisions();
    }

    @GetMapping(value = "/lists", produces = MediaType.APPLICATION_JSON_VALUE)
    public Lists lists() {
        return locationsService.lists();
    }

    @GetMapping(value = "/areas/levels", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AreaLevel> areaLevels() {
        return locationsService.areaLevels();
    }

    @GetMapping(value = "/areas", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Area> areas(@RequestParam(required = false) String parentId, @RequestParam(required = false) String q,
            @RequestParam(defaultValue = LocationsApi.STATUS_ACTIVE) String status) {
        if (q != null && !q.isBlank()) {
            return locationsService.searchAreas(q, status);
        }
        return locationsService.areaChildren(parentId, status);
    }

    @PostMapping(value = "/areas", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Area> createArea(@RequestBody AreaRequest body, HttpServletRequest request) {
        Area area = locationsService.saveArea(new AreaRequest(null, body.name(), body.code(), body.parentId(), null),
                getSysUserId(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(area);
    }

    @PutMapping(value = "/areas/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Area updateArea(@PathVariable String id, @RequestBody AreaRequest body, HttpServletRequest request) {
        return locationsService.saveArea(
                new AreaRequest(id, body.name(), body.code(), body.parentId(), body.lastupdated()),
                getSysUserId(request));
    }

    @PostMapping(value = "/areas/{id}/active", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Area setAreaActive(@PathVariable String id, @RequestBody Map<String, Boolean> body,
            HttpServletRequest request) {
        return locationsService.setAreaActive(id, Boolean.TRUE.equals(body.get("active")), getSysUserId(request));
    }

    @ExceptionHandler(LocationsValidationException.class)
    public ResponseEntity<Map<String, Object>> refused(LocationsValidationException e) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", e.getMessage());
        body.put("fieldErrors", e.getFieldErrors());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(LocationsConflictException.class)
    public ResponseEntity<Map<String, Object>> conflict(LocationsConflictException e) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", e.getMessage());
        if (e.getCurrent() != null) {
            body.put("current", e.getCurrent());
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(LocationsNotFoundException.class)
    public ResponseEntity<Map<String, Object>> missing(LocationsNotFoundException e) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * An unexpected failure is logged for the administrator; the user is not shown
     * the exception's text (a parser or database message), only that the request
     * could not be completed, in their language.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> failed(RuntimeException e) {
        LogEvent.logError(e);
        Map<String, Object> body = new HashMap<>();
        body.put("error", "");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
