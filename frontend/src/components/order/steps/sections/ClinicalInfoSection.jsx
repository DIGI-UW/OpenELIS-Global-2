import React, { useContext, useState, useEffect, useRef } from "react";
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
import CustomDatePicker from "../../../common/CustomDatePicker";
import { ConfigurationContext } from "../../../layout/contexts";
import { formatIsoDateForBackend } from "../../dateUtils";

/**
 * ClinicalInfoSection - Clinical diagnosis and payment status
 *
 * Implements:
 * - ORD-6: Provisional diagnosis (free text field)
 * - Payment status dropdown
 * - Order date, next-visit date and test location, all of which SampleOrderItem
 *   has always carried but the new lanes never bound to an input.
 */

const ClinicalInfoSection = ({ orderData, setOrderData, isReadOnly }) => {
  const intl = useIntl();
  const componentMounted = useRef(true);
  const { configurationProperties = {} } =
    useContext(ConfigurationContext) || {};
  const dateLocale = configurationProperties.DEFAULT_DATE_LOCALE || "en-US";

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

  const updateOrderField = (field, value) =>
    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: { ...prev.sampleOrderItems, [field]: value },
    }));

  // sampleOrderItems dates are held in the site's display format — that is
  // what the server sends in currentDate and what it parses back — unlike the
  // per-sample dates, which are ISO in state and converted on submit. Storing
  // ISO here is rejected by the backend date parser.
  const handleDateChange = (field) => (pickerDate) =>
    updateOrderField(field, pickerDate || "");

  const testLocationCodes =
    orderData?.sampleOrderItems?.testLocationCodeList || [];
  const testLocationCode = orderData?.sampleOrderItems?.testLocationCode || "";
  const isOtherLocation =
    testLocationCode &&
    !testLocationCodes.some(
      (code) => String(code.id) === String(testLocationCode),
    );

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

        {/* requestDate has always been on the payload, defaulted to today and
            stamped server-side, with no input bound to it. */}
        <Column lg={8} md={4} sm={4}>
          <CustomDatePicker
            id="order_requestDate"
            labelText={intl.formatMessage({
              id: "order.requestDate",
              defaultMessage: "Order Date",
            })}
            value={formatIsoDateForBackend(
              orderData?.sampleOrderItems?.requestDate || "",
              dateLocale,
            )}
            onChange={handleDateChange("requestDate")}
            disabled={isReadOnly}
          />
        </Column>

        <Column lg={8} md={4} sm={4}>
          <CustomDatePicker
            id="order_nextVisitDate"
            labelText={intl.formatMessage({
              id: "order.nextVisitDate",
              defaultMessage: "Next Visit Date",
            })}
            value={formatIsoDateForBackend(
              orderData?.sampleOrderItems?.nextVisitDate || "",
              dateLocale,
            )}
            onChange={handleDateChange("nextVisitDate")}
            disabled={isReadOnly}
          />
        </Column>

        {testLocationCodes.length > 0 && (
          <Column lg={8} md={4} sm={4}>
            <Select
              id="testLocationCode"
              labelText={intl.formatMessage({
                id: "order.testLocationCode",
                defaultMessage: "Sampling Performed At",
              })}
              value={isOtherLocation ? "other" : testLocationCode}
              onChange={(e) =>
                updateOrderField(
                  "testLocationCode",
                  e.target.value === "other" ? "" : e.target.value,
                )
              }
              disabled={isReadOnly}
            >
              <SelectItem key="" value="" text="" />
              {testLocationCodes.map((code) => (
                <SelectItem key={code.id} value={code.id} text={code.value} />
              ))}
              <SelectItem
                key="other"
                value="other"
                text={intl.formatMessage({
                  id: "order.testLocationCode.other",
                  defaultMessage: "Other",
                })}
              />
            </Select>
          </Column>
        )}

        {isOtherLocation && (
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="otherLocationCode"
              labelText={intl.formatMessage({
                id: "order.testLocationCode.otherLabel",
                defaultMessage: "If other, specify",
              })}
              value={orderData?.sampleOrderItems?.otherLocationCode || ""}
              onChange={(e) =>
                updateOrderField("otherLocationCode", e.target.value)
              }
              disabled={isReadOnly}
            />
          </Column>
        )}
      </Grid>
    </Tile>
  );
};

export default ClinicalInfoSection;
