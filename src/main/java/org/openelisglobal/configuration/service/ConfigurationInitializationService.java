package org.openelisglobal.configuration.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Properties;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationInitializationService implements ApplicationListener<ContextRefreshedEvent> {

    @Value("${org.openelisglobal.configuration.dir:/var/lib/openelis-global/configuration/backend}")
    private String configurationBaseDir;

    @Value("${org.openelisglobal.configuration.autocreate:true}")
    private boolean autocreateOn;

    @Autowired(required = false)
    private List<DomainConfigurationHandler> domainHandlers;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!autocreateOn) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "onApplicationEvent",
                    "Configuration auto-initialization is disabled. Skipping configuration loading.");
            return;
        }

        if (domainHandlers == null || domainHandlers.isEmpty()) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "onApplicationEvent",
                    "No domain configuration handlers found. Skipping configuration loading.");
            return;
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "onApplicationEvent",
                "Starting configuration initialization from " + configurationBaseDir + "...");

        try {
            for (DomainConfigurationHandler handler : domainHandlers) {
                try {
                    loadDomainConfiguration(handler);
                } catch (Exception e) {
                    LogEvent.logError("Failed to load configuration for domain: " + handler.getDomainName(), e);
                }
            }
        } catch (Exception e) {
            LogEvent.logError(e);
        }
    }

    private void loadDomainConfiguration(DomainConfigurationHandler handler) throws Exception {
        String domainName = handler.getDomainName();
        String checksumsFile = configurationBaseDir + "/" + domainName + "-checksums.properties";
        String classpathPattern = "classpath*:configuration/" + domainName + "/*." + handler.getFileExtension();
        String filesystemDir = configurationBaseDir + "/" + domainName;

        Properties checksums = loadChecksums(checksumsFile);
        boolean checksumsUpdated = false;

        // Load from classpath
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(classpathPattern);

        for (Resource resource : resources) {
            try {
                String fileName = resource.getFilename();
                if (fileName == null) {
                    continue;
                }

                InputStream inputStream = resource.getInputStream();
                String currentChecksum = calculateChecksum(inputStream);
                inputStream.close();

                // Check if this file has been loaded with the same checksum
                String storedChecksum = checksums.getProperty(fileName);
                if (currentChecksum.equals(storedChecksum)) {
                    LogEvent.logInfo(this.getClass().getSimpleName(), "loadDomainConfiguration",
                            domainName + " configuration " + fileName + " unchanged (checksum matches). Skipping.");
                    continue;
                }

                // Load and process the configuration
                inputStream = resource.getInputStream();
                handler.processConfiguration(inputStream, fileName);
                inputStream.close();

                // Update checksum
                checksums.setProperty(fileName, currentChecksum);
                checksumsUpdated = true;

                LogEvent.logInfo(this.getClass().getSimpleName(), "loadDomainConfiguration",
                        "Successfully loaded " + domainName + " configuration: " + fileName);

            } catch (Exception e) {
                LogEvent.logError(
                        "Failed to load " + domainName + " configuration from resource: " + resource.getFilename(), e);
            }
        }

        // Load from filesystem directory
        Path configDir = Paths.get(filesystemDir);
        if (Files.exists(configDir) && Files.isDirectory(configDir)) {
            String extension = handler.getFileExtension();
            File[] files = configDir.toFile().listFiles((dir, name) -> name.toLowerCase().endsWith("." + extension));
            if (files != null) {
                for (File file : files) {
                    try {
                        String fileName = file.getName();
                        String currentChecksum = calculateChecksum(new FileInputStream(file));

                        // Check if this file has been loaded with the same checksum
                        String storedChecksum = checksums.getProperty(fileName);
                        if (currentChecksum.equals(storedChecksum)) {
                            LogEvent.logInfo(this.getClass().getSimpleName(), "loadDomainConfiguration", domainName
                                    + " configuration " + fileName + " unchanged (checksum matches). Skipping.");
                            continue;
                        }

                        // Load and process the configuration
                        handler.processConfiguration(new FileInputStream(file), fileName);

                        // Update checksum
                        checksums.setProperty(fileName, currentChecksum);
                        checksumsUpdated = true;

                        LogEvent.logInfo(this.getClass().getSimpleName(), "loadDomainConfiguration",
                                "Successfully loaded " + domainName + " configuration: " + fileName);

                    } catch (Exception e) {
                        LogEvent.logError(
                                "Failed to load " + domainName + " configuration from file: " + file.getName(), e);
                    }
                }
            }
        }

        // Save updated checksums if any were updated
        if (checksumsUpdated) {
            saveChecksums(checksums, checksumsFile);
        }
    }

    private String calculateChecksum(InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
        }
        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private Properties loadChecksums(String checksumsFile) {
        Properties checksums = new Properties();
        File checksumFile = new File(checksumsFile);
        if (checksumFile.exists()) {
            try (FileInputStream fis = new FileInputStream(checksumFile)) {
                checksums.load(fis);
            } catch (IOException e) {
                LogEvent.logError("Failed to load checksums file: " + checksumsFile, e);
            }
        } else {
            // Create directory if it doesn't exist
            checksumFile.getParentFile().mkdirs();
        }
        return checksums;
    }

    private void saveChecksums(Properties checksums, String checksumsFile) {
        File checksumFile = new File(checksumsFile);
        try {
            checksumFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(checksumFile)) {
                checksums.store(writer, "Configuration file checksums - automatically generated");
            }
        } catch (IOException e) {
            LogEvent.logError("Failed to save checksums file: " + checksumsFile, e);
        }
    }
}
