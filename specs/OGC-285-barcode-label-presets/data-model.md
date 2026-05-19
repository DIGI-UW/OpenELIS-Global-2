# Data Model: OGC-285 Barcode Labels v2

**Source of truth:** FRS v2.5 §7 ([pinned at SHA `7cf6f65`](https://github.com/DIGI-UW/openelis-work/blob/7cf6f65cae9a9794e52f3dd4c5e759c920d87bf5/designs/admin-config/barcode-labels.md#7-data-model)).
**Schema namespace:** `clinlims` (default OpenELIS schema).
**Liquibase changeset paths:**
- `src/main/resources/liquibase/3.3.x.x/029-label-preset-tables.xml`
- `src/main/resources/liquibase/3.3.x.x/030-seed-system-presets.xml`

(NN = `029`, `030` — next available after OGC-284's `028-barcode-info-tables.xml`.)

## 1. Entity-relationship overview

```
                              ┌─────────────────────────┐
                              │       test (existing)   │
                              └────────────┬────────────┘
                                           │  1
                                           │  *
                              ┌────────────▼────────────┐         ┌──────────────────────────┐
                              │  test_label_preset_link │         │     test_label_config    │
                              │  (CREATED by M2 — was   │         │  (new — per-test         │
                              │   OGC-761 territory but │         │   master toggle)         │
                              │   doesn't exist yet)    │         └────────────┬─────────────┘
                              └────────────┬────────────┘                      │  1
                                           │  *                                │  test_id PK
                                           │  1                                │  ↑
                              ┌────────────▼────────────┐                      │
                              │       label_preset      │◀─────────────────────┘
                              │  (new — preset config)  │
                              └────────────┬────────────┘
                                           │  1
                                           │  *
                              ┌────────────▼────────────┐
                              │   label_preset_field    │
                              │  (new — content fields  │
                              │   per preset)           │
                              └─────────────────────────┘

                              ┌─────────────────────────┐
                              │      sample (existing)  │
                              └────────────┬────────────┘
                                           │ 1
                                           │ *
                              ┌────────────▼────────────┐         ┌──────────────────────────┐
                              │   order_label_request   │────────▶│       label_preset       │
                              │  (new — per-order-      │   *:1   │  (snapshot reference)    │
                              │   labels with snapshot) │         └──────────────────────────┘
                              └─────────────────────────┘
                                          │
                                          │  preset_snapshot JSONB column
                                          │  (frozen full preset config at save time)
                                          ▼
```

## 2. New tables

### 2.1 `label_preset` (FRS §7.1)

```sql
CREATE TABLE clinlims.label_preset (
  id                  BIGSERIAL                       PRIMARY KEY,
  name                VARCHAR(120)                    NOT NULL,
  height_mm           INTEGER                         NOT NULL CHECK (height_mm BETWEEN 5 AND 200),
  width_mm            INTEGER                         NOT NULL CHECK (width_mm BETWEEN 5 AND 200),
  barcode_type        VARCHAR(20)                     NOT NULL CHECK (barcode_type IN ('CODE_128','QR','DATAMATRIX')),
  prints_per_order    BOOLEAN                         NOT NULL DEFAULT false,
  prints_per_sample   BOOLEAN                         NOT NULL DEFAULT true,
  default_per_order   INTEGER                         NOT NULL DEFAULT 0  CHECK (default_per_order  >= 0),
  max_per_order       INTEGER                         NOT NULL DEFAULT 10 CHECK (max_per_order      >= default_per_order),
  default_per_sample  INTEGER                         NOT NULL DEFAULT 0  CHECK (default_per_sample >= 0),
  max_per_sample      INTEGER                         NOT NULL DEFAULT 10 CHECK (max_per_sample     >= default_per_sample),
  is_system           BOOLEAN                         NOT NULL DEFAULT false,
  is_active           BOOLEAN                         NOT NULL DEFAULT true,
  created_at          TIMESTAMP WITH TIME ZONE        NOT NULL DEFAULT now(),
  updated_at          TIMESTAMP WITH TIME ZONE        NOT NULL DEFAULT now(),
  CONSTRAINT label_preset_name_uniq             UNIQUE (name),
  CONSTRAINT label_preset_scope_required        CHECK  (prints_per_order OR prints_per_sample)
);

CREATE INDEX idx_label_preset_active   ON clinlims.label_preset (is_active);
CREATE INDEX idx_label_preset_system   ON clinlims.label_preset (is_system);
```

**Key constraints:**

- `label_preset_name_uniq`: exact-match UNIQUE at DDL level
  (defense-in-depth). The service layer enforces the **case-insensitive
  + trim** normalization rule (per `/speckit.clarify` Q3, 2026-05-19):
  - `LabelPresetService.normalizeName(input) = input.trim().toLowerCase()`
  - Pre-insert/update check compares normalized names; on collision
    returns a field-level error before hitting DDL.
- `label_preset_scope_required`: at least one of `prints_per_order` /
  `prints_per_sample` MUST be true (FR-007).
- `max_per_order >= default_per_order` and
  `max_per_sample >= default_per_sample`: FR-007.

**Note on storage of normalized name:** the `name` column stores the
**original casing** (admin-typed); normalization happens only at the
uniqueness check. The display layer always shows the original casing.

**Seed data (5 system presets at v2 release, via separate seed migration):**

| Name | `is_system` | `prints_per_order` | `prints_per_sample` | Source for `default_per_*` / `max_per_*` |
|---|---|---|---|---|
| Order Label | true | true | false | `site_information.barcode.order.{default,max}` → `default_per_order` / `max_per_order` |
| Specimen Label | true | false | true | `site_information.barcode.specimen.{default,max}` → `default_per_sample` / `max_per_sample` |
| Block Label | true | false | true | `site_information.barcode.block.{default,max}` → `default_per_sample` / `max_per_sample` |
| Slide Label | true | false | true | `site_information.barcode.slide.{default,max}` → `default_per_sample` / `max_per_sample` |
| Freezer Label | true | false | true | `site_information.barcode.freezer.{default,max}` → `default_per_sample` / `max_per_sample` |

Dimensions copied from `site_information.barcode.{type}.{height,width}`.
The "inert" scope columns (those whose scope flag is false) receive
schema defaults (`0` / `10`) but are not used.

**Malformed-input handling** (per OGC-284 FR-004 carry-over): if a
legacy `site_information.barcode.{type}.{default,max}` value is
malformed or missing, the seed migration applies the canonical
fallback (default `1`, max `10`) and logs a warning. Migration MUST
NOT fail on bad data.

### 2.2 `label_preset_field` (FRS §7.1)

```sql
CREATE TABLE clinlims.label_preset_field (
  id            BIGSERIAL    PRIMARY KEY,
  preset_id     BIGINT       NOT NULL REFERENCES clinlims.label_preset(id) ON DELETE CASCADE,
  field_key     VARCHAR(60)  NOT NULL,
  source_type   VARCHAR(20)  NOT NULL DEFAULT 'SYSTEM' CHECK (source_type = 'SYSTEM'),
  is_required   BOOLEAN      NOT NULL DEFAULT false,
  display_order INTEGER      NOT NULL,
  CONSTRAINT label_preset_field_order_uniq   UNIQUE (preset_id, display_order),
  CONSTRAINT label_preset_field_key_uniq     UNIQUE (preset_id, field_key)
);

CREATE INDEX idx_label_preset_field_preset ON clinlims.label_preset_field (preset_id);
```

**`source_type`** constrained to a single value (`SYSTEM`) in v2. The
column exists to allow v3+ to introduce additional source types
(`CUSTOM_FREETEXT`, `CUSTOM_FIXED`, `QUERY_DRIVEN`) without an
`ALTER TYPE` round trip. See [research.md §2 Q4](./research.md).

**`field_key`** values for v2 — 15 selectable + `LAB_NUMBER` locked
at position 1:

| `field_key` | UI label | Source |
|---|---|---|
| `LAB_NUMBER` | Lab Number | Always required, locked at position 1 (FRS §2.4). For per-order labels, renders order number; for per-sample labels, renders sample number. |
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

**Migration of v1 "Barcode Label Elements" checkboxes:** the seed
migration populates `label_preset_field` rows for each seeded system
preset matching its current v1 optional toggles (Patient Name, DOB,
Sex, etc.). Lab Number is always added with `is_required = true`
and `display_order = 1`. The exact v1→v2 mapping for each of the 5
system presets is documented in the seed migration changeset
(`030-seed-system-presets.xml`) for traceability.

### 2.3 `test_label_config` (FRS §7.2 — new table)

```sql
CREATE TABLE clinlims.test_label_config (
  test_id                    BIGINT                          PRIMARY KEY REFERENCES clinlims.test(id) ON DELETE CASCADE,
  allow_order_entry_override BOOLEAN                         NOT NULL DEFAULT true,
  updated_at                 TIMESTAMP WITH TIME ZONE        NOT NULL DEFAULT now()
);
```

One row per test (lazy-created on first save of the test's Labels
tab). Master toggle for whether ANY override of label quantities is
permitted at Order Entry for orders containing this test. When false,
all per-link `allow_override` checkboxes for this test are forced off
(FR-017 / AC-12).

### 2.4 `order_label_request` (FRS §7.3 — new table; snapshot store)

```sql
CREATE TABLE clinlims.order_label_request (
  id              BIGSERIAL                       PRIMARY KEY,
  order_id        BIGINT                          NOT NULL,
  sample_id       BIGINT                          NULL,            -- NULL for order-level (per-order scope) labels
  preset_id       BIGINT                          NOT NULL REFERENCES clinlims.label_preset(id),
  qty             INTEGER                         NOT NULL CHECK (qty >= 0),
  preset_snapshot JSONB                           NOT NULL,        -- frozen copy of preset + fields + link settings at save time
  created_at      TIMESTAMP WITH TIME ZONE        NOT NULL DEFAULT now()
);

CREATE INDEX idx_order_label_request_order  ON clinlims.order_label_request (order_id);
CREATE INDEX idx_order_label_request_sample ON clinlims.order_label_request (sample_id) WHERE sample_id IS NOT NULL;
CREATE INDEX idx_order_label_request_preset ON clinlims.order_label_request (preset_id);
```

**Snapshot semantics:**

- One row per `(order, sample, preset)` for per-sample presets.
- One row per `(order, preset)` for per-order presets
  (`sample_id IS NULL`).
- `preset_snapshot` JSONB is the **authoritative source for reprint**
  (AC-20). Reprint MUST NOT re-fetch `label_preset` or
  `test_label_preset_link`.

**Decrease-only post-save semantics** (per `/speckit.clarify` Q1):
when the post-save dialog Print action fires, `qty` is overwritten to
the (possibly lower) value the technician entered. Original saved qty
is preserved transiently in the `preset_snapshot` JSONB (the
`test_link.default_qty` field for test-driven cells, or implied by
`preset_snapshot.preset.default_per_*` for system-default cells).

**Foreign key to order/sample:** the `order_id` and `sample_id`
columns reference the existing OpenELIS sample/order identity. No
explicit FK constraints in this DDL because the OpenELIS
sample/order tables use the `sample` table as the canonical
identity surface and FK conventions vary across the existing schema.
M2 reviewer confirms the right FK target against
`src/main/java/org/openelisglobal/sample/valueholder/Sample.java`
during code review.

## 3. Modified tables

### 3.1 `test_label_preset_link` (was OGC-761; CREATED by M2 since OGC-761 absent on develop)

Per [research.md §5](./research.md), OGC-761 has NOT landed on
develop. M2 CREATEs the full table:

```sql
CREATE TABLE clinlims.test_label_preset_link (
  id             BIGSERIAL    PRIMARY KEY,
  test_id        BIGINT       NOT NULL REFERENCES clinlims.test(id) ON DELETE CASCADE,
  preset_id      BIGINT       NOT NULL REFERENCES clinlims.label_preset(id),
  default_qty    INTEGER      NOT NULL CHECK (default_qty >= 0),
  max_qty        INTEGER      NOT NULL CHECK (max_qty >= default_qty),
  allow_override BOOLEAN      NOT NULL DEFAULT true,
  CONSTRAINT test_label_preset_link_uniq UNIQUE (test_id, preset_id)
);

CREATE INDEX idx_test_label_preset_link_test   ON clinlims.test_label_preset_link (test_id);
CREATE INDEX idx_test_label_preset_link_preset ON clinlims.test_label_preset_link (preset_id);
```

**Per-sample-only constraint** (FRS §3.5): rows MUST reference
presets where `prints_per_sample = true`. PostgreSQL CHECK
constraints cannot reference other tables, so this is enforced at
the **service layer**:

```java
// In LinkService.assertPerSamplePreset(presetId):
LabelPreset preset = labelPresetDAO.getById(presetId);
if (!Boolean.TRUE.equals(preset.getPrintsPerSample())) {
    throw new BusinessException("Cannot link order-only preset to a test");
}
```

A SQL trigger could be added as additional defense-in-depth if a
future audit shows service-layer bypass risk (out of scope for v2).

**If OGC-761 lands BEFORE M2 ships:** the M2 changeset's
`<preConditions onFail="MARK_RAN"><not><tableExists tableName="test_label_preset_link"/></not></preConditions>`
guard skips the CREATE; a follow-up `ALTER TABLE` adds the
`allow_override` column.

### 3.2 `site_information` (legacy mirror; no DDL change)

`site_information.barcode.*` quantity/dimension keys (the 5-type v1
config) are retained as **read-only mirrors** for one release cycle
per FRS §2.7 / FR-030. No DDL change in v2.

`site_information.barcode.preprinted.*` keys (the Preprinted
Accession Number settings) remain fully editable — only the UI host
moves (see [research.md Divergence 3](./research.md)).

A v2.x maintenance migration (NOT in this release) will remove the
legacy quantity/dimension keys via:

```sql
DELETE FROM clinlims.site_information
WHERE name LIKE 'barcode.order.%'
   OR name LIKE 'barcode.specimen.%'
   OR name LIKE 'barcode.block.%'
   OR name LIKE 'barcode.slide.%'
   OR name LIKE 'barcode.freezer.%';
-- (excluding 'barcode.preprinted.*' which remain)
```

## 4. JSONB snapshot shape (FRS §7.3.1, verbatim)

The `order_label_request.preset_snapshot` column MUST conform to this
shape. Reprints read from this shape only; new fields added to
`label_preset` in future releases do NOT retroactively appear in
historical snapshots.

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

- **`preset`** captures everything needed to render the label frame and
  barcode.
- **`fields`** ordered list of `{field_key, field_label, is_required,
  display_order}`. `field_label` is included so the rendered label is
  decoupled from the system label set evolving over time (a future
  field rename does not break old labels).
- **`test_link`** captures the linked-test settings at save time. If
  the cell was driven by the system default rather than a specific
  test link, this object is set to `null` and `qty` reflects
  `label_preset.default_per_*` at save time.
- The snapshot is **frozen**: edits to `label_preset` or
  `test_label_preset_link` made after the order is saved have no
  effect on the rendered label at reprint.

**Schema validation:** M2 ships a `PresetSnapshotDto` Java class +
matching JSON schema validation in
`OrderLabelRequestServiceImpl.persistRequest()` to guarantee the
JSONB column conforms to this shape at write time. Reads tolerate
older shapes (forward compatibility) via Jackson's
`@JsonIgnoreProperties(ignoreUnknown = true)`.

## 5. Hibernate entity mapping notes

### 5.1 Package layout

```
src/main/java/org/openelisglobal/labelpreset/
├── valueholder/
│   ├── LabelPreset.java
│   ├── LabelPresetField.java
│   ├── TestLabelConfig.java
│   ├── OrderLabelRequest.java
│   ├── TestLabelPresetLink.java
│   └── PresetSnapshotDto.java        # NOT a JPA entity; serialized into the JSONB column
├── dao/
│   ├── LabelPresetDAO.java + Impl
│   ├── LabelPresetFieldDAO.java + Impl
│   ├── TestLabelConfigDAO.java + Impl
│   ├── TestLabelPresetLinkDAO.java + Impl
│   └── OrderLabelRequestDAO.java + Impl
├── service/
│   ├── LabelPresetService.java + Impl                # @Transactional
│   ├── TestLabelConfigService.java + Impl            # @Transactional
│   ├── TestLabelPresetLinkService.java + Impl        # @Transactional
│   ├── OrderLabelRequestService.java + Impl          # @Transactional
│   └── OrderEntryLabelRequestService.java + Impl     # aggregation function (M5)
├── controller/rest/
│   ├── LabelPresetRestController.java                # 6 endpoints (M3)
│   ├── TestLabelConfigRestController.java            # 2 endpoints (M4)
│   ├── OrderEntryLabelRequestController.java         # 1 endpoint (M5)
│   └── OrderLabelRequestController.java              # 2 endpoints (M6)
└── form/
    ├── LabelPresetForm.java
    └── TestLabelConfigForm.java
```

Follows constitution Principle IV (5-layer pattern). `@Transactional`
in services ONLY (NEVER controllers). Valueholders use JPA/Hibernate
annotations with Jakarta EE 9 `jakarta.persistence.*` imports.

### 5.2 Valueholder annotations — selected highlights

**`LabelPreset.java`** (excerpt):

```java
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(schema = "clinlims", name = "label_preset")
public class LabelPreset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120, unique = true)
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

    @Column(name = "default_per_order", nullable = false)
    private Integer defaultPerOrder = 0;

    @Column(name = "max_per_order", nullable = false)
    private Integer maxPerOrder = 10;

    @Column(name = "default_per_sample", nullable = false)
    private Integer defaultPerSample = 0;

    @Column(name = "max_per_sample", nullable = false)
    private Integer maxPerSample = 10;

    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "preset", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<LabelPresetField> fields = new ArrayList<>();

    // getters/setters/equals/hashCode follow OpenELIS valueholder conventions
}

public enum BarcodeType {
    CODE_128, QR, DATAMATRIX
}
```

**`OrderLabelRequest.java`** — JSONB snapshot column:

```java
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "clinlims", name = "order_label_request")
public class OrderLabelRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "sample_id", nullable = true)
    private Long sampleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preset_id", nullable = false)
    private LabelPreset preset;

    @Column(nullable = false)
    private Integer qty;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preset_snapshot", nullable = false, columnDefinition = "jsonb")
    private PresetSnapshotDto presetSnapshot;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
```

`PresetSnapshotDto` is a typed Java class matching FRS §7.3.1
serialized via Jackson + Hibernate 6.x's `@JdbcTypeCode(SqlTypes.JSON)`.

**`LabelPresetField.java`**:

```java
@Entity
@Table(schema = "clinlims", name = "label_preset_field",
    uniqueConstraints = {
        @UniqueConstraint(name = "label_preset_field_order_uniq", columnNames = {"preset_id", "display_order"}),
        @UniqueConstraint(name = "label_preset_field_key_uniq",   columnNames = {"preset_id", "field_key"})
    })
public class LabelPresetField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preset_id", nullable = false)
    private LabelPreset preset;

    @Column(name = "field_key", nullable = false, length = 60)
    private String fieldKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private FieldSourceType sourceType = FieldSourceType.SYSTEM;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}

public enum FieldSourceType { SYSTEM /* v3+: CUSTOM_FREETEXT, CUSTOM_FIXED, QUERY_DRIVEN */ }
```

**`TestLabelConfig.java`**:

```java
@Entity
@Table(schema = "clinlims", name = "test_label_config")
public class TestLabelConfig {

    @Id
    @Column(name = "test_id")
    private Long testId;

    @Column(name = "allow_order_entry_override", nullable = false)
    private Boolean allowOrderEntryOverride = true;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
```

**`TestLabelPresetLink.java`**:

```java
@Entity
@Table(schema = "clinlims", name = "test_label_preset_link",
    uniqueConstraints = {
        @UniqueConstraint(name = "test_label_preset_link_uniq", columnNames = {"test_id", "preset_id"})
    })
public class TestLabelPresetLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_id", nullable = false)
    private Long testId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preset_id", nullable = false)
    private LabelPreset preset;

    @Column(name = "default_qty", nullable = false)
    private Integer defaultQty;

    @Column(name = "max_qty", nullable = false)
    private Integer maxQty;

    @Column(name = "allow_override", nullable = false)
    private Boolean allowOverride = true;
}
```

## 6. Read paths and aggregation

### 6.1 Order Entry aggregation function (M5)

The server-side aggregation function (called by `POST
/api/orderEntry/labelRequest`) implements FRS §4.4.1 conflict-
resolution rules:

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

This function is **deterministic**: same inputs always produce the
same output. Test coverage MUST include AC-13, AC-14, AC-15, AC-16,
AC-17, AC-18.

### 6.2 Reprint path (M6)

```
GIVEN order_id from Order View page
1. SELECT order_label_request WHERE order_id = ? ORDER BY preset.is_system DESC, preset.name ASC.
2. For each row: render PDF using preset_snapshot (NOT current label_preset).
3. Snapshot test_link block, if non-null, supplies the qty source.
```

NO read from `label_preset` or `test_label_preset_link` during
reprint. AC-20 verification: mutate `label_preset.height_mm`
post-save, reprint, assert PDF dimensions match snapshot.

## 7. Schema migration order

M2 Liquibase changeset (`029-label-preset-tables.xml`) executes in
this order:

1. Create `label_preset` table + sequence + indexes.
2. Create `label_preset_field` table.
3. Create `test_label_config` table.
4. Conditionally create OR alter `test_label_preset_link`:
   - `<preConditions onFail="MARK_RAN"><not><tableExists tableName="test_label_preset_link"/></not></preConditions>`
     → CREATE full table.
   - Otherwise (table exists from OGC-761 having landed) → ALTER ADD COLUMN `allow_override BOOLEAN NOT NULL DEFAULT true`.
5. Create `order_label_request` table + indexes.

Separate seed changeset (`030-seed-system-presets.xml`):

6. Read `site_information.barcode.*` keys. For each of 5 types
   (Order, Specimen, Block, Slide, Freezer), INSERT one `label_preset`
   row with `is_system = true`, scope flags + dimensions + quantities
   per FRS §2.7 mapping. Apply canonical fallback (`1` / `10`) for
   malformed input.
7. INSERT `label_preset_field` rows for each seeded preset matching
   the v1 "Barcode Label Elements" checkboxes carried over (Lab
   Number always required + position 1).

All changesets MUST include `<rollback>` blocks (constitution
Principle VI). Rollback restores the pre-M2 state and is exercised
in the M2 PR CI.

## 8. References

- [FRS §7 (data model)](https://github.com/DIGI-UW/openelis-work/blob/7cf6f65cae9a9794e52f3dd4c5e759c920d87bf5/designs/admin-config/barcode-labels.md#7-data-model)
- [FRS §7.3.1 (snapshot shape)](https://github.com/DIGI-UW/openelis-work/blob/7cf6f65cae9a9794e52f3dd4c5e759c920d87bf5/designs/admin-config/barcode-labels.md#731-canonical-preset_snapshot-jsonb-shape)
- [FRS §4.4.1 (aggregation rules)](https://github.com/DIGI-UW/openelis-work/blob/7cf6f65cae9a9794e52f3dd4c5e759c920d87bf5/designs/admin-config/barcode-labels.md#441-aggregation-conflict-resolution-rules)
- [spec.md FR-001..FR-033](./spec.md#functional-requirements)
- [research.md](./research.md) (source-code verifications + clarification rationale)
- [plan.md](./plan.md) (milestone + testing strategy)
- [contracts/openapi.yaml](./contracts/openapi.yaml)
- [quickstart.md](./quickstart.md)
