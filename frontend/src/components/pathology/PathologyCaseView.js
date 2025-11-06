import React, { useContext, useState, useEffect, useRef } from "react";
import { useParams } from "react-router-dom";
import config from "../../config.json";
import {
  IconButton,
  Heading,
  TextInput,
  Select,
  FilterableMultiSelect,
  SelectItem,
  Button,
  Grid,
  Column,
  Checkbox,
  Section,
  FileUploader,
  Tag,
  TextArea,
  Loading,
  InlineLoading,
  Link,
} from "@carbon/react";
import { Launch, Subtract, ArrowLeft, ArrowRight } from "@carbon/react/icons";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  postToOpenElisServerForPDF,
  hasRole,
} from "../utils/Utils";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { FormattedMessage, useIntl } from "react-intl";
import PatientHeader from "../common/PatientHeader";
import QuestionnaireResponse from "../common/QuestionnaireResponse";
import "./PathologyDashboard.css";
import PageBreadCrumb from "../common/PageBreadCrumb";

function PathologyCaseView() {
  const intl = useIntl();

  const componentMounted = useRef(false);

  const { pathologySampleId } = useParams();

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);

  const [pathologySampleInfo, setPathologySampleInfo] = useState({});

  const [initialMount, setInitialMount] = useState(false);

  const [statuses, setStatuses] = useState([]);
  const [techniques, setTechniques] = useState([]);
  const [requests, setRequests] = useState([]);
  const [requestStatuses, setRequestStatuses] = useState([]);
  const [conclusions, setConclusions] = useState([]);
  const [immunoHistoChemistryTests, setImmunoHistoChemistryTests] = useState(
    [],
  );
  const [technicianUsers, setTechnicianUsers] = useState([]);
  const [pathologistUsers, setPathologistUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [blocksToAdd, setBlocksToAdd] = useState(1);
  const [slidesToAdd, setSlidesToAdd] = useState(1);
  const [loadingReport, setLoadingReport] = useState(false);
  const [nextPage, setNextPage] = useState(null);
  const [previousPage, setPreviousPage] = useState(null);
  const [pagination, setPagination] = useState(false);
  const [currentApiPage, setCurrentApiPage] = useState(null);
  const [totalApiPages, setTotalApiPages] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [reportParams, setReportParams] = useState({
    0: {
      submited: false,
      reportLink: "",
    },
  });

  async function displayStatus(response) {
    var body = await response.json();
    console.debug(body);
    setIsSubmitting(false);
    var status = response.status;
    setNotificationVisible(true);
    if (status == "200") {
      const save1 = document.getElementById("pathology_save");
      const save2 = document.getElementById("pathology_save2");
      save1.disabled = true;
      save2.disabled = true;
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "success.save.msg" }),
      });
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "error.save.msg" }),
      });
    }
  }

  const reportStatus = async (pdfGenerated, blob, index) => {
    setNotificationVisible(true);
    setLoadingReport(false);
    if (pdfGenerated) {
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "success.report.status" }),
      });
      var params = { ...reportParams };
      if (!params[index]) {
        params[index] = {};
      }
      params[index].submited = true;
      params[index].reportLink = window.URL.createObjectURL(blob, {
        type: "application/pdf",
      });
      setReportParams(params);

      var newReports = [...pathologySampleInfo.reports];
      let encodedFile = await toBase64(blob);
      newReports[index].base64Image = encodedFile;
      setPathologySampleInfo({
        ...pathologySampleInfo,
        reports: newReports,
      });
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "error.report.status" }),
      });
    }
  };

  const toBase64 = (file) =>
    new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => resolve(reader.result);
      reader.onerror = reject;
    });

  const save = (e) => {
    if (isSubmitting) {
      return;
    }
    setIsSubmitting(true);
    let submitValues = {
      assignedTechnicianId: pathologySampleInfo.assignedTechnicianId,
      assignedPathologistId: pathologySampleInfo.assignedPathologistId,
      status: pathologySampleInfo.status,
      blocks: pathologySampleInfo.blocks,
      slides: pathologySampleInfo.slides,
      reports: pathologySampleInfo.reports,
      grossExam: pathologySampleInfo.grossExam,
      microscopyExam: pathologySampleInfo.microscopyExam,
      conclusionText: pathologySampleInfo.conclusionText,
      release:
        pathologySampleInfo.release != undefined
          ? pathologySampleInfo.release
          : false,
      referToImmunoHistoChemistry:
        pathologySampleInfo.referToImmunoHistoChemistry,
    };
    if (pathologySampleInfo.immunoHistoChemistryTestIds) {
      submitValues = {
        ...submitValues,
        immunoHistoChemistryTestIds:
          pathologySampleInfo.immunoHistoChemistryTestIds.map((e) => {
            return e.id;
          }),
      };
    }

    if (pathologySampleInfo.techniques) {
      submitValues = {
        ...submitValues,
        techniques: pathologySampleInfo.techniques.map((e) => {
          return e.id;
        }),
      };
    }
    if (pathologySampleInfo.requests) {
      submitValues = {
        ...submitValues,
        requests: pathologySampleInfo.requests.map((e) => {
          return { value: e.id, status: e.status };
        }),
      };
    }
    if (pathologySampleInfo.conclusions) {
      submitValues = {
        ...submitValues,
        conclusions: pathologySampleInfo.conclusions.map((e) => {
          return e.id;
        }),
      };
    }

    postToOpenElisServerFullResponse(
      "/rest/pathology/caseView/" + pathologySampleId,
      JSON.stringify(submitValues),
      displayStatus,
    );
  };
  let breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "pathology.label.dashboard", link: "/PathologyDashboard" },
  ];

  const setInitialPathologySampleInfo = (e) => {
    if (
      hasRole(userSessionDetails, "Pathologist") &&
      !e.assignedPathologistId &&
      e.status === "READY_PATHOLOGIST"
    ) {
      e.assignedPathologistId = userSessionDetails.userId;
      e.assignedPathologist =
        userSessionDetails.lastName + " " + userSessionDetails.firstName;
    }
    if (!e.assignedTechnicianId) {
      e.assignedTechnicianId = userSessionDetails.userId;
      e.assignedTechnician =
        userSessionDetails.lastName + " " + userSessionDetails.firstName;
    }
    setPathologySampleInfo(e);
    setLoading(false);
    setInitialMount(true);
  };

  useEffect(() => {
    componentMounted.current = true;
    setNextPage(null);
    setPreviousPage(null);
    setPagination(false);
    getFromOpenElisServer("/rest/displayList/PATHOLOGY_STATUS", setStatuses);
    getFromOpenElisServer(
      "/rest/displayList/PATHOLOGY_TECHNIQUES",
      setTechniques,
    );
    getFromOpenElisServer(
      "/rest/displayList/PATHOLOGIST_REQUESTS",
      setRequests,
    );
    getFromOpenElisServer(
      "/rest/displayList/PATHOLOGY_REQUEST_STATUS",
      setRequestStatuses,
    );
    //TODO make conclusions list instead of reusing pathrequest
    getFromOpenElisServer(
      "/rest/displayList/IMMUNOHISTOCHEMISTRY_MARKERS_TESTS",
      setImmunoHistoChemistryTests,
    );
    getFromOpenElisServer(
      "/rest/paginatedDisplayList/PATHOLOGIST_CONCLUSIONS",
      loadConclusionData,
    );
    getFromOpenElisServer("/rest/users", setTechnicianUsers);
    getFromOpenElisServer("/rest/users/Pathologist", setPathologistUsers);
    getFromOpenElisServer(
      "/rest/pathology/caseView/" + pathologySampleId,
      setInitialPathologySampleInfo,
    );

    return () => {
      componentMounted.current = false;
    };
  }, []);

  const loadNextCOnclusionsPage = () => {
    setLoading(true);
    getFromOpenElisServer(
      "/rest/paginatedDisplayList/PATHOLOGIST_CONCLUSIONS" +
        "?page=" +
        nextPage,
      loadConclusionData,
    );
  };

  const loadPreviousConclusionsPage = () => {
    setLoading(true);
    getFromOpenElisServer(
      "/rest/paginatedDisplayList/PATHOLOGIST_CONCLUSIONS" +
        "?page=" +
        previousPage,
      loadConclusionData,
    );
  };

  const loadConclusionData = (res) => {
    // If the response object is not null and has displayItems array with length greater than 0 then set it as data.
    if (res && res.displayItems && res.displayItems.length > 0) {
      setConclusions(res.displayItems);
    } else {
      setConclusions([]);
    }

    // Sets next and previous page numbers based on the total pages and current page number.
    if (res && res.paging) {
      const { totalPages, currentPage } = res.paging;
      if (totalPages > 1) {
        setPagination(true);
        setCurrentApiPage(currentPage);
        setTotalApiPages(totalPages);
        if (parseInt(currentPage) < parseInt(totalPages)) {
          setNextPage(parseInt(currentPage) + 1);
        } else {
          setNextPage(null);
        }

        if (parseInt(currentPage) > 1) {
          setPreviousPage(parseInt(currentPage) - 1);
        } else {
          setPreviousPage(null);
        }
      }
    }

    setLoading(false);
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="pathology.label.title" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <PatientHeader
                id={pathologySampleInfo.labNumber}
                lastName={pathologySampleInfo.lastName}
                firstName={pathologySampleInfo.firstName}
                gender={pathologySampleInfo.sex}
                age={pathologySampleInfo.age}
                orderDate={pathologySampleInfo.requestDate}
                referringFacility={pathologySampleInfo.referringFacility}
                department={pathologySampleInfo.department}
                requester={pathologySampleInfo.requester}
                accesionNumber={pathologySampleInfo.labNumber}
                className="patient-header2"
                isOrderPage={true}
              >
                {" "}
              </PatientHeader>
            </Section>
          </Section>
          <Section>
            <Section>
              <div className="patient-header2">
                <QuestionnaireResponse
                  questionnaireResponse={
                    pathologySampleInfo.programQuestionnaireResponse
                  }
                />
              </div>
            </Section>
          </Section>
        </Column>
      </Grid>
      <Grid fullWidth={true} className="orderLegendBody">
        {notificationVisible === true ? <AlertDialog /> : ""}
        {loading && <Loading description="Loading Dasboard..." />}
        <Column lg={16} md={8} sm={4}>
          <Button
            id="pathology_save"
            disabled={isSubmitting}
            onClick={(e) => {
              e.preventDefault();
              save(e);
            }}
          >
            <FormattedMessage id="label.button.save" />
          </Button>
        </Column>
        <Column lg={16} md={8} sm={4}>
          <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
        </Column>
        <Column lg={4} md={2} sm={2}>
          <Select
            id="status"
            name="status"
            labelText={intl.formatMessage({ id: "label.button.select.status" })}
            value={pathologySampleInfo.status}
            onChange={(event) => {
              setPathologySampleInfo({
                ...pathologySampleInfo,
                status: event.target.value,
              });
            }}
          >
            <SelectItem disabled value="placeholder" text="Status" />

            {statuses.map((status, index) => {
              return (
                <SelectItem key={index} text={status.value} value={status.id} />
              );
            })}
          </Select>
        </Column>
        <Column lg={4} md={2} sm={2}>
          <Select
            id="assignedTechnician"
            name="assignedTechnician"
            labelText={intl.formatMessage({
              id: "label.button.select.technician",
            })}
            value={pathologySampleInfo.assignedTechnicianId}
            onChange={(event) => {
              setPathologySampleInfo({
                ...pathologySampleInfo,
                assignedTechnicianId: event.target.value,
              });
            }}
          >
            <SelectItem />
            {technicianUsers.map((user, index) => {
              return (
                <SelectItem key={index} text={user.value} value={user.id} />
              );
            })}
          </Select>
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Select
            id="assignedPathologist"
            name="assignedPathologist"
            labelText={
              <FormattedMessage id="label.button.select.pathologist" />
            }
            value={pathologySampleInfo.assignedPathologistId}
            onChange={(e) => {
              setPathologySampleInfo({
                ...pathologySampleInfo,
                assignedPathologistId: e.target.value,
              });
            }}
          >
            <SelectItem />
            {pathologistUsers.map((user, index) => {
              return (
                <SelectItem key={index} text={user.value} value={user.id} />
              );
            })}
          </Select>
        </Column>
        <Column lg={16} md={8} sm={4}></Column>
        <Column lg={16} md={8} sm={4}>
          <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
        </Column>
        <Column lg={16} md={8} sm={4}>
          <Grid fullWidth={true} className="gridBoundary">
            <Column lg={16} md={8} sm={4}>
              <h5>
                {" "}
                <FormattedMessage id="immunohistochemistry.label.reports" />
              </h5>
              {loadingReport && <InlineLoading />}
              <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
            </Column>

            {pathologySampleInfo.reports &&
              pathologySampleInfo.reports.map((report, index) => {
                return (
                  <>
                    <Column lg={2} md={4} sm={4}>
                      <IconButton
                        label={intl.formatMessage({
                          id: "label.button.remove.report",
                        })}
                        onClick={() => {
                          var info = { ...pathologySampleInfo };
                          info["reports"].splice(index, 1);
                          setPathologySampleInfo(info);
                        }}
                        kind="tertiary"
                        size="sm"
                      >
                        <Subtract size={18} />{" "}
                        <FormattedMessage id="immunohistochemistry.label.report" />
                      </IconButton>
                    </Column>
                    <Column lg={2} md={4} sm={4}>
                      <FileUploader
                        style={{ marginTop: "-20px" }}
                        buttonLabel={
                          <FormattedMessage id="label.button.uploadfile" />
                        }
                        iconDescription="file upload"
                        multiple={false}
                        accept={["image/jpeg", "image/png", "application/pdf"]}
                        disabled={reportParams[index]?.submited}
                        name=""
                        buttonKind="primary"
                        size="lg"
                        filenameStatus="edit"
                        onChange={async (e) => {
                          e.preventDefault();
                          let file = e.target.files[0];
                          var newReports = [...pathologySampleInfo.reports];
                          let encodedFile = await toBase64(file);
                          newReports[index].base64Image = encodedFile;
                          setPathologySampleInfo({
                            ...pathologySampleInfo,
                            reports: newReports,
                          });
                        }}
                        onClick={function noRefCheck() {}}
                        onDelete={(e) => {
                          e.preventDefault();
                        }}
                      />
                    </Column>
                    <Column lg={3}>
                      <h6>
                        <FormattedMessage id="pathology.label.report" />
                      </h6>
                    </Column>
                    {pathologySampleInfo.reports[index].image && (
                      <>
                        {!reportParams[index]?.submited && (
                          <Column lg={2} md={1} sm={2}>
                            <Button
                              onClick={() => {
                                var win = window.open();
                                win.document.write(
                                  '<iframe src="' +
                                    report.fileType +
                                    ";base64," +
                                    report.image +
                                    '" frameborder="0" style="border:0; top:0px; left:0px; bottom:0px; right:0px; width:100%; height:100%;" allowfullscreen></iframe>',
                                );
                              }}
                            >
                              <Launch />{" "}
                              <FormattedMessage id="pathology.label.view" />
                            </Button>
                          </Column>
                        )}
                      </>
                    )}

                    {reportParams[index]?.submited && (
                      <Column lg={2} md={1} sm={2}>
                        <Button
                          onClick={() => {
                            window.open(
                              reportParams[index]?.reportLink,
                              "_blank",
                            );
                          }}
                        >
                          <Launch />{" "}
                          <FormattedMessage id="pathology.label.view" />
                        </Button>
                      </Column>
                    )}
                    <Column lg={3} md={2} sm={2}>
                      <Button
                        disabled={reportParams[index]?.submited}
                        onClick={(e) => {
                          setLoadingReport(true);
                          const form = {
                            report: "PatientPathologyReport",
                            programSampleId: pathologySampleId,
                          };
                          postToOpenElisServerForPDF(
                            "/rest/ReportPrint",
                            JSON.stringify(form),
                            (e, blob) => reportStatus(e, blob, index),
                          );
                        }}
                      >
                        {" "}
                        <FormattedMessage id="button.label.genarateReport" />
                      </Button>
                    </Column>
                    <Column lg={3} md={2} sm={2} />
                    <Column lg={16} md={8} sm={4}>
                      <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
                    </Column>
                  </>
                );
              })}

            <Column lg={16} md={8} sm={4}>
              <Button
                onClick={() => {
                  setPathologySampleInfo({
                    ...pathologySampleInfo,
                    reports: [
                      ...(pathologySampleInfo.reports || []),
                      { id: "", reportType: "PATHOLOGY" },
                    ],
                  });
                }}
              >
                <FormattedMessage id="immunohistochemistry.label.addreport" />
              </Button>
            </Column>
          </Grid>
        </Column>

        <Column lg={16} md={8} sm={4}>
          <Grid fullWidth={true} className="gridBoundary">
            <Column lg={16} md={8} sm={4}>
              <h5>
                <FormattedMessage id="pathology.label.blocks" />
              </h5>
              <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
            </Column>
            {pathologySampleInfo.blocks &&
              pathologySampleInfo.blocks.map((block, index) => {
                return (
                  <>
                    <Column lg={2} md={8} sm={4}>
                      <IconButton
                        label={intl.formatMessage({
                          id: "label.button.remove.block",
                        })}
                        onClick={() => {
                          var info = { ...pathologySampleInfo };
                          info["blocks"].splice(index, 1);
                          setPathologySampleInfo(info);
                        }}
                        kind="tertiary"
                        size="sm"
                      >
                        <Subtract size={18} />{" "}
                        <FormattedMessage id="pathology.label.block" />
                      </IconButton>
                    </Column>
                    <Column lg={3} md={2} sm={1} key={index}>
                      <TextInput
                        id="blockNumber"
                        labelText={intl.formatMessage({
                          id: "pathology.label.block.number",
                        })}
                        hideLabel={true}
                        placeholder={intl.formatMessage({
                          id: "pathology.label.block.number",
                        })}
                        value={block.blockNumber}
                        type="number"
                        onChange={(e) => {
                          var newBlocks = [...pathologySampleInfo.blocks];
                          newBlocks[index].blockNumber = e.target.value;
                          setPathologySampleInfo({
                            ...pathologySampleInfo,
                            blocks: newBlocks,
                          });
                        }}
                      />
                    </Column>
                    <Column lg={3} md={2} sm={1}>
                      <TextInput
                        id="location"
                        labelText={intl.formatMessage({
                          id: "pathology.label.location",
                        })}
                        hideLabel={true}
                        placeholder={intl.formatMessage({
                          id: "pathology.label.location",
                        })}
                        value={block.location}
                        onChange={(e) => {
                          var newBlocks = [...pathologySampleInfo.blocks];
                          newBlocks[index].location = e.target.value;
                          setPathologySampleInfo({
                            ...pathologySampleInfo,
                            blocks: newBlocks,
                          });
                        }}
                      />
                    </Column>
                    <Column lg={3} md={2} sm={2}>
                      <Button
                        onClick={(e) => {
                          window.open(
                            config.serverBaseUrl +
                              "/LabelMakerServlet?labelType=block&code=" +
                              block.blockNumber,
                            "_blank",
                          );
                        }}
                      >
                        {" "}
                        <FormattedMessage id="pathology.label.printlabel" />
                      </Button>
                    </Column>
                    <Column lg={5} md={2} sm={0} />
                    <Column lg={16} md={8} sm={4}>
                      <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
                    </Column>
                  </>
                );
              })}
            <Column lg={2} md={8} sm={4}>
              <TextInput
                id="blocksToAdd"
                labelText={intl.formatMessage({
                  id: "pathology.label.block.add.number",
                })}
                hideLabel={true}
                placeholder={intl.formatMessage({
                  id: "pathology.label.block.add.number",
                })}
                value={blocksToAdd}
                type="number"
                onChange={(e) => {
                  setBlocksToAdd(e.target.value);
                }}
              />
            </Column>
            <Column lg={14} md={8} sm={4}>
              <Button
                onClick={() => {
                  const maxBlockNumber = pathologySampleInfo.blocks.reduce(
                    (max, block) => {
                      const blockNumber = block.blockNumber || 0;
                      return Math.ceil(Math.max(max, blockNumber));
                    },
                    0,
                  );

                  var allBlocks = pathologySampleInfo.blocks || [];
                  Array.from({ length: blocksToAdd }, (_, index) => {
                    allBlocks.push({
                      id: "",
                      blockNumber: maxBlockNumber + 1 + index,
                    });
                  });

                  setPathologySampleInfo({
                    ...pathologySampleInfo,
                    blocks: allBlocks,
                  });
                }}
              >
                <FormattedMessage id="pathology.label.addblock" />
              </Button>
            </Column>
          </Grid>
        </Column>

        <Column lg={16} md={8} sm={4}>
          <Grid fullWidth={true} className="gridBoundary">
            <Column lg={16} md={8} sm={4}>
              <h5>
                <FormattedMessage id="pathology.label.slides" />
              </h5>
              <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
            </Column>

            {pathologySampleInfo.slides &&
              pathologySampleInfo.slides.map((slide, index) => {
                return (
                  <>
                    <Column lg={2} md={8} sm={4}>
                      <IconButton
                        label={intl.formatMessage({
                          id: "label.button.remove.slide",
                        })}
                        onClick={() => {
                          var info = { ...pathologySampleInfo };
                          info["slides"].splice(index, 1);
                          setPathologySampleInfo(info);
                        }}
                        kind="tertiary"
                        size="sm"
                      >
                        <Subtract size={18} />{" "}
                        <FormattedMessage id="pathology.label.slide" />
                      </IconButton>
                    </Column>
                    <Column lg={3} md={2} sm={1} key={index}>
                      <TextInput
                        id="slideNumber"
                        labelText={intl.formatMessage({
                          id: "pathology.label.slide.number",
                        })}
                        hideLabel={true}
                        placeholder={intl.formatMessage({
                          id: "pathology.label.slide.number",
                        })}
                        value={slide.slideNumber}
                        type="number"
                        onChange={(e) => {
                          var newSlides = [...pathologySampleInfo.slides];
                          newSlides[index].slideNumber = e.target.value;
                          setPathologySampleInfo({
                            ...pathologySampleInfo,
                            slides: newSlides,
                          });
                        }}
                      />
                    </Column>
                    <Column lg={3} md={2} sm={1}>
                      <TextInput
                        id="location"
                        labelText={intl.formatMessage({
                          id: "pathology.label.location",
                        })}
                        hideLabel={true}
                        placeholder={intl.formatMessage({
                          id: "pathology.label.location",
                        })}
                        value={slide.location}
                        onChange={(e) => {
                          var newSlides = [...pathologySampleInfo.slides];
                          newSlides[index].location = e.target.value;
                          setPathologySampleInfo({
                            ...pathologySampleInfo,
                            slides: newSlides,
                          });
                        }}
                      />
                    </Column>
                    <Column lg={3} md={1} sm={2}>
                      <FileUploader
                        style={{ marginTop: "-10px" }}
                        buttonLabel={
                          <FormattedMessage id="label.button.uploadfile" />
                        }
                        iconDescription="file upload"
                        multiple={false}
                        accept={["image/jpeg", "image/png", "application/pdf"]}
                        disabled={false}
                        name=""
                        buttonKind="primary"
                        size="lg"
                        filenameStatus="edit"
                        onChange={async (e) => {
                          e.preventDefault();
                          let file = e.target.files[0];
                          var newSlides = [...pathologySampleInfo.slides];
                          let encodedFile = await toBase64(file);
                          newSlides[index].base64Image = encodedFile;
                          setPathologySampleInfo({
                            ...pathologySampleInfo,
                            slides: newSlides,
                          });
                        }}
                        onClick={function noRefCheck() {}}
                        onDelete={(e) => {
                          e.preventDefault();
                        }}
                      />
                    </Column>
                    <Column lg={3} md={1} sm={2}>
                      {pathologySampleInfo.slides[index].image && (
                        <>
                          <Button
                            onClick={() => {
                              var win = window.open();
                              win.document.write(
                                '<iframe src="' +
                                  slide.fileType +
                                  ";base64," +
                                  slide.image +
                                  '" frameborder="0" style="border:0; top:0px; left:0px; bottom:0px; right:0px; width:100%; height:100%;" allowfullscreen></iframe>',
                              );
                            }}
                          >
                            <Launch />{" "}
                            <FormattedMessage id="pathology.label.view" />
                          </Button>
                        </>
                      )}
                    </Column>
                    <Column lg={2} md={1} sm={2}>
                      <Button
                        onClick={(e) => {
                          window.open(
                            config.serverBaseUrl +
                              "/LabelMakerServlet?labelType=slide&code=" +
                              slide.slideNumber,
                            "_blank",
                          );
                        }}
                      >
                        <FormattedMessage id="pathology.label.printlabel" />
                      </Button>
                    </Column>
                  </>
                );
              })}

            <Column lg={2} md={8} sm={4}>
              <TextInput
                id="slidesToAdd"
                labelText={intl.formatMessage({
                  id: "pathology.label.slide.add.number",
                })}
                hideLabel={true}
                placeholder={intl.formatMessage({
                  id: "pathology.label.slide.add.number",
                })}
                value={slidesToAdd}
                type="number"
                onChange={(e) => {
                  setSlidesToAdd(e.target.value);
                }}
              />
            </Column>
            <Column lg={14} md={8} sm={4}>
              <Button
                onClick={() => {
                  const maxSlideNumber = pathologySampleInfo.slides.reduce(
                    (max, slide) => {
                      const slideNumber = slide.slideNumber || 0;
                      return Math.ceil(Math.max(max, slideNumber));
                    },
                    0,
                  );

                  var allSlides = pathologySampleInfo.slides || [];
                  Array.from({ length: slidesToAdd }, (_, index) => {
                    allSlides.push({
                      id: "",
                      slideNumber: maxSlideNumber + 1 + index,
                    });
                  });

                  setPathologySampleInfo({
                    ...pathologySampleInfo,
                    slides: allSlides,
                  });
                }}
              >
                <FormattedMessage id="pathology.label.addslide" />
              </Button>
            </Column>
          </Grid>
        </Column>

        <Column lg={16} md={8} sm={4}></Column>
        {hasRole(userSessionDetails, "Pathologist") && (
          <>
            <Column lg={16} md={8} sm={4}>
              <Grid fullWidth={true} className="gridBoundary">
                <Column lg={4} md={8} sm={4}>
                  {initialMount && (
                    <FilterableMultiSelect
                      id="techniques"
                      titleText={
                        <FormattedMessage id="pathology.label.techniques" />
                      }
                      items={techniques}
                      itemToString={(item) => (item ? item.value : "")}
                      initialSelectedItems={pathologySampleInfo.techniques}
                      onChange={(changes) => {
                        setPathologySampleInfo({
                          ...pathologySampleInfo,
                          techniques: changes.selectedItems,
                        });
                      }}
                      selectionFeedback="top-after-reopen"
                    />
                  )}
                </Column>
                <Column lg={16} md={8} sm={4}>
                  {pathologySampleInfo.techniques &&
                    pathologySampleInfo.techniques.map((technique, index) => (
                      <Tag
                        key={index}
                        filter
                        onClose={() => {
                          var info = { ...pathologySampleInfo };
                          info["techniques"].splice(index, 1);
                          setPathologySampleInfo(info);
                        }}
                      >
                        {technique.value}
                      </Tag>
                    ))}
                </Column>
              </Grid>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <Grid fullWidth={true} className="gridBoundary">
                <Column lg={4} md={8} sm={4}>
                  {initialMount && (
                    <FilterableMultiSelect
                      id="requests"
                      titleText={
                        <FormattedMessage id="pathology.label.request" />
                      }
                      items={requests}
                      itemToString={(item) => (item ? item.value : "")}
                      initialSelectedItems={pathologySampleInfo.requests}
                      onChange={(changes) => {
                        setPathologySampleInfo({
                          ...pathologySampleInfo,
                          requests: changes.selectedItems,
                        });
                      }}
                      selectionFeedback="top-after-reopen"
                    />
                  )}
                </Column>
                <Column lg={12} md={4} sm={2} />
                <Column lg={16} md={8} sm={4}>
                  <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
                </Column>
                {pathologySampleInfo.requests &&
                  pathologySampleInfo.requests.map((request, index) => (
                    <>
                      <Column lg={2} md={4} sm={2}>
                        <Tag
                          key={index}
                          filter
                          onClose={() => {
                            var info = { ...pathologySampleInfo };
                            info["requests"].splice(index, 1);
                            setPathologySampleInfo(info);
                          }}
                        >
                          {request.value}
                        </Tag>
                      </Column>
                      <Column lg={2} md={8} sm={4}>
                        <Select
                          id={"requeststatus" + index}
                          name="requeststatus"
                          labelText={
                            <FormattedMessage id="label.button.select.status" />
                          }
                          value={request.status ? request.status : "OPENED"}
                          onChange={(e) => {
                            var newRequests = [...pathologySampleInfo.requests];
                            newRequests[index].status = e.target.value;
                            setPathologySampleInfo({
                              ...pathologySampleInfo,
                              requests: newRequests,
                            });
                          }}
                        >
                          <SelectItem />
                          {requestStatuses.map((status, index) => {
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
                      <Column lg={12} md={4} sm={2} />
                      <Column lg={16} md={8} sm={4}>
                        <div> &nbsp; </div>
                      </Column>
                    </>
                  ))}
              </Grid>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <Grid fullWidth={true} className="gridBoundary">
                <Column lg={16} md={8} sm={4}>
                  <TextArea
                    labelText={
                      <FormattedMessage id="pathology.label.grossexam" />
                    }
                    value={pathologySampleInfo.grossExam}
                    onChange={(e) => {
                      setPathologySampleInfo({
                        ...pathologySampleInfo,
                        grossExam: e.target.value,
                      });
                    }}
                  />
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <TextArea
                    labelText={
                      <FormattedMessage id="pathology.label.microexam" />
                    }
                    value={pathologySampleInfo.microscopyExam}
                    onChange={(e) => {
                      setPathologySampleInfo({
                        ...pathologySampleInfo,
                        microscopyExam: e.target.value,
                      });
                    }}
                  />
                </Column>
              </Grid>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <Grid fullWidth={true} className="gridBoundary">
                {pagination && (
                  <Column lg={16} md={8} sm={4}>
                    <Link>
                      {currentApiPage} / {totalApiPages}
                    </Link>
                    <div style={{ display: "flex", gap: "10px" }}>
                      <Button
                        hasIconOnly
                        iconDescription="previous"
                        disabled={previousPage != null ? false : true}
                        onClick={loadPreviousConclusionsPage}
                        renderIcon={ArrowLeft}
                        size="sm"
                      />
                      <Button
                        hasIconOnly
                        iconDescription="next"
                        disabled={nextPage != null ? false : true}
                        renderIcon={ArrowRight}
                        onClick={loadNextCOnclusionsPage}
                        size="sm"
                      />
                    </div>
                  </Column>
                )}
                <Column lg={16}>
                  <br />
                  <br />
                </Column>
                <Column lg={4} md={8} sm={4}>
                  {initialMount && (
                    <FilterableMultiSelect
                      id="conclusion"
                      titleText={
                        <FormattedMessage id="pathology.label.conclusion" />
                      }
                      items={conclusions}
                      itemToString={(item) => (item ? item.value : "")}
                      initialSelectedItems={pathologySampleInfo.conclusions}
                      onChange={(changes) => {
                        setPathologySampleInfo({
                          ...pathologySampleInfo,
                          conclusions: changes.selectedItems,
                        });
                      }}
                      selectionFeedback="top-after-reopen"
                    />
                  )}
                </Column>
                <Column lg={12} md={8} sm={4}>
                  {pathologySampleInfo.conclusions &&
                    pathologySampleInfo.conclusions.map((conclusion, index) => (
                      <Tag
                        key={index}
                        filter
                        onClose={() => {
                          var info = { ...pathologySampleInfo };
                          info["conclusions"].splice(index, 1);
                          setPathologySampleInfo(info);
                        }}
                      >
                        {conclusion.value}
                      </Tag>
                    ))}
                </Column>
              </Grid>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <Grid fullWidth={true} className="gridBoundary">
                <Column lg={16} md={8} sm={4}>
                  <TextArea
                    labelText={
                      <FormattedMessage id="pathology.label.textconclusion" />
                    }
                    value={pathologySampleInfo.conclusionText}
                    onChange={(e) => {
                      setPathologySampleInfo({
                        ...pathologySampleInfo,
                        conclusionText: e.target.value,
                      });
                    }}
                  />
                </Column>
              </Grid>
            </Column>
          </>
        )}
        <Column lg={16} md={8} sm={4}>
          <Grid fullWidth={true} className="gridBoundary">
            <Column lg={4} md={4} sm={2}>
              <Checkbox
                labelText={intl.formatMessage({ id: "pathology.label.refer" })}
                id="referToImmunoHistoChemistry"
                onChange={() => {
                  setPathologySampleInfo({
                    ...pathologySampleInfo,
                    referToImmunoHistoChemistry:
                      !pathologySampleInfo.referToImmunoHistoChemistry,
                  });
                }}
              />
            </Column>
            {pathologySampleInfo.referToImmunoHistoChemistry && (
              <>
                <Column lg={4} md={4} sm={2}>
                  <FilterableMultiSelect
                    id="ihctests"
                    titleText={
                      <FormattedMessage id="label.button.select.test" />
                    }
                    items={immunoHistoChemistryTests}
                    itemToString={(item) => (item ? item.value : "")}
                    onChange={(changes) => {
                      setPathologySampleInfo({
                        ...pathologySampleInfo,
                        immunoHistoChemistryTestIds: changes.selectedItems,
                      });
                    }}
                    selectionFeedback="top-after-reopen"
                  />
                </Column>
                <Column lg={8} md={4} sm={2}>
                  {pathologySampleInfo.immunoHistoChemistryTestIds &&
                    pathologySampleInfo.immunoHistoChemistryTestIds.map(
                      (test, index) => (
                        <Tag
                          key={index}
                          filter
                          onClose={() => {
                            var info = { ...pathologySampleInfo };
                            info["immunoHistoChemistryTestIds"].splice(
                              index,
                              1,
                            );
                            setPathologySampleInfo(info);
                          }}
                        >
                          {test.value}
                        </Tag>
                      ),
                    )}
                </Column>
              </>
            )}
          </Grid>
        </Column>
        {pathologySampleInfo.assignedPathologistId &&
          pathologySampleInfo.assignedTechnicianId && (
            <Column lg={16} md={8} sm={4}>
              <Checkbox
                labelText={intl.formatMessage({
                  id: "pathology.label.release",
                })}
                id="release"
                onChange={() => {
                  setPathologySampleInfo({
                    ...pathologySampleInfo,
                    release: !pathologySampleInfo.release,
                  });
                }}
              />
            </Column>
          )}
        <Column lg={16}>
          <Button
            id="pathology_save2"
            disabled={isSubmitting}
            onClick={(e) => {
              e.preventDefault();
              save(e);
            }}
          >
            <FormattedMessage id="label.button.save" />
          </Button>
        </Column>
      </Grid>
    </>
  );
}

export default PathologyCaseView;
