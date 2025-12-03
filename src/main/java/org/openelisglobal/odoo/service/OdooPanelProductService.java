package org.openelisglobal.odoo.service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.openelisglobal.odoo.client.OdooClient;
import org.openelisglobal.odoo.client.OdooConnection;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.localization.valueholder.Localization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OdooPanelProductService {

    @Autowired
    private OdooConnection odooConnection;

    @Autowired
    private OdooClient odooClient;

    public void syncPanelToOdoo(Panel panel) {
        if (panel == null) {
            log.warn("Cannot sync null panel to Odoo");
            return;
        }

        if (!odooConnection.isAvailable()) {
            log.info("Odoo is not available, skipping product sync for panel {}", panel.getId());
            return;
        }

        try {
            String panelCode = resolvePanelCode(panel);
            String panelName = resolvePanelName(panel);

            if (panelName == null || panelName.trim().isEmpty()) {
                log.warn("No localized name found for panel {}, skipping Odoo panel product sync", panel.getId());
                return;
            }

            // Check if panel product already exists by default_code
            List<Object> criteria = List.of("default_code", "=", panelCode);
            List<String> fields = List.of("id", "name", "default_code");
            Object[] existing = odooConnection.searchAndRead("product.template", criteria, fields);

            Integer existingProductId = extractIdFromSearchResult(existing);

            Map<String, Object> product = new HashMap<>();
            product.put("name", panelName);
            product.put("default_code", panelCode);
            product.put("type", "service");
            product.put("sale_ok", true);
            product.put("purchase_ok", false);

            // Derive list_price from the ELIS panel price when available, otherwise default to 0.0
            Double listPrice = 0.0;
            java.math.BigDecimal panelPrice = panel.getPrice();
            if (panelPrice != null) {
                listPrice = panelPrice.doubleValue();
            }
            product.put("list_price", listPrice);

            // Map ELIS panel active flag to Odoo 'active' field
            boolean active = panel.getIsActive() == null || !"N".equalsIgnoreCase(panel.getIsActive());
            product.put("active", active);

            if (existingProductId != null) {
                // Update existing panel product using low-level OdooClient.write
                List<Object> writeParams = List.of(List.of(existingProductId), product);
                Boolean success = odooClient.write("product.template", writeParams);
                log.info(
                        "Updated Odoo panel product with ID {} for panel {} ({}), success={}",
                        existingProductId, panel.getId(), panelCode, success);
            } else {
                // Create new panel product via OdooConnection abstraction
                Integer productId = odooConnection.create("product.template", List.of(product));
                log.info("Created Odoo panel product with ID {} for panel {} ({})", productId, panel.getId(),
                        panelCode);
            }
        } catch (Exception e) {
            log.error("Error syncing panel {} to Odoo: {}", panel.getId(), e.getMessage(), e);
        }
    }

    private String resolvePanelCode(Panel panel) {
        // Prefer a stable GUID-style identifier when available, with legacy codes as
        // fallbacks for older data.
        if (panel.getGuid() != null && !panel.getGuid().trim().isEmpty()) {
            return panel.getGuid();
        }
        if (panel.getId() != null && !panel.getId().trim().isEmpty()) {
            return "PANEL_" + panel.getId();
        }
        if (panel.getLoinc() != null && !panel.getLoinc().trim().isEmpty()) {
            return panel.getLoinc();
        }
        return "PANEL_UNKNOWN";
    }

    private String resolvePanelName(Panel panel) {
        try {
            Localization localization = panel.getLocalization();
            if (localization != null) {
                String localized = localization.getLocalizedValue(Locale.ENGLISH);
                if (localized != null && !localized.trim().isEmpty()) {
                    return localized;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to resolve localized panel name for panel {}: {}", panel.getId(), e.getMessage());
        }

        if (panel.getPanelName() != null && !panel.getPanelName().trim().isEmpty()) {
            return panel.getPanelName().trim();
        }

        if (panel.getDescription() != null && !panel.getDescription().trim().isEmpty()) {
            return panel.getDescription().trim();
        }

        return "Panel " + panel.getId();
    }

    private Integer extractIdFromSearchResult(Object[] result) {
        if (result == null || result.length == 0) {
            return null;
        }
        Object first = result[0];
        if (first instanceof Map) {
            Map<?, ?> data = (Map<?, ?>) first;
            Object id = data.get("id");
            if (id instanceof Integer) {
                return (Integer) id;
            }
        }
        return null;
    }
}
