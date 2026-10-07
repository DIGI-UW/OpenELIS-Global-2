import React from "react";
import { Column, TextInput } from "@carbon/react";
import { useIntl } from "react-intl";

export default function CultureBottleFields({
  sample,
  sampleIndex,
  onChange,
  isReadOnly,
  includeTime = true,
}) {
  const intl = useIntl();
  const fields = [
    ["container", "order.bottle.container", "text", 255],
    ["bodySite", "order.bottle.bodySite", "text", 40],
    ...(includeTime
      ? [
          ["collectionDate", "collect.sample.collectionDate", "date"],
          ["collectionTime", "collect.sample.collectionTime", "time"],
        ]
      : []),
  ];
  return fields.map(([field, label, type, maxLength]) => (
    <Column lg={4} md={4} sm={4} key={field}>
      <TextInput
        id={`bottle-${field}-${sampleIndex}`}
        type={type}
        maxLength={maxLength}
        labelText={intl.formatMessage({ id: label })}
        value={sample[field] ?? ""}
        onChange={(event) => onChange(field, event.target.value)}
        disabled={isReadOnly}
      />
    </Column>
  ));
}
