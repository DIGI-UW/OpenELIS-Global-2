package org.openelisglobal.microbiology.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstPanelAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstPanelDAO;
import org.openelisglobal.microbiology.dao.MicroCultureSetupDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.form.MicroAntibioticAdminForm;
import org.openelisglobal.microbiology.form.MicroAstPanelAdminForm;
import org.openelisglobal.microbiology.form.MicroAstPanelAntibioticAdminForm;
import org.openelisglobal.microbiology.form.MicroCultureSetupAdminForm;
import org.openelisglobal.microbiology.form.MicroOrganismAdminForm;
import org.openelisglobal.microbiology.form.MicroReferenceAdminPageForm;
import org.openelisglobal.microbiology.form.MicroReferenceAdminQueryForm;
import org.openelisglobal.microbiology.form.MicroReferenceOptionForm;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroAstPanelAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicrobiologyReferenceAdminServiceImpl implements MicrobiologyReferenceAdminService {

    private static final Pattern ORGANISM_WHONET = Pattern.compile("[a-z0-9]{3,10}");
    private static final Pattern ANTIBIOTIC_WHONET = Pattern.compile("[A-Z0-9]{2,10}");
    private static final Set<String> ROUTES = Set.of("ORAL", "IV", "BOTH", "TOPICAL");
    private static final Set<String> REPORT_BEHAVIORS = Set.of("ALWAYS", "CASCADE", "SUPPRESS_UNLESS_RESISTANT");

    private final MicroOrganismDAO organismDAO;
    private final MicroAntibioticDAO antibioticDAO;
    private final MicroAstPanelDAO panelDAO;
    private final MicroAstPanelAntibioticDAO panelAntibioticDAO;
    @SuppressWarnings("unused")
    private final MicroCultureSetupDAO cultureSetupDAO;
    private final MethodService methodService;
    private final TypeOfSampleService typeOfSampleService;

    public MicrobiologyReferenceAdminServiceImpl(MicroOrganismDAO organismDAO, MicroAntibioticDAO antibioticDAO,
            MicroAstPanelDAO panelDAO, MicroAstPanelAntibioticDAO panelAntibioticDAO,
            MicroCultureSetupDAO cultureSetupDAO, MethodService methodService,
            TypeOfSampleService typeOfSampleService) {
        this.organismDAO = organismDAO;
        this.antibioticDAO = antibioticDAO;
        this.panelDAO = panelDAO;
        this.panelAntibioticDAO = panelAntibioticDAO;
        this.cultureSetupDAO = cultureSetupDAO;
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
        List<MicroAstPanelAdminForm> rows = panelDAO.search(normalized.q, normalized.status, normalized.workflow,
                normalized.sort, offset, normalized.pageSize).stream().map(this::toPanelForm).toList();
        return page(rows, panelDAO.countSearch(normalized.q, normalized.status, normalized.workflow), normalized);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCultureSetupAdminForm getCultureSetup(String id) {
        return toCultureSetupForm(cultureSetupDAO.get(id)
                .orElseThrow(() -> new IllegalArgumentException("Culture setup not found: " + id)));
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
        panelDAO.insert(panel);
        savePanelRows(panel.getId(), request.antibiotics);
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
        applyPanel(published, request, actorId);
        panelDAO.insert(published);
        savePanelRows(published.getId(), request.antibiotics);
        return toPanelForm(published);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroReferenceAdminPageForm<MicroCultureSetupAdminForm> getCultureSetups(
            MicroReferenceAdminQueryForm query) {
        MicroReferenceAdminQueryForm normalized = normalizeQuery(query);
        int offset = (normalized.page - 1) * normalized.pageSize;
        List<MicroCultureSetupAdminForm> rows = cultureSetupDAO.search(normalized.q, normalized.status,
                normalized.workflow, normalized.sort, offset, normalized.pageSize).stream()
                .map(this::toCultureSetupForm).toList();
        return page(rows, cultureSetupDAO.countSearch(normalized.q, normalized.status, normalized.workflow),
                normalized);
    }

    @Override
    @Transactional
    public MicroCultureSetupAdminForm saveCultureSetup(String id, MicroCultureSetupAdminForm request, String actorId) {
        requireActor(actorId);
        if (request == null) {
            throw new IllegalArgumentException("Culture setup is required");
        }
        String methodId = requireText(request.methodId, "methodId");
        Method method = methodService.findById(methodId);
        if (method == null) {
            throw new IllegalArgumentException("Method not found: " + methodId);
        }
        String workflow = requireText(request.workflowType, "workflowType").toUpperCase(Locale.ROOT);
        MicroWorkflowType.valueOf(workflow);
        Optional<MicroCultureSetup> existingIdentity = cultureSetupDAO.findByMethodAndWorkflowType(methodId, workflow);
        if (existingIdentity.isPresent() && !existingIdentity.get().getId().equals(id)) {
            throw new MicroReferenceConflictException("A culture setup already exists for this Method and workflow");
        }
        MicroCultureSetup setup = id == null ? new MicroCultureSetup()
                : cultureSetupDAO.get(id)
                        .orElseThrow(() -> new IllegalArgumentException("Culture setup not found: " + id));
        setup.setMethodId(methodId);
        setup.setName(requireText(request.name, "name"));
        setup.setWorkflowType(workflow);
        setup.setMediaDefaults(trimToNull(request.mediaDefaults));
        setup.setIncubationDefaults(trimToNull(request.incubationDefaults));
        setup.setAtmosphereDefaults(trimToNull(request.atmosphereDefaults));
        setup.setReportableTestAnalyteId(trimToNull(request.reportableTestAnalyteId));
        setup.setIsActive(request.active ? "Y" : "N");
        setup.setLastUpdatedBy(actorId);
        if (id == null) {
            cultureSetupDAO.insert(setup);
        } else {
            cultureSetupDAO.update(setup);
        }
        return toCultureSetupForm(setup);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroReferenceOptionForm> getOptions(String resource) {
        if ("methods".equals(resource)) {
            return methodService.getAllActiveMethods().stream()
                    .sorted(Comparator.comparing(Method::getMethodName, String.CASE_INSENSITIVE_ORDER))
                    .map(method -> option(method.getId(), method.getMethodName(), method.getCode())).toList();
        }
        if ("organisms".equals(resource)) {
            return organismDAO.getActiveOrganisms().stream()
                    .map(organism -> option(organism.getId(), organism.getDisplayName(), organism.getWhonetCode()))
                    .toList();
        }
        if ("organism-groups".equals(resource)) {
            return organismDAO.getActiveOrganisms().stream().map(MicroOrganism::getOrganismGroup)
                    .filter(group -> group != null && !group.isBlank()).distinct().sorted(String.CASE_INSENSITIVE_ORDER)
                    .map(group -> option(group, group, null)).toList();
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
        String workflow = requireText(request.workflowType, "workflowType").toUpperCase(Locale.ROOT);
        MicroWorkflowType.valueOf(workflow);
        panel.setWorkflowType(workflow);
        panel.setOrganismGroup(trimToNull(request.organismGroup));
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
            if (row.tier == null || row.tier < 1 || row.tier > 3) {
                throw new IllegalArgumentException("tier must be between 1 and 3");
            }
            if (!REPORT_BEHAVIORS.contains(row.reportBehavior)) {
                throw new IllegalArgumentException("Invalid report behavior: " + row.reportBehavior);
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
            row.setTier(request.tier);
            row.setReportBehavior(request.reportBehavior);
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
        form.workflowType = panel.getWorkflowType();
        form.organismGroup = panel.getOrganismGroup();
        form.specimenTypeId = panel.getSpecimenTypeId();
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
            item.tier = row.getTier();
            item.reportBehavior = row.getReportBehavior();
            form.antibiotics.add(item);
        }
        return form;
    }

    private MicroCultureSetupAdminForm toCultureSetupForm(MicroCultureSetup setup) {
        MicroCultureSetupAdminForm form = new MicroCultureSetupAdminForm();
        form.id = setup.getId();
        form.methodId = setup.getMethodId();
        form.methodName = methodName(setup.getMethodId());
        form.name = setup.getName();
        form.workflowType = setup.getWorkflowType();
        form.mediaDefaults = setup.getMediaDefaults();
        form.incubationDefaults = setup.getIncubationDefaults();
        form.atmosphereDefaults = setup.getAtmosphereDefaults();
        form.reportableTestAnalyteId = setup.getReportableTestAnalyteId();
        form.active = "Y".equals(setup.getIsActive());
        return form;
    }

    private String methodName(String methodId) {
        Method method = methodService.findById(methodId);
        return method == null ? null : method.getMethodName();
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

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

}
