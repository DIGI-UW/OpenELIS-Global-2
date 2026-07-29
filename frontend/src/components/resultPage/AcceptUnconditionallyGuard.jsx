import React, { useState } from "react";
import { Button, TextArea } from "@carbon/react";
import { Warning, Checkmark, Undo } from "@carbon/react/icons";
import { useIntl } from "react-intl";

/**
 * OGC-745: inline two-stage gate for unconditional result acceptance.
 * Replaces the inline-checkbox + native alert() that previously fired only
 * on the first click per row and silently flipped on subsequent clicks.
 *
 * Stages:
 *   - idle      → warning-tone "Accept unconditionally…" trigger
 *   - armed     → TextArea (required) + Confirm (disabled while empty) + Cancel
 *   - committed → "Accepted unconditionally" indicator + Undo
 *
 * Why inline two-stage (not modal): research-backed for audit-impact actions
 * with high frequency. See plan: NN/g modal-fatigue + Fitts'-Law spatial
 * separation + FDA/EMA "override decisions documented with rationale".
 */
const AcceptUnconditionallyGuard = ({
  rowId,
  accepted,
  onAccept,
  onUnaccept,
}) => {
  const intl = useIntl();
  const [armed, setArmed] = useState(false);
  const [reason, setReason] = useState("");

  const t = (id, defaultMessage) => intl.formatMessage({ id, defaultMessage });

  if (accepted) {
    return (
      <div
        data-testid={`accept-uncond-${rowId}-committed`}
        style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
      >
        <Checkmark size={16} />
        <span>{t("result.acceptasis.committed", "Accepted")}</span>
        <Button
          kind="ghost"
          size="sm"
          renderIcon={Undo}
          onClick={() => onUnaccept?.()}
        >
          {t("result.acceptasis.undo", "Undo")}
        </Button>
      </div>
    );
  }

  if (!armed) {
    return (
      <Button
        data-testid={`accept-uncond-${rowId}-trigger`}
        size="sm"
        renderIcon={Warning}
        style={{
          backgroundColor: "var(--cds-support-success, #24a148)",
          borderColor: "var(--cds-support-success, #24a148)",
          color: "#ffffff",
        }}
        onClick={() => {
          setArmed(true);
          setReason("");
        }}
      >
        {t("result.acceptasis.trigger", "Accept")}
      </Button>
    );
  }

  const trimmed = reason.trim();
  const canConfirm = trimmed.length > 0;

  return (
    <div
      data-testid={`accept-uncond-${rowId}-armed`}
      style={{
        position: "relative",
        zIndex: 20,
        display: "flex",
        flexDirection: "column",
        gap: "0.5rem",
        padding: "0.5rem",
        borderLeft: "3px solid var(--cds-support-warning, #f1c21b)",
      }}
    >
      <TextArea
        id={`accept-uncond-${rowId}-reason`}
        data-testid={`accept-uncond-${rowId}-reason`}
        labelText={t(
          "result.acceptasis.reason.label",
          "Reason for unconditional acceptance",
        )}
        placeholder={t(
          "result.acceptasis.reason.placeholder",
          "Required — e.g. retested twice with same result, or no result available",
        )}
        value={reason}
        onChange={(e) => setReason(e.target.value)}
        rows={2}
        required
      />
      <div style={{ display: "flex", gap: "0.5rem" }}>
        <Button
          kind="danger"
          size="sm"
          disabled={!canConfirm}
          onClick={() => {
            onAccept?.(trimmed);
            setArmed(false);
            setReason("");
          }}
        >
          {t("result.acceptasis.confirm", "Confirm acceptance")}
        </Button>
        <Button
          kind="ghost"
          size="sm"
          onClick={() => {
            setArmed(false);
            setReason("");
          }}
        >
          {t("result.acceptasis.cancel", "Cancel")}
        </Button>
      </div>
    </div>
  );
};

export default AcceptUnconditionallyGuard;
