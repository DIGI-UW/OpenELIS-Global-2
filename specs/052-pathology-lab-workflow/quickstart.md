# Quickstart: Pathology Laboratory Workflow

**Date**: 2025-12-14 **Feature**: 052-pathology-lab-workflow **Purpose**:
Developer onboarding guide for implementing pathology workflow

## Prerequisites

Before starting development, ensure you have:

```bash
# Verify Java 21
java -version  # Must show "21.x.x"
sdk env        # Or use SDKMAN auto-switch

# Build project
mvn clean install -DskipTests -Dmaven.test.skip=true

# Start development containers
docker compose -f dev.docker-compose.yml up -d

# Access points
# React UI: https://localhost/
# Legacy UI: https://localhost/api/OpenELIS-Global/
```

---

## Development Workflow Overview

This feature follows the milestone-based development pattern per Constitution
Principle IX:

```
M1: Entities     →  M2: Services    →  M3: Controllers  →  M4: Frontend   →  M5: E2E Tests
(JPA + Liquibase)   (Business Logic)   (REST API)          (React Pages)     (Cypress)
```

---

## Milestone 1: Entities & Database

### Step 1.1: Create Liquibase Changesets

Create migration files in `src/main/resources/liquibase/pathology/`:

```bash
mkdir -p src/main/resources/liquibase/pathology
```

Start with `pathology-001-sample-registration.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.8.xsd">

    <changeSet id="pathology-001-sample-registration" author="speckit">
        <createSequence sequenceName="pathology_sample_registration_seq"
                        startValue="1" incrementBy="1"/>

        <createTable tableName="pathology_sample_registration">
            <column name="id" type="INTEGER">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="sample_item_id" type="VARCHAR(255)">
                <constraints nullable="false" unique="true"/>
            </column>
            <column name="fhir_uuid" type="UUID"/>
            <column name="category" type="VARCHAR(20)">
                <constraints nullable="false"/>
            </column>
            <!-- Add remaining columns per data-model.md -->
            <column name="lastupdated" type="TIMESTAMP"/>
        </createTable>

        <rollback>
            <dropTable tableName="pathology_sample_registration"/>
            <dropSequence sequenceName="pathology_sample_registration_seq"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

Register in master changelog:

```xml
<!-- src/main/resources/liquibase/liquibase-changeLog.xml -->
<include file="pathology/pathology-001-sample-registration.xml" relativeToChangelogFile="true"/>
```

### Step 1.2: Create Entity Classes

Create entity in `src/main/java/org/openelisglobal/pathology/valueholder/`:

```java
package org.openelisglobal.pathology.valueholder;

import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "pathology_sample_registration")
public class PathologySampleRegistration extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
        generator = "pathology_sample_registration_generator")
    @SequenceGenerator(name = "pathology_sample_registration_generator",
        sequenceName = "pathology_sample_registration_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "sample_item_id", nullable = false, unique = true)
    private String sampleItemId;

    @Column(name = "fhir_uuid", columnDefinition = "uuid")
    private UUID fhirUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private SampleCategory category;

    @Column(name = "patient_id")
    private String patientId;

    @Column(name = "requesting_clinician")
    private String requestingClinician;

    @Column(name = "clinical_details", columnDefinition = "TEXT")
    private String clinicalDetails;

    // ... additional fields per data-model.md

    @Version
    private Timestamp lastupdated;

    // Getters and setters
    @Override
    public Integer getId() { return id; }

    @Override
    public void setId(Integer id) { this.id = id; }

    // ... remaining getters/setters
}
```

### Step 1.3: Create ORM Validation Test

Create test in `src/test/java/org/openelisglobal/pathology/`:

```java
package org.openelisglobal.pathology;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.openelisglobal.pathology.valueholder.*;

import static org.junit.Assert.assertNotNull;

public class HibernateMappingValidationTest {

