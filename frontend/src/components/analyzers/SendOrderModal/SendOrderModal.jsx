import React, { useState, useEffect } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  TextInput,
  Tag,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { sendOrder } from "../../../services/analyzerService";
import "./SendOrderModal.css";

const SendOrderModal = ({ analyzer, open, onClose }) => {
  const intl = useIntl();
  const [accessionNumber, setAccessionNumber] = useState("");
  const [status, setStatus] = useState("initial");
  const [message, setMessage] = useState("");
  const [orderCount, setOrderCount] = useState(0);

  useEffect(() => {
    if (open && analyzer) {
      setAccessionNumber("");
      setStatus("initial");
      setMessage("");
      setOrderCount(0);
    }
  }, [open, analyzer]);

  const handleSubmit = () => {
    if (!analyzer?.id) {
      setStatus("error");
      setMessage(
        intl.formatMessage({
          id: "analyzer.sendOrder.validation.analyzerIdRequired",
        }),
      );
      return;
    }
    const trimmed = (accessionNumber || "").trim();
    if (!trimmed) {
      setStatus("error");
      setMessage(
        intl.formatMessage({
          id: "analyzer.sendOrder.validation.accessionRequired",
        }),
      );
      return;
    }

    setStatus("loading");
    setMessage("");

    sendOrder(analyzer.id, trimmed, (response) => {
      if (response.error || response.statusCode >= 400) {
        setStatus("error");
        setMessage(
          response.message ||
            response.error ||
            intl.formatMessage({ id: "analyzer.sendOrder.error" }),
        );
      } else {
        setStatus("success");
        setMessage(
          response.message ||
            intl.formatMessage({ id: "analyzer.sendOrder.success" }),
        );
        setOrderCount(response.orderCount ?? 0);
      }
    });
  };

  return (
    <ComposedModal open={open} onClose={onClose} data-testid="send-order-modal">
      <ModalHeader
        title={intl.formatMessage({ id: "analyzer.sendOrder.title" })}
        data-testid="send-order-modal-header"
      />
      <ModalBody>
        <p className="send-order-subtitle">
          {intl.formatMessage({ id: "analyzer.sendOrder.subtitle" })}
        </p>
        {analyzer && (
          <p className="send-order-analyzer-name">
            <strong>{analyzer.name}</strong>
          </p>
        )}
        <TextInput
          id="send-order-accession"
          labelText={intl.formatMessage({
            id: "analyzer.sendOrder.accessionLabel",
          })}
          value={accessionNumber}
          onChange={(e) => setAccessionNumber(e.target.value)}
          placeholder={intl.formatMessage({
            id: "analyzer.sendOrder.accessionPlaceholder",
          })}
          data-testid="send-order-accession-input"
          disabled={status === "loading"}
        />
        {status === "success" && (
          <Tag type="green" data-testid="send-order-success">
            {message}
            {orderCount > 0 &&
              ` — ${intl.formatMessage({ id: "analyzer.sendOrder.orderCount" }, { count: orderCount })}`}
          </Tag>
        )}
        {status === "error" && (
          <Tag type="red" data-testid="send-order-error">
            {message}
          </Tag>
        )}
      </ModalBody>
      <ModalFooter>
        <Button
          kind="secondary"
          onClick={onClose}
          data-testid="send-order-close-button"
        >
          {intl.formatMessage({ id: "analyzer.sendOrder.close" })}
        </Button>
        {status !== "loading" && (
          <Button
            kind="primary"
            onClick={handleSubmit}
            data-testid="send-order-submit-button"
          >
            {intl.formatMessage({ id: "analyzer.sendOrder.submit" })}
          </Button>
        )}
      </ModalFooter>
    </ComposedModal>
  );
};

export default SendOrderModal;
