package org.openelisglobal.configuration.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
        try {
            storeAll(byDomain);
            for (Map.Entry<String, List<MultipartFile>> entry : byDomain.entrySet()) {
                for (MultipartFile file : entry.getValue()) {
                    plans.add(applyFile(entry.getKey(), fileNameOf(file)));
                }
            }
            failed = plans.stream().anyMatch(plan -> plan.error() != null);
        } catch (RuntimeException e) {
            failed = true;
            LogEvent.logError(CLASS_NAME, "apply", e.getMessage());
            throw e;
        } finally {
            importRunService.finish(run, summarize(plans), failed);
        }
        return new ImportPlan(run.getId(), plans, unresolvedReferenceService.getOpen().size());
    }

    /**
     * Loads one stored file through the configuration tree, on its own, and reports
     * what became of it. The reload names every file it saw, so a file the loader
     * never found, skipped or rejected comes back as an error instead of a row of
     * zero counts, and reading one file at a time keeps each file's counts apart
     * from those of another file in the same domain.
     */
    private FilePlan applyFile(String domain, String fileName) {
        ConfigurationReloadResult result = initializationService
                .reload(ConfigurationReloadOptions.forFiles(Set.of(domain), Set.of(fileName)));
        ConfigurationReloadFileResult outcome = outcomeOf(result, domain, fileName);
        if (outcome == null) {
            return failedPlan(domain, fileName,
                    "the " + domain + " loader did not find " + fileName + " in " + directoryFor(domain));
        }
        if (outcome.status() == ConfigurationReloadFileResult.Status.ERROR) {
            return failedPlan(domain, fileName, outcome.errorMessage());
        }
        if (outcome.status() == ConfigurationReloadFileResult.Status.SKIPPED) {
            return failedPlan(domain, fileName, "not loaded: " + outcome.skippedReason());
        }
        CsvLoadSummary summary = summaryOf(domain, fileName);
        if (summary == null) {
            return failedPlan(domain, fileName,
                    "the " + domain + " loader read " + fileName + " but reported no row counts");
        }
        return toPlan(summary, null);
    }

    /**
     * The reload's verdict on one file, or the domain-wide failure that stood in
     * for it when the loader could not even list the directory.
     */
    private static ConfigurationReloadFileResult outcomeOf(ConfigurationReloadResult result, String domain,
            String fileName) {
        return result.files().stream().filter(file -> domain.equals(file.domain()))
                .filter(file -> fileName.equals(file.fileName()) || file.fileName() == null)
                .min(Comparator.comparing(file -> file.fileName() == null)).orElse(null);
    }

    private CsvLoadSummary summaryOf(String domain, String fileName) {
        return domainHandlers.stream().filter(handler -> handler.getDomainName().equals(domain))
                .map(DomainConfigurationHandler::getLastSummary)
                .filter(summary -> summary != null && fileName.equals(summary.getFileName())).findFirst().orElse(null);
    }

    private static FilePlan failedPlan(String domain, String fileName, String error) {
        return new FilePlan(domain, fileName, 0, 0, 0, List.of(), error);
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

    /**
     * Copies every upload into the configuration tree before anything is loaded, so
     * a batch is kept whole or not at all: when one copy fails, the files this
     * batch added are removed again and the failure names the file and the
     * directory the webapp could not write to. A file that replaced an earlier
     * version stays replaced.
     */
    private void storeAll(Map<String, List<MultipartFile>> byDomain) {
        List<Path> added = new ArrayList<>();
        for (Map.Entry<String, List<MultipartFile>> entry : byDomain.entrySet()) {
            for (MultipartFile file : entry.getValue()) {
                Path target = directoryFor(entry.getKey()).resolve(fileNameOf(file));
                boolean replacing = Files.exists(target);
                try {
                    Files.createDirectories(target.getParent());
                    Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    discard(added);
                    throw new IllegalStateException(
                            "Could not save " + fileNameOf(file) + " to " + target.getParent() + ": " + describe(e), e);
                }
                if (!replacing) {
                    added.add(target);
                }
            }
        }
    }

    private static void discard(List<Path> added) {
        for (Path path : added) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                LogEvent.logWarn(CLASS_NAME, "discard", "Could not remove " + path + ": " + e.getMessage());
            }
        }
    }

    private static String describe(IOException e) {
        if (e instanceof AccessDeniedException) {
            return "permission denied; the configuration directory must be writable by the webapp's user";
        }
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private Path directoryFor(String domain) {
        Path directory = Paths.get(configurationBaseDir, domain);
        if (instanceId != null && !instanceId.isBlank()) {
            directory = directory.resolve(instanceId);
        }
        return directory;
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
