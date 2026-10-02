package org.openelisglobal.organization.locations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.CsvParsingUtil;
import org.openelisglobal.configuration.service.ConfigurationImportRunService;
import org.openelisglobal.configuration.service.ConfigurationReloadFileResult;
import org.openelisglobal.configuration.service.ConfigurationReloadOptions;
import org.openelisglobal.configuration.service.ConfigurationReloadResult;
import org.openelisglobal.configuration.service.ConfigurationReloadService;
import org.openelisglobal.configuration.service.CsvLoadSummary;
import org.openelisglobal.configuration.service.CsvRow;
import org.openelisglobal.configuration.service.ImportRunContext;
import org.openelisglobal.configuration.service.LoadedRow;
import org.openelisglobal.configuration.service.ReferenceAliasService;
import org.openelisglobal.configuration.service.RowTransactionRunner;
import org.openelisglobal.configuration.valueholder.ConfigurationImportRun;
import org.openelisglobal.organization.locations.LocationsApi.ActiveRequest;
import org.openelisglobal.organization.locations.LocationsApi.Detail;
import org.openelisglobal.organization.locations.LocationsApi.Identifier;
import org.openelisglobal.organization.locations.LocationsApi.Query;
import org.openelisglobal.organization.locations.LocationsApi.Referral;
import org.openelisglobal.organization.locations.LocationsApi.Row;
import org.openelisglobal.organization.locations.LocationsApi.SaveRequest;
import org.openelisglobal.organization.locations.LocationsApi.Site;
import org.openelisglobal.organization.locations.LocationsApi.Usage;
import org.openelisglobal.organization.locations.LocationsApi.Ward;
import org.openelisglobal.organization.locations.LocationsApi.WardRequest;
import org.openelisglobal.organization.locations.LocationsImportApi.Candidate;
import org.openelisglobal.organization.locations.LocationsImportApi.Deactivation;
import org.openelisglobal.organization.locations.LocationsImportApi.Decision;
import org.openelisglobal.organization.locations.LocationsImportApi.Options;
import org.openelisglobal.organization.locations.LocationsImportApi.Plan;
import org.openelisglobal.organization.locations.LocationsImportApi.PlanRow;
import org.openelisglobal.organization.locations.LocationsImportApi.RecentRun;
import org.openelisglobal.organization.locations.LocationsImportApi.Scope;
import org.openelisglobal.organization.service.OrganizationChangeService;
import org.openelisglobal.organization.service.OrganizationChangeService.FieldChange;
import org.openelisglobal.organization.service.OrganizationIdentifierService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.service.OrganizationTypeService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.organization.valueholder.OrganizationIdentifier;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.openelisglobal.organization.valueholder.WardServiceType;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.vector.service.VectorSamplingSiteService;
import org.openelisglobal.vector.valueholder.VectorSamplingSite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocationsImportServiceImpl implements LocationsImportService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String ADDRESS_DOMAIN = "address-hierarchy";
    private static final String[] BASE_COLUMNS = { "type", "code", "name", "shortName", "parentCode", "parentName",
            "parentType", "active", "streetAddress", "city", "state", "zipCode", "gpsLatitude", "gpsLongitude",
            "contactName", "phone", "email", "internetAddress", "category", "ownership", "serviceType", "siteType",
            "subtype", "environmentalZone", "description", "approvalStatus", "accreditationBody", "accreditationNumber",
            "accreditationExpiry", "lastReviewDate", "nextReviewDue" };
    private static final String IDENTIFIER_PREFIX = "identifier:";

    @Value("${org.openelisglobal.configuration.dir:/var/lib/openelis-global/configuration/backend}")
    private String configurationBaseDir;

    @Value("${org.openelisglobal.configuration.instance-id:#{null}}")
    private String instanceId;

    @Autowired
    @Lazy
    private LocationsService locationsService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private OrganizationTypeService organizationTypeService;
    @Autowired
    private OrganizationIdentifierService identifierService;
    @Autowired
    private OrganizationChangeService changeService;
    @Autowired
    private ReferenceAliasService aliasService;
    @Autowired
    private ConfigurationImportRunService importRunService;
    @Autowired
    @Lazy
    // Injected by INTERFACE, not by the concrete class develop wrote here. On
    // this branch ConfigurationInitializationService implements
    // ConfigurationReloadService (so reload() can be gated on
    // PRIV_SYSTEM_CONFIGURE), which makes Spring wrap it in a JDK interface
    // proxy; a field typed as the concrete class then fails with "expected to
    // be of type ... but was actually of type jdk.proxy2.$Proxy", and every
    // import silently applied nothing (counts all 0).
    private ConfigurationReloadService initializationService;
    @Autowired
    @Lazy
    private OrganizationsConfigurationHandler organizationsHandler;
    @Autowired
    private VectorSamplingSiteService samplingSiteService;
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private LocationsUsageDAO usageDAO;

    // ---------------------------------------------------------------- preview and
    // apply

    @Override
    public Plan preview(List<MultipartFile> files, List<String> areas, Options options, String sysUserId) {
        List<Upload> uploads = uploads(files, areas);
        ConfigurationImportRun run = importRunService.start(ConfigurationImportRun.SOURCE_PREVIEW, sysUserId);
        List<Plan> plans = new ArrayList<>();
        boolean failed = false;
        try {
            LocationsImportContext.set(options);
            for (Upload upload : uploads) {
                plans.add(previewUpload(upload, options));
            }
            failed = plans.stream().anyMatch(plan -> !plan.errors().isEmpty());
        } finally {
            LocationsImportContext.clear();
            Plan merged = merge(run.getId(), options, plans);
            importRunService.finish(run, summarize(merged, uploads, false), failed);
        }
        return merge(run.getId(), options, plans);
    }

    @Override
    public Plan apply(List<MultipartFile> files, List<String> areas, Options options, String sysUserId) {
        List<Upload> uploads = uploads(files, areas);
        ConfigurationImportRun run = importRunService.start(ConfigurationImportRun.SOURCE_APPLY, sysUserId);
        List<Plan> plans = new ArrayList<>();
        boolean failed = false;
        try {
            LocationsImportContext.set(options);
            List<Upload> areaUploads = uploads.stream().filter(u -> !u.organizations()).collect(Collectors.toList());
            for (Upload upload : areaUploads) {
                plans.add(previewUpload(upload, options));
            }
            if (!areaUploads.isEmpty()) {
                Set<String> names = areaUploads.stream().map(Upload::fileName).collect(Collectors.toSet());
                storeAll(areaUploads);
                ConfigurationReloadResult result = initializationService
                        .reload(ConfigurationReloadOptions.forFiles(Set.of(ADDRESS_DOMAIN), names));
                for (ConfigurationReloadFileResult outcome : result.files()) {
                    if (outcome.status() == ConfigurationReloadFileResult.Status.ERROR) {
                        plans.add(errorPlan(outcome.fileName(), outcome.errorMessage()));
                    }
                }
            }
            List<Upload> organizationUploads = uploads.stream().filter(Upload::organizations)
                    .collect(Collectors.toList());
            if (!organizationUploads.isEmpty()) {
                storeAll(organizationUploads);
                Set<String> names = organizationUploads.stream().map(Upload::fileName).collect(Collectors.toSet());
                ConfigurationReloadResult result = initializationService.reload(
                        ConfigurationReloadOptions.forFiles(Set.of(OrganizationsConfigurationHandler.DOMAIN), names));
                for (ConfigurationReloadFileResult outcome : result.files()) {
                    if (outcome.status() == ConfigurationReloadFileResult.Status.ERROR) {
                        plans.add(errorPlan(outcome.fileName(), outcome.errorMessage()));
                    } else if (outcome.status() == ConfigurationReloadFileResult.Status.SKIPPED) {
                        plans.add(errorPlan(outcome.fileName(), "not loaded: " + outcome.skippedReason()));
                    }
                }
                Plan loaded = organizationsHandler.getLastPlan();
                if (loaded != null) {
                    plans.add(loaded);
                }
            }
            failed = plans.stream().anyMatch(plan -> !plan.errors().isEmpty());
        } catch (RuntimeException e) {
            failed = true;
            LogEvent.logError(this.getClass().getSimpleName(), "apply", e.getMessage());
            plans.add(errorPlan("", e.getMessage()));
        } finally {
            LocationsImportContext.clear();
            Plan merged = merge(run.getId(), options, plans);
            importRunService.finish(run, summarize(merged, uploads, true), failed);
        }
        return merge(run.getId(), options, plans);
    }

    private record Upload(MultipartFile file, String fileName, String area) {
        boolean organizations() {
            return LocationsImportApi.AREA_ORGANIZATIONS.equals(area);
        }
    }

    private static List<Upload> uploads(List<MultipartFile> files, List<String> areas) {
        List<Upload> uploads = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String name = fileNameOf(file);
            String declared = areas != null && i < areas.size() ? areas.get(i) : null;
            String area = GenericValidator.isBlankOrNull(declared) ? inferArea(name) : declared.trim();
            if (!List.of(LocationsImportApi.AREA_ORGANIZATIONS, LocationsImportApi.AREA_LEVELS,
                    LocationsImportApi.AREA_VALUES).contains(area)) {
                throw new IllegalArgumentException("'" + area + "' is not an import area");
            }
            uploads.add(new Upload(file, name, area));
        }
        List<String> order = List.of(LocationsImportApi.AREA_LEVELS, LocationsImportApi.AREA_VALUES,
                LocationsImportApi.AREA_ORGANIZATIONS);
        uploads.sort((a, b) -> Integer.compare(order.indexOf(a.area()), order.indexOf(b.area())));
        return uploads;
    }

    private static String inferArea(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.contains("-levels")) {
            return LocationsImportApi.AREA_LEVELS;
        }
        if (lower.contains("-values")) {
            return LocationsImportApi.AREA_VALUES;
        }
        return LocationsImportApi.AREA_ORGANIZATIONS;
    }

    private static String fileNameOf(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            throw new IllegalArgumentException("Every uploaded file needs a name");
        }
        String name = Paths.get(original.replace('\\', '/')).getFileName().toString();
        if (!name.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException("Import files are CSV; '" + name + "' is not");
        }
        return name;
    }

    private Path directoryFor(String domain) {
        Path directory = Paths.get(configurationBaseDir, domain);
        if (instanceId != null && !instanceId.isBlank()) {
            directory = directory.resolve(instanceId);
        }
        return directory;
    }

    private void storeAll(List<Upload> uploads) {
        for (Upload upload : uploads) {
            String domain = upload.organizations() ? OrganizationsConfigurationHandler.DOMAIN : ADDRESS_DOMAIN;
            Path target = directoryFor(domain).resolve(upload.fileName());
            try {
                Files.createDirectories(target.getParent());
                Files.copy(upload.file().getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Could not save " + upload.fileName() + " to " + target.getParent() + ": " + e.getMessage(), e);
            }
        }
    }

    private Plan previewUpload(Upload upload, Options options) {
        try (InputStream in = upload.file().getInputStream()) {
            if (upload.organizations()) {
                organizationsHandler.processConfiguration(in, upload.fileName(), true);
                Plan plan = organizationsHandler.getLastPlan();
                return plan == null ? errorPlan(upload.fileName(), "the organizations loader produced no plan") : plan;
            }
            if (LocationsImportApi.AREA_LEVELS.equals(upload.area())) {
                return previewLevels(in, upload.fileName());
            }
            return previewValues(in, upload.fileName());
        } catch (Exception e) {
            return errorPlan(upload.fileName(), CsvLoadSummary.reason(e));
        }
    }

    private static Plan errorPlan(String fileName, String error) {
        return new Plan(null, null, null, new LinkedHashMap<>(), List.of(), List.of(), 0,
                List.of((fileName == null || fileName.isEmpty() ? "" : fileName + ": ") + error));
    }

    private static Plan merge(String runId, Options options, List<Plan> plans) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String key : List.of(LocationsImportApi.OUTCOME_NEW, LocationsImportApi.OUTCOME_UPDATED,
                LocationsImportApi.OUTCOME_UNCHANGED, LocationsImportApi.OUTCOME_REACTIVATED, "deactivated",
                LocationsImportApi.OUTCOME_DECISION, LocationsImportApi.OUTCOME_REJECTED)) {
            counts.put(key, 0);
        }
        List<PlanRow> rows = new ArrayList<>();
        List<Deactivation> deactivations = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Scope scope = null;
        int unresolved = 0;
        for (Plan plan : plans) {
            plan.counts().forEach((key, value) -> counts.merge(key, value, Integer::sum));
            rows.addAll(plan.rows());
            deactivations.addAll(plan.deactivations());
            errors.addAll(plan.errors());
            unresolved += plan.unresolvedCount();
            if (plan.scope() != null) {
                scope = plan.scope();
            }
        }
        return new Plan(runId, options == null ? LocationsImportApi.MODE_MERGE : options.mode(), scope, counts, rows,
                deactivations, unresolved, errors);
    }

    private static String summarize(Plan plan, List<Upload> uploads, boolean applied) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("mode", plan.mode());
        summary.put("applied", applied);
        summary.put("files", uploads.stream().map(Upload::fileName).collect(Collectors.toList()));
        summary.put("counts", plan.counts());
        summary.put("errors", plan.errors());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PlanRow row : plan.rows()) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("file", row.file());
            line.put("line", row.line());
            line.put("outcome", row.outcome());
            line.put("type", row.type());
            line.put("code", row.code());
            line.put("name", row.name());
            line.put("reason", row.reason());
            rows.add(line);
        }
        for (Deactivation deactivation : plan.deactivations()) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("outcome", "deactivated");
            line.put("code", deactivation.code());
            line.put("name", deactivation.name());
            rows.add(line);
        }
        summary.put("rows", rows);
        try {
            return JSON.writeValueAsString(summary);
        } catch (Exception e) {
            return "{}";
        }
    }

    // ---------------------------------------------------------------- geographic
    // area files

    private Plan previewLevels(InputStream in, String fileName) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String headerLine = reader.readLine();
        if (headerLine == null) {
            return errorPlan(fileName, "the file is empty");
        }
        String[] headers = CsvParsingUtil.parseCsvLine(headerLine);
        int levelIndex = CsvParsingUtil.findColumn(headers, "level");
        int nameIndex = CsvParsingUtil.findColumn(headers, "typeName");
        if (levelIndex < 0 || nameIndex < 0) {
            return errorPlan(fileName, "a levels file needs 'level' and 'typeName' columns");
        }
        Map<Integer, String> existing = new HashMap<>();
        for (OrganizationType type : organizationTypeService.getAllOrganizationTypes()) {
            if (type.getHierarchyLevel() != null && type.getHierarchyLevel() > 0) {
                existing.put(type.getHierarchyLevel(), type.getName());
            }
        }
        List<PlanRow> rows = new ArrayList<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        String line;
        int lineNumber = 1;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (CsvParsingUtil.isSkippableLine(line)) {
                continue;
            }
            String[] values = CsvParsingUtil.parseCsvLine(line);
            String levelText = CsvParsingUtil.getValueOrEmpty(values, levelIndex).trim();
            String name = CsvParsingUtil.getValueOrEmpty(values, nameIndex).trim();
            String outcome;
            String reason = null;
            if (!levelText.matches("\\d+") || name.isEmpty()) {
                outcome = LocationsImportApi.OUTCOME_REJECTED;
                reason = "level must be a number and typeName is required";
            } else if (name.equalsIgnoreCase(existing.get(Integer.valueOf(levelText)))) {
                outcome = LocationsImportApi.OUTCOME_UNCHANGED;
            } else if (existing.containsKey(Integer.valueOf(levelText))) {
                outcome = LocationsImportApi.OUTCOME_UPDATED;
            } else {
                outcome = LocationsImportApi.OUTCOME_NEW;
            }
            counts.merge(outcome, 1, Integer::sum);
            rows.add(new PlanRow(fileName, lineNumber, outcome, "level " + levelText, null, name, null, reason,
                    List.of(), List.of(), null, false, null));
        }
        return new Plan(null, null, null, counts, rows, List.of(), 0, List.of());
    }

    private Plan previewValues(InputStream in, String fileName) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String headerLine = reader.readLine();
        if (headerLine == null) {
            return errorPlan(fileName, "the file is empty");
        }
        String[] levelNames = CsvParsingUtil.parseCsvLine(headerLine);
        Map<String, Organization> areasByPath = new HashMap<>();
        for (Organization organization : organizationService.getAllWithTypes()) {
            if (LocationsApi.KIND_AREA.equals(LocationsServiceImpl.kindOf(organization))) {
                areasByPath.put(pathKey(organization), organization);
            }
        }
        Set<String> seen = new HashSet<>();
        List<PlanRow> rows = new ArrayList<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        String line;
        int lineNumber = 1;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (CsvParsingUtil.isSkippableLine(line)) {
                continue;
            }
            String[] values = CsvParsingUtil.parseCsvLine(line);
            StringBuilder key = new StringBuilder();
            int created = 0;
            String deepest = null;
            String deepestCode = null;
            for (int i = 0; i < levelNames.length && i < values.length; i++) {
                String cell = values[i] == null ? "" : values[i].trim();
                if (cell.isEmpty()) {
                    break;
                }
                String[] parts = cell.split("%", 2);
                String name = parts[0].trim();
                deepest = name;
                deepestCode = parts.length > 1 ? parts[1].trim() : null;
                key.append('/').append(name.toLowerCase(Locale.ROOT));
                String nodeKey = key.toString();
                if (!areasByPath.containsKey(nodeKey) && seen.add(nodeKey)) {
                    created++;
                }
            }
            String outcome = deepest == null ? LocationsImportApi.OUTCOME_REJECTED
                    : created > 0 ? LocationsImportApi.OUTCOME_NEW : LocationsImportApi.OUTCOME_UNCHANGED;
            counts.merge(outcome, 1, Integer::sum);
            rows.add(new PlanRow(fileName, lineNumber, outcome, "area", deepestCode, deepest, null,
                    deepest == null ? "the row has no area" : null, List.of(), List.of(), null, false, null));
        }
        return new Plan(null, null, null, counts, rows, List.of(), 0, List.of());
    }

    private static String pathKey(Organization area) {
        List<String> names = new ArrayList<>();
        Organization current = area;
        Set<String> guard = new HashSet<>();
        while (current != null && current.getId() != null && guard.add(current.getId())) {
            names.add(0, current.getOrganizationName().toLowerCase(Locale.ROOT));
            Organization parent = current.getOrganization();
            current = parent == null || parent.getId() == null || parent.getId().equals(current.getId()) ? null
                    : parent;
        }
        return "/" + String.join("/", names);
    }

    // ----------------------------------------------------------------
    // organizations file

    /** One parsed and resolved organizations row, before the match. */
    private static final class ParsedRow {
        CsvRow csv;
        List<OrganizationType> types = new ArrayList<>();
        String kind;
        String name;
        String code;
        Map<String, String> identifiers = new LinkedHashMap<>();
        Organization parent;
        ParsedRow pendingParent;
        String parentText;
        String reject;
        Organization matched;
        String outcome;
        List<FieldChange> diffs = List.of();
        List<Candidate> candidates = List.of();
        Candidate pair;
        Organization pairOrganization;
        String createdId;
    }

    private final class Catalogue {
        final Map<String, OrganizationType> typesByName = new HashMap<>();
        final List<Organization> all;
        final Map<String, Organization> byId = new HashMap<>();
        final Map<String, List<Organization>> byLabelValue = new HashMap<>();
        final Map<String, List<Organization>> byKindName = new HashMap<>();
        final Map<Integer, List<String>> formerNames;
        final Set<String> closed;

        Catalogue() {
            for (OrganizationType type : organizationTypeService.getAllOrganizationTypes()) {
                typesByName.put(norm(type.getName()), type);
            }
            all = organizationService.getAllWithTypes();
            for (Organization organization : all) {
                byId.put(organization.getId(), organization);
                byKindName.computeIfAbsent(
                        LocationsServiceImpl.kindOf(organization) + "|" + norm(organization.getOrganizationName()),
                        k -> new ArrayList<>()).add(organization);
            }
            List<Integer> ids = all.stream().map(Organization::getId).filter(id -> id.matches("\\d+"))
                    .map(Integer::valueOf).collect(Collectors.toList());
            for (OrganizationIdentifier identifier : identifierService.getForOrganizations(ids)) {
                Organization owner = byId.get(String.valueOf(identifier.getOrganizationId()));
                if (owner != null) {
                    byLabelValue.computeIfAbsent(norm(identifier.getLabel()) + "|" + norm(identifier.getValue()),
                            k -> new ArrayList<>()).add(owner);
                }
            }
            for (Organization organization : all) {
                if (!GenericValidator.isBlankOrNull(organization.getCode())) {
                    List<Organization> owners = byLabelValue.computeIfAbsent(
                            norm(OrganizationIdentifier.CODE_LABEL) + "|" + norm(organization.getCode()),
                            k -> new ArrayList<>());
                    if (!owners.contains(organization)) {
                        owners.add(organization);
                    }
                }
            }
            formerNames = changeService.formerNames();
            closed = new HashSet<>();
        }

        OrganizationType type(String name) {
            return typesByName.get(norm(name));
        }

        List<Organization> byIdentifier(String label, String value) {
            return byLabelValue.getOrDefault(norm(label) + "|" + norm(value), List.of());
        }

        List<Organization> byName(String kind, String name) {
            List<Organization> found = new ArrayList<>(byKindName.getOrDefault(kind + "|" + norm(name), List.of()));
            String wanted = norm(name);
            for (Organization organization : all) {
                if (!kind.equals(LocationsServiceImpl.kindOf(organization)) || found.contains(organization)) {
                    continue;
                }
                Integer numericId = organization.getId().matches("\\d+") ? Integer.valueOf(organization.getId()) : null;
                if (formerNames.getOrDefault(numericId, List.of()).stream().anyMatch(f -> norm(f).equals(wanted))) {
                    found.add(organization);
                }
            }
            String alias = aliasService.resolve(LocationsImportApi.ALIAS_TYPE, name);
            if (alias != null && byId.containsKey(alias) && !found.contains(byId.get(alias))) {
                found.add(byId.get(alias));
            }
            return found;
        }
    }

    static String norm(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static Organization realParent(Organization organization) {
        Organization parent = organization.getOrganization();
        if (parent == null || parent.getId() == null || parent.getId().equals(organization.getId())) {
            return null;
        }
        return parent;
    }

    @Override
    public Plan loadOrganizations(List<CsvRow> rows, RowTransactionRunner transaction, CsvLoadSummary summary,
            String fileName, Options options) {
        Catalogue catalogue = new Catalogue();
        String actor = ImportRunContext.getRunId() == null ? "Import" : "Import run #" + ImportRunContext.getRunId();
        List<ParsedRow> parsed = new ArrayList<>();
        for (CsvRow row : rows) {
            parsed.add(parse(row, catalogue));
        }
        parsed.sort((a, b) -> Boolean.compare(LocationsApi.KIND_WARD.equals(a.kind),
                LocationsApi.KIND_WARD.equals(b.kind)));
        Map<String, ParsedRow> pendingByCode = new HashMap<>();
        Map<String, ParsedRow> pendingByName = new HashMap<>();
        Set<String> matchedIds = new HashSet<>();
        Set<String> wardParents = new LinkedHashSet<>();
        Set<String> typeNames = new LinkedHashSet<>();

        for (ParsedRow row : parsed) {
            if (row.reject != null) {
                continue;
            }
            resolveParent(row, catalogue, pendingByCode, pendingByName);
            if (row.reject != null) {
                continue;
            }
            match(row, catalogue, options, matchedIds);
            if (row.matched != null) {
                matchedIds.add(row.matched.getId());
            } else if (LocationsImportApi.OUTCOME_NEW.equals(row.outcome)) {
                if (!GenericValidator.isBlankOrNull(row.code)) {
                    pendingByCode.put(norm(row.code), row);
                }
                pendingByName.put(row.kind + "|" + norm(row.name), row);
            }
            if (LocationsApi.KIND_WARD.equals(row.kind) && row.parent != null) {
                wardParents.add(row.parent.getId());
            }
            row.types.forEach(type -> typeNames.add(type.getName()));
        }
        for (ParsedRow row : parsed) {
            if (row.reject == null && LocationsImportApi.OUTCOME_NEW.equals(row.outcome)) {
                checkRename(row, catalogue, options, matchedIds);
            }
        }
        List<Deactivation> deactivations = options.replace()
                ? replaceDeactivations(parsed, catalogue, matchedIds, wardParents, typeNames)
                : List.of();

        boolean apply = !transaction.isDryRun();
        if (apply) {
            for (ParsedRow row : parsed) {
                if (row.reject != null || LocationsImportApi.OUTCOME_DECISION.equals(row.outcome)
                        || LocationsImportApi.OUTCOME_RENAME.equals(row.outcome)
                        || LocationsImportApi.OUTCOME_SKIPPED.equals(row.outcome)
                        || LocationsImportApi.OUTCOME_UNCHANGED.equals(row.outcome)) {
                    continue;
                }
                try {
                    transaction.run(() -> {
                        write(row, options, actor);
                        return null;
                    });
                } catch (RuntimeException e) {
                    row.reject = CsvLoadSummary.reason(e);
                    row.outcome = LocationsImportApi.OUTCOME_REJECTED;
                }
            }
        }
        if (apply) {
            rememberDecisions(parsed, options, transaction);
        }
        List<ParsedRow> inFileOrder = new ArrayList<>(parsed);
        inFileOrder.sort((a, b) -> Integer.compare(a.csv.lineNumber(), b.csv.lineNumber()));
        List<PlanRow> planRows = new ArrayList<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ParsedRow row : inFileOrder) {
            String outcome = row.reject != null ? LocationsImportApi.OUTCOME_REJECTED : row.outcome;
            counts.merge(outcome, 1, Integer::sum);
            recordSummary(summary, row, outcome, fileName);
            planRows.add(new PlanRow(fileName, row.csv.lineNumber(), outcome,
                    row.types.stream().map(OrganizationType::getName).collect(Collectors.joining(";")), row.code,
                    row.name, row.parentText, row.reject, row.diffs, row.candidates, row.pair,
                    row.matched != null && LocationsApi.SOURCE_REGISTRY.equals(row.matched.getSource()),
                    row.matched == null ? row.createdId : row.matched.getId()));
        }
        if (apply && !deactivations.isEmpty()) {
            List<String> ids = deactivations.stream().map(Deactivation::id).collect(Collectors.toList());
            transaction.run(() -> {
                locationsService.setActive(new ActiveRequest(ids, false, false), actorUser(), actor);
                return null;
            });
        }
        counts.put("deactivated", deactivations.size());
        return new Plan(ImportRunContext.getRunId(), options.mode(), scope(options, typeNames, wardParents, catalogue),
                counts, planRows, deactivations, 0, List.of());
    }

    private static String actorUser() {
        return "1";
    }

    private static void recordSummary(CsvLoadSummary summary, ParsedRow row, String outcome, String fileName) {
        int line = row.csv.lineNumber();
        switch (outcome) {
        case LocationsImportApi.OUTCOME_NEW:
            summary.record(LoadedRow.created(row.name), LocationsImportServiceImpl.class.getSimpleName(), line);
            break;
        case LocationsImportApi.OUTCOME_UPDATED:
        case LocationsImportApi.OUTCOME_REACTIVATED:
            summary.record(LoadedRow.updated(row.name), LocationsImportServiceImpl.class.getSimpleName(), line);
            break;
        case LocationsImportApi.OUTCOME_UNCHANGED:
            break;
        default:
            summary.skipped(LocationsImportServiceImpl.class.getSimpleName(), line,
                    row.reject != null ? row.reject : outcome);
        }
    }

    private ParsedRow parse(CsvRow csv, Catalogue catalogue) {
        ParsedRow row = new ParsedRow();
        row.csv = csv;
        row.name = csv.get("name") == null ? "" : csv.get("name").trim();
        row.code = csv.has("code") ? csv.get("code").trim() : "";
        String typeText = csv.get("type") == null ? "" : csv.get("type").trim();
        if (row.name.isEmpty() || typeText.isEmpty()) {
            row.reject = "missing name or type";
            return row;
        }
        boolean level = false;
        boolean dept = false;
        boolean site = false;
        for (String typeName : typeText.split(";")) {
            if (typeName.trim().isEmpty()) {
                continue;
            }
            OrganizationType type = catalogue.type(typeName);
            if (type == null) {
                row.reject = "unknown organization type '" + typeName.trim() + "'";
                return row;
            }
            if (type.getHierarchyLevel() != null && type.getHierarchyLevel() > 0) {
                level = true;
            }
            dept |= LocationsServiceImpl.DEPT_TYPE.equalsIgnoreCase(type.getName().trim());
            site |= LocationsServiceImpl.SITE_TYPE.equalsIgnoreCase(type.getName().trim());
            row.types.add(type);
        }
        if (level) {
            row.reject = "Geographic areas are imported with the geographic levels and areas files";
            return row;
        }
        if ((site && (dept || row.types.size() > 1)) || (dept && row.types.size() > 1)) {
            row.reject = "a sampling site or ward / dept cannot carry another type";
            return row;
        }
        row.kind = dept ? LocationsApi.KIND_WARD : site ? LocationsApi.KIND_SITE : LocationsApi.KIND_FACILITY;
        if (!row.code.isEmpty()) {
            row.identifiers.put(OrganizationIdentifier.CODE_LABEL, row.code);
        }
        for (String column : csv.columns().keySet()) {
            if (column.startsWith(IDENTIFIER_PREFIX)) {
                String label = csv.header(column).substring(IDENTIFIER_PREFIX.length()).trim();
                String value = csv.get(column);
                if (!label.isEmpty() && value != null && !value.trim().isEmpty()) {
                    row.identifiers.putIfAbsent(label, value.trim());
                }
            }
        }
        Set<String> seenLabels = new HashSet<>();
        for (String label : row.identifiers.keySet()) {
            if (!seenLabels.add(norm(label))) {
                row.reject = "identifier label '" + label + "' appears twice";
                return row;
            }
        }
        for (String field : List.of("gpsLatitude", "gpsLongitude")) {
            String value = csv.has(field) ? csv.get(field).trim() : "";
            if (!value.isEmpty()) {
                try {
                    BigDecimal number = new BigDecimal(value);
                    boolean latitude = "gpsLatitude".equals(field);
                    if (number.abs().compareTo(BigDecimal.valueOf(latitude ? 90 : 180)) > 0) {
                        row.reject = field + " '" + value + "' is outside decimal degrees";
                        return row;
                    }
                } catch (NumberFormatException e) {
                    row.reject = field + " '" + value + "' is not decimal degrees";
                    return row;
                }
            }
        }
        if (LocationsApi.KIND_WARD.equals(row.kind)) {
            String service = csv.has("serviceType") ? csv.get("serviceType").trim() : "";
            if (!service.isEmpty() && WardServiceType.parse(service) == null) {
                row.reject = "unknown service type '" + service + "'";
                return row;
            }
        }
        return row;
    }

    private void resolveParent(ParsedRow row, Catalogue catalogue, Map<String, ParsedRow> pendingByCode,
            Map<String, ParsedRow> pendingByName) {
        CsvRow csv = row.csv;
        String parentCode = csv.has("parentCode") ? csv.get("parentCode").trim() : "";
        String parentName = csv.has("parentName") ? csv.get("parentName").trim() : "";
        String parentType = csv.has("parentType") ? csv.get("parentType").trim() : "";
        row.parentText = !parentCode.isEmpty() ? parentCode : parentName;
        boolean ward = LocationsApi.KIND_WARD.equals(row.kind);
        if (parentCode.isEmpty() && parentName.isEmpty()) {
            if (ward) {
                row.reject = "a ward / dept needs its organization (parentCode or parentName)";
            }
            return;
        }
        String wantedKind = ward ? LocationsApi.KIND_FACILITY : LocationsApi.KIND_AREA;
        Organization parent = null;
        if (!parentCode.isEmpty()) {
            for (Organization candidate : catalogue.byIdentifier(OrganizationIdentifier.CODE_LABEL, parentCode)) {
                if (wantedKind.equals(LocationsServiceImpl.kindOf(candidate))) {
                    parent = candidate;
                    break;
                }
            }
            if (parent == null && pendingByCode.containsKey(norm(parentCode))
                    && wantedKind.equals(pendingByCode.get(norm(parentCode)).kind)) {
                row.pendingParent = pendingByCode.get(norm(parentCode));
                return;
            }
        }
        if (parent == null && !parentName.isEmpty()) {
            for (Organization candidate : catalogue.byName(wantedKind, parentName)) {
                boolean typeOk = parentType.isEmpty() || candidate.getOrganizationTypes().stream()
                        .anyMatch(type -> norm(type.getName()).equals(norm(parentType)));
                if (typeOk) {
                    parent = candidate;
                    break;
                }
            }
            if (parent == null && pendingByName.containsKey(wantedKind + "|" + norm(parentName))) {
                row.pendingParent = pendingByName.get(wantedKind + "|" + norm(parentName));
                return;
            }
        }
        if (parent == null) {
            row.reject = "parent '" + row.parentText + "' not found";
            return;
        }
        row.parent = parent;
    }

    private void match(ParsedRow row, Catalogue catalogue, Options options, Set<String> matchedIds) {
        Decision decision = options.decisionFor(row.csv.lineNumber());
        if (decision != null) {
            if (LocationsImportApi.CHOICE_SKIP.equals(decision.choice())) {
                row.outcome = LocationsImportApi.OUTCOME_SKIPPED;
                return;
            }
            if (LocationsImportApi.CHOICE_NEW.equals(decision.choice())) {
                row.outcome = LocationsImportApi.OUTCOME_NEW;
                return;
            }
            if (decision.choice() != null && decision.choice().startsWith(LocationsImportApi.CHOICE_USE)) {
                Organization chosen = catalogue.byId
                        .get(decision.choice().substring(LocationsImportApi.CHOICE_USE.length()));
                if (chosen != null) {
                    settleMatch(row, chosen, options);
                    return;
                }
            }
        }
        Set<Organization> byIdentifier = new LinkedHashSet<>();
        for (Map.Entry<String, String> identifier : row.identifiers.entrySet()) {
            for (Organization candidate : catalogue.byIdentifier(identifier.getKey(), identifier.getValue())) {
                if (row.kind.equals(LocationsServiceImpl.kindOf(candidate))) {
                    byIdentifier.add(candidate);
                }
            }
        }
        if (byIdentifier.size() == 1) {
            settleMatch(row, byIdentifier.iterator().next(), options);
            return;
        }
        if (byIdentifier.size() > 1) {
            decisionNeeded(row, new ArrayList<>(byIdentifier), catalogue);
            return;
        }
        String alias = aliasService.resolve(LocationsImportApi.ALIAS_TYPE, row.name);
        Organization remembered = alias == null ? null : catalogue.byId.get(alias);
        if (remembered != null && row.kind.equals(LocationsServiceImpl.kindOf(remembered))) {
            settleMatch(row, remembered, options);
            return;
        }
        List<Organization> byName = catalogue.byName(row.kind, row.name).stream().filter(candidate -> {
            if (LocationsApi.KIND_WARD.equals(row.kind)) {
                Organization parent = realParent(candidate);
                return row.parent != null && parent != null && parent.getId().equals(row.parent.getId());
            }
            return sharesAType(candidate, row);
        }).filter(candidate -> !matchedIds.contains(candidate.getId())).collect(Collectors.toList());
        if (byName.size() == 1) {
            settleMatch(row, byName.get(0), options);
        } else if (byName.size() > 1) {
            decisionNeeded(row, byName, catalogue);
        } else {
            row.outcome = LocationsImportApi.OUTCOME_NEW;
        }
    }

    private static boolean sharesAType(Organization candidate, ParsedRow row) {
        Set<String> wanted = row.types.stream().map(OrganizationType::getId).collect(Collectors.toSet());
        return candidate.getOrganizationTypes().stream().anyMatch(type -> wanted.contains(type.getId()));
    }

    private void decisionNeeded(ParsedRow row, List<Organization> candidates, Catalogue catalogue) {
        row.outcome = LocationsImportApi.OUTCOME_DECISION;
        row.candidates = candidates.stream().map(this::candidate).collect(Collectors.toList());
    }

    private Candidate candidate(Organization organization) {
        Organization parent = realParent(organization);
        Usage usage = usageDAO.orderCountsForOrganizations(List.of(organization.getId()), Set.of())
                .getOrDefault(organization.getId(), new Usage(0, 0));
        return new Candidate(organization.getId(), organization.getOrganizationName(), organization.getCode(),
                parent == null ? null : parent.getOrganizationName(), "Y".equals(organization.getIsActive()),
                usage.total());
    }

    private void settleMatch(ParsedRow row, Organization matched, Options options) {
        row.matched = matched;
        row.diffs = diffs(row, matched);
        boolean inactive = !"Y".equals(matched.getIsActive());
        boolean rowSaysInactive = row.csv.has("active") && "N".equalsIgnoreCase(row.csv.get("active").trim());
        if (rowSaysInactive && !inactive) {
            row.diffs = append(row.diffs, new FieldChange("active", "Y", "N"));
            row.outcome = LocationsImportApi.OUTCOME_UPDATED;
        } else if (inactive && !rowSaysInactive && options.replace()) {
            row.diffs = append(row.diffs, new FieldChange("active", "N", "Y"));
            row.outcome = LocationsImportApi.OUTCOME_REACTIVATED;
        } else {
            row.outcome = row.diffs.isEmpty() ? LocationsImportApi.OUTCOME_UNCHANGED
                    : LocationsImportApi.OUTCOME_UPDATED;
        }
    }

    private static List<FieldChange> append(List<FieldChange> changes, FieldChange change) {
        List<FieldChange> out = new ArrayList<>(changes);
        out.add(change);
        return out;
    }

    private void checkRename(ParsedRow row, Catalogue catalogue, Options options, Set<String> matchedIds) {
        String rename = options.renameFor(row.csv.lineNumber());
        if (LocationsImportApi.RENAME_DIFFERENT.equals(rename)) {
            return;
        }
        String wanted = norm(row.name);
        Set<String> wantedWords = new HashSet<>(Arrays.asList(wanted.split(" ")));
        Organization best = null;
        for (Organization candidate : catalogue.all) {
            if (!row.kind.equals(LocationsServiceImpl.kindOf(candidate)) || matchedIds.contains(candidate.getId())
                    || !"Y".equals(candidate.getIsActive())) {
                continue;
            }
            if (!sharesAType(candidate, row) && !LocationsApi.KIND_WARD.equals(row.kind)
                    && !LocationsApi.KIND_SITE.equals(row.kind)) {
                continue;
            }
            Organization parent = realParent(candidate);
            String parentId = parent == null ? "" : parent.getId();
            String rowParentId = row.parent == null ? "" : row.parent.getId();
            if (!parentId.equals(rowParentId)) {
                continue;
            }
            String other = norm(candidate.getOrganizationName());
            if (other.equals(wanted)) {
                continue;
            }
            boolean contains = other.contains(wanted) || wanted.contains(other);
            Set<String> otherWords = new HashSet<>(Arrays.asList(other.split(" ")));
            long shared = wantedWords.stream().filter(otherWords::contains).count();
            boolean similar = contains
                    || shared * 100 / Math.max(1, Math.max(wantedWords.size(), otherWords.size())) >= 60;
            if (similar) {
                best = candidate;
                break;
            }
        }
        if (best == null) {
            return;
        }
        if (LocationsImportApi.RENAME_SAME.equals(rename)) {
            settleMatch(row, best, options);
            matchedIds.add(best.getId());
            return;
        }
        row.outcome = LocationsImportApi.OUTCOME_RENAME;
        row.pair = candidate(best);
        row.pairOrganization = best;
    }

    private List<Deactivation> replaceDeactivations(List<ParsedRow> parsed, Catalogue catalogue, Set<String> matchedIds,
            Set<String> wardParents, Set<String> typeNames) {
        Set<String> pendingPairs = parsed.stream().filter(row -> row.pairOrganization != null)
                .map(row -> row.pairOrganization.getId()).collect(Collectors.toSet());
        Set<String> kinds = parsed.stream().filter(row -> row.reject == null && row.kind != null).map(row -> row.kind)
                .collect(Collectors.toSet());
        Set<String> normalizedTypes = typeNames.stream().map(LocationsImportServiceImpl::norm)
                .collect(Collectors.toSet());
        List<Organization> targets = new ArrayList<>();
        for (Organization organization : catalogue.all) {
            String kind = LocationsServiceImpl.kindOf(organization);
            if (!"Y".equals(organization.getIsActive()) || matchedIds.contains(organization.getId())
                    || pendingPairs.contains(organization.getId())
                    || LocationsApi.SOURCE_REGISTRY.equals(organization.getSource())
                    || LocationsApi.KIND_AREA.equals(kind) || !kinds.contains(kind)) {
                continue;
            }
            if (LocationsApi.KIND_WARD.equals(kind)) {
                Organization parent = realParent(organization);
                if (parent == null || !wardParents.contains(parent.getId())) {
                    continue;
                }
            } else if (organization.getOrganizationTypes().stream()
                    .noneMatch(type -> normalizedTypes.contains(norm(type.getName())))) {
                continue;
            }
            targets.add(organization);
        }
        if (targets.isEmpty()) {
            return List.of();
        }
        Map<String, Usage> usage = usageDAO.orderCountsForOrganizations(
                targets.stream().map(Organization::getId).collect(Collectors.toList()), Set.of());
        List<Deactivation> out = new ArrayList<>();
        for (Organization organization : targets) {
            Organization parent = realParent(organization);
            out.add(new Deactivation(organization.getId(), organization.getOrganizationName(), organization.getCode(),
                    parent == null ? null : parent.getOrganizationName(),
                    usage.getOrDefault(organization.getId(), new Usage(0, 0))));
        }
        return out;
    }

    private Scope scope(Options options, Set<String> typeNames, Set<String> wardParents, Catalogue catalogue) {
        if (!options.replace()) {
            return null;
        }
        List<String> parents = wardParents.stream().map(catalogue.byId::get).filter(Objects::nonNull)
                .map(Organization::getOrganizationName).collect(Collectors.toList());
        List<String> untouched = new ArrayList<>();
        for (OrganizationType type : organizationTypeService.getAllOrganizationTypes()) {
            boolean level = type.getHierarchyLevel() != null && type.getHierarchyLevel() > 0;
            if (!level && !typeNames.contains(type.getName())) {
                untouched.add(type.getName());
            }
        }
        String text = "This file has " + String.join(", ", typeNames) + " rows. "
                + (untouched.isEmpty() ? "" : String.join(", ", untouched) + " and geographic areas will not change. ")
                + (parents.isEmpty() ? "No wards / depts will be replaced. "
                        : "Wards / depts will only be replaced at " + String.join(", ", parents) + ". ")
                + "Registry records are never deactivated.";
        return new Scope(text, new ArrayList<>(typeNames), untouched, parents);
    }

    private List<FieldChange> diffs(ParsedRow row, Organization matched) {
        List<FieldChange> changes = new ArrayList<>();
        CsvRow csv = row.csv;
        Map<String, String> stored = storedValues(matched);
        for (String column : BASE_COLUMNS) {
            if ("type".equals(column) || "code".equals(column) || column.startsWith("parent") || "active".equals(column)
                    || !csv.has(column)) {
                continue;
            }
            String value = csv.get(column) == null ? "" : csv.get(column).trim();
            if (value.isEmpty()) {
                continue;
            }
            String current = stored.getOrDefault(column, "");
            if (!norm(current).equals(norm(value))) {
                changes.add(new FieldChange(column, current, value));
            }
        }
        if (row.parent != null) {
            Organization parent = realParent(matched);
            if (parent == null || !parent.getId().equals(row.parent.getId())) {
                changes.add(new FieldChange("location", parent == null ? null : parent.getOrganizationName(),
                        row.parent.getOrganizationName()));
            }
        }
        for (Map.Entry<String, String> identifier : row.identifiers.entrySet()) {
            String current = identifierValue(matched, identifier.getKey());
            if (!norm(current).equals(norm(identifier.getValue()))) {
                changes.add(new FieldChange("identifier:" + identifier.getKey(), current, identifier.getValue()));
            }
        }
        Set<String> newTypes = row.types.stream().map(type -> norm(type.getName())).collect(Collectors.toSet());
        Set<String> oldTypes = matched.getOrganizationTypes().stream().map(type -> norm(type.getName()))
                .collect(Collectors.toSet());
        if (!newTypes.equals(oldTypes) && !LocationsApi.KIND_WARD.equals(row.kind)
                && !LocationsApi.KIND_SITE.equals(row.kind)) {
            changes.add(new FieldChange("types", String.join(", ", oldTypes), String.join(", ", newTypes)));
        }
        return changes;
    }

    private Map<String, String> storedValues(Organization organization) {
        Map<String, String> values = new HashMap<>();
        values.put("name", organization.getOrganizationName());
        values.put("shortName", organization.getShortName());
        values.put("streetAddress", organization.getStreetAddress());
        values.put("city", organization.getCity());
        values.put("state", organization.getState());
        values.put("zipCode", organization.getZipCode());
        values.put("gpsLatitude", organization.getGpsLatitude() == null ? ""
                : organization.getGpsLatitude().stripTrailingZeros().toPlainString());
        values.put("gpsLongitude", organization.getGpsLongitude() == null ? ""
                : organization.getGpsLongitude().stripTrailingZeros().toPlainString());
        values.put("contactName", organization.getContactName());
        values.put("phone", organization.getPhone());
        values.put("email", organization.getEmail());
        values.put("internetAddress", organization.getInternetAddress());
        values.put("category", listLabel(organization.getCategoryId()));
        values.put("ownership", listLabel(organization.getOwnershipId()));
        values.put("serviceType",
                organization.getServiceType() == null ? ""
                        : WardServiceType.parse(organization.getServiceType()) == null ? organization.getServiceType()
                                : WardServiceType.parse(organization.getServiceType()).getLabel());
        values.put("description", organization.getDescription());
        values.put("approvalStatus", organization.getApprovalStatus());
        values.put("accreditationBody", organization.getAccreditationBody());
        values.put("accreditationNumber", organization.getAccreditationNumber());
        values.put("accreditationExpiry", text(organization.getAccreditationExpiry()));
        values.put("lastReviewDate", text(organization.getLastReviewDate()));
        values.put("nextReviewDue", text(organization.getNextReviewDue()));
        if (organization.getId().matches("\\d+")) {
            VectorSamplingSite site = samplingSiteService.getByOrganizationId(Integer.valueOf(organization.getId()));
            if (site != null) {
                values.put("siteType", site.getType());
                values.put("subtype", site.getSubtype());
                values.put("environmentalZone", site.getEnvironmentalZone());
            }
        }
        return values;
    }

    private static String text(java.time.LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private String listLabel(String dictionaryId) {
        if (GenericValidator.isBlankOrNull(dictionaryId)) {
            return "";
        }
        try {
            return locationsService.lists().categories().stream().filter(entry -> entry.id().equals(dictionaryId))
                    .map(entry -> entry.label()).findFirst()
                    .orElseGet(() -> locationsService.lists().ownerships().stream()
                            .filter(entry -> entry.id().equals(dictionaryId)).map(entry -> entry.label()).findFirst()
                            .orElse(dictionaryId));
        } catch (RuntimeException e) {
            return dictionaryId;
        }
    }

    /**
     * The stored value of one identifier label. A record that predates the
     * identifier table, or was written by the registry sync, keeps its code and
     * CLIA number in the organization columns only, so those columns answer when no
     * identifier row exists.
     */
    private String identifierValue(Organization organization, String label) {
        String column = OrganizationIdentifier.CODE_LABEL.equalsIgnoreCase(label) ? organization.getCode()
                : OrganizationIdentifier.CLIA_LABEL.equalsIgnoreCase(label) ? organization.getCliaNum() : null;
        String fallback = column == null ? "" : column;
        if (!organization.getId().matches("\\d+")) {
            return fallback;
        }
        return identifierService.getForOrganization(Integer.valueOf(organization.getId())).stream()
                .filter(identifier -> norm(identifier.getLabel()).equals(norm(label)))
                .map(OrganizationIdentifier::getValue).findFirst().orElse(fallback);
    }

    // ---------------------------------------------------------------- writing a
    // row

    private void write(ParsedRow row, Options options, String actor) {
        CsvRow csv = row.csv;
        String sysUserId = actorUser();
        Organization parent = row.parent;
        if (parent == null && row.pendingParent != null && row.pendingParent.createdId != null) {
            parent = organizationService.get(row.pendingParent.createdId);
        }
        if (parent == null && row.pendingParent != null) {
            throw new IllegalStateException("parent '" + row.parentText + "' was not created");
        }
        if (LocationsApi.KIND_WARD.equals(row.kind)) {
            WardRequest request = new WardRequest(row.matched == null ? null : row.matched.getId(), row.name,
                    value(csv, "code", row.matched == null ? null : row.matched.getCode()),
                    value(csv, "serviceType", row.matched == null ? null : row.matched.getServiceType()),
                    value(csv, "contactName", row.matched == null ? null : row.matched.getContactName()),
                    value(csv, "phone", row.matched == null ? null : row.matched.getPhone()),
                    value(csv, "email", row.matched == null ? null : row.matched.getEmail()),
                    decimal(csv, "gpsLatitude", row.matched == null ? null : row.matched.getGpsLatitude()),
                    decimal(csv, "gpsLongitude", row.matched == null ? null : row.matched.getGpsLongitude()), null);
            Ward ward = locationsService.saveWard(parent.getId(), request, sysUserId, actor);
            row.createdId = ward.id();
            applyActive(row, ward.id(), actor, options);
            return;
        }
        Detail current = row.matched == null ? null : locationsService.get(row.matched.getId());
        List<Identifier> identifiers = new ArrayList<>();
        Map<String, Identifier> byLabel = new LinkedHashMap<>();
        if (current != null) {
            for (Identifier identifier : current.identifiers()) {
                byLabel.put(norm(identifier.label()), identifier);
            }
        }
        for (Map.Entry<String, String> identifier : row.identifiers.entrySet()) {
            boolean reporting = OrganizationIdentifier.CODE_LABEL.equalsIgnoreCase(identifier.getKey());
            Identifier existing = byLabel.get(norm(identifier.getKey()));
            byLabel.put(norm(identifier.getKey()), new Identifier(existing == null ? null : existing.id(),
                    identifier.getKey(), identifier.getValue(), existing == null ? reporting : existing.reporting()));
        }
        identifiers.addAll(byLabel.values());
        if (!identifiers.isEmpty() && identifiers.stream().noneMatch(Identifier::reporting)) {
            Identifier first = identifiers.get(0);
            identifiers.set(0, new Identifier(first.id(), first.label(), first.value(), true));
        }
        Referral referral = current != null && current.referral() != null ? current.referral()
                : new Referral(null, null, null, null, null, null, null);
        referral = new Referral(value(csv, "approvalStatus", referral.approvalStatus()),
                value(csv, "accreditationBody", referral.accreditationBody()),
                value(csv, "accreditationNumber", referral.accreditationNumber()),
                value(csv, "accreditationExpiry", referral.accreditationExpiry()),
                value(csv, "lastReviewDate", referral.lastReviewDate()),
                value(csv, "nextReviewDue", referral.nextReviewDue()), referral.reviewNotes());
        Site site = current != null && current.site() != null ? current.site() : new Site(null, null, null, null);
        site = new Site(site.siteId(), value(csv, "siteType", site.siteType()), value(csv, "subtype", site.subtype()),
                value(csv, "environmentalZone", site.environmentalZone()));
        Row currentRow = current == null ? null : current.row();
        SaveRequest request = new SaveRequest(row.matched == null ? null : row.matched.getId(), row.kind, row.name,
                value(csv, "shortName", currentRow == null ? null : currentRow.shortName()),
                row.types.stream().map(OrganizationType::getId).collect(Collectors.toList()),
                listId(csv, "category",
                        currentRow == null || currentRow.category() == null ? null : currentRow.category().id(), true),
                listId(csv, "ownership",
                        currentRow == null || currentRow.ownership() == null ? null : currentRow.ownership().id(),
                        false),
                value(csv, "description", current == null ? null : current.description()),
                parent != null ? parent.getId() : current == null ? null : current.parentId(),
                value(csv, "streetAddress", current == null ? null : current.streetAddress()),
                value(csv, "city", current == null ? null : current.city()),
                value(csv, "state", current == null ? null : current.state()),
                value(csv, "zipCode", current == null ? null : current.zipCode()),
                decimal(csv, "gpsLatitude", current == null ? null : current.gpsLatitude()),
                decimal(csv, "gpsLongitude", current == null ? null : current.gpsLongitude()),
                value(csv, "contactName", current == null ? null : current.contactName()),
                value(csv, "phone", current == null ? null : current.phone()), current == null ? null : current.fax(),
                value(csv, "email", current == null ? null : current.email()),
                value(csv, "internetAddress", current == null ? null : current.internetAddress()), identifiers,
                referral, site, null, null);
        Detail saved = locationsService.save(request, sysUserId, actor).detail();
        row.createdId = saved.row().id();
        applyActive(row, saved.row().id(), actor, options);
    }

    /**
     * A remembered decision is kept whether or not the chosen record needed any
     * change, so the same spelling resolves itself on the next import.
     */
    private void rememberDecisions(List<ParsedRow> parsed, Options options, RowTransactionRunner transaction) {
        for (ParsedRow row : parsed) {
            Decision decision = options.decisionFor(row.csv.lineNumber());
            if (row.reject == null && row.matched != null && decision != null && decision.remember()) {
                transaction.run(() -> {
                    aliasService.remember(LocationsImportApi.ALIAS_TYPE, row.name, row.matched.getId(), actorUser());
                    return null;
                });
            }
        }
    }

    private void applyActive(ParsedRow row, String id, String actor, Options options) {
        boolean rowSaysInactive = row.csv.has("active") && "N".equalsIgnoreCase(row.csv.get("active").trim());
        if (LocationsImportApi.OUTCOME_REACTIVATED.equals(row.outcome)) {
            locationsService.setActive(new ActiveRequest(List.of(id), true, false), actorUser(), actor);
        } else if (rowSaysInactive) {
            locationsService.setActive(new ActiveRequest(List.of(id), false, false), actorUser(), actor);
        }
    }

    private static String value(CsvRow csv, String column, String current) {
        if (!csv.has(column)) {
            return current;
        }
        String value = csv.get(column);
        return value == null || value.trim().isEmpty() ? current : value.trim();
    }

    private static BigDecimal decimal(CsvRow csv, String column, BigDecimal current) {
        String value = value(csv, column, null);
        return value == null ? current : new BigDecimal(value);
    }

    private String listId(CsvRow csv, String column, String current, boolean category) {
        String value = value(csv, column, null);
        if (value == null) {
            return current;
        }
        var entries = category ? locationsService.lists().categories() : locationsService.lists().ownerships();
        return entries.stream().filter(entry -> norm(entry.label()).equals(norm(value))).map(entry -> entry.id())
                .findFirst().orElse(current);
    }

    // ---------------------------------------------------------------- runs,
    // reports, templates, export

    @Override
    public List<RecentRun> recentRuns() {
        List<RecentRun> runs = new ArrayList<>();
        for (ConfigurationImportRun run : importRunService.getAllOrdered("startedAt", true)) {
            if (runs.size() >= 20) {
                break;
            }
            String summary = run.getSummary();
            String mode = null;
            String counts = null;
            if (summary != null && summary.startsWith("{")) {
                try {
                    JsonNode node = JSON.readTree(summary);
                    if (!node.has("counts")) {
                        continue;
                    }
                    mode = node.path("mode").asText(null);
                    counts = node.path("counts").toString();
                } catch (Exception e) {
                    continue;
                }
            } else {
                continue;
            }
            runs.add(new RecentRun(run.getId(), String.valueOf(run.getStartedAt()),
                    run.getFinishedAt() == null ? null : String.valueOf(run.getFinishedAt()),
                    userName(run.getSystemUserId()), mode, counts, run.getStatus()));
        }
        return runs;
    }

    private String userName(Integer systemUserId) {
        if (systemUserId == null) {
            return "";
        }
        try {
            SystemUser user = systemUserService.getUserById(String.valueOf(systemUserId));
            return user == null ? String.valueOf(systemUserId) : user.getLoginName();
        } catch (RuntimeException e) {
            return String.valueOf(systemUserId);
        }
    }

    @Override
    public String report(String runId) {
        ConfigurationImportRun run = importRunService.get(runId);
        if (run == null || run.getSummary() == null || !run.getSummary().startsWith("{")) {
            throw new LocationsNotFoundException("No import run " + runId);
        }
        StringBuilder csv = new StringBuilder("file,line,outcome,type,code,name,reason\n");
        try {
            JsonNode node = JSON.readTree(run.getSummary());
            for (JsonNode row : node.path("rows")) {
                csv.append(String.join(",", quote(row.path("file").asText("")), quote(row.path("line").asText("")),
                        quote(row.path("outcome").asText("")), quote(row.path("type").asText("")),
                        quote(row.path("code").asText("")), quote(row.path("name").asText("")),
                        quote(row.path("reason").asText("")))).append('\n');
            }
        } catch (Exception e) {
            throw new IllegalStateException("The report of run " + runId + " could not be read", e);
        }
        return csv.toString();
    }

    @Override
    public String template(String area) {
        if (LocationsImportApi.AREA_LEVELS.equals(area)) {
            return "level,typeName\n1,Region\n2,Province\n3,District\n4,LLG\n";
        }
        if (LocationsImportApi.AREA_VALUES.equals(area)) {
            return "Region,Province,District,LLG\nSouthern Region%R-SOU,National Capital District%P-NCD,Moresby"
                    + " South%NCD-MS,\n";
        }
        String header = String.join(",", BASE_COLUMNS) + ",identifier:DHIS2 ID\n";
        return header + "referring clinic;referralLab,PMGH,Port Moresby General Hospital,PMGH,,NCD-MS,,Y,Taurama"
                + " Road,Port Moresby,,,-9.4705,147.1597,Dr. Anna Kila,+675 324 8200,lab@pmgh.gov.pg,,National"
                + " referral hospital,Government,,,,,,Approved,,,,,,Rp1k2mPqH3x\n"
                + "dept,PMGH-OPD,Outpatient Department,,PMGH,,,Y,,,,,,,Sister Ruth Moi,+675 324 8210,,,,,"
                + "Outpatient,,,,,,,,,,,\n"
                + "sampling site,VT-LAE-03,Bumbu Settlement light trap,,,Lae Urban,LLG,Y,,,,,-6.7160,147.0010,John"
                + " Wari,+675 7123 4567,,,,,,Vector trap,CDC light trap,Peri-urban settlement,Weekly collection,,,,,,,"
                + "\n";
    }

    @Override
    public String export(Query query, List<String> ids) {
        List<Row> rows = new ArrayList<>();
        if (ids != null && !ids.isEmpty()) {
            for (String id : ids) {
                rows.add(locationsService.get(id).row());
            }
        } else {
            Query all = new Query(query.view(), query.q(), query.typeIds(), query.locationId(), query.categoryIds(),
                    query.ownershipIds(), query.status(), query.reviewOverdue(), query.sort(), 1, 100000);
            rows.addAll(locationsService.list(all).items());
        }
        List<Detail> details = new ArrayList<>();
        for (Row row : rows) {
            Detail detail = locationsService.get(row.id());
            details.add(detail);
        }
        Set<String> labels = new LinkedHashSet<>();
        for (Detail detail : details) {
            for (Identifier identifier : detail.identifiers()) {
                if (!OrganizationIdentifier.CODE_LABEL.equalsIgnoreCase(identifier.label())) {
                    labels.add(identifier.label());
                }
            }
        }
        StringBuilder csv = new StringBuilder();
        List<String> header = new ArrayList<>(Arrays.asList(BASE_COLUMNS));
        labels.forEach(label -> header.add(IDENTIFIER_PREFIX + label));
        csv.append(header.stream().map(LocationsImportServiceImpl::quote).collect(Collectors.joining(",")))
                .append('\n');
        for (Detail detail : details) {
            csv.append(exportLine(detail, labels, null)).append('\n');
            if (LocationsApi.KIND_FACILITY.equals(detail.row().kind())) {
                for (Ward ward : detail.wards()) {
                    if (ward.active() || !LocationsApi.STATUS_ACTIVE.equals(query.status())) {
                        csv.append(exportWardLine(ward, detail, labels)).append('\n');
                    }
                }
            }
        }
        return csv.toString();
    }

    private String exportLine(Detail detail, Set<String> labels, String unused) {
        Row row = detail.row();
        Map<String, String> values = new HashMap<>();
        values.put("type", row.types().stream().map(t -> t.name()).collect(Collectors.joining(";")));
        values.put("code", row.code());
        values.put("name", row.name());
        values.put("shortName", row.shortName());
        if (row.location() != null) {
            Organization area = organizationService.get(row.location().id());
            values.put("parentCode", area == null ? "" : area.getCode());
            values.put("parentName", row.location().name());
            values.put("parentType", row.location().levelName());
        }
        values.put("active", row.active() ? "Y" : "N");
        values.put("streetAddress", detail.streetAddress());
        values.put("city", detail.city());
        values.put("state", detail.state());
        values.put("zipCode", detail.zipCode());
        values.put("gpsLatitude",
                detail.gpsLatitude() == null ? "" : detail.gpsLatitude().stripTrailingZeros().toPlainString());
        values.put("gpsLongitude",
                detail.gpsLongitude() == null ? "" : detail.gpsLongitude().stripTrailingZeros().toPlainString());
        values.put("contactName", detail.contactName());
        values.put("phone", detail.phone());
        values.put("email", detail.email());
        values.put("internetAddress", detail.internetAddress());
        values.put("category", row.category() == null ? "" : row.category().label());
        values.put("ownership", row.ownership() == null ? "" : row.ownership().label());
        values.put("serviceType", "");
        if (detail.site() != null) {
            values.put("siteType", detail.site().siteType());
            values.put("subtype", detail.site().subtype());
            values.put("environmentalZone", detail.site().environmentalZone());
        }
        values.put("description", detail.description());
        if (detail.referral() != null) {
            values.put("approvalStatus", detail.referral().approvalStatus());
            values.put("accreditationBody", detail.referral().accreditationBody());
            values.put("accreditationNumber", detail.referral().accreditationNumber());
            values.put("accreditationExpiry", detail.referral().accreditationExpiry());
            values.put("lastReviewDate", detail.referral().lastReviewDate());
            values.put("nextReviewDue", detail.referral().nextReviewDue());
        }
        for (Identifier identifier : detail.identifiers()) {
            values.put(IDENTIFIER_PREFIX + identifier.label(), identifier.value());
        }
        return line(values, labels);
    }

    private String exportWardLine(Ward ward, Detail parent, Set<String> labels) {
        Map<String, String> values = new HashMap<>();
        values.put("type", LocationsServiceImpl.DEPT_TYPE);
        values.put("code", ward.code());
        values.put("name", ward.name());
        values.put("parentCode", parent.row().code());
        values.put("parentName", parent.row().name());
        values.put("active", ward.active() ? "Y" : "N");
        if (!ward.gpsInherited()) {
            values.put("gpsLatitude",
                    ward.gpsLatitude() == null ? "" : ward.gpsLatitude().stripTrailingZeros().toPlainString());
            values.put("gpsLongitude",
                    ward.gpsLongitude() == null ? "" : ward.gpsLongitude().stripTrailingZeros().toPlainString());
        }
        values.put("contactName", ward.contactName());
        values.put("phone", ward.phone());
        values.put("email", ward.email());
        WardServiceType service = WardServiceType.parse(ward.serviceType());
        values.put("serviceType", service == null ? ward.serviceType() : service.getLabel());
        return line(values, labels);
    }

    private static String line(Map<String, String> values, Set<String> labels) {
        List<String> cells = new ArrayList<>();
        for (String column : BASE_COLUMNS) {
            cells.add(quote(values.getOrDefault(column, "")));
        }
        for (String label : labels) {
            cells.add(quote(values.getOrDefault(IDENTIFIER_PREFIX + label, "")));
        }
        return String.join(",", cells);
    }

    @Override
    public String exportAreas() {
        List<OrganizationType> levels = organizationTypeService.getAllOrganizationTypes().stream()
                .filter(type -> type.getHierarchyLevel() != null && type.getHierarchyLevel() > 0)
                .sorted((a, b) -> Integer.compare(a.getHierarchyLevel(), b.getHierarchyLevel()))
                .collect(Collectors.toList());
        StringBuilder csv = new StringBuilder();
        csv.append(levels.stream().map(OrganizationType::getName).map(LocationsImportServiceImpl::quote)
                .collect(Collectors.joining(","))).append('\n');
        List<Organization> areas = organizationService.getAllWithTypes().stream()
                .filter(organization -> LocationsApi.KIND_AREA.equals(LocationsServiceImpl.kindOf(organization)))
                .sorted((a, b) -> pathKey(a).compareTo(pathKey(b))).collect(Collectors.toList());
        for (Organization area : areas) {
            List<Organization> chain = new ArrayList<>();
            Organization current = area;
            Set<String> guard = new HashSet<>();
            while (current != null && current.getId() != null && guard.add(current.getId())) {
                chain.add(0, current);
                current = realParent(current);
            }
            List<String> cells = new ArrayList<>();
            for (int i = 0; i < levels.size(); i++) {
                if (i < chain.size()) {
                    Organization node = chain.get(i);
                    String code = GenericValidator.isBlankOrNull(node.getCode()) ? "" : "%" + node.getCode();
                    cells.add(quote(node.getOrganizationName() + code));
                } else {
                    cells.add("");
                }
            }
            csv.append(String.join(",", cells)).append('\n');
        }
        return csv.toString();
    }

    private static String quote(String value) {
        String text = value == null ? "" : value;
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
