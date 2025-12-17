# Notebook Access Control UI Guide

## Overview

The notebook access control system allows admins to configure which
Organizations (Lab Units) can access specific notebook templates, and which
roles are allowed to create/edit entries.

## Backend Implementation Status ✅

### 1. Database Schema ✅

- `notebook_organizations` - Links templates to organizations
- `notebook_allowed_roles` - Defines which roles can create/edit entries
- `notebook_entry.organization_id` - Primary organization for each entry
- `notebook_entry_organizations` - Organizations with access to entries

### 2. Backend Services ✅

- `NotebookSecurityService` - Handles all access control logic
- Filters templates and entries based on user's Lab Unit (from admin panel)
- Maps Lab Unit → Organization via name/shortName/abbreviation

### 3. API Endpoints ✅

#### Get All Organizations (for dropdown)

```
GET /rest/notebook/organizations
```

**Response:**

```json
[
  {
    "id": "1",
    "name": "Immunology Laboratory",
    "shortName": "Immunology"
  },
  {
    "id": "2",
    "name": "Hematology Laboratory",
    "shortName": "Hematology"
  }
]
```

#### Create Template (with organizations)

```
POST /rest/notebook/create
Content-Type: application/json

{
  "title": "Immunology Workflow",
  "objective": "...",
  "isTemplate": true,
  "organizationIds": ["1", "2"],  // Array of organization IDs
  "allowedRoles": ["LAB_TECH", "SUPERVISOR"]  // Array of role names
}
```

#### Update Template

```
POST /rest/notebook/update/{noteBookId}
Content-Type: application/json

{
  "id": 123,
  "title": "Immunology Workflow",
  "organizationIds": ["1", "2"],
  "allowedRoles": ["LAB_TECH", "SUPERVISOR"]
}
```

### 4. Security Checks ✅

- Only admins can create/edit templates
- Users see only templates for their Lab Unit
- Entry creation requires allowed role for user's Lab Unit
- Entries inherit accessible organizations from template

## Frontend UI Required 🔨

### 1. Notebook Template Configuration Component

You need to add dropdowns to the notebook template creation/edit form. Here's a
sample React component:

```jsx
import React, { useState, useEffect } from "react";
import { MultiSelect, FormLabel, Stack } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";

const NotebookTemplateAccessControl = ({
  selectedOrganizationIds = [],
  selectedRoles = [],
  onOrganizationsChange,
  onRolesChange,
}) => {
  const intl = useIntl();
  const [organizations, setOrganizations] = useState([]);
  const [roles, setRoles] = useState([]);

  useEffect(() => {
    // Fetch organizations for dropdown
    getFromOpenElisServer("/rest/notebook/organizations", (response) => {
      const orgItems = response.map((org) => ({
        id: org.id,
        label: org.name,
        value: org.id,
      }));
      setOrganizations(orgItems);
    });

    // Fetch available roles (you may need to add this endpoint)
    getFromOpenElisServer("/rest/role", (response) => {
      const roleItems = response.map((role) => ({
        id: role.id,
        label: role.displayName,
        value: role.name,
      }));
      setRoles(roleItems);
    });
  }, []);

  return (
    <Stack gap={5}>
      <div>
        <FormLabel>
          <FormattedMessage id="notebook.template.organizations" />
        </FormLabel>
        <MultiSelect
          id="template-organizations"
          label={intl.formatMessage({
            id: "notebook.template.organizations.placeholder",
          })}
          items={organizations}
          initialSelectedItems={organizations.filter((org) =>
            selectedOrganizationIds.includes(org.id)
          )}
          onChange={({ selectedItems }) => {
            const orgIds = selectedItems.map((item) => item.id);
            onOrganizationsChange(orgIds);
          }}
          itemToString={(item) => (item ? item.label : "")}
          selectionFeedback="top-after-reopen"
        />
        <div
          style={{
            fontSize: "0.75rem",
            marginTop: "0.25rem",
            color: "#525252",
          }}
        >
          <FormattedMessage id="notebook.template.organizations.help" />
        </div>
      </div>

      <div>
        <FormLabel>
          <FormattedMessage id="notebook.template.allowedRoles" />
        </FormLabel>
        <MultiSelect
          id="template-roles"
          label={intl.formatMessage({
            id: "notebook.template.allowedRoles.placeholder",
          })}
          items={roles}
          initialSelectedItems={roles.filter((role) =>
            selectedRoles.includes(role.value)
          )}
          onChange={({ selectedItems }) => {
            const roleValues = selectedItems.map((item) => item.value);
            onRolesChange(roleValues);
          }}
          itemToString={(item) => (item ? item.label : "")}
          selectionFeedback="top-after-reopen"
        />
        <div
          style={{
            fontSize: "0.75rem",
            marginTop: "0.25rem",
            color: "#525252",
          }}
        >
          <FormattedMessage id="notebook.template.allowedRoles.help" />
        </div>
      </div>
    </Stack>
  );
};

export default NotebookTemplateAccessControl;
```

