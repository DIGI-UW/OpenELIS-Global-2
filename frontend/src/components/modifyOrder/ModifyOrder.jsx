import React, { useContext, useEffect, useState, useRef } from "react";
import {
  Button,
  ProgressIndicator,
  ProgressStep,
  Stack,
  Grid,
  Column,
  InlineNotification,
} from "@carbon/react";
import EditSample from "./EditSample";
import AddOrder from "../addOrder/AddOrder";
import "../addOrder/add-order.scss";
import { ModifyOrderFormValues } from "../formModel/innitialValues/OrderEntryFormValues";
import { ConfigurationContext, NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import {
  postToOpenElisServerFullResponse,
  getFromOpenElisServer,
  resolveApiErrorMessage,
} from "../utils/Utils";
import EditOrderEntryAdditionalQuestions from "./EditOrderEntryAdditionalQuestions";
import OrderSuccessMessage from "../addOrder/OrderSuccessMessage";
import { FormattedMessage, useIntl } from "react-intl";
import PatientHeader from "../common/PatientHeader";
import PageBreadCrumb from "../common/PageBreadCrumb";
import createModifyOrderEntryValidationSchema from "../formModel/validationSchema/ModifyOrderEntryValidationSchema";
import { sampleObject } from "../addOrder/Index";
/**
 * The edit page of the workflow an order was entered in, when that is not the
 * clinical one. Environmental and vector orders have no patient, so the
 * clinical wizard can only show them as "No Patient Information Available";
 * they are edited on their own workflow's Enter Order page instead.
 */
export const nonClinicalEditPath = (order) => {
  const workflowType =
    order?.sampleOrderItems?.environmentalFields?.workflowType;
  if (
    !order?.labNumber ||
    (workflowType !== "environmental" && workflowType !== "vector")
  ) {
    return null;
  }
  return `/order/${workflowType}/enter?labNumber=${encodeURIComponent(order.labNumber)}`;
};

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sample.label.search.Order", link: "/SampleEdit" },
];

