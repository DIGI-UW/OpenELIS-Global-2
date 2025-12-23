package org.openelisglobal.notebook.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.notebook.dao.NoteBookPageDAO;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for NoteBookPage entity. Inherits standard CRUD
 * operations from AuditableBaseObjectServiceImpl.
 */
@Service
public class NoteBookPageServiceImpl extends AuditableBaseObjectServiceImpl<NoteBookPage, Integer>
        implements NoteBookPageService {

    @Autowired
    private NoteBookPageDAO baseObjectDAO;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public NoteBookPageServiceImpl() {
        super(NoteBookPage.class);
        this.auditTrailLog = true;
    }

    @Override
    protected BaseDAO<NoteBookPage, Integer> getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoteBookPage> getByNotebookId(Integer notebookId) {
        return baseObjectDAO.getByNotebookId(notebookId);
    }

    @Override
    @Transactional
    public NoteBookPage updatePageContent(Integer pageId, String newContentJson, String sysUserId) {
        // First, get and detach the page to capture original state for audit trail
        NoteBookPage page = get(pageId);
        if (page == null) {
            throw new IllegalArgumentException("Page not found: " + pageId);
        }

        // Store original content before modification
        String existingContent = page.getContent();

        try {
            // Calculate merged content
            String mergedContent;

            if (existingContent == null || existingContent.isBlank()) {
                // No existing content, use new content as-is
                mergedContent = newContentJson;
            } else {
                // Parse both JSON objects and merge
                JsonNode existingNode = objectMapper.readTree(existingContent);
                JsonNode newNode = objectMapper.readTree(newContentJson);

                if (existingNode.isObject() && newNode.isObject()) {
                    ObjectNode mergedNode = (ObjectNode) existingNode.deepCopy();
                    Iterator<Map.Entry<String, JsonNode>> fields = newNode.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> field = fields.next();
                        mergedNode.set(field.getKey(), field.getValue());
                    }
                    mergedContent = objectMapper.writeValueAsString(mergedNode);
                } else {
                    // If not objects, replace entirely
                    mergedContent = newContentJson;
                }
            }

            // Evict the page from session to allow fresh fetch for audit comparison
            baseObjectDAO.evict(page);

            // Re-fetch to get a fresh managed entity
            page = get(pageId);
            page.setContent(mergedContent);
            page.setSysUserId(sysUserId);
            update(page);

            LogEvent.logInfo(this.getClass().getSimpleName(), "updatePageContent",
                    "Updated page content for pageId=" + pageId + " by user=" + sysUserId);

            return page;
        } catch (JsonProcessingException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "updatePageContent",
                    "Error parsing JSON content for pageId=" + pageId + ": " + e.getMessage());
            throw new IllegalArgumentException("Invalid JSON content: " + e.getMessage(), e);
        }
    }

    @Override
    public void evict(NoteBookPage page) {
        if (page != null) {
            baseObjectDAO.evict(page);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getPageIdByNotebookIdAndOrder(Integer notebookId, Integer pageOrder) {
        return baseObjectDAO.getPageIdByNotebookIdAndOrder(notebookId, pageOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getPageIdByNotebookIdAndTitlePattern(Integer notebookId, String titlePattern) {
        return baseObjectDAO.getPageIdByNotebookIdAndTitlePattern(notebookId, titlePattern);
    }
}
