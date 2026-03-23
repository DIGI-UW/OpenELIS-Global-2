package org.openelisglobal.odoo.config;

import lombok.extern.slf4j.Slf4j;
import org.openelisglobal.config.condition.ConditionalOnProperty;
import org.openelisglobal.odoo.client.NoOpOdooClient;
import org.openelisglobal.odoo.client.OdooClient;
import org.openelisglobal.odoo.client.OdooConnection;
import org.openelisglobal.odoo.client.RealOdooClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Uses custom @ConditionalOnProperty to enable/disable Odoo based on
 * application.properties.
 */
@Slf4j
@Configuration
@SuppressWarnings("unused")
public class OdooConnectionConfig {

    /**
     * Creates a real Odoo connection when org.openelisglobal.odoo.enabled=true
     */
    @Bean
    @ConditionalOnProperty(property = "org.openelisglobal.odoo.enabled", havingValue = "true")
    public OdooConnection realOdooConnection(OdooClient odooClient) {
        log.info("Odoo integration is ENABLED!");
        try {
            odooClient.init();
            log.info("Successfully connected to Odoo.");
            return new RealOdooClient(odooClient);
        } catch (Exception e) {
            log.error("Failed to connect to Odoo at startup: {}", e.getMessage());
            log.warn("Falling back to NoOpOdooClient. Odoo operations will be skipped.");
            return new NoOpOdooClient();
        }
    }

    /**
     * Creates a no-op Odoo connection when org.openelisglobal.odoo.enabled is false
     * or missing
     */
    @Bean
    @ConditionalOnProperty(property = "org.openelisglobal.odoo.enabled", havingValue = "false", matchIfMissing = false)
    public OdooConnection noOpOdooConnection() {
        log.info("Odoo integration is DISABLED (org.openelisglobal.odoo.enabled=false or not set)");
        return new NoOpOdooClient();
    }
}
