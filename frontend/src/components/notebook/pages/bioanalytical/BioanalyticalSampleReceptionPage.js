import React, { useState, useCallback, useContext } from "react";
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
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../../../layout/Layout";
import BioanalyticalManifestImportModal from "../../modals/BioanalyticalManifestImportModal";
import "./BioanalyticalPages.css";

/**
 * BioanalyticalSampleReceptionPage - Stage 1 of bioanalytical workflow.
 *
 * Features:
 * - CSV manifest import with batch sample creation
 * - Reception metadata capture
 * - Barcode/QR code generation
 * - Sample tracking and status management
 *
 * @param {Object} props
 * @param {number} props.entryId - Notebook entry ID
 * @param {Object} props.pageData - Page configuration
 * @param {Object} props.progress - Sample progress counts
 * @param {function} props.onProgressUpdate - Callback after sample changes
 */
function BioanalyticalSampleReceptionPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const { setNotificationVisible } = useContext(NotificationContext);

  const [isLoading, setIsLoading] = useState(false);
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [samples, setSamples] = useState([]);
  const [selectedTab, setSelectedTab] = useState(0);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const handleImportModalOpen = () => {
    setIsImportModalOpen(true);
  };

  const handleImportModalClose = () => {
    setIsImportModalOpen(false);
  };

  const handleImportSuccess = useCallback(
    (results) => {
      setSuccessMessage(
        intl.formatMessage(
          {
            id: "notebook.bioanalytical.reception.importSuccess",
            defaultMessage: "{count} samples imported successfully",
          },
          { count: results.totalCreated || 0 },
        ),
      );

      // Refresh sample list
      if (onProgressUpdate) {
        onProgressUpdate();
      }

      // Close modal after success
      setTimeout(() => {
        setIsImportModalOpen(false);
      }, 1000);
    },
    [intl, onProgressUpdate],
  );

  return (
    <div className="bioanalytical-page">
      <div className="page-instructions">
        <h3>
          <FormattedMessage
            id="notebook.bioanalytical.reception.title"
            defaultMessage="Sample Reception & Metadata Capture"
          />
        </h3>
        <p>
          <FormattedMessage
            id="notebook.bioanalytical.reception.description"
            defaultMessage="Record initial sample reception with metadata including source origin, storage conditions, and analytical tests requested. Import multiple samples via CSV manifest file."
          />
        </p>
      </div>

      {errorMessage && (
        <div style={{ marginBottom: "1rem" }}>
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "notebook.bioanalytical.reception.error",
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
              id: "notebook.bioanalytical.reception.success",
              defaultMessage: "Success",
            })}
            subtitle={successMessage}
            lowContrast
            onCloseButtonClick={() => setSuccessMessage("")}
          />
        </div>
      )}

      <Tabs selectedIndex={selectedTab} onChange={setSelectedTab}>
        <TabList aria-label="Sample reception tabs">
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.reception.tab.import"
              defaultMessage="Manifest Import"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.reception.tab.samples"
              defaultMessage="Received Samples"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.reception.tab.metadata"
              defaultMessage="Reception Metadata"
            />
          </Tab>
        </TabList>

        <TabPanels>
          {/* Tab 1: Manifest Import */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.reception.importSection"
                        defaultMessage="CSV Manifest Import"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.reception.importHelp"
                        defaultMessage="Import multiple samples at once using a CSV manifest file. Required columns: Sample ID, Sample Type, Source Origin, Requested Tests, Date/Time of Receipt, Receiving Personnel."
                      />
                    </p>

                    <div style={{ marginTop: "1.5rem" }}>
                      <Button kind="primary" onClick={handleImportModalOpen}>
                        <FormattedMessage
                          id="notebook.bioanalytical.reception.importButton"
                          defaultMessage="Import Manifest"
                        />
                      </Button>
                    </div>
                  </div>

                  <BioanalyticalManifestImportModal
                    isOpen={isImportModalOpen}
                    onClose={handleImportModalClose}
                    entryId={entryId}
                    onSuccess={handleImportSuccess}
                  />
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* Tab 2: Received Samples */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.reception.samplesReceived"
                        defaultMessage="Samples Received"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.reception.samplesCount"
                        defaultMessage="Total samples received: {count}"
                        values={{ count: progress?.total || 0 }}
                      />
                    </p>
                  </div>

                  {isLoading ? (
                    <Loading description="Loading samples..." />
                  ) : (
                    <div>
                      <Table>
                        <TableHead>
                          <TableRow>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.reception.column.sampleId"
                                defaultMessage="Sample ID"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.reception.column.type"
                                defaultMessage="Sample Type"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.reception.column.origin"
                                defaultMessage="Source Origin"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.reception.column.tests"
                                defaultMessage="Requested Tests"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.reception.column.status"
                                defaultMessage="Status"
                              />
                            </TableHeader>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {samples.length > 0 ? (
                            samples.map((sample, idx) => (
                              <TableRow key={idx}>
                                <TableCell>
                                  {sample.accessionNumber ||
                                    sample.externalId ||
                                    "-"}
                                </TableCell>
                                <TableCell>
                                  {sample.sampleType?.name ||
                                    sample.sampleType ||
                                    "-"}
                                </TableCell>
                                <TableCell>
                                  {sample.sourceOrigin || "-"}
                                </TableCell>
                                <TableCell>
                                  {sample.requestedTests?.join(", ") ||
                                    sample.requestedTests ||
                                    "-"}
                                </TableCell>
                                <TableCell>
                                  <span
                                    className="status-badge"
                                    style={{
                                      backgroundColor:
                                        sample.status === "RECEIVED"
                                          ? "#24a148"
                                          : sample.status === "IN_PROGRESS"
                                            ? "#f1c21b"
                                            : "#8d8d8d",
                                      color: "white",
                                      padding: "0.25rem 0.5rem",
                                      borderRadius: "4px",
                                      fontSize: "0.75rem",
                                    }}
                                  >
                                    {sample.status || "UNKNOWN"}
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
                                  id="notebook.bioanalytical.reception.noSamples"
                                  defaultMessage="No samples received yet. Import samples using the manifest import tool."
                                />
                              </TableCell>
                            </TableRow>
                          )}
                        </TableBody>
                      </Table>
                    </div>
                  )}
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* Tab 3: Reception Metadata */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.reception.metadata"
                        defaultMessage="Reception Metadata"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.reception.metadataDescription"
                        defaultMessage="Review and manage sample reception information including source origin, storage conditions, transport temperature, and verification status."
                      />
                    </p>

                    <div style={{ marginTop: "1.5rem", fontSize: "0.875rem" }}>
                      <table
                        style={{ width: "100%", borderCollapse: "collapse" }}
                      >
                        <tbody>
                          <tr>
                            <td
                              style={{
                                padding: "0.5rem",
                                borderBottom: "1px solid #e0e0e0",
                                fontWeight: "bold",
                              }}
                            >
                              <FormattedMessage
                                id="notebook.bioanalytical.reception.totalReceived"
                                defaultMessage="Total Samples Received:"
                              />
                            </td>
                            <td
                              style={{
                                padding: "0.5rem",
                                borderBottom: "1px solid #e0e0e0",
                              }}
                            >
                              {progress?.total || 0}
                            </td>
                          </tr>
                          <tr>
                            <td
                              style={{
                                padding: "0.5rem",
                                borderBottom: "1px solid #e0e0e0",
                                fontWeight: "bold",
                              }}
                            >
                              <FormattedMessage
                                id="notebook.bioanalytical.reception.percentComplete"
                                defaultMessage="Completion:"
                              />
                            </td>
                            <td
                              style={{
                                padding: "0.5rem",
                                borderBottom: "1px solid #e0e0e0",
                              }}
                            >
                              {progress?.percentage || 0}%
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
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

export default BioanalyticalSampleReceptionPage;
