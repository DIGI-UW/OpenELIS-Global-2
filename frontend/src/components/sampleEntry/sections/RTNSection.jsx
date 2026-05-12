import React from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Section,
  Heading,
  Select,
  SelectItem,
  TextArea,
  Grid,
  Column,
} from "@carbon/react";

const RTNSection = ({
  projectData,
  observations,
  onInputChange,
  onObservationChange,
  dictionaryLists,
}) => {
  const intl = useIntl();

  // Get YES_NO dictionary for under investigation
  const yesNoList =
    dictionaryLists && dictionaryLists["YES_NO"]
      ? dictionaryLists["YES_NO"]
      : [];

  // Check if under investigation comment should be shown
  const shouldShowInvestigationComment = () => {
    const underInvestigation = observations.underInvestigation;
    if (!underInvestigation) return false;

    // Show if "Yes" is selected (check dictionary entry)
    const yesOption = yesNoList.find(
      (item) =>
        item.id === underInvestigation &&
        (item.dictEntry === "Yes" ||
          item.dictEntry === "Oui" ||
          item.localizedName === "Yes"),
    );

    return !!yesOption;
  };

  return (
    <Section>
      <Heading>
        <FormattedMessage
          id="sample.entry.project.RTN.title"
          defaultMessage="RTN (Routine Testing Network)"
        />
      </Heading>

      <Grid fullWidth={true}>
        {/* Under Investigation */}
        <Column lg={8} md={4} sm={4}>
          <Select
            id="underInvestigation"
            labelText={intl.formatMessage({
              id: "patient.project.underInvestigation",
              defaultMessage: "Under Investigation?",
            })}
            value={observations.underInvestigation || ""}
            onChange={(e) =>
              onObservationChange("underInvestigation", e.target.value)
            }
          >
            <SelectItem text="" value="" />
            {yesNoList.map((item) => (
              <SelectItem
                key={item.id}
                text={item.localizedName || item.dictEntry}
                value={item.id}
              />
            ))}
          </Select>
        </Column>

        {/* Under Investigation Comment (Conditional) */}
        {shouldShowInvestigationComment() && (
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="underInvestigationNote"
              labelText={
                <>
                  <span className="required-field">*</span>{" "}
                  {intl.formatMessage({
                    id: "patient.project.underInvestigationComment",
                    defaultMessage: "Investigation Notes",
                  })}
                </>
              }
              value={projectData.underInvestigationNote || ""}
              onChange={(e) =>
                onInputChange("underInvestigationNote", e.target.value)
              }
              placeholder={intl.formatMessage({
                id: "patient.project.underInvestigationComment.placeholder",
                defaultMessage: "Enter investigation notes",
              })}
              rows={3}
              maxLength={1000}
            />
          </Column>
        )}
      </Grid>
    </Section>
  );
};

export default RTNSection;
