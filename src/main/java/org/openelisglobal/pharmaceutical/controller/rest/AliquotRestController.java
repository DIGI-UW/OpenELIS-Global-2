package org.openelisglobal.pharmaceutical.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.pharmaceutical.service.AliquotService;
import org.openelisglobal.pharmaceutical.valueholder.Aliquot;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/pharmaceutical/aliquots")
public class AliquotRestController extends BaseRestController {

    @Autowired
    private AliquotService aliquotService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Aliquot>> getAll() {
        try {
            List<Aliquot> aliquots = aliquotService.getAll();
            return ResponseEntity.ok(aliquots);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Aliquot> getById(@PathVariable Integer id) {
        try {
            Aliquot aliquot = aliquotService.get(id);
            if (aliquot == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(aliquot);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/barcode/{barcode}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Aliquot> getByBarcode(@PathVariable String barcode) {
        try {
            Aliquot aliquot = aliquotService.findByBarcode(barcode);
            if (aliquot == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(aliquot);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/sample/{sampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Aliquot>> getBySampleId(@PathVariable Integer sampleId) {
        try {
            List<Aliquot> aliquots = aliquotService.findByParentSampleId(sampleId);
            return ResponseEntity.ok(aliquots);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/sample/{sampleId}/available", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Aliquot>> getAvailableBySampleId(@PathVariable Integer sampleId) {
        try {
            List<Aliquot> aliquots = aliquotService.findAvailableByParentSample(sampleId);
            return ResponseEntity.ok(aliquots);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/status/{status}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Aliquot>> getByStatus(@PathVariable Aliquot.AliquotStatus status) {
        try {
            List<Aliquot> aliquots = aliquotService.findByStatus(status);
            return ResponseEntity.ok(aliquots);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/storage/{storageLocationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Aliquot>> getByStorageLocation(@PathVariable Integer storageLocationId) {
        try {
            List<Aliquot> aliquots = aliquotService.findByStorageLocation(storageLocationId);
            return ResponseEntity.ok(aliquots);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/freeze-thaw-exceeded", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Aliquot>> getFreezeThawExceeded() {
        try {
            List<Aliquot> aliquots = aliquotService.findExceedingFreezeThawLimit();
            return ResponseEntity.ok(aliquots);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}/freeze-thaw-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FreezeThawStatusResponse> getFreezeThawStatus(@PathVariable Integer id) {
        try {
            Aliquot aliquot = aliquotService.get(id);
            if (aliquot == null) {
                return ResponseEntity.notFound().build();
            }
            boolean exceeded = aliquotService.isFreezeThawLimitExceeded(id);
            return ResponseEntity.ok(new FreezeThawStatusResponse(
                    aliquot.getFreezeThawCount(),
                    aliquot.getFreezeThawLimit(),
                    exceeded));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/sample/{sampleId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Aliquot> createAliquot(
            @PathVariable Integer sampleId,
            @Valid @RequestBody Aliquot aliquot,
            HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String userId = String.valueOf(usd.getSystemUserId());

            Aliquot createdAliquot = aliquotService.createAliquot(sampleId, aliquot, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdAliquot);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/{id}/freeze-thaw", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Aliquot> recordFreezeThaw(
            @PathVariable Integer id,
            HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String userId = String.valueOf(usd.getSystemUserId());

            Aliquot updatedAliquot = aliquotService.recordFreezeThaw(id, userId);
            return ResponseEntity.ok(updatedAliquot);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Aliquot> updateStatus(
            @PathVariable Integer id,
            @RequestParam Aliquot.AliquotStatus status,
            HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String userId = String.valueOf(usd.getSystemUserId());

            Aliquot updatedAliquot = aliquotService.updateStatus(id, status, userId);
            return ResponseEntity.ok(updatedAliquot);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        try {
            aliquotService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public static class FreezeThawStatusResponse {
        private Integer currentCount;
        private Integer limit;
        private Boolean exceeded;

        public FreezeThawStatusResponse(Integer currentCount, Integer limit, Boolean exceeded) {
            this.currentCount = currentCount;
            this.limit = limit;
            this.exceeded = exceeded;
        }

        public Integer getCurrentCount() {
            return currentCount;
        }

        public Integer getLimit() {
            return limit;
        }

        public Boolean getExceeded() {
            return exceeded;
        }
    }
}
