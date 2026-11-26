import React, { useState } from "react";
import {
  Button,
  DataTable,
  InlineNotification,
  Stack,
  Table,
  TableBatchAction,
  TableBatchActions,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  TableSelectAll,
  TableSelectRow,
  TableToolbar,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { ListToolbar, RowIdentity } from "./IdentifiedRow";
import {
  benchStillOwes,
  deactivatedCount,
  isDeactivatedRow,
  isUnsavedRow,
  slideOnBlockName,
} from "./pathologyRows";
import { redoNotice } from "./pathologySections";
import { stageDisplayKey } from "./pathologyStages";
import { focusFirstOf } from "./focus";
import "./pathologyCaseView.scss";

// Carbon's batch bar says how many rows are selected and offers Cancel in
// English of its own; both are put in the reader's language here.
const batchBarText = (intl) => (messageId, args) => {
  if (messageId === "carbon.table.batch.cancel") {
    return intl.formatMessage({ id: "common.cancel" });
  }
  if (messageId === "carbon.table.batch.selectAll") {
    return intl.formatMessage({ id: "pathology.label.selectAllSlides" });
  }
  return intl.formatMessage(
    { id: "pathology.label.slidesSelected" },
    { count: args?.totalSelected ?? 0 },
  );
};

/** The id of the line above a table that says why its section is held. */
export const sectionHoldId = (tableId) => tableId + "Hold";

// Each column's cells carry a class of their own, so the stylesheet can give
// the slide and its stain the room and keep the status and buttons compact.
const columnClass = (key) => "pathology-case-view__slide-col--" + key;

/**
 * The slides of a case as a table the bench works through at staining
 * (`stage="STAINING"`) and coverslipping (FR-8.1, FR-8.2, FR-8.4), with the
 * notices both share: the slides that need redoing, a stage the laboratory
 * no longer tracks, and none cut yet.
 *
 * The first column always names the slide under its block ("A1.1"). Its
 * barcode is shown on hover and read out with the name, since the table
 * shares the center column with the summary and the Microtomy section above
 * already shows it; `columns` are the section's own after it, and `rowActions` the
 * buttons each row offers. Rows can be selected for one batch action, which
 * is confirmed in a row under the toolbar that names every selected slide,
 * never a count; nothing is destroyed by it, so it asks for no modal.
 * Retired slides are listed only on request, cannot be selected and offer
 * nothing.
 *
 * `readOnly` is the shape of a section that cannot be worked in: no
 * selection and no actions. A case the bench no longer owes the work (signed
 * out, or with the pathologist) offers no selection and no batch either,
 * since nothing selected could be marked until the case comes back; its row
 * buttons stay, held, so each says why. `lock` is the section's own hold,
 * from slideWorkLock with no slide: while it holds for a moment (a request
 * out, unsaved work), the selection, the toolbar and every button stay on
 * the page, disabled, so focus is never dropped, and its reason is written
 * out once above the table.
 *
 * The table scrolls sideways inside a labelled region of its own where the
 * section is narrower than its columns, so it never runs out of the section.
 */
const SlideWorkTable = ({
  tableId,
  stage,
  slides,
  blocks,
  caseStatus,
  readOnly,
  untracked = false,
  stainingTracked = true,
  lock,
  columns,
  rowActions,
  batch,
}) => {
  const intl = useIntl();
  const [showDeactivated, setShowDeactivated] = useState(false);
  // The selection the confirmation row was opened for. A different selection
  // closes it, so it can never name slides other than the ones it posts.
  const [confirming, setConfirming] = useState(null);
  // Bumped once a batch has landed and the case is read back, so the table
  // starts again with nothing selected. The batch's own controls have gone by
  // then, and the case view puts focus on the next slide to work.
  const [version, setVersion] = useState(0);

  const allSlides = slides ?? [];
  const listed = allSlides.filter(
    (slide) =>
      !isUnsavedRow(slide) && (showDeactivated || !isDeactivatedRow(slide)),
  );
  const slideById = new Map(listed.map((slide) => [String(slide.id), slide]));
  const rows = listed.map((slide) => ({
    id: String(slide.id),
    disabled: readOnly || isDeactivatedRow(slide),
  }));
  const headers = [
    {
      key: "slide",
      header: intl.formatMessage({ id: "pathology.label.slide" }),
    },
    ...columns.map((column) => ({ key: column.key, header: column.header })),
  ];

  const batchButtonId = tableId + "BatchAction";
  const confirmId = tableId + "BatchConfirm";
  const questionId = tableId + "BatchQuestion";
  const confirmButtonId = tableId + "BatchConfirmButton";
  const cancelButtonId = tableId + "BatchCancel";
  const confirmHintId = tableId + "BatchConfirmHint";
  const lockHintId = sectionHoldId(tableId);
  const regionLabel = intl.formatMessage({ id: stageDisplayKey(stage) });

  const lockText =
    lock.disabled && lock.reasonKey
      ? intl.formatMessage({ id: lock.reasonKey })
      : null;
  const redo = redoNotice(
    stage,
    blocks,
    allSlides,
    caseStatus,
    stainingTracked,
  );
  const selectable = !readOnly && benchStillOwes(caseStatus);
  const nothingCut = allSlides.every(isUnsavedRow);

  const selectionKey = (selected) =>
    selected.map((slide) => String(slide.id)).join(",");

  const openConfirm = (selected) => {
    setConfirming(selectionKey(selected));
    focusFirstOf(["#" + confirmButtonId, "#" + cancelButtonId]);
  };

  const cancelConfirm = () => {
    setConfirming(null);
    focusFirstOf("#" + batchButtonId);
  };

  // The answer arrives once the case has been read back. A batch that landed
  // closes the row and clears the selection; a refused one leaves both, so
  // the bench can see what it tried.
  const confirm = async (selected, button) => {
    const outcome = await batch.onConfirm(selected, button);
    if (outcome?.ok) {
      setConfirming(null);
      setVersion((current) => current + 1);
    }
  };

  const confirmation = (selected) => {
    const names = selected
      .map((slide) => slideOnBlockName(slide, blocks))
      .join(", ");
    const hold = batch.hold(selected);
    const holdText =
      lockText ??
      (hold ? intl.formatMessage({ id: hold.key }, hold.values) : null);

    return (
      <div
        id={confirmId}
        role="group"
        aria-labelledby={questionId}
        className="pathology-case-view__batch-confirm"
      >
        <p id={questionId} className="pathology-case-view__batch-confirm-text">
          <FormattedMessage id={batch.confirmKey} values={{ slides: names }} />
        </p>
        <Button
          kind="primary"
          size="sm"
          id={confirmButtonId}
          disabled={Boolean(holdText)}
          title={holdText ?? undefined}
          aria-describedby={
            holdText ? questionId + " " + confirmHintId : questionId
          }
          onClick={(event) => confirm(selected, event.currentTarget)}
        >
          <FormattedMessage id="common.confirm" />
        </Button>
        <Button
          kind="ghost"
          size="sm"
          id={cancelButtonId}
          onClick={cancelConfirm}
        >
          <FormattedMessage id="common.cancel" />
        </Button>
        {/* A disabled button takes no focus and shows its title to a mouse
            only, so the reason is also written out beside it. */}
        {holdText && (
          <span id={confirmHintId} className="case-view__locked-hint">
            {holdText}
          </span>
        )}
      </div>
    );
  };

  return (
    <Stack gap={6} className="pathology-case-view__slide-work">
      {redo && (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: redo.titleKey }, redo.values)}
          subtitle={
            redo.recipeKey
              ? intl.formatMessage({ id: redo.recipeKey })
              : undefined
          }
        />
      )}
      {untracked && (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "pathology.banner.stageDisabledRows",
          })}
        />
      )}
      {nothingCut ? (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "pathology.empty.noSlides" })}
        />
      ) : (
        <div className="pathology-case-view__slide-work-body">
          {/* The reason the whole section is held, written once where a
              sighted keyboard user can read it, and named by every button
              it holds. */}
          {!readOnly && lockText && (
            <p id={lockHintId} className="case-view__locked-hint">
              {lockText}
            </p>
          )}
          <DataTable key={version} rows={rows} headers={headers}>
            {({
              rows: renderedRows,
              headers: renderedHeaders,
              getBatchActionProps,
              getSelectionProps,
              getTableProps,
              getRowProps,
              selectedRows,
            }) => {
              const selected = selectedRows
                .map((row) => slideById.get(row.id))
                .filter(Boolean);
              const batchProps = getBatchActionProps();
              const batchShown = batchProps.shouldShowBatchActions;
              return (
                <TableContainer>
                  {selectable && (
                    // The toolbar only carries the batch bar, so it takes no
                    // room until a row is selected. It stays mounted, so the
                    // bar can slide in without taking focus from the row.
                    <div
                      className={
                        batchShown
                          ? "pathology-case-view__slide-toolbar"
                          : "pathology-case-view__slide-toolbar pathology-case-view__slide-toolbar--idle"
                      }
                    >
                      <TableToolbar aria-label={regionLabel}>
                        <TableBatchActions
                          {...batchProps}
                          translateWithId={batchBarText(intl)}
                        >
                          {/* Carbon hides the bar from assistive technology
                              while nothing is selected, and takes its own
                              Cancel out of the tab order then; this button
                              follows it. */}
                          <TableBatchAction
                            id={batchButtonId}
                            tabIndex={batchShown ? 0 : -1}
                            disabled={lock.disabled}
                            title={lockText ?? undefined}
                            aria-describedby={lockText ? lockHintId : undefined}
                            onClick={() => openConfirm(selected)}
                          >
                            <FormattedMessage id={batch.labelKey} />
                          </TableBatchAction>
                        </TableBatchActions>
                      </TableToolbar>
                    </div>
                  )}
                  {selectable &&
                    selected.length > 0 &&
                    confirming === selectionKey(selected) &&
                    confirmation(selected)}
                  <div
                    id={tableId + "Region"}
                    className="pathology-case-view__slide-scroll"
                    role="region"
                    aria-label={regionLabel}
                    tabIndex={0}
                  >
                    <Table
                      {...getTableProps()}
                      size="md"
                      className="pathology-case-view__slide-table"
                    >
                      <TableHead>
                        <TableRow>
                          {selectable && (
                            <TableSelectAll
                              {...getSelectionProps()}
                              id={tableId + "SelectAll"}
                              aria-label={intl.formatMessage({
                                id: "pathology.label.selectAllSlides",
                              })}
                            />
                          )}
                          {renderedHeaders.map((header) => (
                            <TableHeader
                              key={header.key}
                              className={columnClass(header.key)}
                            >
                              {header.header}
                            </TableHeader>
                          ))}
                          {!readOnly && (
                            <TableHeader className={columnClass("actions")}>
                              {intl.formatMessage({ id: "common.actions" })}
                            </TableHeader>
                          )}
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {renderedRows.map((row) => {
                          const slide = slideById.get(row.id);
                          // DataTable hands back the previous rows for one
                          // render after they change.
                          if (!slide) {
                            return null;
                          }
                          const name = slideOnBlockName(slide, blocks);
                          const retired = isDeactivatedRow(slide);
                          const context = {
                            name,
                            index: allSlides.indexOf(slide),
                            retired,
                          };
                          return (
                            <TableRow
                              {...getRowProps({ row })}
                              key={row.id}
                              data-testid={tableId + "Row-" + slide.id}
                            >
                              {selectable && (
                                <TableSelectRow
                                  {...getSelectionProps({ row })}
                                  aria-label={intl.formatMessage(
                                    { id: "pathology.label.selectSlide" },
                                    { slide: name },
                                  )}
                                />
                              )}
                              <TableCell
                                className={columnClass("slide")}
                                title={slide.barcode ?? undefined}
                              >
                                <RowIdentity
                                  identifier={name}
                                  barcode={slide.barcode}
                                  barcodeHidden
                                  deactivated={retired}
                                />
                              </TableCell>
                              {columns.map((column) => (
                                <TableCell
                                  key={column.key}
                                  className={columnClass(column.key)}
                                >
                                  {column.render(slide, context)}
                                </TableCell>
                              ))}
                              {!readOnly && (
                                <TableCell className={columnClass("actions")}>
                                  {retired ? null : (
                                    <div className="pathology-case-view__slide-actions">
                                      {rowActions(slide, context)}
                                    </div>
                                  )}
                                </TableCell>
                              )}
                            </TableRow>
                          );
                        })}
                      </TableBody>
                    </Table>
                  </div>
                </TableContainer>
              );
            }}
          </DataTable>
          <ListToolbar
            deactivatedCount={deactivatedCount(allSlides)}
            showDeactivated={showDeactivated}
            onToggleDeactivated={setShowDeactivated}
            toggleId={tableId + "ShowDeactivated"}
          />
        </div>
      )}
    </Stack>
  );
};

