import { useIntl, FormattedMessage } from "react-intl";
import { Select, SelectItem, Tag, Tooltip } from "@carbon/react";

/**
 * A clinical sample's container type (OGC-1611 FR-B24, B26, G8). The choices
 * are the active containers yielding the sample's type; a saved sample keeps
 * a container retired since. A proposed sample carries a Proposed tag naming
 * the tests it was proposed for, and a sample the user changed before saving
 * an Edited tag. The container is required when the catalog names containers
 * for a test on the sample.
 */
const ContainerTypeField = ({ sample, sampleIndex, isReadOnly, onChange }) => {
  const intl = useIntl();
  const choices = sample.containerChoices || [];
  const value = sample.containerTypeId ? String(sample.containerTypeId) : "";
  const listed = choices.some((choice) => String(choice.id) === value);
  const options =
    value && !listed
      ? [
          ...choices,
          {
            id: value,
            name: sample.containerTypeName
              ? intl.formatMessage(
                  { id: "collect.sample.container.retired" },
                  { name: sample.containerTypeName },
                )
              : value,
          },
        ]
      : choices;
  const selected = options.find((choice) => String(choice.id) === value);
  const missing = !!sample.containerRequired && !value;
  const unsaved = !sample.sampleItemId;
  const proposedFor = (sample.proposedFor || []).join(", ");

  return (
    <div
      className="sample-container-field"
      data-testid={`container-field-${sampleIndex}`}
    >
      <Select
        id={`containerType-${sampleIndex}`}
        labelText={
          intl.formatMessage({ id: "collect.sample.container" }) +
          (sample.containerRequired ? " *" : "")
        }
        value={value}
        onChange={(event) => onChange(event.target.value)}
        disabled={isReadOnly}
        invalid={missing}
        invalidText={intl.formatMessage({
          id: "collect.sample.container.required",
        })}
      >
        <SelectItem
          value=""
          text={intl.formatMessage({
            id: "collect.sample.container.placeholder",
          })}
        />
        {options.map((choice) => (
          <SelectItem
            key={choice.id}
            value={String(choice.id)}
            text={choice.name}
          />
        ))}
      </Select>
      <div className="sample-container-tags">
        {selected?.capColourHex && (
          <span
            className="containerTypeSwatch"
            data-testid={`container-swatch-${sampleIndex}`}
            style={{ backgroundColor: selected.capColourHex }}
            aria-hidden="true"
          />
        )}
        {unsaved && sample.proposed && !sample.containerEdited && (
          <Tooltip
            label={intl.formatMessage(
              { id: "collect.sample.container.proposedFor" },
              { tests: proposedFor },
            )}
            align="bottom"
          >
            <span
              tabIndex={0}
              className="sample-container-proposed"
              data-testid={`container-proposed-${sampleIndex}`}
              aria-label={intl.formatMessage(
                { id: "collect.sample.container.proposedFor" },
                { tests: proposedFor },
              )}
            >
              <Tag type="purple" size="sm">
                <FormattedMessage id="collect.sample.container.proposed" />
              </Tag>
            </span>
          </Tooltip>
        )}
        {unsaved &&
          sample.proposed &&
          !sample.containerEdited &&
          sample.proposedCount > 1 && (
            <Tag
              type="cool-gray"
              size="sm"
              data-testid={`container-draw-${sampleIndex}`}
            >
              <FormattedMessage
                id="collect.sample.container.draw"
                values={{ count: sample.proposedCount }}
              />
            </Tag>
          )}
        {unsaved && sample.containerEdited && (
          <Tag
            type="warm-gray"
            size="sm"
            data-testid={`container-edited-${sampleIndex}`}
          >
            <FormattedMessage id="collect.sample.container.edited" />
          </Tag>
        )}
      </div>
    </div>
  );
};

export default ContainerTypeField;
