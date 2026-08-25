import React, { useCallback, useEffect, useState } from "react";
import {
  Button,
  InlineNotification,
  Loading,
  NumberInput,
  Stack,
  StructuredListWrapper,
  StructuredListHead,
  StructuredListBody,
  StructuredListRow,
  StructuredListCell,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";

/**
 * Display Order section (OGC-189): positions this lab unit in the
 * system-wide lab unit menus (the real sortOrder). Shows every lab unit in
 * its current order with this one highlighted; type a position (or use the
 * steppers) and save — the backend renumbers the whole sequence densely.
 */
function DisplayOrderSection({ labUnitId }) {
  const intl = useIntl();
  const [labUnits, setLabUnits] = useState([]);
  const [position, setPosition] = useState(1);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState(null);

  const load = useCallback(() => {
    setLoading(true);
    getFromOpenElisServer("/rest/lab-units-management", (response) => {
      const list =
        response && response.success && Array.isArray(response.data)
          ? response.data
          : [];
      setLabUnits(list);
      const index = list.findIndex(
        (item) => String(item.id) === String(labUnitId),
      );
      setPosition(index >= 0 ? index + 1 : 1);
      setLoading(false);
    });
  }, [labUnitId]);

  useEffect(() => {
    load();
  }, [load]);

  const save = useCallback(() => {
    setSaving(true);
    setSaved(false);
    setError(null);
    putToOpenElisServer(
      `/rest/lab-units-management/${labUnitId}/display-order`,
      JSON.stringify({ position }),
      (status) => {
        setSaving(false);
        if (status === 200) {
          setSaved(true);
          load();
        } else {
          setError(
            intl.formatMessage({
              id: "label.labUnit.displayOrder.saveError",
            }),
          );
        }
      },
    );
  }, [labUnitId, position, load, intl]);

  if (loading) {
    return (
      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: "var(--cds-spacing-03)",
        }}
      >
        <Loading small withOverlay={false} />
        <FormattedMessage id="label.labUnit.displayOrder.loading" />
      </div>
    );
  }

  return (
    <Stack gap={6} data-testid="labUnit-displayOrder-section">
      <p style={{ fontSize: "14px", color: "var(--cds-text-secondary)" }}>
        <FormattedMessage id="label.labUnit.displayOrder.intro" />
      </p>

      {saved && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({
            id: "label.labUnit.displayOrder.saved",
          })}
          lowContrast
          hideCloseButton={false}
          onCloseButtonClick={() => setSaved(false)}
        />
      )}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          lowContrast
          hideCloseButton={false}
          onCloseButtonClick={() => setError(null)}
        />
      )}

      <div
        style={{
          display: "flex",
          alignItems: "flex-end",
          gap: "var(--cds-spacing-05)",
          flexWrap: "wrap",
        }}
      >
        <NumberInput
          id="lu-display-order-position"
          label={intl.formatMessage({ id: "label.labUnit.displayOrder" })}
          min={1}
          max={Math.max(labUnits.length, 1)}
          value={position}
          onChange={(_e, { value }) => {
            const parsed = Number(value);
            if (!Number.isNaN(parsed)) {
              setPosition(Math.max(1, Math.min(parsed, labUnits.length || 1)));
            }
          }}
          style={{ maxWidth: "16rem" }}
        />
        <Button
          id="lu-display-order-save"
          kind="primary"
          size="md"
          onClick={save}
          disabled={saving}
        >
          {saving ? (
            <FormattedMessage id="button.saving" defaultMessage="Saving..." />
          ) : (
            <FormattedMessage id="button.save" defaultMessage="Save Changes" />
          )}
        </Button>
      </div>

      <StructuredListWrapper isCondensed>
        <StructuredListHead>
          <StructuredListRow head>
            <StructuredListCell head>#</StructuredListCell>
            <StructuredListCell head>
              <FormattedMessage id="label.labUnit.name" />
            </StructuredListCell>
            <StructuredListCell head>
              <FormattedMessage id="label.labUnit.status" />
            </StructuredListCell>
          </StructuredListRow>
        </StructuredListHead>
        <StructuredListBody>
          {labUnits.map((labUnit, index) => {
            const isCurrent = String(labUnit.id) === String(labUnitId);
            return (
              <StructuredListRow
                key={labUnit.id}
                data-testid={
                  isCurrent ? "display-order-current-row" : undefined
                }
                style={
                  isCurrent
                    ? { background: "var(--cds-layer-selected, #e0e0e0)" }
                    : undefined
                }
              >
                <StructuredListCell>{index + 1}</StructuredListCell>
                <StructuredListCell>
                  <span style={{ fontWeight: isCurrent ? 600 : 400 }}>
                    {labUnit.name}
                  </span>
                  {isCurrent && (
                    <Tag
                      type="blue"
                      size="sm"
                      style={{ marginLeft: "var(--cds-spacing-03)" }}
                    >
                      <FormattedMessage id="label.labUnit.displayOrder.current" />
                    </Tag>
                  )}
                </StructuredListCell>
                <StructuredListCell>
                  {labUnit.isActive ? (
                    <FormattedMessage id="label.active" />
                  ) : (
                    <FormattedMessage id="label.inactive" />
                  )}
                </StructuredListCell>
              </StructuredListRow>
            );
          })}
        </StructuredListBody>
      </StructuredListWrapper>
    </Stack>
  );
}

export default DisplayOrderSection;
