import React, { useContext, useEffect, useState } from "react";
import { Button, ProgressIndicator, ProgressStep, Stack } from "@carbon/react";
import PatientInfo from "./PatientInfo";
import AddSample from "./AddSample";
import AddOrder from "./AddOrder";
import "./add-order.scss";
import { SampleOrderFormValues } from "../formModel/innitialValues/OrderEntryFormValues";
import { NotificationContext, ConfigurationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { getFromOpenElisServer, postToOpenElisServer } from "../utils/Utils";
import OrderEntryAdditionalQuestions from "./OrderEntryAdditionalQuestions";
import OrderSuccessMessage from "./OrderSuccessMessage";
import { FormattedMessage, useIntl } from "react-intl";
import OrderEntryValidationSchema from "../formModel/validationSchema/OrderEntryValidationSchema";
import config from "../../config.json";
import PageBreadCrumb from "../common/PageBreadCrumb";
let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sidenav.label.addorder", link: "/SamplePatientEntry" },
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

  const [page, setPage] = useState(firstPageNumber);
  const [orderFormValues, setOrderFormValues] = useState(SampleOrderFormValues);
  const [samples, setSamples] = useState([sampleObject]);
  const [errors, setErrors] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  let SampleTypes = [];
  let sampleTypeMap = {};
  let initializePanelTests = false;
  let allTestsMap = {};
  let panelTestsMap = {};
  let crossTestSampleTypeTestIdMap = {};
  let sampleTypeTestIdMap = {};
  let sampleTypeOrder;
  let crossSampleTypeMap = {};
  let crossSampleTypeOrderMap = {};
  let CrossPanels = [];
  let CrossTests = [];

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  useEffect(() => {
    if (configurationProperties.ACCEPT_EXTERNAL_ORDERS === "true") {
      const urlParams = new URLSearchParams(window.location.search);
      const externalId = urlParams.get("ID");
      checkOrderReferral(externalId);
    } else {
      setOrderFormValues({
        ...orderFormValues,
        sampleOrderItems: {
          ...orderFormValues.sampleOrderItems,
          externalOrderNumber: "",
        },
      });
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
      failure = () => {};
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
    CrossPanels = [];
    CrossTests = [];
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

      //TODO not translated over for 3.0 Unsure if needed
      // parseCrossPanels(
      //   order.crosspanel,
      //   crossSampleTypeMap,
      //   crossSampleTypeOrderMap,
      // );
      // parseCrossTests(
      //   order.crosstest,
      //   crossSampleTypeMap,
      //   crossSampleTypeOrderMap,
      // );
      // populateCrossPanelsAndTests(CrossPanels, CrossTests, '${entryDate}');
      // displaySampleTypes('${entryDate}');

      // if (SampleTypes.length > 0) sampleClicked(1);
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
      ...orderFormValues.patientProperties,
      guid: patient.guid,
    };
  };

  const parseRequester = (newOrderFormValues, requester) => {
    const providerId = requester.personId;
    if (providerId) {
      newOrderFormValues.sampleOrderItems = {
        ...orderFormValues.sampleOrderItems,
        providerId: providerId,
      };
      getFromOpenElisServer(
        "/rest/practitioner?providerId=" + providerId,
        (data) => {
          setOrderFormValues({
            ...orderFormValues,
            sampleOrderItems: {
              ...orderFormValues.sampleOrderItems,
              providerId: providerId,
              providerFirstName: data.person.firstName,
              providerLastName: data.person.lastName,
              providerWorkPhone: data.person.workPhone,
              providerEmail: data.person.email,
              providerFax: data.person.fax,
            },
          });
        },
      );
    } else {
      newOrderFormValues.sampleOrderItems = {
        ...orderFormValues.sampleOrderItems,
        providerFirstName: requester.firstName,
        providerLastName: requester.lastName,
        providerWorkPhone: requester.phone,
        providerEmail: requester.email,
        providerFax: requester.fax,
      };
    }
  };

  const parseRequestingOrg = (newOrderFormValues, requestingOrg) => {
    newOrderFormValues.sampleOrderItems = {
      ...orderFormValues.sampleOrderItems,
      referringSiteId: requestingOrg.id,
    };
    getFromOpenElisServer(
      "/rest/departments-for-site?refferingSiteId=" + requestingOrg.id,
      () => {},
    );
  };

  const parseLocation = (newOrderFormValues, location) => {
    newOrderFormValues.sampleOrderItems = {
      ...orderFormValues.sampleOrderItems,
      referringSiteId: location.id,
    };
    getFromOpenElisServer(
      "/rest/departments-for-site?refferingSiteId=" + location.id,
      () => {},
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

  // const parseCrossPanels = (
  //   crosspanels,
  //   crossSampleTypeMap,
  //   crossSampleTypeOrderMap,
  // ) => {
  //   for (let i = 0; i < crosspanels.length; i++) {
  //     var crossPanelName = crosspanels[i].name;
  //     var crossPanelId = crosspanels[i].id;
  //     var crossSampleTypes = crosspanels[i].crosssampletypes;

  //     CrossPanels[i] = newCrossPanel(crossPanelId, crossPanelName);
  //     CrossPanels[i].sampleTypes = getNodeNamesByTagName(
  //       crossSampleTypes,
  //       "crosssampletype",
  //     );
  //     CrossPanels[i].typeMap = [CrossPanels[i].sampleTypes.length];

  //     for (let j = 0; j < CrossPanels[i].sampleTypes.length; j = j + 1) {
  //       CrossPanels[i].typeMap[CrossPanels[i].sampleTypes[j].name] = "t";
  //       var sampleType = crossSampleTypeMap[CrossPanels[i].sampleTypes[j].id];

  //       if (sampleType === undefined) {
  //         crossSampleTypeMap[CrossPanels[i].sampleTypes[j].id] =
  //           CrossPanels[i].sampleTypes[j];
  //         sampleTypeOrder = sampleTypeOrder + 1;
  //         crossSampleTypeOrderMap[sampleTypeOrder] =
  //           CrossPanels[i].sampleTypes[j].id;
  //       }
  //     }
  //   }
  // };

  // const parseCrossTests = (
  //   crosstests,
  //   crossSampleTypeMap,
  //   crossSampleTypeOrderMap,
  // ) => {
  //   for (let x = 0; x < crosstests.length; x = x + 1) {
  //     var crossTestName = crosstests[x].name;
  //     var crossSampleTypes = crosstests[x].crosssampletypes;

  //     CrossTests[x] = newCrossTest(crossTestName);
  //     CrossTests[x].sampleTypes = getNodeNamesByTagName(
  //       crossSampleTypes,
  //       "crosssampletype",
  //     );
  //     CrossTests[x].typeMap = [CrossTests[x].sampleTypes.length];
  //     var sTypes = [];
  //     for (var y = 0; y < CrossTests[x].sampleTypes.length; y++) {
  //       //alert(crossTestName + " " + CrossTests[x].sampleTypes[y].id + " testid=" + CrossTests[x].sampleTypes[y].testId);
  //       sTypes[y] = CrossTests[x].sampleTypes[y];
  //       CrossTests[x].typeMap[CrossTests[x].sampleTypes[y].name] = "t";
  //       var sType = crossSampleTypeMap[CrossTests[x].sampleTypes[y].id];

  //       if (sType === undefined) {
  //         crossSampleTypeMap[CrossTests[x].sampleTypes[y].id] =
  //           CrossTests[x].sampleTypes[y];
  //         sampleTypeOrder++;
  //         crossSampleTypeOrderMap[sampleTypeOrder] =
  //           CrossTests[x].sampleTypes[y].id;
  //       }
  //     }
  //     crossTestSampleTypeTestIdMap[crossTestName] = sTypes;
  //   }
  // };

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
        rejected: false,
        rejectionReason: "",
        collectionTime: "",
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

  const handlePost = (status) => {
    setIsSubmitting(false);
    if (status === 200) {
      showAlertMessage(
        <FormattedMessage id="save.order.success.msg" />,
        NotificationKinds.success,
      );
      setPage(page + 1);
    } else {
      showAlertMessage(
        <FormattedMessage id="server.error.msg" />,
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
    OrderEntryValidationSchema.validate(orderFormValues, { abortEarly: false })
      .then((validData) => {
        setErrors([]);
        console.debug("Valid Data:", validData);
      })
      .catch((errors) => {
        setErrors(errors);
        console.error("Validation Errors:", errors.errors);
      });
  }, [orderFormValues]);

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
            sampleXmlString += `<sample sampleID='${sampleItem.sampleTypeId}' date='${sampleItem.sampleXML.collectionDate}' time='${sampleItem.sampleXML.collectionTime}' collector='${sampleItem.sampleXML.collector}' tests='${tests}' testSectionMap='' testSampleTypeMap='' panels='${panels}' rejected='${sampleItem.sampleXML.rejected}' rejectReasonId='${sampleItem.sampleXML.rejectionReason}' initialConditionIds=''/>`;
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

            {page === patientInfoPageNumber && (
              <PatientInfo
                orderFormValues={orderFormValues}
                setOrderFormValues={setOrderFormValues}
                error={elementError}
              />
            )}
            {page === programPageNumber && (
              <OrderEntryAdditionalQuestions
                orderFormValues={orderFormValues}
                setOrderFormValues={setOrderFormValues}
              />
            )}
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
              />
            )}

            {page === successMsgPageNumber && (
              <OrderSuccessMessage
                orderFormValues={orderFormValues}
                setOrderFormValues={setOrderFormValues}
                setSamples={setSamples}
                setPage={setPage}
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
                    isSubmitting || errors?.errors?.length > 0 ? true : false
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
