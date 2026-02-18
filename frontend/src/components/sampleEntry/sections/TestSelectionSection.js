import React from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { Section, Heading, Checkbox, Grid, Column } from "@carbon/react";

const TestSelectionSection = ({
  projectData,
  onTestChange,
  selectedProject,
}) => {
  const intl = useIntl();

  const resolveChecked = (event, data) => {
    // Carbon v11 signature: (event, { checked, id })
    if (data && typeof data.checked === "boolean") {
      return data.checked;
    }
    // Carbon v10 signature: (checked, id, event) - first arg is boolean
    if (typeof event === "boolean") {
      return event;
    }
    // Fallback: read from event.target.checked
    if (event?.target && typeof event.target.checked === "boolean") {
      return event.target.checked;
    }
    // Default to false
    return false;
  };

  const setIfNotSelected = (field) => {
    if (!projectData?.[field]) {
      onTestChange(field, true);
    }
  };

  const ensureAnySelected = (fields, fallbackField) => {
    const hasAny = fields.some((field) => projectData?.[field]);
    if (!hasAny && fallbackField) {
      setIfNotSelected(fallbackField);
    }
  };

  const autoSelectSpecimen = (field, checked) => {
    if (!checked) {
      return;
    }

    const specimenFields = [
      "dryTubeTaken",
      "edtaTubeTaken",
      "dbsTaken",
      "dbsvlTaken",
      "pscvlTaken",
      "plasmaTaken",
      "serumTaken",
      "preservCytTaken",
      "abbottOrRocheAnalysis",
      "geneXpertAnalysis",
    ];

    if (specimenFields.includes(field)) {
      return;
    }

    const dryTubeTests = [
      "serologyHIVTest",
      "glycemiaTest",
      "creatinineTest",
      "transaminaseTest",
      "transaminaseALTLTest",
      "transaminaseASTLTest",
      "murexTest",
      "integralTest",
      "genscreenTest",
      "genieIITest",
      "vironostikaTest",
      "genieII100Test",
      "genieII10Test",
      "wb1Test",
      "wb2Test",
      "p24AgTest",
      "innoliaTest",
    ];

    const edtaTests = [
      "nfsTest",
      "gbTest",
      "neutTest",
      "lymphTest",
      "monoTest",
      "eoTest",
      "basoTest",
      "grTest",
      "hbTest",
      "hctTest",
      "vgmTest",
      "tcmhTest",
      "ccmhTest",
      "plqTest",
      "cd4cd8Test",
      "cd3CountTest",
      "cd4CountTest",
      "viralLoadTest",
      "genotypingTest",
    ];

    if (isARVProject || isSpecialRequestProject) {
      if (dryTubeTests.includes(field)) {
        setIfNotSelected("dryTubeTaken");
      }
      if (edtaTests.includes(field)) {
        setIfNotSelected("edtaTubeTaken");
      }
      if (field === "dnaPCR") {
        setIfNotSelected("dbsTaken");
      }
    }

    if (isRTNProject || isIndeterminateProject) {
      if (field === "serologyHIVTest") {
        setIfNotSelected("dryTubeTaken");
      }
    }

    if (isEIDProject) {
      if (field === "dnaPCR") {
        setIfNotSelected("dbsTaken");
      }
    }

    if (
      selectedProject?.includes("VL") ||
      selectedProject === "ARV_VIRAL_LOAD"
    ) {
      if (field === "viralLoadTest") {
        ensureAnySelected(
          ["edtaTubeTaken", "dbsvlTaken", "pscvlTaken"],
          "edtaTubeTaken",
        );
      }
    }

    if (isRecencyProject) {
      if (field === "asanteTest") {
        ensureAnySelected(["plasmaTaken", "serumTaken"], "plasmaTaken");
      }
    }

    if (isHPVProject) {
      if (field === "hpvTest") {
        setIfNotSelected("preservCytTaken");
        ensureAnySelected(
          ["abbottOrRocheAnalysis", "geneXpertAnalysis"],
          "abbottOrRocheAnalysis",
        );
      }
    }
  };

  const onCheckboxChange = (field) => (event, data) => {
    const checked = resolveChecked(event, data);
    onTestChange(field, checked);
    autoSelectSpecimen(field, checked);
  };

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
                onChange={onCheckboxChange("dryTubeTaken")}
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
                onChange={onCheckboxChange("edtaTubeTaken")}
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
                onChange={onCheckboxChange("serologyHIVTest")}
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
                onChange={onCheckboxChange("glycemiaTest")}
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
                onChange={onCheckboxChange("creatinineTest")}
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
                onChange={onCheckboxChange("transaminaseTest")}
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
                onChange={onCheckboxChange("nfsTest")}
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
                onChange={onCheckboxChange("cd4cd8Test")}
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
                onChange={onCheckboxChange("viralLoadTest")}
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
                onChange={onCheckboxChange("genotypingTest")}
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
                onChange={onCheckboxChange("dryTubeTaken")}
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
                onChange={onCheckboxChange("dbsTaken")}
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
                onChange={onCheckboxChange("dnaPCR")}
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
                onChange={onCheckboxChange("dryTubeTaken")}
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
                onChange={onCheckboxChange("serologyHIVTest")}
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
                onChange={onCheckboxChange("dryTubeTaken")}
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
                onChange={onCheckboxChange("serologyHIVTest")}
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
                onChange={onCheckboxChange("dryTubeTaken")}
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
                onChange={onCheckboxChange("edtaTubeTaken")}
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
                onChange={onCheckboxChange("dbsTaken")}
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
                onChange={onCheckboxChange("murexTest")}
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
                onChange={onCheckboxChange("genscreenTest")}
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
                onChange={onCheckboxChange("vironostikaTest")}
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
                onChange={onCheckboxChange("innoliaTest")}
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
                onChange={onCheckboxChange("glycemiaTest")}
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
                onChange={onCheckboxChange("creatinineTest")}
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
                onChange={onCheckboxChange("transaminaseTest")}
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
                onChange={onCheckboxChange("transaminaseALTLTest")}
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
                onChange={onCheckboxChange("transaminaseASTLTest")}
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
                onChange={onCheckboxChange("nfsTest")}
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
                onChange={onCheckboxChange("gbTest")}
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
                onChange={onCheckboxChange("lymphTest")}
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
                onChange={onCheckboxChange("monoTest")}
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
                onChange={onCheckboxChange("eoTest")}
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
                onChange={onCheckboxChange("basoTest")}
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
                onChange={onCheckboxChange("grTest")}
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
                onChange={onCheckboxChange("hbTest")}
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
                onChange={onCheckboxChange("hctTest")}
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
                onChange={onCheckboxChange("vgmTest")}
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
                onChange={onCheckboxChange("tcmhTest")}
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
                onChange={onCheckboxChange("ccmhTest")}
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
                onChange={onCheckboxChange("plqTest")}
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
                onChange={onCheckboxChange("cd4cd8Test")}
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
                onChange={onCheckboxChange("cd3CountTest")}
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
                onChange={onCheckboxChange("cd4CountTest")}
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
                onChange={onCheckboxChange("dnaPCR")}
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
                onChange={onCheckboxChange("viralLoadTest")}
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
                onChange={onCheckboxChange("genotypingTest")}
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
                onChange={onCheckboxChange("preservCytTaken")}
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
                onChange={onCheckboxChange("hpvTest")}
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
                onChange={onCheckboxChange("abbottOrRocheAnalysis")}
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
                onChange={onCheckboxChange("geneXpertAnalysis")}
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
                onChange={onCheckboxChange("plasmaTaken")}
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
                onChange={onCheckboxChange("serumTaken")}
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
                onChange={onCheckboxChange("asanteTest")}
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
                onChange={onCheckboxChange("edtaTubeTaken")}
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
                onChange={onCheckboxChange("dbsvlTaken")}
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
                onChange={onCheckboxChange("pscvlTaken")}
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
                onChange={onCheckboxChange("viralLoadTest")}
              />
            </Column>
          </>
        )}
      </Grid>
    </Section>
  );
};

export default TestSelectionSection;
