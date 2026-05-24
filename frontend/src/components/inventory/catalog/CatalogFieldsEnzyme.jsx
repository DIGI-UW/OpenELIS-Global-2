import React from "react";
import { TextInput } from "@carbon/react";

const CatalogFieldsEnzyme = ({ formData, onChange }) => (
  <TextInput
    id="enzymeType"
    labelText="Enzyme type"
    value={formData.enzymeType}
    onChange={(e) => onChange("enzymeType", e.target.value)}
    placeholder="e.g., TAG_DNA_POLYMERASE, Q5_POLYMERASE"
    required
    helperText="Required for enzyme catalog items (matches seeded enzyme_type values)"
  />
);

export default CatalogFieldsEnzyme;
