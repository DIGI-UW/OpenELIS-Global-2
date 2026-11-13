import React, { useState } from "react";
import {
  Button,
  InlineLoading,
  InlineNotification,
  Stack,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { putToOpenElisServerFullResponse } from "../../../utils/Utils";
import {
  useInvalidateServerData,
  useServerData,
} from "../../../utils/useServerData";
import ValidationPolicyEditor, {
  policyError,
  policyOutcomes,
  roleNameOf,
  toDraft,
} from "./ValidationPolicyEditor";
import LabUnitOverrides, { LAB_UNIT_POLICIES_API } from "./LabUnitOverrides";
import EffectivePolicy from "./EffectivePolicy";

export { policyError };

export const VALIDATION_POLICY_API = "/rest/validation-policy/default";

export default function ValidationPolicySection({ policy: loaded }) {
  const intl = useIntl();
  const invalidate = useInvalidateServerData();
  const { data: labUnitPolicies, isError: labUnitsFailed } = useServerData(
    LAB_UNIT_POLICIES_API,
  );
  const [policy, setPolicy] = useState(loaded);
  const [draft, setDraft] = useState(() => toDraft(loaded));
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState(null);

  const [seen, setSeen] = useState(loaded);
  if (loaded !== seen) {
    setSeen(loaded);
    setPolicy(loaded);
    if (draft.trigger === policy.trigger && draft.levels === policy.levels) {
      setDraft(toDraft(loaded));
    }
  }

  const error = policyError(draft, policy.maxLevels);
  const changed =
    draft.trigger !== policy.trigger || draft.levels !== policy.levels;
  const roleName = roleNameOf(policy);
  const message = (code, values) =>
    intl.formatMessage({ id: `admin.validationPolicy.${code}` }, values);

  const { clean, flagged } = policyOutcomes(
    draft,
    roleName,
    policy.blockSelfValidation,
    intl,
  );
  const blocked = policy.blockSelfValidation && draft.trigger !== "ALL";
  const turnsReviewOff =
    !error && draft.trigger === "NONE" && policy.trigger !== "NONE";

  const editDraft = (next) => {
    setDraft(next);
    setNotice(null);
  };

  const save = () => {
    setSaving(true);
    setNotice(null);
    putToOpenElisServerFullResponse(
      VALIDATION_POLICY_API,
      JSON.stringify({ trigger: draft.trigger, levels: draft.levels }),
      async (response) => {
        setSaving(false);
        if (response && response.ok) {
          const saved = await response.json();
          setPolicy(saved);
          setDraft(toDraft(saved));
          invalidate(VALIDATION_POLICY_API);
          setNotice({ kind: "success", code: "saved" });
          return;
        }
        let body;
        try {
          body = (await response?.json()) || {};
        } catch {
          body = {};
        }
        setNotice({
          kind: "error",
          code: `refused.${body.code || "saveFailed"}`,
        });
      },
    );
  };

  return (
    <div className="validationPolicySection" data-testid="validation-policy">
      <Stack gap={7}>
        <Stack gap={5}>
          <div>
            <h4>
              <FormattedMessage id="admin.validationPolicy.title" />
            </h4>
            <h5>
              <FormattedMessage id="admin.validationPolicy.labDefault" />
            </h5>
            <p className="cds--label">
              <FormattedMessage id="admin.validationPolicy.labDefault.help" />
            </p>
          </div>
          <ValidationPolicyEditor
            idPrefix="validation-policy"
            draft={draft}
            onChange={editDraft}
            maxLevels={policy.maxLevels}
            roleName={roleName}
          />
          {blocked && (
            <InlineNotification
              kind="warning"
              lowContrast
              hideCloseButton
              title={message("blockSelfValidation")}
            />
          )}
          {turnsReviewOff && (
            <InlineNotification
              kind="warning"
              lowContrast
              hideCloseButton
              title={message("labDefault.turnsReviewOff")}
            />
          )}
          {!error && (
            <div data-testid="validation-policy-summary">
              <p>
                <FormattedMessage
                  id="admin.validationPolicy.summary.clean"
                  values={{ outcome: clean }}
                />
              </p>
              <p>
                <FormattedMessage
                  id="admin.validationPolicy.summary.flagged"
                  values={{ outcome: flagged }}
                />
              </p>
            </div>
          )}
          {notice && (
            <InlineNotification
              kind={notice.kind}
              lowContrast
              title={message(notice.code)}
              onClose={() => setNotice(null)}
            />
          )}
          <div>
            {saving ? (
              <InlineLoading description={message("saving")} />
            ) : (
              <Button
                id="validation-policy-save"
                disabled={!changed || Boolean(error)}
                onClick={save}
              >
                <FormattedMessage id="admin.validationPolicy.save" />
              </Button>
            )}
          </div>
        </Stack>
        <LabUnitOverrides
          policies={labUnitPolicies}
          failed={labUnitsFailed}
          labDefault={policy}
        />
        {labUnitPolicies && (
          <EffectivePolicy
            labUnits={labUnitPolicies.labUnits}
            overrides={labUnitPolicies.overrides}
            labDefault={error ? toDraft(policy) : draft}
            roleName={roleName}
            blockSelfValidation={policy.blockSelfValidation}
          />
        )}
      </Stack>
    </div>
  );
}
