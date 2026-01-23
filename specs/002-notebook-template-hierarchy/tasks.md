# Tasks: Notebook Template Hierarchy

**Spec Reference:** [spec.md](./spec.md) **Plan Reference:**
[plan.md](./plan.md) **Status:** Ready for Implementation **Created:**
2025-12-28

---

## Task Overview

| Phase                | Tasks | Est. Complexity |
| -------------------- | ----- | --------------- |
| 1. Database & Entity | 3     | Medium          |
| 2. DAO Layer         | 4     | Low             |
| 3. Service Layer     | 6     | High            |
| 4. REST Controller   | 5     | Medium          |
| 5. Frontend TreeView | 4     | Medium          |
| 6. Frontend Modal    | 3     | Low             |
| 7. Dashboard Updates | 5     | High            |
| 8. Testing           | 4     | Medium          |

**Total: 34 tasks**

---

## Phase 1: Database & Entity Layer

### Task 1.1: Create Liquibase Migration

**File:** `src/main/resources/liquibase/3.5.x.x/001-notebook-hierarchy.xml`
(NEW)

**Acceptance Criteria:**

- [ ] Create new migration file with proper changeset ID
- [ ] Add `parent_notebook_id` column (BIGINT, nullable)
- [ ] Add foreign key constraint with ON DELETE CASCADE
- [ ] Add index on `parent_notebook_id`
- [ ] Include migration in master changelog

**Implementation:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.8.xsd">

    <changeSet id="1" author="openelis">
        <comment>Add parent_notebook_id for template hierarchy</comment>
        <addColumn tableName="notebook">
            <column name="parent_notebook_id" type="BIGINT"/>
        </addColumn>
    </changeSet>

    <changeSet id="2" author="openelis">
        <addForeignKeyConstraint
            baseTableName="notebook"
            baseColumnNames="parent_notebook_id"
            referencedTableName="notebook"
            referencedColumnNames="id"
            constraintName="fk_notebook_parent"
            onDelete="CASCADE"/>
    </changeSet>

    <changeSet id="3" author="openelis">
        <createIndex tableName="notebook" indexName="idx_notebook_parent">
            <column name="parent_notebook_id"/>
        </createIndex>
    </changeSet>
</databaseChangeLog>
```

**Dependencies:** None

---

### Task 1.2: Update NoteBook Entity - Add Parent Relationship

**File:** `src/main/java/org/openelisglobal/notebook/valueholder/NoteBook.java`

**Acceptance Criteria:**

- [ ] Add `parentNotebook` field with @ManyToOne annotation
- [ ] Add `childInstances` field with @OneToMany annotation
- [ ] Use LAZY fetch type for both relationships
- [ ] Add orphanRemoval=true for childInstances

**Implementation:**

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parent_notebook_id")
private NoteBook parentNotebook;

@OneToMany(mappedBy = "parentNotebook", cascade = CascadeType.ALL, orphanRemoval = true)
@OrderBy("title ASC")
private List<NoteBook> childInstances = new ArrayList<>();

// Getters and setters
public NoteBook getParentNotebook() {
    return parentNotebook;
}

public void setParentNotebook(NoteBook parentNotebook) {
    this.parentNotebook = parentNotebook;
}

public List<NoteBook> getChildInstances() {
    return childInstances;
}

public void setChildInstances(List<NoteBook> childInstances) {
    this.childInstances = childInstances;
}
```

**Dependencies:** Task 1.1

---

### Task 1.3: Add Helper Methods to NoteBook Entity

**File:** `src/main/java/org/openelisglobal/notebook/valueholder/NoteBook.java`

**Acceptance Criteria:**

- [ ] Add `isParentTemplate()` method
- [ ] Add `isChildInstance()` method
- [ ] Add `getEffectivePages()` method for live inheritance
- [ ] Add `addChildInstance()` helper method

**Implementation:**

