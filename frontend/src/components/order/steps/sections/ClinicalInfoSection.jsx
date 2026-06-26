import React, { useState, useEffect, useRef } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Grid,
  Column,
  Tile,
  TextInput,
  Select,
  SelectItem,
} from "@carbon/react";
import { getFromOpenElisServer } from "../../../utils/Utils";

/**
 * ClinicalInfoSection - Clinical diagnosis and payment status
 *
 * Implements:
 * - ORD-6: Provisional diagnosis (free text field)
 * - Payment status dropdown
 */

const ClinicalInfoSection = ({ orderData, setOrderData, isReadOnly }) => {
  const intl = useIntl();
  const componentMounted = useRef(true);

  // Payment options state - fetched from API
  const [paymentOptions, setPaymentOptions] = useState([]);

  // Fetch payment options on mount
  useEffect(() => {
    componentMounted.current = true;

    getFromOpenElisServer(
      "/rest/displayList/SAMPLE_PATIENT_PAYMENT_OPTIONS",
      (response) => {
        if (componentMounted.current && Array.isArray(response)) {
          setPaymentOptions(response);
        }
      },
    );

    return () => {
      componentMounted.current = false;
    };
  }, []);

  // Use fetched options or fallback to orderData options
  const paymentStatuses =
    paymentOptions.length > 0
      ? paymentOptions
      : orderData?.sampleOrderItems?.paymentOptions || [];

  // Handle diagnosis change (free text)
  const handleDiagnosisChange = (e) => {
    const value = e.target.value;
    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        provisionalClinicalDiagnosis: value,
      },
    }));
  };

  // Handle payment status change
  const handlePaymentStatusChange = (e) => {
    const value = e.target.value;
    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        paymentOptionSelection: value,
      },
    }));
  };

  return (
    <Tile className="order-section clinical-info-section">
      <h4 className="section-title">
        <FormattedMessage
          id="order.clinicalInfo"
          defaultMessage="Clinical Information"
        />
      </h4>
      <p className="helper-text">
        <FormattedMessage
          id="order.clinicalInfo.helper"
          defaultMessage="Optional context to guide test selection and reporting — not recorded as a confirmed diagnosis."
        />
      </p>

      <Grid>
        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="provisionalDiagnosis"
            labelText={intl.formatMessage({
              id: "order.provisionalDiagnosis",
              defaultMessage: "Provisional Diagnosis",
            })}
            value={
              orderData?.sampleOrderItems?.provisionalClinicalDiagnosis || ""
            }
            onChange={handleDiagnosisChange}
            placeholder={intl.formatMessage({
              id: "order.diagnosis.placeholder",
              defaultMessage: "Enter diagnosis or clinical notes",
            })}
            helperText={intl.formatMessage({
              id: "order.diagnosis.helper",
              defaultMessage:
                "Working diagnosis — guides test selection. Not recorded as a confirmed diagnosis.",
            })}
            disabled={isReadOnly}
          />
        </Column>

        <Column lg={8} md={4} sm={4}>
          <Select
            id="paymentStatus"
            labelText={intl.formatMessage({
              id: "order.paymentStatus",
              defaultMessage: "Payment Status",
            })}
            value={orderData?.sampleOrderItems?.paymentOptionSelection || ""}
            onChange={handlePaymentStatusChange}
            helperText={intl.formatMessage({
              id: "order.paymentStatus.helper",
              defaultMessage: "How payment will be handled for this order.",
            })}
            disabled={isReadOnly}
          >
            <SelectItem key="" value="" text="" />
            {paymentStatuses.map((status) => (
              <SelectItem
                key={status.id}
                value={status.id}
                text={status.value}
              />
            ))}
          </Select>
        </Column>
      </Grid>
    </Tile>
  );
};

export default ClinicalInfoSection;
