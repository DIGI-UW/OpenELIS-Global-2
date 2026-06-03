import React, { useContext, useEffect, useState } from "react";
import {
  Button,
  ProgressIndicator,
  ProgressStep,
  SkeletonText,
  Stack,
} from "@carbon/react";
import PatientInfo from "./PatientInfo";
import AddSample from "./AddSample";
import AddOrder from "./AddOrder";
import "./add-order.scss";
import { SampleOrderFormValues } from "../formModel/innitialValues/OrderEntryFormValues";
import { NotificationContext, ConfigurationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import {
  getFromOpenElisServer,
  postToOpenElisServerFormData,
  postToOpenElisServerJsonResponse,
  resolveApiErrorMessage,
} from "../utils/Utils";
import OrderEntryAdditionalQuestions from "./OrderEntryAdditionalQuestions";
import OrderSuccessMessage from "./OrderSuccessMessage";
import EQASampleEntry from "../eqa/EQASampleEntry";
import EQAOrderForm from "../eqa/EQAOrderForm";
import { FormattedMessage, useIntl } from "react-intl";
import { createOrderEntryValidationSchema } from "../formModel/validationSchema/OrderEntryValidationSchema";
import config from "../../config.json";
import PageBreadCrumb from "../common/PageBreadCrumb";
let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sidenav.label.addorder", link: "/SamplePatientEntry" },
];

// Step identifiers — decoupled from physical tab position
const STEP_PATIENT_INFO = "patient_info";
const STEP_PROGRAM = "program";
const STEP_ADD_SAMPLE = "add_sample";
const STEP_ADD_ORDER = "add_order";
const STEP_SUCCESS = "success";

const STEP_LABELS = {
  [STEP_PATIENT_INFO]: "order.step.patient.info",
  [STEP_PROGRAM]: "order.step.program.selection",
  [STEP_ADD_SAMPLE]: "sample.add.action",
  [STEP_ADD_ORDER]: "order.label.add",
};

