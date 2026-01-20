package org.openelisglobal.panel.service.middleware;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.common.ReportTransmission;
import org.openelisglobal.dataexchange.common.ReportTransmission.HTTP_TYPE;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PanelMiddlewareSyncServiceImpl implements PanelMiddlewareSyncService {

    @Autowired
    private PanelItemService panelItemService;

    @Value("${org.openelisglobal.middleware.test.sync.enabled:false}")
    private boolean syncEnabled;

    @Value("${org.openelisglobal.middleware.test.sync.url:}")
    private String middlewareUrl;

    @Value("${org.openelisglobal.middleware.test.sync.secret:change-me}")
    private String middlewareSecret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void syncPanelToMiddleware(Panel panel) {
        if (!syncEnabled) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "syncPanelToMiddleware",
                    "Middleware test/panel sync is disabled (org.openelisglobal.middleware.test.sync.enabled=false), skipping");
            return;
        }

        if (panel == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "syncPanelToMiddleware", "Cannot sync null panel");
            return;
        }

        if (GenericValidator.isBlankOrNull(middlewareUrl)) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "syncPanelToMiddleware",
                    "Middleware URL is not configured (org.openelisglobal.middleware.test.sync.url), skipping");
            return;
        }

        if (GenericValidator.isBlankOrNull(panel.getGuid())) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "syncPanelToMiddleware",
                    "Panel " + panel.getId() + " has no GUID, skipping middleware sync");
            return;
        }

        try {
            PanelDefinitionEventPayload payload = buildPayload(panel);
            String json = serializePayload(payload);

            LogEvent.logInfo(this.getClass().getSimpleName(), "syncPanelToMiddleware",
                    "Sending panel definition to middleware: panelId=" + panel.getId() + ", guid=" + panel.getGuid()
                            + ", url=" + middlewareUrl);

            ReportTransmission transmission = new ReportTransmission();
            transmission.sendRawReport(json, middlewareUrl, true, null, HTTP_TYPE.POST,
                    "X-Nidan-Webhook-Secret", middlewareSecret);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "syncPanelToMiddleware",
                    "Error syncing panel to middleware: " + e.getMessage());
            LogEvent.logError(this.getClass().getSimpleName(), "syncPanelToMiddleware", "Full error: " + e);
        }
    }

    private PanelDefinitionEventPayload buildPayload(Panel panel) {
        PanelDefinitionEventPayload payload = new PanelDefinitionEventPayload();

        // We treat panel events as updates to the catalog; create/update distinction is
        // not available from the current PanelCreatedOrUpdatedEvent.
        payload.eventType = "LabTestDefinitionUpdated";
        payload.sourceSystem = "OPENELIS";
        payload.occurredAt = OffsetDateTime.now().toString();
        payload.eventId = UUID.randomUUID().toString();

        payload.changeType = "UPDATED";

        String panelUuid = !GenericValidator.isBlankOrNull(panel.getGuid()) ? panel.getGuid() : panel.getId();
        payload.testUuid = panelUuid;
        payload.openelisTestId = panel.getId();
        payload.loincCode = panel.getLoinc();

        String name = resolvePanelName(panel);
        payload.name = name;
        payload.description = panel.getDescription();

        // Panels are always flagged as panel=true
        payload.panel = true;
        payload.componentTestUuids = buildComponentTestUuids(panel);

        payload.defaultPrice = panel.getPrice();
        payload.price = panel.getPrice();

        return payload;
    }

    private String resolvePanelName(Panel panel) {
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
            title = panel.getGuid();
        }
        return title;
    }

    private List<String> buildComponentTestUuids(Panel panel) {
        List<String> uuids = new ArrayList<>();

        List<PanelItem> items = panelItemService.getPanelItemsForPanel(panel.getId());
        LogEvent.logInfo(this.getClass().getSimpleName(), "buildComponentTestUuids",
                "Panel " + panel.getDescription() + " has " + items.size() + " test members");

        for (PanelItem item : items) {
            Test test = item.getTest();
            if (test != null && !GenericValidator.isBlankOrNull(test.getGuid())) {
                uuids.add(test.getGuid());
            } else {
                LogEvent.logWarn(this.getClass().getSimpleName(), "buildComponentTestUuids",
                        "Panel item has no test or test has no GUID, skipping");
            }
        }

        return uuids;
    }

    private String serializePayload(PanelDefinitionEventPayload payload) throws JsonProcessingException {
        return objectMapper.writeValueAsString(payload);
    }

    private static class PanelDefinitionEventPayload {
        public String eventType;
        public String sourceSystem;
        public String occurredAt;
        public String eventId;

        public String changeType;

        public String testUuid;
        public String openelisTestId;
        public String loincCode;
        public String name;
        public String description;

        public Boolean orderable;
        public BigDecimal price;

        public boolean panel;
        public List<String> componentTestUuids;

        public BigDecimal defaultPrice;
        public String currency;
    }
}
