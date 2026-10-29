package org.openelisglobal.odoo.client;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RealOdooClient implements OdooConnection {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_INITIAL_DELAY_MS = 1000;
    private static final long RETRY_JITTER_MAX_MS = 1000;

    private final OdooClient odooClient;
    private boolean available = false;

    public RealOdooClient(OdooClient odooClient) {
        this.odooClient = odooClient;
        this.available = connectToOdoo();
    }

    private boolean connectToOdoo() {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                odooClient.init();
                log.info("Connected to Odoo on attempt {}/{}", attempt, MAX_RETRIES);
                return true;
            } catch (Exception e) {
                log.error("Attempt {}/{} failed to connect to Odoo: {}",
                        attempt, MAX_RETRIES, e.getMessage(), e);

                if (attempt < MAX_RETRIES) {
                    // Exponential backoff: 1s, 2s, 4s... with random jitter
                    // to prevent thundering herd on simultaneous reconnections
                    long delay = (long) (RETRY_INITIAL_DELAY_MS * Math.pow(2, attempt - 1));
                    long jitter = (long) (Math.random() * RETRY_JITTER_MAX_MS);
                    long totalDelay = delay + jitter;

                    log.info("Retrying in {}ms... ({}/{})", totalDelay, attempt, MAX_RETRIES);
                    try {
                        Thread.sleep(totalDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Connection retry interrupted on attempt {}/{}", attempt, MAX_RETRIES);
                        return false;
                    }
                }
            }
        }
        log.error("All {} attempts to connect to Odoo failed.", MAX_RETRIES);
        return false;
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