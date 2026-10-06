import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { Button, InlineNotification, SkeletonText, Tile } from "@carbon/react";
import { Renew } from "@carbon/icons-react";
import LabelsSection, {
  seedPersistPayload,
} from "../../../barcodeWorkflow/LabelsSection";
import { useOrderContext, SaveStatus } from "../../OrderContext";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import config from "../../../../config.json";

/**
 * The tubes that get labels: physical samples with a sample type, excluding
 * QC summaries and rejected specimens. Each keeps its position in the order's
 * sample list, which is also the position the save correlates label rows by.
 */
export const labelSamplesOf = (samples) =>
  (samples || [])
    .map((sample, index) => ({ sample, index }))
    .filter(
      ({ sample }) =>
        sample &&
        sample.sampleTypeId &&
        !sample.qcMetadata?.qcType &&
        !sample.sampleRejected,
    );

const testIdsOf = (sample) =>
  (sample.tests || [])
    .map((test) => Number(test?.id ?? test))
    .filter((id) => !Number.isNaN(id));

/**
 * A saved tube is addressed by its own sample item id (prefixed so it can never
 * be mistaken for a list position); an unsaved one by its position, which the
 * save resolves once the item exists.
 */
export const localIdOf = ({ sample, index }) =>
  sample.sampleItemId ? `item-${sample.sampleItemId}` : String(index);

/** The POST /api/orderEntry/labelRequest body for the order's tubes. */
export const buildLabelRequestBody = (labelSamples) => ({
  test_ids: [
    ...new Set(labelSamples.flatMap(({ sample }) => testIdsOf(sample))),
  ],
  samples: labelSamples.map((entry) => ({
    sample_id_local: localIdOf(entry),
    sample_type: entry.sample.sampleTypeId,
    test_ids: testIdsOf(entry.sample),
  })),
});

const clampToMax = (qty, max) =>
  max > 0 ? Math.min(Math.max(0, qty), max) : Math.max(0, qty);

/**
 * The quantities the order already saved take the place of the presets'
 * proposals: an order row per order-level preset, a tube row per saved sample
 * item and preset. A cell with a saved row is marked "saved"; a cell without
 * one (a new tube, a preset added since) keeps its proposal and leaves the
 * section pending until the next save.
 */
export const applySavedQuantities = (labelRequest, savedRows) => {
  // The saved rows arrive with snake_case wire keys (preset_id, sample_item_id).
  const rows = (Array.isArray(savedRows) ? savedRows : []).map((row) => ({
    presetId: row.preset_id ?? row.presetId,
    sampleItemId: row.sample_item_id ?? row.sampleItemId ?? null,
    qty: row.qty,
  }));
  let pending = false;
  const orderRowFor = (presetId) =>
    rows.find(
      (row) => !row.sampleItemId && String(row.presetId) === String(presetId),
    );
  const sampleRowFor = (sampleIdLocal, presetId) => {
    if (!String(sampleIdLocal).startsWith("item-")) {
      return undefined;
    }
    const itemId = String(sampleIdLocal).slice("item-".length);
    return rows.find(
      (row) =>
        String(row.sampleItemId) === itemId &&
        String(row.presetId) === String(presetId),
    );
  };
  const applyCell = (cell, saved) => {
    if (!saved) {
      pending = true;
      return cell;
    }
    return {
      ...cell,
      default: clampToMax(Number(saved.qty) || 0, cell.max),
      source: "saved",
    };
  };
  const applied = {
    ...labelRequest,
    order_row: {
      ...(labelRequest.order_row || {}),
      cells: (labelRequest.order_row?.cells || []).map((cell) =>
        applyCell(cell, orderRowFor(cell.preset_id)),
      ),
    },
    sample_rows: (labelRequest.sample_rows || []).map((row) => ({
      ...row,
      cells: (row.cells || []).map((cell) =>
        applyCell(cell, sampleRowFor(row.sample_id_local, cell.preset_id)),
      ),
    })),
  };
  return { labelRequest: applied, pending };
};

const buildPdfUrl = (orderId, target) => {
  const params = new URLSearchParams();
  if (target?.presetId != null) {
    params.set("presetId", String(target.presetId));
  }
  if (target?.sampleItemId) {
    params.set("sampleItemId", String(target.sampleItemId));
  }
  if (target?.scope === "order" || target?.scope === "sample") {
    params.set("scope", target.scope);
  }
  const query = params.toString();
  return `${config.serverBaseUrl}/api/orders/${encodeURIComponent(orderId)}/labels/pdf${query ? `?${query}` : ""}`;
};

