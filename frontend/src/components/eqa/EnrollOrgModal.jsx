import React, { useState, useEffect, useCallback } from "react";
import { Modal, Tag, ComboBox } from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";

const EnrollOrgModal = ({ open, programId, onClose, onSubmit }) => {
  const intl = useIntl();
  const [organizations, setOrganizations] = useState([]);
  const [enrolledOrgIds, setEnrolledOrgIds] = useState(new Set());
  const [selectedOrgs, setSelectedOrgs] = useState([]);

  const fetchData = useCallback(() => {
    if (!open) return;

    getFromOpenElisServer("/rest/organization-list", (orgs) => {
      if (orgs && Array.isArray(orgs)) {
        const active = orgs
          .filter((o) => o.isActive === "Y")
          .map((o) => ({ id: o.id, value: o.organizationName }));
        setOrganizations(active);
      }
    });

    if (programId) {
      getFromOpenElisServer(
        `/rest/eqa/programs/${programId}/enrollments`,
        (enrollments) => {
          if (enrollments && Array.isArray(enrollments)) {
            setEnrolledOrgIds(
              new Set(
                enrollments
                  .filter((e) => e.status === "Active")
                  .map((e) => String(e.organizationId)),
              ),
            );
          }
        },
      );
    }
  }, [programId, open]);

  useEffect(() => {
    if (open) {
      fetchData();
      setSelectedOrgs([]);
    }
  }, [open, fetchData]);

  const availableItems = organizations
    .filter((o) => !enrolledOrgIds.has(String(o.id)))
    .filter((o) => !selectedOrgs.find((s) => String(s.id) === String(o.id)))
    .map((o) => ({ id: String(o.id), text: o.value }));

  const handleOrgSelect = (e) => {
    if (e.selectedItem) {
      setSelectedOrgs([
        ...selectedOrgs,
        { id: e.selectedItem.id, name: e.selectedItem.text },
      ]);
    }
  };

  const handleRemoveOrg = (orgId) => {
    setSelectedOrgs(selectedOrgs.filter((o) => String(o.id) !== String(orgId)));
  };

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
      onRequestClose={() => {
        onClose();
        setSelectedOrgs([]);
      }}
      onRequestSubmit={handleSubmit}
      primaryButtonDisabled={selectedOrgs.length === 0}
      size="lg"
    >
      <p style={{ marginBottom: "1rem", color: "#525252" }}>
        {intl.formatMessage({ id: "eqa.enrollment.selectOrgsPrompt" })}
      </p>
      <ComboBox
        id="enroll-org-combobox"
        titleText={intl.formatMessage({
          id: "eqa.enrollment.organizationName",
        })}
        items={availableItems}
        itemToString={(item) => (item ? item.text : "")}
        onChange={handleOrgSelect}
        placeholder={intl.formatMessage({
          id: "eqa.enrollment.searchOrgs",
        })}
        selectedItem={null}
        shouldFilterItem={({ item, inputValue }) =>
          !inputValue ||
          item.text.toLowerCase().includes(inputValue.toLowerCase())
        }
      />
      {selectedOrgs.length > 0 && (
        <div style={{ marginTop: "1rem" }}>
          <p
            style={{
              fontSize: "0.875rem",
              color: "#0043ce",
              fontWeight: 500,
              marginBottom: "0.5rem",
            }}
          >
            {selectedOrgs.length}{" "}
            {intl.formatMessage({ id: "eqa.enrollment.selected" })}
          </p>
          <div style={{ display: "flex", flexWrap: "wrap", gap: "0.25rem" }}>
            {selectedOrgs.map((org) => (
              <Tag
                key={org.id}
                type="blue"
                filter
                onClose={() => handleRemoveOrg(org.id)}
              >
                {org.name}
              </Tag>
            ))}
          </div>
        </div>
      )}
    </Modal>
  );
};

export default EnrollOrgModal;
