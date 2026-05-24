import React from "react";
import { NumberInput, RadioButtonGroup, RadioButton } from "@carbon/react";
import { FormattedMessage } from "react-intl";

const CatalogFieldsRdt = ({ formData, onChange }) => (
  <>
    <NumberInput
      id="testsPerKit"
      label={<FormattedMessage id="catalog.item.testsPerKit" />}
      helperText="Number of individual tests in this kit"
      value={formData.testsPerKit ?? 0}
      onChange={(e, { value }) => onChange("testsPerKit", value ?? 0)}
      min={1}
      max={1000}
      required
    />
    <RadioButtonGroup
      legendText={<FormattedMessage id="catalog.item.individualTracking" />}
      name="individualTracking"
      valueSelected={formData.individualTracking}
      onChange={(value) => onChange("individualTracking", value)}
    >
      <RadioButton
        labelText="Yes - Track each test individually"
        value="Y"
        id="tracking-yes"
      />
      <RadioButton
        labelText="No - Track kit as whole"
        value="N"
        id="tracking-no"
      />
    </RadioButtonGroup>
  </>
);

export default CatalogFieldsRdt;
