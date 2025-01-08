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
  Loading,
  RadioButtonGroup,
  RadioButton,
  InlineLoading,
} from "@carbon/react";
import { Launch, Subtract } from "@carbon/react/icons";
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
import ConfirmPopup from "../common/ConfirmPopup";
import PatientHeader from "../common/PatientHeader";
import QuestionnaireResponse from "../common/QuestionnaireResponse";
import "../pathology/PathologyDashboard.css";
import PageBreadCrumb from "../common/PageBreadCrumb";
let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "cytology.label.dashboard", link: "/CytologyDashboard" },
];

function CytologyCaseView() {
  const componentMounted = useRef(false);

  const { cytologySampleId } = useParams();

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);

  const intl = useIntl();

  const [pathologySampleInfo, setPathologySampleInfo] = useState({});

  const [initialMount, setInitialMount] = useState(false);

  const [statuses, setStatuses] = useState([]);
  const [satisfactoryForEvaluation, setSatisfactoryForEvaluation] = useState(
    [],
  );
  const [unSatisfactoryForEvaluation, setUnSatisfactoryForEvaluation] =
    useState([]);
  const [adequacySatisfactionList, setAdequacySatisfactionList] = useState([]);
  const [
    diagnosisResultEpithelialCellSquamous,
    setDiagnosisResultEpithelialCellSquamous,
  ] = useState([]);
  const [
    diagnosisResultEpithelialCellGlandular,
    setDiagnosisResultEpithelialCellGlandular,
  ] = useState([]);
  const [
    diagnosisResultNonNeoPlasticCellular,
    setDiagnosisResultNonNeoPlasticCellular,
  ] = useState([]);
  const [diagnosisResultReactiveCellular, setDiagnosisResultReactiveCellular] =
    useState([]);
  const [diagnosisResultOrganisms, setDiagnosisResultOrganisms] = useState([]);
  const [diagnosisResultOther, setDiagnosisResultOther] = useState([]);
  const [combinedDiagnoses, setcombinedDiagnoses] = useState(true);
  const [technicianUsers, setTechnicianUsers] = useState([]);
  const [pathologistUsers, setPathologistUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadingReport, setLoadingReport] = useState(false);
  const [reportTypes, setReportTypes] = useState([]);
  const [slidesToAdd, setSlidesToAdd] = useState(1);
  const [isConfirmOpen, setConfirmOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [reportParams, setReportParams] = useState({
    0: {
      submited: false,
      reportLink: "",
    },
  });

  const handleConfirm = () => {
    var diagnosis = { ...pathologySampleInfo.diagnosis };
    diagnosis.negativeDiagnosis = true;
    diagnosis.diagnosisResultsMaps = [];
    setPathologySampleInfo({
      ...pathologySampleInfo,
      diagnosis: diagnosis,
    });
    setConfirmOpen(false);
  };

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
        message: intl.formatMessage({ id: "save.success" }),
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
    let specimenAdequacy = null;
    if (pathologySampleInfo.specimenAdequacy) {
      specimenAdequacy = pathologySampleInfo.specimenAdequacy;
      if (pathologySampleInfo.specimenAdequacy.values) {
        specimenAdequacy = {
          ...specimenAdequacy,
          values: pathologySampleInfo.specimenAdequacy.values.map((e) => {
            return e.id;
          }),
        };
      }
    }

    let diagnosis = null;
    if (pathologySampleInfo.diagnosis) {
      diagnosis = pathologySampleInfo.diagnosis;
      if (
        !pathologySampleInfo.diagnosis.negativeDiagnosis &&
        pathologySampleInfo.diagnosis.diagnosisResultsMaps
      ) {
        var newDiagnosisResultsMaps = [];
        pathologySampleInfo.diagnosis.diagnosisResultsMaps.forEach(
          (resultMap) => {
            var newResultMap = resultMap;
            var results = filterDiagnosisResultsByCategory(
              resultMap.category,
              resultMap.resultType,
            ).results.map((e) => {
              return e.id;
            });
            newResultMap.results = results;
            newDiagnosisResultsMaps.push(newResultMap);
          },
        );
        diagnosis = {
          ...diagnosis,
          diagnosisResultsMaps: newDiagnosisResultsMaps,
        };
      }
    }

    let submitValues = {
      assignedTechnicianId: pathologySampleInfo.assignedTechnicianId,
      assignedCytoPathologistId: pathologySampleInfo.assignedPathologistId,
      status: pathologySampleInfo.status,
      slides: pathologySampleInfo.slides,
      reports: pathologySampleInfo.reports,
      release:
        pathologySampleInfo.release != undefined
          ? pathologySampleInfo.release
          : false,
    };

    if (specimenAdequacy) {
      submitValues = {
        ...submitValues,
        specimenAdequacy: specimenAdequacy,
      };
    }

    if (diagnosis) {
      submitValues = {
        ...submitValues,
        diagnosis: diagnosis,
      };
    }
    console.group("submitting");
    console.debug(" ..submit....");
    console.debug(JSON.stringify(submitValues));
    console.groupEnd();
    postToOpenElisServerFullResponse(
      "/rest/cytology/caseView/" + cytologySampleId,
      JSON.stringify(submitValues),
      displayStatus,
    );
  };
  const filterDiagnosisResultsByCategory = (category, type) => {
    return pathologySampleInfo.diagnosis?.diagnosisResultsMaps?.find(
      (r) => r.category === category && r.resultType === type,
    );
  };

  const setReportTypeList = (reportTypes) => {
    if (componentMounted.current) {
      setReportTypes(reportTypes);
    }
  };

  const setInitialPathologySampleInfo = (e) => {
    if (
      hasRole(userSessionDetails, "CytoPathologist") &&
      !e.assignedPathologistId &&
      e.status === "READY_FOR_CYTOPATHOLOGIST"
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

  const combineDiagnoses = () => {
    if (
      diagnosisResultEpithelialCellGlandular &&
      diagnosisResultEpithelialCellSquamous
    ) {
      var diagnoses = diagnosisResultEpithelialCellGlandular.concat(
        diagnosisResultEpithelialCellSquamous,
      );
      setcombinedDiagnoses(diagnoses);
    }
  };

  useEffect(() => {
    combineDiagnoses();
  }, [
    diagnosisResultEpithelialCellGlandular,
    diagnosisResultEpithelialCellSquamous,
  ]);

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/displayList/CYTOLOGY_STATUS", setStatuses);
    getFromOpenElisServer(
      "/rest/displayList/CYTOLOGY_SATISFACTORY_FOR_EVALUATION",
      setSatisfactoryForEvaluation,
    );
    getFromOpenElisServer(
      "/rest/displayList/CYTOLOGY_UN_SATISFACTORY_FOR_EVALUATION",
      setUnSatisfactoryForEvaluation,
    );
    getFromOpenElisServer(
      "/rest/displayList/CYTOLOGY_SPECIMEN_ADEQUACY_SATISFACTION",
      setAdequacySatisfactionList,
    );
    getFromOpenElisServer(
      "/rest/displayList/CYTOLOGY_DIAGNOSIS_RESULT_EPITHELIAL_CELL_SQUAMOUS",
      setDiagnosisResultEpithelialCellSquamous,
    );
    getFromOpenElisServer(
      "/rest/displayList/CYTOLOGY_DIAGNOSIS_RESULT_EPITHELIAL_CELL_GLANDULAR",
      setDiagnosisResultEpithelialCellGlandular,
    );
    getFromOpenElisServer(
      "/rest/displayList/CYTOLOGY_DIAGNOSIS_RESULT_NON_NEO_PLASTIC_CELLULAR",
      setDiagnosisResultNonNeoPlasticCellular,
    );
    getFromOpenElisServer(
      "/rest/displayList/CYTOLOGY_DIAGNOSIS_RESULT_REACTIVE_CELLULAR",
      setDiagnosisResultReactiveCellular,
    );
    getFromOpenElisServer(
      "/rest/displayList/CYTOLOGY_DIAGNOSIS_RESULT_ORGANISMS",
      setDiagnosisResultOrganisms,
    );
    getFromOpenElisServer(
      "/rest/displayList/CYTOLOGY_DIAGNOSIS_RESULT_OTHER",
      setDiagnosisResultOther,
    );
    getFromOpenElisServer(
      "/rest/displayList/CYTOLOGY_REPORT_TYPES",
      setReportTypeList,
    );
    //TODO make conclusions list instead of reusing pathrequest
    getFromOpenElisServer("/rest/users", setTechnicianUsers);
    getFromOpenElisServer("/rest/users/Cytopathologist", setPathologistUsers);
    getFromOpenElisServer(
      "/rest/cytology/caseView/" + cytologySampleId,
      setInitialPathologySampleInfo,
    );

    return () => {
      componentMounted.current = false;
    };
  }, []);

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="cytology.label.title" />
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
        <ConfirmPopup
          isOpen={isConfirmOpen}
          onClose={() => setConfirmOpen(false)}
          onConfirm={handleConfirm}
          messageCode="cytology.label.confirmSelect"
        />
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
            labelText={intl.formatMessage({ id: "label.filters.status" })}
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
        <Column lg={2} md={1} sm={2}></Column>
        <Column lg={2} md={1} sm={2}>
          <Select
            id="assignedTechnician"
            name="assignedTechnician"
            labelText={intl.formatMessage({ id: "assigned.technician.label" })}
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
        <Column lg={2} md={4} sm={2} />
        <Column lg={4} md={2} sm={2}>
          <Select
            id="assignedPathologist"
            name="assignedPathologist"
            labelText={intl.formatMessage({
              id: "assigned.cytopathologist.label",
            })}
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
        <Column lg={16} md={8} sm={4}>
          <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
        </Column>
        <Column lg={16} md={8} sm={4}>
          <Grid fullWidth={true} className="gridBoundary">
            <Column lg={16} md={8} sm={4}>
              <h5>
                {" "}
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
                        labelText={
                          <FormattedMessage id="pathology.label.slide.number" />
                        }
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
                        labelText={
                          <FormattedMessage id="pathology.label.location" />
                        }
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
                    <Column lg={2} md={1} sm={2}>
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
                        {" "}
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

        <Column lg={16} md={8} sm={4}>
          <Grid fullWidth={true} className="gridBoundary">
            <Column lg={4} md={2} sm={2}>
              <Select
                id="report"
                name="report"
                labelText={
                  <FormattedMessage id="immunohistochemistry.label.addreport" />
                }
                onChange={(event) => {
                  setPathologySampleInfo({
                    ...pathologySampleInfo,
                    reports: [
                      ...(pathologySampleInfo.reports || []),
                      { id: "", reportType: event.target.value },
                    ],
                  });
                }}
              >
                <SelectItem
                  disabled
                  value="ADD"
                  text={intl.formatMessage({
                    id: "immunohistochemistry.label.addreport",
                  })}
                />
                {reportTypes.map((report, index) => {
                  return (
                    <SelectItem
                      key={index}
                      text={report.value}
                      value={report.id}
                    />
                  );
                })}
              </Select>
            </Column>
            <Column lg={12} md={2} sm={2}></Column>
            <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
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
                    <Column lg={2} md={8} sm={4}>
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

                    <Column lg={3} md={1} sm={2}>
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
                    <Column lg={4}>
                      <h6>
                        {
                          reportTypes.filter(
                            (type) => type.id === report.reportType,
                          )[0]?.value
                        }
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
                        id={"generate_report_" + index}
                        onClick={(e) => {
                          setLoadingReport(true);
                          const form = {
                            report: "PatientCytologyReport",
                            programSampleId: cytologySampleId,
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
                    <Column lg={2} md={2} sm={2} />
                    <Column lg={16} md={8} sm={4}>
                      <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
                    </Column>
                  </>
                );
              })}
          </Grid>
        </Column>

        <Column lg={16} md={8} sm={4}></Column>
        {hasRole(userSessionDetails, "Cytopathologist") && initialMount && (
          <>
            <Column lg={16} md={8} sm={4}>
              <Grid fullWidth={true} className="gridBoundary">
                <Column lg={4} md={1} sm={2}>
                  <Select
                    id="specimenAdequacy"
                    name="specimenAdequacy"
                    labelText={
                      <FormattedMessage id="cytology.label.specimen" />
                    }
                    value={pathologySampleInfo.specimenAdequacy?.satisfaction}
                    onChange={(event) => {
                      var specimenAdequacy = {
                        ...pathologySampleInfo.specimenAdequacy,
                      };
                      specimenAdequacy.satisfaction = event.target.value;
                      specimenAdequacy.resultType = "DICTIONARY";
                      specimenAdequacy.values = [];
                      setPathologySampleInfo({
                        ...pathologySampleInfo,
                        specimenAdequacy: specimenAdequacy,
                      });
                    }}
                  >
                    <SelectItem />
                    {adequacySatisfactionList.map((user, index) => {
                      return (
                        <SelectItem
                          key={index}
                          text={user.value}
                          value={user.id}
                        />
                      );
                    })}
                  </Select>
                </Column>
                {pathologySampleInfo.specimenAdequacy &&
                  pathologySampleInfo.specimenAdequacy.satisfaction ===
                    "UN_SATISFACTORY_FOR_EVALUATION" && (
                    <>
                      <Column lg={4} md={4} sm={2}>
                        {initialMount && (
                          <FilterableMultiSelect
                            id="adequacy"
                            titleText={
                              <FormattedMessage id="label.button.select" />
                            }
                            items={unSatisfactoryForEvaluation}
                            itemToString={(item) => (item ? item.value : "")}
                            initialSelectedItems={
                              pathologySampleInfo.specimenAdequacy?.values
                            }
                            onChange={(changes) => {
                              var specimenAdequacy = {
                                ...pathologySampleInfo.specimenAdequacy,
                              };
                              specimenAdequacy.values = changes.selectedItems;
                              setPathologySampleInfo({
                                ...pathologySampleInfo,
                                specimenAdequacy: specimenAdequacy,
                              });
                            }}
                            selectionFeedback="top-after-reopen"
                          />
                        )}
                      </Column>
                      <Column lg={8} md={4} sm={2}>
                        {pathologySampleInfo.specimenAdequacy &&
                          pathologySampleInfo.specimenAdequacy.values.map(
                            (adequacy, index) => (
                              <Tag key={index} onClose={() => {}}>
                                {adequacy.value}
                              </Tag>
                            ),
                          )}
                      </Column>
                    </>
                  )}
                {pathologySampleInfo.specimenAdequacy?.satisfaction ===
                  "SATISFACTORY_FOR_EVALUATION" && (
                  <Column lg={8}>
                    <RadioButtonGroup
                      valueSelected={
                        pathologySampleInfo.specimenAdequacy?.values[0]?.id
                      }
                      legendText={intl.formatMessage({
                        id: "label.button.select",
                      })}
                      name="adequacy"
                      id="adequacy"
                      onChange={(value) => {
                        var specimenAdequacy = {
                          ...pathologySampleInfo.specimenAdequacy,
                        };
                        specimenAdequacy.values = [{ id: value }];
                        setPathologySampleInfo({
                          ...pathologySampleInfo,
                          specimenAdequacy: specimenAdequacy,
                        });
                      }}
                    >
                      {satisfactoryForEvaluation.map((adequacy, index) => (
                        <RadioButton
                          key={index}
                          index={index}
                          id={"adquacy" + index}
                          labelText={adequacy.value}
                          value={adequacy.id}
                        />
                      ))}
                    </RadioButtonGroup>
                  </Column>
                )}
              </Grid>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
            </Column>

            <Column lg={16} md={8} sm={4}>
              <Checkbox
                checked={
                  pathologySampleInfo.diagnosis
                    ? pathologySampleInfo.diagnosis.negativeDiagnosis
                    : true
                }
                labelText={intl.formatMessage({
                  id: "cytology.label.negative",
                })}
                id="checked"
                onChange={(e) => {
                  if (e.target.checked) {
                    setConfirmOpen(true);
                  } else {
                    var diagnosis = { ...pathologySampleInfo.diagnosis };
                    diagnosis.negativeDiagnosis = e.target.checked;
                    diagnosis.diagnosisResultsMaps = [];
                    setPathologySampleInfo({
                      ...pathologySampleInfo,
                      diagnosis: diagnosis,
                    });
                  }
                }}
              />
            </Column>
            {pathologySampleInfo.diagnosis &&
              !pathologySampleInfo.diagnosis.negativeDiagnosis && (
                <>
                  <Column lg={16} md={8} sm={4}>
                    <Grid fullWidth={true} className="gridBoundary">
                      <Column lg={16} md={8} sm={4}>
                        <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
                        <FormattedMessage id="cytology.label.cellabnomality" />
                      </Column>
                      <Column lg={4} md={8} sm={4}>
                        <FilterableMultiSelect
                          id="cellAbnomality"
                          titleText={
                            <FormattedMessage id="label.button.select" />
                          }
                          items={combinedDiagnoses}
                          itemToString={(item) => (item ? item.value : "")}
                          initialSelectedItems={
                            filterDiagnosisResultsByCategory(
                              "EPITHELIAL_CELL_ABNORMALITY",
                              "DICTIONARY",
                            )?.results
                          }
                          onChange={(changes) => {
                            var diagnosis = {
                              ...pathologySampleInfo.diagnosis,
                            };
                            var diagnosisResultsMaps =
                              diagnosis.diagnosisResultsMaps;
                            var filteredMapIndex =
                              diagnosisResultsMaps?.findIndex(
                                (r) =>
                                  r.category ===
                                    "EPITHELIAL_CELL_ABNORMALITY" &&
                                  r.resultType === "DICTIONARY",
                              );
                            var diagnosisResultMap = {};
                            var newDiagnosisResultMaps = [];
                            diagnosisResultMap.category =
                              "EPITHELIAL_CELL_ABNORMALITY";
                            diagnosisResultMap.resultType = "DICTIONARY";
                            diagnosisResultMap.results = changes.selectedItems;

                            if (filteredMapIndex != -1) {
                              diagnosisResultsMaps[filteredMapIndex] =
                                diagnosisResultMap;
                              newDiagnosisResultMaps = diagnosisResultsMaps;
                            } else {
                              if (!diagnosisResultsMaps) {
                                diagnosisResultsMaps = [];
                              }
                              newDiagnosisResultMaps = [
                                ...diagnosisResultsMaps,
                                diagnosisResultMap,
                              ];
                            }
                            diagnosis.diagnosisResultsMaps =
                              newDiagnosisResultMaps;
                            setPathologySampleInfo({
                              ...pathologySampleInfo,
                              diagnosis: diagnosis,
                            });
                          }}
                          selectionFeedback="top-after-reopen"
                        />
                      </Column>
                      {diagnosisResultEpithelialCellSquamous &&
                        pathologySampleInfo && (
                          <>
                            <Column lg={1} md={4} sm={2}>
                              <FormattedMessage id="cytology.label.squamous" />
                            </Column>
                            <Column lg={3} md={4} sm={2}>
                              {filterDiagnosisResultsByCategory(
                                "EPITHELIAL_CELL_ABNORMALITY",
                                "DICTIONARY",
                              )
                                ?.results.filter((result) =>
                                  diagnosisResultEpithelialCellSquamous?.some(
                                    (item) => item.id == result.id,
                                  ),
                                )
                                ?.map((result, index) => (
                                  <Tag
                                    key={index}
                                    filter
                                    onClose={() => {
                                      var diagnosisResultsMap =
                                        filterDiagnosisResultsByCategory(
                                          "EPITHELIAL_CELL_ABNORMALITY",
                                          "DICTIONARY",
                                        );
                                      var resultIndex =
                                        diagnosisResultsMap.results.findIndex(
                                          (r) => r.id == result.id,
                                        );
                                      diagnosisResultsMap["results"].splice(
                                        resultIndex,
                                        1,
                                      );
                                      var newDiagnosis = {
                                        ...pathologySampleInfo.diagnosis,
                                      };
                                      var newDiagnosisResultsMaps =
                                        newDiagnosis.diagnosisResultsMaps;
                                      var filteredMapIndex =
                                        newDiagnosisResultsMaps?.findIndex(
                                          (r) =>
                                            r.category ===
                                              "EPITHELIAL_CELL_ABNORMALITY" &&
                                            r.resultType === "DICTIONARY",
                                        );
                                      newDiagnosisResultsMaps[
                                        filteredMapIndex
                                      ] = diagnosisResultsMap;
                                      newDiagnosis.diagnosisResultsMaps =
                                        newDiagnosisResultsMaps;
                                      setPathologySampleInfo({
                                        ...pathologySampleInfo,
                                        diagnosis: newDiagnosis,
                                      });
                                    }}
                                  >
                                    {result.value}
                                  </Tag>
                                ))}
                            </Column>
                          </>
                        )}

                      {diagnosisResultEpithelialCellGlandular &&
                        pathologySampleInfo && (
                          <>
                            <Column lg={1} md={4} sm={2}>
                              <FormattedMessage id="cytology.label.glandular" />
                            </Column>
                            <Column lg={3} md={4} sm={2}>
                              {filterDiagnosisResultsByCategory(
                                "EPITHELIAL_CELL_ABNORMALITY",
                                "DICTIONARY",
                              )
                                ?.results.filter((result) =>
                                  diagnosisResultEpithelialCellGlandular?.some(
                                    (item) => item.id == result.id,
                                  ),
                                )
                                ?.map((result, index) => (
                                  <Tag
                                    key={index}
                                    filter
                                    onClose={() => {
                                      var diagnosisResultsMap =
                                        filterDiagnosisResultsByCategory(
                                          "EPITHELIAL_CELL_ABNORMALITY",
                                          "DICTIONARY",
                                        );
                                      var resultIndex =
                                        diagnosisResultsMap.results.findIndex(
                                          (r) => r.id == result.id,
                                        );
                                      diagnosisResultsMap["results"].splice(
                                        resultIndex,
                                        1,
                                      );
                                      var newDiagnosis = {
                                        ...pathologySampleInfo.diagnosis,
                                      };
                                      var newDiagnosisResultsMaps =
                                        newDiagnosis.diagnosisResultsMaps;
                                      var filteredMapIndex =
                                        newDiagnosisResultsMaps?.findIndex(
                                          (r) =>
                                            r.category ===
                                              "EPITHELIAL_CELL_ABNORMALITY" &&
                                            r.resultType === "DICTIONARY",
                                        );
                                      newDiagnosisResultsMaps[
                                        filteredMapIndex
                                      ] = diagnosisResultsMap;
                                      newDiagnosis.diagnosisResultsMaps =
                                        newDiagnosisResultsMaps;
                                      setPathologySampleInfo({
                                        ...pathologySampleInfo,
                                        diagnosis: newDiagnosis,
                                      });
                                    }}
                                  >
                                    {result.value}
                                  </Tag>
                                ))}
                            </Column>
                          </>
                        )}

                      <Column lg={4} md={4} sm={2}>
                        <FormattedMessage id="cytology.label.other" /> :
                        <TextInput
                          id="otherNeoPlasms"
                          labelText={intl.formatMessage({
                            id: "enterText.label",
                          })}
                          hideLabel={true}
                          placeholder={intl.formatMessage({
                            id: "otherMalignant.placeholder",
                          })}
                          value={
                            filterDiagnosisResultsByCategory(
                              "EPITHELIAL_CELL_ABNORMALITY",
                              "TEXT",
                            )?.results[0].value
                          }
                          onChange={(e) => {
                            var diagnosis = {
                              ...pathologySampleInfo.diagnosis,
                            };
                            var diagnosisResultsMaps =
                              diagnosis.diagnosisResultsMaps;
                            var filteredMapIndex =
                              diagnosisResultsMaps?.findIndex(
                                (r) =>
                                  r.category ===
                                    "EPITHELIAL_CELL_ABNORMALITY" &&
                                  r.resultType === "TEXT",
                              );
                            var diagnosisResultMap = {};
                            var newDiagnosisResultMaps = [];
                            diagnosisResultMap.category =
                              "EPITHELIAL_CELL_ABNORMALITY";
                            diagnosisResultMap.resultType = "TEXT";
                            diagnosisResultMap.results = [
                              { id: e.target.value, value: e.target.value },
                            ];

                            if (filteredMapIndex != -1) {
                              diagnosisResultsMaps[filteredMapIndex] =
                                diagnosisResultMap;
                              newDiagnosisResultMaps = diagnosisResultsMaps;
                            } else {
                              if (!diagnosisResultsMaps) {
                                diagnosisResultsMaps = [];
                              }
                              newDiagnosisResultMaps = [
                                ...diagnosisResultsMaps,
                                diagnosisResultMap,
                              ];
                            }
                            diagnosis.diagnosisResultsMaps =
                              newDiagnosisResultMaps;
                            setPathologySampleInfo({
                              ...pathologySampleInfo,
                              diagnosis: diagnosis,
                            });
                          }}
                        />
                      </Column>
                    </Grid>
                  </Column>
                  <Column lg={16} md={8} sm={4}>
                    <Grid fullWidth={true} className="gridBoundary">
                      <Column lg={16} md={8} sm={4}>
                        <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
                        <FormattedMessage id="cytology.label.neoplastic" />
                      </Column>
                      <Column lg={4} md={8} sm={4}>
                        <FilterableMultiSelect
                          id="nonNeoPlastic"
                          titleText={intl.formatMessage({
                            id: "selectResult.title",
                          })}
                          items={diagnosisResultNonNeoPlasticCellular}
                          itemToString={(item) => (item ? item.value : "")}
                          initialSelectedItems={
                            filterDiagnosisResultsByCategory(
                              "NON_NEOPLASTIC_CELLULAR_VARIATIONS",
                              "DICTIONARY",
                            )?.results
                          }
                          onChange={(changes) => {
                            var diagnosis = {
                              ...pathologySampleInfo.diagnosis,
                            };
                            var diagnosisResultsMaps =
                              diagnosis.diagnosisResultsMaps;
                            var filteredMapIndex =
                              diagnosisResultsMaps?.findIndex(
                                (r) =>
                                  r.category ===
                                  "NON_NEOPLASTIC_CELLULAR_VARIATIONS",
                              );
                            var diagnosisResultMap = {};
                            var newDiagnosisResultMaps = [];
                            diagnosisResultMap.category =
                              "NON_NEOPLASTIC_CELLULAR_VARIATIONS";
                            diagnosisResultMap.resultType = "DICTIONARY";
                            diagnosisResultMap.results = changes.selectedItems;

                            if (filteredMapIndex != -1) {
                              diagnosisResultsMaps[filteredMapIndex] =
                                diagnosisResultMap;
                              newDiagnosisResultMaps = diagnosisResultsMaps;
                            } else {
                              if (!diagnosisResultsMaps) {
                                diagnosisResultsMaps = [];
                              }
                              newDiagnosisResultMaps = [
                                ...diagnosisResultsMaps,
                                diagnosisResultMap,
                              ];
                            }
                            diagnosis.diagnosisResultsMaps =
                              newDiagnosisResultMaps;
                            setPathologySampleInfo({
                              ...pathologySampleInfo,
                              diagnosis: diagnosis,
                            });
                          }}
                          selectionFeedback="top-after-reopen"
                        />
                      </Column>
                      <Column lg={12} md={4} sm={2}>
                        {filterDiagnosisResultsByCategory(
                          "NON_NEOPLASTIC_CELLULAR_VARIATIONS",
                          "DICTIONARY",
                        )?.results.map((result, index) => (
                          <Tag
                            key={index}
                            filter
                            onClose={() => {
                              var diagnosisResultsMap =
                                filterDiagnosisResultsByCategory(
                                  "NON_NEOPLASTIC_CELLULAR_VARIATIONS",
                                  "DICTIONARY",
                                );
                              diagnosisResultsMap["results"].splice(index, 1);
                              var newDiagnosis = {
                                ...pathologySampleInfo.diagnosis,
                              };
                              var newDiagnosisResultsMaps =
                                newDiagnosis.diagnosisResultsMaps;
                              var filteredMapIndex =
                                newDiagnosisResultsMaps?.findIndex(
                                  (r) =>
                                    r.category ===
                                    "NON_NEOPLASTIC_CELLULAR_VARIATIONS",
                                );
                              newDiagnosisResultsMaps[filteredMapIndex] =
                                diagnosisResultsMap;
                              newDiagnosis.diagnosisResultsMaps =
                                newDiagnosisResultsMaps;
                              setPathologySampleInfo({
                                ...pathologySampleInfo,
                                diagnosis: newDiagnosis,
                              });
                            }}
                          >
                            {result.value}
                          </Tag>
                        ))}
                      </Column>
                    </Grid>
                  </Column>

                  <Column lg={16} md={8} sm={4}>
                    <Grid fullWidth={true} className="gridBoundary">
                      <Column lg={16} md={8} sm={4}>
                        <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
                        <FormattedMessage id="cytology.label.reactive" />
                      </Column>
                      <Column lg={4} md={8} sm={4}>
                        <FilterableMultiSelect
                          id="reactiveChanges"
                          titleText={intl.formatMessage({
                            id: "selectResult.title",
                          })}
                          items={diagnosisResultReactiveCellular}
                          itemToString={(item) => (item ? item.value : "")}
                          initialSelectedItems={
                            filterDiagnosisResultsByCategory(
                              "REACTIVE_CELLULAR_CHANGES",
                              "DICTIONARY",
                            )?.results
                          }
                          onChange={(changes) => {
                            var diagnosis = {
                              ...pathologySampleInfo.diagnosis,
                            };
                            var diagnosisResultsMaps =
                              diagnosis.diagnosisResultsMaps;
                            var filteredMapIndex =
                              diagnosisResultsMaps?.findIndex(
                                (r) =>
                                  r.category === "REACTIVE_CELLULAR_CHANGES",
                              );
                            var diagnosisResultMap = {};
                            var newDiagnosisResultMaps = [];
                            diagnosisResultMap.category =
                              "REACTIVE_CELLULAR_CHANGES";
                            diagnosisResultMap.resultType = "DICTIONARY";
                            diagnosisResultMap.results = changes.selectedItems;

                            if (filteredMapIndex != -1) {
                              diagnosisResultsMaps[filteredMapIndex] =
                                diagnosisResultMap;
                              newDiagnosisResultMaps = diagnosisResultsMaps;
                            } else {
                              if (!diagnosisResultsMaps) {
                                diagnosisResultsMaps = [];
                              }
                              newDiagnosisResultMaps = [
                                ...diagnosisResultsMaps,
                                diagnosisResultMap,
                              ];
                            }
                            diagnosis.diagnosisResultsMaps =
                              newDiagnosisResultMaps;
                            setPathologySampleInfo({
                              ...pathologySampleInfo,
                              diagnosis: diagnosis,
                            });
                          }}
                          selectionFeedback="top-after-reopen"
                        />
                      </Column>
                      <Column lg={12} md={4} sm={2}>
                        {filterDiagnosisResultsByCategory(
                          "REACTIVE_CELLULAR_CHANGES",
                          "DICTIONARY",
                        )?.results.map((result, index) => (
                          <Tag
                            key={index}
                            filter
                            onClose={() => {
                              var diagnosisResultsMap =
                                filterDiagnosisResultsByCategory(
                                  "REACTIVE_CELLULAR_CHANGES",
                                  "DICTIONARY",
                                );
                              diagnosisResultsMap["results"].splice(index, 1);
                              var newDiagnosis = {
                                ...pathologySampleInfo.diagnosis,
                              };
                              var newDiagnosisResultsMaps =
                                newDiagnosis.diagnosisResultsMaps;
                              var filteredMapIndex =
                                newDiagnosisResultsMaps?.findIndex(
                                  (r) =>
                                    r.category === "REACTIVE_CELLULAR_CHANGES",
                                );
                              newDiagnosisResultsMaps[filteredMapIndex] =
                                diagnosisResultsMap;
                              newDiagnosis.diagnosisResultsMaps =
                                newDiagnosisResultsMaps;
                              setPathologySampleInfo({
                                ...pathologySampleInfo,
                                diagnosis: newDiagnosis,
                              });
                            }}
                          >
                            {result.value}
                          </Tag>
                        ))}
                      </Column>
                    </Grid>
                  </Column>
                  <Column lg={16} md={8} sm={4}>
                    <Grid fullWidth={true} className="gridBoundary">
                      <Column lg={16} md={8} sm={4}>
                        <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
                        <FormattedMessage id="cytology.label.organisms" />
                      </Column>
                      <Column lg={4} md={8} sm={4}>
                        <FilterableMultiSelect
                          id="organisms"
                          titleText={intl.formatMessage({
                            id: "selectResult.title",
                          })}
                          items={diagnosisResultOrganisms}
                          itemToString={(item) => (item ? item.value : "")}
                          initialSelectedItems={
                            filterDiagnosisResultsByCategory(
                              "ORGANISMS",
                              "DICTIONARY",
                            )?.results
                          }
                          onChange={(changes) => {
                            var diagnosis = {
                              ...pathologySampleInfo.diagnosis,
                            };
                            var diagnosisResultsMaps =
                              diagnosis.diagnosisResultsMaps;
                            var filteredMapIndex =
                              diagnosisResultsMaps?.findIndex(
                                (r) => r.category === "ORGANISMS",
                              );
                            var diagnosisResultMap = {};
                            var newDiagnosisResultMaps = [];
                            diagnosisResultMap.category = "ORGANISMS";
                            diagnosisResultMap.resultType = "DICTIONARY";
                            diagnosisResultMap.results = changes.selectedItems;

                            if (filteredMapIndex != -1) {
                              diagnosisResultsMaps[filteredMapIndex] =
                                diagnosisResultMap;
                              newDiagnosisResultMaps = diagnosisResultsMaps;
                            } else {
                              if (!diagnosisResultsMaps) {
                                diagnosisResultsMaps = [];
                              }
                              newDiagnosisResultMaps = [
                                ...diagnosisResultsMaps,
                                diagnosisResultMap,
                              ];
                            }
                            diagnosis.diagnosisResultsMaps =
                              newDiagnosisResultMaps;
                            setPathologySampleInfo({
                              ...pathologySampleInfo,
                              diagnosis: diagnosis,
                            });
                          }}
                          selectionFeedback="top-after-reopen"
                        />
                      </Column>
                      <Column lg={12} md={4} sm={2}>
                        {filterDiagnosisResultsByCategory(
                          "ORGANISMS",
                          "DICTIONARY",
                        )?.results.map((result, index) => (
                          <Tag
                            key={index}
                            filter
                            onClose={() => {
                              var diagnosisResultsMap =
                                filterDiagnosisResultsByCategory(
                                  "ORGANISMS",
                                  "DICTIONARY",
                                );
                              diagnosisResultsMap["results"].splice(index, 1);
                              var newDiagnosis = {
                                ...pathologySampleInfo.diagnosis,
                              };
                              var newDiagnosisResultsMaps =
                                newDiagnosis.diagnosisResultsMaps;
                              var filteredMapIndex =
                                newDiagnosisResultsMaps?.findIndex(
                                  (r) => r.category === "ORGANISMS",
                                );
                              newDiagnosisResultsMaps[filteredMapIndex] =
                                diagnosisResultsMap;
                              newDiagnosis.diagnosisResultsMaps =
                                newDiagnosisResultsMaps;
                              setPathologySampleInfo({
                                ...pathologySampleInfo,
                                diagnosis: newDiagnosis,
                              });
                            }}
                          >
                            {result.value}
                          </Tag>
                        ))}
                      </Column>
                    </Grid>
                  </Column>
                  <Column lg={16} md={8} sm={4}>
                    <Grid fullWidth={true} className="gridBoundary">
                      <Column lg={16} md={8} sm={4}>
                        <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
                        <FormattedMessage id="cytology.label.otherResult" />
                      </Column>
                      <Column lg={4} md={8} sm={4}>
                        <FilterableMultiSelect
                          id="OTHER"
                          titleText={intl.formatMessage({
                            id: "selectResult.title",
                          })}
                          items={diagnosisResultOther}
                          itemToString={(item) => (item ? item.value : "")}
                          initialSelectedItems={
                            filterDiagnosisResultsByCategory(
                              "OTHER",
                              "DICTIONARY",
                            )?.results
                          }
                          onChange={(changes) => {
                            var diagnosis = {
                              ...pathologySampleInfo.diagnosis,
                            };
                            var diagnosisResultsMaps =
                              diagnosis.diagnosisResultsMaps;
                            var filteredMapIndex =
                              diagnosisResultsMaps?.findIndex(
                                (r) => r.category === "OTHER",
                              );
                            var diagnosisResultMap = {};
                            var newDiagnosisResultMaps = [];
                            diagnosisResultMap.category = "OTHER";
                            diagnosisResultMap.resultType = "DICTIONARY";
                            diagnosisResultMap.results = changes.selectedItems;

                            if (filteredMapIndex != -1) {
                              diagnosisResultsMaps[filteredMapIndex] =
                                diagnosisResultMap;
                              newDiagnosisResultMaps = diagnosisResultsMaps;
                            } else {
                              if (!diagnosisResultsMaps) {
                                diagnosisResultsMaps = [];
                              }
                              newDiagnosisResultMaps = [
                                ...diagnosisResultsMaps,
                                diagnosisResultMap,
                              ];
                            }
                            diagnosis.diagnosisResultsMaps =
                              newDiagnosisResultMaps;
                            setPathologySampleInfo({
                              ...pathologySampleInfo,
                              diagnosis: diagnosis,
                            });
                          }}
                          selectionFeedback="top-after-reopen"
                        />
                      </Column>
                      <Column lg={12} md={4} sm={2}>
                        {filterDiagnosisResultsByCategory(
                          "OTHER",
                          "DICTIONARY",
                        )?.results.map((result, index) => (
                          <Tag
                            key={index}
                            filter
                            onClose={() => {
                              var diagnosisResultsMap =
                                filterDiagnosisResultsByCategory(
                                  "OTHER",
                                  "DICTIONARY",
                                );
                              diagnosisResultsMap["results"].splice(index, 1);
                              var newDiagnosis = {
                                ...pathologySampleInfo.diagnosis,
                              };
                              var newDiagnosisResultsMaps =
                                newDiagnosis.diagnosisResultsMaps;
                              var filteredMapIndex =
                                newDiagnosisResultsMaps?.findIndex(
                                  (r) => r.category === "OTHER",
                                );
                              newDiagnosisResultsMaps[filteredMapIndex] =
                                diagnosisResultsMap;
                              newDiagnosis.diagnosisResultsMaps =
                                newDiagnosisResultsMaps;
                              setPathologySampleInfo({
                                ...pathologySampleInfo,
                                diagnosis: newDiagnosis,
                              });
                            }}
                          >
                            {result.value}
                          </Tag>
                        ))}
                      </Column>
                    </Grid>
                  </Column>
                </>
              )}
          </>
        )}
        {pathologySampleInfo.assignedPathologistId &&
          pathologySampleInfo.assignedTechnicianId && (
            <Column lg={16}>
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

export default CytologyCaseView;
