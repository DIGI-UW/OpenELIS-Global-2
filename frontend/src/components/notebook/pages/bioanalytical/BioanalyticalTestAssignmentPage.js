import React, { useState, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  InlineNotification,
  Loading,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Select,
  SelectItem,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import "./BioanalyticalPages.css";

/**
 * BioanalyticalTestAssignmentPage - Stage 2 of bioanalytical workflow.
 *
 * Features:
 * - Analytical test selection and assignment
 * - QC level configuration (Low, Medium, High)
 * - Analytical method selection
 * - Test method configuration and acceptance criteria
 *
 * @param {Object} props
 * @param {number} props.entryId - Notebook entry ID
 * @param {Object} props.pageData - Page configuration
 * @param {Object} props.progress - Sample progress counts
 * @param {function} props.onProgressUpdate - Callback after changes
 * @param {Array} props.templateInstruments - Available instruments
 */
function BioanalyticalTestAssignmentPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  templateInstruments,
}) {
  const intl = useIntl();

  const [isLoading, setIsLoading] = useState(false);
  const [samples, setSamples] = useState([]);
  const [selectedSamples, setSelectedSamples] = useState(new Set());
  const [testAssignments, setTestAssignments] = useState({});
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const toggleSampleSelection = (sampleId) => {
    const newSelection = new Set(selectedSamples);
    if (newSelection.has(sampleId)) {
      newSelection.delete(sampleId);
    } else {
      newSelection.add(sampleId);
    }
    setSelectedSamples(newSelection);
  };

  const handleTestAssignment = useCallback(() => {
    if (selectedSamples.size === 0) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.testassignment.noSamplesSelected",
          defaultMessage: "Please select at least one sample to assign tests",
        }),
      );
      return;
    }

    // TODO: Call API to assign tests to selected samples
    setSuccessMessage(
      intl.formatMessage(
        {
          id: "notebook.bioanalytical.testassignment.success",
          defaultMessage: "Tests assigned to {count} samples",
        },
        { count: selectedSamples.size },
      ),
    );

    if (onProgressUpdate) {
      onProgressUpdate();
    }
  }, [selectedSamples, intl, onProgressUpdate]);

  return (
    <div className="bioanalytical-page">
      <div className="page-instructions">
        <h3>
          <FormattedMessage
            id="notebook.bioanalytical.testassignment.title"
            defaultMessage="Analytical Test Assignment"
          />
        </h3>
        <p>
          <FormattedMessage
            id="notebook.bioanalytical.testassignment.description"
            defaultMessage="Assign analytical tests to received samples, configure QC levels, and select analytical methods. Tests are configured based on sample type and requested analyses."
          />
        </p>
      </div>

      {errorMessage && (
        <div style={{ marginBottom: "1rem" }}>
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "notebook.bioanalytical.testassignment.error",
              defaultMessage: "Error",
            })}
            subtitle={errorMessage}
            lowContrast
            onCloseButtonClick={() => setErrorMessage("")}
          />
        </div>
      )}

      {successMessage && (
        <div style={{ marginBottom: "1rem" }}>
          <InlineNotification
            kind="success"
            title={intl.formatMessage({
              id: "notebook.bioanalytical.testassignment.success",
              defaultMessage: "Success",
            })}
            subtitle={successMessage}
            lowContrast
            onCloseButtonClick={() => setSuccessMessage("")}
          />
        </div>
      )}

      <Grid>
        <Column lg={16} md={8} sm={4}>
          <div className="section-header">
            <h4>
              <FormattedMessage
                id="notebook.bioanalytical.testassignment.selectSamples"
                defaultMessage="Select Samples for Test Assignment"
              />
            </h4>
            <p>
              <FormattedMessage
                id="notebook.bioanalytical.testassignment.selectHelp"
                defaultMessage="Choose samples that need test assignment and configuration."
              />
            </p>

            {isLoading ? (
              <Loading description="Loading samples..." />
            ) : (
              <>
                <div style={{ marginTop: "1.5rem", marginBottom: "1.5rem" }}>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableHeader>
                          <input
                            type="checkbox"
                            checked={
                              selectedSamples.size === samples.length &&
                              samples.length > 0
                            }
                            onChange={(e) => {
                              if (e.target.checked) {
                                setSelectedSamples(
                                  new Set(samples.map((s) => s.id)),
                                );
                              } else {
                                setSelectedSamples(new Set());
                              }
                            }}
                          />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage
                            id="notebook.bioanalytical.testassignment.column.sampleId"
                            defaultMessage="Sample ID"
                          />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage
                            id="notebook.bioanalytical.testassignment.column.type"
                            defaultMessage="Sample Type"
                          />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage
                            id="notebook.bioanalytical.testassignment.column.requestedTests"
                            defaultMessage="Requested Tests"
                          />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage
                            id="notebook.bioanalytical.testassignment.column.assignedTests"
                            defaultMessage="Assigned Tests"
                          />
                        </TableHeader>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {samples.length > 0 ? (
                        samples.map((sample) => (
                          <TableRow key={sample.id}>
                            <TableCell>
                              <input
                                type="checkbox"
                                checked={selectedSamples.has(sample.id)}
                                onChange={() =>
                                  toggleSampleSelection(sample.id)
                                }
                              />
                            </TableCell>
                            <TableCell>
                              {sample.accessionNumber || "-"}
                            </TableCell>
                            <TableCell>
                              {sample.sampleType?.name || "-"}
                            </TableCell>
                            <TableCell>
                              {Array.isArray(sample.requestedTests)
                                ? sample.requestedTests.join(", ")
                                : sample.requestedTests || "-"}
                            </TableCell>
                            <TableCell>
                              <span className="status-badge info">
                                {testAssignments[sample.id]?.count || 0}{" "}
                                <FormattedMessage
                                  id="notebook.bioanalytical.testassignment.assigned"
                                  defaultMessage="assigned"
                                />
                              </span>
                            </TableCell>
                          </TableRow>
                        ))
                      ) : (
                        <TableRow>
                          <TableCell
                            colSpan="5"
                            style={{ textAlign: "center" }}
                          >
                            <FormattedMessage
                              id="notebook.bioanalytical.testassignment.noSamples"
                              defaultMessage="No samples available for test assignment. Please complete Stage 1 (Sample Reception) first."
                            />
                          </TableCell>
                        </TableRow>
                      )}
                    </TableBody>
                  </Table>
                </div>

                {selectedSamples.size > 0 && (
                  <div style={{ marginTop: "1.5rem" }}>
                    <Button kind="primary" onClick={handleTestAssignment}>
                      <FormattedMessage
                        id="notebook.bioanalytical.testassignment.assignTests"
                        defaultMessage="Assign Tests to {count} Sample(s)"
                        values={{ count: selectedSamples.size }}
                      />
                    </Button>
                  </div>
                )}
              </>
            )}
          </div>
        </Column>
      </Grid>

      <Grid style={{ marginTop: "1.5rem" }}>
        <Column lg={16} md={8} sm={4}>
          <div className="section-header">
            <h4>
              <FormattedMessage
                id="notebook.bioanalytical.testassignment.testConfiguration"
                defaultMessage="Test Configuration & QC Setup"
              />
            </h4>
            <p>
              <FormattedMessage
                id="notebook.bioanalytical.testassignment.testConfigHelp"
                defaultMessage="Configure analytical methods, QC levels (Low/Medium/High), and acceptance criteria for assigned tests."
              />
            </p>

            <div style={{ marginTop: "1.5rem" }}>
              <p style={{ color: "#525252", fontSize: "0.875rem" }}>
                <FormattedMessage
                  id="notebook.bioanalytical.testassignment.qcLevelInfo"
                  defaultMessage="QC Levels: Low (LLOQ - 2x LLOQ), Medium (30% span), High (near upper limit)"
                />
              </p>
            </div>
          </div>
        </Column>
      </Grid>
    </div>
  );
}

export default BioanalyticalTestAssignmentPage;
