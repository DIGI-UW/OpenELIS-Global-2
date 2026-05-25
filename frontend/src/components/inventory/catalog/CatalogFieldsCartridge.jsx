import React from "react";
import { TextInput, RadioButtonGroup, RadioButton } from "@carbon/react";
import { FormattedMessage } from "react-intl";

const CatalogFieldsCartridge = ({ formData, onChange }) => (
  <>
    <TextInput
      id="compatibleAnalyzers"
      labelText={<FormattedMessage id="catalog.item.compatibleAnalyzers" />}
      value={formData.compatibleAnalyzers}
      onChange={(e) => onChange("compatibleAnalyzers", e.target.value)}
      placeholder="e.g., GeneXpert, Cobas 6800"
      required
    />
    <RadioButtonGroup
      legendText={<FormattedMessage id="catalog.item.calibrationRequired" />}
      name="calibrationRequired"
      valueSelected={formData.calibrationRequired}
      onChange={(value) => onChange("calibrationRequired", value)}
    >
      <RadioButton labelText="Yes" value="Y" id="calibration-yes" />
      <RadioButton labelText="No" value="N" id="calibration-no" />
    </RadioButtonGroup>
  </>
);

export default CatalogFieldsCartridge;
