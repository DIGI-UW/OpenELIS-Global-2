package org.openelisglobal.scratchinv;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scratchPermitAll")
@PreAuthorize("permitAll()")
public class ScratchPermitAllController {
    @GetMapping("/leak")
    public String leak() {
        return "leak";
    }
}
