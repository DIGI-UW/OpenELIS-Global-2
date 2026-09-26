import React, { useState } from "react";
import {
  Button,
  FileUploader,
  Heading,
  Section,
  TextInput,
} from "@carbon/react";
import { Launch, Subtract } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../config.json";
import "../pathologyCaseView.scss";

/**
 * FR-7: the slides cut from the case's blocks, each with its own number,
 * storage location, label and optional scanned image.
 */
const MicrotomySection = ({ caseInfo, updateCase, readOnly, onSlideFile }) => {
  const intl = useIntl();
  const [slidesToAdd, setSlidesToAdd] = useState(1);

  const slides = caseInfo.slides ?? [];

  const patchSlide = (index, patch) =>
    updateCase((prev) => ({
      slides: (prev.slides ?? []).map((slide, position) =>
        position === index ? { ...slide, ...patch } : slide,
      ),
    }));

  const removeSlide = (index) =>
    updateCase((prev) => ({
      slides: (prev.slides ?? []).filter((_, position) => position !== index),
    }));

  const addSlides = () => {
    const highest = slides.reduce(
      (max, slide) => Math.ceil(Math.max(max, slide.slideNumber || 0)),
      0,
    );
    const added = Array.from({ length: slidesToAdd }, (_, index) => ({
      id: "",
      slideNumber: highest + 1 + index,
    }));
    updateCase((prev) => ({ slides: [...(prev.slides ?? []), ...added] }));
  };

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

  return (
    <div>
      <Section>
        <Heading className="pathology-case-view__heading">
          <FormattedMessage id="pathology.label.slides" />
        </Heading>
      </Section>
      {slides.map((slide, index) => (
        <div className="pathology-case-view__row" key={index}>
          <div className="pathology-case-view__row-field">
            <TextInput
              id={"slideNumber" + index}
              disabled={readOnly}
              labelText={intl.formatMessage({
                id: "pathology.label.slide.number",
              })}
              value={slide.slideNumber}
              type="number"
              onChange={(e) =>
                patchSlide(index, { slideNumber: e.target.value })
              }
            />
          </div>
          <div className="pathology-case-view__row-field">
            <TextInput
              id={"slideLocation" + index}
              disabled={readOnly}
              labelText={intl.formatMessage({
                id: "pathology.label.location",
              })}
              value={slide.location}
              onChange={(e) => patchSlide(index, { location: e.target.value })}
            />
          </div>
          <div className="pathology-case-view__row-actions">
            <FileUploader
              buttonLabel={intl.formatMessage({
                id: "label.button.uploadfile",
              })}
              iconDescription={intl.formatMessage({
                id: "label.button.uploadfile",
              })}
              multiple={false}
              accept={["image/jpeg", "image/png", "application/pdf"]}
              disabled={readOnly}
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
            <Button
              kind="tertiary"
              size="md"
              disabled={readOnly}
              onClick={() =>
                window.open(
                  config.serverBaseUrl +
                    "/LabelMakerServlet?labelType=slide&code=" +
                    slide.slideNumber,
                  "_blank",
                )
              }
            >
              <FormattedMessage id="pathology.label.printlabel" />
            </Button>
            {/* See the same button in GrossingSection: an IconButton clips
                the word it was given beside its glyph. */}
            <Button
              kind="ghost"
              size="md"
              renderIcon={Subtract}
              disabled={readOnly}
              onClick={() => removeSlide(index)}
            >
              <FormattedMessage id="label.button.remove.slide" />
            </Button>
          </div>
        </div>
      ))}
      <div className="pathology-case-view__add-row">
        <div className="pathology-case-view__add-count">
          <TextInput
            id="slidesToAdd"
            disabled={readOnly}
            labelText={intl.formatMessage({
              id: "pathology.label.slide.add.number",
            })}
            value={slidesToAdd}
            type="number"
            onChange={(e) => setSlidesToAdd(e.target.value)}
          />
        </div>
        <Button size="md" disabled={readOnly} onClick={addSlides}>
          <FormattedMessage id="pathology.label.addslide" />
        </Button>
      </div>
    </div>
  );
};

export default MicrotomySection;
