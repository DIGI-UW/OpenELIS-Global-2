package org.openelisglobal.module.kitfixturebad.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/modules/kitfixture")
public class OpenController {

    @GetMapping("/open")
    public String open() {
        return "anyone";
    }
}
