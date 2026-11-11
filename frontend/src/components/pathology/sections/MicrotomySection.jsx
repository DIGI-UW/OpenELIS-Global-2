import ClinicalAttachmentPreview from "../../common/ClinicalAttachment/ClinicalAttachmentPreview";
import React, { useEffect, useRef, useState } from "react";
import {
  Button,
  ComboBox,
  FileUploader,
  Heading,
  InlineNotification,
  Section,
  Stack,
  TextInput,
} from "@carbon/react";
import { Add, ChevronDown, ChevronUp } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../config.json";
import StatusBadge from "../../caseView/StatusBadge";
import IdentifiedRow, { ListToolbar, RowIdentity } from "../IdentifiedRow";
import LabelVerificationTag from "../LabelVerificationTag";
import {
  blockIdentifier,
  cassetteStateBadge,
  countedRows,
  deactivatedCount,
  heldOrServedItems,
  labelStreamUrl,
  microtomyGroups,
  nextUnverifiedSlide,
  rowListEditors,
  slideOnBlockName,
  unsavedPosition,
  unverifiedSlides,
} from "../pathologyRows";
import {
  addSlideLock,
  MICROTOMY_SECTION_ID,
  rowActionLock,
  scanVerificationRequired,
  sectionHeader,
  verifyLabelLock,
} from "../pathologySections";
import { focusFirstOf } from "../focus";
import "../pathologyCaseView.scss";

// The column a slide's level is stored in.
const LEVEL_MAX_LENGTH = 16;

const LEGACY_GROUP_KEY = "legacy";

const groupKey = (group) =>
  group.legacy ? LEGACY_GROUP_KEY : String(group.block.id);

const EMPTY_SCAN = Object.freeze({ block: "", slide: "" });

// The control that opens a slide's label check, and the two fields in it,
// which the keyboard and the answers move focus between.
const verifyLabelButtonId = (slideId) => "verifyLabelButton-" + slideId;
const verifyBlockId = (slideId) => "verifyLabel-" + slideId;
const scanBlockId = (slideId) => "scanBlock-" + slideId;
const scanSlideId = (slideId) => "scanSlide-" + slideId;
const verifyHintId = (slideId) => "verifyLabelHint-" + slideId;

const isEnter = (event) => event.key === "Enter";

// A scanner types into whatever field holds focus, so a field that takes
// focus selects what it holds and the next read replaces it.
const selectOnFocus = (event) => event.target.select();

// Where focus lands once no slide label is left to check.
const MICROTOMY_SECTION_HEADER = sectionHeader(MICROTOMY_SECTION_ID);

/**
 * FR-7: the case's blocks, each expandable to the slides cut from it. A slide
 * is added under the block it is cut from, so every slide names its block
 * without a picker; slides recorded before slides named their block are
 * listed together last. Each slide records its level and stain (FR-7.2),
 * and once saved and printed its label is checked against its block
 * (FR-7.3 to FR-7.5). With the stage switched off the slides on record are
 * shown read-only.
 */
