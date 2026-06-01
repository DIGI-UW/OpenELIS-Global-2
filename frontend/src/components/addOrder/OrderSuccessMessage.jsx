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

  // OGC-285 M6 (T172): when the save response carries the persisted
  // order_label_request rows (the JSONB-snapshot model — preset name + chosen
  // qty + frozen dimensions per preset), drive the dialog from THOSE. Each row's
  // Print button hits the snapshot reprint endpoint
  // GET /api/barcode/print/{parentSampleId}/{presetId}, which renders from the
  // FROZEN snapshot (AC-20) — it deliberately never re-derives from the live
  // preset or from LabelMakerServlet. parent_sample_id is the internal Sample id
  // carried on each view row, so no separate id field is needed.
  //
  // Until the backend echoes orderLabelRequests in the save response,
  // persistedRequests is undefined and we fall through to the legacy
  // printableLabelTypes (LabelMakerServlet) path below, unchanged.
  const persistedRequests = saveResponse?.orderLabelRequests;

  const buildSnapshotReprintUrl = (parentSampleId, presetId) =>
    `${config.serverBaseUrl}/api/barcode/print/${encodeURIComponent(
      parentSampleId,
    )}/${encodeURIComponent(presetId)}`;

  const mapPersistedRequest = (request) => {
    const snapshot = request?.presetSnapshot ?? request?.preset_snapshot;
    const preset = snapshot?.preset;
    const labelName = preset?.name ?? request?.labelType ?? "";
    const savedQty = request?.qty > 0 ? request.qty : 1;
    const presetId = request?.presetId ?? request?.preset_id ?? preset?.id;
    const parentSampleId =
      request?.parentSampleId ?? request?.parent_sample_id ?? null;
    const sampleNumber =
      typeof request?.sampleNumber === "number" ? request.sampleNumber : null;
    const dimensionsMm =
      preset?.height_mm && preset?.width_mm
        ? `${preset.width_mm} × ${preset.height_mm} mm`
        : "";
    // The snapshot reprint URL requires both the Sample id and the preset id.
    // If either is missing we leave printUrl blank rather than re-deriving from
    // the accession (which would bypass the frozen snapshot — see AC-20).
    const printUrl =
      parentSampleId != null && presetId != null
        ? buildSnapshotReprintUrl(parentSampleId, presetId)
        : "";
    return {
      presetId: presetId ?? null,
      labelName,
      sampleNumber,
      savedQty,
      dimensionsMm,
      printUrl,
    };
  };

  // An explicit empty array means the backend has no printable labels —
  // honour it. Only fall back to a default Order entry when the dialog model
  // itself is absent (legacy server / failed POST).
  const printableTypes = dialogModel?.printableLabelTypes ?? ["order"];

  // Legacy (OGC-284) path: each entry is a LabelMakerServlet URL keyed by
  // accession. Maps onto the dialog's preset-row shape with presetId=null so the
  // NumberInput is capped at the backend's quantity and Print opens the URL.
  const printableLabels = Array.isArray(persistedRequests)
    ? persistedRequests.map(mapPersistedRequest)
    : printableTypes.map((labelType) => {
        const isObject = typeof labelType !== "string";
        const normalizedType = isObject ? labelType.labelType : labelType;
        const quantity =
          isObject && labelType.quantity > 0 ? labelType.quantity : 1;
        const sampleNumber = isObject ? labelType.sampleNumber : null;
        const backendUrl = isObject ? labelType.printUrl : "";
        return {
          presetId: null,
          labelName: normalizedType,
          sampleNumber,
          savedQty: quantity,
          dimensionsMm: isObject ? (labelType.dimensionsMm ?? "") : "",
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
            onSkip={handleDone}
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
