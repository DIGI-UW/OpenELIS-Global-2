import React, { useState, useEffect, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  TextInput,
  Select,
  SelectItem,
  FileUploader,
  DatePicker,
  DatePickerInput,
  TextArea,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tag,
  Tile,
  Loading,
  Modal,
  RadioButtonGroup,
  RadioButton,
  Checkbox,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";

/**
 * VaccineevelopmentPage - Specialized Stage 3 page for vaccine development workflow
 *
 * Covers 5 vaccine development stages as per PDF requirements:
 * 1. Virus Isolation - Link to culture batch
 * 2. Titer Measurement - TCID50, PFU/ml values
 * 3. Genome Sequencing - FASTA uploads, GenBank accessions
 * 4. Seed Virus Production - Selection criteria, batch tracking
 * 5. Preclinical Trials - Animal testing tracking (external)
 */
const VaccineevelopmentPage = ({
  entryId,
  pageData,
  onProgressUpdate,
  notebookId,
  notebookInstruments = [],
}) => {
  const intl = useIntl();
  const { notificationVisible, addNotification, removeNotification } =
    React.useContext(NotificationContext);

  // Main state management
  const [loading, setLoading] = useState(false);
  const [currentStage, setCurrentStage] = useState("virus_isolation");
  const [cultureBatches, setCultureBatches] = useState([]);
  const [vaccineData, setVaccineData] = useState({
    virusIsolation: {},
    titerMeasurement: {},
    genomeSequencing: {},
    seedVirusProduction: {},
    preclinicalTrials: {},
    clinicalTrials: {},
  });

  // Modal states
  const [showTiterModal, setShowTiterModal] = useState(false);
  const [showSequenceModal, setShowSequenceModal] = useState(false);
  const [showTrialModal, setShowTrialModal] = useState(false);

  // Load culture batches from Stage 2
  const loadCultureBatches = useCallback(async () => {
    try {
      setLoading(true);
      const response = await getFromOpenElisServer(
        `/rest/notebook/virology/entry/${entryId}/culture-batches`,
      );
      if (response) {
        setCultureBatches(response);
      }
    } catch (error) {
      console.error("Error loading culture batches:", error);
      addNotification({
        title: intl.formatMessage({
          id: "notification.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "vaccine.development.error.loadingBatches",
          defaultMessage: "Failed to load culture batches",
        }),
        kind: "error",
      });
    } finally {
      setLoading(false);
    }
  }, [entryId, intl, addNotification]);

  // Load existing vaccine development data
  const loadVaccineData = useCallback(async () => {
    try {
      const response = await getFromOpenElisServer(
        `/rest/notebook/virology/entry/${entryId}/vaccine-development`,
      );
      if (response) {
        setVaccineData(response);
      }
    } catch (error) {
      console.error("Error loading vaccine data:", error);
    }
  }, [entryId]);

  useEffect(() => {
    loadCultureBatches();
    loadVaccineData();
  }, [loadCultureBatches, loadVaccineData]);

  // Save vaccine development data
  const saveVaccineData = async (stageData, stageName) => {
    try {
      setLoading(true);
      const response = await postToOpenElisServer(
        `/rest/notebook/virology/entry/${entryId}/vaccine-development/${stageName}`,
        stageData,
      );

      if (response) {
        setVaccineData((prev) => ({
          ...prev,
          [stageName]: stageData,
        }));

        addNotification({
          title: intl.formatMessage({
            id: "notification.success",
            defaultMessage: "Success",
          }),
          message: intl.formatMessage({
            id: "vaccine.development.saved",
            defaultMessage: "Vaccine development data saved successfully",
          }),
          kind: "success",
        });

        // Update progress
        if (onProgressUpdate) {
          onProgressUpdate("vaccine_development", {
            completed: true,
            stage: stageName,
          });
        }
      }
    } catch (error) {
      console.error("Error saving vaccine data:", error);
      addNotification({
        title: intl.formatMessage({
          id: "notification.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "vaccine.development.error.saving",
          defaultMessage: "Failed to save vaccine development data",
        }),
        kind: "error",
      });
    } finally {
      setLoading(false);
    }
  };

  // Render virus isolation section
  const renderVirusIsolation = () => (
    <Tile className="vaccine-stage-tile">
      <h4>
        <FormattedMessage
          id="vaccine.development.virusIsolation.title"
          defaultMessage="1. Virus Isolation"
        />
      </h4>
      <p>
        <FormattedMessage
          id="vaccine.development.virusIsolation.description"
          defaultMessage="Link to culture batch ID - Document viral isolation from infected cell culture with LMIS tracking"
        />
      </p>

      <Grid>
        <Column lg={8}>
          <Select
            id="culture-batch-select"
            labelText={
              <FormattedMessage
                id="vaccine.development.cultureBatch"
                defaultMessage="Culture Batch ID *"
              />
            }
            value={vaccineData.virusIsolation.cultureBatchId || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                virusIsolation: {
                  ...prev.virusIsolation,
                  cultureBatchId: e.target.value,
                },
              }))
            }
          >
            <SelectItem text="Select culture batch..." value="" />
            {cultureBatches.map((batch) => (
              <SelectItem
                key={batch.id}
                text={`${batch.batchId} - ${batch.virusStrain} (${batch.status})`}
                value={batch.id}
              />
            ))}
          </Select>
        </Column>

        <Column lg={8}>
          <Select
            id="isolation-method"
            labelText={
              <FormattedMessage
                id="vaccine.development.isolationMethod"
                defaultMessage="Isolation Method *"
              />
            }
            value={vaccineData.virusIsolation.isolationMethod || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                virusIsolation: {
                  ...prev.virusIsolation,
                  isolationMethod: e.target.value,
                },
              }))
            }
          >
            <SelectItem text="Select isolation method..." value="" />
            <SelectItem
              text="Plaque Purification"
              value="plaque_purification"
            />
            <SelectItem text="Limiting Dilution" value="limiting_dilution" />
            <SelectItem
              text="Single Cell Sorting"
              value="single_cell_sorting"
            />
            <SelectItem text="Direct Isolation" value="direct_isolation" />
            <SelectItem text="Endpoint Dilution" value="endpoint_dilution" />
          </Select>
        </Column>

        <Column lg={8}>
          <Select
            id="cell-line"
            labelText={
              <FormattedMessage
                id="vaccine.development.cellLine"
                defaultMessage="Cell Line Used *"
              />
            }
            value={vaccineData.virusIsolation.cellLine || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                virusIsolation: {
                  ...prev.virusIsolation,
                  cellLine: e.target.value,
                },
              }))
            }
          >
            <SelectItem text="Select cell line..." value="" />
            <SelectItem text="Vero E6" value="vero_e6" />
            <SelectItem text="Vero CCL-81" value="vero_ccl81" />
            <SelectItem text="MDCK" value="mdck" />
            <SelectItem text="HEK-293" value="hek293" />
            <SelectItem text="A549" value="a549" />
            <SelectItem text="CHO-K1" value="cho_k1" />
            <SelectItem text="BHK-21" value="bhk21" />
          </Select>
        </Column>

        <Column lg={8}>
          <TextInput
            id="passage-number"
            labelText={
              <FormattedMessage
                id="vaccine.development.passageNumber"
                defaultMessage="Passage Number *"
              />
            }
            value={vaccineData.virusIsolation.passageNumber || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                virusIsolation: {
                  ...prev.virusIsolation,
                  passageNumber: e.target.value,
                },
              }))
            }
            type="number"
            step="1"
            min="0"
            helperText="Track viral passage history for strain stability"
          />
        </Column>

        <Column lg={8}>
          <DatePicker dateFormat="Y-m-d" datePickerType="single">
            <DatePickerInput
              id="isolation-date"
              labelText={
                <FormattedMessage
                  id="vaccine.development.isolationDate"
                  defaultMessage="Isolation Date *"
                />
              }
              value={vaccineData.virusIsolation.isolationDate || ""}
              onChange={(e) =>
                setVaccineData((prev) => ({
                  ...prev,
                  virusIsolation: {
                    ...prev.virusIsolation,
                    isolationDate: e.target.value,
                  },
                }))
              }
            />
          </DatePicker>
        </Column>

        <Column lg={8}>
          <TextInput
            id="initial-titer"
            labelText={
              <FormattedMessage
                id="vaccine.development.initialTiter"
                defaultMessage="Initial Titer (Log10)"
              />
            }
            value={vaccineData.virusIsolation.initialTiter || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                virusIsolation: {
                  ...prev.virusIsolation,
                  initialTiter: e.target.value,
                },
              }))
            }
            type="number"
            step="0.1"
            min="0"
            helperText="Initial viral titer at isolation (TCID50/ml or PFU/ml)"
          />
        </Column>

        <Column lg={8}>
          <Select
            id="cpe-score"
            labelText={
              <FormattedMessage
                id="vaccine.development.cpeScore"
                defaultMessage="CPE Score"
              />
            }
            value={vaccineData.virusIsolation.cpeScore || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                virusIsolation: {
                  ...prev.virusIsolation,
                  cpeScore: e.target.value,
                },
              }))
            }
          >
            <SelectItem text="Select CPE score..." value="" />
            <SelectItem text="0 (No CPE)" value="0" />
            <SelectItem text="1+ (25% CPE)" value="1" />
            <SelectItem text="2+ (50% CPE)" value="2" />
            <SelectItem text="3+ (75% CPE)" value="3" />
            <SelectItem text="4+ (100% CPE)" value="4" />
          </Select>
        </Column>

        <Column lg={8}>
          <Select
            id="sterility-status"
            labelText={
              <FormattedMessage
                id="vaccine.development.sterilityStatus"
                defaultMessage="Sterility Status"
              />
            }
            value={vaccineData.virusIsolation.sterilityStatus || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                virusIsolation: {
                  ...prev.virusIsolation,
                  sterilityStatus: e.target.value,
                },
              }))
            }
          >
            <SelectItem text="Select status..." value="" />
            <SelectItem text="Sterile (USP <71> Compliant)" value="sterile" />
            <SelectItem text="Contaminated" value="contaminated" />
            <SelectItem
              text="Under Investigation"
              value="under_investigation"
            />
          </Select>
        </Column>

        <Column lg={16}>
          <TextArea
            id="isolation-notes"
            labelText={
              <FormattedMessage
                id="vaccine.development.isolationNotes"
                defaultMessage="Isolation Process Notes & LMIS Tracking"
              />
            }
            placeholder="Document: isolation procedure, incubation conditions (37°C, 5% CO₂), harvest time, morphological observations, contamination checks, equipment used (biosafety cabinet, incubator), personnel involved..."
            value={vaccineData.virusIsolation.notes || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                virusIsolation: {
                  ...prev.virusIsolation,
                  notes: e.target.value,
                },
              }))
            }
            rows={4}
          />
        </Column>
      </Grid>

      <div style={{ marginTop: "1rem" }}>
        <Button
          onClick={() =>
            saveVaccineData(vaccineData.virusIsolation, "virusIsolation")
          }
          disabled={
            !vaccineData.virusIsolation.cultureBatchId ||
            !vaccineData.virusIsolation.isolationDate ||
            !vaccineData.virusIsolation.isolationMethod ||
            !vaccineData.virusIsolation.cellLine ||
            !vaccineData.virusIsolation.passageNumber
          }
        >
          <FormattedMessage
            id="vaccine.development.saveIsolation"
            defaultMessage="Save Virus Isolation Data"
          />
        </Button>
      </div>
    </Tile>
  );

  // Render titer measurement section
  const renderTiterMeasurement = () => (
    <Tile className="vaccine-stage-tile">
      <h4>
        <FormattedMessage
          id="vaccine.development.titerMeasurement.title"
          defaultMessage="2. Titer Measurement"
        />
      </h4>
      <p>
        <FormattedMessage
          id="vaccine.development.titerMeasurement.description"
          defaultMessage="Record titer values (TCID50, PFU/ml, etc.) with comprehensive LMIS tracking and quality control"
        />
      </p>

      <Grid>
        <Column lg={8}>
          <Select
            id="titer-method"
            labelText={
              <FormattedMessage
                id="vaccine.development.titerMethod"
                defaultMessage="Primary Assay Method *"
              />
            }
            value={vaccineData.titerMeasurement.method || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                titerMeasurement: {
                  ...prev.titerMeasurement,
                  method: e.target.value,
                },
              }))
            }
          >
            <SelectItem text="Select primary assay..." value="" />
            <SelectItem
              text="TCID50 (Tissue Culture Infectious Dose)"
              value="TCID50"
            />
            <SelectItem text="PFU/ml (Plaque Forming Units)" value="PFU_ML" />
            <SelectItem text="FFU/ml (Focus Forming Units)" value="FFU_ML" />
            <SelectItem text="HA (Hemagglutination Assay)" value="HA" />
            <SelectItem text="qRT-PCR (Quantitative RT-PCR)" value="QPCR" />
            <SelectItem text="Luminescent Assay" value="LUMINESCENT" />
            <SelectItem text="MTT Cell Viability" value="MTT" />
          </Select>
        </Column>

        <Column lg={8}>
          <TextInput
            id="tcid50-value"
            labelText={
              <FormattedMessage
                id="vaccine.development.tcid50"
                defaultMessage="TCID50/ml (Log10)"
              />
            }
            value={vaccineData.titerMeasurement.tcid50 || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                titerMeasurement: {
                  ...prev.titerMeasurement,
                  tcid50: e.target.value,
                },
              }))
            }
            type="number"
            step="0.1"
            min="0"
            helperText="50% tissue culture infectious dose per ml"
          />
        </Column>

        <Column lg={8}>
          <TextInput
            id="pfu-value"
            labelText={
              <FormattedMessage
                id="vaccine.development.pfuMl"
                defaultMessage="PFU/ml (Log10)"
              />
            }
            value={vaccineData.titerMeasurement.pfuMl || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                titerMeasurement: {
                  ...prev.titerMeasurement,
                  pfuMl: e.target.value,
                },
              }))
            }
            type="number"
            step="0.1"
            min="0"
            helperText="Plaque forming units per ml"
          />
        </Column>

        <Column lg={8}>
          <TextInput
            id="ha-titer"
            labelText={
              <FormattedMessage
                id="vaccine.development.haTiter"
                defaultMessage="HA Titer (HAU/ml)"
              />
            }
            value={vaccineData.titerMeasurement.haTiter || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                titerMeasurement: {
                  ...prev.titerMeasurement,
                  haTiter: e.target.value,
                },
              }))
            }
            type="number"
            step="1"
            min="0"
            helperText="Hemagglutination units per ml"
          />
        </Column>

        <Column lg={8}>
          <DatePicker dateFormat="Y-m-d" datePickerType="single">
            <DatePickerInput
              id="titer-date"
              labelText={
                <FormattedMessage
                  id="vaccine.development.titerDate"
                  defaultMessage="Measurement Date *"
                />
              }
              value={vaccineData.titerMeasurement.measurementDate || ""}
              onChange={(e) =>
                setVaccineData((prev) => ({
                  ...prev,
                  titerMeasurement: {
                    ...prev.titerMeasurement,
                    measurementDate: e.target.value,
                  },
                }))
              }
            />
          </DatePicker>
        </Column>

        <Column lg={8}>
          <Select
            id="cell-line-titer"
            labelText={
              <FormattedMessage
                id="vaccine.development.cellLineTiter"
                defaultMessage="Cell Line for Assay *"
              />
            }
            value={vaccineData.titerMeasurement.cellLine || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                titerMeasurement: {
                  ...prev.titerMeasurement,
                  cellLine: e.target.value,
                },
              }))
            }
          >
            <SelectItem text="Select cell line..." value="" />
            <SelectItem text="Vero E6" value="vero_e6" />
            <SelectItem text="Vero CCL-81" value="vero_ccl81" />
            <SelectItem text="MDCK" value="mdck" />
            <SelectItem text="HEK-293T" value="hek293t" />
            <SelectItem text="A549" value="a549" />
            <SelectItem text="LLC-MK2" value="llc_mk2" />
          </Select>
        </Column>

        <Column lg={8}>
          <TextInput
            id="dilution-series"
            labelText={
              <FormattedMessage
                id="vaccine.development.dilutionSeries"
                defaultMessage="Dilution Series"
              />
            }
            value={vaccineData.titerMeasurement.dilutionSeries || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                titerMeasurement: {
                  ...prev.titerMeasurement,
                  dilutionSeries: e.target.value,
                },
              }))
            }
            placeholder="e.g., 10⁻¹ to 10⁻⁸, 1:2 serial"
            helperText="Document dilution range and fold"
          />
        </Column>

        <Column lg={8}>
          <TextInput
            id="replicates"
            labelText={
              <FormattedMessage
                id="vaccine.development.replicates"
                defaultMessage="Number of Replicates *"
              />
            }
            value={vaccineData.titerMeasurement.replicates || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                titerMeasurement: {
                  ...prev.titerMeasurement,
                  replicates: e.target.value,
                },
              }))
            }
            type="number"
            step="1"
            min="1"
            max="96"
            helperText="Typical: 3-6 wells per dilution"
          />
        </Column>

        <Column lg={8}>
          <Select
            id="incubation-conditions"
            labelText={
              <FormattedMessage
                id="vaccine.development.incubationConditions"
                defaultMessage="Incubation Conditions"
              />
            }
            value={vaccineData.titerMeasurement.incubationConditions || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                titerMeasurement: {
                  ...prev.titerMeasurement,
                  incubationConditions: e.target.value,
                },
              }))
            }
          >
            <SelectItem text="Select conditions..." value="" />
            <SelectItem text="37°C, 5% CO₂, 3-7 days" value="standard_37c" />
            <SelectItem text="33°C, 5% CO₂, 5-7 days" value="standard_33c" />
            <SelectItem
              text="37°C, ambient CO₂, 3-5 days"
              value="ambient_37c"
            />
            <SelectItem text="Custom conditions" value="custom" />
          </Select>
        </Column>

        <Column lg={8}>
          <Select
            id="equipment-used"
            labelText={
              <FormattedMessage
                id="vaccine.development.equipmentUsed"
                defaultMessage="Equipment Used *"
              />
            }
            value={vaccineData.titerMeasurement.equipmentUsed || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                titerMeasurement: {
                  ...prev.titerMeasurement,
                  equipmentUsed: e.target.value,
                },
              }))
            }
          >
            <SelectItem text="Select equipment..." value="" />
            <SelectItem
              text="Inverted Microscope + Plate Reader"
              value="microscope_reader"
            />
            <SelectItem text="Automated Plate Reader" value="plate_reader" />
            <SelectItem text="Manual Microscopy" value="manual_microscope" />
            <SelectItem text="Flow Cytometer" value="flow_cytometer" />
            <SelectItem text="Luminometer" value="luminometer" />
          </Select>
        </Column>

        <Column lg={8}>
          <Select
            id="qc-status"
            labelText={
              <FormattedMessage
                id="vaccine.development.qcStatus"
                defaultMessage="QC Status *"
              />
            }
            value={vaccineData.titerMeasurement.qcStatus || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                titerMeasurement: {
                  ...prev.titerMeasurement,
                  qcStatus: e.target.value,
                },
              }))
            }
          >
            <SelectItem text="Select QC status..." value="" />
            <SelectItem text="Passed - Within Range" value="passed" />
            <SelectItem text="Passed - Retest Required" value="passed_retest" />
            <SelectItem
              text="Failed - Out of Specification"
              value="failed_oos"
            />
            <SelectItem text="Under Investigation" value="investigation" />
          </Select>
        </Column>

        <Column lg={16}>
          <TextArea
            id="titer-notes"
            labelText={
              <FormattedMessage
                id="vaccine.development.titerNotes"
                defaultMessage="Titer Measurement Notes & LMIS Tracking"
              />
            }
            placeholder="Document: endpoint criteria, morphological observations, controls (positive/negative), reagent lots, standard curve data, coefficient of variation (CV%), acceptance criteria, deviations..."
            value={vaccineData.titerMeasurement.notes || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                titerMeasurement: {
                  ...prev.titerMeasurement,
                  notes: e.target.value,
                },
              }))
            }
            rows={4}
          />
        </Column>
      </Grid>

      <div style={{ marginTop: "1rem" }}>
        <Button
          onClick={() =>
            saveVaccineData(vaccineData.titerMeasurement, "titerMeasurement")
          }
          disabled={
            !vaccineData.titerMeasurement.method ||
            !vaccineData.titerMeasurement.measurementDate ||
            !vaccineData.titerMeasurement.cellLine ||
            !vaccineData.titerMeasurement.replicates ||
            !vaccineData.titerMeasurement.equipmentUsed ||
            !vaccineData.titerMeasurement.qcStatus
          }
        >
          <FormattedMessage
            id="vaccine.development.saveTiter"
            defaultMessage="Save Titer Measurement"
          />
        </Button>
      </div>
    </Tile>
  );

  // Render genome sequencing section
  const renderGenomeSequencing = () => (
    <Tile className="vaccine-stage-tile">
      <h4>
        <FormattedMessage
          id="vaccine.development.genomeSequencing.title"
          defaultMessage="3. Genome Sequencing"
        />
      </h4>
      <p>
        <FormattedMessage
          id="vaccine.development.genomeSequencing.description"
          defaultMessage="Upload FASTA sequences and manage GenBank submissions"
        />
      </p>

      <Grid>
        <Column lg={16}>
          <FileUploader
            labelTitle={
              <FormattedMessage
                id="vaccine.development.fastaUpload"
                defaultMessage="Upload FASTA Sequence File"
              />
            }
            labelDescription={
              <FormattedMessage
                id="vaccine.development.fastaUpload.description"
                defaultMessage="Upload viral genome sequence in FASTA format"
              />
            }
            buttonLabel={
              <FormattedMessage
                id="vaccine.development.fastaUpload.button"
                defaultMessage="Select FASTA file"
              />
            }
            filenameStatus="edit"
            accept={[".fasta", ".fa", ".fas", ".txt"]}
            multiple={false}
            iconDescription={
              <FormattedMessage
                id="vaccine.development.fastaUpload.clear"
                defaultMessage="Clear file"
              />
            }
            onChange={(e) => {
              if (e.target.files && e.target.files[0]) {
                setVaccineData((prev) => ({
                  ...prev,
                  genomeSequencing: {
                    ...prev.genomeSequencing,
                    fastaFile: e.target.files[0],
                    fastaFileName: e.target.files[0].name,
                  },
                }));
              }
            }}
          />
        </Column>

        <Column lg={8}>
          <TextInput
            id="genbank-accession"
            labelText={
              <FormattedMessage
                id="vaccine.development.genbankAccession"
                defaultMessage="GenBank Accession Number"
              />
            }
            placeholder="e.g., MN996532.1"
            value={vaccineData.genomeSequencing.genbankAccession || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                genomeSequencing: {
                  ...prev.genomeSequencing,
                  genbankAccession: e.target.value,
                },
              }))
            }
          />
        </Column>

        <Column lg={8}>
          <DatePicker dateFormat="Y-m-d" datePickerType="single">
            <DatePickerInput
              id="sequencing-date"
              labelText={
                <FormattedMessage
                  id="vaccine.development.sequencingDate"
                  defaultMessage="Sequencing Date *"
                />
              }
              value={vaccineData.genomeSequencing.sequencingDate || ""}
              onChange={(e) =>
                setVaccineData((prev) => ({
                  ...prev,
                  genomeSequencing: {
                    ...prev.genomeSequencing,
                    sequencingDate: e.target.value,
                  },
                }))
              }
            />
          </DatePicker>
        </Column>

        <Column lg={16}>
          <TextArea
            id="sequence-analysis"
            labelText={
              <FormattedMessage
                id="vaccine.development.sequenceAnalysis"
                defaultMessage="Sequence Analysis Results"
              />
            }
            placeholder="Document sequence quality, mutations, phylogenetic analysis..."
            value={vaccineData.genomeSequencing.analysisResults || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                genomeSequencing: {
                  ...prev.genomeSequencing,
                  analysisResults: e.target.value,
                },
              }))
            }
          />
        </Column>
      </Grid>

      <div style={{ marginTop: "1rem" }}>
        <Button
          onClick={() =>
            saveVaccineData(vaccineData.genomeSequencing, "genomeSequencing")
          }
          disabled={!vaccineData.genomeSequencing.sequencingDate}
        >
          <FormattedMessage
            id="vaccine.development.saveSequencing"
            defaultMessage="Save Genome Sequencing Data"
          />
        </Button>
      </div>
    </Tile>
  );

  // Render seed virus production section
  const renderSeedVirusProduction = () => (
    <Tile className="vaccine-stage-tile">
      <h4>
        <FormattedMessage
          id="vaccine.development.seedVirusProduction.title"
          defaultMessage="4. Seed Virus Production"
        />
      </h4>
      <p>
        <FormattedMessage
          id="vaccine.development.seedVirusProduction.description"
          defaultMessage="Select strain for vaccine and document production criteria"
        />
      </p>

      <Grid>
        <Column lg={8}>
          <TextInput
            id="seed-batch-id"
            labelText={
              <FormattedMessage
                id="vaccine.development.seedBatchId"
                defaultMessage="Seed Virus Batch ID *"
              />
            }
            placeholder="e.g., MVS-2024-001"
            value={vaccineData.seedVirusProduction.seedBatchId || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                seedVirusProduction: {
                  ...prev.seedVirusProduction,
                  seedBatchId: e.target.value,
                },
              }))
            }
          />
        </Column>

        <Column lg={8}>
          <Select
            id="seed-type"
            labelText={
              <FormattedMessage
                id="vaccine.development.seedType"
                defaultMessage="Seed Type *"
              />
            }
            value={vaccineData.seedVirusProduction.seedType || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                seedVirusProduction: {
                  ...prev.seedVirusProduction,
                  seedType: e.target.value,
                },
              }))
            }
          >
            <SelectItem text="Select seed type..." value="" />
            <SelectItem text="Master Seed Virus" value="MASTER" />
            <SelectItem text="Working Seed Virus" value="WORKING" />
            <SelectItem text="Production Seed" value="PRODUCTION" />
          </Select>
        </Column>

        <Column lg={16}>
          <TextArea
            id="selection-criteria"
            labelText={
              <FormattedMessage
                id="vaccine.development.selectionCriteria"
                defaultMessage="Selection Criteria *"
              />
            }
            placeholder="Document strain selection criteria: immunogenicity, safety profile, genetic stability..."
            value={vaccineData.seedVirusProduction.selectionCriteria || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                seedVirusProduction: {
                  ...prev.seedVirusProduction,
                  selectionCriteria: e.target.value,
                },
              }))
            }
          />
        </Column>

        <Column lg={8}>
          <TextInput
            id="passage-level"
            labelText={
              <FormattedMessage
                id="vaccine.development.passageLevel"
                defaultMessage="Passage Level"
              />
            }
            value={vaccineData.seedVirusProduction.passageLevel || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                seedVirusProduction: {
                  ...prev.seedVirusProduction,
                  passageLevel: e.target.value,
                },
              }))
            }
            type="number"
            step="1"
            min="0"
          />
        </Column>

        <Column lg={8}>
          <DatePicker dateFormat="Y-m-d" datePickerType="single">
            <DatePickerInput
              id="production-date"
              labelText={
                <FormattedMessage
                  id="vaccine.development.productionDate"
                  defaultMessage="Production Date *"
                />
              }
              value={vaccineData.seedVirusProduction.productionDate || ""}
              onChange={(e) =>
                setVaccineData((prev) => ({
                  ...prev,
                  seedVirusProduction: {
                    ...prev.seedVirusProduction,
                    productionDate: e.target.value,
                  },
                }))
              }
            />
          </DatePicker>
        </Column>
      </Grid>

      <div style={{ marginTop: "1rem" }}>
        <Button
          onClick={() =>
            saveVaccineData(
              vaccineData.seedVirusProduction,
              "seedVirusProduction",
            )
          }
          disabled={
            !vaccineData.seedVirusProduction.seedBatchId ||
            !vaccineData.seedVirusProduction.seedType ||
            !vaccineData.seedVirusProduction.selectionCriteria ||
            !vaccineData.seedVirusProduction.productionDate
          }
        >
          <FormattedMessage
            id="vaccine.development.saveSeedProduction"
            defaultMessage="Save Seed Virus Production Data"
          />
        </Button>
      </div>
    </Tile>
  );

  // Render preclinical trials section
  const renderPreclinicalTrials = () => (
    <Tile className="vaccine-stage-tile">
      <h4>
        <FormattedMessage
          id="vaccine.development.preclinicalTrials.title"
          defaultMessage="5. Preclinical Trials (External)"
        />
      </h4>
      <p>
        <FormattedMessage
          id="vaccine.development.preclinicalTrials.description"
          defaultMessage="Track animal testing trials and outcomes"
        />
      </p>

      <Grid>
        <Column lg={8}>
          <DatePicker dateFormat="Y-m-d" datePickerType="single">
            <DatePickerInput
              id="trial-initiation"
              labelText={
                <FormattedMessage
                  id="vaccine.development.trialInitiation"
                  defaultMessage="Trial Initiation Date *"
                />
              }
              value={vaccineData.preclinicalTrials.initiationDate || ""}
              onChange={(e) =>
                setVaccineData((prev) => ({
                  ...prev,
                  preclinicalTrials: {
                    ...prev.preclinicalTrials,
                    initiationDate: e.target.value,
                  },
                }))
              }
            />
          </DatePicker>
        </Column>

        <Column lg={8}>
          <Select
            id="animal-species"
            labelText={
              <FormattedMessage
                id="vaccine.development.animalSpecies"
                defaultMessage="Animal Species *"
              />
            }
            value={vaccineData.preclinicalTrials.animalSpecies || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                preclinicalTrials: {
                  ...prev.preclinicalTrials,
                  animalSpecies: e.target.value,
                },
              }))
            }
          >
            <SelectItem text="Select species..." value="" />
            <SelectItem text="Mice" value="MICE" />
            <SelectItem text="Rats" value="RATS" />
            <SelectItem text="Rabbits" value="RABBITS" />
            <SelectItem text="Non-human Primates" value="NHP" />
            <SelectItem text="Ferrets" value="FERRETS" />
            <SelectItem text="Other" value="OTHER" />
          </Select>
        </Column>

        <Column lg={8}>
          <TextInput
            id="animal-count"
            labelText={
              <FormattedMessage
                id="vaccine.development.animalCount"
                defaultMessage="Number of Animals"
              />
            }
            value={vaccineData.preclinicalTrials.animalCount || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                preclinicalTrials: {
                  ...prev.preclinicalTrials,
                  animalCount: e.target.value,
                },
              }))
            }
            type="number"
            step="1"
            min="1"
          />
        </Column>

        <Column lg={8}>
          <Select
            id="trial-status"
            labelText={
              <FormattedMessage
                id="vaccine.development.trialStatus"
                defaultMessage="Trial Status"
              />
            }
            value={vaccineData.preclinicalTrials.status || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                preclinicalTrials: {
                  ...prev.preclinicalTrials,
                  status: e.target.value,
                },
              }))
            }
          >
            <SelectItem text="Select status..." value="" />
            <SelectItem text="Planned" value="PLANNED" />
            <SelectItem text="In Progress" value="IN_PROGRESS" />
            <SelectItem text="Completed" value="COMPLETED" />
            <SelectItem text="On Hold" value="ON_HOLD" />
            <SelectItem text="Terminated" value="TERMINATED" />
          </Select>
        </Column>

        <Column lg={16}>
          <TextArea
            id="trial-outcomes"
            labelText={
              <FormattedMessage
                id="vaccine.development.trialOutcomes"
                defaultMessage="Trial Outcomes (Immunogenicity & Safety)"
              />
            }
            placeholder="Document immunogenicity results, safety profile, adverse events..."
            value={vaccineData.preclinicalTrials.outcomes || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                preclinicalTrials: {
                  ...prev.preclinicalTrials,
                  outcomes: e.target.value,
                },
              }))
            }
          />
        </Column>
      </Grid>

      <div style={{ marginTop: "1rem" }}>
        <Button
          onClick={() =>
            saveVaccineData(vaccineData.preclinicalTrials, "preclinicalTrials")
          }
          disabled={
            !vaccineData.preclinicalTrials.initiationDate ||
            !vaccineData.preclinicalTrials.animalSpecies
          }
        >
          <FormattedMessage
            id="vaccine.development.saveTrials"
            defaultMessage="Save Preclinical Trial Data"
          />
        </Button>
      </div>
    </Tile>
  );

  // Render clinical trials section
  const renderClinicalTrials = () => (
    <Tile className="vaccine-stage-tile">
      <h4>
        <FormattedMessage
          id="vaccine.development.clinicalTrials.title"
          defaultMessage="6. Clinical Trials (External)"
        />
      </h4>
      <p>
        <FormattedMessage
          id="vaccine.development.clinicalTrials.description"
          defaultMessage="Human testing (external) - Link trial phases (Phase I/II/III), outcomes, regulatory submissions"
        />
      </p>

      {/* PDF Requirements: Link trial phases (Phase I/II/III), outcomes, regulatory submissions */}
      <Grid>
        <Column lg={8}>
          <Select
            id="trial-phase"
            labelText={
              <FormattedMessage
                id="vaccine.development.clinicalPhase"
                defaultMessage="Clinical Trial Phase *"
              />
            }
            value={vaccineData.clinicalTrials.phase || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                clinicalTrials: {
                  ...prev.clinicalTrials,
                  phase: e.target.value,
                },
              }))
            }
          >
            <SelectItem text="Select phase..." value="" />
            <SelectItem text="Phase I - Safety & Dosage" value="PHASE_I" />
            <SelectItem
              text="Phase II - Efficacy & Side Effects"
              value="PHASE_II"
            />
            <SelectItem
              text="Phase III - Large Scale Testing"
              value="PHASE_III"
            />
            <SelectItem
              text="Phase IV - Post-Market Surveillance"
              value="PHASE_IV"
            />
          </Select>
        </Column>

        <Column lg={8}>
          <DatePicker dateFormat="Y-m-d" datePickerType="single">
            <DatePickerInput
              id="trial-initiation-date"
              labelText={
                <FormattedMessage
                  id="vaccine.development.trialInitiationDate"
                  defaultMessage="Trial Initiation Date *"
                />
              }
              value={vaccineData.clinicalTrials.initiationDate || ""}
              onChange={(e) =>
                setVaccineData((prev) => ({
                  ...prev,
                  clinicalTrials: {
                    ...prev.clinicalTrials,
                    initiationDate: e.target.value,
                  },
                }))
              }
            />
          </DatePicker>
        </Column>

        <Column lg={8}>
          <TextInput
            id="trial-identifier"
            labelText={
              <FormattedMessage
                id="vaccine.development.trialIdentifier"
                defaultMessage="Trial Identifier/Registry Number"
              />
            }
            placeholder="e.g., NCT#, EudraCT#, CTRI#"
            value={vaccineData.clinicalTrials.identifier || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                clinicalTrials: {
                  ...prev.clinicalTrials,
                  identifier: e.target.value,
                },
              }))
            }
          />
        </Column>

        <Column lg={8}>
          <TextInput
            id="participant-count"
            labelText={
              <FormattedMessage
                id="vaccine.development.participantCount"
                defaultMessage="Participant Count"
              />
            }
            value={vaccineData.clinicalTrials.participantCount || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                clinicalTrials: {
                  ...prev.clinicalTrials,
                  participantCount: e.target.value,
                },
              }))
            }
            type="number"
            step="1"
            min="1"
          />
        </Column>

        <Column lg={8}>
          <Select
            id="trial-status"
            labelText={
              <FormattedMessage
                id="vaccine.development.trialStatus"
                defaultMessage="Trial Status *"
              />
            }
            value={vaccineData.clinicalTrials.status || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                clinicalTrials: {
                  ...prev.clinicalTrials,
                  status: e.target.value,
                },
              }))
            }
          >
            <SelectItem text="Select status..." value="" />
            <SelectItem text="Planning" value="PLANNING" />
            <SelectItem text="Recruiting" value="RECRUITING" />
            <SelectItem text="Active" value="ACTIVE" />
            <SelectItem text="Completed" value="COMPLETED" />
            <SelectItem text="Suspended" value="SUSPENDED" />
            <SelectItem text="Terminated" value="TERMINATED" />
            <SelectItem text="Withdrawn" value="WITHDRAWN" />
          </Select>
        </Column>

        <Column lg={8}>
          <TextInput
            id="regulatory-authority"
            labelText={
              <FormattedMessage
                id="vaccine.development.regulatoryAuthority"
                defaultMessage="Regulatory Authority"
              />
            }
            placeholder="e.g., FDA, EMA, NMPA, CDSCO"
            value={vaccineData.clinicalTrials.regulatoryAuthority || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                clinicalTrials: {
                  ...prev.clinicalTrials,
                  regulatoryAuthority: e.target.value,
                },
              }))
            }
          />
        </Column>

        <Column lg={8}>
          <TextInput
            id="submission-number"
            labelText={
              <FormattedMessage
                id="vaccine.development.submissionNumber"
                defaultMessage="IND/CTA Submission Number"
              />
            }
            placeholder="e.g., IND#, CTA#"
            value={vaccineData.clinicalTrials.submissionNumber || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                clinicalTrials: {
                  ...prev.clinicalTrials,
                  submissionNumber: e.target.value,
                },
              }))
            }
          />
        </Column>

        <Column lg={8}>
          <DatePicker dateFormat="Y-m-d" datePickerType="single">
            <DatePickerInput
              id="completion-date"
              labelText={
                <FormattedMessage
                  id="vaccine.development.completionDate"
                  defaultMessage="Completion Date"
                />
              }
              value={vaccineData.clinicalTrials.completionDate || ""}
              onChange={(e) =>
                setVaccineData((prev) => ({
                  ...prev,
                  clinicalTrials: {
                    ...prev.clinicalTrials,
                    completionDate: e.target.value,
                  },
                }))
              }
            />
          </DatePicker>
        </Column>

        <Column lg={16}>
          <TextArea
            id="trial-outcomes"
            labelText={
              <FormattedMessage
                id="vaccine.development.trialOutcomes"
                defaultMessage="Trial Outcomes & Key Findings"
              />
            }
            placeholder="Document primary and secondary endpoints, safety profile, efficacy data, immunogenicity results..."
            value={vaccineData.clinicalTrials.outcomes || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                clinicalTrials: {
                  ...prev.clinicalTrials,
                  outcomes: e.target.value,
                },
              }))
            }
            rows={4}
          />
        </Column>

        <Column lg={16}>
          <TextArea
            id="regulatory-submissions"
            labelText={
              <FormattedMessage
                id="vaccine.development.regulatorySubmissions"
                defaultMessage="Regulatory Submissions & Approvals"
              />
            }
            placeholder="Document regulatory interactions, submission dates, approval status, post-market commitments..."
            value={vaccineData.clinicalTrials.regulatorySubmissions || ""}
            onChange={(e) =>
              setVaccineData((prev) => ({
                ...prev,
                clinicalTrials: {
                  ...prev.clinicalTrials,
                  regulatorySubmissions: e.target.value,
                },
              }))
            }
            rows={4}
          />
        </Column>
      </Grid>

      <div style={{ marginTop: "1rem" }}>
        <Button
          onClick={() =>
            saveVaccineData(vaccineData.clinicalTrials, "clinicalTrials")
          }
          disabled={
            !vaccineData.clinicalTrials.phase ||
            !vaccineData.clinicalTrials.initiationDate ||
            !vaccineData.clinicalTrials.status
          }
        >
          <FormattedMessage
            id="vaccine.development.saveClinicalTrials"
            defaultMessage="Save Clinical Trial Data"
          />
        </Button>
      </div>
    </Tile>
  );

  // Stage navigation
  const stages = [
    { key: "virus_isolation", label: "Virus Isolation" },
    { key: "titer_measurement", label: "Titer Measurement" },
    { key: "genome_sequencing", label: "Genome Sequencing" },
    { key: "seed_virus_production", label: "Seed Virus Production" },
    { key: "preclinical_trials", label: "Preclinical Trials" },
    { key: "clinical_trials", label: "Clinical Trials" },
  ];

  if (loading) {
    return <Loading />;
  }

  return (
    <div className="vaccine-development-page">
      <h3>
        <FormattedMessage
          id="vaccine.development.title"
          defaultMessage="Vaccine Development Workflow"
        />
      </h3>
      <p>
        <FormattedMessage
          id="vaccine.development.subtitle"
          defaultMessage="Complete the 5-stage vaccine development process from virus isolation to preclinical trials"
        />
      </p>

      {/* Stage Navigation */}
      <div style={{ marginBottom: "2rem" }}>
        <RadioButtonGroup
          legendText={
            <FormattedMessage
              id="vaccine.development.selectStage"
              defaultMessage="Select Development Stage"
            />
          }
          name="vaccine-stage"
          value={currentStage}
          onChange={setCurrentStage}
          orientation="horizontal"
        >
          {stages.map((stage) => (
            <RadioButton
              key={stage.key}
              labelText={stage.label}
              value={stage.key}
              id={stage.key}
            />
          ))}
        </RadioButtonGroup>
      </div>

      {/* Render current stage */}
      {currentStage === "virus_isolation" && renderVirusIsolation()}
      {currentStage === "titer_measurement" && renderTiterMeasurement()}
      {currentStage === "genome_sequencing" && renderGenomeSequencing()}
      {currentStage === "seed_virus_production" && renderSeedVirusProduction()}
      {currentStage === "preclinical_trials" && renderPreclinicalTrials()}
      {currentStage === "clinical_trials" && renderClinicalTrials()}

      {/* Progress Summary */}
      <div style={{ marginTop: "2rem" }}>
        <h4>
          <FormattedMessage
            id="vaccine.development.progress"
            defaultMessage="Development Progress"
          />
        </h4>
        <Grid>
          {stages.map((stage) => {
            const stageData = vaccineData[stage.key.replace("_", "")];
            const isCompleted = stageData && Object.keys(stageData).length > 0;
            return (
              <Column lg={3} key={stage.key}>
                <Tag type={isCompleted ? "green" : "gray"}>
                  {stage.label}: {isCompleted ? "✓" : "○"}
                </Tag>
              </Column>
            );
          })}
        </Grid>
      </div>
    </div>
  );
};

export default VaccineevelopmentPage;
