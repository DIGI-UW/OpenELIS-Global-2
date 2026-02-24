import React, { useState, useEffect, useCallback } from "react";
import { Modal, FilterableMultiSelect } from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";

const EnrollOrgModal = ({ open, programId, onClose, onSubmit }) => {
  const intl = useIntl();
  const [organizations, setOrganizations] = useState([]);
  const [selectedOrgs, setSelectedOrgs] = useState([]);

  const fetchOrganizations = useCallback(() => {
    if (!programId || !open) return;
    getFromOpenElisServer(
      `/rest/eqa/eligible-organizations?programId=${programId}`,
      (data) => {
        if (data && Array.isArray(data)) {
          setOrganizations(data);
        }
      },
    );
  }, [programId, open]);

  useEffect(() => {
    if (open) {
      fetchOrganizations();
      setSelectedOrgs([]);
    }
  }, [open, fetchOrganizations]);

  const availableOrgs = organizations
    .filter((o) => !o.alreadyEnrolled)
    .map((o) => ({
      id: String(o.id),
      text: o.organizationName || "",
    }));

  const enrolledOrgs = organizations.filter((o) => o.alreadyEnrolled);

  const handleSubmit = () => {
    if (selectedOrgs.length > 0) {
      onSubmit(selectedOrgs.map((o) => Number(o.id)));
    }
  };

  return (
    <Modal
      open={open}
      modalHeading={intl.formatMessage({
        id: "eqa.enrollment.enrollParticipant",
      })}
      primaryButtonText={intl.formatMessage(
        { id: "eqa.enrollment.enrollSelected" },
        { count: selectedOrgs.length },
      )}
      secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
      onRequestClose={onClose}
      onRequestSubmit={handleSubmit}
      primaryButtonDisabled={selectedOrgs.length === 0}
      size="lg"
    >
      <p style={{ marginBottom: "1rem", color: "#525252" }}>
        {intl.formatMessage({ id: "eqa.enrollment.selectOrgsPrompt" })}
      </p>
      <FilterableMultiSelect
        id="org-multiselect"
        titleText={intl.formatMessage({
          id: "eqa.enrollment.organizationName",
        })}
        items={availableOrgs}
        itemToString={(item) => (item ? item.text : "")}
        onChange={(e) => setSelectedOrgs(e.selectedItems)}
        placeholder={intl.formatMessage({
          id: "eqa.enrollment.searchOrgs",
        })}
      />
      {selectedOrgs.length > 0 && (
        <p
          style={{
            marginTop: "0.5rem",
            fontSize: "0.875rem",
            color: "#0043ce",
            fontWeight: 500,
          }}
        >
          {selectedOrgs.length}{" "}
          {intl.formatMessage({ id: "eqa.enrollment.selected" })}
        </p>
      )}
      {enrolledOrgs.length > 0 && (
        <p
          style={{
            marginTop: "1rem",
            fontSize: "0.75rem",
            color: "#525252",
          }}
        >
          {intl.formatMessage(
            { id: "eqa.enrollment.alreadyEnrolledCount" },
            { count: enrolledOrgs.length },
          )}
        </p>
      )}
    </Modal>
  );
};

export default EnrollOrgModal;
