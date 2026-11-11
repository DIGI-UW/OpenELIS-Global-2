package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.microbiology.form.MicroLotSelectionRequestForm;
import org.openelisglobal.microbiology.service.MicroLotSelection;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Shared authenticated-actor lookup for microbiology write endpoints. */
abstract class MicrobiologyRestControllerSupport extends BaseRestController {
    static final String BENCH_ACCESS = "hasAnyRole('ADMIN', 'RESULTS', 'VALIDATION')";
    static final String SUPERVISOR_ACCESS = "hasAnyRole('ADMIN', 'VALIDATION')";

    protected String authenticatedUserId(HttpServletRequest request) {
        String userId = getSysUserId(request);
        if (userId == null || userId.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated system user is required");
        }
        return userId;
    }

    protected <T extends Enum<T>> T requiredEnum(Class<T> enumType, String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(fieldName + " is invalid");
        }
    }

    protected List<MicroLotSelection> lotSelections(List<MicroLotSelectionRequestForm> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream()
                .map(request -> new MicroLotSelection(request.analysisId, request.testReagentLinkId, request.lotId))
                .toList();
    }
}
