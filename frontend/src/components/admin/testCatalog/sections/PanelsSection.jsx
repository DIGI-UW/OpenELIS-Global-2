import React, { useContext, useEffect, useState } from "react";
import {
  Stack,
  ComboBox,
  TextInput,
  Button,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Loading,
  InlineNotification,
} from "@carbon/react";
import { Add, TrashCan } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";

/**
 * OGC-949 M9 / OGC-980..982 — Panels section.
 *
 * Manages which panels a test belongs to (add via typeahead, remove) and this
 * test's position within each (numeric). Persists via PUT
 * /rest/test-catalog/tests/{id}/panels. New panels are created in Master Lists →
 * Panel Management (they need sample-type + ordering-module scaffolding to be
 * orderable), so "Create new panel" points there rather than creating inline.
 */
const toInt = (v) => {
  if (v === "" || v === null || v === undefined) {
    return null;
  }
  const n = parseInt(v, 10);
  return Number.isNaN(n) ? null : n;
};

const PanelsSection = ({ testId }) => {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [saving, setSaving] = useState(false);
  const [memberships, setMemberships] = useState([]);
  const [panels, setPanels] = useState([]);
  const [showCreateHint, setShowCreateHint] = useState(false);
  const [comboKey, setComboKey] = useState(0);

  useEffect(() => {
    if (!testId) {
      return;
    }
    setLoading(true);
    setError(false);
    getFromOpenElisServer("/rest/test-catalog/panels", (panelsRes) => {
      getFromOpenElisServer(
        `/rest/test-catalog/tests/${testId}/panels`,
        (res) => {
          setLoading(false);
          if (!res || !panelsRes) {
            setError(true);
            return;
          }
          setPanels(panelsRes);
          setMemberships(res.memberships || []);
        },
      );
    });
  }, [testId]);

  const addPanel = (panel) => {
    if (!panel || memberships.some((m) => m.panelId === panel.id)) {
      return;
    }
    setMemberships((prev) => [
      ...prev,
      { panelId: panel.id, panelName: panel.name, position: null },
    ]);
    setComboKey((k) => k + 1); // reset the typeahead after a pick
  };

  const setPosition = (panelId, value) =>
    setMemberships((prev) =>
      prev.map((m) => (m.panelId === panelId ? { ...m, position: value } : m)),
    );

  const removePanel = (panelId) =>
    setMemberships((prev) => prev.filter((m) => m.panelId !== panelId));

  const handleSave = () => {
    setSaving(true);
    const payload = {
      memberships: memberships.map((m) => ({
        panelId: m.panelId,
        position: toInt(m.position),
      })),
    };
    putToOpenElisServer(
      `/rest/test-catalog/tests/${testId}/panels`,
      JSON.stringify(payload),
      (status) => {
        setSaving(false);
        setNotificationVisible(true);
        if (status === 200) {
          addNotification({
            kind: "success",
            title: intl.formatMessage({
              id: "label.testCatalog.section.panels",
            }),
            message: intl.formatMessage({
              id: "label.testCatalog.panels.saved",
            }),
          });
        } else {
          addNotification({
            kind: "error",
            title: intl.formatMessage({ id: "error.title" }),
            message: intl.formatMessage({ id: "server.error.msg" }),
          });
        }
      },
    );
  };

  if (loading) {
    return (
      <Loading
        description={intl.formatMessage({ id: "label.loading" })}
        withOverlay={false}
      />
    );
  }
  if (error) {
    return (
      <InlineNotification
        kind="error"
        lowContrast
        hideCloseButton
        title={intl.formatMessage({ id: "error.title" })}
        subtitle={intl.formatMessage({
          id: "label.testCatalog.panels.loadError",
        })}
      />
    );
  }

  const addable = panels.filter(
    (p) => !memberships.some((m) => m.panelId === p.id),
  );

  return (
    <Stack gap={6} data-testid="panels-section">
      <p>
        <FormattedMessage id="label.testCatalog.panels.intro" />
      </p>

      <ComboBox
        key={comboKey}
        id="panels-add"
        data-cy="panel-typeahead"
        titleText={intl.formatMessage({
          id: "label.testCatalog.panels.addToPanel",
        })}
        placeholder={intl.formatMessage({
          id: "label.testCatalog.panels.addToPanel",
        })}
        items={addable}
        itemToString={(item) => (item ? item.name : "")}
        selectedItem={null}
        onChange={({ selectedItem }) => addPanel(selectedItem)}
      />

      {memberships.length === 0 ? (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "label.testCatalog.panels.empty" })}
        />
      ) : (
        <Table size="lg" aria-label="panel-memberships">
          <TableHead>
            <TableRow>
              <TableHeader>
                <FormattedMessage id="label.testCatalog.panels.col.panel" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="label.testCatalog.panels.col.position" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="label.testCatalog.panels.col.actions" />
              </TableHeader>
            </TableRow>
          </TableHead>
          <TableBody>
            {memberships.map((m) => (
              <TableRow
                key={m.panelId}
                data-testid={`panel-membership-${m.panelId}`}
              >
                <TableCell>{m.panelName}</TableCell>
                <TableCell>
                  <TextInput
                    id={`panel-position-${m.panelId}`}
                    type="number"
                    labelText=""
                    hideLabel
                    value={m.position == null ? "" : m.position}
                    onChange={(e) => setPosition(m.panelId, e.target.value)}
                  />
                </TableCell>
                <TableCell>
                  <Button
                    kind="ghost"
                    size="sm"
                    hasIconOnly
                    renderIcon={TrashCan}
                    iconDescription={intl.formatMessage({
                      id: "label.testCatalog.panels.remove",
                    })}
                    onClick={() => removePanel(m.panelId)}
                  />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      <Stack gap={3}>
        <Button
          kind="ghost"
          renderIcon={Add}
          onClick={() => setShowCreateHint(true)}
        >
          <FormattedMessage id="label.testCatalog.panels.createPanel" />
        </Button>
        {showCreateHint && (
          <InlineNotification
            kind="info"
            lowContrast
            onCloseButtonClick={() => setShowCreateHint(false)}
            title={intl.formatMessage({
              id: "label.testCatalog.panels.createPanel",
            })}
            subtitle={intl.formatMessage({
              id: "label.testCatalog.panels.createPanelHint",
            })}
          />
        )}
      </Stack>

      <Button kind="primary" disabled={saving} onClick={handleSave}>
        <FormattedMessage id="label.button.save" />
      </Button>
    </Stack>
  );
};

export default PanelsSection;
