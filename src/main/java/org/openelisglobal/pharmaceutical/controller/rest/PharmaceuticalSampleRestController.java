package org.openelisglobal.pharmaceutical.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.pharmaceutical.service.PharmaceuticalSampleService;
import org.openelisglobal.pharmaceutical.valueholder.PharmaceuticalSample;
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
@RequestMapping("/rest/pharmaceutical/samples")
public class PharmaceuticalSampleRestController extends BaseRestController {

    @Autowired
    private PharmaceuticalSampleService pharmaceuticalSampleService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PharmaceuticalSample>> getAll() {
        try {
            List<PharmaceuticalSample> samples = pharmaceuticalSampleService.getAll();
            return ResponseEntity.ok(samples);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PharmaceuticalSample> getById(@PathVariable Integer id) {
        try {
            PharmaceuticalSample sample = pharmaceuticalSampleService.get(id);
            if (sample == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(sample);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}/details", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getSampleWithDetails(@PathVariable Integer id) {
        try {
            Map<String, Object> details = pharmaceuticalSampleService.getSampleWithDetails(id);
            if (details == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/barcode/{barcode}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PharmaceuticalSample> getByBarcode(@PathVariable String barcode) {
        try {
            PharmaceuticalSample sample = pharmaceuticalSampleService.findByBarcode(barcode);
            if (sample == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(sample);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/unique-id/{uniqueSampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PharmaceuticalSample> getByUniqueSampleId(@PathVariable String uniqueSampleId) {
        try {
            PharmaceuticalSample sample = pharmaceuticalSampleService.findByUniqueSampleId(uniqueSampleId);
            if (sample == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(sample);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/status/{status}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PharmaceuticalSample>> getByStatus(
            @PathVariable PharmaceuticalSample.SampleStatus status) {
        try {
            List<PharmaceuticalSample> samples = pharmaceuticalSampleService.findByStatus(status);
            return ResponseEntity.ok(samples);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/lab-type/{labType}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PharmaceuticalSample>> getByLabType(
            @PathVariable PharmaceuticalSample.LabType labType) {
        try {
            List<PharmaceuticalSample> samples = pharmaceuticalSampleService.findByLabType(labType);
            return ResponseEntity.ok(samples);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/expiring", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PharmaceuticalSample>> getExpiringSoon(
            @RequestParam(defaultValue = "30") int daysAhead) {
        try {
            List<PharmaceuticalSample> samples = pharmaceuticalSampleService.findExpiringSoon(daysAhead);
            return ResponseEntity.ok(samples);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PharmaceuticalSample>> search(@RequestParam String query) {
        try {
            List<PharmaceuticalSample> samples = pharmaceuticalSampleService.searchByName(query);
            return ResponseEntity.ok(samples);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PharmaceuticalSample> registerSample(
            @Valid @RequestBody PharmaceuticalSample sample,
            HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String userId = String.valueOf(usd.getSystemUserId());

            PharmaceuticalSample registeredSample = pharmaceuticalSampleService.registerSample(sample, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(registeredSample);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PharmaceuticalSample> update(
            @PathVariable Integer id,
            @Valid @RequestBody PharmaceuticalSample sample,
            HttpServletRequest request) {
        try {
            PharmaceuticalSample existingSample = pharmaceuticalSampleService.get(id);
            if (existingSample == null) {
                return ResponseEntity.notFound().build();
            }

            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String userId = String.valueOf(usd.getSystemUserId());

            sample.setId(id);
            sample.setSysUserId(userId);
            PharmaceuticalSample updatedSample = pharmaceuticalSampleService.update(sample);
            return ResponseEntity.ok(updatedSample);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PharmaceuticalSample> updateStatus(
            @PathVariable Integer id,
            @RequestParam PharmaceuticalSample.SampleStatus status,
            HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String userId = String.valueOf(usd.getSystemUserId());

            PharmaceuticalSample updatedSample = pharmaceuticalSampleService.updateStatus(id, status, userId);
            return ResponseEntity.ok(updatedSample);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        try {
            pharmaceuticalSampleService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
