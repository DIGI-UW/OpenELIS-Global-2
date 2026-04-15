import React, { useState, useEffect } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { Button, Loading, InlineNotification, Modal } from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";
import "../../Style.css";

function DatabaseCleaning() {
  const intl = useIntl();
  const [loading, setLoading] = useState(true);
  const [isTrainingInstallation, setIsTrainingInstallation] = useState(false);
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [cleaning, setCleaning] = useState(false);
  const [notification, setNotification] = useState(null);

  useEffect(() => {
    getFromOpenElisServer("/rest/database-cleaning/status", (response) => {
      if (response) {
        setIsTrainingInstallation(response.trainingInstallation);
      }
      setLoading(false);
    });
  }, []);

  const handleCleanDatabase = () => {
    setCleaning(true);
    setNotification(null);

    postToOpenElisServerFullResponse(
      "/rest/database-cleaning",
      null,
      async (response) => {
        setCleaning(false);
        setShowConfirmModal(false);

        if (response.ok) {
          setNotification({
            kind: "success",
            title: intl.formatMessage({ id: "http.success" }),
            message: intl.formatMessage({ id: "database.clean.success" }),
          });
        } else {
          const errorData = await response.json().catch(() => ({}));
          setNotification({
            kind: "error",
            title: intl.formatMessage({ id: "alert.error" }),
            message: intl.formatMessage({ id: "database.clean.error" }),
          });
        }
      },
    );
  };

  if (loading) {
    return <Loading />;
  }

  if (!isTrainingInstallation) {
    return (
      <div className="adminPageContent">
        <InlineNotification
          kind="warning"
          title={intl.formatMessage({ id: "alert.warning" })}
          subtitle={intl.formatMessage({ id: "database.clean.not.training" })}
        />
      </div>
    );
  }

  return (
    <div className="adminPageContent">
      <h2>
        <FormattedMessage id="database.clean" />
      </h2>

      {notification && (
        <InlineNotification
          kind={notification.kind}
          title={notification.title}
          subtitle={notification.message}
          onClose={() => setNotification(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      <div style={{ marginTop: "2rem" }}>
        <InlineNotification
          kind="warning"
          title={intl.formatMessage({ id: "alert.warning" })}
          subtitle={intl.formatMessage({ id: "database.clean.warning" })}
          lowContrast
        />

        <Button
          kind="danger"
          onClick={() => setShowConfirmModal(true)}
          style={{ marginTop: "1rem" }}
          disabled={cleaning}
        >
          <FormattedMessage id="database.clean.button" />
        </Button>
      </div>

      <Modal
        open={showConfirmModal}
        danger
        modalHeading={intl.formatMessage({
          id: "database.clean.confirm.title",
        })}
        primaryButtonText={intl.formatMessage({ id: "label.button.confirm" })}
        secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
        onRequestClose={() => setShowConfirmModal(false)}
        onRequestSubmit={handleCleanDatabase}
        preventCloseOnClickOutside
      >
        <p>
          <FormattedMessage id="database.clean.confirm.message" />
        </p>
        <p style={{ marginTop: "1rem", fontWeight: "bold", color: "red" }}>
          <FormattedMessage id="database.clean.confirm.warning" />
        </p>
      </Modal>
    </div>
  );
}

export default DatabaseCleaning;
