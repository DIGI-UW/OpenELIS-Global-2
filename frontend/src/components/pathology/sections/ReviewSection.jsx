import React from "react";
import { Select, SelectItem, Stack } from "@carbon/react";
import { useIntl } from "react-intl";
import RequestsTable from "../RequestsTable";
import { requestActionLock } from "../pathologySections";
import "../pathologyCaseView.scss";

/**
 * FR-10: who is reading the case, and the further work the pathologist has
 * asked the bench for, as a table of requests each raised and changed on its
 * own (see RequestsTable).
 *
 * The section takes its own read-only state and what is in flight as two
 * props, as Staining does: `readOnly` is the section's state, which holds its
 * fields, and `held` a change still out. The requests table answers to two
 * others: `takesRequests` offers New request and every request action (the
 * pathologist's open review only), and in bench mode (`benchRequests`,
 * FR-10.12) the requests can still be completed, cancelled and reopened,
 * which is how the bench works them. A signed-out case's review takes
 * neither, while its fields follow the section's state as before. While a
 * change is out the table keeps its rows and what was typed, held, and only
 * the pathologist field goes read-only with the rest of the form.
 *
 * Every request action is held by requestActionLock, read from the case's
 * stage, unsaved work (`dirty`), a save (`saving`) and a change in flight,
 * as Staining holds its marks. `requestPrefill` opens a new request with its
 * kind and target set.
 */
const ReviewSection = ({
  caseInfo,
  updateCase,
  readOnly,
  held = false,
  takesRequests = false,
  benchRequests = false,
  pathologistUsers,
  stains,
  dirty = false,
  saving = false,
  requestPrefill,
  onRaiseRequest,
  onChangeRequestStatus,
}) => {
  const intl = useIntl();

  return (
    <Stack gap={6}>
      <div className="pathology-case-view__field-group">
        <Select
          id="assignedPathologist"
          name="assignedPathologist"
          disabled={readOnly || held}
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
      <RequestsTable
        requests={caseInfo.requests}
        blocks={caseInfo.blocks}
        slides={caseInfo.slides}
        tissueTypeNames={caseInfo.tissueTypeNames}
        stains={stains}
        canRaise={takesRequests}
        canChangeStatus={takesRequests || benchRequests}
        lock={requestActionLock({
          status: caseInfo.status,
          dirty,
          saving,
          moving: held,
        })}
        held={held}
        prefill={requestPrefill}
        onRaise={onRaiseRequest}
        onChangeStatus={onChangeRequestStatus}
      />
    </Stack>
  );
};

export default ReviewSection;
