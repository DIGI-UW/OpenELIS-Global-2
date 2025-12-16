import React, { useContext, useState, useEffect, useRef } from "react";
import { useParams } from "react-router-dom";
import PageBreadCrumb from "../common/PageBreadCrumb";
import {
  Button,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  MultiSelect,
  FileUploader,
  FilterableMultiSelect,
  Grid,
  Column,
  InlineLoading,
  Section,
  Heading,
  Tile,
  Modal,
  InlineNotification,
  FileUploaderDropContainer,
  FileUploaderItem,
  Loading,
  Tag,
  Accordion,
  AccordionItem,
  ContentSwitcher,
  Switch,
  DataTable,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Pagination,
} from "@carbon/react";
import {
  Launch,
  Subtract,
  ArrowLeft,
  ArrowRight,
  Checkmark,
  Add,
} from "@carbon/react/icons";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { FormattedMessage, useIntl } from "react-intl";
import {
  NoteBookFormValues,
  NoteBookInitialData,
} from "../formModel/innitialValues/NoteBookFormValues";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  postToOpenElisServer,
  hasRole,
  toBase64,
} from "../utils/Utils";
import NotebookWorkflowTab from "./workflow/NotebookWorkflowTab";
import MNTDWorkflowTab from "./workflow/MNTDWorkflowTab";
import TBWorkflowTab from "./workflow/TBWorkflowTab";
import PharmaceuticalWorkflowTab from "./workflow/PharmaceuticalWorkflowTab";

