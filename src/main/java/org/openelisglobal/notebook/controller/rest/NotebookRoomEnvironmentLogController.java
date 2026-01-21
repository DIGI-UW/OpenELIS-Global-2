package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.service.NotebookEntryRoomEnvironmentLogService;
import org.openelisglobal.notebook.valueholder.NotebookEntryRoomEnvironmentLog;
import org.springframework.beans.factory.annotation.Autowired;
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
 * REST controller for notebook entry room environment logging. Handles O2 and
 * humidity monitoring data for biorepository and other laboratory workflows.
 *
 * Room environment logs are persisted to the
 * notebook_entry_room_environment_log table.
 */
@RestController
@RequestMapping(value = "/rest/notebook-entry")
public class NotebookRoomEnvironmentLogController extends BaseRestController {

    @Autowired
    private NotebookEntryRoomEnvironmentLogService roomEnvironmentLogService;

    /**
     * Get room environment logs for a notebook entry. GET
     * /rest/notebook-entry/{entryId}/room-environment-logs
     *
     * @param entryId the notebook entry ID
     * @return list of room environment logs
     */
    @GetMapping(value = "/{entryId}/room-environment-logs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<RoomEnvironmentLogDTO>> getRoomEnvironmentLogs(
            @PathVariable("entryId") Integer entryId) {
        List<NotebookEntryRoomEnvironmentLog> logs = roomEnvironmentLogService.findByEntryId(entryId);
        // Convert to DTOs to avoid lazy loading issues
        List<RoomEnvironmentLogDTO> dtos = logs.stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private RoomEnvironmentLogDTO toDTO(NotebookEntryRoomEnvironmentLog log) {
        RoomEnvironmentLogDTO dto = new RoomEnvironmentLogDTO();
        dto.setId(log.getId());
        dto.setEntryId(log.getNotebookEntry() != null ? log.getNotebookEntry().getId() : null);
        dto.setRoomId(log.getRoomId());
        dto.setRoomName(log.getRoomName());
        dto.setOxygenLevel(log.getOxygenLevel());
        dto.setHumidity(log.getHumidity());
        dto.setCheckedBy(log.getCheckedBy());
        dto.setCheckedDateTime(log.getCheckedDateTime());
        dto.setNotes(log.getNotes());
        dto.setLoggedBy(log.getLoggedBy());
        dto.setLoggedAt(log.getLoggedAt());
        return dto;
    }

    /**
     * Log a room environment check for a notebook entry. POST
     * /rest/notebook-entry/{entryId}/room-environment-logs
     *
     * @param entryId     the notebook entry ID
     * @param request     the room environment log data
     * @param httpRequest for getting user session
     * @return the created room environment log
     */
    @PostMapping(value = "/{entryId}/room-environment-logs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> logRoomEnvironment(@PathVariable("entryId") Integer entryId,
            @RequestBody RoomEnvironmentLogRequest request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        // Validate - at least one measurement required
        if (request.getOxygenLevel() == null && request.getHumidity() == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "At least one measurement (O2 level or humidity) is required");
            return ResponseEntity.badRequest().body(error);
        }

        // Parse checked date/time
        Timestamp checkedDateTime;
        if (request.getCheckedDateTime() != null && !request.getCheckedDateTime().isBlank()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
                LocalDateTime dateTime = LocalDateTime.parse(request.getCheckedDateTime(), formatter);
                checkedDateTime = Timestamp.valueOf(dateTime);
            } catch (Exception e) {
                checkedDateTime = new Timestamp(System.currentTimeMillis());
            }
        } else {
            checkedDateTime = new Timestamp(System.currentTimeMillis());
        }

        try {
            // Create the room environment log using the service
            NotebookEntryRoomEnvironmentLog log = roomEnvironmentLogService.logRoomEnvironment(entryId,
                    request.getRoomId(), request.getRoomName(), request.getOxygenLevel(), request.getHumidity(),
                    request.getCheckedBy(), checkedDateTime, request.getNotes(), sysUserId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("log", toDTO(log));

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to save room environment log: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
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
     * Room environment log request DTO.
     */
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class RoomEnvironmentLogRequest {
        private String roomId;
        private String roomName;
        private Double oxygenLevel; // Percentage
        private Double humidity; // Percentage
        private String checkedBy;
        private String checkedDateTime;
        private String notes;

        public String getRoomId() {
            return roomId;
        }

        public void setRoomId(String roomId) {
            this.roomId = roomId;
        }

        public String getRoomName() {
            return roomName;
        }

        public void setRoomName(String roomName) {
            this.roomName = roomName;
        }

        public Double getOxygenLevel() {
            return oxygenLevel;
        }

        public void setOxygenLevel(Double oxygenLevel) {
            this.oxygenLevel = oxygenLevel;
        }

        public Double getHumidity() {
            return humidity;
        }

        public void setHumidity(Double humidity) {
            this.humidity = humidity;
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
     * Room environment log DTO for API responses.
     */
    public static class RoomEnvironmentLogDTO {
        private Integer id;
        private Integer entryId;
        private String roomId;
        private String roomName;
        private Double oxygenLevel;
        private Double humidity;
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

        public String getRoomId() {
            return roomId;
        }

        public void setRoomId(String roomId) {
            this.roomId = roomId;
        }

        public String getRoomName() {
            return roomName;
        }

        public void setRoomName(String roomName) {
            this.roomName = roomName;
        }

        public Double getOxygenLevel() {
            return oxygenLevel;
        }

        public void setOxygenLevel(Double oxygenLevel) {
            this.oxygenLevel = oxygenLevel;
        }

        public Double getHumidity() {
            return humidity;
        }

        public void setHumidity(Double humidity) {
            this.humidity = humidity;
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
