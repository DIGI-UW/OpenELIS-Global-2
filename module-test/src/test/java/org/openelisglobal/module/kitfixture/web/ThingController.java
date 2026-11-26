package org.openelisglobal.module.kitfixture.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/modules/kitfixture")
public class ThingController {

    @GetMapping("/things")
    @PreAuthorize("hasAuthority('mod.kitfixture.view')")
    public String things() {
        return "[]";
    }
}
