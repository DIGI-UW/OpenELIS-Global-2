package org.openelisglobal.calendar.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.calendar.service.PublicHolidayService;
import org.openelisglobal.calendar.service.WeekendConfigService;
import org.openelisglobal.calendar.valueholder.PublicHoliday;
import org.openelisglobal.common.rest.BaseRestController;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/rest/calendar")
public class CalendarManagementRestController extends BaseRestController {

    @Autowired
    private PublicHolidayService publicHolidayService;

    @Autowired
    private WeekendConfigService weekendConfigService;

    // ========== Holiday Endpoints ==========

    @GetMapping("/holidays")
    public ResponseEntity<Map<String, Object>> getHolidays(@RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        int targetYear = year != null ? year : Calendar.getInstance().get(Calendar.YEAR);
        List<PublicHoliday> holidays = publicHolidayService.getHolidaysForYear(targetYear, includeInactive);
        Set<Integer> weekendDays = weekendConfigService.getWeekendDayNumbers().stream().collect(Collectors.toSet());

        List<Map<String, Object>> holidayList = holidays.stream().map(h -> toHolidayResponse(h, weekendDays))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("year", targetYear);
        response.put("holidays", holidayList);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/holidays")
    public ResponseEntity<Map<String, Object>> createHoliday(@RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        PublicHoliday holiday = parseHolidayFromBody(body);
        Integer sysUserId = Integer.valueOf(getSysUserId(request));
        try {
            PublicHoliday created = publicHolidayService.create(holiday, sysUserId);
            Set<Integer> weekendDays = weekendConfigService.getWeekendDayNumbers().stream().collect(Collectors.toSet());
            return ResponseEntity.status(HttpStatus.CREATED).body(toHolidayResponse(created, weekendDays));
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
            }
            throw e;
        }
    }

