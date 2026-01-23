# Quickstart: Immunology Laboratory Workflow

**Feature**: OGC-51 Immunology Laboratory Workflow **Date**: 2025-12-07

This guide provides step-by-step instructions for implementing the immunology
workflow feature.

---

## Prerequisites

1. **Development Environment**

   ```bash
   # Verify Java 21
   java -version  # Must show 21.x.x
   sdk env        # Auto-switch via SDKMAN

   # Build project
   mvn clean install -DskipTests -Dmaven.test.skip=true
   ```

2. **Read Constitution**

   - Review [constitution principles](.specify/memory/constitution.md)
   - Key principles: Layered Architecture (IV), TDD (V), Carbon Design (II)

3. **Understand Existing Entities**
   - Review [research.md](research.md) for NoteBook, SampleItem, Storage
     structures
   - Review [data-model.md](data-model.md) for new entity definitions

---

## Milestone 1: NotebookPageSample Entity

### Step 1.1: Create Liquibase Changeset

**File**: `src/main/resources/liquibase/3.4.x.x/001-notebook-page-sample.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.8.xsd">

    <changeSet id="notebook-001-page-sample-table" author="openelis">
        <createSequence sequenceName="notebook_page_sample_seq" startValue="1"/>

        <createTable tableName="notebook_page_sample">
            <column name="id" type="INTEGER" defaultValueSequenceNext="notebook_page_sample_seq">
                <constraints primaryKey="true"/>
            </column>
            <column name="notebook_page_id" type="INTEGER">
                <constraints nullable="false"/>
            </column>
            <column name="sample_item_id" type="INTEGER">
                <constraints nullable="false"/>
            </column>
            <column name="status" type="VARCHAR(20)" defaultValue="PENDING">
                <constraints nullable="false"/>
            </column>
            <column name="data" type="JSONB"/>
            <column name="questionnaire_response_uuid" type="UUID"/>
            <column name="completed_by" type="INTEGER"/>
            <column name="completed_at" type="TIMESTAMP"/>
            <column name="lastupdated" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP"/>
        </createTable>

        <addForeignKeyConstraint
            baseTableName="notebook_page_sample"
            baseColumnNames="notebook_page_id"
            referencedTableName="notebook_page"
            referencedColumnNames="id"
            constraintName="fk_nps_page"
            onDelete="CASCADE"/>

        <addForeignKeyConstraint
            baseTableName="notebook_page_sample"
            baseColumnNames="sample_item_id"
            referencedTableName="sample_item"
            referencedColumnNames="id"
            constraintName="fk_nps_sample"/>

        <addForeignKeyConstraint
            baseTableName="notebook_page_sample"
            baseColumnNames="completed_by"
            referencedTableName="system_user"
            referencedColumnNames="id"
            constraintName="fk_nps_completed_by"/>

        <addUniqueConstraint
            tableName="notebook_page_sample"
            columnNames="notebook_page_id, sample_item_id"
            constraintName="uq_nps_page_sample"/>

        <sql>
            ALTER TABLE notebook_page_sample
            ADD CONSTRAINT chk_nps_status
            CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'SKIPPED'));
        </sql>

        <createIndex tableName="notebook_page_sample" indexName="idx_nps_page_status">
            <column name="notebook_page_id"/>
            <column name="status"/>
        </createIndex>

        <createIndex tableName="notebook_page_sample" indexName="idx_nps_sample">
            <column name="sample_item_id"/>
        </createIndex>

        <rollback>
            <dropTable tableName="notebook_page_sample"/>
            <dropSequence sequenceName="notebook_page_sample_seq"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

### Step 1.2: Create Valueholder

**File**:
`src/main/java/org/openelisglobal/notebook/valueholder/NotebookPageSample.java`

```java
package org.openelisglobal.notebook.valueholder;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.valueholder.SystemUser;

import jakarta.persistence.*;

@Entity
@Table(name = "notebook_page_sample",
    uniqueConstraints = @UniqueConstraint(columnNames = {"notebook_page_id", "sample_item_id"}))
public class NotebookPageSample extends BaseObject<Integer> {

    public enum Status {
        PENDING, IN_PROGRESS, COMPLETED, SKIPPED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notebook_page_sample_seq")
    @SequenceGenerator(name = "notebook_page_sample_seq",
        sequenceName = "notebook_page_sample_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_page_id", nullable = false)
    private NoteBookPage notebookPage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_item_id", nullable = false)
    private SampleItem sampleItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", columnDefinition = "jsonb")
    private Map<String, Object> data;

    @Column(name = "questionnaire_response_uuid")
    private UUID questionnaireResponseUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by")
    private SystemUser completedBy;

    @Column(name = "completed_at")
    private Timestamp completedAt;

