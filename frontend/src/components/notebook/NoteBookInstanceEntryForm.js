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
  postToOpenElisServer,
  hasRole,
  toBase64,
} from "../utils/Utils";
import { Add } from "@carbon/icons-react";
import AddSample from "../addOrder/AddSample";
import { sampleObject } from "../addOrder/Index";
import { ModifyOrderFormValues } from "../formModel/innitialValues/OrderEntryFormValues";
import { SearchResults } from "../resultPage/SearchResultForm";
import CustomLabNumberInput from "../common/CustomLabNumberInput";

const NoteBookInstanceEntryForm = () => {
  let breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "notebook.label.dashboard", link: "/NoteBookDashboard" },
  ];

  const MODES = Object.freeze({
    CREATE: "CREATE",
    EDIT: "EDIT",
  });

  const TABS = Object.freeze({
    ACCESSION: "ACCESSION",
    PATIENT: "PATIENT",
  });
  const intl = useIntl();
  const componentMounted = useRef(false);
  const [mode, setMode] = useState(MODES.CREATE);
  const { notebookid } = useParams();
  const { notebookentryid } = useParams();

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const [statuses, setStatuses] = useState([]);
  const [types, setTypes] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSubmittingSample, setIsSubmittingSample] = useState(false);
  const [loading, setLoading] = useState(false);
  const [noteBookData, setNoteBookData] = useState(NoteBookInitialData);
  const [noteBookForm, setNoteBookForm] = useState(NoteBookFormValues);
  const [sampleList, setSampleList] = useState([]);
  const [analyzerList, setAnalyzerList] = useState([]);
  const [uploadedFiles, setUploadedFiles] = useState([]);
  const [addedSampleIds, setAddedSampleIds] = useState([]);
  const [accession, setAccesiion] = useState("");
  const [initialMount, setInitialMount] = useState(false);
  const [allTests, setAllTests] = useState([]);
  const [samples, setSamples] = useState([sampleObject]);
  const [orderFormValues, setOrderFormValues] = useState(ModifyOrderFormValues);
  const [errors, setErrors] = useState([]);
  const [selectedTab, setSelectedTab] = useState(0);
  const [comments, setComments] = useState([]);
  const [newComment, setNewComment] = useState("");
  const [auditTrailItems, setAuditTrailItems] = useState([]);
  const [auditTrailLoading, setAuditTrailLoading] = useState(false);
  const [auditTrailPage, setAuditTrailPage] = useState(1);
  const [auditTrailPageSize, setAuditTrailPageSize] = useState(10);
  const [resultsAccession, setResultsAccession] = useState("");
  const [results, setResults] = useState({ testResult: [] });
  const [resultsLoading, setResultsLoading] = useState(false);

  const handleSubmit = (status) => {
    if (isSubmitting) {
      return;
    }
    if (mode === MODES.CREATE) {
      noteBookData.status = status ? status : noteBookData.status;
    }
    setIsSubmitting(true);
    noteBookForm.id = noteBookData.id;
    noteBookForm.isTemplate = false;
    noteBookForm.templateId = notebookid;
    noteBookForm.title = noteBookData.title;
    noteBookForm.type = noteBookData.type;
    noteBookForm.project = noteBookData.project;
    noteBookForm.objective = noteBookData.objective;
    noteBookForm.protocol = noteBookData.protocol;
    noteBookForm.content = noteBookData.content;
    noteBookForm.status = getNextStatus(noteBookData.status).id;
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

  // Add sample to noteBookData.samples
  const handleAddSample = (sample) => {
    if (addedSampleIds.includes(sample.id)) return; // prevent duplicates

    setNoteBookData((prev) => ({
      ...prev,
      samples: [...prev.samples, sample],
    }));
    setAddedSampleIds((prev) => [...prev, sample.id]);
  };

  // Remove sample from selected samples
  const handleRemoveSample = (sampleId) => {
    setNoteBookData((prev) => ({
      ...prev,
      samples: prev.samples.filter((s) => s.id !== sampleId),
    }));
    setAddedSampleIds((prev) => prev.filter((id) => id !== sampleId));
  };

  const handleSubmitOrderForm = (e) => {
    e.preventDefault();
    if (isSubmittingSample) {
      return;
    }
    setIsSubmittingSample(true);
    orderFormValues.sampleXML = getSamplesXmlValues();
    orderFormValues.sampleOrderItems.modified = true;
    //remove display Lists rom the form
    orderFormValues.sampleOrderItems.priorityList = [];
    orderFormValues.sampleOrderItems.programList = [];
    orderFormValues.sampleOrderItems.referringSiteList = [];
    orderFormValues.initialSampleConditionList = [];
    orderFormValues.testSectionList = [];
    orderFormValues.sampleOrderItems.providersList = [];
    orderFormValues.sampleOrderItems.paymentOptions = [];
    orderFormValues.sampleOrderItems.testLocationCodeList = [];
    console.log(JSON.stringify(orderFormValues));
    postToOpenElisServer(
      "/rest/SampleEdit",
      JSON.stringify(orderFormValues),
      handlePost,
    );
  };

  const handlePost = (status) => {
    setIsSubmittingSample(false);
    if (status === 200) {
      showAlertMessage(
        <FormattedMessage id="save.order.success.msg" />,
        NotificationKinds.success,
      );
      setSamples([sampleObject]);
      getFromOpenElisServer(
        "/rest/notebook/samples?accession=" + accession,
        setSampleList,
      );
    } else {
      showAlertMessage(
        <FormattedMessage id="server.error.msg" />,
        NotificationKinds.error,
      );
    }
  };

  const showAlertMessage = (msg, kind) => {
    setNotificationVisible(true);
    addNotification({
      kind: kind,
      title: intl.formatMessage({ id: "notification.title" }),
      message: msg,
    });
  };

  const getSamplesXmlValues = () => {
    let sampleXmlString = "";
    let referralItems = [];
    if (samples.length > 0) {
      if (samples[0].tests.length > 0) {
        sampleXmlString = '<?xml version="1.0" encoding="utf-8"?>';
        sampleXmlString += "<samples>";
        let tests = null;
        samples.map((sampleItem) => {
          if (sampleItem.tests.length > 0) {
            tests = Object.keys(sampleItem.tests)
              .map(function (i) {
                return sampleItem.tests[i].id;
              })
              .join(",");
            sampleXmlString += `<sample sampleID='${sampleItem.sampleTypeId}' date='${sampleItem.sampleXML.collectionDate}' time='${sampleItem.sampleXML.collectionTime}' collector='${sampleItem.sampleXML.collector}' tests='${tests}' testSectionMap='' testSampleTypeMap='' panels='' rejected='${sampleItem.sampleXML.rejected}' rejectReasonId='${sampleItem.sampleXML.rejectionReason}' initialConditionIds=''/>`;
          }
          if (sampleItem.referralItems.length > 0) {
            const referredInstitutes = Object.keys(sampleItem.referralItems)
              .map(function (i) {
                return sampleItem.referralItems[i].institute;
              })
              .join(",");

            const sentDates = Object.keys(sampleItem.referralItems)
              .map(function (i) {
                return sampleItem.referralItems[i].sentDate;
              })
              .join(",");

            const referralReasonIds = Object.keys(sampleItem.referralItems)
              .map(function (i) {
                return sampleItem.referralItems[i].reasonForReferral;
              })
              .join(",");

            const referrers = Object.keys(sampleItem.referralItems)
              .map(function (i) {
                return sampleItem.referralItems[i].referrer;
              })
              .join(",");
            referralItems.push({
              referrer: referrers,
              referredInstituteId: referredInstitutes,
              referredTestId: tests,
              referredSendDate: sentDates,
              referralReasonId: referralReasonIds,
            });
          }
        });
        sampleXmlString += "</samples>";
      }
    }
    return sampleXmlString;
  };

  const elementError = (path) => {
    if (errors?.errors?.length > 0) {
      let error = errors.inner?.find((e) => e.path === path);
      if (error) {
        return error.message;
      } else {
        return null;
      }
    }
  };

  const loadOrderValues = (data) => {
    if (componentMounted.current) {
      data.sampleOrderItems.referringSiteName = "";
      setOrderFormValues(data);
    }
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

  const handleAccesionChange = (e, rawValue) => {
    setAccesiion(rawValue ? rawValue : e?.target?.value);
  };

  const handleAccesionSearch = () => {
    getFromOpenElisServer(
      "/rest/notebook/samples?accession=" + accession,
      setSampleList,
    );

    getFromOpenElisServer(
      "/rest/SampleEdit?accessionNumber=" + accession,
      loadOrderValues,
    );
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
    if (!notebookentryid) {
      setMode(MODES.CREATE);
    } else {
      setMode(MODES.EDIT);
      setLoading(true);
      getFromOpenElisServer(
        "/rest/notebook/view/" + notebookentryid,
        loadInitialData,
      );
    }
  }, [notebookentryid]);

  useEffect(() => {
    if (notebookid) {
      setLoading(true);
      getFromOpenElisServer(
        "/rest/notebook/view/" + notebookid,
        loadInitialProjectData,
      );
    }
  }, []);

  const loadInitialProjectData = (data) => {
    if (componentMounted.current) {
      if (data && data.id) {
        data.id = null;
        data.isTemplate = false;
        data.dateCreated = null;
        data.status = "NEW";
        setNoteBookData(data);
        setLoading(false);
      }
    }
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

  const handleResultsAccessionChange = (e, rawValue) => {
    setResultsAccession(rawValue ? rawValue : e?.target?.value);
  };

  const handleResultsSearch = () => {
    if (!resultsAccession.trim()) {
      return;
    }
    setResultsLoading(true);
    setResults({ testResult: [] });
    // Extract lab number from accession (format: LAB-NUMBER or just LAB-NUMBER)
    const labNumber = resultsAccession.split("-")[0];
    const searchEndPoint =
      "/rest/LogbookResults?" +
      "labNumber=" +
      labNumber +
      "&doRange=" +
      false +
      "&finished=" +
      false +
      "&patientPK=" +
      "&collectionDate=" +
      "&recievedDate=" +
      "&selectedTest=" +
      "&selectedSampleStatus=" +
      "&selectedAnalysisStatus=";
    getFromOpenElisServer(searchEndPoint, (data) => {
      if (data && data.testResult) {
        // Add IDs to results for SearchResults component
        var i = 0;
        data.testResult.forEach((item) => (item.id = "" + i++));
        setResults(data);
      } else {
        setResults({ testResult: [] });
      }
      setResultsLoading(false);
    });
  };

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
                <FormattedMessage id="notebook.page.modal.title.label" /> :
                {noteBookData.title}
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
              <Column lg={8} md={8} sm={4}>
                <p style={{ margin: 0 }}>
                  <strong>
                    {intl.formatMessage({ id: "notebook.label.project" })}:{" "}
                  </strong>
                  {noteBookData.project ||
                    intl.formatMessage({ id: "not.available" })}
                </p>
              </Column>
              <Column lg={16} md={8} sm={4}>
                <br />
              </Column>
              <Column lg={16} md={8} sm={4}>
                <p style={{ margin: 0 }}>
                  <strong>
                    <FormattedMessage id="notebook.tags.title" />:{" "}
                  </strong>
                  {noteBookData.tags && noteBookData.tags.length > 0 ? (
                    <span>
                      {noteBookData.tags.map((tag, index) => (
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
                      {noteBookData.technicianName && (
                        <p
                          style={{
                            margin: 0,
                            fontSize: "0.875rem",
                            color: "#525252",
                          }}
                        >
                          {intl.formatMessage({ id: "notebook.label.author" })}:{" "}
                          {noteBookData.technicianName}
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
            <Switch text={intl.formatMessage({ id: "notebook.tab.samples" })} />
            <Switch
              text={intl.formatMessage({ id: "notebook.tab.workflow" })}
            />
            <Switch text={intl.formatMessage({ id: "notebook.tab.results" })} />
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
                <br />
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
              <Column lg={8} md={8} sm={4}>
                <h5>
                  {" "}
                  <FormattedMessage id="notebook.label.sampleManagement" />
                </h5>
              </Column>
              <Column lg={16} md={8} sm={4}>
                <br></br>
              </Column>

              <Column lg={8} md={8} sm={4}>
                <CustomLabNumberInput
                  id="accession"
                  name="accession"
                  value={accession}
                  placeholder={intl.formatMessage({
                    id: "notebook.search.byAccession",
                  })}
                  onChange={handleAccesionChange}
                />
              </Column>
              <Column lg={8} md={8} sm={4}>
                <Button
                  size="md"
                  onClick={handleAccesionSearch}
                  labelText={intl.formatMessage({
                    id: "label.button.search",
                  })}
                >
                  <FormattedMessage id="label.button.search" />
                </Button>
              </Column>

              <Column lg={16} md={8} sm={4}>
                <br></br>
              </Column>
              <Column lg={16} md={8} sm={4}>
                {orderFormValues?.sampleOrderItems.labNo === accession &&
                  accession != "" && (
                    <Accordion>
                      <AccordionItem title="Add Sample">
                        <Grid className="gridBoundary">
                          <Column lg={16} md={8} sm={4}>
                            <AddSample
                              error={elementError}
                              setSamples={setSamples}
                              samples={samples}
                            />
                          </Column>
                          <Column lg={16} md={8} sm={4}>
                            <Button
                              data-cy="submit-order"
                              kind="primary"
                              className="forwardButton"
                              onClick={handleSubmitOrderForm}
                              disabled={isSubmittingSample}
                            >
                              <FormattedMessage id="label.button.submit" />
                            </Button>
                          </Column>
                        </Grid>
                      </AccordionItem>
                    </Accordion>
                  )}
              </Column>

              <Column lg={16} md={8} sm={4}>
                <br></br>
              </Column>

              <Column lg={16} md={8} sm={4}>
                <h5>
                  {intl.formatMessage({ id: "notebook.samples.available" })}
                </h5>
              </Column>

              <Column lg={16} md={8} sm={4}>
                <Grid className="gridBoundary">
                  {sampleList?.length === 0 && (
                    <Column lg={16} md={8} sm={4}>
                      <InlineNotification
                        kind="info"
                        title={intl.formatMessage({
                          id: "notebook.samples.none.title",
                        })}
                        subtitle={intl.formatMessage({
                          id: "notebook.samples.none.subtitle",
                        })}
                      />
                    </Column>
                  )}
                  {sampleList?.map((sample) => (
                    <Column key={sample.id} lg={16} md={8} sm={12}>
                      <Grid fullWidth={true} className="gridBoundary">
                        <Column lg={16} md={8} sm={4}>
                          <h5>
                            {sample.sampleType} - {sample.externalId}
                          </h5>
                        </Column>
                        <Column lg={2} md={8} sm={4}>
                          <h6>
                            {intl.formatMessage({
                              id: "sample.collection.date",
                            })}
                          </h6>
                        </Column>
                        <Column lg={14} md={8} sm={4}>
                          {sample.collectionDate ||
                            intl.formatMessage({ id: "not.available" })}
                        </Column>

                        <Column lg={2} md={8} sm={4}>
                          <h6>
                            {intl.formatMessage({
                              id: "notebook.samples.resultsRecorded",
                            })}
                          </h6>
                        </Column>
                        <Column lg={14} md={8} sm={4}>
                          {sample.results.length}
                        </Column>
                        <Column lg={16} md={8} sm={4}>
                          <Button
                            kind="primary"
                            disabled={addedSampleIds.includes(sample.id)}
                            size="sm"
                            onClick={() => handleAddSample(sample)}
                          >
                            <Add /> <FormattedMessage id="label.button.add" />
                          </Button>
                        </Column>
                      </Grid>
                    </Column>
                  ))}
                </Grid>
              </Column>

              <Column lg={16} md={8} sm={4}>
                <h5>
                  {intl.formatMessage({ id: "notebook.samples.selected" })}
                </h5>
              </Column>

              <Column lg={16} md={8} sm={4}>
                {noteBookData?.samples?.length === 0 && (
                  <Grid className="gridBoundary">
                    <Column lg={16} md={8} sm={4}>
                      <InlineNotification
                        kind="info"
                        title={intl.formatMessage({
                          id: "notebook.samples.none.title.selected",
                        })}
                        subtitle={intl.formatMessage({
                          id: "notebook.samples.none.subtitle.selected",
                        })}
                      />
                    </Column>
                  </Grid>
                )}
                {noteBookData?.samples?.length > 0 && (
                  <>
                    <Grid className="gridBoundary">
                      {noteBookData.samples.map((sample) => (
                        <Column
                          key={sample.id || Math.random()}
                          lg={16}
                          md={8}
                          sm={4}
                        >
                          <Grid fullWidth={true} className="gridBoundary">
                            <Column lg={16} md={8} sm={4}>
                              <h5>
                                {sample.sampleType} - {sample.externalId}{" "}
                              </h5>
                            </Column>
                            <Column lg={2} md={8} sm={4}>
                              <h6>
                                {intl.formatMessage({
                                  id: "sample.collection.date",
                                })}
                              </h6>
                            </Column>
                            <Column lg={14} md={8} sm={4}>
                              {sample.collectionDate}
                            </Column>

                            <Column lg={2} md={8} sm={4}>
                              <h6>
                                {intl.formatMessage({
                                  id: "notebook.samples.resultsRecorded",
                                })}
                              </h6>
                            </Column>
                            <Column lg={14} md={8} sm={4}>
                              {sample.results &&
                              Array.isArray(sample.results) &&
                              sample.results.length > 0 ? (
                                <div>
                                  {sample.results.map((result, resultIndex) => (
                                    <Tile
                                      key={resultIndex}
                                      style={{
                                        marginBottom: "0.5rem",
                                        padding: "0.75rem",
                                      }}
                                    >
                                      <div style={{ marginBottom: "0.25rem" }}>
                                        <strong>
                                          {result.test ||
                                            intl.formatMessage({
                                              id: "not.available",
                                            })}
                                        </strong>
                                      </div>
                                      <div
                                        style={{
                                          marginBottom: "0.25rem",
                                          color: "#525252",
                                        }}
                                      >
                                        {intl.formatMessage({
                                          id: "column.name.result",
                                        })}
                                        :{" "}
                                        {result.result ||
                                          intl.formatMessage({
                                            id: "not.available",
                                          })}
                                      </div>
                                      <div
                                        style={{
                                          fontSize: "0.875rem",
                                          color: "#525252",
                                        }}
                                      >
                                        {intl.formatMessage({
                                          id: "notebook.sample.result.dateCreated",
                                        })}
                                        :{" "}
                                        {result.dateCreated
                                          ? new Date(
                                              result.dateCreated,
                                            ).toLocaleString()
                                          : intl.formatMessage({
                                              id: "not.available",
                                            })}
                                      </div>
                                    </Tile>
                                  ))}
                                </div>
                              ) : (
                                <span>
                                  {intl.formatMessage({
                                    id: "notebook.samples.none.title",
                                  })}
                                </span>
                              )}
                            </Column>
                            {sample.voided && (
                              <>
                                <Column lg={16} md={8} sm={4}>
                                  <InlineNotification
                                    kind="warning"
                                    title={intl.formatMessage({
                                      id: "sample.voided.title",
                                    })}
                                    subtitle={sample.voidReason}
                                    hideCloseButton
                                  />
                                </Column>
                                <Column lg={16} md={8} sm={4}>
                                  <br></br>
                                </Column>
                              </>
                            )}
                            <Column lg={16} md={8} sm={4}>
                              <Button
                                kind="danger--tertiary"
                                size="sm"
                                onClick={() => handleRemoveSample(sample.id)}
                              >
                                <FormattedMessage id="label.button.remove" />
                              </Button>
                            </Column>
                          </Grid>
                        </Column>
                      ))}
                    </Grid>
                  </>
                )}
              </Column>
            </Grid>
          </Column>
        )}
        {selectedTab === 1 && (
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
                              >
                                <FormattedMessage id="notebook.page.markComplete" />
                              </Button>
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
          </Column>
        )}
        {selectedTab === 5 && (
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
        {selectedTab === 6 && (
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
        {selectedTab === 4 && (
          <Column lg={16} md={8} sm={4}>
            <Grid fullWidth={true} className="gridBoundary">
              <Column lg={16} md={8} sm={4}>
                <h5>
                  <FormattedMessage id="notebook.results.title" />
                </h5>
              </Column>
              <Column lg={16} md={8} sm={4}>
                <br />
              </Column>
              <Column lg={8} md={8} sm={4}>
                <CustomLabNumberInput
                  id="resultsAccession"
                  name="resultsAccession"
                  value={resultsAccession}
                  placeholder={intl.formatMessage({
                    id: "notebook.search.byAccession",
                  })}
                  onChange={handleResultsAccessionChange}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") {
                      handleResultsSearch();
                    }
                  }}
                />
              </Column>
              <Column lg={8} md={8} sm={4}>
                <Button
                  size="md"
                  onClick={handleResultsSearch}
                  disabled={resultsLoading || !resultsAccession.trim()}
                >
                  <FormattedMessage id="label.button.search" />
                </Button>
              </Column>
              <Column lg={16} md={8} sm={4}>
                <br />
              </Column>
              {resultsLoading && (
                <Column lg={16} md={8} sm={4}>
                  <Loading />
                </Column>
              )}
              {!resultsLoading && results.testResult.length > 0 && (
                <Column lg={16} md={8} sm={4}>
                  <SearchResults
                    results={results}
                    setResultForm={setResults}
                    refreshOnSubmit={false}
                  />
                </Column>
              )}
              {!resultsLoading &&
                results.testResult.length === 0 &&
                resultsAccession && (
                  <Column lg={16} md={8} sm={4}>
                    <InlineNotification
                      kind="info"
                      title={intl.formatMessage({
                        id: "notebook.results.none.title",
                      })}
                      subtitle={intl.formatMessage({
                        id: "notebook.results.none.subtitle",
                      })}
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
      <Grid fullWidth={true} className="orderLegendBody">
        <Column lg={16} md={8} sm={4}>
          <Grid fullWidth={true} className="gridBoundary">
            <Column lg={8} md={8} sm={4}>
              <Button
                kind="danger--tertiary"
                disabled={isSubmitting || noteBookData.status === "ARCHIVED"}
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

export default NoteBookInstanceEntryForm;
