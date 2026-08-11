import React, { useContext, useEffect, useState } from "react";
import {
  Button,
  ComboBox,
  IconButton,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@carbon/react";
import { Add, ArrowDown, ArrowUp, TrashCan } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServerFullResponse,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";

/**
 * OGC-224 — Panel editor · Tests (the centerpiece, FRS v2.2).
 *
 * The ordered list of member tests: order (writes panel_item.sort_order — the
 * same field the test-side Panels section edits: one model, two views), test
 * name, test code, remove. "Add a test" is a typeahead searchable by name or
 * code, DOMAIN-GUARDED: only tests in the panel's domain are offered, so a
 * panel never mixes domains. Picking a result appends it to the end. LOINC is
 * deliberately not shown here. Membership writes keep order entry's
 * SAMPLETYPE_PANEL junction in sync server-side.
 */
const PanelTestsSection = ({ panel, autoActivate, onSaved }) => {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [members, setMembers] = useState([]);
  const [loaded, setLoaded] = useState(false);
  const [candidates, setCandidates] = useState([]);
  const [saving, setSaving] = useState(false);
  // remount the ComboBox after each pick so its input clears (the proven
  // pattern from the test-side PanelsSection / AssociatedTestsSection)
  const [comboKey, setComboKey] = useState(0);

  useEffect(() => {
    if (!panel?.id) {
      return;
    }
    getFromOpenElisServer(
      `/rest/test-catalog/panels/${panel.id}/test-order`,
      (res) => {
        setMembers(Array.isArray(res?.tests) ? res.tests : []);
        setLoaded(true);
      },
    );
  }, [panel?.id]);

  // Domain-guarded picker: only the panel's domain is ever fetched (the
  // server enforces the same guard on save). One fetch; Carbon filters
  // client-side by name or code as the user types.
  useEffect(() => {
    const params = new URLSearchParams();
    params.set("domain", panel?.domain || "CLINICAL");
    params.set("status", "active");
    params.set("page", "1");
    params.set("pageSize", "2000");
    getFromOpenElisServer(
      `/rest/test-catalog/tests?${params.toString()}`,
      (res) => setCandidates(Array.isArray(res?.rows) ? res.rows : []),
    );
  }, [panel?.domain]);

  const addable = candidates.filter(
    (candidate) => !members.some((m) => m.testId === candidate.testId),
  );

  const move = (index, delta) =>
    setMembers((current) => {
      const target = index + delta;
      if (target < 0 || target >= current.length) {
        return current;
      }
      const next = [...current];
      [next[index], next[target]] = [next[target], next[index]];
      return next;
    });

  const remove = (index) =>
    setMembers((current) => current.filter((_, i) => i !== index));

  const notify = (kind, messageId) => {
    addNotification({
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({ id: messageId }),
      kind,
    });
    setNotificationVisible(true);
  };

  const handleSave = () => {
    setSaving(true);
    putToOpenElisServerFullResponse(
      `/rest/test-catalog/panels/${panel.id}/tests`,
      JSON.stringify({
        tests: members.map((m, i) => ({ testId: m.testId, position: i + 1 })),
        autoActivate: !!autoActivate,
      }),
      async (response) => {
        setSaving(false);
        if (response && response.ok) {
          const body = await response.json();
          setMembers(Array.isArray(body?.tests) ? body.tests : []);
          notify(NotificationKinds.success, "success.add.edited.msg");
          if (body?.panel) {
            onSaved(body.panel);
          }
        } else {
          notify(NotificationKinds.error, "error.panel.save");
        }
      },
    );
  };

  return (
    <Stack gap={5} style={{ maxWidth: "46rem" }}>
      <p style={{ fontSize: "0.875rem" }} data-testid="panel-tests-count">
        <strong>{members.length}</strong>{" "}
        <FormattedMessage id="label.panel.testsCount" />
      </p>
      <ComboBox
        key={comboKey}
        id="panel-add-test"
        titleText={intl.formatMessage({ id: "label.panel.addTest" })}
        placeholder={intl.formatMessage({ id: "placeholder.panel.addTest" })}
        helperText={
          intl.formatMessage(
            { id: "helper.panel.domainGuard" },
            {
              domain: intl.formatMessage({
                id: `label.domain.${panel?.domain || "CLINICAL"}`,
                defaultMessage: panel?.domain || "CLINICAL",
              }),
            },
          ) +
          " " +
          intl.formatMessage({ id: "helper.panel.membershipSync" })
        }
        items={addable}
        itemToString={(item) =>
          item ? `${item.name}${item.code ? ` — ${item.code}` : ""}` : ""
        }
        shouldFilterItem={({ item, inputValue }) =>
          !inputValue ||
          (item?.name || "").toLowerCase().includes(inputValue.toLowerCase()) ||
          (item?.code || "").toLowerCase().includes(inputValue.toLowerCase())
        }
        selectedItem={null}
        onChange={({ selectedItem }) => {
          if (!selectedItem) {
            return;
          }
          setMembers((current) => [
            ...current,
            {
              testId: selectedItem.testId,
              testName: selectedItem.name,
              code: selectedItem.code,
            },
          ]);
          setComboKey((k) => k + 1);
        }}
      />
      <Table size="md" data-testid="panel-tests-table">
        <TableHead>
          <TableRow>
            <TableHeader style={{ width: "5rem" }}>
              <FormattedMessage id="label.panel.col.order" />
            </TableHeader>
            <TableHeader>
              <FormattedMessage id="label.panel.col.testName" />
            </TableHeader>
            <TableHeader>
              <FormattedMessage id="label.panel.col.code" />
            </TableHeader>
            <TableHeader style={{ width: "9rem" }} />
          </TableRow>
        </TableHead>
        <TableBody>
          {loaded && members.length === 0 ? (
            <TableRow>
              <TableCell colSpan={4}>
                <FormattedMessage id="empty.panel.tests" />
              </TableCell>
            </TableRow>
          ) : (
            members.map((member, index) => (
              <TableRow key={member.testId}>
                <TableCell>{index + 1}</TableCell>
                <TableCell>{member.testName}</TableCell>
                <TableCell>
                  {member.code ? <code>{member.code}</code> : "—"}
                </TableCell>
                <TableCell>
                  <IconButton
                    kind="ghost"
                    size="sm"
                    label={intl.formatMessage({ id: "label.panel.moveUp" })}
                    onClick={() => move(index, -1)}
                    data-testid={`panel-test-up-${member.testId}`}
                  >
                    <ArrowUp />
                  </IconButton>
                  <IconButton
                    kind="ghost"
                    size="sm"
                    label={intl.formatMessage({ id: "label.panel.moveDown" })}
                    onClick={() => move(index, 1)}
                    data-testid={`panel-test-down-${member.testId}`}
                  >
                    <ArrowDown />
                  </IconButton>
                  <IconButton
                    kind="ghost"
                    size="sm"
                    label={intl.formatMessage({ id: "label.button.remove" })}
                    onClick={() => remove(index)}
                    data-testid={`panel-test-remove-${member.testId}`}
                  >
                    <TrashCan />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>
      <p
        style={{
          fontSize: "0.75rem",
          color: "var(--cds-text-secondary, #6f6f6f)",
        }}
      >
        <FormattedMessage id="note.panel.positionShared" />
      </p>
      <div>
        <Button
          kind="primary"
          renderIcon={Add}
          onClick={handleSave}
          disabled={saving || !loaded}
        >
          <FormattedMessage id="label.button.save" />
        </Button>
      </div>
    </Stack>
  );
};

export default PanelTestsSection;
