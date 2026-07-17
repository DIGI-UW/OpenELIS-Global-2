package org.openelisglobal.system.controller.rest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class SystemRestController {

    @GetMapping(value = "/server-time", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getServerTime() {
        try {
            Map<String, Object> response = new HashMap<>();

            ZoneId zoneId = ZoneId.systemDefault();
            LocalDate now = LocalDate.now(zoneId);
            LocalTime time = LocalTime.now(zoneId);

            response.put("date", now.format(DateTimeFormatter.ISO_LOCAL_DATE));
            response.put("time", time.format(DateTimeFormatter.ofPattern("HH:mm")));
            response.put("timezone", zoneId.getId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "getServerTime",
                    "Error getting server time: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
