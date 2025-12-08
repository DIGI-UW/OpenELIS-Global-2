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
            log.error("Failed to connect to Odoo at startup: {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public Integer create(String model, List<Map<String, Object>> dataParams) {
        if (!available)
            throw new IllegalStateException("Odoo is not available");
        return odooClient.create(model, dataParams);
    }

    @Override
    public Object[] searchAndRead(String model, List<Object> criteria, List<String> fields) {
        if (!available)
            throw new IllegalStateException("Odoo is not available");
        return odooClient.searchAndRead(model, criteria, fields);
    }
}
