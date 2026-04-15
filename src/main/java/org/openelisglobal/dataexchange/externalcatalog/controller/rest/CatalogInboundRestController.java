package org.openelisglobal.dataexchange.externalcatalog.controller.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.config.condition.ConditionalOnProperty;
import org.openelisglobal.dataexchange.externalcatalog.dto.CatalogDefinitionRequest;
import org.openelisglobal.dataexchange.externalcatalog.exception.CatalogValidationException;
import org.openelisglobal.dataexchange.externalcatalog.service.CatalogInboundService;
import org.openelisglobal.dataexchange.externalcatalog.validation.CatalogRequestValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/catalog")
@ConditionalOnProperty(property = "org.openelisglobal.external.catalog.inbound.enabled", havingValue = "true")
public class CatalogInboundRestController extends BaseRestController {

    @Autowired
    private CatalogInboundService catalogInboundService;

    @Autowired
    private CatalogRequestValidator catalogRequestValidator;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CatalogResponse> upsertCatalog(@RequestBody CatalogDefinitionRequest request,
            HttpServletRequest servletRequest) {

        // --- Layer 1: pre-flight validation ---
        try {
            catalogRequestValidator.validate(request);
        } catch (CatalogValidationException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "upsertCatalog",
                    "Validation failed for catalog request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(CatalogResponse.validationError(e.getErrors()));
        }

        // --- Layer 2: upsert ---
        try {
            String currentUserId = getSysUserId(servletRequest);
            String guid = catalogInboundService.upsert(request, currentUserId);
            return ResponseEntity.ok(CatalogResponse.success(guid));
        } catch (CatalogValidationException e) {
            // Resolver-level validation (second line of defence)
            LogEvent.logWarn(this.getClass().getSimpleName(), "upsertCatalog",
                    "Resolver validation failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(CatalogResponse.validationError(e.getErrors()));
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "upsertCatalog",
                    "Unexpected error upserting catalog item: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CatalogResponse.error("Internal error processing catalog item"));
        }
    }

    // -------------------------------------------------------------------------
    // Jackson deserialization failure — e.g. price sent as a string
    // Returns our standard CatalogResponse envelope instead of Spring's
    // ProblemDetail
    // -------------------------------------------------------------------------

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CatalogResponse> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        LogEvent.logWarn(this.getClass().getSimpleName(), "upsertCatalog",
                "Malformed request body: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(CatalogResponse.validationError(List
                .of("Request body is malformed — check field types (e.g. 'price' must be a number, not a string)")));
    }

    // -------------------------------------------------------------------------
    // Response envelope — always JSON, never a raw string
    // -------------------------------------------------------------------------

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CatalogResponse {

        @JsonProperty("status")
        private String status;

        @JsonProperty("message")
        private String message;

        @JsonProperty("guid")
        private String guid;

        /** Populated only on VALIDATION_ERROR responses. */
        @JsonProperty("errors")
        private List<String> errors;

        static CatalogResponse success(String guid) {
            CatalogResponse r = new CatalogResponse();
            r.status = "SUCCESS";
            r.message = "Catalog item upserted";
            r.guid = guid;
            return r;
        }

        static CatalogResponse validationError(List<String> errors) {
            CatalogResponse r = new CatalogResponse();
            r.status = "VALIDATION_ERROR";
            r.message = "Request failed validation";
            r.errors = errors;
            return r;
        }

        static CatalogResponse error(String message) {
            CatalogResponse r = new CatalogResponse();
            r.status = "ERROR";
            r.message = message;
            return r;
        }
    }
}
