package org.openelisglobal.lesotho.clinicalinfo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.lesotho.clinicalinfo.service.OrderClinicalInfoService;
import org.openelisglobal.lesotho.clinicalinfo.service.OrderClinicalInfoService.SaveResult;
import org.openelisglobal.lesotho.clinicalinfo.valueholder.OrderClinicalInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * LESOTHO: reads and saves the Clinical Information section of Add Order. The
 * order is saved first through /rest/SamplePatientEntry; the page then posts
 * this section for the saved order's lab number, the same way it uploads the
 * order's attachments.
 */
@Controller
@RequestMapping(value = "/rest/lesotho/")
public class OrderClinicalInfoRestController extends BaseRestController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private OrderClinicalInfoService clinicalInfoService;

    @GetMapping(value = "order-clinical-information/{accessionNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> get(@PathVariable String accessionNumber) {
        Optional<OrderClinicalInfo> latest = clinicalInfoService.getLatest(accessionNumber);
        Map<String, Object> body = new HashMap<>();
        body.put("data", null);
        if (latest.isPresent()) {
            try {
                body.put("data", MAPPER.readTree(latest.get().getClinicalData()));
                body.put("lastUpdated", Objects.toString(latest.get().getLastupdated(), null));
            } catch (Exception e) {
                LogEvent.logError(e);
            }
        }
        return ResponseEntity.ok(body);
    }

    @PostMapping(value = "order-clinical-information/{accessionNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> save(HttpServletRequest request, @PathVariable String accessionNumber,
            @RequestBody JsonNode clinicalInformation) {
        try {
            SaveResult result = clinicalInfoService.save(accessionNumber,
                    MAPPER.writeValueAsString(clinicalInformation), getSysUserId(request));
            Map<String, Object> body = new HashMap<>();
            body.put("status", result.changed() ? "saved" : "unchanged");
            body.put("lastUpdated", Objects.toString(result.info().getLastupdated(), null));
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to save clinical information"));
        }
    }
}
