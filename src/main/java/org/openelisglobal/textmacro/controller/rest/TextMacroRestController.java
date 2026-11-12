package org.openelisglobal.textmacro.controller.rest;

import org.openelisglobal.textmacro.form.TextMacroListForm;
import org.openelisglobal.textmacro.service.TextMacroService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/text-macros")
@PreAuthorize("isAuthenticated()")
public class TextMacroRestController extends TextMacroRestControllerSupport {

    private final TextMacroService service;

    public TextMacroRestController(TextMacroService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<TextMacroListForm> findActive(@RequestParam String context,
            @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(new TextMacroListForm(service.findActive(context, q, limit)));
    }
}
