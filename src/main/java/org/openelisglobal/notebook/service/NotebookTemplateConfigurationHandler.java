package org.openelisglobal.notebook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.notebook.dao.NoteBookDAO;
import org.openelisglobal.notebook.dao.NoteBookPageDAO;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scaffolds notebook templates from JSON configuration files at application
 * startup.
 *
 * <p>
 * Each JSON file under {@code volume/configuration/backend/notebook-templates/}
 * describes a single lab template. This handler reads the file and creates or
 * updates the corresponding {@code notebook} and {@code notebook_page} rows —
 * exactly what the per-lab Liquibase changesets do, but without requiring a
 * schema migration for every new lab.
 *
 * <p>
 * <strong>Behavior:</strong> if a template with the same title already exists
 * (e.g. seeded by a Liquibase changeset), the template and all its pages are
 * re-processed (metadata updated, pages replaced wholesale). This allows config
 * changes to be picked up at runtime without restarts.
 *
 * <p>
 * <strong>JSON contract:</strong>
 *
 * <pre>{@code
 * {
 *   "title":        "Lab Name",           // required, must be unique
 *   "workflowType": "lab-key",            // used by the frontend for workflow routing
 *   "departments":  ["Department Name"],  // linked to test_section by name; warns if not found
 *   "objective":    "...",
 *   "protocol":     "...",
 *   "content":      "...",
 *   "status":       "ACTIVE",             // defaults to ACTIVE if omitted or invalid
 *   "tags":         ["tag1"],
 *   "pages": [
 *     {
 *       "order":        1,                // required, 1-based; throws if missing or < 1
 *       "title":        "...",
 *       "instructions": "...",
 *       "content":      "...",
 *       "pageType":     "STORAGE_ASSIGNMENT", // optional; legacy routing/storage categorization for Liquibase pages
 *       "pageId":       "gbd_storage_monitoring", // optional; lab-specific page identifier (enables routing/storage for JSON pages)
 *       "data":         {}                // optional; lab-specific configuration data (JSONB)
 *     }
 *   ]
 * }
 * }</pre>
 */
