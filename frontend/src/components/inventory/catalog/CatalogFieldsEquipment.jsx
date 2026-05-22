import React from "react";
import {
  TextInput,
  Dropdown,
  DatePicker,
  DatePickerInput,
  RadioButtonGroup,
  RadioButton,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

const CONDITION_OPTIONS = [
  { id: "functional", text: "Functional" },
  { id: "non-functional", text: "Non-functional" },
  { id: "under-repair", text: "Under Repair" },
  { id: "decommissioned", text: "Decommissioned" },
];

const CatalogFieldsEquipment = ({
  formData,
  onChange,
  analyzers = [],
  analyzersLoading = false,
}) => {
  const intl = useIntl();

  const analyzerItems = analyzers.map((a) => ({
    id: a.id,
    text: a.name ? `${a.name}${a.machineId ? ` (${a.machineId})` : ""}` : a.id,
  }));

  return (
    <>
      <Dropdown
        id="analyzerId"
        titleText="Linked analyzer (optional)"
        label={
          analyzersLoading ? "Loading analyzers…" : "Select analyzer instrument"
        }
        items={analyzerItems}
        itemToString={(item) => (item ? item.text : "")}
        selectedItem={
          analyzerItems.find((item) => item.id === formData.analyzerId) || null
        }
        onChange={({ selectedItem }) =>
          onChange("analyzerId", selectedItem?.id || "")
        }
        disabled={analyzersLoading}
        helperText="Links this equipment record to the analyzer configuration used for result import."
      />

      <TextInput
        id="modelNumber"
        labelText="Model number"
        value={formData.modelNumber}
        onChange={(e) => onChange("modelNumber", e.target.value)}
        placeholder="e.g., QuantStudio-3, BX43"
        required
      />

      <TextInput
        id="serialNumber"
        labelText="Serial number"
        value={formData.serialNumber}
        onChange={(e) => onChange("serialNumber", e.target.value)}
        placeholder="e.g., QS3-2024-001"
      />

      <TextInput
        id="ahriTag"
        labelText="AHRI tag"
        value={formData.ahriTag}
        onChange={(e) => onChange("ahriTag", e.target.value)}
        placeholder="e.g., AHRI-PCR-001"
      />

      <Dropdown
        id="equipmentCondition"
        titleText="Equipment condition"
        label="Select condition"
        items={CONDITION_OPTIONS}
        selectedItem={
          CONDITION_OPTIONS.find(
            (item) => item.id === formData.equipmentCondition,
          ) || CONDITION_OPTIONS[0]
        }
        itemToString={(item) => (item ? item.text : "")}
        onChange={({ selectedItem }) =>
          onChange("equipmentCondition", selectedItem?.id || "functional")
        }
        required
      />

      <TextInput
        id="compatibleAnalyzers"
        labelText={
          <FormattedMessage id="catalog.item.compatibleAnalyzers" />
        }
        value={formData.compatibleAnalyzers}
        onChange={(e) => onChange("compatibleAnalyzers", e.target.value)}
        placeholder="Optional — e.g., GeneXpert"
      />

      <RadioButtonGroup
        legendText={<FormattedMessage id="catalog.item.calibrationRequired" />}
        name="equipmentCalibrationRequired"
        valueSelected={formData.calibrationRequired || "N"}
        onChange={(value) => onChange("calibrationRequired", value)}
      >
        <RadioButton labelText="Yes" value="Y" id="equip-calibration-yes" />
        <RadioButton labelText="No" value="N" id="equip-calibration-no" />
      </RadioButtonGroup>

      <DatePicker datePickerType="single">
        <DatePickerInput
          id="installationDate"
          placeholder="mm/dd/yyyy"
          labelText="Installation date"
          value={formData.installationDate}
          onChange={(e) => onChange("installationDate", e.target.value)}
        />
      </DatePicker>

      <DatePicker datePickerType="single">
        <DatePickerInput
          id="lastServiceDate"
          placeholder="mm/dd/yyyy"
          labelText="Last service date"
          value={formData.lastServiceDate}
          onChange={(e) => onChange("lastServiceDate", e.target.value)}
        />
      </DatePicker>

      <DatePicker datePickerType="single">
        <DatePickerInput
          id="lastMaintenanceDate"
          placeholder="mm/dd/yyyy"
          labelText="Last maintenance date"
          value={formData.lastMaintenanceDate}
          onChange={(e) => onChange("lastMaintenanceDate", e.target.value)}
        />
      </DatePicker>

      <DatePicker datePickerType="single">
        <DatePickerInput
          id="nextMaintenanceDate"
          placeholder="mm/dd/yyyy"
          labelText={intl.formatMessage({
            id: "catalog.item.nextMaintenanceDate",
            defaultMessage: "Next maintenance date",
          })}
          value={formData.nextMaintenanceDate}
          onChange={(e) => onChange("nextMaintenanceDate", e.target.value)}
        />
      </DatePicker>
    </>
  );
};

export default CatalogFieldsEquipment;