```java
/**
 * Returns true if this is a parent template (can spawn children, cannot have entries)
 */
public boolean isParentTemplate() {
    return Boolean.TRUE.equals(isTemplate) && parentNotebook == null;
}

/**
 * Returns true if this is a child instance (can have entries, cannot spawn children)
 */
public boolean isChildInstance() {
    return !Boolean.TRUE.equals(isTemplate) && parentNotebook != null;
}

/**
 * Returns the effective pages for this notebook.
 * Child instances inherit pages from their parent template (live inheritance).
 */
public List<NoteBookPage> getEffectivePages() {
    if (isChildInstance() && parentNotebook != null) {
        return parentNotebook.getPages();
    }
    return this.pages;
}

/**
 * Helper method to add a child instance
 */
public void addChildInstance(NoteBook child) {
    childInstances.add(child);
    child.setParentNotebook(this);
}
```

**Dependencies:** Task 1.2

---

## Phase 2: DAO Layer

### Task 2.1: Add DAO Interface Methods

**File:** `src/main/java/org/openelisglobal/notebook/dao/NoteBookDAO.java`

**Acceptance Criteria:**

- [ ] Add `findChildrenByParentId(Integer parentId)` method signature
- [ ] Add `findAllParentTemplates()` method signature
- [ ] Add `countEntriesForChildren(List<Integer> childIds)` method signature
- [ ] Add `findEntriesByChildIds(List<Integer> childIds, Pageable pageable)`
      method signature

**Implementation:**

```java
List<NoteBook> findChildrenByParentId(Integer parentId);

List<NoteBook> findAllParentTemplates();

Map<Integer, Long> countEntriesForChildren(List<Integer> childIds);

Page<NotebookEntry> findEntriesByChildIds(List<Integer> childIds, Pageable pageable);
```

**Dependencies:** Task 1.3

---

### Task 2.2: Implement findChildrenByParentId

**File:** `src/main/java/org/openelisglobal/notebook/dao/NoteBookDAOImpl.java`

**Acceptance Criteria:**

- [ ] Query children by parent_notebook_id
- [ ] Order by title ascending
- [ ] Return empty list if no children

**Implementation:**

```java
@Override
public List<NoteBook> findChildrenByParentId(Integer parentId) {
    String hql = "FROM NoteBook nb WHERE nb.parentNotebook.id = :parentId ORDER BY nb.title ASC";
    Query<NoteBook> query = entityManager.unwrap(Session.class).createQuery(hql, NoteBook.class);
    query.setParameter("parentId", parentId);
    return query.list();
}
```

**Dependencies:** Task 2.1

---

### Task 2.3: Implement findAllParentTemplates

**File:** `src/main/java/org/openelisglobal/notebook/dao/NoteBookDAOImpl.java`

**Acceptance Criteria:**

- [ ] Return notebooks where isTemplate=true AND parentNotebook IS NULL
- [ ] Order by title ascending
- [ ] Include child count in result (optional optimization)

**Implementation:**

```java
@Override
public List<NoteBook> findAllParentTemplates() {
    String hql = "FROM NoteBook nb WHERE nb.isTemplate = true AND nb.parentNotebook IS NULL ORDER BY nb.title ASC";
    Query<NoteBook> query = entityManager.unwrap(Session.class).createQuery(hql, NoteBook.class);
    return query.list();
}
```

**Dependencies:** Task 2.1

---

### Task 2.4: Implement Aggregated Entry Queries

**File:** `src/main/java/org/openelisglobal/notebook/dao/NoteBookDAOImpl.java`

**Acceptance Criteria:**

- [ ] Implement `countEntriesForChildren` - returns map of childId -> entryCount
- [ ] Implement `findEntriesByChildIds` - paginated entries across children
- [ ] Support filtering by status, date range, tags

**Implementation:**

