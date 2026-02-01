import React from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { Section, Heading, Checkbox, Grid, Column } from "@carbon/react";

const TestSelectionSection = ({
  projectData,
  onTestChange,
  selectedProject,
}) => {
  const intl = useIntl();

  // Determine which test set to show based on project
  const isARVProject =
    selectedProject?.includes("ARV") ||
    selectedProject?.includes("Initial") ||
    selectedProject?.includes("Follow");
  const isEIDProject = selectedProject?.includes("EID");
  const isRTNProject = selectedProject?.includes("RTN");
  const isIndeterminateProject = selectedProject?.includes("Indeterminate");
  const isSpecialRequestProject = selectedProject?.includes("Special");
  const isHPVProject = selectedProject?.includes("HPV");

  return (
    <Section>
      <Heading>
        <FormattedMessage
          id="sample.entry.project.title.specimen"
          defaultMessage="Specimens Collected"
        />
      </Heading>

      <Grid fullWidth={true}>
        {/* ARV Projects: Initial ARV, Follow-up ARV */}
        {isARVProject && (
          <>
            {/* Specimen Collection */}
            <Column lg={16} md={8} sm={4}>
              <h4 style={{ marginTop: "1rem", marginBottom: "1rem" }}>
                <FormattedMessage
                  id="sample.entry.project.title.specimen"
                  defaultMessage="Specimens Collected"
                />
              </h4>
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="dryTubeTaken"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.ARV.dryTubeTaken",
                  defaultMessage: "Dry Tube Taken",
                })}
                checked={projectData.dryTubeTaken || false}
                onChange={(e) => onTestChange("dryTubeTaken", e.target.checked)}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="edtaTubeTaken"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.ARV.edtaTubeTaken",
                  defaultMessage: "EDTA Tube Taken",
                })}
                checked={projectData.edtaTubeTaken || false}
                onChange={(e) =>
                  onTestChange("edtaTubeTaken", e.target.checked)
                }
              />
            </Column>

            {/* Dry Tube Tests */}
            <Column lg={16} md={8} sm={4}>
              <h4 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                <FormattedMessage
                  id="sample.entry.project.title.dryTube"
                  defaultMessage="Dry Tube Tests"
                />
              </h4>
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="serologyHIVTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.serologyHIVTest",
                  defaultMessage: "Serology HIV Test",
                })}
                checked={projectData.serologyHIVTest || false}
                onChange={(e) =>
                  onTestChange("serologyHIVTest", e.target.checked)
                }
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="glycemiaTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.ARV.glycemiaTest",
                  defaultMessage: "Glycemia Test",
                })}
                checked={projectData.glycemiaTest || false}
                onChange={(e) => onTestChange("glycemiaTest", e.target.checked)}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="creatinineTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.ARV.creatinineTest",
                  defaultMessage: "Creatinine Test",
                })}
                checked={projectData.creatinineTest || false}
                onChange={(e) =>
                  onTestChange("creatinineTest", e.target.checked)
                }
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="transaminaseTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.ARV.transaminaseTest",
                  defaultMessage: "Transaminase Test",
                })}
                checked={projectData.transaminaseTest || false}
                onChange={(e) =>
                  onTestChange("transaminaseTest", e.target.checked)
                }
              />
            </Column>

            {/* EDTA Tube Tests */}
            <Column lg={16} md={8} sm={4}>
              <h4 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                <FormattedMessage
                  id="sample.entry.project.title.edtaTube"
                  defaultMessage="EDTA Tube Tests"
                />
              </h4>
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="nfsTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.ARV.nfsTest",
                  defaultMessage: "NFS Test",
                })}
                checked={projectData.nfsTest || false}
                onChange={(e) => onTestChange("nfsTest", e.target.checked)}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="cd4cd8Test"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.ARV.cd4cd8Test",
                  defaultMessage: "CD4/CD8 Test",
                })}
                checked={projectData.cd4cd8Test || false}
                onChange={(e) => onTestChange("cd4cd8Test", e.target.checked)}
              />
            </Column>

            {/* Other Tests */}
            <Column lg={16} md={8} sm={4}>
              <h4 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                <FormattedMessage
                  id="sample.entry.project.title.otherTests"
                  defaultMessage="Other Tests"
                />
              </h4>
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="viralLoadTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.ARV.viralLoadTest",
                  defaultMessage: "Viral Load Test",
                })}
                checked={projectData.viralLoadTest || false}
                onChange={(e) =>
                  onTestChange("viralLoadTest", e.target.checked)
                }
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="genotypingTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.ARV.genotypingTest",
                  defaultMessage: "Genotyping Test",
                })}
                checked={projectData.genotypingTest || false}
                onChange={(e) =>
                  onTestChange("genotypingTest", e.target.checked)
                }
              />
            </Column>
          </>
        )}

        {/* EID Project */}
        {isEIDProject && (
          <>
            {/* Specimen Collection */}
            <Column lg={16} md={8} sm={4}>
              <h4 style={{ marginTop: "1rem", marginBottom: "1rem" }}>
                <FormattedMessage
                  id="sample.entry.project.title.specimen"
                  defaultMessage="Specimens Collected"
                />
              </h4>
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="dryTubeTaken"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.ARV.dryTubeTaken",
                  defaultMessage: "Dry Tube Taken",
                })}
                checked={projectData.dryTubeTaken || false}
                onChange={(e) => onTestChange("dryTubeTaken", e.target.checked)}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="dbsTaken"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.title.dryBloodSpot",
                  defaultMessage: "DBS (Dry Blood Spot)",
                })}
                checked={projectData.dbsTaken || false}
                onChange={(e) => onTestChange("dbsTaken", e.target.checked)}
              />
            </Column>

            {/* Tests */}
            <Column lg={16} md={8} sm={4}>
              <h4 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                <FormattedMessage
                  id="sample.entry.project.title.tests"
                  defaultMessage="Tests"
                />
              </h4>
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="dnaPCR"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.dnaPCR",
                  defaultMessage: "DNA PCR",
                })}
                checked={projectData.dnaPCR || false}
                onChange={(e) => onTestChange("dnaPCR", e.target.checked)}
              />
            </Column>
          </>
        )}

        {/* RTN, Indeterminate, Special Request - Simple test selection */}
        {(isRTNProject ||
          isIndeterminateProject ||
          isSpecialRequestProject) && (
          <>
            <Column lg={16} md={8} sm={4}>
              <h4 style={{ marginTop: "1rem", marginBottom: "1rem" }}>
                <FormattedMessage
                  id="sample.entry.project.title.specimen"
                  defaultMessage="Specimens Collected"
                />
              </h4>
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="dryTubeTaken"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.ARV.dryTubeTaken",
                  defaultMessage: "Dry Tube Taken",
                })}
                checked={projectData.dryTubeTaken || false}
                onChange={(e) => onTestChange("dryTubeTaken", e.target.checked)}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="edtaTubeTaken"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.ARV.edtaTubeTaken",
                  defaultMessage: "EDTA Tube Taken",
                })}
                checked={projectData.edtaTubeTaken || false}
                onChange={(e) =>
                  onTestChange("edtaTubeTaken", e.target.checked)
                }
              />
            </Column>
          </>
        )}

        {/* HPV Project */}
        {isHPVProject && (
          <>
            <Column lg={16} md={8} sm={4}>
              <h4 style={{ marginTop: "1rem", marginBottom: "1rem" }}>
                <FormattedMessage
                  id="sample.entry.project.title.specimen"
                  defaultMessage="Specimens Collected"
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
                onChange={(e) =>
                  onTestChange("preservCytTaken", e.target.checked)
                }
              />
            </Column>

            <Column lg={16} md={8} sm={4}>
              <h4 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                <FormattedMessage
                  id="sample.entry.project.title.tests"
                  defaultMessage="Tests"
                />
              </h4>
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="hpvTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.hpv.test",
                  defaultMessage: "HPV Test",
                })}
                checked={projectData.hpvTest || false}
                onChange={(e) => onTestChange("hpvTest", e.target.checked)}
              />
            </Column>
          </>
        )}
      </Grid>
    </Section>
  );
};

export default TestSelectionSection;
