package org.openelisglobal.storage.controller;

import java.util.Map;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;

/**
 * Response-boundary translation of the raw-statusId {@code status} field on a
 * sample map to the spec-compliant enum string. Spec contract:
 * specs/001-sample-storage/contracts/storage-api.json:862,885 —
 * {@code "status": { "enum": ["active", "disposed"] }}.
 */
final class SampleStatusResponse {

    private SampleStatusResponse() {
    }

    // Shared by the listing and the search endpoint: the client reads this
    // field to draw the disposed badge, and a raw status id draws Active.
    static void normalize(Map<String, Object> sample, IStatusService statusService) {
        Object raw = sample.get("status");
        if (!(raw instanceof String) || ((String) raw).isEmpty()) {
            sample.put("status", "active");
            return;
        }
        String statusId = (String) raw;
        if (statusService.matches(statusId, SampleStatus.Disposed)) {
            sample.put("status", "disposed");
        } else {
            sample.put("status", "active");
        }
    }
}
