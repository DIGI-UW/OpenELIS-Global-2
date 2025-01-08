import React, { useContext, useEffect, useState, useRef } from "react";
import { useParams } from "react-router-dom";
import {
  Button,
  ProgressIndicator,
  ProgressStep,
  Stack,
  Section,
  Tag,
} from "@carbon/react";
import EditSample from "./EditSample";
import AddOrder from "../addOrder/AddOrder";
import "../addOrder/add-order.scss";
import { ModifyOrderFormValues } from "../formModel/innitialValues/OrderEntryFormValues";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { postToOpenElisServer, getFromOpenElisServer } from "../utils/Utils";
import EditOrderEntryAdditionalQuestions from "./EditOrderEntryAdditionalQuestions";
import OrderSuccessMessage from "../addOrder/OrderSuccessMessage";
import { FormattedMessage, useIntl } from "react-intl";
import PatientHeader from "../common/PatientHeader";
import PageBreadCrumb from "../common/PageBreadCrumb";
import ModifyOrderEntryValidationSchema from "../formModel/validationSchema/ModifyOrderEntryValidationSchema";
let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sample.label.search.Order", link: "/SampleEdit" },
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
const ModifyOrder = () => {
  const componentMounted = useRef(false);

  const intl = useIntl();

  const firstPageNumber = 0;
  const lastPageNumber = 3;
  const programPageNumber = firstPageNumber + 0;
  const samplePageNumber = firstPageNumber + 1;
  const orderPageNumber = firstPageNumber + 2;
  const successMsgPageNumber = lastPageNumber;

  const [page, setPage] = useState(firstPageNumber);
  const [orderFormValues, setOrderFormValues] = useState(ModifyOrderFormValues);
  const [samples, setSamples] = useState([sampleObject]);
  const [errors, setErrors] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    componentMounted.current = true;
    let patientId = new URLSearchParams(window.location.search).get(
      "patientId",
    );
    let accessionNumber = new URLSearchParams(window.location.search).get(
      "accessionNumber",
    );
    accessionNumber = accessionNumber ? accessionNumber : "";
    patientId = patientId ? patientId : "";
    getFromOpenElisServer(
      "/rest/sample-edit?patientId=" +
        patientId +
        "&accessionNumber=" +
        accessionNumber,
      loadOrderValues,
    );
    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    ModifyOrderEntryValidationSchema.validate(orderFormValues, {
      abortEarly: false,
    })
      .then((validData) => {
        setErrors([]);
        console.debug("Valid Data:", validData);
      })
      .catch((errors) => {
        setErrors(errors);
        console.error("Validation Errors:", errors.errors);
      });
  }, [orderFormValues]);

  const loadOrderValues = (data) => {
    if (componentMounted.current) {
      data.sampleOrderItems.referringSiteName = "";
      setOrderFormValues(data);
    }
  };

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

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
    } else {
      showAlertMessage(
        <FormattedMessage id="server.error.msg" />,
        NotificationKinds.error,
      );
    }
  };
  const handleSubmitOrderForm = (e) => {
    e.preventDefault();
    if (isSubmitting) {
      return;
    }
    setIsSubmitting(true);
    setPage(page + 1);
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
      "/rest/sample-edit",
      JSON.stringify(orderFormValues),
      handlePost,
    );
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
  useEffect(() => {
    if (page === samplePageNumber + 1) {
      attacheSamplesToFormValues();
    }
  }, [page]);

  const attacheSamplesToFormValues = () => {
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
    setOrderFormValues({
      ...orderFormValues,
      // useReferral: true,
      sampleXML: sampleXmlString,
      // referralItems: referralItems,
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

      <PatientHeader
        id={orderFormValues?.nationalId}
        patientName={orderFormValues?.patientName}
        gender={orderFormValues?.gender}
        dob={orderFormValues?.dob}
        nationalId={orderFormValues?.nationalId}
        accesionNumber={orderFormValues?.accessionNumber}
        className="patient-header3"
        isOrderPage={true}
      >
        {" "}
      </PatientHeader>
      <Stack gap={10}>
        <div className="pageContent">
          {notificationVisible === true ? <AlertDialog /> : ""}
          {orderFormValues?.sampleOrderItems && (
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
                    disabled={orderFormValues.sampleOrderItems.labNo == ""}
                    label={intl.formatMessage({
                      id: "order.step.program.selection",
                    })}
                  />
                  <ProgressStep
                    disabled={orderFormValues.sampleOrderItems.labNo == ""}
                    label={intl.formatMessage({ id: "sample.add.action" })}
                  />
                  <ProgressStep
                    disabled={orderFormValues.sampleOrderItems.labNo == ""}
                    label={intl.formatMessage({ id: "order.label.add" })}
                  />
                </ProgressIndicator>
              )}
              {page === programPageNumber && (
                <EditOrderEntryAdditionalQuestions
                  orderFormValues={orderFormValues}
                  setOrderFormValues={setOrderFormValues}
                />
              )}
              {page === samplePageNumber && (
                <EditSample
                  orderFormValues={orderFormValues}
                  setOrderFormValues={setOrderFormValues}
                  setSamples={setSamples}
                  samples={samples}
                  error={elementError}
                />
              )}
              {page === orderPageNumber && (
                <AddOrder
                  orderFormValues={orderFormValues}
                  setOrderFormValues={setOrderFormValues}
                  samples={samples}
                  error={elementError}
                  isModifyOrder={true}
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
                    onClick={handleSubmitOrderForm}
                    disabled={
                      isSubmitting || errors?.errors?.length > 0 ? true : false
                    }
                  >
                    <FormattedMessage id="label.button.submit" />
                  </Button>
                )}
              </div>
            </div>
          )}
        </div>
      </Stack>
    </>
  );
};

export default ModifyOrder;
