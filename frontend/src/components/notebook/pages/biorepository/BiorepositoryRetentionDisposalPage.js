import React, { useState, useEffect, useCallback, useRef } from "react";
import {
  Grid,
  Column,
  Tabs,
  Tab,
  TabList,
  TabPanels,
  TabPanel,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectAll,
  TableSelectRow,
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
  Button,
  Tag,
  Modal,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  InlineNotification,
  Loading,
  Checkbox,
} from "@carbon/react";
import { TrashCan, Renew, DocumentTasks, Search } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";

/**
 * BiorepositoryRetentionDisposalPage - Retention & Disposal workflow page
 * Stage 6 of the Biorepository workflow
 *
 * Features:
 * - Dashboard showing expired and expiring samples
 * - Manual sample selection for disposal
 * - Disposal form with verification step
 * - Disposal history tracking
 */
function BiorepositoryRetentionDisposalPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();
  const componentMounted = useRef(true);

  // State for samples
  const [expiredSamples, setExpiredSamples] = useState([]);
  const [expiringSamples30, setExpiringSamples30] = useState([]);
  const [expiringSamples60, setExpiringSamples60] = useState([]);
  const [expiringSamples90, setExpiringSamples90] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // State for selection and disposal
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [disposalModalOpen, setDisposalModalOpen] = useState(false);
  const [verificationStep, setVerificationStep] = useState(false);
  const [disposalForm, setDisposalForm] = useState({
    reason: "",
    method: "",
    notes: "",
  });
  const [disposing, setDisposing] = useState(false);
  const [notification, setNotification] = useState(null);

  // Active tab
  const [activeTab, setActiveTab] = useState(0);

  // State for manual disposal tab
  const [barcodeSearch, setBarcodeSearch] = useState("");
  const [searchedSample, setSearchedSample] = useState(null);
  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState(null);

  // Load samples on mount
  useEffect(() => {
    componentMounted.current = true;
    loadAllSamples();

    return () => {
      componentMounted.current = false;
    };
  }, []);

  const loadAllSamples = useCallback(() => {
    setLoading(true);
    setError(null);

    // Load expired samples
    getFromOpenElisServer(
      "/rest/biorepository/sample/expiring?status=expired",
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            setExpiredSamples(response);
          }
        }
      },
    );

    // Load samples expiring within 30 days
    getFromOpenElisServer(
      "/rest/biorepository/sample/expiring?status=expiring&days=30",
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            setExpiringSamples30(response);
          }
        }
      },
    );

    // Load samples expiring within 60 days
    getFromOpenElisServer(
      "/rest/biorepository/sample/expiring?status=expiring&days=60",
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            setExpiringSamples60(response);
          }
        }
      },
    );

    // Load samples expiring within 90 days
    getFromOpenElisServer(
      "/rest/biorepository/sample/expiring?status=expiring&days=90",
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            setExpiringSamples90(response);
          }
          setLoading(false);
        }
      },
    );
  }, []);

  // Search for a sample by barcode
  const handleBarcodeSearch = useCallback(() => {
    if (!barcodeSearch.trim()) {
      setSearchError(
        intl.formatMessage({
          id: "biorepository.disposal.search.emptyBarcode",
          defaultMessage: "Please enter a barcode to search.",
        }),
      );
      return;
    }

    setSearching(true);
    setSearchError(null);
    setSearchedSample(null);

    getFromOpenElisServer(
      `/rest/biorepository/sample/by-barcode/${encodeURIComponent(barcodeSearch.trim())}`,
      (response) => {
        if (componentMounted.current) {
          setSearching(false);
          if (response && response.id) {
            // Check if sample is already disposed
            if (
              response.workflowStatus === "DISPOSED" ||
              response.statusId === "24"
            ) {
              setSearchError(
                intl.formatMessage({
                  id: "biorepository.disposal.search.alreadyDisposed",
                  defaultMessage:
                    "This sample has already been disposed and cannot be disposed again.",
                }),
              );
              setSearchedSample(null);
            } else {
              setSearchedSample(response);
            }
          } else if (response && response.error) {
            setSearchError(response.error);
          } else {
            setSearchError(
              intl.formatMessage(
                {
                  id: "biorepository.disposal.search.notFound",
                  defaultMessage: 'No sample found with barcode "{barcode}".',
                },
                { barcode: barcodeSearch.trim() },
              ),
            );
          }
        }
      },
    );
  }, [barcodeSearch, intl]);

  // Handle manual disposal of searched sample
  const handleManualDisposal = () => {
    if (!searchedSample) return;

    setDisposalForm({ reason: "", method: "", notes: "" });
    setVerificationStep(false);
    setSelectedSampleIds([
      searchedSample.id?.toString() || searchedSample.sampleItemId?.toString(),
    ]);
    setDisposalModalOpen(true);
  };

  // Handle disposal completion for manual tab
  const handleManualDisposalComplete = () => {
    setSearchedSample(null);
    setBarcodeSearch("");
  };

  // Get current samples based on active tab
  const getCurrentSamples = () => {
    switch (activeTab) {
      case 0:
        return expiredSamples;
      case 1:
        return expiringSamples30;
      case 2:
        return expiringSamples60;
      case 3:
        return expiringSamples90;
      case 4:
        // Manual disposal tab - return searched sample if available
        return searchedSample ? [searchedSample] : [];
      default:
        return [];
    }
  };

  // Format date for display
  const formatDate = (dateStr) => {
    if (!dateStr) return "-";
    try {
      return new Date(dateStr).toLocaleDateString();
    } catch {
      return dateStr;
    }
  };

  // Calculate days until expiry
  const getDaysUntilExpiry = (expiryDate) => {
    if (!expiryDate) return null;
    const expiry = new Date(expiryDate);
    const today = new Date();
    const diffTime = expiry - today;
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays;
  };

  // Get urgency tag for expiry
  const getExpiryTag = (expiryDate) => {
    const days = getDaysUntilExpiry(expiryDate);
    if (days === null) return null;

    if (days < 0) {
      return (
        <Tag type="red" size="sm">
          {intl.formatMessage(
            {
              id: "biorepository.retention.expired",
              defaultMessage: "Expired {days} days ago",
            },
            { days: Math.abs(days) },
          )}
        </Tag>
      );
    } else if (days <= 30) {
      return (
        <Tag type="red" size="sm">
          {intl.formatMessage(
            {
              id: "biorepository.retention.expiresIn",
              defaultMessage: "Expires in {days} days",
            },
            { days },
          )}
        </Tag>
      );
    } else if (days <= 60) {
      return (
        <Tag type="orange" size="sm">
          {intl.formatMessage(
            {
              id: "biorepository.retention.expiresIn",
              defaultMessage: "Expires in {days} days",
            },
            { days },
          )}
        </Tag>
      );
    } else {
      return (
        <Tag type="gray" size="sm">
          {intl.formatMessage(
            {
              id: "biorepository.retention.expiresIn",
              defaultMessage: "Expires in {days} days",
            },
            { days },
          )}
        </Tag>
      );
    }
  };

  // Handle opening disposal modal
  const handleOpenDisposalModal = () => {
    if (selectedSampleIds.length === 0) {
      setNotification({
        kind: "warning",
        title: intl.formatMessage({
          id: "biorepository.disposal.noSelection",
          defaultMessage: "No Samples Selected",
        }),
        subtitle: intl.formatMessage({
          id: "biorepository.disposal.noSelection.message",
          defaultMessage: "Please select at least one sample to dispose.",
        }),
      });
      return;
    }
    setDisposalForm({ reason: "", method: "", notes: "" });
    setVerificationStep(false);
    setDisposalModalOpen(true);
  };

  // Handle disposal form submission
  const handleDisposalSubmit = () => {
    if (!verificationStep) {
      // Move to verification step
      setVerificationStep(true);
      return;
    }

    // Perform disposal
    setDisposing(true);

    // Dispose each selected sample
    const disposePromises = selectedSampleIds.map((sampleId) => {
      return new Promise((resolve) => {
        const currentSamples = getCurrentSamples();
        const sample = currentSamples.find(
          (s) =>
            s.id?.toString() === sampleId ||
            s.sampleItemId?.toString() === sampleId,
        );
        const sampleItemId = sample?.sampleItemId || sampleId;

        postToOpenElisServer(
          "/rest/biorepository/sample/dispose",
          JSON.stringify({
            sampleItemId: sampleItemId.toString(),
            reason: disposalForm.reason,
            method: disposalForm.method,
            notes: disposalForm.notes,
          }),
          (response) => {
            resolve(response);
          },
        );
      });
    });

    Promise.all(disposePromises).then((results) => {
      if (componentMounted.current) {
        setDisposing(false);
        setDisposalModalOpen(false);
        setSelectedSampleIds([]);
        setNotification({
          kind: "success",
          title: intl.formatMessage({
            id: "biorepository.disposal.success",
            defaultMessage: "Disposal Complete",
          }),
          subtitle: intl.formatMessage(
            {
              id: "biorepository.disposal.success.message",
              defaultMessage: "{count} sample(s) disposed successfully.",
            },
            { count: results.length },
          ),
        });
        // Clear manual disposal state if on manual tab
        if (activeTab === 4) {
          handleManualDisposalComplete();
        }
        // Reload samples
        loadAllSamples();
      }
    });
  };

  // Table headers
  const headers = [
    {
      key: "barcode",
      header: intl.formatMessage({
        id: "biorepository.column.barcode",
        defaultMessage: "Barcode",
      }),
    },
    {
      key: "sampleType",
      header: intl.formatMessage({
        id: "biorepository.column.sampleType",
        defaultMessage: "Sample Type",
      }),
    },
    {
      key: "projectName",
      header: intl.formatMessage({
        id: "biorepository.column.project",
        defaultMessage: "Project",
      }),
    },
    {
      key: "retentionPolicyName",
      header: intl.formatMessage({
        id: "biorepository.column.retentionPolicy",
        defaultMessage: "Retention Policy",
      }),
    },
    {
      key: "retentionExpiryDate",
      header: intl.formatMessage({
        id: "biorepository.column.expiryDate",
        defaultMessage: "Expiry Date",
      }),
    },
    {
      key: "originLab",
      header: intl.formatMessage({
        id: "biorepository.column.originLab",
        defaultMessage: "Origin",
      }),
    },
  ];

  // Transform samples to rows
  const getRows = (samples) => {
    return samples.map((sample) => ({
      id: sample.id?.toString() || sample.sampleItemId?.toString(),
      barcode: sample.barcode || sample.externalId || "-",
      sampleType: sample.sampleType?.description || "-",
      projectName: sample.projectId || "-",
      retentionPolicyName: sample.retentionPolicyName || "-",
      retentionExpiryDate: sample.retentionExpiryDate,
      status: sample.workflowStatus || "REGISTERED",
      originLab: sample.originLab,
      _original: sample,
    }));
  };

  const currentSamples = getCurrentSamples();
  const rows = getRows(currentSamples);

  return (
    <div className="biorepository-retention-disposal-page">
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          {/* Header */}
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: "1rem",
            }}
          >
            <div>
              <h4 style={{ marginBottom: "0.5rem" }}>
                <FormattedMessage
                  id="biorepository.retention.title"
                  defaultMessage="Retention & Disposal"
                />
              </h4>
              <p style={{ color: "#525252", fontSize: "0.875rem" }}>
                <FormattedMessage
                  id="biorepository.retention.description"
                  defaultMessage="Review samples approaching or past retention expiry. Select samples and proceed with disposal workflow."
                />
              </p>
            </div>
            <Button
              kind="ghost"
              renderIcon={Renew}
              onClick={loadAllSamples}
              disabled={loading}
            >
              <FormattedMessage
                id="biorepository.refresh"
                defaultMessage="Refresh"
              />
            </Button>
          </div>

          {/* Notification */}
          {notification && (
            <InlineNotification
              kind={notification.kind}
              title={notification.title}
              subtitle={notification.subtitle}
              onCloseButtonClick={() => setNotification(null)}
              style={{ marginBottom: "1rem" }}
            />
          )}

          {/* Summary Cards */}
          <div
            style={{
              display: "flex",
              gap: "1rem",
              marginBottom: "1.5rem",
              flexWrap: "wrap",
            }}
          >
            <div
              style={{
                background: "#fff1f1",
                padding: "1rem",
                borderRadius: "4px",
                minWidth: "150px",
              }}
            >
              <div
                style={{
                  fontSize: "2rem",
                  fontWeight: "600",
                  color: "#da1e28",
                }}
              >
                {expiredSamples.length}
              </div>
              <div style={{ fontSize: "0.875rem", color: "#525252" }}>
                <FormattedMessage
                  id="biorepository.retention.expired.label"
                  defaultMessage="Expired"
                />
              </div>
            </div>
            <div
              style={{
                background: "#fff8e1",
                padding: "1rem",
                borderRadius: "4px",
                minWidth: "150px",
              }}
            >
              <div
                style={{
                  fontSize: "2rem",
                  fontWeight: "600",
                  color: "#f1c21b",
                }}
              >
                {expiringSamples30.length}
              </div>
              <div style={{ fontSize: "0.875rem", color: "#525252" }}>
                <FormattedMessage
                  id="biorepository.retention.expiring30"
                  defaultMessage="Expiring in 30 days"
                />
              </div>
            </div>
            <div
              style={{
                background: "#e0f7fa",
                padding: "1rem",
                borderRadius: "4px",
                minWidth: "150px",
              }}
            >
              <div
                style={{
                  fontSize: "2rem",
                  fontWeight: "600",
                  color: "#0072c3",
                }}
              >
                {expiringSamples60.length}
              </div>
              <div style={{ fontSize: "0.875rem", color: "#525252" }}>
                <FormattedMessage
                  id="biorepository.retention.expiring60"
                  defaultMessage="Expiring in 60 days"
                />
              </div>
            </div>
            <div
              style={{
                background: "#f4f4f4",
                padding: "1rem",
                borderRadius: "4px",
                minWidth: "150px",
              }}
            >
              <div
                style={{
                  fontSize: "2rem",
                  fontWeight: "600",
                  color: "#525252",
                }}
              >
                {expiringSamples90.length}
              </div>
              <div style={{ fontSize: "0.875rem", color: "#525252" }}>
                <FormattedMessage
                  id="biorepository.retention.expiring90"
                  defaultMessage="Expiring in 90 days"
                />
              </div>
            </div>
          </div>

          {/* Tabs and Table */}
          <Tabs
            selectedIndex={activeTab}
            onChange={({ selectedIndex }) => setActiveTab(selectedIndex)}
          >
            <TabList aria-label="Retention tabs">
              <Tab>
                <FormattedMessage
                  id="biorepository.retention.tab.expired"
                  defaultMessage="Expired"
                />
                {expiredSamples.length > 0 && (
                  <Tag type="red" size="sm" style={{ marginLeft: "0.5rem" }}>
                    {expiredSamples.length}
                  </Tag>
                )}
              </Tab>
              <Tab>
                <FormattedMessage
                  id="biorepository.retention.tab.30days"
                  defaultMessage="30 Days"
                />
                {expiringSamples30.length > 0 && (
                  <Tag type="orange" size="sm" style={{ marginLeft: "0.5rem" }}>
                    {expiringSamples30.length}
                  </Tag>
                )}
              </Tab>
              <Tab>
                <FormattedMessage
                  id="biorepository.retention.tab.60days"
                  defaultMessage="60 Days"
                />
              </Tab>
              <Tab>
                <FormattedMessage
                  id="biorepository.retention.tab.90days"
                  defaultMessage="90 Days"
                />
              </Tab>
              <Tab>
                <Search size={16} style={{ marginRight: "0.25rem" }} />
                <FormattedMessage
                  id="biorepository.retention.tab.manual"
                  defaultMessage="Manual Disposal"
                />
              </Tab>
            </TabList>
            <TabPanels>
              {[0, 1, 2, 3].map((tabIndex) => (
                <TabPanel key={tabIndex}>
                  {activeTab !== tabIndex ? null : loading ? (
                    <Loading
                      withOverlay={false}
                      description="Loading samples..."
                    />
                  ) : rows.length === 0 ? (
                    <InlineNotification
                      kind="info"
                      title={intl.formatMessage({
                        id: "biorepository.retention.noSamples",
                        defaultMessage: "No Samples",
                      })}
                      subtitle={intl.formatMessage({
                        id: "biorepository.retention.noSamples.message",
                        defaultMessage: "No samples found in this category.",
                      })}
                      lowContrast
                      hideCloseButton
                    />
                  ) : (
                    <DataTable rows={rows} headers={headers} isSortable>
                      {({
                        rows,
                        headers,
                        getTableProps,
                        getHeaderProps,
                        getRowProps,
                        getSelectionProps,
                        selectedRows,
                      }) => (
                        <TableContainer>
                          <TableToolbar>
                            <TableToolbarContent>
                              <TableToolbarSearch onChange={() => {}} />
                              <Button
                                kind="danger"
                                renderIcon={TrashCan}
                                onClick={handleOpenDisposalModal}
                                disabled={selectedSampleIds.length === 0}
                              >
                                <FormattedMessage
                                  id="biorepository.disposal.button"
                                  defaultMessage="Dispose Selected ({count})"
                                  values={{ count: selectedSampleIds.length }}
                                />
                              </Button>
                            </TableToolbarContent>
                          </TableToolbar>
                          <Table {...getTableProps()}>
                            <TableHead>
                              <TableRow>
                                <TableSelectAll
                                  {...getSelectionProps()}
                                  onSelect={(e) => {
                                    if (e.target.checked) {
                                      setSelectedSampleIds(
                                        rows.map((r) => r.id),
                                      );
                                    } else {
                                      setSelectedSampleIds([]);
                                    }
                                  }}
                                />
                                {headers.map((header) => (
                                  <TableHeader
                                    key={header.key}
                                    {...getHeaderProps({ header })}
                                  >
                                    {header.header}
                                  </TableHeader>
                                ))}
                              </TableRow>
                            </TableHead>
                            <TableBody>
                              {rows.map((row) => (
                                <TableRow
                                  key={row.id}
                                  {...getRowProps({ row })}
                                >
                                  <TableSelectRow
                                    {...getSelectionProps({ row })}
                                    checked={selectedSampleIds.includes(row.id)}
                                    onSelect={() => {
                                      if (selectedSampleIds.includes(row.id)) {
                                        setSelectedSampleIds(
                                          selectedSampleIds.filter(
                                            (id) => id !== row.id,
                                          ),
                                        );
                                      } else {
                                        setSelectedSampleIds([
                                          ...selectedSampleIds,
                                          row.id,
                                        ]);
                                      }
                                    }}
                                  />
                                  {row.cells.map((cell) => (
                                    <TableCell key={cell.id}>
                                      {cell.info.header ===
                                      "retentionExpiryDate" ? (
                                        <div
                                          style={{
                                            display: "flex",
                                            alignItems: "center",
                                            gap: "0.5rem",
                                          }}
                                        >
                                          {formatDate(cell.value)}
                                          {getExpiryTag(cell.value)}
                                        </div>
                                      ) : cell.info.header === "status" ? (
                                        <Tag
                                          type={
                                            cell.value === "DISPOSED"
                                              ? "gray"
                                              : "blue"
                                          }
                                          size="sm"
                                        >
                                          {cell.value}
                                        </Tag>
                                      ) : (
                                        cell.value || "-"
                                      )}
                                    </TableCell>
                                  ))}
                                </TableRow>
                              ))}
                            </TableBody>
                          </Table>
                        </TableContainer>
                      )}
                    </DataTable>
                  )}
                </TabPanel>
              ))}
              {/* Manual Disposal Tab Panel */}
              <TabPanel>
                <div style={{ maxWidth: "600px" }}>
                  <h5 style={{ marginBottom: "1rem" }}>
                    <FormattedMessage
                      id="biorepository.disposal.manual.title"
                      defaultMessage="Search Sample by Barcode"
                    />
                  </h5>
                  <p
                    style={{
                      color: "#525252",
                      fontSize: "0.875rem",
                      marginBottom: "1.5rem",
                    }}
                  >
                    <FormattedMessage
                      id="biorepository.disposal.manual.description"
                      defaultMessage="Enter a sample barcode to search and dispose it manually. Only samples that are currently in storage can be disposed."
                    />
                  </p>

                  {/* Search Input */}
                  <div
                    style={{
                      display: "flex",
                      gap: "0.5rem",
                      marginBottom: "1.5rem",
                    }}
                  >
                    <TextInput
                      id="barcode-search"
                      labelText={intl.formatMessage({
                        id: "biorepository.disposal.manual.barcodeLabel",
                        defaultMessage: "Sample Barcode",
                      })}
                      placeholder={intl.formatMessage({
                        id: "biorepository.disposal.manual.barcodePlaceholder",
                        defaultMessage: "Enter barcode (e.g., BIO-2026-001)",
                      })}
                      value={barcodeSearch}
                      onChange={(e) => {
                        setBarcodeSearch(e.target.value);
                        setSearchError(null);
                      }}
                      onKeyDown={(e) => {
                        if (e.key === "Enter") {
                          handleBarcodeSearch();
                        }
                      }}
                      style={{ flex: 1 }}
                    />
                    <Button
                      kind="primary"
                      renderIcon={Search}
                      onClick={handleBarcodeSearch}
                      disabled={searching || !barcodeSearch.trim()}
                      style={{ alignSelf: "flex-end" }}
                    >
                      {searching ? (
                        <FormattedMessage
                          id="biorepository.searching"
                          defaultMessage="Searching..."
                        />
                      ) : (
                        <FormattedMessage
                          id="biorepository.search"
                          defaultMessage="Search"
                        />
                      )}
                    </Button>
                  </div>

                  {/* Search Error */}
                  {searchError && (
                    <InlineNotification
                      kind="error"
                      title={intl.formatMessage({
                        id: "biorepository.disposal.search.error",
                        defaultMessage: "Search Error",
                      })}
                      subtitle={searchError}
                      onCloseButtonClick={() => setSearchError(null)}
                      style={{ marginBottom: "1rem" }}
                    />
                  )}

                  {/* Search Result */}
                  {searchedSample && (
                    <div
                      style={{
                        background: "#f4f4f4",
                        padding: "1.5rem",
                        borderRadius: "4px",
                        border: "1px solid #e0e0e0",
                      }}
                    >
                      <h6 style={{ marginBottom: "1rem" }}>
                        <FormattedMessage
                          id="biorepository.disposal.manual.sampleFound"
                          defaultMessage="Sample Found"
                        />
                      </h6>
                      <div
                        style={{
                          display: "grid",
                          gridTemplateColumns: "1fr 1fr",
                          gap: "1rem",
                          marginBottom: "1.5rem",
                        }}
                      >
                        <div>
                          <div
                            style={{
                              fontSize: "0.75rem",
                              color: "#525252",
                              marginBottom: "0.25rem",
                            }}
                          >
                            <FormattedMessage
                              id="biorepository.column.barcode"
                              defaultMessage="Barcode"
                            />
                          </div>
                          <div style={{ fontWeight: "600" }}>
                            {searchedSample.barcode ||
                              searchedSample.externalId ||
                              "-"}
                          </div>
                        </div>
                        <div>
                          <div
                            style={{
                              fontSize: "0.75rem",
                              color: "#525252",
                              marginBottom: "0.25rem",
                            }}
                          >
                            <FormattedMessage
                              id="biorepository.column.sampleType"
                              defaultMessage="Sample Type"
                            />
                          </div>
                          <div>
                            {searchedSample.sampleType?.description || "-"}
                          </div>
                        </div>
                        <div>
                          <div
                            style={{
                              fontSize: "0.75rem",
                              color: "#525252",
                              marginBottom: "0.25rem",
                            }}
                          >
                            <FormattedMessage
                              id="biorepository.column.status"
                              defaultMessage="Status"
                            />
                          </div>
                          <Tag
                            type={
                              searchedSample.workflowStatus === "STORED"
                                ? "green"
                                : "blue"
                            }
                            size="sm"
                          >
                            {searchedSample.workflowStatus || "REGISTERED"}
                          </Tag>
                        </div>
                        <div>
                          <div
                            style={{
                              fontSize: "0.75rem",
                              color: "#525252",
                              marginBottom: "0.25rem",
                            }}
                          >
                            <FormattedMessage
                              id="biorepository.column.expiryDate"
                              defaultMessage="Expiry Date"
                            />
                          </div>
                          <div
                            style={{
                              display: "flex",
                              alignItems: "center",
                              gap: "0.5rem",
                            }}
                          >
                            {formatDate(searchedSample.retentionExpiryDate)}
                            {searchedSample.retentionExpiryDate &&
                              getExpiryTag(searchedSample.retentionExpiryDate)}
                          </div>
                        </div>
                        <div>
                          <div
                            style={{
                              fontSize: "0.75rem",
                              color: "#525252",
                              marginBottom: "0.25rem",
                            }}
                          >
                            <FormattedMessage
                              id="biorepository.column.project"
                              defaultMessage="Project"
                            />
                          </div>
                          <div>{searchedSample.projectId || "-"}</div>
                        </div>
                        <div>
                          <div
                            style={{
                              fontSize: "0.75rem",
                              color: "#525252",
                              marginBottom: "0.25rem",
                            }}
                          >
                            <FormattedMessage
                              id="biorepository.column.originLab"
                              defaultMessage="Origin"
                            />
                          </div>
                          <div>{searchedSample.originLab || "-"}</div>
                        </div>
                      </div>
                      <Button
                        kind="danger"
                        renderIcon={TrashCan}
                        onClick={handleManualDisposal}
                      >
                        <FormattedMessage
                          id="biorepository.disposal.manual.disposeButton"
                          defaultMessage="Dispose This Sample"
                        />
                      </Button>
                    </div>
                  )}
                </div>
              </TabPanel>
            </TabPanels>
          </Tabs>
        </Column>
      </Grid>

      {/* Disposal Modal */}
      <Modal
        open={disposalModalOpen}
        onRequestClose={() => setDisposalModalOpen(false)}
        onRequestSubmit={handleDisposalSubmit}
        modalHeading={
          verificationStep
            ? intl.formatMessage({
                id: "biorepository.disposal.verify.heading",
                defaultMessage: "Confirm Disposal",
              })
            : intl.formatMessage({
                id: "biorepository.disposal.heading",
                defaultMessage: "Dispose Samples",
              })
        }
        primaryButtonText={
          verificationStep
            ? intl.formatMessage({
                id: "biorepository.disposal.confirm",
                defaultMessage: "Confirm Disposal",
              })
            : intl.formatMessage({
                id: "biorepository.disposal.continue",
                defaultMessage: "Continue",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "biorepository.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={
          disposing ||
          (!verificationStep && (!disposalForm.reason || !disposalForm.method))
        }
        danger={verificationStep}
        size="md"
      >
        {disposing ? (
          <Loading withOverlay={false} description="Disposing samples..." />
        ) : verificationStep ? (
          <div>
            <InlineNotification
              kind="warning"
              title={intl.formatMessage({
                id: "biorepository.disposal.verify.warning",
                defaultMessage: "This action cannot be undone",
              })}
              subtitle={intl.formatMessage(
                {
                  id: "biorepository.disposal.verify.message",
                  defaultMessage:
                    "You are about to dispose {count} sample(s). Please verify the details below.",
                },
                { count: selectedSampleIds.length },
              )}
              lowContrast
              hideCloseButton
              style={{ marginBottom: "1rem" }}
            />
            <div style={{ marginBottom: "1rem" }}>
              <strong>
                <FormattedMessage
                  id="biorepository.disposal.sampleCount"
                  defaultMessage="Samples to dispose:"
                />
              </strong>{" "}
              {selectedSampleIds.length}
            </div>
            <div style={{ marginBottom: "1rem" }}>
              <strong>
                <FormattedMessage
                  id="biorepository.disposal.reason"
                  defaultMessage="Reason:"
                />
              </strong>{" "}
              {disposalForm.reason}
            </div>
            <div style={{ marginBottom: "1rem" }}>
              <strong>
                <FormattedMessage
                  id="biorepository.disposal.method"
                  defaultMessage="Method:"
                />
              </strong>{" "}
              {disposalForm.method}
            </div>
            {disposalForm.notes && (
              <div style={{ marginBottom: "1rem" }}>
                <strong>
                  <FormattedMessage
                    id="biorepository.disposal.notes"
                    defaultMessage="Notes:"
                  />
                </strong>{" "}
                {disposalForm.notes}
              </div>
            )}
            <Checkbox
              id="disposal-verification-checkbox"
              labelText={intl.formatMessage({
                id: "biorepository.disposal.verify.checkbox",
                defaultMessage:
                  "I confirm that I have verified the disposal details and authorize this action.",
              })}
            />
          </div>
        ) : (
          <div>
            <p style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="biorepository.disposal.form.description"
                defaultMessage="Complete the disposal form for {count} selected sample(s)."
                values={{ count: selectedSampleIds.length }}
              />
            </p>
            <Select
              id="disposal-reason"
              labelText={intl.formatMessage({
                id: "biorepository.disposal.reason.label",
                defaultMessage: "Disposal Reason",
              })}
              value={disposalForm.reason}
              onChange={(e) =>
                setDisposalForm({ ...disposalForm, reason: e.target.value })
              }
              style={{ marginBottom: "1rem" }}
            >
              <SelectItem
                value=""
                text={intl.formatMessage({
                  id: "biorepository.select",
                  defaultMessage: "Select...",
                })}
              />
              <SelectItem
                value="RETENTION_EXPIRED"
                text={intl.formatMessage({
                  id: "biorepository.disposal.reason.expired",
                  defaultMessage: "Retention Period Expired",
                })}
              />
              <SelectItem
                value="QUALITY_FAILED"
                text={intl.formatMessage({
                  id: "biorepository.disposal.reason.quality",
                  defaultMessage: "Quality Check Failed",
                })}
              />
              <SelectItem
                value="DAMAGED"
                text={intl.formatMessage({
                  id: "biorepository.disposal.reason.damaged",
                  defaultMessage: "Sample Damaged",
                })}
              />
              <SelectItem
                value="CONSENT_WITHDRAWN"
                text={intl.formatMessage({
                  id: "biorepository.disposal.reason.consent",
                  defaultMessage: "Consent Withdrawn",
                })}
              />
              <SelectItem
                value="OTHER"
                text={intl.formatMessage({
                  id: "biorepository.disposal.reason.other",
                  defaultMessage: "Other",
                })}
              />
            </Select>
            <Select
              id="disposal-method"
              labelText={intl.formatMessage({
                id: "biorepository.disposal.method.label",
                defaultMessage: "Disposal Method",
              })}
              value={disposalForm.method}
              onChange={(e) =>
                setDisposalForm({ ...disposalForm, method: e.target.value })
              }
              style={{ marginBottom: "1rem" }}
            >
              <SelectItem
                value=""
                text={intl.formatMessage({
                  id: "biorepository.select",
                  defaultMessage: "Select...",
                })}
              />
              <SelectItem
                value="AUTOCLAVE"
                text={intl.formatMessage({
                  id: "biorepository.disposal.method.autoclave",
                  defaultMessage: "Autoclaving",
                })}
              />
              <SelectItem
                value="INCINERATION"
                text={intl.formatMessage({
                  id: "biorepository.disposal.method.incineration",
                  defaultMessage: "Incineration",
                })}
              />
              <SelectItem
                value="CHEMICAL_NEUTRALIZATION"
                text={intl.formatMessage({
                  id: "biorepository.disposal.method.chemical",
                  defaultMessage: "Chemical Neutralization",
                })}
              />
              <SelectItem
                value="OTHER"
                text={intl.formatMessage({
                  id: "biorepository.disposal.method.other",
                  defaultMessage: "Other",
                })}
              />
            </Select>
            <TextArea
              id="disposal-notes"
              labelText={intl.formatMessage({
                id: "biorepository.disposal.notes.label",
                defaultMessage: "Notes (Optional)",
              })}
              value={disposalForm.notes}
              onChange={(e) =>
                setDisposalForm({ ...disposalForm, notes: e.target.value })
              }
              placeholder={intl.formatMessage({
                id: "biorepository.disposal.notes.placeholder",
                defaultMessage:
                  "Add any additional notes about this disposal...",
              })}
              rows={3}
            />
          </div>
        )}
      </Modal>
    </div>
  );
}

BiorepositoryRetentionDisposalPage.propTypes = {
  entryId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  pageData: PropTypes.object,
  progress: PropTypes.object,
  onProgressUpdate: PropTypes.func,
  notebookId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
};

export default BiorepositoryRetentionDisposalPage;
