import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Button,
  Checkbox,
  InlineNotification,
  Loading,
  Modal,
  Select,
  SelectItem,
  Stack,
  StructuredListWrapper,
  StructuredListHead,
  StructuredListBody,
  StructuredListRow,
  StructuredListCell,
  Tag,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import useDomains from "../../../common/useDomains";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";

const DOMAIN_TAG_COLORS = ["green", "purple", "teal", "cyan", "magenta"];

/**
 * Assigned Tests section (OGC-189): table of the tests assigned to this lab
 * unit with bulk selection, bulk Assign (pull tests in from other units) and
 * bulk Reassign (move selected tests to a destination unit) behind
 * confirmation dialogs. Assignment lives on the test (one lab unit per test),
 * so assigning here removes the test from its previous unit — the Assign
 * dialog shows each candidate's current unit for that reason.
 */
function AssignedTestsSection({ labUnitId, onChange }) {
  const intl = useIntl();

  const domains = useDomains();
  const domainColor = (id) => {
    const index = domains.findIndex((d) => d.id === id);
    return index >= 0
      ? DOMAIN_TAG_COLORS[index % DOMAIN_TAG_COLORS.length]
      : "gray";
  };
  const domainLabel = (id) => {
    const match = domains.find((d) => d.id === id);
    return match ? intl.formatMessage({ id: match.labelKey }) : id;
  };

  const [tests, setTests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Bulk selection of assigned tests (for Reassign).
  const [selectedIds, setSelectedIds] = useState(new Set());

  // Destination options for the Reassign dialog.
  const [labUnits, setLabUnits] = useState([]);

  // Assign dialog state.
  const [assignOpen, setAssignOpen] = useState(false);
  const [candidates, setCandidates] = useState([]);
  const [candidatesLoading, setCandidatesLoading] = useState(false);
  const [assignSearch, setAssignSearch] = useState("");
  const [assignSelectedIds, setAssignSelectedIds] = useState(new Set());

  // Reassign dialog state.
  const [reassignOpen, setReassignOpen] = useState(false);
  const [destinationId, setDestinationId] = useState("");

  const [actionBusy, setActionBusy] = useState(false);

  const applyTests = useCallback(
    (updated) => {
      setTests(updated);
      setSelectedIds(new Set());
      if (onChange) {
        onChange(updated);
      }
    },
    [onChange],
  );

  useEffect(() => {
    setLoading(true);
    setError(null);
    getFromOpenElisServer(
      `/rest/lab-units-management/${labUnitId}/tests`,
      (response) => {
        if (Array.isArray(response)) {
          applyTests(response);
        } else {
          applyTests([]);
          setError(intl.formatMessage({ id: "label.labUnit.tests.loadError" }));
        }
        setLoading(false);
      },
    );
    // applyTests/intl are stable enough; refetch only on unit change.
  }, [labUnitId]);

  // Destination units for the Reassign dialog (everything but this unit).
  useEffect(() => {
    getFromOpenElisServer("/rest/lab-units-management", (response) => {
      const list =
        response && response.success && Array.isArray(response.data)
          ? response.data
          : [];
      setLabUnits(list.filter((unit) => String(unit.id) !== String(labUnitId)));
    });
  }, [labUnitId]);

  const openAssignDialog = useCallback(() => {
    setAssignOpen(true);
    setAssignSearch("");
    setAssignSelectedIds(new Set());
    setCandidatesLoading(true);
    getFromOpenElisServer(
      `/rest/lab-units-management/${labUnitId}/assignable-tests`,
      (response) => {
        setCandidates(Array.isArray(response) ? response : []);
        setCandidatesLoading(false);
      },
    );
  }, [labUnitId]);

  const filteredCandidates = useMemo(() => {
    if (!assignSearch.trim()) {
      return candidates;
    }
    const needle = assignSearch.trim().toLowerCase();
    return candidates.filter((test) =>
      test.name.toLowerCase().includes(needle),
    );
  }, [candidates, assignSearch]);

  const selectedTests = useMemo(
    () => tests.filter((test) => selectedIds.has(test.id)),
    [tests, selectedIds],
  );

  const toggleSet = (set, id) => {
    const next = new Set(set);
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    return next;
  };

  const runBulkAction = useCallback(
    (endpoint, payload, successKey) => {
      setActionBusy(true);
      setError(null);
      postToOpenElisServerJsonResponse(
        `/rest/lab-units-management/${labUnitId}${endpoint}`,
        JSON.stringify(payload),
        (result) => {
          setActionBusy(false);
          if (result && result.success && Array.isArray(result.data)) {
            applyTests(result.data);
            setSuccessMessage(intl.formatMessage({ id: successKey }));
            setAssignOpen(false);
            setReassignOpen(false);
            setDestinationId("");
          } else {
            setError(
              (result && (result.message || result.error)) ||
                intl.formatMessage({ id: "error.labUnit.tests.action" }),
            );
          }
        },
      );
    },
    [labUnitId, applyTests, intl],
  );

  const confirmAssign = useCallback(() => {
    runBulkAction(
      "/tests/assign",
      { testIds: Array.from(assignSelectedIds) },
      "message.labUnit.tests.assigned",
    );
  }, [runBulkAction, assignSelectedIds]);

  const confirmReassign = useCallback(() => {
    runBulkAction(
      "/tests/reassign",
      {
        testIds: Array.from(selectedIds),
        destinationLabUnitId: destinationId,
      },
      "message.labUnit.tests.reassigned",
    );
  }, [runBulkAction, selectedIds, destinationId]);

  const destinationName =
    labUnits.find((unit) => String(unit.id) === String(destinationId))?.name ||
    "";

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
        <FormattedMessage id="label.labUnit.tests.loading" />
      </div>
    );
  }

  return (
    <Stack gap={6} data-testid="labUnit-assignedTests-section">
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          lowContrast
          hideCloseButton={false}
          onCloseButtonClick={() => setError(null)}
        />
      )}
      {successMessage && (
        <InlineNotification
          kind="success"
          title={successMessage}
          lowContrast
          hideCloseButton={false}
          onCloseButtonClick={() => setSuccessMessage(null)}
        />
      )}

      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          flexWrap: "wrap",
          gap: "var(--cds-spacing-04)",
        }}
      >
        <p
          style={{
            fontSize: "14px",
            color: "var(--cds-text-secondary)",
            margin: 0,
          }}
        >
          <FormattedMessage
            id="label.labUnit.tests.count"
            values={{ count: tests.length }}
          />
        </p>
        <Stack orientation="horizontal" gap={3}>
          <Button
            kind="tertiary"
            size="sm"
            onClick={openAssignDialog}
            disabled={actionBusy}
          >
            <FormattedMessage id="button.labUnit.tests.assign" />
          </Button>
          <Button
            kind="primary"
            size="sm"
            onClick={() => setReassignOpen(true)}
            disabled={actionBusy || selectedIds.size === 0}
          >
            <FormattedMessage
              id="button.labUnit.tests.reassign"
              values={{ count: selectedIds.size }}
            />
          </Button>
        </Stack>
      </div>

      {tests.length > 0 ? (
        <StructuredListWrapper isCondensed>
          <StructuredListHead>
            <StructuredListRow head>
              <StructuredListCell head>
                <Checkbox
                  id="lu-tests-select-all"
                  labelText=""
                  hideLabel
                  checked={
                    selectedIds.size === tests.length && tests.length > 0
                  }
                  indeterminate={
                    selectedIds.size > 0 && selectedIds.size < tests.length
                  }
                  onChange={(_e, { checked }) =>
                    setSelectedIds(
                      checked
                        ? new Set(tests.map((test) => test.id))
                        : new Set(),
                    )
                  }
                />
              </StructuredListCell>
              <StructuredListCell head>
                <FormattedMessage id="label.labUnit.tests.testName" />
              </StructuredListCell>
              <StructuredListCell head>
                <FormattedMessage id="label.labUnit.domain" />
              </StructuredListCell>
              <StructuredListCell head>
                <FormattedMessage id="label.labUnit.status" />
              </StructuredListCell>
            </StructuredListRow>
          </StructuredListHead>
          <StructuredListBody>
            {tests.map((test) => (
              <StructuredListRow key={test.id}>
                <StructuredListCell>
                  <Checkbox
                    id={`lu-test-select-${test.id}`}
                    labelText=""
                    hideLabel
                    checked={selectedIds.has(test.id)}
                    onChange={() =>
                      setSelectedIds((prev) => toggleSet(prev, test.id))
                    }
                  />
                </StructuredListCell>
                <StructuredListCell>{test.name}</StructuredListCell>
                <StructuredListCell>
                  <Tag type={domainColor(test.domain)} size="sm">
                    {domainLabel(test.domain)}
                  </Tag>
                </StructuredListCell>
                <StructuredListCell>
                  {test.active ? (
                    <FormattedMessage id="label.active" />
                  ) : (
                    <FormattedMessage id="label.inactive" />
                  )}
                </StructuredListCell>
              </StructuredListRow>
            ))}
          </StructuredListBody>
        </StructuredListWrapper>
      ) : (
        <p style={{ fontSize: "14px", color: "var(--cds-text-secondary)" }}>
          <FormattedMessage id="label.labUnit.tests.none" />
        </p>
      )}

      {/* Bulk Assign dialog: pick tests from other lab units to move here. */}
      <Modal
        open={assignOpen}
        modalHeading={intl.formatMessage({
          id: "heading.labUnit.tests.assign",
        })}
        primaryButtonText={intl.formatMessage(
          { id: "button.labUnit.tests.assign.confirm" },
          { count: assignSelectedIds.size },
        )}
        secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
        primaryButtonDisabled={actionBusy || assignSelectedIds.size === 0}
        onRequestClose={() => setAssignOpen(false)}
        onRequestSubmit={confirmAssign}
        size="md"
      >
        <Stack gap={5}>
          <p style={{ fontSize: "14px", color: "var(--cds-text-secondary)" }}>
            <FormattedMessage id="label.labUnit.tests.assign.explain" />
          </p>
          <TextInput
            id="lu-assign-search"
            labelText={intl.formatMessage({
              id: "placeholder.labUnit.tests.search",
            })}
            hideLabel
            placeholder={intl.formatMessage({
              id: "placeholder.labUnit.tests.search",
            })}
            value={assignSearch}
            onChange={(e) => setAssignSearch(e.target.value)}
          />
          {candidatesLoading ? (
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "var(--cds-spacing-03)",
              }}
            >
              <Loading small withOverlay={false} />
              <FormattedMessage id="label.labUnit.tests.loading" />
            </div>
          ) : filteredCandidates.length > 0 ? (
            <div style={{ maxHeight: "18rem", overflowY: "auto" }}>
              {filteredCandidates.map((test) => (
                <div
                  key={test.id}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "var(--cds-spacing-03)",
                    padding: "var(--cds-spacing-02) 0",
                  }}
                >
                  <Checkbox
                    id={`lu-assign-candidate-${test.id}`}
                    labelText={test.name}
                    checked={assignSelectedIds.has(test.id)}
                    onChange={() =>
                      setAssignSelectedIds((prev) => toggleSet(prev, test.id))
                    }
                  />
                  {test.currentLabUnitName && (
                    <Tag type="gray" size="sm">
                      {test.currentLabUnitName}
                    </Tag>
                  )}
                </div>
              ))}
            </div>
          ) : (
            <p style={{ fontSize: "14px", color: "var(--cds-text-secondary)" }}>
              <FormattedMessage id="label.labUnit.tests.assign.none" />
            </p>
          )}
        </Stack>
      </Modal>

      {/* Bulk Reassign dialog: destination selector + the list of tests being
          moved + confirmation (OGC-189 Tests tab). */}
      <Modal
        open={reassignOpen}
        modalHeading={intl.formatMessage({
          id: "heading.labUnit.tests.reassign",
        })}
        primaryButtonText={intl.formatMessage(
          { id: "button.labUnit.tests.reassign.confirm" },
          { count: selectedIds.size, destination: destinationName },
        )}
        secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
        primaryButtonDisabled={actionBusy || !destinationId}
        danger
        onRequestClose={() => setReassignOpen(false)}
        onRequestSubmit={confirmReassign}
        size="sm"
      >
        <Stack gap={5}>
          <Select
            id="lu-reassign-destination"
            labelText={intl.formatMessage({
              id: "label.labUnit.tests.reassign.destination",
            })}
            value={destinationId}
            onChange={(e) => setDestinationId(e.target.value)}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "placeholder.labUnit.tests.reassign.destination",
              })}
            />
            {labUnits.map((unit) => (
              <SelectItem key={unit.id} value={unit.id} text={unit.name} />
            ))}
          </Select>
          <div>
            <p style={{ fontSize: "14px", fontWeight: 600 }}>
              <FormattedMessage
                id="label.labUnit.tests.reassign.moving"
                values={{ count: selectedTests.length }}
              />
            </p>
            <ul style={{ margin: 0, paddingLeft: "1.25rem" }}>
              {selectedTests.map((test) => (
                <li key={test.id} style={{ fontSize: "14px" }}>
                  {test.name}
                </li>
              ))}
            </ul>
          </div>
          <p style={{ fontSize: "13px", color: "var(--cds-text-secondary)" }}>
            <FormattedMessage id="label.labUnit.tests.reassign.explain" />
          </p>
        </Stack>
      </Modal>
    </Stack>
  );
}

export default AssignedTestsSection;
