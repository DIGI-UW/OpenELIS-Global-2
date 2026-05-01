import React, { useEffect } from "react";
import { Button, Row, Stack } from "@carbon/react";
import { CheckmarkFilled } from "@carbon/icons-react";
import config from "../../config.json";
import { SampleOrderFormValues } from "../formModel/innitialValues/OrderEntryFormValues";
import { sampleObject } from "./Index";
import { FormattedMessage, useIntl } from "react-intl";
import PostSavePrintDialog from "../barcodeWorkflow/PostSavePrintDialog";

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
const buildFallbackPrintUrl = (accessionNumber, labelType, quantity) => {
  const safeQuantity = quantity > 0 ? quantity : 1;
  const servletType = mapLabelTypeForUrl(labelType || "");
  return (
    config.serverBaseUrl +
    "/LabelMakerServlet" +
    `?labNo=${encodeURIComponent(accessionNumber || "")}` +
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
        buildFallbackPrintUrl(accessionNumber, normalizedType, quantity),
    };
  });

  // Resets the form so the user can start a fresh order; PostSavePrintDialog
  // hides Done unless onDone is wired.
  const handleDone = () => {
    setOrderFormValues(SampleOrderFormValues);
    setSamples([sampleObject]);
    setPage(0);
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
            onDone={handleDone}
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
        </Row>
      </div>
    </div>
  );
};

export default OrderSuccessMessage;