    @PutMapping("/holidays/{id}")
    public ResponseEntity<Map<String, Object>> updateHoliday(@PathVariable Integer id,
            @RequestBody Map<String, Object> body, HttpServletRequest request) {
        PublicHoliday existing = publicHolidayService.getById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        updateHolidayFromBody(existing, body);
        Integer sysUserId = Integer.valueOf(getSysUserId(request));
        try {
            PublicHoliday updated = publicHolidayService.update(existing, sysUserId);
            Set<Integer> weekendDays = weekendConfigService.getWeekendDayNumbers().stream().collect(Collectors.toSet());
            return ResponseEntity.ok(toHolidayResponse(updated, weekendDays));
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
            }
            throw e;
        }
    }

    @DeleteMapping("/holidays/{id}")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Integer id) {
        PublicHoliday existing = publicHolidayService.getById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        publicHolidayService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ========== CSV Import/Export ==========

    @PostMapping(value = "/holidays/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importHolidays(@RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Integer year, HttpServletRequest request) throws IOException {
        int targetYear = year != null ? year : Calendar.getInstance().get(Calendar.YEAR);
        Integer sysUserId = Integer.valueOf(getSysUserId(request));

        List<PublicHoliday> parsed = parseCsvFile(file);
        PublicHolidayService.ImportResult result = publicHolidayService.importHolidays(parsed, targetYear, sysUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("imported", result.imported());
        response.put("skipped", result.skipped());
        response.put("errors", result.errors().stream().map(e -> Map.of("row", e.row(), "reason", e.reason()))
                .collect(Collectors.toList()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/holidays/export")
    public void exportHolidays(@RequestParam(required = false) Integer year, HttpServletResponse response)
            throws IOException {
        int targetYear = year != null ? year : Calendar.getInstance().get(Calendar.YEAR);
        List<PublicHoliday> holidays = publicHolidayService.getHolidaysForYear(targetYear, true);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"holidays-" + targetYear + ".csv\"");
        PrintWriter writer = response.getWriter();
        writer.println("date,name,recurring,active");
        for (PublicHoliday h : holidays) {
            writer.printf("%s,%s,%s,%s%n", h.getHolidayDate().toString(), escapeCsv(h.getHolidayName()),
                    h.getIsRecurring(), h.getIsActive());
        }
        writer.flush();
    }

    // ========== Weekend Config Endpoints ==========

    @GetMapping("/weekends")
    public ResponseEntity<Map<String, Object>> getWeekends() {
        List<Integer> weekendDays = weekendConfigService.getWeekendDayNumbers();
        return ResponseEntity.ok(Map.of("weekendDays", weekendDays));
    }

    @PutMapping("/weekends")
    public ResponseEntity<Map<String, Object>> updateWeekends(@RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        List<Integer> weekendDays = (List<Integer>) body.get("weekendDays");
        if (weekendDays == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "weekendDays is required"));
        }
        // Validate range 0-6
        for (Integer day : weekendDays) {
            if (day < 0 || day > 6) {
                return ResponseEntity.badRequest().body(Map.of("error", "Day of week must be 0-6, got: " + day));
            }
        }
        Integer sysUserId = Integer.valueOf(getSysUserId(request));
        weekendConfigService.updateWeekendDays(weekendDays, sysUserId);
        return ResponseEntity.ok(Map.of("weekendDays", weekendDays));
    }

    // ========== Helpers ==========

    private Map<String, Object> toHolidayResponse(PublicHoliday h, Set<Integer> weekendDays) {
        LocalDate localDate = h.getHolidayDate().toLocalDate();
        DayOfWeek dow = localDate.getDayOfWeek();
        // Convert Java DayOfWeek (1=Mon..7=Sun) to our convention (0=Sun..6=Sat)
        int dayNum = dow.getValue() % 7;

        Map<String, Object> map = new HashMap<>();
        map.put("id", h.getId());
        map.put("date", h.getHolidayDate().toString());
        map.put("name", h.getHolidayName());
        map.put("isRecurring", h.getIsRecurring());
        map.put("isActive", h.getIsActive());
        map.put("dayOfWeek", dow.name().substring(0, 1) + dow.name().substring(1, 3).toLowerCase());
        map.put("isWeekendDay", weekendDays.contains(dayNum));
        map.put("lastUpdated", h.getLastupdated() != null ? h.getLastupdated().toString() : null);
        return map;
    }

    private PublicHoliday parseHolidayFromBody(Map<String, Object> body) {
        PublicHoliday holiday = new PublicHoliday();
        holiday.setHolidayDate(Date.valueOf((String) body.get("date")));
        holiday.setHolidayName((String) body.get("name"));
        holiday.setIsRecurring(body.get("isRecurring") != null && (Boolean) body.get("isRecurring"));
        return holiday;
    }

    private void updateHolidayFromBody(PublicHoliday holiday, Map<String, Object> body) {
        if (body.containsKey("date")) {
            holiday.setHolidayDate(Date.valueOf((String) body.get("date")));
        }
        if (body.containsKey("name")) {
            holiday.setHolidayName((String) body.get("name"));
        }
        if (body.containsKey("isRecurring")) {
            holiday.setIsRecurring((Boolean) body.get("isRecurring"));
        }
        if (body.containsKey("isActive")) {
            holiday.setIsActive((Boolean) body.get("isActive"));
        }
    }

    private List<PublicHoliday> parseCsvFile(MultipartFile file) throws IOException {
        List<PublicHoliday> holidays = new ArrayList<>();
        try (var reader = new java.io.BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue; // skip header row
                }
                line = line.trim();
                if (line.isEmpty())
                    continue;

                String[] parts = line.split(",", -1);
                if (parts.length < 2)
                    continue;

                PublicHoliday holiday = new PublicHoliday();
                try {
                    holiday.setHolidayDate(Date.valueOf(parts[0].trim()));
                } catch (DateTimeParseException | IllegalArgumentException e) {
                    // Will be caught by import validation
                    continue;
                }
                holiday.setHolidayName(parts[1].trim().replaceAll("^\"|\"$", ""));
                if (parts.length > 2) {
                    holiday.setIsRecurring(Boolean.parseBoolean(parts[2].trim()));
                }
                holidays.add(holiday);
            }
        }
        return holidays;
    }

    private String escapeCsv(String value) {
        if (value == null)
            return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
