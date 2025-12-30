# Spec: Notebook Template Hierarchy (Parent-Child Workflows)

**Spec ID:** 002-notebook-template-hierarchy **Status:** Draft **Created:**
2025-12-28 **Author:** Claude (via /speckit.specify)

---

## 1. Problem Statement

Currently, all notebooks (projects) in the sidebar are displayed as flat items
where users can directly add entries. However, the intended design is:

1. **Notebook Templates (Parents)** should act as blueprints/templates for
   creating child lab instances
2. Users should NOT be able to add entries directly to parent templates
3. **Child Notebooks (Instances)** should be cloned from parent templates and
   function like the current setup (can add entries)
4. Parent templates should aggregate and display entries from ALL their children
5. The sidebar should display a hierarchical tree structure showing parent-child
   relationships

### Current Behavior (Screenshot Analysis)

- Projects list shows: MNTD Laboratory, Medical Laboratory, Immunology
  Laboratory, Pharmaceuticals Laboratory, Bacteriology Laboratory
- Each has a "New Entry" button
- No visual hierarchy in the sidebar
- No concept of "child workflows" or "instances"

### Desired Behavior

- Parent templates cannot have entries added directly
- Parents have a "Create Instance" or "Add Child Lab" button instead of "New
  Entry"
- Child instances inherit metadata/pages from parent and CAN have entries
- Parent view aggregates all entries from children
- Tree view in sidebar shows hierarchy

---

## 2. Functional Requirements

### FR-1: Template vs Instance Distinction

- **FR-1.1:** Notebooks marked as `isTemplate=true` are "Parent Templates" and
  CANNOT have entries added directly
- **FR-1.2:** Child notebooks (cloned instances) have `isTemplate=false` and CAN
  have entries
- **FR-1.3:** Only root-level templates can be created via "+ Project" button
- **FR-1.4:** Children CANNOT be cloned (only parents can spawn children)

### FR-2: Child Instance Creation

- **FR-2.1:** Parent templates display "Create Instance" button instead of "New
  Entry"
- **FR-2.2:** Creating an instance:
  - **Copies (editable by child):**
    - Title (editable by user at creation)
    - Objective
    - Protocol
    - Content/Description
    - Organizations/departments access
    - Allowed roles
  - **Inherits from parent (live, not copied):**
    - All pages (with their configurations) - changes to parent pages reflect in
      children
