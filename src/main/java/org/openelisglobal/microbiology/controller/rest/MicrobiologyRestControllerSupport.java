package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Shared authenticated-actor lookup for microbiology write endpoints. */
abstract class MicrobiologyRestControllerSupport extends BaseRestController {

    protected String authenticatedUserId(HttpServletRequest request) {
        String userId = getSysUserId(request);
        if (userId == null || userId.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated system user is required");
        }
        return userId;
    }
}
