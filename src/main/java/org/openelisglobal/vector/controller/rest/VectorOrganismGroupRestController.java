package org.openelisglobal.vector.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.vector.service.VectorOrganismGroupService;
import org.openelisglobal.vector.valueholder.VectorOrganismGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/admin/vector/groups")
public class VectorOrganismGroupRestController {

    @Autowired
    private VectorOrganismGroupService vectorOrganismGroupService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<VectorOrganismGroup>> getAllGroups() {
        try {
            return ResponseEntity.ok(vectorOrganismGroupService.getAll());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/active", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<VectorOrganismGroup>> getActiveGroups() {
        try {
            return ResponseEntity.ok(vectorOrganismGroupService.getActiveGroups());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VectorOrganismGroup> getGroup(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(vectorOrganismGroupService.get(id));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VectorOrganismGroup> createGroup(@RequestBody VectorOrganismGroup group,
            HttpServletRequest request) {
        try {
            group.setSysUserId(ControllerUtills.getSysUserId(request));
            Integer id = vectorOrganismGroupService.insert(group);
            group.setId(id);
            return ResponseEntity.status(HttpStatus.CREATED).body(group);
        } catch (LIMSRuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VectorOrganismGroup> updateGroup(@PathVariable Integer id,
            @RequestBody VectorOrganismGroup group, HttpServletRequest request) {
        try {
            VectorOrganismGroup updated = vectorOrganismGroupService.patchUpdate(id, group,
                    ControllerUtills.getSysUserId(request));
            return ResponseEntity.ok(updated);
        } catch (LIMSRuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