- **FR-2.3:** Child instance gets a unique identifier (e.g., "Pharmaceuticals
  Laboratory - Lab 1")
- **FR-2.4:** Child instance is linked to parent via `parentNotebook`
  relationship
- **FR-2.5:** Child instance inherits `isTemplate=false` (it's an instance, not
  a template)
- **FR-2.6:** Deleting a parent template cascades deletion to all child
  instances

### FR-3: Entry Management

- **FR-3.1:** Entries can ONLY be added to child instances (not parent
  templates)
- **FR-3.2:** When viewing a parent template, display aggregated entries from
  ALL children
- **FR-3.3:** Aggregated view shows which child each entry belongs to
- **FR-3.4:** Filter/search applies across all aggregated child entries
- **FR-3.5:** Statistics cards (Total Entries, Drafts, Pending Review,
  Finalized) aggregate from all children

### FR-4: Hierarchical Sidebar Navigation

- **FR-4.1:** Use Carbon Design System TreeView component for hierarchy display
- **FR-4.2:** Parent templates shown as expandable nodes
- **FR-4.3:** Child instances shown as leaf nodes under their parent
- **FR-4.4:** Visual indicators to distinguish parents from children (e.g.,
  icons)
- **FR-4.5:** Expand/collapse functionality for each parent
- **FR-4.6:** Entry count displayed at both parent (aggregate) and child
  (individual) levels
- **FR-4.7:** "All Entries" option shows entries from all notebooks (existing
  behavior)

### FR-5: UI Changes

- **FR-5.1:** Parent template view:
  - Replace "New Entry" button with "Create Instance" / "Add Child Lab" button
  - Show aggregated statistics from all children
  - Show aggregated entries list with child source indication
  - Show list of child instances (optional expandable section)
- **FR-5.2:** Child instance view:
  - Keep existing "New Entry" button
  - Keep existing entry management functionality
  - Show breadcrumb indicating parent template
- **FR-5.3:** Instance creation modal:
  - Input for instance name/title
  - Preview of inherited properties
  - Confirm/Cancel actions

---

## 3. Non-Functional Requirements

### NFR-1: Performance

- Aggregated entry queries should be optimized (indexed by parent_notebook_id)
- Sidebar tree should lazy-load children on expand (if >20 children)
- Entry aggregation should support pagination

### NFR-2: Constitution Compliance

- Use Carbon Design System components (TreeView, Modal)
- Use React Intl for all user-facing strings
- Follow 5-layer architecture (Valueholder > DAO > Service > Controller > Form)
- Database changes via Liquibase migrations
- @Transactional only in service layer

### NFR-3: Backwards Compatibility

- Existing notebooks with entries should continue to function
- Migration should convert existing templates to parent status
- No data loss during migration

---

## 4. Technical Design

### 4.1 Database Schema Changes

**Modify `notebook` table:**

```sql
ALTER TABLE notebook ADD COLUMN parent_notebook_id BIGINT;
ALTER TABLE notebook ADD CONSTRAINT fk_notebook_parent
  FOREIGN KEY (parent_notebook_id) REFERENCES notebook(id) ON DELETE CASCADE;
CREATE INDEX idx_notebook_parent ON notebook(parent_notebook_id);
```

**Existing `is_template` column usage:**

- `is_template = true` AND `parent_notebook_id IS NULL` → Parent Template
- `is_template = false` AND `parent_notebook_id IS NOT NULL` → Child Instance
- Children cannot have children (enforced at service level)

### 4.2 Entity Changes

**NoteBook.java additions:**

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parent_notebook_id")
private NoteBook parentNotebook;

@OneToMany(mappedBy = "parentNotebook", cascade = CascadeType.ALL, orphanRemoval = true)
private List<NoteBook> childInstances = new ArrayList<>();

public boolean isParentTemplate() {
    return isTemplate && parentNotebook == null;
}

public boolean isChildInstance() {
    return !isTemplate && parentNotebook != null;
}

// For child instances, pages come from parent (live inheritance)
public List<NoteBookPage> getEffectivePages() {
    if (isChildInstance() && parentNotebook != null) {
        return parentNotebook.getPages();
    }
    return this.pages;
}
```

### 4.2.1 Live Inheritance Model

**Key Design Decision:** Child instances do NOT copy pages from parent. Instead,
they reference the parent's pages directly.

- **Pages:** Children inherit pages from parent via `getEffectivePages()`. When
  parent pages are updated, all children see the changes.
- **Metadata (editable by children):** Title, description, organizations,
  departments, roles - these are copied at creation and can be modified
  independently.
- **Entries:** Belong to the child instance only, not affected by parent
  changes.

**Implementation:**

- Child notebooks have `pages` collection empty/null
- UI and services call `getEffectivePages()` to get the actual pages
- Parent page edits automatically reflect in all children

### 4.3 Service Layer Changes

**NoteBookService.java additions:**

```java
// Create child instance from parent template
NoteBook createChildInstance(String parentId, String instanceTitle, String currentUserId);

// Get all child instances for a parent
List<NoteBook> getChildInstances(String parentId);

// Get aggregated entries from all children
Page<NotebookEntry> getAggregatedEntriesForParent(String parentId, Pageable pageable, EntryFilterDTO filters);

// Get aggregated statistics for parent
NotebookStatisticsDTO getAggregatedStatistics(String parentId);

// Validate that a notebook can accept entries (must be child instance)
boolean canAcceptEntries(String notebookId);
```

### 4.4 REST API Changes

**New Endpoints:**

```
POST   /rest/notebook/{parentId}/instances          - Create child instance
GET    /rest/notebook/{parentId}/instances          - List child instances
GET    /rest/notebook/{parentId}/aggregated-entries - Get aggregated entries
GET    /rest/notebook/{parentId}/aggregated-stats   - Get aggregated statistics
GET    /rest/notebook/{id}/can-accept-entries       - Check if entries allowed
```

**Modified Endpoints:**

```
GET    /rest/notebook/dashboard/notebooks           - Add isParentTemplate, childCount fields
POST   /rest/notebook/{id}/entries                  - Validate notebook is child instance
```

### 4.5 Frontend Component Changes

**New Components:**

- `NotebookTreeView.js` - Hierarchical sidebar using Carbon TreeView
- `CreateInstanceModal.js` - Modal for creating child instances
- `AggregatedEntriesView.js` - View for parent template showing all children's
  entries

**Modified Components:**

- `NoteBookDashBoard.js` - Conditional rendering based on parent vs child
- `GlobalSideBar.js` - Replace flat list with NotebookTreeView

### 4.6 DTO Changes

**NotebookDTO additions:**

```java
private boolean isParentTemplate;
private boolean isChildInstance;
private String parentNotebookId;
private String parentNotebookTitle;
private int childCount;
private List<NotebookSummaryDTO> childInstances; // Optional, for expanded view
```

**NotebookStatisticsDTO:**

```java
private int totalEntries;
private int drafts;
private int pendingReview;
private int finalizedThisWeek;
private boolean isAggregated; // true if stats are from multiple children
```

---

## 5. User Stories

### US-1: Create Child Instance

**As a** lab supervisor **I want to** create a child lab instance from a parent
template **So that** I can set up a new lab workflow with pre-configured pages
and settings

**Acceptance Criteria:**

- [ ] "Create Instance" button visible on parent template view
- [ ] Modal opens with title input field
- [ ] Child inherits all pages and metadata from parent
- [ ] Child appears in sidebar under parent
- [ ] Child has "New Entry" button (not "Create Instance")

### US-2: View Aggregated Entries

**As a** lab manager **I want to** see all entries from all child labs when
viewing a parent template **So that** I can monitor overall workflow across all
labs

**Acceptance Criteria:**

- [ ] Parent template view shows aggregated entry count
- [ ] Entry list shows entries from all children
- [ ] Each entry indicates which child lab it belongs to
- [ ] Filters work across aggregated entries
- [ ] Statistics are summed across all children

### US-3: Navigate Hierarchy

**As a** lab technician **I want to** see the lab hierarchy in the sidebar **So
that** I can quickly navigate to my specific lab instance

**Acceptance Criteria:**

- [ ] Sidebar shows tree structure with expandable parents
- [ ] Children are indented under parents
- [ ] Visual distinction between parents and children
- [ ] Entry counts visible at each level
- [ ] Clicking an item navigates to that notebook

### US-4: Prevent Direct Parent Entries

**As a** system **I want to** prevent entries from being added directly to
parent templates **So that** the template/instance hierarchy is maintained

**Acceptance Criteria:**

- [ ] "New Entry" button not visible on parent templates
- [ ] API rejects entry creation on parent templates
- [ ] Clear messaging if user attempts to add entry to parent

---

## 6. Out of Scope

- Nested hierarchy (children of children) - only one level of nesting
- Moving entries between children
- Merging child instances
- Template versioning
- Bulk child instance creation
- UI for deleting notebooks (cascade delete handled at DB level only)

---

## 7. Migration Strategy

1. **Identify existing templates:** All notebooks with `is_template=true` become
   parent templates
2. **Existing entries:** Notebooks with entries that are templates need review:
   - Option A: Convert to child instances automatically
   - Option B: Flag for manual review
3. **No data deletion:** All existing data preserved
4. **Rollback plan:** Add `parent_notebook_id` is nullable, can be reverted

---

## 8. Resolved Questions

1. **Q:** Should parent templates be editable after children are created?

   - **A:** Yes, changes to parent templates WILL propagate to existing children
     (live inheritance)

2. **Q:** How to handle deletion of parent with children?

   - **A:** Cascade deletion to children. Add DB constraint `ON DELETE CASCADE`.
     Note: No UI delete option currently exists, but constraint should be in
     place for data integrity.

3. **Q:** Should children be able to modify inherited pages?

   - **A:** Children can modify metadata only (locations, roles, titles,
     descriptions). Page structure/order is inherited from parent and not
     editable at child level (keeping it simple - no page order editing in UI).

4. **Q:** What icon/visual to distinguish parents from children?
   - **A:** Use Carbon icons:
     - **Parent Templates:** `Template` icon (grid/blueprint style) - represents
       a blueprint/template
     - **Child Instances:** `DataBase` or `Report` icon - represents an actual
       working instance
     - Alternative option: `FolderParent` for parents, `Document` for children

---

## 9. Dependencies

- Carbon Design System TreeView component
- React Intl message definitions
- Existing NoteBook entity and service infrastructure
- NotebookEntry system

---

## 10. Success Metrics

- All parent templates show aggregated statistics correctly
- Child instance creation completes in <2 seconds
- Sidebar tree renders in <500ms with 50+ notebooks
- Zero entries created directly on parent templates after deployment
- User can navigate from parent to specific child in 2 clicks

---

## 11. References

- Screenshot: Current notebook dashboard showing flat project list
- [NoteBook.java](../../src/main/java/org/openelisglobal/notebook/valueholder/NoteBook.java)
- [NoteBookDashBoard.js](../../frontend/src/components/notebook/NoteBookDashBoard.js)
- [Carbon TreeView](https://carbondesignsystem.com/components/tree-view/usage/)