/**
 * Labels section of Prepare Samples (OGC-1422, FR-I2 to FR-I7): the order's
 * label types from the active presets and the test catalog, a quantity per
 * tube and per label type, and Print row / Print column / Print all. The
 * quantities travel with the step's save (FR-E5); printing saves first when
 * anything is pending, then opens the saved labels as a PDF (FR-I6). A print
 * that fails says so and offers Retry; a blocked print window falls back to
 * a download and says so.
 */
const PrepareLabelsSection = ({
  isReadOnly = false,
  onSaveBeforePrint,
  registerPrintRow,
}) => {
  const intl = useIntl();
  const {
    orderId,
    labNumber,
    samples,
    isDirty,
    saveStatus,
    setLabelPersistRequest,
  } = useOrderContext();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [labelRequest, setLabelRequest] = useState(null);
  const [loading, setLoading] = useState(false);
  const [loadFailed, setLoadFailed] = useState(false);
  const [printing, setPrinting] = useState(false);
  const [printError, setPrintError] = useState(null);
  const [quantitiesPending, setQuantitiesPending] = useState(false);

  const latestSamplesRef = useRef(samples);
  latestSamplesRef.current = samples;
  const latestOrderIdRef = useRef(orderId);
  latestOrderIdRef.current = orderId;
  const seededPendingRef = useRef(true);

  const labelSamples = useMemo(() => labelSamplesOf(samples), [samples]);
  const signature = useMemo(
    () =>
      JSON.stringify(
        labelSamples.map((entry) => [
          localIdOf(entry),
          entry.sample.sampleTypeId,
          testIdsOf(entry.sample),
        ]),
      ),
    [labelSamples],
  );

  useEffect(() => {
    if (labelSamples.length === 0) {
      setLabelRequest(null);
      setLabelPersistRequest(null);
      return undefined;
    }
    let cancelled = false;
    setLoading(true);
    setLoadFailed(false);
    const seed = (response, savedRows) => {
      const { labelRequest: applied, pending } = applySavedQuantities(
        response,
        savedRows,
      );
      seededPendingRef.current = pending;
      setLabelRequest(applied);
    };
    postToOpenElisServerJsonResponse(
      "/api/orderEntry/labelRequest",
      JSON.stringify(buildLabelRequestBody(labelSamples)),
      (response) => {
        if (cancelled) {
          return;
        }
        if (!response || response.error || !response.order_row) {
          setLoading(false);
          setLoadFailed(true);
          return;
        }
        const savedOrderId = latestOrderIdRef.current;
        if (!savedOrderId) {
          setLoading(false);
          seed(response, []);
          return;
        }
        getFromOpenElisServer(
          `/api/orders/${encodeURIComponent(savedOrderId)}/labels`,
          (savedRows) => {
            if (cancelled) {
              return;
            }
            setLoading(false);
            seed(response, savedRows);
          },
        );
      },
    );
    return () => {
      cancelled = true;
    };
    // The order id can arrive after the tubes (the step is opened by lab
    // number), and the saved rows are only readable once it has.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [signature, orderId]);

  useEffect(() => {
    if (!labelRequest) {
      return;
    }
    setLabelPersistRequest(seedPersistPayload(labelRequest));
    setQuantitiesPending(seededPendingRef.current);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [labelRequest]);

  useEffect(() => {
    if (saveStatus === SaveStatus.SAVED) {
      setQuantitiesPending(false);
    }
  }, [saveStatus]);

  const handleChange = ({ persistPayload }) => {
    setLabelPersistRequest(persistPayload);
    setQuantitiesPending(true);
  };

  const sampleFor = (sampleIdLocal) => {
    const current = latestSamplesRef.current || [];
    const byItem = current.find(
      (sample) =>
        sample.sampleItemId && `item-${sample.sampleItemId}` === sampleIdLocal,
    );
    if (byItem) {
      return byItem;
    }
    return current[Number(sampleIdLocal)];
  };

  const rowLabel = (row) => {
    const sample = sampleFor(row.sampleIdLocal);
    const position = (latestSamplesRef.current || []).indexOf(sample);
    const number = position >= 0 ? position + 1 : row.sampleNumber;
    const name = sample?.sampleTypeName || sample?.name || "";
    return `${labNumber ? `${labNumber}-${number}` : number} ${name}`.trim();
  };

  const notify = (kind, messageId) => {
    addNotification({
      kind,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({ id: messageId }),
    });
    setNotificationVisible(true);
  };

  const print = useCallback(
    async (target) => {
      setPrintError(null);
      // Open the window inside the click so popup blockers let it through,
      // then point it at the PDF once the labels are ready.
      let printWindow = null;
      try {
        printWindow = window.open("", "_blank");
      } catch (e) {
        printWindow = null;
      }
      const giveUp = (error) => {
        if (printWindow) {
          printWindow.close();
        }
        setPrintError(error);
      };

      if (isDirty || quantitiesPending || !latestOrderIdRef.current) {
        const saved = onSaveBeforePrint ? await onSaveBeforePrint() : false;
        if (!saved) {
          giveUp(null);
          return;
        }
        setQuantitiesPending(false);
      }
      const currentOrderId = latestOrderIdRef.current;
      if (!currentOrderId) {
        giveUp({ kind: "failed", status: 0, target });
        return;
      }

      const resolved = { ...target };
      if (target?.scope === "sample" && target.sampleIdLocal != null) {
        const sample = sampleFor(target.sampleIdLocal);
        if (!sample?.sampleItemId) {
          giveUp({ kind: "nothing", target });
          return;
        }
        resolved.sampleItemId = sample.sampleItemId;
      }

      setPrinting(true);
      try {
        const response = await fetch(buildPdfUrl(currentOrderId, resolved), {
          credentials: "include",
        });
        if (response.status === 404) {
          giveUp({ kind: "nothing", target });
          return;
        }
        if (!response.ok) {
          giveUp({ kind: "failed", status: response.status, target });
          return;
        }
        const blob = await response.blob();
        const objectUrl = URL.createObjectURL(blob);
        if (printWindow) {
          printWindow.location.href = objectUrl;
        } else {
          const anchor = document.createElement("a");
          anchor.href = objectUrl;
          anchor.download = `${labNumber || "order"}-labels.pdf`;
          document.body.appendChild(anchor);
          anchor.click();
          anchor.remove();
          notify(NotificationKinds.info, "orderEntry.labels.downloaded");
        }
      } catch (e) {
        giveUp({ kind: "failed", status: 0, target });
      } finally {
        setPrinting(false);
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [isDirty, quantitiesPending, onSaveBeforePrint, labNumber],
  );

  useEffect(() => {
    if (registerPrintRow) {
      registerPrintRow((sampleIndex) => {
        const sample = (latestSamplesRef.current || [])[sampleIndex];
        const sampleIdLocal = sample
          ? localIdOf({ sample, index: sampleIndex })
          : String(sampleIndex);
        print({ scope: "sample", sampleIdLocal });
      });
    }
  }, [registerPrintRow, print]);

  const printErrorText = () => {
    if (!printError) {
      return null;
    }
    if (printError.kind === "nothing") {
      return intl.formatMessage({ id: "orderEntry.labels.nothingToPrint" });
    }
    if (!printError.status) {
      return intl.formatMessage({
        id: "orderEntry.labels.printFailed.unreachable",
      });
    }
    return intl.formatMessage(
      { id: "orderEntry.labels.printFailed.detail" },
      { status: printError.status },
    );
  };

  return (
    <Tile
      className="order-section prepare-labels-section"
      data-testid="prepare-labels-section"
    >
      <h4 className="section-title">
        <FormattedMessage id="orderEntry.labels.title" />
      </h4>

      {labelSamples.length === 0 ? (
        <p className="helper-text">
          <FormattedMessage id="orderEntry.labels.empty" />
        </p>
      ) : loading && !labelRequest ? (
        <SkeletonText paragraph lineCount={3} />
      ) : loadFailed ? (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "orderEntry.labels.loadFailed" })}
        />
      ) : labelRequest ? (
        <>
          {printError ? (
            <div
              className="prepare-labels-section__error"
              data-testid="prepare-labels-print-error"
            >
              <InlineNotification
                kind="error"
                lowContrast
                title={intl.formatMessage({
                  id: "orderEntry.labels.printFailed",
                })}
                subtitle={printErrorText()}
                onCloseButtonClick={() => setPrintError(null)}
              />
              {printError.kind === "failed" ? (
                <Button
                  kind="ghost"
                  size="sm"
                  renderIcon={Renew}
                  onClick={() => print(printError.target)}
                  data-testid="prepare-labels-retry"
                >
                  <FormattedMessage id="orderEntry.labels.retry" />
                </Button>
              ) : null}
            </div>
          ) : null}
          <LabelsSection
            labelRequest={labelRequest}
            onChange={handleChange}
            sampleLabelFormatter={rowLabel}
            onPrintRow={isReadOnly ? undefined : print}
            onPrintColumn={isReadOnly ? undefined : print}
            onPrintAll={isReadOnly ? undefined : () => print({})}
            printDisabled={printing}
            pendingSave={isDirty || quantitiesPending}
          />
        </>
      ) : null}
    </Tile>
  );
};

export default PrepareLabelsSection;
