import React from "react";
import { Button, Select, SelectItem, TextInput } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { IdValue } from "./ExpandedPanel";

/**
 * OGC-1023 (R4, FR-F1/F2) — "Refer this test" is a distinct row action, not an
 * NCE disposition. Reference laboratory and reason are required; the referral
 * date defaults to now and stays editable. There is deliberately NO
 * "test to perform" field — the referred test IS this row's test, and the save
 * payload carries its id silently. The referral persists with the row's Save
 * (e-signature), riding the legacy handleReferrals path, which also sets
 * Analysis.referredOut and hands off to the Referrals subsystem (FR-F3).
 */
export interface ReferralDraft {
  referredInstituteId: string;
  referralReasonId: string;
  referredSendDate: string;
}

export const emptyReferralDraft = (today: string): ReferralDraft => ({
  referredInstituteId: "",
  referralReasonId: "",
  referredSendDate: today,
});

interface ReferralActionProps {
  rowKey: string;
  organizations: IdValue[];
  reasons: IdValue[];
  draft: ReferralDraft;
  onDraftChange: (draft: ReferralDraft) => void;
  onCancel: () => void;
}

const ReferralAction: React.FC<ReferralActionProps> = ({
  rowKey,
  organizations,
  reasons,
  draft,
  onDraftChange,
  onCancel,
}) => {
  const intl = useIntl();
  return (
    <div className="unifiedReferral" data-testid={`referral-${rowKey}`}>
      <div className="unifiedReferralGrid">
        <Select
          id={`referral-org-${rowKey}`}
          labelText={intl.formatMessage({
            id: "label.results.referral.organization",
          })}
          value={draft.referredInstituteId}
          onChange={(e: React.ChangeEvent<HTMLSelectElement>) =>
            onDraftChange({ ...draft, referredInstituteId: e.target.value })
          }
        >
          <SelectItem value="" text="" />
          {organizations.map((org) => (
            <SelectItem key={org.id} value={org.id} text={org.value} />
          ))}
        </Select>
        <Select
          id={`referral-reason-${rowKey}`}
          labelText={intl.formatMessage({
            id: "label.results.referral.reason",
          })}
          value={draft.referralReasonId}
          onChange={(e: React.ChangeEvent<HTMLSelectElement>) =>
            onDraftChange({ ...draft, referralReasonId: e.target.value })
          }
        >
          <SelectItem value="" text="" />
          {reasons.map((reason) => (
            <SelectItem key={reason.id} value={reason.id} text={reason.value} />
          ))}
        </Select>
        <TextInput
          id={`referral-date-${rowKey}`}
          labelText={intl.formatMessage({ id: "label.results.referral.date" })}
          value={draft.referredSendDate}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
            onDraftChange({ ...draft, referredSendDate: e.target.value })
          }
        />
      </div>
      <div className="unifiedReferralHint">
        <FormattedMessage id="label.results.referral.hint" />
        <Button kind="ghost" size="sm" onClick={onCancel}>
          <FormattedMessage id="label.results.referral.cancel" />
        </Button>
      </div>
    </div>
  );
};

export default ReferralAction;
