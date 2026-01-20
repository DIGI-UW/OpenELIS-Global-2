package org.openelisglobal.odoo.client;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RealOdooClient implements OdooConnection {

    private final OdooClient odooClient;

    private boolean available = false;

    public RealOdooClient(OdooClient odooClient) {
        this.odooClient = odooClient;
        try {
            odooClient.init();
            available = true;
            log.info("Successfully connected to Odoo at startup.");
        } catch (Exception e) {
            available = false;
            log.error("Failed to connect to Odoo at startup: {}. Will retry on next operation.", e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        checkConnection();
        return available;
    }

    private synchronized void checkConnection() {
        if (!available) {
            try {
                log.info("Attempting to reconnect to Odoo...");
                odooClient.init();
                available = true;
                log.info("Successfully re-connected to Odoo!");
            } catch (Exception e) {
                log.warn("Re-connection to Odoo failed: {}", e.getMessage());
            }
        }
    }

    @Override
    public Integer create(String model, List<Map<String, Object>> dataParams) {
        checkConnection();
        if (!available) {
            log.warn("Odoo is not available. Skipping create operation for model: {}", model);
            return null;
        }
        return odooClient.create(model, dataParams);
    }

    @Override
    public Object[] searchAndRead(String model, List<Object> criteria, List<String> fields) {
        checkConnection();
        if (!available) {
            log.warn("Odoo is not available. Skipping searchAndRead operation for model: {}", model);
            return new Object[0];
        }
        return odooClient.searchAndRead(model, criteria, fields);
    }
}
