# OpenELIS Global 2 - Menu Configuration Guide

This directory contains configuration files for managing the OpenELIS menu
system.

## Overview

OpenELIS Global 2 supports two types of menu configuration:

1. **Menu Filtering** (`menu_config.json`) - Runtime filtering of existing menus
2. **Menu Seeding** (`menu_seed.json`) - Startup creation of menu entries

## File Descriptions

### `menu_config.json` - Menu Filtering Configuration

**Purpose**: Filter (show/hide) existing menu items at runtime

**When to use**:

- Hide specific menus for certain implementations
- Show only a subset of available menus
- Runtime menu visibility control

**Requirements**:

- Set `org.openelisglobal.menu.configuration.autocreate=true` in
  `application.properties`
- Menus must already exist in the database

**Format**:

```json
{
  "includes": [
    {
      "elementId": "menu_home",
      "childMenus": []
    }
  ]
}
```

**Modes**:

- `includes` - Whitelist mode (show only specified menus)
- `excludes` - Blacklist mode (hide specified menus)

---

### `menu_seed.json` - Menu Seeding Configuration

**Purpose**: Create menu entries in the database at application startup

**When to use**:

- Ship OpenELIS with a predefined menu structure
- Deploy implementation-specific menu layouts
- Initialize menus without manual UI configuration or Liquibase changesets

**Requirements**:

- Set `org.openelisglobal.menu.seed.enabled=true` in `application.properties`
- Create `menu_seed.json` with menu definitions

**Behavior**:

- ✅ Creates menus that don't exist (by `elementId`)
- ✅ Preserves existing menus (idempotent)
- ✅ Runs once at application startup
- ✅ Supports parent-child menu hierarchies
- ✅ Forces menu cache rebuild after seeding

**Format**:

```json
{
  "menus": [
    {
      "elementId": "menu_home",
      "displayKey": "banner.menu.home",
      "toolTipKey": "banner.menu.home.tooltip",
      "actionURL": "/Dashboard",
      "presentationOrder": 1,
      "isActive": true,
      "openInNewWindow": false,
      "hideInOldUI": false,
      "childMenus": []
    }
  ]
}
```

**Field Reference**:

| Field               | Required | Default | Description                                  |
| ------------------- | -------- | ------- | -------------------------------------------- |
| `elementId`         | ✅ Yes   | -       | Unique identifier (used for existence check) |
| `displayKey`        | ✅ Yes   | -       | i18n key for menu label                      |
| `toolTipKey`        | No       | -       | i18n key for tooltip                         |
| `actionURL`         | No       | -       | URL or path to navigate to                   |
| `clickAction`       | No       | -       | JavaScript action                            |
| `presentationOrder` | ✅ Yes   | -       | Sort order (lower numbers first)             |
| `isActive`          | No       | `true`  | Initial visibility state                     |
| `openInNewWindow`   | No       | `false` | Open link in new window                      |
| `hideInOldUI`       | No       | `false` | Hide in legacy UI                            |
| `childMenus`        | No       | `[]`    | Array of child menu definitions              |

---

## Configuration Properties

Add these properties to `application.properties`:

```properties
# Menu Filtering
org.openelisglobal.menu.configuration.autocreate=false

# Menu Seeding
org.openelisglobal.menu.seed.enabled=false
org.openelisglobal.menu.seed.config.path=/var/lib/openelis-global/menu/menu_seed.json
```

---

## Usage Examples

### Example 1: Filter Menus (Runtime)

**Goal**: Show only Home, Sample, and Patient menus

**File**: `menu_config.json`

```json
{
  "includes": [
    { "elementId": "menu_home", "childMenus": [] },
    { "elementId": "menu_sample", "childMenus": [] },
    { "elementId": "menu_patient", "childMenus": [] }
  ]
}
```

**Configuration**:

```properties
org.openelisglobal.menu.configuration.autocreate=true
```

---

### Example 2: Seed Implementation-Specific Menus (Startup)

**Goal**: Create a simplified menu structure for a clinic implementation

**File**: `menu_seed.json`

```json
{
  "menus": [
    {
      "elementId": "menu_clinic_home",
      "displayKey": "clinic.menu.home",
      "actionURL": "/Dashboard",
      "presentationOrder": 1,
      "isActive": true
    },
    {
      "elementId": "menu_clinic_register",
      "displayKey": "clinic.menu.register",
      "actionURL": "/PatientManagement",
      "presentationOrder": 2,
      "isActive": true
    },
    {
      "elementId": "menu_clinic_results",
      "displayKey": "clinic.menu.results",
      "presentationOrder": 3,
      "isActive": true,
      "childMenus": [
        {
          "elementId": "menu_clinic_results_view",
          "displayKey": "clinic.menu.results.view",
          "actionURL": "/ResultsByPatient",
          "presentationOrder": 1,
          "isActive": true
        },
        {
          "elementId": "menu_clinic_results_print",
          "displayKey": "clinic.menu.results.print",
          "actionURL": "/Reports",
          "presentationOrder": 2,
          "isActive": true
        }
      ]
    }
  ]
}
```

**Configuration**:

```properties
org.openelisglobal.menu.seed.enabled=true
```

**Result**: On first startup, these menus are created. On subsequent restarts,
existing menus are preserved.

---

### Example 3: Combined Approach

**Use Case**: Seed a comprehensive menu structure, then filter to show only
relevant menus for specific users

**Step 1**: Seed all possible menus via `menu_seed.json`

