import React, { useEffect, useState } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  InlineNotification,
} from "@carbon/react";
import { useIntl } from "react-intl";
import config from "../../../config.json";

/**
 * Delete confirmation modal for StorageBox
 *
 * Props:
 * - open: boolean
 * - box: box object
 * - onClose: () => void
 * - onDeleted: () => void
 */
const DeleteBoxModal = ({ open, box, onClose, onDeleted }) => {
  const intl = useIntl();
  const [canDelete, setCanDelete] = useState(true);
  const [constraintMessage, setConstraintMessage] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!open || !box?.id) {
      setCanDelete(true);
      setConstraintMessage(null);
      setIsDeleting(false);
      setError(null);
      return;
    }

    setError(null);
    setIsDeleting(false);

    fetch(config.serverBaseUrl + `/rest/storage/boxes/${box.id}/can-delete`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
      credentials: "include",
    })
      .then(async (res) => {
        if (res.status >= 200 && res.status < 300) {
          const data = await res.json().catch(() => ({}));
          setCanDelete(!!data?.canDelete);
          setConstraintMessage(null);
          return;
        }

        if (res.status === 409) {
          const data = await res.json().catch(() => ({}));
          setCanDelete(false);
          setConstraintMessage(
            data?.message ||
              intl.formatMessage({
                id: "storage.box.delete.blocked",
                defaultMessage: "This box cannot be deleted.",
              }),
          );
          return;
        }

        // Unknown response; keep delete disabled and show generic constraint
        setCanDelete(false);
        setConstraintMessage(
          intl.formatMessage({
            id: "storage.box.delete.blocked",
            defaultMessage: "This box cannot be deleted.",
          }),
        );
      })
      .catch(() => {
        setCanDelete(false);
        setConstraintMessage(
          intl.formatMessage({
            id: "storage.box.delete.blocked",
            defaultMessage: "This box cannot be deleted.",
          }),
        );
      });
  }, [open, box?.id, intl]);

  const handleDelete = async () => {
    if (!box?.id) {
      return;
    }

    setIsDeleting(true);
    setError(null);

    try {
      const res = await fetch(
        config.serverBaseUrl + `/rest/storage/boxes/${box.id}`,
        {
          method: "DELETE",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          credentials: "include",
        },
      );

      if (res.status === 204) {
        if (onDeleted) {
          onDeleted();
        }
        if (onClose) {
          onClose();
        }
        return;
      }

      let message = intl.formatMessage({
        id: "storage.box.delete.error",
        defaultMessage: "Unable to delete box.",
      });
      const contentType = res.headers.get("content-type");
      if (contentType && contentType.includes("application/json")) {
        try {
          const data = await res.json();
          message = data?.message || data?.error || message;
        } catch (e) {
          // ignore
        }
      }
      setError(message);
    } catch (e) {
      setError(
        e?.message ||
          intl.formatMessage({
            id: "storage.box.delete.error",
            defaultMessage: "Unable to delete box.",
          }),
      );
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <ComposedModal
      open={open}
      onClose={onClose}
      size="sm"
      data-testid="delete-box-modal"
    >
      <ModalHeader
        title={intl.formatMessage({
          id: "storage.box.delete.title",
          defaultMessage: "Delete Box/Plate",
        })}
      />
      <ModalBody>
        {error && (
          <InlineNotification
            kind="error"
            lowContrast
            title={intl.formatMessage({
              id: "notification.error",
              defaultMessage: "Error",
            })}
            subtitle={error}
          />
        )}
        {!canDelete && constraintMessage && (
          <InlineNotification
            kind="error"
            lowContrast
            title={intl.formatMessage({
              id: "storage.box.delete.blocked.title",
              defaultMessage: "Cannot delete",
            })}
            subtitle={constraintMessage}
          />
        )}
        <p>
          {intl.formatMessage(
            {
              id: "storage.box.delete.confirm",
              defaultMessage: "Are you sure you want to delete {label}?",
            },
            { label: box?.label || "" },
          )}
        </p>
      </ModalBody>
      <ModalFooter>
        <Button kind="secondary" onClick={onClose}>
          {intl.formatMessage({
            id: "button.cancel",
            defaultMessage: "Cancel",
          })}
        </Button>
        <Button
          kind="danger"
          disabled={!canDelete || isDeleting}
          onClick={handleDelete}
        >
          {intl.formatMessage({
            id: "button.delete",
            defaultMessage: "Delete",
          })}
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default DeleteBoxModal;
