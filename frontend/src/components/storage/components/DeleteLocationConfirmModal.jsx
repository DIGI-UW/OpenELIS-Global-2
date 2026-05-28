import React, { useEffect, useMemo, useState } from "react";
import {
  Modal,
  Checkbox,
  InlineNotification,
  InlineLoading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import useDeleteLocation from "../pages/hooks/useDeleteLocation";

const ENDPOINT_BY_TYPE = {
  room: "rooms",
  device: "devices",
  shelf: "shelves",
  rack: "racks",
  box: "boxes",
};

const CASCADE_TYPES = new Set(["room", "device", "shelf", "rack"]);

export default function DeleteLocationConfirmModal({
  isOpen,
  type,
  location,
  onClose,
  onDeleted,
}) {
  const intl = useIntl();
  const deleteLocation = useDeleteLocation();
  const [summary, setSummary] = useState(null);
  const [loadingSummary, setLoadingSummary] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState(null);
  const [confirmed, setConfirmed] = useState(false);

  const endpoint = ENDPOINT_BY_TYPE[type];
  const isCascadeDelete = CASCADE_TYPES.has(type);
  const locationId = location?.id;
  const locationLabel = location?.name || location?.label || "";

  useEffect(() => {
    if (!isOpen) {
      setSummary(null);
      setError(null);
      setLoadingSummary(false);
      setDeleting(false);
      setConfirmed(false);
      return;
    }

    setError(null);
    setSummary(null);
    setConfirmed(false);

    if (!isCascadeDelete || !endpoint || !locationId) {
      return;
    }

    setLoadingSummary(true);
    getFromOpenElisServer(
      `/rest/storage/${endpoint}/${encodeURIComponent(
        String(locationId),
      )}/cascade-delete-summary`,
      (response) => {
        if (response && !response.error) {
          setSummary(response);
        } else {
          setError(
            response?.message ||
              intl.formatMessage({
                id: "storage.delete.error",
                defaultMessage: "Failed to delete location",
              }),
          );
        }
        setLoadingSummary(false);
      },
    );
  }, [endpoint, intl, isCascadeDelete, isOpen, locationId]);

  const childCount = summary?.childLocationCount || 0;
  const childType = summary?.childLocationType || "location";
  const sampleCount = summary?.sampleCount || 0;

  const modalHeading = useMemo(() => {
    const headingIdByType = {
      room: "storage.delete.room",
      device: "storage.delete.device",
      shelf: "storage.delete.shelf",
      rack: "storage.delete.rack",
      box: "storage.delete.location",
    };
    return intl.formatMessage({
      id: headingIdByType[type] || "storage.delete.location",
      defaultMessage: "Delete Location",
    });
  }, [intl, type]);

  const handleDelete = async () => {
    if (!endpoint || !locationId) return;
    setDeleting(true);
    setError(null);
    try {
      await deleteLocation(type, locationId);
      onDeleted?.();
    } catch (e) {
      setError(
        e?.message ||
          intl.formatMessage({
            id: "storage.delete.error",
            defaultMessage: "Failed to delete location",
          }),
      );
    } finally {
      setDeleting(false);
    }
  };

  const disableDelete =
    deleting || loadingSummary || (isCascadeDelete && !confirmed);

  return (
    <Modal
      open={isOpen}
      modalHeading={modalHeading}
      primaryButtonText={intl.formatMessage({
        id: "label.delete",
        defaultMessage: "Delete",
      })}
      secondaryButtonText={intl.formatMessage({
        id: "label.cancel",
        defaultMessage: "Cancel",
      })}
      danger
      onRequestClose={onClose}
      onRequestSubmit={handleDelete}
      primaryButtonDisabled={disableDelete}
    >
      {loadingSummary && (
        <InlineLoading
          description={intl.formatMessage({
            id: "storage.delete.checking",
            defaultMessage: "Checking constraints...",
          })}
        />
      )}

      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "label.error",
            defaultMessage: "Error",
          })}
          subtitle={error}
          lowContrast
        />
      )}

      <p>
        <FormattedMessage
          id="storage.delete.are.you.sure"
          defaultMessage="Are you sure you want to delete"
        />{" "}
        <strong>{locationLabel}</strong>?{" "}
        <FormattedMessage
          id="storage.delete.cannot.be.undone"
          defaultMessage="This action cannot be undone."
        />
      </p>

      {isCascadeDelete && (
        <>
          <InlineNotification
            kind="warning"
            title={intl.formatMessage({
              id: "storage.delete.cascade.warning",
              defaultMessage: "Cascade Delete Warning",
            })}
            subtitle={intl.formatMessage(
              {
                id: "storage.delete.cascade.summary",
                defaultMessage:
                  "This will delete {childCount} {childType}(s) and unassign {sampleCount} sample(s). Are you sure you want to proceed?",
              },
              {
                childCount,
                childType,
                sampleCount,
              },
            )}
            lowContrast
          />

          <Checkbox
            id="storage-delete-confirmation"
            labelText={intl.formatMessage({
              id: "storage.delete.cascade.confirmation.checkbox",
              defaultMessage:
                "I confirm that I want to delete this location and all its child locations. All samples will be unassigned. This action cannot be undone.",
            })}
            checked={confirmed}
            onChange={(_event, { checked }) => setConfirmed(checked)}
          />
        </>
      )}
    </Modal>
  );
}
