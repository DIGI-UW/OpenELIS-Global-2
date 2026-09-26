import React from "react";
import {
  DismissibleTag,
  FilterableMultiSelect,
  Select,
  SelectItem,
  Stack,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import "../pathologyCaseView.scss";

/**
 * FR-10: who is reading the case, and the further work the pathologist has
 * asked the bench for before they will sign it out.
 *
 * A request is stamped open the moment it is picked, which is the status the
 * server stores for a request raised without one. Doing it here rather than
 * relying on that default is what lets the summary panel, the rail and the
 * section badge all read the one status field, instead of each having to
 * treat an absent status as a fourth meaning of its own.
 */
const ReviewSection = ({
  caseInfo,
  updateCase,
  readOnly,
  pathologistUsers,
  requests,
  requestStatuses,
  formVersion,
}) => {
  const intl = useIntl();

  const selectedRequests = caseInfo.requests ?? [];

  // Carbon labels a tag's close button from title, and falls back to
  // dismissTooltipLabel only once the tag's own text has been ellipsised, so
  // both have to be given or the button reads Carbon's English "Dismiss".
  const removeLabel = (name) =>
    intl.formatMessage({ id: "common.removeSelection" }, { name });

  const removeRequest = (index) =>
    updateCase((prev) => ({
      requests: (prev.requests ?? []).filter(
        (_, position) => position !== index,
      ),
    }));

  const patchRequest = (index, patch) =>
    updateCase((prev) => ({
      requests: (prev.requests ?? []).map((request, position) =>
        position === index ? { ...request, ...patch } : request,
      ),
    }));

  return (
    <Stack gap={6}>
      <div className="pathology-case-view__field-group">
        <Select
          id="assignedPathologist"
          name="assignedPathologist"
          disabled={readOnly}
          labelText={intl.formatMessage({
            id: "label.button.select.pathologist",
          })}
          value={caseInfo.assignedPathologistId}
          onChange={(e) =>
            updateCase({ assignedPathologistId: e.target.value })
          }
        >
          <SelectItem
            value=""
            text={intl.formatMessage({ id: "common.select" })}
          />
          {pathologistUsers.map((user, index) => (
            <SelectItem key={index} text={user.value} value={user.id} />
          ))}
        </Select>
      </div>
      <div className="pathology-case-view__field-group">
        {/*
          Carbon reads initialSelectedItems once, at mount, so a discard that
          restores the saved case would otherwise leave the control showing
          what was discarded. Remounting on the form version is what makes the
          reset real.
        */}
        <FilterableMultiSelect
          key={"requests-" + formVersion}
          id="requests"
          disabled={readOnly}
          titleText={<FormattedMessage id="pathology.label.request" />}
          items={requests}
          itemToString={(item) => (item ? item.value : "")}
          initialSelectedItems={caseInfo.requests}
          onChange={(changes) =>
            updateCase({
              requests: changes.selectedItems.map((item) =>
                item.status ? item : { ...item, status: "OPENED" },
              ),
            })
          }
          placeholder={intl.formatMessage({ id: "common.select" })}
          selectionFeedback="top-after-reopen"
        />
      </div>
      <div>
        {selectedRequests.map((request, index) => (
          <div className="pathology-case-view__row" key={index}>
            <span className="pathology-case-view__row-field">
              <DismissibleTag
                text={request.value}
                disabled={readOnly}
                onClose={() => removeRequest(index)}
                title={removeLabel(request.value)}
                dismissTooltipLabel={removeLabel(request.value)}
              />
            </span>
            <div className="pathology-case-view__row-field">
              {/*
                No blank option: every request in the form carries a status,
                and an empty one posts as no status at all, which the server
                reads as open. Offering it would let a closed request be
                reopened by picking the option that looks like it means
                nothing.
              */}
              <Select
                id={"requeststatus" + index}
                name="requeststatus"
                disabled={readOnly}
                labelText={intl.formatMessage({
                  id: "label.button.select.status",
                })}
                value={request.status}
                onChange={(e) =>
                  patchRequest(index, { status: e.target.value })
                }
              >
                {requestStatuses.map((status, position) => (
                  <SelectItem
                    key={position}
                    text={status.value}
                    value={status.id}
                  />
                ))}
              </Select>
            </div>
          </div>
        ))}
      </div>
    </Stack>
  );
};

export default ReviewSection;
