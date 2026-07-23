import React, { useState, useEffect, useRef } from "react";
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
  InlineNotification,
} from "@carbon/react";
import { Checkmark } from "@carbon/icons-react";
import { getFromOpenElisServer } from "../../../utils/Utils";
import TestAssignmentModal from "./TestAssignmentModal";

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
  testSampleAssignments,
  assignTestToSample,
  removeTestFromSample,
  sampleTypes,
  isReadOnly,
}) => {
  const intl = useIntl();
  const componentMounted = useRef(true);

  // Modal state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedTest, setSelectedTest] = useState(null);
  const [selectedSampleType, setSelectedSampleType] = useState(null);

  // Test-to-sample-type compatibility cache
  const [testSampleTypeMap, setTestSampleTypeMap] = useState({});
  const [isLoadingCompatibility, setIsLoadingCompatibility] = useState(false);

  // Collect all unique tests from all samples
  const allTests = [];
  const allPanels = [];
  const seenTestIds = new Set();
  const seenPanelIds = new Set();

  samples.forEach((sample) => {
    if (sample.tests) {
      sample.tests.forEach((test) => {
        if (!seenTestIds.has(test.id)) {
          seenTestIds.add(test.id);
          allTests.push(test);
        }
      });
    }
    if (sample.panels) {
      sample.panels.forEach((panel) => {
        if (!seenPanelIds.has(panel.id)) {
          seenPanelIds.add(panel.id);
          allPanels.push(panel);
        }
      });
    }
  });

  // Combine panels and tests for display
  const requestedItems = [
    ...allPanels.map((p) => ({ ...p, isPanel: true })),
    ...allTests.map((t) => ({ ...t, isPanel: false })),
  ];

  // Fetch test-sample-type compatibility when tests change
  useEffect(() => {
    componentMounted.current = true;

    const testIds = allTests.map((t) => t.id).join(",");
    if (!testIds) return;

    setIsLoadingCompatibility(true);
    getFromOpenElisServer(
      `/rest/test-sample-types?testIds=${testIds}`,
      (response) => {
        if (componentMounted.current && response?.tests) {
          const map = {};
          response.tests.forEach((t) => {
            map[t.testId] = t.compatibleSampleTypes || [];
          });
          setTestSampleTypeMap(map);
          setIsLoadingCompatibility(false);
        }
      },
    );

    return () => {
      componentMounted.current = false;
    };
  }, [allTests.length]);

  // Get compatible sample types for a test
  const getCompatibleSampleTypes = (testId) => {
    return testSampleTypeMap[testId] || [];
  };

  // Get sample assignments for a test
  const getSampleAssignments = (testId, isPanel) => {
    // Check which samples have this test
    const assignments = [];
    samples.forEach((sample, index) => {
      const hasTest = isPanel
        ? sample.panels?.some((p) => p.id === testId)
        : sample.tests?.some((t) => t.id === testId);
      if (hasTest) {
        const sampleTypeName =
          sample.sampleTypeName ||
          sampleTypes.find((st) => st.id === sample.sampleTypeId)?.value ||
          `Sample ${index + 1}`;
        assignments.push({
          sampleIndex: index,
          sampleTypeName,
        });
      }
    });
    return assignments;
  };

  // Handle clicking a sample type tag
  const handleSampleTypeClick = (test, sampleType) => {
    if (isReadOnly) return;
    setSelectedTest(test);
    setSelectedSampleType(sampleType);
    setIsModalOpen(true);
  };

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
    return samples
      .map((sample, index) => ({ ...sample, index }))
      .filter((sample) => sample.sampleTypeId === sampleTypeId);
  };

  // Table headers
  const headers = [
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
  ];

  // Build table rows
  const rows = requestedItems.map((item) => {
    const compatibleTypes = getCompatibleSampleTypes(item.id);
    const assignments = getSampleAssignments(item.id, item.isPanel);

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
        </div>
      ),
      compatibleTypes: (
        <div className="compatible-types-cell">
          {isLoadingCompatibility ? (
            <span className="loading-text">Loading...</span>
          ) : compatibleTypes.length > 0 ? (
            compatibleTypes.map((st) => (
              <Tag
                key={st.id}
                type="green"
                size="sm"
                className="sample-type-tag clickable"
                onClick={() => handleSampleTypeClick(item, st)}
              >
                + {st.name} {st.code ? `(${st.code})` : ""}
              </Tag>
            ))
          ) : (
            // If no compatibility data, show all sample types as options
            sampleTypes.slice(0, 5).map((st) => (
              <Tag
                key={st.id}
                type="green"
                size="sm"
                className="sample-type-tag clickable"
                onClick={() =>
                  handleSampleTypeClick(item, { id: st.id, name: st.value })
                }
              >
                + {st.value}
              </Tag>
            ))
          )}
        </div>
      ),
      sampleAssignments: (
        <div className="sample-assignments-cell">
          {assignments.length > 0 ? (
            assignments.map((a, i) => (
              <Tag key={i} type="purple" size="sm" className="assignment-tag">
                <Checkmark size={12} className="checkmark-icon" />
                {a.sampleTypeName} (Sample {a.sampleIndex + 1})
              </Tag>
            ))
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
    };
  });

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
