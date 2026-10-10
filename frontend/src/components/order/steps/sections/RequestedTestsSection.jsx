import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Tile,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tag,
  OperationalTag,
  InlineNotification,
  OverflowMenu,
  OverflowMenuItem,
} from "@carbon/react";
import { Checkmark } from "@carbon/icons-react";
import { getFromOpenElisServer } from "../../../utils/Utils";
import TestAssignmentModal from "./TestAssignmentModal";
import TestedElsewhereFields from "./TestedElsewhereFields";
import {
  listTestedElsewhere,
  markTestedElsewhere,
  unmarkTestedElsewhere,
} from "../../api/orderEntryCleanupApi";

const compatibilityMapKey = (id, isPanel) =>
  `${isPanel ? "panel" : "test"}-${id}`;

export const getAssignableSamplesOfType = (samples = [], sampleTypeId) =>
  samples
    .map((sample, index) => ({ ...sample, index }))
    .filter(
      (sample) =>
        sample.sampleTypeId === sampleTypeId && !sample.sampleRejected,
    );

/**
 * RequestedTestsSection - Shows ordered tests/panels with sample type assignment
 *
 * Features:
 * - Displays tests and panels ordered in Step 1
 * - Shows compatible sample types as clickable tags
 * - Shows current sample assignments
 * - Opens modal to assign test to existing or new sample
 */

