package org.openelisglobal.scratchinv;

import org.springframework.web.bind.annotation.RestController;

// no class-level @RequestMapping, no @PreAuthorize; handler inherited from ScratchBase
@RestController
public class ScratchInheritedHandlerController extends ScratchBase {
}