    // Getters and setters
    @Override
    public Integer getId() { return id; }
    @Override
    public void setId(Integer id) { this.id = id; }

    public NoteBookPage getNotebookPage() { return notebookPage; }
    public void setNotebookPage(NoteBookPage notebookPage) { this.notebookPage = notebookPage; }

    public SampleItem getSampleItem() { return sampleItem; }
    public void setSampleItem(SampleItem sampleItem) { this.sampleItem = sampleItem; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    public UUID getQuestionnaireResponseUuid() { return questionnaireResponseUuid; }
    public void setQuestionnaireResponseUuid(UUID uuid) { this.questionnaireResponseUuid = uuid; }

    public SystemUser getCompletedBy() { return completedBy; }
    public void setCompletedBy(SystemUser completedBy) { this.completedBy = completedBy; }

    public Timestamp getCompletedAt() { return completedAt; }
    public void setCompletedAt(Timestamp completedAt) { this.completedAt = completedAt; }
}
```

### Step 1.3: Create DAO

**File**:
`src/main/java/org/openelisglobal/notebook/dao/NotebookPageSampleDAO.java`

```java
package org.openelisglobal.notebook.dao;

import java.util.List;
import java.util.Map;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.notebook.valueholder.NotebookPageSample.Status;

public interface NotebookPageSampleDAO extends BaseDAO<NotebookPageSample, Integer> {

    List<NotebookPageSample> getByPageId(Integer pageId);

    List<NotebookPageSample> getByPageIdAndStatus(Integer pageId, Status status);

    NotebookPageSample getByPageIdAndSampleItemId(Integer pageId, Integer sampleItemId);

    Map<Status, Long> getStatusCountsByPageId(Integer pageId);

