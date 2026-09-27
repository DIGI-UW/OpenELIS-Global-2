import React, { useState, useEffect, useRef } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Tile,
  Grid,
  Column,
  MultiSelect,
  Tag,
  InlineLoading,
} from "@carbon/react";
import { getFromOpenElisServer } from "../../../utils/Utils";

const ComplianceStandardsSection = ({
  orderData,
  setOrderData,
  isReadOnly,
}) => {
  const intl = useIntl();
  const componentMounted = useRef(true);
  const [standards, setStandards] = useState([]);
  const [loading, setLoading] = useState(true);

  const environmentalFields =
    orderData?.sampleOrderItems?.environmentalFields || {};

  const selectedIds = (() => {
    try {
      const raw = environmentalFields.complianceStandards;
      return raw ? JSON.parse(raw) : [];
    } catch {
      return [];
    }
  })();

  useEffect(() => {
    componentMounted.current = true;
    setLoading(true);
    getFromOpenElisServer("/rest/compliance/standards/active", (data) => {
      if (componentMounted.current) {
        setStandards(Array.isArray(data) ? data : []);
        setLoading(false);
      }
    });
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const handleSelectionChange = ({ selectedItems }) => {
    const ids = selectedItems.map((s) => s.id);
    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        environmentalFields: {
          ...prev.sampleOrderItems?.environmentalFields,
          complianceStandards: JSON.stringify(ids),
        },
      },
    }));
  };

  const items = standards.map((s) => ({
    id: s.id,
    label: `${s.regulationNumber} — ${s.name}`,
  }));

  const initialSelectedItems = items.filter((item) =>
    selectedIds.includes(item.id),
  );

  return (
    <Tile className="order-section">
      <h4 className="section-title">
        <FormattedMessage
          id="env.complianceStandards.title"
          defaultMessage="Applicable Compliance Standards"
        />
      </h4>
      <p className="helper-text" style={{ marginBottom: "1rem" }}>
        <FormattedMessage
          id="env.complianceStandards.subtitle"
          defaultMessage="Select the regulatory standards that apply to this order."
        />
      </p>

      <Grid>
        <Column lg={12} md={6} sm={4}>
          {loading ? (
            <InlineLoading
              description={intl.formatMessage({
                id: "loading",
                defaultMessage: "Loading...",
              })}
            />
          ) : (
            <MultiSelect
              key={`cs-${selectedIds.join(",")}`}
              id="complianceStandards"
              titleText={intl.formatMessage({
                id: "env.complianceStandards.label",
                defaultMessage: "Compliance Standards",
              })}
              label={intl.formatMessage({
                id: "env.complianceStandards.placeholder",
                defaultMessage: "Select standards...",
              })}
              items={items}
              itemToString={(item) => item?.label || ""}
              initialSelectedItems={initialSelectedItems}
              onChange={handleSelectionChange}
              disabled={isReadOnly || items.length === 0}
            />
          )}

          {!loading && items.length === 0 && (
            <p className="helper-text" style={{ marginTop: "0.5rem" }}>
              <FormattedMessage
                id="env.complianceStandards.empty"
                defaultMessage="No active compliance standards configured. Add them in Administration → Compliance Standards."
              />
            </p>
          )}

          {selectedIds.length > 0 && (
            <div
              style={{
                marginTop: "0.75rem",
                display: "flex",
                flexWrap: "wrap",
                gap: "0.5rem",
              }}
            >
              {standards
                .filter((s) => selectedIds.includes(s.id))
                .map((s) => (
                  <Tag key={s.id} type="blue" size="md">
                    {s.regulationNumber}
                  </Tag>
                ))}
            </div>
          )}
        </Column>
      </Grid>
    </Tile>
  );
};

export default ComplianceStandardsSection;
