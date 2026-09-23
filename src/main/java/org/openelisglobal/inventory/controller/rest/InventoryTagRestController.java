package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.inventory.service.InventoryTagService;
import org.openelisglobal.inventory.service.InventoryTagService.TagSummary;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The tag directory: light governance over free-form strings, so a lab can see
 * what it has accumulated and retire the duplicates.
 */
@RestController
@RequestMapping("/rest/inventory/tags")
public class InventoryTagRestController extends BaseRestController {

    @Autowired
    private InventoryTagService inventoryTagService;

    /** Every tag, in use or merely declared, with how many items carry it. */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TagSummary>> getDirectory() {
        try {
            return ResponseEntity.ok(inventoryTagService.getDirectory());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(@RequestBody TagRequest body, HttpServletRequest request) {
        try {
            TagSummary created = inventoryTagService.createTag(body.getName(), sysUserId(request));
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (LocalizedValidationException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            error.put("errorCode", e.getErrorCode());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Stops the tag being suggested or filtered on. Every item carrying it keeps
     * it, so this is reversible and loses nothing.
     */
    @PostMapping(value = "/deactivate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deactivate(@RequestBody TagRequest body, HttpServletRequest request) {
        return setActive(body, false, request);
    }

    @PostMapping(value = "/activate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> activate(@RequestBody TagRequest body, HttpServletRequest request) {
        return setActive(body, true, request);
    }

    /**
     * Answers with the directory as it now stands, so the caller needs no second
     * read.
     */
    private ResponseEntity<?> setActive(TagRequest body, boolean active, HttpServletRequest request) {
        try {
            if (body == null || body.getName() == null || body.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            inventoryTagService.setActive(body.getName(), active, sysUserId(request));
            return ResponseEntity.ok(inventoryTagService.getDirectory());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String sysUserId(HttpServletRequest request) {
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
        return String.valueOf(usd.getSystemUserId());
    }

    @Getter
    @Setter
    public static class TagRequest {
        private String name;
    }
}
