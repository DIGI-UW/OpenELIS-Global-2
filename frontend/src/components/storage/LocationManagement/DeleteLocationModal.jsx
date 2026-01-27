import React, { useState, useEffect, useContext } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  Checkbox,
  InlineNotification,
  Tooltip,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServerV2,
  hasRole,
  Roles,
  deleteFromOpenElisServerFullResponse,
} from "../../utils/Utils";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import "./DeleteLocationModal.css";

/**
 * Modal for deleting location entities (Room, Device, Shelf, Rack)
 * Checks constraints before showing confirmation dialog
 * Displays error message if constraints exist, or confirmation dialog if no constraints
 *
 * Props:
 * - open: boolean - Whether modal is open
 * - location: object - Location entity data { id, name, code, type, ... }
 * - locationType: string - "room" | "device" | "shelf" | "rack"
 * - onClose: function - Callback when modal closes
 * - onDelete: function - Callback when delete is successful
 */
const DeleteLocationModal = ({
  open,
  location,
  locationType,
  onClose,
  onDelete,
}) => {
  const intl = useIntl();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  // Check for both "Global Administrator" and "Admin" roles (database may use either name)
  const isAdmin =
    hasRole(userSessionDetails, Roles.GLOBAL_ADMIN) ||
    hasRole(userSessionDetails, "Admin");
  const [constraints, setConstraints] = useState(null);
  const [isCheckingConstraints, setIsCheckingConstraints] = useState(false);
  const [confirmed, setConfirmed] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [error, setError] = useState(null);
  const [cascadeSummary, setCascadeSummary] = useState(null);

  // Check constraints when modal opens
  useEffect(() => {
    if (open && location) {
      checkConstraints();
    }
  }, [open, location]);

  // Reset state when modal closes
  useEffect(() => {
    if (!open) {
      setConstraints(null);
      setIsCheckingConstraints(false);
      setConfirmed(false);
      setIsDeleting(false);
      setError(null);
      setCascadeSummary(null);
    }
  }, [open]);

  const getLocationTypePlural = (type) => {
    const pluralMap = {
      room: "rooms",
      device: "devices",
      shelf: "shelves", // Irregular plural
      rack: "racks",
    };
    return pluralMap[type] || `${type}s`;
  };

  const checkConstraints = () => {
    setIsCheckingConstraints(true);
    setError(null);

    // Build endpoint to check constraints and admin status
    const endpoint = `/rest/storage/${getLocationTypePlural(locationType)}/${location.id}/can-delete`;

    getFromOpenElisServerV2(endpoint)
      .then((response) => {
        setIsCheckingConstraints(false);
        // Some tests may mock { status, data }, while real API returns JSON body directly.
        if (
          response &&
          (response.status === 409 || response.error || response.message)
        ) {
          // Constraints exist
          const errorMsg =
            response.message ||
            response.error ||
            response.data?.message ||
            response.data?.error ||
            "Cannot delete location";
          setConstraints({
            error:
              response.error ||
              response.data?.error ||
              "Cannot delete location",
            message: errorMsg,
          });

          // If admin and constraints exist, fetch cascade summary
          if (isAdmin && (response.isAdmin || response.data?.isAdmin)) {
            fetchCascadeSummary();
          }
        } else if (response && response.status === 200) {
          // No constraints, can delete
          setConstraints(null);
        } else {
          // Assume can delete if no error
          setConstraints(null);
        }
      })
      .catch(() => {
        setIsCheckingConstraints(false);
        // On error, assume we can check on DELETE attempt
        setConstraints(null);
      });
  };

  const fetchCascadeSummary = () => {
    const endpoint = `/rest/storage/${getLocationTypePlural(locationType)}/${location.id}/cascade-delete-summary`;
    getFromOpenElisServerV2(endpoint)
      .then((response) => {
        if (response && !response.error) {
          setCascadeSummary(response);
        }
      })
      .catch((error) => {
        // Ignore errors - summary is optional
        console.error("Error fetching cascade summary:", error);
      });
  };

  const handleDelete = () => {
    if (!confirmed || !location) return;

    setIsDeleting(true);
    setError(null);

    const endpoint = `/rest/storage/${getLocationTypePlural(locationType)}/${location.id}`;

    deleteFromOpenElisServerFullResponse(
      endpoint,
      async (response) => {
        setIsDeleting(false);

        if (response.ok) {
          // Success
          if (onDelete) {
            onDelete(location);
          }
          handleClose();
        } else if (response.status === 403) {
          // Forbidden - not admin
          const errorData = await response.json().catch(() => ({}));
          const errorMessage =
            errorData.message ||
            errorData.error ||
            intl.formatMessage({
              id: "storage.delete.admin.only",
              defaultMessage: "Only Global Administrators can delete locations",
            });
          setError(errorMessage);
        } else if (response.status === 409) {
          // Constraints exist
          const errorData = await response.json().catch(() => ({}));
          const errorMessage =
            errorData.message ||
            errorData.error ||
            "Cannot delete location due to constraints";
          setError(errorMessage);
          setConstraints({
            error: errorData.error || "Cannot delete location",
            message: errorMessage,
          });
        } else {
          // Other error
          const errorData = await response.json().catch(() => ({}));
          setError(
            errorData.message ||
              errorData.error ||
              intl.formatMessage({
                id: "storage.delete.error",
                defaultMessage: "Failed to delete location",
              }),
          );
        }
      },
      null,
    );
  };

  const handleClose = () => {
    setConstraints(null);
    setIsCheckingConstraints(false);
    setConfirmed(false);
    setIsDeleting(false);
    setError(null);
    onClose();
  };

  const locationName =
    location?.name || location?.label || location?.code || "Location";
  // Admin can delete even with constraints (after confirmation), non-admin cannot delete with constraints
  const canDelete =
    !isCheckingConstraints &&
    confirmed &&
    // If constraints exist but we don't have cascade summary, treat as not deletable
    // (prevents "blank modal" for admins when backend doesn't return cascade metadata)
    (!constraints || (isAdmin && !!cascadeSummary));

  return (
    <ComposedModal
      open={open}
      onClose={handleClose}
      size="md"
      data-testid="delete-location-modal"
    >
      <ModalHeader
        title={intl.formatMessage({
          id: "storage.delete.location",
          defaultMessage: "Delete Location",
        })}
      />
      <ModalBody>
        {isCheckingConstraints && (
          <div data-testid="delete-location-checking">
            {intl.formatMessage({
              id: "storage.delete.checking",
              defaultMessage: "Checking constraints...",
            })}
          </div>
        )}

        {error && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "storage.error",
              defaultMessage: "Error",
            })}
            subtitle={error}
            lowContrast
            onClose={() => setError(null)}
            data-testid="delete-location-error"
          />
        )}

        {/* Constraints (cannot delete) - show for non-admins and admins without cascade metadata */}
        {!isCheckingConstraints &&
          constraints &&
          (!isAdmin || !cascadeSummary) && (
            <InlineNotification
              kind="error"
              title={intl.formatMessage({
                id: "storage.delete.constraints.title",
                defaultMessage: "Cannot Delete",
              })}
              subtitle={constraints.message || constraints.error}
              lowContrast
              data-testid="delete-location-constraints-error"
            >
              <span data-testid="delete-location-constraints-message">
                {constraints.message || constraints.error}
              </span>
            </InlineNotification>
          )}

        {/* Admin cascade delete warning - only when backend provides cascade metadata */}
        {!isCheckingConstraints &&
          constraints &&
          isAdmin &&
          !!cascadeSummary && (
            <div className="delete-location-cascade-warning">
              <InlineNotification
                kind="warning"
                title={intl.formatMessage({
                  id: "storage.delete.cascade.warning",
                  defaultMessage: "Cascade Delete Warning",
                })}
                lowContrast
                data-testid="delete-location-cascade-warning"
              >
                <p data-testid="delete-location-cascade-summary">
                  {intl.formatMessage(
                    {
                      id: "storage.delete.cascade.summary",
                      defaultMessage:
                        "This will delete {childCount} {childType}(s) and unassign {sampleCount} sample(s). Are you sure you want to proceed?",
                    },
                    {
                      childCount: cascadeSummary.childLocationCount || 0,
                      childType:
                        cascadeSummary.childLocationType || "child location",
                      sampleCount: cascadeSummary.sampleCount || 0,
                    },
                  )}
                </p>
              </InlineNotification>
              <Checkbox
                id="delete-confirmation"
                data-testid="delete-location-confirmation-checkbox"
                labelText={intl.formatMessage({
                  id: "storage.delete.cascade.confirmation.checkbox",
                  defaultMessage:
                    "I confirm that I want to delete this location and all its child locations. All samples will be unassigned. This action cannot be undone.",
                })}
                checked={confirmed}
                onChange={(_, { checked }) => setConfirmed(checked)}
              />
            </div>
          )}

        {!isCheckingConstraints && !constraints && (
          <div className="delete-location-confirmation">
            <p data-testid="delete-location-warning-message">
              <span data-testid="delete-location-are-you-sure">
                {intl.formatMessage({
                  id: "storage.delete.are.you.sure",
                  defaultMessage: "Are you sure you want to delete",
                })}
              </span>{" "}
              <strong>{locationName}</strong>?{" "}
              <span data-testid="delete-location-cannot-be-undone">
                {intl.formatMessage({
                  id: "storage.delete.cannot.be.undone",
                  defaultMessage: "This action cannot be undone.",
                })}
              </span>
            </p>
            <Checkbox
              id="delete-confirmation"
              data-testid="delete-location-confirmation-checkbox"
              labelText={intl.formatMessage({
                id: "storage.delete.confirmation.checkbox",
                defaultMessage:
                  "I confirm that I want to delete this location. This action cannot be undone.",
              })}
              checked={confirmed}
              onChange={(_, { checked }) => setConfirmed(checked)}
            />
          </div>
        )}
      </ModalBody>
      <ModalFooter>
        <Button
          kind="secondary"
          onClick={handleClose}
          disabled={isDeleting}
          data-testid="delete-location-cancel-button"
        >
          <FormattedMessage id="label.button.cancel" defaultMessage="Cancel" />
        </Button>
        {(!constraints || (constraints && isAdmin)) && (
          <Tooltip
            label={
              !isAdmin && constraints
                ? intl.formatMessage({
                    id: "storage.delete.admin.only",
                    defaultMessage:
                      "Only Global Administrators can delete locations",
                  })
                : ""
            }
            align="top"
          >
            <Button
              kind="danger"
              onClick={handleDelete}
              disabled={!canDelete || isDeleting}
              data-testid="delete-location-confirm-button"
            >
              <FormattedMessage
                id={
                  constraints && isAdmin
                    ? "storage.delete.cascade.confirm"
                    : "storage.confirm.delete"
                }
                defaultMessage={
                  constraints && isAdmin
                    ? "Confirm Cascade Delete"
                    : "Confirm Delete"
                }
              />
            </Button>
          </Tooltip>
        )}
      </ModalFooter>
    </ComposedModal>
  );
};

export default DeleteLocationModal;
