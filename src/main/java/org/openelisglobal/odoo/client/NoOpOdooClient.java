package org.openelisglobal.odoo.client;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoOpOdooClient implements OdooConnection {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public Integer create(String model, List<Map<String, Object>> dataParams) {
        log.warn("Odoo is not available. Skipping create operation for model: {}", model);
        return null;
    }

    @Override
    public Object[] searchAndRead(String model, List<Object> criteria, List<String> fields) {
        log.warn("Odoo is not available. Skipping searchAndRead operation for model: {}", model);
        return new Object[0];
    }
}
