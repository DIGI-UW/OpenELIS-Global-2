# Generic Metadata Translation System - Implementation Plan

## Executive Summary

The current OpenELIS-Global-2 system has a **hardcoded bilingual
(English/French) translation system** for metadata. This plan outlines how to
refactor to a **generic, extensible translation system** that can support any
number of languages without schema changes.

---

## Current State Analysis

### Existing `localization` Table Schema

```sql
CREATE TABLE clinlims.localization (
    id numeric NOT NULL,
    description text,          -- Context/purpose of the translation
    english text NOT NULL,     -- HARDCODED column
    french text NOT NULL,      -- HARDCODED column
    lastupdated timestamp with time zone
);
```

**Problem:** Adding a new language (e.g., Spanish, Portuguese) requires:

1. Database schema migration (new column)
2. Java entity changes
3. Hibernate mapping changes
4. Service layer updates
5. All existing Liquibase migrations updated

### Current Java Implementation

**[Localization.java](src/main/java/org/openelisglobal/localization/valueholder/Localization.java)**

- Already uses `Map<Locale, String> localeValues` internally
- Has explicit TODO comments (lines 50-52, 63-65, 70-72, 85-86):
  > "these methods are here until we have time to refactor the database to store
  > the localeValues Map in its own table instead of as columns"

**[LocalizationServiceImpl.java](src/main/java/org/openelisglobal/localization/service/LocalizationServiceImpl.java)**

- Line 146: `getAllActiveLocales()` returns hardcoded
  `Arrays.asList(Locale.ENGLISH, Locale.FRENCH)`

### Tables Using Localization (FK pattern)

| Table            | Localization FK Column(s)                                |
| ---------------- | -------------------------------------------------------- |
| `test`           | `name_localization_id`, `reporting_name_localization_id` |
| `panel`          | `name_localization_id`                                   |
| `type_of_sample` | `name_localization_id`                                   |
| `dictionary`     | `name_localization_id`                                   |
| `method`         | (implied)                                                |
| `test_section`   | (implied)                                                |

---

## Proposed Solution: Normalized Translation Table

### New Database Schema

#### Option A: Separate Translation Values Table (Recommended)

```sql
-- Master localization record (keeps existing ID references intact)
-- NOTE: 'english' and 'french' columns removed after code migration
CREATE TABLE clinlims.localization (
    id numeric NOT NULL PRIMARY KEY,
    description text,
    lastupdated timestamp with time zone
);

-- Normalized translation values (one row per language per entry)
CREATE TABLE clinlims.localization_value (
    id numeric NOT NULL PRIMARY KEY,
    localization_id numeric NOT NULL REFERENCES clinlims.localization(id) ON DELETE CASCADE,
    locale varchar(10) NOT NULL,  -- e.g., 'en', 'fr', 'es', 'pt'
    value text NOT NULL,
    lastupdated timestamp with time zone,
    UNIQUE (localization_id, locale)
);

-- Supported languages configuration
CREATE TABLE clinlims.supported_locale (
    id numeric NOT NULL PRIMARY KEY,
    locale_code varchar(10) NOT NULL UNIQUE,  -- e.g., 'en', 'fr', 'es'
    display_name text NOT NULL,               -- e.g., 'English', 'Français'
    is_active boolean DEFAULT true,
    is_fallback boolean DEFAULT false,        -- 'en' marked as fallback
    sort_order integer DEFAULT 0,
    lastupdated timestamp with time zone
);

-- Indexes
CREATE INDEX idx_localization_value_locale ON clinlims.localization_value(locale);
CREATE INDEX idx_localization_value_localization_id ON clinlims.localization_value(localization_id);
```

**Fallback Strategy:** English (`en`) is marked with `is_fallback = true`. When
a translation is missing for the user's locale, the system returns the English
value.

#### Option B: JSONB Column (Simpler but less queryable)

```sql
CREATE TABLE clinlims.localization (
    id numeric NOT NULL PRIMARY KEY,
    description text,
    translations jsonb NOT NULL,  -- {"en": "Test", "fr": "Essai", "es": "Prueba"}
    lastupdated timestamp with time zone
);
```

**Recommendation:** Option A is preferred because:

- Better query performance for specific locales
- Easier to enforce NOT NULL constraints per language
- Compatible with existing FK relationships
- Allows per-language audit trails
- Easier to generate "missing translations" reports

---

## Migration Strategy

### Phase 1: Schema Migration (Non-Breaking) - DEPLOY FIRST

