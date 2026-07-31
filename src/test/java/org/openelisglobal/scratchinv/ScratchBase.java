package org.openelisglobal.scratchinv;

import org.springframework.web.bind.annotation.GetMapping;

public abstract class ScratchBase {
    // handler declared only on the superclass, full path on the method
    @GetMapping("/api/scratchInherited/leak")
    public String leak() {
        return "leak";
    }
}
