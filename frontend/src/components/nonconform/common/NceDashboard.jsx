import React, { useState, useEffect, useContext, useCallback } from "react";
import {
  Button,
  Search,
  Select,
  SelectItem,
  Tag,
  Tile,
  Pagination,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  TextArea,
  Loading,
  ComboBox,
} from "@carbon/react";
import {
  Add,
  Warning,
  CheckmarkFilled,
  InProgress,
  UserFollow,
  DocumentAdd,
  Time,
  Chemistry,
  DataBase,
  Download,
  Document,
  View,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, postToOpenElisServer } from "../../utils/Utils";
import config from "../../../config.json";
import { NotificationContext } from "../../layout/Layout";
import { useHistory } from "react-router-dom";
import "./NceDashboard.css";

const STATUS_CONFIG = {
  Pending: { type: "green", icon: InProgress, labelKey: "nce.status.open" },
  "Under Investigation": {
    type: "blue",
    icon: InProgress,
    labelKey: "nce.status.underInvestigation",
  },
  "Corrective Action": {
    type: "purple",
    icon: CheckmarkFilled,
    labelKey: "nce.status.correctiveAction",
  },
  Closed: {
    type: "gray",
    icon: CheckmarkFilled,
    labelKey: "nce.status.closed",
  },
};

export const NceDashboard = () => {
  const intl = useIntl();
  const history = useHistory();
  const { addNotification } = useContext(NotificationContext);

  // State
  const [loading, setLoading] = useState(true);
  const [nceList, setNceList] = useState([]);
  const [filteredList, setFilteredList] = useState([]);
  const [categories, setCategories] = useState([]);
  const [expandedRows, setExpandedRows] = useState({});

  // Filters
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("");
  const [severityFilter, setSeverityFilter] = useState("");

  // Pagination
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);

  // Summary counts
  const [summaryCounts, setSummaryCounts] = useState({
    critical: 0,
    major: 0,
    minor: 0,
    overdue: 0,
  });

  // Inline form state (keyed by nce id)
  const [noteFormOpen, setNoteFormOpen] = useState(null);
  const [assignFormOpen, setAssignFormOpen] = useState(null);
  const [noteText, setNoteText] = useState("");
  const [assignee, setAssignee] = useState(null);
  const [users, setUsers] = useState([]);

  // Load NCE data
  const loadNceData = useCallback(() => {
    setLoading(true);
    getFromOpenElisServer("/rest/nce/dashboard", (data) => {
      if (data && data.nceList) {
        setNceList(data.nceList);
        setFilteredList(data.nceList);
        calculateSummaryCounts(data.nceList);
      }
      setLoading(false);
    });
  }, []);

  // Load categories
  const loadCategories = useCallback(() => {
    getFromOpenElisServer("/rest/nce/categories", (data) => {
      if (data) {
        setCategories(data);
      }
    });
  }, []);

  // Load users for assignment
  const loadUsers = useCallback(() => {
    getFromOpenElisServer("/rest/nce/users", (data) => {
      if (data && Array.isArray(data)) {
        setUsers(data);
      }
    });
  }, []);

  useEffect(() => {
    loadNceData();
    loadCategories();
    loadUsers();
  }, [loadNceData, loadCategories, loadUsers]);

  // Calculate summary counts
  const calculateSummaryCounts = (list) => {
    const counts = {
      critical: 0,
      major: 0,
      minor: 0,
      overdue: 0,
    };

    const now = new Date();
    list.forEach((nce) => {
      if (nce.severity === "CRITICAL") counts.critical++;
      else if (nce.severity === "MAJOR") counts.major++;
      else if (nce.severity === "MINOR" || nce.severity === "LOW")
        counts.minor++;

      // Check if overdue (more than 7 days old and not closed)
      if (nce.status !== "Closed" && nce.dateOfEvent) {
        const eventDate = new Date(nce.dateOfEvent);
        const daysDiff = Math.floor((now - eventDate) / (1000 * 60 * 60 * 24));
        if (daysDiff > 7) counts.overdue++;
      }
    });

    setSummaryCounts(counts);
  };

  // Apply filters
  useEffect(() => {
    let filtered = [...nceList];

    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter(
        (nce) =>
          nce.nceNumber?.toLowerCase().includes(term) ||
          nce.title?.toLowerCase().includes(term) ||
          nce.description?.toLowerCase().includes(term) ||
          nce.labOrderNumber?.toLowerCase().includes(term),
      );
    }

    if (statusFilter) {
      filtered = filtered.filter((nce) => nce.status === statusFilter);
    }

    if (categoryFilter) {
      filtered = filtered.filter(
        (nce) => nce.nceCategoryId === parseInt(categoryFilter),
      );
    }

    if (severityFilter) {
      filtered = filtered.filter((nce) => nce.severity === severityFilter);
    }

    setFilteredList(filtered);
    setCurrentPage(1);
  }, [searchTerm, statusFilter, categoryFilter, severityFilter, nceList]);

  // Clear all filters
  const clearFilters = () => {
    setSearchTerm("");
    setStatusFilter("");
    setCategoryFilter("");
    setSeverityFilter("");
  };

  // Toggle row expansion
  const toggleRowExpansion = (nceId) => {
    setExpandedRows((prev) => ({
      ...prev,
      [nceId]: !prev[nceId],
    }));
  };

  // Get days since event
  const getDaysSince = (dateString) => {
    if (!dateString) return null;
    const eventDate = new Date(dateString);
    const now = new Date();
    return Math.floor((now - eventDate) / (1000 * 60 * 60 * 24));
  };

  // Check if NCE is overdue
  const isOverdue = (nce) => {
    if (nce.status === "Closed") return false;
    const days = getDaysSince(nce.dateOfEvent);
    return days !== null && days > 7;
  };

  // Get paginated data
  const getPaginatedData = () => {
    const startIndex = (currentPage - 1) * pageSize;
    return filteredList.slice(startIndex, startIndex + pageSize);
  };

  // Handle acknowledge
  const handleAcknowledge = (nce) => {
    // Log acknowledgment via history
    const payload = {
      nceId: nce.id,
      activity: "ACKNOWLEDGED",
      description: "NCE acknowledged",
    };
    postToOpenElisServer(
      "/rest/nce/history",
      JSON.stringify(payload),
      (response) => {
        if (response) {
          addNotification({
            kind: "success",
            title: intl.formatMessage({
              id: "notification.success",
              defaultMessage: "Success",
            }),
            message: intl.formatMessage({
              id: "nce.acknowledge.success",
              defaultMessage: "NCE acknowledged successfully",
            }),
          });
          loadNceData(); // Refresh data
        }
      },
    );
  };

  // Toggle inline assign form
  const handleAssign = (nce) => {
    if (assignFormOpen === nce.id) {
      setAssignFormOpen(null);
    } else {
      setAssignFormOpen(nce.id);
      setNoteFormOpen(null);
      setAssignee(null);
      loadUsers();
    }
  };

  // Submit assignment
  const submitAssign = (nceId) => {
    if (!assignee || !assignee.id) {
      addNotification({
        kind: "warning",
        title: intl.formatMessage({
          id: "notification.warning",
          defaultMessage: "Warning",
        }),
        message: intl.formatMessage({
          id: "nce.assign.selectUser",
          defaultMessage: "Please select a user to assign",
        }),
      });
      return;
    }

    const payload = {
      nceId: nceId,
      assignedTo: assignee.id,
    };
    postToOpenElisServer(
      "/rest/nce/assign",
      JSON.stringify(payload),
      (response) => {
        if (response) {
          addNotification({
            kind: "success",
            title: intl.formatMessage({
              id: "notification.success",
              defaultMessage: "Success",
            }),
            message: intl.formatMessage({
              id: "nce.assign.success",
              defaultMessage: "NCE assigned successfully",
            }),
          });
          setAssignFormOpen(null);
          setAssignee(null);
          loadNceData();
        }
      },
    );
  };

  // Toggle inline note form
  const handleAddNote = (nce) => {
    if (noteFormOpen === nce.id) {
      setNoteFormOpen(null);
    } else {
      setNoteFormOpen(nce.id);
      setAssignFormOpen(null);
      setNoteText("");
    }
  };

  // Submit note
  const submitNote = (nceId) => {
    if (!noteText.trim()) {
      addNotification({
        kind: "warning",
        title: intl.formatMessage({
          id: "notification.warning",
          defaultMessage: "Warning",
        }),
        message: intl.formatMessage({
          id: "nce.note.empty",
          defaultMessage: "Please enter a note",
        }),
      });
      return;
    }

    const payload = {
      nceId: nceId,
      activity: "NOTE_ADDED",
      description: noteText,
    };
    postToOpenElisServer(
      "/rest/nce/history",
      JSON.stringify(payload),
      (response) => {
        if (response) {
          addNotification({
            kind: "success",
            title: intl.formatMessage({
              id: "notification.success",
              defaultMessage: "Success",
            }),
            message: intl.formatMessage({
              id: "nce.note.success",
              defaultMessage: "Note added successfully",
            }),
          });
          setNoteFormOpen(null);
          setNoteText("");
          loadNceData();
        } else {
          addNotification({
            kind: "error",
            title: intl.formatMessage({
              id: "notification.error",
              defaultMessage: "Error",
            }),
            message: intl.formatMessage({
              id: "nce.note.error",
              defaultMessage: "Failed to add note",
            }),
          });
        }
      },
    );
  };

  // Navigate to report NCE
  const handleReportNce = () => {
    history.push("/ReportNonConformingEvent");
  };

  // Navigate to NCE details
  const handleViewDetails = (nce) => {
    history.push(`/ViewNonConformingEvent?nceNumber=${nce.nceNumber}`);
  };

  // Handle attachment download with authentication
  const handleDownloadAttachment = async (attachmentId, fileName) => {
    try {
      const url = `${config.serverBaseUrl}/rest/nce/attachments/${attachmentId}/download`;
      const response = await fetch(url, {
        method: "GET",
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error("Download failed");
      }

      const blob = await response.blob();
      const downloadUrl = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = downloadUrl;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(downloadUrl);
    } catch (error) {
      console.error("Error downloading attachment:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({
          id: "notification.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "nce.attachment.downloadError",
          defaultMessage: "Failed to download attachment",
        }),
      });
    }
  };

  // Handle attachment view (for images and PDFs) with authentication
  const handleViewAttachment = async (attachmentId, fileType) => {
    try {
      const url = `${config.serverBaseUrl}/rest/nce/attachments/${attachmentId}/download`;
      const response = await fetch(url, {
        method: "GET",
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error("View failed");
      }

      const blob = await response.blob();
      const viewUrl = window.URL.createObjectURL(blob);
      const newWindow = window.open(viewUrl, "_blank");
      // Revoke object URL after the new window loads to free memory
      if (newWindow) {
        newWindow.addEventListener("load", () => {
          window.URL.revokeObjectURL(viewUrl);
        });
      } else {
        // Fallback: revoke after a timeout if popup was blocked
        setTimeout(() => window.URL.revokeObjectURL(viewUrl), 60000);
      }
    } catch (error) {
      console.error("Error viewing attachment:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({
          id: "notification.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "nce.attachment.viewError",
          defaultMessage: "Failed to view attachment",
        }),
      });
    }
  };

  // Check if file type is viewable in browser
  const isViewableFileType = (fileType) => {
    if (!fileType) return false;
    return (
      fileType.startsWith("image/") ||
      fileType === "application/pdf" ||
      fileType.startsWith("text/")
    );
  };

  // Format file size for display
  const formatFileSize = (bytes) => {
    if (!bytes) return "";
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  // Get category name
  const getCategoryName = (categoryId) => {
    for (const cat of categories) {
      if (cat.id === String(categoryId)) return cat.name;
    }
    return "";
  };

  // Get type name
  const getTypeName = (categoryId, typeId) => {
    for (const cat of categories) {
      if (cat.id === String(categoryId) && cat.types) {
        const type = cat.types.find((t) => t.id === String(typeId));
        if (type) return type.name;
      }
    }
    return "";
  };

  if (loading) {
    return (
      <div className="nce-dashboard-loading">
        <Loading description="Loading NCE data..." withOverlay={false} />
      </div>
    );
  }

  return (
    <div className="nce-dashboard">
      {/* Header */}
      <div className="nce-dashboard-header">
        <div className="nce-dashboard-title">
          <span className="nce-breadcrumb">NCE &gt; All NCEs</span>
          <h1>
            <Warning size={24} />
            <FormattedMessage
              id="nce.dashboard.title"
              defaultMessage="All NCEs"
            />
          </h1>
          <p className="nce-dashboard-subtitle">
            <FormattedMessage
              id="nce.dashboard.subtitle"
              defaultMessage="All open NCEs across the laboratory"
            />
          </p>
        </div>
        <Button renderIcon={Add} onClick={handleReportNce}>
          <FormattedMessage
            id="nce.button.reportNce"
            defaultMessage="Report NCE"
          />
        </Button>
      </div>

      {/* Filters */}
      <div className="nce-dashboard-filters">
        <Search
          labelText=""
          placeholder={intl.formatMessage({
            id: "nce.search.placeholder",
            defaultMessage: "Search NCEs...",
          })}
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="nce-search"
        />
        <Select
          id="status-filter"
          labelText=""
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="nce-filter-select"
        >
          <SelectItem
            value=""
            text={intl.formatMessage({
              id: "nce.filter.allStatus",
              defaultMessage: "All Status",
            })}
          />
          <SelectItem value="Pending" text="Open" />
          <SelectItem value="Under Investigation" text="Under Investigation" />
          <SelectItem value="Corrective Action" text="Corrective Action" />
          <SelectItem value="Closed" text="Closed" />
        </Select>
        <Select
          id="category-filter"
          labelText=""
          value={categoryFilter}
          onChange={(e) => setCategoryFilter(e.target.value)}
          className="nce-filter-select"
        >
          <SelectItem
            value=""
            text={intl.formatMessage({
              id: "nce.filter.allCategories",
              defaultMessage: "All Categories",
            })}
          />
          {categories.map((cat) => (
            <SelectItem key={cat.id} value={cat.id} text={cat.name} />
          ))}
        </Select>
        <Select
          id="severity-filter"
          labelText=""
          value={severityFilter}
          onChange={(e) => setSeverityFilter(e.target.value)}
          className="nce-filter-select"
        >
          <SelectItem
            value=""
            text={intl.formatMessage({
              id: "nce.filter.allSeverities",
              defaultMessage: "All Severities",
            })}
          />
          <SelectItem value="CRITICAL" text="Critical" />
          <SelectItem value="MAJOR" text="Major" />
          <SelectItem value="MINOR" text="Minor" />
          <SelectItem value="LOW" text="Low" />
        </Select>
        <Button kind="ghost" onClick={clearFilters}>
          <FormattedMessage
            id="nce.filter.clearAll"
            defaultMessage="Clear All"
          />
        </Button>
      </div>

      {/* Summary Cards */}
      <div className="nce-summary-cards">
        <Tile className="nce-summary-card nce-summary-critical">
          <span className="nce-summary-icon">
            <Warning size={20} />
          </span>
          <span className="nce-summary-label">
            <FormattedMessage
              id="nce.summary.critical"
              defaultMessage="Critical"
            />
          </span>
          <span className="nce-summary-count">{summaryCounts.critical}</span>
        </Tile>
        <Tile className="nce-summary-card nce-summary-major">
          <span className="nce-summary-icon">
            <Warning size={20} />
          </span>
          <span className="nce-summary-label">
            <FormattedMessage id="nce.summary.major" defaultMessage="Major" />
          </span>
          <span className="nce-summary-count">{summaryCounts.major}</span>
        </Tile>
        <Tile className="nce-summary-card nce-summary-minor">
          <span className="nce-summary-icon">
            <Warning size={20} />
          </span>
          <span className="nce-summary-label">
            <FormattedMessage id="nce.summary.minor" defaultMessage="Minor" />
          </span>
          <span className="nce-summary-count">{summaryCounts.minor}</span>
        </Tile>
        <Tile className="nce-summary-card nce-summary-overdue">
          <span className="nce-summary-icon">
            <Time size={20} />
          </span>
          <span className="nce-summary-label">
            <FormattedMessage
              id="nce.summary.overdue"
              defaultMessage="Overdue"
            />
          </span>
          <span className="nce-summary-count">{summaryCounts.overdue}</span>
        </Tile>
      </div>

      {/* NCE List */}
      <div className="nce-list">
        {getPaginatedData().map((nce) => (
          <div key={nce.id} className="nce-list-item">
            <div
              className="nce-list-item-header"
              onClick={() => toggleRowExpansion(nce.id)}
            >
              <div className="nce-list-item-main">
                <input
                  type="checkbox"
                  className="nce-checkbox"
                  onClick={(e) => e.stopPropagation()}
                />
                <div className="nce-item-info">
                  <div className="nce-item-top">
                    <span className="nce-number">{nce.nceNumber}</span>
                    {nce.linkedSpecimens && nce.linkedSpecimens.length > 0 && (
                      <span className="nce-linked-badge">
                        <DataBase size={14} />
                        {nce.linkedSpecimens.map((spec, idx) => (
                          <span key={idx} className="nce-linked-specimen">
                            {spec.labOrderNumber}
                            {spec.sampleType && ` (${spec.sampleType})`}
                            {idx < nce.linkedSpecimens.length - 1 && ", "}
                          </span>
                        ))}
                      </span>
                    )}
                    <Tag
                      type={STATUS_CONFIG[nce.status]?.type || "gray"}
                      size="sm"
                    >
                      {STATUS_CONFIG[nce.status]?.labelKey
                        ? intl.formatMessage({
                            id: STATUS_CONFIG[nce.status].labelKey,
                          })
                        : nce.status}
                    </Tag>
                    {isOverdue(nce) && (
                      <Tag type="red" size="sm">
                        <FormattedMessage
                          id="nce.tag.overdue"
                          defaultMessage="Overdue"
                        />
                      </Tag>
                    )}
                  </div>
                  <div className="nce-item-title">
                    {nce.title || nce.description}
                  </div>
                  <div className="nce-item-meta">
                    <span>{getCategoryName(nce.nceCategoryId)}</span>
                    {nce.nceTypeId && (
                      <>
                        <span> - </span>
                        <span>
                          {getTypeName(nce.nceCategoryId, nce.nceTypeId)}
                        </span>
                      </>
                    )}
                    {nce.assignedTo && (
                      <>
                        <span> - Assigned: </span>
                        <span>{nce.assignedToName}</span>
                      </>
                    )}
                  </div>
                </div>
              </div>
              <div className="nce-item-days">
                {getDaysSince(nce.dateOfEvent) !== null && (
                  <span className={isOverdue(nce) ? "overdue" : ""}>
                    {getDaysSince(nce.dateOfEvent)} days
                  </span>
                )}
              </div>
            </div>

            {expandedRows[nce.id] && (
              <div className="nce-list-item-details">
                <Tabs>
                  <TabList aria-label="NCE details tabs">
                    <Tab>
                      <FormattedMessage
                        id="nce.tab.eventDetails"
                        defaultMessage="Event Details"
                      />
                    </Tab>
                    <Tab>
                      <FormattedMessage
                        id="nce.tab.investigation"
                        defaultMessage="Investigation"
                      />
                    </Tab>
                    <Tab>
                      <FormattedMessage
                        id="nce.tab.capa"
                        defaultMessage="CAPA"
                      />{" "}
                      ({nce.capaCount || 0})
                    </Tab>
                    <Tab>
                      <FormattedMessage
                        id="nce.tab.history"
                        defaultMessage="History"
                      />
                    </Tab>
                  </TabList>
                  <TabPanels>
                    {/* Event Details Tab */}
                    <TabPanel>
                      <div className="nce-detail-section">
                        <h4>
                          <FormattedMessage
                            id="nce.field.description"
                            defaultMessage="Description"
                          />
                        </h4>
                        <p>{nce.description || "-"}</p>
                      </div>
                      <div className="nce-detail-section">
                        <h4>
                          <FormattedMessage
                            id="nce.field.immediateAction"
                            defaultMessage="Immediate Action"
                          />
                        </h4>
                        <p>{nce.immediateAction || "-"}</p>
                      </div>
                      {nce.triggerSourceType && (
                        <div className="nce-detail-section">
                          <h4>
                            <FormattedMessage
                              id="nce.field.trigger"
                              defaultMessage="Trigger"
                            />
                          </h4>
                          <p>{nce.triggerSourceType}</p>
                        </div>
                      )}
                      {nce.linkedSpecimens &&
                        nce.linkedSpecimens.length > 0 && (
                          <div className="nce-detail-section">
                            <h4>
                              <FormattedMessage
                                id="nce.field.linkedItems"
                                defaultMessage="Linked Items"
                              />
                            </h4>
                            <div className="nce-linked-items">
                              {nce.linkedSpecimens.map((specimen, idx) => (
                                <div key={idx} className="nce-linked-item">
                                  <DataBase size={16} />
                                  <span>
                                    Specimen #{specimen.sampleItemId}
                                    {specimen.sampleType
                                      ? ` — ${specimen.sampleType}`
                                      : ""}
                                    {specimen.labOrderNumber
                                      ? ` (${specimen.labOrderNumber})`
                                      : ""}
                                  </span>
                                  {specimen.testName && (
                                    <span style={{ marginLeft: "0.25rem" }}>
                                      <Chemistry size={16} />
                                      {` Test: ${specimen.testName}`}
                                    </span>
                                  )}
                                </div>
                              ))}
                              {nce.linkedResults &&
                                nce.linkedResults.map((result, idx) => (
                                  <div key={idx} className="nce-linked-item">
                                    <Chemistry size={16} />
                                    <span>Result: {result.testName}</span>
                                  </div>
                                ))}
                            </div>
                          </div>
                        )}
                      {/* Attachments section */}
                      {nce.attachments && nce.attachments.length > 0 && (
                        <div className="nce-detail-section">
                          <h4>
                            <FormattedMessage
                              id="nce.field.attachments"
                              defaultMessage="Attachments"
                            />
                          </h4>
                          <div className="nce-attachments-list">
                            {nce.attachments.map((attachment) => (
                              <div
                                key={attachment.id}
                                className="nce-attachment-item"
                              >
                                <Document size={16} />
                                <span className="nce-attachment-name">
                                  {attachment.fileName}
                                </span>
                                <span className="nce-attachment-size">
                                  {formatFileSize(attachment.fileSize)}
                                </span>
                                <div className="nce-attachment-actions">
                                  {isViewableFileType(attachment.fileType) && (
                                    <Button
                                      kind="ghost"
                                      size="sm"
                                      hasIconOnly
                                      iconDescription={intl.formatMessage({
                                        id: "label.button.view",
                                        defaultMessage: "View",
                                      })}
                                      renderIcon={View}
                                      onClick={(e) => {
                                        e.stopPropagation();
                                        handleViewAttachment(
                                          attachment.id,
                                          attachment.fileType,
                                        );
                                      }}
                                    />
                                  )}
                                  <Button
                                    kind="ghost"
                                    size="sm"
                                    hasIconOnly
                                    iconDescription={intl.formatMessage({
                                      id: "nce.attachment.download",
                                      defaultMessage: "Download",
                                    })}
                                    renderIcon={Download}
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      handleDownloadAttachment(
                                        attachment.id,
                                        attachment.fileName,
                                      );
                                    }}
                                  />
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
                      {/* Notes section */}
                      {nce.notes && nce.notes.length > 0 && (
                        <div className="nce-detail-section">
                          <h4>
                            <FormattedMessage
                              id="nce.field.notes"
                              defaultMessage="Notes"
                            />{" "}
                            ({nce.notes.length})
                          </h4>
                          <div className="nce-notes-list">
                            {nce.notes.map((note) => (
                              <div key={note.id} className="nce-note-item">
                                <p className="nce-note-text">{note.text}</p>
                                <div className="nce-note-meta">
                                  <span className="nce-note-user">
                                    {note.userName || intl.formatMessage({ id: "nce.history.system", defaultMessage: "System" })}
                                  </span>
                                  <span className="nce-note-time">
                                    {note.timestamp
                                      ? new Date(
                                          note.timestamp,
                                        ).toLocaleString()
                                      : ""}
                                  </span>
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
                      <div className="nce-detail-footer">
                        <span>
                          {nce.notesCount || 0}{" "}
                          <FormattedMessage
                            id="nce.field.notes"
                            defaultMessage="Notes"
                          />
                        </span>
                        <span>
                          {nce.attachments ? nce.attachments.length : 0}{" "}
                          <FormattedMessage
                            id="nce.field.attachments"
                            defaultMessage="Attachments"
                          />
                        </span>
                      </div>
                    </TabPanel>

                    {/* Investigation Tab */}
                    <TabPanel>
                      <div className="nce-detail-section">
                        <h4>
                          <FormattedMessage
                            id="nce.field.suspectedCauses"
                            defaultMessage="Suspected Causes"
                          />
                        </h4>
                        <p>{nce.suspectedCauses || "-"}</p>
                      </div>
                      <div className="nce-detail-section">
                        <h4>
                          <FormattedMessage
                            id="nce.field.proposedAction"
                            defaultMessage="Proposed Action"
                          />
                        </h4>
                        <p>{nce.proposedAction || "-"}</p>
                      </div>
                    </TabPanel>

                    {/* CAPA Tab */}
                    <TabPanel>
                      <p>
                        <FormattedMessage
                          id="nce.capa.noItems"
                          defaultMessage="No corrective/preventive actions recorded yet."
                        />
                      </p>
                    </TabPanel>

                    {/* History Tab */}
                    <TabPanel>
                      {nce.history && nce.history.length > 0 ? (
                        <div className="nce-history-list">
                          {nce.history.map((entry, idx) => (
                            <div
                              key={entry.id || idx}
                              className="nce-history-item"
                            >
                              <div className="nce-history-activity">
                                {entry.activity}
                              </div>
                              {entry.description && (
                                <div className="nce-history-description">
                                  {entry.description}
                                </div>
                              )}
                              <div className="nce-history-meta">
                                <span className="nce-history-user">
                                  {entry.userName || intl.formatMessage({ id: "nce.history.system", defaultMessage: "System" })}
                                </span>
                                <span className="nce-history-time">
                                  {entry.timestamp
                                    ? new Date(entry.timestamp).toLocaleString()
                                    : ""}
                                </span>
                              </div>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <p>
                          <FormattedMessage
                            id="nce.history.noItems"
                            defaultMessage="No history available."
                          />
                        </p>
                      )}
                    </TabPanel>
                  </TabPanels>
                </Tabs>

                {/* Action buttons */}
                <div className="nce-detail-actions">
                  <Button
                    kind="primary"
                    size="sm"
                    onClick={() => handleAcknowledge(nce)}
                  >
                    <FormattedMessage
                      id="nce.action.acknowledge"
                      defaultMessage="Acknowledge"
                    />
                  </Button>
                  <Button
                    kind={assignFormOpen === nce.id ? "secondary" : "tertiary"}
                    size="sm"
                    renderIcon={UserFollow}
                    onClick={() => handleAssign(nce)}
                  >
                    <FormattedMessage
                      id="nce.action.assignTo"
                      defaultMessage="Assign To"
                    />
                  </Button>
                  <Button
                    kind={noteFormOpen === nce.id ? "secondary" : "ghost"}
                    size="sm"
                    renderIcon={DocumentAdd}
                    onClick={() => handleAddNote(nce)}
                  >
                    <FormattedMessage
                      id="nce.action.addNote"
                      defaultMessage="Add Note"
                    />
                  </Button>
                </div>

                {/* Inline assign form */}
                {assignFormOpen === nce.id && (
                  <div className="nce-inline-form">
                    <ComboBox
                      id={`assign-user-${nce.id}`}
                      titleText={intl.formatMessage({
                        id: "nce.modal.selectUser",
                        defaultMessage: "Select User",
                      })}
                      placeholder={intl.formatMessage({
                        id: "nce.modal.searchUser",
                        defaultMessage: "Search for a user...",
                      })}
                      items={users}
                      itemToString={(user) => {
                        if (!user) return "";
                        const name =
                          user.displayName ||
                          `${user.firstName || ""} ${user.lastName || ""}`.trim();
                        if (name && user.loginName) {
                          return `${name} (${user.loginName})`;
                        }
                        return name || user.loginName || "";
                      }}
                      shouldFilterItem={({ item, inputValue }) => {
                        if (!inputValue) return true;
                        const name =
                          item.displayName ||
                          `${item.firstName || ""} ${item.lastName || ""}`.trim();
                        const label =
                          `${name} (${item.loginName || ""})`.toLowerCase();
                        return label.includes(inputValue.toLowerCase());
                      }}
                      selectedItem={assignee}
                      onChange={({ selectedItem }) => setAssignee(selectedItem)}
                    />
                    <div className="nce-inline-form-buttons">
                      <Button
                        kind="primary"
                        size="sm"
                        onClick={() => submitAssign(nce.id)}
                      >
                        <FormattedMessage
                          id="label.button.assign"
                          defaultMessage="Assign"
                        />
                      </Button>
                      <Button
                        kind="ghost"
                        size="sm"
                        onClick={() => setAssignFormOpen(null)}
                      >
                        <FormattedMessage
                          id="label.button.cancel"
                          defaultMessage="Cancel"
                        />
                      </Button>
                    </div>
                  </div>
                )}

                {/* Inline note form */}
                {noteFormOpen === nce.id && (
                  <div className="nce-inline-form">
                    <TextArea
                      labelText={intl.formatMessage({
                        id: "nce.modal.addNote",
                        defaultMessage: "Add Note",
                      })}
                      placeholder={intl.formatMessage({
                        id: "nce.modal.notePlaceholder",
                        defaultMessage: "Enter your note...",
                      })}
                      value={noteText}
                      onChange={(e) => setNoteText(e.target.value)}
                      rows={3}
                    />
                    <div className="nce-inline-form-buttons">
                      <Button
                        kind="primary"
                        size="sm"
                        onClick={() => submitNote(nce.id)}
                      >
                        <FormattedMessage
                          id="label.button.save"
                          defaultMessage="Save"
                        />
                      </Button>
                      <Button
                        kind="ghost"
                        size="sm"
                        onClick={() => setNoteFormOpen(null)}
                      >
                        <FormattedMessage
                          id="label.button.cancel"
                          defaultMessage="Cancel"
                        />
                      </Button>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        ))}

        {filteredList.length === 0 && (
          <div className="nce-empty-state">
            <p>
              <FormattedMessage
                id="nce.list.empty"
                defaultMessage="No NCEs found matching your criteria."
              />
            </p>
          </div>
        )}
      </div>

      {/* Pagination */}
      <Pagination
        totalItems={filteredList.length}
        pageSize={pageSize}
        pageSizes={[10, 25, 50, 100]}
        page={currentPage}
        onChange={({ page, pageSize: newPageSize }) => {
          setCurrentPage(page);
          setPageSize(newPageSize);
        }}
      />
    </div>
  );
};

export default NceDashboard;
