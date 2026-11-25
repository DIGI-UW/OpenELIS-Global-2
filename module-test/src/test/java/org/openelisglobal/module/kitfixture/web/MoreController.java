package org.openelisglobal.module.kitfixture.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * No class-level mapping: the method's full path is what counts, as in the
 * core.
 */
@RestController
@PreAuthorize("hasAuthority('mod.kitfixture.view')")
public class MoreController {

    @GetMapping("/rest/modules/kitfixture/more")
    public String more() {
        return "[]";
    }
}
