package org.openelisglobal.configuration.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.valueholder.ConfigurationImportRun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CatalogImportServiceImpl implements CatalogImportService {

    private static final String CLASS_NAME = "CatalogImportServiceImpl";

    @Value("${org.openelisglobal.configuration.dir:/var/lib/openelis-global/configuration/backend}")
    private String configurationBaseDir;

    @Value("${org.openelisglobal.configuration.instance-id:#{null}}")
    private String instanceId;

    @Autowired
    private List<DomainConfigurationHandler> domainHandlers;

    @Autowired
    private ConfigurationInitializationService initializationService;

    @Autowired
    private ConfigurationImportRunService importRunService;

    @Autowired
    private UnresolvedReferenceService unresolvedReferenceService;

    @Override
    public List<String> getImportableDomains() {
        Set<String> domains = new LinkedHashSet<>();
        domainHandlers.stream().filter(DomainConfigurationHandler::supportsDryRun)
                .sorted(Comparator.comparingInt(DomainConfigurationHandler::getLoadOrder))
                .forEach(handler -> domains.add(handler.getDomainName()));
        return new ArrayList<>(domains);
    }

    @Override
    public ImportPlan preview(List<MultipartFile> files, List<String> domains, String sysUserId) {
        Map<String, List<MultipartFile>> byDomain = groupByDomain(files, domains);
        ConfigurationImportRun run = importRunService.start(ConfigurationImportRun.SOURCE_PREVIEW, sysUserId);
        List<FilePlan> plans = new ArrayList<>();
        boolean failed = false;
        try {
            for (Map.Entry<String, List<MultipartFile>> entry : byDomain.entrySet()) {
                for (MultipartFile file : entry.getValue()) {
                    plans.add(previewFile(entry.getKey(), file));
                }
            }
            failed = plans.stream().anyMatch(plan -> plan.error() != null);
        } finally {
            importRunService.finish(run, summarize(plans), failed);
        }
        return new ImportPlan(run.getId(), plans, unresolvedReferenceService.getOpen().size());
    }

    @Override
    public ImportPlan apply(List<MultipartFile> files, List<String> domains, String sysUserId) {
        Map<String, List<MultipartFile>> byDomain = groupByDomain(files, domains);
        ConfigurationImportRun run = importRunService.start(ConfigurationImportRun.SOURCE_APPLY, sysUserId);
        List<FilePlan> plans = new ArrayList<>();
        boolean failed = false;
        Set<String> uploaded = new LinkedHashSet<>();
        try {
            for (Map.Entry<String, List<MultipartFile>> entry : byDomain.entrySet()) {
                for (MultipartFile file : entry.getValue()) {
                    store(entry.getKey(), file);
                    uploaded.add(fileNameOf(file));
                }
            }
            initializationService.reload(ConfigurationReloadOptions.forFiles(byDomain.keySet(), uploaded));
            for (Map.Entry<String, List<MultipartFile>> entry : byDomain.entrySet()) {
                DomainConfigurationHandler handler = handlerFor(entry.getKey());
                CsvLoadSummary summary = handler == null ? null : handler.getLastSummary();
                for (MultipartFile file : entry.getValue()) {
                    String fileName = fileNameOf(file);
                    plans.add(summary != null && fileName.equals(summary.getFileName()) ? toPlan(summary, null)
                            : new FilePlan(entry.getKey(), fileName, 0, 0, 0, List.of(), null));
                }
            }
        } catch (RuntimeException e) {
            failed = true;
            LogEvent.logError(CLASS_NAME, "apply", e.getMessage());
            throw e;
        } finally {
            importRunService.finish(run, summarize(plans), failed);
        }
        return new ImportPlan(run.getId(), plans, unresolvedReferenceService.getOpen().size());
    }

    private FilePlan previewFile(String domain, MultipartFile file) {
        String fileName = fileNameOf(file);
        DomainConfigurationHandler handler = handlerFor(domain);
        if (handler == null) {
            return new FilePlan(domain, fileName, 0, 0, 0, List.of(), "no loader for domain '" + domain + "'");
        }
        if (!handler.supportsDryRun()) {
            return new FilePlan(domain, fileName, 0, 0, 0, List.of(),
                    "the " + domain + " loader cannot preview a file");
        }
        try (InputStream in = file.getInputStream()) {
            handler.processConfiguration(in, fileName, true);
            return toPlan(handler.getLastSummary(), null);
        } catch (Exception e) {
            return new FilePlan(domain, fileName, 0, 0, 0, List.of(), CsvLoadSummary.reason(e));
        }
    }

    private FilePlan toPlan(CsvLoadSummary summary, String error) {
        List<RowPlan> rows = new ArrayList<>();
        for (CsvLoadSummary.RowOutcome row : summary.getRows()) {
            rows.add(new RowPlan(row.lineNumber(), row.outcome().name(), row.reason()));
        }
        return new FilePlan(summary.getDomain(), summary.getFileName(), summary.getCreated(), summary.getUpdated(),
                summary.getSkipped(), rows, error);
    }

    /**
     * The files of each domain, in the order the loaders run, so a preview or an
     * apply sees tests before the attributes that point at them.
     */
    private Map<String, List<MultipartFile>> groupByDomain(List<MultipartFile> files, List<String> domains) {
        Map<String, List<MultipartFile>> byDomain = new LinkedHashMap<>();
        List<String> importable = getImportableDomains();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String declared = domains != null && i < domains.size() ? domains.get(i) : null;
            String domain = declared != null && !declared.isBlank() ? declared.trim()
                    : inferDomain(fileNameOf(file), importable);
            if (domain == null) {
                throw new IllegalArgumentException("Cannot tell which catalog domain '" + fileNameOf(file)
                        + "' belongs to; name the file after its domain or choose one");
            }
            if (!importable.contains(domain)) {
                throw new IllegalArgumentException("'" + domain + "' is not a catalog domain that can be imported");
            }
            byDomain.computeIfAbsent(domain, k -> new ArrayList<>()).add(file);
        }
        Map<String, List<MultipartFile>> ordered = new LinkedHashMap<>();
        for (String domain : importable) {
            if (byDomain.containsKey(domain)) {
                ordered.put(domain, byDomain.get(domain));
            }
        }
        return ordered;
    }

    /**
     * The domain a file name starts with; the longest match wins (test-results
     * before tests).
     */
    private static String inferDomain(String fileName, List<String> importable) {
        String name = fileName.toLowerCase(Locale.ROOT);
        String best = null;
        for (String domain : importable) {
            if (name.startsWith(domain) && (best == null || domain.length() > best.length())) {
                best = domain;
            }
        }
        return best;
    }

    private DomainConfigurationHandler handlerFor(String domain) {
        return domainHandlers.stream().filter(handler -> handler.getDomainName().equals(domain))
                .min(Comparator.comparingInt(DomainConfigurationHandler::getLoadOrder)).orElse(null);
    }

    private void store(String domain, MultipartFile file) {
        Path directory = Paths.get(configurationBaseDir, domain);
        if (instanceId != null && !instanceId.isBlank()) {
            directory = directory.resolve(instanceId);
        }
        try {
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), directory.resolve(fileNameOf(file)),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not save " + fileNameOf(file) + " to " + directory + ": " + e.getMessage(), e);
        }
    }

    /** The upload's own name, without any path a browser may have sent. */
    private static String fileNameOf(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            throw new IllegalArgumentException("Every uploaded catalog file needs a name");
        }
        String name = Paths.get(original.replace('\\', '/')).getFileName().toString();
        if (!name.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException("Catalog files are CSV; '" + name + "' is not");
        }
        return name;
    }

    private static String summarize(List<FilePlan> plans) {
        StringBuilder text = new StringBuilder();
        for (FilePlan plan : plans) {
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append(plan.domain()).append('/').append(plan.fileName()).append(": created=").append(plan.created())
                    .append(" updated=").append(plan.updated()).append(" skipped=").append(plan.skipped());
            if (plan.error() != null) {
                text.append(" error=").append(plan.error());
            }
        }
        return text.toString();
    }
}
