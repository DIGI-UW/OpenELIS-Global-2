import React from "react";
import {
  Checkbox,
  ComboBox,
  NumberInput,
  Select,
  SelectItem,
} from "@carbon/react";
import { useIntl } from "react-intl";
import MacroTextArea from "../common/textMacro/MacroTextArea";

export const emptyMicrobiologyOrderDetail = {
  cultureMethodId: "",
  patientOrigin: "",
  numberOfSets: "",
  clinicalHistory: "",
  antibioticExposure: false,
  criticalNotificationPreference: null,
};

const MicrobiologyOrderDetailFields = ({
  fields,
  onChange,
  methods = [],
  patientOrigins = [],
  idPrefix = "microbiology-order-detail",
  isReadOnly = false,
  showCultureMethod = true,
}) => {
  const intl = useIntl();
  const selectedMethod =
    methods.find(
      (method) => String(method.methodId) === String(fields.cultureMethodId),
    ) || null;

  return (
    <div className="microbiology-form-grid">
      {showCultureMethod && (
        <ComboBox
          id={`${idPrefix}-culture-method`}
          titleText={intl.formatMessage({
            id: "microbiology.orderDetail.cultureMethod",
          })}
          items={methods}
          itemToString={(method) => method?.methodName || ""}
          selectedItem={selectedMethod}
          onChange={({ selectedItem }) =>
            onChange("cultureMethodId", selectedItem?.methodId || "")
          }
          invalid={!selectedMethod}
          invalidText={intl.formatMessage({
            id: "microbiology.orderDetail.cultureMethodRequired",
          })}
          disabled={isReadOnly}
        />
      )}
      <Select
        id={`${idPrefix}-patient-origin`}
        labelText={intl.formatMessage({
          id: "microbiology.orderDetail.patientOrigin",
        })}
        value={fields.patientOrigin}
        onChange={(event) => onChange("patientOrigin", event.target.value)}
        disabled={isReadOnly}
      >
        <SelectItem value="" text="" />
        {patientOrigins.map((option) => (
          <SelectItem
            key={option.code}
            value={option.code}
            text={option.label}
          />
        ))}
      </Select>
      <NumberInput
        id={`${idPrefix}-number-of-sets`}
        label={intl.formatMessage({
          id: "microbiology.orderDetail.numberOfSets",
        })}
        value={fields.numberOfSets}
        min={1}
        max={10}
        allowEmpty
        onChange={(event, state = {}) =>
          onChange("numberOfSets", state.value ?? event.target.value)
        }
        disabled={isReadOnly}
      />
      <div className="microbiology-form-grid__wide">
        <MacroTextArea
          id={`${idPrefix}-clinical-history`}
          labelText={intl.formatMessage({
            id: "microbiology.orderDetail.clinicalHistory",
          })}
          context="MICROBIOLOGY_CLINICAL_HISTORY"
          value={fields.clinicalHistory}
          onChange={(event) => onChange("clinicalHistory", event.target.value)}
          maxLength={1000}
          disabled={isReadOnly}
        />
      </div>
      <Checkbox
        id={`${idPrefix}-antibiotic-exposure`}
        labelText={intl.formatMessage({
          id: "microbiology.orderDetail.antibioticExposure",
        })}
        checked={Boolean(fields.antibioticExposure)}
        onChange={(_, { checked }) => onChange("antibioticExposure", checked)}
        disabled={isReadOnly}
      />
      <Checkbox
        id={`${idPrefix}-critical-notification-preference`}
        labelText={intl.formatMessage({
          id: "microbiology.orderDetail.criticalNotificationPreference",
        })}
        checked={Boolean(fields.criticalNotificationPreference)}
        onChange={(_, { checked }) =>
          onChange("criticalNotificationPreference", checked)
        }
        disabled={isReadOnly}
      />
    </div>
  );
};

export default MicrobiologyOrderDetailFields;
