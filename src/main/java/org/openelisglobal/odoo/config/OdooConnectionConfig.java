package org.openelisglobal.odoo.config;

import lombok.extern.slf4j.Slf4j;
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
        try {
            odooClient.init();
            return new RealOdooClient(odooClient);
        } catch (Exception e) {
            log.error("Failed to connect to Odoo at startup: {}", e.getMessage());
            return new NoOpOdooClient();
        }
    }
}
