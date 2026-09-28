import React, { useState } from "react";
import {
  Button,
  Checkbox,
  Column,
  Grid,
  InlineNotification,
  Link,
  NumberInput,
  Tag,
  TextArea,
  Tile,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { resolveApiErrorMessage } from "../../../utils/Utils";
import {
  hintStyle,
  isDispatched,
  kpiLabelStyle,
  kpiValueStyle,
} from "../../eqaCommon";
import { savePrep, requestReadyToShip } from "./workbenchApi";

/**
 * Prep workbench: inventory progress against what the cycle owes
 * its participants, the homogeneity QC record, and the ready-to-ship request.
 *
 * The button is disabled from the server's own verdict (readyToShipAllowed) and
 * the request is still refused server-side with 409 when a stale page clicks it
 * — the gate has one home, in the cycle transition.
 */
const PrepWorkbench = ({
  prep,
  shipmentRows = [],
  onChanged,
  onNotice,
  onGoToShipments,
}) => {
  const intl = useIntl();
  const t = (id, defaultMessage, values) =>
    intl.formatMessage({ id, defaultMessage }, values);

  const [drafts, setDrafts] = useState({});
  const [saving, setSaving] = useState(null);

  const panels = prep?.panels || [];
  const blockers = prep?.blockers || [];
  const selected = prep?.participantCount ?? 0;
  const enrolled = prep?.enrolledParticipantCount ?? selected;
  const dispatchedCount = shipmentRows.filter(isDispatched).length;
  // Prep can only clear a cycle while it is in prep, so every other state is a
  // reason in its own right rather than a missing prerequisite.
  const inPrep = prep?.cycleStatus === "PREP_IN_PROGRESS";
  const pastPrep = !inPrep && prep?.cycleStatus !== "PLANNED";

  const draftOf = (panel) => ({
    aliquotsProduced: panel.aliquotsProduced,
    aliquotsReserved: panel.aliquotsReserved,
    homogeneityQcPassed: panel.homogeneityQcPassed,
    homogeneityQcNotes: panel.homogeneityQcNotes || "",
    ...(drafts[panel.panelId] || {}),
  });

  const setDraft = (panelId, patch) =>
    setDrafts((prev) => ({
      ...prev,
      [panelId]: { ...(prev[panelId] || {}), ...patch },
    }));

  const handleSave = (panel) => {
    const draft = draftOf(panel);
    setSaving(panel.panelId);
    savePrep(panel.panelId, draft, ({ ok, body }) => {
      setSaving(null);
      if (ok) {
        setDrafts((prev) => ({ ...prev, [panel.panelId]: undefined }));
        onChanged(body);
        onNotice({
          kind: "success",
          text: t("eqa.prep.saved", "Prep record updated."),
        });
        return;
      }
      onNotice({
        kind: "error",
        text: resolveApiErrorMessage(intl, body, "eqa.prep.saveFailed"),
      });
    });
  };

  const handleReadyToShip = () => {
    requestReadyToShip(prep.cycleId, ({ ok, body }) => {
      if (ok) {
        onChanged(null);
        onNotice({
          kind: "success",
          text: t(
            "eqa.prep.readyToShip.success",
            "Cycle cleared to ship — record courier details on the Shipments tab.",
          ),
        });
        return;
      }
      // The 409 body carries the gate's own reason (short of aliquots, QC not
      // passed) — surface it verbatim rather than a generic failure.
      onNotice({
        kind: "error",
        text: resolveApiErrorMessage(
          intl,
          body,
          "eqa.prep.readyToShip.refused",
        ),
      });
    });
  };

  return (
    <>
      <Grid style={{ marginBottom: "1rem" }}>
        <Column sm={4} md={2} lg={4}>
          <Tile>
            <div style={kpiValueStyle}>
              {t("eqa.prep.ofTotal", "{n} of {total}", {
                n: selected,
                total: enrolled,
              })}
            </div>
            <div style={kpiLabelStyle}>
              {t(
                "eqa.prep.tile.participants",
                "Enrolled laboratories selected for this cycle",
              )}
            </div>
          </Tile>
        </Column>
        <Column sm={4} md={2} lg={4}>
          <Tile>
            <div style={kpiValueStyle}>{panels.length}</div>
            <div style={kpiLabelStyle}>
              {t("eqa.prep.tile.panels", "Material panels prepared")}
            </div>
          </Tile>
        </Column>
        <Column sm={4} md={2} lg={4}>
          <Tile>
            <div style={kpiValueStyle}>
              {t("eqa.prep.ofTotal", "{n} of {total}", {
                n: dispatchedCount,
                total: shipmentRows.length,
              })}
            </div>
            <div style={kpiLabelStyle}>
              {t("eqa.prep.dispatched", "Participant shipments dispatched")}
            </div>
          </Tile>
        </Column>
        <Column sm={4} md={4} lg={8}>
          <Tile>
            <div style={kpiLabelStyle}>
              {t("eqa.prep.gate", "Ready-to-ship gate")}
            </div>
            {blockers.length === 0 ? (
              <Tag type="green" size="sm">
                {t("eqa.prep.gate.clear", "All prep requirements met")}
              </Tag>
            ) : (
              <ul style={{ margin: "0.25rem 0 0 1rem", ...hintStyle }}>
                {blockers.map((blocker) => (
                  <li key={blocker}>{blocker}</li>
                ))}
              </ul>
            )}
          </Tile>
        </Column>
      </Grid>

      {panels.length === 0 && (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={t("eqa.prep.noPanel.title", "No panel prepared")}
          subtitle={t(
            "eqa.prep.noPanel.body",
            "Create the cycle's panel and its samples before recording prep progress.",
          )}
        />
      )}

      {panels.map((panel) => {
        const draft = draftOf(panel);
        return (
          <Tile key={panel.panelId} style={{ marginBottom: "1rem" }}>
            <h4 style={{ margin: "0 0 0.25rem" }}>{panel.panelName}</h4>
            <div style={hintStyle}>
              {t(
                "eqa.prep.needExplained",
                "{samples, plural, one {# sample} other {# samples}} x {participants, plural, one {# participant} other {# participants}} + {reserved} held in reserve = {needed, plural, one {# aliquot} other {# aliquots}} needed; {shipped, plural, one {# aliquot} other {# aliquots}} dispatched so far.",
                {
                  samples: panel.sampleCount,
                  participants: prep.participantCount,
                  reserved: panel.aliquotsReserved,
                  needed: panel.aliquotsNeeded,
                  shipped: panel.aliquotsShipped,
                },
              )}
            </div>
            <Grid style={{ marginTop: "0.75rem" }}>
              <Column sm={4} md={2} lg={4}>
                <NumberInput
                  id={`produced-${panel.panelId}`}
                  min={0}
                  value={draft.aliquotsProduced}
                  label={t("eqa.prep.produced", "Aliquots produced")}
                  onChange={(_e, { value }) =>
                    setDraft(panel.panelId, {
                      aliquotsProduced: Number(value),
                    })
                  }
                />
                {panel.shortfall > 0 && (
                  <Tag type="red" size="sm">
                    {t("eqa.prep.shortfall", "{n} short", {
                      n: panel.shortfall,
                    })}
                  </Tag>
                )}
              </Column>
              <Column sm={4} md={2} lg={4}>
                <NumberInput
                  id={`reserved-${panel.panelId}`}
                  min={0}
                  value={draft.aliquotsReserved}
                  label={t("eqa.prep.reserved", "Aliquots reserved")}
                  helperText={t(
                    "eqa.prep.reserved.recommend",
                    "{n} recommended: max(10% of participants, 5)",
                    {
                      n: Math.max(
                        Math.ceil((prep.participantCount || 0) * 0.1),
                        5,
                      ),
                    },
                  )}
                  onChange={(_e, { value }) =>
                    setDraft(panel.panelId, {
                      aliquotsReserved: Number(value),
                    })
                  }
                />
              </Column>
              <Column sm={4} md={4} lg={8}>
                <Checkbox
                  id={`qc-${panel.panelId}`}
                  labelText={t("eqa.prep.homogeneity", "Homogeneity QC passed")}
                  checked={!!draft.homogeneityQcPassed}
                  onChange={(_e, { checked }) =>
                    setDraft(panel.panelId, { homogeneityQcPassed: checked })
                  }
                />
                <TextArea
                  id={`qc-notes-${panel.panelId}`}
                  rows={2}
                  labelText={t("eqa.prep.homogeneityNotes", "QC notes")}
                  value={draft.homogeneityQcNotes}
                  onChange={(e) =>
                    setDraft(panel.panelId, {
                      homogeneityQcNotes: e.target.value,
                    })
                  }
                />
              </Column>
            </Grid>
            <Button
              kind="tertiary"
              size="sm"
              style={{ marginTop: "0.75rem" }}
              disabled={saving === panel.panelId}
              onClick={() => handleSave(panel)}
            >
              {t("eqa.prep.save", "Save prep record")}
            </Button>
          </Tile>
        );
      })}

      <Button disabled={!prep?.readyToShipAllowed} onClick={handleReadyToShip}>
        {t("eqa.prep.readyToShip", "Mark cycle ready to ship")}
      </Button>
      {/* A disabled control has to say which of the two rules stopped it: an
          unmet prerequisite, or a cycle that is simply past this step. The
          second is not a failure, so it reads as the state it is in and points
          at where the work continues. */}
      {!prep?.readyToShipAllowed && (
        <div style={{ ...hintStyle, marginTop: "0.5rem" }}>
          {inPrep
            ? t(
                "eqa.prep.readyToShip.blockedByGate",
                "Prep is incomplete. The ready-to-ship gate above lists what is still outstanding.",
              )
            : t(
                "eqa.prep.readyToShip.notInPrep",
                "Cycle state: {status}. Clearing to ship is only offered while the cycle is in prep.",
                {
                  status: t(
                    `eqa.cycle.status.${(prep?.cycleStatus || "").toLowerCase()}`,
                    prep?.cycleStatus || "",
                  ),
                },
              )}
          {pastPrep && onGoToShipments && (
            <>
              {" "}
              <Link href="#" onClick={onGoToShipments}>
                {t("eqa.prep.goToShipments", "Open the Shipments tab")}
              </Link>
            </>
          )}
        </div>
      )}
    </>
  );
};

export default PrepWorkbench;
