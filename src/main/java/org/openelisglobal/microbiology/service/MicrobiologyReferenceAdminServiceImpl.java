package org.openelisglobal.microbiology.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstPanelAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstPanelAssignmentDAO;
import org.openelisglobal.microbiology.dao.MicroAstPanelDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointRuleDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.dao.MicroPatientOriginDAO;
import org.openelisglobal.microbiology.form.MicroAntibioticAdminForm;
import org.openelisglobal.microbiology.form.MicroAstPanelAdminForm;
import org.openelisglobal.microbiology.form.MicroAstPanelAntibioticAdminForm;
import org.openelisglobal.microbiology.form.MicroOrganismAdminForm;
import org.openelisglobal.microbiology.form.MicroPanelCoverageForm;
import org.openelisglobal.microbiology.form.MicroPatientOriginAdminForm;
import org.openelisglobal.microbiology.form.MicroReferenceAdminPageForm;
import org.openelisglobal.microbiology.form.MicroReferenceAdminQueryForm;
import org.openelisglobal.microbiology.form.MicroReferenceOptionForm;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroAstPanelAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstPanelAssignment;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.microbiology.valueholder.MicroPatientOrigin;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicrobiologyReferenceAdminServiceImpl implements MicrobiologyReferenceAdminService {

    private static final Pattern ORGANISM_WHONET = Pattern.compile("[a-z0-9]{3,10}");
    private static final Pattern ANTIBIOTIC_WHONET = Pattern.compile("[A-Z0-9]{2,10}");
    private static final Set<String> ROUTES = Set.of("ORAL", "IV", "BOTH", "TOPICAL");

    private final MicroOrganismDAO organismDAO;
    private final MicroAntibioticDAO antibioticDAO;
    private final MicroAstPanelDAO panelDAO;
    private final MicroAstPanelAntibioticDAO panelAntibioticDAO;
    private final MicroAstPanelAssignmentDAO assignmentDAO;
    private final MicroBreakpointRuleDAO breakpointRuleDAO;
    private final MicroPatientOriginDAO patientOriginDAO;
    private final MethodService methodService;
    private final TypeOfSampleService typeOfSampleService;

    public MicrobiologyReferenceAdminServiceImpl(MicroOrganismDAO organismDAO, MicroAntibioticDAO antibioticDAO,
            MicroAstPanelDAO panelDAO, MicroAstPanelAntibioticDAO panelAntibioticDAO,
            MicroPatientOriginDAO patientOriginDAO, MethodService methodService,
            TypeOfSampleService typeOfSampleService, MicroAstPanelAssignmentDAO assignmentDAO,
            MicroBreakpointRuleDAO breakpointRuleDAO) {
        this.organismDAO = organismDAO;
        this.antibioticDAO = antibioticDAO;
        this.panelDAO = panelDAO;
        this.panelAntibioticDAO = panelAntibioticDAO;
        this.assignmentDAO = assignmentDAO;
        this.breakpointRuleDAO = breakpointRuleDAO;
        this.patientOriginDAO = patientOriginDAO;
        this.methodService = methodService;
        this.typeOfSampleService = typeOfSampleService;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroReferenceAdminPageForm<MicroOrganismAdminForm> getOrganisms(MicroReferenceAdminQueryForm query) {
        MicroReferenceAdminQueryForm normalized = normalizeQuery(query);
        int offset = (normalized.page - 1) * normalized.pageSize;
        List<MicroOrganismAdminForm> rows = organismDAO.search(normalized.q, normalized.status, normalized.category,
                normalized.sort, offset, normalized.pageSize).stream().map(this::toOrganismForm).toList();
        return page(rows, organismDAO.countSearch(normalized.q, normalized.status, normalized.category), normalized);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroOrganismAdminForm getOrganism(String id) {
        return toOrganismForm(
                organismDAO.get(id).orElseThrow(() -> new IllegalArgumentException("Organism not found: " + id)));
    }

    @Override
    @Transactional
    public MicroOrganismAdminForm saveOrganism(String id, MicroOrganismAdminForm request, String actorId) {
        requireActor(actorId);
        if (request == null) {
            throw new IllegalArgumentException("Organism is required");
        }
        String name = requireText(request.displayName, "displayName");
        String code = normalizeOrganismCode(request.whonetCode);
        requireText(request.organismGroup, "organismGroup");
        requireText(request.initialSignificance, "initialSignificance");
        assertAvailable(organismDAO.findByDisplayNameIgnoreCase(name), id, "Organism name is already in use");
        assertAvailable(organismDAO.findByWhonetCodeIgnoreCase(code), id, "WHONET code is already in use");

        MicroOrganism organism = id == null ? new MicroOrganism()
                : organismDAO.get(id).orElseThrow(() -> new IllegalArgumentException("Organism not found: " + id));
        organism.setDisplayName(name);
        organism.setShortName(trimToNull(request.shortName));
        organism.setWhonetCode(code);
        organism.setOclCode(trimToNull(request.oclCode));
        organism.setOrganismGroup(request.organismGroup.trim());
        organism.setSusceptibilityProtocol(
                MicroTbResistanceClassifier.Protocol.valueOf(request.susceptibilityProtocol).name());
        organism.setGramStain(trimToNull(request.gramStain));
        organism.setInitialSignificance(request.initialSignificance.trim().toUpperCase(Locale.ROOT));
        organism.setDefaultAstPanelId(trimToNull(request.defaultAstPanelId));
        organism.setNotes(trimToNull(request.notes));
        organism.setIsActive(request.active ? "Y" : "N");
        organism.setLastUpdatedBy(actorId);
        if (id == null) {
            organismDAO.insert(organism);
        } else {
            organismDAO.update(organism);
        }
        return toOrganismForm(organism);
    }

    @Override
    @Transactional
    public MicroOrganismAdminForm setOrganismActive(String id, boolean active, String actorId) {
        requireActor(actorId);
        MicroOrganism organism = organismDAO.get(id)
                .orElseThrow(() -> new IllegalArgumentException("Organism not found: " + id));
        organism.setIsActive(active ? "Y" : "N");
        organism.setLastUpdatedBy(actorId);
        organismDAO.update(organism);
        MicroOrganismAdminForm form = toOrganismForm(organism);
        form.referenceCount = organismDAO.countWorkflowReferences(id);
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroReferenceAdminPageForm<MicroAntibioticAdminForm> getAntibiotics(MicroReferenceAdminQueryForm query) {
        MicroReferenceAdminQueryForm normalized = normalizeQuery(query);
        int offset = (normalized.page - 1) * normalized.pageSize;
        List<MicroAntibioticAdminForm> rows = antibioticDAO.search(normalized.q, normalized.status, normalized.category,
                normalized.sort, offset, normalized.pageSize).stream().map(this::toAntibioticForm).toList();
        return page(rows, antibioticDAO.countSearch(normalized.q, normalized.status, normalized.category), normalized);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroAntibioticAdminForm getAntibiotic(String id) {
        return toAntibioticForm(
                antibioticDAO.get(id).orElseThrow(() -> new IllegalArgumentException("Antibiotic not found: " + id)));
    }

    @Override
    @Transactional
    public MicroAntibioticAdminForm saveAntibiotic(String id, MicroAntibioticAdminForm request, String actorId) {
        requireActor(actorId);
        if (request == null) {
            throw new IllegalArgumentException("Antibiotic is required");
        }
        String name = requireText(request.displayName, "displayName");
        String code = normalizeAntibioticCode(request.whonetCode);
        String antibioticClass = requireText(request.antibioticClass, "antibioticClass");
        String route = requireText(request.route, "route").toUpperCase(Locale.ROOT);
        if (!ROUTES.contains(route)) {
            throw new IllegalArgumentException("route must be ORAL, IV, BOTH, or TOPICAL");
        }
        assertAvailable(antibioticDAO.findByDisplayNameIgnoreCase(name), id, "Antibiotic name is already in use");
        assertAvailable(antibioticDAO.findByWhonetCodeIgnoreCase(code), id, "WHONET code is already in use");

        MicroAntibiotic antibiotic = id == null ? new MicroAntibiotic()
                : antibioticDAO.get(id).orElseThrow(() -> new IllegalArgumentException("Antibiotic not found: " + id));
        antibiotic.setDisplayName(name);
        antibiotic.setWhonetCode(code);
        antibiotic.setAntibioticClass(antibioticClass);
        antibiotic.setResistanceRole(MicroTbResistanceClassifier.Role.valueOf(request.resistanceRole).name());
        antibiotic.setRoute(route);
        antibiotic.setNotes(trimToNull(request.notes));
        antibiotic.setIsActive(request.active ? "Y" : "N");
        antibiotic.setLastUpdatedBy(actorId);
        if (id == null) {
            antibioticDAO.insert(antibiotic);
        } else {
            antibioticDAO.update(antibiotic);
        }
        return toAntibioticForm(antibiotic);
    }

    @Override
    @Transactional
    public MicroAntibioticAdminForm setAntibioticActive(String id, boolean active, String actorId) {
        requireActor(actorId);
        MicroAntibiotic antibiotic = antibioticDAO.get(id)
                .orElseThrow(() -> new IllegalArgumentException("Antibiotic not found: " + id));
        antibiotic.setIsActive(active ? "Y" : "N");
        antibiotic.setLastUpdatedBy(actorId);
        antibioticDAO.update(antibiotic);
        MicroAntibioticAdminForm form = toAntibioticForm(antibiotic);
        form.referenceCount = antibioticDAO.countWorkflowReferences(id);
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroReferenceAdminPageForm<MicroAstPanelAdminForm> getAstPanels(MicroReferenceAdminQueryForm query) {
        MicroReferenceAdminQueryForm normalized = normalizeQuery(query);
        int offset = (normalized.page - 1) * normalized.pageSize;
        List<MicroAstPanelAdminForm> rows = panelDAO.search(normalized.q, normalized.status, normalized.organismGroup,
                normalized.sort, offset, normalized.pageSize).stream().map(this::toPanelForm).toList();
        return page(rows, panelDAO.countSearch(normalized.q, normalized.status, normalized.organismGroup), normalized);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroAstPanelAdminForm getAstPanel(String id) {
        return toPanelForm(
                panelDAO.get(id).orElseThrow(() -> new IllegalArgumentException("AST panel not found: " + id)));
    }

    @Override
    @Transactional
    public MicroAstPanelAdminForm createPanel(MicroAstPanelAdminForm request, String actorId) {
        requireActor(actorId);
        MicroAstPanel panel = new MicroAstPanel();
        panel.setLogicalKey(UUID.randomUUID().toString());
        panel.setVersionNumber(1);
        applyPanel(panel, request, actorId);
        boolean defaultsChanged = applyDefaultFor(panel, request.defaultFor);
        panelDAO.insert(panel);
        savePanelRows(panel.getId(), request.antibiotics);
        if (defaultsChanged) {
            assignPanelDefaults(Map.of(), actorId);
        }
        return toPanelForm(panel);
    }

    @Override
    @Transactional
    public MicroAstPanelAdminForm publishPanelVersion(String currentPanelId, MicroAstPanelAdminForm request,
            String actorId) {
        requireActor(actorId);
        MicroAstPanel selected = panelDAO.get(currentPanelId)
                .orElseThrow(() -> new IllegalArgumentException("AST panel not found: " + currentPanelId));
        MicroAstPanel current = panelDAO.findCurrentByLogicalKey(selected.getLogicalKey());
        if (current == null || !current.getId().equals(currentPanelId)) {
            throw new MicroReferenceConflictException("A newer AST panel version already exists");
        }
        current.setIsCurrent("N");
        panelDAO.update(current);

        MicroAstPanel published = new MicroAstPanel();
        published.setLogicalKey(current.getLogicalKey());
        published.setVersionNumber(current.getVersionNumber() + 1);
        published.setSupersedesPanelId(current.getId());
        published.setDefaultFor(current.getDefaultFor());
        published.setDefaultForFile(current.getDefaultForFile());
        published.setConfigurationContent(current.getConfigurationContent());
        applyPanel(published, request, actorId);
        boolean defaultsChanged = applyDefaultFor(published, request.defaultFor);
        panelDAO.insert(published);
        savePanelRows(published.getId(), request.antibiotics);
        for (MicroOrganism organism : organismDAO.getByDefaultAstPanelId(current.getId())) {
            organism.setDefaultAstPanelId(published.getId());
            organism.setLastUpdatedBy(actorId);
            organismDAO.update(organism);
        }
        for (MicroAstPanelAssignment assignment : assignmentDAO.getOpenByPanelId(current.getId())) {
            assignment.setPanelId(published.getId());
            assignmentDAO.update(assignment);
        }
        if (defaultsChanged) {
            assignPanelDefaults(Map.of(), actorId);
        }
        return toPanelForm(published);
    }

    private static boolean applyDefaultFor(MicroAstPanel panel, List<String> requested) {
        if (requested == null) {
            return false;
        }
        List<String> wanted = defaultForEntries(String.join(";", requested));
        if (wanted.equals(defaultForEntries(panel.getDefaultFor()))) {
            return false;
        }
        panel.setDefaultFor(String.join("; ", wanted));
        panel.setDefaultForFile(null);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroPanelCoverageForm> getPanelOrganismCoverage(List<String> antibioticIds, String protocol) {
        Set<String> drugs = new HashSet<>(antibioticIds);
        Map<String, Set<String>> byOrganism = new HashMap<>();
        Map<String, Set<String>> byGroup = new HashMap<>();
        Set<String> forEveryOrganism = new HashSet<>();
        for (MicroBreakpointRule rule : breakpointRuleDAO.getActiveByAntibioticIds(drugs)) {
            Set<String> covered = rule.getOrganismId() != null
                    ? byOrganism.computeIfAbsent(rule.getOrganismId(), key -> new HashSet<>())
                    : rule.getOrganismGroup() != null
                            ? byGroup.computeIfAbsent(rule.getOrganismGroup(), key -> new HashSet<>())
                            : forEveryOrganism;
            covered.add(rule.getAntibioticId());
        }
        List<MicroPanelCoverageForm> rows = new ArrayList<>();
        Map<String, Set<String>> labGroups = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (MicroOrganism organism : organismDAO.getActiveOrganisms()) {
            if (!Objects.equals(protocol, organism.getSusceptibilityProtocol())) {
                continue;
            }
            Set<String> covered = new HashSet<>(forEveryOrganism);
            covered.addAll(byOrganism.getOrDefault(organism.getId(), Set.of()));
            covered.addAll(byGroup.getOrDefault(organism.getOrganismGroup(), Set.of()));
            rows.add(coverage(organism.getDisplayName(), false, covered.size(), drugs.size()));
            for (String group : organism.getLabGroups()) {
                labGroups.computeIfAbsent(group, key -> new HashSet<>()).addAll(covered);
            }
        }
        labGroups.forEach((group, covered) -> rows.add(coverage(group, true, covered.size(), drugs.size())));
        rows.sort(Comparator.comparingInt((MicroPanelCoverageForm row) -> -row.covered).thenComparing(row -> row.value,
                String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    private static MicroPanelCoverageForm coverage(String value, boolean group, int covered, int total) {
        MicroPanelCoverageForm form = new MicroPanelCoverageForm();
        form.value = value;
        form.group = group;
        form.covered = covered;
        form.total = total;
        return form;
    }

    @Override
    @Transactional
    public void assignPanelDefaults(Map<String, Integer> lines, String actorId) {
        List<MicroAstPanel> claimants = panelDAO.getAll().stream()
                .filter(p -> "Y".equals(p.getIsCurrent()) && "Y".equals(p.getIsActive())
                        && !defaultForEntries(p.getDefaultFor()).isEmpty())
                .sorted(Comparator.comparing(MicroAstPanel::getName, String.CASE_INSENSITIVE_ORDER)).toList();
        Map<MicroOrganism, MicroAstPanel> winners = new LinkedHashMap<>();
        Set<String> matched = new HashSet<>();
        List<String> errors = new ArrayList<>();
        for (var organism : organismDAO.getActiveOrganisms()) {
            Map<MicroAstPanel, String> bestEntries = new LinkedHashMap<>();
            int bestRank = 0;
            for (var panel : claimants) {
                for (String entry : defaultForEntries(panel.getDefaultFor())) {
                    int rank = specificity(entry, organism.getDisplayName(), organism.getLabGroups());
                    if (rank > 0) {
                        matched.add(panel.getId() + "\n" + entry);
                    }
                    if (rank > bestRank) {
                        bestRank = rank;
                        bestEntries.clear();
                    }
                    if (rank > 0 && rank == bestRank) {
                        bestEntries.putIfAbsent(panel, entry);
                    }
                }
            }
            if (bestEntries.isEmpty()) {
                continue;
            }
            var tied = new ArrayList<>(bestEntries.entrySet());
            if (tied.size() > 1) {
                errors.add(line(lines, bestEntries.keySet().toArray(MicroAstPanel[]::new)) + organism.getDisplayName()
                        + " is claimed equally specifically by " + tied
                                .stream().map(claim -> "\"" + claim.getKey().getName() + "\" (\"" + claim.getValue()
                                        + "\", " + source(lines, claim.getKey()) + ")")
                                .collect(Collectors.joining(", ")));
                continue;
            }
            MicroAstPanel best = tied.get(0).getKey();
            String bestEntry = tied.get(0).getValue();
            // Identification assigns the default to every organism except NTM, and
            // refuses a panel whose protocol differs from the organism's.
            if (!"NTM".equals(organism.getSusceptibilityProtocol())
                    && !Objects.equals(organism.getSusceptibilityProtocol(), best.getSusceptibilityProtocol())) {
                errors.add(line(lines, best) + "panel \"" + best.getName() + "\" uses the "
                        + best.getSusceptibilityProtocol() + " protocol but its defaultFor \"" + bestEntry
                        + "\" matches " + organism.getDisplayName() + ", which uses "
                        + organism.getSusceptibilityProtocol());
            } else {
                winners.put(organism, best);
            }
        }
        for (var panel : claimants) {
            if (lines.containsKey(panel.getId())) {
                for (String entry : defaultForEntries(panel.getDefaultFor())) {
                    if (!matched.contains(panel.getId() + "\n" + entry)) {
                        errors.add(line(lines, panel) + "Panel \"" + panel.getName() + "\": defaultFor \"" + entry
                                + "\" matches no organism");
                    }
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
        winners.forEach((organism, panel) -> {
            if (!panel.getId().equals(organism.getDefaultAstPanelId())) {
                organism.setDefaultAstPanelId(panel.getId());
                organism.setLastUpdatedBy(actorId);
                organismDAO.update(organism);
            }
        });
    }

    static List<String> defaultForEntries(String defaultFor) {
        return defaultFor == null ? List.of()
                : Stream.of(defaultFor.split(";")).map(String::trim).filter(entry -> !entry.isEmpty()).toList();
    }

    private static String source(Map<String, Integer> lines, MicroAstPanel panel) {
        return lines.containsKey(panel.getId()) ? "line " + lines.get(panel.getId())
                : Objects.requireNonNullElse(panel.getDefaultForFile(), "AST panels page");
    }

    private static String line(Map<String, Integer> lines, MicroAstPanel... panels) {
        List<String> numbers = Stream.of(panels).map(panel -> lines.get(panel.getId())).filter(Objects::nonNull)
                .sorted().map(String::valueOf).toList();
        return numbers.isEmpty() ? ""
                : (numbers.size() == 1 ? "Line " : "Lines ") + String.join(" and ", numbers) + ": ";
    }

    /**
     * 3 for the species itself, 2 for its genus, 1 for one of its laboratory
     * groups, 0 otherwise.
     */
    static int specificity(String entry, String organismName, Set<String> labGroups) {
        if (entry.isBlank() || organismName == null) {
            return 0;
        }
        String name = organismName.trim();
        if (name.equalsIgnoreCase(entry)) {
            return 3;
        }
        if (name.toLowerCase(Locale.ROOT).startsWith(entry.toLowerCase(Locale.ROOT) + " ")) {
            return 2;
        }
        return labGroups.stream().anyMatch(entry::equalsIgnoreCase) ? 1 : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroReferenceAdminPageForm<MicroPatientOriginAdminForm> getPatientOrigins(
            MicroReferenceAdminQueryForm query) {
        MicroReferenceAdminQueryForm normalized = normalizeQuery(query);
        int offset = (normalized.page - 1) * normalized.pageSize;
        List<MicroPatientOriginAdminForm> rows = patientOriginDAO
                .search(normalized.q, normalized.status, normalized.sort, offset, normalized.pageSize).stream()
                .map(this::toPatientOriginForm).toList();
        return page(rows, patientOriginDAO.countSearch(normalized.q, normalized.status), normalized);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroReferenceOptionForm> getOptions(String resource) {
        if ("methods".equals(resource)) {
            return methodService.getAllActiveMethods().stream()
                    .sorted(Comparator.comparing(Method::getMethodName, String.CASE_INSENSITIVE_ORDER))
                    .map(method -> option(method.getId(), method.getMethodName(), method.getCode())).toList();
        }
        if ("organism-groups".equals(resource)) {
            return organismDAO.getActiveOrganismGroups().stream().filter(group -> !group.isBlank())
                    .sorted(String.CASE_INSENSITIVE_ORDER).map(group -> option(group, group, null)).toList();
        }
        if ("lab-organism-groups".equals(resource)) {
            return organismDAO.getLabGroupNames().stream().map(group -> option(group, group, null)).toList();
        }
        if ("antibiotics".equals(resource)) {
            return antibioticDAO.getActiveAntibiotics().stream().map(
                    antibiotic -> option(antibiotic.getId(), antibiotic.getDisplayName(), antibiotic.getWhonetCode()))
                    .toList();
        }
        if ("specimen-types".equals(resource)) {
            return typeOfSampleService.getAllTypeOfSamplesSortOrdered().stream().filter(type -> type.getIsActive())
                    .map(type -> option(type.getId(), type.getLocalizedName(), type.getLocalAbbreviation())).toList();
        }
        if ("ast-panels".equals(resource)) {
            return panelDAO.search("", "ACTIVE", null, "name", 0, 1000).stream()
                    .filter(panel -> "Y".equals(panel.getIsCurrent()))
                    .map(panel -> option(panel.getId(), panel.getName(), "v" + panel.getVersionNumber())).toList();
        }
        throw new IllegalArgumentException("Unsupported option resource: " + resource);
    }

    private void applyPanel(MicroAstPanel panel, MicroAstPanelAdminForm request, String actorId) {
        if (request == null) {
            throw new IllegalArgumentException("AST panel is required");
        }
        panel.setName(requireText(request.name, "name"));
        panel.setOrganismGroup(trimToNull(request.organismGroup));
        panel.setSusceptibilityProtocol(
                MicroTbResistanceClassifier.Protocol.valueOf(request.susceptibilityProtocol).name());
        panel.setSpecimenTypeId(trimToNull(request.specimenTypeId));
        panel.setIsActive(request.active ? "Y" : "N");
        panel.setIsCurrent("Y");
        panel.setPublishedAt(new Timestamp(System.currentTimeMillis()));
        panel.setPublishedBy(actorId);
        validatePanelRows(request.antibiotics);
    }

    private void validatePanelRows(List<MicroAstPanelAntibioticAdminForm> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("At least one antibiotic is required");
        }
        Set<String> seen = new java.util.HashSet<>();
        for (MicroAstPanelAntibioticAdminForm row : rows) {
            String antibioticId = requireText(row.antibioticId, "antibioticId");
            MicroAntibiotic antibiotic = antibioticDAO.get(antibioticId)
                    .orElseThrow(() -> new IllegalArgumentException("Antibiotic not found: " + antibioticId));
            if (!"Y".equals(antibiotic.getIsActive())) {
                throw new IllegalArgumentException("Inactive antibiotic cannot be added: " + antibioticId);
            }
            if (!seen.add(antibioticId)) {
                throw new IllegalArgumentException("Duplicate antibiotic: " + antibioticId);
            }
            if (row.diskContent != null && row.diskContent.trim().length() > 40) {
                throw new IllegalArgumentException("Disk content is longer than 40 characters: " + antibioticId);
            }
        }
    }

    private void savePanelRows(String panelId, List<MicroAstPanelAntibioticAdminForm> rows) {
        int order = 1;
        for (MicroAstPanelAntibioticAdminForm request : rows) {
            MicroAstPanelAntibiotic row = new MicroAstPanelAntibiotic();
            row.setPanelId(panelId);
            row.setAntibioticId(request.antibioticId);
            row.setDisplayOrder(order++);
            row.setDiskContent(trimToNull(request.diskContent));
            panelAntibioticDAO.insert(row);
        }
    }

    private MicroOrganismAdminForm toOrganismForm(MicroOrganism organism) {
        MicroOrganismAdminForm form = new MicroOrganismAdminForm();
        form.id = organism.getId();
        form.displayName = organism.getDisplayName();
        form.shortName = organism.getShortName();
        form.whonetCode = organism.getWhonetCode();
        form.oclCode = organism.getOclCode();
        form.organismGroup = organism.getOrganismGroup();
        form.labGroups = organism.getLabGroups().isEmpty() ? null
                : String.join("; ", new java.util.TreeSet<>(organism.getLabGroups()));
        form.susceptibilityProtocol = organism.getSusceptibilityProtocol();
        form.gramStain = organism.getGramStain();
        form.initialSignificance = organism.getInitialSignificance();
        form.defaultAstPanelId = organism.getDefaultAstPanelId();
        if (organism.getDefaultAstPanelId() != null) {
            form.defaultAstPanelName = panelDAO.get(organism.getDefaultAstPanelId()).map(MicroAstPanel::getName)
                    .orElse(null);
        }
        form.notes = organism.getNotes();
        form.active = "Y".equals(organism.getIsActive());
        form.seeded = organism.isSeeded();
        form.referenceCount = organismDAO.countWorkflowReferences(organism.getId());
        return form;
    }

    private MicroAntibioticAdminForm toAntibioticForm(MicroAntibiotic antibiotic) {
        MicroAntibioticAdminForm form = new MicroAntibioticAdminForm();
        form.id = antibiotic.getId();
        form.displayName = antibiotic.getDisplayName();
        form.whonetCode = antibiotic.getWhonetCode();
        form.antibioticClass = antibiotic.getAntibioticClass();
        form.resistanceRole = antibiotic.getResistanceRole();
        form.route = antibiotic.getRoute();
        form.notes = antibiotic.getNotes();
        form.active = "Y".equals(antibiotic.getIsActive());
        form.seeded = antibiotic.isSeeded();
        form.referenceCount = antibioticDAO.countWorkflowReferences(antibiotic.getId());
        return form;
    }

    private MicroAstPanelAdminForm toPanelForm(MicroAstPanel panel) {
        MicroAstPanelAdminForm form = new MicroAstPanelAdminForm();
        form.id = panel.getId();
        form.logicalKey = panel.getLogicalKey();
        form.versionNumber = panel.getVersionNumber();
        form.supersedesPanelId = panel.getSupersedesPanelId();
        form.name = panel.getName();
        form.organismGroup = panel.getOrganismGroup();
        form.susceptibilityProtocol = panel.getSusceptibilityProtocol();
        form.specimenTypeId = panel.getSpecimenTypeId();
        form.defaultFor = defaultForEntries(panel.getDefaultFor());
        form.active = "Y".equals(panel.getIsActive());
        form.current = "Y".equals(panel.getIsCurrent());
        form.publishedBy = panel.getPublishedBy();
        form.publishedAt = panel.getPublishedAt() == null ? null : panel.getPublishedAt().toInstant().toString();
        form.antibiotics = new ArrayList<>();
        for (MicroAstPanelAntibiotic row : panelAntibioticDAO.getByPanelId(panel.getId())) {
            MicroAstPanelAntibioticAdminForm item = new MicroAstPanelAntibioticAdminForm();
            item.id = row.getId();
            item.antibioticId = row.getAntibioticId();
            antibioticDAO.get(row.getAntibioticId()).ifPresent(antibiotic -> {
                item.antibioticName = antibiotic.getDisplayName();
                item.whonetCode = antibiotic.getWhonetCode();
            });
            item.displayOrder = row.getDisplayOrder();
            item.diskContent = row.getDiskContent();
            form.antibiotics.add(item);
        }
        return form;
    }

    private MicroPatientOriginAdminForm toPatientOriginForm(MicroPatientOrigin origin) {
        MicroPatientOriginAdminForm form = new MicroPatientOriginAdminForm();
        form.id = origin.getId();
        form.code = origin.getCode();
        form.displayName = origin.getDisplayName();
        form.whonetCode = origin.getWhonetCode();
        form.active = "Y".equals(origin.getIsActive());
        return form;
    }

    private MicroReferenceOptionForm option(String id, String label, String code) {
        MicroReferenceOptionForm option = new MicroReferenceOptionForm();
        option.id = id;
        option.label = label;
        option.code = code;
        return option;
    }

    private <T> MicroReferenceAdminPageForm<T> page(List<T> rows, long total, MicroReferenceAdminQueryForm query) {
        MicroReferenceAdminPageForm<T> page = new MicroReferenceAdminPageForm<>();
        page.rows = new ArrayList<>(rows);
        page.total = total;
        page.page = query.page;
        page.pageSize = query.pageSize;
        return page;
    }

    private MicroReferenceAdminQueryForm normalizeQuery(MicroReferenceAdminQueryForm input) {
        MicroReferenceAdminQueryForm query = input == null ? new MicroReferenceAdminQueryForm() : input;
        query.q = query.q == null ? "" : query.q.trim();
        query.status = query.status == null || query.status.isBlank() ? "ALL" : query.status.toUpperCase(Locale.ROOT);
        query.status = Set.of("ALL", "ACTIVE", "INACTIVE").contains(query.status) ? query.status : "ALL";
        query.page = Math.max(query.page, 1);
        query.pageSize = Set.of(20, 50, 100).contains(query.pageSize) ? query.pageSize : 20;
        query.sort = Set.of("name", "name-desc").contains(query.sort) ? query.sort : "name";
        return query;
    }

    private <T extends org.openelisglobal.common.valueholder.BaseObject<String>> void assertAvailable(Optional<T> match,
            String currentId, String message) {
        if (match.isPresent() && !match.get().getId().equals(currentId)) {
            throw new MicroReferenceConflictException(message);
        }
    }

    private String normalizeOrganismCode(String code) {
        String normalized = requireText(code, "whonetCode").toLowerCase(Locale.ROOT);
        if (!ORGANISM_WHONET.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Organism WHONET code must be 3-10 lowercase letters or digits");
        }
        return normalized;
    }

    private String normalizeAntibioticCode(String code) {
        String normalized = requireText(code, "whonetCode").toUpperCase(Locale.ROOT);
        if (!ANTIBIOTIC_WHONET.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Antibiotic WHONET code must be 2-10 uppercase letters or digits");
        }
        return normalized;
    }

    private String requireText(String value, String field) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private void requireActor(String actorId) {
        requireText(actorId, "authenticated actor");
    }

    private Integer optionalPositive(Integer value, String field) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return value;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

}
