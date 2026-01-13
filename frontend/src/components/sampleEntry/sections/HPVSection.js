import React from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Section,
  Heading,
  RadioButtonGroup,
  RadioButton,
  Checkbox,
  Grid,
  Column,
} from "@carbon/react";

const HPVSection = ({ projectData, onInputChange }) => {
  const intl = useIntl();

  return (
    <Section>
      <Heading>
        <FormattedMessage
          id="sample.entry.hpv.section"
          defaultMessage="HPV (Human Papillomavirus) Testing Information"
        />
      </Heading>

      <Grid fullWidth={true}>
        {/* Collection Method */}
        <Column lg={16} md={8} sm={4}>
          <RadioButtonGroup
            legendText={intl.formatMessage({
              id: "sample.entry.hpv.collection.method",
              defaultMessage: "Sample Collection Method",
            })}
            name="hpv-collection-method"
            valueSelected={
              projectData.selfCollection
                ? "self"
                : projectData.collectionDoneByHealthWorker
                  ? "health_worker"
                  : ""
            }
            onChange={(value) => {
              if (value === "self") {
                onInputChange("selfCollection", true);
                onInputChange("collectionDoneByHealthWorker", false);
              } else if (value === "health_worker") {
                onInputChange("selfCollection", false);
                onInputChange("collectionDoneByHealthWorker", true);
              }
            }}
            orientation="vertical"
          >
            <RadioButton
              id="selfCollection"
              labelText={intl.formatMessage({
                id: "sample.entry.hpv.self.collection",
                defaultMessage: "Self Collection",
              })}
              value="self"
            />
            <RadioButton
              id="collectionDoneByHealthWorker"
              labelText={intl.formatMessage({
                id: "sample.entry.hpv.health.worker.collection",
                defaultMessage: "Collection Done by Health Worker",
              })}
              value="health_worker"
            />
          </RadioButtonGroup>
        </Column>

        {/* Analysis Type */}
        <Column lg={16} md={8} sm={4}>
          <h4 style={{ marginTop: "1rem", marginBottom: "1rem" }}>
            <FormattedMessage
              id="sample.entry.hpv.analysis.type"
              defaultMessage="Analysis Type"
            />
          </h4>
        </Column>

        <Column lg={8} md={4} sm={4}>
          <Checkbox
            id="abbottOrRocheAnalysis"
            labelText={intl.formatMessage({
              id: "sample.entry.hpv.abbott.roche",
              defaultMessage: "Abbott/Roche Analysis",
            })}
            checked={projectData.abbottOrRocheAnalysis || false}
            onChange={(e) =>
              onInputChange("abbottOrRocheAnalysis", e.target.checked)
            }
          />
        </Column>

        <Column lg={8} md={4} sm={4}>
          <Checkbox
            id="geneXpertAnalysis"
            labelText={intl.formatMessage({
              id: "sample.entry.hpv.genexpert",
              defaultMessage: "GeneXpert Analysis",
            })}
            checked={projectData.geneXpertAnalysis || false}
            onChange={(e) =>
              onInputChange("geneXpertAnalysis", e.target.checked)
            }
          />
        </Column>

        {/* PreservCyt Sample */}
        <Column lg={16} md={8} sm={4}>
          <h4 style={{ marginTop: "1rem", marginBottom: "1rem" }}>
            <FormattedMessage
              id="sample.entry.hpv.sample.type"
              defaultMessage="Sample Type"
            />
          </h4>
        </Column>

        <Column lg={8} md={4} sm={4}>
          <Checkbox
            id="preservCytTaken"
            labelText={intl.formatMessage({
              id: "sample.entry.hpv.preserv.cyt",
              defaultMessage: "PreservCyt Sample Taken",
            })}
            checked={projectData.preservCytTaken || false}
            onChange={(e) => onInputChange("preservCytTaken", e.target.checked)}
          />
        </Column>
      </Grid>
    </Section>
  );
};

export default HPVSection;