    List<NotebookPageSample> getBySampleItemId(Integer sampleItemId);
}
```

**File**:
`src/main/java/org/openelisglobal/notebook/daoimpl/NotebookPageSampleDAOImpl.java`

```java
package org.openelisglobal.notebook.daoimpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.notebook.dao.NotebookPageSampleDAO;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.notebook.valueholder.NotebookPageSample.Status;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class NotebookPageSampleDAOImpl extends BaseDAOImpl<NotebookPageSample, Integer>
        implements NotebookPageSampleDAO {

    public NotebookPageSampleDAOImpl() {
        super(NotebookPageSample.class);
    }

    @Override
    public List<NotebookPageSample> getByPageId(Integer pageId) {
        String hql = "FROM NotebookPageSample nps " +
                     "LEFT JOIN FETCH nps.sampleItem " +
                     "WHERE nps.notebookPage.id = :pageId " +
                     "ORDER BY nps.sampleItem.id";
        return entityManager.createQuery(hql, NotebookPageSample.class)
                .setParameter("pageId", pageId)
                .getResultList();
    }

    @Override
    public List<NotebookPageSample> getByPageIdAndStatus(Integer pageId, Status status) {
        String hql = "FROM NotebookPageSample nps " +
                     "LEFT JOIN FETCH nps.sampleItem " +
                     "WHERE nps.notebookPage.id = :pageId AND nps.status = :status " +
                     "ORDER BY nps.sampleItem.id";
        return entityManager.createQuery(hql, NotebookPageSample.class)
                .setParameter("pageId", pageId)
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    public NotebookPageSample getByPageIdAndSampleItemId(Integer pageId, Integer sampleItemId) {
        String hql = "FROM NotebookPageSample nps " +
                     "WHERE nps.notebookPage.id = :pageId " +
                     "AND nps.sampleItem.id = :sampleItemId";
        List<NotebookPageSample> results = entityManager.createQuery(hql, NotebookPageSample.class)
                .setParameter("pageId", pageId)
                .setParameter("sampleItemId", sampleItemId.toString())
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public Map<Status, Long> getStatusCountsByPageId(Integer pageId) {
        String hql = "SELECT nps.status, COUNT(nps) FROM NotebookPageSample nps " +
                     "WHERE nps.notebookPage.id = :pageId GROUP BY nps.status";
        List<Object[]> results = entityManager.createQuery(hql, Object[].class)
                .setParameter("pageId", pageId)
                .getResultList();

        Map<Status, Long> counts = new HashMap<>();
        for (Status s : Status.values()) {
            counts.put(s, 0L);
        }
        for (Object[] row : results) {
            counts.put((Status) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    public List<NotebookPageSample> getBySampleItemId(Integer sampleItemId) {
        String hql = "FROM NotebookPageSample nps " +
                     "LEFT JOIN FETCH nps.notebookPage " +
                     "WHERE nps.sampleItem.id = :sampleItemId";
        return entityManager.createQuery(hql, NotebookPageSample.class)
                .setParameter("sampleItemId", sampleItemId.toString())
                .getResultList();
    }
}
```

### Step 1.4: Write ORM Validation Test

**File**:
`src/test/java/org/openelisglobal/notebook/NotebookPageSampleMappingTest.java`

```java
package org.openelisglobal.notebook;

import static org.junit.Assert.assertNotNull;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;

public class NotebookPageSampleMappingTest {

    @Test
    public void testHibernateMappingLoadsSuccessfully() {
        Configuration config = new Configuration();
        config.addAnnotatedClass(NotebookPageSample.class);
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        config.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");

        SessionFactory sf = config.buildSessionFactory();
        assertNotNull("NotebookPageSample mapping should load without errors", sf);
        sf.close();
    }
}
```

---

## Milestone 2: Sample Entry Service

### Step 2.1: Create Service Interface

**File**:
`src/main/java/org/openelisglobal/notebook/service/NotebookSampleEntryService.java`

```java
package org.openelisglobal.notebook.service;

import java.util.List;
import java.util.Map;

import org.openelisglobal.notebook.form.ManifestImportResult;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.web.multipart.MultipartFile;

public interface NotebookSampleEntryService {

    List<SampleItem> searchSamples(String query, int limit);

    int linkSamplesToNotebook(Integer notebookId, List<Integer> sampleItemIds);

    ManifestImportResult importManifest(Integer notebookId, MultipartFile file,
            Map<String, String> columnMapping);

    List<SampleItem> createChildSamples(Integer notebookId, List<Integer> parentIds,
            String idPrefix, Double extractionVolume);
}
```

### Step 2.2: Create Service Implementation

Key points:

- Use `@Transactional` on service methods
- Batch process large lists (50 items per batch)
- Validate sample types against TypeOfSample
- Create NotebookPageSample records for all pages when linking

---

## Milestone 3: Bulk Operations Service

### Step 3.1: Batch Processing Pattern

```java
@Service
@Transactional
public class NotebookBulkOperationServiceImpl implements NotebookBulkOperationService {

    private static final int BATCH_SIZE = 50;

    @Override
    public BulkOperationResult applyToSamples(Integer pageId, List<Integer> sampleIds,
            Map<String, Object> data) {
        int updated = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < sampleIds.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, sampleIds.size());
            List<Integer> batch = sampleIds.subList(i, endIndex);

            try {
                updated += processBatch(pageId, batch, data);
                entityManager.flush();
                entityManager.clear(); // Prevent memory buildup
            } catch (Exception e) {
                errors.add("Batch " + (i / BATCH_SIZE) + " failed: " + e.getMessage());
            }
        }

        return new BulkOperationResult(updated, errors);
    }

    private int processBatch(Integer pageId, List<Integer> sampleIds,
            Map<String, Object> data) {
        String hql = "UPDATE NotebookPageSample nps SET nps.data = :data " +
                     "WHERE nps.notebookPage.id = :pageId " +
                     "AND nps.sampleItem.id IN :sampleIds";
        return entityManager.createQuery(hql)
                .setParameter("data", data)
                .setParameter("pageId", pageId)
                .setParameter("sampleIds", sampleIds.stream()
                        .map(String::valueOf).collect(Collectors.toList()))
                .executeUpdate();
    }
}
```

---

## Milestone 6: Frontend Workflow UI

### Step 6.1: SampleGrid Component with Virtualization

**File**: `frontend/src/components/notebook/workflow/SampleGrid.js`

```javascript
import React, { useState, useMemo, useCallback } from "react";
import { useIntl } from "react-intl";
import {
  DataTable,
  Table,
  TableHead,
  TableBody,
  TableRow,
  TableHeader,
  TableCell,
  TableSelectRow,
  TableSelectAll,
  Pagination,
  Tag,
  InlineLoading,
} from "@carbon/react";
import { FixedSizeList as List } from "react-window";

const SampleGrid = ({
  samples,
  loading,
  selectedIds,
  onSelectionChange,
  onSampleEdit,
  columns,
  pageSize = 50,
  totalItems,
}) => {
  const intl = useIntl();
  const [currentPage, setCurrentPage] = useState(1);

  const headers = useMemo(
    () =>
      columns.map((col) => ({
        key: col.key,
        header: intl.formatMessage({ id: col.labelId }),
      })),
    [columns, intl]
  );

  const rows = useMemo(
    () =>
      samples.map((sample) => ({
        id: String(sample.sampleItemId),
        ...sample,
      })),
    [samples]
  );

  const getStatusTag = (status) => {
    const tagType =
      {
        PENDING: "gray",
        IN_PROGRESS: "blue",
        COMPLETED: "green",
        SKIPPED: "purple",
      }[status] || "gray";

    return <Tag type={tagType}>{status}</Tag>;
  };

  if (loading) {
    return (
      <InlineLoading description={intl.formatMessage({ id: "loading" })} />
    );
  }

  return (
    <>
      <DataTable
        rows={rows}
        headers={headers}
        isSortable
        render={({
          rows,
          headers,
          getTableProps,
          getHeaderProps,
          getRowProps,
          getSelectionProps,
          selectedRows,
        }) => (
          <Table {...getTableProps()}>
            <TableHead>
              <TableRow>
                <TableSelectAll {...getSelectionProps()} />
                {headers.map((header) => (
                  <TableHeader key={header.key} {...getHeaderProps({ header })}>
                    {header.header}
                  </TableHeader>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.id} {...getRowProps({ row })}>
                  <TableSelectRow {...getSelectionProps({ row })} />
                  {row.cells.map((cell) => (
                    <TableCell key={cell.id}>
                      {cell.info.header === "status"
                        ? getStatusTag(cell.value)
                        : cell.value}
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      />
      <Pagination
        totalItems={totalItems}
        pageSize={pageSize}
        pageSizes={[10, 20, 30, 50, 100]}
        page={currentPage}
        onChange={({ page, pageSize }) => {
          setCurrentPage(page);
          // Trigger data fetch for new page
        }}
      />
    </>
  );
};

export default SampleGrid;
```

### Step 6.2: Add Internationalization Keys

**File**: `frontend/src/languages/en.json` (add entries)

```json
{
  "notebook.workflow.tab": "Workflow",
  "notebook.workflow.progress": "{completed}/{total} samples completed",
  "notebook.workflow.page.reception": "Sample Reception",
  "notebook.workflow.page.processing": "Initial Processing",
  "notebook.workflow.page.assays": "Pre-Analysis Assays",
  "notebook.workflow.page.childSamples": "Child Sample Creation",
  "notebook.workflow.page.prep": "Sample Preparation",
  "notebook.workflow.page.analysis": "Main Analysis Execution",
  "notebook.workflow.page.storage": "Post-Analysis Handling",
  "notebook.workflow.page.results": "Result Compilation",
  "notebook.workflow.page.archive": "End of Project Archiving",
  "notebook.workflow.bulkApply": "Apply to Selected",
  "notebook.workflow.markComplete": "Mark Page Complete",
  "notebook.workflow.selectAll": "Select All",
  "notebook.workflow.status.pending": "Pending",
  "notebook.workflow.status.inProgress": "In Progress",
  "notebook.workflow.status.completed": "Completed",
  "notebook.workflow.status.skipped": "Skipped",
  "notebook.workflow.routing.internal": "Internal Analysis",
  "notebook.workflow.routing.external": "External Lab",
  "notebook.workflow.routing.storage": "Storage",
  "notebook.workflow.analyzer.import": "Import Analyzer Results",
  "notebook.workflow.analyzer.mapColumns": "Map Columns",
  "notebook.workflow.analyzer.preview": "Preview Import"
}
```

---

## Running Tests

### Backend Unit Tests

```bash
mvn test -Dtest=NotebookPageSampleServiceTest
```

### Backend Integration Tests

```bash
mvn test -Dtest=NotebookBulkOperationControllerTest
```

### Frontend Unit Tests

```bash
cd frontend
npm test -- --testPathPattern=SampleGrid
```

### E2E Tests (Individual)

```bash
cd frontend
npm run cy:run -- --spec "cypress/e2e/notebookWorkflowSampleReception.cy.js"
```

---

## Pre-Commit Checklist

```bash
# 1. Format code
mvn spotless:apply
cd frontend && npm run format && cd ..

# 2. Build backend
mvn clean install -DskipTests -Dmaven.test.skip=true

# 3. Run tests
mvn test
cd frontend && npm test && cd ..

# 4. Run individual E2E test
cd frontend && npm run cy:run -- --spec "cypress/e2e/notebookWorkflow*.cy.js"
```

---

## Constitution Compliance Checklist

- [ ] **Layered Architecture**: Valueholder -> DAO -> Service -> Controller
- [ ] **Carbon Design System**: Only @carbon/react components, no custom CSS
      frameworks
- [ ] **Internationalization**: All strings via React Intl
- [ ] **@Transactional**: Only in service layer, NOT controllers
- [ ] **Data Compilation**: Services eagerly fetch all data within transaction
- [ ] **Liquibase**: All schema changes via changesets with rollback
- [ ] **JUnit 4**: Use org.junit.Test, not org.junit.jupiter
- [ ] **jakarta.persistence**: Not javax.persistence
- [ ] **Individual E2E Tests**: Run single test files during development
