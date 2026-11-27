import React from "react";
import { InlineNotification, Stack } from "@carbon/react";
import { useIntl } from "react-intl";
import SlideStatusTag from "../SlideStatusTag";
import SlideWorkTable, {
  sectionHoldId,
  SlideStamp,
  SlideWorkButton,
} from "../SlideWorkTable";
import { nextAfterAll } from "../pathologyRows";
import {
  batchHold,
  COVERSLIPPING_SECTION_ID,
  sectionHeader,
  slideWorkLock,
  stageEntries,
} from "../pathologySections";
import RunReferenceField from "./RunReferenceField";
import StageEventList from "./StageEventList";
import "../pathologyCaseView.scss";

const COVERSLIPPING_HEADER = sectionHeader(COVERSLIPPING_SECTION_ID);

const qcPassId = (slide) => "qcPass-" + slide.id;
const qcFailId = (slide) => "qcFail-" + slide.id;

/**
 * FR-8.4: the coverslipping batch the case went through, and each slide's
 * coverslip checked and passed or failed, one at a time, or passed as a batch
 * of selected slides. A failed slide holds the case until it is coverslipped
 * again and passes, or is recut. The batch is written once on the case's
 * entry into coverslipping, as the processor run is on its entry into
 * processing.
 *
 * `readOnly` is narrower here than in the sibling sections: it is the
 * section's own read-only state, without the move or mark in flight that
 * theirs also covers, since a read-only table drops its selection and
 * buttons, and focus with them (see the case view). What is in flight comes
 * in two props instead: `held` is a move or a mark still out, `saving` is a
 * save. `held` holds the batch field; either holds every button, without
 * taking the table off the page. Where the laboratory does not track
 * staining no slide is ever marked stained, so the table has no stain status
 * column and a pass asks for none.
 */
const CoverslippingSection = ({
  caseInfo,
  updateCase,
  readOnly,
  held,
  untracked = false,
  dirty,
  saving,
  slideWork,
  onSlideWork,
}) => {
  const intl = useIntl();
  const blocks = caseInfo.blocks ?? [];
  const slides = caseInfo.slides ?? [];
  const { stainingTracked } = slideWork;
  const summary = slideWork.COVERSLIPPING;
  // A batch is written once per entry, so after a return and a new entry
  // the field is empty again; the earlier batches stay on the list below it.
  // The list is named by its batches: the slide table below already takes
  // the stage's name, and the field above the batch's own.
  const coverslipEvents = stageEntries(caseInfo, "COVERSLIPPING");

  const lockFor = (action, slide) =>
    slideWorkLock({
      status: caseInfo.status,
      dirty,
      saving,
      moving: held,
      action,
      slide,
      stainingTracked,
    });

  const record = (result, worked, startedBy) => {
    const next = nextAfterAll(
      summary.outstanding.map((entry) => entry.slide),
      worked,
    );
    return onSlideWork({
      value: result,
      slides: worked,
      startedBy,
      focusTargets: next
        ? ["#" + qcPassId(next), COVERSLIPPING_HEADER]
        : COVERSLIPPING_HEADER,
      refusalFocusTargets: ["#" + startedBy.id, COVERSLIPPING_HEADER],
    });
  };

  const columns = [
    {
      key: "stain",
      header: intl.formatMessage({ id: "pathology.label.stain" }),
      render: (slide) => caseInfo.stainNames?.[slide.stainId] ?? "",
    },
    ...(stainingTracked
      ? [
          {
            key: "stainStatus",
            header: intl.formatMessage({ id: "pathology.label.stainStatus" }),
            render: (slide) => (
              <SlideStatusTag
                kind="stain"
                slide={slide}
                caseStatus={caseInfo.status}
              />
            ),
          },
        ]
      : []),
    {
      key: "qc",
      header: intl.formatMessage({ id: "pathology.label.qc" }),
      // Who checked the coverslip, and when, reads under the result, as the
      // staining table puts who stained a slide under its status.
      render: (slide) => (
        <div className="pathology-case-view__slide-status">
          <SlideStatusTag
            kind="qc"
            slide={slide}
            caseStatus={caseInfo.status}
          />
          <SlideStamp stamp={caseInfo.coverslipQcStamps?.[String(slide.id)]} />
        </div>
      ),
    },
  ];

  const rowActions = (slide, { name }) => (
    <>
      {slide.coverslipQc !== "PASS" && (
        <SlideWorkButton
          id={qcPassId(slide)}
          labelKey="pathology.action.qcPass"
          nameKey="pathology.action.qcPassOf"
          name={name}
          lock={lockFor("pass", slide)}
          sectionHoldId={sectionHoldId("coverslipping")}
          onClick={(event) => record("PASS", [slide], event.currentTarget)}
        />
      )}
      {slide.coverslipQc !== "FAIL" && (
        <SlideWorkButton
          id={qcFailId(slide)}
          labelKey="pathology.action.qcFail"
          nameKey="pathology.action.qcFailOf"
          name={name}
          lock={lockFor("fail", slide)}
          sectionHoldId={sectionHoldId("coverslipping")}
          onClick={(event) => record("FAIL", [slide], event.currentTarget)}
        />
      )}
    </>
  );

  return (
    <Stack gap={6}>
      {caseInfo.status === "COVERSLIPPING" &&
        summary.outstanding.length > 0 && (
          <InlineNotification
            kind="warning"
            lowContrast
            hideCloseButton
            title={intl.formatMessage(
              { id: "pathology.banner.slidesToQc" },
              { count: summary.outstanding.length },
            )}
            subtitle={intl.formatMessage(
              { id: "pathology.banner.slidesToQcDetail" },
              {
                designations: summary.names.join(", "),
              },
            )}
          />
        )}
      <RunReferenceField
        id="coverslipRunReference"
        stage="COVERSLIPPING"
        caseInfo={caseInfo}
        value={caseInfo.coverslipRunReference}
        recordable={caseInfo.coverslipRunReferenceEditable}
        readOnly={readOnly}
        held={held}
        labelKey="pathology.label.coverslipBatch"
        onceKey="pathology.label.coverslipBatchOnce"
        missingKey="pathology.locked.noCoverslipStageEvent"
        onChange={(coverslipRunReference) =>
          updateCase({ coverslipRunReference })
        }
      />
      {coverslipEvents.length > 0 && (
        <StageEventList
          events={coverslipEvents}
          ariaLabelKey="pathology.label.coverslipBatches"
          showStage={false}
          runReferenceKey="pathology.label.coverslipBatch"
        />
      )}
      <SlideWorkTable
        tableId="coverslipping"
        stage="COVERSLIPPING"
        slides={slides}
        blocks={blocks}
        caseStatus={caseInfo.status}
        readOnly={readOnly}
        untracked={untracked}
        stainingTracked={stainingTracked}
        lock={lockFor("pass")}
        columns={columns}
        rowActions={rowActions}
        batch={{
          labelKey: "pathology.action.passSelected",
          confirmKey: "pathology.label.confirmQcBatch",
          hold: (selected) =>
            batchHold(blocks, selected, "pass", stainingTracked),
          onConfirm: (selected, button) => record("PASS", selected, button),
        }}
      />
    </Stack>
  );
};

export default CoverslippingSection;
