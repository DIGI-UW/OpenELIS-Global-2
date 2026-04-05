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
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import org.springframework.web.server.ResponseStatusException;

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
            @RequestParam(defaultValue = "false") boolean includeInactive, HttpServletRequest request) {
        requireAuthenticatedUser(request);
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

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/holidays")
    public ResponseEntity<Map<String, Object>> createHoliday(@RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Integer sysUserId = requireAuthenticatedUser(request);
        PublicHoliday holiday = parseHolidayFromBody(body);
        try {
            PublicHoliday created = publicHolidayService.create(holiday, sysUserId);
            Set<Integer> weekendDays = weekendConfigService.getWeekendDayNumbers().stream().collect(Collectors.toSet());
            return ResponseEntity.status(HttpStatus.CREATED).body(toHolidayResponse(created, weekendDays));
        } catch (LIMSRuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
            }
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/holidays/{id}")
    public ResponseEntity<Map<String, Object>> updateHoliday(@PathVariable Integer id,
            @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Integer sysUserId = requireAuthenticatedUser(request);
        PublicHoliday existing = publicHolidayService.getById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        updateHolidayFromBody(existing, body);
        try {
            PublicHoliday updated = publicHolidayService.update(existing, sysUserId);
            Set<Integer> weekendDays = weekendConfigService.getWeekendDayNumbers().stream().collect(Collectors.toSet());
            return ResponseEntity.ok(toHolidayResponse(updated, weekendDays));
        } catch (LIMSRuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
            }
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/holidays/{id}")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Integer id, HttpServletRequest request) {
        requireAuthenticatedUser(request);
        PublicHoliday existing = publicHolidayService.getById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        publicHolidayService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ========== CSV Import/Export ==========

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/holidays/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importHolidays(@RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Integer year, HttpServletRequest request) throws IOException {
        int targetYear = year != null ? year : Calendar.getInstance().get(Calendar.YEAR);
        Integer sysUserId = requireAuthenticatedUser(request);

        if (file.getSize() > 1_000_000) {
            return ResponseEntity.badRequest().body(Map.of("error", "File too large (max 1MB)"));
        }

        CsvParseResult parsed = parseCsvFile(file);
        PublicHolidayService.ImportResult result = publicHolidayService.importHolidays(parsed.holidays(), targetYear,
                sysUserId);

        // Merge parse errors with import errors for complete reporting
        List<Map<String, Object>> allErrors = new ArrayList<>();
        parsed.parseErrors().stream().map(e -> Map.<String, Object>of("row", e.row(), "reason", e.reason()))
                .forEach(allErrors::add);
        result.errors().stream().map(e -> Map.<String, Object>of("row", e.row(), "reason", e.reason()))
                .forEach(allErrors::add);

        Map<String, Object> response = new HashMap<>();
        response.put("imported", result.imported());
        response.put("skipped", result.skipped() + parsed.parseErrors().size());
        response.put("errors", allErrors);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/holidays/export")
    public void exportHolidays(@RequestParam(required = false) Integer year, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        requireAuthenticatedUser(request);
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
    public ResponseEntity<Map<String, Object>> getWeekends(HttpServletRequest request) {
        requireAuthenticatedUser(request);
        List<Integer> weekendDays = weekendConfigService.getWeekendDayNumbers();
        return ResponseEntity.ok(Map.of("weekendDays", weekendDays));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/weekends")
    public ResponseEntity<Map<String, Object>> updateWeekends(@RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Integer sysUserId = requireAuthenticatedUser(request);
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
        String dateStr = (String) body.get("date");
        String name = (String) body.get("name");
        if (dateStr == null || dateStr.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date is required");
        }
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        try {
            holiday.setHolidayDate(Date.valueOf(dateStr));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format, expected yyyy-MM-dd");
        }
        holiday.setHolidayName(name.trim());
        holiday.setIsRecurring(Boolean.TRUE.equals(body.get("isRecurring")));
        return holiday;
    }

    private void updateHolidayFromBody(PublicHoliday holiday, Map<String, Object> body) {
        if (body.containsKey("date")) {
            String dateStr = (String) body.get("date");
            if (dateStr == null || dateStr.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date must not be blank");
            }
            try {
                holiday.setHolidayDate(Date.valueOf(dateStr));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format, expected yyyy-MM-dd");
            }
        }
        if (body.containsKey("name")) {
            String name = (String) body.get("name");
            if (name == null || name.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name must not be blank");
            }
            holiday.setHolidayName(name.trim());
        }
        if (body.containsKey("isRecurring")) {
            holiday.setIsRecurring((Boolean) body.get("isRecurring"));
        }
        if (body.containsKey("isActive")) {
            holiday.setIsActive((Boolean) body.get("isActive"));
        }
    }

    private record CsvParseResult(List<PublicHoliday> holidays, List<PublicHolidayService.ImportError> parseErrors) {
    }

    private CsvParseResult parseCsvFile(MultipartFile file) throws IOException {
        List<PublicHoliday> holidays = new ArrayList<>();
        List<PublicHolidayService.ImportError> parseErrors = new ArrayList<>();
        try (var reader = new java.io.BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            int rowNum = 0;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (header) {
                    header = false;
                    continue; // skip header row
                }
                line = line.trim();
                if (line.isEmpty())
                    continue;

                // Handle quoted CSV fields (commas inside quotes)
                List<String> parts = new ArrayList<>();
                boolean inQuotes = false;
                StringBuilder field = new StringBuilder();
                for (char c : line.toCharArray()) {
                    if (c == '"') {
                        inQuotes = !inQuotes;
                    } else if (c == ',' && !inQuotes) {
                        parts.add(field.toString());
                        field.setLength(0);
                    } else {
                        field.append(c);
                    }
                }
                parts.add(field.toString());
                if (parts.size() < 2) {
                    parseErrors.add(new PublicHolidayService.ImportError(rowNum, "Too few columns (need date,name)"));
                    continue;
                }

                PublicHoliday holiday = new PublicHoliday();
                try {
                    holiday.setHolidayDate(Date.valueOf(parts.get(0).trim()));
                } catch (DateTimeParseException | IllegalArgumentException e) {
                    parseErrors.add(new PublicHolidayService.ImportError(rowNum,
                            "Invalid date format: " + parts.get(0).trim()));
                    continue;
                }
                holiday.setHolidayName(parts.get(1).trim());
                if (parts.size() > 2) {
                    holiday.setIsRecurring(Boolean.parseBoolean(parts.get(2).trim()));
                }
                holidays.add(holiday);
            }
        }
        return new CsvParseResult(holidays, parseErrors);
    }

    /** CSV formula injection protection + quoting */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // Prevent CSV formula injection (=, +, -, @, \t, \r)
        if (!value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0) {
            value = "'" + value;
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /** Verify user is authenticated, return user ID or throw 401 */
    private Integer requireAuthenticatedUser(HttpServletRequest request) {
        String userId = getSysUserId(request);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return Integer.valueOf(userId);
    }
}
