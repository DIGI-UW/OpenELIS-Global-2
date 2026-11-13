import React from "react";
import {
  InlineNotification,
  NumberInput,
  RadioButton,
  RadioButtonGroup,
  Select,
  SelectItem,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

export const TRIGGERS = ["ALL", "NEEDS_REVIEW", "NONE"];

export const LEVEL_ROLES_API = "/rest/validation-policy/roles";

export const levelRolesUrl = (labUnitId) =>
  labUnitId
    ? `${LEVEL_ROLES_API}?labUnitId=${encodeURIComponent(labUnitId)}`
    : LEVEL_ROLES_API;

export function policyError(policy, maxLevels) {
  if (maxLevels != null && policy.levels > maxLevels) {
    return "levelsOutOfRange";
  }
  if (policy.levels === 0 && policy.trigger !== "NONE") {
    return "zeroReviewsNeedsNoReview";
  }
  if (policy.levels > 0 && policy.trigger === "NONE") {
    return "noReviewWithReviews";
  }
  return null;
}

export const toDraft = (policy) => ({
  trigger: policy.trigger,
  levels: policy.levels,
  roleIds: (policy.levelRoles || []).map((level) => level.roleId),
});

/** The role id of each level the draft asks for; a level left unset is null. */
export const draftRoleIds = (draft) =>
  Array.from({ length: Math.max(draft.levels, 0) }, (_, index) =>
    draft.roleIds && draft.roleIds[index] != null ? draft.roleIds[index] : null,
  );

export const draftPayload = (draft) => ({
  trigger: draft.trigger,
  levels: draft.levels,
  roleIds: draftRoleIds(draft),
});

export const sameDraft = (a, b) =>
  a.trigger === b.trigger &&
  a.levels === b.levels &&
  JSON.stringify(draftRoleIds(a)) === JSON.stringify(draftRoleIds(b));

/**
 * The name of each level's role: the draft's choice when the role list is
 * known, otherwise what the saved policy says, Validation when unset.
 */
export function roleNamesOf(policy, roleOptions) {
  const saved = policy.levelRoles || [];
  const ids = policy.roleIds
    ? draftRoleIds(policy)
    : saved.map((level) => level.roleId);
  return Array.from({ length: Math.max(policy.levels, 0) }, (_, index) => {
    const roleId = ids[index];
    const option = (roleOptions || []).find((role) => role.id === roleId);
    if (option) {
      return option.name;
    }
    const level = saved[index];
    if (level && level.roleId === roleId && level.roleName) {
      return level.roleName;
    }
    return "Validation";
  });
}

/**
 * What a policy does with the results the clearance rule clears and with the
 * ones that need review, as the page words them.
 */
export function policyOutcomes(policy, roleNames, blockSelfValidation, intl) {
  const message = (code, values) =>
    intl.formatMessage({ id: `admin.validationPolicy.${code}` }, values);
  const names = Array.isArray(roleNames) ? roleNames : [roleNames];
  const reviews = message("summary.reviews", {
    count: policy.levels,
    role: names.slice(0, Math.max(policy.levels, 1)).join(" → "),
  });
  const released = message("summary.released");
  if (blockSelfValidation && policy.trigger !== "ALL") {
    const waits = message("summary.blocked");
    return { clean: waits, flagged: waits };
  }
  return {
    clean: policy.trigger === "ALL" ? reviews : released,
    flagged: policy.trigger === "NONE" ? released : reviews,
  };
}

/**
 * Which results need review, the number of reviews and the role of each level,
 * shared by the lab-wide default and the lab unit overrides. A level whose role
 * nobody active can sign for the lab unit is flagged.
 */
export default function ValidationPolicyEditor({
  idPrefix,
  draft,
  onChange,
  maxLevels,
  roleOptions,
  labUnitName,
}) {
  const intl = useIntl();
  const message = (code, values) =>
    intl.formatMessage({ id: `admin.validationPolicy.${code}` }, values);
  const error = policyError(draft, maxLevels);
  const roles = roleOptions || [];
  const defaultRoleId = roles.length > 0 ? roles[0].id : null;
  const roleIds = draftRoleIds(draft);

  const setTrigger = (trigger) => {
    const levels =
      trigger === "NONE" ? 0 : Math.max(1, Math.min(draft.levels, maxLevels));
    onChange({ ...draft, trigger, levels });
  };

  const setLevels = (value) => {
    const levels = Number.isNaN(Number(value)) ? 0 : Number(value);
    onChange({ ...draft, levels });
  };

  const setRole = (index, roleId) => {
    const next = draftRoleIds({ ...draft, levels: Math.max(draft.levels, 0) });
    next[index] = roleId;
    onChange({ ...draft, roleIds: next });
  };

  const shownLevels = Math.max(0, Math.min(draft.levels, maxLevels));

  return (
    <>
      <RadioButtonGroup
        legendText={message("trigger.legend")}
        name={`${idPrefix}-trigger`}
        orientation="vertical"
        valueSelected={draft.trigger}
        onChange={(value) => setTrigger(value)}
      >
        {TRIGGERS.map((trigger) => (
          <RadioButton
            key={trigger}
            id={`${idPrefix}-trigger-${trigger}`}
            value={trigger}
            labelText={message(`trigger.${trigger}`)}
          />
        ))}
      </RadioButtonGroup>
      {draft.trigger === "NEEDS_REVIEW" && (
        <p className="cds--label">
          <FormattedMessage id="admin.validationPolicy.trigger.NEEDS_REVIEW.help" />
        </p>
      )}
      <NumberInput
        id={`${idPrefix}-levels`}
        label={message("levels.label")}
        helperText={message("levels.help", { max: maxLevels })}
        min={0}
        max={maxLevels}
        step={1}
        value={draft.levels}
        allowEmpty={false}
        onChange={(event, { value }) => setLevels(value)}
      />
      {Array.from({ length: shownLevels }).map((_, index) => {
        const roleId = roleIds[index] != null ? roleIds[index] : defaultRoleId;
        const option = roles.find((role) => role.id === roleId);
        return (
          <React.Fragment key={index}>
            <Select
              id={`${idPrefix}-level-${index + 1}-role`}
              labelText={message("level.role", { level: index + 1 })}
              helperText={message("level.role.help")}
              value={roleId == null ? "" : String(roleId)}
              disabled={roles.length === 0}
              onChange={(event) => setRole(index, Number(event.target.value))}
            >
              {roles.map((role) => (
                <SelectItem
                  key={role.id}
                  value={String(role.id)}
                  text={role.name}
                />
              ))}
            </Select>
            {option && option.signers === 0 && (
              <InlineNotification
                kind="warning"
                lowContrast
                hideCloseButton
                data-testid={`${idPrefix}-level-${index + 1}-no-signer`}
                title={message("level.noSigner", {
                  role: option.name,
                  unit: labUnitName || message("level.noSigner.anyUnit"),
                })}
              />
            )}
          </React.Fragment>
        );
      })}
      {shownLevels > 1 && (
        <p className="cds--label" data-testid={`${idPrefix}-different-people`}>
          <FormattedMessage id="admin.validationPolicy.levels.differentPeople" />
        </p>
      )}
      {error && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={message(`refused.${error}`)}
        />
      )}
    </>
  );
}
