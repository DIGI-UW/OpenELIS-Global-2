import React, { useContext, useEffect, useRef, useState } from "react";
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
  const [changed, setChanged] = useState({
    "sampleOrderItems.providerFirstName": false,
    "sampleOrderItems.providerLastName": false,
    "sampleOrderItems.labNo": false,
  });
  const [page, setPage] = useState(firstPageNumber);
  const [orderFormValues, setOrderFormValues] = useState(SampleOrderFormValues);
  const [samples, setSamples] = useState([sampleObject]);
  const [errors, setErrors] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [incomingOrderNumber, setIncomingOrderNumber] = useState("");
  const globalTestsByIdRef = useRef(null);
  const globalPanelsByIdRef = useRef(null);
  const [phoneValidation, setPhoneValidation] = useState({
    primaryPhone: { body: "", status: true },
    contactPhone: { body: "", status: true },
  });

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
      // Incoming orders reuse this wizard but must NOT trigger external referral lookup.
      const incoming = urlParams.get("incomingOrderNumber");
      if (!incoming) {
        checkOrderReferral(externalId);
      }
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
    const urlParams = new URLSearchParams(window.location.search);
    const incoming = urlParams.get("incomingOrderNumber");
    if (incoming) {
      setIncomingOrderNumber(incoming);
      loadIncomingOrderForm(incoming);
    }
  }, []);

  const loadIncomingOrderForm = (externalOrderNumber) => {
    getFromOpenElisServer(
      "/rest/incoming-orders/" +
        encodeURIComponent(externalOrderNumber) +
        "/sample-patient-entry-form",
      (form) => {
        if (!form) {
          return;
        }

        const sampleOrderItems = form.sampleOrderItems || {};
        const patientProperties = form.patientProperties || {};
        const resolvedExternalOrderNumber =
          sampleOrderItems.externalOrderNumber || externalOrderNumber || "";

        setOrderFormValues({
          ...SampleOrderFormValues,
          ...form,
          patientProperties: {
            ...SampleOrderFormValues.patientProperties,
            ...patientProperties,
          },
          sampleOrderItems: {
            ...SampleOrderFormValues.sampleOrderItems,
            ...sampleOrderItems,
            // Incoming-order review flow uses incomingOrderNumber to skip referral lookup,
            // but must still submit externalOrderNumber so it can be persisted and later
            // included in middleware result sync payloads.
            externalOrderNumber: resolvedExternalOrderNumber,
          },
        });

        const mappedSamples = mapSamplesFromXml(form.sampleXML);
        const initialSamples = mappedSamples.length > 0 ? mappedSamples : [sampleObject];
        setSamples(initialSamples);
        resolveIncomingSampleNames(initialSamples);
        setPage(orderPageNumber);
      },
    );
  };

  const resolveIncomingSampleNames = (samplesToResolve) => {
    if (!samplesToResolve || samplesToResolve.length === 0) {
      return;
    }

    samplesToResolve.forEach((s, idx) => {
      const sampleTypeId = s?.sampleTypeId;
      if (!sampleTypeId) {
        return;
      }

      getFromOpenElisServer(
        `/rest/sample-type-tests?sampleType=${encodeURIComponent(sampleTypeId)}`,
        (res) => {
          const testsById = {};
          const panelsById = {};

          if (res && Array.isArray(res.tests)) {
            res.tests.forEach((t) => {
              if (t && t.id != null) {
                testsById[String(t.id)] = t.name;
              }
            });
          }
          if (res && Array.isArray(res.panels)) {
            res.panels.forEach((p) => {
              if (p && p.id != null) {
                panelsById[String(p.id)] = {
                  name: p.name,
                  testIds: p.testIds,
                };
              }
            });
          }

          const applyResolvedNames = () => {
            setSamples((prev) => {
              if (!Array.isArray(prev) || !prev[idx]) {
                return prev;
              }

              const next = [...prev];
              const current = next[idx];
              if (!current) {
                return prev;
              }

              const globalTestsById = globalTestsByIdRef.current || {};
              const globalPanelsById = globalPanelsByIdRef.current || {};

              next[idx] = {
                ...current,
                tests: Array.isArray(current.tests)
                  ? current.tests.map((t) => {
                      const id = String(t.id);
                      return {
                        ...t,
                        name:
                          testsById[id] ||
                          globalTestsById[id] ||
                          t.name ||
                          id,
                      };
                    })
                  : current.tests,
                panels: Array.isArray(current.panels)
                  ? current.panels
                      .map((p) => {
                        const id = String(p.id);
                        const resolved = panelsById[id];
                        return {
                          ...p,
                          name:
                            resolved?.name ||
                            globalPanelsById[id] ||
                            p.name ||
                            id,
                          testIds: resolved?.testIds,
                        };
                      })
                      // Incoming-order panels coming from XML lack testIds.
                      // SampleType panel-selection logic requires testIds, so drop panels
                      // which cannot be resolved for this sample type.
                      .filter((p) => p && p.testIds)
                  : current.panels,
              };

              return next;
            });
          };

          const needsGlobalTests =
            Array.isArray(s.tests) &&
            s.tests.some((t) => !testsById[String(t.id)]);
          const needsGlobalPanels =
            Array.isArray(s.panels) &&
            s.panels.some((p) => !panelsById[String(p.id)]);

          const fetches = [];

          if (needsGlobalTests && !globalTestsByIdRef.current) {
            fetches.push(
              new Promise((resolve) => {
                getFromOpenElisServer("/rest/tests", (list) => {
                  const map = {};
                  if (Array.isArray(list)) {
                    list.forEach((item) => {
                      if (item && item.id != null) {
                        map[String(item.id)] = item.value;
                      }
                    });
                  }
                  globalTestsByIdRef.current = map;
                  resolve();
                });
              }),
            );
          }

          if (needsGlobalPanels && !globalPanelsByIdRef.current) {
            fetches.push(
              new Promise((resolve) => {
                getFromOpenElisServer("/rest/panels", (list) => {
                  const map = {};
                  if (Array.isArray(list)) {
                    list.forEach((item) => {
                      if (item && item.id != null) {
                        map[String(item.id)] = item.value;
                      }
                    });
                  }
                  globalPanelsByIdRef.current = map;
                  resolve();
                });
              }),
            );
          }

          if (fetches.length > 0) {
            Promise.all(fetches).then(() => applyResolvedNames());
          } else {
            applyResolvedNames();
          }
        },
      );
    });
  };

  const mapSamplesFromXml = (sampleXML) => {
    if (!sampleXML) {
      return [];
    }

    try {
      const parser = new DOMParser();
      const doc = parser.parseFromString(sampleXML, "text/xml");
      const parseError = doc.getElementsByTagName("parsererror");
      if (parseError && parseError.length > 0) {
        return [];
      }

      const nodes = Array.from(doc.getElementsByTagName("sample"));
      return nodes.map((node, idx) => {
        const sampleTypeId = node.getAttribute("sampleID") || "";
        const date = node.getAttribute("date") || "";
        const time = node.getAttribute("time") || "";
        const collector = node.getAttribute("collector") || "";
        const quantity = node.getAttribute("quantity") || "";
        const uom = node.getAttribute("uom") || "";
        const testsAttr = node.getAttribute("tests") || "";
        const panelsAttr = node.getAttribute("panels") || "";

        const tests = testsAttr
          ? testsAttr
              .split(",")
              .map((id) => id.trim())
              .filter((id) => id)
              .map((id) => ({ id: id, name: "" }))
          : [];

        const panels = panelsAttr
          ? panelsAttr
              .split(",")
              .map((id) => id.trim())
              .filter((id) => id)
              .map((id) => ({ id: id, name: "" }))
          : [];

        return {
          index: idx + 1,
          sampleRejected: false,
          rejectionReason: "",
          requestReferralEnabled: false,
          referralItems: [],
          sampleTypeId: String(sampleTypeId),
          sampleXML: {
            collectionDate: date,
            collectionTime: time,
            collector: collector,
            quantity: quantity,
            uom: uom,
            rejected: false,
            rejectionReason: "",
          },
          panels: panels,
          tests: tests,
        };
      });
    } catch (e) {
      return [];
    }
  };

  useEffect(() => {
    if (incomingOrderNumber) {
      return;
    }

    checkOrderReferral(orderFormValues.sampleOrderItems.externalOrderNumber);
  }, [orderFormValues.sampleOrderItems.externalOrderNumber]);

  const checkOrderReferral = (externalOrderNumber) => {
    if (incomingOrderNumber) {
      return;
    }

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
        ...newOrderFormValues.sampleOrderItems,
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
      ...newOrderFormValues.sampleOrderItems,
      referringSiteId: requestingOrg.id,
    };
    getFromOpenElisServer(
      "/rest/departments-for-site?refferingSiteId=" + requestingOrg.id,
      () => {},
    );
  };

  const parseLocation = (newOrderFormValues, location) => {
    newOrderFormValues.sampleOrderItems = {
      ...newOrderFormValues.sampleOrderItems,
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
        quantity: "",
        uom: "",
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
      if (incomingOrderNumber) {
        postToOpenElisServer(
          "/rest/incoming-orders/" +
            encodeURIComponent(incomingOrderNumber) +
            "/finalize",
          JSON.stringify({}),
          () => {},
        );
      }
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

    const payload = JSON.parse(JSON.stringify(orderFormValues));

    if (payload.patientProperties && "years" in payload.patientProperties) {
      delete payload.patientProperties.years;
    }
    if (payload.patientProperties && "months" in payload.patientProperties) {
      delete payload.patientProperties.months;
    }
    if (payload.patientProperties && "days" in payload.patientProperties) {
      delete payload.patientProperties.days;
    }
    if (payload.sampleOrderItems && "questionnaire" in payload.sampleOrderItems) {
      delete payload.sampleOrderItems.questionnaire;
    }

    // Remove display-only lists that backend does not accept in JSON binding.
    if (payload.patientProperties) {
      delete payload.patientProperties.educationList;
      delete payload.patientProperties.maritialList;
      delete payload.patientProperties.nationalityList;
      delete payload.patientProperties.patientTypes;
      delete payload.patientProperties.genders;
      delete payload.patientProperties.addressDepartments;
      delete payload.patientProperties.healthRegions;
      delete payload.patientProperties.healthDistricts;
    }

    // remove display Lists rom the form
    if (payload.sampleOrderItems) {
      payload.sampleOrderItems.priorityList = [];
      payload.sampleOrderItems.programList = [];
      payload.sampleOrderItems.referringSiteList = [];
      payload.sampleOrderItems.providersList = [];
      payload.sampleOrderItems.paymentOptions = [];
      payload.sampleOrderItems.testLocationCodeList = [];
    }
    payload.initialSampleConditionList = [];
    payload.testSectionList = [];

    console.log(JSON.stringify(payload));
    postToOpenElisServer(
      "/rest/SamplePatientEntry",
      JSON.stringify(payload),
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
    OrderEntryValidationSchema.validate(orderFormValues, { abortEarly: false })
      .then((validData) => {
        setErrors([]);
        console.debug("Valid Data:", validData);
      })
      .catch((errors) => {
        setErrors(errors);
        console.error("Validation Errors:", errors.errors);
      });
  }, [changed, orderFormValues]);

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

            sampleXmlString += `<sample sampleID='${sampleItem.sampleTypeId}' date='${sampleItem.sampleXML.collectionDate}' time='${sampleItem.sampleXML.collectionTime}' collector='${sampleItem.sampleXML.collector}' quantity='${sampleItem.sampleXML.quantity}' uom='${sampleItem.sampleXML.uom}' tests='${tests}' testSectionMap='' testSampleTypeMap='' panels='${panels}' rejected='${sampleItem.sampleXML.rejected}' rejectReasonId='${sampleItem.sampleXML.rejectionReason}' initialConditionIds='' storageLocationId='${storageLocationId}' storageLocationType='${storageLocationType}' storagePositionCoordinate='${storagePositionCoordinate}'/>`;
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
                setPhoneValidation={setPhoneValidation}
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
                changed={changed}
                setChanged={setChanged}
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