const NoteBookInstanceEntryForm = () => {
  let breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "notebook.label.dashboard", link: "/NoteBookDashboard" },
  ];

  const MODES = Object.freeze({
    CREATE: "CREATE",
    EDIT: "EDIT",
    VIEW: "VIEW",
  });

  const TABS = Object.freeze({
    CONTENT: 0,
    ATTACHMENTS: 1,
    WORKFLOW: 2,
    COMMENTS: 3,
    AUDIT_TRAIL: 4,
  });
  const intl = useIntl();
  const componentMounted = useRef(false);
  const [mode, setMode] = useState(MODES.CREATE);
  const { notebookid } = useParams();
  const { notebookentryid } = useParams();

  // Get mode from query parameter
  const urlParams = new URLSearchParams(window.location.search);
  const viewModeParam = urlParams.get("mode"); // 'view' or 'edit'
  const isViewMode = mode === MODES.VIEW; // Helper for read-only checks

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const [statuses, setStatuses] = useState([]);
  const [types, setTypes] = useState([]);
  const [technicianUsers, setTechnicianUsers] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loading, setLoading] = useState(false);
  const [noteBookData, setNoteBookData] = useState(NoteBookInitialData);
  const [noteBookForm, setNoteBookForm] = useState(NoteBookFormValues);
  const [analyzerList, setAnalyzerList] = useState([]);
  const [uploadedFiles, setUploadedFiles] = useState([]);
  const [initialMount, setInitialMount] = useState(false);
  const [allTests, setAllTests] = useState([]);
  const [allPanels, setAllPanels] = useState([]);
  const [sampleTypes, setSampleTypes] = useState([]);
  const [errors, setErrors] = useState([]);
  const [selectedTab, setSelectedTab] = useState(TABS.CONTENT);
  const [comments, setComments] = useState([]);
  const [newComment, setNewComment] = useState("");
  const [auditTrailItems, setAuditTrailItems] = useState([]);
  const [auditTrailLoading, setAuditTrailLoading] = useState(false);
  const [auditTrailPage, setAuditTrailPage] = useState(1);
  const [auditTrailPageSize, setAuditTrailPageSize] = useState(10);
  const [questionnaires, setQuestionnaires] = useState([]);
  const [projectTags, setProjectTags] = useState([]); // Template tags (for display only)
  const [projectFiles, setProjectFiles] = useState([]); // Template files (for display only)

  const handleSubmit = () => {
    if (isSubmitting) {
      return;
    }
    setIsSubmitting(true);
    noteBookForm.id = noteBookData.id;
    noteBookForm.isTemplate = false;
    noteBookForm.templateId = notebookid;
    noteBookForm.title = noteBookData.title;
    noteBookForm.type = noteBookData.type;
    noteBookForm.objective = noteBookData.objective;
    noteBookForm.protocol = noteBookData.protocol;
    noteBookForm.content = noteBookData.content;
    noteBookForm.status = noteBookData.status;
    noteBookForm.technicianId = noteBookData.technicianId;
    noteBookForm.sampleIds = noteBookData.samples.map((entry) =>
      Number(entry.id),
    );
    noteBookForm.pages = noteBookData.pages;
    noteBookForm.files = noteBookData.files;
    noteBookForm.analyzerIds = noteBookData.analyzers.map((entry) =>
      Number(entry.id),
    );
    noteBookForm.tags = noteBookData.tags;
    // Send only new comments (those without id) with just text
    noteBookForm.comments = comments
      .filter((c) => c.id === null)
      .map((c) => ({ id: null, text: c.text }));
    console.log(JSON.stringify(noteBookForm));
    var url =
      mode === MODES.EDIT
        ? "/rest/notebook/update/" + notebookentryid
        : "/rest/notebook/create";
    postToOpenElisServerFullResponse(
      url,
      JSON.stringify(noteBookForm),
      handleSubmited,
    );
  };

  const handleSubmited = async (response) => {
    var body = await response.json();
    console.log(body);
    var status = response.status;
    setIsSubmitting(false);
    setNotificationVisible(true);
    if (status == "200") {
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "save.success" }),
      });
      // Reload data to get comments with proper id and author from backend
      getFromOpenElisServer("/rest/notebook/view/" + body.id, loadInitialData);
      // Reload audit trail after save
      loadAuditTrail(body.id);
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "error.save.msg" }),
      });
    }
    window.location.href = "/NoteBookInstanceEditForm/" + body.id;
  };

  const showAlertMessage = (msg, kind) => {
    setNotificationVisible(true);
    addNotification({
      kind: kind,
      title: intl.formatMessage({ id: "notification.title" }),
      message: msg,
    });
  };

  const [showTagModal, setShowTagModal] = useState(false);
  const [newTag, setNewTag] = useState("");
  const [tagError, setTagError] = useState("");

  const openTagModal = () => {
    setNewTag("");
    setTagError("");
    setShowTagModal(true);
  };

  const closeTagModal = () => setShowTagModal(false);

  const handleTagChange = (e) => {
    const { name, value } = e.target;
    setNewTag(value);
  };

  const handleAddTag = () => {
    if (!newTag.trim()) {
      setTagError(
        intl.formatMessage({ id: "notebook.tags.modal.add.errorRequired" }),
      );
      return;
    }
    setNoteBookData((prev) => ({
      ...prev,
      tags: [...prev.tags, newTag],
    }));
    setShowTagModal(false);
  };

  // Mark page as complete
  const handleMarkPageComplete = (index) => {
    setNoteBookData((prev) => {
      const updatedPages = [...prev.pages];
      updatedPages[index] = { ...updatedPages[index], completed: true };
      return {
        ...prev,
        pages: updatedPages,
      };
    });
  };

  const handleRemoveTag = (index) => {
    setNoteBookData((prev) => ({
      ...prev,
      tags: prev.tags.filter((_, i) => i !== index),
    }));
  };

  const handleAddFiles = async (event) => {
    const newFiles = Array.from(event.target.files);

    // convert files to base64
    const fileForms = await Promise.all(
      newFiles.map(async (file) => {
        const base64 = await toBase64(file);
        return {
          base64File: base64,
          fileType: file.type,
          fileName: file.name,
        };
      }),
    );

    setNoteBookData((prev) => ({
      ...prev,
      files: [...prev.files, ...fileForms],
    }));

    // update UI list (and mark them as complete)
    setUploadedFiles((prev) => [
      ...prev,
      ...newFiles.map((f) => ({ file: f, status: "complete" })),
    ]);
  };

  const handleRemoveFile = (index) => {
    setNoteBookData((prev) => ({
      ...prev,
      files: prev.files.filter((_, i) => i !== index),
    }));
    setUploadedFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const handleAddComment = () => {
    if (!newComment.trim()) {
      return;
    }
    // Add comment to local state for immediate UI update
    // The backend will assign proper id and author
    const comment = {
      id: null, // Will be set by backend
      text: newComment,
      author: null, // Will be set by backend
      dateCreated: null, // Will be set by backend
    };
    setComments((prev) => [...prev, comment]);
    setNewComment("");
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/displayList/NOTEBOOK_STATUS", setStatuses);
    getFromOpenElisServer("/rest/displayList/NOTEBOOK_EXPT_TYPE", setTypes);
    getFromOpenElisServer("/rest/displayList/ANALYZER_LIST", setAnalyzerList);
    getFromOpenElisServer("/rest/displayList/ALL_TESTS", setAllTests);
    getFromOpenElisServer("/rest/users", setTechnicianUsers);
    getFromOpenElisServer("/rest/panels", setAllPanels);
    getFromOpenElisServer("/rest/user-sample-types", setSampleTypes);
    getFromOpenElisServer("/rest/notebook/questionnaires", setQuestionnaires);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    if (!notebookentryid) {
      setMode(MODES.CREATE);
    } else {
      // Set mode based on query parameter
      if (viewModeParam === "view") {
        setMode(MODES.VIEW);
      } else {
        setMode(MODES.EDIT);
      }
      setLoading(true);
      getFromOpenElisServer(
        "/rest/notebook/view/" + notebookentryid,
        loadInitialData,
      );
    }
  }, [notebookentryid, viewModeParam]);

  useEffect(() => {
    if (notebookid) {
      setLoading(true);
      getFromOpenElisServer(
        "/rest/notebook/view/" + notebookid,
        loadInitialProjectData,
      );
    }
  }, []);

  // Check if user is authorized to access this notebook
  const checkAuthorization = (technicianId) => {
    if (technicianId && userSessionDetails.userId != technicianId) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "notebook.error.notAssigned",
          defaultMessage: "You are not assigned to this project",
        }),
      });
      setNotificationVisible(true);
      // Redirect back to dashboard
      setTimeout(() => {
        window.location.href = "/NoteBookDashboard";
      }, 100);
      return false;
    }
    return true;
  };

  const loadInitialProjectData = (data) => {
    if (componentMounted.current) {
      if (data && data.id) {
        // Check authorization
        if (!checkAuthorization(data.technicianId)) {
          setLoading(false);
          return;
        }

        // Store project (template) tags and files separately for display only
        setProjectTags(data.tags || []);
        setProjectFiles(data.files || []);

        // Create new instance data without template tags and files
        const instanceData = {
          ...data,
          id: null,
          isTemplate: false,
          dateCreated: null,
          status: "DRAFT",
          tags: [], // Instance starts with no tags
          files: [], // Instance starts with no files
          samples: [], // Instance starts with no samples
          comments: [], // Instance starts with no comments
          creatorName:
            userSessionDetails.firstName + " " + userSessionDetails.lastName,
        };
        setNoteBookData(instanceData);
        setLoading(false);
      }
    }
  };

  const loadInitialData = (data) => {
    if (componentMounted.current) {
      if (data && data.id) {
        // Check authorization
        if (!checkAuthorization(data.technicianId)) {
          setLoading(false);
          return;
        }

        // If this is an instance (isTemplate=false) and we have templateId from backend,
        // fetch the latest parent template properties to ensure we always display the most up-to-date template data
        if (data.isTemplate === false && data.templateId) {
          getFromOpenElisServer(
            "/rest/notebook/view/" + data.templateId,
            (templateData) => {
              // Merge pages: Keep existing instance pages, add new template pages that don't exist
              const instancePages = data.pages || [];
              const templatePages = templateData.pages || [];

              // Create a set of existing page identifiers (id and title)
              const existingPageIds = new Set(
                instancePages.map((p) => p.id).filter((id) => id != null),
              );
              const existingPageTitles = new Set(
                instancePages
                  .map((p) => p.title?.trim().toLowerCase())
                  .filter((t) => t),
              );

              // Add new pages from template that don't exist in instance
              const newPagesFromTemplate = templatePages.filter(
                (templatePage) => {
                  const pageId = templatePage.id;
                  const pageTitle = templatePage.title?.trim().toLowerCase();
                  // Add page if neither ID nor title matches existing pages
                  return (
                    !existingPageIds.has(pageId) &&
                    !existingPageTitles.has(pageTitle)
                  );
                },
              );

              const mergedPages = [...instancePages, ...newPagesFromTemplate];

              // Merge template properties with instance-specific data
              // Store project (template) tags and files separately for display
              setProjectTags(templateData.tags || []);
              setProjectFiles(templateData.files || []);

              const mergedData = {
                ...data,
                // Override with latest template properties (for display)
                title: templateData.title,
                type: templateData.type,
                objective: templateData.objective,
                protocol: templateData.protocol,
                content: templateData.content,
                questionnaireFhirUuid: templateData.questionnaireFhirUuid,
                technicianId: templateData.technicianId,
                technicianName: templateData.technicianName,
                // Keep instance-specific properties
                id: data.id,
                status: data.status,
                creatorName: data.creatorName,
                dateCreated: data.dateCreated,
                samples: data.samples,
                files: data.files || [], // Instance-specific files only
                comments: data.comments,
                tags: data.tags || [], // Instance-specific tags only
                isTemplate: data.isTemplate,
                templateId: data.templateId,
                pages: mergedPages, // Merged pages (existing + new from template)
                analyzers: data.analyzers,
              };
              setNoteBookData(mergedData);
            },
          );
        } else {
          setNoteBookData(data);
        }

        // Load comments from backend (with proper id and author)
        if (data.comments && Array.isArray(data.comments)) {
          setComments(
            data.comments.map((c) => ({
              id: c.id,
              text: c.text,
              author: c.author
                ? c.author.displayName || c.author.name
                : "Unknown",
              dateCreated: c.dateCreated,
            })),
          );
        }
        // Load audit trail
        loadAuditTrail(data.id);
        setLoading(false);
        setInitialMount(true);
      }
    }
  };

  const loadAuditTrail = (notebookId) => {
    if (!notebookId) {
      return;
    }
    setAuditTrailLoading(true);
    getFromOpenElisServer(
      "/rest/notebook/auditTrail?notebookId=" + notebookId,
      (data) => {
        if (data && data.log && Array.isArray(data.log)) {
          const updatedAuditTrailItems = data.log.map((item, index) => {
            // Format time from timestamp as "DD/MM/YYYY HH:MM"
            let formattedTime = "-";
            if (item.timeStamp) {
              const date = new Date(item.timeStamp);
              formattedTime = date.toLocaleString("en-GB", {
                day: "2-digit",
                month: "2-digit",
                year: "numeric",
                hour: "2-digit",
                minute: "2-digit",
                hour12: false,
              });
            }
            return { ...item, id: index + 1, time: formattedTime };
          });
          setAuditTrailItems(updatedAuditTrailItems);
        } else {
          setAuditTrailItems([]);
        }
        setAuditTrailLoading(false);
      },
      () => {
        setAuditTrailItems([]);
        setAuditTrailLoading(false);
      },
    );
  };

  const handleAuditTrailPageChange = (pageInfo) => {
    setAuditTrailPage(pageInfo.page);
    setAuditTrailPageSize(pageInfo.pageSize);
  };

  const statusColors = {
    DRAFT: "gray",
    SUBMITTED: "cyan",
    FINALIZED: "green",
    LOCKED: "purple",
    ARCHIVED: "gray",
    NEW: "gray",
  };

  const getExperimentTypeName = () => {
    if (!noteBookData.type) return "";
    const typeObj = types.find((t) => t.id == noteBookData.type);
    return typeObj ? typeObj.value : "";
  };

  const getStatusColor = (status) => {
    return statusColors[status] || "gray";
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="notebook.project.entry.form.title" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && <Loading></Loading>}
      <Grid fullWidth={true} className="orderLegendBody">
        {/* Status & Metadata Section */}
        <Column lg={16} md={8} sm={4}>
          <Section>
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                marginBottom: "1rem",
              }}
            >
              <Heading style={{ margin: 0 }}>
                <FormattedMessage id="notebook.status.metadata.title" />
              </Heading>
              {noteBookData.status && (
                <Tag type={getStatusColor(noteBookData.status)} size="sm">
                  {statuses.find((s) => s.id === noteBookData.status)?.value ||
                    noteBookData.status}
                </Tag>
              )}
            </div>
            <Grid fullWidth={true} className="gridBoundary">
              {noteBookData.title && (
                <>
                  <Column lg={8} md={8} sm={4}>
                    <p style={{ margin: 0 }}>
                      <strong>
                        {intl.formatMessage({
                          id: "notebook.label.project.title",
                        })}
                        :{" "}
                      </strong>
                      {noteBookData.title}
                    </p>
                  </Column>
                  <Column lg={16} md={8} sm={4}>
                    <br />
                  </Column>
                </>
              )}
              <Column lg={8} md={8} sm={4}>
                <p style={{ margin: 0 }}>
                  <strong>
                    {intl.formatMessage({
                      id: "notebook.label.experimentType",
                    })}
                    :{" "}
                  </strong>
                  {getExperimentTypeName() ||
                    intl.formatMessage({ id: "not.available" })}
                </p>
              </Column>
              <Column lg={16} md={8} sm={4}>
                <br />
              </Column>
              {noteBookData.protocol && (
                <>
                  <Column lg={8} md={8} sm={4}>
                    <p style={{ margin: 0 }}>
                      <strong>
                        {intl.formatMessage({
                          id: "notebook.label.protocol",
                        })}
                        :{" "}
                      </strong>
                      {noteBookData.protocol ||
                        intl.formatMessage({ id: "not.available" })}
                    </p>
                  </Column>
                  <Column lg={16} md={8} sm={4}>
                    <br />
                  </Column>
                </>
              )}
              {noteBookData.questionnaireFhirUuid && (
                <>
                  <Column lg={8} md={8} sm={4}>
                    <p style={{ margin: 0 }}>
                      <strong>
                        {intl.formatMessage({
                          id: "notebook.label.questionnaire",
                        })}
                        :{" "}
                      </strong>
                      {(() => {
                        const questionnaire = questionnaires.find(
                          (q) => q.id === noteBookData.questionnaireFhirUuid,
                        );
                        return questionnaire
                          ? questionnaire.value
                          : noteBookData.questionnaireFhirUuid;
                      })()}
                    </p>
                  </Column>
                  <Column lg={16} md={8} sm={4}>
                    <br />
                  </Column>
                </>
              )}
              <Column lg={16} md={8} sm={4}>
                <br />
              </Column>
              <Column lg={16} md={8} sm={4}>
                <p style={{ margin: 0 }}>
                  <strong>
                    <FormattedMessage id="notebook.label.projectTags" />:{" "}
                  </strong>
                  {projectTags && projectTags.length > 0 ? (
                    <span>
                      {projectTags.map((tag, index) => (
                        <Tag
                          key={index}
                          type="blue"
                          size="sm"
                          style={{
                            marginRight: "0.5rem",
                            marginBottom: "0.5rem",
                          }}
                        >
                          {tag}
                        </Tag>
                      ))}
                    </span>
                  ) : (
                    <span style={{ color: "#525252" }}>
                      {intl.formatMessage({ id: "not.available" })}
                    </span>
                  )}
                </p>
              </Column>
              <Column lg={16} md={8} sm={4}>
                <br />
              </Column>
              {/* Right side: Date Created, Author */}
              <Column lg={16} md={8} sm={4}>
                <Grid fullWidth={true}>
                  <Column lg={12} md={8} sm={4}></Column>
                  <Column lg={4} md={8} sm={4} style={{ textAlign: "right" }}>
                    <div
                      style={{
                        display: "flex",
                        flexDirection: "column",
                        gap: "0.5rem",
                        alignItems: "flex-end",
                      }}
                    >
                      {noteBookData.dateCreated && (
                        <p
                          style={{
                            margin: 0,
                            fontSize: "0.875rem",
                            color: "#525252",
                          }}
                        >
                          {intl.formatMessage({ id: "date.created" })}:{" "}
                          {noteBookData.dateCreated}
                        </p>
                      )}
                      {noteBookData.creatorName && (
                        <p
                          style={{
                            margin: 0,
                            fontSize: "0.875rem",
                            color: "#525252",
                          }}
                        >
                          {intl.formatMessage({ id: "notebook.label.author" })}:{" "}
                          {noteBookData.creatorName}
                        </p>
                      )}
                    </div>
                  </Column>
                </Grid>
              </Column>
            </Grid>
          </Section>
        </Column>
        <Column lg={16} md={8} sm={4}>
          <br />
        </Column>
        <Column lg={16} md={8} sm={4}>
          <ContentSwitcher
            selectedIndex={selectedTab}
            onChange={({ index }) => setSelectedTab(index)}
          >
            <Switch text={intl.formatMessage({ id: "notebook.tab.content" })} />
            <Switch
              text={intl.formatMessage({ id: "notebook.tab.attachments" })}
            />
            <Switch
              text={intl.formatMessage({ id: "notebook.tab.workflow" })}
            />
            <Switch
              text={intl.formatMessage({ id: "notebook.tab.comments" })}
            />
            <Switch
              text={intl.formatMessage({ id: "notebook.tab.auditTrail" })}
            />
          </ContentSwitcher>
        </Column>
        <Column lg={16} md={8} sm={4}>
          <br />
        </Column>
        {selectedTab === TABS.CONTENT && (
          <Column lg={16} md={8} sm={4}>
            <Grid fullWidth={true} className="gridBoundary">
              <Column lg={16} md={8} sm={4}>
                <h5>
                  {intl.formatMessage({ id: "notebook.label.objective" })}
                </h5>
              </Column>
              <Column lg={16} md={8} sm={4}>
                <Tile style={{ padding: "1.5rem", marginBottom: "1rem" }}>
                  <p
                    style={{
                      whiteSpace: "pre-wrap",
                      margin: 0,
                      lineHeight: "1.5",
                    }}
                  >
                    {noteBookData.objective ||
                      intl.formatMessage({ id: "not.available" })}
                  </p>
                </Tile>
              </Column>
              <Column lg={16} md={8} sm={4}>
                <br />
              </Column>
              <Column lg={16} md={8} sm={4}>
                <h5>{intl.formatMessage({ id: "notebook.label.content" })}</h5>
              </Column>
              <Column lg={16} md={8} sm={4}>
                <Tile style={{ padding: "1.5rem" }}>
                  <p
                    style={{
                      whiteSpace: "pre-wrap",
                      margin: 0,
                      lineHeight: "1.5",
                    }}
                  >
                    {noteBookData.content ||
                      intl.formatMessage({ id: "not.available" })}
                  </p>
                </Tile>
              </Column>
              <Column lg={16} md={8} sm={4}>
                <br />
              </Column>
              {noteBookData.protocol && (
                <>
                  <Column lg={16} md={8} sm={4}>
                    <h5>
                      {intl.formatMessage({ id: "notebook.label.protocol" })}
                    </h5>
                  </Column>
                  <Column lg={16} md={8} sm={4}>
                    <Tile style={{ padding: "1.5rem" }}>
                      <p
                        style={{
                          whiteSpace: "pre-wrap",
                          margin: 0,
                          lineHeight: "1.5",
                        }}
                      >
                        {noteBookData.protocol ||
                          intl.formatMessage({ id: "not.available" })}
                      </p>
                    </Tile>
                  </Column>
                  <Column lg={16} md={8} sm={4}>
                    <br />
                  </Column>
                </>
              )}
              <Column lg={16} md={8} sm={4}>
                <h5>
                  <FormattedMessage id="notebook.instruments.title" />
                </h5>
              </Column>
              <Column lg={16} md={8} sm={4}>
                {(initialMount || mode === MODES.CREATE) && (
                  <FilterableMultiSelect
                    id="instruments"
                    titleText={
                      <FormattedMessage id="notebook.instruments.title" />
                    }
                    items={analyzerList}
                    itemToString={(item) => (item ? item.value : "")}
                    initialSelectedItems={noteBookData.analyzers}
                    onChange={(changes) => {
                      setNoteBookData({
                        ...noteBookData,
                        analyzers: changes.selectedItems,
                      });
                    }}
                    selectionFeedback="top-after-reopen"
                  />
                )}
              </Column>
              <Column lg={16} md={8} sm={4}>
                {noteBookData.analyzers &&
                  noteBookData.analyzers.map((item, index) => (
                    <Tag
                      key={index}
                      filter
                      onClose={() => {
                        var info = { ...noteBookData };
                        info["analyzers"].splice(index, 1);
                        setNoteBookData(info);
                      }}
                    >
                      {item.value}
                    </Tag>
                  ))}
              </Column>
              <Column lg={16} md={8} sm={4}>
                <br />
              </Column>
              <Column lg={2} md={4} sm={4}>
                <h5>
                  <FormattedMessage id="notebook.label.entryTags" />
                </h5>
              </Column>
              <Column lg={8} md={8} sm={4}>
                <Button
                  onClick={openTagModal}
                  kind="primary"
                  size="sm"
                  disabled={isViewMode}
                >
                  <Add />
                  <FormattedMessage id="notebook.tags.add" />
                </Button>
              </Column>
              <Column lg={16} md={8} sm={4}>
                <br />
              </Column>
              <Column lg={16} md={8} sm={4}>
                {noteBookData.tags && noteBookData.tags.length > 0 ? (
                  noteBookData.tags.map((tag, index) => (
                    <Tag
                      key={index}
                      filter
                      onClose={() => {
                        handleRemoveTag(index);
                      }}
                    >
                      {tag}
                    </Tag>
                  ))
                ) : (
                  <span style={{ color: "#525252" }}>
                    {intl.formatMessage({ id: "not.available" })}
                  </span>
                )}
              </Column>
            </Grid>
          </Column>
        )}
        {selectedTab === TABS.ATTACHMENTS && (
          <Column lg={16} md={8} sm={4}>
            <Grid fullWidth={true} className="gridBoundary">
              {/* Project Files (from template) */}
              {projectFiles && projectFiles.length > 0 && (
                <>
                  <Column lg={16} md={8} sm={4}>
                    <h5>
                      <FormattedMessage id="notebook.label.projectFiles" />
                    </h5>
                  </Column>
                  <Column lg={16} md={8} sm={4}>
                    <Grid style={{ marginTop: "1rem" }}>
                      {projectFiles.map((file, index) => (
                        <Column key={index} lg={8} md={8} sm={12}>
                          <Tile style={{ marginBottom: "1rem" }}>
                            <p>{file.fileName}</p>
                            <Button
                              size="sm"
                              onClick={() => {
                                var win = window.open();
                                win.document.write(
                                  '<iframe src="' +
                                    "data:" +
                                    file.fileType +
                                    ";base64," +
                                    file.fileData +
                                    '" frameborder="0" style="border:0; top:0px; left:0px; bottom:0px; right:0px; width:100%; height:100%;" allowfullscreen></iframe>',
                                );
                              }}
                            >
                              <Launch />{" "}
                              <FormattedMessage id="pathology.label.view" />
                            </Button>
                          </Tile>
                        </Column>
                      ))}
                    </Grid>
                  </Column>
                  <Column lg={16} md={8} sm={4}>
                    <br />
                  </Column>
                </>
              )}
              {/* Entry Files (instance-specific) */}
              <Column lg={16} md={8} sm={4}>
                <h5>
                  <FormattedMessage id="notebook.label.entryFiles" />
                </h5>
              </Column>
              <Column lg={16} md={8} sm={4}>
                {!isViewMode && (
                  <FileUploaderDropContainer
                    labelText={intl.formatMessage({
                      id: "notebook.attachments.uploadPrompt",
                    })}
                    multiple
                    onAddFiles={handleAddFiles}
                    accept={[".pdf", ".png", ".jpg", ".txt"]}
                  />
                )}
                {uploadedFiles.map((fileObj, index) => (
                  <FileUploaderItem
                    key={index}
                    name={fileObj.file.name}
                    status={fileObj.status}
                    onDelete={() => handleRemoveFile(index)}
                  />
                ))}
              </Column>
              <Column lg={16} md={8} sm={4}>
                {noteBookData.files && noteBookData.files.length > 0 && (
                  <Grid style={{ marginTop: "1rem" }}>
                    {noteBookData.files.map((file, index) => (
                      <Column key={index} lg={8} md={8} sm={12}>
                        <Tile style={{ marginBottom: "1rem" }}>
                          <p>{file.fileName}</p>
                          <Button
                            size="sm"
                            onClick={() => {
                              var win = window.open();
                              win.document.write(
                                '<iframe src="' +
                                  "data:" +
                                  file.fileType +
                                  ";base64," +
                                  file.fileData +
                                  '" frameborder="0" style="border:0; top:0px; left:0px; bottom:0px; right:0px; width:100%; height:100%;" allowfullscreen></iframe>',
                              );
                            }}
                          >
                            <Launch />{" "}
                            <FormattedMessage id="pathology.label.view" />
                          </Button>
                          <Button
                            kind="danger--tertiary"
                            size="sm"
                            onClick={() => handleRemoveFile(index)}
                          >
                            <FormattedMessage id="label.button.remove" />
                          </Button>
                        </Tile>
                      </Column>
                    ))}
                  </Grid>
                )}
              </Column>
            </Grid>
          </Column>
        )}
        {selectedTab === TABS.WORKFLOW && (
          <Column lg={16} md={8} sm={4}>
            {/* Use enhanced workflow view for notebook instances (non-templates) */}
            {/* Detect workflow type based on notebook title */}
            {noteBookData?.isTemplate !== true &&
              noteBookData?.id &&
              noteBookData?.title
                ?.toLowerCase()
                .includes("malaria and neglected tropical disease") && (
                <MNTDWorkflowTab notebookId={noteBookData.id} />
              )}
            {noteBookData?.isTemplate !== true &&
              noteBookData?.id &&
              noteBookData?.title?.toLowerCase().includes("pharmaceutical") && (
                <PharmaceuticalWorkflowTab notebookId={noteBookData.id} />
              )}
            {noteBookData?.isTemplate !== true &&
              noteBookData?.id &&
              noteBookData?.title
                ?.toLowerCase()
                .includes("tuberculosis") &&
              !noteBookData?.title
                ?.toLowerCase()
                .includes("malaria and neglected tropical disease") && (
                <TBWorkflowTab notebookId={noteBookData.id} />
              )}
            {noteBookData?.isTemplate !== true &&
              noteBookData?.id &&
              !noteBookData?.title
                ?.toLowerCase()
                .includes("tuberculosis") &&
              !noteBookData?.title
                ?.toLowerCase()
                .includes("malaria and neglected tropical disease") && (
                <NotebookWorkflowTab notebookId={noteBookData.id} />
              )}
            {/* Use accordion view for templates or when no ID is available */}
            {(noteBookData?.isTemplate === true || !noteBookData?.id) && (
              <Grid fullWidth={true} className="gridBoundary">
                <Column lg={16} md={8} sm={4}>
                  <h5>
                    {" "}
                    <FormattedMessage id="notebook.label.pages" />
                  </h5>
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <br></br>
                </Column>
                <Column lg={16} md={8} sm={4}>
                  {noteBookData?.pages?.length === 0 && (
                    <InlineNotification
                      kind="info"
                      title={intl.formatMessage({
                        id: "notebook.pages.none.title",
                      })}
                      subtitle={intl.formatMessage({
                        id: "notebook.pages.none.subtitle",
                      })}
                    />
                  )}
                  {noteBookData?.pages?.length > 0 && (
                    <Accordion>
                      {noteBookData.pages.map((page, index) => (
                        <AccordionItem
                          key={index}
                          style={{ marginBottom: "1rem" }}
                          title={
                            <span
                              style={{
                                display: "flex",
                                alignItems: "center",
                                gap: "0.5rem",
                              }}
                            >
                              {intl.formatMessage(
                                { id: "pagination.page" },
                                { page: page.order || index + 1 },
                              )}
                              :{" "}
                              <h5 style={{ margin: 0, display: "inline" }}>
                                {page.title}
                              </h5>
                              {page.completed && (
                                <Tag type="green" size="sm">
                                  <FormattedMessage id="notebook.page.completed" />
                                </Tag>
                              )}
                            </span>
                          }
                        >
                          <Grid>
                            <Column lg={2} md={8} sm={4}>
                              <h6>
                                {intl.formatMessage({
                                  id: "notebook.page.instructions",
                                })}
                              </h6>
                            </Column>
                            <Column lg={14} md={8} sm={4}>
                              {page.instructions}
                            </Column>
                            <Column lg={2} md={8} sm={4}>
                              <h6>
                                {intl.formatMessage({
                                  id: "notebook.page.content",
                                })}
                              </h6>
                            </Column>
                            <Column lg={14} md={8} sm={4}>
                              {page.content}
                            </Column>
                            {page.sampleTypeId && (
                              <>
                                <Column lg={2} md={8} sm={4}>
                                  <h6>
                                    {intl.formatMessage({
                                      id: "sample.type",
                                    })}
                                  </h6>
                                </Column>
                                <Column lg={14} md={8} sm={4}>
                                  <div>
                                    <span style={{ marginRight: "0.5rem" }}>
                                      {intl.formatMessage({
                                        id: "sample.type",
                                      })}
                                      :{" "}
                                    </span>
                                    {(() => {
                                      const sampleType = sampleTypes.find(
                                        (st) => st.id == page.sampleTypeId,
                                      );
                                      return sampleType ? (
                                        <Tag type="blue" size="sm">
                                          {sampleType.value}
                                        </Tag>
                                      ) : (
                                        <></>
                                      );
                                    })()}
                                  </div>
                                </Column>
                              </>
                            )}
                            {page.panels &&
                              Array.isArray(page.panels) &&
                              page.panels.length > 0 && (
                                <>
                                  <Column lg={2} md={8} sm={4}>
                                    <h6>
                                      <FormattedMessage id="sample.label.orderpanel" />
                                    </h6>
                                  </Column>
                                  <Column lg={14} md={8} sm={4}>
                                    <div>
                                      <span style={{ marginRight: "0.5rem" }}>
                                        <FormattedMessage id="sample.label.orderpanel" />
                                        :{" "}
                                      </span>
                                      {page.panels
                                        .filter((panelId) => panelId != null)
                                        .map((panelId, panelIndex) => {
                                          // Try to find panel by ID (handle both string and number)
                                          const panel = allPanels.find((p) => {
                                            if (!p || p.id == null)
                                              return false;
                                            // Normalize both to strings for comparison
                                            const pId = String(p.id).trim();
                                            const pagePanelId =
                                              String(panelId).trim();
                                            // Compare as both string and number
                                            return (
                                              pId === pagePanelId ||
                                              Number(p.id) ===
                                                Number(panelId) ||
                                              p.id == panelId
                                            );
                                          });
                                          // Only show panel if found (don't show ID fallback)
                                          return panel ? (
                                            <Tag
                                              key={panelIndex}
                                              type="green"
                                              size="sm"
                                              style={{ marginRight: "0.5rem" }}
                                            >
                                              {panel.value}
                                            </Tag>
                                          ) : null;
                                        })
                                        .filter((tag) => tag !== null)}
                                    </div>
                                  </Column>
                                </>
                              )}
                            {page.tests &&
                              Array.isArray(page.tests) &&
                              page.tests.length > 0 && (
                                <>
                                  <Column lg={2} md={8} sm={4}>
                                    <h6>
                                      {intl.formatMessage({
                                        id: "barcode.label.info.tests",
                                      })}
                                    </h6>
                                  </Column>
                                  <Column lg={14} md={8} sm={4}>
                                    <div>
                                      {page.tests.map((testId, testIndex) => {
                                        const test = allTests.find(
                                          (t) => t.id == testId,
                                        );
                                        return test ? (
                                          <Tag
                                            key={testIndex}
                                            type="blue"
                                            size="sm"
                                          >
                                            {test.value}
                                          </Tag>
                                        ) : (
                                          <></>
                                        );
                                      })}
                                    </div>
                                  </Column>
                                </>
                              )}
                            <Column lg={16} md={8} sm={4}>
                              <br />
                              {!page.completed ? (
                                <Button
                                  kind="primary"
                                  size="sm"
                                  onClick={() => handleMarkPageComplete(index)}
                                  style={{ marginRight: "0.5rem" }}
                                  hasIconOnly
                                  renderIcon={Checkmark}
                                  iconDescription={intl.formatMessage({
                                    id: "notebook.page.markComplete",
                                  })}
                                  disabled={isViewMode}
                                />
                              ) : (
                                <Tag
                                  type="green"
                                  size="sm"
                                  style={{ marginRight: "0.5rem" }}
                                >
                                  <FormattedMessage id="notebook.page.completed" />
                                </Tag>
                              )}
                            </Column>
                          </Grid>
                        </AccordionItem>
                      ))}
                    </Accordion>
                  )}
                </Column>
              </Grid>
            )}
          </Column>
        )}
        {selectedTab === TABS.COMMENTS && (
          <Column lg={16} md={8} sm={4}>
            <Grid fullWidth={true} className="gridBoundary">
              <Column lg={16} md={8} sm={4}>
                <h5>
                  <FormattedMessage id="notebook.comments.title" />
                </h5>
              </Column>
              <Column lg={16} md={8} sm={4}>
                <br />
              </Column>
              <Column lg={12} md={8} sm={4}>
                <TextArea
                  id="newComment"
                  placeholder={intl.formatMessage({
                    id: "notebook.comments.add.label",
                  })}
                  value={newComment}
                  onChange={(e) => setNewComment(e.target.value)}
                  rows={3}
                />
              </Column>
              <Column lg={4} md={8} sm={4}>
                <Button
                  onClick={handleAddComment}
                  kind="primary"
                  size="sm"
                  hasIconOnly
                  renderIcon={Add}
                  iconDescription={intl.formatMessage({
                    id: "notebook.comments.add.button",
                  })}
                  disabled={isViewMode}
                />
              </Column>
              <Column lg={16} md={8} sm={4}>
                <br />
              </Column>
              <Column lg={16} md={8} sm={4}>
                {comments.length === 0 ? (
                  <InlineNotification
                    kind="info"
                    title={intl.formatMessage({
                      id: "notebook.comments.none.title",
                    })}
                    subtitle={intl.formatMessage({
                      id: "notebook.comments.none.subtitle",
                    })}
                  />
                ) : (
                  comments.map((comment) => (
                    <Tile
                      key={comment.id || Math.random()}
                      style={{ marginBottom: "1rem" }}
                    >
                      <p>{comment.text}</p>
                      <p style={{ fontSize: "0.875rem", color: "#525252" }}>
                        {comment.author ||
                          userSessionDetails.firstName +
                            " " +
                            userSessionDetails.lastName}
                        {comment.dateCreated
                          ? new Date(comment.dateCreated).toLocaleString()
                          : "Just now"}
                      </p>
                    </Tile>
                  ))
                )}
              </Column>
            </Grid>
          </Column>
        )}
        {selectedTab === TABS.AUDIT_TRAIL && (
          <Column lg={16} md={8} sm={4}>
            <Grid fullWidth={true} className="gridBoundary">
              <Column lg={16} md={8} sm={4}>
                <h5>
                  <FormattedMessage id="notebook.auditTrail.title" />
                </h5>
              </Column>
              <Column lg={16} md={8} sm={4}>
                <br />
              </Column>
              {auditTrailLoading ? (
                <Column lg={16} md={8} sm={4}>
                  <Loading />
                </Column>
              ) : auditTrailItems.length === 0 ? (
                <Column lg={16} md={8} sm={4}>
                  <InlineNotification
                    kind="info"
                    title={intl.formatMessage({
                      id: "notebook.auditTrail.none.title",
                    })}
                    subtitle={intl.formatMessage({
                      id: "notebook.auditTrail.none.subtitle",
                    })}
                  />
                </Column>
              ) : (
                <Column lg={16} md={8} sm={4}>
                  <DataTable
                    rows={auditTrailItems}
                    headers={[
                      {
                        key: "user",
                        header: intl.formatMessage({
                          id: "audittrail.table.heading.user",
                        }),
                      },
                      {
                        key: "action",
                        header: intl.formatMessage({
                          id: "audittrail.table.heading.action",
                        }),
                      },
                      {
                        key: "time",
                        header: intl.formatMessage({
                          id: "audittrail.table.heading.time",
                        }),
                      },
                    ]}
                    isSortable
                  >
                    {({ rows, headers, getHeaderProps, getTableProps }) => (
                      <TableContainer>
                        <Table {...getTableProps()}>
                          <TableHead>
                            <TableRow>
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
                            {rows
                              .slice((auditTrailPage - 1) * auditTrailPageSize)
                              .slice(0, auditTrailPageSize)
                              .map((row) => (
                                <TableRow key={row.id}>
                                  {row.cells.map((cell) => {
                                    let cellValue = cell.value || "-";
                                    // Translate action if it's a message code
                                    if (cell.info.header === "action") {
                                      cellValue = intl.formatMessage({
                                        id: cellValue,
                                      });
                                    }
                                    return (
                                      <TableCell key={cell.id}>
                                        {cellValue}
                                      </TableCell>
                                    );
                                  })}
                                </TableRow>
                              ))}
                          </TableBody>
                        </Table>
                      </TableContainer>
                    )}
                  </DataTable>
                  <Pagination
                    onChange={handleAuditTrailPageChange}
                    page={auditTrailPage}
                    pageSize={auditTrailPageSize}
                    pageSizes={[10, 30, 50, 100]}
                    totalItems={auditTrailItems.length}
                    forwardText={intl.formatMessage({
                      id: "pagination.forward",
                    })}
                    backwardText={intl.formatMessage({
                      id: "pagination.backward",
                    })}
                    itemRangeText={(min, max, total) =>
                      intl.formatMessage(
                        { id: "pagination.item-range" },
                        { min: min, max: max, total: total },
                      )
                    }
                    itemsPerPageText={intl.formatMessage({
                      id: "pagination.items-per-page",
                    })}
                    itemText={(min, max) =>
                      intl.formatMessage(
                        { id: "pagination.item" },
                        { min: min, max: max },
                      )
                    }
                    pageNumberText={intl.formatMessage({
                      id: "pagination.page-number",
                    })}
                    pageRangeText={(_current, total) =>
                      intl.formatMessage(
                        { id: "pagination.page-range" },
                        { total: total },
                      )
                    }
                  />
                </Column>
              )}
            </Grid>
          </Column>
        )}
      </Grid>
      <Modal
        open={showTagModal}
        modalHeading={intl.formatMessage({
          id: "notebook.tags.modal.add.title",
        })}
        primaryButtonText={intl.formatMessage({ id: "notebook.tags.add" })}
        secondaryButtonText={intl.formatMessage({
          id: "label.button.cancel",
        })}
        onRequestClose={closeTagModal}
        onRequestSubmit={handleAddTag}
      >
        {tagError && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({ id: "notification.title" })}
            subtitle={tagError}
          />
        )}
        <TextInput
          id="tag"
          name="tag"
          labelText={intl.formatMessage({
            id: "notebook.tags.modal.add.label",
          })}
          value={newTag}
          onChange={handleTagChange}
          required
        />
      </Modal>
      {/* Results Modal */}
      {/* Status Section */}
      <Grid fullWidth={true} className="orderLegendBody">
        <Column lg={16} md={8} sm={4}>
          <Grid fullWidth={true} className="gridBoundary">
            <Column lg={8} md={8} sm={4}>
              <Select
                id="status"
                name="status"
                labelText={intl.formatMessage({ id: "notebook.label.status" })}
                value={noteBookData.status || ""}
                onChange={(event) => {
                  setNoteBookData({
                    ...noteBookData,
                    status: event.target.value,
                  });
                }}
                disabled={isViewMode}
              >
                <SelectItem />
                {statuses.map((status, index) => {
                  return (
                    <SelectItem
                      key={index}
                      text={status.value}
                      value={status.id}
                    />
                  );
                })}
              </Select>
            </Column>
            <Column lg={8} md={8} sm={4}>
              <TextInput
                id="technician"
                name="technician"
                labelText={intl.formatMessage({
                  id: "notebook.label.technician",
                })}
                value={noteBookData.technicianName || ""}
                disabled
                readOnly
              />
            </Column>
          </Grid>
        </Column>
        <Column lg={16} md={8} sm={4}>
          <br />
        </Column>
        <Column lg={16} md={8} sm={4}>
          <Grid fullWidth={true} className="gridBoundary">
            <Column lg={8} md={8} sm={4}>
              <Button
                kind="primary"
                disabled={isSubmitting || isViewMode}
                onClick={() => handleSubmit()}
              >
                <FormattedMessage id="label.button.save" />
              </Button>
            </Column>
          </Grid>
        </Column>
      </Grid>
    </>
  );
};

export default NoteBookInstanceEntryForm;
