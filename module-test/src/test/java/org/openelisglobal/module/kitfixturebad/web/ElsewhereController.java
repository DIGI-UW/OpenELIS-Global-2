package org.openelisglobal.module.kitfixturebad.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/elsewhere")
@PreAuthorize("hasAuthority('mod.kitfixture.view')")
public class ElsewhereController {

    @GetMapping("/things")
    public String things() {
        return "[]";
    }
}