const MicrotomySection = ({
  caseInfo,
  updateCase,
  readOnly,
  untracked = false,
  stains,
  configurationProperties,
  onSlideFile,
  dirty,
  saving,
  moving,
  onDeactivate,
  onVerifyLabel,
}) => {
  const intl = useIntl();
  const deactivateLock = rowActionLock({
    status: caseInfo.status,
    dirty,
    saving,
    moving,
    saveFirstKey: "pathology.locked.saveBeforeDeactivate",
  });
  const [showDeactivated, setShowDeactivated] = useState(false);
  // The groups the person opened or closed themselves; any other group is
  // open exactly when it holds a slide.
  const [expanded, setExpanded] = useState({});
  // The slide just added and the Add slide that added it, until the new row
  // is on the page and its level field can take focus.
  const pendingFocus = useRef(null);
  // The slides whose label check is open, what has been scanned or typed into
  // each, and the mismatch the server last answered for each, by slide id.
  const [verifyOpen, setVerifyOpen] = useState({});
  const [scans, setScans] = useState({});
  const [mismatches, setMismatches] = useState({});
  // The slides whose check is still unanswered. This section is the one
  // place a check in flight is kept, since the request, the fields and the
  // focus that follows it are all the row's own; the case view only posts.
  // The ref is the guard, so a scanner's Enter arriving twice cannot post
  // twice; the state renders the row's buttons held meanwhile.
  const verifyingRef = useRef(new Set());
  const [verifying, setVerifying] = useState({});
  const scanMode = scanVerificationRequired(configurationProperties);

  const slides = caseInfo.slides ?? [];
  const blocks = caseInfo.blocks ?? [];
  const groups = microtomyGroups(blocks, slides, showDeactivated);

  const { patchRow, removeRow, addRow } = rowListEditors("slides", updateCase);
  // The stains are a dictionary category the laboratory keeps, so an empty
  // one is a configuration gap the bench is told about. Until the list has
  // arrived there is nothing to pick and nothing yet to say about it.
  const stainsUnknown = !Array.isArray(stains);
  const noStains = !stainsUnknown && stains.length === 0;

  const verifyLockOf = (slide) =>
    verifyLabelLock({
      slide,
      labelVerifications: caseInfo.labelVerifications,
      status: caseInfo.status,
      readOnly,
      untracked,
      saving,
      moving,
      verifying: Boolean(verifying[slide.id]),
    });

  const unverified = unverifiedSlides(
    blocks,
    slides,
    caseInfo.labelVerifications,
  );

  const scanOf = (slide) => scans[slide.id] ?? EMPTY_SCAN;

  const setScan = (slide, patch) =>
    setScans((prev) => ({
      ...prev,
      [slide.id]: { ...(prev[slide.id] ?? EMPTY_SCAN), ...patch },
    }));

  const closeVerify = (slide) => {
    setVerifyOpen((prev) => ({ ...prev, [slide.id]: false }));
    setScans((prev) => ({ ...prev, [slide.id]: EMPTY_SCAN }));
    setMismatches((prev) => ({ ...prev, [slide.id]: null }));
  };

  const toggleVerify = (slide, button) => {
    if (verifyOpen[slide.id]) {
      closeVerify(slide);
      return;
    }
    setVerifyOpen((prev) => ({ ...prev, [slide.id]: true }));
    // Opened to scan into, so the block field takes the scanner's first read.
    focusFirstOf("#" + scanBlockId(slide.id), { onlyFrom: [button] });
  };

  const cancelVerify = (slide) => {
    closeVerify(slide);
    focusFirstOf("#" + verifyLabelButtonId(slide.id));
  };

  const scanComplete = (slide) => {
    const scan = scanOf(slide);
    return scan.block.trim() !== "" && scan.slide.trim() !== "";
  };

  const confirmVerify = async (slide) => {
    if (
      !scanComplete(slide) ||
      verifyingRef.current.has(slide.id) ||
      verifyLockOf(slide).disabled
    ) {
      return;
    }
    const scan = scanOf(slide);
    const next = nextUnverifiedSlide(
      blocks,
      slides,
      caseInfo.labelVerifications,
      slide,
    );
    verifyingRef.current.add(slide.id);
    setVerifying((prev) => ({ ...prev, [slide.id]: true }));
    setMismatches((prev) => ({ ...prev, [slide.id]: null }));
    const outcome = await onVerifyLabel(
      slide.id,
      scan.block.trim(),
      scan.slide.trim(),
    );
    verifyingRef.current.delete(slide.id);
    setVerifying((prev) => ({ ...prev, [slide.id]: false }));
    if (outcome?.ok) {
      closeVerify(slide);
      // On to the next label to check, away only from the check that just
      // closed: a person who has gone on to another control keeps it.
      focusFirstOf(
        next
          ? ["#" + verifyLabelButtonId(next.id), MICROTOMY_SECTION_HEADER]
          : MICROTOMY_SECTION_HEADER,
        { unlessHeld: true },
      );
    } else if (outcome?.mismatch) {
      // What was scanned stays in the fields, so the bench can see which of
      // the two was wrong, and the slide field is the one rescanned. A scanner
      // ends its read with Enter in that field, so focus never left it and no
      // focus event selects the old value: select it here, so the next read
      // replaces it instead of being appended.
      setMismatches((prev) => ({ ...prev, [slide.id]: outcome.mismatch }));
      focusFirstOf("#" + scanSlideId(slide.id));
      setTimeout(() =>
        document.getElementById(scanSlideId(slide.id))?.select(),
      );
    }
  };

  const isOpen = (group) =>
    expanded[groupKey(group)] ?? group.slides.length > 0;

  const setOpen = (group, open) =>
    setExpanded((prev) => ({ ...prev, [groupKey(group)]: open }));

  const addSlideTo = (group, button) => {
    const clientKey = addRow({
      blockId: group.block.id,
      level: "",
      stainId: null,
      location: "",
    });
    pendingFocus.current = { clientKey, from: button };
    setOpen(group, true);
  };

  // The new row's level is where the person types next, so focus goes there
  // from the Add slide that was pressed, never from a field someone is in.
  useEffect(() => {
    const pending = pendingFocus.current;
    if (!pending) {
      return;
    }
    const index = slides.findIndex(
      (slide) => slide.clientKey === pending.clientKey,
    );
    if (index < 0) {
      return;
    }
    pendingFocus.current = null;
    focusFirstOf("#slideLevel" + index, { onlyFrom: [pending.from] });
  }, [slides]);

  // FR-7.3 to FR-7.5: the block and the slide's printed label, scanned, or
  // typed as printed where the laboratory has no scanner, and checked by the
  // server. A scanner ends each read with Enter, which moves on from the
  // block to the slide and confirms from the slide.
  const verifyBlock = (slide, lock, hiddenName) => {
    const scan = scanOf(slide);
    const mismatch = mismatches[slide.id];
    const complete = scanComplete(slide);
    // A request in flight is said first, as on the row's other actions.
    const holdKey =
      lock.reasonKey ?? (complete ? null : "pathology.locked.scanBothFields");
    const holdText = holdKey ? intl.formatMessage({ id: holdKey }) : null;

    return (
      <div
        id={verifyBlockId(slide.id)}
        className="pathology-case-view__verify pathology-case-view__own-line"
      >
        <p className="pathology-case-view__hint">
          <FormattedMessage
            id={
              scanMode
                ? "pathology.label.scanVerificationHint"
                : "pathology.label.confirmVerificationHint"
            }
          />
        </p>
        {mismatch && (
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            role="alert"
            title={intl.formatMessage({
              id: "pathology.banner.labelMismatchTitle",
            })}
            subtitle={intl.formatMessage(
              { id: "pathology.banner.labelMismatch" },
              mismatch,
            )}
          />
        )}
        <div className="pathology-case-view__verify-fields">
          <div className="pathology-case-view__row-field">
            <TextInput
              id={scanBlockId(slide.id)}
              autoComplete="off"
              labelText={
                <>
                  {intl.formatMessage({
                    id: scanMode
                      ? "pathology.label.scanBlock"
                      : "pathology.label.block",
                  })}
                  {hiddenName}
                </>
              }
              value={scan.block}
              onFocus={selectOnFocus}
              onChange={(e) => setScan(slide, { block: e.target.value })}
              onKeyDown={(event) => {
                if (isEnter(event)) {
                  event.preventDefault();
                  document.getElementById(scanSlideId(slide.id))?.focus();
                }
              }}
            />
          </div>
          <div className="pathology-case-view__row-field">
            <TextInput
              id={scanSlideId(slide.id)}
              autoComplete="off"
              labelText={
                <>
                  {intl.formatMessage({
                    id: scanMode
                      ? "pathology.label.scanSlideLabel"
                      : "pathology.label.slide",
                  })}
                  {hiddenName}
                </>
              }
              value={scan.slide}
              onFocus={selectOnFocus}
              onChange={(e) => setScan(slide, { slide: e.target.value })}
              onKeyDown={(event) => {
                if (isEnter(event)) {
                  event.preventDefault();
                  confirmVerify(slide);
                }
              }}
            />
          </div>
          <Button
            kind="tertiary"
            size="md"
            disabled={!complete || lock.disabled}
            title={holdText ?? undefined}
            aria-describedby={holdText ? verifyHintId(slide.id) : undefined}
            onClick={() => confirmVerify(slide)}
          >
            <FormattedMessage id="pathology.action.confirmLabel" />
            {hiddenName}
          </Button>
          <Button
            kind="ghost"
            size="md"
            disabled={Boolean(verifying[slide.id])}
            onClick={() => cancelVerify(slide)}
          >
            <FormattedMessage id="common.cancel" />
            {hiddenName}
          </Button>
          {/* A disabled button takes no focus and shows its title to a mouse
              only, so the reason is also written out beside it. */}
          {holdText && (
            <span
              id={verifyHintId(slide.id)}
              className="case-view__locked-hint pathology-case-view__own-line"
            >
              {holdText}
            </span>
          )}
        </div>
      </div>
    );
  };

  const slideRow = (slide) => {
    // The row's place in the case's own list, not in its group, so every
    // edit lands on this row.
    const index = slides.indexOf(slide);
    const stainItems = heldOrServedItems(
      stains,
      slide.stainId,
      caseInfo.stainNames,
    );
    const verifyLock = verifyLockOf(slide);
    const verifyLockedReason = verifyLock.reasonKey
      ? intl.formatMessage({ id: verifyLock.reasonKey })
      : undefined;

    return (
      <IdentifiedRow
        key={slide.id ?? slide.clientKey}
        kind="slide"
        row={slide}
        position={unsavedPosition(slides, index)}
        readOnly={readOnly}
        showDeactivated={showDeactivated}
        deactivateLocked={deactivateLock.disabled}
        deactivateLockedReason={
          deactivateLock.reasonKey &&
          intl.formatMessage({ id: deactivateLock.reasonKey })
        }
        onRemove={() => removeRow(index)}
        onDeactivate={onDeactivate}
        identityTag={
          <LabelVerificationTag
            slide={slide}
            labelVerifications={caseInfo.labelVerifications}
          />
        }
        actions={({ disabled }) => (
          <>
            {verifyLock.offered && (
              <Button
                kind="ghost"
                size="md"
                id={verifyLabelButtonId(slide.id)}
                aria-label={intl.formatMessage(
                  { id: "pathology.action.verifyLabelOf" },
                  { slide: slideOnBlockName(slide, blocks) },
                )}
                aria-expanded={Boolean(verifyOpen[slide.id])}
                aria-controls={verifyBlockId(slide.id)}
                disabled={verifyLock.disabled}
                title={verifyLock.disabled ? verifyLockedReason : undefined}
                onClick={(event) => toggleVerify(slide, event.currentTarget)}
              >
                <FormattedMessage id="pathology.action.verifyLabel" />
              </Button>
            )}
            {/* The server leaves a deactivated slide untouched, so an image
                attached to one would be dropped without a word. */}
            <p
              className="pathology-case-view__hint pathology-case-view__own-line pathology-case-view__upload-hint"
              aria-hidden="true"
            >
              <FormattedMessage id="clinicalAttachment.uploadHint" />
            </p>
            <FileUploader
              className="pathology-case-view__slide-upload"
              labelDescription={intl.formatMessage({
                id: "clinicalAttachment.uploadHint",
              })}
              buttonLabel={intl.formatMessage({
                id: "label.button.uploadfile",
              })}
              iconDescription={intl.formatMessage({
                id: "label.button.uploadfile",
              })}
              multiple={false}
              accept={["image/jpeg", "image/png", "application/pdf"]}
              disabled={disabled}
              name=""
              buttonKind="tertiary"
              size="md"
              filenameStatus="edit"
              onChange={(e) => {
                e.preventDefault();
                onSlideFile(index, e.target.files[0]);
              }}
              onClick={function noRefCheck() {}}
              onDelete={(e) => {
                e.preventDefault();
              }}
            />
            {slide.image && (
              <ClinicalAttachmentPreview
                kind="tertiary"
                size="md"
                attachment={slide}
              />
            )}
          </>
        )}
      >
        {({ hiddenName, disabled }) => (
          <>
            <div className="pathology-case-view__row-field">
              <TextInput
                id={"slideLevel" + index}
                disabled={disabled}
                maxLength={LEVEL_MAX_LENGTH}
                labelText={
                  <>
                    {intl.formatMessage({ id: "pathology.label.level" })}
                    {hiddenName}
                  </>
                }
                value={slide.level ?? ""}
                onChange={(e) => patchRow(index, { level: e.target.value })}
              />
            </div>
            <div className="pathology-case-view__row-field">
              <ComboBox
                id={"slideStain" + index}
                size="md"
                titleText={
                  <>
                    {intl.formatMessage({ id: "pathology.label.stain" })}
                    {hiddenName}
                  </>
                }
                placeholder={intl.formatMessage({ id: "common.select" })}
                items={stainItems}
                itemToString={(item) => item?.value ?? ""}
                selectedItem={
                  stainItems.find(
                    (stain) => String(stain.id) === String(slide.stainId),
                  ) ?? null
                }
                onChange={({ selectedItem }) =>
                  patchRow(index, {
                    stainId: selectedItem ? selectedItem.id : null,
                  })
                }
                disabled={disabled || stainsUnknown || noStains}
                helperText={
                  // A row locked for its own reasons says nothing about the
                  // list, which is not what keeps it locked.
                  noStains && !disabled
                    ? intl.formatMessage({ id: "pathology.empty.noStains" })
                    : undefined
                }
              />
            </div>
            <div className="pathology-case-view__row-field">
              <TextInput
                id={"slideLocation" + index}
                disabled={disabled}
                labelText={
                  <>
                    {intl.formatMessage({ id: "pathology.label.location" })}
                    {hiddenName}
                  </>
                }
                value={slide.location ?? ""}
                onChange={(e) => patchRow(index, { location: e.target.value })}
              />
            </div>
            {verifyLock.offered &&
              verifyOpen[slide.id] &&
              verifyBlock(slide, verifyLock, hiddenName)}
          </>
        )}
      </IdentifiedRow>
    );
  };

  const groupHeader = (group, open, bodyId) => {
    const inUse = countedRows(group.slides).length;
    const block = group.block;
    const designation = block ? blockIdentifier(block) : null;
    const expandName = group.legacy
      ? intl.formatMessage({ id: "pathology.label.slidesWithoutBlock" })
      : intl.formatMessage({ id: "pathology.label.slidesOf" }, { designation });
    const lock = group.legacy
      ? null
      : addSlideLock({
          status: caseInfo.status,
          block,
          configurationProperties,
        });
    // A read-only section says why on its own badge, so only a lock of the
    // block's own is spelt out here.
    const hintText =
      lock && !group.retired && !readOnly && lock.reasonKey
        ? intl.formatMessage({ id: lock.reasonKey })
        : null;
    const hintId = "slideGroupHint-" + groupKey(group);

    return (
      <div className="pathology-case-view__group-header">
        <Button
          kind="ghost"
          size="sm"
          hasIconOnly
          renderIcon={open ? ChevronUp : ChevronDown}
          iconDescription={expandName}
          aria-expanded={open}
          aria-controls={bodyId}
          onClick={() => setOpen(group, !open)}
        />
        {group.legacy ? (
          <span className="pathology-case-view__group-title">
            <FormattedMessage id="pathology.label.slidesWithoutBlock" />
          </span>
        ) : (
          <>
            <RowIdentity
              identifier={designation}
              barcode={block.barcode}
              stateBadge={cassetteStateBadge(block)}
              deactivated={group.retired}
            />
            <span className="pathology-case-view__group-meta">
              <span className="cds--visually-hidden">
                {intl.formatMessage({ id: "pathology.label.tissueType" }) + " "}
              </span>
              {caseInfo.tissueTypeNames?.[block.tissueTypeId] ?? (
                <FormattedMessage id="caseView.label.notRecorded" />
              )}
            </span>
          </>
        )}
        {inUse > 0 && (
          <StatusBadge
            kind="inProgress"
            textKey="pathology.badge.slideCount"
            values={{ count: inUse }}
          />
        )}
        {/* Only a block with nothing listed under it has had none cut; one
            showing retired slides has, and the legacy group is no block. */}
        {!group.legacy && group.slides.length === 0 && (
          <span className="pathology-case-view__group-meta">
            <FormattedMessage id="pathology.empty.noSlidesUnderBlock" />
          </span>
        )}
        {/* A retired block takes no new slide, so it offers none. */}
        {lock && !group.retired && (
          <Button
            kind="tertiary"
            size="sm"
            renderIcon={Add}
            className="pathology-case-view__group-add"
            aria-label={intl.formatMessage(
              { id: "pathology.action.addSlideTo" },
              { designation },
            )}
            disabled={readOnly || lock.disabled}
            title={hintText ?? undefined}
            aria-describedby={hintText ? hintId : undefined}
            onClick={(event) => addSlideTo(group, event.currentTarget)}
          >
            <FormattedMessage id="pathology.action.addSlide" />
          </Button>
        )}
        {/* A disabled button takes no focus and shows its title to a mouse
            only, so the reason is also written out under it. */}
        {hintText && (
          <span
            id={hintId}
            className="case-view__locked-hint pathology-case-view__own-line pathology-case-view__group-hint"
          >
            {hintText}
          </span>
        )}
      </div>
    );
  };

  return (
    <Stack gap={6}>
      {caseInfo.status === "MICROTOMY" && unverified.length > 0 && (
        <InlineNotification
          kind="warning"
          lowContrast
          hideCloseButton
          title={intl.formatMessage(
            { id: "pathology.banner.slidesUnverified" },
            { count: unverified.length },
          )}
          subtitle={intl.formatMessage(
            { id: "pathology.banner.slidesUnverifiedDetail" },
            {
              designations: unverified.map((entry) => entry.name).join(", "),
            },
          )}
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
      {groups.length === 0 && (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "pathology.empty.noCassettes",
          })}
        />
      )}
      <div>
        <Section>
          <Heading className="pathology-case-view__heading">
            <FormattedMessage id="pathology.label.slides" />
          </Heading>
        </Section>
        <p className="pathology-case-view__lead">
          <FormattedMessage id="pathology.label.blockByBlock" />
        </p>
        {groups.map((group) => {
          const key = groupKey(group);
          const bodyId = "slideGroup-" + key;
          const open = isOpen(group);
          return (
            <div key={key} className="pathology-case-view__group">
              {groupHeader(group, open, bodyId)}
              {/* Kept on the page while closed, so its slides stay part of
                  the record and their controls keep their state. */}
              <div
                id={bodyId}
                className="pathology-case-view__group-body"
                hidden={!open}
              >
                {group.slides.map(slideRow)}
              </div>
            </div>
          );
        })}
        <ListToolbar
          printLabelKey="pathology.action.printSlideLabels"
          printUrl={labelStreamUrl(
            config.serverBaseUrl,
            "slide",
            caseInfo.labNumber,
          )}
          canPrint={countedRows(slides).length > 0}
          // A retired block with no slide in use is listed only on request
          // too, so the switch is there for it as well.
          deactivatedCount={deactivatedCount(slides) + deactivatedCount(blocks)}
          showDeactivated={showDeactivated}
          onToggleDeactivated={setShowDeactivated}
          toggleId="showDeactivatedSlides"
        />
      </div>
    </Stack>
  );
};

export default MicrotomySection;
