import React, { useState } from "react";
import {
  Button,
  ComboBox,
  FileUploader,
  Heading,
  Section,
  TextInput,
} from "@carbon/react";
import { Launch } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../config.json";
import StatusBadge from "../../caseView/StatusBadge";
import IdentifiedRow, { ListToolbar } from "../IdentifiedRow";
import {
  blockIdentifier,
  countedRows,
  deactivatedCount,
  isDeactivatedRow,
  isUnsavedRow,
  labelStreamUrl,
  needsParentBlock,
  parentBlockOf,
  rowListEditors,
  unsavedPosition,
} from "../pathologyRows";
import "../pathologyCaseView.scss";

/**
 * FR-7: the slides cut from the case's blocks, each naming its parent block.
 * A new slide names its block from the saved cassettes in use; the server
 * refuses one without.
 */
const MicrotomySection = ({
  caseInfo,
  updateCase,
  readOnly,
  onSlideFile,
  dirty,
  saving,
  saveRefusedForBlock = false,
  onDeactivate,
}) => {
  const intl = useIntl();
  const [showDeactivated, setShowDeactivated] = useState(false);

  const slides = caseInfo.slides ?? [];
  const blocks = caseInfo.blocks ?? [];
  const blocksToCutFrom = countedRows(blocks);
  const noBlockToCutFrom = blocksToCutFrom.length === 0;

  const { patchRow, removeRow, addRow } = rowListEditors("slides", updateCase);

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

  const hiddenName = (objectName) => (
    <span className="cds--visually-hidden">{" " + objectName}</span>
  );

  const parentField = (slide, index, objectName, disabled) => {
    const parent = parentBlockOf(slide, blocks);

    if (isUnsavedRow(slide)) {
      const invalid = saveRefusedForBlock && needsParentBlock(slide);
      return (
        <div className="pathology-case-view__row-field">
          <ComboBox
            id={"slideBlock" + index}
            size="md"
            titleText={
              <>
                {intl.formatMessage({ id: "pathology.label.parentBlock" })}
                {hiddenName(objectName)}
              </>
            }
            placeholder={intl.formatMessage({ id: "common.select" })}
            items={blocksToCutFrom}
            itemToString={(block) => (block ? blockIdentifier(block) : "")}
            selectedItem={parent ?? null}
            onChange={({ selectedItem }) =>
              patchRow(index, { blockId: selectedItem?.id ?? null })
            }
            invalid={invalid}
            aria-invalid={invalid || undefined}
            invalidText={intl.formatMessage({
              id: "pathology.locked.slideNeedsBlock",
            })}
            disabled={disabled}
          />
        </div>
      );
    }

    return (
      <div className="pathology-case-view__row-meta">
        <span className="pathology-case-view__row-meta-label">
          <FormattedMessage id="pathology.label.parentBlock" />
        </span>
        <span>
          {parent ? (
            blockIdentifier(parent)
          ) : (
            <FormattedMessage id="caseView.label.notRecorded" />
          )}
        </span>
        {isDeactivatedRow(parent) && (
          <StatusBadge kind="none" textKey="caseView.badge.deactivated" />
        )}
      </div>
    );
  };

  return (
    <div>
      <Section>
        <Heading className="pathology-case-view__heading">
          <FormattedMessage id="pathology.label.slides" />
        </Heading>
      </Section>
      {slides.map((slide, index) => (
        <IdentifiedRow
          key={slide.id ?? slide.clientKey}
          kind="slide"
          row={slide}
          position={unsavedPosition(slides, index)}
          readOnly={readOnly}
          showDeactivated={showDeactivated}
          deactivateLocked={dirty || saving}
          deactivateLockedReason={intl.formatMessage({
            id: "pathology.locked.saveBeforeDeactivate",
          })}
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
          {({ objectName, disabled }) => (
            <>
              {parentField(slide, index, objectName, disabled)}
              <div className="pathology-case-view__row-field">
                <TextInput
                  id={"slideLocation" + index}
                  disabled={disabled}
                  labelText={
                    <>
                      {intl.formatMessage({ id: "pathology.label.location" })}
                      {hiddenName(objectName)}
                    </>
                  }
                  value={slide.location ?? ""}
                  onChange={(e) =>
                    patchRow(index, { location: e.target.value })
                  }
                />
              </div>
            </>
          )}
        </IdentifiedRow>
      ))}
      <ListToolbar
        addLabelKey="pathology.action.addSlide"
        onAdd={() => addRow({ blockId: null, location: "" })}
        addDisabled={readOnly || noBlockToCutFrom}
        addTitle={
          !readOnly && noBlockToCutFrom
            ? intl.formatMessage({
                id: "pathology.locked.slideNeedsSavedBlock",
              })
            : undefined
        }
        printLabelKey="pathology.action.printSlideLabels"
        printUrl={labelStreamUrl(
          config.serverBaseUrl,
          "slide",
          caseInfo.labNumber,
        )}
        canPrint={countedRows(slides).length > 0}
        deactivatedCount={deactivatedCount(slides)}
        showDeactivated={showDeactivated}
        onToggleDeactivated={setShowDeactivated}
        toggleId="showDeactivatedSlides"
      />
    </div>
  );
};

export default MicrotomySection;
