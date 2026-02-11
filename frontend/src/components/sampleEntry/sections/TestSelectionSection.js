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
  const isIndeterminateProject =
    selectedProject?.includes("Indeterminate") ||
    selectedProject === "INDETERMINATE";
  const isSpecialRequestProject =
    selectedProject?.includes("Special") ||
    selectedProject === "SPECIAL_REQUEST";
  const isHPVProject =
    selectedProject?.includes("HPV") || selectedProject === "HPV_TESTING";
  const isRecencyProject =
    selectedProject?.includes("Recency") ||
    selectedProject === "RECENCY_TESTING";

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

        {/* RTN Project - Only dryTubeTaken and serologyHIVTest */}
        {isRTNProject && (
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
          </>
        )}

        {/* Indeterminate Project - Only dryTubeTaken and serologyHIVTest */}
        {isIndeterminateProject && (
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
          </>
        )}

        {/* Special Request - Comprehensive test selection */}
        {isSpecialRequestProject && (
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
                id="murexTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.murexTest",
                  defaultMessage: "Murex Test",
                })}
                checked={projectData.murexTest || false}
                onChange={(e) => onTestChange("murexTest", e.target.checked)}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="genscreenTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.genscreenTest",
                  defaultMessage: "Genscreen Test",
                })}
                checked={projectData.genscreenTest || false}
                onChange={(e) =>
                  onTestChange("genscreenTest", e.target.checked)
                }
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="vironostikaTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.vironostikaTest",
                  defaultMessage: "Vironostika Test",
                })}
                checked={projectData.vironostikaTest || false}
                onChange={(e) =>
                  onTestChange("vironostikaTest", e.target.checked)
                }
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="innoliaTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.innoliaTest",
                  defaultMessage: "Innolia Test",
                })}
                checked={projectData.innoliaTest || false}
                onChange={(e) => onTestChange("innoliaTest", e.target.checked)}
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

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="transaminaseALTLTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.transaminaseALTLTest",
                  defaultMessage: "Transaminase ALTL Test",
                })}
                checked={projectData.transaminaseALTLTest || false}
                onChange={(e) =>
                  onTestChange("transaminaseALTLTest", e.target.checked)
                }
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="transaminaseASTLTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.transaminaseASTLTest",
                  defaultMessage: "Transaminase ASTL Test",
                })}
                checked={projectData.transaminaseASTLTest || false}
                onChange={(e) =>
                  onTestChange("transaminaseASTLTest", e.target.checked)
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
                id="gbTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.gbTest",
                  defaultMessage: "GB Test",
                })}
                checked={projectData.gbTest || false}
                onChange={(e) => onTestChange("gbTest", e.target.checked)}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="lymphTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.lymphTest",
                  defaultMessage: "Lymph Test",
                })}
                checked={projectData.lymphTest || false}
                onChange={(e) => onTestChange("lymphTest", e.target.checked)}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="monoTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.monoTest",
                  defaultMessage: "Mono Test",
                })}
                checked={projectData.monoTest || false}
                onChange={(e) => onTestChange("monoTest", e.target.checked)}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="eoTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.eoTest",
                  defaultMessage: "EO Test",
                })}
                checked={projectData.eoTest || false}
                onChange={(e) => onTestChange("eoTest", e.target.checked)}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="basoTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.basoTest",
                  defaultMessage: "Baso Test",
                })}
                checked={projectData.basoTest || false}
                onChange={(e) => onTestChange("basoTest", e.target.checked)}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="grTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.grTest",
                  defaultMessage: "GR Test",
                })}
                checked={projectData.grTest || false}
                onChange={(e) => onTestChange("grTest", e.target.checked)}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="hbTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.hbTest",
                  defaultMessage: "HB Test",
                })}
                checked={projectData.hbTest || false}
                onChange={(e) => onTestChange("hbTest", e.target.checked)}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="hctTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.hctTest",
                  defaultMessage: "HCT Test",
                })}
                checked={projectData.hctTest || false}
                onChange={(e) => onTestChange("hctTest", e.target.checked)}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="vgmTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.vgmTest",
                  defaultMessage: "VGM Test",
                })}
                checked={projectData.vgmTest || false}
                onChange={(e) => onTestChange("vgmTest", e.target.checked)}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="tcmhTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.tcmhTest",
                  defaultMessage: "TCMH Test",
                })}
                checked={projectData.tcmhTest || false}
                onChange={(e) => onTestChange("tcmhTest", e.target.checked)}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="ccmhTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.ccmhTest",
                  defaultMessage: "CCMH Test",
                })}
                checked={projectData.ccmhTest || false}
                onChange={(e) => onTestChange("ccmhTest", e.target.checked)}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="plqTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.plqTest",
                  defaultMessage: "PLQ Test",
                })}
                checked={projectData.plqTest || false}
                onChange={(e) => onTestChange("plqTest", e.target.checked)}
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

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="cd3CountTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.cd3CountTest",
                  defaultMessage: "CD3 Count Test",
                })}
                checked={projectData.cd3CountTest || false}
                onChange={(e) => onTestChange("cd3CountTest", e.target.checked)}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="cd4CountTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.cd4CountTest",
                  defaultMessage: "CD4 Count Test",
                })}
                checked={projectData.cd4CountTest || false}
                onChange={(e) => onTestChange("cd4CountTest", e.target.checked)}
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
                id="dnaPCR"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.dnaPCR",
                  defaultMessage: "DNA PCR",
                })}
                checked={projectData.dnaPCR || false}
                onChange={(e) => onTestChange("dnaPCR", e.target.checked)}
              />
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
                  id: "sample.entry.project.HPV.preservCytTaken",
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
                  id: "sample.entry.project.hpv.hpvKit",
                  defaultMessage: "HPV Kit",
                })}
                checked={projectData.hpvTest || false}
                onChange={(e) => onTestChange("hpvTest", e.target.checked)}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="abbottOrRocheAnalysis"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.hpv.abottOrRocheAnalysis",
                  defaultMessage: "Abbott/Roche Analysis",
                })}
                checked={projectData.abbottOrRocheAnalysis || false}
                onChange={(e) =>
                  onTestChange("abbottOrRocheAnalysis", e.target.checked)
                }
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="geneXpertAnalysis"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.hpv.geneXpertAnalysis",
                  defaultMessage: "GeneXpert Analysis",
                })}
                checked={projectData.geneXpertAnalysis || false}
                onChange={(e) =>
                  onTestChange("geneXpertAnalysis", e.target.checked)
                }
              />
            </Column>
          </>
        )}

        {/* Recency Testing Project */}
        {isRecencyProject && (
          <>
            <Column lg={16} md={8} sm={4}>
              <h4 style={{ marginTop: "1rem", marginBottom: "1rem" }}>
                <FormattedMessage
                  id="sample.entry.project.title.sampleType"
                  defaultMessage="Sample Type"
                />
              </h4>
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="plasmaTaken"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.recency.plasma",
                  defaultMessage: "Plasma",
                })}
                checked={projectData.plasmaTaken || false}
                onChange={(e) => onTestChange("plasmaTaken", e.target.checked)}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="serumTaken"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.recency.serum",
                  defaultMessage: "Serum",
                })}
                checked={projectData.serumTaken || false}
                onChange={(e) => onTestChange("serumTaken", e.target.checked)}
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
                id="asanteTest"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.recency.asanteKit",
                  defaultMessage: "Asante Kit",
                })}
                checked={projectData.asanteTest || false}
                onChange={(e) => onTestChange("asanteTest", e.target.checked)}
              />
            </Column>
          </>
        )}

        {/* ARV - Viral Load Project */}
        {(selectedProject?.includes("VL") ||
          selectedProject === "ARV_VIRAL_LOAD") && (
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

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="dbsvlTaken"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.title.dryBloodSpot",
                  defaultMessage: "DBS (Dry Blood Spot)",
                })}
                checked={projectData.dbsvlTaken || false}
                onChange={(e) => onTestChange("dbsvlTaken", e.target.checked)}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <Checkbox
                id="pscvlTaken"
                labelText={intl.formatMessage({
                  id: "sample.entry.project.title.psc",
                  defaultMessage: "PSC",
                })}
                checked={projectData.pscvlTaken || false}
                onChange={(e) => onTestChange("pscvlTaken", e.target.checked)}
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
          </>
        )}
      </Grid>
    </Section>
  );
};

export default TestSelectionSection;
