# Roles Configuration

This directory contains role configuration files in CSV format. Role entries are
loaded automatically during application initialization.

**Note:** Example files are located in the `examples/` subdirectory and are NOT
automatically loaded. Copy them to this directory to use them.

## File Format

Each CSV file should contain a header row followed by role entries with the
following columns:

### Required Columns

- **name**: The unique name of the role (e.g., "Lab Technician", "Results
  Validator")

### Optional Columns

- **description**: Detailed description of the role's purpose
- **displayKey**: Internationalization key for localized display names (e.g.,
  "role.lab.technician")
- **active**: Whether the role is active ("Y" or "N", defaults to "Y")
- **editable**: Whether the role can be edited in the UI ("Y" or "N", defaults
  to "Y")
- **isGroupingRole**: Whether this is a grouping/parent role ("Y" or "N",
  defaults to "N")
- **groupingParent**: Name of the parent role for hierarchical organization
  (optional)

## Example

```csv
name,description,displayKey,active,editable,isGroupingRole,groupingParent
Lab Staff,Grouping role for all laboratory staff,role.lab.staff,Y,N,Y,
Lab Technician,Basic laboratory technician role,role.lab.technician,Y,Y,N,Lab Staff
Results Validator,Can validate and approve test results,role.results.validator,Y,Y,N,Lab Staff
Sample Collector,Authorized to collect patient samples,role.sample.collector,Y,Y,N,Lab Staff
```

## CSV Format Notes

- First line must be the header with column names
- Column names are case-insensitive
- If a value contains commas, wrap it in double quotes: `"Value, with comma"`
- Empty lines are ignored
- Only the `name` column is required

## How It Works

1. Configuration files are loaded from:

   - Classpath: `src/main/resources/configuration/roles/*.csv`
   - Filesystem: `/var/lib/openelis-global/configuration/backend/roles/*.csv`
     (mapped from `./configuration/backend/roles/` in Docker)

2. Files are only reprocessed when their content changes (tracked by checksum)

3. If a role already exists with the same name, it will be updated with the new
   values from the CSV

4. Each row in the CSV creates or updates one role

## Field Details

### name

- **Required**: Yes
- **Type**: String
- **Unique**: Yes (within the system)
- **Purpose**: The primary identifier and display name for the role

### description

- **Required**: No
- **Type**: String
- **Default**: If not provided, uses the role name
- **Purpose**: Provides additional context about the role's responsibilities

### displayKey

- **Required**: No
- **Type**: String (i18n key)
- **Default**: None
- **Purpose**: Enables localized display names using React Intl. For example,
  `role.lab.technician` can map to "Técnico de Laboratorio" in Spanish

### active

- **Required**: No
- **Type**: Boolean (Y/N or true/false)
- **Default**: Y (true)
- **Purpose**: Controls whether the role appears in the system and can be
  assigned to users

### editable

- **Required**: No
- **Type**: Boolean (Y/N or true/false)
- **Default**: Y (true)
- **Purpose**: Controls whether the role can be modified through the UI.
  System-critical roles should be set to N (false)

### isGroupingRole

- **Required**: No
- **Type**: Boolean (Y/N or true/false)
- **Default**: N (false)
- **Purpose**: Indicates if this role is a parent/container for other roles,
  used for hierarchical role organization

### groupingParent

- **Required**: No
- **Type**: String (role name)
- **Default**: None
- **Purpose**: Specifies the parent role for hierarchical organization. Value
  should be the **name** of another role (will be resolved to role ID
  automatically). If the parent role doesn't exist, a warning is logged but the
  role is still created without a parent

## Examples

### Simple Roles with Minimal Fields

```csv
name,description
Lab Assistant,Entry-level laboratory assistant
Senior Technician,Experienced laboratory technician
Department Manager,Manages laboratory department
```

### Roles with All Fields

```csv
name,description,displayKey,active,editable,isGroupingRole,groupingParent
Lab Staff Group,Grouping role for all lab staff,role.group.lab.staff,Y,N,Y,
Lab Technician,Performs routine laboratory tests,role.lab.tech,Y,Y,N,Lab Staff Group
Results Validator,Validates and approves test results,role.validator,Y,Y,N,Lab Staff Group
System Administrator,Full system access,role.sys.admin,Y,N,N,
```

### Inactive or Non-Editable Roles

```csv
name,description,active,editable
Deprecated Role,Old role no longer in use,N,N
System Role,Critical system role - do not modify,Y,N
```

## Integration with Permissions

Roles created through this configuration system are automatically available for:

- User assignment (via UserRole)
- Lab unit-specific assignments (via LabUnitRoleMap)
- Permission module configuration (via SystemRoleModule)

For assigning permissions to roles, use the OpenELIS UI or database migrations.

## Best Practices

1. **Use descriptive names**: Make role names clear and self-explanatory
2. **Leverage displayKey for i18n**: Always provide displayKey for user-facing
   roles
3. **Set editable=N for system roles**: Protect critical roles from accidental
   modification
4. **Use isGroupingRole for organization**: Create logical role hierarchies for
   better management
5. **Define parent roles first**: When using groupingParent, ensure parent roles
   appear before child roles in the CSV file
6. **Document role permissions separately**: This CSV only creates roles; assign
   permissions via SystemRoleModule

## Notes

- Role names should be unique across the system
- Existing roles with the same name will be updated with new CSV values
- Changes to CSV files are detected automatically via checksum tracking
- Set `org.openelisglobal.configuration.autocreate=false` in properties to
  disable automatic loading
- Invalid rows will be logged but won't stop the processing of other rows