export default SlideWorkTable;

/**
 * A row's own action on a slide. Its accessible name starts with the words it
 * shows and then names the slide ("Mark stained: A1.1"), so a screen reader
 * moving through the column hears which slide it acts on and voice control
 * can still say what is on the button. Held, it says why in its title and in
 * the sentence it is described by: a reason of the row's own is written under
 * the button, a reason that holds the whole section is the line above the
 * table (`sectionHoldId`).
 */
export const SlideWorkButton = ({
  id,
  labelKey,
  nameKey,
  name,
  lock,
  sectionHoldId,
  onClick,
}) => {
  const intl = useIntl();
  const reason = lock.reasonKey
    ? intl.formatMessage({ id: lock.reasonKey })
    : null;
  const hintId = lock.own ? id + "-hint" : sectionHoldId;

  return (
    <>
      <Button
        kind="ghost"
        size="sm"
        id={id}
        aria-label={intl.formatMessage({ id: nameKey }, { slide: name })}
        disabled={lock.disabled}
        title={reason ?? undefined}
        aria-describedby={reason ? hintId : undefined}
        onClick={onClick}
      >
        <FormattedMessage id={labelKey} />
      </Button>
      {reason && lock.own && (
        <span id={hintId} className="case-view__locked-hint">
          {reason}
        </span>
      )}
    </>
  );
};

/**
 * Who recorded a slide's status and when, from the case read, or nothing
 * where nobody has.
 */
export const SlideStamp = ({ stamp }) => {
  const intl = useIntl();
  if (!stamp) {
    return null;
  }
  return (
    <span className="pathology-case-view__slide-stamp">
      {intl.formatMessage(
        { id: "pathology.label.stampedBy" },
        { name: stamp.by ?? "", date: stamp.at ?? "" },
      )}
    </span>
  );
};
