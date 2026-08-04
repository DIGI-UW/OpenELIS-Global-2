import React from "react";
import { RadioButton, RadioButtonGroup, Tag } from "@carbon/react";
import { useIntl } from "react-intl";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";

export const getReagentSelectionKey = (requirement) =>
  `${requirement.analysisId}:${requirement.linkId}`;

const ReagentLotPicker = ({
  id,
  requirements = [],
  selectedLots = {},
  onChange,
  disabled = false,
}) => {
  const intl = useIntl();

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

  return (
    <div className="microbiology-reagent-lots">
      <div>
        <h4>{intl.formatMessage({ id: "microbiology.reagentLots.title" })}</h4>
        <p className="microbiology-card__hint">
          {intl.formatMessage({ id: "microbiology.reagentLots.hint" })}
        </p>
      </div>
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
                      disabled={disabled || !lot.available}
                    />
                    <div className="microbiology-reagent-lots__tags">
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
