package org.openelisglobal.dataexchange.fhir.service;

import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.common.ReportTransmission;
import org.openelisglobal.dataexchange.common.ReportTransmission.HTTP_TYPE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link ResultMiddlewareSyncService} that sends
 * validated result FHIR Bundles to an external middleware via HTTP POST.
 *
 * Behaviour is fully controlled via configuration properties and is disabled
 * by default to avoid impacting existing deployments.
 */
@Service
public class ResultMiddlewareSyncServiceImpl implements ResultMiddlewareSyncService {

    @Value("${org.openelisglobal.middleware.result.sync.enabled:false}")
    private boolean syncEnabled;

    @Value("${org.openelisglobal.middleware.result.sync.url:}")
    private String middlewareUrl;

    @Value("${org.openelisglobal.middleware.result.sync.secret:change-me}")
    private String middlewareSecret;

    @Autowired
    private FhirContext fhirContext;

    @Override
    public void sendValidatedResultsBundle(Bundle bundle) {
        if (!syncEnabled) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "sendValidatedResultsBundle",
                    "Middleware result sync is disabled (org.openelisglobal.middleware.result.sync.enabled=false), skipping");
            return;
        }

        if (bundle == null || !bundle.hasEntry()) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "sendValidatedResultsBundle",
                    "Validated results bundle is null or empty, skipping middleware sync");
            return;
        }

        if (GenericValidator.isBlankOrNull(middlewareUrl)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "sendValidatedResultsBundle",
                    "Middleware result sync URL is not configured (org.openelisglobal.middleware.result.sync.url), skipping");
            return;
        }

        try {
            String json = fhirContext.newJsonParser().encodeResourceToString(bundle);

            LogEvent.logInfo(this.getClass().getSimpleName(), "sendValidatedResultsBundle",
                    "Sending validated results FHIR bundle to middleware: entryCount=" + bundle.getEntry().size()
                            + ", url=" + middlewareUrl);

            ReportTransmission transmission = new ReportTransmission();
            transmission.sendRawReport(json, middlewareUrl, true, null, HTTP_TYPE.POST,
                    "X-Nidan-Webhook-Secret", middlewareSecret);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "sendValidatedResultsBundle",
                    "Error syncing validated results bundle to middleware: " + e.getMessage());
            LogEvent.logError(this.getClass().getSimpleName(), "sendValidatedResultsBundle", "Full error: " + e);
        }
    }
}
