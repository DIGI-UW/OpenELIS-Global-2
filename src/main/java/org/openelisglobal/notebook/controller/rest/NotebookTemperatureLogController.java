package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for notebook entry temperature logging. Handles environmental
 * monitoring data for MNTD and other laboratory workflows.
 *
 * Temperature logs are stored as JSON in the notebook_entry.data field under
 * the key "temperatureLogs".
 */
@RestController
@RequestMapping(value = "/rest/notebook-entry")
public class NotebookTemperatureLogController extends BaseRestController {

    // In-memory storage for temperature logs (in production, this would be
    // persisted to database)
    // This is a simplified implementation - in a full implementation, you would use
    // a proper
    // service and repository layer
    private static final Map<Integer, List<TemperatureLog>> temperatureLogStore = new HashMap<>();

    /**
     * Get temperature logs for a notebook entry. GET
     * /rest/notebook-entry/{entryId}/temperature-logs
     *
     * @param entryId the notebook entry ID
     * @return list of temperature logs
     */
    @GetMapping(value = "/{entryId}/temperature-logs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<TemperatureLog>> getTemperatureLogs(@PathVariable("entryId") Integer entryId) {
        List<TemperatureLog> logs = temperatureLogStore.getOrDefault(entryId, new ArrayList<>());
        // Return logs sorted by date descending (most recent first)
        logs.sort((a, b) -> {
            if (a.getCheckedDateTime() == null && b.getCheckedDateTime() == null) {
                return 0;
            }
            if (a.getCheckedDateTime() == null) {
                return 1;
            }
            if (b.getCheckedDateTime() == null) {
                return -1;
            }
            return b.getCheckedDateTime().compareTo(a.getCheckedDateTime());
        });
        return ResponseEntity.ok(logs);
    }

    /**
     * Log a temperature check for a notebook entry. POST
     * /rest/notebook-entry/{entryId}/temperature-logs
     *
     * @param entryId     the notebook entry ID
     * @param request     the temperature log data
     * @param httpRequest for getting user session
     * @return the created temperature log
     */
    @PostMapping(value = "/{entryId}/temperature-logs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> logTemperature(@PathVariable("entryId") Integer entryId,
            @RequestBody TemperatureLogRequest request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        // Validate required fields
        if (request.getFreezerId() == null || request.getFreezerId().isBlank()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Freezer ID is required");
            return ResponseEntity.badRequest().body(error);
        }

        if (request.getTemperatureValue() == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Temperature value is required");
            return ResponseEntity.badRequest().body(error);
        }

        // Create the temperature log
        TemperatureLog log = new TemperatureLog();
        log.setId(generateLogId(entryId));
        log.setEntryId(entryId);
        log.setFreezerId(request.getFreezerId());
        log.setCheckTime(request.getCheckTime() != null ? request.getCheckTime() : "AM");
        log.setTemperatureValue(request.getTemperatureValue());
        log.setTemperatureUnit(request.getTemperatureUnit() != null ? request.getTemperatureUnit() : "C");
        log.setCheckedBy(request.getCheckedBy());

        // Parse checked date/time
        if (request.getCheckedDateTime() != null && !request.getCheckedDateTime().isBlank()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
                LocalDateTime dateTime = LocalDateTime.parse(request.getCheckedDateTime(), formatter);
                log.setCheckedDateTime(Timestamp.valueOf(dateTime));
            } catch (Exception e) {
                log.setCheckedDateTime(new Timestamp(System.currentTimeMillis()));
            }
        } else {
            log.setCheckedDateTime(new Timestamp(System.currentTimeMillis()));
        }

        log.setNotes(request.getNotes());
        log.setLoggedBy(sysUserId);
        log.setLoggedAt(new Timestamp(System.currentTimeMillis()));

        // Store the log
        temperatureLogStore.computeIfAbsent(entryId, k -> new ArrayList<>()).add(log);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("log", log);

        return ResponseEntity.ok(result);
    }

    private Integer generateLogId(Integer entryId) {
        List<TemperatureLog> logs = temperatureLogStore.getOrDefault(entryId, new ArrayList<>());
        return logs.size() + 1;
    }

    @Override
    protected String getSysUserId(HttpServletRequest request) {
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
        if (usd == null) {
            return null;
        }
        return String.valueOf(usd.getSystemUserId());
    }

    /**
     * Temperature log request DTO.
     */
    public static class TemperatureLogRequest {
        private String freezerId;
        private String checkTime; // AM or PM
        private Double temperatureValue;
        private String temperatureUnit; // C or F
        private String checkedBy;
        private String checkedDateTime;
        private String notes;

        public String getFreezerId() {
            return freezerId;
        }

        public void setFreezerId(String freezerId) {
            this.freezerId = freezerId;
        }

        public String getCheckTime() {
            return checkTime;
        }

        public void setCheckTime(String checkTime) {
            this.checkTime = checkTime;
        }

        public Double getTemperatureValue() {
            return temperatureValue;
        }

        public void setTemperatureValue(Double temperatureValue) {
            this.temperatureValue = temperatureValue;
        }

        public String getTemperatureUnit() {
            return temperatureUnit;
        }

        public void setTemperatureUnit(String temperatureUnit) {
            this.temperatureUnit = temperatureUnit;
        }

        public String getCheckedBy() {
            return checkedBy;
        }

        public void setCheckedBy(String checkedBy) {
            this.checkedBy = checkedBy;
        }

        public String getCheckedDateTime() {
            return checkedDateTime;
        }

        public void setCheckedDateTime(String checkedDateTime) {
            this.checkedDateTime = checkedDateTime;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    /**
     * Temperature log entity.
     */
    public static class TemperatureLog {
        private Integer id;
        private Integer entryId;
        private String freezerId;
        private String checkTime;
        private Double temperatureValue;
        private String temperatureUnit;
        private String checkedBy;
        private Timestamp checkedDateTime;
        private String notes;
        private String loggedBy;
        private Timestamp loggedAt;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer getEntryId() {
            return entryId;
        }

        public void setEntryId(Integer entryId) {
            this.entryId = entryId;
        }

        public String getFreezerId() {
            return freezerId;
        }

        public void setFreezerId(String freezerId) {
            this.freezerId = freezerId;
        }

        public String getCheckTime() {
            return checkTime;
        }

        public void setCheckTime(String checkTime) {
            this.checkTime = checkTime;
        }

        public Double getTemperatureValue() {
            return temperatureValue;
        }

        public void setTemperatureValue(Double temperatureValue) {
            this.temperatureValue = temperatureValue;
        }

        public String getTemperatureUnit() {
            return temperatureUnit;
        }

        public void setTemperatureUnit(String temperatureUnit) {
            this.temperatureUnit = temperatureUnit;
        }

        public String getCheckedBy() {
            return checkedBy;
        }

        public void setCheckedBy(String checkedBy) {
            this.checkedBy = checkedBy;
        }

        public Timestamp getCheckedDateTime() {
            return checkedDateTime;
        }

        public void setCheckedDateTime(Timestamp checkedDateTime) {
            this.checkedDateTime = checkedDateTime;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public String getLoggedBy() {
            return loggedBy;
        }

        public void setLoggedBy(String loggedBy) {
            this.loggedBy = loggedBy;
        }

        public Timestamp getLoggedAt() {
            return loggedAt;
        }

        public void setLoggedAt(Timestamp loggedAt) {
            this.loggedAt = loggedAt;
        }
    }
}