```java
@Override
public Map<Integer, Long> countEntriesForChildren(List<Integer> childIds) {
    if (childIds == null || childIds.isEmpty()) {
        return Collections.emptyMap();
    }
    String hql = "SELECT e.notebook.id, COUNT(e) FROM NotebookEntry e WHERE e.notebook.id IN :childIds GROUP BY e.notebook.id";
    Query<Object[]> query = entityManager.unwrap(Session.class).createQuery(hql, Object[].class);
    query.setParameter("childIds", childIds);

    Map<Integer, Long> result = new HashMap<>();
    for (Object[] row : query.list()) {
        result.put((Integer) row[0], (Long) row[1]);
    }
    return result;
}

@Override
public Page<NotebookEntry> findEntriesByChildIds(List<Integer> childIds, Pageable pageable) {
    if (childIds == null || childIds.isEmpty()) {
        return Page.empty(pageable);
    }

    String countHql = "SELECT COUNT(e) FROM NotebookEntry e WHERE e.notebook.id IN :childIds";
    String dataHql = "FROM NotebookEntry e WHERE e.notebook.id IN :childIds ORDER BY e.dateCreated DESC";

    Query<Long> countQuery = entityManager.unwrap(Session.class).createQuery(countHql, Long.class);
    countQuery.setParameter("childIds", childIds);
    Long total = countQuery.uniqueResult();

    Query<NotebookEntry> dataQuery = entityManager.unwrap(Session.class).createQuery(dataHql, NotebookEntry.class);
    dataQuery.setParameter("childIds", childIds);
    dataQuery.setFirstResult((int) pageable.getOffset());
    dataQuery.setMaxResults(pageable.getPageSize());

    return new PageImpl<>(dataQuery.list(), pageable, total);
}
```

**Dependencies:** Task 2.1

---

## Phase 3: Service Layer

### Task 3.1: Add Service Interface Methods

**File:**
`src/main/java/org/openelisglobal/notebook/service/NoteBookService.java`

**Acceptance Criteria:**

- [ ] Add `createChildInstance(String parentId, String title, String sysUserId)`
      signature
- [ ] Add `getChildInstances(String parentId)` signature
- [ ] Add `getHierarchyTree(String sysUserId)` signature
- [ ] Add `getAggregatedEntriesForParent(String parentId, Pageable, filters)`
      signature
- [ ] Add `getAggregatedStatistics(String parentId)` signature
- [ ] Add `canAcceptEntries(String notebookId)` signature

**Dependencies:** Task 2.4

---

### Task 3.2: Implement createChildInstance

**File:**
`src/main/java/org/openelisglobal/notebook/service/NoteBookServiceImpl.java`

**Acceptance Criteria:**

- [ ] Validate parent exists and is a parent template
- [ ] Create new NoteBook with isTemplate=false
- [ ] Copy metadata: title, objective, protocol, content
- [ ] Copy access control: organizations, departments, allowedRoles
- [ ] Set parentNotebook reference
- [ ] DO NOT copy pages (live inheritance)
- [ ] Set creator and audit fields
- [ ] Return saved child instance

**Implementation:**

```java
@Override
@Transactional
public NoteBook createChildInstance(String parentId, String title, String sysUserId) {
    NoteBook parent = get(parentId);
    if (parent == null) {
        throw new IllegalArgumentException("Parent notebook not found: " + parentId);
    }
    if (!parent.isParentTemplate()) {
        throw new IllegalArgumentException("Cannot create instance from non-template notebook");
    }

    NoteBook child = new NoteBook();
    child.setTitle(title);
    child.setIsTemplate(false);
    child.setParentNotebook(parent);

    // Copy metadata
    child.setObjective(parent.getObjective());
    child.setProtocol(parent.getProtocol());
    child.setContent(parent.getContent());
    child.setType(parent.getType());

    // Copy access control (deep copy sets)
    child.setOrganizations(new HashSet<>(parent.getOrganizations()));
    child.setDepartments(new HashSet<>(parent.getDepartments()));
    child.setAllowedRoles(new ArrayList<>(parent.getAllowedRoles()));

    // Set audit fields
    child.setSysUserId(sysUserId);
    child.setStatus(NoteBookStatus.ACTIVE);

    // Pages are NOT copied - they inherit from parent via getEffectivePages()

    return save(child);
}
```