### 2. Integration Example

Add to your notebook template form:

```jsx
import NotebookTemplateAccessControl from "./NotebookTemplateAccessControl";

const NotebookTemplateForm = () => {
  const [formData, setFormData] = useState({
    title: "",
    objective: "",
    isTemplate: true,
    organizationIds: [],
    allowedRoles: [],
  });

  const handleSave = async () => {
    const response = await fetch("/rest/notebook/create", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(formData),
    });
    // Handle response...
  };

  return (
    <Form>
      {/* Other form fields... */}

      <NotebookTemplateAccessControl
        selectedOrganizationIds={formData.organizationIds}
        selectedRoles={formData.allowedRoles}
        onOrganizationsChange={(orgIds) =>
          setFormData({ ...formData, organizationIds: orgIds })
        }
        onRolesChange={(roles) =>
          setFormData({ ...formData, allowedRoles: roles })
        }
      />

      <Button onClick={handleSave}>
        <FormattedMessage id="button.save" />
      </Button>
    </Form>
  );
};
```

### 3. Required Translation Keys

Add to `frontend/src/languages/en.json`:

```json
{
  "notebook.template.organizations": "Accessible Organizations",
  "notebook.template.organizations.placeholder": "Select organizations that can use this template",
  "notebook.template.organizations.help": "Select which lab units/organizations can see and use this notebook template. Users must have their Lab Unit set in admin to one of these organizations.",
  "notebook.template.allowedRoles": "Allowed Roles",
  "notebook.template.allowedRoles.placeholder": "Select roles that can create/edit entries",
  "notebook.template.allowedRoles.help": "Only users with these roles (for their lab unit) can create or edit notebook entries from this template. Leave empty to allow all authenticated users."
}
```

## How It Works

### Admin Workflow

1. Admin creates/edits notebook template
2. Selects which Organizations can access the template via dropdown
3. Selects which Roles can create/edit entries (optional)
4. Saves template

### User Experience

1. User logs in with Lab Unit set (e.g., "Immunology")
2. System matches Lab Unit to Organization
3. User sees only templates assigned to their Organization
4. User can create entries only if they have an allowed role

### Example Scenario

**Setup:**

- Template: "Immunology Workflow"
- Organizations: ["Immunology Laboratory", "Research Lab"]
- Allowed Roles: ["LAB_TECH", "SUPERVISOR"]

**User A:**

- Lab Unit: "Immunology"
- Roles: ["LAB_TECH"]
- ✅ Can see template
- ✅ Can create/edit entries

**User B:**

- Lab Unit: "Hematology"
- Roles: ["LAB_TECH"]
- ❌ Cannot see template (wrong lab unit)

**User C:**

- Lab Unit: "Immunology"
- Roles: ["DATA_ENTRY"]
- ✅ Can see template
- ❌ Cannot create/edit entries (role not allowed)

## Testing

1. **As Admin:**
   - Create template with organizations selected
   - Verify form saves organizationIds correctly
2. **As Regular User:**

   - Login with different Lab Units
   - Verify you only see templates for your Lab Unit
   - Try creating entries with different roles

3. **Edge Cases:**
   - Template with NO organizations = visible to all (backward compatibility)
   - Template with NO roles = all users with org access can create entries
   - Multiple organizations = user sees template if ANY match their Lab Unit

## Summary

✅ **Backend Complete** - All access control logic, database schema, and API
endpoints ✅ **API Endpoint Added** - `GET /rest/notebook/organizations` for
dropdown 🔨 **Frontend Needed** - Multi-select dropdowns for organizations and
roles in template form

The implementation correctly uses the "Lab Unit" field from the admin panel user
settings!
