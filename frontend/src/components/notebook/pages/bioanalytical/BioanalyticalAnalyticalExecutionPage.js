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
  FileUploader,
  Checkbox,
  NumberInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import "./BioanalyticalPages.css";

/**
 * BioanalyticalAnalyticalExecutionPage - Stage 3 of bioanalytical workflow.
 *
 * Features:
 * - Analytical instrument data upload (mzML, CDF, CSV, PDF)
 * - Instrument type selection and file format validation
 * - Calibration curve validation (r², slope, intercept)
 * - QC result tracking with Westgard rule detection
 * - Levey-Jennings trending data management
 * - System suitability verification
 * - Data integrity and acceptance validation
 *
 * @param {Object} props
 * @param {number} props.entryId - Notebook entry ID
 * @param {Object} props.pageData - Page configuration
 * @param {Object} props.progress - Sample progress counts
 * @param {function} props.onProgressUpdate - Callback after changes
 * @param {Array} props.templateInstruments - Available instruments
 */
function BioanalyticalAnalyticalExecutionPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  templateInstruments,
}) {
  const intl = useIntl();

  const [isLoading, setIsLoading] = useState(false);
  const [selectedTab, setSelectedTab] = useState(0);
  const [selectedInstrument, setSelectedInstrument] = useState("");
  const [uploadedFiles, setUploadedFiles] = useState([]);
  const [analyzerResults, setAnalyzerResults] = useState([]);
  const [calibrationData, setCalibrationData] = useState(null);
  const [qcResults, setQcResults] = useState([]);
  const [westgardRules, setWestgardRules] = useState([]);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  // Mock instrument list (would come from props in real implementation)
  const instruments = [
    {
      id: "1",
      name: "LC-MS/MS System",
      type: "LCMS",
      formats: ["mzML", "CDF"],
    },
    { id: "2", name: "HPLC System", type: "HPLC", formats: ["CSV", "PDF"] },
    {
      id: "3",
      name: "Dissolution Tester",
      type: "DISSOLUTION",
      formats: ["CSV"],
    },
    {
      id: "4",
      name: "USP Apparatus II",
      type: "APPARATUS",
      formats: ["CSV", "PDF"],
    },
  ];

  const handleFileUpload = useCallback(
    (files) => {
      if (files.length === 0) return;

      const file = files[0];
      const fileExtension = file.name.split(".").pop().toUpperCase();

      // Validate file format based on selected instrument
      const instrument = instruments.find((i) => i.id === selectedInstrument);
      if (!instrument) {
        setErrorMessage(
          intl.formatMessage({
            id: "notebook.bioanalytical.execution.selectInstrument",
            defaultMessage: "Please select an instrument first",
          }),
        );
        return;
      }

      if (!instrument.formats.includes(fileExtension)) {
        setErrorMessage(
          intl.formatMessage(
            {
              id: "notebook.bioanalytical.execution.invalidFormat",
              defaultMessage:
                "File format {ext} not supported for {instrument}. Supported formats: {formats}",
            },
            {
              ext: fileExtension,
              instrument: instrument.name,
              formats: instrument.formats.join(", "),
            },
          ),
        );
        return;
      }

      // Add file to list
      const newFile = {
        id: Date.now(),
        name: file.name,
        size: file.size,
        instrument: instrument.name,
        status: "PENDING_VALIDATION",
        uploadedAt: new Date().toISOString(),
      };

      setUploadedFiles([...uploadedFiles, newFile]);
      setSuccessMessage(
        intl.formatMessage(
          {
            id: "notebook.bioanalytical.execution.fileUploaded",
            defaultMessage: "File {name} uploaded successfully",
          },
          { name: file.name },
        ),
      );
    },
    [selectedInstrument, intl],
  );

  const handleValidateData = useCallback(() => {
    if (uploadedFiles.length === 0) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.execution.noFiles",
          defaultMessage: "Please upload analyzer data files first",
        }),
      );
      return;
    }

    setIsLoading(true);

    // Simulate validation process
    setTimeout(() => {
      // Mock calibration data
      setCalibrationData({
        equation: "y = 0.9876x + 0.0234",
        rSquared: 0.9987,
        slope: 0.9876,
        intercept: 0.0234,
        range: "10-10000 ng/mL",
        pointsUsed: 6,
        qualityAssessment: "PASS",
      });

      // Mock QC results (3 levels: Low, Medium, High)
      const mockQcResults = [
        {
          id: "1",
          level: "LOW",
          spikedConcentration: "20 ng/mL",
          measuredValue: "19.8 ng/mL",
          accuracy: "99.0%",
          precision: "2.1%",
          status: "PASS",
        },
        {
          id: "2",
          level: "MEDIUM",
          spikedConcentration: "500 ng/mL",
          measuredValue: "497.5 ng/mL",
          accuracy: "99.5%",
          precision: "1.8%",
          status: "PASS",
        },
        {
          id: "3",
          level: "HIGH",
          spikedConcentration: "8000 ng/mL",
          measuredValue: "7950 ng/mL",
          accuracy: "99.4%",
          precision: "2.3%",
          status: "PASS",
        },
      ];
      setQcResults(mockQcResults);

      // Mock Westgard rules check
      const mockWestgardRules = [
        {
          rule: "1-3S",
          status: "PASS",
          description: "No value exceeds 3 sigma",
        },
        {
          rule: "2-2S",
          status: "PASS",
          description: "No 2 consecutive values exceed 2 sigma",
        },
        {
          rule: "R-4S",
          status: "PASS",
          description: "Range between consecutive runs does not exceed 4 sigma",
        },
        {
          rule: "4-1S",
          status: "PASS",
          description: "No 4 consecutive values on same side of mean",
        },
        {
          rule: "10X",
          status: "PASS",
          description: "No 10 consecutive values on same side",
        },
      ];
      setWestgardRules(mockWestgardRules);

      // Mock analyzer results
      const mockResults = [
        {
          id: "1",
          sampleId: "S001",
          result: "456.2 ng/mL",
          replicate: "Rep 1",
          quality: "PASSED",
          recoveryRate: "98.5%",
        },
        {
          id: "2",
          sampleId: "S001",
          result: "457.8 ng/mL",
          replicate: "Rep 2",
          quality: "PASSED",
          recoveryRate: "99.1%",
        },
        {
          id: "3",
          sampleId: "S002",
          result: "523.1 ng/mL",
          replicate: "Rep 1",
          quality: "PASSED",
          recoveryRate: "99.8%",
        },
      ];
      setAnalyzerResults(mockResults);

      setSuccessMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.execution.validationComplete",
          defaultMessage: "Data validation completed successfully",
        }),
      );
      setIsLoading(false);
    }, 2000);
  }, [uploadedFiles, intl]);

  const handleApproveResults = useCallback(() => {
    if (analyzerResults.length === 0) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.execution.noResults",
          defaultMessage: "No validated results to approve",
        }),
      );
      return;
    }

    setSuccessMessage(
      intl.formatMessage(
        {
          id: "notebook.bioanalytical.execution.resultsApproved",
          defaultMessage: "{count} sample results approved for QA review",
        },
        { count: analyzerResults.length },
      ),
    );

    if (onProgressUpdate) {
      onProgressUpdate();
    }
  }, [analyzerResults, intl, onProgressUpdate]);

  return (
    <div className="bioanalytical-page">
      <div className="page-instructions">
        <h3>
          <FormattedMessage
            id="notebook.bioanalytical.execution.title"
            defaultMessage="Analytical Execution & Data Acquisition"
          />
        </h3>
        <p>
          <FormattedMessage
            id="notebook.bioanalytical.execution.description"
            defaultMessage="Upload raw analytical instrument data (mzML, CDF, CSV, PDF), validate calibration curves, monitor QC results using Westgard rules, and verify system suitability before approving data for QA review."
          />
        </p>
      </div>

      {errorMessage && (
        <div style={{ marginBottom: "1rem" }}>
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "notebook.bioanalytical.execution.error",
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
              id: "notebook.bioanalytical.execution.success",
              defaultMessage: "Success",
            })}
            subtitle={successMessage}
            lowContrast
            onCloseButtonClick={() => setSuccessMessage("")}
          />
        </div>
      )}

      <Tabs selectedIndex={selectedTab} onChange={setSelectedTab}>
        <TabList aria-label="Analytical execution tabs">
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.execution.tab.dataUpload"
              defaultMessage="Data Upload"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.execution.tab.calibration"
              defaultMessage="Calibration Validation"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.execution.tab.qcResults"
              defaultMessage="QC Results & Trending"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.execution.tab.analyzerResults"
              defaultMessage="Analyzer Results"
            />
          </Tab>
        </TabList>

        <TabPanels>
          {/* Tab 1: Data Upload */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.uploadSection"
                        defaultMessage="Upload Analytical Data"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.uploadHelp"
                        defaultMessage="Select an analytical instrument and upload raw data files (mzML, CDF, CSV, or PDF format). Supported instruments: LC-MS/MS, HPLC, Dissolution, USP Apparatus."
                      />
                    </p>

                    <div style={{ marginTop: "1.5rem" }}>
                      <Select
                        id="instrument-select"
                        labelText={intl.formatMessage({
                          id: "notebook.bioanalytical.execution.selectInstrument",
                          defaultMessage: "Select Instrument",
                        })}
                        value={selectedInstrument}
                        onChange={(e) => setSelectedInstrument(e.target.value)}
                      >
                        <SelectItem
                          value=""
                          text="-- Choose an instrument --"
                        />
                        {instruments.map((instrument) => (
                          <SelectItem
                            key={instrument.id}
                            value={instrument.id}
                            text={instrument.name}
                          />
                        ))}
                      </Select>
                    </div>

                    {selectedInstrument && (
                      <div
                        style={{
                          marginTop: "1.5rem",
                          padding: "1rem",
                          backgroundColor: "#f4f4f4",
                          borderRadius: "4px",
                        }}
                      >
                        <p
                          style={{
                            fontSize: "0.875rem",
                            marginBottom: "0.5rem",
                          }}
                        >
                          <strong>
                            <FormattedMessage
                              id="notebook.bioanalytical.execution.supportedFormats"
                              defaultMessage="Supported Formats:"
                            />
                          </strong>
                        </p>
                        <p style={{ fontSize: "0.875rem", color: "#525252" }}>
                          {instruments
                            .find((i) => i.id === selectedInstrument)
                            ?.formats.join(", ")}
                        </p>
                      </div>
                    )}

                    <div style={{ marginTop: "1.5rem" }}>
                      <FileUploader
                        labelTitle={intl.formatMessage({
                          id: "notebook.bioanalytical.execution.uploadFile",
                          defaultMessage: "Upload File",
                        })}
                        accept={
                          selectedInstrument
                            ? instruments
                                .find((i) => i.id === selectedInstrument)
                                ?.formats.map((f) => `.${f.toLowerCase()}`)
                                .join(",")
                            : ""
                        }
                        multiple={false}
                        onChange={(e) => {
                          if (e.target.files) {
                            handleFileUpload(e.target.files);
                          }
                        }}
                      />
                    </div>

                    {uploadedFiles.length > 0 && (
                      <div style={{ marginTop: "1.5rem" }}>
                        <h5 style={{ marginBottom: "1rem" }}>
                          <FormattedMessage
                            id="notebook.bioanalytical.execution.uploadedFiles"
                            defaultMessage="Uploaded Files ({count})"
                            values={{ count: uploadedFiles.length }}
                          />
                        </h5>
                        <Table>
                          <TableHead>
                            <TableRow>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.fileName"
                                  defaultMessage="File Name"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.size"
                                  defaultMessage="Size"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.instrument"
                                  defaultMessage="Instrument"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.status"
                                  defaultMessage="Status"
                                />
                              </TableHeader>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {uploadedFiles.map((file) => (
                              <TableRow key={file.id}>
                                <TableCell>{file.name}</TableCell>
                                <TableCell>
                                  {(file.size / 1024).toFixed(2)} KB
                                </TableCell>
                                <TableCell>{file.instrument}</TableCell>
                                <TableCell>
                                  <span className="status-badge warning">
                                    {file.status.replace(/_/g, " ")}
                                  </span>
                                </TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </div>
                    )}

                    {selectedInstrument && uploadedFiles.length > 0 && (
                      <div style={{ marginTop: "1.5rem" }}>
                        <Button
                          kind="primary"
                          onClick={handleValidateData}
                          disabled={isLoading}
                        >
                          {isLoading ? (
                            <>
                              <Loading description="Validating..." />
                            </>
                          ) : (
                            <FormattedMessage
                              id="notebook.bioanalytical.execution.validateData"
                              defaultMessage="Validate Uploaded Data"
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

          {/* Tab 2: Calibration Validation */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.calibrationSection"
                        defaultMessage="Calibration Curve Validation"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.calibrationHelp"
                        defaultMessage="Review calibration curve regression analysis. Acceptable criteria: r² ≥ 0.99 (99% of variance explained). Display linear regression equation, slope, intercept, and calibration range."
                      />
                    </p>

                    {calibrationData ? (
                      <div style={{ marginTop: "1.5rem" }}>
                        <div
                          style={{
                            padding: "1.5rem",
                            backgroundColor: "#f4f4f4",
                            borderRadius: "4px",
                          }}
                        >
                          <div style={{ marginBottom: "1rem" }}>
                            <strong>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.equation"
                                defaultMessage="Regression Equation:"
                              />
                            </strong>
                            <p
                              style={{
                                fontSize: "1rem",
                                marginTop: "0.25rem",
                                color: "#161616",
                              }}
                            >
                              {calibrationData.equation}
                            </p>
                          </div>

                          <table
                            style={{
                              width: "100%",
                              borderCollapse: "collapse",
                            }}
                          >
                            <tbody>
                              <tr>
                                <td
                                  style={{
                                    padding: "0.5rem",
                                    borderBottom: "1px solid #e0e0e0",
                                  }}
                                >
                                  <strong>
                                    <FormattedMessage
                                      id="notebook.bioanalytical.execution.rSquared"
                                      defaultMessage="R² Value:"
                                    />
                                  </strong>
                                </td>
                                <td
                                  style={{
                                    padding: "0.5rem",
                                    borderBottom: "1px solid #e0e0e0",
                                  }}
                                >
                                  {calibrationData.rSquared.toFixed(4)}
                                  <span
                                    style={{
                                      marginLeft: "0.5rem",
                                      padding: "0.25rem 0.5rem",
                                      backgroundColor:
                                        calibrationData.rSquared >= 0.99
                                          ? "#24a148"
                                          : "#da1e28",
                                      color: "white",
                                      borderRadius: "4px",
                                      fontSize: "0.75rem",
                                    }}
                                  >
                                    {calibrationData.rSquared >= 0.99
                                      ? "PASS"
                                      : "FAIL"}
                                  </span>
                                </td>
                              </tr>
                              <tr>
                                <td
                                  style={{
                                    padding: "0.5rem",
                                    borderBottom: "1px solid #e0e0e0",
                                  }}
                                >
                                  <strong>
                                    <FormattedMessage
                                      id="notebook.bioanalytical.execution.slope"
                                      defaultMessage="Slope:"
                                    />
                                  </strong>
                                </td>
                                <td
                                  style={{
                                    padding: "0.5rem",
                                    borderBottom: "1px solid #e0e0e0",
                                  }}
                                >
                                  {calibrationData.slope}
                                </td>
                              </tr>
                              <tr>
                                <td
                                  style={{
                                    padding: "0.5rem",
                                    borderBottom: "1px solid #e0e0e0",
                                  }}
                                >
                                  <strong>
                                    <FormattedMessage
                                      id="notebook.bioanalytical.execution.intercept"
                                      defaultMessage="Intercept:"
                                    />
                                  </strong>
                                </td>
                                <td
                                  style={{
                                    padding: "0.5rem",
                                    borderBottom: "1px solid #e0e0e0",
                                  }}
                                >
                                  {calibrationData.intercept}
                                </td>
                              </tr>
                              <tr>
                                <td
                                  style={{
                                    padding: "0.5rem",
                                    borderBottom: "1px solid #e0e0e0",
                                  }}
                                >
                                  <strong>
                                    <FormattedMessage
                                      id="notebook.bioanalytical.execution.range"
                                      defaultMessage="Calibration Range:"
                                    />
                                  </strong>
                                </td>
                                <td
                                  style={{
                                    padding: "0.5rem",
                                    borderBottom: "1px solid #e0e0e0",
                                  }}
                                >
                                  {calibrationData.range}
                                </td>
                              </tr>
                              <tr>
                                <td
                                  style={{
                                    padding: "0.5rem",
                                    borderBottom: "1px solid #e0e0e0",
                                  }}
                                >
                                  <strong>
                                    <FormattedMessage
                                      id="notebook.bioanalytical.execution.pointsUsed"
                                      defaultMessage="Points Used:"
                                    />
                                  </strong>
                                </td>
                                <td
                                  style={{
                                    padding: "0.5rem",
                                    borderBottom: "1px solid #e0e0e0",
                                  }}
                                >
                                  {calibrationData.pointsUsed}
                                </td>
                              </tr>
                            </tbody>
                          </table>
                        </div>

                        <div
                          style={{
                            marginTop: "1rem",
                            padding: "1rem",
                            backgroundColor: "#e7f1f5",
                            borderRadius: "4px",
                            borderLeft: "4px solid #0043ce",
                          }}
                        >
                          <p style={{ fontSize: "0.875rem", margin: 0 }}>
                            <strong>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.calibrationNote"
                                defaultMessage="Calibration Quality Assessment:"
                              />
                            </strong>
                          </p>
                          <p
                            style={{
                              fontSize: "0.875rem",
                              color: "#24a148",
                              margin: "0.25rem 0 0 0",
                            }}
                          >
                            {calibrationData.rSquared >= 0.99
                              ? "✓ Excellent correlation with r² > 0.99"
                              : "✗ Calibration does not meet acceptance criteria"}
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
                            id="notebook.bioanalytical.execution.noCalibration"
                            defaultMessage="Upload and validate data to see calibration results"
                          />
                        </p>
                      </div>
                    )}
                  </div>
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* Tab 3: QC Results & Trending */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.qcSection"
                        defaultMessage="QC Results & Westgard Rule Monitoring"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.qcHelp"
                        defaultMessage="Monitor QC results across Low/Medium/High levels. Acceptance criteria: accuracy 90-110%, precision RSD ≤ 15%. Westgard multi-rule detection identifies out-of-control conditions: 1-3S, 2-2S, R-4S, 4-1S, 10X."
                      />
                    </p>

                    {qcResults.length > 0 && (
                      <div style={{ marginTop: "1.5rem" }}>
                        <h5 style={{ marginBottom: "1rem" }}>
                          <FormattedMessage
                            id="notebook.bioanalytical.execution.qcByLevel"
                            defaultMessage="QC Results by Level"
                          />
                        </h5>
                        <Table>
                          <TableHead>
                            <TableRow>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.qcLevel"
                                  defaultMessage="QC Level"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.spiked"
                                  defaultMessage="Spiked Conc."
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.measured"
                                  defaultMessage="Measured Value"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.accuracy"
                                  defaultMessage="Accuracy"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.precision"
                                  defaultMessage="Precision (RSD)"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.status"
                                  defaultMessage="Status"
                                />
                              </TableHeader>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {qcResults.map((qc) => (
                              <TableRow key={qc.id}>
                                <TableCell>{qc.level}</TableCell>
                                <TableCell>{qc.spikedConcentration}</TableCell>
                                <TableCell>{qc.measuredValue}</TableCell>
                                <TableCell>{qc.accuracy}</TableCell>
                                <TableCell>{qc.precision}</TableCell>
                                <TableCell>
                                  <span
                                    className="status-badge"
                                    style={{
                                      backgroundColor:
                                        qc.status === "PASS"
                                          ? "#24a148"
                                          : "#da1e28",
                                      color: "white",
                                      padding: "0.25rem 0.5rem",
                                      borderRadius: "4px",
                                      fontSize: "0.75rem",
                                    }}
                                  >
                                    {qc.status}
                                  </span>
                                </TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </div>
                    )}

                    {westgardRules.length > 0 && (
                      <div style={{ marginTop: "2rem" }}>
                        <h5 style={{ marginBottom: "1rem" }}>
                          <FormattedMessage
                            id="notebook.bioanalytical.execution.westgard"
                            defaultMessage="Westgard Multi-Rule Detection"
                          />
                        </h5>
                        <Table>
                          <TableHead>
                            <TableRow>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.rule"
                                  defaultMessage="Rule"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.status"
                                  defaultMessage="Status"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.description"
                                  defaultMessage="Description"
                                />
                              </TableHeader>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {westgardRules.map((wr) => (
                              <TableRow key={wr.rule}>
                                <TableCell>
                                  <strong>{wr.rule}</strong>
                                </TableCell>
                                <TableCell>
                                  <span
                                    className="status-badge"
                                    style={{
                                      backgroundColor:
                                        wr.status === "PASS"
                                          ? "#24a148"
                                          : "#da1e28",
                                      color: "white",
                                      padding: "0.25rem 0.5rem",
                                      borderRadius: "4px",
                                      fontSize: "0.75rem",
                                    }}
                                  >
                                    {wr.status}
                                  </span>
                                </TableCell>
                                <TableCell style={{ fontSize: "0.875rem" }}>
                                  {wr.description}
                                </TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </div>
                    )}

                    {qcResults.length === 0 && (
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
                            id="notebook.bioanalytical.execution.noQCData"
                            defaultMessage="Upload and validate data to see QC results and Westgard rule monitoring"
                          />
                        </p>
                      </div>
                    )}
                  </div>
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* Tab 4: Analyzer Results */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.resultsSection"
                        defaultMessage="Analyzer Results & Sample Data"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.resultsHelp"
                        defaultMessage="Review analyzed sample results with replicate values, acceptance quality assessment, and analyte recovery rates. All results must pass QA review before data release."
                      />
                    </p>

                    {analyzerResults.length > 0 && (
                      <div style={{ marginTop: "1.5rem" }}>
                        <h5 style={{ marginBottom: "1rem" }}>
                          <FormattedMessage
                            id="notebook.bioanalytical.execution.analyzerResults"
                            defaultMessage="Sample Analysis Results ({count})"
                            values={{ count: analyzerResults.length }}
                          />
                        </h5>
                        <Table>
                          <TableHead>
                            <TableRow>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.sampleId"
                                  defaultMessage="Sample ID"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.replicate"
                                  defaultMessage="Replicate"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.result"
                                  defaultMessage="Result"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.recovery"
                                  defaultMessage="Recovery Rate"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.quality"
                                  defaultMessage="Quality"
                                />
                              </TableHeader>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {analyzerResults.map((result) => (
                              <TableRow key={result.id}>
                                <TableCell>{result.sampleId}</TableCell>
                                <TableCell>{result.replicate}</TableCell>
                                <TableCell>{result.result}</TableCell>
                                <TableCell>{result.recoveryRate}</TableCell>
                                <TableCell>
                                  <span
                                    className="status-badge"
                                    style={{
                                      backgroundColor:
                                        result.quality === "PASSED"
                                          ? "#24a148"
                                          : "#da1e28",
                                      color: "white",
                                      padding: "0.25rem 0.5rem",
                                      borderRadius: "4px",
                                      fontSize: "0.75rem",
                                    }}
                                  >
                                    {result.quality}
                                  </span>
                                </TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>

                        <div style={{ marginTop: "1.5rem" }}>
                          <Button kind="primary" onClick={handleApproveResults}>
                            <FormattedMessage
                              id="notebook.bioanalytical.execution.approveResults"
                              defaultMessage="Approve Results for QA Review"
                            />
                          </Button>
                        </div>
                      </div>
                    )}

                    {analyzerResults.length === 0 && (
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
                            id="notebook.bioanalytical.execution.noResults"
                            defaultMessage="Upload and validate data to see analyzer results"
                          />
                        </p>
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

export default BioanalyticalAnalyticalExecutionPage;
