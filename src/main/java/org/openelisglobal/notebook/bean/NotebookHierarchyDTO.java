package org.openelisglobal.notebook.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing a notebook in the hierarchy tree structure. Used for the
 * TreeView sidebar display.
 */
public class NotebookHierarchyDTO {

    private Integer id;
    private String title;
    private boolean isParentTemplate;
    private boolean isChildInstance;
    private Integer parentNotebookId;
    private String parentNotebookTitle;
    private int entryCount;
    private int totalEntries; // For parents: sum of all children's entries
    private int orphanEntryCount; // For parents: entries directly on template (legacy/unassigned)
    private List<NotebookHierarchyDTO> children = new ArrayList<>();

    public NotebookHierarchyDTO() {
    }

    public NotebookHierarchyDTO(Integer id, String title, boolean isParentTemplate) {
        this.id = id;
        this.title = title;
        this.isParentTemplate = isParentTemplate;
        this.isChildInstance = !isParentTemplate;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isParentTemplate() {
        return isParentTemplate;
    }

    public void setParentTemplate(boolean parentTemplate) {
        isParentTemplate = parentTemplate;
    }

    public boolean isChildInstance() {
        return isChildInstance;
    }

    public void setChildInstance(boolean childInstance) {
        isChildInstance = childInstance;
    }

    public Integer getParentNotebookId() {
        return parentNotebookId;
    }

    public void setParentNotebookId(Integer parentNotebookId) {
        this.parentNotebookId = parentNotebookId;
    }

    public String getParentNotebookTitle() {
        return parentNotebookTitle;
    }

    public void setParentNotebookTitle(String parentNotebookTitle) {
        this.parentNotebookTitle = parentNotebookTitle;
    }

    public int getEntryCount() {
        return entryCount;
    }

    public void setEntryCount(int entryCount) {
        this.entryCount = entryCount;
    }

    public int getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(int totalEntries) {
        this.totalEntries = totalEntries;
    }

    public int getOrphanEntryCount() {
        return orphanEntryCount;
    }

    public void setOrphanEntryCount(int orphanEntryCount) {
        this.orphanEntryCount = orphanEntryCount;
    }

    public List<NotebookHierarchyDTO> getChildren() {
        return children;
    }

    public void setChildren(List<NotebookHierarchyDTO> children) {
        this.children = children;
    }

    public void addChild(NotebookHierarchyDTO child) {
        this.children.add(child);
    }
}
