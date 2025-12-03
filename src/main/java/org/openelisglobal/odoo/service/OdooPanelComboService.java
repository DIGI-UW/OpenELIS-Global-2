package org.openelisglobal.odoo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.openelisglobal.odoo.client.OdooClient;
import org.openelisglobal.odoo.client.OdooConnection;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.localization.valueholder.Localization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OdooPanelComboService {

    @Autowired
    private OdooConnection odooConnection;

    @Autowired
    private OdooClient odooClient;

    @Autowired
    private PanelItemService panelItemService;

    public void syncPanelComboToOdoo(Panel panel) {
        if (panel == null) {
            log.warn("Cannot sync combo for null panel to Odoo");
            return;
        }

        if (!odooConnection.isAvailable()) {
            log.info("Odoo is not available, skipping combo sync for panel {}", panel.getId());
            return;
        }

        try {
            String panelCode = resolvePanelCode(panel);
            String panelName = resolvePanelName(panel);

            if (panelName == null || panelName.trim().isEmpty()) {
                log.warn("No localized name found for panel {}, skipping Odoo combo sync", panel.getId());
                return;
            }

            // Find the panel product.template by default_code
            Integer panelTemplateId = findPanelProductTemplateId(panelCode);
            if (panelTemplateId == null) {
                log.warn("No Odoo product.template found for panel {} (code={}), skipping combo sync", panel.getId(),
                        panelCode);
                return;
            }

            // Ensure a product.combo exists for this panel
            Integer comboId = findOrCreateCombo(panelName);
            if (comboId == null) {
                log.warn("Could not resolve or create product.combo for panel {} ({}), skipping combo sync",
                        panel.getId(), panelName);
                return;
            }

            // Link combo to panel product via combo_ids M2M field
            linkComboToPanelProduct(panelTemplateId, comboId, panel);

            // Rebuild combo items from current panel membership
            rebuildComboItems(panel, comboId);
        } catch (Exception e) {
            log.error("Error syncing panel combo {} to Odoo: {}", panel.getId(), e.getMessage(), e);
        }
    }

    private String resolvePanelCode(Panel panel) {
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

    private Integer findPanelProductTemplateId(String panelCode) {
        List<Object> criteria = List.of("default_code", "=", panelCode);
        List<String> fields = List.of("id");
        Object[] existing = odooConnection.searchAndRead("product.template", criteria, fields);
        return extractIdFromSearchResult(existing);
    }

    private Integer findOrCreateCombo(String panelName) {
        List<Object> criteria = List.of("name", "=", panelName);
        List<String> fields = List.of("id", "name");
        Object[] existing = odooConnection.searchAndRead("product.combo", criteria, fields);
        Integer comboId = extractIdFromSearchResult(existing);
        if (comboId != null) {
            return comboId;
        }

        Map<String, Object> combo = new HashMap<>();
        combo.put("name", panelName);
        combo.put("sequence", 10);
        Integer createdId = odooConnection.create("product.combo", List.of(combo));
        log.info("Created Odoo product.combo with ID {} for panel {}", createdId, panelName);
        return createdId;
    }

    private void linkComboToPanelProduct(Integer panelTemplateId, Integer comboId, Panel panel) {
        try {
            // Use Odoo M2M command: [(6, 0, [comboId])] to replace combo_ids with this combo
            List<Object> command = new ArrayList<>();
            command.add(6); // replace
            command.add(0);
            command.add(List.of(comboId));

            List<Object> commands = new ArrayList<>();
            commands.add(command);

            Map<String, Object> update = new HashMap<>();
            update.put("combo_ids", commands);

            List<Object> writeParams = List.of(List.of(panelTemplateId), update);
            Boolean success = odooClient.write("product.template", writeParams);
            log.info("Linked combo {} to panel product.template {} for panel {}, success={}", comboId,
                    panelTemplateId, panel.getId(), success);
        } catch (Exception e) {
            log.warn("Failed to link combo {} to panel product.template {} for panel {}: {}", comboId, panelTemplateId,
                    panel.getId(), e.getMessage());
        }
    }

    private void rebuildComboItems(Panel panel, Integer comboId) {
        try {
            // Delete existing combo items for this combo
            List<Object> criteria = List.of("combo_id", "=", comboId);
            Object[] existingIds = odooClient.search("product.combo.item", criteria);

            if (existingIds != null && existingIds.length > 0) {
                List<Object> ids = new ArrayList<>();
                for (Object id : existingIds) {
                    if (id instanceof Integer) {
                        ids.add(id);
                    }
                }
                if (!ids.isEmpty()) {
                    List<Object> deleteParams = List.of(ids);
                    Boolean deleted = odooClient.delete("product.combo.item", deleteParams);
                    log.info("Deleted {} existing product.combo.item records for combo {} (success={})", ids.size(),
                            comboId, deleted);
                }
            }

            // Recreate combo items from current panel tests
            List<PanelItem> panelItems = panelItemService.getPanelItemsForPanel(panel.getId());
            for (PanelItem panelItem : panelItems) {
                Test test = panelItem.getTest();
                if (test == null) {
                    continue;
                }
                Integer variantId = resolveVariantIdForTest(test);
                if (variantId == null) {
                    log.warn("No Odoo product.product variant found for test {} when building combo for panel {}",
                            test.getId(), panel.getId());
                    continue;
                }

                Map<String, Object> item = new HashMap<>();
                item.put("combo_id", comboId);
                item.put("product_id", variantId);
                item.put("extra_price", 0.0);

                Integer itemId = odooConnection.create("product.combo.item", List.of(item));
                log.info("Created product.combo.item {} for combo {} (panel {}, test {})", itemId, comboId,
                        panel.getId(), test.getId());
            }
        } catch (Exception e) {
            log.warn("Failed to rebuild combo items for panel {} and combo {}: {}", panel.getId(), comboId,
                    e.getMessage());
        }
    }

    private Integer resolveVariantIdForTest(Test test) {
        String testCode = resolveTestCode(test);
        if (testCode == null || testCode.trim().isEmpty()) {
            return null;
        }

        // Find the product.template for this test
        List<Object> criteria = List.of("default_code", "=", testCode);
        List<String> fields = List.of("id");
        Object[] existing = odooConnection.searchAndRead("product.template", criteria, fields);
        Integer templateId = extractIdFromSearchResult(existing);
        if (templateId == null) {
            return null;
        }

        // Find a product.product variant for this template
        List<Object> variantCriteria = List.of("product_tmpl_id", "=", templateId);
        Object[] variantIds = odooClient.search("product.product", variantCriteria);
        if (variantIds == null || variantIds.length == 0) {
            return null;
        }
        if (variantIds[0] instanceof Integer) {
            return (Integer) variantIds[0];
        }
        return null;
    }

    private String resolveTestCode(Test test) {
        if (test.getGuid() != null && !test.getGuid().trim().isEmpty()) {
            return test.getGuid();
        }
        if (test.getLocalCode() != null && !test.getLocalCode().trim().isEmpty()) {
            return test.getLocalCode();
        }
        if (test.getLoinc() != null && !test.getLoinc().trim().isEmpty()) {
            return test.getLoinc();
        }
        return "OE_TEST_" + test.getId();
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
