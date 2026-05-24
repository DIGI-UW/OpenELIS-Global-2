import React from "react";
import { TextInput, NumberInput } from "@carbon/react";
import { FormattedMessage } from "react-intl";

const CatalogFieldsKit = ({ formData, onChange }) => (
  <>
    <TextInput
      id="sourceOrganization"
      labelText={<FormattedMessage id="catalog.item.sourceOrganization" />}
      value={formData.sourceOrganization}
      onChange={(e) => onChange("sourceOrganization", e.target.value)}
      placeholder="e.g., WHO, CDC, PEPFAR"
      required
    />
    <TextInput
      id="kitTestType"
      labelText={<FormattedMessage id="catalog.item.kitTestType" />}
      value={formData.kitTestType}
      onChange={(e) => onChange("kitTestType", e.target.value)}
      placeholder={
        formData.itemType === "HIV_KIT" ? "e.g., HIV-1/2" : "e.g., RPR, TPHA"
      }
      required
    />
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
  </>
);

export default CatalogFieldsKit;