const ModifyOrder = () => {
  const componentMounted = useRef(false);

  const intl = useIntl();
  const { configurationProperties = {} } = useContext(ConfigurationContext);

  const firstPageNumber = 0;
  const lastPageNumber = 3;
  const programPageNumber = firstPageNumber + 0;
  const samplePageNumber = firstPageNumber + 1;
  const orderPageNumber = firstPageNumber + 2;
  const successMsgPageNumber = lastPageNumber;

  const [page, setPage] = useState(firstPageNumber);
  const [orderFormValues, setOrderFormValues] = useState(ModifyOrderFormValues);
  const [staleSave, setStaleSave] = useState(null);
  const [samples, setSamples] = useState([sampleObject]);
  const [errors, setErrors] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [patientId, setPatientId] = useState("");
  const [orderNotFound, setOrderNotFound] = useState(false);
  const [patientHeaderInfo, setPatientHeaderInfo] = useState({
    patientName: "",
    gender: "",
    dob: "",
    nationalId: "",
    patientId: "",
    subjectNumber: "",
    accessionNumber: "",
  });
  const [changed, setChanged] = useState({
    "sampleOrderItems.providerFirstName": false,
    "sampleOrderItems.providerLastName": false,
    "sampleOrderItems.labNo": false,
  });

  useEffect(() => {
    componentMounted.current = true;
    let patientIdParam = new URLSearchParams(window.location.search).get(
      "patientId",
    );
    let accessionNumber = new URLSearchParams(window.location.search).get(
      "accessionNumber",
    );
    accessionNumber = accessionNumber ? accessionNumber : "";
    patientIdParam = patientIdParam ? patientIdParam : "";

    const loadForModify = () => {
      // If searching by accession number and no patientId, fetch patient from accession number
      if (!patientIdParam && accessionNumber) {
        getFromOpenElisServer(
          "/rest/patientByLabNumer?accessionNumber=" + accessionNumber,
          (response) => {
            if (componentMounted.current && response && response.id) {
              setPatientId(response.id);
            }
          },
        );
      } else {
        setPatientId(patientIdParam);
      }

      getFromOpenElisServer(
        "/rest/SampleEdit?patientId=" +
          patientIdParam +
          "&accessionNumber=" +
          accessionNumber,
        loadOrderValues,
      );
    };

    if (!patientIdParam && accessionNumber) {
      getFromOpenElisServer(
        "/rest/order/search?labNumber=" + encodeURIComponent(accessionNumber),
        (order) => {
          if (!componentMounted.current) return;
          const editPath = nonClinicalEditPath(order);
          if (editPath) {
            window.location.replace(editPath);
            return;
          }
          loadForModify();
        },
      );
    } else {
      loadForModify();
    }
    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    createModifyOrderEntryValidationSchema(configurationProperties)
      .validate(orderFormValues, {
        abortEarly: false,
      })
      .then((validData) => {
        setErrors([]);
        console.debug("Valid Data:", validData);
      })
      .catch((errors) => {
        setErrors(errors);
        console.debug("Validation Errors:", errors.errors);
      });
  }, [changed, configurationProperties, orderFormValues]);

  const loadOrderValues = (data) => {
    if (componentMounted.current) {
      if (data?.noSampleFound) {
        setOrderNotFound(true);
        return;
      }
      if (data?.sampleOrderItems) {
        // OGC-1191 — Do not blank the loaded referring-site name. It carried
        // over from the Vite migration and left a required field empty in form
        // state while the AutoComplete still displayed it from referringSiteId,
        // hiding the emptied value from the user.
        setOrderFormValues(data);
        setPatientHeaderInfo({
          patientName: data.patientName || "",
          gender: data.gender || "",
          dob: data.dob || "",
          nationalId: data.nationalId || "",
          patientId: data.patientId || "",
          subjectNumber: data.subjectNumber || "",
          accessionNumber: data.accessionNumber || "",
        });
      }
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

  // OGC-1191 — after a successful reassignment the specimen carries its new
  // accession, so the post-save label print and the page URL must both switch
  // to the new number; the old one no longer exists. No-op when nothing was
  // reassigned.
  const reflectReassignmentOnSuccess = () => {
    const reassigned = orderFormValues.newAccessionNumber;
    if (!reassigned || reassigned === orderFormValues.accessionNumber) {
      return;
    }
    setOrderFormValues({
      ...orderFormValues,
      accessionNumber: reassigned,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        labNo: reassigned,
      },
      newAccessionNumber: "",
    });
    const url = new URL(window.location.href);
    url.searchParams.set("accessionNumber", reassigned);
    window.history.replaceState(null, "", url.toString());
  };

  // Advance to the success page only after the backend confirms. On 4xx/5xx,
  // surface the actual reason from the response body (a translatable
  // messageKey, or {"message":"..."} on errors like "Position B12 is already
  // occupied") instead of the generic server.error.msg.
  const handlePost = async (response) => {
    setIsSubmitting(false);
    if (response && response.ok) {
      reflectReassignmentOnSuccess();
      showAlertMessage(
        <FormattedMessage id="save.order.success.msg" />,
        NotificationKinds.success,
      );
      setPage(page + 1);
      return;
    }
    let body;
    if (response) {
      try {
        body = await response.json();
      } catch (_) {
        // Body wasn't JSON — fall through to the generic key.
      }
    }
    if (
      response?.status === 409 &&
      body?.messageKey === "error.order.staleSave"
    ) {
      setStaleSave(resolveApiErrorMessage(intl, body, "server.error.msg"));
      return;
    }
    showAlertMessage(
      resolveApiErrorMessage(intl, body, "server.error.msg"),
      NotificationKinds.error,
    );
  };
  const handleSubmitOrderForm = (e) => {
    e.preventDefault();
    if (isSubmitting) {
      return;
    }
    setIsSubmitting(true);
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
    postToOpenElisServerFullResponse(
      "/rest/SampleEdit",
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

            // Extract storage location data if present
            const storageLocation = sampleItem.sampleXML?.storageLocation;
            const storageLocationId = storageLocation?.id || "";
            const storageLocationType = storageLocation?.type || "";
            const storagePositionCoordinate =
              storageLocation?.positionCoordinate || "";

            sampleXmlString += `<sample sampleID='${sampleItem.sampleTypeId}' date='${sampleItem.sampleXML.collectionDate}' time='${sampleItem.sampleXML.collectionTime}' collector='${sampleItem.sampleXML.collector}' tests='${tests}' testSectionMap='' testSampleTypeMap='' panels='' rejected='${sampleItem.sampleXML.rejected}' rejectReasonId='${sampleItem.sampleXML.rejectionReason}' initialConditionIds='' storageLocationId='${storageLocationId}' storageLocationType='${storageLocationType}' storagePositionCoordinate='${storagePositionCoordinate}' />`;
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
      <br />

      {orderNotFound ? (
        <InlineNotification
          kind="error"
          hideCloseButton
          lowContrast
          title={intl.formatMessage({ id: "sample.search.nosample" })}
        />
      ) : (
        <PatientHeader
          id={patientId}
          patientName={patientHeaderInfo.patientName}
          gender={patientHeaderInfo.gender}
          dob={patientHeaderInfo.dob}
          nationalId={patientHeaderInfo.nationalId}
          patientId={patientHeaderInfo.patientId}
          subjectNumber={patientHeaderInfo.subjectNumber}
          accesionNumber={patientHeaderInfo.accessionNumber}
          className="patient-header2"
          isOrderPage={true}
        >
          {" "}
        </PatientHeader>
      )}
      <Grid>
        <Column lg={16} md={8} sm={4}>
          <Stack gap={10}>
            <div className="pageContent">
              {notificationVisible === true ? <AlertDialog /> : ""}
              {!orderNotFound && orderFormValues?.sampleOrderItems && (
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
                  {/* OGC-1191 — Submit is gated on these validation errors but
                      they were computed and never shown, so a required field
                      the user cannot see (e.g. Requester Last Name) left Submit
                      permanently disabled with nothing on screen explaining
                      why. Surface each gating error so the block is legible. */}
                  {page === orderPageNumber &&
                    errors?.errors?.length > 0 &&
                    errors.errors.map((message, index) => (
                      <InlineNotification
                        key={index}
                        kind="error"
                        lowContrast
                        hideCloseButton
                        title={intl.formatMessage({ id: "error.title" })}
                        subtitle={message}
                        data-cy="modify-order-validation-error"
                      />
                    ))}
                  {page === orderPageNumber && staleSave && (
                    <div data-cy="modify-order-stale-save">
                      <InlineNotification
                        kind="error"
                        lowContrast
                        hideCloseButton
                        title={intl.formatMessage({ id: "error.title" })}
                        subtitle={staleSave}
                      />
                      <Button
                        kind="secondary"
                        size="sm"
                        onClick={() => window.location.reload()}
                      >
                        <FormattedMessage id="label.results.refresh" />
                      </Button>
                    </div>
                  )}
                  <div className="navigationButtonsLayout">
                    {page !== firstPageNumber && page <= orderPageNumber && (
                      <Button
                        kind="tertiary"
                        onClick={() => navigateBackWards()}
                      >
                        <FormattedMessage id="back.action.button" />
                      </Button>
                    )}

                    {page < orderPageNumber && (
                      <Button
                        data-cy="next-button"
                        kind="primary"
                        className="forwardButton"
                        onClick={() => navigateForward()}
                      >
                        <FormattedMessage id="next.action.button" />
                      </Button>
                    )}

                    {page === orderPageNumber && (
                      <Button
                        data-cy="submit-order"
                        kind="primary"
                        className="forwardButton"
                        onClick={handleSubmitOrderForm}
                        disabled={
                          isSubmitting ||
                          Boolean(staleSave) ||
                          errors?.errors?.length > 0
                            ? true
                            : false
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
        </Column>
      </Grid>
    </>
  );
};

export default ModifyOrder;
