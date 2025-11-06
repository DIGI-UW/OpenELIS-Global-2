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
    noteBookForm.sampleIds = noteBookData.samples.map((entry) =>
      Number(entry.id),
    );
    noteBookForm.pages = noteBookData.pages;
    noteBookForm.files = noteBookData.files;
    noteBookForm.analyzerIds = noteBookData.analyzers.map((entry) =>
      Number(entry.id),
    );
    noteBookForm.tags = noteBookData.tags;
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
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "error.save.msg" }),
      });
    }
    window.location.href = "/NoteBookEntryForm/" + body.id;
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
    setIsSubmitting(false);
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

  const [showPageModal, setShowPageModal] = useState(false);
  const [showTagModal, setShowTagModal] = useState(false);
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

  // Open modal
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

  // Add new page to noteBookData.pages
  const handleAddPage = () => {
    if (!newPage.title.trim() || !newPage.content.trim()) {
      setPageError(
        intl.formatMessage({ id: "notebook.page.modal.add.errorRequired" }),
      );
      return;
    }
    setNoteBookData((prev) => ({
      ...prev,
      pages: [...prev.pages, newPage],
    }));
    setShowPageModal(false);
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

  const handleAccesionChange = (e) => {
    const { name, value } = e.target;
    setAccesiion(value);
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

  const loadOrderValues = (data) => {
    if (componentMounted.current) {
      data.sampleOrderItems.referringSiteName = "";
      setOrderFormValues(data);
    }
  };

  const loadInitialData = (data) => {
    if (componentMounted.current) {
      if (data && data.id) {
        setNoteBookData(data);
        setLoading(false);
        setInitialMount(true);
      }
    }
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
          <Grid fullWidth={true} className="gridBoundary">
            <Column lg={16} md={8} sm={4}>
              <h5>
                {" "}
                <FormattedMessage id="notebook.label.basicinfo" />
              </h5>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <br />
            </Column>
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
              <br></br>
            </Column>

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
                    <SelectItem key={index} text={type.value} value={type.id} />
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
                  setNoteBookData({ ...noteBookData, project: e.target.value });
                }}
              />
            </Column>
            <Column lg={16} md={8} sm={4}>
              <br></br>
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
              <br></br>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <TextInput
                id="protocol"
                labelText={
                  <>
                    {intl.formatMessage({
                      id: "notebook.label.protocol",
                    })}
                  </>
                }
                placeholder={intl.formatMessage({
                  id: "notebook.label.protocol",
                })}
                value={noteBookData.protocol}
                type="text"
                onChange={(e) => {
                  setNoteBookData({
                    ...noteBookData,
                    protocol: e.target.value,
                  });
                }}
              />
            </Column>
          </Grid>
        </Column>
        <Column lg={16} md={8} sm={4}>
          <Grid fullWidth={true} className="gridBoundary">
            <Column lg={16} md={8} sm={4}>
              <h5>
                {" "}
                <FormattedMessage id="notebook.label.experimentDoc" />
              </h5>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <br></br>
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
                  setNoteBookData({ ...noteBookData, content: e.target.value });
                }}
              />
            </Column>
          </Grid>
        </Column>
        <Column lg={16} md={8} sm={4}>
          <br></br>
        </Column>
        <Column lg={16} md={8} sm={4}>
          <hr></hr>
        </Column>
        <Column lg={16} md={8} sm={4}>
          <br></br>
        </Column>
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
          <Grid fullWidth={true} className="gridBoundary">
            {noteBookData?.pages?.length === 0 && (
              <Column lg={16} md={8} sm={4}>
                <InlineNotification
                  kind="info"
                  title={intl.formatMessage({
                    id: "notebook.pages.none.title",
                  })}
                  subtitle={intl.formatMessage({
                    id: "notebook.pages.none.subtitle",
                  })}
                />
              </Column>
            )}
            {noteBookData?.pages?.length > 0 && (
              <Column lg={16} md={8} sm={4}>
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
              </Column>
            )}
          </Grid>
        </Column>
        <Modal
          open={showPageModal}
          modalHeading={intl.formatMessage({
            id: "notebook.page.modal.add.title",
          })}
          primaryButtonText={intl.formatMessage({
            id: "notebook.label.addpage",
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
            initialSelectedItems={[]}
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
              <TextInput
                id="aceesion"
                name="acession"
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
                          {intl.formatMessage({ id: "sample.collection.date" })}
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
              <h5>{intl.formatMessage({ id: "notebook.samples.selected" })}</h5>
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
                            {sample.results.length}
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

        <Column lg={16} md={8} sm={4}>
          <Grid fullWidth={true} className="gridBoundary">
            <Column lg={8} md={8} sm={4}>
              <h5>
                {" "}
                {intl.formatMessage({ id: "notebook.attachments.title" })}
              </h5>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <br></br>
            </Column>
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
        <Column lg={16} md={8} sm={4}>
          <Grid fullWidth={true} className="gridBoundary">
            <Column lg={8} md={8} sm={4}>
              <h5>
                {" "}
                {intl.formatMessage({ id: "notebook.instruments.title" })}
              </h5>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <br></br>
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
          </Grid>
        </Column>
        <Column lg={16} md={8} sm={4}>
          <Grid fullWidth={true} className="gridBoundary">
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
        </Column>
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
