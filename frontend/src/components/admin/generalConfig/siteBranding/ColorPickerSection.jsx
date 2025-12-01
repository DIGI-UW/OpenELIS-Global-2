/**
 * Color Picker Section Component
 * 
 * Handles color selection with color picker and hex input
 * 
 * Task Reference: T051
 */

import React, { useState, useEffect } from "react";
import {
  Grid,
  Column,
  Section,
  TextInput,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

function ColorPickerSection({ 
  label, 
  description,
  value, 
  onChange,
  helperText,
}) {
  const intl = useIntl();
  const [hexValue, setHexValue] = useState(value || "#1d4ed8");
  const [error, setError] = useState(null);

  useEffect(() => {
    setHexValue(value || "#1d4ed8");
  }, [value]);

  const validateHexColor = (hex) => {
    const hexPattern = /^#[0-9A-Fa-f]{3,6}$/;
    return hexPattern.test(hex);
  };

  const handleColorPickerChange = (event) => {
    const newColor = event.target.value;
    setHexValue(newColor);
    setError(null);
    if (onChange) {
      onChange(newColor);
    }
  };

  const handleHexInputChange = (event) => {
    const newHex = event.target.value;
    setHexValue(newHex);

    if (newHex && !validateHexColor(newHex)) {
      setError(intl.formatMessage({ id: "site.branding.color.format.error" }));
    } else {
      setError(null);
      if (onChange && validateHexColor(newHex)) {
        onChange(newHex);
      }
    }
  };

  return (
    <Section>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <h4>{label}</h4>
          {description && <p>{description}</p>}

          {error && (
            <InlineNotification
              kind="error"
              title={intl.formatMessage({ id: "error.title" })}
              subtitle={error}
              onClose={() => setError(null)}
            />
          )}

          <div style={{ display: "flex", alignItems: "center", gap: "1rem", marginTop: "1rem" }}>
            {/* Color Preview */}
            <div
              data-testid="color-preview"
              style={{
                width: "40px",
                height: "40px",
                backgroundColor: hexValue,
                border: "1px solid #ccc",
                borderRadius: "4px",
              }}
              aria-label={`Color preview: ${hexValue}`}
            />

            {/* HTML5 Color Picker */}
            <input
              type="color"
              value={hexValue}
              onChange={handleColorPickerChange}
              style={{
                width: "60px",
                height: "40px",
                border: "1px solid #ccc",
                borderRadius: "4px",
                cursor: "pointer",
              }}
              aria-label={label}
            />

            {/* Hex Input */}
            <TextInput
              id={`${label.toLowerCase().replace(/\s+/g, "-")}-hex`}
              labelText={intl.formatMessage({ id: "site.branding.color.hex.label" })}
              value={hexValue}
              onChange={handleHexInputChange}
              placeholder="#RRGGBB"
              invalid={!!error}
              invalidText={error}
              helperText={helperText || intl.formatMessage({ id: "site.branding.colorPicker.helperText" })}
              style={{ flex: 1, maxWidth: "200px" }}
            />
          </div>
        </Column>
      </Grid>
    </Section>
  );
}

export default ColorPickerSection;