**Dependencies:** Task 3.1

---

### Task 3.3: Implement getHierarchyTree

**File:**
`src/main/java/org/openelisglobal/notebook/service/NoteBookServiceImpl.java`

**Acceptance Criteria:**

- [ ] Fetch all parent templates
- [ ] For each parent, fetch children
- [ ] Calculate entry counts at parent (aggregate) and child levels
- [ ] Apply security filtering based on user's organizations/departments
- [ ] Return nested DTO structure for frontend

**Implementation:**

```java
@Override
@Transactional(readOnly = true)
public List<NotebookHierarchyDTO> getHierarchyTree(String sysUserId) {
    List<NoteBook> parents = baseObjectDAO.findAllParentTemplates();
    List<NotebookHierarchyDTO> hierarchy = new ArrayList<>();

    for (NoteBook parent : parents) {
        // Security check
        if (!notebookSecurityService.canViewTemplate(parent, sysUserId)) {
            continue;
        }

        NotebookHierarchyDTO parentDTO = new NotebookHierarchyDTO();
        parentDTO.setId(parent.getId());
        parentDTO.setTitle(parent.getTitle());
        parentDTO.setIsParentTemplate(true);

        List<NoteBook> children = baseObjectDAO.findChildrenByParentId(parent.getId());
        List<Integer> childIds = children.stream().map(NoteBook::getId).collect(Collectors.toList());
        Map<Integer, Long> entryCounts = baseObjectDAO.countEntriesForChildren(childIds);

        long totalEntries = 0;
        List<NotebookHierarchyDTO> childDTOs = new ArrayList<>();

        for (NoteBook child : children) {
            NotebookHierarchyDTO childDTO = new NotebookHierarchyDTO();
            childDTO.setId(child.getId());
            childDTO.setTitle(child.getTitle());
            childDTO.setIsChildInstance(true);
            long childEntryCount = entryCounts.getOrDefault(child.getId(), 0L);
            childDTO.setEntryCount((int) childEntryCount);
            totalEntries += childEntryCount;
            childDTOs.add(childDTO);
        }

        parentDTO.setChildren(childDTOs);
        parentDTO.setTotalEntries((int) totalEntries);
        hierarchy.add(parentDTO);
    }

    return hierarchy;
}
```

**Dependencies:** Task 3.1, Task 2.3, Task 2.4

---

### Task 3.4: Implement getAggregatedEntriesForParent

**File:**
`src/main/java/org/openelisglobal/notebook/service/NoteBookServiceImpl.java`

**Acceptance Criteria:**

- [ ] Get all child instances for parent
- [ ] Fetch entries across all children
- [ ] Support pagination
- [ ] Support filtering (status, date, tags)
- [ ] Include child notebook info in each entry DTO

**Dependencies:** Task 3.1, Task 2.4

---

### Task 3.5: Implement getAggregatedStatistics

**File:**
`src/main/java/org/openelisglobal/notebook/service/NoteBookServiceImpl.java`

**Acceptance Criteria:**

- [ ] Calculate total entries across all children
- [ ] Calculate drafts count
- [ ] Calculate pending review count
- [ ] Calculate finalized this week count
- [ ] Return statistics DTO

**Dependencies:** Task 3.1

---

### Task 3.6: Implement canAcceptEntries

**File:**
`src/main/java/org/openelisglobal/notebook/service/NoteBookServiceImpl.java`

**Acceptance Criteria:**

- [ ] Return false for parent templates
- [ ] Return true for child instances
- [ ] Used to validate entry creation requests

**Implementation:**

```java
@Override
@Transactional(readOnly = true)
public boolean canAcceptEntries(String notebookId) {
    NoteBook notebook = get(notebookId);
    if (notebook == null) {
        return false;
    }
    return notebook.isChildInstance();
}
```

**Dependencies:** Task 3.1

---

## Phase 4: REST Controller

### Task 4.1: Add Create Instance Endpoint

**File:**
`src/main/java/org/openelisglobal/notebook/controller/rest/NoteBookRestController.java`

