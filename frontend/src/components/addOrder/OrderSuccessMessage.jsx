import React, { useEffect } from "react";
import { Button, Row, Stack } from "@carbon/react";
import { CheckmarkFilled } from "@carbon/icons-react";
import config from "../../config.json";
import { SampleOrderFormValues } from "../formModel/innitialValues/OrderEntryFormValues";
import { sampleObject } from "./Index";
import { FormattedMessage, useIntl } from "react-intl";
import PostSavePrintDialog from "../barcodeWorkflow/PostSavePrintDialog";

// Mirror BarcodeWorkflowPrintServiceImpl.mapLabelTypeForUrl on the backend:
// the servlet/BarcodeLabelMaker expect the *Order suffix variants for
// pathology and freezer cases even though the dialog's labelType is the
// simpler form. Keep this mapping in lockstep with the Java side so the
// fallback URL doesn't open the wrong servlet branch (or, in the case of
// freezer, an unimplemented one) when the backend printUrl is missing.
const mapLabelTypeForUrl = (labelType) => {
  if (labelType === "block") return "blockOrder";
  if (labelType === "slide") return "slideOrder";
  if (labelType === "freezer") return "freezerOrder";
  return labelType;
};

// Build a safe local fallback URL for the rare case that the backend doesn't
// supply printUrl on a printable label entry. The shape mirrors what
// BarcodeWorkflowPrintServiceImpl.buildPrintUrl produces, with every component
// URL-encoded so accession numbers / label types containing reserved
// characters can't malform the request.
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
  const printableTypes =
    dialogModel?.printableLabelTypes &&
    dialogModel.printableLabelTypes.length > 0
      ? dialogModel.printableLabelTypes
      : ["order"];

  // Pass each backend-supplied printable label entry through with its
  // quantity, sampleNumber, and printUrl intact. The dialog opens
  // entry.printUrl directly, so the URL-construction logic stays in one
  // place (BarcodeWorkflowPrintServiceImpl), with this fallback only
  // covering the edge case where printUrl is missing entirely.
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

  // Done on the success page resets the form and returns the user to page 0
  // so they can start a fresh order. Without an explicit onDone, the dialog
  // hides the Done button entirely (rather than no-op when clicked).
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
