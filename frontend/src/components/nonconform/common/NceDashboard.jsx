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
  Modal,
  TextArea,
  Loading,
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
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
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
  Closed: { type: "gray", icon: CheckmarkFilled, labelKey: "nce.status.closed" },
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

  // Modal state
  const [noteModalOpen, setNoteModalOpen] = useState(false);
  const [selectedNce, setSelectedNce] = useState(null);
  const [noteText, setNoteText] = useState("");

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

  useEffect(() => {
    loadNceData();
    loadCategories();
  }, [loadNceData, loadCategories]);

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
  const handleAcknowledge = () => {
    // TODO: Implement acknowledge API call
  };

  // Handle assign
  const handleAssign = () => {
    // TODO: Implement assign modal/API
  };

  // Handle add note
  const handleAddNote = (nce) => {
    setSelectedNce(nce);
    setNoteText("");
    setNoteModalOpen(true);
  };

  // Submit note
  const submitNote = () => {
    // TODO: Implement note submission API
    setNoteModalOpen(false);
    setSelectedNce(null);
    setNoteText("");
  };

  // Navigate to report NCE
  const handleReportNce = () => {
    history.push("/ReportNonConformingEvent");
  };

  // Navigate to NCE details
  const handleViewDetails = (nce) => {
    history.push(`/ViewNonConformingEvent?nceNumber=${nce.nceNumber}`);
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
                        ? intl.formatMessage({ id: STATUS_CONFIG[nce.status].labelKey })
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
                      {nce.linkedSamples && nce.linkedSamples.length > 0 && (
                        <div className="nce-detail-section">
                          <h4>
                            <FormattedMessage
                              id="nce.field.linkedItems"
                              defaultMessage="Linked Items"
                            />
                          </h4>
                          <div className="nce-linked-items">
                            {nce.linkedSamples.map((sample, idx) => (
                              <div key={idx} className="nce-linked-item">
                                <DataBase size={16} />
                                <span>Sample: {sample.labOrderNumber}</span>
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
                      <div className="nce-detail-footer">
                        <span>{nce.notesCount || 0} notes</span>
                        <span>{nce.attachmentsCount || 0} attachments</span>
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
                      <p>
                        <FormattedMessage
                          id="nce.history.noItems"
                          defaultMessage="No history available."
                        />
                      </p>
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
                    kind="tertiary"
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
                    kind="ghost"
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

      {/* Add Note Modal */}
      <Modal
        open={noteModalOpen}
        onRequestClose={() => setNoteModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "nce.modal.addNote",
          defaultMessage: "Add Note",
        })}
        primaryButtonText={intl.formatMessage({
          id: "label.button.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.button.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={submitNote}
      >
        <p>
          <FormattedMessage
            id="nce.modal.addNoteDescription"
            defaultMessage="Add a note to NCE {nceNumber}"
            values={{ nceNumber: selectedNce?.nceNumber }}
          />
        </p>
        <TextArea
          labelText=""
          placeholder={intl.formatMessage({
            id: "nce.modal.notePlaceholder",
            defaultMessage: "Enter your note...",
          })}
          value={noteText}
          onChange={(e) => setNoteText(e.target.value)}
          rows={4}
        />
      </Modal>
    </div>
  );
};

export default NceDashboard;
