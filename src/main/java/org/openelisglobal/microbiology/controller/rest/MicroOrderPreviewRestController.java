package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.microbiology.form.MicroOrderPreviewForm;
import org.openelisglobal.microbiology.form.MicroOrderPreviewRequestForm;
import org.openelisglobal.microbiology.service.MicroOrderPreviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/microbiology/order-preview")
@PreAuthorize("isAuthenticated()")
public class MicroOrderPreviewRestController extends MicrobiologyRestControllerSupport {
    private final MicroOrderPreviewService service;

    public MicroOrderPreviewRestController(MicroOrderPreviewService service) {
        this.service = service;
    }

    @PostMapping
    public MicroOrderPreviewForm preview(@RequestBody MicroOrderPreviewRequestForm request,
            HttpServletRequest httpRequest) {
        return service.preview(request, authenticatedUserId(httpRequest));
    }
}
