package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.inventory.service.InventoryStorageLocationService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LocationType;
import org.openelisglobal.inventory.valueholder.InventoryStorageLocation;
import org.openelisglobal.login.valueholder.UserSessionData;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/inventory-storage-locations")
public class InventoryStorageLocationRestController extends BaseRestController {

    @Autowired
    private InventoryStorageLocationService storageLocationService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryStorageLocation>> getAllActive() {
        try {
            List<InventoryStorageLocation> locations = storageLocationService.getAllActive();
            return ResponseEntity.ok(locations);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryStorageLocation> getById(@PathVariable String id) {
        try {
            InventoryStorageLocation location = storageLocationService.get(Long.valueOf(id));
            if (location == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(location);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/type/{locationType}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryStorageLocation>> getByType(@PathVariable LocationType locationType) {
        try {
            List<InventoryStorageLocation> locations = storageLocationService.getByLocationType(locationType);
            return ResponseEntity.ok(locations);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{parentId}/children", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryStorageLocation>> getChildren(@PathVariable String parentId) {
        try {
            List<InventoryStorageLocation> locations = storageLocationService.getChildLocations(Long.valueOf(parentId));
            return ResponseEntity.ok(locations);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/top-level", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryStorageLocation>> getTopLevel() {
        try {
            List<InventoryStorageLocation> locations = storageLocationService.getTopLevelLocations();
            return ResponseEntity.ok(locations);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/code/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryStorageLocation> getByCode(@PathVariable String code) {
        try {
            InventoryStorageLocation location = storageLocationService.getByLocationCode(code);
            if (location == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(location);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}/path", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PathResponse> getPath(@PathVariable String id) {
        try {
            String path = storageLocationService.getLocationPath(Long.valueOf(id));
            return ResponseEntity.ok(new PathResponse(path));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}/has-active-lots", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HasActiveLotsResponse> hasActiveLots(@PathVariable String id) {
        try {
            boolean hasLots = storageLocationService.hasActiveLots(Long.valueOf(id));
            return ResponseEntity.ok(new HasActiveLotsResponse(hasLots));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryStorageLocation> create(@Valid @RequestBody InventoryStorageLocation location,
            HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());
            location.setSysUserId(sysUserId);

            InventoryStorageLocation savedLocation = storageLocationService.save(location);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedLocation);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryStorageLocation> update(@PathVariable String id,
            @Valid @RequestBody InventoryStorageLocation location, HttpServletRequest request) {
        try {
            InventoryStorageLocation existingLocation = storageLocationService.get(Long.valueOf(id));
            if (existingLocation == null) {
                return ResponseEntity.notFound().build();
            }

            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());
            location.setId(Long.valueOf(id));
            location.setSysUserId(sysUserId);

            InventoryStorageLocation updatedLocation = storageLocationService.update(location);
            return ResponseEntity.ok(updatedLocation);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}/deactivate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deactivate(@PathVariable String id, HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());

            storageLocationService.deactivateLocation(Long.valueOf(id), sysUserId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Setter
    @Getter
    public static class PathResponse {
        private String path;

        public PathResponse(String path) {
            this.path = path;
        }

    }

    @Setter
    @Getter
    public static class HasActiveLotsResponse {
        private Boolean hasActiveLots;

        public HasActiveLotsResponse(Boolean hasActiveLots) {
            this.hasActiveLots = hasActiveLots;
        }

    }

    @Setter
    @Getter
    public static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

    }

}
