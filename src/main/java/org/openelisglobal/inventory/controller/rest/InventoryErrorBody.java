package org.openelisglobal.inventory.controller.rest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.exception.LocalizedValidationException;

/**
 * JSON error bodies the inventory REST controllers share, so a client sees one
 * shape per kind of failure instead of one per endpoint.
 */
final class InventoryErrorBody {

    private InventoryErrorBody() {
    }

    /**
     * {message, errorCode, params}: the frontend translates errorCode with params.
     */
    static Map<String, Object> localized(LocalizedValidationException e) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", e.getMessage());
        body.put("errorCode", e.getErrorCode());
        body.put("params", e.getParams());
        return body;
    }

    static Map<String, String> error(String message) {
        return Collections.singletonMap("error", message);
    }

    static Map<String, String> notFound(ObjectNotFoundException e) {
        String entityName = e.getEntityName();
        String simpleName = entityName.substring(entityName.lastIndexOf('.') + 1);
        return error(simpleName + " " + e.getIdentifier() + " not found");
    }
}
