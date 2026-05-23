package org.openelisglobal.department.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.service.NoteBookService;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentIsolationService {

    @Autowired
    private org.openelisglobal.notebook.service.NotebookSecurityService notebookSecurityService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    @Autowired
    private NoteBookService noteBookService;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private SampleService sampleService;

    public String getSysUserId(HttpServletRequest request) {
        UserSessionData usd = getUserSessionData(request);
        return usd != null ? String.valueOf(usd.getSystemUserId()) : null;
    }

    public String getLoginLabUnit(HttpServletRequest request) {
        UserSessionData usd = getUserSessionData(request);
        if (usd == null || usd.getLoginLabUnit() == 0) {
            return null;
        }
        TestSection testSection = testSectionService.getTestSectionById(String.valueOf(usd.getLoginLabUnit()));
        if (testSection == null) {
            return String.valueOf(usd.getLoginLabUnit());
        }
        if (testSection.getTestSectionName() != null && !testSection.getTestSectionName().isBlank()) {
            return testSection.getTestSectionName();
        }
        String localizedName = testSection.getLocalizedName();
        return localizedName != null && !localizedName.isBlank() ? localizedName
                : String.valueOf(usd.getLoginLabUnit());
    }

    public boolean hasUnrestrictedDepartmentAccess(HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return false;
        }
        return notebookSecurityService.hasGlobalAdminRole(sysUserId) || userRoleService.userInRole(sysUserId,
                Constants.ROLE_SYSTEM_ADMIN) || hasAllLabUnitsAccess(sysUserId);
    }

    /**
     * Lab departments ({@code test_section}) the user may assign when creating
     * department-owned records (storage rooms, inventory catalog items, etc.).
     * Uses real test sections — not notebook workflow templates.
     */
    @Transactional(readOnly = true)
    public List<Map<String, String>> getAssignableLabDepartments(HttpServletRequest request) {
        if (hasUnrestrictedDepartmentAccess(request)) {
            List<TestSection> active = testSectionService.getAllActiveTestSections();
            return buildDepartmentRows(active != null ? active : List.of(), null);
        }
        return buildDepartmentRows(loadTestSections(getSelectableUserTestSectionIds(request)), null);
    }

    @Transactional(readOnly = true)
    public boolean canAccessSampleItemIdentifier(String identifier, HttpServletRequest request) {
        SampleItem sampleItem = resolveSampleItem(identifier);
        return sampleItem != null && canAccessSampleItem(sampleItem, request);
    }

    @Transactional(readOnly = true)
    public boolean canAccessInventoryItem(InventoryItem item, HttpServletRequest request) {
        if (item == null) {
            return false;
        }
        if (hasUnrestrictedDepartmentAccess(request)) {
            return true;
        }
        Set<String> userDepartmentKeys = getAllowedDepartmentKeys(request);
        if (userDepartmentKeys.isEmpty()) {
            return false;
        }
        Set<String> itemDepartmentKeys = resolveInventoryDepartmentKeys(item);
        return !itemDepartmentKeys.isEmpty() && intersects(userDepartmentKeys, itemDepartmentKeys);
    }

    @Transactional(readOnly = true)
    public List<Map<String, String>> getAssignableWorkflowDepartments(HttpServletRequest request) {
        Map<Integer, String> workflowDepartments = getAccessibleWorkflowDepartmentLabels(request);
        if (workflowDepartments.isEmpty()) {
            return List.of();
        }
        if (hasUnrestrictedDepartmentAccess(request)) {
            return buildDepartmentRows(loadTestSections(workflowDepartments.keySet()), workflowDepartments);
        }
        Set<Integer> selectableIds = getSelectableUserTestSectionIds(request);
        selectableIds.retainAll(workflowDepartments.keySet());
        return buildDepartmentRows(loadTestSections(selectableIds), workflowDepartments);
    }

    @Transactional(readOnly = true)
    public List<Map<String, String>> getAssignableInventoryProjects(HttpServletRequest request, Integer departmentId) {
        Integer effectiveDepartmentId = departmentId;
        boolean unrestricted = hasUnrestrictedDepartmentAccess(request);
        if (!unrestricted) {
            Set<Integer> restrictedIds = getRestrictedUserTestSectionIds(request);
            if (restrictedIds.isEmpty()) {
                return List.of();
            }
            Integer activeDepartmentId = restrictedIds.iterator().next();
            if (effectiveDepartmentId != null && !activeDepartmentId.equals(effectiveDepartmentId)) {
                return List.of();
            }
            effectiveDepartmentId = activeDepartmentId;
        } else if (effectiveDepartmentId == null) {
            // Admin/unrestricted users must pick an owning department first.
            return List.of();
        }

        String sysUserId = getSysUserId(request);
        String loginLabUnit = getLoginLabUnit(request);
        Map<String, String> projectsById = new HashMap<>();
        for (NoteBook template : noteBookService.getAllParentTemplates()) {
            if (template == null || template.getId() == null) {
                continue;
            }
            if (!unrestricted && !notebookSecurityService.canViewTemplate(template.getId(), sysUserId, loginLabUnit)) {
                continue;
            }
            Set<Integer> notebookDepartmentIds = resolveNotebookDepartmentIds(template);
            if (effectiveDepartmentId != null && !notebookDepartmentIds.contains(effectiveDepartmentId)) {
                continue;
            }

            List<NoteBook> children = noteBookService.getChildInstances(template.getId());
            boolean addedChild = false;
            if (children != null) {
                for (NoteBook child : children) {
                    if (child == null || child.getId() == null || child.getTitle() == null
                            || child.getTitle().isBlank()) {
                        continue;
                    }
                    Set<Integer> childDepartmentIds = resolveNotebookDepartmentIds(child);
                    if (effectiveDepartmentId != null && !childDepartmentIds.contains(effectiveDepartmentId)) {
                        continue;
                    }
                    projectsById.putIfAbsent(String.valueOf(child.getId()), child.getTitle().trim());
                    addedChild = true;
                }
            }

            if (!addedChild) {
                String title = template.getTitle();
                if (title != null && !title.isBlank()) {
                    projectsById.putIfAbsent(String.valueOf(template.getId()), title.trim());
                }
            }
        }
        List<Map<String, String>> rows = new ArrayList<>();
        projectsById.entrySet().stream().sorted(Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER))
                .forEach(entry -> {
                    Map<String, String> row = new HashMap<>();
                    row.put("id", entry.getKey());
                    row.put("value", entry.getValue());
                    rows.add(row);
                });
        return rows;
    }

    @Transactional(readOnly = true)
    public boolean isInventoryProjectConsistent(Integer departmentId, String projectName) {
        if (departmentId == null || projectName == null || projectName.isBlank()) {
            return true;
        }
        NoteBook notebook = findNotebookForProject(projectName.trim());
        if (notebook == null) {
            return true;
        }
        Set<Integer> notebookDepartmentIds = resolveNotebookDepartmentIds(notebook);
        return notebookDepartmentIds.isEmpty() || notebookDepartmentIds.contains(departmentId);
    }

    @Transactional(readOnly = true)
    public boolean inventoryBelongsToDepartment(InventoryItem item, Integer departmentId) {
        if (item == null || departmentId == null) {
            return false;
        }
        return resolveInventoryDepartmentIds(item).contains(departmentId);
    }

    /**
     * Whether the user may view or modify storage under the given room (or any
     * child location). Unrestricted users: always true. Restricted: room must have
     * a non-null {@link StorageRoom#getDepartmentTestSectionId() department} and it
     * must be in the user's allowed test sections.
     */
    @Transactional(readOnly = true)
    public boolean canAccessStorageRoom(StorageRoom room, HttpServletRequest request) {
        if (room == null) {
            return false;
        }
        return canAccessDepartmentScopedLocation(room.getDepartmentTestSectionId(), request);
    }

    /**
     * Same rules as {@link #canAccessStorageRoom}, keyed only by department id (for
     * DTO maps).
     */
    @Transactional(readOnly = true)
    public boolean canAccessDepartmentScopedLocation(Integer departmentTestSectionId, HttpServletRequest request) {
        if (hasUnrestrictedDepartmentAccess(request)) {
            return true;
        }
        if (departmentTestSectionId == null) {
            return false;
        }
        Set<Integer> allowedIds = getRestrictedUserTestSectionIds(request);
        return !allowedIds.isEmpty() && allowedIds.contains(departmentTestSectionId);
    }

    /**
     * Numeric {@code test_section.id} values for the current user (login lab unit
     * plus lab-unit role map). Empty if none; does not include global admin /
     * all-lab units (callers should use {@link #hasUnrestrictedDepartmentAccess}
     * first).
     */
    @Transactional(readOnly = true)
    public Set<Integer> getRestrictedUserTestSectionIds(HttpServletRequest request) {
        Set<Integer> ids = new LinkedHashSet<>();
        UserSessionData usd = getUserSessionData(request);
        if (usd != null && usd.getLoginLabUnit() > 0) {
            ids.add(usd.getLoginLabUnit());
        }
        return ids;
    }

    /**
     * All departments a user may select as active login context.
     * This is NOT data scope; data scope uses {@link #getRestrictedUserTestSectionIds(HttpServletRequest)}.
     */
    @Transactional(readOnly = true)
    public Set<Integer> getSelectableUserTestSectionIds(HttpServletRequest request) {
        Set<Integer> ids = new LinkedHashSet<>();
        UserSessionData usd = getUserSessionData(request);
        if (usd != null && usd.getLoginLabUnit() > 0) {
            ids.add(usd.getLoginLabUnit());
        }
        String sysUserId = getSysUserId(request);
        if (sysUserId != null) {
            UserLabUnitRoles userLabRoles = userRoleService.getUserLabUnitRoles(sysUserId);
            if (userLabRoles != null && userLabRoles.getLabUnitRoleMap() != null) {
                for (LabUnitRoleMap roleMap : userLabRoles.getLabUnitRoleMap()) {
                    if (roleMap == null) {
                        continue;
                    }
                    String mappedLabUnit = roleMap.getLabUnit();
                    if (mappedLabUnit == null || mappedLabUnit.isBlank()
                            || "AllLabUnits".equalsIgnoreCase(mappedLabUnit.trim())) {
                        continue;
                    }
                    addTestSectionIdFromLabUnitRef(ids, mappedLabUnit.trim());
                }
            }
        }
        return ids;
    }

    /**
     * Picks {@code test_section.id} when a department-scoped user creates an owned
     * resource (storage room, inventory catalog item, etc.). Unrestricted callers:
     * {@code explicitDepartmentId} is returned as-is (may be {@code null}).
     * Restricted callers: prefers session login lab unit if allowed; else
     * {@code explicitDepartmentId} if allowed; else the user's sole allowed
     * section; otherwise {@code null}.
     */
    @Transactional(readOnly = true)
    public Integer resolveDepartmentForScopedCreate(HttpServletRequest request, Integer explicitDepartmentId) {
        if (hasUnrestrictedDepartmentAccess(request)) {
            return explicitDepartmentId;
        }
        UserSessionData usd = getUserSessionData(request);
        if (usd == null || usd.getLoginLabUnit() <= 0) {
            return null;
        }
        Integer active = usd.getLoginLabUnit();
        if (explicitDepartmentId == null || explicitDepartmentId.equals(active)) {
            return active;
        }
        return null;
    }

    private void addTestSectionIdFromLabUnitRef(Set<Integer> ids, String labUnitRef) {
        try {
            int parsed = Integer.parseInt(labUnitRef);
            if (parsed > 0) {
                ids.add(parsed);
            }
        } catch (NumberFormatException e) {
            TestSection section = testSectionService.getTestSectionByName(labUnitRef);
            if (section != null && section.getId() != null) {
                try {
                    ids.add(Integer.parseInt(section.getId().trim()));
                } catch (NumberFormatException ignored) {
                    // ignore
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public boolean canAccessSampleItem(SampleItem sampleItem, HttpServletRequest request) {
        if (sampleItem == null) {
            return false;
        }
        if (hasUnrestrictedDepartmentAccess(request)) {
            return true;
        }
        Set<String> userDepartmentKeys = getAllowedDepartmentKeys(request);
        if (userDepartmentKeys.isEmpty()) {
            return false;
        }
        Set<String> sampleDepartmentKeys = resolveSampleDepartmentKeys(sampleItem);
        return !sampleDepartmentKeys.isEmpty() && intersects(userDepartmentKeys, sampleDepartmentKeys);
    }

    @Transactional(readOnly = true)
    public boolean canAccessBioSample(BioSample bioSample, HttpServletRequest request) {
        if (bioSample == null) {
            return false;
        }
        if (hasUnrestrictedDepartmentAccess(request)) {
            return true;
        }
        if (bioSample.getDepartmentTestSectionId() != null) {
            return canAccessDepartmentScopedLocation(bioSample.getDepartmentTestSectionId(), request);
        }
        SampleItem sampleItem = bioSample.getSampleItem();
        if (sampleItem == null) {
            return false;
        }
        return canAccessSampleItem(sampleItem, request);
    }

    private UserSessionData getUserSessionData(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(IActionConstants.USER_SESSION_DATA);
        if (usd == null) {
            usd = (UserSessionData) request.getAttribute(IActionConstants.USER_SESSION_DATA);
        }
        return usd;
    }

    private boolean hasAllLabUnitsAccess(String sysUserId) {
        UserLabUnitRoles userLabRoles = userRoleService.getUserLabUnitRoles(sysUserId);
        return userLabRoles != null && userLabRoles.getLabUnitRoleMap() != null && userLabRoles.getLabUnitRoleMap()
                .stream().anyMatch(roleMap -> "AllLabUnits".equalsIgnoreCase(roleMap.getLabUnit()));
    }

    @Transactional(readOnly = true)
    protected Set<String> getAllowedDepartmentKeys(HttpServletRequest request) {
        if (hasUnrestrictedDepartmentAccess(request)) {
            return Collections.emptySet();
        }

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return Collections.emptySet();
        }

        Set<String> keys = new LinkedHashSet<>();
        addDepartmentKeys(keys, getLoginLabUnit(request));
        UserSessionData usd = getUserSessionData(request);
        if (usd != null && usd.getLoginLabUnit() > 0) {
            TestSection activeSection = testSectionService.getTestSectionById(String.valueOf(usd.getLoginLabUnit()));
            addDepartmentKeys(keys, usd.getLoginLabUnit());
            addDepartmentKeys(keys, departmentName(activeSection));
            addDepartmentKeys(keys, departmentLocalizedName(activeSection));
        }

        return keys;
    }

    @Transactional(readOnly = true)
    protected Set<String> resolveInventoryDepartmentKeys(InventoryItem item) {
        if (item == null) {
            return Collections.emptySet();
        }
        if (item.getDepartmentTestSectionId() == null) {
            return Collections.emptySet();
        }
        TestSection section = testSectionService.getTestSectionById(String.valueOf(item.getDepartmentTestSectionId()));
        if (section == null) {
            return Collections.emptySet();
        }
        Set<String> keys = new LinkedHashSet<>();
        addDepartmentKeys(keys, section.getId());
        addDepartmentKeys(keys, departmentName(section));
        addDepartmentKeys(keys, departmentLocalizedName(section));
        return keys;
    }

    @Transactional(readOnly = true)
    protected Set<Integer> resolveInventoryDepartmentIds(InventoryItem item) {
        if (item == null) {
            return Collections.emptySet();
        }
        if (item.getDepartmentTestSectionId() == null) {
            return Collections.emptySet();
        }
        return Set.of(item.getDepartmentTestSectionId());
    }

    @Transactional(readOnly = true)
    public boolean canAccessInventoryItemStrictIntersection(InventoryItem item, HttpServletRequest request) {
        return canAccessInventoryItem(item, request);
    }

    @Transactional(readOnly = true)
    public Integer resolveDepartmentForStrictScopedCreate(HttpServletRequest request, Integer explicitDepartmentId,
            String projectName) {
        return resolveDepartmentForScopedCreate(request, explicitDepartmentId);
    }

    @Transactional(readOnly = true)
    protected Set<String> resolveSampleDepartmentKeys(SampleItem sampleItem) {
        if (sampleItem == null || sampleItem.getId() == null) {
            return Collections.emptySet();
        }
        List<NotebookEntry> entries = notebookEntryService.findBySampleItemId(Integer.valueOf(sampleItem.getId()));
        if (entries == null || entries.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> keys = new LinkedHashSet<>();
        for (NotebookEntry entry : entries) {
            keys.addAll(resolveEntryDepartmentKeys(entry));
        }
        return keys;
    }

    private Set<String> resolveEntryDepartmentKeys(NotebookEntry entry) {
        if (entry == null) {
            return Collections.emptySet();
        }
        return resolveNotebookDepartmentKeys(entry.getNotebook());
    }

    @Transactional(readOnly = true)
    protected Set<String> resolveNotebookDepartmentKeys(NoteBook notebook) {
        if (notebook == null) {
            return Collections.emptySet();
        }

        Set<String> keys = new LinkedHashSet<>();
        for (TestSection department : resolveNotebookDepartments(notebook)) {
            addDepartmentKeys(keys, department.getId());
            addDepartmentKeys(keys, departmentName(department));
            addDepartmentKeys(keys, departmentLocalizedName(department));
        }

        return keys;
    }

    @Transactional(readOnly = true)
    protected Set<Integer> resolveNotebookDepartmentIds(NoteBook notebook) {
        if (notebook == null) {
            return Collections.emptySet();
        }
        Set<Integer> ids = new LinkedHashSet<>();
        for (TestSection department : resolveNotebookDepartments(notebook)) {
            Integer id = parseDepartmentId(department);
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    private NoteBook findNotebookForProject(String projectName) {
        if (projectName.matches("\\d+")) {
            NoteBook byId = noteBookService.get(Integer.valueOf(projectName));
            if (byId != null) {
                return byId;
            }
        }
        List<NoteBook> matches = noteBookService.getAllMatching("title", projectName);
        if (matches == null || matches.isEmpty()) {
            return null;
        }
        return matches.get(0);
    }

    private SampleItem resolveSampleItem(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return null;
        }

        String trimmed = identifier.trim();

        SampleItem byId = sampleItemService.getData(trimmed);
        if (byId != null && byId.getId() != null) {
            return byId;
        }

        Sample accessionSample = sampleService.getSampleByAccessionNumber(trimmed);
        if (accessionSample != null && accessionSample.getId() != null) {
            List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(accessionSample.getId());
            if (sampleItems != null && sampleItems.size() == 1) {
                return sampleItems.get(0);
            }
        }

        List<SampleItem> byExternalId = sampleItemService.getSampleItemsByExternalID(trimmed);
        if (byExternalId != null && byExternalId.size() == 1) {
            return byExternalId.get(0);
        }

        return null;
    }

    private boolean intersects(Set<String> left, Set<String> right) {
        for (String key : left) {
            if (right.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private void addDepartmentKeys(Set<String> keys, Object rawValue) {
        String normalized = normalize(rawValue);
        if (!normalized.isEmpty()) {
            keys.add(normalized);
        }
    }

    private String normalize(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private TestSection safeGetTestSection(String labUnit) {
        try {
            return testSectionService.get(labUnit);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String departmentName(TestSection section) {
        return section != null ? section.getTestSectionName() : null;
    }

    private String departmentLocalizedName(TestSection section) {
        if (section == null) {
            return null;
        }
        try {
            return section.getLocalizedName();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<Integer, String> getAccessibleWorkflowDepartmentLabels(HttpServletRequest request) {
        Map<Integer, String> departments = new HashMap<>();
        String sysUserId = getSysUserId(request);
        String loginLabUnit = getLoginLabUnit(request);
        boolean unrestricted = hasUnrestrictedDepartmentAccess(request);
        for (NoteBook template : noteBookService.getAllTemplateNoteBooks()) {
            if (template == null || template.getId() == null) {
                continue;
            }
            if (!unrestricted && !notebookSecurityService.canViewTemplate(template.getId(), sysUserId, loginLabUnit)) {
                continue;
            }
            String title = template.getTitle();
            TestSection linkedDepartment = selectPrimaryLinkedDepartment(template, title);
            Integer linkedDepartmentId = parseDepartmentId(linkedDepartment);
            if (linkedDepartmentId != null) {
                departments.putIfAbsent(linkedDepartmentId,
                        title != null && !title.isBlank() ? title.trim() : resolveTestSectionLabel(linkedDepartment));
                continue;
            }
            TestSection exactMatch = resolveTestSectionByNotebookTitle(title);
            Integer exactMatchId = parseDepartmentId(exactMatch);
            if (exactMatchId != null && title != null && !title.isBlank()) {
                departments.putIfAbsent(exactMatchId, title.trim());
            }
        }
        return departments;
    }

    private Set<TestSection> resolveNotebookDepartments(NoteBook notebook) {
        Integer effectiveNotebookId = notebook.getId();
        if (Boolean.FALSE.equals(notebook.getIsTemplate())) {
            if (notebook.getParentNotebook() != null && notebook.getParentNotebook().getId() != null) {
                effectiveNotebookId = notebook.getParentNotebook().getId();
            } else {
                NoteBook parentTemplate = noteBookService.getParentTemplate(notebook.getId());
                if (parentTemplate != null && parentTemplate.getId() != null) {
                    effectiveNotebookId = parentTemplate.getId();
                }
            }
        }

        if (effectiveNotebookId != null) {
            Set<TestSection> departments = noteBookService.getNoteBookDepartments(effectiveNotebookId);
            if (departments != null && !departments.isEmpty()) {
                return departments;
            }
        }

        TestSection fallback = resolveTestSectionByNotebookTitle(notebook.getTitle());
        if (fallback == null) {
            return Collections.emptySet();
        }
        return Set.of(fallback);
    }

    private TestSection selectPrimaryLinkedDepartment(NoteBook template, String notebookTitle) {
        if (template == null || template.getDepartments() == null || template.getDepartments().isEmpty()) {
            return null;
        }
        List<TestSection> linkedDepartments = template.getDepartments().stream().filter(Objects::nonNull)
                .sorted(Comparator.comparing(this::resolveTestSectionLabel, String.CASE_INSENSITIVE_ORDER)).toList();
        for (TestSection department : linkedDepartments) {
            if (notebookTitleMatchesDepartment(notebookTitle, department)) {
                return department;
            }
        }
        return linkedDepartments.get(0);
    }

    private TestSection resolveTestSectionByNotebookTitle(String notebookTitle) {
        if (notebookTitle == null || notebookTitle.isBlank()) {
            return null;
        }
        TestSection byName = testSectionService.getTestSectionByName(notebookTitle.trim());
        if (byName != null) {
            return byName;
        }
        List<TestSection> activeSections = testSectionService.getAllActiveTestSections();
        if (activeSections == null || activeSections.isEmpty()) {
            return null;
        }
        return activeSections.stream().filter(section -> notebookTitleMatchesDepartment(notebookTitle, section)).findFirst()
                .orElse(null);
    }

    private boolean notebookTitleMatchesDepartment(String notebookTitle, TestSection department) {
        if (notebookTitle == null || department == null) {
            return false;
        }
        String normalizedTitle = notebookTitle.trim();
        if (department.getTestSectionName() != null
                && normalizedTitle.equalsIgnoreCase(department.getTestSectionName().trim())) {
            return true;
        }
        return department.getLocalizedName() != null
                && normalizedTitle.equalsIgnoreCase(department.getLocalizedName().trim());
    }

    private Integer parseDepartmentId(TestSection department) {
        if (department == null || department.getId() == null) {
            return null;
        }
        try {
            return Integer.valueOf(department.getId());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private List<TestSection> loadTestSections(Set<Integer> ids) {
        List<TestSection> sections = new ArrayList<>();
        if (ids == null || ids.isEmpty()) {
            return sections;
        }
        for (Integer id : ids) {
            TestSection ts = testSectionService.getTestSectionById(String.valueOf(id));
            if (ts != null) {
                sections.add(ts);
            }
        }
        return sections;
    }

    private List<Map<String, String>> buildDepartmentRows(List<TestSection> sections,
            Map<Integer, String> workflowDepartments) {
        if (sections == null || sections.isEmpty()) {
            return List.of();
        }
        List<TestSection> sorted = new ArrayList<>(sections);
        sorted.sort(Comparator.comparing(section -> resolveDepartmentLabel(section, workflowDepartments),
                String.CASE_INSENSITIVE_ORDER));
        List<Map<String, String>> rows = new ArrayList<>();
        for (TestSection section : sorted) {
            if (section == null || section.getId() == null) {
                continue;
            }
            Map<String, String> row = new HashMap<>();
            row.put("id", String.valueOf(section.getId()));
            row.put("value", resolveDepartmentLabel(section, workflowDepartments));
            rows.add(row);
        }
        return rows;
    }

    private String resolveDepartmentLabel(TestSection section, Map<Integer, String> workflowDepartments) {
        Integer id = parseDepartmentId(section);
        if (id != null && workflowDepartments != null) {
            String notebookTitle = workflowDepartments.get(id);
            if (notebookTitle != null && !notebookTitle.isBlank()) {
                return notebookTitle;
            }
        }
        return resolveTestSectionLabel(section);
    }

    private String resolveTestSectionLabel(TestSection section) {
        if (section == null) {
            return "";
        }
        try {
            String localizedName = section.getLocalizedName();
            if (localizedName != null && !localizedName.isBlank()) {
                return localizedName;
            }
        } catch (Exception ignored) {
            // Fall back to test section name when localization services are unavailable
        }
        if (section.getTestSectionName() != null && !section.getTestSectionName().isBlank()) {
            return section.getTestSectionName();
        }
        return section.getId() != null ? section.getId() : "";
    }
}
