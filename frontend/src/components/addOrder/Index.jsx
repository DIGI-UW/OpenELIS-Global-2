import React, { useContext, useEffect, useState } from "react";
import {
  Button,
  InlineNotification,
  ProgressIndicator,
  ProgressStep,
  Select,
  SelectItem,
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
  { label: "breadcrumb.label.addOrder", link: "/SamplePatientEntry" },
];

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

  const firstPageNumber = 0;
  const lastPageNumber = 4;
  const patientInfoPageNumber = firstPageNumber;
  const programPageNumber = firstPageNumber + 1;
  const samplePageNumber = firstPageNumber + 2;
  const orderPageNumber = firstPageNumber + 3;
  const successMsgPageNumber = lastPageNumber;
  const [changed, setChanged] = useState({
    "sampleOrderItems.providerFirstName": false,
    "sampleOrderItems.providerLastName": false,
    "sampleOrderItems.labNo": false,
  });
  const [page, setPage] = useState(firstPageNumber);
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
  // OGC-1145 FR-8 — e-order tests/panels whose sample type couldn't be resolved
  // from the message (multi-specimen test, no specimen coding): the accessioner
  // picks the specimen here, which files the orderable under that sample type.
  const [crossTests, setCrossTests] = useState([]);
  const [crossPanels, setCrossPanels] = useState([]);

  let SampleTypes = [];
  let sampleTypeMap = {};

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  useEffect(() => {
    if (configurationProperties.ACCEPT_EXTERNAL_ORDERS === "true") {
      const urlParams = new URLSearchParams(window.location.search);
      const externalId = urlParams.get("ID");
      checkOrderReferral(externalId);
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
        //indicator: 'throbbing',
        headers: {
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
      },
    )
      .then((response) => response.json())
      .then((jsonResponse) => {
        success(jsonResponse);
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
        failure();
      });
  };

  const processLabOrderSuccess = (labOrder) => {
    // clearOrderData();
    let message = labOrder.fieldmessage.message;
    let formField = labOrder.fieldmessage.formfield;
    let order = formField.order;

    let newOrderFormValues = { ...orderFormValues };

    SampleTypes = [];
    sampleTypeMap = {};

    //TODO all these actions mimic other areas of the code. Possible rework could centralize these calls into a context
    if (message === "valid") {
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
      // A pure awaiting-specimen order carries no pre-bound sample types; keep
      // the blank sample entry so the Add Sample step stays usable while the
      // chooser below resolves the specimens.
      setSamples(SampleTypes.length > 0 ? SampleTypes : [sampleObject]);

      // OGC-1145 FR-8 — orderables the provider couldn't bind to one specimen
      // (<crosstest>/<crosspanel> in the provider XML) go to the chooser instead
      // of being first-matched; resolving one files it under the chosen type.
      setCrossTests(order.crosstest ? parseCrossList(order.crosstest) : []);
      setCrossPanels(order.crosspanel ? parseCrossList(order.crosspanel) : []);
    } else {
      alert(message);
    }

    // if (attemptAutoSave) {
    // let validToSave =  patientFormValid() && sampleEntryTopValid();
    // if (validToSave) {
    //   savePage();
    // }
    // }
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

  // <crosstest>/<crosspanel> nodes → { id?, name, options: [{id, name, testId}] }.
  // Options are the candidate sample types the orderable may run under.
  const parseCrossList = (crossNodes) => {
    const nodes = crossNodes instanceof Array ? crossNodes : [crossNodes];
    return nodes.filter(Boolean).map((node) => ({
      id: node.id ? "" + node.id : null,
      name: node.name,
      options: getNodeNamesByTagName(
        node.crosssampletypes || {},
        "crosssampletype",
      ),
      chosenId: "",
    }));
  };

  // Files a resolved orderable under the chosen sample type, creating the
  // sample entry if the order didn't already carry one of that type (mirrors
  // parseSampletype's autofill behavior).
  const addUnderSampleType = (option, applyToSampleType) => {
    setSamples((prev) => {
      const next = prev
        .filter((s) => s.sampleTypeId !== "")
        .map((s) => ({
          ...s,
          tests: [...s.tests],
          panels: [...s.panels],
        }));
      let sampleType = next.find((s) => s.sampleTypeId === "" + option.id);
      if (!sampleType) {
        sampleType = newSampleType(option.id, option.name, next.length + 1);
        if (configurationProperties?.AUTOFILL_COLLECTION_DATE === "true") {
          sampleType.sampleXML.collectionDate =
            configurationProperties.currentDateAsText;
          sampleType.sampleXML.collectionTime =
            configurationProperties.currentTimeAsText;
        }
        next.push(sampleType);
      }
      applyToSampleType(sampleType);
      return next;
    });
  };

  const resolveCrossTest = (index, optionId) => {
    const crossTest = crossTests[index];
    const option = crossTest.options.find((o) => o.id === optionId);
    if (!option) {
      return;
    }
    addUnderSampleType(option, (sampleType) => {
      if (!sampleType.tests.some((t) => t.id === "" + option.testId)) {
        sampleType.tests.push(newTest(option.testId, crossTest.name));
      }
    });
    setCrossTests((prev) => prev.filter((_, i) => i !== index));
  };

  const resolveCrossPanel = (index, optionId) => {
    const crossPanel = crossPanels[index];
    const option = crossPanel.options.find((o) => o.id === optionId);
    if (!option) {
      return;
    }
    addUnderSampleType(option, (sampleType) => {
      if (!sampleType.panels.some((p) => p.id === crossPanel.id)) {
        sampleType.panels.push(newPanel(crossPanel.id, crossPanel.name));
      }
    });
    setCrossPanels((prev) => prev.filter((_, i) => i !== index));
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
    //initialize helper objects
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
      // setCrossPanels: "false",
      // setCrossTests: "false",
      // crossPanels: [],
      // crossTests: [],
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
      setPage(page + 1);
    } else {
      // Surface the backend's actual error/fieldErrors instead of the generic
      // "Oops, Server error..." fallback.
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
    // Prevent multiple submissions.
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
    // readOnly is frontend-only, do not send to backend
    if ("readOnly" in orderFormValues.patientProperties) {
      delete orderFormValues.patientProperties.readOnly;
    }
    //remove display Lists rom the form
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
    if (page === samplePageNumber + 1) {
      attacheSamplesToFormValues();
    }
  }, [page]);

  useEffect(() => {
    console.log(changed);
    createOrderEntryValidationSchema(configurationProperties)
      .validate(orderFormValues, { abortEarly: false })
      .then((validData) => {
        setErrors([]);
        console.debug("Valid Data:", validData);
      })
      .catch((errors) => {
        setErrors(errors);
        console.error("Validation Errors:", errors.errors);
      });
  }, [changed, configurationProperties, orderFormValues]);

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
            // Extract storage location data if present
            const storageLocation = sampleItem.sampleXML?.storageLocation;
            const storageLocationId = storageLocation?.id || "";
            const storageLocationType = storageLocation?.type || "";
            const storagePositionCoordinate =
              storageLocation?.positionCoordinate || "";

            // Extract GPS coordinates data if present
            const gpsLatitude = sampleItem.sampleXML?.gpsLatitude || "";
            const gpsLongitude = sampleItem.sampleXML?.gpsLongitude || "";
            const gpsAccuracy = sampleItem.sampleXML?.gpsAccuracy || "";
            const gpsCaptureMethod =
              sampleItem.sampleXML?.gpsCaptureMethod || "";

            // OGC-651: specimen detail freetext attributes (LO-03-01).
            // Backend reads via SampleAddService.attributeValue("...");
            // persists to sample_item.collection_method / sample_temperature /
            // specimen_origin columns (Liquibase 3.5.0-020).
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
    if (page <= lastPageNumber && page >= firstPageNumber) {
      setPage(page + 1);
    }
  };

  const navigateBackWards = () => {
    if (page > firstPageNumber) {
      setPage(page + -1);
    }
  };
  const handleTabClickHandler = (e) => {
    setPage(e);
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
            {page <= orderPageNumber && (
              <ProgressIndicator
                currentIndex={page}
                className="ProgressIndicator"
                spaceEqually={true}
                onChange={(e) => handleTabClickHandler(e)}
              >
                <ProgressStep
                  complete
                  label={intl.formatMessage({ id: "order.step.patient.info" })}
                />
                <ProgressStep
                  label={intl.formatMessage({
                    id: "order.step.program.selection",
                  })}
                />
                <ProgressStep
                  label={intl.formatMessage({ id: "sample.add.action" })}
                />
                <ProgressStep
                  label={intl.formatMessage({ id: "order.label.add" })}
                />
              </ProgressIndicator>
            )}

            {(crossTests.length > 0 || crossPanels.length > 0) && (
              <div
                style={{ marginTop: "1rem" }}
                data-testid="awaiting-specimen-chooser"
              >
                <InlineNotification
                  kind="warning"
                  lowContrast
                  hideCloseButton
                  title={intl.formatMessage({
                    id: "notice.testCatalog.intake.awaitingSpecimen",
                  })}
                />
                {crossTests.map((crossTest, i) => (
                  <Select
                    key={`cross-test-${crossTest.name}-${i}`}
                    id={`cross-test-${i}`}
                    labelText={crossTest.name}
                    defaultValue=""
                    onChange={(e) => resolveCrossTest(i, e.target.value)}
                  >
                    <SelectItem
                      value=""
                      text={intl.formatMessage({
                        id: "label.testCatalog.specimenType",
                      })}
                    />
                    {crossTest.options.map((option) => (
                      <SelectItem
                        key={option.id}
                        value={option.id}
                        text={option.name}
                      />
                    ))}
                  </Select>
                ))}
                {crossPanels.map((crossPanel, i) => (
                  <Select
                    key={`cross-panel-${crossPanel.name}-${i}`}
                    id={`cross-panel-${i}`}
                    labelText={crossPanel.name}
                    defaultValue=""
                    onChange={(e) => resolveCrossPanel(i, e.target.value)}
                  >
                    <SelectItem
                      value=""
                      text={intl.formatMessage({
                        id: "label.testCatalog.specimenType",
                      })}
                    />
                    {crossPanel.options.map((option) => (
                      <SelectItem
                        key={option.id}
                        value={option.id}
                        text={option.name}
                      />
                    ))}
                  </Select>
                ))}
              </div>
            )}

            {page === patientInfoPageNumber && (
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
            {page === programPageNumber &&
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
            {page === samplePageNumber && (
              <AddSample
                error={elementError}
                setSamples={setSamples}
                samples={samples}
              />
            )}
            {page === orderPageNumber && (
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

            {page === successMsgPageNumber && (
              <OrderSuccessMessage
                orderFormValues={orderFormValues}
                setOrderFormValues={setOrderFormValues}
                setSamples={setSamples}
                setPage={setPage}
                saveResponse={saveResponse}
              />
            )}
            <div className="navigationButtonsLayout">
              {page !== firstPageNumber && page <= orderPageNumber && (
                <Button kind="tertiary" onClick={() => navigateBackWards()}>
                  <FormattedMessage id="back.action.button" />
                </Button>
              )}

              {page < orderPageNumber && (
                <Button
                  kind="primary"
                  className="forwardButton"
                  onClick={() => navigateForward()}
                >
                  <FormattedMessage id="next.action.button" />
                </Button>
              )}

              {page === orderPageNumber && (
                <Button
                  kind="primary"
                  className="forwardButton"
                  disabled={
                    isSubmitting ||
                    Object.values(phoneValidation).some(
                      (item) => item.status === false,
                    ) ||
                    errors?.errors?.length > 0
                      ? true
                      : false
                  }
                  onClick={handleSubmitOrderForm}
                >
                  <FormattedMessage id="label.button.submit" />
                </Button>
              )}
            </div>
          </div>
        </div>
      </Stack>
    </>
  );
};

export default Index;
