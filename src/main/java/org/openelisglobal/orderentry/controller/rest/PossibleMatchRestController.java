package org.openelisglobal.orderentry.controller.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.orderentry.service.OrderEntryRequestRefusedException;
import org.openelisglobal.orderentry.service.PossibleMatchScorer.Scored;
import org.openelisglobal.orderentry.service.PossibleMatchService;
import org.openelisglobal.orderentry.valueholder.PossibleMatchCandidate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The possible-match check behind Create on a new patient, facility or provider
 * in order entry (FRS clinical order entry v4, FR-B6a, D-216): up to five
 * existing records that look like the one being created, each with the fields
 * it matched on, and the record of Create new anyway.
 */
@RestController
@RequestMapping("/rest/possible-matches")
public class PossibleMatchRestController extends BaseRestController {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private PossibleMatchService possibleMatchService;

    @GetMapping(value = "/patient", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> patients(@RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName, @RequestParam(required = false) String birthDate,
            @RequestParam(required = false) String identifier) {
        return matches(PossibleMatchService.PATIENT,
                possibleMatchService.patients(firstName, lastName, parseBirthDate(birthDate), identifier));
    }

    @GetMapping(value = "/facility", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> facilities(@RequestParam(required = false) String name,
            @RequestParam(required = false) String code) {
        return matches(PossibleMatchService.FACILITY, possibleMatchService.facilities(name, code));
    }

    @GetMapping(value = "/provider", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> providers(@RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName) {
        return matches(PossibleMatchService.PROVIDER, possibleMatchService.providers(firstName, lastName));
    }

    /**
     * Records Create new anyway. The body carries {@code entered} (what the user
     * typed) and {@code matches} (the possible matches the user saw).
     */
    @PostMapping(value = "/{kind}/override", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> recordOverride(HttpServletRequest request, @PathVariable String kind,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            Integer id = possibleMatchService.recordOverride(kind, toJson(body.get("entered")),
                    toJson(body.get("matches")), getSysUserId(request));
            response.put("id", id);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (OrderEntryRequestRefusedException | JsonProcessingException e) {
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    static LocalDate parseBirthDate(String birthDate) {
        if (GenericValidator.isBlankOrNull(birthDate)) {
            return null;
        }
        String value = birthDate.trim();
        try {
            return LocalDate.parse(value.length() > 10 ? value.substring(0, 10) : value);
        } catch (DateTimeParseException e) {
            try {
                java.sql.Date parsed = DateUtil.convertStringDateToSqlDate(value);
                return parsed == null ? null : parsed.toLocalDate();
            } catch (RuntimeException notADate) {
                return null;
            }
        }
    }

    private static Map<String, Object> matches(String kind, List<Scored> scored) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Scored match : scored) {
            PossibleMatchCandidate candidate = match.getCandidate();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", candidate.getId());
            row.put("kind", kind);
            row.put("matchedOn", match.getMatchedOn());
            if (PossibleMatchService.FACILITY.equals(kind)) {
                row.put("name", candidate.getName());
                row.put("code", candidate.getCode());
                row.put("city", candidate.getCity());
                row.put("active", candidate.isActive());
            } else {
                row.put("firstName", candidate.getFirstName());
                row.put("lastName", candidate.getLastName());
                if (PossibleMatchService.PATIENT.equals(kind)) {
                    row.put("birthDate", candidate.getBirthDate() == null ? "" : candidate.getBirthDate().toString());
                    row.put("identifier", candidate.getIdentifier() == null ? "" : candidate.getIdentifier());
                } else {
                    row.put("title", candidate.getCode() == null ? "" : candidate.getCode());
                }
            }
            rows.add(row);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("matches", rows);
        return response;
    }

    private static String toJson(Object value) throws JsonProcessingException {
        return value == null ? null : JSON.writeValueAsString(value);
    }
}
