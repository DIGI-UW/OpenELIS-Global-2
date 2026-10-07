import React from "react";
import {
  DatePicker,
  DatePickerInput,
  TextArea,
  TextInput,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { formatDateOnly, toLocalIsoDate } from "../utils/Utils";
import { calendarOnlyInput } from "./eqaCommon";

const RepeatShipmentFields = ({ form, onChange }) => {
  const intl = useIntl();
  const t = (id, defaultMessage) => intl.formatMessage({ id, defaultMessage });

  return (
    <>
      <TextInput
        id="eqa-repeat-courier"
        labelText={t("eqa.shipment.courier", "Courier")}
        value={form.courier ?? ""}
        onChange={(event) => onChange({ courier: event.target.value })}
      />
      <TextInput
        id="eqa-repeat-tracking"
        labelText={t("eqa.shipment.tracking", "Tracking number")}
        value={form.trackingNumber ?? ""}
        onChange={(event) => onChange({ trackingNumber: event.target.value })}
      />
      <DatePicker
        datePickerType="single"
        dateFormat="d/m/Y"
        value={formatDateOnly(form.estimatedDeliveryDate)}
        onChange={(dates) =>
          onChange({
            estimatedDeliveryDate: dates[0] ? toLocalIsoDate(dates[0]) : "",
          })
        }
      >
        <DatePickerInput
          id="eqa-repeat-expected"
          labelText={t("eqa.shipment.expected", "Expected delivery")}
          placeholder="dd/mm/yyyy"
          {...calendarOnlyInput}
        />
      </DatePicker>
      <TextArea
        id="eqa-repeat-override-note"
        labelText={t("eqa.receipt.overrideNote", "Override note")}
        value={form.overrideNote ?? ""}
        onChange={(event) => onChange({ overrideNote: event.target.value })}
        rows={3}
      />
    </>
  );
};

export default RepeatShipmentFields;
