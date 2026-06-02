package org.openelisglobal.notebook.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.hibernate.Hibernate;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotebookDepartmentScopeServiceImpl implements NotebookDepartmentScopeService {

    private static final Logger logger = LoggerFactory.getLogger(NotebookDepartmentScopeServiceImpl.class);

    private static final String BIOREPOSITORY_LABORATORY = "Biorepository Laboratory";

    @Autowired
    private NoteBookService noteBookService;

    @Autowired
    private TestSectionService testSectionService;

    @Override
    @Transactional(readOnly = true)
    public Set<Integer> resolveNotebookDepartmentIds(Integer notebookId) {
        return resolveNotebookDepartmentIds(notebookId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Integer> resolveNotebookDepartmentIds(Integer notebookId, boolean biorepositoryOnly) {
        if (notebookId == null) {
            return applyBiorepositoryFallbackIfNeeded(new HashSet<>(), biorepositoryOnly, null);
        }

        Set<Integer> ids = collectDepartmentIds(noteBookService.getNoteBookDepartments(notebookId));
        if (!ids.isEmpty()) {
            return expandNotebookDepartmentIdsByName(ids);
        }

        NoteBook notebook = noteBookService.get(notebookId);
        if (notebook == null) {
            return applyBiorepositoryFallbackIfNeeded(ids, biorepositoryOnly, null);
        }

        if (isBiorepositoryNotebook(notebook)) {
            addDepartmentId(ids, testSectionService.getTestSectionByName(BIOREPOSITORY_LABORATORY));
            if (!ids.isEmpty()) {
                return expandNotebookDepartmentIdsByName(ids);
            }
        }

        String normalizedTitle = normalizeNotebookDepartmentTitle(notebook.getTitle());

        addDepartmentId(ids, selectPrimaryLinkedDepartment(notebook, normalizedTitle));
        if (!ids.isEmpty()) {
            return expandNotebookDepartmentIdsByName(ids);
        }

        if (notebook.isChildInstance() && notebook.getParentNotebook() != null) {
            NoteBook parent = notebook.getParentNotebook();
            Hibernate.initialize(parent.getDepartments());
            ids.addAll(collectDepartmentIds(parent.getDepartments()));
            if (ids.isEmpty()) {
                addDepartmentId(ids,
                        selectPrimaryLinkedDepartment(parent, normalizeNotebookDepartmentTitle(parent.getTitle())));
            }
            if (!ids.isEmpty()) {
                return expandNotebookDepartmentIdsByName(ids);
            }
        }

        addDepartmentId(ids, resolveTestSectionByTemplateTitle(normalizedTitle));
        if (!ids.isEmpty()) {
            return expandNotebookDepartmentIdsByName(ids);
        }

        if ("bacteriology".equalsIgnoreCase(notebook.getWorkflowType())
                || (normalizedTitle != null && normalizedTitle.toLowerCase().contains("bacteriology"))) {
            addDepartmentId(ids, testSectionService.getTestSectionByName("Bacteriology"));
        }

        return applyBiorepositoryFallbackIfNeeded(expandNotebookDepartmentIdsByName(ids), biorepositoryOnly,
                notebook);
    }

    @Override
    public Set<String> resolveNotebookDepartmentNames(Set<Integer> departmentIds) {
        Set<String> names = new HashSet<>();
        if (departmentIds == null || departmentIds.isEmpty()) {
            return names;
        }
        for (Integer id : departmentIds) {
            TestSection section = testSectionService.getTestSectionById(String.valueOf(id));
            if (section != null && section.getTestSectionName() != null && !section.getTestSectionName().isBlank()) {
                names.add(section.getTestSectionName().trim());
            }
        }
        return names;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBiorepositoryNotebook(Integer notebookId) {
        if (notebookId == null) {
            return false;
        }
        NoteBook notebook = noteBookService.get(notebookId);
        return isBiorepositoryNotebook(notebook);
    }

    private boolean isBiorepositoryNotebook(NoteBook notebook) {
        if (notebook == null) {
            return false;
        }
        if ("biorepository".equalsIgnoreCase(notebook.getWorkflowType())) {
            return true;
        }
        String title = notebook.getTitle();
        return title != null && title.toLowerCase().contains("biorepository");
    }

    private Set<Integer> applyBiorepositoryFallbackIfNeeded(Set<Integer> ids, boolean biorepositoryOnly,
            NoteBook notebook) {
        Set<Integer> result = ids != null ? new HashSet<>(ids) : new HashSet<>();
        if (!result.isEmpty()) {
            return result;
        }
        if (!biorepositoryOnly && !isBiorepositoryNotebook(notebook)) {
            return result;
        }
        TestSection biorepository = testSectionService.getTestSectionByName(BIOREPOSITORY_LABORATORY);
        if (biorepository == null) {
            biorepository = resolveTestSectionByTemplateTitle(BIOREPOSITORY_LABORATORY);
        }
        addDepartmentId(result, biorepository);
        if (result.isEmpty()) {
            logger.warn(
                    "Biorepository department fallback could not resolve test section by name '{}'",
                    BIOREPOSITORY_LABORATORY);
        }
        return expandNotebookDepartmentIdsByName(result);
    }

    private Set<Integer> expandNotebookDepartmentIdsByName(Set<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return ids != null ? ids : new HashSet<>();
        }
        Set<Integer> expanded = new HashSet<>(ids);
        Set<String> names = resolveNotebookDepartmentNames(ids);
        List<TestSection> activeSections = testSectionService.getAllActiveTestSections();
        if (activeSections == null || activeSections.isEmpty()) {
            return expanded;
        }
        for (TestSection section : activeSections) {
            if (section == null || section.getTestSectionName() == null) {
                continue;
            }
            String sectionName = section.getTestSectionName().trim();
            for (String name : names) {
                if (name.equalsIgnoreCase(sectionName)) {
                    addDepartmentId(expanded, section);
                    break;
                }
            }
        }
        return expanded;
    }

    private Set<Integer> collectDepartmentIds(Set<TestSection> departments) {
        Set<Integer> ids = new HashSet<>();
        if (departments == null) {
            return ids;
        }
        for (TestSection department : departments) {
            addDepartmentId(ids, department);
        }
        return ids;
    }

    private void addDepartmentId(Set<Integer> ids, TestSection department) {
        Integer id = parseDepartmentId(department);
        if (id != null) {
            ids.add(id);
        }
    }

    private Integer parseDepartmentId(TestSection department) {
        if (department == null || department.getId() == null) {
            return null;
        }
        try {
            return Integer.valueOf(department.getId());
        } catch (NumberFormatException e) {
            logger.warn("Skipping non-numeric department id {} for notebook scope", department.getId());
            return null;
        }
    }

    private TestSection selectPrimaryLinkedDepartment(NoteBook template, String notebookTitle) {
        if (template == null) {
            return null;
        }
        Hibernate.initialize(template.getDepartments());
        if (template.getDepartments() == null || template.getDepartments().isEmpty()) {
            return null;
        }
        List<TestSection> linkedDepartments = template.getDepartments().stream().filter(Objects::nonNull)
                .sorted((left, right) -> resolveTestSectionLabel(left).compareToIgnoreCase(resolveTestSectionLabel(right)))
                .toList();
        for (TestSection department : linkedDepartments) {
            if (templateTitleMatchesDepartment(notebookTitle, department)) {
                return department;
            }
        }
        return linkedDepartments.get(0);
    }

    private TestSection resolveTestSectionByTemplateTitle(String notebookTitle) {
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
        return activeSections.stream().filter(section -> templateTitleMatchesDepartment(notebookTitle, section))
                .findFirst().orElse(null);
    }

    private String resolveTestSectionLabel(TestSection department) {
        if (department == null) {
            return "";
        }
        if (department.getTestSectionName() != null && !department.getTestSectionName().isBlank()) {
            return department.getTestSectionName().trim();
        }
        if (department.getLocalizedName() != null && !department.getLocalizedName().isBlank()) {
            return department.getLocalizedName().trim();
        }
        return "";
    }

    private boolean templateTitleMatchesDepartment(String notebookTitle, TestSection department) {
        if (notebookTitle == null || department == null) {
            return false;
        }
        String normalizedTitle = notebookTitle.trim();
        String departmentName = department.getTestSectionName() != null ? department.getTestSectionName().trim() : null;
        if (departmentName != null) {
            if (normalizedTitle.equalsIgnoreCase(departmentName)) {
                return true;
            }
            if (normalizedTitle.equalsIgnoreCase(departmentName + " Laboratory")
                    || normalizedTitle.regionMatches(true, 0, departmentName, 0, departmentName.length())
                            && normalizedTitle.length() > departmentName.length()
                            && Character.isWhitespace(normalizedTitle.charAt(departmentName.length()))) {
                return true;
            }
        }
        return department.getLocalizedName() != null
                && normalizedTitle.equalsIgnoreCase(department.getLocalizedName().trim());
    }

    private String normalizeNotebookDepartmentTitle(String title) {
        if (title == null) {
            return null;
        }
        return title.trim().replaceFirst("\\s+-\\s+Lab\\s+\\d+.*$", "").replaceFirst("\\s+-\\s+Entry\\s+#?\\d+.*$", "");
    }
}
