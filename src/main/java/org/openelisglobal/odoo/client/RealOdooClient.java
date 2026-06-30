package org.openelisglobal.odoo.client;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RealOdooClient implements OdooConnection {

    private final OdooClient odooClient;

    private volatile boolean available = false;

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
        if (!available) {
            try {
                odooClient.init();
                available = true;
                log.info("Odoo connection restored.");
            } catch (Exception e) {
                log.debug("Odoo still unavailable: {}", e.getMessage());
                available = false;
            }
        }
        return available;
    }

    @Override
    public Integer create(String model, List<Map<String, Object>> dataParams) {
        if (!isAvailable())
            throw new IllegalStateException("Odoo is not available");
        try {
            return odooClient.create(model, dataParams);
        } catch (Exception e) {
            available = false;
            log.error("Odoo create call failed, marking unavailable: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Object[] searchAndRead(String model, List<Object> criteria, List<String> fields) {
        if (!isAvailable())
            throw new IllegalStateException("Odoo is not available");
        try {
            return odooClient.searchAndRead(model, criteria, fields);
        } catch (Exception e) {
            available = false;
            log.error("Odoo searchAndRead call failed, marking unavailable: {}", e.getMessage(), e);
            throw e;
        }
    }
}
