import React, { useState, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  InlineNotification,
  Loading,
  Tabs,
  TabList,
  Tab,
  TabPanel,
  TabPanels,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Select,
  SelectItem,
  Checkbox,
  TextArea,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import "./BioanalyticalPages.css";

/**
 * BioanalyticalReportingPage - Stage 4 of bioanalytical workflow.
 *
 * Features:
 * - Bioequivalence study result review and validation
 * - Quality assurance (QA) approval workflow
 * - Regulatory compliance verification
 * - External reporting and export (LMIS, REDCap, CDISC/SDTM)
 * - Final data release authorization
 *
 * @param {Object} props
 * @param {number} props.entryId - Notebook entry ID
 * @param {Object} props.pageData - Page configuration
 * @param {Object} props.progress - Sample progress counts
 * @param {function} props.onProgressUpdate - Callback after changes
 */
function BioanalyticalReportingPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();

  const [isLoading, setIsLoading] = useState(false);
  const [selectedTab, setSelectedTab] = useState(0);
  const [studyResults, setStudyResults] = useState([]);
  const [qaComments, setQaComments] = useState("");
  const [qaApproved, setQaApproved] = useState(false);
  const [exportFormat, setExportFormat] = useState("");
  const [exportStatus, setExportStatus] = useState(null);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const exportFormats = [
    { id: "lmis", label: "LMIS (Laboratory Management Information System)" },
    { id: "redcap", label: "REDCap (Research Electronic Data Capture)" },
    { id: "cdisc", label: "CDISC/SDTM (Clinical Data Interchange Standards)" },
    { id: "research", label: "Research Format (CSV)" },
    { id: "pdf", label: "PDF Report" },
  ];

  const loadStudyResults = useCallback(() => {
    setIsLoading(true);

    setTimeout(() => {
      const mockResults = [
        {
          id: "1",
          testName: "Analyte X - Plasma",
          dataPoints: 24,
          mean: "1245.3 ng/mL",
          sd: "156.8",
          cv: "12.6%",
          min: "987.2 ng/mL",
          max: "1512.1 ng/mL",
          regulatoryStatus: "COMPLIANT",
        },
        {
          id: "2",
          testName: "Analyte X - Urine",
          dataPoints: 24,
          mean: "87.5 ng/mL",
          sd: "11.2",
          cv: "12.8%",
          min: "65.3 ng/mL",
          max: "112.4 ng/mL",
          regulatoryStatus: "COMPLIANT",
        },
        {
          id: "3",
          testName: "Metabolite Y - Plasma",
          dataPoints: 24,
          mean: "312.7 ng/mL",
          sd: "42.1",
          cv: "13.5%",
          min: "245.1 ng/mL",
          max: "389.2 ng/mL",
          regulatoryStatus: "COMPLIANT",
        },
      ];
      setStudyResults(mockResults);
      setIsLoading(false);
    }, 1500);
  }, []);

  React.useEffect(() => {
    loadStudyResults();
  }, []);

  const handleQaApproval = useCallback(() => {
    if (!qaApproved) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.reporting.confirmQa",
          defaultMessage:
            "Please confirm QA approval checkbox before proceeding",
        }),
      );
      return;
    }

    if (!qaComments.trim()) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.reporting.qaCommentsRequired",
          defaultMessage: "QA review comments are required",
        }),
      );
      return;
    }

    setSuccessMessage(
      intl.formatMessage({
        id: "notebook.bioanalytical.reporting.qaApprovalComplete",
        defaultMessage:
          "QA approval completed. Study data is ready for external reporting.",
      }),
    );

    if (onProgressUpdate) {
      onProgressUpdate();
    }
  }, [qaApproved, qaComments, intl, onProgressUpdate]);

  const handleExport = useCallback(() => {
    if (!exportFormat) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.reporting.selectFormat",
          defaultMessage: "Please select an export format",
        }),
      );
      return;
    }

    if (!qaApproved) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.reporting.qaRequired",
          defaultMessage: "QA approval is required before exporting data",
        }),
      );
      return;
    }

    setIsLoading(true);

    // Simulate export process
    setTimeout(() => {
      const selectedExportFormat = exportFormats.find(
        (f) => f.id === exportFormat,
      );
      setExportStatus({
        format: selectedExportFormat.label,
        records: studyResults.length,
        status: "EXPORT_COMPLETE",
        timestamp: new Date().toLocaleString(),
        filename: `bioanalytical_study_${entryId}_${exportFormat}.${exportFormat === "pdf" ? "pdf" : "csv"}`,
      });

      setSuccessMessage(
        intl.formatMessage(
          {
            id: "notebook.bioanalytical.reporting.exportSuccess",
            defaultMessage:
              "Export to {format} completed successfully. {records} records exported.",
          },
          {
            format: selectedExportFormat.label,
            records: studyResults.length,
          },
        ),
      );
      setIsLoading(false);
    }, 2000);
  }, [exportFormat, qaApproved, studyResults.length, entryId, intl]);

  return (
    <div className="bioanalytical-page">
      <div className="page-instructions">
        <h3>
          <FormattedMessage
            id="notebook.bioanalytical.reporting.title"
            defaultMessage="Quality Assurance Review & External Reporting"
          />
        </h3>
        <p>
          <FormattedMessage
            id="notebook.bioanalytical.reporting.description"
            defaultMessage="Conduct comprehensive QA review of bioequivalence study results, verify regulatory compliance, and export validated data to external systems (LMIS, REDCap, CDISC/SDTM) or research formats."
          />
        </p>
      </div>

      {errorMessage && (
        <div style={{ marginBottom: "1rem" }}>
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "notebook.bioanalytical.reporting.error",
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
              id: "notebook.bioanalytical.reporting.success",
              defaultMessage: "Success",
            })}
            subtitle={successMessage}
            lowContrast
            onCloseButtonClick={() => setSuccessMessage("")}
          />
        </div>
      )}

      <Tabs selectedIndex={selectedTab} onChange={setSelectedTab}>
        <TabList aria-label="Reporting and QA tabs">
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.reporting.tab.results"
              defaultMessage="Study Results Review"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.reporting.tab.qaReview"
              defaultMessage="QA Approval"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.reporting.tab.externalExport"
              defaultMessage="External Reporting"
            />
          </Tab>
        </TabList>

        <TabPanels>
          {/* Tab 1: Study Results Review */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.reporting.resultsSection"
                        defaultMessage="Bioequivalence Study Results"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.reporting.resultsHelp"
                        defaultMessage="Review summary statistics for all analyzed analytes and metabolites. Data includes mean values, standard deviation, coefficient of variation, and regulatory compliance status."
                      />
                    </p>

                    {isLoading ? (
                      <Loading description="Loading study results..." />
                    ) : studyResults.length > 0 ? (
                      <div style={{ marginTop: "1.5rem" }}>
                        <Table>
                          <TableHead>
                            <TableRow>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.reporting.testName"
                                  defaultMessage="Test Name"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.reporting.dataPoints"
                                  defaultMessage="Data Points"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.reporting.mean"
                                  defaultMessage="Mean"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.reporting.sd"
                                  defaultMessage="Std Dev"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.reporting.cv"
                                  defaultMessage="CV %"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.reporting.range"
                                  defaultMessage="Min - Max"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.reporting.status"
                                  defaultMessage="Status"
                                />
                              </TableHeader>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {studyResults.map((result) => (
                              <TableRow key={result.id}>
                                <TableCell>{result.testName}</TableCell>
                                <TableCell>{result.dataPoints}</TableCell>
                                <TableCell>{result.mean}</TableCell>
                                <TableCell>{result.sd}</TableCell>
                                <TableCell>{result.cv}</TableCell>
                                <TableCell style={{ fontSize: "0.875rem" }}>
                                  {result.min} - {result.max}
                                </TableCell>
                                <TableCell>
                                  <span
                                    className="status-badge"
                                    style={{
                                      backgroundColor: "#24a148",
                                      color: "white",
                                      padding: "0.25rem 0.5rem",
                                      borderRadius: "4px",
                                      fontSize: "0.75rem",
                                    }}
                                  >
                                    {result.regulatoryStatus}
                                  </span>
                                </TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>

                        <div
                          style={{
                            marginTop: "1.5rem",
                            padding: "1rem",
                            backgroundColor: "#e7f1f5",
                            borderRadius: "4px",
                            borderLeft: "4px solid #0043ce",
                          }}
                        >
                          <p style={{ fontSize: "0.875rem", margin: 0 }}>
                            <strong>
                              <FormattedMessage
                                id="notebook.bioanalytical.reporting.complianceNote"
                                defaultMessage="Regulatory Compliance Summary:"
                              />
                            </strong>
                          </p>
                          <p
                            style={{
                              fontSize: "0.875rem",
                              color: "#161616",
                              margin: "0.25rem 0 0 0",
                            }}
                          >
                            ✓ All analytes meet FDA bioequivalence criteria (CV
                            &lt; 20%, Mean accuracy 80-120%)
                          </p>
                          <p
                            style={{
                              fontSize: "0.875rem",
                              color: "#161616",
                              margin: "0.25rem 0 0 0",
                            }}
                          >
                            ✓ Quality control parameters within specified limits
                          </p>
                          <p
                            style={{
                              fontSize: "0.875rem",
                              color: "#161616",
                              margin: "0.25rem 0 0 0",
                            }}
                          >
                            ✓ Data integrity verified (no anomalies detected)
                          </p>
                        </div>
                      </div>
                    ) : (
                      <div
                        style={{
                          marginTop: "1.5rem",
                          padding: "1rem",
                          backgroundColor: "#f4f4f4",
                          borderRadius: "4px",
                          textAlign: "center",
                        }}
                      >
                        <p style={{ color: "#525252" }}>
                          <FormattedMessage
                            id="notebook.bioanalytical.reporting.noResults"
                            defaultMessage="No study results available"
                          />
                        </p>
                      </div>
                    )}
                  </div>
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* Tab 2: QA Approval */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.reporting.qaSection"
                        defaultMessage="Quality Assurance Approval"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.reporting.qaHelp"
                        defaultMessage="Conduct final QA review before data release. Document review findings and approval decision. Only approved studies can be exported to external systems."
                      />
                    </p>

                    <div
                      style={{
                        marginTop: "1.5rem",
                        padding: "1rem",
                        backgroundColor: "#f4f4f4",
                        borderRadius: "4px",
                      }}
                    >
                      <h5 style={{ marginBottom: "1rem" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.reporting.qaChecklist"
                          defaultMessage="QA Review Checklist"
                        />
                      </h5>
                      <div
                        style={{
                          display: "flex",
                          flexDirection: "column",
                          gap: "0.75rem",
                        }}
                      >
                        <div style={{ display: "flex", alignItems: "center" }}>
                          <Checkbox id="qa-check-1" checked={true} readOnly />
                          <label
                            htmlFor="qa-check-1"
                            style={{
                              marginLeft: "0.5rem",
                              fontSize: "0.875rem",
                            }}
                          >
                            <FormattedMessage
                              id="notebook.bioanalytical.reporting.qaCheck1"
                              defaultMessage="All raw data files validated and processed"
                            />
                          </label>
                        </div>
                        <div style={{ display: "flex", alignItems: "center" }}>
                          <Checkbox id="qa-check-2" checked={true} readOnly />
                          <label
                            htmlFor="qa-check-2"
                            style={{
                              marginLeft: "0.5rem",
                              fontSize: "0.875rem",
                            }}
                          >
                            <FormattedMessage
                              id="notebook.bioanalytical.reporting.qaCheck2"
                              defaultMessage="Calibration curves meet acceptance criteria (r² ≥ 0.99)"
                            />
                          </label>
                        </div>
                        <div style={{ display: "flex", alignItems: "center" }}>
                          <Checkbox id="qa-check-3" checked={true} readOnly />
                          <label
                            htmlFor="qa-check-3"
                            style={{
                              marginLeft: "0.5rem",
                              fontSize: "0.875rem",
                            }}
                          >
                            <FormattedMessage
                              id="notebook.bioanalytical.reporting.qaCheck3"
                              defaultMessage="QC results pass Westgard rules (all 5 rules passed)"
                            />
                          </label>
                        </div>
                        <div style={{ display: "flex", alignItems: "center" }}>
                          <Checkbox id="qa-check-4" checked={true} readOnly />
                          <label
                            htmlFor="qa-check-4"
                            style={{
                              marginLeft: "0.5rem",
                              fontSize: "0.875rem",
                            }}
                          >
                            <FormattedMessage
                              id="notebook.bioanalytical.reporting.qaCheck4"
                              defaultMessage="System suitability parameters verified"
                            />
                          </label>
                        </div>
                        <div style={{ display: "flex", alignItems: "center" }}>
                          <Checkbox id="qa-check-5" checked={true} readOnly />
                          <label
                            htmlFor="qa-check-5"
                            style={{
                              marginLeft: "0.5rem",
                              fontSize: "0.875rem",
                            }}
                          >
                            <FormattedMessage
                              id="notebook.bioanalytical.reporting.qaCheck5"
                              defaultMessage="Sample results within acceptance criteria"
                            />
                          </label>
                        </div>
                      </div>
                    </div>

                    <div style={{ marginTop: "1.5rem" }}>
                      <label
                        htmlFor="qa-comments"
                        style={{
                          display: "block",
                          marginBottom: "0.5rem",
                          fontWeight: "bold",
                          fontSize: "0.875rem",
                        }}
                      >
                        <FormattedMessage
                          id="notebook.bioanalytical.reporting.qaComments"
                          defaultMessage="QA Review Comments"
                        />
                      </label>
                      <TextArea
                        id="qa-comments"
                        placeholder={intl.formatMessage({
                          id: "notebook.bioanalytical.reporting.qaCommentsPlaceholder",
                          defaultMessage:
                            "Document any observations, deviations, or approvals...",
                        })}
                        value={qaComments}
                        onChange={(e) => setQaComments(e.target.value)}
                        rows={6}
                      />
                    </div>

                    <div
                      style={{
                        marginTop: "1.5rem",
                        display: "flex",
                        alignItems: "center",
                        gap: "0.5rem",
                      }}
                    >
                      <Checkbox
                        id="qa-approve"
                        checked={qaApproved}
                        onChange={(e) => setQaApproved(e.target.checked)}
                      />
                      <label
                        htmlFor="qa-approve"
                        style={{ fontSize: "0.875rem", fontWeight: "bold" }}
                      >
                        <FormattedMessage
                          id="notebook.bioanalytical.reporting.qaApprovalConfirm"
                          defaultMessage="I confirm QA approval and authorize data release"
                        />
                      </label>
                    </div>

                    <div style={{ marginTop: "1.5rem" }}>
                      <Button kind="primary" onClick={handleQaApproval}>
                        <FormattedMessage
                          id="notebook.bioanalytical.reporting.completeQa"
                          defaultMessage="Complete QA Approval"
                        />
                      </Button>
                    </div>
                  </div>
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* Tab 3: External Reporting */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.reporting.exportSection"
                        defaultMessage="External Data Export"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.reporting.exportHelp"
                        defaultMessage="Export validated study data to external systems supporting LMIS (CHAI integration), REDCap clinical data management, CDISC/SDTM regulatory formats, or custom research formats."
                      />
                    </p>

                    <div style={{ marginTop: "1.5rem" }}>
                      <Select
                        id="export-format"
                        labelText={intl.formatMessage({
                          id: "notebook.bioanalytical.reporting.selectExportFormat",
                          defaultMessage: "Select Export Format",
                        })}
                        value={exportFormat}
                        onChange={(e) => setExportFormat(e.target.value)}
                        disabled={!qaApproved}
                      >
                        <SelectItem
                          value=""
                          text="-- Choose export format --"
                        />
                        {exportFormats.map((format) => (
                          <SelectItem
                            key={format.id}
                            value={format.id}
                            text={format.label}
                          />
                        ))}
                      </Select>
                      {!qaApproved && (
                        <p
                          style={{
                            marginTop: "0.5rem",
                            fontSize: "0.875rem",
                            color: "#da1e28",
                          }}
                        >
                          <FormattedMessage
                            id="notebook.bioanalytical.reporting.qaRequiredNote"
                            defaultMessage="QA approval is required before exporting data"
                          />
                        </p>
                      )}
                    </div>

                    {exportFormat && qaApproved && (
                      <div
                        style={{
                          marginTop: "1.5rem",
                          padding: "1rem",
                          backgroundColor: "#e7f1f5",
                          borderRadius: "4px",
                          borderLeft: "4px solid #0043ce",
                        }}
                      >
                        <p style={{ fontSize: "0.875rem", margin: 0 }}>
                          <strong>
                            <FormattedMessage
                              id="notebook.bioanalytical.reporting.exportDetails"
                              defaultMessage="Export Details:"
                            />
                          </strong>
                        </p>
                        <p
                          style={{
                            fontSize: "0.875rem",
                            color: "#161616",
                            margin: "0.25rem 0 0 0",
                          }}
                        >
                          {
                            exportFormats.find((f) => f.id === exportFormat)
                              ?.label
                          }
                        </p>
                        <p
                          style={{
                            fontSize: "0.875rem",
                            color: "#161616",
                            margin: "0.25rem 0 0 0",
                          }}
                        >
                          <FormattedMessage
                            id="notebook.bioanalytical.reporting.recordsToExport"
                            defaultMessage="Records: {count}"
                            values={{ count: studyResults.length }}
                          />
                        </p>
                      </div>
                    )}

                    {exportStatus && (
                      <div
                        style={{
                          marginTop: "1.5rem",
                          padding: "1rem",
                          backgroundColor: "#d0e2d4",
                          borderRadius: "4px",
                          borderLeft: "4px solid #24a148",
                        }}
                      >
                        <p
                          style={{
                            fontSize: "0.875rem",
                            margin: 0,
                            fontWeight: "bold",
                          }}
                        >
                          <FormattedMessage
                            id="notebook.bioanalytical.reporting.exportCompleted"
                            defaultMessage="Export Completed"
                          />
                        </p>
                        <p
                          style={{
                            fontSize: "0.875rem",
                            color: "#161616",
                            margin: "0.5rem 0 0 0",
                          }}
                        >
                          <strong>
                            <FormattedMessage
                              id="notebook.bioanalytical.reporting.format"
                              defaultMessage="Format:"
                            />
                          </strong>{" "}
                          {exportStatus.format}
                        </p>
                        <p
                          style={{
                            fontSize: "0.875rem",
                            color: "#161616",
                            margin: "0.25rem 0 0 0",
                          }}
                        >
                          <strong>
                            <FormattedMessage
                              id="notebook.bioanalytical.reporting.recordsExported"
                              defaultMessage="Records Exported:"
                            />
                          </strong>{" "}
                          {exportStatus.records}
                        </p>
                        <p
                          style={{
                            fontSize: "0.875rem",
                            color: "#161616",
                            margin: "0.25rem 0 0 0",
                          }}
                        >
                          <strong>
                            <FormattedMessage
                              id="notebook.bioanalytical.reporting.filename"
                              defaultMessage="File:"
                            />
                          </strong>{" "}
                          {exportStatus.filename}
                        </p>
                        <p
                          style={{
                            fontSize: "0.875rem",
                            color: "#525252",
                            margin: "0.25rem 0 0 0",
                          }}
                        >
                          {exportStatus.timestamp}
                        </p>
                      </div>
                    )}

                    {qaApproved && (
                      <div style={{ marginTop: "1.5rem" }}>
                        <Button
                          kind="primary"
                          onClick={handleExport}
                          disabled={!exportFormat || isLoading}
                        >
                          {isLoading ? (
                            <>
                              <Loading description="Exporting..." />
                            </>
                          ) : (
                            <FormattedMessage
                              id="notebook.bioanalytical.reporting.exportNow"
                              defaultMessage="Export Now"
                            />
                          )}
                        </Button>
                      </div>
                    )}
                  </div>
                </Column>
              </Grid>
            </div>
          </TabPanel>
        </TabPanels>
      </Tabs>
    </div>
  );
}

export default BioanalyticalReportingPage;
