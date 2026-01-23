import React, { useState, useContext } from "react";
import {
  Modal,
  TextInput,
  InlineLoading,
  InlineNotification,
} from "@carbon/react";
import { postToOpenElisServerJsonResponse } from "../utils/Utils";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../layout/Layout";

/**
 * CreateInstanceModal - Modal for creating a child instance from a parent template.
 *
 * @param {Boolean} open - Whether the modal is open
 * @param {Function} onClose - Callback when modal is closed
 * @param {Object} parentNotebook - The parent template notebook data
 * @param {Function} onSuccess - Callback when instance is created successfully. Receives the created instance data.
 */
const CreateInstanceModal = ({ open, onClose, parentNotebook, onSuccess }) => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [title, setTitle] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Generate a suggested title when modal opens
  React.useEffect(() => {
    if (open && parentNotebook) {
      // Count existing children + 1 for new instance
      const childCount = parentNotebook.children?.length || 0;
      setTitle(`${parentNotebook.title} - Lab ${childCount + 1}`);
      setError(null);
    }
  }, [open, parentNotebook]);

  const handleCreate = () => {
    if (!title.trim()) {
      setError(
        intl.formatMessage({
          id: "notebook.createInstance.error.titleRequired",
          defaultMessage: "Instance name is required",
        }),
      );
      return;
    }

    setLoading(true);
    setError(null);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${parentNotebook.id}/instances`,
      JSON.stringify({ title: title.trim() }),
      (response) => {
        setLoading(false);
        if (response && response.id) {
          // Success
          addNotification({
            kind: "success",
            title: intl.formatMessage({
              id: "notebook.createInstance.success.title",
              defaultMessage: "Success",
            }),
            message: intl.formatMessage(
              {
                id: "notebook.createInstance.success.message",
                defaultMessage: "Lab instance '{title}' created successfully",
              },
              { title: response.title },
            ),
          });
          setNotificationVisible(true);
          onSuccess(response);
          handleClose();
        } else if (response && response.error) {
          setError(response.error);
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.createInstance.error.unknown",
              defaultMessage: "Failed to create instance",
            }),
          );
        }
      },
      (errorResponse) => {
        setLoading(false);
        setError(
          errorResponse?.error ||
            intl.formatMessage({
              id: "notebook.createInstance.error.unknown",
              defaultMessage: "Failed to create instance",
            }),
        );
      },
    );
  };

  const handleClose = () => {
    setTitle("");
    setError(null);
    setLoading(false);
    onClose();
  };

  return (
    <Modal
      open={open}
      modalHeading={intl.formatMessage({
        id: "notebook.createInstance.title",
        defaultMessage: "Create Lab Instance",
      })}
      primaryButtonText={
        loading
          ? intl.formatMessage({
              id: "label.button.creating",
              defaultMessage: "Creating...",
            })
          : intl.formatMessage({
              id: "label.button.create",
              defaultMessage: "Create",
            })
      }
      secondaryButtonText={intl.formatMessage({
        id: "label.button.cancel",
        defaultMessage: "Cancel",
      })}
      onRequestClose={handleClose}
      onRequestSubmit={handleCreate}
      primaryButtonDisabled={loading || !title.trim()}
      size="md"
    >
      <div style={{ marginBottom: "1rem" }}>
        <p style={{ color: "#525252", marginBottom: "1rem" }}>
          <FormattedMessage
            id="notebook.createInstance.description"
            defaultMessage="Create a new lab instance from the template '{templateName}'. The instance will inherit the workflow pages from the template."
            values={{ templateName: parentNotebook?.title || "" }}
          />
        </p>
      </div>

      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "label.error",
            defaultMessage: "Error",
          })}
          subtitle={error}
          lowContrast
          hideCloseButton
          style={{ marginBottom: "1rem" }}
        />
      )}

      {loading ? (
        <InlineLoading
          description={intl.formatMessage({
            id: "notebook.createInstance.creating",
            defaultMessage: "Creating instance...",
          })}
        />
      ) : (
        <>
          <TextInput
            id="instance-title"
            labelText={intl.formatMessage({
              id: "notebook.createInstance.nameLabel",
              defaultMessage: "Instance Name",
            })}
            placeholder={intl.formatMessage({
              id: "notebook.createInstance.namePlaceholder",
              defaultMessage: "Enter a name for this lab instance",
            })}
            value={title}
            onChange={(e) => {
              setTitle(e.target.value);
              if (error) setError(null);
            }}
            invalid={!!error && !title.trim()}
            invalidText={error}
            required
          />

          {parentNotebook && (
            <div
              style={{
                marginTop: "1.5rem",
                padding: "1rem",
                backgroundColor: "#f4f4f4",
                borderRadius: "4px",
              }}
            >
              <h6
                style={{
                  marginBottom: "0.5rem",
                  fontSize: "0.75rem",
                  fontWeight: "600",
                  color: "#525252",
                }}
              >
                <FormattedMessage
                  id="notebook.createInstance.inheritedProperties"
                  defaultMessage="Inherited from template:"
                />
              </h6>
              <ul
                style={{
                  margin: 0,
                  paddingLeft: "1rem",
                  fontSize: "0.875rem",
                  color: "#6f6f6f",
                }}
              >
                <li>
                  <FormattedMessage
                    id="notebook.createInstance.inheritedPages"
                    defaultMessage="Workflow pages (live inheritance)"
                  />
                </li>
                <li>
                  <FormattedMessage
                    id="notebook.createInstance.inheritedAccess"
                    defaultMessage="Access control settings"
                  />
                </li>
                <li>
                  <FormattedMessage
                    id="notebook.createInstance.inheritedMetadata"
                    defaultMessage="Protocol and objective"
                  />
                </li>
              </ul>
            </div>
          )}
        </>
      )}
    </Modal>
  );
};

export default CreateInstanceModal;
