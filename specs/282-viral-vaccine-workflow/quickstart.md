# Quickstart Guide: Viral and Vaccine Laboratory Workflow

**Feature**: `282-viral-vaccine-workflow`  
**Date**: 2025-12-14  
**Target Audience**: Developers implementing this feature

## Prerequisites

Before starting implementation, ensure you have:

✅ **Development Environment Setup**:
- Java 21 LTS installed (`sdk use java 21.0.2-open`)
- Maven 3.8+ (`mvn -version`)
- Node.js 16+ (`node --version`)
- Docker + Docker Compose running
- PostgreSQL 14+ (via Docker)

✅ **Repository Setup**:
```bash
# Clone and setup
git checkout -b 282-viral-vaccine-workflow
git submodule update --init --recursive

# Verify Java version
java -version  # Must show 21.x.x

# Build backend (skip tests for speed)
mvn clean install -DskipTests -Dmaven.test.skip=true

# Install frontend dependencies
cd frontend && npm install && cd ..

# Start development containers
docker compose -f dev.docker-compose.yml up -d
```

✅ **Documentation Read**:
- [x] `specs/282-viral-vaccine-workflow/spec.md` - Feature requirements
- [x] `specs/282-viral-vaccine-workflow/plan.md` - Implementation plan
- [x] `specs/282-viral-vaccine-workflow/research.md` - Technical decisions
- [x] `specs/282-viral-vaccine-workflow/data-model.md` - Entity relationships
- [x] `.specify/guides/testing-roadmap.md` - Testing strategy
- [x] `AGENTS.md` - OpenELIS architecture and conventions

## Development Workflow Overview

This feature follows a **4-milestone** development approach:

1. **M1: Backend Core** (4 days) - Entities, DAOs, Services, Cold Chain + Biosafety
2. **M2: Frontend Core** (4 days, parallel with M1) - Sample reception, QC, Aliquoting UI
3. **M3: Testing Workflows** (5 days) - PCR, ELISA, Viral Culture, Vaccine Testing
4. **M4: Cryostorage & Review** (6 days) - Storage management, Temperature monitoring, Result review

Total: ~15-20 days

## Phase 1: Backend Core (M1)

### Step 1: Create Liquibase Changesets

**Location**: `src/main/resources/liquibase/viral/`

Create changesets for all 16 entity tables:

```bash
cd src/main/resources/liquibase
mkdir -p viral
```

**Example**: `001-viral-sample.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.8.xsd">
    
    <changeSet id="viral-001-create-viral-sample" author="openelis">
        <createTable tableName="viral_sample">
            <column name="id" type="VARCHAR(36)">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="sample_id" type="VARCHAR(36)">
                <constraints nullable="false" foreignKeyName="fk_viral_sample_sample"
                             references="sample(id)" deleteCascade="false"/>
            </column>
            <column name="specimen_type" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="biosafety_level" type="VARCHAR(10)">
                <constraints nullable="false"/>
            </column>
            <column name="cold_chain_status" type="VARCHAR(20)">
                <constraints nullable="false"/>
            </column>
            <column name="collection_site" type="VARCHAR(100)"/>
            <column name="collector_id" type="VARCHAR(36)">
                <constraints foreignKeyName="fk_viral_sample_collector"
                             references="sys_user(id)" deleteCascade="false"/>
            </column>
            <column name="study_protocol_number" type="VARCHAR(50)"/>
            <column name="consent_status" type="VARCHAR(20)"/>
            <column name="fhir_uuid" type="UUID">
                <constraints unique="true"/>
            </column>
            <column name="sys_user_id" type="VARCHAR(36)">
                <constraints nullable="false" foreignKeyName="fk_viral_sample_sys_user"
                             references="sys_user(id)" deleteCascade="false"/>
            </column>
            <column name="lastupdated" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
        </createTable>
        
        <addDefaultValue tableName="viral_sample" columnName="fhir_uuid" 
                         defaultValueComputed="uuid_generate_v4()"/>
        <addDefaultValue tableName="viral_sample" columnName="lastupdated" 
                         defaultValueComputed="NOW()"/>
        
        <createIndex tableName="viral_sample" indexName="idx_viral_sample_sample">
            <column name="sample_id"/>
        </createIndex>
        
        <createIndex tableName="viral_sample" indexName="idx_viral_sample_biosafety">
            <column name="biosafety_level"/>
            <column name="cold_chain_status"/>
        </createIndex>
    </changeSet>
    
    <changeSet id="viral-001-rollback" author="openelis">
        <rollback>
            <dropTable tableName="viral_sample" cascadeConstraints="true"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

**Create all 17 changesets** following this pattern for:
- `002-cold-chain-log.xml`
- `003-biosafety-clearance.xml`
- `004-sample-aliquot.xml`
- `005-pcr-run.xml`
- `006-pcr-result.xml`
- `007-elisa-run.xml`
- `008-elisa-result.xml`
- `009-viral-culture.xml`
- `010-vaccine-lot.xml`
- `011-cryogenic-dewar.xml`
- `012-ultracold-freezer.xml`
- `013-storage-position.xml`
- `014-temperature-log.xml`
- `015-temperature-excursion.xml`
- `016-autoclave-cycle.xml`
- `017-result-review.xml`

**Register changesets** in `src/main/resources/liquibase/master.xml`:

```xml
<include file="liquibase/viral/001-viral-sample.xml"/>
<include file="liquibase/viral/002-cold-chain-log.xml"/>
<!-- ... include all 17 changesets ... -->
```

### Step 2: Create JPA Entity Classes

**Location**: `src/main/java/org/openelisglobal/viral/valueholder/`

**Example**: `ViralSample.java`

```java
package org.openelisglobal.viral.valueholder;