```json
{
  "menus": [
    {
      "elementId": "menu_lab",
      "displayKey": "lab.menu",
      "presentationOrder": 1,
      "isActive": true
    },
    {
      "elementId": "menu_clinic",
      "displayKey": "clinic.menu",
      "presentationOrder": 2,
      "isActive": true
    },
    {
      "elementId": "menu_admin",
      "displayKey": "admin.menu",
      "presentationOrder": 3,
      "isActive": true
    }
  ]
}
```

**Step 2**: Filter to show only clinic menus via `menu_config.json`

```json
{
  "includes": [{ "elementId": "menu_clinic", "childMenus": [] }]
}
```

**Configuration**:

```properties
org.openelisglobal.menu.seed.enabled=true
org.openelisglobal.menu.configuration.autocreate=true
```

---

## Deployment Workflow

### For Implementation Teams

1. **Design your menu structure** in `menu_seed.json`
2. **Enable seeding** in `application.properties`:
   ```properties
   org.openelisglobal.menu.seed.enabled=true
   ```
3. **Deploy** - Menus are created on first startup
4. **(Optional) Fine-tune** via UI Menu Configuration at `/globalMenuManagement`
5. **(Optional) Apply runtime filters** via `menu_config.json`

### For Development Teams

1. **Create example seed files** for common implementations
2. **Document required i18n keys** in `displayKey` and `toolTipKey`
3. **Test idempotency** - verify menus aren't duplicated on restart
4. **Validate hierarchy** - ensure parent-child relationships are correct

---

## Troubleshooting

### Menus not appearing after seeding

**Check**:

1. Is `org.openelisglobal.menu.seed.enabled=true`?
2. Does `/var/lib/openelis-global/menu/menu_seed.json` exist?
3. Check logs for `MenuSeedService` errors
4. Verify `elementId` doesn't conflict with existing menus
5. Ensure `isActive` is `true`

### Menus duplicated

**Cause**: Menu seeding creates entries by `elementId`. If `elementId` changes
between runs, duplicates occur.

**Solution**: Use consistent `elementId` values across deployments.

### Child menus not showing

**Check**:

1. Parent menu must exist before children
2. Verify `presentationOrder` is set correctly
3. Check parent menu's `isActive` is `true`
4. Ensure parent has valid `elementId`

### Filtering not working

**Check**:

1. Is `org.openelisglobal.menu.configuration.autocreate=true`?
2. Does `menu_config.json` use correct mode (`includes` or `excludes`)?
3. Menu filtering only affects **existing** menus (not creation)

---

## Best Practices

### 1. Use Semantic Element IDs

```json
// Good
{"elementId": "menu_sample_create_initial"}

// Bad
{"elementId": "menu_123"}
```

### 2. Define Presentation Order Gaps

```json
// Good - allows insertion
{"presentationOrder": 10}
{"presentationOrder": 20}
{"presentationOrder": 30}

// Bad - hard to insert between
{"presentationOrder": 1}
{"presentationOrder": 2}
{"presentationOrder": 3}
```

### 3. Document Custom i18n Keys

If using custom `displayKey` values, document them in your implementation guide.

### 4. Version Control Your Config

Track `menu_seed.json` in version control for your implementation.

### 5. Test Before Production

Seed menus in a test environment before production deployment.

---

## Integration with UI Menu Configuration

**Menu Seeding** and **UI Menu Configuration** work together:

1. **Seeding** creates the initial menu structure (startup)
2. **UI Configuration** allows runtime customization
   (`/administration#globalMenuManagement`)
3. Changes made via UI are persisted to the database
4. Re-seeding does **not** overwrite UI changes (idempotent by `elementId`)

**Workflow**:

```
Startup → Seed menus → UI customization → Database persistence
         (if not exist)   (anytime)         (ongoing)
```

---

## API Endpoints

- `GET /rest/menu` - Retrieve full menu tree
- `GET /rest/menu/{elementId}` - Retrieve specific menu
- `POST /rest/menu` - Update menu tree (bulk)
- `POST /rest/menu/{elementId}` - Update specific menu

After seeding, menus are accessible via these endpoints.

---

## Migration Guide

### Migrating from Liquibase Menu Creation

**Before** (Liquibase changeset):

```xml
<insert tableName="menu" schemaName="clinlims">
    <column name="id" valueSequenceNext="menu_seq"/>
    <column name="element_id" value="menu_custom"/>
    <column name="display_key" value="custom.menu.label"/>
    <column name="action_url" value="/CustomPage"/>
    <column name="presentation_order" value="5"/>
    <column name="is_active" value="true"/>
</insert>
```

**After** (`menu_seed.json`):

```json
{
  "menus": [
    {
      "elementId": "menu_custom",
      "displayKey": "custom.menu.label",
      "actionURL": "/CustomPage",
      "presentationOrder": 5,
      "isActive": true
    }
  ]
}
```

**Benefits**:

- ✅ Easier to maintain (JSON vs XML)
- ✅ No Liquibase changesets needed
- ✅ Idempotent (safe to re-run)
- ✅ Implementation-specific (not in core codebase)

---

## Support

For questions or issues:

1. Check logs: `MenuSeedService` entries
2. Verify configuration properties
3. Review this documentation
4. Consult OpenELIS Global 2 community

---

## File Locations

| File                     | Default Location                                       | Purpose           |
| ------------------------ | ------------------------------------------------------ | ----------------- |
| `menu_config.json`       | `/var/lib/openelis-global/menu/menu_config.json`       | Runtime filtering |
| `menu_seed.json`         | `/var/lib/openelis-global/menu/menu_seed.json`         | Startup seeding   |
| `menu_seed.json.example` | `/var/lib/openelis-global/menu/menu_seed.json.example` | Example template  |

---

**OpenELIS Global Version**: 2.8.x+
