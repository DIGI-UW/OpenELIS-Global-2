/**
 * Color Picker Section Component
 *
 * Handles color selection with color picker and text input.
 * Accepts any valid CSS color format (hex, named colors, rgb(), hsl(), etc.)
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
  const [colorValue, setColorValue] = useState(value || "#0f62fe");
  const [error, setError] = useState(null);

  useEffect(() => {
    setColorValue(value || "#0f62fe");
  }, [value]);

  const handleColorPickerChange = (event) => {
    const newColor = event.target.value;
    setColorValue(newColor);
    setError(null);
    if (onChange) {
      onChange(newColor);
    }
  };

  const handleColorInputChange = (event) => {
    // Accept any CSS color format - validation happens via the preview square.
    // Named colors (e.g., "rebeccapurple"), rgb(), hsl(), etc. are all valid.
    const newColor = event.target.value;
    setColorValue(newColor);
    setError(null);
    if (onChange && newColor) {
      onChange(newColor);
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

          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "1rem",
              marginTop: "1rem",
            }}
          >
            {/* Color Preview */}
            <div
              data-testid="color-preview"
              style={{
                width: "40px",
                height: "40px",
                backgroundColor: colorValue,
                border: "1px solid #ccc",
                borderRadius: "4px",
              }}
              aria-label={`Color preview: ${colorValue}`}
            />

            {/* HTML5 Color Picker */}
            <input
              type="color"
              value={colorValue}
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

            {/* Color Input */}
            <TextInput
              id={`${label.toLowerCase().replace(/\s+/g, "-")}-color`}
              labelText={intl.formatMessage({
                id: "site.branding.color.hex.label",
              })}
              value={colorValue}
              onChange={handleColorInputChange}
              placeholder={intl.formatMessage({
                id: "site.branding.colorPicker.placeholder",
                defaultMessage: "#0f62fe or blue",
              })}
              invalid={!!error}
              invalidText={error}
              helperText={
                helperText ||
                intl.formatMessage({
                  id: "site.branding.colorPicker.helperText",
                  defaultMessage:
                    "Enter any CSS color (e.g., #0f62fe, blue, rgb(15, 98, 254))",
                })
              }
              style={{ flex: 1, maxWidth: "200px" }}
            />
          </div>
        </Column>
      </Grid>
    </Section>
  );
}

export default ColorPickerSection;
