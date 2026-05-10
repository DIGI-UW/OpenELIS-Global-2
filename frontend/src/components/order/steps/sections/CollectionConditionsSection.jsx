import React, { useState, useEffect, useRef } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Grid,
  Column,
  Tile,
  Select,
  SelectItem,
  TextInput,
  TextArea,
} from "@carbon/react";
import { getFromOpenElisServer } from "../../../utils/Utils";

const CollectionConditionsSection = ({
  orderData,
  setOrderData,
  isReadOnly,
}) => {
  const intl = useIntl();
  const componentMounted = useRef(true);

  const [collectionMethods, setCollectionMethods] = useState([]);
  const [weatherOptions, setWeatherOptions] = useState([]);

  const environmentalFields =
    orderData?.sampleOrderItems?.environmentalFields || {};

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer(
      "/rest/vector/dictionary/env-collection-methods",
      (data) => {
        if (componentMounted.current) setCollectionMethods(data || []);
      },
    );
    getFromOpenElisServer("/rest/vector/dictionary/env-weather", (data) => {
      if (componentMounted.current) setWeatherOptions(data || []);
    });
    return () => {
      componentMounted.current = false;
    };
  }, []);

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
          defaultMessage="Default Collection Conditions"
        />
      </h4>
      <p className="helper-text" style={{ marginBottom: "1rem" }}>
        <FormattedMessage
          id="env.collectionConditions.subtitle"
          defaultMessage="Defaults applied to all samples in this batch."
        />
      </p>

      <Grid>
        <Column lg={5} md={4} sm={4}>
          <Select
            id="collectionMethod"
            labelText={
              <span>
                <FormattedMessage
                  id="env.collectionMethod"
                  defaultMessage="Collection Method"
                />
                <span style={{ color: "#da1e28" }}> *</span>
              </span>
            }
            value={environmentalFields.collectionMethod || ""}
            onChange={(e) => updateField("collectionMethod", e.target.value)}
            disabled={isReadOnly}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "label.select",
                defaultMessage: "Select...",
              })}
            />
            {collectionMethods.map((m) => (
              <SelectItem key={m.id} value={m.dictEntry} text={m.dictEntry} />
            ))}
          </Select>
        </Column>

        <Column lg={4} md={4} sm={4}>
          <TextInput
            id="waterTemp"
            labelText={intl.formatMessage({
              id: "env.waterTemp",
              defaultMessage: "Water Temp (°C)",
            })}
            placeholder=""
            value={environmentalFields.waterTemp || ""}
            onChange={(e) => updateField("waterTemp", e.target.value)}
            disabled={isReadOnly}
          />
        </Column>

        <Column lg={4} md={4} sm={4}>
          <TextInput
            id="ambientTemp"
            labelText={intl.formatMessage({
              id: "env.ambientTemp",
              defaultMessage: "Ambient Temp (°C)",
            })}
            placeholder=""
            value={environmentalFields.ambientTemp || ""}
            onChange={(e) => updateField("ambientTemp", e.target.value)}
            disabled={isReadOnly}
          />
        </Column>

        <Column lg={5} md={4} sm={4}>
          <Select
            id="weather"
            labelText={intl.formatMessage({
              id: "env.weather",
              defaultMessage: "Weather",
            })}
            value={environmentalFields.weather || ""}
            onChange={(e) => updateField("weather", e.target.value)}
            disabled={isReadOnly}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "label.select",
                defaultMessage: "Select...",
              })}
            />
            {weatherOptions.map((w) => (
              <SelectItem key={w.id} value={w.dictEntry} text={w.dictEntry} />
            ))}
          </Select>
        </Column>

        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="preservationMethod"
            labelText={intl.formatMessage({
              id: "env.preservationMethod",
              defaultMessage: "Preservation Method",
            })}
            placeholder={intl.formatMessage({
              id: "env.preservationMethod.placeholder",
              defaultMessage: "e.g., HNO3 acidification, 4°C cooler",
            })}
            value={environmentalFields.preservationMethod || ""}
            onChange={(e) => updateField("preservationMethod", e.target.value)}
            disabled={isReadOnly}
          />
        </Column>

        <Column lg={16} md={8} sm={4}>
          <TextArea
            id="fieldNotes"
            labelText={intl.formatMessage({
              id: "env.fieldNotes",
              defaultMessage: "Field Notes",
            })}
            placeholder={intl.formatMessage({
              id: "env.fieldNotes.placeholder",
              defaultMessage: "Enter field observations...",
            })}
            value={environmentalFields.fieldNotes || ""}
            onChange={(e) => updateField("fieldNotes", e.target.value)}
            disabled={isReadOnly}
            rows={4}
          />
        </Column>
      </Grid>
    </Tile>
  );
};

export default CollectionConditionsSection;