**Acceptance Criteria:**

- [ ] POST `/rest/notebook/{parentId}/instances`
- [ ] Accept title in request body
- [ ] Validate user has permission to create instance
- [ ] Return created child instance DTO

**Implementation:**

```java
@PostMapping("/{parentId}/instances")
public ResponseEntity<NotebookDTO> createChildInstance(
        @PathVariable String parentId,
        @RequestBody CreateInstanceRequest request,
        HttpServletRequest httpRequest) {
    String sysUserId = getSysUserId(httpRequest);
    NoteBook child = noteBookService.createChildInstance(parentId, request.getTitle(), sysUserId);
    return ResponseEntity.ok(convertToDTO(child));
}
```

**Dependencies:** Task 3.2

---

### Task 4.2: Add Get Children Endpoint

**File:**
`src/main/java/org/openelisglobal/notebook/controller/rest/NoteBookRestController.java`

**Acceptance Criteria:**

- [ ] GET `/rest/notebook/{parentId}/instances`
- [ ] Return list of child instance DTOs
- [ ] Include entry count for each child

**Dependencies:** Task 3.3

---

### Task 4.3: Add Hierarchy Endpoint

**File:**
`src/main/java/org/openelisglobal/notebook/controller/rest/NoteBookRestController.java`

**Acceptance Criteria:**

- [ ] GET `/rest/notebook/hierarchy`
- [ ] Return nested tree structure
- [ ] Apply security filtering

**Implementation:**

```java
@GetMapping("/hierarchy")
public ResponseEntity<List<NotebookHierarchyDTO>> getHierarchy(HttpServletRequest request) {
    String sysUserId = getSysUserId(request);
    List<NotebookHierarchyDTO> hierarchy = noteBookService.getHierarchyTree(sysUserId);
    return ResponseEntity.ok(hierarchy);
}
```

**Dependencies:** Task 3.3

---

### Task 4.4: Add Aggregated Entries Endpoint

**File:**
`src/main/java/org/openelisglobal/notebook/controller/rest/NoteBookRestController.java`

**Acceptance Criteria:**

- [ ] GET `/rest/notebook/{parentId}/aggregated-entries`
- [ ] Support pagination parameters
- [ ] Support filter parameters (status, dateFrom, dateTo, tags)
- [ ] Return paginated entry DTOs with child info

**Dependencies:** Task 3.4

---

### Task 4.5: Add Aggregated Stats Endpoint

**File:**
`src/main/java/org/openelisglobal/notebook/controller/rest/NoteBookRestController.java`

**Acceptance Criteria:**

- [ ] GET `/rest/notebook/{parentId}/aggregated-stats`
- [ ] Return statistics DTO

**Dependencies:** Task 3.5

---

## Phase 5: Frontend - TreeView Component

### Task 5.1: Create NotebookTreeView Component

**File:** `frontend/src/components/notebook/NotebookTreeView.js` (NEW)

**Acceptance Criteria:**

- [ ] Use Carbon TreeView component
- [ ] Fetch hierarchy from `/rest/notebook/hierarchy`
- [ ] Render parent templates as expandable nodes
- [ ] Render child instances as leaf nodes
- [ ] Show entry counts at each level
- [ ] Use Template icon for parents, Document icon for children
- [ ] Handle node selection callback

**Implementation Outline:**

```jsx
import React, { useState, useEffect } from "react";
import { TreeView, TreeNode } from "@carbon/react";
import { Template, Document } from "@carbon/icons-react";
import { getFromOpenElisServer } from "../../utils/Utils";
import { FormattedMessage } from "react-intl";

const NotebookTreeView = ({ onSelectNotebook, selectedId }) => {
  const [hierarchy, setHierarchy] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getFromOpenElisServer("/rest/notebook/hierarchy", (data) => {
      setHierarchy(data);
      setLoading(false);
    });
  }, []);

  const handleSelect = (event, node) => {
    onSelectNotebook(node.id, node.isParentTemplate);
  };

  return (
    <TreeView label="Notebooks" onSelect={handleSelect}>
      {hierarchy.map((parent) => (
        <TreeNode
          key={parent.id}
          id={parent.id}
          label={`${parent.title} (${parent.totalEntries})`}
          renderIcon={Template}
          isParentTemplate={true}
        >
          {parent.children.map((child) => (
            <TreeNode
              key={child.id}
              id={child.id}
              label={`${child.title} (${child.entryCount})`}
              renderIcon={Document}
              isChildInstance={true}
            />
          ))}
        </TreeNode>
      ))}
    </TreeView>
  );
};

export default NotebookTreeView;
```

