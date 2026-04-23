# Data Model: Unified Application Navigation

**Feature**: 016-unified-app-navigation **Date**: 2026-04-06

## Entities

### Menu (Existing — Extended)

**Package**: `org.openelisglobal.menu.valueholder` **Table**: `menu`

#### Existing Fields

| Field             | Type               | DB Column          | Notes                     |
| ----------------- | ------------------ | ------------------ | ------------------------- |
| id                | String             | id                 | PK, from BaseObject       |
| parent            | Menu (ValueHolder) | parent_id          | Self-referential FK       |
| presentationOrder | int                | presentation_order | Sort order within parent  |
| elementId         | String             | element_id         | Unique element identifier |
| actionURL         | String             | action_url         | Navigation target URL     |
| clickAction       | String             | click_action       | JS click handler (legacy) |
| displayKey        | String             | display_key        | React Intl message key    |
| toolTipKey        | String             | tool_tip_key       | React Intl tooltip key    |
| openInNewWindow   | boolean            | open_in_new_window | Opens in new tab          |
| isActive          | boolean            | is_active          | Visibility toggle         |
| hideInOldUI       | boolean            | hide_in_old_ui     | Legacy UI visibility      |

#### New Fields (M2 — Liquibase Migration)

| Field        | Type   | DB Column     | Default        | Notes                                            |
| ------------ | ------ | ------------- | -------------- | ------------------------------------------------ |
| iconName     | String | icon_name     | NULL           | Maps to @carbon/icons-react component name       |
| requiredRole | String | required_role | NULL           | Role required to see this menu item (NULL = all) |
| fhirUuid     | UUID   | fhir_uuid     | NULL           | FHIR resource UUID for external integration      |
| configSource | String | config_source | 'distribution' | Provenance: 'distribution' or 'admin'            |

#### Validation Rules

- `elementId` MUST be unique across all menu items
- `displayKey` MUST correspond to an existing React Intl message key
- `iconName` MUST match a key in `MenuIconRegistry.js` (validated at load time)
- `requiredRole` MUST match a role name in the `system_role` table (or NULL for
  unrestricted)
- `configSource` MUST be one of: `'distribution'`, `'admin'`
- `fhirUuid` is auto-generated on insert if NULL

#### Relationships

```
Menu (self-referential)
  └── parent_id → Menu.id  (many-to-one, nullable for root items)
```

### MenuItem (Existing — Unchanged)

**Package**: `org.openelisglobal.menu.util` **Type**: POJO (not persisted)

A tree-structure wrapper around `Menu` used for API responses.

| Field      | Type           | Notes                      |
| ---------- | -------------- | -------------------------- |
| menu       | Menu           | The underlying menu entity |
| childMenus | List<MenuItem> | Ordered child items        |

### Menu Configuration JSON Schema

**File**: `configuration/menus/menus.json` **Type**: Configuration file (not
persisted directly)

```json
{
  "menus": [
    {
      "elementId": "admin",
      "displayKey": "sidenav.label.admin",
      "iconName": "Settings",
      "requiredRole": "GLOBAL_ADMIN",
      "actionURL": "/admin",
      "presentationOrder": 1,
      "isActive": true,
      "children": [
        {
          "elementId": "admin-userManagement",
          "displayKey": "sidenav.label.admin.userManagement",
          "iconName": "User",
          "requiredRole": "GLOBAL_ADMIN",
          "actionURL": "/admin/userManagement",
          "presentationOrder": 1,
          "isActive": true
        }
      ]
    }
  ]
}
```

## State Transitions

### Menu Item Config Source

```
[JSON Config File] --load on startup--> config_source = 'distribution'
                                              |
                                    [Admin edits via UI]
                                              |
                                              v
                                   config_source = 'admin'
                                              |
                                    [Admin clicks "Reset"]
                                              |
                                              v
                                   config_source = 'distribution'
                                   (reverts to distribution value)
```

### Config Reload Behavior

```
On startup / config reload:
  1. Read JSON config files (classpath or filesystem)
  2. Compute checksum of each file
  3. If checksum unchanged → skip (no DB writes)
  4. If checksum changed:
     a. For each menu item in JSON:
        - If DB row exists with config_source = 'distribution' → UPDATE
        - If DB row exists with config_source = 'admin' → SKIP (preserve admin override)
        - If no DB row exists → INSERT with config_source = 'distribution'
     b. Menu items in DB with config_source = 'distribution' NOT in JSON → DELETE
```

## Liquibase Migration

**Changeset ID**: `2026-04-menu-navigation-enhancements`

```xml
<changeSet id="2026-04-menu-navigation-enhancements" author="openelis">
    <addColumn tableName="menu">
        <column name="icon_name" type="VARCHAR(100)"/>
        <column name="required_role" type="VARCHAR(100)"/>
        <column name="fhir_uuid" type="UUID"/>
        <column name="config_source" type="VARCHAR(20)" defaultValue="distribution">
            <constraints nullable="false"/>
        </column>
    </addColumn>
    <createIndex tableName="menu" indexName="idx_menu_config_source">
        <column name="config_source"/>
    </createIndex>
    <createIndex tableName="menu" indexName="idx_menu_fhir_uuid" unique="true">
        <column name="fhir_uuid"/>
    </createIndex>
</changeSet>
```
