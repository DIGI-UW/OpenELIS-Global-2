package org.openelisglobal.vector.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.vector.service.VectorTrapTypeService;
import org.openelisglobal.vector.valueholder.VectorTrapType;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/admin/vector/trap-types")
public class VectorTrapTypeRestController {

    @Autowired
    private VectorTrapTypeService vectorTrapTypeService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<VectorTrapType>> getTrapTypes(@RequestParam(required = false) Integer groupId) {
        try {
            List<VectorTrapType> result = groupId != null
                    ? vectorTrapTypeService.getByGroupId(groupId)
                    : vectorTrapTypeService.getAll();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VectorTrapType> getTrapType(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(vectorTrapTypeService.get(id));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VectorTrapType> createTrapType(@RequestBody VectorTrapType trapType,
            HttpServletRequest request) {
        try {
            Set<Integer> groupIds = trapType.getGroups().stream()
                    .map(g -> g.getId())
                    .collect(Collectors.toSet());
            Integer id = vectorTrapTypeService.create(trapType, groupIds, ControllerUtills.getSysUserId(request));
            trapType.setId(id);
            return ResponseEntity.status(HttpStatus.CREATED).body(trapType);
        } catch (LIMSRuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VectorTrapType> updateTrapType(@PathVariable Integer id,
            @RequestBody VectorTrapType trapType, HttpServletRequest request) {
        try {
            Set<Integer> groupIds = trapType.getGroups().stream()
                    .map(g -> g.getId())
                    .collect(Collectors.toSet());
            VectorTrapType updated = vectorTrapTypeService.patchUpdate(id, trapType, groupIds,
                    ControllerUtills.getSysUserId(request));
            return ResponseEntity.ok(updated);
        } catch (LIMSRuntimeException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
