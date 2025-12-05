package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import javax.validation.Valid;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.inventory.service.StorageLocationService;
import org.openelisglobal.inventory.valueholder.StorageLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/storage-locations")
public class StorageLocationRestController extends BaseRestController {

    @Autowired
    private StorageLocationService storageLocationService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StorageLocation>> getAllStorageLocations() {
        List<StorageLocation> locations = storageLocationService.findAllActive();
        return ResponseEntity.ok(locations);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StorageLocation> getStorageLocationById(@PathVariable String id) {
        StorageLocation location = storageLocationService.get(id);
        if (location == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(location);
    }

    @GetMapping(value = "/by-code/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StorageLocation> getStorageLocationByCode(@PathVariable String code) {
        StorageLocation location = storageLocationService.findByLocationCode(code);
        if (location == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(location);
    }

    @GetMapping(value = "/by-parent/{parentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StorageLocation>> getStorageLocationsByParent(@PathVariable String parentId) {
        List<StorageLocation> locations = storageLocationService.findByParentLocationId(parentId);
        return ResponseEntity.ok(locations);
    }

    @GetMapping(value = "/roots", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StorageLocation>> getRootStorageLocations() {
        List<StorageLocation> locations = storageLocationService.findRootLocations();
        return ResponseEntity.ok(locations);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StorageLocation> createStorageLocation(HttpServletRequest httpRequest,
            @Valid @RequestBody StorageLocation location) {
        location.setSysUserId(getSysUserId(httpRequest));
        String id = storageLocationService.insert(location);
        StorageLocation created = storageLocationService.get(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StorageLocation> updateStorageLocation(HttpServletRequest httpRequest,
            @PathVariable String id, @Valid @RequestBody StorageLocation location) {
        StorageLocation existing = storageLocationService.get(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        location.setId(id);
        location.setSysUserId(getSysUserId(httpRequest));
        storageLocationService.update(location);
        StorageLocation updated = storageLocationService.get(id);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> deleteStorageLocation(HttpServletRequest httpRequest, @PathVariable String id) {
        StorageLocation location = storageLocationService.get(id);
        if (location == null) {
            return ResponseEntity.notFound().build();
        }
        location.setIsActive(false);
        location.setSysUserId(getSysUserId(httpRequest));
        storageLocationService.update(location);
        return ResponseEntity.noContent().build();
    }
}
