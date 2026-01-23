import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Heading,
  Button,
  Grid,
  Column,
  Section,
  Tile,
  Loading,
  FilterableMultiSelect,
  TextInput,
  Tag,
  Breadcrumb,
  BreadcrumbItem,
} from "@carbon/react";
import NotebookTreeView from "./NotebookTreeView";
import CreateInstanceModal from "./CreateInstanceModal";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { getFromOpenElisServer } from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog } from "../common/CustomNotification";
import { FormattedMessage, useIntl } from "react-intl";
import "../pathology/PathologyDashboard.css";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { usePermissions } from "../../hooks/usePermissions";
import CustomDatePicker from "../common/CustomDatePicker";
import {
  Document,
  Time,
  Tag as TagIcon,
  Edit,
  Checkmark,
  Edit as EditIcon,
  InProgress,
  Locked,
  Archive,
  View,
  List,
} from "@carbon/react/icons";
import "./NoteBook.css";

function NoteBookDashBoard() {
  const componentMounted = useRef(false);

  const { notificationVisible } = useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const { hasRoleForCurrentLabUnit } = usePermissions();

  const [statuses, setStatuses] = useState([]);
  const [noteBookEntries, setNoteBookEntries] = useState([]);
  const [selectedNoteBook, setSelectedNoteBook] = useState(null);
  const [isParentTemplate, setIsParentTemplate] = useState(false);
  const [types, setTypes] = useState([]);
  const [filters, setFilters] = useState({
    statuses: [],
    types: [],
    tags: "",
    fromdate: "",
    todate: "",
    notebookid: null,
    orphanOnly: false,
  });
  const [createInstanceModalOpen, setCreateInstanceModalOpen] = useState(false);

  const [counts, setCounts] = useState({
    total: 0,
    drafts: 0,
    pending: 0,
    finalized: 0,
  });
  const [loading, setLoading] = useState(true);
  const intl = useIntl();

  const setStatusList = (statusList) => {
    if (componentMounted.current) {
      setStatuses(statusList);
    }
  };

  const statusColors = {
    DRAFT: "gray",
    SUBMITTED: "cyan",
    FINALIZED: "green",
    LOCKED: "purple",
    ARCHIVED: "gray",
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case "DRAFT":
        return <EditIcon size={15} />;
      case "SUBMITTED":
        return <InProgress size={15} />;
      case "FINALIZED":
        return <Checkmark size={15} />;
      case "LOCKED":
        return <Locked size={15} />;
      case "ARCHIVED":
        return <Archive size={15} />;
      default:
        return <Document size={15} />;
    }
  };

  const handleDatePickerChangeDate = (datePicker, date) => {
    switch (datePicker) {
      case "startDate":
        setFilters({ ...filters, fromdate: date });
        break;
      case "endDate":
        setFilters({ ...filters, todate: date });
        break;
      default:
    }
  };

  const loadNoteBookEntries = (entries) => {
    if (componentMounted.current) {
      if (Array.isArray(entries) && entries.length > 0) {
        setNoteBookEntries(entries);
      } else {
        setNoteBookEntries([]);
      }
      setLoading(false);
    }
  };

  const filtersToParameters = () => {
    let params =
      "statuses=" +
      filters.statuses.map((entry) => entry.id).join(",") +
      "&types=" +
      filters.types.map((entry) => entry.id).join(",") +
      "&fromDate=" +
      filters.fromdate +
      "&toDate=" +
      filters.todate +
      "&tags=" +
      filters.tags;

    if (selectedNoteBook) {
      params += "&noteBookId=" + filters.notebookid;
    }
    if (filters.orphanOnly) {
      params += "&orphanOnly=true";
    }
    return params;
  };

  const refreshItems = () => {
    getFromOpenElisServer(
      "/rest/notebook/dashboard/entries?" + filtersToParameters(),
      loadNoteBookEntries,
    );
  };

  const openNoteBookInstanceEntryForm = () => {
    window.location.href = "/NoteBookInstanceEntryForm/" + selectedNoteBook.id;
  };

  const openNoteBookInstanceView = (id) => {
    window.location.href = "/NoteBookInstanceEditForm/" + id + "?mode=view";
  };

  const openNoteBookInstanceEdit = (id) => {
    window.location.href = "/NoteBookInstanceEditForm/" + id + "?mode=edit";
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/displayList/NOTEBOOK_STATUS", setStatusList);
    getFromOpenElisServer("/rest/displayList/NOTEBOOK_EXPT_TYPE", setTypes);
    getFromOpenElisServer("/rest/notebook/dashboard/metrics", loadCounts);

    return () => {
      componentMounted.current = false;
    };
  }, []);

  const loadCounts = (data) => {
    setCounts(data);
  };

  const tileList = [
    {
      title: intl.formatMessage({ id: "notebook.label.total" }),
      count: counts.total,
    },
    {
      title: intl.formatMessage({ id: "notebook.label.drafts" }),
      count: counts.drafts,
    },
    {
      title: intl.formatMessage({ id: "notebook.label.pending" }),
      count: counts.pending,
    },
    {
      title: intl.formatMessage({ id: "notebook.label.finalized" }),
      count: counts.finalized,
    },
  ];

  useEffect(() => {
    componentMounted.current = true;
    refreshItems();
    return () => {
      componentMounted.current = false;
    };
  }, [filters]);

  useEffect(() => {
    componentMounted.current = true;
    if (selectedNoteBook) {
      setFilters({ ...filters, notebookid: selectedNoteBook.id });
    }
    return () => {
      componentMounted.current = false;
    };
  }, [selectedNoteBook]);

  let breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "label.button.newEntry", link: "/NoteBookEntryForm" },
  ];

  // Handler for tree view selection
  const handleTreeSelect = (notebookId, isParent, notebookData) => {
    setSelectedNoteBook(notebookData);
    setIsParentTemplate(isParent);
    if (notebookData) {
      setFilters({
        ...filters,
        notebookid: notebookId,
        orphanOnly: notebookData.showOrphanEntries === true,
      });
    }
  };

  // Store the refresh function from NotebookTreeView
  const refreshTreeRef = useRef(null);

  const handleTreeRefresh = (refreshFn) => {
    refreshTreeRef.current = refreshFn;
  };

  // Handler for successful instance creation
  const handleInstanceCreated = (newInstance) => {
    // Refresh the tree view
    if (refreshTreeRef.current) {
      refreshTreeRef.current();
    }
    // Navigate to the new instance
    if (newInstance && newInstance.id) {
      handleTreeSelect(newInstance.id, false, {
        id: newInstance.id,
        title: newInstance.title,
        isChildInstance: true,
        parentNotebookId: newInstance.parentNotebookId,
      });
    }
  };

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && (
        <Loading
          description={intl.formatMessage({ id: "loading.description" })}
        />
      )}

      <PageBreadCrumb breadcrumbs={breadcrumbs} />

      <Grid fullWidth={true}>
        <Column lg={16}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="notebook.page.title" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <Grid fullWidth={true}>
        <Column lg={3} md={8} sm={4}>
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <br />
            </Column>
            <Column lg={16} md={8} sm={4}>
              <Button
                style={{ width: "70%" }}
                size="sm"
                onClick={() => {
                  setFilters({
                    statuses: [],
                    types: [],
                    tags: "",
                    fromdate: "",
                    todate: "",
                    notebookid: null,
                  });
                  setSelectedNoteBook(null);
                }}
              >
                <List />
                <FormattedMessage id="notebook.heading.allEntries" />
              </Button>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <br />
            </Column>
          </Grid>
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <h4>
                <FormattedMessage id="notebook.heading.notebooks" />
              </h4>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <NotebookTreeView
                onSelectNotebook={handleTreeSelect}
                selectedId={selectedNoteBook?.id}
                onRefresh={handleTreeRefresh}
              />
            </Column>
          </Grid>
        </Column>
        <Column lg={13}>
          <div className="dashboard-container">
            {tileList.map((tile, index) => (
              <Tile key={index} className="dashboard-tile">
                <h3 className="tile-title">{tile.title}</h3>
                <p className="tile-value">{tile.count}</p>
              </Tile>
            ))}
          </div>
          <div className="orderLegendBody">
            <Grid fullWidth={true}>
              {selectedNoteBook ? (
                <>
                  <Column lg={16} md={8} sm={4}>
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "flex-start",
                      }}
                    >
                      <div>
                        <h4> {selectedNoteBook.title} </h4>
                        {selectedNoteBook.parentNotebookTitle && (
                          <Breadcrumb noTrailingSlash>
                            <BreadcrumbItem
                              href="#"
                              onClick={(e) => {
                                e.preventDefault();
                                // Navigate to parent
                                handleTreeSelect(
                                  selectedNoteBook.parentNotebookId,
                                  true,
                                  {
                                    id: selectedNoteBook.parentNotebookId,
                                    title: selectedNoteBook.parentNotebookTitle,
                                  },
                                );
                              }}
                            >
                              {selectedNoteBook.parentNotebookTitle}
                            </BreadcrumbItem>
                            <BreadcrumbItem isCurrentPage>
                              {selectedNoteBook.title}
                            </BreadcrumbItem>
                          </Breadcrumb>
                        )}
                      </div>
                      <div>
                        {isParentTemplate ? (
                          <Button
                            size="sm"
                            onClick={() => setCreateInstanceModalOpen(true)}
                          >
                            <FormattedMessage id="notebook.button.createInstance" />
                          </Button>
                        ) : (
                          <Button
                            size="sm"
                            disabled={
                              // Check if user has any of the notebook's specific allowedRoles
                              // If no allowedRoles defined, anyone can access (empty = no restriction)
                              (() => {
                                const roles = selectedNoteBook?.allowedRoles
                                  ? Array.from(selectedNoteBook.allowedRoles)
                                  : [];
                                // No roles = no restriction = enabled
                                if (roles.length === 0) return false;
                                return !hasRoleForCurrentLabUnit(roles);
                              })()
                            }
                            title={(() => {
                              const roles = selectedNoteBook?.allowedRoles
                                ? Array.from(selectedNoteBook.allowedRoles)
                                : [];
                              if (roles.length === 0) return undefined;
                              return !hasRoleForCurrentLabUnit(roles)
                                ? intl.formatMessage({
                                    id: "notebook.permission.entry.edit.required",
                                    defaultMessage:
                                      "You need permission to create or edit notebook entries",
                                  })
                                : undefined;
                            })()}
                            onClick={() => {
                              openNoteBookInstanceEntryForm();
                            }}
                          >
                            <FormattedMessage id="label.button.newEntry" />
                          </Button>
                        )}
                      </div>
                    </div>
                    {isParentTemplate && (
                      <div className="parent-template-banner">
                        <p>
                          <FormattedMessage
                            id="notebook.parentTemplate.banner"
                            defaultMessage="This is a project template. Create a new project or use an existing one to add entries."
                          />
                        </p>
                      </div>
                    )}
                  </Column>
                </>
              ) : (
                <Column lg={16} md={8} sm={4}>
                  <>
                    {" "}
                    <h4>
                      <FormattedMessage id="notebook.heading.allEntries" />
                    </h4>
                  </>
                </Column>
              )}

              <Column lg={16} md={8} sm={4}>
                <br />
              </Column>
            </Grid>

            <Grid fullWidth={true} className="gridBoundary">
              <Column lg={16} md={8} sm={4}>
                <FormattedMessage id="filters.label" /> :
              </Column>
              <Column lg={2} md={16} sm={16}>
                <FilterableMultiSelect
                  id="statuses"
                  titleText={intl.formatMessage({ id: "label.filters.status" })}
                  items={statuses}
                  itemToString={(item) => (item ? item.value : "")}
                  initialSelectedItems={filters.statuses}
                  onChange={(changes) => {
                    setFilters({ ...filters, statuses: changes.selectedItems });
                  }}
                  selectionFeedback="top-after-reopen"
                />
              </Column>
              <Column lg={3} md={8} sm={8}>
                <FilterableMultiSelect
                  id="types"
                  titleText={intl.formatMessage({
                    id: "notebook.label.filter.types",
                  })}
                  items={types}
                  initialSelectedItems={filters.types}
                  itemToString={(item) => (item ? item.value : "")}
                  onChange={(changes) => {
                    setFilters({ ...filters, types: changes.selectedItems });
                  }}
                  selectionFeedback="top-after-reopen"
                />
              </Column>
              <Column lg={2} md={8} sm={8}>
                <TextInput
                  id="title"
                  name="title"
                  labelText={intl.formatMessage({
                    id: "notebook.tags.modal.add.label",
                  })}
                  placeholder={intl.formatMessage({
                    id: "notebook.tag.placeholder",
                  })}
                  value={filters.tags}
                  onChange={(e) => {
                    setFilters({ ...filters, tags: e.target.value });
                  }}
                  required
                />
              </Column>
              <Column lg={3} md={8} sm={8}>
                <CustomDatePicker
                  key="startDate"
                  id={"startDate"}
                  labelText={intl.formatMessage({
                    id: "eorder.date.start",
                    defaultMessage: "Start Date",
                  })}
                  // disallowFutureDate={true}
                  autofillDate={true}
                  value={filters.statuses}
                  onChange={(date) =>
                    handleDatePickerChangeDate("startDate", date)
                  }
                />
              </Column>
              <Column lg={3} md={8} sm={8}>
                <CustomDatePicker
                  key="endDate"
                  id={"endDate"}
                  labelText={intl.formatMessage({
                    id: "eorder.date.end",
                    defaultMessage: "End Date",
                  })}
                  //disallowFutureDate={true}
                  autofillDate={true}
                  value={filters.todate}
                  onChange={(date) =>
                    handleDatePickerChangeDate("endDate", date)
                  }
                />
              </Column>

              <Column lg={16} md={8} sm={4}></Column>
            </Grid>
            <Grid>
              <Column lg={16} md={8} sm={4}>
                <div className="notebook-dashboard-container">
                  {noteBookEntries.map((entry, index) => (
                    <Tile key={index} className="notebook-dashboard-tile">
                      <div className="notebook-tile-content">
                        <Grid>
                          <Column lg={16} md={8} sm={4}>
                            <h3 className="notebook-tile-title">
                              {entry.notebookName && entry.entryNumber
                                ? `${entry.notebookName} - ${intl.formatMessage(
                                    { id: "notebook.entry.number" },
                                    { number: entry.entryNumber },
                                  )}`
                                : entry.title}
                            </h3>
                            <hr></hr>
                          </Column>
                          <Column lg={2} md={8} sm={4}>
                            {getStatusIcon(entry.status)}
                          </Column>
                          <Column lg={14} md={8} sm={4}>
                            <Tag
                              style={{
                                fontWeight: "bold",
                              }}
                              size="sm"
                              type={statusColors[entry.status]}
                            >
                              {entry.status}
                            </Tag>
                          </Column>
                          <Column lg={2} md={8} sm={4}>
                            <Document size={15} />
                          </Column>
                          <Column lg={14} md={8} sm={4}>
                            <div className="notebook-tile-subtitle">
                              {entry.typeName}
                            </div>
                          </Column>
                          <Column lg={2} md={8} sm={4}>
                            <Time size={15} />
                          </Column>
                          <Column lg={14} md={8} sm={4}>
                            <div className="notebook-tile-subtitle">
                              {entry.dateCreated}
                            </div>
                          </Column>
                          <Column lg={2} md={8} sm={4}>
                            <TagIcon size={15} />
                          </Column>
                          <Column lg={14} md={8} sm={4}>
                            {entry.tags.map((tag) => (
                              <Tag
                                key={tag}
                                style={{
                                  fontSize: "0.6rem",
                                }}
                              >
                                {tag}
                              </Tag>
                            ))}
                          </Column>
                        </Grid>
                      </div>
                      <div className="notebook-tile-buttons">
                        <Grid>
                          <Column lg={8} md={8} sm={4}>
                            <Button
                              kind="secondary"
                              size="sm"
                              disabled={(() => {
                                // Check if user has any of the entry's allowedRoles (from template)
                                const entryRoles = entry.allowedRoles
                                  ? Array.isArray(entry.allowedRoles)
                                    ? entry.allowedRoles
                                    : Array.from(entry.allowedRoles)
                                  : [];
                                // User has required role = enabled
                                if (
                                  entryRoles.length === 0 ||
                                  hasRoleForCurrentLabUnit(entryRoles)
                                ) {
                                  return false;
                                }
                                // User is the assigned technician = enabled
                                if (
                                  entry.technicianId != null &&
                                  userSessionDetails.userId ==
                                    entry.technicianId
                                ) {
                                  return false;
                                }
                                // Otherwise disabled
                                return true;
                              })()}
                              onClick={() => openNoteBookInstanceView(entry.id)}
                            >
                              <View size={13} />
                              <FormattedMessage id="notebook.button.view" />
                            </Button>
                          </Column>
                          <Column lg={8} md={8} sm={4}>
                            {entry.status != "ARCHIVED" && (
                              <Button
                                kind="primary"
                                size="sm"
                                disabled={(() => {
                                  // Check if user has any of the entry's allowedRoles (from template)
                                  const entryRoles = entry.allowedRoles
                                    ? Array.isArray(entry.allowedRoles)
                                      ? entry.allowedRoles
                                      : Array.from(entry.allowedRoles)
                                    : [];
                                  // User has required role = enabled
                                  if (
                                    entryRoles.length === 0 ||
                                    hasRoleForCurrentLabUnit(entryRoles)
                                  ) {
                                    return false;
                                  }
                                  // User is the assigned technician = enabled
                                  if (
                                    entry.technicianId != null &&
                                    userSessionDetails.userId ==
                                      entry.technicianId
                                  ) {
                                    return false;
                                  }
                                  // Otherwise disabled
                                  return true;
                                })()}
                                title={(() => {
                                  const entryRoles = entry.allowedRoles
                                    ? Array.isArray(entry.allowedRoles)
                                      ? entry.allowedRoles
                                      : Array.from(entry.allowedRoles)
                                    : [];
                                  if (
                                    entryRoles.length === 0 ||
                                    hasRoleForCurrentLabUnit(entryRoles)
                                  ) {
                                    return undefined;
                                  }
                                  if (
                                    entry.technicianId != null &&
                                    userSessionDetails.userId ==
                                      entry.technicianId
                                  ) {
                                    return undefined;
                                  }
                                  return intl.formatMessage({
                                    id: "notebook.permission.entry.edit.required",
                                    defaultMessage:
                                      "You need permission to create or edit notebook entries",
                                  });
                                })()}
                                onClick={() =>
                                  openNoteBookInstanceEdit(entry.id)
                                }
                              >
                                <Edit size={13} />
                                <FormattedMessage id="notebook.button.edit" />
                              </Button>
                            )}
                          </Column>
                        </Grid>
                      </div>
                    </Tile>
                  ))}
                </div>
              </Column>
            </Grid>
          </div>
        </Column>
      </Grid>

      <CreateInstanceModal
        open={createInstanceModalOpen}
        onClose={() => setCreateInstanceModalOpen(false)}
        parentNotebook={selectedNoteBook}
        onSuccess={handleInstanceCreated}
      />
    </>
  );
}

export default NoteBookDashBoard;
