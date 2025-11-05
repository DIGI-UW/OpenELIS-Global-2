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
  postToOpenElisServerForPDF,
  postToOpenElisServer,
  hasRole,
  toBase64,
} from "../utils/Utils";
import SearchPatientForm from "../patient/SearchPatientForm";
import { fil } from "date-fns/locale";
import { Add } from "@carbon/icons-react";
import PatientHeader from "../common/PatientHeader";

const NoteBookEntryForm = () => {
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

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const [statuses, setStatuses] = useState([]);
  const [types, setTypes] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loading, setLoading] = useState(false);
  const [noteBookData, setNoteBookData] = useState(NoteBookInitialData);
  const [noteBookForm, setNoteBookForm] = useState(NoteBookFormValues);
  const [sampleList, setSampleList] = useState([]);
  const [analyzerList, setAnalyzerList] = useState([]);
  const [uploadedFiles, setUploadedFiles] = useState([]);
  const [addedSampleIds, setAddedSampleIds] = useState([]);
  const [activeTab, setActiveTab] = useState(TABS.ACCESSION);
  const [accession, setAccesiion] = useState("");
  const [initialMount, setInitialMount] = useState(false);

  const handleSubmit = () => {
    if (isSubmitting) {
      return;
    }
    if (mode === MODES.CREATE) {
      if (noteBookData.samples.length > 0) {
        noteBookForm.patientId = noteBookData.samples[0].patientId;
      } else {
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({
            id: "notebook.samples.none.title.selected",
          }),
        });
        return;
      }
    } else {
      noteBookForm.patientId = noteBookData.patientId;
    }
    setIsSubmitting(true);
    noteBookForm.id = noteBookData.id;
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
    postToOpenElisServer(url, JSON.stringify(noteBookForm), handleSubmited);
  };

  const handleSubmited = (status) => {
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
    window.location.reload();
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

  const [showPageModal, setShowPageModal] = useState(false);
  const [showTagModal, setShowTagModal] = useState(false);
  const [newPage, setNewPage] = useState({
    title: "",
    content: "",
    instructions: "",
  });
  const [newTag, setNewTag] = useState("");
  const [pageError, setPageError] = useState("");
  const [tagError, setTagError] = useState("");

  // Open modal
  const openPageModal = () => {
    setNewPage({ title: "", content: "", instructions: "" });
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
    setNoteBookData((prev) => ({
      ...prev,
      pages: prev.pages.filter((_, i) => i !== index),
    }));
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

  const getSelectedPatient = (patient) => {
    getFromOpenElisServer(
      "/rest/notebook/samples?patientId=" + patient.patientPK,
      setSampleList,
    );
  };

  const handleAccesionChange = (e) => {
    const { name, value } = e.target;
    setAccesiion(value);
  };

  const handleAccesionSearch = () => {
    getFromOpenElisServer(
      "/rest/notebook/samples?accession=" + accession,
      setSampleList,
    );
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/displayList/NOTEBOOK_STATUS", setStatuses);
    getFromOpenElisServer("/rest/displayList/NOTEBOOK_EXPT_TYPE", setTypes);
    getFromOpenElisServer("/rest/displayList/ANALYZER_LIST", setAnalyzerList);
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

  const loadInitialData = (data) => {
    if (componentMounted.current) {
      if (data && data.id) {
        setNoteBookData(data);
        setLoading(false);
        getFromOpenElisServer(
          "/rest/notebook/samples?patientId=" + data.patientId,
          setSampleList,
        );
        setAddedSampleIds(data.samples.map((entry) => entry.id));
        setInitialMount(true);
      }
    }
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
      {mode === MODES.EDIT && (
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Section>
                <PatientHeader
                  id={noteBookData.patientId}
                  lastName={noteBookData.lastName}
                  firstName={noteBookData.firstName}
                  gender={noteBookData.gender}
                  orderDate={noteBookData.dateCreated}
                  className="patient-header2"
                  isOrderPage={true}
                >
                  {" "}
                </PatientHeader>
              </Section>
            </Section>
          </Column>
        </Grid>
      )}
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
            {noteBookData.pages.map((page, index) => (
              <Column key={index} lg={16} md={8} sm={4}>
                <Grid fullWidth={true} className="gridBoundary">
                  <Column lg={16} md={8} sm={4}>
                    <h5>{page.title}</h5>
                  </Column>
                  <Column lg={2} md={8} sm={4}>
                    <h6>
                      {intl.formatMessage({ id: "notebook.page.instructions" })}
                    </h6>
                  </Column>
                  <Column lg={14} md={8} sm={4}>
                    {page.instructions}
                  </Column>
                  <Column lg={2} md={8} sm={4}>
                    <h6>
                      {intl.formatMessage({ id: "notebook.page.content" })}
                    </h6>
                  </Column>
                  <Column lg={14} md={8} sm={4}>
                    {page.content}
                  </Column>
                  <Column lg={16} md={8} sm={4}>
                    <Button
                      kind="danger--tertiary"
                      size="sm"
                      onClick={() => handleRemovePage(index)}
                    >
                      <FormattedMessage id="label.button.remove" />
                    </Button>
                  </Column>
                </Grid>
              </Column>
            ))}
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
              title="Error"
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
            {mode === MODES.CREATE && (
              <>
                <Column lg={16} md={8} sm={4}>
                  <Tabs>
                    <TabList
                      style={{ width: "100%" }}
                      aria-label="List of tabs"
                      contained
                    >
                      <Tab onClick={() => setActiveTab(TABS.ACCESSION)}>
                        {intl.formatMessage({
                          id: "notebook.search.byAccession",
                        })}
                      </Tab>
                      <Tab onClick={() => setActiveTab(TABS.PATIENT)}>
                        {intl.formatMessage({
                          id: "notebook.search.byPatient",
                        })}
                      </Tab>
                    </TabList>
                  </Tabs>
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <br></br>
                </Column>

                {activeTab === TABS.PATIENT && (
                  <Column lg={16} md={8} sm={4}>
                    <SearchPatientForm
                      getSelectedPatient={getSelectedPatient}
                    ></SearchPatientForm>
                  </Column>
                )}

                {activeTab === TABS.ACCESSION && (
                  <>
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
                  </>
                )}
                <Column lg={16} md={8} sm={4}>
                  <br></br>
                </Column>
              </>
            )}
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
                        {sample.collectionDate || "N/A"}
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
                title="Error"
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
            <Button
              kind="danger--tertiary"
              disabled={
                isSubmitting ||
                noteBookData.status === "ARCHIVED" ||
                noteBookData?.samples?.length === 0
              }
              onClick={() => handleSubmit()}
            >
              {intl.formatMessage({
                id: `notebook.status.${getNextStatus(noteBookData.status).id.toLowerCase()}`,
              })}
            </Button>
          </Grid>
        </Column>
      </Grid>
    </>
  );
};

export default NoteBookEntryForm;
