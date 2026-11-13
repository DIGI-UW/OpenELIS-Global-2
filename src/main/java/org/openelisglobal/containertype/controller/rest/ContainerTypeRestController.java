package org.openelisglobal.containertype.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.containertype.form.ContainerTypeForm;
import org.openelisglobal.containertype.form.ContainerTypeUsage;
import org.openelisglobal.containertype.form.ContainerTypeView;
import org.openelisglobal.containertype.service.ContainerTypeRefusedException;
import org.openelisglobal.containertype.service.ContainerTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OGC-1611 FR-G1 to G3: the Container Types admin list. */
@RestController
@RequestMapping("/rest/container-types")
@PreAuthorize("hasRole('ADMIN')")
public class ContainerTypeRestController extends BaseRestController {

    @Autowired
    private ContainerTypeService containerTypeService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ContainerTypeView> list(@RequestParam(required = false) String search,
            @RequestParam(required = false) String domain, @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return containerTypeService.list(search, domain, category, includeInactive);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ContainerTypeView add(HttpServletRequest request, @RequestBody ContainerTypeForm form) {
        return containerTypeService.save(null, form, getSysUserId(request));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ContainerTypeView edit(HttpServletRequest request, @PathVariable Long id,
            @RequestBody ContainerTypeForm form) {
        return containerTypeService.save(id, form, getSysUserId(request));
    }

    @GetMapping(value = "/{id}/usage", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ContainerTypeUsage> usage(@PathVariable Long id) {
        return ResponseEntity.of(containerTypeService.usage(id));
    }

    @PostMapping(value = "/{id}/deactivate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ContainerTypeView> deactivate(HttpServletRequest request, @PathVariable Long id) {
        return ResponseEntity.of(containerTypeService.setActive(id, false, getSysUserId(request)));
    }

    @PostMapping(value = "/{id}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ContainerTypeView> activate(HttpServletRequest request, @PathVariable Long id) {
        return ResponseEntity.of(containerTypeService.setActive(id, true, getSysUserId(request)));
    }

    @ExceptionHandler(ContainerTypeRefusedException.class)
    public ResponseEntity<Map<String, String>> refused(ContainerTypeRefusedException refusal) {
        int status = "notFound".equals(refusal.getCode()) ? 404 : 400;
        return ResponseEntity.status(status).body(Map.of("field", refusal.getField(), "code", refusal.getCode()));
    }
}
