# Data Model: OGC-285 Barcode Labels v2

**Source of truth:** FRS v2.5 §7 ([pinned at SHA `7cf6f65`](https://github.com/DIGI-UW/openelis-work/blob/7cf6f65cae9a9794e52f3dd4c5e759c920d87bf5/designs/admin-config/barcode-labels.md#7-data-model)).
**Code-truth reference entities:** [`SampleBarcodeInfo.java`](../../src/main/java/org/openelisglobal/barcode/valueholder/SampleBarcodeInfo.java), [`SampleItemBarcodeInfo.java`](../../src/main/java/org/openelisglobal/barcode/valueholder/SampleItemBarcodeInfo.java) (the canonical OGC-284 pattern for new JPA-annotated entities). [`BaseObject<PK>`](../../src/main/java/org/openelisglobal/common/valueholder/BaseObject.java) is the `@MappedSuperclass` parent.
**Schema namespace:** `clinlims`.
**Liquibase changeset paths:** `src/main/resources/liquibase/3.3.x.x/029-label-preset-tables.xml`, `030-seed-system-presets.xml`, `031-seed-system-preset-fields.xml`.

## 1. Entity-relationship overview

```
                              ┌─────────────────────────┐
                              │       test (existing,   │
                              │       String id legacy) │
                              └────────────┬────────────┘
                                           │ 1
                                           │ *
                              ┌────────────▼────────────┐         ┌──────────────────────────┐
                              │  test_label_preset_link │         │     test_label_config    │
                              │  (CREATED by M2;        │         │  (new, Integer id;       │
                              │   Integer id, FK to     │         │   PK = test_id which is  │
                              │   test(numeric(10,0)))  │         │   numeric(10,0))         │
                              └────────────┬────────────┘         └────────────┬─────────────┘
                                           │ *                                 │ 1
                                           │ 1                                 │ ↑
                              ┌────────────▼────────────┐                      │
                              │       label_preset      │◀─────────────────────┘
                              │  (new, Integer id)      │
                              └────────────┬────────────┘
                                           │ 1
                                           │ *
                              ┌────────────▼────────────┐
                              │   label_preset_field    │
                              │  (new, Integer id; FK to│
                              │   label_preset.id)      │
                              └─────────────────────────┘

                              ┌─────────────────────────┐
                              │      sample (existing,  │
                              │      String id legacy)  │
                              └────────────┬────────────┘
                                           │ 1
                                           │ *
                              ┌────────────▼────────────┐         ┌──────────────────────────┐
                              │   order_label_request   │────────▶│      label_preset        │
                              │  (new, Integer id;      │   *:1   │  (snapshot reference)    │
                              │   parent_sample_id FK   │         └──────────────────────────┘
                              │   to sample(numeric))   │
                              └─────────────────────────┘
                                          │
                                          │  preset_snapshot JSONB column
                                          │  (frozen full preset config at save time)
                                          ▼
```

## OE legacy-vs-new ID convention (load-bearing)

**Two patterns coexist in OpenELIS Global 2:**

1. **Legacy entities** (pre-Jakarta migration): `String id` in Java; DB column type `numeric(10,0)`. Mapped via XML `.hbm.xml` or with `@Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")`. Example: `Sample`, `SampleItem`, `Test`.
2. **New JPA-annotated entities** (post-Jakarta, post-OGC-284): `Integer id` in Java; DB column type `INTEGER`; sequence-driven. Mapped via `jakarta.persistence.*` annotations. NO custom UserType needed because the type matches end-to-end. Example: `SampleBarcodeInfo`, `SampleItemBarcodeInfo` (the canonical new-pattern templates).

**All OGC-285 entities follow pattern 2** — `BaseObject<Integer>` + `Integer id` + `INTEGER` DB column + `@SequenceGenerator`. For FK references to legacy entities (e.g., the `sample` table), declare a JPA `@ManyToOne` relationship to the legacy entity; Hibernate routes through the legacy entity's `LIMSStringNumberUserType` automatically. We do not introduce raw `numeric(10,0)` columns in our Java code; we declare typed JPA relationships and let Hibernate bridge.

## 2. New tables

### 2.1 `label_preset` (FRS §7.1)

Liquibase changeset (matches OGC-284 `028-barcode-info-tables.xml` idiom):

```xml
<changeSet id="label-preset-001-label-preset" author="ogc-285">
    <preConditions onFail="MARK_RAN">
        <not><tableExists tableName="label_preset" schemaName="clinlims"/></not>
    </preConditions>
    <comment>Create label_preset table and sequence for configurable label preset config</comment>

    <createSequence schemaName="clinlims" sequenceName="label_preset_seq" startValue="1" incrementBy="1"/>

    <createTable tableName="label_preset" schemaName="clinlims">
        <column name="id" type="INTEGER" defaultValueSequenceNext="label_preset_seq">
            <constraints primaryKey="true" nullable="false"/>
        </column>
        <column name="name"               type="VARCHAR(120)"><constraints nullable="false" unique="true" uniqueConstraintName="label_preset_name_uniq"/></column>
        <column name="height_mm"          type="INTEGER">    <constraints nullable="false"/></column>
        <column name="width_mm"           type="INTEGER">    <constraints nullable="false"/></column>
        <column name="barcode_type"       type="VARCHAR(20)"><constraints nullable="false"/></column>
        <column name="prints_per_order"   type="BOOLEAN"     defaultValueBoolean="false"><constraints nullable="false"/></column>
        <column name="prints_per_sample"  type="BOOLEAN"     defaultValueBoolean="true"> <constraints nullable="false"/></column>
        <column name="default_per_order"  type="INTEGER"     defaultValueNumeric="0">    <constraints nullable="false"/></column>
        <column name="max_per_order"      type="INTEGER"     defaultValueNumeric="10">   <constraints nullable="false"/></column>
        <column name="default_per_sample" type="INTEGER"     defaultValueNumeric="0">    <constraints nullable="false"/></column>
        <column name="max_per_sample"     type="INTEGER"     defaultValueNumeric="10">   <constraints nullable="false"/></column>
        <column name="is_system"          type="BOOLEAN"     defaultValueBoolean="false"><constraints nullable="false"/></column>
        <column name="is_active"          type="BOOLEAN"     defaultValueBoolean="true"> <constraints nullable="false"/></column>
        <column name="last_updated"       type="TIMESTAMP"/>
    </createTable>

    <!-- Table-level CHECK constraints (cross-column rules) -->
    <sql>ALTER TABLE clinlims.label_preset
         ADD CONSTRAINT label_preset_height_range  CHECK (height_mm BETWEEN 5 AND 200),
         ADD CONSTRAINT label_preset_width_range   CHECK (width_mm  BETWEEN 5 AND 200),
         ADD CONSTRAINT label_preset_barcode_type  CHECK (barcode_type IN ('CODE_128','QR','DATAMATRIX')),
         ADD CONSTRAINT label_preset_default_nonneg
             CHECK (default_per_order >= 0 AND default_per_sample >= 0),
         ADD CONSTRAINT label_preset_max_gte_default
             CHECK (max_per_order >= default_per_order AND max_per_sample >= default_per_sample),
         ADD CONSTRAINT label_preset_scope_required
             CHECK (prints_per_order OR prints_per_sample);</sql>

    <createIndex indexName="idx_label_preset_active" tableName="label_preset" schemaName="clinlims">
        <column name="is_active"/>
    </createIndex>
    <createIndex indexName="idx_label_preset_system" tableName="label_preset" schemaName="clinlims">
        <column name="is_system"/>
    </createIndex>

    <rollback>
        <dropIndex indexName="idx_label_preset_active" tableName="label_preset" schemaName="clinlims"/>
        <dropIndex indexName="idx_label_preset_system" tableName="label_preset" schemaName="clinlims"/>
        <dropTable tableName="label_preset" schemaName="clinlims"/>
        <dropSequence sequenceName="label_preset_seq" schemaName="clinlims"/>
    </rollback>
</changeSet>
```

**Notes:**

- **Sequence** uses the OE convention (`<createSequence>` + `defaultValueSequenceNext`). Matches `028-barcode-info-tables.xml` style; do not use PostgreSQL `BIGSERIAL` shorthand.
- **Audit column** `last_updated TIMESTAMP` (no TZ; matches OGC-284 idiom). Mapped via inherited `@Version` from `BaseObject`. Hibernate updates automatically on entity mutation.
- **CHECK constraints** are table-level (added via raw `<sql>` ALTER TABLE because Liquibase's native column-level CHECK syntax doesn't support cross-column references). Service-layer pre-validation gives nice error messages; CHECK is defense-in-depth.
- **Name uniqueness** is exact-match at DDL; service layer enforces case-insensitive + whitespace-trim normalization (per `/speckit.clarify` Q3).

**Seed data (5 system presets, separate changeset):** see §2.5 below.

### 2.2 `label_preset_field` (FRS §7.1)

```xml
<changeSet id="label-preset-002-label-preset-field" author="ogc-285">
    <preConditions onFail="MARK_RAN">
        <not><tableExists tableName="label_preset_field" schemaName="clinlims"/></not>
    </preConditions>

    <createSequence schemaName="clinlims" sequenceName="label_preset_field_seq" startValue="1" incrementBy="1"/>

    <createTable tableName="label_preset_field" schemaName="clinlims">
        <column name="id" type="INTEGER" defaultValueSequenceNext="label_preset_field_seq">
            <constraints primaryKey="true" nullable="false"/>
        </column>
        <column name="preset_id"     type="INTEGER">
            <constraints nullable="false"
                foreignKeyName="fk_label_preset_field_preset"
                references="label_preset(id)"
                deleteCascade="true"/>
        </column>
        <column name="field_key"     type="VARCHAR(60)"><constraints nullable="false"/></column>
        <column name="source_type"   type="VARCHAR(20)" defaultValue="SYSTEM"><constraints nullable="false"/></column>
        <column name="is_required"   type="BOOLEAN"     defaultValueBoolean="false"><constraints nullable="false"/></column>
        <column name="display_order" type="INTEGER"><constraints nullable="false"/></column>
        <column name="last_updated"  type="TIMESTAMP"/>
    </createTable>

    <addUniqueConstraint constraintName="label_preset_field_order_uniq" tableName="label_preset_field" schemaName="clinlims"
        columnNames="preset_id, display_order"/>
    <addUniqueConstraint constraintName="label_preset_field_key_uniq"   tableName="label_preset_field" schemaName="clinlims"
        columnNames="preset_id, field_key"/>

    <sql>ALTER TABLE clinlims.label_preset_field
         ADD CONSTRAINT label_preset_field_source_type CHECK (source_type = 'SYSTEM');</sql>

    <createIndex indexName="idx_label_preset_field_preset" tableName="label_preset_field" schemaName="clinlims">
        <column name="preset_id"/>
    </createIndex>

    <rollback>
        <dropIndex indexName="idx_label_preset_field_preset" tableName="label_preset_field" schemaName="clinlims"/>
        <dropTable tableName="label_preset_field" schemaName="clinlims"/>
        <dropSequence sequenceName="label_preset_field_seq" schemaName="clinlims"/>
    </rollback>
</changeSet>
```

**`source_type`** constrained to `'SYSTEM'` in v2. Column exists for v3+ extension (see [research.md §2 Q4](./research.md)).

**`field_key`** values for v2 — 15 selectable + `LAB_NUMBER` locked at position 1:

| `field_key` | UI label | Source |
|---|---|---|
| `LAB_NUMBER` | Lab Number | Always required, locked at position 1 (FRS §2.4). |
| `PATIENT_NAME` | Patient Name | patient master |
| `PATIENT_ID` | Patient ID | patient master |
| `PATIENT_DOB` | Patient Date of Birth | patient master |
| `PATIENT_SEX` | Patient Sex | patient master |
| `SITE_ID` | Site ID | site config |
| `COLLECTION_DATETIME` | Collection Date and Time | sample |
| `COLLECTED_BY` | Collected By | sample |
| `TESTS` | Tests | test list on the order |
| `SPECIMEN_TYPE` | Specimen Type | sample type |
| `BLOCK_ID` | Block ID | pathology block record |
| `SLIDE_ID` | Slide ID | pathology slide record |
| `STAIN_TYPE` | Stain Type | pathology slide record |
| `CASE_NUMBER` | Case Number | pathology case |
| `STORAGE_LOCATION` | Storage Location | storage record |
| `EXPIRY_DATE` | Expiry Date | storage record |

(Lab Number = 1 locked + 15 selectable = 16 total fields.)

### 2.3 `test_label_config` (FRS §7.2 — new table)

Note: the PK is `test_id` (a FK to the existing legacy `test` table, which uses `numeric(10,0)` for its id column). Therefore the PK column type is `numeric(10,0)`, NOT `INTEGER`. This is the one place where the new schema directly inherits a legacy ID type.

```xml
<changeSet id="label-preset-003-test-label-config" author="ogc-285">
    <preConditions onFail="MARK_RAN">
        <not><tableExists tableName="test_label_config" schemaName="clinlims"/></not>
    </preConditions>

    <createTable tableName="test_label_config" schemaName="clinlims">
        <column name="test_id" type="numeric(10,0)">
            <constraints primaryKey="true" nullable="false"
                foreignKeyName="fk_test_label_config_test"
                references="test(id)"
                deleteCascade="true"/>
        </column>
        <column name="allow_order_entry_override" type="BOOLEAN" defaultValueBoolean="true"><constraints nullable="false"/></column>
        <column name="last_updated" type="TIMESTAMP"/>
    </createTable>

    <rollback>
        <dropTable tableName="test_label_config" schemaName="clinlims"/>
    </rollback>
</changeSet>
```

One row per test (lazy-created on first save of the test's Labels tab). Master toggle (FR-017 / AC-12).

### 2.4 `order_label_request` (FRS §7.3 — new table; snapshot store)

```xml
<changeSet id="label-preset-005-order-label-request" author="ogc-285">
    <preConditions onFail="MARK_RAN">
        <not><tableExists tableName="order_label_request" schemaName="clinlims"/></not>
    </preConditions>

    <createSequence schemaName="clinlims" sequenceName="order_label_request_seq" startValue="1" incrementBy="1"/>

    <createTable tableName="order_label_request" schemaName="clinlims">
        <column name="id" type="INTEGER" defaultValueSequenceNext="order_label_request_seq">
            <constraints primaryKey="true" nullable="false"/>
        </column>
        <column name="parent_sample_id" type="numeric(10,0)">
            <constraints nullable="false"
                foreignKeyName="fk_order_label_request_parent_sample"
                references="sample(id)"/>
        </column>
        <column name="sample_item_id" type="numeric(10,0)">
            <constraints nullable="true"
                foreignKeyName="fk_order_label_request_sample_item"
                references="sample_item(id)"/>
        </column>
        <column name="preset_id" type="INTEGER">
            <constraints nullable="false"
                foreignKeyName="fk_order_label_request_preset"
                references="label_preset(id)"/>
        </column>
        <column name="qty" type="INTEGER"><constraints nullable="false"/></column>
        <column name="preset_snapshot" type="JSONB"><constraints nullable="false"/></column>
        <column name="last_updated" type="TIMESTAMP"/>
    </createTable>

    <sql>ALTER TABLE clinlims.order_label_request
         ADD CONSTRAINT order_label_request_qty_nonneg CHECK (qty >= 0);</sql>

    <createIndex indexName="idx_order_label_request_parent_sample" tableName="order_label_request" schemaName="clinlims">
        <column name="parent_sample_id"/>
    </createIndex>
    <createIndex indexName="idx_order_label_request_sample_item" tableName="order_label_request" schemaName="clinlims">
        <column name="sample_item_id"/>
    </createIndex>
    <createIndex indexName="idx_order_label_request_preset" tableName="order_label_request" schemaName="clinlims">
        <column name="preset_id"/>
    </createIndex>

    <rollback>
        <dropIndex indexName="idx_order_label_request_parent_sample" tableName="order_label_request" schemaName="clinlims"/>
        <dropIndex indexName="idx_order_label_request_sample_item"  tableName="order_label_request" schemaName="clinlims"/>
        <dropIndex indexName="idx_order_label_request_preset"       tableName="order_label_request" schemaName="clinlims"/>
        <dropTable tableName="order_label_request" schemaName="clinlims"/>
        <dropSequence sequenceName="order_label_request_seq" schemaName="clinlims"/>
    </rollback>
</changeSet>
```

**Snapshot semantics:**

- One row per `(parent_sample, sample_item, preset)` for per-sample presets.
- One row per `(parent_sample, preset)` with `sample_item_id IS NULL` for per-order presets.
- `preset_snapshot` JSONB is the **authoritative source for reprint** (AC-20). Reprint MUST NOT re-fetch `label_preset` or `test_label_preset_link`.
- Naming: column is `parent_sample_id` (not `order_id`) because OpenELIS has no separate "order" table — the order concept is rooted in the `sample` parent identity. Per-sample-item rows additionally reference `sample_item.id`.

**Decrease-only post-save semantics** (per `/speckit.clarify` Q1): when the post-save dialog Print action fires, `qty` is overwritten to the (possibly lower) value the technician entered. The snapshot retains the original saved qty for audit (via the `test_link.default_qty` field or the implicit `preset_default_per_*`).

### 2.5 Seed data — split into two changesets

**`030-seed-system-presets.xml`** — inserts 5 `label_preset` rows from `site_information.barcode.*` keys per FRS §2.7. Uses raw `<sql>` blocks because we read multiple `site_information` rows + apply canonical-fallback (`1` / `10`) for malformed values:

```xml
<changeSet id="label-preset-seed-001-system-presets" author="ogc-285">
    <preConditions onFail="MARK_RAN">
        <sqlCheck expectedResult="0">
            SELECT COUNT(*) FROM clinlims.label_preset WHERE is_system = true
        </sqlCheck>
    </preConditions>

    <comment>Seed 5 system label presets from legacy site_information.barcode.* keys per FRS §2.7</comment>

    <sql>
        WITH si AS (
            SELECT name, value FROM clinlims.site_information
            WHERE name LIKE 'barcode.order.%' OR name LIKE 'barcode.specimen.%'
               OR name LIKE 'barcode.block.%' OR name LIKE 'barcode.slide.%'
               OR name LIKE 'barcode.freezer.%'
        ),
        normalized AS (
            SELECT
                CASE WHEN value ~ '^[0-9]+$' THEN value::INTEGER ELSE NULL END AS num,
                name
            FROM si
        )
        INSERT INTO clinlims.label_preset
            (id, name, height_mm, width_mm, barcode_type,
             prints_per_order, prints_per_sample,
             default_per_order, max_per_order,
             default_per_sample, max_per_sample,
             is_system, is_active, last_updated)
        SELECT
            nextval('clinlims.label_preset_seq'),
            preset_name,
            COALESCE((SELECT num FROM normalized WHERE name = 'barcode.' || legacy_key || '.height'), 25)  AS height_mm,
            COALESCE((SELECT num FROM normalized WHERE name = 'barcode.' || legacy_key || '.width'),  76)  AS width_mm,
            'CODE_128',
            (legacy_key = 'order')      AS prints_per_order,
            (legacy_key &lt;&gt; 'order') AS prints_per_sample,
            CASE WHEN legacy_key = 'order'      THEN COALESCE((SELECT num FROM normalized WHERE name = 'barcode.order.default'), 1) ELSE 0 END,
            CASE WHEN legacy_key = 'order'      THEN COALESCE((SELECT num FROM normalized WHERE name = 'barcode.order.max'),    10) ELSE 10 END,
            CASE WHEN legacy_key &lt;&gt; 'order' THEN COALESCE((SELECT num FROM normalized WHERE name = 'barcode.' || legacy_key || '.default'), 1) ELSE 0 END,
            CASE WHEN legacy_key &lt;&gt; 'order' THEN COALESCE((SELECT num FROM normalized WHERE name = 'barcode.' || legacy_key || '.max'),    10) ELSE 10 END,
            true,
            true,
            CURRENT_TIMESTAMP
        FROM (VALUES
            ('order',    'Order Label'),
            ('specimen', 'Specimen Label'),
            ('block',    'Block Label'),
            ('slide',    'Slide Label'),
            ('freezer',  'Freezer Label')
        ) AS seed(legacy_key, preset_name);
    </sql>

    <rollback>
        <sql>DELETE FROM clinlims.label_preset WHERE is_system = true;</sql>
    </rollback>
</changeSet>
```

**`031-seed-system-preset-fields.xml`** — inserts `label_preset_field` rows for each seeded preset, looking up presets by NAME (not by PK; the PK was generated by the previous changeset's INSERT and isn't stable across migrations):

```xml
<changeSet id="label-preset-seed-002-system-preset-fields" author="ogc-285">
    <preConditions onFail="MARK_RAN">
        <sqlCheck expectedResult="0">
            SELECT COUNT(*) FROM clinlims.label_preset_field
            WHERE preset_id IN (SELECT id FROM clinlims.label_preset WHERE is_system = true)
        </sqlCheck>
    </preConditions>

    <comment>Seed LAB_NUMBER (required, position 1) + v1 carry-over fields for each system preset</comment>

    <!-- Every system preset gets LAB_NUMBER at position 1 -->
    <sql>
        INSERT INTO clinlims.label_preset_field (id, preset_id, field_key, source_type, is_required, display_order, last_updated)
        SELECT nextval('clinlims.label_preset_field_seq'),
               id, 'LAB_NUMBER', 'SYSTEM', true, 1, CURRENT_TIMESTAMP
        FROM clinlims.label_preset
        WHERE is_system = true;
    </sql>

    <!-- v1 carry-over optional fields per type (Patient Name, Patient Sex, Patient DOB, etc.)
         driven by legacy site_information toggles. M2 engineer fills in this block based on
         the actual v1 BarcodeConfiguration.jsx checkbox list. -->

    <rollback>
        <sql>
            DELETE FROM clinlims.label_preset_field
            WHERE preset_id IN (SELECT id FROM clinlims.label_preset WHERE is_system = true);
        </sql>
    </rollback>
</changeSet>
```

The split avoids the FK-chicken-and-egg problem: `031` references `label_preset.id` rows that `030` just inserted, looked up by `name` (which IS stable).

## 3. Modified / contingent tables

### 3.1 `test_label_preset_link` (was OGC-761; CREATED by M2 since OGC-761 absent on develop)

Per [research.md §5](./research.md), OGC-761 has NOT landed on develop. M2 CREATEs the full table:

```xml
<changeSet id="label-preset-004-test-label-preset-link" author="ogc-285">
    <preConditions onFail="MARK_RAN">
        <not><tableExists tableName="test_label_preset_link" schemaName="clinlims"/></not>
    </preConditions>

    <createSequence schemaName="clinlims" sequenceName="test_label_preset_link_seq" startValue="1" incrementBy="1"/>

    <createTable tableName="test_label_preset_link" schemaName="clinlims">
        <column name="id" type="INTEGER" defaultValueSequenceNext="test_label_preset_link_seq">
            <constraints primaryKey="true" nullable="false"/>
        </column>
        <column name="test_id" type="numeric(10,0)">
            <constraints nullable="false"
                foreignKeyName="fk_test_label_preset_link_test"
                references="test(id)"
                deleteCascade="true"/>
        </column>
        <column name="preset_id" type="INTEGER">
            <constraints nullable="false"
                foreignKeyName="fk_test_label_preset_link_preset"
                references="label_preset(id)"/>
        </column>
        <column name="default_qty"    type="INTEGER">                          <constraints nullable="false"/></column>
        <column name="max_qty"        type="INTEGER">                          <constraints nullable="false"/></column>
        <column name="allow_override" type="BOOLEAN" defaultValueBoolean="true"><constraints nullable="false"/></column>
        <column name="last_updated"   type="TIMESTAMP"/>
    </createTable>

    <addUniqueConstraint constraintName="test_label_preset_link_uniq" tableName="test_label_preset_link" schemaName="clinlims"
        columnNames="test_id, preset_id"/>

    <sql>ALTER TABLE clinlims.test_label_preset_link
         ADD CONSTRAINT test_label_preset_link_qty_nonneg CHECK (default_qty >= 0),
         ADD CONSTRAINT test_label_preset_link_max_gte_default CHECK (max_qty >= default_qty);</sql>

    <createIndex indexName="idx_test_label_preset_link_test"   tableName="test_label_preset_link" schemaName="clinlims">
        <column name="test_id"/>
    </createIndex>
    <createIndex indexName="idx_test_label_preset_link_preset" tableName="test_label_preset_link" schemaName="clinlims">
        <column name="preset_id"/>
    </createIndex>

    <rollback>
        <dropIndex indexName="idx_test_label_preset_link_test"   tableName="test_label_preset_link" schemaName="clinlims"/>
        <dropIndex indexName="idx_test_label_preset_link_preset" tableName="test_label_preset_link" schemaName="clinlims"/>
        <dropTable tableName="test_label_preset_link" schemaName="clinlims"/>
        <dropSequence sequenceName="test_label_preset_link_seq" schemaName="clinlims"/>
    </rollback>
</changeSet>
```

**Per-sample-only constraint** (FRS §3.5): rows MUST reference presets where `prints_per_sample = true`. **There is no DDL CHECK constraint for this** — PostgreSQL CHECK cannot reference other tables. Enforcement is **service-layer only**:

```java
// In TestLabelPresetLinkServiceImpl.assertPerSamplePreset(presetId):
LabelPreset preset = labelPresetDAO.getById(presetId);
if (!Boolean.TRUE.equals(preset.getPrintsPerSample())) {
    throw new BusinessException("Cannot link order-only preset to a test");
}
```

If a future audit shows service-layer bypass risk, a PostgreSQL TRIGGER could be added (triggers CAN reference other tables; CHECK cannot). Out of scope for v2.

**If OGC-761 lands BEFORE M2 ships:** the precondition skips the CREATE; a follow-up `ALTER TABLE` adds the `allow_override` column. The two-path contingency is documented inline in the M2 PR body.

### 3.2 `site_information` (legacy mirror; no DDL change)

`site_information.barcode.*` quantity/dimension keys are retained as **read-only mirrors** for one release cycle per FRS §2.7 / FR-030. No DDL change in v2.

`site_information.barcode.preprinted.*` keys (Preprinted Accession Number) remain editable — only the UI host moves (see [research.md Divergence 3](./research.md)).

A v2.x maintenance migration (NOT in this release) will remove the legacy quantity/dimension keys.

## 4. JSONB snapshot shape (FRS §7.3.1, verbatim)

The `order_label_request.preset_snapshot` column MUST conform to this shape:

```json
{
  "preset": {
    "id": 17,
    "name": "Specimen Label",
    "height_mm": 25,
    "width_mm": 76,
    "barcode_type": "CODE_128"
  },
  "fields": [
    { "field_key": "LAB_NUMBER",          "field_label": "Lab Number",          "is_required": true,  "display_order": 1 },
    { "field_key": "PATIENT_NAME",        "field_label": "Patient Name",        "is_required": false, "display_order": 2 },
    { "field_key": "COLLECTION_DATETIME", "field_label": "Collection Date/Time","is_required": false, "display_order": 3 }
  ],
  "test_link": {
    "test_id": 412,
    "default_qty": 2,
    "max_qty": 5,
    "allow_override": true
  }
}
```

Notes:

- **`preset`** captures everything needed to render the label frame + barcode.
- **`fields`** ordered list; `field_label` decouples the rendered label from the system label set evolving.
- **`test_link`** captures linked-test settings at save time; `null` if cell was driven by system default.
- Snapshot is **frozen** — edits to `label_preset` or `test_label_preset_link` made after the order is saved have no effect at reprint.

**JSONB binding in Hibernate:** OpenELIS already has a `JsonBinaryType` UserType at [`src/main/java/org/openelisglobal/hibernate/type/JsonBinaryType.java`](../../src/main/java/org/openelisglobal/hibernate/type/JsonBinaryType.java) — used by `Alert` and `PatientMergeAudit`. New entity uses `@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)` + `@Type(type = "jsonb")` on the column (matches existing OE pattern; do NOT introduce `@JdbcTypeCode(SqlTypes.JSON)` which is Hibernate 6.x native and not yet adopted by this codebase).

**Schema validation:** `PresetSnapshotDto` Java class provides type-safe binding. Reads tolerate older shapes (forward compat) via Jackson's `@JsonIgnoreProperties(ignoreUnknown = true)`.

## 5. Hibernate entity mapping notes

### 5.1 Package layout

```
src/main/java/org/openelisglobal/labelpreset/
├── valueholder/
│   ├── LabelPreset.java              # @Entity extends BaseObject<Integer>
│   ├── LabelPresetField.java         # @Entity extends BaseObject<Integer>
│   ├── TestLabelConfig.java          # @Entity extends BaseObject<...>   — see §5.3 note
│   ├── TestLabelPresetLink.java      # @Entity extends BaseObject<Integer>
│   ├── OrderLabelRequest.java        # @Entity extends BaseObject<Integer>; JSONB via JsonBinaryType
│   └── PresetSnapshotDto.java        # NOT JPA; serialized into JSONB
├── dao/      # @Repository, no @Transactional
├── service/  # @Service, @Transactional ONLY here
├── controller/rest/  # @RestController, NO @Transactional
└── form/     # Spring form beans
```

Follows constitution Principle IV (5-layer pattern). All new entities use `jakarta.persistence.*` imports (NOT `javax.persistence.*`).

### 5.2 Valueholder annotations — pattern (matches `SampleBarcodeInfo`)

**`LabelPreset.java`**:

```java
package org.openelisglobal.labelpreset.valueholder;

import jakarta.persistence.*;
import org.openelisglobal.common.valueholder.BaseObject;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "label_preset")
public class LabelPreset extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "label_preset_generator")
    @SequenceGenerator(name = "label_preset_generator", sequenceName = "label_preset_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false, length = 120, unique = true)
    private String name;

    @Column(name = "height_mm", nullable = false)
    private Integer heightMm;

    @Column(name = "width_mm", nullable = false)
    private Integer widthMm;

    @Enumerated(EnumType.STRING)
    @Column(name = "barcode_type", nullable = false, length = 20)
    private BarcodeType barcodeType;

    @Column(name = "prints_per_order", nullable = false)
    private Boolean printsPerOrder = false;

    @Column(name = "prints_per_sample", nullable = false)
    private Boolean printsPerSample = true;

    @Column(name = "default_per_order",  nullable = false) private Integer defaultPerOrder  = 0;
    @Column(name = "max_per_order",      nullable = false) private Integer maxPerOrder      = 10;
    @Column(name = "default_per_sample", nullable = false) private Integer defaultPerSample = 0;
    @Column(name = "max_per_sample",     nullable = false) private Integer maxPerSample     = 10;

    @Column(name = "is_system", nullable = false) private Boolean isSystem = false;
    @Column(name = "is_active", nullable = false) private Boolean isActive = true;

    @OneToMany(mappedBy = "preset", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<LabelPresetField> fields = new ArrayList<>();

    public LabelPreset() { super(); }

    @Override public Integer getId() { return id; }
    @Override public void setId(Integer id) { this.id = id; }

    // ... domain getters/setters
}

public enum BarcodeType { CODE_128, QR, DATAMATRIX }
```

Key conventions inherited from `BaseObject<Integer>`:

- **`last_updated`** column is inherited (`@Column(name = "last_updated") @Version private Timestamp lastupdated;` declared in `BaseObject`). Hibernate auto-updates on entity mutation.
- **Optimistic locking** via `@Version` on the inherited `lastupdated` column.
- **No `@CreationTimestamp` / `@UpdateTimestamp`** — `BaseObject` handles it.
- **`java.sql.Timestamp`** is the OE convention (NOT `OffsetDateTime` or `LocalDateTime`).

**`OrderLabelRequest.java`** — JSONB snapshot column:

```java
package org.openelisglobal.labelpreset.valueholder;

import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.type.JsonBinaryType;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

@Entity
@Table(name = "order_label_request")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class OrderLabelRequest extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_label_request_generator")
    @SequenceGenerator(name = "order_label_request_generator", sequenceName = "order_label_request_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_sample_id", referencedColumnName = "id", nullable = false)
    private Sample parentSample;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_item_id", referencedColumnName = "id", nullable = true)
    private SampleItem sampleItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preset_id", referencedColumnName = "id", nullable = false)
    private LabelPreset preset;

    @Column(name = "qty", nullable = false)
    private Integer qty;

    @Type(type = "jsonb")
    @Column(name = "preset_snapshot", nullable = false, columnDefinition = "jsonb")
    private PresetSnapshotDto presetSnapshot;

    @Override public Integer getId() { return id; }
    @Override public void setId(Integer id) { this.id = id; }
}
```

`Sample` and `SampleItem` are the **existing legacy entities** (with `String id` and `numeric(10,0)` DB columns). The `@ManyToOne` relationship + `@JoinColumn(referencedColumnName = "id")` lets Hibernate route through their existing `LIMSStringNumberUserType` automatically — we don't need to declare `String parentSampleId` or apply any custom type ourselves on this side.

**`LabelPresetField.java`**:

```java
@Entity
@Table(name = "label_preset_field",
    uniqueConstraints = {
        @UniqueConstraint(name = "label_preset_field_order_uniq", columnNames = {"preset_id", "display_order"}),
        @UniqueConstraint(name = "label_preset_field_key_uniq",   columnNames = {"preset_id", "field_key"})
    })
public class LabelPresetField extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "label_preset_field_generator")
    @SequenceGenerator(name = "label_preset_field_generator", sequenceName = "label_preset_field_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preset_id", referencedColumnName = "id", nullable = false)
    private LabelPreset preset;

    @Column(name = "field_key", nullable = false, length = 60) private String fieldKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private FieldSourceType sourceType = FieldSourceType.SYSTEM;

    @Column(name = "is_required",   nullable = false) private Boolean isRequired = false;
    @Column(name = "display_order", nullable = false) private Integer displayOrder;

    @Override public Integer getId() { return id; }
    @Override public void setId(Integer id) { this.id = id; }
}

public enum FieldSourceType { SYSTEM /* v3+: CUSTOM_FREETEXT, CUSTOM_FIXED, QUERY_DRIVEN */ }
```

### 5.3 `TestLabelConfig` — special: PK is a legacy-typed FK

`TestLabelConfig`'s PK is `test_id`, which IS `numeric(10,0)` in the DB (matches the legacy `test.id`). This is the one case where the new schema directly inherits a legacy ID column type. Options for the Java entity:

**Recommended:** declare a JPA `@MapsId` relationship to `Test`, so Java sees a `Test` object (via the legacy entity's mapping) instead of a raw String/numeric value:

```java
@Entity
@Table(name = "test_label_config")
public class TestLabelConfig extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "test_id")
    private String testId;   // String + numeric(10,0) bridged by LIMSStringNumberUserType

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "test_id", referencedColumnName = "id")
    private org.openelisglobal.test.valueholder.Test test;

    @Column(name = "allow_order_entry_override", nullable = false)
    private Boolean allowOrderEntryOverride = true;

    @Override public String getId() { return testId; }
    @Override public void setId(String id) { this.testId = id; }
}
```

`BaseObject<String>` here because the PK is `String` (mapped through `Test`'s legacy convention). This is the ONE place we adopt the legacy ID pattern in OGC-285, because we're using an existing legacy PK as our own PK.

### 5.4 `TestLabelPresetLink`:

```java
@Entity
@Table(name = "test_label_preset_link",
    uniqueConstraints = {
        @UniqueConstraint(name = "test_label_preset_link_uniq", columnNames = {"test_id", "preset_id"})
    })
public class TestLabelPresetLink extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_label_preset_link_generator")
    @SequenceGenerator(name = "test_label_preset_link_generator", sequenceName = "test_label_preset_link_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", referencedColumnName = "id", nullable = false)
    private org.openelisglobal.test.valueholder.Test test;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preset_id", referencedColumnName = "id", nullable = false)
    private LabelPreset preset;

    @Column(name = "default_qty",    nullable = false) private Integer defaultQty;
    @Column(name = "max_qty",        nullable = false) private Integer maxQty;
    @Column(name = "allow_override", nullable = false) private Boolean allowOverride = true;

    @Override public Integer getId() { return id; }
    @Override public void setId(Integer id) { this.id = id; }
}
```

## 6. Read paths and aggregation

### 6.1 Order Entry aggregation function (M5a)

Server-side aggregation function (called by `POST /api/orderEntry/labelRequest`), implementing FRS §4.4.1 conflict-resolution:

```
GIVEN test_ids = [412, 518], samples = [{S1, BLOOD_EDTA}, {S2, TISSUE}]
1. SELECT all per-order presets where is_active = true and prints_per_order = true.
2. SELECT distinct preset_ids from test_label_preset_link
   WHERE test_id IN test_ids
   AND preset.prints_per_sample = true
   AND preset.is_active = true.
3. For each (sample, preset) cell:
   3a. Find all test_label_preset_link rows for tests in test_ids that link preset.
   3b. default_qty = MAX(link.default_qty), or preset.default_per_sample if no links.
   3c. max_qty = MAX(link.max_qty), or preset.max_per_sample if no links.
   3d. allow_override = AND(link.allow_override) AND test_label_config.allow_order_entry_override
       (most-restrictive: any false anywhere → cell locked).
   3e. source = "test" with source_test_id+name if any link drove the default; else "preset_default".
4. For Order Labels row:
   4a. default_qty = preset.default_per_order.
   4b. max_qty = preset.max_per_order.
   4c. No source tag (per-order is lab-wide, not test-driven).
5. Sort columns: system presets first (in id order), then custom presets alphabetically.
```

Deterministic: same inputs → same output. Test coverage MUST include AC-13, AC-14, AC-15, AC-16, AC-17, AC-18.

### 6.2 Reprint path (M6)

```
GIVEN parent_sample_id from Order View page
1. SELECT order_label_request WHERE parent_sample_id = ? ORDER BY preset.is_system DESC, preset.name ASC.
2. For each row: render PDF using preset_snapshot (NOT current label_preset).
3. Snapshot test_link block, if non-null, supplies the qty source.
```

NO read from `label_preset` or `test_label_preset_link` during reprint. AC-20 verification: mutate `label_preset.height_mm` post-save, reprint, assert PDF dimensions match snapshot.

## 7. Schema migration order

M2 Liquibase changesets (in execution order):

1. `029-label-preset-tables.xml`:
   - `label-preset-001-label-preset` — CREATE `label_preset` + sequence + indexes + table-level CHECKs.
   - `label-preset-002-label-preset-field` — CREATE `label_preset_field` + sequence + indexes.
   - `label-preset-003-test-label-config` — CREATE `test_label_config`.
   - `label-preset-004-test-label-preset-link` — Conditional CREATE-or-ALTER on `test_label_preset_link` (preconditions check for absence per research.md §5).
   - `label-preset-005-order-label-request` — CREATE `order_label_request` + sequence + indexes.
2. `030-seed-system-presets.xml`:
   - `label-preset-seed-001-system-presets` — INSERT 5 `label_preset` rows from `site_information.barcode.*` with canonical fallback.
3. `031-seed-system-preset-fields.xml`:
   - `label-preset-seed-002-system-preset-fields` — INSERT `label_preset_field` rows (LAB_NUMBER always; v1 carry-over optional fields).

All changesets MUST include `<rollback>` blocks (Principle VI). Rollback exercised in M2 PR CI.

## 8. References

- [FRS §7 (data model)](https://github.com/DIGI-UW/openelis-work/blob/7cf6f65cae9a9794e52f3dd4c5e759c920d87bf5/designs/admin-config/barcode-labels.md#7-data-model)
- [FRS §7.3.1 (snapshot shape)](https://github.com/DIGI-UW/openelis-work/blob/7cf6f65cae9a9794e52f3dd4c5e759c920d87bf5/designs/admin-config/barcode-labels.md#731-canonical-preset_snapshot-jsonb-shape)
- [FRS §4.4.1 (aggregation rules)](https://github.com/DIGI-UW/openelis-work/blob/7cf6f65cae9a9794e52f3dd4c5e759c920d87bf5/designs/admin-config/barcode-labels.md#441-aggregation-conflict-resolution-rules)
- [`SampleBarcodeInfo.java`](../../src/main/java/org/openelisglobal/barcode/valueholder/SampleBarcodeInfo.java) — canonical new-entity JPA pattern
- [`BaseObject<PK>`](../../src/main/java/org/openelisglobal/common/valueholder/BaseObject.java) — parent class with inherited `last_updated` + `@Version`
- [`JsonBinaryType`](../../src/main/java/org/openelisglobal/hibernate/type/JsonBinaryType.java) — existing OE JSONB binding (used by `Alert`, `PatientMergeAudit`)
- [`LIMSStringNumberUserType`](../../src/main/java/org/openelisglobal/hibernate/resources/usertype/LIMSStringNumberUserType.java) — legacy `String id` ↔ `numeric(10,0)` bridge
- [spec.md FR-001..FR-033](./spec.md#functional-requirements)
- [research.md](./research.md) — code-truth verifications + clarification rationale
- [plan.md](./plan.md) — milestone + testing strategy
- [contracts/openapi.yaml](./contracts/openapi.yaml)
- [quickstart.md](./quickstart.md)
