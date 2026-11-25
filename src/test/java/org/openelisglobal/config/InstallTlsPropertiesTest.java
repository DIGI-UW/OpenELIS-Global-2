package org.openelisglobal.config;

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import org.junit.Test;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.env.MockEnvironment;

public class InstallTlsPropertiesTest {
    @Test
    public void springConsumersResolveTheSameDefaultsAndCustomPasswordsAsGeneration() throws Exception {
        assertPasswords(Path.of("volume/properties/common.properties"));
    }

    @Test
    public void harnessEnvironmentOverridesPasswordsInAnOlderBootstrappedPropertiesFile() {
        Properties legacy = new Properties();
        legacy.setProperty("server.ssl.key-store-password", "kspass");
        legacy.setProperty("server.ssl.key-password", "kspass");
        legacy.setProperty("server.ssl.trust-store-password", "tspass");
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources()
                .addFirst(new SystemEnvironmentPropertySource("harness",
                        Map.of("SERVER_SSL_KEY_STORE_PASSWORD", "custom-key-password", "SERVER_SSL_KEY_PASSWORD",
                                "custom-key-password", "SERVER_SSL_TRUST_STORE_PASSWORD", "custom-trust-password")));
        environment.getPropertySources().addLast(new PropertiesPropertySource("legacy", legacy));
        assertEquals("custom-key-password", environment.getProperty("server.ssl.key-store-password"));
        assertEquals("custom-key-password", environment.getProperty("server.ssl.key-password"));
        assertEquals("custom-trust-password", environment.getProperty("server.ssl.trust-store-password"));
    }

    private void assertPasswords(Path path) throws Exception {
        Properties properties = new Properties();
        try (var input = Files.newInputStream(path)) {
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
