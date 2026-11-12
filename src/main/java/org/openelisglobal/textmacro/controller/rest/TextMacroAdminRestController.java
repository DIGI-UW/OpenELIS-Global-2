package org.openelisglobal.textmacro.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.textmacro.form.TextMacroAdminForm;
import org.openelisglobal.textmacro.form.TextMacroAdminQueryForm;
import org.openelisglobal.textmacro.form.TextMacroBulkRequestForm;
import org.openelisglobal.textmacro.form.TextMacroBulkResultForm;
import org.openelisglobal.textmacro.form.TextMacroPageForm;
import org.openelisglobal.textmacro.service.TextMacroService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/text-macros/admin")
@PreAuthorize("hasRole('ADMIN')")
public class TextMacroAdminRestController extends TextMacroRestControllerSupport {

    private final TextMacroService service;

    public TextMacroAdminRestController(TextMacroService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<TextMacroPageForm> search(@ModelAttribute TextMacroAdminQueryForm query) {
        return ResponseEntity.ok(service.searchAdmin(query));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TextMacroAdminForm> get(@PathVariable String id) {
        return ResponseEntity.ok(service.getAdmin(id));
    }

    @PostMapping
    public ResponseEntity<TextMacroAdminForm> create(HttpServletRequest request,
            @RequestBody TextMacroAdminForm macro) {
        return ResponseEntity.ok(service.save(null, macro, authenticatedUserId(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TextMacroAdminForm> update(@PathVariable String id, HttpServletRequest request,
            @RequestBody TextMacroAdminForm macro) {
        return ResponseEntity.ok(service.save(id, macro, authenticatedUserId(request)));
    }

    @GetMapping("/export")
    public ResponseEntity<String> export() {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"openelis-text-macros.csv\"")
                .body(service.exportCsv());
    }

    @PostMapping("/bulk")
    public ResponseEntity<TextMacroBulkResultForm> bulk(HttpServletRequest request,
            @RequestBody TextMacroBulkRequestForm bulkRequest) {
        return ResponseEntity.ok(service.bulk(bulkRequest, authenticatedUserId(request)));
    }
}
