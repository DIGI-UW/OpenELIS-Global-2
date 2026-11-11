import React, { useContext, useEffect, useMemo, useRef } from "react";
import { useHistory, useLocation } from "react-router-dom";
import { FormattedMessage, useIntl } from "react-intl";
import { Tile, Tag, Button } from "@carbon/react";
import { Checkmark } from "@carbon/icons-react";
import OrderWorkflowLayout from "../OrderWorkflowLayout";
import { useOrderContext } from "../OrderContext";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";

/**
 * VectorOrderComplete — terminal step of the vector order workflow.
 *
 * Reached after a successful QA Review submit (OrderQA navigates here).
 * Refreshes order data from the backend on mount so a refresh, deep link,
 * or stepper click still shows the lab number, animal, quantity-in-pool,
 * Processing status, and full collection details.
 */
const VectorOrderComplete = () => {
  const history = useHistory();
  const location = useLocation();
  const intl = useIntl();
  const { orderData, samples, resetOrder, labNumber, loadOrder } =
    useOrderContext();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const announcedLabRef = useRef(null);

  // OrderQA passes ?labNumber=... when navigating here so the page survives
  // refresh / direct nav even if the OrderProvider state has been wiped.
  const queryLabNumber = (() => {
    const params = new URLSearchParams(location.search || "");
    return params.get("labNumber") || "";
  })();

  const displayLabNumber =
    labNumber || orderData?.sampleOrderItems?.labNo || queryLabNumber || "";

  // Pull samples back from the server when context is empty (refresh, deep link)
  // or when the URL points at an order different from the one in context.
  useEffect(() => {
    if (!displayLabNumber) {
      return;
    }
    const contextLab = labNumber || orderData?.sampleOrderItems?.labNo || "";
    const contextHasOrder = contextLab === displayLabNumber;
    const hasSampleData =
      Array.isArray(samples) && samples.some((s) => s && s.sampleTypeId);
    if (!contextHasOrder || !hasSampleData) {
      loadOrder(displayLabNumber, false).catch(() => {});
    }
  }, [displayLabNumber, labNumber, orderData, samples, loadOrder]);

  // Fire the "Order Complete — Processing" message as a toast through the
  // global NotificationContext instead of rendering it inline. Guarded by
  // a ref so re-renders (e.g. dictionary fetches resolving) don't re-toast
  // the same lab number.
  useEffect(() => {
    if (!displayLabNumber || announcedLabRef.current === displayLabNumber) {
      return;
    }
    announcedLabRef.current = displayLabNumber;
    addNotification({
      kind: NotificationKinds.success,
      title: intl.formatMessage({
        id: "order.complete.processing.title",
        defaultMessage: "Order Complete — Processing",
      }),
      message: intl.formatMessage(
        {
          id: "order.complete.processing.description.plain",
          defaultMessage:
            "{labNumber} is now in Processing status and will appear in the V-03 Vector Identification worklist.",
        },
        { labNumber: displayLabNumber },
      ),
    });
    setNotificationVisible(true);
  }, [displayLabNumber, addNotification, setNotificationVisible, intl]);

  const summary = useMemo(() => {
    const sampleTypes = orderData?.sampleTypes || [];
    const findTypeName = (id) => {
      const match = sampleTypes.find(
        (t) => String(t.id) === String(id) || String(t.value) === String(id),
      );
      return match?.value || match?.name || "";
    };

    if (!Array.isArray(samples) || samples.length === 0) {
      return { animal: "", quantity: 0 };
    }
    const list = samples.filter(
      (s) => s && (s.sampleTypeId || s.sampleTypeName),
    );
    if (list.length === 0) {
      return { animal: "", quantity: 0 };
    }
    const animals = Array.from(
      new Set(
        list
          .map((s) => s.sampleTypeName || findTypeName(s.sampleTypeId))
          .filter(Boolean),
      ),
    );
    const total = list.reduce((sum, s) => {
      const raw = s.vectorFields?.collectionVolume ?? s.quantity ?? "";
      const num = parseFloat(raw);
      return Number.isFinite(num) ? sum + num : sum;
    }, 0);

    return { animal: animals.join(", "), quantity: total };
  }, [samples, orderData]);

  const animalDisplay = summary.animal || "—";
  const quantityDisplay = summary.quantity > 0 ? summary.quantity : null;

  const handleNewOrder = () => {
    resetOrder();
    history.push("/order/vector/enter");
  };

  const handleCancel = () => {
    history.push("/order/vector");
  };

  return (
    <OrderWorkflowLayout title="order.step.complete" showSaveButtons={false}>
      {notificationVisible && <AlertDialog />}
      <Tile className="vector-order-complete-tile">
        <div className="vector-success-header">
          <div className="vector-success-icon-box" aria-hidden="true">
            <Checkmark size={24} />
          </div>
          <h3>
            <FormattedMessage
              id="order.complete.processing.title"
              defaultMessage="Order Complete — Processing"
            />
          </h3>
        </div>

        <div className="vector-success-cards">
          <div className="vector-success-card">
            <div className="vector-success-card-label">
              <FormattedMessage
                id="vector.complete.labNumber"
                defaultMessage="Lab Number"
              />
            </div>
            <div className="vector-success-card-value">
              {displayLabNumber || "—"}
            </div>
          </div>
          <div className="vector-success-card">
            <div className="vector-success-card-label">
              <FormattedMessage
                id="vector.complete.animal"
                defaultMessage="Animal"
              />
            </div>
            <div className="vector-success-card-value">{animalDisplay}</div>
          </div>
          <div className="vector-success-card">
            <div className="vector-success-card-label">
              <FormattedMessage
                id="vector.complete.quantity"
                defaultMessage="Quantity"
              />
            </div>
            <div className="vector-success-card-value">
              {quantityDisplay !== null ? (
                <FormattedMessage
                  id="vector.complete.quantity.value"
                  defaultMessage="{count} organisms"
                  values={{ count: quantityDisplay }}
                />
              ) : (
                "—"
              )}
            </div>
          </div>
          <div className="vector-success-card vector-success-card--status">
            <div className="vector-success-card-label">
              <FormattedMessage
                id="vector.complete.status"
                defaultMessage="Status"
              />
            </div>
            <div className="vector-success-card-value">
              <Tag type="blue" size="sm">
                <FormattedMessage
                  id="vector.complete.status.processing"
                  defaultMessage="Processing"
                />
              </Tag>
            </div>
          </div>
        </div>

        <div className="vector-success-actions">
          <Button kind="tertiary" size="sm" onClick={handleNewOrder}>
            <FormattedMessage
              id="vector.complete.newOrder"
              defaultMessage="New Vector Order"
            />
          </Button>
          <Button kind="primary" size="sm" onClick={handleCancel}>
            <FormattedMessage
              id="vector.complete.cancel"
              defaultMessage="Cancel"
            />
          </Button>
        </div>
      </Tile>
    </OrderWorkflowLayout>
  );
};

export default VectorOrderComplete;
