package org.openelisglobal.configuration.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
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

    private static final String CLASS_NAME = "ConfigurationInitializationService";

    @Value("${org.openelisglobal.configuration.dir:/var/lib/openelis-global/configuration/backend}")
    private String configurationBaseDir;

    @Value("${org.openelisglobal.configuration.autocreate:true}")
    private boolean autocreateOn;

    /**
     * Identifies this OpenELIS instance for loading instance-specific
     * configuration. When set, the system looks for files in a subdirectory named
     * after this ID (e.g., {@code configuration/tests/{instanceId}/}) and, if
     * found, uses those instead of the base domain files.
     *
     * <p>
     * Maps from env var {@code OPENELIS_CONFIGURATION_INSTANCE_ID} via Spring's
     * relaxed binding.
     */
    @Value("${org.openelisglobal.configuration.instance-id:#{null}}")
    private String instanceId;

    private List<DomainConfigurationHandler> domainHandlers;

    private final PathMatchingResourcePatternResolver resolver;

    private volatile boolean initialized = false;

    ConfigurationInitializationService() {
        this(new PathMatchingResourcePatternResolver());
    }

    ConfigurationInitializationService(PathMatchingResourcePatternResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (initialized) {
            return;
        }
        initialized = true;

        if (!autocreateOn) {
            LogEvent.logInfo(CLASS_NAME, "onApplicationEvent",
                    "Configuration auto-initialization is disabled. Skipping configuration loading.");
            return;
        }

        if (domainHandlers == null || domainHandlers.isEmpty()) {
            LogEvent.logInfo(CLASS_NAME, "onApplicationEvent",
                    "No domain configuration handlers found. Skipping configuration loading.");
            return;
        }

        LogEvent.logInfo(CLASS_NAME, "onApplicationEvent",
                "Starting configuration initialization from " + configurationBaseDir + "...");

        if (instanceId != null && !instanceId.isBlank()) {
            LogEvent.logInfo(CLASS_NAME, "onApplicationEvent", "Instance ID is set to '" + instanceId
                    + "'. Instance-specific configurations will be preferred when available.");
        }

        try {
            LogEvent.logInfo(CLASS_NAME, "onApplicationEvent",
                    "Loading configuration handlers in order: "
                            + domainHandlers.stream().map(h -> h.getDomainName() + "(" + h.getLoadOrder() + ")")
                                    .collect(Collectors.joining(", ")));

            // Tracks files already claimed by a more-specific handler so that
            // broader-pattern handlers in the same domain skip them.
            Set<String> claimedFiles = new HashSet<>();

            for (DomainConfigurationHandler handler : domainHandlers) {
                try {
                    loadDomainConfiguration(handler, claimedFiles);
                } catch (Exception e) {
                    LogEvent.logError("Failed to load configuration for domain: " + handler.getDomainName(), e);
                }
            }
        } catch (Exception e) {
            LogEvent.logError("Error occurred while processing domains", e);
        }
    }

    private void loadDomainConfiguration(DomainConfigurationHandler handler, Set<String> claimedFiles)
            throws Exception {
        String domainName = handler.getDomainName();
        String fileMatcher = handler.getFileMatcher();
        String checksumsFile = configurationBaseDir + "/" + domainName + "-checksums.properties";
        Properties checksums = loadChecksums(checksumsFile);

        // When an instance ID is configured, try instance-specific paths first.
        // If any instance files are found (classpath or filesystem), use only those
        // and skip the base domain files entirely.
        if (instanceId != null && !instanceId.isBlank()) {
            String instanceSubPath = domainName + "/" + instanceId + "/" + fileMatcher;
            Map<String, InputStreamSource> instanceFiles = collectFiles(
                    "file:" + configurationBaseDir + "/" + instanceSubPath,
                    "classpath*:configuration/" + instanceSubPath);

            if (!instanceFiles.isEmpty()) {
                LoadResult result = processFiles(handler, instanceFiles, checksums, domainName, claimedFiles);
                if (result.checksumsUpdated()) {
                    saveChecksums(checksums, checksumsFile);
                }
                LogEvent.logInfo(CLASS_NAME, "loadDomainConfiguration",
                        "Using instance-specific configuration for domain: " + domainName + " (instance: " + instanceId
                                + ")");
                return;
            }

            LogEvent.logInfo(CLASS_NAME, "loadDomainConfiguration",
                    "No instance-specific configuration found for domain: " + domainName + " (instance: " + instanceId
                            + "). Falling back to base configuration.");
        }

        // Base behavior: load from filesystem then classpath
        String baseSubPath = domainName + "/" + fileMatcher;
        Map<String, InputStreamSource> baseFiles = collectFiles("file:" + configurationBaseDir + "/" + baseSubPath,
                "classpath*:configuration/" + baseSubPath);

        LoadResult result = processFiles(handler, baseFiles, checksums, domainName, claimedFiles);
        if (result.checksumsUpdated()) {
            saveChecksums(checksums, checksumsFile);
        }
    }

    /**
     * Collects configuration files from filesystem first, falling back to
     * classpath. The two sources are never merged — configuration is loaded from
     * exactly one place. Both patterns are resolved through
     * {@link PathMatchingResourcePatternResolver}, which supports {@code file:} and
     * {@code classpath*:} prefixes with Ant-style globs.
     *
     * <p>
     * Stream sources are used instead of buffering file contents so that large
     * files are not held entirely in memory. Each consumer (checksum calculation,
     * handler processing) opens its own stream.
     */
    private Map<String, InputStreamSource> collectFiles(String filesystemPattern, String classpathPattern) {
        // Filesystem files take precedence — if any exist, use only those
        Map<String, InputStreamSource> fsFiles = resolveResources(filesystemPattern);
        if (!fsFiles.isEmpty()) {
            return fsFiles;
        }

        // Fall back to classpath resources
        return resolveResources(classpathPattern);
    }

    private Map<String, InputStreamSource> resolveResources(String locationPattern) {
        Map<String, InputStreamSource> files = new LinkedHashMap<>();
        try {
            Resource[] resources = resolver.getResources(locationPattern);
            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                if (fileName == null) {
                    continue;
                }
                files.put(fileName, resource::getInputStream);
            }
        } catch (IOException e) {
            LogEvent.logError("Failed to resolve resource pattern: " + locationPattern, e);
        }
        return files;
    }

    /**
     * Processes collected files through the handler, skipping any whose checksum
     * matches the previously stored value. Each file is read twice via its stream
     * source: once for checksum calculation, once for handler processing. This
     * avoids buffering entire file contents in memory.
     *
     * <p>
     * Files already present in {@code claimedFiles} (claimed by a handler with
     * lower load order) are skipped. Every file this handler processes is added to
     * the set so that later, broader-pattern handlers won't reprocess it.
     */
    private LoadResult processFiles(DomainConfigurationHandler handler, Map<String, InputStreamSource> files,
            Properties checksums, String domainName, Set<String> claimedFiles) {
        boolean filesFound = false;
        boolean checksumsUpdated = false;

        for (Map.Entry<String, InputStreamSource> entry : files.entrySet()) {
            String fileName = entry.getKey();
            InputStreamSource streamSource = entry.getValue();
            filesFound = true;

            // Skip files already claimed by a more-specific handler
            String fileKey = domainName + "/" + fileName;
            if (!claimedFiles.add(fileKey)) {
                LogEvent.logDebug(CLASS_NAME, "processFiles", "Handler " + handler.getClass().getSimpleName()
                        + " skipping already-claimed file: " + fileName);
                continue;
            }

            try {
                // Check if this file has been loaded with the same checksum
                String currentChecksum;
                try (InputStream is = streamSource.open()) {
                    currentChecksum = calculateChecksum(is);
                }

                String storedChecksum = checksums.getProperty(fileName);
                if (currentChecksum.equals(storedChecksum)) {
                    LogEvent.logInfo(CLASS_NAME, "loadDomainConfiguration",
                            domainName + " configuration " + fileName + " unchanged (checksum matches). Skipping.");
                    continue;
                }

                // Load and process the configuration
                try (InputStream is = streamSource.open()) {
                    handler.processConfiguration(is, fileName);
                }

                // Update checksum
                checksums.setProperty(fileName, currentChecksum);
                checksumsUpdated = true;

                LogEvent.logInfo(CLASS_NAME, "loadDomainConfiguration",
                        "Successfully loaded " + domainName + " configuration: " + fileName);
            } catch (Exception e) {
                LogEvent.logError("Failed to load " + domainName + " configuration from file: " + fileName, e);
            }
        }

        return new LoadResult(filesFound, checksumsUpdated);
    }

    private static String calculateChecksum(InputStream inputStream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by the JVM specification; this cannot happen
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
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

    /**
     * Used internally to load {@code InputStream}s for files that have been
     * identified
     */
    @FunctionalInterface
    interface InputStreamSource {
        InputStream open() throws IOException;
    }

    // Sort handlers by load order to ensure dependencies are loaded first
    @Autowired(required = false)
    void setDomainHandlers(List<DomainConfigurationHandler> domainHandlers) {
        if (domainHandlers != null && !domainHandlers.isEmpty()) {
            this.domainHandlers = domainHandlers.stream()
                    .sorted(Comparator.comparingInt(DomainConfigurationHandler::getLoadOrder))
                    .collect(Collectors.toList());
        } else {
            this.domainHandlers = domainHandlers;
        }
    }

    /**
     * This is used to hold the result of attempting to load a file
     *
     * @param filesFound       Whether files were found (used so that classpath
     *                         overrides filesystem)
     * @param checksumsUpdated Whether checksums were updated so these can be
     *                         flushed to disk
     */
    private record LoadResult(boolean filesFound, boolean checksumsUpdated) {
    }
}
