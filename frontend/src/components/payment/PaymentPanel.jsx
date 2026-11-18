import React, { useCallback, useContext, useEffect, useState } from "react";
import {
  Button,
  InlineLoading,
  InlineNotification,
  Link,
  Modal,
  StructuredListBody,
  StructuredListCell,
  StructuredListRow,
  StructuredListWrapper,
  Tag,
  Tile,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import {
  getFromOpenElisServer,
  hasRole,
  postToOpenElisServerFullResponse,
  Roles,
} from "../utils/Utils";
import "./PaymentPanel.css";

const STATUS_TAG = {
  NOT_REQUESTED: { type: "gray", id: "payment.status.notRequested" },
  CREATED: { type: "gray", id: "payment.status.created" },
  PENDING: { type: "blue", id: "payment.status.pending" },
  PAID: { type: "green", id: "payment.status.paid" },
  FAILED: { type: "red", id: "payment.status.failed" },
  CANCELLED: { type: "warm-gray", id: "payment.status.cancelled" },
  REFUNDED: { type: "purple", id: "payment.status.refunded" },
};

/** Statuses after which staff may ask for a new link. */
const CAN_CREATE_LINK = ["NOT_REQUESTED", "FAILED", "CANCELLED"];

/** While watching, re-check every 2 s, at most 5 times (10 s). */
const AWAITING_AUTO_LINK = ["NOT_REQUESTED", "CREATED"];
const AUTO_LINK_INTERVAL_MS = 2000;
const AUTO_LINK_CHECKS = 5;

/** Messages for the HTTP statuses PaymentRestController returns. */
const LINK_ERRORS = {
  503: "payment.error.disabled",
  422: "payment.error.unpriced",
  404: "payment.error.notFound",
  502: "payment.error.gateway",
};
const REFUND_ERRORS = {
  503: "payment.error.disabled",
  422: "payment.error.refundNotAllowed",
  404: "payment.error.notFound",
  403: "payment.error.forbidden",
  502: "payment.error.gateway",
};

const rupees = (amount) => `₹${amount}`;

/**
 * Payment for one order: amounts, status, the Razorpay link to pay at, and the
 * staff actions (create the link, refund). Renders nothing when the lab number
 * does not resolve to a sample.
 *
 * With watchForAutoLink (the screen shown right after saving an order) the
 * panel re-checks for a while: the link is created in the background after the
 * order commits, and offering "Create payment link" before then could race it.
 */
const PaymentPanel = ({ accessionNumber, watchForAutoLink = false }) => {
  const intl = useIntl();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const isAdmin = hasRole(userSessionDetails, Roles.GLOBAL_ADMIN);

  const [sampleId, setSampleId] = useState(null);
  const [summary, setSummary] = useState(null);
  const [paidPaymentId, setPaidPaymentId] = useState(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null);
  const [confirmRefund, setConfirmRefund] = useState(false);
  const [autoLinkChecks, setAutoLinkChecks] = useState(0);

  // Callers remount the panel per order (key={accessionNumber}), so state
  // never carries over from a previous lab number.
  useEffect(() => {
    if (!accessionNumber) {
      return;
    }
    getFromOpenElisServer(
      "/rest/sample/all-by-accession/" + encodeURIComponent(accessionNumber),
      (forms) => {
        if (Array.isArray(forms) && forms.length > 0 && forms[0].id) {
          setSampleId(String(forms[0].id));
        }
      },
    );
  }, [accessionNumber]);

  const load = useCallback(() => {
    if (!sampleId) {
      return;
    }
    getFromOpenElisServer(
      "/rest/payments/sample/" + sampleId + "/summary",
      // Error bodies (e.g. 403 for a user without the role) also have a
      // "status", but a number: only a real summary for this sample counts.
      (data) =>
        setSummary(
          data &&
            String(data.sampleId) === sampleId &&
            Object.hasOwn(STATUS_TAG, data.status)
            ? data
            : null,
        ),
    );
    getFromOpenElisServer("/rest/payments/sample/" + sampleId, (payments) => {
      const paid = Array.isArray(payments)
        ? payments.find((p) => p.status === "PAID")
        : null;
      setPaidPaymentId(paid ? paid.id : null);
    });
  }, [sampleId]);

  useEffect(() => {
    load();
  }, [load]);

  const waitingForAutoLink =
    watchForAutoLink &&
    summary != null &&
    summary.amountDueRupees != null &&
    AWAITING_AUTO_LINK.includes(summary.status) &&
    autoLinkChecks < AUTO_LINK_CHECKS;

  useEffect(() => {
    if (!waitingForAutoLink) {
      return undefined;
    }
    const timer = setTimeout(() => {
      setAutoLinkChecks((n) => n + 1);
      load();
    }, AUTO_LINK_INTERVAL_MS);
    return () => clearTimeout(timer);
  }, [waitingForAutoLink, autoLinkChecks, load]);

  const post = (url, errors, successId) => {
    setBusy(true);
    setMessage(null);
    postToOpenElisServerFullResponse(url, null, (response) => {
      setBusy(false);
      const status = response ? response.status : 0;
      if (status >= 200 && status < 300) {
        if (successId) {
          setMessage({ kind: "success", id: successId });
        }
        load();
      } else {
        setMessage({
          kind: "error",
          id: errors[status] || "payment.error.generic",
        });
      }
    });
  };

  const createLink = () =>
    post("/rest/payments/sample/" + sampleId + "/link", LINK_ERRORS, null);

  const refund = () => {
    setConfirmRefund(false);
    post(
      "/rest/payments/" + paidPaymentId + "/refund",
      REFUND_ERRORS,
      "payment.refund.requested",
    );
  };

  if (!summary) {
    return null;
  }

  const tag = STATUS_TAG[summary.status] || STATUS_TAG.NOT_REQUESTED;
  const priced = summary.amountDueRupees != null;
  const unpriced = summary.unpricedTestNames || [];
  const showCreate =
    priced &&
    !summary.paid &&
    !waitingForAutoLink &&
    CAN_CREATE_LINK.includes(summary.status);
  const showRefund = isAdmin && summary.status === "PAID" && paidPaymentId;

  const amountRows = [
    ["payment.label.subtotal", summary.subtotalRupees],
    ["payment.label.gst", summary.gstRupees],
    ["payment.label.total", summary.amountDueRupees],
  ];

  return (
    <Tile className="payment-panel" data-testid="payment-panel">
      <div className="payment-panel__header">
        <h4>
          <FormattedMessage id="payment.panel.title" />
        </h4>
        <Tag type={tag.type}>
          <FormattedMessage id={tag.id} />
        </Tag>
      </div>

      {priced && (
        <StructuredListWrapper isCondensed className="payment-panel__amounts">
          <StructuredListBody>
            {amountRows.map(([labelId, value]) => (
              <StructuredListRow key={labelId}>
                <StructuredListCell>
                  <FormattedMessage id={labelId} />
                </StructuredListCell>
                <StructuredListCell className="payment-panel__amount">
                  {rupees(value)}
                </StructuredListCell>
              </StructuredListRow>
            ))}
          </StructuredListBody>
        </StructuredListWrapper>
      )}

      {unpriced.length > 0 && (
        <InlineNotification
          kind="warning"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "payment.unpriced.title" })}
          subtitle={intl.formatMessage(
            { id: "payment.unpriced.subtitle" },
            { tests: unpriced.join(", ") },
          )}
        />
      )}

      {waitingForAutoLink && (
        <InlineLoading
          className="payment-panel__waiting"
          description={intl.formatMessage({ id: "payment.autoLink.waiting" })}
        />
      )}

      {summary.shortUrl && (
        <p className="payment-panel__link">
          <FormattedMessage id="payment.label.link" />{" "}
          <Link
            href={summary.shortUrl}
            target="_blank"
            rel="noopener noreferrer"
          >
            {summary.shortUrl}
          </Link>
        </p>
      )}

      {message && (
        <InlineNotification
          kind={message.kind}
          lowContrast
          title={intl.formatMessage({ id: message.id })}
          onCloseButtonClick={() => setMessage(null)}
        />
      )}

      {(showCreate || showRefund) && (
        <div className="payment-panel__actions">
          {showCreate && (
            <Button size="sm" onClick={createLink} disabled={busy}>
              <FormattedMessage id="payment.action.createLink" />
            </Button>
          )}
          {showRefund && (
            <Button
              size="sm"
              kind="danger--tertiary"
              onClick={() => setConfirmRefund(true)}
              disabled={busy}
            >
              <FormattedMessage id="payment.action.refund" />
            </Button>
          )}
        </div>
      )}

      {showRefund && (
        <Modal
          open={confirmRefund}
          danger
          modalHeading={intl.formatMessage({ id: "payment.refund.heading" })}
          primaryButtonText={intl.formatMessage(
            { id: "payment.refund.confirm" },
            { amount: rupees(summary.amountDueRupees) },
          )}
          secondaryButtonText={intl.formatMessage({
            id: "label.button.cancel",
          })}
          onRequestSubmit={refund}
          onRequestClose={() => setConfirmRefund(false)}
        >
          <p>
            <FormattedMessage id="payment.refund.body" />
          </p>
        </Modal>
      )}
    </Tile>
  );
};

export default PaymentPanel;
