package org.openelisglobal.panel.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Reference;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implementation of Panel FHIR transformation and sync service.
 * 
 * Transforms OpenELIS Panels to FHIR List resources and syncs to OpenMRS:
 * - Panel → FHIR List → OpenMRS LabSet Concept
 * - Panel members (tests) → List entries → Concept setMembers
 */
@Service
public class PanelFhirTransformServiceImpl implements PanelFhirTransformService {

    @Autowired
    private FhirContext fhirContext;

    @Autowired
    private FhirConfig fhirConfig;

    @Autowired
    private PanelItemService panelItemService;

    @Value("${org.openelisglobal.fhir.panel.sync.enabled:false}")
    private boolean syncEnabled;

    @Override
    public void syncPanelToFhir(Panel panel) {
        if (!syncEnabled) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "syncPanelToFhir",
                    "Panel FHIR sync is disabled");
            return;
        }

        if (panel == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "syncPanelToFhir",
                    "Panel is null, skipping sync");
            return;
        }

        if (GenericValidator.isBlankOrNull(panel.getGuid())) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "syncPanelToFhir",
                    "Panel " + panel.getId() + " has no GUID, skipping FHIR sync");
            return;
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "syncPanelToFhir",
                "Syncing panel to FHIR: " + panel.getDescription() + " (guid=" + panel.getGuid() + ")");

        try {
            // Transform to FHIR List
            ListResource fhirList = transformPanelToList(panel);

            // Sync to each configured server
            for (String serverUrl : fhirConfig.getRemoteStorePaths()) {
                syncToServer(fhirList, serverUrl);
            }
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "syncPanelToFhir",
                    "Error syncing panel " + panel.getGuid() + ": " + e.getMessage());
            throw new RuntimeException("Failed to sync panel to FHIR", e);
        }
    }

    /**
     * Transform OpenELIS Panel to FHIR List resource.
     * 
     * Mapping:
     * - panel.guid → List.id
     * - panel.description → List.title
     * - "current" → List.status
     * - panel_item.test.guid → List.entry[].item.reference
     */
    private ListResource transformPanelToList(Panel panel) {
        ListResource list = new ListResource();

        // 1. Set ID (use panel GUID)
        list.setId(panel.getGuid());

        // 2. Set title (prioritize localized name, then description, then name)
        String title = null;
        if (panel.getLocalization() != null) {
            title = panel.getLocalization().getEnglish();
        }
        if (GenericValidator.isBlankOrNull(title)) {
            title = panel.getDescription();
        }
        if (GenericValidator.isBlankOrNull(title)) {
            title = panel.getPanelName();
        }
        if (GenericValidator.isBlankOrNull(title)) {
            title = panel.getGuid(); // Fallback to GUID
        }
        list.setTitle(title);

        // 3. Set status (always "current")
        list.setStatus(ListResource.ListStatus.CURRENT);

        // 4. Add entries (panel members - tests)
        List<PanelItem> items = panelItemService.getPanelItemsForPanel(panel.getId());

        LogEvent.logInfo(this.getClass().getSimpleName(), "transformPanelToList",
                "Panel " + panel.getDescription() + " has " + items.size() + " test members");

        for (PanelItem item : items) {
            Test test = item.getTest();
            if (test != null && !GenericValidator.isBlankOrNull(test.getGuid())) {
                ListResource.ListEntryComponent entry = new ListResource.ListEntryComponent();

                // Reference format: "ObservationDefinition/{testGuid}"
                Reference testRef = new Reference("ObservationDefinition/" + test.getGuid());
                entry.setItem(testRef);

                list.addEntry(entry);

                LogEvent.logDebug(this.getClass().getSimpleName(), "transformPanelToList",
                        "Added test member: " + test.getDescription() + " (guid=" + test.getGuid() + ")");
            } else {
                LogEvent.logWarn(this.getClass().getSimpleName(), "transformPanelToList",
                        "Panel item has no test or test has no GUID, skipping");
            }
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "transformPanelToList",
                "Transformed panel: " + panel.getDescription() +
                        " (guid=" + panel.getGuid() + ", members=" + list.getEntry().size() + ")");

        return list;
    }

    /**
     * Sync FHIR List to OpenMRS server using PUT upsert.
     */
    private void syncToServer(ListResource list, String serverUrl) {
        try {
            IGenericClient fhirClient = fhirContext.newRestfulGenericClient(serverUrl);

            // Add authentication if configured
            if (!GenericValidator.isBlankOrNull(fhirConfig.getUsername())) {
                IClientInterceptor authInterceptor = new BasicAuthInterceptor(
                        fhirConfig.getUsername(),
                        fhirConfig.getPassword());
                fhirClient.registerInterceptor(authInterceptor);
            }

            // PUT upsert (create or update)
            MethodOutcome outcome = fhirClient.update()
                    .resource(list)
                    .execute();

            // Check if this was a create or update
            if (outcome.getCreated() != null && outcome.getCreated()) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "syncToServer",
                        "✅ Created Panel in OpenMRS via PUT upsert: " + serverUrl +
                                " (Panel GUID=" + list.getId() + " preserved as OpenMRS UUID)");
            } else {
                LogEvent.logInfo(this.getClass().getSimpleName(), "syncToServer",
                        "✅ Updated Panel in OpenMRS: " + serverUrl + " (id=" + list.getId() + ")");
            }

            // Log the response ID if available
            if (outcome.getId() != null) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "syncToServer",
                        "OpenMRS confirmed UUID: " + outcome.getId().getIdPart());
            }

        } catch (ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "syncToServer",
                    "❌ FHIR Server Error - HTTP " + e.getStatusCode() + ": " + e.getMessage());
            LogEvent.logError(this.getClass().getSimpleName(), "syncToServer",
                    "Response body: " + e.getResponseBody());
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "syncToServer",
                    "Error syncing panel to FHIR server " + serverUrl + ": " + e.getMessage());
            throw new RuntimeException("Failed to sync panel to server", e);
        }
    }
}