**Dependencies:** Task 4.3

---

### Task 5.2: Add Intl Messages for TreeView

**Files:** `frontend/src/languages/en.json` (and other language files)

**Acceptance Criteria:**

- [ ] Add message: `notebook.tree.title` = "Notebooks"
- [ ] Add message: `notebook.tree.entries` = "entries"
- [ ] Add message: `notebook.tree.noChildren` = "No lab instances"

**Dependencies:** None

---

### Task 5.3: Integrate TreeView into Dashboard

**File:** `frontend/src/components/notebook/NoteBookDashBoard.js`

**Acceptance Criteria:**

- [ ] Replace existing DataTable sidebar with NotebookTreeView
- [ ] Pass `onSelectNotebook` handler
- [ ] Track whether selected item is parent or child
- [ ] Maintain "All Entries" option above tree

**Dependencies:** Task 5.1

---

### Task 5.4: Style TreeView Component

**File:** `frontend/src/components/notebook/NotebookTreeView.css` (NEW)

**Acceptance Criteria:**

- [ ] Style tree nodes with proper indentation
- [ ] Style icons for parent vs child distinction
- [ ] Style selected state
- [ ] Style entry count badges

**Dependencies:** Task 5.1

---

## Phase 6: Frontend - Instance Creation Modal

### Task 6.1: Create CreateInstanceModal Component

**File:** `frontend/src/components/notebook/CreateInstanceModal.js` (NEW)

**Acceptance Criteria:**

- [ ] Modal with title input field
- [ ] Pre-fill title with parent name + suggested suffix
- [ ] Show inherited properties preview (read-only)
- [ ] Confirm and Cancel buttons
- [ ] Call POST endpoint on confirm
- [ ] Show loading state during creation
- [ ] Close and refresh on success

**Implementation Outline:**

```jsx
import React, { useState } from "react";
import { Modal, TextInput, InlineLoading } from "@carbon/react";
import { postToOpenElisServer } from "../../utils/Utils";
import { FormattedMessage, useIntl } from "react-intl";

const CreateInstanceModal = ({ open, onClose, parentNotebook, onSuccess }) => {
  const intl = useIntl();
  const [title, setTitle] = useState(`${parentNotebook?.title} - Lab 1`);
  const [loading, setLoading] = useState(false);

  const handleCreate = () => {
    setLoading(true);
    postToOpenElisServer(
      `/rest/notebook/${parentNotebook.id}/instances`,
      JSON.stringify({ title }),
      (response) => {
        setLoading(false);
        onSuccess(response);
        onClose();
      }
    );
  };

  return (
    <Modal
      open={open}
      modalHeading={intl.formatMessage({ id: "notebook.createInstance.title" })}
      primaryButtonText={intl.formatMessage({ id: "label.button.create" })}
      secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
      onRequestClose={onClose}
      onRequestSubmit={handleCreate}
      primaryButtonDisabled={loading || !title.trim()}
    >
      {loading ? (
        <InlineLoading description="Creating instance..." />
      ) : (
        <>
          <TextInput
            id="instance-title"
            labelText={intl.formatMessage({ id: "notebook.instance.name" })}
            value={title}
            onChange={(e) => setTitle(e.target.value)}
          />
          {/* Preview section */}
        </>
      )}
    </Modal>
  );
};

export default CreateInstanceModal;
```