const RequestedTestsSection = ({
  samples,
  setSamples,
  testSampleAssignments: _testSampleAssignments,
  assignTestToSample,
  removeTestFromSample: _removeTestFromSample,
  sampleTypes,
  isReadOnly,
  labNumber = "",
  referringSite = null,
}) => {
  const intl = useIntl();

  // FR-B20: tests whose result another laboratory reported, by test id.
  const [testedElsewhere, setTestedElsewhere] = useState({});
  const [testedElsewhereError, setTestedElsewhereError] = useState("");

  useEffect(() => {
    if (!labNumber) {
      return undefined;
    }
    let active = true;
    listTestedElsewhere(labNumber)
      .then((marks) => {
        if (!active) return;
        const byTest = {};
        (marks || []).forEach((mark) => {
          byTest[String(mark.testId)] = mark;
        });
        setTestedElsewhere(byTest);
      })
      .catch(() => {
        if (active) setTestedElsewhere({});
      });
    return () => {
      active = false;
    };
  }, [labNumber]);

  const saveTestedElsewhere = useCallback(
    (testId, changes) => {
      const current = testedElsewhere[String(testId)] || {};
      const next = { ...current, ...changes };
      setTestedElsewhereError("");
      markTestedElsewhere({
        labNumber,
        testId,
        performingLabId: next.performingLabId,
        reportedValue: next.reportedValue,
      })
        .then((saved) =>
          setTestedElsewhere((previous) => ({
            ...previous,
            [String(testId)]: saved,
          })),
        )
        .catch((error) =>
          setTestedElsewhereError(
            intl.formatMessage(
              { id: "order.tests.testedElsewhere.saveFailed" },
              { reason: error.message },
            ),
          ),
        );
    },
    [intl, labNumber, testedElsewhere],
  );

  const markTest = useCallback(
    (testId) =>
      saveTestedElsewhere(testId, {
        performingLabId: referringSite?.id || "",
        performingLabName: referringSite?.name || "",
        reportedValue: "",
      }),
    [referringSite, saveTestedElsewhere],
  );

  const unmarkTest = useCallback(
    (testId) => {
      setTestedElsewhereError("");
      unmarkTestedElsewhere(labNumber, testId)
        .then(() =>
          setTestedElsewhere((previous) => {
            const next = { ...previous };
            delete next[String(testId)];
            return next;
          }),
        )
        .catch((error) =>
          setTestedElsewhereError(
            intl.formatMessage(
              { id: "order.tests.testedElsewhere.saveFailed" },
              { reason: error.message },
            ),
          ),
        );
    },
    [intl, labNumber],
  );

  // Modal state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedTest, setSelectedTest] = useState(null);
  const [selectedSampleType, setSelectedSampleType] = useState(null);

  // Test-to-sample-type compatibility cache
  const [testSampleTypeMap, setTestSampleTypeMap] = useState({});
  const [loadedCompatibilityIds, setLoadedCompatibilityIds] = useState("");

  const requestedItems = useMemo(() => {
    const tests = [];
    const panels = [];
    const seenTestIds = new Set();
    const seenPanelIds = new Set();

    samples.forEach((sample) => {
      if (sample.sampleRejected) return;
      (sample.tests || []).forEach((test) => {
        if (!seenTestIds.has(test.id)) {
          seenTestIds.add(test.id);
          tests.push(test);
        }
      });
      (sample.panels || []).forEach((panel) => {
        if (!seenPanelIds.has(panel.id)) {
          seenPanelIds.add(panel.id);
          panels.push(panel);
        }
      });
    });

    return [
      ...panels.map((panel) => ({ ...panel, isPanel: true })),
      ...tests.map((test) => ({ ...test, isPanel: false })),
    ];
  }, [samples]);
  const testIds = useMemo(
    () =>
      requestedItems
        .filter((item) => !item.isPanel)
        .map((test) => test.id)
        .join(","),
    [requestedItems],
  );
  const panelIds = useMemo(
    () =>
      requestedItems
        .filter((item) => item.isPanel)
        .map((panel) => panel.id)
        .join(","),
    [requestedItems],
  );
  const compatibilityKey = `${testIds}|${panelIds}`;
  const isLoadingCompatibility =
    Boolean(testIds || panelIds) && loadedCompatibilityIds !== compatibilityKey;

  useEffect(() => {
    if (!testIds && !panelIds) return;

    let current = true;
    getFromOpenElisServer(
      `/rest/test-sample-types?testIds=${testIds}&panelIds=${panelIds}`,
      (response) => {
        if (!current) return;
        const map = {};
        (response?.tests || []).forEach((t) => {
          map[compatibilityMapKey(t.testId, false)] =
            t.compatibleSampleTypes || [];
        });
        (response?.panels || []).forEach((p) => {
          map[compatibilityMapKey(p.panelId, true)] =
            p.compatibleSampleTypes || [];
        });
        setTestSampleTypeMap(map);
        setLoadedCompatibilityIds(compatibilityKey);
      },
    );

    return () => {
      current = false;
    };
  }, [testIds, panelIds]);

  const getCompatibleSampleTypes = useCallback(
    (id, isPanel) => testSampleTypeMap[compatibilityMapKey(id, isPanel)] || [],
    [testSampleTypeMap],
  );

  // Get sample assignments for a test
  const getSampleAssignments = useCallback(
    (testId, isPanel) => {
      const assignments = [];
      samples.forEach((sample, index) => {
        if (sample.sampleRejected) return;
        const hasTest = isPanel
          ? sample.panels?.some((panel) => panel.id === testId)
          : sample.tests?.some((test) => test.id === testId);
        if (hasTest) {
          const sampleTypeName =
            sample.sampleTypeName ||
            sampleTypes.find((type) => type.id === sample.sampleTypeId)
              ?.value ||
            `Sample ${index + 1}`;
          assignments.push({ sampleIndex: index, sampleTypeName });
        }
      });
      return assignments;
    },
    [sampleTypes, samples],
  );

  // Handle clicking a sample type tag
  const handleSampleTypeClick = useCallback(
    (test, sampleType) => {
      if (isReadOnly) return;
      setSelectedTest(test);
      setSelectedSampleType(sampleType);
      setIsModalOpen(true);
    },
    [isReadOnly],
  );

  // Handle modal assignment
  const handleAssign = (sampleIndex, createNew) => {
    if (createNew) {
      // Create a new sample with this sample type and test
      const newSample = {
        index: samples.length,
        sampleRejected: false,
        rejectionReason: "",
        sampleTypeId: selectedSampleType.id,
        sampleTypeName: selectedSampleType.name,
        sampleXML: null,
        panels: selectedTest.isPanel
          ? [{ id: selectedTest.id, name: selectedTest.name }]
          : [],
        tests: selectedTest.isPanel
          ? []
          : [{ id: selectedTest.id, name: selectedTest.name }],
        requestReferralEnabled: false,
        referralItems: [],
        quantity: "",
        quantityUnit: "mL",
        collectionConditions: "",
        collectionDate: "",
        collectionTime: "",
        collectorId: "",
        receivedDate: "",
        receivedTime: "",
        receivedBy: "",
        hasNCE: false,
        nceId: "",
      };
      setSamples([...samples, newSample]);
    } else {
      // Add test to existing sample
      assignTestToSample(
        selectedTest.id,
        selectedTest.name,
        selectedTest.isPanel,
        sampleIndex,
      );
    }
    setIsModalOpen(false);
    setSelectedTest(null);
    setSelectedSampleType(null);
  };

  // Get existing samples of a specific sample type
  const getSamplesOfType = (sampleTypeId) => {
    return getAssignableSamplesOfType(samples, sampleTypeId);
  };

  // Table headers
  const headers = useMemo(
    () => [
      {
        key: "testPanel",
        header: intl.formatMessage({
          id: "collect.table.testPanel",
          defaultMessage: "Test / Panel",
        }),
      },
      {
        key: "compatibleTypes",
        header: intl.formatMessage({
          id: "collect.table.compatibleTypes",
          defaultMessage: "Compatible Sample Types",
        }),
      },
      {
        key: "sampleAssignments",
        header: intl.formatMessage({
          id: "collect.table.sampleAssignments",
          defaultMessage: "Sample Assignment(s)",
        }),
      },
      { key: "actions", header: "" },
    ],
    [intl],
  );

  // Build table rows
  const rows = useMemo(
    () =>
      requestedItems.map((item) => {
        const compatibleTypes = getCompatibleSampleTypes(item.id, item.isPanel);
        const assignments = getSampleAssignments(item.id, item.isPanel);
        const mark = item.isPanel ? null : testedElsewhere[String(item.id)];

        return {
          id: `${item.isPanel ? "panel" : "test"}-${item.id}`,
          testPanel: (
            <div className="test-panel-cell">
              {item.isPanel && (
                <Tag type="blue" size="sm" className="panel-indicator">
                  <FormattedMessage id="label.panel" defaultMessage="Panel" />
                </Tag>
              )}
              <span className={item.isPanel ? "panel-name" : "test-name"}>
                {item.name}
              </span>
              {item.isPanel && item.testIds && (
                <span className="panel-test-count">
                  ({item.testIds.split(",").length}{" "}
                  <FormattedMessage id="label.tests" defaultMessage="tests" />)
                </span>
              )}
              {mark && (
                <Tag
                  type="purple"
                  size="sm"
                  data-testid={`tested-elsewhere-tag-${item.id}`}
                >
                  {mark.performingLabName ? (
                    <FormattedMessage
                      id="order.tests.testedElsewhereAt"
                      values={{ lab: mark.performingLabName }}
                    />
                  ) : (
                    <FormattedMessage id="order.tests.tag.testedElsewhere" />
                  )}
                </Tag>
              )}
              {mark && (
                <TestedElsewhereFields
                  key={`${item.id}-${mark.performingLabId || ""}`}
                  testId={item.id}
                  mark={mark}
                  onSave={(changes) => saveTestedElsewhere(item.id, changes)}
                  disabled={isReadOnly}
                />
              )}
            </div>
          ),
          compatibleTypes: (
            <div className="compatible-types-cell">
              {isLoadingCompatibility ? (
                <span className="loading-text">Loading...</span>
              ) : compatibleTypes.length > 0 ? (
                compatibleTypes.map((st) => (
                  <OperationalTag
                    key={st.id}
                    type="green"
                    size="sm"
                    className="sample-type-tag clickable"
                    text={`+ ${st.name}`}
                    onClick={() => handleSampleTypeClick(item, st)}
                  />
                ))
              ) : (
                <span
                  className="no-compatible-types"
                  data-testid={`no-compatible-types-${item.id}`}
                >
                  <FormattedMessage
                    id="collect.noCompatibleSampleTypes"
                    defaultMessage="No sample type is set up for this test in the test catalog"
                  />
                </span>
              )}
            </div>
          ),
          sampleAssignments: (
            <div className="sample-assignments-cell">
              {assignments.length > 0 ? (
                assignments.map((a, i) => (
                  <Tag
                    key={i}
                    type="purple"
                    size="sm"
                    className="assignment-tag"
                  >
                    <Checkmark size={12} className="checkmark-icon" />
                    {a.sampleTypeName} (Sample {a.sampleIndex + 1})
                  </Tag>
                ))
              ) : mark ? (
                <span className="tested-elsewhere-at">
                  <FormattedMessage
                    id="order.tests.testedElsewhereAt"
                    values={{
                      lab:
                        mark.performingLabName ||
                        intl.formatMessage({ id: "common.notRecorded" }),
                    }}
                  />
                </span>
              ) : (
                <span className="no-assignment">
                  <FormattedMessage
                    id="collect.noSampleYet"
                    defaultMessage="— No sample yet"
                  />
                </span>
              )}
            </div>
          ),
          actions:
            !item.isPanel && labNumber && !isReadOnly ? (
              <OverflowMenu
                size="sm"
                flipped
                aria-label={intl.formatMessage({ id: "common.moreActions" })}
                iconDescription={intl.formatMessage({
                  id: "common.moreActions",
                })}
                data-testid={`test-row-actions-${item.id}`}
              >
                {mark ? (
                  <OverflowMenuItem
                    itemText={intl.formatMessage({
                      id: "order.tests.action.unmarkTestedElsewhere",
                    })}
                    onClick={() => unmarkTest(item.id)}
                  />
                ) : (
                  <OverflowMenuItem
                    itemText={intl.formatMessage({
                      id: "order.tests.action.markTestedElsewhere",
                    })}
                    onClick={() => markTest(item.id)}
                  />
                )}
              </OverflowMenu>
            ) : null,
        };
      }),
    [
      getCompatibleSampleTypes,
      getSampleAssignments,
      handleSampleTypeClick,
      intl,
      isLoadingCompatibility,
      isReadOnly,
      labNumber,
      markTest,
      requestedItems,
      sampleTypes,
      saveTestedElsewhere,
      testedElsewhere,
      unmarkTest,
    ],
  );

  if (requestedItems.length === 0) {
    return (
      <Tile className="order-section requested-tests-section">
        <h4 className="section-title">
          <FormattedMessage
            id="collect.requestedTests.title"
            defaultMessage="Requested Tests"
          />
        </h4>
        <InlineNotification
          kind="info"
          title=""
          subtitle={intl.formatMessage({
            id: "collect.noTestsOrdered",
            defaultMessage:
              "No tests or panels have been ordered yet. Go back to Step 1 to add tests.",
          })}
          hideCloseButton
          lowContrast
        />
      </Tile>
    );
  }

  return (
    <Tile className="order-section requested-tests-section">
      <h4 className="section-title">
        <FormattedMessage
          id="collect.requestedTests.title"
          defaultMessage="Requested Tests"
        />
      </h4>

      <p className="section-description">
        <FormattedMessage
          id="collect.requestedTests.info"
          defaultMessage="Tests and panels ordered in Step 1. Click a sample type to assign. If a matching sample already exists, you choose whether to add to it or draw a new sample. If no match, a new sample is created directly. A test or panel can be assigned to multiple samples when needed."
        />
      </p>

      {testedElsewhereError && (
        <InlineNotification
          kind="error"
          lowContrast
          title=""
          subtitle={testedElsewhereError}
          onCloseButtonClick={() => setTestedElsewhereError("")}
          data-testid="tested-elsewhere-error"
        />
      )}
      <DataTable rows={rows} headers={headers} size="lg">
        {({ rows, headers, getTableProps, getHeaderProps, getRowProps }) => (
          <Table {...getTableProps()}>
            <TableHead>
              <TableRow>
                {headers.map((header) => (
                  <TableHeader key={header.key} {...getHeaderProps({ header })}>
                    {header.header}
                  </TableHeader>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.id} {...getRowProps({ row })}>
                  {row.cells.map((cell) => (
                    <TableCell key={cell.id}>{cell.value}</TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </DataTable>

      {/* Example multi-sample assignment note */}
      <div className="multi-sample-example">
        <h6>
          <FormattedMessage
            id="collect.multiSampleExample.title"
            defaultMessage="Example: Multi-Sample Assignment"
          />
        </h6>
        <p>
          <FormattedMessage
            id="collect.multiSampleExample.description"
            defaultMessage="A test or panel can be assigned to multiple samples. The 'Sample Assignment(s)' column reflects all assignments."
          />
        </p>
        <div className="example-tags">
          <Tag type="purple" size="sm">
            <Checkmark size={12} /> Plasma (Sample 1)
          </Tag>
          <span className="plus-sign">+</span>
          <Tag type="purple" size="sm">
            <Checkmark size={12} /> Plasma (Sample 2)
          </Tag>
          <span className="equals-sign">=</span>
          <span className="example-text">
            <FormattedMessage
              id="collect.multiSampleExample.result"
              defaultMessage="same test on two separate Plasma draws"
            />
          </span>
        </div>
      </div>

      {/* Assignment Modal */}
      <TestAssignmentModal
        isOpen={isModalOpen}
        onClose={() => {
          setIsModalOpen(false);
          setSelectedTest(null);
          setSelectedSampleType(null);
        }}
        test={selectedTest}
        sampleType={selectedSampleType}
        existingSamples={
          selectedSampleType ? getSamplesOfType(selectedSampleType.id) : []
        }
        onAssign={handleAssign}
      />
    </Tile>
  );
};

export default RequestedTestsSection;
