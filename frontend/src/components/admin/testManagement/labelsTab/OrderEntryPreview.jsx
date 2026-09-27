import React from "react";
import {
  StructuredListWrapper,
  StructuredListHead,
  StructuredListRow,
  StructuredListCell,
  StructuredListBody,
  Tag,
} from "@carbon/react";
import { Locked } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";

/**
 * OrderEntryPreview (OGC-285 M4, T113 / AC-10).
 *
 * Renders a read-only Carbon StructuredList summarising how the linked presets
 * will appear at Order Entry time. Columns: Preset Name | Default Qty | Max Qty
 * | Lock indicator (if allow_override = false).
 *
 * Props:
 *   links          - array of { presetId, presetName, defaultQty, maxQty, allowOverride }
 *   masterOverride - boolean; when false all rows show the locked indicator
 */
function OrderEntryPreview({ links = [], masterOverride = true }) {
  const intl = useIntl();

  if (links.length === 0) {
    return null;
  }

  return (
    <>
      <p
        className="cds--label"
        style={{
          marginTop: "1.5rem",
          marginBottom: "0.5rem",
          fontWeight: "600",
        }}
      >
        <FormattedMessage id="admin.testCatalog.labels.preview.title" />
      </p>
      <StructuredListWrapper
        ariaLabel={intl.formatMessage({
          id: "admin.testCatalog.labels.preview.title",
        })}
      >
        <StructuredListHead>
          <StructuredListRow head>
            <StructuredListCell head>
              <FormattedMessage id="admin.testCatalog.labels.col.presetName" />
            </StructuredListCell>
            <StructuredListCell head>
              <FormattedMessage id="admin.testCatalog.labels.col.defaultQty" />
            </StructuredListCell>
            <StructuredListCell head>
              <FormattedMessage id="admin.testCatalog.labels.col.maxQty" />
            </StructuredListCell>
            <StructuredListCell head>
              <FormattedMessage id="admin.testCatalog.labels.preview.col.locked" />
            </StructuredListCell>
          </StructuredListRow>
        </StructuredListHead>
        <StructuredListBody>
          {links.map((link, index) => {
            const effectivelyLocked = !masterOverride || !link.allowOverride;
            return (
              <StructuredListRow key={link.presetId ?? index}>
                <StructuredListCell>
                  {link.presetName || link.presetId}
                </StructuredListCell>
                <StructuredListCell>{link.defaultQty}</StructuredListCell>
                <StructuredListCell>{link.maxQty}</StructuredListCell>
                <StructuredListCell>
                  {effectivelyLocked ? (
                    <Tag type="red" renderIcon={Locked}>
                      <FormattedMessage id="admin.testCatalog.labels.preview.locked" />
                    </Tag>
                  ) : (
                    <Tag type="green">
                      <FormattedMessage id="admin.testCatalog.labels.preview.unlocked" />
                    </Tag>
                  )}
                </StructuredListCell>
              </StructuredListRow>
            );
          })}
        </StructuredListBody>
      </StructuredListWrapper>
    </>
  );
}

export default OrderEntryPreview;
