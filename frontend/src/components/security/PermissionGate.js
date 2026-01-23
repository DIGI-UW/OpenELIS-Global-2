import React, { cloneElement, Children } from "react";
import PropTypes from "prop-types";
import { Tooltip } from "@carbon/react";
import { usePermissions } from "../../hooks/usePermissions";

/**
 * PermissionGate - Declarative permission checking component
 *
 * Wraps children and controls their rendering based on user permissions.
 * By default (per design decision 4B), shows disabled state with tooltip
 * when user lacks permission. Can be configured to hide completely.
 *
 * @example
 * // Basic usage - disable with tooltip if no access
 * <PermissionGate
 *   roles={[Roles.GLOBAL_ADMIN, Roles.NOTEBOOK_ADMIN]}
 *   disabledTooltip="You need admin access to perform this action"
 * >
 *   <Button>Add Notebook</Button>
 * </PermissionGate>
 *
 * @example
 * // Hide completely if no access
 * <PermissionGate
 *   roles={[Roles.GLOBAL_ADMIN]}
 *   hideCompletely
 * >
 *   <AdminPanel />
 * </PermissionGate>
 *
 * @example
 * // Lab-unit specific role check
 * <PermissionGate
 *   labUnitRoles={{ "Cytology": [Roles.TECHNICIAN, Roles.SUPERVISOR] }}
 *   disabledTooltip="You need Cytology access"
 * >
 *   <CytologyWorkflow />
 * </PermissionGate>
 *
 * @example
 * // Require all roles (AND logic)
 * <PermissionGate
 *   roles={[Roles.RESULTS, Roles.VALIDATION]}
 *   requireAll
 *   disabledTooltip="You need both Results and Validation roles"
 * >
 *   <ApproveButton />
 * </PermissionGate>
 */
const PermissionGate = ({
  children,
  roles = [],
  labUnitRoles = {},
  requireAll = false,
  disabledTooltip = "You do not have permission to access this feature",
  hideCompletely = false,
  fallback = null,
}) => {
  const { hasRole, hasAnyRole, hasAllRoles, hasLabUnitRole } = usePermissions();

  /**
   * Check if user has required global roles
   */
  const checkGlobalRoles = () => {
    if (!roles || roles.length === 0) {
      return true; // No global role requirement
    }
    return requireAll ? hasAllRoles(roles) : hasAnyRole(roles);
  };

  /**
   * Check if user has required lab-unit specific roles
   */
  const checkLabUnitRoles = () => {
    if (!labUnitRoles || Object.keys(labUnitRoles).length === 0) {
      return true; // No lab unit role requirement
    }

    // Check each lab unit's role requirements
    const results = Object.entries(labUnitRoles).map(([labUnit, unitRoles]) => {
      if (!unitRoles || unitRoles.length === 0) {
        return true;
      }
      // Check if user has any of the required roles for this lab unit
      return unitRoles.some((role) => hasLabUnitRole(labUnit, role));
    });

    // If requireAll, all lab units must have at least one matching role
    // Otherwise, at least one lab unit must match
    return requireAll ? results.every(Boolean) : results.some(Boolean);
  };

  /**
   * Determine if user has permission
   */
  const hasPermission = checkGlobalRoles() && checkLabUnitRoles();

  // If user has permission, render children normally
  if (hasPermission) {
    return <>{children}</>;
  }

  // If configured to hide completely, render fallback or nothing
  if (hideCompletely) {
    return fallback;
  }

  // Default behavior: show disabled state with tooltip
  // Clone children and add disabled prop
  const disabledChildren = Children.map(children, (child) => {
    if (!React.isValidElement(child)) {
      return child;
    }

    // Clone the child with disabled prop
    const clonedChild = cloneElement(child, {
      disabled: true,
      className: `${child.props.className || ""} permission-denied`.trim(),
      "aria-disabled": true,
      onClick: (e) => {
        e.preventDefault();
        e.stopPropagation();
      },
    });

    return clonedChild;
  });

  // Wrap with tooltip explaining why it's disabled
  return (
    <Tooltip
      label={disabledTooltip}
      align="bottom"
      className="permission-gate-tooltip"
    >
      <span
        style={{ display: "inline-block", cursor: "not-allowed" }}
        className="permission-gate-wrapper"
      >
        {disabledChildren}
      </span>
    </Tooltip>
  );
};

PermissionGate.propTypes = {
  /** Child components to render/control */
  children: PropTypes.node.isRequired,

  /** Global roles required (checked with OR logic by default) */
  roles: PropTypes.arrayOf(PropTypes.string),

  /** Lab-unit specific roles: { labUnitName: [roleNames] } */
  labUnitRoles: PropTypes.objectOf(PropTypes.arrayOf(PropTypes.string)),

  /** If true, require ALL roles instead of ANY role */
  requireAll: PropTypes.bool,

  /** Tooltip text shown when user lacks permission (used when not hiding) */
  disabledTooltip: PropTypes.string,

  /** If true, hide completely instead of showing disabled state */
  hideCompletely: PropTypes.bool,

  /** Element to render when hideCompletely is true and user lacks permission */
  fallback: PropTypes.node,
};

export default PermissionGate;
