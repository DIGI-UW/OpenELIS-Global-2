import React from "react";
import { NumberInput, TextArea, TextInput } from "@carbon/react";
import { useIntl } from "react-intl";

export const emptyMicrobiologyOrderDetail = {
  patientOrigin: "",
  numberOfSets: "",
  clinicalHistory: "",
  antibioticExposure: "",
  criticalNotificationPreference: "",
};

const MicrobiologyOrderDetailFields = ({
  fields,
  onChange,
  idPrefix = "microbiology-order-detail",
}) => {
  const intl = useIntl();

  return (
    <div className="microbiology-form-grid">
      <TextInput
        id={`${idPrefix}-patient-origin`}
        labelText={intl.formatMessage({
          id: "microbiology.orderDetail.patientOrigin",
        })}
        value={fields.patientOrigin}
        onChange={(event) => onChange("patientOrigin", event.target.value)}
      />
      <NumberInput
        id={`${idPrefix}-number-of-sets`}
        label={intl.formatMessage({
          id: "microbiology.orderDetail.numberOfSets",
        })}
        value={fields.numberOfSets}
        min={1}
        allowEmpty
        onChange={(event) => onChange("numberOfSets", event.target.value)}
      />
      <div className="microbiology-form-grid__wide">
        <TextArea
          id={`${idPrefix}-clinical-history`}
          labelText={intl.formatMessage({
            id: "microbiology.orderDetail.clinicalHistory",
          })}
          value={fields.clinicalHistory}
          onChange={(event) => onChange("clinicalHistory", event.target.value)}
        />
      </div>
      <div className="microbiology-form-grid__wide">
        <TextArea
          id={`${idPrefix}-antibiotic-exposure`}
          labelText={intl.formatMessage({
            id: "microbiology.orderDetail.antibioticExposure",
          })}
          value={fields.antibioticExposure}
          onChange={(event) =>
            onChange("antibioticExposure", event.target.value)
          }
        />
      </div>
      <TextInput
        id={`${idPrefix}-critical-notification-preference`}
        labelText={intl.formatMessage({
          id: "microbiology.orderDetail.criticalNotificationPreference",
        })}
        value={fields.criticalNotificationPreference}
        onChange={(event) =>
          onChange("criticalNotificationPreference", event.target.value)
        }
      />
    </div>
  );
};

export default MicrobiologyOrderDetailFields;
