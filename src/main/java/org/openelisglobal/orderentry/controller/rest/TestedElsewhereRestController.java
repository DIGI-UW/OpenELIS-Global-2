package org.openelisglobal.orderentry.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.orderentry.service.OrderEntryRequestRefusedException;
import org.openelisglobal.orderentry.service.TestedElsewhereService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mark tested elsewhere on a test of an order (FRS clinical order entry v4,
 * FR-B20): the performing laboratory and the reported value. A refused request
 * answers 400 with the reason, never a silent no-op.
 */
@RestController
@RequestMapping("/rest/order-tests/tested-elsewhere")
public class TestedElsewhereRestController extends BaseRestController {

    @Autowired
    private TestedElsewhereService testedElsewhereService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> forOrder(@RequestParam String labNumber) {
        try {
            List<Map<String, Object>> marks = testedElsewhereService.forOrder(labNumber);
            return ResponseEntity.ok(marks);
        } catch (OrderEntryRequestRefusedException e) {
            return refused(e);
        }
    }

    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> mark(HttpServletRequest request, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(testedElsewhereService.mark(body.get("labNumber"), body.get("testId"),
                    body.get("performingLabId"), body.get("reportedValue"), getSysUserId(request)));
        } catch (OrderEntryRequestRefusedException e) {
            return refused(e);
        }
    }

    @DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> unmark(@RequestParam String labNumber, @RequestParam String testId) {
        try {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("removed", testedElsewhereService.unmark(labNumber, testId));
            return ResponseEntity.ok(response);
        } catch (OrderEntryRequestRefusedException e) {
            return refused(e);
        }
    }

    private static ResponseEntity<Map<String, Object>> refused(OrderEntryRequestRefusedException e) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }
}
