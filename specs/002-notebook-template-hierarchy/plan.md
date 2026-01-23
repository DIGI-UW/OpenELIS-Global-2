# Implementation Plan: Notebook Template Hierarchy

**Spec Reference:** [spec.md](./spec.md) **Status:** Draft **Created:**
2025-12-28

---

## Constitution Compliance Check

| Principle                  | Status | Notes                                               |
| -------------------------- | ------ | --------------------------------------------------- |
| 5-Layer Architecture       | ✅     | Valueholder → DAO → Service → Controller → Frontend |
| Carbon Design System       | ✅     | Using TreeView from @carbon/react                   |
| React Intl                 | ✅     | All new strings via FormattedMessage                |
| Liquibase Migrations       | ✅     | New changeset for parent_notebook_id                |
| @Transactional in Services | ✅     | All DB operations in service layer                  |
| TDD Workflow               | ✅     | Tests written before implementation                 |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         FRONTEND                                │
├─────────────────────────────────────────────────────────────────┤
│  NoteBookDashBoard.js                                           │
│    ├── NotebookTreeView.js (NEW) - Carbon TreeView sidebar      │
│    ├── CreateInstanceModal.js (NEW) - Instance creation         │
│    └── AggregatedEntriesView.js (NEW) - Parent entry view       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      REST CONTROLLER                            │
├─────────────────────────────────────────────────────────────────┤
│  NoteBookRestController.java                                    │
│    ├── POST /{parentId}/instances - Create child instance       │
│    ├── GET  /{parentId}/instances - List children               │
│    ├── GET  /hierarchy - Get tree structure                     │
│    └── GET  /{parentId}/aggregated-entries - Aggregated entries │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        SERVICE LAYER                            │
├─────────────────────────────────────────────────────────────────┤
│  NoteBookService.java / NoteBookServiceImpl.java                │
│    ├── createChildInstance()                                    │
│    ├── getChildInstances()                                      │
│    ├── getHierarchyTree()                                       │
│    ├── getAggregatedEntriesForParent()                          │
│    ├── getAggregatedStatistics()                                │
│    └── canAcceptEntries()                                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                          DAO LAYER                              │
├─────────────────────────────────────────────────────────────────┤
│  NoteBookDAO.java / NoteBookDAOImpl.java                        │
│    ├── findChildrenByParentId()                                 │
│    ├── findAllParentTemplates()                                 │
│    ├── findEntriesForChildren()                                 │
│    └── countEntriesForParent()                                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                       VALUEHOLDER                               │
├─────────────────────────────────────────────────────────────────┤
│  NoteBook.java                                                  │
│    ├── parentNotebook (ManyToOne)                               │
│    ├── childInstances (OneToMany)                               │
│    ├── isParentTemplate()                                       │
│    ├── isChildInstance()                                        │
│    └── getEffectivePages()                                      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        DATABASE                                 │
├─────────────────────────────────────────────────────────────────┤
│  notebook table                                                 │
│    └── parent_notebook_id (FK to notebook.id, ON DELETE CASCADE)│
└─────────────────────────────────────────────────────────────────┘
```

---

## Implementation Phases

### Phase 1: Database & Entity Layer

**Files to modify:**

- [notebook.xml](../../src/main/resources/liquibase/3.2.x.x/notebook.xml) -
  Reference only
- NEW: `src/main/resources/liquibase/3.5.x.x/001-notebook-hierarchy.xml`
- [NoteBook.java](../../src/main/java/org/openelisglobal/notebook/valueholder/NoteBook.java)

**Tasks:**

1. Create Liquibase migration to add `parent_notebook_id` column
2. Add foreign key constraint with ON DELETE CASCADE
3. Add index on `parent_notebook_id` for query performance
4. Update NoteBook entity with parent/child relationships
5. Add helper methods: `isParentTemplate()`, `isChildInstance()`,
   `getEffectivePages()`

### Phase 2: DAO Layer

**Files to modify:**

- [NoteBookDAO.java](../../src/main/java/org/openelisglobal/notebook/dao/NoteBookDAO.java)
- [NoteBookDAOImpl.java](../../src/main/java/org/openelisglobal/notebook/dao/NoteBookDAOImpl.java)

**Tasks:**

1. Add `findChildrenByParentId(Integer parentId)` method
2. Add `findAllParentTemplates()` method (isTemplate=true AND
   parentNotebookId=null)
3. Add `findEntriesForChildren(List<Integer> childIds)` method
4. Add `countEntriesForParent(Integer parentId)` method
5. Optimize existing `findParentTemplate()` to use new column

### Phase 3: Service Layer

**Files to modify:**

- [NoteBookService.java](../../src/main/java/org/openelisglobal/notebook/service/NoteBookService.java)
- [NoteBookServiceImpl.java](../../src/main/java/org/openelisglobal/notebook/service/NoteBookServiceImpl.java)

**Tasks:**

1. Add `createChildInstance(String parentId, String title, String sysUserId)`
   method
   - Validate parent is a template
   - Copy metadata (title, objective, protocol, organizations, roles)
   - Set parentNotebook reference
   - Set isTemplate=false
   - DO NOT copy pages (they inherit from parent)
2. Add `getChildInstances(String parentId)` method
3. Add `getHierarchyTree()` method - returns nested structure for TreeView
4. Add `getAggregatedEntriesForParent(String parentId, Pageable, filters)`
   method
5. Add `getAggregatedStatistics(String parentId)` method
6. Add `canAcceptEntries(String notebookId)` method - returns true only for
   children
7. Update entry creation to validate notebook can accept entries

### Phase 4: REST Controller

**Files to modify:**

- [NoteBookRestController.java](../../src/main/java/org/openelisglobal/notebook/controller/rest/NoteBookRestController.java)

**Tasks:**

1. Add `POST /rest/notebook/{parentId}/instances` endpoint
2. Add `GET /rest/notebook/{parentId}/instances` endpoint
3. Add `GET /rest/notebook/hierarchy` endpoint - returns tree structure
4. Add `GET /rest/notebook/{parentId}/aggregated-entries` endpoint
5. Add `GET /rest/notebook/{parentId}/aggregated-stats` endpoint
6. Modify entry creation endpoint to validate canAcceptEntries

### Phase 5: Frontend - TreeView Component

**Files to create:**

- `frontend/src/components/notebook/NotebookTreeView.js`

**Files to modify:**

- [NoteBookDashBoard.js](../../frontend/src/components/notebook/NoteBookDashBoard.js)
- `frontend/src/languages/en.json` (and other language files)

**Tasks:**

1. Create `NotebookTreeView` component using Carbon TreeView
2. Fetch hierarchy data from `/rest/notebook/hierarchy`
3. Render parent templates as expandable nodes with Template icon
4. Render child instances as leaf nodes with Document icon
5. Show entry counts at both levels
6. Handle node selection to update main content area
7. Replace existing DataTable sidebar with TreeView
8. Add intl messages for new UI strings

### Phase 6: Frontend - Instance Creation

**Files to create:**

- `frontend/src/components/notebook/CreateInstanceModal.js`

**Tasks:**

1. Create modal component with:
   - Title input field
   - Preview of inherited properties (read-only)
   - Confirm/Cancel buttons
2. Call `POST /rest/notebook/{parentId}/instances` on confirm
3. Refresh tree view after successful creation
4. Add intl messages

### Phase 7: Frontend - Dashboard Updates

**Files to modify:**

- [NoteBookDashBoard.js](../../frontend/src/components/notebook/NoteBookDashBoard.js)

**Tasks:**

1. Detect if selected notebook is parent or child
2. For parent templates:
   - Show "Create Instance" button instead of "New Entry"
   - Fetch and display aggregated entries from all children
   - Show aggregated statistics
   - Indicate which child each entry belongs to
3. For child instances:
   - Keep existing "New Entry" button
   - Show breadcrumb to parent template
   - Show individual statistics
4. Update entry card to show source child name when viewing parent

---

## File Changes Summary

### New Files

| File                                                              | Purpose                 |
| ----------------------------------------------------------------- | ----------------------- |
| `src/main/resources/liquibase/3.5.x.x/001-notebook-hierarchy.xml` | DB migration            |
| `frontend/src/components/notebook/NotebookTreeView.js`            | TreeView sidebar        |
| `frontend/src/components/notebook/CreateInstanceModal.js`         | Instance creation modal |

### Modified Files

| File                            | Changes                                            |
| ------------------------------- | -------------------------------------------------- |
| `NoteBook.java`                 | Add parentNotebook, childInstances, helper methods |
| `NoteBookDAO.java`              | Add hierarchy query methods                        |
| `NoteBookDAOImpl.java`          | Implement hierarchy queries                        |
| `NoteBookService.java`          | Add hierarchy service methods                      |
| `NoteBookServiceImpl.java`      | Implement hierarchy logic                          |
| `NoteBookRestController.java`   | Add hierarchy endpoints                            |
| `NoteBookDashBoard.js`          | Integrate TreeView, conditional rendering          |
| `frontend/src/languages/*.json` | Add intl messages                                  |

---

## API Contract

### POST /rest/notebook/{parentId}/instances

**Request:**

```json
{
  "title": "Pharmaceuticals Laboratory - Lab 1"
}
```

**Response:**

```json
{
  "id": 123,
  "title": "Pharmaceuticals Laboratory - Lab 1",
  "parentNotebookId": 45,
  "isChildInstance": true,
  "status": "ACTIVE"
}
```

### GET /rest/notebook/hierarchy

**Response:**

```json
[
  {
    "id": 45,
    "title": "Pharmaceuticals Laboratory",
    "isParentTemplate": true,
    "totalEntries": 15,
    "children": [
      {
        "id": 123,
        "title": "Pharmaceuticals Laboratory - Lab 1",
        "isChildInstance": true,
        "entryCount": 8
      },
      {
        "id": 124,
        "title": "Pharmaceuticals Laboratory - Lab 2",
        "isChildInstance": true,
        "entryCount": 7
      }
    ]
  },
  {
    "id": 46,
    "title": "Immunology Laboratory",
    "isParentTemplate": true,
    "totalEntries": 3,
    "children": []
  }
]
```

### GET /rest/notebook/{parentId}/aggregated-entries

**Response:**

```json
{
  "content": [
    {
      "id": 501,
      "title": "Entry #1",
      "status": "DRAFT",
      "childNotebookId": 123,
      "childNotebookTitle": "Pharmaceuticals Laboratory - Lab 1",
      "dateCreated": "2025-12-28"
    }
  ],
  "totalElements": 15,
  "totalPages": 2,
  "number": 0
}
```

---

## Testing Strategy

### Unit Tests (Backend)

- `NoteBookServiceImplTest.java` - Test hierarchy methods
- `NoteBookDAOImplTest.java` - Test DAO queries

### Integration Tests

- Test child instance creation
- Test aggregated entry retrieval
- Test cascade delete behavior

### E2E Tests (Cypress)

- `notebookHierarchy.cy.js` - Test tree navigation
- `notebookInstanceCreation.cy.js` - Test instance creation flow

---

## Migration Considerations

1. **Existing Data:** All current notebooks with `isTemplate=true` and no parent
   will become parent templates
2. **Existing Entries:** Current entries linked via `notebook_entries` table
   continue to work
3. **No Data Loss:** Migration only adds column, doesn't modify existing data
4. **Rollback:** Column is nullable, can be removed if needed

---

## Risk Assessment

| Risk                            | Mitigation                              |
| ------------------------------- | --------------------------------------- |
| Performance with many children  | Index on parent_notebook_id, pagination |
| Live inheritance complexity     | Clear documentation, careful testing    |
| UI complexity with TreeView     | Follow Carbon design patterns           |
| Breaking existing functionality | Extensive testing, feature flag option  |

---

## Dependencies

- Carbon Design System TreeView component (`@carbon/react`)
- React Intl for internationalization
- Existing NoteBook entity and service infrastructure
- NotebookEntry system for entry management
