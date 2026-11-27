import React, { useState } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import { Modal, RadioTile, TextArea, Tile, TileGroup } from "@carbon/react";
import { selectedStandardIds } from "./ComplianceStandardsSection";

export const REGULATION_DRIVEN = "REGULATION_DRIVEN";
export const AD_HOC = "AD_HOC";
const REFERENCE_MAX = 500;

/**
 * The order type an environmental order is on: the one it was saved with, or,
 * for an order saved before the choice existed, regulation-driven when it names
 * a compliance standard. A new order starts ad-hoc, which asks for nothing new.
 */
export const orderBranchOf = (orderData) => {
  const fields = orderData?.sampleOrderItems?.environmentalFields || {};
  if (
    fields.orderBranch === REGULATION_DRIVEN ||
    fields.orderBranch === AD_HOC
  ) {
    return fields.orderBranch;
  }
  return selectedStandardIds(orderData).length > 0 ? REGULATION_DRIVEN : AD_HOC;
};

/**
 * OGC-768: chooses between a regulation-driven order (one or more compliance
 * standards whose tests are suggested) and an ad-hoc one (tests picked freely,
 * with an optional regulatory reference for the record). Switching clears
 * what only the other type uses, after a confirmation.
 */
const EnvironmentalOrderBranch = ({ orderData, setOrderData, isReadOnly }) => {
  const intl = useIntl();
  const [pending, setPending] = useState(null);
  const fields = orderData?.sampleOrderItems?.environmentalFields || {};
  const branch = orderBranchOf(orderData);
  const reference = fields.regulatoryReference || "";

  const update = (changes) =>
    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        environmentalFields: {
          ...prev.sampleOrderItems?.environmentalFields,
          ...changes,
        },
      },
    }));

  const switchTo = (next) => {
    if (next === AD_HOC) {
      update({ orderBranch: AD_HOC, complianceStandards: "[]" });
    } else {
      update({ orderBranch: REGULATION_DRIVEN, regulatoryReference: "" });
    }
  };

  const choose = (next) => {
    if (next === branch) {
      return;
    }
    const loses =
      next === AD_HOC
        ? selectedStandardIds(orderData).length > 0
        : reference.trim().length > 0;
    if (loses) {
      setPending(next);
    } else {
      switchTo(next);
    }
  };

  return (
    <Tile className="order-section" data-testid="env-order-branch">
      <TileGroup
        name="env-order-branch"
        legend={intl.formatMessage({ id: "env.orderBranch.legend" })}
        valueSelected={branch}
        onChange={(value) => choose(value)}
        disabled={isReadOnly}
      >
        {[REGULATION_DRIVEN, AD_HOC].map((value) => (
          <RadioTile
            key={value}
            id={`env-order-branch-${value}`}
            value={value}
            disabled={isReadOnly}
          >
            <strong>
              <FormattedMessage id={`env.orderBranch.${value}`} />
            </strong>
            <p className="helper-text">
              <FormattedMessage id={`env.orderBranch.${value}.help`} />
            </p>
          </RadioTile>
        ))}
      </TileGroup>

      {branch === AD_HOC && (
        <TextArea
          id="env-regulatory-reference"
          labelText={intl.formatMessage({ id: "env.regulatoryReference" })}
          placeholder={intl.formatMessage({
            id: "env.regulatoryReference.placeholder",
          })}
          helperText={intl.formatMessage({
            id: "env.regulatoryReference.help",
          })}
          maxCount={REFERENCE_MAX}
          enableCounter
          rows={2}
          value={reference}
          disabled={isReadOnly}
          onChange={(event) =>
            update({
              orderBranch: AD_HOC,
              regulatoryReference: event.target.value.slice(0, REFERENCE_MAX),
            })
          }
        />
      )}

      {pending && (
        <Modal
          open
          id="env-order-branch-confirm"
          modalHeading={intl.formatMessage({
            id: "env.orderBranch.switch.title",
          })}
          primaryButtonText={intl.formatMessage({
            id: "env.orderBranch.switch.confirm",
          })}
          secondaryButtonText={intl.formatMessage({
            id: "env.orderBranch.switch.cancel",
          })}
          onRequestSubmit={() => {
            switchTo(pending);
            setPending(null);
          }}
          onRequestClose={() => setPending(null)}
        >
          <p>
            <FormattedMessage
              id={
                pending === AD_HOC
                  ? "env.orderBranch.switch.toAdHoc"
                  : "env.orderBranch.switch.toRegulation"
              }
            />
          </p>
        </Modal>
      )}
    </Tile>
  );
};

export default EnvironmentalOrderBranch;