@Component
@Transactional
public class NotebookTemplateConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private NoteBookDAO noteBookDAO;

    @Autowired
    private NoteBookPageDAO noteBookPageDAO;

    @Autowired
    private TestSectionService testSectionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getDomainName() {
        return "notebook-templates";
    }

    @Override
    public String getFileExtension() {
        return "json";
    }

    @Override
    public int getLoadOrder() {
        return 210;
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        JsonNode root = objectMapper.readTree(inputStream);

        validateTemplate(root, fileName);

        String title = textOrNull(root, "title");
        NoteBook template = findTemplate(title);
        if (template != null) {
            updateTemplate(root, template, title, fileName);
        } else {
            template = createTemplate(root, title, fileName);
            linkDepartments(root, template, title, fileName);
            createPages(root, template, fileName);
        }
    }

    private NoteBook findTemplate(String title) {
        List<NoteBook> matches = noteBookDAO.getAllMatching("title", title);
        for (NoteBook nb : matches) {
            if (Boolean.TRUE.equals(nb.getIsTemplate())) {
                return nb;
            }
        }
        return null;
    }

    private NoteBook createTemplate(JsonNode root, String title, String fileName) {
        NoteBook template = new NoteBook();
        template.setTitle(title);
        template.setWorkflowType(textOrNull(root, "workflowType"));
        template.setObjective(textOrNull(root, "objective"));
        template.setProtocol(textOrNull(root, "protocol"));
        template.setContent(textOrNull(root, "content"));
        template.setIsTemplate(true);

        String status = textOrNull(root, "status");
        try {
            template.setStatus(
                    status != null ? NoteBook.NoteBookStatus.valueOf(status) : NoteBook.NoteBookStatus.ACTIVE);
        } catch (IllegalArgumentException e) {
            template.setStatus(NoteBook.NoteBookStatus.ACTIVE);
        }

        JsonNode tagsNode = root.get("tags");
        if (tagsNode != null && tagsNode.isArray()) {
            for (JsonNode tagNode : tagsNode) {
                String tag = tagNode.asText("").trim();
                if (!tag.isEmpty()) {
                    template.getTags().add(tag);
                }
            }
        }

        template.setDateCreated(new java.util.Date());
        template.setSysUserId("1");
        noteBookDAO.insert(template);
        LogEvent.logInfo(this.getClass().getSimpleName(), "createTemplate",
                "Created notebook template '" + title + "' from " + fileName);
        return template;
    }

    private void linkDepartments(JsonNode root, NoteBook template, String title, String fileName) {
        JsonNode departmentsNode = root.get("departments");
        if (departmentsNode == null || !departmentsNode.isArray()) {
            return;
        }
        for (JsonNode deptNode : departmentsNode) {
            String deptName = deptNode.asText("").trim();
            if (deptName.isEmpty()) {
                continue;
            }
            TestSection dept = testSectionService.getTestSectionByName(deptName);
            if (dept == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "linkDepartments",
                        "Department '" + deptName + "' not found for template '" + title + "' in " + fileName);
                continue;
            }
            template.getDepartments().add(dept);
        }
        noteBookDAO.update(template);
    }

    private void createPages(JsonNode root, NoteBook template, String fileName) {
        JsonNode pagesNode = root.get("pages");
        if (pagesNode == null || !pagesNode.isArray()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "createPages",
                    "No pages array in " + fileName + " for template '" + template.getTitle() + "'");
            return;
        }

        int created = 0;
        for (JsonNode pageNode : pagesNode) {
            // All pages are guaranteed to have valid order >= 1 by validateTemplate()
            int order = pageNode.get("order").asInt();

            NoteBookPage page = new NoteBookPage();
            page.setNotebook(template);
            page.setOrder(order);
            page.setTitle(textOrNull(pageNode, "title"));
            page.setInstructions(textOrNull(pageNode, "instructions"));
            page.setContent(textOrNull(pageNode, "content"));
            page.setPageType(textOrNull(pageNode, "pageType"));

            // Parse and persist page-level configuration data
            JsonNode dataNode = pageNode.get("data");
            if (dataNode != null && !dataNode.isNull()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = objectMapper.convertValue(dataNode, Map.class);
                    page.setData(dataMap);
                } catch (IllegalArgumentException e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "createPages",
                            "Failed to parse 'data' object for page order=" + order + " in " + fileName + ": "
                                    + e.getMessage());
                }
            }

            page.setCompleted(false);
            page.setSysUserId("1");
            noteBookPageDAO.insert(page);
            created++;
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "createPages",
                "Template '" + template.getTitle() + "': " + created + " pages created from " + fileName);
    }

    /**
     * Validates the parsed JSON root for structural correctness. Throws
     * IllegalArgumentException immediately on the first error found.
     *
     * @param root     the parsed JSON root node
     * @param fileName source file name used only in error messages
     * @throws IllegalArgumentException if any required field is absent or invalid
     */
    private void validateTemplate(JsonNode root, String fileName) {
        // Validate title is present and non-blank
        String title = textOrNull(root, "title");
        if (title == null) {
            throw new IllegalArgumentException("Notebook template config " + fileName + " missing 'title'");
        }

        // Validate pages is present and is a non-empty array
        JsonNode pagesNode = root.get("pages");
        if (pagesNode == null || !pagesNode.isArray()) {
            throw new IllegalArgumentException(
                    "Notebook template config " + fileName + " has no 'pages' array or pages is empty");
        }
        if (pagesNode.size() == 0) {
            throw new IllegalArgumentException(
                    "Notebook template config " + fileName + " has no 'pages' array or pages is empty");
        }

        // Validate each page
        java.util.Set<Integer> seenOrders = new java.util.HashSet<>();
        int pageIndex = 0;
        for (JsonNode pageNode : pagesNode) {
            // Validate order is present and >= 1
            if (!pageNode.has("order")) {
                throw new IllegalArgumentException(
                        "Page at index " + pageIndex + " in " + fileName + " has missing 'order'");
            }
            int order = pageNode.get("order").asInt(-1);
            if (order < 1) {
                throw new IllegalArgumentException(
                        "Page at index " + pageIndex + " in " + fileName + " has invalid 'order' (must be >= 1)");
            }

            // Validate order is unique within the template
            if (seenOrders.contains(order)) {
                throw new IllegalArgumentException("Page at index " + pageIndex + " in " + fileName
                        + " has duplicate 'order=" + order + "' (all page orders must be unique)");
            }
            seenOrders.add(order);

            pageIndex++;
        }
    }

    /**
     * Updates an existing notebook template in-place from the given JSON root. All
     * scalar metadata fields are overwritten. Pages are replaced wholesale (delete
     * all existing, insert from JSON). Department links are re-synced.
     *
     * @param root     the parsed JSON root node
     * @param template the existing NoteBook entity (isTemplate=true)
     * @param title    the template title (already validated)
     * @param fileName source file name for log messages
     */
    private void updateTemplate(JsonNode root, NoteBook template, String title, String fileName) {
        // Update scalar fields (same fields as createTemplate)
        template.setWorkflowType(textOrNull(root, "workflowType"));
        template.setObjective(textOrNull(root, "objective"));
        template.setProtocol(textOrNull(root, "protocol"));
        template.setContent(textOrNull(root, "content"));

        // Update status with same fallback as createTemplate
        String status = textOrNull(root, "status");
        try {
            template.setStatus(
                    status != null ? NoteBook.NoteBookStatus.valueOf(status) : NoteBook.NoteBookStatus.ACTIVE);
        } catch (IllegalArgumentException e) {
            template.setStatus(NoteBook.NoteBookStatus.ACTIVE);
        }

        // Update tags
        template.getTags().clear();
        JsonNode tagsNode = root.get("tags");
        if (tagsNode != null && tagsNode.isArray()) {
            for (JsonNode tagNode : tagsNode) {
                String tag = tagNode.asText("").trim();
                if (!tag.isEmpty()) {
                    template.getTags().add(tag);
                }
            }
        }

        template.setSysUserId("1");
        noteBookDAO.update(template);

        // Re-sync departments: clear then re-add
        template.getDepartments().clear();
        noteBookDAO.update(template);
        linkDepartments(root, template, title, fileName);

        // Replace pages: clear existing then create new ones
        template.getPages().clear();
        noteBookDAO.update(template); // Cascade orphan removal deletes old pages
        createPages(root, template, fileName);

        LogEvent.logInfo(this.getClass().getSimpleName(), "updateTemplate",
                "Updated notebook template '" + title + "' from " + fileName);
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            return null;
        }
        String value = child.asText("").trim();
        return value.isEmpty() ? null : value;
    }
}
