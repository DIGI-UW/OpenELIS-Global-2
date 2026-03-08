package org.openelisglobal.notebook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;
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
 * <p>Each JSON file under {@code volume/configuration/backend/notebook-templates/}
 * describes a single lab template. This handler reads the file and creates the
 * corresponding {@code notebook} and {@code notebook_page} rows — exactly what
 * the per-lab Liquibase changesets do, but without requiring a schema migration
 * for every new lab.
 *
 * <p><strong>Idempotency:</strong> if a template with the same title already
 * exists (e.g. seeded by a Liquibase changeset), the file is skipped entirely.
 *
 * <p><strong>JSON contract:</strong>
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
 *       "order":        1,                // required, 1-based; entries with order < 1 are skipped
 *       "title":        "...",
 *       "pageType":     "...",            // optional; maps to a frontend page component key
 *       "instructions": "...",
 *       "content":      "..."
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

        String title = textOrNull(root, "title");
        if (title == null) {
            throw new IllegalArgumentException("Notebook template config " + fileName + " missing 'title'");
        }

        NoteBook template = findTemplate(title);
        if (template != null) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                    "Template '" + title + "' already exists — skipping.");
            return;
        }

        template = createTemplate(root, title, fileName);
        linkDepartments(root, template, title, fileName);
        createPages(root, template, fileName);
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
            int order = pageNode.has("order") ? pageNode.get("order").asInt() : -1;
            if (order < 1) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "createPages",
                        "Skipping page with missing/invalid order in " + fileName);
                continue;
            }

            NoteBookPage page = new NoteBookPage();
            page.setNotebook(template);
            page.setOrder(order);
            page.setTitle(textOrNull(pageNode, "title"));
            page.setPageType(textOrNull(pageNode, "pageType"));
            page.setInstructions(textOrNull(pageNode, "instructions"));
            page.setContent(textOrNull(pageNode, "content"));
            page.setCompleted(false);
            page.setSysUserId("1");
            noteBookPageDAO.insert(page);
            created++;
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "createPages",
                "Template '" + template.getTitle() + "': " + created + " pages created from " + fileName);
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
