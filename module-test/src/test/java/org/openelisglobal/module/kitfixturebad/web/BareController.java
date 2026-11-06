package org.openelisglobal.module.kitfixturebad.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Maps the bare module prefix, which the core refuses at start-up. */
@RestController
@RequestMapping("/rest/modules/kitfixture")
@PreAuthorize("hasAuthority('mod.kitfixture.view')")
public class BareController {

    @GetMapping
    public String bare() {
        return "[]";
    }
}