**Liquibase file:** `001-add-localization-value-table.xml`

This migration is **safe to run with the current Java code** - nothing breaks.

1. Create new tables (`localization_value`, `supported_locale`)
2. Migrate existing data from old columns to new table
3. **Keep `english` and `french` columns** - old code still reads from them

```sql
-- Create sequences
CREATE SEQUENCE IF NOT EXISTS clinlims.localization_value_seq;
CREATE SEQUENCE IF NOT EXISTS clinlims.supported_locale_seq;

-- Create supported_locale table
CREATE TABLE clinlims.supported_locale (
    id numeric NOT NULL PRIMARY KEY,
    locale_code varchar(10) NOT NULL UNIQUE,
    display_name text NOT NULL,
    is_active boolean DEFAULT true,
    is_fallback boolean DEFAULT false,  -- marks 'en' as fallback
    sort_order integer DEFAULT 0,
    lastupdated timestamp with time zone
);

-- Create localization_value table
CREATE TABLE clinlims.localization_value (
    id numeric NOT NULL PRIMARY KEY,
    localization_id numeric NOT NULL REFERENCES clinlims.localization(id) ON DELETE CASCADE,
    locale varchar(10) NOT NULL,
    value text NOT NULL,
    lastupdated timestamp with time zone,
    UNIQUE (localization_id, locale)
);

-- Indexes
CREATE INDEX idx_localization_value_locale ON clinlims.localization_value(locale);
CREATE INDEX idx_localization_value_localization_id ON clinlims.localization_value(localization_id);

-- Populate supported_locale
INSERT INTO clinlims.supported_locale (id, locale_code, display_name, is_active, is_fallback, sort_order, lastupdated)
VALUES
  (nextval('supported_locale_seq'), 'en', 'English', true, true, 1, now()),
  (nextval('supported_locale_seq'), 'fr', 'Français', true, false, 2, now());

-- Migrate existing English translations
INSERT INTO clinlims.localization_value (id, localization_id, locale, value, lastupdated)
SELECT nextval('localization_value_seq'), id, 'en', english, COALESCE(lastupdated, now())
FROM clinlims.localization WHERE english IS NOT NULL AND english != '';

-- Migrate existing French translations
INSERT INTO clinlims.localization_value (id, localization_id, locale, value, lastupdated)
SELECT nextval('localization_value_seq'), id, 'fr', french, COALESCE(lastupdated, now())
FROM clinlims.localization WHERE french IS NOT NULL AND french != '';
```

**After this migration:**

- Old code continues to work (reads `english`/`french` columns)
- New tables exist with copied data
- No downtime required

### Phase 2: Code Refactoring

1. **New Entity: `LocalizationValue.java`**

   ```java
   @Entity
   @Table(name = "localization_value")
   public class LocalizationValue extends BaseObject<String> {
       private String id;
       private Localization localization;
       private String locale;
       private String value;
   }
   ```

2. **Update `Localization.java`**

   ```java
   private static final String FALLBACK_LOCALE = "en";

   @OneToMany(mappedBy = "localization", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
   @MapKey(name = "locale")
   private Map<String, LocalizationValue> values = new HashMap<>();

   public String getLocalizedValue(Locale locale) {
       // Try exact locale match first (e.g., "fr")
       LocalizationValue lv = values.get(locale.getLanguage());
       if (lv != null) {
           return lv.getValue();
       }
       // Fallback to English
       LocalizationValue fallback = values.get(FALLBACK_LOCALE);
       if (fallback != null) {
           return fallback.getValue();
       }
       // Last resort: return any available value
       return values.values().stream()
           .findFirst()
           .map(LocalizationValue::getValue)
           .orElse("");
   }

   // DEPRECATED: Keep temporarily for backwards compatibility during migration
   @Deprecated
   public String getEnglish() {
       return getLocalizedValue(Locale.ENGLISH);
   }

   @Deprecated
   public String getFrench() {
       return getLocalizedValue(Locale.FRENCH);
   }
   ```

3. **New Entity: `SupportedLocale.java`**

   ```java
   @Entity
   @Table(name = "supported_locale")
   public class SupportedLocale extends BaseObject<String> {
       private String id;
       private String localeCode;
       private String displayName;
       private boolean isActive;
       private int sortOrder;
   }
   ```

4. **Update `LocalizationServiceImpl.java`**

   ```java
   @Autowired
   private SupportedLocaleDAO supportedLocaleDAO;

   @Override
   public List<Locale> getAllActiveLocales() {
       return supportedLocaleDAO.getAllActive().stream()
           .map(sl -> Locale.forLanguageTag(sl.getLocaleCode()))
           .collect(Collectors.toList());
   }
   ```

