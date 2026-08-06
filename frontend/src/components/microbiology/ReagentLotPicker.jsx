import React, { useState } from "react";
import { Information } from "@carbon/icons-react";
import {
  Button,
  InlineNotification,
  RadioButton,
  RadioButtonGroup,
  Search,
  Tag,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";

export const getReagentSelectionKey = (requirement) =>
  `${requirement.analysisId}:${requirement.linkId}`;

const selectedRequirementForLot = (requirements, selectedLots, lotNumber) =>
  requirements.find((requirement) => {
    const selected = selectedLots[getReagentSelectionKey(requirement)];
    return requirement.lots?.some(
      (lot) =>
        String(lot.id) === String(selected?.lotId) &&
        lot.lotNumber === lotNumber,
    );
  }) ||
  requirements.find((requirement) =>
    requirement.lots?.some((lot) => lot.lotNumber === lotNumber),
  );

export const formatReagentLotConflict = (
  response,
  requirements,
  selectedLots,
  intl,
) => {
  if (response?.error !== "MICROBIOLOGY_LOT_CONFLICT") return "";
  const requirement = selectedRequirementForLot(
    requirements,
    selectedLots,
    response.lotNumber,
  );
  const messageId = [
    "INVENTORY_LOT_EXPIRED",
    "INVENTORY_LOT_QC_FAILED",
    "INVENTORY_LOT_QC_NOT_PASSED",
    "INVENTORY_LOT_INSUFFICIENT_QUANTITY",
  ].includes(response.message)
    ? response.message
    : "INVENTORY_LOT_UNAVAILABLE";
  return intl.formatMessage(
    { id: `microbiology.reagentLots.conflict.${messageId}` },
    {
      reagent:
        requirement?.reagentName ||
        intl.formatMessage({ id: "microbiology.reagentLots.reagent" }),
      lot: response.lotNumber || "",
    },
  );
};

const ReagentLotPicker = ({
  id,
  requirements = [],
  selectedLots = {},
  onChange,
  disabled = false,
}) => {
  const intl = useIntl();
  const [scanValue, setScanValue] = useState("");
  const [scanFeedback, setScanFeedback] = useState(null);

  if (requirements.length === 0) {
    return (
      <p className="microbiology-reagent-lots__empty">
        {intl.formatMessage({ id: "microbiology.reagentLots.empty" })}
      </p>
    );
  }

  const unavailableReasonLabel = (reason) =>
    intl.formatMessage({
      id: `microbiology.reagentLots.reason.${reason}`,
      defaultMessage: formatMicrobiologyEnum(reason),
    });

  const scanLot = () => {
    const normalized = scanValue.trim().toLowerCase();
    if (!normalized) return;
    const matches = requirements.flatMap((requirement) =>
      (requirement.lots || [])
        .filter((lot) => lot.lotNumber?.trim().toLowerCase() === normalized)
        .map((lot) => ({ requirement, lot })),
    );
    if (matches.length !== 1) {
      setScanFeedback({
        kind: "error",
        message: intl.formatMessage({
          id:
            matches.length === 0
              ? "microbiology.reagentLots.scan.notFound"
              : "microbiology.reagentLots.scan.ambiguous",
        }),
      });
      return;
    }
    const { requirement, lot } = matches[0];
    if (!lot.available) {
      setScanFeedback({
        kind: "error",
        message: formatReagentLotConflict(
          {
            error: "MICROBIOLOGY_LOT_CONFLICT",
            message: lot.unavailableReason,
            lotNumber: lot.lotNumber,
          },
          requirements,
          {
            [getReagentSelectionKey(requirement)]: { lotId: lot.id },
          },
          intl,
        ),
      });
      return;
    }
    onChange({
      analysisId: requirement.analysisId,
      testReagentLinkId: requirement.linkId,
      lotId: Number(lot.id),
    });
    setScanValue("");
    setScanFeedback({
      kind: "success",
      message: intl.formatMessage(
        { id: "microbiology.reagentLots.scan.selected" },
        { lot: lot.lotNumber },
      ),
    });
  };

  return (
    <div className="microbiology-reagent-lots">
      <div>
        <div className="microbiology-reagent-lots__title">
          <h4>
            {intl.formatMessage({ id: "microbiology.reagentLots.title" })}
          </h4>
          <Button
            type="button"
            kind="ghost"
            size="sm"
            hasIconOnly
            renderIcon={Information}
            iconDescription={intl.formatMessage({
              id: "microbiology.reagentLots.fifo.tooltip",
            })}
          />
        </div>
        <p className="microbiology-card__hint">
          {intl.formatMessage({ id: "microbiology.reagentLots.hint" })}
        </p>
      </div>
      <Search
        id={`${id}-lot-scan`}
        labelText={intl.formatMessage({
          id: "microbiology.reagentLots.scan.label",
        })}
        placeholder={intl.formatMessage({
          id: "microbiology.reagentLots.scan.placeholder",
        })}
        value={scanValue}
        onChange={(event) => {
          setScanValue(event.target.value);
          setScanFeedback(null);
        }}
        onKeyDown={(event) => {
          if (event.key === "Enter") {
            event.preventDefault();
            scanLot();
          }
        }}
        disabled={disabled}
        size="lg"
      />
      {scanFeedback ? (
        <InlineNotification
          kind={scanFeedback.kind}
          title={scanFeedback.message}
          lowContrast
          hideCloseButton
        />
      ) : null}
      {requirements.map((requirement) => {
        const selectionKey = getReagentSelectionKey(requirement);
        const selectedLotId = selectedLots[selectionKey]?.lotId;
        const legend = intl.formatMessage(
          { id: "microbiology.reagentLots.legend" },
          { reagent: requirement.reagentName },
        );

        return (
          <div
            className="microbiology-reagent-lots__requirement"
            key={selectionKey}
          >
            <div className="microbiology-reagent-lots__heading">
              <div>
                <strong>{requirement.reagentName}</strong>
                <span className="microbiology-reagent-lots__test-name">
                  {requirement.testName}
                </span>
              </div>
              {requirement.usageType ? (
                <Tag type="cool-gray">
                  {intl.formatMessage({
                    id: `microbiology.reagentLots.role.${requirement.usageType}`,
                    defaultMessage: formatMicrobiologyEnum(
                      requirement.usageType,
                    ),
                  })}
                </Tag>
              ) : null}
            </div>
            <p className="microbiology-reagent-lots__quantity">
              {intl.formatMessage(
                { id: "microbiology.reagentLots.quantityPerTest" },
                {
                  quantity: requirement.quantityPerTest || 1,
                  unit: requirement.quantityUnit || "",
                },
              )}
            </p>
            <RadioButtonGroup
              name={`${id}-${selectionKey}`}
              legendText={legend}
              valueSelected={selectedLotId == null ? "" : String(selectedLotId)}
              onChange={(lotId, _name, event) =>
                onChange({
                  analysisId: requirement.analysisId,
                  testReagentLinkId: requirement.linkId,
                  lotId: Number(
                    event?.target?.value || lotId?.target?.value || lotId,
                  ),
                })
              }
              orientation="vertical"
              disabled={disabled}
            >
              {(requirement.lots || []).map((lot) => {
                const expiration = lot.effectiveExpirationDate
                  ? intl.formatDate(lot.effectiveExpirationDate, {
                      dateStyle: "medium",
                    })
                  : intl.formatMessage({
                      id: "microbiology.reagentLots.noExpiry",
                    });
                const label = intl.formatMessage(
                  { id: "microbiology.reagentLots.option" },
                  {
                    lot: lot.lotNumber,
                    quantity: lot.currentQuantity,
                    unit: requirement.quantityUnit || "",
                    expiration,
                  },
                );

                return (
                  <div
                    className="microbiology-reagent-lots__option"
                    key={lot.id}
                  >
                    <RadioButton
                      id={`${id}-${selectionKey}-${lot.id}`}
                      value={String(lot.id)}
                      labelText={label}
                      checked={String(selectedLotId) === String(lot.id)}
                      disabled={disabled || !lot.available}
                    />
                    <div className="microbiology-reagent-lots__tags">
                      <Tag type={lot.qcStatus === "PASSED" ? "green" : "gray"}>
                        {intl.formatMessage({
                          id: `microbiology.reagentLots.qc.${lot.qcStatus}`,
                          defaultMessage: `QC ${formatMicrobiologyEnum(
                            lot.qcStatus,
                          ).toLowerCase()}`,
                        })}
                      </Tag>
                      {lot.fefoRecommended ? (
                        <Tag type="green">
                          {intl.formatMessage({
                            id: "microbiology.reagentLots.fefo",
                          })}
                        </Tag>
                      ) : null}
                      {!lot.available ? (
                        <Tag type="red">
                          {intl.formatMessage(
                            { id: "microbiology.reagentLots.blocked" },
                            {
                              reason: unavailableReasonLabel(
                                lot.unavailableReason,
                              ),
                            },
                          )}
                        </Tag>
                      ) : null}
                    </div>
                  </div>
                );
              })}
              {(requirement.lots || []).length === 0 ? (
                <p className="microbiology-reagent-lots__empty">
                  {intl.formatMessage({
                    id: "microbiology.reagentLots.noLots",
                  })}
                </p>
              ) : null}
            </RadioButtonGroup>
          </div>
        );
      })}
    </div>
  );
};

export default ReagentLotPicker;
