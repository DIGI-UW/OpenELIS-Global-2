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
  Tabs,
  TabList,
  Tab,
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
import { Launch, Subtract, ArrowLeft, ArrowRight } from "@carbon/react/icons";
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
  hasRole,
  toBase64,
} from "../utils/Utils";
import { Add, Json } from "@carbon/icons-react";

const NoteBookEntryForm = () => {
  let breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "notebook.label.dashboard", link: "/NoteBookDashboard" },
  ];

  const MODES = Object.freeze({
    CREATE: "CREATE",
    EDIT: "EDIT",
  });

  const intl = useIntl();
  const componentMounted = useRef(false);
  const [mode, setMode] = useState(MODES.CREATE);
  const { notebookid } = useParams();

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const [statuses, setStatuses] = useState([]);
  const [types, setTypes] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loading, setLoading] = useState(false);
  const [noteBookData, setNoteBookData] = useState(NoteBookInitialData);
  const [noteBookForm, setNoteBookForm] = useState(NoteBookFormValues);
  const [analyzerList, setAnalyzerList] = useState([]);
  const [uploadedFiles, setUploadedFiles] = useState([]);
  const [initialMount, setInitialMount] = useState(false);
  const [allTests, setAllTests] = useState([]);
  const [selectedTab, setSelectedTab] = useState(0);
  const [comments, setComments] = useState([]);
  const [newComment, setNewComment] = useState("");
  const [auditTrailItems, setAuditTrailItems] = useState([]);
  const [auditTrailLoading, setAuditTrailLoading] = useState(false);
  const [auditTrailPage, setAuditTrailPage] = useState(1);
  const [auditTrailPageSize, setAuditTrailPageSize] = useState(10);

  const isFormValid = () => {
    return (
      noteBookData.title?.trim() !== "" &&
      noteBookData.type !== null &&
      noteBookData.type !== "" &&
      noteBookData.project?.trim() !== "" &&
      noteBookData.objective?.trim() !== ""
    );
  };

  const handleSubmit = (status) => {
    if (isSubmitting) {
      return;
    }
    if (mode === MODES.CREATE) {
      if (!noteBookData.title || noteBookData.title.trim() === "") {
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({
            id: "notebook.validation.title.required",
          }),
        });
        return;
      }

      if (!noteBookData.type) {
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({
            id: "notebook.validation.type.required",
          }),
        });
        return;
      }

      if (!noteBookData.project || noteBookData.project.trim() === "") {
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({
            id: "notebook.validation.project.required",
          }),
        });
        return;
      }

      if (!noteBookData.objective || noteBookData.objective.trim() === "") {
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({
            id: "notebook.validation.objective.required",
          }),
        });
        return;
      }
      noteBookData.status = status ? status : noteBookData.status;
    }
    setIsSubmitting(true);
    noteBookForm.id = noteBookData.id;
    noteBookForm.isTemplate = true;
    noteBookForm.title = noteBookData.title;
    noteBookForm.type = noteBookData.type;
    noteBookForm.project = noteBookData.project;
    noteBookForm.objective = noteBookData.objective;
    noteBookForm.protocol = noteBookData.protocol;
    noteBookForm.content = noteBookData.content;
    noteBookForm.status = getNextStatus(noteBookData.status).id;
    noteBookForm.technicianId = noteBookData.technicianId;
    noteBookForm.sampleIds = noteBookData.samples
      ? noteBookData.samples.map((entry) => Number(entry.id))
      : [];
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
        ? "/rest/notebook/update/" + notebookid
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
    window.location.href = "/NoteBookEntryForm/" + body.id;
  };

  const [showPageModal, setShowPageModal] = useState(false);
  const [showTagModal, setShowTagModal] = useState(false);
  const [editingPageIndex, setEditingPageIndex] = useState(null);
  const [newPage, setNewPage] = useState({
    order: null,
    title: "",
    content: "",
    instructions: "",
    tests: [],
  });
  const [newTag, setNewTag] = useState("");
  const [pageError, setPageError] = useState("");
  const [tagError, setTagError] = useState("");

  // Open modal for adding new page
  const openPageModal = () => {
    // Calculate next order number (consecutively starting with 1)
    const nextOrder =
      noteBookData.pages.length > 0
        ? Math.max(...noteBookData.pages.map((page) => page.order || 0)) + 1
        : 1;

    setNewPage({
      order: nextOrder,
      title: "",
      content: "",
      instructions: "",
      tests: [],
    });
    setEditingPageIndex(null);
    setPageError("");
    setShowPageModal(true);
  };

  // Open modal for editing existing page
  const openEditPageModal = (index) => {
    const page = noteBookData.pages[index];
    setNewPage({
      order: page.order,
      title: page.title || "",
      content: page.content || "",
      instructions: page.instructions || "",
      tests: page.tests || [],
    });
    setEditingPageIndex(index);
    setPageError("");
    setShowPageModal(true);
  };

  const openTagModal = () => {
    setNewTag("");
    setTagError("");
    setShowTagModal(true);
  };

  // Close modal
  const closePageModal = () => setShowPageModal(false);
  const closeTagModal = () => setShowTagModal(false);

  // Handle modal input changes
  const handlePageChange = (e) => {
    const { name, value } = e.target;
    setNewPage((prev) => ({ ...prev, [name]: value }));
  };

  const handleTagChange = (e) => {
    const { name, value } = e.target;
    setNewTag(value);
  };

  // Add or update page in noteBookData.pages
  const handleAddPage = () => {
    if (!newPage.title.trim() || !newPage.content.trim()) {
      setPageError(
        intl.formatMessage({ id: "notebook.page.modal.add.errorRequired" }),
      );
      return;
    }
    if (editingPageIndex !== null) {
      // Update existing page
      setNoteBookData((prev) => {
        const updatedPages = [...prev.pages];
        updatedPages[editingPageIndex] = { ...newPage };
        return {
          ...prev,
          pages: updatedPages,
        };
      });
    } else {
      // Add new page
      setNoteBookData((prev) => ({
        ...prev,
        pages: [...prev.pages, newPage],
      }));
    }
    setShowPageModal(false);
    setEditingPageIndex(null);
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

  // Remove page by index
  const handleRemovePage = (index) => {
    setNoteBookData((prev) => {
      const updatedPages = prev.pages
        .filter((_, i) => i !== index)
        .map((page, i) => ({ ...page, order: i + 1 })); // reassign order

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

  const loadInitialData = (data) => {
    if (componentMounted.current) {
      if (data && data.id) {
        setNoteBookData(data);
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

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/displayList/NOTEBOOK_STATUS", setStatuses);
    getFromOpenElisServer("/rest/displayList/NOTEBOOK_EXPT_TYPE", setTypes);
    getFromOpenElisServer("/rest/displayList/ANALYZER_LIST", setAnalyzerList);
    getFromOpenElisServer("/rest/displayList/ALL_TESTS", setAllTests);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    if (!notebookid) {
      setMode(MODES.CREATE);
    } else {
      setMode(MODES.EDIT);
      setLoading(true);
      getFromOpenElisServer(
        "/rest/notebook/view/" + notebookid,
        loadInitialData,
      );
    }
  }, [notebookid]);

  const statusMap = [
    { id: "DRAFT", value: "Save Draft" },
    { id: "SUBMITTED", value: "Submit for Review" },
    { id: "FINALIZED", value: "Finalize Entry" },
    { id: "LOCKED", value: "Lock Entry" },
    { id: "ARCHIVED", value: "Archive Entry" },
  ];

  const statusFlow = {
    NEW: "DRAFT",
    DRAFT: "SUBMITTED",
    SUBMITTED: "FINALIZED",
    FINALIZED: "LOCKED",
    LOCKED: "ARCHIVED",
    ARCHIVED: "ARCHIVED",
  };

  function getNextStatus(currentStatus) {
    const nextStatus = currentStatus
      ? statusFlow[currentStatus]
      : statusFlow["NEW"];
    return statusMap.find((s) => s.id === nextStatus);
  }

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="notebook.label.formEntry" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && <Loading></Loading>}
      <Grid fullWidth={true} className="orderLegendBody">
        <Column lg={16} md={8} sm={4}>
          <ContentSwitcher
            selectedIndex={selectedTab}
            onChange={({ index }) => setSelectedTab(index)}
          >
            <Switch
              text={intl.formatMessage({ id: "notebook.tab.entryDetails" })}
            />
            <Switch
              text={intl.formatMessage({ id: "notebook.tab.metadata" })}
            />
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
        {selectedTab === 0 && (
          <Column lg={16} md={8} sm={4}>
            <Grid fullWidth={true} className="gridBoundary">
              <Column lg={16} md={8} sm={4}>
                <TextInput
                  id="entryTitle"
                  labelText={
                    <>
                      {intl.formatMessage({
                        id: "notebook.label.title",
                      })}
                      <span className="requiredlabel">*</span>
                    </>
                  }
                  placeholder={intl.formatMessage({
                    id: "notebook.label.title",
                  })}
                  value={noteBookData.title}
                  type="text"
                  onChange={(e) => {
                    setNoteBookData({ ...noteBookData, title: e.target.value });
                  }}
                />
              </Column>
              <Column lg={16} md={8} sm={4}>
                <br />
              </Column>
              <Column lg={16} md={8} sm={4}>
                <TextArea
                  id="content"
                  labelText={
                    <>
                      {intl.formatMessage({
                        id: "notebook.label.content",
                      })}
                    </>
                  }
                  placeholder={intl.formatMessage({
                    id: "notebook.label.content",
                  })}
                  value={noteBookData.content}
                  type="text"
                  onChange={(e) => {
                    setNoteBookData({
                      ...noteBookData,
                      content: e.target.value,
                    });
                  }}
                />
              </Column>
            </Grid>
          </Column>
        )}
        {selectedTab === 1 && (
          <Column lg={16} md={8} sm={4}>
            <Grid fullWidth={true} className="gridBoundary">
              <Column lg={8} md={2} sm={2}>
                <Select
                  id="experimenttype"
                  name="experimenttype"
                  labelText={
                    <>
                      {intl.formatMessage({
                        id: "notebook.label.experimentType",
                      })}
                      <span className="requiredlabel">*</span>
                    </>
                  }
                  value={noteBookData.type || ""}
                  onChange={(event) => {
                    setNoteBookData({
                      ...noteBookData,
                      type: event.target.value,
                    });
                  }}
                >
                  <SelectItem />
                  {types.map((type, index) => {
                    return (
                      <SelectItem
                        key={index}
                        text={type.value}
                        value={type.id}
                      />
                    );
                  })}
                </Select>
              </Column>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="entryProject"
                  labelText={
                    <>
                      {intl.formatMessage({
                        id: "notebook.label.project",
                      })}
                      <span className="requiredlabel">*</span>
                    </>
                  }
                  placeholder={intl.formatMessage({
                    id: "notebook.label.project",
                  })}
                  value={noteBookData.project}
                  type="text"
                  onChange={(e) => {
                    setNoteBookData({
                      ...noteBookData,
                      project: e.target.value,
                    });
                  }}
                />
              </Column>
              <Column lg={16} md={8} sm={4}>
                <br />
              </Column>
              <Column lg={16} md={8} sm={4}>
                <TextArea
                  id="objective"
                  labelText={
                    <>
                      {intl.formatMessage({
                        id: "notebook.label.objective",
                      })}
                      <span className="requiredlabel">*</span>
                    </>
                  }
                  placeholder={intl.formatMessage({
                    id: "notebook.label.objective",
                  })}
                  value={noteBookData.objective}
                  type="text"
                  onChange={(e) => {
                    setNoteBookData({
                      ...noteBookData,
                      objective: e.target.value,
                    });
                  }}
                />
              </Column>
              <Column lg={16} md={8} sm={4}>
                <br />
              </Column>
              <Column lg={4} md={8} sm={4}>
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
              <Column lg={1} md={8} sm={4}>
                <h5>
                  {" "}
                  <FormattedMessage id="notebook.tags.title" />
                </h5>
              </Column>
              <Column lg={8} md={8} sm={4}>
                <Button onClick={openTagModal} kind="primary" size="sm">
                  <Add />
                  <FormattedMessage id="notebook.tags.add" />
                </Button>
              </Column>
              <Column lg={16} md={8} sm={4}>
                <br></br>
              </Column>
              <Column lg={16} md={8} sm={4}>
                {noteBookData.tags.map((tag, index) => (
                  <Tag
                    key={index}
                    filter
                    onClose={() => {
                      handleRemoveTag(index);
                    }}
                  >
                    {tag}
                  </Tag>
                ))}
              </Column>
            </Grid>
          </Column>
        )}
        {selectedTab === 2 && (
          <Column lg={16} md={8} sm={4}>
            <Grid fullWidth={true} className="gridBoundary">
              <Column lg={16} md={8} sm={4}>
                <FileUploaderDropContainer
                  labelText={intl.formatMessage({
                    id: "notebook.attachments.uploadPrompt",
                  })}
                  multiple
                  onAddFiles={handleAddFiles}
                  accept={[".pdf", ".png", ".jpg", ".txt"]}
                />
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
                {noteBookData.files.length > 0 && (
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
        {selectedTab === 3 && (
          <Column lg={16} md={8} sm={4}>
            <Grid fullWidth={true} className="gridBoundary">
              <Column lg={2} md={2} sm={4}>
                <h5>
                  {" "}
                  <FormattedMessage id="notebook.label.pages" />
                </h5>
              </Column>
              <Column lg={2} md={2} sm={4}>
                <Button onClick={openPageModal} size="sm" kind="primary">
                  <Add /> <FormattedMessage id="notebook.label.addpage" />
                </Button>
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
                            <Button
                              kind="primary"
                              size="sm"
                              onClick={() => openEditPageModal(index)}
                              style={{ marginRight: "0.5rem" }}
                            >
                              <FormattedMessage id="label.button.edit" />
                            </Button>
                            <Button
                              kind="danger--tertiary"
                              size="sm"
                              onClick={() => handleRemovePage(index)}
                            >
                              <FormattedMessage id="label.button.remove" />
                            </Button>
                          </Column>
                        </Grid>
                      </AccordionItem>
                    ))}
                  </Accordion>
                )}
              </Column>
            </Grid>
          </Column>
        )}
        {selectedTab === 4 && (
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
                <Button onClick={handleAddComment} kind="primary" size="sm">
                  <FormattedMessage id="notebook.comments.add.button" />
                </Button>
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
                        {comment.author || "Unknown"} -{" "}
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
        {selectedTab === 5 && (
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
        open={showPageModal}
        modalHeading={intl.formatMessage({
          id:
            editingPageIndex !== null
              ? "notebook.page.modal.edit.title"
              : "notebook.page.modal.add.title",
        })}
        primaryButtonText={intl.formatMessage({
          id:
            editingPageIndex !== null
              ? "label.button.save"
              : "notebook.label.addpage",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.button.cancel",
        })}
        onRequestClose={closePageModal}
        onRequestSubmit={handleAddPage}
      >
        {pageError && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({ id: "notification.title" })}
            subtitle={pageError}
          />
        )}
        <TextInput
          id="title"
          name="title"
          labelText={intl.formatMessage({
            id: "notebook.page.modal.title.label",
          })}
          value={newPage.title}
          onChange={handlePageChange}
          required
        />
        <FilterableMultiSelect
          key={showPageModal ? "open" : "closed"}
          id="tests"
          titleText={intl.formatMessage({
            id: "barcode.label.info.tests",
          })}
          items={allTests}
          itemToString={(item) => (item ? item.value : "")}
          initialSelectedItems={
            editingPageIndex !== null
              ? allTests.filter((test) => newPage.tests.includes(test.id))
              : []
          }
          onChange={(changes) => {
            setNewPage({
              ...newPage,
              tests: changes.selectedItems.map((test) => test.id),
            });
          }}
          selectionFeedback="top-after-reopen"
        />
        <TextArea
          id="instructions"
          name="instructions"
          labelText={intl.formatMessage({
            id: "notebook.page.modal.instructions.label",
          })}
          value={newPage.instructions}
          onChange={handlePageChange}
        />
        <TextArea
          id="content"
          name="content"
          labelText={intl.formatMessage({
            id: "notebook.page.modal.content.label",
          })}
          value={newPage.content}
          onChange={handlePageChange}
          required
        />
      </Modal>
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
      <Grid fullWidth={true} className="orderLegendBody">
        <Column lg={16} md={8} sm={4}>
          <Grid fullWidth={true} className="gridBoundary">
            <Column lg={8} md={8} sm={4}>
              <Button
                kind="danger--tertiary"
                disabled={
                  isSubmitting ||
                  noteBookData.status === "ARCHIVED" ||
                  (mode === MODES.CREATE && !isFormValid())
                }
                onClick={() => handleSubmit()}
              >
                {intl.formatMessage({
                  id: `notebook.status.${getNextStatus(noteBookData.status).id.toLowerCase()}`,
                })}
              </Button>
            </Column>
            {noteBookData.status == "NEW" && (
              <Column lg={8} md={8} sm={4}>
                <Button
                  kind="danger--tertiary"
                  disabled={mode === MODES.CREATE && !isFormValid()}
                  onClick={() => handleSubmit("DRAFT")}
                >
                  {intl.formatMessage({
                    id: `notebook.status.${getNextStatus("DRAFT").id.toLowerCase()}`,
                  })}
                </Button>
              </Column>
            )}
          </Grid>
        </Column>
      </Grid>
    </>
  );
};

export default NoteBookEntryForm;
