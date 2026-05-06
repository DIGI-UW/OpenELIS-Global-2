import React from "react";
import { useIntl, FormattedMessage } from "react-intl";
import { Grid, Column, Tile, TextInput } from "@carbon/react";

const CollectionConditionsSection = ({
  orderData,
  setOrderData,
  isReadOnly,
}) => {
  const intl = useIntl();

  const environmentalFields =
    orderData?.sampleOrderItems?.environmentalFields || {};

  const updateField = (field, value) => {
    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        environmentalFields: {
          ...prev.sampleOrderItems?.environmentalFields,
          [field]: value,
        },
      },
    }));
  };

  return (
    <Tile className="order-section">
      <h4 className="section-title">
        <FormattedMessage
          id="env.collectionConditions.title"
          defaultMessage="Collection Conditions"
        />
      </h4>
      <Grid>
        <Column lg={5} md={4} sm={4}>
          <TextInput
            id="environmentalConditions"
            labelText={intl.formatMessage({
              id: "env.conditions",
              defaultMessage: "Environmental Conditions",
            })}
            placeholder={intl.formatMessage({
              id: "env.conditions.placeholder",
              defaultMessage: "e.g., 20°C, clear weather, dry season",
            })}
            value={environmentalFields.environmentalConditions || ""}
            onChange={(e) =>
              updateField("environmentalConditions", e.target.value)
            }
            disabled={isReadOnly}
          />
        </Column>
        <Column lg={5} md={4} sm={4}>
          <TextInput
            id="regulatoryReference"
            labelText={intl.formatMessage({
              id: "env.regulatoryReference",
              defaultMessage: "Regulatory Reference",
            })}
            placeholder={intl.formatMessage({
              id: "env.regulatoryReference.placeholder",
              defaultMessage: "e.g., PP No. 22/2021 Batu Mutu Air",
            })}
            value={environmentalFields.regulatoryReference || ""}
            onChange={(e) => updateField("regulatoryReference", e.target.value)}
            disabled={isReadOnly}
          />
        </Column>
        <Column lg={5} md={4} sm={4}>
          <TextInput
            id="collectionMethod"
            labelText={intl.formatMessage({
              id: "env.collectionMethod",
              defaultMessage: "Collection Method",
            })}
            placeholder={intl.formatMessage({
              id: "env.collectionMethod.placeholder",
              defaultMessage: "e.g., Grab sample, Composite 24h",
            })}
            value={environmentalFields.collectionMethod || ""}
            onChange={(e) => updateField("collectionMethod", e.target.value)}
            disabled={isReadOnly}
          />
        </Column>
      </Grid>
    </Tile>
  );
};

export default CollectionConditionsSection;
