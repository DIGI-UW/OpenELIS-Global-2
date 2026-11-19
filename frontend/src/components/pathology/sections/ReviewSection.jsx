import React from "react";
import { Stack, TextArea } from "@carbon/react";
import { useIntl } from "react-intl";
import SearchableSelect from "../../common/SearchableSelect";
import RequestsTable from "../RequestsTable";
import SlidesReceived from "./SlidesReceived";
import {
  neverCheckedSlides,
  requestActionLock,
  slidesReceived,
} from "../pathologySections";
import "../pathologyCaseView.scss";

// Where a slide whose label was never checked is still worth naming: while
// the case waits for the pathologist and while it is read.
const READING_STAGES = Object.freeze(["READY_PATHOLOGIST", "UNDER_REVIEW"]);

/**
 * FR-12.2 and FR-10, in order: who is reading the case, found by typing part
 * of a name; the slides received, with a note naming any whose label was
 * never checked; the pathologist's review notes, kept with the case and not
 * printed; and the further work the pathologist has asked the bench for, as
 * a table of requests each raised and changed on its own (see RequestsTable).
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
 * the pathologist field and the notes go read-only with the rest of the form.
 * Clearing the pathologist box changes nothing: the server keeps the stored
 * pathologist, so an empty choice is not sent as one.
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
  const neverChecked = READING_STAGES.includes(caseInfo.status)
    ? neverCheckedSlides(caseInfo)
    : [];

  const choosePathologist = (id) => {
    if (!id) {
      return;
    }
    const chosen = (pathologistUsers ?? []).find(
      (user) => String(user.id) === String(id),
    );
    updateCase({
      assignedPathologistId: id,
      assignedPathologist: chosen?.value ?? "",
    });
  };

  return (
    <Stack gap={6}>
      <div className="pathology-case-view__field-group">
        <SearchableSelect
          id="assignedPathologist"
          labelText={intl.formatMessage({
            id: "label.button.select.pathologist",
          })}
          options={pathologistUsers ?? []}
          value={caseInfo.assignedPathologistId}
          onChange={choosePathologist}
          disabled={readOnly || held}
        />
      </div>
      <SlidesReceived
        rows={slidesReceived(caseInfo, intl)}
        labelVerifications={caseInfo.labelVerifications}
        neverChecked={neverChecked}
      />
      <TextArea
        id="reviewNotes"
        className="pathology-case-view__review-notes"
        labelText={intl.formatMessage({ id: "pathology.label.reviewNotes" })}
        helperText={intl.formatMessage({
          id: "pathology.label.reviewNotesHelp",
        })}
        rows={4}
        value={caseInfo.reviewNotes ?? ""}
        disabled={readOnly || held}
        onChange={(e) => updateCase({ reviewNotes: e.target.value })}
      />
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
