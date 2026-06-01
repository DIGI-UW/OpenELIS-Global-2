import React, { useContext, useEffect, useState } from "react";
import { Button, Row, Stack } from "@carbon/react";
import { Checkmark, CheckmarkFilled } from "@carbon/icons-react";
import config from "../../config.json";
import { SampleOrderFormValues } from "../formModel/innitialValues/OrderEntryFormValues";
import { sampleObject } from "./Index";
import { FormattedMessage, useIntl } from "react-intl";
import PostSavePrintDialog from "../barcodeWorkflow/PostSavePrintDialog";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds } from "../common/CustomNotification";
import { getFromOpenElisServer } from "../utils/Utils";

// Single Order print fallback for an order with no persisted label snapshot
// (e.g. a no-test order: AddOrder only fetches the test-driven aggregation when
// a sample carries tests, so such orders persist no order_label_request rows).
// System presets cover the no-test flow via the Order label; this mirrors the
// existing ExistingOrder.jsx order-print URL.
const buildOrderFallbackPrintUrl = (accessionNumber) =>
  config.serverBaseUrl +
  "/LabelMakerServlet" +
  `?labNo=${encodeURIComponent(accessionNumber || "")}` +
  `&type=order&quantity=1`;

const OrderSuccessMessage = (props) => {
  const { orderFormValues, setOrderFormValues, setSamples, setPage } = props;
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const accessionNumber = orderFormValues.sampleOrderItems.labNo;

  // OGC-285: drive the post-save print dialog from the persisted
  // order_label_request rows (the JSONB-snapshot model — preset name + chosen
  // qty + frozen dimensions per preset). The just-saved order's rows are
  // committed before the save POST returns, so we re-read them from the
  // accession-keyed endpoint (frontends hold the accession, not the Sample PK).
  // Each row's Print button hits GET /api/barcode/print/{parentSampleId}/{presetId},
  // which renders from the FROZEN snapshot (AC-20) — never re-deriving from the
  // live preset or LabelMakerServlet. The legacy BarcodeWorkflowPrintService
  // LabelsSection/PostSavePrintDialog (count) model this component used to
  // consume from the save response is gone.
  const [persistedRequests, setPersistedRequests] = useState(null);

  useEffect(() => {
    if (!accessionNumber) {
      setPersistedRequests([]);
      return;
    }
    getFromOpenElisServer(
      `/api/orders/by-accession/${encodeURIComponent(accessionNumber)}/labels`,
      (response) => {
        setPersistedRequests(Array.isArray(response) ? response : []);
      },
    );
  }, [accessionNumber]);

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

  // Drive the dialog from the persisted snapshot rows. When the order has no
  // persisted rows (a no-test order — see above), fall back to a single Order
  // label so the no-test flow still prints an order barcode.
  const hasPersisted =
    Array.isArray(persistedRequests) && persistedRequests.length > 0;
  const printableLabels = hasPersisted
    ? persistedRequests.map(mapPersistedRequest)
    : [
        {
          presetId: null,
          labelName: "order",
          sampleNumber: null,
          savedQty: 1,
          dimensionsMm: "",
          printUrl: buildOrderFallbackPrintUrl(accessionNumber),
        },
      ];

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
