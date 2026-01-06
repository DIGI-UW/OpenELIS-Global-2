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
 * BioanalyticalStorageArchivingPage - Stage 5 of bioanalytical workflow.
 *
 * Features:
 * - Sample storage location and condition tracking
 * - Retention period management per regulatory requirements
 * - Long-term archival and retrieval planning
 * - Disposal scheduling and compliance documentation
 * - Final sample/data disposition tracking
 *
 * @param {Object} props
 * @param {number} props.entryId - Notebook entry ID
 * @param {Object} props.pageData - Page configuration
 * @param {Object} props.progress - Sample progress counts
 * @param {function} props.onProgressUpdate - Callback after changes
 */
function BioanalyticalStorageArchivingPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();

  const [isLoading, setIsLoading] = useState(false);
  const [selectedTab, setSelectedTab] = useState(0);
  const [storageSamples, setStorageSamples] = useState([]);
  const [storageCondition, setStorageCondition] = useState("");
  const [retentionPeriod, setRetentionPeriod] = useState("");
  const [disposalMethod, setDisposalMethod] = useState("");
  const [disposalSchedule, setDisposalSchedule] = useState("");
  const [archivalNotes, setArchivalNotes] = useState("");
  const [storageApproved, setStorageApproved] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const storageConditions = [
    { id: "2_8", label: "2-8°C (Refrigerated)" },
    { id: "-20", label: "-20°C (Frozen)" },
    { id: "-70", label: "-70°C (Deep Freeze)" },
    { id: "rt", label: "Room Temperature (15-25°C)" },
    { id: "dry", label: "Dry Storage (Desiccated)" },
  ];

  const retentionPeriods = [
    { id: "6m", label: "6 Months" },
    { id: "1y", label: "1 Year" },
    { id: "2y", label: "2 Years" },
    { id: "5y", label: "5 Years" },
    { id: "10y", label: "10 Years (FDA Requirement)" },
    { id: "indefinite", label: "Indefinite (Reference Standards)" },
  ];

  const disposalMethods = [
    { id: "incineration", label: "Incineration (Hazardous Waste)" },
    { id: "chemical", label: "Chemical Degradation" },
    { id: "biohazard", label: "Biohazard Disposal" },
    { id: "landfill", label: "Sanitary Landfill (Non-hazardous)" },
    { id: "return", label: "Return to Sponsor" },
    { id: "research", label: "Transfer to Research" },
  ];

  const loadStorageSamples = useCallback(() => {
    setIsLoading(true);

    setTimeout(() => {
      const mockSamples = [
        {
          id: "1",
          sampleId: "S001",
          type: "Plasma",
          volume: "5.0 mL",
          location: "Freezer A-12",
          storageTemp: "-20°C",
          status: "STORED",
          dateStored: "2026-01-03",
        },
        {
          id: "2",
          sampleId: "S002",
          type: "Plasma",
          volume: "5.0 mL",
          location: "Freezer A-13",
          storageTemp: "-20°C",
          status: "STORED",
          dateStored: "2026-01-03",
        },
        {
          id: "3",
          sampleId: "S003",
          type: "Urine",
          volume: "10.0 mL",
          location: "Freezer B-05",
          storageTemp: "-20°C",
          status: "STORED",
          dateStored: "2026-01-03",
        },
        {
          id: "4",
          sampleId: "S004",
          type: "Urine",
          volume: "10.0 mL",
          location: "Freezer B-06",
          storageTemp: "-20°C",
          status: "STORED",
          dateStored: "2026-01-03",
        },
        {
          id: "5",
          sampleId: "S005",
          type: "Plasma",
          volume: "5.0 mL",
          location: "Freezer A-14",
          storageTemp: "-20°C",
          status: "STORED",
          dateStored: "2026-01-03",
        },
        {
          id: "6",
          sampleId: "S006",
          type: "Plasma",
          volume: "5.0 mL",
          location: "Freezer A-15",
          storageTemp: "-20°C",
          status: "STORED",
          dateStored: "2026-01-03",
        },
      ];
      setStorageSamples(mockSamples);
      setIsLoading(false);
    }, 1500);
  }, []);

  React.useEffect(() => {
    loadStorageSamples();
  }, []);

  const handleApproveStorage = useCallback(() => {
    if (!storageCondition) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.storage.selectCondition",
          defaultMessage: "Please select storage condition",
        }),
      );
      return;
    }

    if (!retentionPeriod) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.storage.selectRetention",
          defaultMessage: "Please select retention period",
        }),
      );
      return;
    }

    if (!disposalMethod) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.storage.selectDisposal",
          defaultMessage: "Please select disposal method",
        }),
      );
      return;
    }

    if (!storageApproved) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.storage.confirmApproval",
          defaultMessage: "Please confirm storage and archival approval",
        }),
      );
      return;
    }

    setSuccessMessage(
      intl.formatMessage(
        {
          id: "notebook.bioanalytical.storage.approvalComplete",
          defaultMessage:
            "Storage and archival plan approved. {count} samples documented for long-term retention.",
        },
        { count: storageSamples.length },
      ),
    );

    if (onProgressUpdate) {
      onProgressUpdate();
    }
  }, [
    storageCondition,
    retentionPeriod,
    disposalMethod,
    storageApproved,
    storageSamples.length,
    intl,
    onProgressUpdate,
  ]);

  return (
    <div className="bioanalytical-page">
      <div className="page-instructions">
        <h3>
          <FormattedMessage
            id="notebook.bioanalytical.storage.title"
            defaultMessage="Sample Storage & Archival"
          />
        </h3>
        <p>
          <FormattedMessage
            id="notebook.bioanalytical.storage.description"
            defaultMessage="Document sample storage locations and conditions, establish retention periods per regulatory requirements (typically 10 years for FDA bioequivalence studies), plan long-term archival, and schedule final disposal or archival transfers."
          />
        </p>
      </div>

      {errorMessage && (
        <div style={{ marginBottom: "1rem" }}>
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "notebook.bioanalytical.storage.error",
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
              id: "notebook.bioanalytical.storage.success",
              defaultMessage: "Success",
            })}
            subtitle={successMessage}
            lowContrast
            onCloseButtonClick={() => setSuccessMessage("")}
          />
        </div>
      )}

      <Tabs selectedIndex={selectedTab} onChange={setSelectedTab}>
        <TabList aria-label="Storage and archival tabs">
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.storage.tab.inventory"
              defaultMessage="Sample Inventory"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.storage.tab.storageConfig"
              defaultMessage="Storage Configuration"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.storage.tab.retentionDisposal"
              defaultMessage="Retention & Disposal"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.storage.tab.archivalPlan"
              defaultMessage="Archival Plan"
            />
          </Tab>
        </TabList>

        <TabPanels>
          {/* Tab 1: Sample Inventory */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.storage.inventorySection"
                        defaultMessage="Stored Sample Inventory"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.storage.inventoryHelp"
                        defaultMessage="View complete inventory of all samples with storage location, temperature, and storage date. This inventory is maintained throughout the retention period and updated with any sample movements or consumptions."
                      />
                    </p>

                    {isLoading ? (
                      <Loading description="Loading sample inventory..." />
                    ) : storageSamples.length > 0 ? (
                      <div style={{ marginTop: "1.5rem" }}>
                        <div
                          style={{
                            marginBottom: "1rem",
                            padding: "0.75rem",
                            backgroundColor: "#f4f4f4",
                            borderRadius: "4px",
                          }}
                        >
                          <p style={{ fontSize: "0.875rem", margin: 0 }}>
                            <strong>
                              <FormattedMessage
                                id="notebook.bioanalytical.storage.totalSamples"
                                defaultMessage="Total Samples in Storage:"
                              />
                            </strong>{" "}
                            {storageSamples.length}
                          </p>
                        </div>
                        <Table>
                          <TableHead>
                            <TableRow>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.storage.sampleId"
                                  defaultMessage="Sample ID"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.storage.type"
                                  defaultMessage="Type"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.storage.volume"
                                  defaultMessage="Volume"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.storage.location"
                                  defaultMessage="Storage Location"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.storage.temperature"
                                  defaultMessage="Temperature"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.storage.dateStored"
                                  defaultMessage="Date Stored"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.storage.status"
                                  defaultMessage="Status"
                                />
                              </TableHeader>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {storageSamples.map((sample) => (
                              <TableRow key={sample.id}>
                                <TableCell>{sample.sampleId}</TableCell>
                                <TableCell>{sample.type}</TableCell>
                                <TableCell>{sample.volume}</TableCell>
                                <TableCell style={{ fontSize: "0.875rem" }}>
                                  {sample.location}
                                </TableCell>
                                <TableCell>{sample.storageTemp}</TableCell>
                                <TableCell>{sample.dateStored}</TableCell>
                                <TableCell>
                                  <span
                                    className="status-badge info"
                                    style={{
                                      backgroundColor: "#0043ce",
                                      color: "white",
                                      padding: "0.25rem 0.5rem",
                                      borderRadius: "4px",
                                      fontSize: "0.75rem",
                                    }}
                                  >
                                    {sample.status}
                                  </span>
                                </TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
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
                            id="notebook.bioanalytical.storage.noSamples"
                            defaultMessage="No samples in storage"
                          />
                        </p>
                      </div>
                    )}
                  </div>
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* Tab 2: Storage Configuration */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.storage.configSection"
                        defaultMessage="Storage Conditions"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.storage.configHelp"
                        defaultMessage="Define storage conditions for all samples. Temperature and humidity must be maintained within specified ranges. Standard practice for bioanalytical samples is -20°C or -70°C frozen storage."
                      />
                    </p>

                    <div style={{ marginTop: "1.5rem" }}>
                      <Select
                        id="storage-condition"
                        labelText={intl.formatMessage({
                          id: "notebook.bioanalytical.storage.selectStorageCondition",
                          defaultMessage: "Select Storage Condition",
                        })}
                        value={storageCondition}
                        onChange={(e) => setStorageCondition(e.target.value)}
                      >
                        <SelectItem
                          value=""
                          text="-- Choose storage condition --"
                        />
                        {storageConditions.map((condition) => (
                          <SelectItem
                            key={condition.id}
                            value={condition.id}
                            text={condition.label}
                          />
                        ))}
                      </Select>
                    </div>

                    {storageCondition && (
                      <div
                        style={{
                          marginTop: "1.5rem",
                          padding: "1rem",
                          backgroundColor: "#f4f4f4",
                          borderRadius: "4px",
                        }}
                      >
                        <p style={{ fontSize: "0.875rem", margin: 0 }}>
                          <strong>
                            <FormattedMessage
                              id="notebook.bioanalytical.storage.selectedCondition"
                              defaultMessage="Selected Condition:"
                            />
                          </strong>{" "}
                          {
                            storageConditions.find(
                              (c) => c.id === storageCondition,
                            )?.label
                          }
                        </p>
                        <p
                          style={{
                            fontSize: "0.875rem",
                            color: "#525252",
                            margin: "0.5rem 0 0 0",
                          }}
                        >
                          <FormattedMessage
                            id="notebook.bioanalytical.storage.stabilityInfo"
                            defaultMessage="Samples are stable under these conditions per ICH Q1A guidelines"
                          />
                        </p>
                      </div>
                    )}

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
                            id="notebook.bioanalytical.storage.storageRequirements"
                            defaultMessage="Storage Requirements:"
                          />
                        </strong>
                      </p>
                      <ul
                        style={{
                          fontSize: "0.875rem",
                          color: "#161616",
                          margin: "0.5rem 0 0 0",
                          paddingLeft: "1.5rem",
                        }}
                      >
                        <li>
                          <FormattedMessage
                            id="notebook.bioanalytical.storage.req1"
                            defaultMessage="Temperature monitored daily with alarm systems"
                          />
                        </li>
                        <li>
                          <FormattedMessage
                            id="notebook.bioanalytical.storage.req2"
                            defaultMessage="Humidity control (typically 30-70% RH)"
                          />
                        </li>
                        <li>
                          <FormattedMessage
                            id="notebook.bioanalytical.storage.req3"
                            defaultMessage="Protected from light and contamination"
                          />
                        </li>
                        <li>
                          <FormattedMessage
                            id="notebook.bioanalytical.storage.req4"
                            defaultMessage="Regular backup power and maintenance"
                          />
                        </li>
                      </ul>
                    </div>
                  </div>
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* Tab 3: Retention & Disposal */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.storage.retentionSection"
                        defaultMessage="Retention Period & Disposal Planning"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.storage.retentionHelp"
                        defaultMessage="Establish retention periods based on regulatory requirements. FDA requires 10-year retention for bioequivalence studies. Plan final disposal method after retention period expires."
                      />
                    </p>

                    <div style={{ marginTop: "1.5rem" }}>
                      <Select
                        id="retention-period"
                        labelText={intl.formatMessage({
                          id: "notebook.bioanalytical.storage.selectRetentionPeriod",
                          defaultMessage: "Select Retention Period",
                        })}
                        value={retentionPeriod}
                        onChange={(e) => setRetentionPeriod(e.target.value)}
                      >
                        <SelectItem
                          value=""
                          text="-- Choose retention period --"
                        />
                        {retentionPeriods.map((period) => (
                          <SelectItem
                            key={period.id}
                            value={period.id}
                            text={period.label}
                          />
                        ))}
                      </Select>
                    </div>

                    <div style={{ marginTop: "1.5rem" }}>
                      <Select
                        id="disposal-method"
                        labelText={intl.formatMessage({
                          id: "notebook.bioanalytical.storage.selectDisposalMethod",
                          defaultMessage: "Select Final Disposal Method",
                        })}
                        value={disposalMethod}
                        onChange={(e) => setDisposalMethod(e.target.value)}
                      >
                        <SelectItem
                          value=""
                          text="-- Choose disposal method --"
                        />
                        {disposalMethods.map((method) => (
                          <SelectItem
                            key={method.id}
                            value={method.id}
                            text={method.label}
                          />
                        ))}
                      </Select>
                    </div>

                    <div style={{ marginTop: "1.5rem" }}>
                      <label
                        htmlFor="disposal-schedule"
                        style={{
                          display: "block",
                          marginBottom: "0.5rem",
                          fontWeight: "bold",
                          fontSize: "0.875rem",
                        }}
                      >
                        <FormattedMessage
                          id="notebook.bioanalytical.storage.disposalSchedule"
                          defaultMessage="Disposal Schedule (Target Date)"
                        />
                      </label>
                      <input
                        id="disposal-schedule"
                        type="date"
                        value={disposalSchedule}
                        onChange={(e) => setDisposalSchedule(e.target.value)}
                        style={{
                          padding: "0.5rem",
                          borderRadius: "4px",
                          border: "1px solid #8d8d8d",
                          width: "200px",
                        }}
                      />
                    </div>

                    {retentionPeriod && disposalMethod && (
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
                              id="notebook.bioanalytical.storage.disposalSummary"
                              defaultMessage="Disposal Plan Summary:"
                            />
                          </strong>
                        </p>
                        <p
                          style={{
                            fontSize: "0.875rem",
                            color: "#161616",
                            margin: "0.5rem 0 0 0",
                          }}
                        >
                          <FormattedMessage
                            id="notebook.bioanalytical.storage.disposalPlan"
                            defaultMessage="Retain samples for {retention}. Dispose via {method}."
                            values={{
                              retention: retentionPeriods.find(
                                (p) => p.id === retentionPeriod,
                              )?.label,
                              method: disposalMethods.find(
                                (m) => m.id === disposalMethod,
                              )?.label,
                            }}
                          />
                        </p>
                      </div>
                    )}
                  </div>
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* Tab 4: Archival Plan */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.storage.archivalSection"
                        defaultMessage="Long-Term Archival & Approval"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.storage.archivalHelp"
                        defaultMessage="Document archival plan including storage facility, environmental monitoring, sample integrity checks, and compliance verification. Obtain approval to finalize sample handling and archival."
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
                          id="notebook.bioanalytical.storage.archivalChecklist"
                          defaultMessage="Archival Checklist"
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
                          <Checkbox
                            id="arch-check-1"
                            checked={true}
                            readOnly
                            labelText=""
                          />
                          <label
                            htmlFor="arch-check-1"
                            style={{
                              marginLeft: "0.5rem",
                              fontSize: "0.875rem",
                            }}
                          >
                            <FormattedMessage
                              id="notebook.bioanalytical.storage.archCheck1"
                              defaultMessage="Secure storage facility with controlled access"
                            />
                          </label>
                        </div>
                        <div style={{ display: "flex", alignItems: "center" }}>
                          <Checkbox
                            id="arch-check-2"
                            checked={true}
                            readOnly
                            labelText=""
                          />
                          <label
                            htmlFor="arch-check-2"
                            style={{
                              marginLeft: "0.5rem",
                              fontSize: "0.875rem",
                            }}
                          >
                            <FormattedMessage
                              id="notebook.bioanalytical.storage.archCheck2"
                              defaultMessage="Temperature/humidity monitoring with alerts"
                            />
                          </label>
                        </div>
                        <div style={{ display: "flex", alignItems: "center" }}>
                          <Checkbox
                            id="arch-check-3"
                            checked={true}
                            readOnly
                            labelText=""
                          />
                          <label
                            htmlFor="arch-check-3"
                            style={{
                              marginLeft: "0.5rem",
                              fontSize: "0.875rem",
                            }}
                          >
                            <FormattedMessage
                              id="notebook.bioanalytical.storage.archCheck3"
                              defaultMessage="Inventory tracking and barcode system"
                            />
                          </label>
                        </div>
                        <div style={{ display: "flex", alignItems: "center" }}>
                          <Checkbox
                            id="arch-check-4"
                            checked={true}
                            readOnly
                            labelText=""
                          />
                          <label
                            htmlFor="arch-check-4"
                            style={{
                              marginLeft: "0.5rem",
                              fontSize: "0.875rem",
                            }}
                          >
                            <FormattedMessage
                              id="notebook.bioanalytical.storage.archCheck4"
                              defaultMessage="Periodic sample integrity verification"
                            />
                          </label>
                        </div>
                        <div style={{ display: "flex", alignItems: "center" }}>
                          <Checkbox
                            id="arch-check-5"
                            checked={true}
                            readOnly
                            labelText=""
                          />
                          <label
                            htmlFor="arch-check-5"
                            style={{
                              marginLeft: "0.5rem",
                              fontSize: "0.875rem",
                            }}
                          >
                            <FormattedMessage
                              id="notebook.bioanalytical.storage.archCheck5"
                              defaultMessage="Regulatory compliance documentation"
                            />
                          </label>
                        </div>
                      </div>
                    </div>

                    <div style={{ marginTop: "1.5rem" }}>
                      <label
                        htmlFor="archival-notes"
                        style={{
                          display: "block",
                          marginBottom: "0.5rem",
                          fontWeight: "bold",
                          fontSize: "0.875rem",
                        }}
                      >
                        <FormattedMessage
                          id="notebook.bioanalytical.storage.archivalNotes"
                          defaultMessage="Archival Notes & Documentation"
                        />
                      </label>
                      <TextArea
                        id="archival-notes"
                        labelText=""
                        placeholder={intl.formatMessage({
                          id: "notebook.bioanalytical.storage.archivalNotesPlaceholder",
                          defaultMessage:
                            "Document archival facility details, monitoring procedures, sample integrity checks, and compliance verification...",
                        })}
                        value={archivalNotes}
                        onChange={(e) => setArchivalNotes(e.target.value)}
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
                        id="storage-approve"
                        checked={storageApproved}
                        onChange={(e) => setStorageApproved(e.target.checked)}
                      />
                      <label
                        htmlFor="storage-approve"
                        style={{ fontSize: "0.875rem", fontWeight: "bold" }}
                      >
                        <FormattedMessage
                          id="notebook.bioanalytical.storage.approvalConfirm"
                          defaultMessage="I confirm storage and archival plan is complete and compliant"
                        />
                      </label>
                    </div>

                    <div style={{ marginTop: "1.5rem" }}>
                      <Button kind="primary" onClick={handleApproveStorage}>
                        <FormattedMessage
                          id="notebook.bioanalytical.storage.completeStorage"
                          defaultMessage="Approve Storage & Archival Plan"
                        />
                      </Button>
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

export default BioanalyticalStorageArchivingPage;