### Phase 3: Hibernate Mapping Updates

Replace
[Localization.hbm.xml](src/main/resources/hibernate/hbm/Localization.hbm.xml):

```xml
<hibernate-mapping package="org.openelisglobal.localization.valueholder">
    <class name="Localization" table="localization">
        <id name="id" .../>
        <version name="lastupdated" .../>
        <property name="description" .../>

        <map name="values" table="localization_value" cascade="all-delete-orphan">
            <key column="localization_id"/>
            <map-key column="locale" type="string"/>
            <one-to-many class="LocalizationValue"/>
        </map>
    </class>

    <class name="LocalizationValue" table="localization_value">
        <id name="id" .../>
        <version name="lastupdated" .../>
        <many-to-one name="localization" column="localization_id"/>
        <property name="locale" column="locale"/>
        <property name="value" column="value"/>
    </class>
</hibernate-mapping>
```

### Phase 4: Deprecate Old Columns - DEPLOY LAST

**Liquibase file:** `099-drop-legacy-localization-columns.xml`

> **CRITICAL:** Only run this migration AFTER:
>
> 1. New Java code is deployed and running in production
> 2. You have verified the application works correctly
> 3. You have a database backup

```sql
-- DANGER: Only run after new code is deployed!
ALTER TABLE clinlims.localization DROP COLUMN english;
ALTER TABLE clinlims.localization DROP COLUMN french;
```

**Also in this phase:**

1. Remove `getEnglish()`, `setEnglish()`, `getFrench()`, `setFrench()` methods
   from `Localization.java`
2. Update Hibernate mapping to remove old column references
3. Update all future Liquibase seed data to use new structure

---

## API Changes

### Current API Response

```json
{
  "test": {
    "localizedTestName": {
      "id": "123",
      "english": "COVID-19 PCR",
      "french": "COVID-19 PCR"
    }
  }
}
```

### New API Response

```json
{
  "test": {
    "localizedTestName": {
      "id": "123",
      "description": "test name",
      "values": {
        "en": "COVID-19 PCR",
        "fr": "COVID-19 PCR",
        "es": "COVID-19 PCR"
      },
      "currentValue": "COVID-19 PCR" // Based on Accept-Language header
    }
  }
}
```

---

## Admin UI for Language Management

Create a new admin page to:

1. **Manage Supported Languages**

   - Add/remove/activate languages
   - Set display order

2. **Bulk Translation Management**

   - Export all translations as CSV/Excel
   - Import translations from CSV/Excel
   - Filter by language, category (test names, panels, etc.)

3. **Missing Translation Report**
   - Show entries missing translations for active languages
   - Allow inline editing

---

## Liquibase Migration Pattern (Going Forward)

### Old Pattern (Hardcoded)

```xml
<insert tableName="localization">
    <column name="english" value="New Test"/>
    <column name="french" value="Nouveau Test"/>
</insert>
```

### New Pattern (Extensible)

```xml
<!-- Create localization entry -->
<insert tableName="localization">
    <column name="id" valueSequenceNext="localization_seq"/>
    <column name="description" value="test name"/>
</insert>

<!-- Add translations -->
<insert tableName="localization_value">
    <column name="localization_id" valueComputed="(SELECT currval('localization_seq'))"/>
    <column name="locale" value="en"/>
    <column name="value" value="New Test"/>
</insert>
<insert tableName="localization_value">
    <column name="localization_id" valueComputed="(SELECT currval('localization_seq'))"/>
    <column name="locale" value="fr"/>
    <column name="value" value="Nouveau Test"/>
</insert>
```

Or use a stored procedure:

```sql
SELECT create_localized_entry('test name', 'en', 'New Test', 'fr', 'Nouveau Test');
```

---

---

## Production Deployment Sequence (CRITICAL)

This table shows the **safe order** for deploying changes to production:

| Step | Deploy                                     | Database State           | Code State | Safe?   |
| ---- | ------------------------------------------ | ------------------------ | ---------- | ------- |
| 1    | `001-add-localization-value-table.xml`     | New tables + old columns | Old code   | **YES** |
| 2    | New Java code (Phases 2-3)                 | New tables + old columns | New code   | **YES** |
| 3    | Verify application works                   | New tables + old columns | New code   | **YES** |
| 4    | `099-drop-legacy-localization-columns.xml` | New tables only          | New code   | **YES** |

