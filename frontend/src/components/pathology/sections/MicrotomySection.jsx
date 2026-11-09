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
import { Add, ChevronDown, ChevronUp, Launch } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../config.json";
import StatusBadge from "../../caseView/StatusBadge";
import IdentifiedRow, { ListToolbar, RowIdentity } from "../IdentifiedRow";
import {
  blockIdentifier,
  cassetteStateBadge,
  countedRows,
  deactivatedCount,
  heldOrServedItems,
  labelStreamUrl,
  microtomyGroups,
  rowListEditors,
  unsavedPosition,
} from "../pathologyRows";
import { addSlideLock, rowActionLock } from "../pathologySections";
import { focusFirstOf } from "../focus";
import "../pathologyCaseView.scss";

// The column a slide's level is stored in.
const LEVEL_MAX_LENGTH = 16;

const LEGACY_GROUP_KEY = "legacy";

const groupKey = (group) =>
  group.legacy ? LEGACY_GROUP_KEY : String(group.block.id);

/**
 * FR-7: the case's blocks, each expandable to the slides cut from it. A slide
 * is added under the block it is cut from, so every slide names its block
 * without a picker; slides recorded before slides named their block are
 * listed together last. Each slide records its level and stain (FR-7.2).
 * With the stage switched off the slides on record are shown read-only.
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

  const slides = caseInfo.slides ?? [];
  const blocks = caseInfo.blocks ?? [];
  const groups = microtomyGroups(blocks, slides, showDeactivated);

  const { patchRow, removeRow, addRow } = rowListEditors("slides", updateCase);
  // The stains are a dictionary category the laboratory keeps, so an empty
  // one is a configuration gap the bench is told about. Until the list has
  // arrived there is nothing to pick and nothing yet to say about it.
  const stainsUnknown = !Array.isArray(stains);
  const noStains = !stainsUnknown && stains.length === 0;

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

  const openImage = (slide) => {
    const win = window.open();
    win.document.write(
      '<iframe src="' +
        slide.fileType +
        ";base64," +
        slide.image +
        '" frameborder="0" style="border:0; top:0px; left:0px; bottom:0px; right:0px; width:100%; height:100%;" allowfullscreen></iframe>',
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
        actions={({ disabled }) => (
          <>
            {/* The server leaves a deactivated slide untouched, so an image
                attached to one would be dropped without a word. */}
            <FileUploader
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
              <Button
                kind="tertiary"
                size="md"
                renderIcon={Launch}
                onClick={() => openImage(slide)}
              >
                <FormattedMessage id="pathology.label.view" />
              </Button>
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
            className="case-view__locked-hint pathology-case-view__group-hint"
          >
            {hintText}
          </span>
        )}
      </div>
    );
  };

  return (
    <Stack gap={6}>
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