export let sampleObject = {
  index: 0,
  sampleRejected: false,
  rejectionReason: "",
  sampleTypeId: "",
  sampleXML: null,
  panels: [],
  tests: [],
  requestReferralEnabled: false,
  referralItems: [],
};
const Index = () => {
  const intl = useIntl();

  const [changed, setChanged] = useState({
    "sampleOrderItems.providerFirstName": false,
    "sampleOrderItems.providerLastName": false,
    "sampleOrderItems.labNo": false,
  });
  const [currentStep, setCurrentStep] = useState(STEP_PATIENT_INFO);
  const [isLoadingReferral, setIsLoadingReferral] = useState(false);
  const isEQAFromUrl =
    new URLSearchParams(window.location.search).get("isEQA") === "true";
  const [orderFormValues, setOrderFormValues] = useState(SampleOrderFormValues);
  const [samples, setSamples] = useState([sampleObject]);
  const [errors, setErrors] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [saveResponse, setSaveResponse] = useState(null);
  const [phoneValidation, setPhoneValidation] = useState({
    primaryPhone: { body: "", status: true },
    contactPhone: { body: "", status: true },
  });
  const [stagedAttachments, setStagedAttachments] = useState([]);

  // Derived step values
  const domain = orderFormValues?.sampleOrderItems?.domain;
  const isNonClinicalDomain = domain === "E" || domain === "V";

  const visibleSteps = isNonClinicalDomain
    ? [STEP_PROGRAM, STEP_ADD_SAMPLE, STEP_ADD_ORDER]
    : [STEP_PATIENT_INFO, STEP_PROGRAM, STEP_ADD_SAMPLE, STEP_ADD_ORDER];

  const currentStepIndex = visibleSteps.indexOf(currentStep);
  const isFirstStep = currentStepIndex === 0;
  const isLastStep = currentStepIndex === visibleSteps.length - 1;
  const isOnSuccess = currentStep === STEP_SUCCESS;

  let SampleTypes = [];
  let sampleTypeMap = {};
  let CrossPanels = [];
  let CrossTests = [];
  let sampleTypeOrder;
  let crossSampleTypeMap = {};
  let crossSampleTypeOrderMap = {};

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  // If current step becomes invisible (e.g. domain changed while on patient
  // info), snap to first visible step
  useEffect(() => {
    if (
      !isOnSuccess &&
      currentStep !== STEP_PATIENT_INFO &&
      !visibleSteps.includes(currentStep)
    ) {
      setCurrentStep(visibleSteps[0]);
    }
  }, [isNonClinicalDomain]);

  useEffect(() => {
    if (configurationProperties.ACCEPT_EXTERNAL_ORDERS === "true") {
      const urlParams = new URLSearchParams(window.location.search);
      const externalId = urlParams.get("ID");
      if (externalId) {
        setIsLoadingReferral(true);
        checkOrderReferral(externalId);
      }
    } else {
      setOrderFormValues((prev) => ({
        ...prev,
        sampleOrderItems: {
          ...prev.sampleOrderItems,
          externalOrderNumber: "",
        },
      }));
    }
  }, [configurationProperties.ACCEPT_EXTERNAL_ORDERS]);

  useEffect(() => {
    checkOrderReferral(orderFormValues.sampleOrderItems.externalOrderNumber);
  }, [orderFormValues.sampleOrderItems.externalOrderNumber]);

  const checkOrderReferral = (externalOrderNumber) => {
    if (externalOrderNumber) {
      getLabOrder(externalOrderNumber, processLabOrderSuccess);
    }
  };

  const getLabOrder = (orderNumber, success, failure) => {
    if (!failure) {
      failure = () => {
        // Default failure handler - no-op
      };
    }

    fetch(
      config.serverBaseUrl +
        "/ajaxQueryXML?asJSON=true&provider=LabOrderSearchProvider&orderNumber=" +
        orderNumber,
      {
        method: "get",
        headers: {
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
      },
    )
      .then((response) => response.json())
      .then((jsonResponse) => {
        success(jsonResponse);
        setIsLoadingReferral(false);
      })
      .catch((error) => {
        console.error(error);
        if (error instanceof SyntaxError) {
          addNotification({
            title: intl.formatMessage({
              id: "notification.title",
            }),
            message: intl.formatMessage({
              id: "notification.response.syntax.error",
            }),
            kind: NotificationKinds.error,
          });
          setNotificationVisible(true);
        }
        setIsLoadingReferral(false);
        failure();
      });
  };

  const processLabOrderSuccess = (labOrder) => {
    let message = labOrder.fieldmessage.message;
    let formField = labOrder.fieldmessage.formfield;
    let order = formField.order;

    let newOrderFormValues = { ...orderFormValues };

    SampleTypes = [];
    CrossPanels = [];
    CrossTests = [];
    sampleTypeMap = {};

    if (message === "valid") {
      // DOMAIN
      if (order.domain) {
        const workflowType =
          order.domain === "E"
            ? "environmental"
            : order.domain === "V"
              ? "vector"
              : "clinical";
        newOrderFormValues = {
          ...newOrderFormValues,
          sampleOrderItems: {
            ...newOrderFormValues.sampleOrderItems,
            domain: order.domain,
            environmentalFields: {
              ...newOrderFormValues.sampleOrderItems.environmentalFields,
              workflowType: workflowType,
            },
          },
        };
      }

      // PATIENT
      if (order.patient) {
        parsePatient(newOrderFormValues, order.patient);
      }

      // REQUESTER
      if (order.requester) {
        parseRequester(newOrderFormValues, order.requester);
      }

      if (order.requestingOrg) {
        parseRequestingOrg(newOrderFormValues, order.requestingOrg);
      }
      if (order.location && !order.requestingOrg.id) {
        parseLocation(newOrderFormValues, order.location);
      }

      if (order.user_alert) {
        alert(order.user_alert);
      }

      // initialize objects and globals
      sampleTypeOrder = -1;
      crossSampleTypeMap = {};
      crossSampleTypeOrderMap = {};

      if (order.sampleTypes != "") {
        parseSampletypes(
          newOrderFormValues,
          order.sampleTypes instanceof Array
            ? order.sampleTypes
            : [{ sampleType: order.sampleTypes.sampleType }],
          SampleTypes,
        );
      }

      const urlParams = new URLSearchParams(window.location.search);
      const externalId = urlParams.get("ID");
      const labNumber = urlParams.get("labNumber");

      newOrderFormValues = {
        ...newOrderFormValues,
        sampleOrderItems: {
          ...newOrderFormValues.sampleOrderItems,
          externalOrderNumber: externalId,
          labNo: labNumber,
        },
      };
      setOrderFormValues(newOrderFormValues);
      setSamples(SampleTypes);

      // Set initial step based on resolved domain
      const resolvedDomain = order.domain;
      if (resolvedDomain === "E" || resolvedDomain === "V") {
        setCurrentStep(STEP_PROGRAM);
      }
    } else {
      alert(message);
    }
  };

  const parsePatient = (newOrderFormValues, patient) => {
    newOrderFormValues.patientProperties = {
      ...newOrderFormValues.patientProperties,
      guid: patient.guid,
    };
  };

  const parseRequester = (newOrderFormValues, requester) => {
    const providerId = requester.personId;
    if (providerId) {
      newOrderFormValues.sampleOrderItems = {
        ...newOrderFormValues.sampleOrderItems,
        providerId: providerId,
      };
      getFromOpenElisServer(
        "/rest/practitioner?providerId=" + providerId,
        (data) => {
          setOrderFormValues({
            ...orderFormValues,
            sampleOrderItems: {
              ...orderFormValues.sampleOrderItems,
              providerId: data.id,
              providerPersonId: data.person.id,
              providerFirstName: data.person.firstName ?? "",
              providerLastName: data.person.lastName ?? "",
              providerWorkPhone: data.person.workPhone ?? "",
              providerEmail: data.person.email ?? "",
              providerFax: data.person.fax ?? "",
            },
          });
        },
      );
    } else {
      newOrderFormValues.sampleOrderItems = {
        ...newOrderFormValues.sampleOrderItems,
        providerFirstName: requester.firstName ?? "",
        providerLastName: requester.lastName ?? "",
        providerWorkPhone: requester.phone ?? "",
        providerEmail: requester.email ?? "",
        providerFax: requester.fax ?? "",
      };
    }
  };

  const parseRequestingOrg = (newOrderFormValues, requestingOrg) => {
    newOrderFormValues.sampleOrderItems = {
      ...newOrderFormValues.sampleOrderItems,
      referringSiteId: requestingOrg.id,
    };
    getFromOpenElisServer(
      "/rest/departments-for-site?refferingSiteId=" + requestingOrg.id,
      () => {
        // Departments loaded - handled elsewhere
      },
    );
  };

  const parseLocation = (newOrderFormValues, location) => {
    newOrderFormValues.sampleOrderItems = {
      ...newOrderFormValues.sampleOrderItems,
      referringSiteId: location.id,
    };
    getFromOpenElisServer(
      "/rest/departments-for-site?refferingSiteId=" + location.id,
      () => {
        // Departments loaded - handled elsewhere
      },
    );
  };

  const parseSampletypes = (newOrderFormValues, sampletypes, SampleTypes) => {
    let index = 0;
    for (let i = 0; i < sampletypes.length; i++) {
      index = parseSampletype(index, sampletypes[i].sampleType, SampleTypes);
    }
  };

  const parseSampletype = (index, sampleType, SampleTypes) => {
    let sampleTypeName = sampleType.name;
    let sampleTypeId = sampleType.id;
    let panels = sampleType.panels;
    let tests = sampleType.tests;
    let collection = sampleType.collection;
    let sampleTypeInList = sampleTypeMap[sampleTypeId];
    if (!sampleTypeInList) {
      index++;
      SampleTypes[index - 1] = newSampleType(
        sampleTypeId,
        sampleTypeName,
        index,
      );
      sampleTypeMap[sampleTypeId] = SampleTypes[index - 1];
      SampleTypes[index - 1].rowid = index;
      sampleTypeInList = SampleTypes[index - 1];
    }
    let panelnodes = getNodeNamesByTagName(panels, "panel");
    let testnodes = getNodeNamesByTagName(tests, "test");
    let collectionDate = collection.date;
    let collectionTime = collection.time;

    addPanelsToSampleType(sampleTypeInList, panelnodes);
    addTestsToSampleType(sampleTypeInList, testnodes);
    if (collectionDate) {
      sampleTypeInList.sampleXML.collectionDate = collectionDate;
    } else {
      sampleTypeInList.sampleXML.collectionDate =
        configurationProperties?.AUTOFILL_COLLECTION_DATE === "true"
          ? configurationProperties.currentDateAsText
          : "";
    }
    if (collectionTime) {
      sampleTypeInList.sampleXML.collectionTime = collectionTime;
    } else {
      sampleTypeInList.sampleXML.collectionTime =
        configurationProperties?.AUTOFILL_COLLECTION_DATE === "true"
          ? configurationProperties.currentTimeAsText
          : "";
    }
    return index;
  };

  function addPanelsToSampleType(sampleType, panelNodes) {
    for (let i = 0; i < panelNodes.length; i++) {
      sampleType.panels[sampleType.panels.length] = panelNodes[i];
    }
  }
  function addTestsToSampleType(sampleType, testNodes) {
    for (let i = 0; i < testNodes.length; i++) {
      sampleType.tests[sampleType.tests.length] = newTest(
        testNodes[i].id,
        testNodes[i].name,
      );
    }
  }

  function getNodeNamesByTagName(elements, tag) {
    let allTestsMap = {};
    let panelTestsMap = {};

    if (elements[tag] === undefined) {
      return [];
    }
    let nodes =
      elements[tag] instanceof Array ? elements[tag] : [elements[tag]];
    let objList = [];

    for (let j = 0; j < nodes.length; j++) {
      let name = nodes[j].name;
      let id = nodes[j].id;
      if (tag == "panel") {
        objList[j] = newPanel(id, name);
        let testNodes = nodes[j].panelTests;
        if (testNodes.length === undefined) {
          testNodes = [testNodes];
        }
        for (let x = 0; x < testNodes.length; x++) {
          let ptNodes = testNodes[x].test;
          for (let y = 0; y < ptNodes.length; y++) {
            let pName = ptNodes[y].name;
            let pId = ptNodes[y].id;
            if (objList[j].tests.length == 0) {
              objList[j].tests = pName;
              objList[j].testIds = pId;
            } else {
              objList[j].tests = objList[j].tests + "," + pName;
              objList[j].testIds = objList[j].testIds + "," + pId;
            }
          }
        }
      } else if (tag == "test") {
        objList[j] = newTest(id, name);
        allTestsMap[id] = name;
      } else if (tag == "crosssampletype") {
        let testtag = nodes[j].testid;
        if (testtag) {
          objList[j] = newCrossSampleType(id, name, testtag);
        } else objList[j] = newCrossSampleType(id, name);
      }
    }

    return objList;
  }

  const newSampleType = (id, name, index) => {
    return {
      index: index,
      sampleRejected: true,
      rejectionReason: "",
      requestReferralEnabled: false,
      referralItems: [],
      sampleTypeId: "" + id,
      sampleXML: {
        collectionDate: "",
        collector: "",
        quantity: "",
        uom: "",
        rejected: false,
        rejectionReason: "",
        collectionTime: "",
        collectionMethod: "",
        sampleTemperature: "",
        specimenOrigin: "",
        numOrderLabels: 1,
        numSpecimenLabels: 1,
      },
      id: "" + id,
      name: name,
      panels: [],
      tests: [],
    };
  };

  const newPanel = (id, name) => {
    return {
      id: "" + id,
      name: name,
      tests: "",
      testIds: "",
    };
  };
  const newTest = (id, name) => {
    return { id: "" + id, name: name };
  };
  const newCrossSampleType = (id, name, testId) => {
    return {
      id: "" + id,
      name: name,
      testId: testId,
    };
  };
  const newCrossPanel = (id, name) => {
    return {
      id: "" + id,
      name: name,
      sampleTypes: [],
      typeMap: [],
    };
  };
  const newCrossTest = (name) => {
    return {
      name: name,
      sampleTypes: [],
      typeMap: [],
    };
  };

  const showAlertMessage = (msg, kind) => {
    setNotificationVisible(true);
    addNotification({
      kind: kind,
      title: intl.formatMessage({ id: "notification.title" }),
      message: msg,
    });
  };

  const uploadStagedAttachments = (savedAccessionNumber) => {
    if (!stagedAttachments || stagedAttachments.length === 0) return;
    if (!savedAccessionNumber) return;
    const formData = new FormData();
    stagedAttachments.forEach((a) => {
      formData.append("files", a.file, a.fileName);
    });
    postToOpenElisServerFormData(
      "/rest/order/" +
        encodeURIComponent(savedAccessionNumber) +
        "/attachments",
      formData,
      (status) => {
        if (!status || status >= 400) {
          showAlertMessage(
            <FormattedMessage id="order.attachment.upload.failed" />,
            NotificationKinds.warning,
          );
        }
        setStagedAttachments([]);
      },
    );
  };

  const handlePost = (response) => {
    setIsSubmitting(false);
    const responseStatus = response?.statusCode ?? response?.status ?? 200;
    if (response && !response.error && responseStatus < 400) {
      setSaveResponse(response);
      showAlertMessage(
        <FormattedMessage id="save.order.success.msg" />,
        NotificationKinds.success,
      );
      uploadStagedAttachments(response?.sampleOrderItems?.labNo);
      setCurrentStep(STEP_SUCCESS);
    } else {
      showAlertMessage(
        resolveApiErrorMessage(intl, response, "server.error.msg"),
        NotificationKinds.error,
      );
    }
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

  const handleSubmitOrderForm = (e) => {
    e.preventDefault();
    if (isSubmitting) {
      return;
    }
    setIsSubmitting(true);
    if ("years" in orderFormValues.patientProperties) {
      delete orderFormValues.patientProperties.years;
    }
    if ("months" in orderFormValues.patientProperties) {
      delete orderFormValues.patientProperties.months;
    }
    if ("days" in orderFormValues.patientProperties) {
      delete orderFormValues.patientProperties.days;
    }
    if ("questionnaire" in orderFormValues.sampleOrderItems) {
      delete orderFormValues.sampleOrderItems.questionnaire;
    }
    // domain is frontend-only (used for step/validation logic), not a backend field
    delete orderFormValues.sampleOrderItems.domain;
    if ("readOnly" in orderFormValues.patientProperties) {
      delete orderFormValues.patientProperties.readOnly;
    }
    orderFormValues.sampleOrderItems.priorityList = [];
    orderFormValues.sampleOrderItems.programList = [];
    orderFormValues.sampleOrderItems.referringSiteList = [];
    orderFormValues.initialSampleConditionList = [];
    orderFormValues.testSectionList = [];
    orderFormValues.sampleOrderItems.providersList = [];
    orderFormValues.sampleOrderItems.paymentOptions = [];
    orderFormValues.sampleOrderItems.testLocationCodeList = [];
    postToOpenElisServerJsonResponse(
      "/rest/SamplePatientEntry",
      JSON.stringify(orderFormValues),
      handlePost,
    );
  };

  useEffect(() => {
    if (currentStep === STEP_ADD_ORDER) {
      attacheSamplesToFormValues();
    }
  }, [currentStep]);

  useEffect(() => {
    const schema = createOrderEntryValidationSchema(
      domain,
      configurationProperties,
    );
    schema
      .validate(orderFormValues, { abortEarly: false })
      .then((validData) => {
        setErrors([]);
      })
      .catch((errors) => {
        setErrors(errors);
      });
  }, [changed, configurationProperties, orderFormValues, domain]);

  useEffect(() => {
    const labNumber = new URLSearchParams(window.location.search).get(
      "labNumber",
    );
    const newOrderFormValues = {
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        labNo: labNumber ? labNumber : "",
      },
    };
    setOrderFormValues(newOrderFormValues);
  }, []);

  const attacheSamplesToFormValues = () => {
    let sampleXmlString = "";
    let referralItems = [];
    if (samples.length > 0) {
      if (samples[0].tests.length > 0) {
        sampleXmlString = '<?xml version="1.0" encoding="utf-8"?>';
        sampleXmlString += "<samples>";
        let tests = null;
        let panels = "";
        samples.map((sampleItem) => {
          if (sampleItem.tests.length > 0) {
            tests = Object.keys(sampleItem.tests)
              .map(function (i) {
                return sampleItem.tests[i].id;
              })
              .join(",");

            if (sampleItem?.panels.length > 0) {
              panels = Object.keys(sampleItem.panels)
                .map(function (i) {
                  return sampleItem.panels[i].id;
                })
                .join(",");
            }
            const storageLocation = sampleItem.sampleXML?.storageLocation;
            const storageLocationId = storageLocation?.id || "";
            const storageLocationType = storageLocation?.type || "";
            const storagePositionCoordinate =
              storageLocation?.positionCoordinate || "";

            const gpsLatitude = sampleItem.sampleXML?.gpsLatitude || "";
            const gpsLongitude = sampleItem.sampleXML?.gpsLongitude || "";
            const gpsAccuracy = sampleItem.sampleXML?.gpsAccuracy || "";
            const gpsCaptureMethod =
              sampleItem.sampleXML?.gpsCaptureMethod || "";

            const collectionMethod =
              sampleItem.sampleXML?.collectionMethod || "";
            const sampleTemperature =
              sampleItem.sampleXML?.sampleTemperature || "";
            const specimenOrigin = sampleItem.sampleXML?.specimenOrigin || "";

            sampleXmlString += `<sample sampleID='${sampleItem.sampleTypeId}' date='${sampleItem.sampleXML.collectionDate}' time='${sampleItem.sampleXML.collectionTime}' collector='${sampleItem.sampleXML.collector}' quantity='${sampleItem.sampleXML.quantity}' uom='${sampleItem.sampleXML.uom}' tests='${tests}' testSectionMap='' testSampleTypeMap='' panels='${panels}' rejected='${sampleItem.sampleXML.rejected}' rejectReasonId='${sampleItem.sampleXML.rejectionReason}' initialConditionIds='' storageLocationId='${storageLocationId}' storageLocationType='${storageLocationType}' storagePositionCoordinate='${storagePositionCoordinate}' gpsLatitude='${gpsLatitude}' gpsLongitude='${gpsLongitude}' gpsAccuracy='${gpsAccuracy}' gpsCaptureMethod='${gpsCaptureMethod}' collectionMethod='${collectionMethod}' sampleTemperature='${sampleTemperature}' specimenOrigin='${specimenOrigin}' numOrderLabels='${sampleItem.sampleXML?.numOrderLabels || 1}' numSpecimenLabels='${sampleItem.sampleXML?.numSpecimenLabels || 1}'/>`;
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
    setOrderFormValues({
      ...orderFormValues,
      useReferral: true,
      sampleXML: sampleXmlString,
      referralItems: referralItems,
    });
  };

  const navigateForward = () => {
    if (currentStepIndex < visibleSteps.length - 1) {
      setCurrentStep(visibleSteps[currentStepIndex + 1]);
    }
  };

  const navigateBackward = () => {
    if (currentStepIndex > 0) {
      setCurrentStep(visibleSteps[currentStepIndex - 1]);
    }
  };

  const handleTabClick = (physicalIndex) => {
    const targetStep = visibleSteps[physicalIndex];
    if (targetStep) {
      setCurrentStep(targetStep);
    }
  };

  // Compat wrapper: OrderSuccessMessage calls setPage(0) to reset
  const handleResetToFirstStep = (pageNumber) => {
    if (pageNumber === 0) {
      setCurrentStep(visibleSteps[0]);
    }
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Stack gap={10}>
        <div className="pageContent">
          {notificationVisible === true ? <AlertDialog /> : ""}
          <div className="orderWorkFlowDiv">
            <h2>
              <FormattedMessage id="order.test.request.heading" />
            </h2>

            {isLoadingReferral ? (
              <div style={{ padding: "1rem" }}>
                <SkeletonText paragraph lineCount={5} />
              </div>
            ) : (
              <>
                {!isOnSuccess && (
                  <ProgressIndicator
                    currentIndex={currentStepIndex}
                    className="ProgressIndicator"
                    spaceEqually={true}
                    onChange={handleTabClick}
                  >
                    {visibleSteps.map((stepId) => (
                      <ProgressStep
                        key={stepId}
                        label={intl.formatMessage({ id: STEP_LABELS[stepId] })}
                      />
                    ))}
                  </ProgressIndicator>
                )}

                {currentStep === STEP_PATIENT_INFO && (
                  <>
                    {(configurationProperties.EQA_ENABLED === "true" ||
                      isEQAFromUrl ||
                      orderFormValues?.sampleOrderItems?.isEQASample) && (
                      <EQASampleEntry
                        orderFormValues={orderFormValues}
                        setOrderFormValues={setOrderFormValues}
                        autoEnable={isEQAFromUrl}
                      />
                    )}
                    <PatientInfo
                      orderFormValues={orderFormValues}
                      setOrderFormValues={setOrderFormValues}
                      error={elementError}
                      setPhoneValidation={setPhoneValidation}
                    />
                  </>
                )}
                {currentStep === STEP_PROGRAM &&
                  (orderFormValues?.sampleOrderItems?.isEQASample ? (
                    <EQAOrderForm
                      orderFormValues={orderFormValues}
                      setOrderFormValues={setOrderFormValues}
                    />
                  ) : (
                    <OrderEntryAdditionalQuestions
                      orderFormValues={orderFormValues}
                      setOrderFormValues={setOrderFormValues}
                    />
                  ))}
                {currentStep === STEP_ADD_SAMPLE && (
                  <AddSample
                    error={elementError}
                    setSamples={setSamples}
                    samples={samples}
                    domain={domain}
                  />
                )}
                {currentStep === STEP_ADD_ORDER && (
                  <AddOrder
                    orderFormValues={orderFormValues}
                    setOrderFormValues={setOrderFormValues}
                    samples={samples}
                    error={elementError}
                    isModifyOrder={false}
                    changed={changed}
                    setChanged={setChanged}
                    stagedAttachments={stagedAttachments}
                    setStagedAttachments={setStagedAttachments}
                  />
                )}

                {currentStep === STEP_SUCCESS && (
                  <OrderSuccessMessage
                    orderFormValues={orderFormValues}
                    setOrderFormValues={setOrderFormValues}
                    setSamples={setSamples}
                    setPage={handleResetToFirstStep}
                    saveResponse={saveResponse}
                  />
                )}
                <div className="navigationButtonsLayout">
                  {!isFirstStep && !isOnSuccess && (
                    <Button kind="tertiary" onClick={navigateBackward}>
                      <FormattedMessage id="back.action.button" />
                    </Button>
                  )}

                  {!isLastStep && !isOnSuccess && (
                    <Button
                      kind="primary"
                      className="forwardButton"
                      onClick={navigateForward}
                    >
                      <FormattedMessage id="next.action.button" />
                    </Button>
                  )}

                  {isLastStep && !isOnSuccess && (
                    <Button
                      kind="primary"
                      className="forwardButton"
                      disabled={
                        isSubmitting ||
                        Object.values(phoneValidation).some(
                          (item) => item.status === false,
                        ) ||
                        errors?.errors?.length > 0
                      }
                      onClick={handleSubmitOrderForm}
                    >
                      <FormattedMessage id="label.button.submit" />
                    </Button>
                  )}
                </div>
              </>
            )}
          </div>
        </div>
      </Stack>
    </>
  );
};

export default Index;
