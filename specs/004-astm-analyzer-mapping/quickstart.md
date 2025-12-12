# Quickstart Guide: ASTM Analyzer Field Mapping

**Feature**: 004-astm-analyzer-mapping  
**Date**: 2025-11-14

This guide provides step-by-step instructions for developers to set up and start
implementing the ASTM analyzer field mapping feature.

## Prerequisites

- Java 21 LTS (verify: `java -version`)
- Maven 3.8+
- PostgreSQL 14+ (running in Docker)
- Node.js 16+ (for frontend)
- Docker + Docker Compose

## Step 0: ASTM Communication Infrastructure Setup

### 0.1 ASTM-HTTP Bridge Configuration

The ASTM-HTTP bridge is integrated into the standard development environment and
starts automatically with the dev Docker Compose setup.

**Verify Bridge Configuration**:

```bash
# Check bridge service in docker-compose
grep -A 20 "astm-http-bridge" dev.docker-compose.yml

# Verify bridge configuration file exists
cat volume/astm-bridge/configuration.yml

# Start development environment (includes bridge)
docker compose -f dev.docker-compose.yml up -d

# Verify bridge container is running
docker ps | grep astm-bridge

# Check bridge logs
docker logs openelis-astm-bridge
```

**Bridge Configuration** (`volume/astm-bridge/configuration.yml`):

```yaml
openelis:
  url: https://oe.openelis.org:8443/api/OpenELIS-Global/analyzer/astm
  timeout: 30000
  ssl:
    verify: false # Development only

astm:
  port: 5001
  bind: 0.0.0.0
  timeout: 30000

logging:
  level: DEBUG
```

**Communication Flows**:

1. **Production-Like (via Bridge)**: Analyzer (TCP:5000) → Bridge (TCP:5001) →
   OpenELIS (HTTP POST)
2. **Direct HTTP Push (Testing)**: Mock Server (HTTP POST) → OpenELIS (HTTP
   POST)

**Bridge Access**:

- Bridge IP (Docker network): `172.20.1.101:5001`
- Bridge Port (Host): `5001` (exposed on host)
- OpenELIS Endpoint:
  `https://oe.openelis.org:8443/api/OpenELIS-Global/analyzer/astm`

### 0.2 Analyzer Identification Setup

Analyzers are identified from incoming messages using three strategies (in
order):

1. **ASTM Header Parsing**: Parse H-segment for manufacturer/model → lookup by
   analyzer name
2. **Client IP Address**: Extract IP from HTTP request → lookup by IP address
3. **Plugin Fallback**: Use existing plugin system → lookup by plugin analyzer
   name

**Verify Identification Methods**:

```bash
# Check DAO methods exist
grep -A 5 "findByIpAddress\|findByAnalyzerName" \
  src/main/java/org/openelisglobal/analyzer/dao/AnalyzerConfigurationDAO.java

# Check service methods exist
grep -A 5 "getByIpAddress\|getByAnalyzerName" \
  src/main/java/org/openelisglobal/analyzer/service/AnalyzerConfigurationService.java

# Check identification implementation
grep -A 10 "identifyAnalyzerFromMessage" \
  src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/ASTMAnalyzerReader.java
```

**Testing Identification**:

- Configure analyzer with IP address: `172.20.1.101` (bridge IP) or analyzer's
  actual IP
- Send ASTM message via bridge or direct HTTP POST
- Verify analyzer identified correctly in logs

**Additional Resources**:

- [OpenELIS ASTM Communication Documentation](https://uwdigi.atlassian.net/wiki/external/YTllOWIzZWEzMmQ3NDllOWI4MGJlODc3MTQzYTI1MWI) -
  Comprehensive guide to ASTM analyzer communication workflow, requirements, and
  integration patterns

## Step 1: Database Setup

### 1.1 Run Liquibase Migrations

All database schema changes are managed via Liquibase changesets:

```bash
# Build project (Liquibase runs automatically on startup)
mvn clean install -DskipTests -Dmaven.test.skip=true

# Or run migrations manually (if needed)
# Migrations are in: src/main/resources/liquibase/analyzer/
```

**Verification**:

```bash
# Verify analyzer migrations are included in master changelog
grep -A 2 "analyzer/base.xml" src/main/resources/liquibase/3.3.x.x/base.xml

# After application startup, verify migrations applied
docker exec openelisglobal-database psql -U clinlims -d clinlims -c \
  "SELECT id, filename FROM databasechangelog WHERE filename LIKE '%analyzer%' ORDER BY dateexecuted;"
```

**New Tables Created**:

- `analyzer_configuration` - Analyzer connection settings
- `analyzer_field` - Analyzer fields/codes
- `analyzer_field_mapping` - Field mappings
- `qualitative_result_mapping` - Qualitative value mappings
- `unit_mapping` - Unit mappings
- `analyzer_error` - Error queue

### 1.2 Verify Database Schema

```sql
-- Connect to PostgreSQL
psql -U clinlims -d clinlims

-- Verify tables exist
\dt analyzer_*

-- Check table structure
\d analyzer_field
\d analyzer_field_mapping
```

## Step 2: Backend Implementation

### 2.1 Create Entity Classes

Create JPA entities in `src/main/java/org/openelisglobal/analyzer/valueholder/`:

1. **AnalyzerConfiguration.java** - Extends `Analyzer` with connection settings
2. **AnalyzerField.java** - Analyzer fields/codes
3. **AnalyzerFieldMapping.java** - Field mappings
4. **QualitativeResultMapping.java** - Qualitative value mappings
5. **UnitMapping.java** - Unit mappings
6. **AnalyzerError.java** - Error queue

**Example Entity Structure**:

```java
@Entity
@Table(name = "analyzer_field")
public class AnalyzerField extends BaseObject<String> {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyzer_id", nullable = false)
    private Analyzer analyzer;

    // ... other fields
}
```

See [data-model.md](./data-model.md) for complete entity definitions.

### 2.2 Create DAO Layer

Create DAO interfaces and implementations in
`src/main/java/org/openelisglobal/analyzer/dao/`:

- `AnalyzerFieldDAO.java` / `AnalyzerFieldDAOImpl.java`
- `AnalyzerFieldMappingDAO.java` / `AnalyzerFieldMappingDAOImpl.java`
- etc.

**DAO Pattern**:

```java
@Component
@Transactional
public class AnalyzerFieldDAOImpl extends BaseDAOImpl<AnalyzerField, String>
    implements AnalyzerFieldDAO {

    public AnalyzerFieldDAOImpl() {
        super(AnalyzerField.class);
    }

    // Custom query methods using HQL
}
```

### 2.3 Create Service Layer

Create service interfaces and implementations in
`src/main/java/org/openelisglobal/analyzer/service/`:

- `AnalyzerFieldService.java` / `AnalyzerFieldServiceImpl.java`
- `AnalyzerFieldMappingService.java` / `AnalyzerFieldMappingServiceImpl.java`
- `AnalyzerQueryService.java` / `AnalyzerQueryServiceImpl.java`
- etc.

**Service Pattern**:

```java
@Service
@Transactional
public class AnalyzerFieldMappingServiceImpl implements AnalyzerFieldMappingService {

    @Autowired
    private AnalyzerFieldMappingDAO mappingDAO;

    @Override
    @Transactional
    public AnalyzerFieldMapping saveMapping(AnalyzerFieldMapping mapping) {
        // Validate type compatibility
        // Eagerly fetch related entities
        // Save mapping
        return mappingDAO.insert(mapping);
    }
}
```

**Important**: Services MUST eagerly fetch ALL data needed for responses using
JOIN FETCH (prevents LazyInitializationException).

### 2.4 Create Controller Layer

Create REST controllers in
`src/main/java/org/openelisglobal/analyzer/controller/`:

- `AnalyzerRestController.java` - Analyzer CRUD operations
- `AnalyzerFieldMappingRestController.java` - Mapping operations
- `AnalyzerErrorRestController.java` - Error dashboard operations

**Controller Pattern**:

```java
@RestController
@RequestMapping("/rest/analyzer")
public class AnalyzerRestController extends BaseRestController {

    @Autowired
    private AnalyzerService analyzerService;

    @GetMapping
    public ResponseEntity<?> listAnalyzers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        // Delegate to service (NO @Transactional here!)
        return ResponseEntity.ok(analyzerService.listAnalyzers(page, size));
    }
}
```

**Important**: Controllers MUST NOT have `@Transactional` annotations (belongs
in service layer).

### 2.5 Create Form/DTO Classes

Create form classes in `src/main/java/org/openelisglobal/analyzer/form/`:

- `AnalyzerForm.java`
- `AnalyzerFieldMappingForm.java`
- etc.

## Step 3: Frontend Implementation

### 3.1 Create React Components

Create components in `frontend/src/components/analyzers/`:

1. **AnalyzersList/** - Analyzers list page with DataTable
2. **AnalyzerForm/** - Add/Edit analyzer modal
3. **FieldMapping/** - Dual-panel mapping interface
4. **ErrorDashboard/** - Error dashboard page
5. **TestConnectionModal/** - Connection test modal

### 3.2 Use Carbon Design System

All UI components MUST use Carbon Design System:

```jsx
import {
  DataTable,
  Button,
  ComposedModal,
  Search,
  MultiSelect,
} from "@carbon/react";

function AnalyzersList() {
  return (
    <Grid>
      <Column lg={16}>
        <DataTable
          headers={headers}
          rows={rows}
          // ... Carbon props
        />
      </Column>
    </Grid>
  );
}
```

### 3.3 Internationalization

All user-facing strings MUST use React Intl:

```jsx
import { useIntl } from "react-intl";

function AnalyzerForm() {
  const intl = useIntl();

  return <Button>{intl.formatMessage({ id: "button.save" })}</Button>;
}
```

**Translation Files**: `frontend/src/languages/{locale}.json`

### 3.4 API Integration

Use existing OpenELIS API utilities:

```jsx
import { getFromOpenElisServer, postToOpenElisServer } from "../utils/Utils";

// Fetch analyzers
const analyzers = await getFromOpenElisServer("/rest/analyzer");

// Create analyzer
await postToOpenElisServer("/rest/analyzer", analyzerData);
```

### 3.5 Configure Navigation Menu (Backend-Driven via Liquibase)

The left-hand navigation is populated from the `clinlims.menu` table via
`/rest/menu`. Menu items are created automatically via Liquibase changeset
`004-009-add-menu-items.xml` when migrations run. The changeset creates:

- Parent "Analyzers" node (element_id: `menu_analyzers`, presentation_order: 26)
- Child routes:
  1. Analyzers Dashboard (`menu_analyzers_list`, `/analyzers`)
  2. Error Dashboard (`menu_analyzers_errors`, `/analyzers/errors`)
  3. Field Mappings (`menu_analyzers_field_mappings`, `/analyzers/:id/mappings`)
  4. Quality Control placeholders (to be added in feature 003-westgard-qc)

**Verification**: After application startup with migrations applied, verify menu
items exist:

```bash
docker exec openelisglobal-database psql -U clinlims -d clinlims -c \
  "SELECT element_id, display_key, action_url FROM menu WHERE element_id LIKE 'menu_analyzers%';"
```

**Note**: For manual testing or development, SQL inserts can be used as an
alternative, but Liquibase is the production method. See
`004-009-add-menu-items.xml` for the canonical menu structure.

## Step 4: Testing

### 4.1 Unit Tests

Create unit tests in `src/test/java/org/openelisglobal/analyzer/service/`:

```java
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerFieldMappingServiceTest {
    @Mock
    private AnalyzerFieldMappingDAO mappingDAO;

    @InjectMocks
    private AnalyzerFieldMappingServiceImpl mappingService;

    @Test
    public void testSaveMapping_ValidMapping_SavesSuccessfully() {
        // Test implementation
    }
}
```

**Remember**: Use JUnit 4 (`org.junit.Test`), NOT JUnit 5.

### 4.2 ORM Validation Tests

Create ORM validation test in
`src/test/java/org/openelisglobal/analyzer/valueholder/`:

```java
@Test
public void testHibernateMappingsLoadSuccessfully() {
    Configuration config = new Configuration();
    config.addAnnotatedClass(AnalyzerField.class);
    config.addAnnotatedClass(AnalyzerFieldMapping.class);
    // ... add all entities
    config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

    SessionFactory sf = config.buildSessionFactory();
    assertNotNull("All mappings should load", sf);
    sf.close();
}
```

### 4.3 Integration Tests

Create integration tests in
`src/test/java/org/openelisglobal/analyzer/controller/`:

```java
@RunWith(SpringRunner.class)
@SpringBootTest
public class AnalyzerRestControllerIntegrationTest {
    @Autowired
    private AnalyzerService analyzerService;

    @Test
    public void testListAnalyzers_ReturnsPaginatedResults() {
        // Test implementation
    }
}
```

### 4.4 E2E Tests

Create Cypress tests in `frontend/cypress/e2e/`:

```javascript
describe("User Story P1: Configure Field Mappings", () => {
  it("should map analyzer test code to OpenELIS test", () => {
    cy.visit("/analyzers");
    cy.login("admin", "password");
    // ... test steps
  });
});
```

**Remember**: Run tests individually during development (not full suite).

## Step 5: Code Formatting

Before committing, format code:

```bash
# Backend
mvn spotless:apply

# Frontend
cd frontend && npm run format && cd ..
```

## Step 6: Build and Run

### 6.1 Build Backend

```bash
mvn clean install -DskipTests -Dmaven.test.skip=true
```

### 6.2 Start Development Environment

```bash
docker compose -f dev.docker-compose.yml up -d
```

### 6.3 Access Application

- React UI: https://localhost/
- Legacy UI: https://localhost/api/OpenELIS-Global/
- FHIR Server: https://fhir.openelis.org:8443/fhir/

## Common Issues and Solutions

### Issue: LazyInitializationException

**Solution**: Services must eagerly fetch all data using JOIN FETCH:

```java
String hql = "SELECT m FROM AnalyzerFieldMapping m " +
             "LEFT JOIN FETCH m.analyzerField " +
             "LEFT JOIN FETCH m.analyzerField.analyzer " +
             "WHERE m.analyzerField.analyzer.id = :analyzerId";
```

### Issue: Build fails with Java version error

**Solution**: Verify Java 21 is installed:

```bash
java -version  # Must show "openjdk version 21.x.x"
sdk env        # Use SDKMAN for automatic switching
```

### Issue: Tests fail with JUnit import errors

**Solution**: Use JUnit 4 imports:

```java
import org.junit.Test;  // NOT org.junit.jupiter.api.Test
import org.junit.Assert.*;  // NOT org.junit.jupiter.api.Assertions.*
```

## Next Steps

1. Review [data-model.md](./data-model.md) for entity definitions
2. Review [contracts/api-contracts.md](./contracts/api-contracts.md) for API
   specifications
3. Review [research.md](./research.md) for technical decisions
4. Follow TDD workflow: Write tests first, then implement

## References

- [Constitution](../.specify/memory/constitution.md) - Core principles
- [AGENTS.md](../../AGENTS.md) - Project context
- [plan.md](./plan.md) - Implementation plan
- [spec.md](./spec.md) - Feature specification