**Dependencies:** Task 4.1

---

### Task 6.2: Add Intl Messages for Modal

**Files:** `frontend/src/languages/en.json` (and other language files)

**Acceptance Criteria:**

- [ ] Add message: `notebook.createInstance.title` = "Create Lab Instance"
- [ ] Add message: `notebook.instance.name` = "Instance Name"
- [ ] Add message: `notebook.createInstance.success` = "Lab instance created
      successfully"

**Dependencies:** None

---

### Task 6.3: Integrate Modal with Dashboard

**File:** `frontend/src/components/notebook/NoteBookDashBoard.js`

**Acceptance Criteria:**

- [ ] Add state for modal open/close
- [ ] Show "Create Instance" button for parent templates
- [ ] Hide "New Entry" button for parent templates
- [ ] Open modal on button click
- [ ] Refresh tree after successful creation

**Dependencies:** Task 6.1, Task 5.3

---

## Phase 7: Dashboard Updates

### Task 7.1: Detect Parent vs Child Selection

**File:** `frontend/src/components/notebook/NoteBookDashBoard.js`

**Acceptance Criteria:**

- [ ] Track `isParentTemplate` state from tree selection
- [ ] Fetch notebook details including parent info
- [ ] Conditional rendering based on notebook type

**Dependencies:** Task 5.3

---

### Task 7.2: Parent Template View - Aggregated Entries

**File:** `frontend/src/components/notebook/NoteBookDashBoard.js`

**Acceptance Criteria:**

- [ ] When parent is selected, fetch from `/aggregated-entries` endpoint
- [ ] Show entries from all children
- [ ] Add column/badge showing which child each entry belongs to
- [ ] Filters apply across all children

**Dependencies:** Task 4.4, Task 7.1

---

### Task 7.3: Parent Template View - Aggregated Stats

**File:** `frontend/src/components/notebook/NoteBookDashBoard.js`

**Acceptance Criteria:**

- [ ] When parent is selected, fetch from `/aggregated-stats` endpoint
- [ ] Display aggregated statistics in dashboard tiles
- [ ] Optionally show breakdown by child

**Dependencies:** Task 4.5, Task 7.1

---

### Task 7.4: Child Instance View - Breadcrumb

**File:** `frontend/src/components/notebook/NoteBookDashBoard.js`

**Acceptance Criteria:**

- [ ] When child is selected, show breadcrumb: "Parent Name > Child Name"
- [ ] Breadcrumb parent link navigates to parent view
- [ ] Use Carbon Breadcrumb component

**Dependencies:** Task 7.1

---

### Task 7.5: Conditional Button Rendering

**File:** `frontend/src/components/notebook/NoteBookDashBoard.js`

**Acceptance Criteria:**

- [ ] Parent template: Show "Create Instance" button, hide "New Entry"
- [ ] Child instance: Show "New Entry" button, hide "Create Instance"
- [ ] "All Entries" view: Hide both instance-related buttons

**Dependencies:** Task 7.1, Task 6.3

---

## Phase 8: Testing

### Task 8.1: Backend Unit Tests - Service Layer

**File:**
`src/test/java/org/openelisglobal/notebook/service/NoteBookServiceImplTest.java`

**Acceptance Criteria:**

- [ ] Test `createChildInstance` - success case
- [ ] Test `createChildInstance` - parent not found
- [ ] Test `createChildInstance` - not a template
- [ ] Test `getHierarchyTree` - returns correct structure
- [ ] Test `canAcceptEntries` - parent vs child

**Dependencies:** Phase 3

---

### Task 8.2: Backend Unit Tests - DAO Layer

**File:**
`src/test/java/org/openelisglobal/notebook/dao/NoteBookDAOImplTest.java`

**Acceptance Criteria:**

- [ ] Test `findChildrenByParentId`
- [ ] Test `findAllParentTemplates`
- [ ] Test `countEntriesForChildren`

**Dependencies:** Phase 2

---

### Task 8.3: Backend Integration Tests

