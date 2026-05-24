import React from "react";
import { NumberInput, TextInput, TextArea } from "@carbon/react";
import { FormattedMessage } from "react-intl";

const CatalogFieldsReagent = ({ formData, onChange }) => (
  <>
    <NumberInput
      id="stabilityAfterOpening"
      label={<FormattedMessage id="catalog.item.stabilityAfterOpening" />}
      helperText="Days until reagent expires after opening"
      value={formData.stabilityAfterOpening ?? 0}
      onChange={(e, { value }) => onChange("stabilityAfterOpening", value ?? 0)}
      min={1}
      max={365}
      required
    />
    <TextInput
      id="concentration"
      labelText="Concentration"
      value={formData.concentration}
      onChange={(e) => onChange("concentration", e.target.value)}
      placeholder="e.g., 1 M, 5 mg/mL, 10x"
    />
    <TextArea
      id="dilutionNotes"
      labelText={<FormattedMessage id="catalog.item.dilutionNotes" />}
      value={formData.dilutionNotes}
      onChange={(e) => onChange("dilutionNotes", e.target.value)}
      placeholder="e.g., Dilute 1:10 with distilled water"
      rows={2}
    />
    <TextArea
      id="storageRequirements"
      labelText={<FormattedMessage id="catalog.item.storageRequirements" />}
      value={formData.storageRequirements}
      onChange={(e) => onChange("storageRequirements", e.target.value)}
      placeholder="e.g., Store at 2-8°C, protect from light"
      rows={2}
    />
  </>
);

export default CatalogFieldsReagent;
