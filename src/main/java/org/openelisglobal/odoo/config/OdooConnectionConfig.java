package org.openelisglobal.odoo.config;

import lombok.extern.slf4j.Slf4j;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.odoo.client.NoOpOdooClient;
import org.openelisglobal.odoo.client.OdooClient;
import org.openelisglobal.odoo.client.OdooConnection;
import org.openelisglobal.odoo.client.RealOdooClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@SuppressWarnings("unused")
public class OdooConnectionConfig {

    @Bean
    public OdooConnection odooConnection(OdooClient odooClient) {
        // Check if Odoo integration is enabled in configuration
        boolean odooEnabled = ConfigurationProperties.getInstance()
                .isPropertyValueEqual(Property.ENABLE_OPENELIS_TO_ODOO_CONNECTION, "true");

        if (!odooEnabled) {
            log.info("Odoo integration is disabled in configuration. Using NoOpOdooClient.");
            return new NoOpOdooClient();
        }

        try {
            log.info("Odoo integration is enabled. Attempting to connect to Odoo...");
            odooClient.init();
            log.info("Successfully connected to Odoo.");
            return new RealOdooClient(odooClient);
        } catch (Exception e) {
            log.error("Failed to connect to Odoo at startup: {}", e.getMessage());
            log.warn("Falling back to NoOpOdooClient. Odoo operations will be skipped.");
            return new NoOpOdooClient();
        }
    }
}
