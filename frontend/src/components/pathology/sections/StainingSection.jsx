import React from "react";
import { InlineNotification, Stack } from "@carbon/react";
import { useIntl } from "react-intl";
import SlideStatusTag from "../SlideStatusTag";
import SlideWorkTable, {
  sectionHoldId,
  SlideStamp,
  SlideWorkButton,
} from "../SlideWorkTable";
import StainPicker from "../StainPicker";
import { nextAfterAll, rowListEditors } from "../pathologyRows";
import {
  batchHold,
  sectionHeader,
  slideWorkLock,
  STAINING_SECTION_ID,
} from "../pathologySections";
import "../pathologyCaseView.scss";

const STAINING_HEADER = sectionHeader(STAINING_SECTION_ID);

const markStainedId = (slide) => "markStained-" + slide.id;
const markUnsatisfactoryId = (slide) => "markUnsatisfactory-" + slide.id;

/**
 * FR-8.1 to FR-8.3: each slide of the case is stained with the stain on its
 * row and marked stained, one at a time or as a batch of selected slides, or
 * marked unsatisfactory, which holds the case for a restain or a recut. The
 * stain on a stained slide is the one that was applied, so it can be changed
 * again only once the slide is marked unsatisfactory.
 *
 * `readOnly` is narrower here than in the sibling sections: it is the
 * section's own read-only state, without the move or mark in flight that
 * theirs also covers, since a read-only table drops its selection and
 * buttons, and focus with them (see the case view). What is in flight comes
 * in two props instead: `held` is a move or a mark still out, `saving` is a
 * save. Either holds every button, and the stain picker, without taking the
 * table off the page: a stain picked while a save is out would be read back
 * over by the save's own answer.
 */
const StainingSection = ({
  caseInfo,
  updateCase,
  readOnly,
  held,
  untracked = false,
  stains,
  dirty,
  saving,
  slideWork,
  onSlideWork,
}) => {
  const intl = useIntl();
  const blocks = caseInfo.blocks ?? [];
  const slides = caseInfo.slides ?? [];
  const summary = slideWork.STAINING;
  const { patchRow } = rowListEditors("slides", updateCase);

  const lockFor = (action, slide) =>
    slideWorkLock({
      status: caseInfo.status,
      dirty,
      saving,
      moving: held,
      action,
      slide,
    });

  // After the case is read again, focus goes on to the next slide still to
  // stain, else to the section's header, never away from a control the
  // person moved to meanwhile. A refused mark puts focus back where it
  // started.
  const mark = (status, worked, startedBy) => {
    const next = nextAfterAll(
      summary.outstanding.map((entry) => entry.slide),
      worked,
    );
    return onSlideWork({
      value: status,
      slides: worked,
      startedBy,
      focusTargets: next
        ? ["#" + markStainedId(next), STAINING_HEADER]
        : STAINING_HEADER,
      refusalFocusTargets: ["#" + startedBy.id, STAINING_HEADER],
    });
  };

  const columns = [
    {
      key: "stain",
      header: intl.formatMessage({ id: "pathology.label.stain" }),
      render: (slide, { name, index, retired }) => (
        <StainPicker
          id={"stainingStain" + index}
          size="sm"
          hideLabel
          slide={slide}
          stains={stains}
          stainNames={caseInfo.stainNames}
          labelText={intl.formatMessage(
            { id: "pathology.label.stainFor" },
            { slide: name },
          )}
          disabled={readOnly || held || saving || retired}
          onChange={(stainId) => patchRow(index, { stainId })}
        />
      ),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "common.status" }),
      // Who recorded the status, and when, reads under it rather than in a
      // column of its own, which the section has no room for beside the
      // stain picker and the buttons.
      render: (slide) => (
        <div className="pathology-case-view__slide-status">
          <SlideStatusTag
            kind="stain"
            slide={slide}
            caseStatus={caseInfo.status}
          />
          <SlideStamp stamp={caseInfo.stainStamps?.[String(slide.id)]} />
        </div>
      ),
    },
  ];

  const rowActions = (slide, { name }) => (
    <>
      {slide.stainStatus !== "STAINED" && (
        <SlideWorkButton
          id={markStainedId(slide)}
          labelKey="pathology.action.markStained"
          nameKey="pathology.action.markStainedOf"
          name={name}
          lock={lockFor("stained", slide)}
          sectionHoldId={sectionHoldId("staining")}
          onClick={(event) => mark("STAINED", [slide], event.currentTarget)}
        />
      )}
      {slide.stainStatus !== "UNSATISFACTORY" && (
        <SlideWorkButton
          id={markUnsatisfactoryId(slide)}
          labelKey="pathology.action.markUnsatisfactory"
          nameKey="pathology.action.markUnsatisfactoryOf"
          name={name}
          lock={lockFor("unsatisfactory", slide)}
          sectionHoldId={sectionHoldId("staining")}
          onClick={(event) =>
            mark("UNSATISFACTORY", [slide], event.currentTarget)
          }
        />
      )}
    </>
  );

  return (
    <Stack gap={6}>
      {caseInfo.status === "STAINING" && summary.outstanding.length > 0 && (
        <InlineNotification
          kind="warning"
          lowContrast
          hideCloseButton
          title={intl.formatMessage(
            { id: "pathology.banner.slidesToStain" },
            { count: summary.outstanding.length },
          )}
          subtitle={intl.formatMessage(
            { id: "pathology.banner.slidesToStainDetail" },
            {
              designations: summary.names.join(", "),
            },
          )}
        />
      )}
      <SlideWorkTable
        tableId="staining"
        stage="STAINING"
        slides={slides}
        blocks={blocks}
        caseStatus={caseInfo.status}
        readOnly={readOnly}
        untracked={untracked}
        stainingTracked={slideWork.stainingTracked}
        lock={lockFor("stained")}
        columns={columns}
        rowActions={rowActions}
        batch={{
          labelKey: "pathology.action.markSelectedStained",
          confirmKey: "pathology.label.confirmStainBatch",
          hold: (selected) => batchHold(blocks, selected, "stained"),
          onConfirm: (selected, button) => mark("STAINED", selected, button),
        }}
      />
    </Stack>
  );
};

export default StainingSection;
