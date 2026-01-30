package org.openelisglobal.odoo.controller;

import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.odoo.client.OdooConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SuppressWarnings("unused")
public class HealthCheckController {

    private final OdooConnection odooConnection;

    public HealthCheckController(OdooConnection odooConnection) {
        this.odooConnection = odooConnection;
    }

    @GetMapping("/health/odoo")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> odooHealth() {
        Map<String, Object> health = new HashMap<>();

        if (odooConnection.isAvailable()) {
            health.put("status", "UP");
            health.put("odoo", "Available");
            return ResponseEntity.ok(health);
        } else {
            health.put("status", "DOWN");
            health.put("odoo", "Unavailable");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }
}