### What Breaks If Done Wrong

| Mistake                                      | Result                                                              |
| -------------------------------------------- | ------------------------------------------------------------------- |
| Drop columns before deploying new code       | `SQLGrammarException: column "english" does not exist`              |
| Deploy new code before running migration 001 | `SQLGrammarException: relation "localization_value" does not exist` |
| Skip data migration                          | All translations disappear                                          |

### Rollback Strategy

If issues occur after deploying new code (but before dropping columns):

1. Redeploy old code version
2. Old code reads from `english`/`french` columns (still exist)
3. No data loss

---

## Implementation Phases

### Phase 1: Foundation (Week 1-2)

- [ ] Create Liquibase migration for new tables
- [ ] Create `SupportedLocale` entity and service
- [ ] Create `LocalizationValue` entity
- [ ] Update `LocalizationService` interface

### Phase 2: Migration (Week 2-3)

- [ ] Write data migration script
- [ ] Update Hibernate mappings
- [ ] Update `Localization` entity to use new structure
- [ ] Maintain backwards compatibility with old getters/setters

### Phase 3: Service Updates (Week 3-4)

- [ ] Update `LocalizationServiceImpl`
- [ ] Update all services that create/update localizations
- [ ] Update REST controllers

### Phase 4: Admin UI (Week 4-5)

- [ ] Create Language Management admin page
- [ ] Create Translation Management page
- [ ] Create Missing Translation report

### Phase 5: Cleanup (Week 5-6)

- [ ] Remove deprecated columns
- [ ] Update all seed data in Liquibase
- [ ] Update documentation
- [ ] Add integration tests

---

## Testing Strategy

1. **Unit Tests**

   - `LocalizationServiceTest`: Test fallback logic
   - `SupportedLocaleServiceTest`: Test CRUD operations

2. **Integration Tests**

   - Test migration script with sample data
   - Test API responses with different Accept-Language headers

3. **E2E Tests**
   - Test admin UI for language management
   - Test translation editing workflow

---

## Risks and Mitigations

| Risk                            | Mitigation                                                   |
| ------------------------------- | ------------------------------------------------------------ |
| Breaking existing FK references | Keep `localization.id` unchanged; only add new child table   |
| Performance degradation         | Add proper indexes; use eager fetching for common queries    |
| Data loss during migration      | Run migration in transaction; test on staging first          |
| Backwards compatibility         | Maintain `getEnglish()`/`getFrench()` as deprecated adapters |

---

## Success Criteria

1. Adding a new language requires only:

   - One INSERT into `supported_locale`
   - INSERTs into `localization_value` for new translations
   - No code changes, no schema changes

2. All existing functionality continues to work

3. API response includes translations for all active languages

4. Admin users can manage languages and translations via UI

---

## Appendix: Files to Modify

### Database

- [ ] New:
      `src/main/resources/liquibase/3.5.x.x/001-generic-translation-tables.xml`
- [ ] New:
      `src/main/resources/liquibase/3.5.x.x/002-migrate-localization-data.xml`

### Java Entities

- [ ] Modify:
      `src/main/java/org/openelisglobal/localization/valueholder/Localization.java`
- [ ] New:
      `src/main/java/org/openelisglobal/localization/valueholder/LocalizationValue.java`
- [ ] New:
      `src/main/java/org/openelisglobal/localization/valueholder/SupportedLocale.java`

### DAOs

- [ ] New:
      `src/main/java/org/openelisglobal/localization/dao/LocalizationValueDAO.java`
- [ ] New:
      `src/main/java/org/openelisglobal/localization/dao/SupportedLocaleDAO.java`

### Services

- [ ] Modify:
      `src/main/java/org/openelisglobal/localization/service/LocalizationService.java`
- [ ] Modify:
      `src/main/java/org/openelisglobal/localization/service/LocalizationServiceImpl.java`
- [ ] New:
      `src/main/java/org/openelisglobal/localization/service/SupportedLocaleService.java`

### Hibernate Mappings

- [ ] Modify: `src/main/resources/hibernate/hbm/Localization.hbm.xml`
- [ ] New: `src/main/resources/hibernate/hbm/LocalizationValue.hbm.xml`
- [ ] New: `src/main/resources/hibernate/hbm/SupportedLocale.hbm.xml`

### Frontend

- [ ] New: `frontend/src/components/admin/LanguageManagement/`
- [ ] New: `frontend/src/components/admin/TranslationManagement/`

---

_Document created: 2026-03-03_ _Last updated: 2026-03-03_
