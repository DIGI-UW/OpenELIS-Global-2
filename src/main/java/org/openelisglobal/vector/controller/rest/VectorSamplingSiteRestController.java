package org.openelisglobal.vector.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.vector.service.VectorSamplingSiteService;
import org.openelisglobal.vector.valueholder.VectorSamplingSite;
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

@RestController
@RequestMapping("/rest/admin/vector/sampling-sites")
public class VectorSamplingSiteRestController {

    @Autowired
    private VectorSamplingSiteService vectorSamplingSiteService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<VectorSamplingSite>> getAllSites(@RequestParam(required = false) String type) {
        try {
            List<VectorSamplingSite> sites;
            if (type != null && !type.isBlank()) {
                sites = vectorSamplingSiteService.getByType(type);
            } else {
                sites = vectorSamplingSiteService.getAll();
            }
            return ResponseEntity.ok(sites);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/active", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<VectorSamplingSite>> getActiveSites() {
        try {
            return ResponseEntity.ok(vectorSamplingSiteService.getActive());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VectorSamplingSite> getSite(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(vectorSamplingSiteService.get(id));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VectorSamplingSite> createSite(@RequestBody VectorSamplingSite site,
            HttpServletRequest request) {
        try {
            site.setSysUserId(ControllerUtills.getSysUserId(request));
            if (site.getSource() == null || site.getSource().isBlank()) {
                site.setSource("LOCAL");
            }
            if (site.getActive() == null) {
                site.setActive(true);
            }
            Integer id = vectorSamplingSiteService.insert(site);
            site.setId(id);
            return ResponseEntity.status(HttpStatus.CREATED).body(site);
        } catch (LIMSRuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VectorSamplingSite> updateSite(@PathVariable Integer id, @RequestBody VectorSamplingSite site,
            HttpServletRequest request) {
        try {
            VectorSamplingSite updated = vectorSamplingSiteService.patchUpdate(id, site,
                    ControllerUtills.getSysUserId(request));
            return ResponseEntity.ok(updated);
        } catch (LIMSRuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