**File:**
`src/test/java/org/openelisglobal/notebook/NotebookHierarchyIntegrationTest.java`
(NEW)

**Acceptance Criteria:**

- [ ] Test full flow: create parent, create child, add entry
- [ ] Test cascade delete behavior
- [ ] Test aggregated entry retrieval
- [ ] Test security filtering

**Dependencies:** Phase 4

---

### Task 8.4: E2E Tests - Cypress

**File:** `frontend/cypress/e2e/notebookHierarchy.cy.js` (NEW)

**Acceptance Criteria:**

- [ ] Test tree view renders correctly
- [ ] Test expand/collapse parent nodes
- [ ] Test navigate to parent shows aggregated view
- [ ] Test navigate to child shows individual view
- [ ] Test create instance modal flow
- [ ] Test "New Entry" button visibility rules

**Dependencies:** Phase 7

---

## Dependency Graph

```
Phase 1 (DB/Entity)
    │
    ├── Task 1.1 (Migration)
    │       │
    │       └── Task 1.2 (Entity Relations)
    │               │
    │               └── Task 1.3 (Helper Methods)
    │
    ▼
Phase 2 (DAO)
    │
    ├── Task 2.1 (Interface)
    │       │
    │       ├── Task 2.2 (findChildren)
    │       ├── Task 2.3 (findParents)
    │       └── Task 2.4 (Aggregated Queries)
    │
    ▼
Phase 3 (Service)
    │
    ├── Task 3.1 (Interface)
    │       │
    │       ├── Task 3.2 (createChildInstance)
    │       ├── Task 3.3 (getHierarchyTree)
    │       ├── Task 3.4 (getAggregatedEntries)
    │       ├── Task 3.5 (getAggregatedStats)
    │       └── Task 3.6 (canAcceptEntries)
    │
    ▼
Phase 4 (REST) ─────────────────────────┐
    │                                   │
    ├── Task 4.1 (Create Instance)      │
    ├── Task 4.2 (Get Children)         │
    ├── Task 4.3 (Hierarchy)            │
    ├── Task 4.4 (Aggregated Entries)   │
    └── Task 4.5 (Aggregated Stats)     │
                                        │
    ┌───────────────────────────────────┘
    │
    ▼
Phase 5 (TreeView)          Phase 6 (Modal)
    │                           │
    ├── Task 5.1 (Component)    ├── Task 6.1 (Component)
    ├── Task 5.2 (Intl)         ├── Task 6.2 (Intl)
    ├── Task 5.3 (Integrate)    └── Task 6.3 (Integrate)
    └── Task 5.4 (Styles)           │
            │                       │
            └───────────┬───────────┘
                        │
                        ▼
                Phase 7 (Dashboard)
                        │
                        ├── Task 7.1 (Detect Type)
                        ├── Task 7.2 (Aggregated Entries)
                        ├── Task 7.3 (Aggregated Stats)
                        ├── Task 7.4 (Breadcrumb)
                        └── Task 7.5 (Conditional Buttons)
                                │
                                ▼
                        Phase 8 (Testing)
```

---

## Implementation Order (Recommended)

1. **Task 1.1** - Liquibase migration
2. **Task 1.2** - Entity parent relationship
3. **Task 1.3** - Entity helper methods
4. **Task 2.1-2.4** - DAO layer (can be parallelized)
5. **Task 3.1** - Service interface
6. **Task 3.2** - createChildInstance
7. **Task 3.6** - canAcceptEntries
8. **Task 3.3** - getHierarchyTree
9. **Task 4.3** - Hierarchy endpoint
10. **Task 5.1-5.2** - TreeView component + intl
11. **Task 5.3-5.4** - TreeView integration + styles
12. **Task 4.1** - Create instance endpoint
13. **Task 6.1-6.3** - Modal component + integration
14. **Task 3.4-3.5** - Aggregated entries/stats
15. **Task 4.4-4.5** - Aggregated endpoints
16. **Task 7.1-7.5** - Dashboard updates
17. **Task 8.1-8.4** - Testing
