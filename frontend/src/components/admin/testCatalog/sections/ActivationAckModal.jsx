import React from "react";
import { Modal, InlineNotification } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

/**
 * OGC-949 M7 / OGC-973 — coverage-gap acknowledgment modal (the H-03 safety
 * gate). Shown when activating a test whose reference ranges have uncovered age
 * windows; the admin must explicitly acknowledge the gaps to proceed.
 *
 * Presentational: the parent owns the activate POST and the 409 → re-POST flow.
 */
const ActivationAckModal = ({ open, report, onAcknowledge, onCancel }) => {
  const intl = useIntl();

  const fmtAge = (years) => {
    if (years < 1) {
      return `${Math.round(years * 365)} ${intl.formatMessage({
        id: "label.testCatalog.ranges.days",
      })}`;
    }
    return `${years} ${intl.formatMessage({
      id: "label.testCatalog.ranges.years",
    })}`;
  };

  const renderSex = (label, sexCoverage) => {
    if (!sexCoverage || !sexCoverage.gaps || sexCoverage.gaps.length === 0) {
      return null;
    }
    return (
      <div style={{ marginTop: "0.75rem" }}>
        <strong>{label}</strong>
        <ul style={{ marginLeft: "1.25rem", listStyle: "disc" }}>
          {sexCoverage.gaps.map((gap, i) => (
            <li key={i}>
              {intl.formatMessage(
                { id: "label.testCatalog.ranges.gapRange" },
                { from: fmtAge(gap.fromAge), to: fmtAge(gap.toAge) },
              )}
            </li>
          ))}
        </ul>
      </div>
    );
  };

  return (
    <Modal
      open={open}
      danger
      modalHeading={intl.formatMessage({
        id: "label.testCatalog.ranges.ackModal.title",
      })}
      primaryButtonText={intl.formatMessage({
        id: "label.testCatalog.ranges.ackModal.confirm",
      })}
      secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
      onRequestSubmit={onAcknowledge}
      onRequestClose={onCancel}
      onSecondarySubmit={onCancel}
    >
      <InlineNotification
        kind="warning"
        lowContrast
        hideCloseButton
        title={intl.formatMessage({
          id: "label.testCatalog.ranges.ackModal.warning",
        })}
      />
      <p style={{ marginTop: "0.5rem" }}>
        <FormattedMessage id="label.testCatalog.ranges.ackModal.body" />
      </p>
      {report &&
        renderSex(
          intl.formatMessage({ id: "label.testCatalog.ranges.male" }),
          report.male,
        )}
      {report &&
        renderSex(
          intl.formatMessage({ id: "label.testCatalog.ranges.female" }),
          report.female,
        )}
    </Modal>
  );
};

export default ActivationAckModal;