import jakarta.persistence.*;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.hibernate.annotations.GenericGenerator;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "viral_sample")
public class ViralSample extends BaseObject<String> {
    
    @Id
    @GenericGenerator(name = "viral_sample_generator", strategy = "org.openelisglobal.common.util.IdGenerator")
    @GeneratedValue(generator = "viral_sample_generator")
    @Column(name = "id", length = 36)
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "sample_id", referencedColumnName = "id")
    private Sample sample;
    
    @Column(name = "specimen_type", length = 50, nullable = false)
    private String specimenType;
    
    @Column(name = "biosafety_level", length = 10, nullable = false)
    private String biosafety Level;
    
    @Column(name = "cold_chain_status", length = 20, nullable = false)
    private String coldChainStatus;
    
    @Column(name = "collection_site", length = 100)
    private String collectionSite;
    
    @ManyToOne
    @JoinColumn(name = "collector_id", referencedColumnName = "id")
    private SystemUser collector;
    
    @Column(name = "study_protocol_number", length = 50)
    private String studyProtocolNumber;
    
    @Column(name = "consent_status", length = 20)
    private String consentStatus;
    
    @Column(name = "fhir_uuid")
    private UUID fhirUuid;
    
    @ManyToOne
    @JoinColumn(name = "sys_user_id", referencedColumnName = "id")
    private SystemUser sysUser;
    
    @Column(name = "lastupdated", nullable = false)
    private Timestamp lastupdated;
    
    // Relationships
    @OneToMany(mappedBy = "viralSample", cascade = CascadeType.ALL)
    private List<ColdChainLog> coldChainLogs;
    
    @OneToOne(mappedBy = "viralSample", cascade = CascadeType.ALL)
    private BiosafetyClearance biosafetyClearance;
    
    @OneToMany(mappedBy = "parentSample", cascade = CascadeType.ALL)
    private List<SampleAliquot> aliquots;
    
    // Getters and setters (MUST match property names exactly)
    @Override
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Sample getSample() { return sample; }
    public void setSample(Sample sample) { this.sample = sample; }
    
    public String getSpecimenType() { return specimenType; }
    public void setSpecimenType(String specimenType) { this.specimenType = specimenType; }
    
    public String getBiosafety Level() { return biosafety Level; }
    public void setBiosafety Level(String biosafety Level) { this.biosafety Level = biosafety Level; }
    
    public String getColdChainStatus() { return coldChainStatus; }
    public void setColdChainStatus(String coldChainStatus) { this.coldChainStatus = coldChainStatus; }
    
    // ... remaining getters/setters ...
}
```

**Create all 16 entity classes** following this pattern.

### Step 3: Create DAO Layer

**Location**: `src/main/java/org/openelisglobal/viral/dao/`

**Example**: `ViralSampleDAO.java` (Interface)

```java
package org.openelisglobal.viral.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.viral.valueholder.ViralSample;

