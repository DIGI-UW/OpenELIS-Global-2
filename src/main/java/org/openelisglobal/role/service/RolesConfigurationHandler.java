package org.openelisglobal.role.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.role.valueholder.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handler for loading role configuration files. Supports CSV format with role
 * entries.
 *
 * Expected CSV format:
 * name,description,displayKey,active,editable,isGroupingRole,groupingParent Lab
 * Technician,Basic laboratory technician role,role.lab.tech,Y,Y,N, Results
 * Validator,Can validate test results,role.validator,Y,Y,N,
 *
 * Notes: - First line is the header (required) - name is required field -
 * description, displayKey, active, editable, isGroupingRole, groupingParent are
 * optional - active and editable default to "Y" if not specified -
 * isGroupingRole defaults to "N" if not specified - groupingParent should be
 * the name of the parent role (will be resolved to role ID)
 */
@Component
public class RolesConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private RoleService roleService;

    @Override
    public String getDomainName() {
        return "roles";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 300; // Independent higher-level configuration
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        // Read and validate header
        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("Role configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);
        validateHeaders(headers, fileName);

        // Get column indices
        int nameIndex = findColumnIndex(headers, "name");
        int descriptionIndex = findColumnIndex(headers, "description");
        int displayKeyIndex = findColumnIndex(headers, "displayKey");
        int activeIndex = findColumnIndex(headers, "active");
        int editableIndex = findColumnIndex(headers, "editable");
        int isGroupingRoleIndex = findColumnIndex(headers, "isGroupingRole");
        int groupingParentIndex = findColumnIndex(headers, "groupingParent");

        List<Role> processedRoles = new ArrayList<>();
        String line;
        int lineNumber = 1; // Start at 1 since we already read the header

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            // Skip empty lines
            if (line.trim().isEmpty()) {
                continue;
            }

            try {
                String[] values = parseCsvLine(line);
                Role role = processCsvLine(values, nameIndex, descriptionIndex, displayKeyIndex, activeIndex,
                        editableIndex, isGroupingRoleIndex, groupingParentIndex);
                if (role != null) {
                    processedRoles.add(role);
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing line " + lineNumber + " in file " + fileName + ": " + e.getMessage());
            }
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Successfully loaded " + processedRoles.size() + " roles from " + fileName);
    }

    private String[] parseCsvLine(String line) {
        // Simple CSV parser that handles quoted fields
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(currentValue.toString().trim());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }
        values.add(currentValue.toString().trim());

        return values.toArray(new String[0]);
    }

    private void validateHeaders(String[] headers, String fileName) {
        boolean hasNameColumn = false;

        for (String header : headers) {
            if ("name".equalsIgnoreCase(header)) {
                hasNameColumn = true;
            }
        }

        if (!hasNameColumn) {
            throw new IllegalArgumentException("Role configuration file " + fileName + " must have a 'name' column");
        }
    }

    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (columnName.equalsIgnoreCase(headers[i])) {
                return i;
            }
        }
        return -1;
    }

    private Role processCsvLine(String[] values, int nameIndex, int descriptionIndex, int displayKeyIndex,
            int activeIndex, int editableIndex, int isGroupingRoleIndex, int groupingParentIndex) {

        // Get required field
        String name = getValueOrEmpty(values, nameIndex);

        if (name.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine", "Skipping row with missing name");
            return null;
        }

        // Check if role already exists
        Role existingRole = roleService.getRoleByName(name);
        if (existingRole != null) {
            // Update existing role
            updateRoleFromCsv(existingRole, values, descriptionIndex, displayKeyIndex, activeIndex, editableIndex,
                    isGroupingRoleIndex, groupingParentIndex);
            roleService.update(existingRole);
            return existingRole;
        } else {
            // Create new role
            return createRole(name, values, descriptionIndex, displayKeyIndex, activeIndex, editableIndex,
                    isGroupingRoleIndex, groupingParentIndex);
        }
    }

    private String getValueOrEmpty(String[] values, int index) {
        if (index >= 0 && index < values.length) {
            String value = values[index];
            return value != null ? value : "";
        }
        return "";
    }

    private void updateRoleFromCsv(Role role, String[] values, int descriptionIndex, int displayKeyIndex,
            int activeIndex, int editableIndex, int isGroupingRoleIndex, int groupingParentIndex) {

        // Set optional fields
        String description = getValueOrEmpty(values, descriptionIndex);
        if (!description.isEmpty()) {
            role.setDescription(description);
        }

        String displayKey = getValueOrEmpty(values, displayKeyIndex);
        if (!displayKey.isEmpty()) {
            role.setDisplayKey(displayKey);
        }

        String active = getValueOrEmpty(values, activeIndex);
        if (!active.isEmpty()) {
            role.setActive("Y".equalsIgnoreCase(active) || "true".equalsIgnoreCase(active));
        } else {
            role.setActive(true); // Default to active
        }

        String editable = getValueOrEmpty(values, editableIndex);
        if (!editable.isEmpty()) {
            role.setEditable("Y".equalsIgnoreCase(editable) || "true".equalsIgnoreCase(editable));
        } else {
            role.setEditable(true); // Default to editable
        }

        String isGroupingRole = getValueOrEmpty(values, isGroupingRoleIndex);
        if (!isGroupingRole.isEmpty()) {
            role.setGroupingRole("Y".equalsIgnoreCase(isGroupingRole) || "true".equalsIgnoreCase(isGroupingRole));
        } else {
            role.setGroupingRole(false); // Default to not a grouping role
        }

        // Set grouping parent (resolve parent role name to ID)
        String groupingParentName = getValueOrEmpty(values, groupingParentIndex);
        if (!groupingParentName.isEmpty()) {
            Role parentRole = roleService.getRoleByName(groupingParentName);
            if (parentRole != null) {
                role.setGroupingParent(parentRole.getId());
            } else {
                LogEvent.logWarn(this.getClass().getSimpleName(), "updateRoleFromCsv",
                        "Parent role '" + groupingParentName + "' not found for role '" + role.getName() + "'");
            }
        }

        // Set system user ID for audit
        role.setSysUserId("1"); // System user for configuration loading
    }

    private Role createRole(String name, String[] values, int descriptionIndex, int displayKeyIndex, int activeIndex,
            int editableIndex, int isGroupingRoleIndex, int groupingParentIndex) {

        Role role = new Role();
        role.setName(name);

        // Set description (default to name if not provided)
        String description = getValueOrEmpty(values, descriptionIndex);
        role.setDescription(!description.isEmpty() ? description : name);

        // Set displayKey (optional, for i18n)
        String displayKey = getValueOrEmpty(values, displayKeyIndex);
        if (!displayKey.isEmpty()) {
            role.setDisplayKey(displayKey);
        }

        // Set active (default to true)
        String active = getValueOrEmpty(values, activeIndex);
        if (!active.isEmpty()) {
            role.setActive("Y".equalsIgnoreCase(active) || "true".equalsIgnoreCase(active));
        } else {
            role.setActive(true);
        }

        // Set editable (default to true)
        String editable = getValueOrEmpty(values, editableIndex);
        if (!editable.isEmpty()) {
            role.setEditable("Y".equalsIgnoreCase(editable) || "true".equalsIgnoreCase(editable));
        } else {
            role.setEditable(true);
        }

        // Set isGroupingRole (default to false)
        String isGroupingRole = getValueOrEmpty(values, isGroupingRoleIndex);
        if (!isGroupingRole.isEmpty()) {
            role.setGroupingRole("Y".equalsIgnoreCase(isGroupingRole) || "true".equalsIgnoreCase(isGroupingRole));
        } else {
            role.setGroupingRole(false);
        }

        // Set grouping parent (resolve parent role name to ID)
        String groupingParentName = getValueOrEmpty(values, groupingParentIndex);
        if (!groupingParentName.isEmpty()) {
            Role parentRole = roleService.getRoleByName(groupingParentName);
            if (parentRole != null) {
                role.setGroupingParent(parentRole.getId());
            } else {
                LogEvent.logWarn(this.getClass().getSimpleName(), "createRole",
                        "Parent role '" + groupingParentName + "' not found for role '" + name + "'");
            }
        }

        role.setSysUserId("1"); // System user for configuration loading

        String roleId = roleService.insert(role);
        role = roleService.get(roleId);
        LogEvent.logInfo(this.getClass().getSimpleName(), "createRole", "Created new role: " + name);
        return role;
    }
}
