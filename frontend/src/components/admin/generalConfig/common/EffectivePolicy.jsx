import React from "react";
import {
  StructuredListBody,
  StructuredListCell,
  StructuredListHead,
  StructuredListRow,
  StructuredListWrapper,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { policyOutcomes, roleNamesOf } from "./ValidationPolicyEditor";

/**
 * The policy each active lab unit follows: its active override, otherwise the
 * lab-wide default as it stands on the page, saved or not.
 */
export default function EffectivePolicy({
  labUnits,
  overrides,
  labDefault,
  labDefaultRoleNames,
  roleOptions,
  blockSelfValidation,
}) {
  const intl = useIntl();
  const rows = labUnits.map((unit) => {
    const override = overrides.find(
      (candidate) => candidate.active && candidate.testSectionId === unit.id,
    );
    const outcomes = override
      ? policyOutcomes(
          override,
          roleNamesOf(override, roleOptions),
          blockSelfValidation,
          intl,
        )
      : policyOutcomes(
          labDefault,
          labDefaultRoleNames,
          blockSelfValidation,
          intl,
        );
    return { unit, override, outcomes };
  });

  return (
    <div data-testid="validation-effective-policy">
      <h5>
        <FormattedMessage id="admin.validationPolicy.effective.title" />
      </h5>
      <p className="cds--label">
        <FormattedMessage id="admin.validationPolicy.effective.help" />
      </p>
      {rows.length === 0 ? (
        <p>
          <FormattedMessage id="admin.validationPolicy.effective.empty" />
        </p>
      ) : (
        <StructuredListWrapper isCondensed>
          <StructuredListHead>
            <StructuredListRow head>
              <StructuredListCell head>
                <FormattedMessage id="admin.validationPolicy.overrides.column.labUnit" />
              </StructuredListCell>
              <StructuredListCell head>
                <FormattedMessage id="admin.validationPolicy.effective.follows" />
              </StructuredListCell>
              <StructuredListCell head>
                <FormattedMessage id="admin.validationPolicy.effective.clean" />
              </StructuredListCell>
              <StructuredListCell head>
                <FormattedMessage id="admin.validationPolicy.effective.flagged" />
              </StructuredListCell>
            </StructuredListRow>
          </StructuredListHead>
          <StructuredListBody>
            {rows.map(({ unit, override, outcomes }) => (
              <StructuredListRow
                key={unit.id}
                data-testid={`validation-effective-${unit.id}`}
              >
                <StructuredListCell>{unit.name}</StructuredListCell>
                <StructuredListCell>
                  <Tag size="sm" type={override ? "purple" : "gray"}>
                    <FormattedMessage
                      id={
                        override
                          ? "admin.validationPolicy.effective.override"
                          : "admin.validationPolicy.effective.default"
                      }
                    />
                  </Tag>
                </StructuredListCell>
                <StructuredListCell>{outcomes.clean}</StructuredListCell>
                <StructuredListCell>{outcomes.flagged}</StructuredListCell>
              </StructuredListRow>
            ))}
          </StructuredListBody>
        </StructuredListWrapper>
      )}
      <p className="cds--label">
        <FormattedMessage id="admin.validationPolicy.effective.alwaysValidate" />
      </p>
    </div>
  );
}
