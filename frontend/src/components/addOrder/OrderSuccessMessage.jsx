import React, { useContext, useEffect } from "react";
import { Button, Row, Stack } from "@carbon/react";
import { Checkmark, CheckmarkFilled } from "@carbon/icons-react";
import config from "../../config.json";
import { SampleOrderFormValues } from "../formModel/innitialValues/OrderEntryFormValues";
import { sampleObject } from "./Index";
import { FormattedMessage, useIntl } from "react-intl";
import PostSavePrintDialog from "../barcodeWorkflow/PostSavePrintDialog";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds } from "../common/CustomNotification";

// Mirror of BarcodeWorkflowPrintServiceImpl.mapLabelTypeForUrl. Keep both
// sides in lockstep: the bare types silently produce empty PDFs server-side.
const mapLabelTypeForUrl = (labelType) => {
  if (labelType === "block") return "blockOrder";
  if (labelType === "slide") return "slideOrder";
  if (labelType === "freezer") return "freezerOrder";
  return labelType;
};

// Fallback URL for the rare case that the backend omits printUrl. Mirrors
// BarcodeWorkflowPrintServiceImpl.buildPrintUrl with every component encoded.
// Specimen entries MUST include the sortOrder suffix (labNo.<n>) — otherwise
// LabelMakerServlet treats type=specimen as "every sample item" and multiplies
// the count by N, recreating the bug this PR fixes elsewhere.
const buildFallbackPrintUrl = (
  accessionNumber,
  labelType,
  quantity,
  sampleNumber,
) => {
  const safeQuantity = quantity > 0 ? quantity : 1;
  const servletType = mapLabelTypeForUrl(labelType || "");
  const labNo =
    sampleNumber != null
      ? `${accessionNumber || ""}.${sampleNumber}`
      : accessionNumber || "";
  return (
    config.serverBaseUrl +
    "/LabelMakerServlet" +
    `?labNo=${encodeURIComponent(labNo)}` +
    `&type=${encodeURIComponent(servletType)}` +
    `&quantity=${safeQuantity}`
  );
};

const OrderSuccessMessage = (props) => {
  const {
    orderFormValues,
    setOrderFormValues,
    setSamples,
    setPage,
    saveResponse,
  } = props;
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const dialogModel = saveResponse?.postSavePrintDialog;
  const accessionNumber =
    dialogModel?.accessionNumber || orderFormValues.sampleOrderItems.labNo;
  // An explicit empty array means the backend has no printable labels —
  // honour it. Only fall back to a default Order entry when the dialog model
  // itself is absent (legacy server / failed POST).
  const printableTypes = dialogModel?.printableLabelTypes ?? ["order"];

  // Forward each backend entry's quantity / sampleNumber / printUrl unchanged.
  // The dialog opens printUrl directly, so URL building stays centralized in
  // BarcodeWorkflowPrintServiceImpl; the fallback only covers a missing URL.
  const printableLabels = printableTypes.map((labelType) => {
    const isObject = typeof labelType !== "string";
    const normalizedType = isObject ? labelType.labelType : labelType;
    const quantity =
      isObject && labelType.quantity > 0 ? labelType.quantity : 1;
    const sampleNumber = isObject ? labelType.sampleNumber : null;
    const backendUrl = isObject ? labelType.printUrl : "";
    return {
      labelType: normalizedType,
      sampleNumber,
      quantity,
      printUrl:
        backendUrl ||
        buildFallbackPrintUrl(
          accessionNumber,
          normalizedType,
          quantity,
          sampleNumber,
        ),
    };
  });

  // Resets the form so the user can start a fresh order. The Done button
  // belongs to this consumer (not the dialog) — the dialog is reused on case
  // views where there is no "done" semantic.
  const handleDone = () => {
    setOrderFormValues(SampleOrderFormValues);
    setSamples([sampleObject]);
    setPage(0);
  };

  // Mirror OrderLabel.jsx: a blocked popup never opened the PDF, so surface an
  // error toast instead of the silent console.warn the dialog logs by default.
  const handlePopupBlocked = () => {
    addNotification({
      kind: NotificationKinds.error,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({
        id: "label.print.error.popupBlocked",
        defaultMessage:
          "Popup blocked. Please allow popups for this site to print labels.",
      }),
    });
    setNotificationVisible(true);
  };

  const handleAnotherSiteOrder = () => {
    const siteId = orderFormValues.sampleOrderItems.referringSiteId;
    const siteName = orderFormValues.sampleOrderItems.referringSiteName;
    const providerId = orderFormValues.sampleOrderItems.providerId;
    const providerFirstName =
      orderFormValues.sampleOrderItems.providerFirstName;
    const providerLastName = orderFormValues.sampleOrderItems.providerLastName;
    const providerWorkPhone =
      orderFormValues.sampleOrderItems.providerWorkPhone;
    const providerFax = orderFormValues.sampleOrderItems.providerFax;
    const providerEmail = orderFormValues.sampleOrderItems.providerEmail;

    setOrderFormValues(SampleOrderFormValues);

    setOrderFormValues({
      ...SampleOrderFormValues,
      rememberSiteAndRequester: true,
      sampleOrderItems: {
        ...SampleOrderFormValues.sampleOrderItems,
        referringSiteId: siteId,
        referringSiteName: siteName,
        providerId: providerId,
        providerFirstName: providerFirstName,
        providerLastName: providerLastName,
        providerWorkPhone: providerWorkPhone,
        providerFax: providerFax,
        providerEmail: providerEmail,
      },
    });
    setPage(0);
  };

  useEffect(() => {
    if (!orderFormValues.rememberSiteAndRequester) {
      setOrderFormValues(SampleOrderFormValues);
    }
    setSamples([sampleObject]);
  }, []);

  return (
    <div className="orderLegendBody">
      <div className="orderEntrySuccessMsg">
        <Stack gap={4} className="orderEntrySuccessHeader">
          <CheckmarkFilled
            size={120}
            className="orderEntrySuccessIcon"
            aria-label={intl.formatMessage({ id: "save.success" })}
          />
          <h4 className="orderEntrySuccessTitle">
            <FormattedMessage id="save.success" />
          </h4>
        </Stack>
        <div className="orderEntrySuccessPrintPanel">
          <PostSavePrintDialog
            accessionNumber={accessionNumber}
            printableLabelTypes={printableLabels}
            onPopupBlocked={handlePopupBlocked}
          />
        </div>
        <Row className="orderEntrySuccessActions">
          {orderFormValues.rememberSiteAndRequester && (
            <Button
              className="placeAnotherOrderBtn"
              kind="tertiary"
              onClick={handleAnotherSiteOrder}
            >
              <FormattedMessage id="request.samesite.order" />
            </Button>
          )}
          <Button
            className="orderEntryDoneBtn"
            kind="secondary"
            renderIcon={Checkmark}
            onClick={handleDone}
          >
            <FormattedMessage id="barcode.print.done" />
          </Button>
        </Row>
      </div>
    </div>
  );
};

export default OrderSuccessMessage;
