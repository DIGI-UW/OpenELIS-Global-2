package org.openelisglobal.config;

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.Test;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.mock.env.MockEnvironment;

public class InstallTlsPropertiesTest {
    @Test
    public void springConsumersResolveTheSameDefaultsAndCustomPasswordsAsGeneration() throws Exception {
        Properties properties = new Properties();
        try (var input = Files.newInputStream(Path.of("volume/properties/common.properties"))) {
            properties.load(input);
        }
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addLast(new PropertiesPropertySource("install", properties));
        assertEquals("kspass", environment.getProperty("server.ssl.key-store-password"));
        assertEquals("kspass", environment.getProperty("server.ssl.key-password"));
        assertEquals("tspass", environment.getProperty("server.ssl.trust-store-password"));
        environment.setProperty("SSL_KEYSTORE_PASSWORD", "custom-key-password");
        environment.setProperty("SSL_TRUSTSTORE_PASSWORD", "custom-trust-password");
        assertEquals("custom-key-password", environment.getProperty("server.ssl.key-store-password"));
        assertEquals("custom-key-password", environment.getProperty("server.ssl.key-password"));
        assertEquals("custom-trust-password", environment.getProperty("server.ssl.trust-store-password"));
    }
}
