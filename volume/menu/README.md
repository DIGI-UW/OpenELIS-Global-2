# OpenELIS Global 2 - Menu Configuration Guide

This directory contains configuration files for managing the OpenELIS menu
system.

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Menu Seeding - NEW Feature](#menu-seeding---new-feature)
3. [Menu Filtering](#menu-filtering)
4. [Quick Start Guide](#quick-start-guide)
5. [Configuration Reference](#configuration-reference)
6. [Examples](#examples)
7. [Troubleshooting](#troubleshooting)
8. [Best Practices](#best-practices)

---

## Overview

OpenELIS Global 2 supports two types of menu configuration:

1. **Menu Seeding** (`menu_seed.json`) - Automatic menu creation at startup ⭐
   NEW
2. **Menu Filtering** (`menu_config.json`) - Runtime filtering of existing menus

---

## Menu Seeding - NEW Feature

### 🎯 What is Menu Seeding?

Menu Seeding allows you to automatically create menu entries in the database at
application startup by defining them in a JSON configuration file. This
eliminates the need for:

- Manual menu configuration via UI after deployment
- Complex Liquibase changesets for menu creation
- Time-consuming setup for each new installation

### ✨ Key Features

| Feature                | Description                                                    |
| ---------------------- | -------------------------------------------------------------- |
| **Automatic Creation** | Menus are created on first application startup                 |
| **Idempotent**         | Safe to run multiple times - no duplicates created             |
| **Hierarchical**       | Supports parent-child menu relationships                       |
| **Non-Destructive**    | Only creates new menus, never updates or deletes existing ones |
| **Flexible**           | JSON-based configuration, easy to customize                    |
| **Optional**           | Disabled by default - opt-in via configuration                 |

### 🚀 Quick Setup

#### Step 1: Enable Menu Seeding

Add to `application.properties` or `common.properties`:

```properties
# Enable menu seeding from config file
org.openelisglobal.menu.seed.enabled=true

# Optional: Specify custom config path (default shown below)
org.openelisglobal.menu.seed.config.path=/var/lib/openelis-global/menu/menu_seed.json
```

#### Step 2: Create Menu Configuration

Copy the example file and customize:

```bash
cd /var/lib/openelis-global/menu/
cp menu_seed.json.example menu_seed.json
# Edit menu_seed.json with your menu structure
```

#### Step 3: Start/Restart Application

```bash
# On first startup with new menus
INFO: MenuSeedServiceImpl - Starting menu seeding from config
INFO: MenuSeedServiceImpl - Created menu: menu_home (parent: null)
INFO: MenuSeedServiceImpl - Created menu: menu_sample (parent: null)
INFO: MenuSeedServiceImpl - Menu seeding completed successfully: 18 menus created

# On subsequent restarts (idempotency)
INFO: MenuSeedServiceImpl - Starting menu seeding from config
INFO: MenuSeedServiceImpl - Menu seeding completed successfully: 0 menus created
```

### 📄 Menu Seed Configuration Format

**File:** `menu_seed.json`

**Basic Structure:**

```json
{
  "menus": [
    {
      "elementId": "menu_unique_id",
      "displayKey": "i18n.key.for.label",
      "toolTipKey": "i18n.key.for.tooltip",
      "actionURL": "/PagePath",
      "clickAction": "javascriptFunction()",
      "presentationOrder": 1,
      "isActive": true,
      "openInNewWindow": false,
      "hideInOldUI": false,
      "childMenus": []
    }
  ]
}
```

**Field Reference:**

| Field               | Type    | Required | Default | Description                                      |
| ------------------- | ------- | -------- | ------- | ------------------------------------------------ |
| `elementId`         | String  | ✅ Yes   | -       | Unique identifier (used to check if menu exists) |
| `displayKey`        | String  | ✅ Yes   | -       | i18n key for menu label                          |
| `toolTipKey`        | String  | No       | null    | i18n key for tooltip                             |
| `actionURL`         | String  | No       | ""      | URL or path to navigate to                       |
| `clickAction`       | String  | No       | null    | JavaScript function to execute                   |
| `presentationOrder` | Integer | ✅ Yes   | -       | Sort order (lower numbers first)                 |
| `isActive`          | Boolean | No       | true    | Initial visibility state                         |
| `openInNewWindow`   | Boolean | No       | false   | Open link in new window/tab                      |
| `hideInOldUI`       | Boolean | No       | false   | Hide in legacy UI                                |
| `childMenus`        | Array   | No       | []      | Array of child menu definitions                  |

### 🔄 How Menu Seeding Works

1. **Application Startup** → `MenuSeedService` is initialized via
   `@PostConstruct`
2. **Check if Enabled** → Reads `org.openelisglobal.menu.seed.enabled` property
3. **Load Configuration** → Parses `menu_seed.json` file
4. **Check Existing Menus** → For each menu, checks if `elementId` already
   exists in database
5. **Create New Menus** → Creates only menus that don't exist
6. **Process Hierarchies** → Recursively creates parent-child relationships
7. **Rebuild Cache** → Forces menu cache rebuild to include new menus
8. **Complete** → Logs number of menus created

**Important:** Menu seeding is **idempotent** - running it multiple times will
NOT create duplicates!

### 📝 Example: Complete Menu Structure

```json
{
  "menus": [
    {
      "elementId": "menu_home",
      "displayKey": "banner.menu.home",
      "toolTipKey": "banner.menu.home.tooltip",
      "actionURL": "/Dashboard",
      "presentationOrder": 1,
      "isActive": true
    },
    {
      "elementId": "menu_sample",
      "displayKey": "banner.menu.sample",
      "actionURL": "",
      "presentationOrder": 2,
      "isActive": true,
      "childMenus": [
        {
          "elementId": "menu_sample_add",
          "displayKey": "banner.menu.sample.add",
          "actionURL": "/SampleAdd",
          "presentationOrder": 1,
          "isActive": true
        },
        {
          "elementId": "menu_sample_edit",
          "displayKey": "banner.menu.sample.edit",
          "actionURL": "/SampleEdit",
          "presentationOrder": 2,
          "isActive": true
        }
      ]
    },
    {
      "elementId": "menu_reports",
      "displayKey": "banner.menu.reports",
      "actionURL": "",
      "presentationOrder": 5,
      "isActive": true,
      "childMenus": [
        {
          "elementId": "menu_reports_routine",
          "displayKey": "banner.menu.reports.routine",
          "actionURL": "/ReportsMenu",
          "presentationOrder": 1,
          "isActive": true
        }
      ]
    }
  ]
}
```

### 🛡️ Safety Features

✅ **JSON Parsing Safety** - Ignores unknown fields (e.g., comments)  
✅ **Validation** - Skips menus with missing `elementId`  
✅ **Transaction Safety** - Menu creation is transactional  
✅ **Error Handling** - Errors logged, but don't stop application startup  
✅ **Idempotency** - Safe to run multiple times  
✅ **Non-Destructive** - Never modifies or deletes existing menus

---

## Menu Filtering

### 📌 What is Menu Filtering?

Menu Filtering allows you to show/hide existing menu items at runtime without
modifying the database.

**When to use:**

- Hide specific menus for certain implementations
- Show only a subset of available menus
- Runtime menu visibility control

**Requirements:**

- Set `org.openelisglobal.menu.configuration.autocreate=true`
- Menus must already exist in the database (use Menu Seeding to create them
  first!)

### Configuration Format

**File:** `menu_config.json`

**Whitelist Mode (includes):**

```json
{
  "includes": [
    {
      "elementId": "menu_home",
      "childMenus": []
    },
    {
      "elementId": "menu_sample",
      "childMenus": [
        {
          "elementId": "menu_sample_add",
          "childMenus": []
        }
      ]
    }
  ]
}
```

**Blacklist Mode (excludes):**

```json
{
  "excludes": [
    {
      "elementId": "menu_admin",
      "childMenus": []
    }
  ]
}
```

---

## Quick Start Guide

### Scenario 1: New Installation with Custom Menus

**Goal:** Deploy OpenELIS with a custom menu structure

```bash
# 1. Enable menu seeding
echo "org.openelisglobal.menu.seed.enabled=true" >> application.properties

# 2. Create menu configuration
cd /var/lib/openelis-global/menu/
cp menu_seed.json.example menu_seed.json
nano menu_seed.json  # Edit to define your menus

# 3. Start application
# Menus will be created automatically on first startup

# 4. (Optional) Filter menus at runtime
cp menu_config.json.example menu_config.json
nano menu_config.json  # Define which menus to show/hide
```

### Scenario 2: Existing Installation - Add New Menus

**Goal:** Add new menus to an existing OpenELIS installation

```bash
# 1. Edit menu_seed.json to add new menu entries
nano /var/lib/openelis-global/menu/menu_seed.json

# 2. Add new menu definitions
# Example:
{
  "elementId": "menu_new_feature",
  "displayKey": "banner.menu.new.feature",
  "actionURL": "/NewFeature",
  "presentationOrder": 100,
  "isActive": true
}

# 3. Restart application
# Only NEW menus will be created (existing menus preserved)

# 4. Verify in logs
grep "MenuSeedServiceImpl" /var/log/tomcat/catalina.out
# Look for: "Created menu: menu_new_feature"
```

### Scenario 3: Implementation-Specific Menu Structure

**Goal:** Different menu structures for different implementations

```bash
# Implementation A (Full menus)
cp menu_seed.full.json menu_seed.json

# Implementation B (Clinic menus only)
cp menu_seed.clinic.json menu_seed.json

# Implementation C (Lab menus only)
cp menu_seed.lab.json menu_seed.json
```

---

## Configuration Reference

### Application Properties

```properties
# ===== Menu Seeding =====
# Enable/disable menu seeding at startup
org.openelisglobal.menu.seed.enabled=false

# Path to menu seed configuration file
org.openelisglobal.menu.seed.config.path=/var/lib/openelis-global/menu/menu_seed.json

# ===== Menu Filtering =====
# Enable/disable menu filtering at runtime
org.openelisglobal.menu.configuration.autocreate=false
```

### File Locations

| File                     | Default Location                                       | Purpose                             |
| ------------------------ | ------------------------------------------------------ | ----------------------------------- |
| `menu_seed.json`         | `/var/lib/openelis-global/menu/menu_seed.json`         | Menu creation configuration         |
| `menu_seed.json.example` | `/var/lib/openelis-global/menu/menu_seed.json.example` | Example template with documentation |
| `menu_config.json`       | `/var/lib/openelis-global/menu/menu_config.json`       | Menu filtering configuration        |
| `README.md`              | `/var/lib/openelis-global/menu/README.md`              | This documentation                  |

---

## Examples

### Example 1: Minimal Menu Structure

```json
{
  "menus": [
    {
      "elementId": "menu_home",
      "displayKey": "banner.menu.home",
      "actionURL": "/Dashboard",
      "presentationOrder": 1,
      "isActive": true
    },
    {
      "elementId": "menu_sample_add",
      "displayKey": "banner.menu.sample.add",
      "actionURL": "/SampleAdd",
      "presentationOrder": 2,
      "isActive": true
    },
    {
      "elementId": "menu_results",
      "displayKey": "banner.menu.results",
      "actionURL": "/Results",
      "presentationOrder": 3,
      "isActive": true
    }
  ]
}
```

### Example 2: Hierarchical Menu (3 Levels)

```json
{
  "menus": [
    {
      "elementId": "menu_laboratory",
      "displayKey": "banner.menu.laboratory",
      "actionURL": "",
      "presentationOrder": 1,
      "isActive": true,
      "childMenus": [
        {
          "elementId": "menu_lab_sample",
          "displayKey": "banner.menu.lab.sample",
          "actionURL": "",
          "presentationOrder": 1,
          "isActive": true,
          "childMenus": [
            {
              "elementId": "menu_lab_sample_create",
              "displayKey": "banner.menu.lab.sample.create",
              "actionURL": "/SamplePatientEntry",
              "presentationOrder": 1,
              "isActive": true
            },
            {
              "elementId": "menu_lab_sample_edit",
              "displayKey": "banner.menu.lab.sample.edit",
              "actionURL": "/SampleEdit",
              "presentationOrder": 2,
              "isActive": true
            }
          ]
        },
        {
          "elementId": "menu_lab_results",
          "displayKey": "banner.menu.lab.results",
          "actionURL": "/ResultsEntry",
          "presentationOrder": 2,
          "isActive": true
        }
      ]
    }
  ]
}
```

### Example 3: Menu with External Link

```json
{
  "menus": [
    {
      "elementId": "menu_external_help",
      "displayKey": "banner.menu.help.external",
      "toolTipKey": "banner.menu.help.external.tooltip",
      "actionURL": "https://docs.openelis.org",
      "presentationOrder": 99,
      "isActive": true,
      "openInNewWindow": true
    }
  ]
}
```

### Example 4: Combined Seeding + Filtering

**Step 1: Seed all possible menus** (`menu_seed.json`):

```json
{
  "menus": [
    {"elementId": "menu_lab", ...},
    {"elementId": "menu_clinic", ...},
    {"elementId": "menu_admin", ...}
  ]
}
```

**Step 2: Filter to show only clinic menus** (`menu_config.json`):

```json
{
  "includes": [{ "elementId": "menu_clinic", "childMenus": [] }]
}
```

---

## Troubleshooting

### Issue: Menus Not Created

**Check:**

1. ✅ Is `org.openelisglobal.menu.seed.enabled=true` in properties?
2. ✅ Does `/var/lib/openelis-global/menu/menu_seed.json` exist?
3. ✅ Check logs for errors: `grep "MenuSeedServiceImpl" catalina.out`
4. ✅ Verify JSON syntax is valid: `cat menu_seed.json | python -m json.tool`
5. ✅ Ensure `elementId` is unique and not empty

**Common Errors:**

```
ERROR: Error reading menu seed config file
→ Check file path and JSON syntax

WARN: Skipping menu with missing elementId
→ Add elementId field to menu definition

DEBUG: Menu already exists, skipping
→ Expected behavior (idempotency)
```

### Issue: Menu Created But Not Visible

**Check:**

1. ✅ Is `isActive` set to `true`?
2. ✅ Is menu being filtered by `menu_config.json`?
3. ✅ Check `presentationOrder` value (valid range)
4. ✅ Verify i18n key exists for `displayKey`
5. ✅ Clear browser cache and reload

### Issue: Duplicate Menus Created

**Cause:** Menu seeding should NEVER create duplicates (idempotency)

**If this happens:**

1. Check if `elementId` changed between runs
2. Verify database has unique constraint on `element_id`
3. Report as bug - this should not happen

### Issue: JSON Parsing Errors

**Error:** `Unrecognized field`

**Solution:** This shouldn't happen as we ignore unknown fields, but if it does:

- Remove metadata fields (`_comment`, `_description`, etc.) from production
  `menu_seed.json`
- Keep those only in `menu_seed.json.example`

---

## Best Practices

### 1. Use Semantic Element IDs

```json
// ✅ Good
{"elementId": "menu_patient_registration"}

// ❌ Bad
{"elementId": "menu_123"}
```

### 2. Define Presentation Order with Gaps

```json
// ✅ Good - allows insertion between items
{"presentationOrder": 10}
{"presentationOrder": 20}
{"presentationOrder": 30}

// ❌ Bad - hard to insert between
{"presentationOrder": 1}
{"presentationOrder": 2}
{"presentationOrder": 3}
```

### 3. Document Custom i18n Keys

If using custom `displayKey` values, document them in your implementation guide:

```
i18n keys needed for custom menus:
- banner.menu.custom.feature → "Custom Feature"
- banner.menu.custom.feature.tooltip → "Access custom feature module"
```

### 4. Version Control Your Configuration

```bash
# Track menu_seed.json in version control for your implementation
git add menu_seed.json
git commit -m "feat: add laboratory workflow menus"
```

### 5. Test in Development First

```bash
# 1. Test in dev environment
org.openelisglobal.menu.seed.enabled=true

# 2. Verify menus created correctly
# Check logs, check database, test UI

# 3. Deploy to production
```

### 6. Keep Production Config Clean

- Use `menu_seed.json.example` for documentation and comments
- Keep production `menu_seed.json` clean (no metadata fields)
- Full documentation is in this README

### 7. Use Descriptive Tooltips

```json
{
  "elementId": "menu_sample_batch",
  "displayKey": "banner.menu.sample.batch",
  "toolTipKey": "banner.menu.sample.batch.tooltip", // ✅ Helpful for users
  "actionURL": "/BatchSampleEntry"
}
```

---

## Integration with UI Menu Configuration

**Menu Seeding** and **UI Menu Configuration** work together:

1. **Seeding** creates the initial menu structure (startup, one-time)
2. **UI Configuration** allows runtime customization
   (`/admin#globalMenuManagement`)
3. Changes made via UI are persisted to the database
4. Re-seeding does **not** overwrite UI changes (idempotent by `elementId`)

**Workflow:**

```
Startup → Seed menus → UI customization → Database persistence
         (if not exist)   (anytime)         (ongoing)
```

---

## Migration Guide

### From Liquibase Menu Creation

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

**Benefits:**

- ✅ Easier to maintain (JSON vs XML)
- ✅ No Liquibase changesets needed
- ✅ Idempotent (safe to re-run)
- ✅ Implementation-specific (not in core codebase)
- ✅ Can be version controlled per implementation

---

## API Endpoints

After seeding, menus are accessible via REST API:

- `GET /rest/menu` - Retrieve full menu tree
- `GET /rest/menu/{elementId}` - Retrieve specific menu
- `POST /rest/menu` - Update menu tree (bulk)
- `POST /rest/menu/{elementId}` - Update specific menu

---

## Technical Details

### Architecture

```
MenuSeedService (Interface)
    ↓
MenuSeedServiceImpl (@Service, @PostConstruct)
    ↓
MenuSeedConfig (DTO with @JsonIgnoreProperties)
    ↓
MenuService (Database operations)
```

### Execution Flow

1. Application starts
2. `@PostConstruct` triggers `seedMenusFromConfig()`
3. Check if seeding enabled
4. Parse `menu_seed.json`
5. For each menu in config:
   - Check if `elementId` exists in database
   - If not exists: create menu
   - If exists: skip (idempotency)
   - Process child menus recursively
6. Rebuild menu cache
7. Log completion

### Performance

- **Startup Impact:** < 1 second for typical menu structures (10-50 menus)
- **Database Operations:** One SELECT per menu to check existence, one INSERT
  per new menu
- **Memory:** Minimal - config file loaded once, parsed, then released

---

## Support & Troubleshooting

### Logging

Menu seeding logs are prefixed with `MenuSeedServiceImpl`:

```bash
# View menu seeding logs
grep "MenuSeedServiceImpl" /var/log/tomcat/catalina.out

# Common log messages
INFO  - Starting menu seeding from config: /var/lib/openelis-global/menu/menu_seed.json
INFO  - Created menu: menu_home (parent: null)
INFO  - Menu already exists, skipping: menu_sample
INFO  - Menu seeding completed successfully: 5 menus created
ERROR - Error reading menu seed config file: [error details]
```

### Validation

Verify menu seeding worked:

```sql
-- Connect to database
psql -U clinlims -d clinlims

-- Check menus created
SELECT element_id, display_key, action_url, presentation_order, is_active
FROM clinlims.menu
WHERE element_id LIKE 'menu_%'
ORDER BY presentation_order;

-- Count total menus
SELECT COUNT(*) FROM clinlims.menu;

-- Check parent-child relationships
SELECT
    parent.element_id as parent_menu,
    child.element_id as child_menu,
    child.display_key
FROM clinlims.menu child
LEFT JOIN clinlims.menu parent ON child.parent_id = parent.id
WHERE child.parent_id IS NOT NULL
ORDER BY parent.element_id, child.presentation_order;
```

---

## FAQ

**Q: Will menu seeding overwrite my manual menu changes?**  
A: No. Menu seeding is non-destructive and idempotent. It only creates menus
that don't exist (by `elementId`). Existing menus are never modified or deleted.

**Q: Can I disable menu seeding after initial setup?**  
A: Yes. Set `org.openelisglobal.menu.seed.enabled=false` in properties. Existing
menus remain in the database.

**Q: What happens if I change a menu's `elementId` in the config?**  
A: The old menu remains in the database, and a new menu with the new `elementId`
will be created. Consider this a new menu.

**Q: Can I seed menus and use menu filtering together?**  
A: Yes! Seed all possible menus first, then use `menu_config.json` to show/hide
specific menus per implementation.

**Q: How do I remove a menu created by seeding?**  
A: Use the UI Menu Configuration (`/administration#globalMenuManagement`) or
manually delete from database. Menu seeding never deletes menus.

**Q: What if my JSON has syntax errors?**  
A: Application will log the error and continue startup without seeding. Check
logs for parsing errors.

**Q: Can I update menu properties by changing the JSON?**  
A: No. Menu seeding only creates new menus. To update existing menus, use the UI
or database directly.

---

## Version History

- **v2.8.0** - Menu Seeding feature introduced
  - MenuSeedService implementation
  - Dataset-driven testing
  - JSON-based configuration
  - Idempotent menu creation
  - Hierarchical menu support

---

## Contributing

When adding new menu types or improving menu seeding:

1. Update this README with new examples
2. Add test cases to `MenuSeedServiceTest.java`
3. Update `menu_seed.json.example` with new menu examples
4. Document any new i18n keys needed

---

## Resources

- **OpenELIS Global Documentation:** https://docs.openelis-global.org
- **Menu Configuration UI:** `/admin#globalMenuManagement`
- **Source Code:**
  `src/main/java/org/openelisglobal/menu/service/MenuSeedService*.java`
- **Tests:**
  `src/test/java/org/openelisglobal/menu/service/MenuSeedServiceTest.java`

---

**OpenELIS Global Version:** 2.8.x+  
**Last Updated:** December 2025  
**Feature Status:** ✅ Production Ready
