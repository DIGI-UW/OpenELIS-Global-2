import React, { useState } from "react";
import {
  Button,
  Heading,
  Section,
  Select,
  SelectItem,
  Stack,
  TextArea,
  TextInput,
} from "@carbon/react";
import { Subtract } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../config.json";
import "../pathologyCaseView.scss";

/**
 * FR-3: what the bench did with the specimen at grossing — the macroscopic
 * description, the blocks cut from it, and who worked it.
 *
 * The description comes first and is the tallest thing in the section,
 * because it is the work: the blocks record what the description was cut
 * into, and the technician selector is an attribution the case mostly fills
 * in by itself. Laid out the other way round, as the flat form had it, the
 * description read as a footnote to a dropdown.
 *
 * The gross description is available to everyone who can open the case. The
 * bench writes it while the specimen is in front of them (FR-3.1) and the
 * pathologist completes the same field at reading (FR-12.3), so gating it on
 * the pathologist role, as the flat form did, meant the person actually
 * holding the specimen could not record what they saw.
 */
const GrossingSection = ({
  caseInfo,
  updateCase,
  readOnly,
  technicianUsers,
}) => {
  const intl = useIntl();
  const [blocksToAdd, setBlocksToAdd] = useState(1);

  const blocks = caseInfo.blocks ?? [];

  // Every edit replaces the row rather than writing through to the object the
  // case is still holding, so the case state is only ever changed by the one
  // update helper and a rejected save leaves nothing half-applied.
  const patchBlock = (index, patch) =>
    updateCase((prev) => ({
      blocks: (prev.blocks ?? []).map((block, position) =>
        position === index ? { ...block, ...patch } : block,
      ),
    }));

  const removeBlock = (index) =>
    updateCase((prev) => ({
      blocks: (prev.blocks ?? []).filter((_, position) => position !== index),
    }));

  const addBlocks = () => {
    const highest = blocks.reduce(
      (max, block) => Math.ceil(Math.max(max, block.blockNumber || 0)),
      0,
    );
    const added = Array.from({ length: blocksToAdd }, (_, index) => ({
      id: "",
      blockNumber: highest + 1 + index,
    }));
    updateCase((prev) => ({ blocks: [...(prev.blocks ?? []), ...added] }));
  };

  return (
    <Stack gap={6}>
      <TextArea
        id="grossExam"
        disabled={readOnly}
        rows={6}
        labelText={intl.formatMessage({ id: "pathology.label.grossexam" })}
        value={caseInfo.grossExam}
        onChange={(e) => updateCase({ grossExam: e.target.value })}
      />
      <div>
        <Section>
          <Heading className="pathology-case-view__heading">
            <FormattedMessage id="pathology.label.blocks" />
          </Heading>
        </Section>
        {blocks.map((block, index) => (
          <div className="pathology-case-view__row" key={index}>
            <div className="pathology-case-view__row-field">
              <TextInput
                id={"blockNumber" + index}
                disabled={readOnly}
                labelText={intl.formatMessage({
                  id: "pathology.label.block.number",
                })}
                value={block.blockNumber}
                type="number"
                onChange={(e) =>
                  patchBlock(index, { blockNumber: e.target.value })
                }
              />
            </div>
            <div className="pathology-case-view__row-field">
              <TextInput
                id={"blockLocation" + index}
                disabled={readOnly}
                labelText={intl.formatMessage({
                  id: "pathology.label.location",
                })}
                value={block.location}
                onChange={(e) =>
                  patchBlock(index, { location: e.target.value })
                }
              />
            </div>
            <div className="pathology-case-view__row-actions">
              <Button
                kind="tertiary"
                size="md"
                disabled={readOnly}
                onClick={() =>
                  window.open(
                    config.serverBaseUrl +
                      "/LabelMakerServlet?labelType=block&code=" +
                      block.blockNumber,
                    "_blank",
                  )
                }
              >
                <FormattedMessage id="pathology.label.printlabel" />
              </Button>
              {/* A Carbon IconButton renders its children as the icon, so the
                  word forced in beside the glyph was laid over it and clipped
                  by the button's square box. A ghost button takes both, and
                  its label is text rather than a tooltip. */}
              <Button
                kind="ghost"
                size="md"
                renderIcon={Subtract}
                disabled={readOnly}
                onClick={() => removeBlock(index)}
              >
                <FormattedMessage id="label.button.remove.block" />
              </Button>
            </div>
          </div>
        ))}
        <div className="pathology-case-view__add-row">
          <div className="pathology-case-view__add-count">
            <TextInput
              id="blocksToAdd"
              disabled={readOnly}
              labelText={intl.formatMessage({
                id: "pathology.label.block.add.number",
              })}
              value={blocksToAdd}
              type="number"
              onChange={(e) => setBlocksToAdd(e.target.value)}
            />
          </div>
          <Button size="md" disabled={readOnly} onClick={addBlocks}>
            <FormattedMessage id="pathology.label.addblock" />
          </Button>
        </div>
      </div>
      <div className="pathology-case-view__field-group">
        <Select
          id="assignedTechnician"
          name="assignedTechnician"
          disabled={readOnly}
          labelText={intl.formatMessage({
            id: "label.button.select.technician",
          })}
          value={caseInfo.assignedTechnicianId}
          onChange={(event) =>
            updateCase({ assignedTechnicianId: event.target.value })
          }
        >
          <SelectItem
            value=""
            text={intl.formatMessage({ id: "common.select" })}
          />
          {technicianUsers.map((user, index) => (
            <SelectItem key={index} text={user.value} value={user.id} />
          ))}
        </Select>
      </div>
    </Stack>
  );
};

export default GrossingSection;
