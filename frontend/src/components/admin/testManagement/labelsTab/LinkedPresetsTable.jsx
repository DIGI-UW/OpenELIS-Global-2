import React from "react";
import {
  StructuredListWrapper,
  StructuredListHead,
  StructuredListRow,
  StructuredListCell,
  StructuredListBody,
  NumberInput,
  Checkbox,
  Button,
} from "@carbon/react";
import { TrashCan } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";

/**
 * LinkedPresetsTable (OGC-285 M4, T112).
 *
 * Renders a Carbon StructuredList of per-link rows. Each row shows:
 *   - Preset name
 *   - Default Qty NumberInput
 *   - Max Qty NumberInput
 *   - Allow Override Checkbox
 *   - Remove (TrashCan) action
 *
 * Props:
 *   links            - array of { id, presetId, presetName, defaultQty, maxQty, allowOverride }
 *   masterOverride   - boolean; when false all Allow Override boxes are forced off + disabled
 *   onLinkChange     - (index, field, value) => void
 *   onRemove         - (index) => void
 */
function LinkedPresetsTable({
  links = [],
  masterOverride = true,
  onLinkChange,
  onRemove,
}) {
  const intl = useIntl();

  if (links.length === 0) {
    return (
      <p className="cds--label" style={{ marginTop: "1rem" }}>
        <FormattedMessage id="admin.testCatalog.labels.linkedPresets.empty" />
      </p>
    );
  }

  return (
    <StructuredListWrapper
      ariaLabel={intl.formatMessage({
        id: "admin.testCatalog.labels.linkedPresets.title",
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
            <FormattedMessage id="admin.testCatalog.labels.col.allowOverride" />
          </StructuredListCell>
          <StructuredListCell head>
            <FormattedMessage id="admin.testCatalog.labels.col.actions" />
          </StructuredListCell>
        </StructuredListRow>
      </StructuredListHead>
      <StructuredListBody>
        {links.map((link, index) => {
          const effectiveAllowOverride = masterOverride
            ? link.allowOverride
            : false;
          return (
            <StructuredListRow key={link.presetId ?? index}>
              <StructuredListCell>
                {link.presetName || link.presetId}
              </StructuredListCell>
              <StructuredListCell>
                <NumberInput
                  id={`defaultQty-${index}`}
                  labelText={intl.formatMessage({
                    id: "admin.testCatalog.labels.col.defaultQty",
                  })}
                  hideLabel
                  min={0}
                  max={999}
                  value={link.defaultQty}
                  onChange={(_e, { value }) =>
                    onLinkChange(index, "defaultQty", value)
                  }
                />
              </StructuredListCell>
              <StructuredListCell>
                <NumberInput
                  id={`maxQty-${index}`}
                  labelText={intl.formatMessage({
                    id: "admin.testCatalog.labels.col.maxQty",
                  })}
                  hideLabel
                  min={0}
                  max={999}
                  value={link.maxQty}
                  onChange={(_e, { value }) =>
                    onLinkChange(index, "maxQty", value)
                  }
                />
              </StructuredListCell>
              <StructuredListCell>
                <Checkbox
                  id={`allowOverride-${index}`}
                  labelText={intl.formatMessage({
                    id: "admin.testCatalog.labels.col.allowOverride",
                  })}
                  hideLabel
                  checked={effectiveAllowOverride}
                  disabled={!masterOverride}
                  onChange={(e) =>
                    onLinkChange(index, "allowOverride", e.target.checked)
                  }
                />
              </StructuredListCell>
              <StructuredListCell>
                <Button
                  kind="ghost"
                  size="sm"
                  iconDescription={intl.formatMessage({
                    id: "admin.testCatalog.labels.action.remove",
                  })}
                  renderIcon={TrashCan}
                  hasIconOnly
                  onClick={() => onRemove(index)}
                />
              </StructuredListCell>
            </StructuredListRow>
          );
        })}
      </StructuredListBody>
    </StructuredListWrapper>
  );
}

export default LinkedPresetsTable;