    @Test
    public void testPathologyMappingsLoadSuccessfully() {
        Configuration config = new Configuration();

        // Add all pathology entities
        config.addAnnotatedClass(PathologySampleRegistration.class);
        config.addAnnotatedClass(QualityControlRecord.class);
        config.addAnnotatedClass(ProcessingLogEntry.class);
        config.addAnnotatedClass(TestResultRecord.class);
        config.addAnnotatedClass(StorageEnvironmentLog.class);
        config.addAnnotatedClass(ReferenceDocument.class);
        config.addAnnotatedClass(ProjectAccess.class);

        config.setProperty("hibernate.dialect",
            "org.hibernate.dialect.PostgreSQLDialect");

        SessionFactory sf = config.buildSessionFactory();
        assertNotNull("All Hibernate mappings should load without errors", sf);
        sf.close();
    }
}
```

Run ORM test:

```bash
mvn test -Dtest=HibernateMappingValidationTest
```

---

## Milestone 2: Service Layer

### Step 2.1: Create DAO Interface and Implementation

```java
// dao/PathologySampleRegistrationDAO.java
package org.openelisglobal.pathology.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.pathology.valueholder.PathologySampleRegistration;
import java.util.List;

public interface PathologySampleRegistrationDAO
        extends BaseDAO<PathologySampleRegistration, Integer> {

    PathologySampleRegistration getBySampleItemId(String sampleItemId);
    List<PathologySampleRegistration> getByCategory(SampleCategory category);
    List<PathologySampleRegistration> searchByPatientId(String patientId);
}
```

```java
// dao/PathologySampleRegistrationDAOImpl.java
package org.openelisglobal.pathology.dao;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.pathology.valueholder.PathologySampleRegistration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
@Transactional
public class PathologySampleRegistrationDAOImpl
        extends BaseDAOImpl<PathologySampleRegistration, Integer>
        implements PathologySampleRegistrationDAO {

    public PathologySampleRegistrationDAOImpl() {
        super(PathologySampleRegistration.class);
    }

    @Override
    public PathologySampleRegistration getBySampleItemId(String sampleItemId) {
        String hql = "FROM PathologySampleRegistration WHERE sampleItemId = :sampleItemId";
        return entityManager.createQuery(hql, PathologySampleRegistration.class)
            .setParameter("sampleItemId", sampleItemId)
            .getResultStream()
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<PathologySampleRegistration> getByCategory(SampleCategory category) {
        String hql = "FROM PathologySampleRegistration WHERE category = :category";
        return entityManager.createQuery(hql, PathologySampleRegistration.class)
            .setParameter("category", category)
            .getResultList();
    }

    @Override
    public List<PathologySampleRegistration> searchByPatientId(String patientId) {
        String hql = "FROM PathologySampleRegistration WHERE patientId LIKE :patientId";
        return entityManager.createQuery(hql, PathologySampleRegistration.class)
            .setParameter("patientId", "%" + patientId + "%")
            .getResultList();
    }
}
```

### Step 2.2: Create Service Interface and Implementation

```java
// service/PathologySampleRegistrationService.java
package org.openelisglobal.pathology.service;

import org.openelisglobal.common.service.AuditableBaseObjectService;
import org.openelisglobal.pathology.valueholder.PathologySampleRegistration;
import java.util.List;
import java.util.Map;

public interface PathologySampleRegistrationService
        extends AuditableBaseObjectService<PathologySampleRegistration, Integer> {

    PathologySampleRegistration registerSample(PathologySampleRegistration registration,
                                               String userId);

    PathologySampleRegistration getBySampleItemId(String sampleItemId);

    List<PathologySampleRegistration> searchSamples(String query, String userId);

    Map<String, Object> getSampleWithFullDetails(String sampleItemId);
}
```

```java
// service/PathologySampleRegistrationServiceImpl.java
package org.openelisglobal.pathology.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.pathology.dao.PathologySampleRegistrationDAO;
import org.openelisglobal.pathology.valueholder.PathologySampleRegistration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Service
public class PathologySampleRegistrationServiceImpl
        extends AuditableBaseObjectServiceImpl<PathologySampleRegistration, Integer>
        implements PathologySampleRegistrationService {

    @Autowired
    private PathologySampleRegistrationDAO pathologySampleRegistrationDAO;

    @Autowired
    private ProjectAccessService projectAccessService;

    public PathologySampleRegistrationServiceImpl() {
        super(PathologySampleRegistration.class);
    }

    @Override
    protected BaseDAO<PathologySampleRegistration, Integer> getBaseObjectDAO() {
        return pathologySampleRegistrationDAO;
    }

    @Override
    @Transactional
    public PathologySampleRegistration registerSample(
            PathologySampleRegistration registration, String userId) {

        // Set defaults
        if (registration.getSampleSource() == null) {
            registration.setSampleSource("Alert Hospital");
        }
        registration.setReceivingDate(Timestamp.from(Instant.now()));

        // Generate FHIR UUID
        registration.setFhirUuid(UUID.randomUUID());

        // Persist
        Integer id = insert(registration);
        registration.setId(id);

        return registration;
    }

    @Override
    @Transactional(readOnly = true)
    public PathologySampleRegistration getBySampleItemId(String sampleItemId) {
        return pathologySampleRegistrationDAO.getBySampleItemId(sampleItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PathologySampleRegistration> searchSamples(String query, String userId) {
        List<PathologySampleRegistration> allResults =
            pathologySampleRegistrationDAO.searchByPatientId(query);

        // Filter by access control for research samples
        return allResults.stream()
            .filter(sample -> {
                if (sample.getCategory() == SampleCategory.CLINICAL) {
                    return true;  // Clinical samples - no restriction
                }
                // Research samples - check project access
                return projectAccessService.hasAccess(
                    sample.getStudyId(), userId);
            })
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSampleWithFullDetails(String sampleItemId) {
        // CRITICAL: Compile all data within transaction to avoid LazyInitializationException
        PathologySampleRegistration reg = getBySampleItemId(sampleItemId);
        if (reg == null) return null;

        Map<String, Object> result = new HashMap<>();
        result.put("registration", reg);

        // Fetch related QC records
        List<QualityControlRecord> qcRecords =
            qualityControlService.getBySampleItemId(sampleItemId);
        result.put("qcRecords", qcRecords);

        // Fetch processing log
        List<ProcessingLogEntry> processingLog =
            processingLogService.getBySampleItemId(sampleItemId);
        result.put("processingLog", processingLog);

        // Fetch test results
        List<TestResultRecord> testResults =
            testResultService.getBySampleItemId(sampleItemId);
        result.put("testResults", testResults);

        return result;
    }
}
```

### Step 2.3: Create Unit Tests (TDD)

Write tests FIRST (Red), then implement to pass (Green):

```java
// service/PathologySampleRegistrationServiceTest.java
package org.openelisglobal.pathology.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.pathology.dao.PathologySampleRegistrationDAO;
import org.openelisglobal.pathology.valueholder.PathologySampleRegistration;
import org.openelisglobal.pathology.valueholder.SampleCategory;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PathologySampleRegistrationServiceTest {

    @Mock
    private PathologySampleRegistrationDAO pathologySampleRegistrationDAO;

    @Mock
    private ProjectAccessService projectAccessService;

    @InjectMocks
    private PathologySampleRegistrationServiceImpl service;

    @Test
    public void testRegisterSample_SetsDefaultSource() {
        // Arrange
        PathologySampleRegistration reg = new PathologySampleRegistration();
        reg.setCategory(SampleCategory.CLINICAL);
        reg.setSampleItemId("SAMPLE-001");
        // Note: sampleSource is NULL

        when(pathologySampleRegistrationDAO.insert(any())).thenReturn(1);

        // Act
        PathologySampleRegistration result = service.registerSample(reg, "user1");

        // Assert
        assertEquals("Alert Hospital", result.getSampleSource());
        assertNotNull(result.getReceivingDate());
        assertNotNull(result.getFhirUuid());
    }

    @Test
    public void testSearchSamples_FiltersByProjectAccess() {
        // Arrange
        PathologySampleRegistration clinical = new PathologySampleRegistration();
        clinical.setCategory(SampleCategory.CLINICAL);
        clinical.setPatientId("P001");

        PathologySampleRegistration research = new PathologySampleRegistration();
        research.setCategory(SampleCategory.RESEARCH);
        research.setStudyId("STUDY-001");

        when(pathologySampleRegistrationDAO.searchByPatientId("P00"))
            .thenReturn(Arrays.asList(clinical, research));
        when(projectAccessService.hasAccess("STUDY-001", "user1"))
            .thenReturn(false);  // User has no access

        // Act
        List<PathologySampleRegistration> results =
            service.searchSamples("P00", "user1");

        // Assert
        assertEquals(1, results.size());  // Only clinical returned
        assertEquals(SampleCategory.CLINICAL, results.get(0).getCategory());
    }
}
```

Run service tests:

```bash
mvn test -Dtest=PathologySampleRegistrationServiceTest
```

---

## Milestone 3: REST Controllers

### Step 3.1: Create Controller

```java
// controller/rest/PathologySampleController.java
package org.openelisglobal.pathology.controller.rest;

import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.pathology.form.PathologySampleForm;
import org.openelisglobal.pathology.service.PathologySampleRegistrationService;
import org.openelisglobal.pathology.valueholder.PathologySampleRegistration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest/pathology/samples")
public class PathologySampleController extends BaseRestController {

    @Autowired
    private PathologySampleRegistrationService registrationService;

    @PostMapping
    public ResponseEntity<?> registerSample(
            @Valid @RequestBody PathologySampleForm form) {

        String userId = getSysUserId(request);

        PathologySampleRegistration registration = new PathologySampleRegistration();
        // Map form to entity
        registration.setCategory(form.getCategory());
        registration.setSampleItemId(form.getSampleItemId());
        registration.setPatientId(form.getPatientId());
        registration.setRequestingClinician(form.getRequestingClinician());
        registration.setClinicalDetails(form.getClinicalDetails());
        registration.setStudyId(form.getStudyId());
        registration.setPiName(form.getPiName());
        registration.setSampleSource(form.getSampleSource());

        PathologySampleRegistration result =
            registrationService.registerSample(registration, userId);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    public ResponseEntity<List<PathologySampleRegistration>> searchSamples(
            @RequestParam String query) {

        String userId = getSysUserId(request);
        List<PathologySampleRegistration> results =
            registrationService.searchSamples(query, userId);

        return ResponseEntity.ok(results);
    }

    @GetMapping("/{sampleItemId}")
    public ResponseEntity<Map<String, Object>> getSampleDetails(
            @PathVariable String sampleItemId) {

        Map<String, Object> details =
            registrationService.getSampleWithFullDetails(sampleItemId);

        if (details == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(details);
    }
}
```

### Step 3.2: Create Form/DTO

```java
// form/PathologySampleForm.java
package org.openelisglobal.pathology.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.openelisglobal.pathology.valueholder.SampleCategory;

public class PathologySampleForm {

    @NotBlank(message = "Sample Item ID is required")
    private String sampleItemId;

    @NotNull(message = "Sample category is required")
    private SampleCategory category;

    private String patientId;
    private String requestingClinician;
    private String clinicalDetails;
    private String specimenSite;
    private String studyId;
    private String piName;
    private String participantId;
    private String ethicalApprovalRef;
    private String sampleSource;

    // Getters and setters
    public String getSampleItemId() { return sampleItemId; }
    public void setSampleItemId(String sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public SampleCategory getCategory() { return category; }
    public void setCategory(SampleCategory category) {
        this.category = category;
    }

    // ... remaining getters/setters
}
```

### Step 3.3: Create Integration Test

```java
// controller/PathologySampleControllerTest.java
package org.openelisglobal.pathology.controller;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.pathology.service.PathologySampleRegistrationService;
import org.openelisglobal.pathology.valueholder.PathologySampleRegistration;
import org.openelisglobal.pathology.valueholder.SampleCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class PathologySampleControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PathologySampleRegistrationService registrationService;

    @Test
    public void testRegisterSample_ReturnsCreatedSample() throws Exception {
        // Arrange
        PathologySampleRegistration expected = new PathologySampleRegistration();
        expected.setId(1);
        expected.setSampleItemId("SAMPLE-001");
        expected.setCategory(SampleCategory.CLINICAL);
        expected.setSampleSource("Alert Hospital");

        when(registrationService.registerSample(any(), any()))
            .thenReturn(expected);

        String requestBody = """
            {
                "sampleItemId": "SAMPLE-001",
                "category": "CLINICAL",
                "patientId": "P-12345"
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/rest/pathology/samples")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sampleItemId").value("SAMPLE-001"))
            .andExpect(jsonPath("$.sampleSource").value("Alert Hospital"));
    }

    @Test
    public void testGetSampleDetails_NotFound_Returns404() throws Exception {
        when(registrationService.getSampleWithFullDetails("NONEXISTENT"))
            .thenReturn(null);

        mockMvc.perform(get("/rest/pathology/samples/NONEXISTENT"))
            .andExpect(status().isNotFound());
    }
}
```

Run integration tests:

```bash
mvn test -Dtest=PathologySampleControllerTest
```

---

## Milestone 4: Frontend

### Step 4.1: Create React Page Component

```jsx
// frontend/src/components/pathology/pages/SampleReceptionPage.js
import React, { useState } from "react";
import { useIntl } from "react-intl";
import {
  Form,
  FormGroup,
  TextInput,
  TextArea,
  RadioButtonGroup,
  RadioButton,
  Dropdown,
  Button,
  InlineNotification,
  Grid,
  Column,
} from "@carbon/react";
import { postToOpenElisServer } from "../../utils/Utils";

const SampleReceptionPage = () => {
  const intl = useIntl();

  const [formData, setFormData] = useState({
    sampleItemId: "",
    category: "CLINICAL",
    patientId: "",
    requestingClinician: "",
    clinicalDetails: "",
    specimenSite: "",
    studyId: "",
    piName: "",
    sampleSource: "Alert Hospital",
  });

  const [notification, setNotification] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleCategoryChange = (value) => {
    setFormData((prev) => ({ ...prev, category: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    postToOpenElisServer(
      "/rest/pathology/samples",
      JSON.stringify(formData),
      (response) => {
        setLoading(false);
        if (response.id) {
          setNotification({
            kind: "success",
            title: intl.formatMessage({ id: "pathology.sample.registered" }),
            subtitle: `${intl.formatMessage({
              id: "pathology.accession",
            })}: ${response.sampleItemId}`,
          });
          // Reset form
          setFormData({
            sampleItemId: "",
            category: "CLINICAL",
            patientId: "",
            requestingClinician: "",
            clinicalDetails: "",
            specimenSite: "",
            studyId: "",
            piName: "",
            sampleSource: "Alert Hospital",
          });
        }
      }
    );
  };

  return (
    <Grid>
      <Column lg={16}>
        <h2>{intl.formatMessage({ id: "pathology.reception.title" })}</h2>

        {notification && (
          <InlineNotification
            kind={notification.kind}
            title={notification.title}
            subtitle={notification.subtitle}
            onClose={() => setNotification(null)}
          />
        )}

        <Form onSubmit={handleSubmit}>
          <FormGroup
            legendText={intl.formatMessage({ id: "pathology.category" })}
          >
            <RadioButtonGroup
              name="category"
              valueSelected={formData.category}
              onChange={handleCategoryChange}
            >
              <RadioButton
                value="CLINICAL"
                labelText={intl.formatMessage({
                  id: "pathology.category.clinical",
                })}
              />
              <RadioButton
                value="RESEARCH"
                labelText={intl.formatMessage({
                  id: "pathology.category.research",
                })}
              />
            </RadioButtonGroup>
          </FormGroup>

          <FormGroup
            legendText={intl.formatMessage({ id: "pathology.sampleSource" })}
          >
            <Dropdown
              id="sampleSource"
              items={["Alert Hospital", "External Lab", "Research Project"]}
              selectedItem={formData.sampleSource}
              onChange={(e) =>
                setFormData((prev) => ({
                  ...prev,
                  sampleSource: e.selectedItem,
                }))
              }
              label={intl.formatMessage({ id: "pathology.selectSource" })}
            />
          </FormGroup>

          {formData.category === "CLINICAL" && (
            <>
              <TextInput
                id="patientId"
                name="patientId"
                labelText={intl.formatMessage({ id: "pathology.patientId" })}
                value={formData.patientId}
                onChange={handleInputChange}
              />
              <TextInput
                id="requestingClinician"
                name="requestingClinician"
                labelText={intl.formatMessage({
                  id: "pathology.requestingClinician",
                })}
                value={formData.requestingClinician}
                onChange={handleInputChange}
              />
              <TextArea
                id="clinicalDetails"
                name="clinicalDetails"
                labelText={intl.formatMessage({
                  id: "pathology.clinicalDetails",
                })}
                value={formData.clinicalDetails}
                onChange={handleInputChange}
                rows={4}
              />
            </>
          )}

          {formData.category === "RESEARCH" && (
            <>
              <TextInput
                id="studyId"
                name="studyId"
                labelText={intl.formatMessage({ id: "pathology.studyId" })}
                value={formData.studyId}
                onChange={handleInputChange}
              />
              <TextInput
                id="piName"
                name="piName"
                labelText={intl.formatMessage({ id: "pathology.piName" })}
                value={formData.piName}
                onChange={handleInputChange}
              />
            </>
          )}

          <Button type="submit" disabled={loading}>
            {intl.formatMessage({ id: "pathology.registerSample" })}
          </Button>
        </Form>
      </Column>
    </Grid>
  );
};

export default SampleReceptionPage;
```

### Step 4.2: Add i18n Keys

Add to `frontend/src/languages/en.json`:

```json
{
  "pathology.reception.title": "Sample Reception",
  "pathology.category": "Sample Category",
  "pathology.category.clinical": "Clinical Diagnostic Specimen",
  "pathology.category.research": "Research Specimen",
  "pathology.sampleSource": "Sample Source",
  "pathology.selectSource": "Select source",
  "pathology.patientId": "Patient ID",
  "pathology.requestingClinician": "Requesting Clinician",
  "pathology.clinicalDetails": "Clinical Details",
  "pathology.studyId": "Study ID",
  "pathology.piName": "Principal Investigator",
  "pathology.registerSample": "Register Sample",
  "pathology.sample.registered": "Sample Registered Successfully",
  "pathology.accession": "Accession Number"
}
```

Add to `frontend/src/languages/fr.json`:

```json
{
  "pathology.reception.title": "Réception des échantillons",
  "pathology.category": "Catégorie d'échantillon",
  "pathology.category.clinical": "Spécimen diagnostique clinique",
  "pathology.category.research": "Spécimen de recherche",
  "pathology.sampleSource": "Source de l'échantillon",
  "pathology.selectSource": "Sélectionner la source",
  "pathology.patientId": "ID du patient",
  "pathology.requestingClinician": "Clinicien demandeur",
  "pathology.clinicalDetails": "Détails cliniques",
  "pathology.studyId": "ID de l'étude",
  "pathology.piName": "Chercheur principal",
  "pathology.registerSample": "Enregistrer l'échantillon",
  "pathology.sample.registered": "Échantillon enregistré avec succès",
  "pathology.accession": "Numéro d'accession"
}
```

### Step 4.3: Create Jest Test

```jsx
// frontend/src/components/pathology/__tests__/SampleReceptionPage.test.jsx
import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import SampleReceptionPage from "../pages/SampleReceptionPage";
import messages from "../../../languages/en.json";

jest.mock("../../utils/Utils", () => ({
  postToOpenElisServer: jest.fn(),
}));

import { postToOpenElisServer } from "../../utils/Utils";

const renderWithIntl = (component) => {
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </BrowserRouter>
  );
};

describe("SampleReceptionPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("renders form with Clinical category selected by default", () => {
    renderWithIntl(<SampleReceptionPage />);

    expect(
      screen.getByLabelText(/clinical diagnostic specimen/i)
    ).toBeChecked();
    expect(screen.getByLabelText(/patient id/i)).toBeInTheDocument();
  });

  test("shows research fields when Research category selected", async () => {
    renderWithIntl(<SampleReceptionPage />);

    const researchRadio = screen.getByLabelText(/research specimen/i);
    await userEvent.click(researchRadio);

    await waitFor(() => {
      expect(screen.getByLabelText(/study id/i)).toBeInTheDocument();
      expect(
        screen.getByLabelText(/principal investigator/i)
      ).toBeInTheDocument();
    });
  });

  test("submits form and shows success notification", async () => {
    postToOpenElisServer.mockImplementation((url, data, callback) => {
      callback({ id: 1, sampleItemId: "A24-0001" });
    });

    renderWithIntl(<SampleReceptionPage />);

    const patientIdInput = screen.getByLabelText(/patient id/i);
    await userEvent.type(patientIdInput, "P-12345");

    const submitButton = screen.getByRole("button", {
      name: /register sample/i,
    });
    await userEvent.click(submitButton);

    await waitFor(() => {
      expect(
        screen.getByText(/sample registered successfully/i)
      ).toBeInTheDocument();
    });
  });
});
```

Run Jest tests:

```bash
cd frontend
npm test -- --testPathPattern=SampleReceptionPage
```

---

## Milestone 5: E2E Tests

### Step 5.1: Create Cypress Test

```javascript
// frontend/cypress/e2e/pathologySampleReception.cy.js
describe("Pathology Sample Reception", () => {
  before(() => {
    cy.login("admin", "adminADMIN!");
  });

  beforeEach(() => {
    cy.viewport(1025, 900);
    cy.visit("/pathology/reception");
  });

  it("should register a clinical sample successfully", () => {
    // Arrange
    cy.intercept("POST", "/rest/pathology/samples").as("registerSample");

    // Act
    cy.get('[data-testid="clinical-radio"]').click();
    cy.get('[data-testid="patient-id-input"]').type("P-12345");
    cy.get('[data-testid="clinician-input"]').type("Dr. Smith");
    cy.get('[data-testid="clinical-details-input"]').type(
      "Lymph node biopsy for staging"
    );
    cy.get('[data-testid="submit-button"]').click();

    // Assert
    cy.wait("@registerSample").its("response.statusCode").should("eq", 200);
    cy.get('[data-testid="success-notification"]').should("be.visible");
  });

  it("should show research fields when Research category selected", () => {
    cy.get('[data-testid="research-radio"]').click();

    cy.get('[data-testid="study-id-input"]').should("be.visible");
    cy.get('[data-testid="pi-name-input"]').should("be.visible");
    cy.get('[data-testid="patient-id-input"]').should("not.exist");
  });

  it("should default sample source to Alert Hospital", () => {
    cy.get('[data-testid="sample-source-dropdown"]')
      .find(".cds--list-box__label")
      .should("contain", "Alert Hospital");
  });
});
```

Run E2E test:

```bash
cd frontend
npm run cy:run -- --spec "cypress/e2e/pathologySampleReception.cy.js"
```

---

## Formatting Checklist

Before committing any code:

```bash
# Backend formatting (MUST run)
mvn spotless:apply

# Frontend formatting (MUST run)
cd frontend && npm run format && cd ..

# Verify build
mvn clean install -DskipTests -Dmaven.test.skip=true

# Verify tests pass
mvn test -Dtest=PathologySampleRegistrationServiceTest
mvn test -Dtest=PathologySampleControllerTest
cd frontend && npm test -- --testPathPattern=pathology
```

---

## Constitution Compliance Summary

| Principle                | Status | How Verified                       |
| ------------------------ | ------ | ---------------------------------- |
| I. Configuration-Driven  | ✓      | Sample types via DB config         |
| II. Carbon Design System | ✓      | All components from @carbon/react  |
| III. FHIR Compliance     | ✓      | fhir_uuid on all entities          |
| IV. Layered Architecture | ✓      | Valueholder→DAO→Service→Controller |
| V. TDD                   | ✓      | Unit tests written first           |
| VI. Liquibase            | ✓      | All schema via changesets          |
| VII. i18n                | ✓      | All strings via React Intl         |
| VIII. Security           | ✓      | RBAC, audit trail, validation      |
| IX. Spec-Driven          | ✓      | Milestone-based development        |

---

## Next Steps

1. Complete remaining entities (QualityControlRecord, ProcessingLogEntry, etc.)
2. Implement remaining pages (QC, Processing, Testing, Storage)
3. Run `/speckit.tasks` to generate detailed task breakdown
4. Execute implementation following TDD workflow