import java.util.List;

public interface ViralSampleDAO extends BaseDAO<ViralSample, String> {
    List<ViralSample> findByBiosafety Level(String level);
    List<ViralSample> findByColdChainStatus(String status);
    ViralSample findBySampleId(String sampleId);
}
```

**Example**: `ViralSampleDAOImpl.java` (Implementation)

```java
package org.openelisglobal.viral.dao;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.viral.valueholder.ViralSample;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
public class ViralSampleDAOImpl extends BaseDAOImpl<ViralSample, String> implements ViralSampleDAO {
    
    public ViralSampleDAOImpl() {
        super(ViralSample.class);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ViralSample> findByBiosafety Level(String level) {
        String hql = "FROM ViralSample vs WHERE vs.biosafety Level = :level";
        try {
            Query<ViralSample> query = entityManager.unwrap(Session.class).createQuery(hql, ViralSample.class);
            query.setParameter("level", level);
            return query.getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error in ViralSampleDAO findByBiosafety Level", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ViralSample> findByColdChainStatus(String status) {
        String hql = "FROM ViralSample vs WHERE vs.coldChainStatus = :status";
        try {
            Query<ViralSample> query = entityManager.unwrap(Session.class).createQuery(hql, ViralSample.class);
            query.setParameter("status", status);
            return query.getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error in ViralSampleDAO findByColdChainStatus", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public ViralSample findBySampleId(String sampleId) {
        String hql = "FROM ViralSample vs LEFT JOIN FETCH vs.sample WHERE vs.sample.id = :sampleId";
        try {
            Query<ViralSample> query = entityManager.unwrap(Session.class).createQuery(hql, ViralSample.class);
            query.setParameter("sampleId", sampleId);
            return query.uniqueResult();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error in ViralSampleDAO findBySampleId", e);
        }
    }
}
```

**Create all 16 DAO interfaces and implementations**.

### Step 4: Create Service Layer

**Location**: `src/main/java/org/openelisglobal/viral/service/`

**Example**: `ViralSampleService.java` (Interface)

```java
package org.openelisglobal.viral.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.viral.valueholder.ViralSample;

import java.util.List;
import java.util.Map;

public interface ViralSampleService extends BaseObjectService<ViralSample, String> {
    List<ViralSample> getSamplesByBiosafety Level(String level);
    Map<String, Object> getSampleWithColdChain(String id);
    void registerSampleWithColdChain(ViralSample sample, Map<String, Object> coldChainData);
}
```

**Example**: `ViralSampleServiceImpl.java` (Implementation)

```java
package org.openelisglobal.viral.service;

import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.viral.dao.ViralSampleDAO;
import org.openelisglobal.viral.dao.ColdChainLogDAO;
import org.openelisglobal.viral.valueholder.ViralSample;
import org.openelisglobal.viral.valueholder.ColdChainLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ViralSampleServiceImpl extends BaseObjectServiceImpl<ViralSample, String> implements ViralSampleService {
    
    @Autowired
    private ViralSampleDAO viralSampleDAO;
    
    @Autowired
    private ColdChainLogDAO coldChainLogDAO;
    
    public ViralSampleServiceImpl() {
        super(ViralSampleDAO.class);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ViralSample> getSamplesByBiosafety Level(String level) {
        return viralSampleDAO.findByBiosafety Level(level);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSampleWithColdChain(String id) {
        // CRITICAL: Eagerly fetch ALL data within transaction using JOIN FETCH
        String hql = "FROM ViralSample vs " +
                     "LEFT JOIN FETCH vs.coldChainLogs " +
                     "LEFT JOIN FETCH vs.biosafetyClearance " +
                     "LEFT JOIN FETCH vs.sample " +
                     "WHERE vs.id = :id";
        
        ViralSample sample = viralSampleDAO.get(id);
        
        // Compile all data within transaction
        Map<String, Object> result = new HashMap<>();
        result.put("id", sample.getId());
        result.put("specimenType", sample.getSpecimenType());
        result.put("biosafety Level", sample.getBiosafety Level());
        result.put("coldChainStatus", sample.getColdChainStatus());
        result.put("coldChainLogs", sample.getColdChainLogs());
        result.put("biosafetyClearance", sample.getBiosafetyClearance());
        
        return result;  // Return complete data structure
    }
    
    @Override
    @Transactional
    public void registerSampleWithColdChain(ViralSample sample, Map<String, Object> coldChainData) {
        // Save sample
        sample.setLastupdated(new Timestamp(System.currentTimeMillis()));
        viralSampleDAO.save(sample);
        
        // Save cold chain log
        ColdChainLog log = new ColdChainLog();
        log.setViralSample(sample);
        log.setEventType((String) coldChainData.get("eventType"));
        log.setTemperature((Double) coldChainData.get("temperature"));
        log.setEventTimestamp(new Timestamp(System.currentTimeMillis()));
        log.setTransportMethod((String) coldChainData.get("transportMethod"));
        log.setExcursionFlag((Boolean) coldChainData.getOrDefault("excursionFlag", false));
        coldChainLogDAO.save(log);
    }
}
```

**Create all 16 service interfaces and implementations**.

### Step 5: Write Unit Tests

**Location**: `src/test/java/org/openelisglobal/viral/service/`

**Example**: `ViralSampleServiceTest.java`

```java
package org.openelisglobal.viral.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.viral.dao.ViralSampleDAO;
import org.openelisglobal.viral.valueholder.ViralSample;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ViralSampleServiceTest {
    
    @Mock
    private ViralSampleDAO viralSampleDAO;
    
    @InjectMocks
    private ViralSampleServiceImpl viralSampleService;
    
    @Test
    public void testGetSamplesByBiosafety Level_ReturnsSamples() {
        // Arrange
        ViralSample sample1 = new ViralSample();
        sample1.setId("123");
        sample1.setBiosafety Level("BSL-3");
        
        ViralSample sample2 = new ViralSample();
        sample2.setId("456");
        sample2.setBiosafety Level("BSL-3");
        
        when(viralSampleDAO.findByBiosafety Level("BSL-3")).thenReturn(Arrays.asList(sample1, sample2));
        
        // Act
        List<ViralSample> result = viralSampleService.getSamplesByBiosafety Level("BSL-3");
        
        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Should return 2 samples", 2, result.size());
        assertEquals("First sample ID should match", "123", result.get(0).getId());
        assertEquals("Second sample ID should match", "456", result.get(1).getId());
        
        verify(viralSampleDAO, times(1)).findByBiosafety Level("BSL-3");
    }
}
```

**Create unit tests for all services**.

### Step 6: Run Tests and Verify

```bash
# Run all unit tests
mvn test

# Run specific test class
mvn -Dtest=ViralSampleServiceTest test

# Verify ORM mappings (should complete in <5s)
mvn -Dtest=HibernateMappingValidationTest test

# Run integration tests
mvn verify
```

---

## Phase 2: Frontend Core (M2)

### Step 1: Create React Components

**Location**: `frontend/src/components/viral/`

**Example**: `ViralSampleReception.js`

```jsx
import React, { useState } from 'react';
import { useIntl } from 'react-intl';
import { Form, TextInput, Select, SelectItem, Button, Grid, Column } from '@carbon/react';
import { getFromOpenElisServer, postToOpenElisServer } from '../utils/Utils';

const ViralSampleReception = () => {
  const intl = useIntl();
  const [formData, setFormData] = useState({
    sampleId: '',
    specimenType: '',
    biosafety Level: 'BSL-2',
    dispatchTemperature: '',
    arrivalTemperature: '',
    transportMethod: 'DRY_ICE'
  });

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    const payload = {
      sampleId: formData.sampleId,
      specimenType: formData.specimenType,
      biosafety Level: formData.biosafety Level,
      coldChain: {
        dispatchTemperature: parseFloat(formData.dispatchTemperature),
        arrivalTemperature: parseFloat(formData.arrivalTemperature),
        transportMethod: formData.transportMethod,
        excursionFlag: Math.abs(parseFloat(formData.arrivalTemperature) - parseFloat(formData.dispatchTemperature)) > 2
      }
    };
    
    try {
      const response = await postToOpenElisServer('/rest/viral/samples', payload);
      console.log('Sample registered:', response);
      // Show success notification
    } catch (error) {
      console.error('Error registering sample:', error);
      // Show error notification
    }
  };

  return (
    <div>
      <h2>{intl.formatMessage({ id: 'viral.sample.reception.title' })}</h2>
      <Form onSubmit={handleSubmit}>
        <Grid>
          <Column lg={8}>
            <TextInput
              id="sampleId"
              name="sampleId"
              labelText={intl.formatMessage({ id: 'viral.sample.id.label' })}
              value={formData.sampleId}
              onChange={handleInputChange}
              required
            />
          </Column>
          <Column lg={8}>
            <Select
              id="specimenType"
              name="specimenType"
              labelText={intl.formatMessage({ id: 'viral.specimen.type.label' })}
              value={formData.specimenType}
              onChange={handleInputChange}
              required
            >
              <SelectItem value="" text={intl.formatMessage({ id: 'select.placeholder' })} />
              <SelectItem value="NP_SWAB" text={intl.formatMessage({ id: 'viral.specimen.np_swab' })} />
              <SelectItem value="SERUM" text={intl.formatMessage({ id: 'viral.specimen.serum' })} />
              <SelectItem value="VIRAL_CULTURE" text={intl.formatMessage({ id: 'viral.specimen.culture' })} />
              <SelectItem value="VACCINE_VIAL" text={intl.formatMessage({ id: 'viral.specimen.vaccine' })} />
            </Select>
          </Column>
          <Column lg={8}>
            <Select
              id="biosafety Level"
              name="biosafety Level"
              labelText={intl.formatMessage({ id: 'viral.biosafety.level.label' })}
              value={formData.biosafety Level}
              onChange={handleInputChange}
              required
            >
              <SelectItem value="BSL-1" text="BSL-1" />
              <SelectItem value="BSL-2" text="BSL-2" />
              <SelectItem value="BSL-3" text="BSL-3" />
              <SelectItem value="BSL-4" text="BSL-4" />
            </Select>
          </Column>
          <Column lg={8}>
            <TextInput
              id="dispatchTemperature"
              name="dispatchTemperature"
              labelText={intl.formatMessage({ id: 'viral.coldchain.dispatch.temp.label' })}
              placeholder="-80.0"
              value={formData.dispatchTemperature}
              onChange={handleInputChange}
              required
            />
          </Column>
          <Column lg={8}>
            <TextInput
              id="arrivalTemperature"
              name="arrivalTemperature"
              labelText={intl.formatMessage({ id: 'viral.coldchain.arrival.temp.label' })}
              placeholder="-78.5"
              value={formData.arrivalTemperature}
              onChange={handleInputChange}
              required
            />
          </Column>
          <Column lg={8}>
            <Select
              id="transportMethod"
              name="transportMethod"
              labelText={intl.formatMessage({ id: 'viral.coldchain.transport.method.label' })}
              value={formData.transportMethod}
              onChange={handleInputChange}
            >
              <SelectItem value="DRY_ICE" text={intl.formatMessage({ id: 'viral.transport.dry_ice' })} />
              <SelectItem value="REFRIGERATED" text={intl.formatMessage({ id: 'viral.transport.refrigerated' })} />
              <SelectItem value="AMBIENT" text={intl.formatMessage({ id: 'viral.transport.ambient' })} />
            </Select>
          </Column>
          <Column lg={16}>
            <Button type="submit">
              {intl.formatMessage({ id: 'button.submit' })}
            </Button>
          </Column>
        </Grid>
      </Form>
    </div>
  );
};

export default ViralSampleReception;
```

**Create all 13 frontend components**.

### Step 2: Add Internationalization Keys

**Location**: `frontend/src/languages/en.json`

```json
{
  "viral.sample.reception.title": "Viral Sample Reception",
  "viral.sample.id.label": "Sample ID",
  "viral.specimen.type.label": "Specimen Type",
  "viral.specimen.np_swab": "Nasopharyngeal Swab",
  "viral.specimen.serum": "Serum",
  "viral.specimen.culture": "Viral Culture",
  "viral.specimen.vaccine": "Vaccine Vial",
  "viral.biosafety.level.label": "Biosafety Level",
  "viral.coldchain.dispatch.temp.label": "Dispatch Temperature (°C)",
  "viral.coldchain.arrival.temp.label": "Arrival Temperature (°C)",
  "viral.coldchain.transport.method.label": "Transport Method",
  "viral.transport.dry_ice": "Dry Ice",
  "viral.transport.refrigerated": "Refrigerated (2-8°C)",
  "viral.transport.ambient": "Ambient Temperature",
  "button.submit": "Submit"
}
```

**Add ~600 i18n keys for all UI strings**.

### Step 3: Write Jest Tests

**Location**: `frontend/src/components/viral/__tests__/`

**Example**: `ViralSampleReception.test.jsx`

```javascript
import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import { IntlProvider } from 'react-intl';
import { BrowserRouter } from 'react-router-dom';
import ViralSampleReception from '../ViralSampleReception';
import messages from '../../../languages/en.json';
import * as Utils from '../../utils/Utils';

jest.mock('../../utils/Utils', () => ({
  getFromOpenElisServer: jest.fn(),
  postToOpenElisServer: jest.fn(),
}));

const renderWithIntl = (component) => {
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </BrowserRouter>
  );
};

describe('ViralSampleReception', () => {
  test('testSubmitForm_CreatesViralSample', async () => {
    // Arrange
    Utils.postToOpenElisServer.mockResolvedValue({ id: 'uuid-123' });
    renderWithIntl(<ViralSampleReception />);

    // Act
    const sampleIdInput = screen.getByLabelText(/sample id/i);
    await userEvent.type(sampleIdInput, 'VS-2025-001', { delay: 0 });

    const specimenTypeSelect = screen.getByLabelText(/specimen type/i);
    await userEvent.selectOptions(specimenTypeSelect, 'NP_SWAB');

    const dispatchTempInput = screen.getByLabelText(/dispatch temperature/i);
    await userEvent.type(dispatchTempInput, '-80.0', { delay: 0 });

    const arrivalTempInput = screen.getByLabelText(/arrival temperature/i);
    await userEvent.type(arrivalTempInput, '-78.5', { delay: 0 });

    const submitButton = screen.getByRole('button', { name: /submit/i });
    await userEvent.click(submitButton);

    // Assert
    await waitFor(() => {
      expect(Utils.postToOpenElisServer).toHaveBeenCalledWith(
        '/rest/viral/samples',
        expect.objectContaining({
          sampleId: 'VS-2025-001',
          specimenType: 'NP_SWAB',
          coldChain: expect.objectContaining({
            dispatchTemperature: -80.0,
            arrivalTemperature: -78.5
          })
        })
      );
    });
  });
});
```

**Create Jest tests for all components**.

---

## Common Pitfalls & Tips

### ❌ DON'Ts

1. **DON'T** put `@Transactional` on controllers (service layer only)
2. **DON'T** traverse entity relationships in controllers (lazy loading will fail)
3. **DON'T** hardcode strings in JSX (use React Intl)
4. **DON'T** use JUnit 5 annotations (use JUnit 4)
5. **DON'T** skip running `mvn spotless:apply` and `npm run format` before commits
6. **DON'T** run full E2E suite during development (run individual files)

### ✅ DOs

1. **DO** use `JOIN FETCH` in HQL queries to eagerly load related data
2. **DO** compile all data in service layer within transaction
3. **DO** use builders/factories for test data
4. **DO** run ORM validation tests (<5s, no database)
5. **DO** follow TDD: Red → Green → Refactor
6. **DO** use Carbon Design System components exclusively

---

## Next Steps

After completing M1 and M2:

1. **Review code**: Ensure all constitution principles are met
2. **Run full test suite**: `mvn clean install` (with tests enabled)
3. **Create milestone PR**: Branch `feat/282-viral-vaccine-workflow/m1-backend-core`
4. **Request code review**: Minimum 1 approval from core team
5. **Proceed to M3**: Testing workflows (PCR, ELISA, viral culture, vaccine)

---

## Getting Help

- **Slack**: #openelis-dev channel
- **Documentation**: `AGENTS.md`, `.specify/guides/testing-roadmap.md`
- **Examples**: Check `specs/001-medical-lab-workflow/` for reference implementation
- **Issues**: File on GitHub with label `282-viral-vaccine-workflow`

---

**Happy Coding! 🚀**
