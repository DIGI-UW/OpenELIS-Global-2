import React from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { Section, Heading, Checkbox, Grid, Column } from "@carbon/react";

const TestSelectionSection = ({ projectData, onTestChange }) => {
  const intl = useIntl();

  return (
    <Section>
      <Heading>
        <FormattedMessage
          id="sample.entry.test.selection"
          defaultMessage="Test Selection"
        />
      </Heading>

      <Grid fullWidth={true}>
        {/* Sample Types Section */}
        <Column lg={16} md={8} sm={4}>
          <h4 style={{ marginTop: "1rem", marginBottom: "1rem" }}>
            <FormattedMessage
              id="sample.entry.sample.types"
              defaultMessage="Sample Types"
            />
          </h4>
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="dryTubeTaken"
            labelText={intl.formatMessage({
              id: "sample.entry.dry.tube",
              defaultMessage: "Dry Tube",
            })}
            checked={projectData.dryTubeTaken || false}
            onChange={(e) => onTestChange("dryTubeTaken", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="edtaTubeTaken"
            labelText={intl.formatMessage({
              id: "sample.entry.edta.tube",
              defaultMessage: "EDTA Tube",
            })}
            checked={projectData.edtaTubeTaken || false}
            onChange={(e) => onTestChange("edtaTubeTaken", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="dbsTaken"
            labelText={intl.formatMessage({
              id: "sample.entry.dbs",
              defaultMessage: "DBS",
            })}
            checked={projectData.dbsTaken || false}
            onChange={(e) => onTestChange("dbsTaken", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="dbsvlTaken"
            labelText={intl.formatMessage({
              id: "sample.entry.dbs.viral.load",
              defaultMessage: "DBS Viral Load",
            })}
            checked={projectData.dbsvlTaken || false}
            onChange={(e) => onTestChange("dbsvlTaken", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="pscvlTaken"
            labelText={intl.formatMessage({
              id: "sample.entry.plasma.viral.load",
              defaultMessage: "Plasma Viral Load",
            })}
            checked={projectData.pscvlTaken || false}
            onChange={(e) => onTestChange("pscvlTaken", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="plasmaTaken"
            labelText={intl.formatMessage({
              id: "sample.entry.plasma",
              defaultMessage: "Plasma",
            })}
            checked={projectData.plasmaTaken || false}
            onChange={(e) => onTestChange("plasmaTaken", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="serumTaken"
            labelText={intl.formatMessage({
              id: "sample.entry.serum",
              defaultMessage: "Serum",
            })}
            checked={projectData.serumTaken || false}
            onChange={(e) => onTestChange("serumTaken", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="preservCytTaken"
            labelText={intl.formatMessage({
              id: "sample.entry.preserv.cyt",
              defaultMessage: "PreservCyt (HPV)",
            })}
            checked={projectData.preservCytTaken || false}
            onChange={(e) => onTestChange("preservCytTaken", e.target.checked)}
          />
        </Column>

        {/* HIV Serology Tests Section */}
        <Column lg={16} md={8} sm={4}>
          <h4 style={{ marginTop: "2rem", marginBottom: "1rem" }}>
            <FormattedMessage
              id="sample.entry.hiv.serology"
              defaultMessage="HIV Serology Tests"
            />
          </h4>
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="serologyHIVTest"
            labelText={intl.formatMessage({
              id: "sample.entry.serology.hiv",
              defaultMessage: "Serology HIV",
            })}
            checked={projectData.serologyHIVTest || false}
            onChange={(e) => onTestChange("serologyHIVTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="murexTest"
            labelText={intl.formatMessage({
              id: "sample.entry.murex",
              defaultMessage: "Murex",
            })}
            checked={projectData.murexTest || false}
            onChange={(e) => onTestChange("murexTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="integralTest"
            labelText={intl.formatMessage({
              id: "sample.entry.integral",
              defaultMessage: "Integral",
            })}
            checked={projectData.integralTest || false}
            onChange={(e) => onTestChange("integralTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="genscreenTest"
            labelText={intl.formatMessage({
              id: "sample.entry.genscreen",
              defaultMessage: "Genscreen",
            })}
            checked={projectData.genscreenTest || false}
            onChange={(e) => onTestChange("genscreenTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="genieIITest"
            labelText={intl.formatMessage({
              id: "sample.entry.genie.ii",
              defaultMessage: "Genie II",
            })}
            checked={projectData.genieIITest || false}
            onChange={(e) => onTestChange("genieIITest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="vironostikaTest"
            labelText={intl.formatMessage({
              id: "sample.entry.vironostika",
              defaultMessage: "Vironostika",
            })}
            checked={projectData.vironostikaTest || false}
            onChange={(e) => onTestChange("vironostikaTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="genieII100Test"
            labelText={intl.formatMessage({
              id: "sample.entry.genie.ii.100",
              defaultMessage: "Genie II 1/100",
            })}
            checked={projectData.genieII100Test || false}
            onChange={(e) => onTestChange("genieII100Test", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="genieII10Test"
            labelText={intl.formatMessage({
              id: "sample.entry.genie.ii.10",
              defaultMessage: "Genie II 1/10",
            })}
            checked={projectData.genieII10Test || false}
            onChange={(e) => onTestChange("genieII10Test", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="WB1Test"
            labelText={intl.formatMessage({
              id: "sample.entry.wb1",
              defaultMessage: "Western Blot 1",
            })}
            checked={projectData.WB1Test || false}
            onChange={(e) => onTestChange("WB1Test", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="WB2Test"
            labelText={intl.formatMessage({
              id: "sample.entry.wb2",
              defaultMessage: "Western Blot 2",
            })}
            checked={projectData.WB2Test || false}
            onChange={(e) => onTestChange("WB2Test", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="p24AgTest"
            labelText={intl.formatMessage({
              id: "sample.entry.p24ag",
              defaultMessage: "P24 Ag",
            })}
            checked={projectData.p24AgTest || false}
            onChange={(e) => onTestChange("p24AgTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="innoliaTest"
            labelText={intl.formatMessage({
              id: "sample.entry.innolia",
              defaultMessage: "Innolia",
            })}
            checked={projectData.innoliaTest || false}
            onChange={(e) => onTestChange("innoliaTest", e.target.checked)}
          />
        </Column>

        {/* Molecular Tests Section */}
        <Column lg={16} md={8} sm={4}>
          <h4 style={{ marginTop: "2rem", marginBottom: "1rem" }}>
            <FormattedMessage
              id="sample.entry.molecular.tests"
              defaultMessage="Molecular Tests"
            />
          </h4>
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="cd4cd8Test"
            labelText={intl.formatMessage({
              id: "sample.entry.cd4.cd8",
              defaultMessage: "CD4/CD8",
            })}
            checked={projectData.cd4cd8Test || false}
            onChange={(e) => onTestChange("cd4cd8Test", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="cd4CountTest"
            labelText={intl.formatMessage({
              id: "sample.entry.cd4.count",
              defaultMessage: "CD4 Count",
            })}
            checked={projectData.cd4CountTest || false}
            onChange={(e) => onTestChange("cd4CountTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="cd3CountTest"
            labelText={intl.formatMessage({
              id: "sample.entry.cd3.count",
              defaultMessage: "CD3 Count",
            })}
            checked={projectData.cd3CountTest || false}
            onChange={(e) => onTestChange("cd3CountTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="viralLoadTest"
            labelText={intl.formatMessage({
              id: "sample.entry.viral.load",
              defaultMessage: "Viral Load",
            })}
            checked={projectData.viralLoadTest || false}
            onChange={(e) => onTestChange("viralLoadTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="genotypingTest"
            labelText={intl.formatMessage({
              id: "sample.entry.genotyping",
              defaultMessage: "Genotyping",
            })}
            checked={projectData.genotypingTest || false}
            onChange={(e) => onTestChange("genotypingTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="dnaPCR"
            labelText={intl.formatMessage({
              id: "sample.entry.dna.pcr",
              defaultMessage: "DNA PCR",
            })}
            checked={projectData.dnaPCR || false}
            onChange={(e) => onTestChange("dnaPCR", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="hpvTest"
            labelText={intl.formatMessage({
              id: "sample.entry.hpv",
              defaultMessage: "HPV Test",
            })}
            checked={projectData.hpvTest || false}
            onChange={(e) => onTestChange("hpvTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="asanteTest"
            labelText={intl.formatMessage({
              id: "sample.entry.asante",
              defaultMessage: "Asante Test",
            })}
            checked={projectData.asanteTest || false}
            onChange={(e) => onTestChange("asanteTest", e.target.checked)}
          />
        </Column>

        {/* Chemistry Tests Section */}
        <Column lg={16} md={8} sm={4}>
          <h4 style={{ marginTop: "2rem", marginBottom: "1rem" }}>
            <FormattedMessage
              id="sample.entry.chemistry.tests"
              defaultMessage="Chemistry Tests"
            />
          </h4>
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="glycemiaTest"
            labelText={intl.formatMessage({
              id: "sample.entry.glycemia",
              defaultMessage: "Glycemia",
            })}
            checked={projectData.glycemiaTest || false}
            onChange={(e) => onTestChange("glycemiaTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="creatinineTest"
            labelText={intl.formatMessage({
              id: "sample.entry.creatinine",
              defaultMessage: "Creatinine",
            })}
            checked={projectData.creatinineTest || false}
            onChange={(e) => onTestChange("creatinineTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="transaminaseTest"
            labelText={intl.formatMessage({
              id: "sample.entry.transaminase",
              defaultMessage: "Transaminase",
            })}
            checked={projectData.transaminaseTest || false}
            onChange={(e) => onTestChange("transaminaseTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="transaminaseALTLTest"
            labelText={intl.formatMessage({
              id: "sample.entry.alt",
              defaultMessage: "ALT (SGPT)",
            })}
            checked={projectData.transaminaseALTLTest || false}
            onChange={(e) =>
              onTestChange("transaminaseALTLTest", e.target.checked)
            }
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="transaminaseASTLTest"
            labelText={intl.formatMessage({
              id: "sample.entry.ast",
              defaultMessage: "AST (SGOT)",
            })}
            checked={projectData.transaminaseASTLTest || false}
            onChange={(e) =>
              onTestChange("transaminaseASTLTest", e.target.checked)
            }
          />
        </Column>

        {/* Hematology/NFS Tests Section */}
        <Column lg={16} md={8} sm={4}>
          <h4 style={{ marginTop: "2rem", marginBottom: "1rem" }}>
            <FormattedMessage
              id="sample.entry.hematology.tests"
              defaultMessage="Hematology / NFS Tests"
            />
          </h4>
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="nfsTest"
            labelText={intl.formatMessage({
              id: "sample.entry.nfs",
              defaultMessage: "NFS (CBC)",
            })}
            checked={projectData.nfsTest || false}
            onChange={(e) => onTestChange("nfsTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="gbTest"
            labelText={intl.formatMessage({
              id: "sample.entry.gb",
              defaultMessage: "GB (WBC)",
            })}
            checked={projectData.gbTest || false}
            onChange={(e) => onTestChange("gbTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="neutTest"
            labelText={intl.formatMessage({
              id: "sample.entry.neut",
              defaultMessage: "Neutrophils",
            })}
            checked={projectData.neutTest || false}
            onChange={(e) => onTestChange("neutTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="lymphTest"
            labelText={intl.formatMessage({
              id: "sample.entry.lymph",
              defaultMessage: "Lymphocytes",
            })}
            checked={projectData.lymphTest || false}
            onChange={(e) => onTestChange("lymphTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="monoTest"
            labelText={intl.formatMessage({
              id: "sample.entry.mono",
              defaultMessage: "Monocytes",
            })}
            checked={projectData.monoTest || false}
            onChange={(e) => onTestChange("monoTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="eoTest"
            labelText={intl.formatMessage({
              id: "sample.entry.eo",
              defaultMessage: "Eosinophils",
            })}
            checked={projectData.eoTest || false}
            onChange={(e) => onTestChange("eoTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="basoTest"
            labelText={intl.formatMessage({
              id: "sample.entry.baso",
              defaultMessage: "Basophils",
            })}
            checked={projectData.basoTest || false}
            onChange={(e) => onTestChange("basoTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="grTest"
            labelText={intl.formatMessage({
              id: "sample.entry.gr",
              defaultMessage: "GR (RBC)",
            })}
            checked={projectData.grTest || false}
            onChange={(e) => onTestChange("grTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="hbTest"
            labelText={intl.formatMessage({
              id: "sample.entry.hb",
              defaultMessage: "Hemoglobin",
            })}
            checked={projectData.hbTest || false}
            onChange={(e) => onTestChange("hbTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="hctTest"
            labelText={intl.formatMessage({
              id: "sample.entry.hct",
              defaultMessage: "Hematocrit",
            })}
            checked={projectData.hctTest || false}
            onChange={(e) => onTestChange("hctTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="vgmTest"
            labelText={intl.formatMessage({
              id: "sample.entry.vgm",
              defaultMessage: "VGM (MCV)",
            })}
            checked={projectData.vgmTest || false}
            onChange={(e) => onTestChange("vgmTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="tcmhTest"
            labelText={intl.formatMessage({
              id: "sample.entry.tcmh",
              defaultMessage: "TCMH (MCH)",
            })}
            checked={projectData.tcmhTest || false}
            onChange={(e) => onTestChange("tcmhTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="ccmhTest"
            labelText={intl.formatMessage({
              id: "sample.entry.ccmh",
              defaultMessage: "CCMH (MCHC)",
            })}
            checked={projectData.ccmhTest || false}
            onChange={(e) => onTestChange("ccmhTest", e.target.checked)}
          />
        </Column>

        <Column lg={4} md={2} sm={2}>
          <Checkbox
            id="plqTest"
            labelText={intl.formatMessage({
              id: "sample.entry.plq",
              defaultMessage: "Platelets",
            })}
            checked={projectData.plqTest || false}
            onChange={(e) => onTestChange("plqTest", e.target.checked)}
          />
        </Column>
      </Grid>
    </Section>
  );
};

export default TestSelectionSection;
