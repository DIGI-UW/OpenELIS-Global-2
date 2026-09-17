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

/**
 * Shared authenticated-actor lookup for microbiology write endpoints.
 *
 * <p>
 * The BENCH_ACCESS and SUPERVISOR_ACCESS role expressions that used to live
 * here are gone: authorization moved to the service layer as privilege gates,
 * which is what the S011c build check enforces. Their reach was preserved
 * exactly — BENCH_ACCESS (ADMIN/RESULTS/VALIDATION) became micro:view +
 * micro:bench and SUPERVISOR_ACCESS (ADMIN/VALIDATION) became micro:supervise,
 * granted to those same roles in Liquibase 012-004d. Add gates to the service
 * interface, not here.
 */
abstract class MicrobiologyRestControllerSupport extends BaseRestController {

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
