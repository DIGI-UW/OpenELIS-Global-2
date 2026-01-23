import React from "react";
import { InlineNotification } from "@carbon/react";
import { useIntl } from "react-intl";

/**
 * AccessDeniedMessage - Displays a compact message when user lacks permission
 *
 * Used when:
 * - User tries to access a page they don't have role permission for
 * - User cannot perform an action due to insufficient permissions
 * - Role-based access control blocks page/feature access
 *
 * @param {Object} props
 * @param {string} props.page - Page name (e.g., "Sample Reception & Registration")
 * @param {string} props.reason - Optional detailed reason for the denial
 * @param {string[]} props.requiredRoles - Optional array of required role names
 * @param {function} props.onGoBack - Optional callback for go back button
 * @returns {JSX.Element}
 */
export const AccessDeniedMessage = ({
  page = "Page",
  reason = null,
  requiredRoles = [],
}) => {
  const intl = useIntl();

  // Format required roles as comma-separated string
  const rolesText =
    requiredRoles && requiredRoles.length > 0 ? requiredRoles.join(", ") : null;

  // Format title as string
  const title = intl.formatMessage({
    id: "access.denied.title",
    defaultMessage: "Access Restricted",
  });

  // Format subtitle as string
  let subtitle = intl.formatMessage(
    {
      id: "access.denied.message",
      defaultMessage: "You do not have permission to access the {page} page.",
    },
    { page },
  );

  if (rolesText) {
    subtitle +=
      " " +
      intl.formatMessage(
        {
          id: "access.denied.required",
          defaultMessage: "Required roles: {roles}",
        },
        { roles: rolesText },
      );
  }

  if (reason) {
    subtitle += " " + reason;
  }

  return (
    <div style={{ padding: "1rem" }}>
      <InlineNotification
        kind="error"
        title={title}
        subtitle={subtitle}
        hideCloseButton={true}
      />
    </div>
  );
};

export default AccessDeniedMessage;
