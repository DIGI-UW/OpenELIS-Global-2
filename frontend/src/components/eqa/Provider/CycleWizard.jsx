import React, { useEffect, useRef, useState } from "react";
import {
  Button,
  Checkbox,
  Column,
  DatePicker,
  DatePickerInput,
  Grid,
  InlineNotification,
  Loading,
  NumberInput,
  ProgressIndicator,
  ProgressStep,
  RadioButton,
  RadioButtonGroup,
  Select,
  SelectItem,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  TextArea,
  TextInput,
  Tile,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { useHistory, useLocation } from "react-router-dom";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import {
  createCycle,
  createPanel,
  failed,
  fetchTests,
  transitionCycle,
} from "../eqaApi";
import { hintStyle } from "../eqaCommon";
import { formatDateOnly, toLocalIsoDate } from "../../utils/Utils";
import { fetchEnrollments, fetchProviderSchemes } from "./providerApi";
import {
  DISTRIBUTION_METHODS,
  aliquotsNeeded,
  cyclePayload,
  panelPayload,
  wizardBlockers,
} from "./cycleWizardRules";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "banner.menu.eqa.provider", link: "/qa/eqa/provider/schemes" },
  { label: "eqa.provider.cycle.new", link: "/qa/eqa/provider/cycles/new" },
];

// Exactly the EQAPanelSourceType / EQAStorageTemp constants; the server rejects
// anything else, so these are not free text.
const SOURCE_TYPES = ["IN_HOUSE_ALIQUOTED", "VENDOR_SOURCED", "MIXED"];
const STORAGE_TEMPS = [
  "AMBIENT",
  "REFRIGERATED_2_8C",
  "FROZEN_MINUS_20C",
  "ULTRA_FROZEN_MINUS_80C",
  "DRY_ICE",
];

const emptySample = (index) => ({
  key: `S${String(index).padStart(2, "0")}`,
  testId: "",
  targetValue: "",
  targetUnit: "",
  rangeLow: "",
  rangeHigh: "",
});

/**
 * FR-V2.5-02: cycle details, panel samples and source, participants,
 * distribution method, confirm. The confirm step is the only step that writes —
 * an abandoned wizard leaves no half-built cycle behind.
 *
 * There are no draft cycles; add a resume path when a provider asks for one.
 */
const CycleWizard = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const t = (id, defaultMessage, values) =>
    intl.formatMessage({ id, defaultMessage }, values);

  const [step, setStep] = useState(0);
  const [busy, setBusy] = useState(false);
  const [notification, setNotification] = useState(null);
  const [schemes, setSchemes] = useState([]);
  const [tests, setTests] = useState([]);
  const [participants, setParticipants] = useState([]);

  const [cycle, setCycle] = useState({
    schemeId: new URLSearchParams(location.search).get("schemeId") || "",
    cycleName: "",
    plannedStartDate: "",
    plannedEndDate: "",
  });
  const [samples, setSamples] = useState([emptySample(1)]);
  const [prep, setPrep] = useState({
    sourceType: "IN_HOUSE_ALIQUOTED",
    lotNumber: "",
    aliquotsProduced: 0,
    storageTemp: "REFRIGERATED_2_8C",
    expirationDate: "",
    homogeneityQcPassed: false,
    homogeneityQcNotes: "",
  });
  const [distributionMethod, setDistributionMethod] = useState("FHIR");

  // A panel write that fails after the cycle is written must not create a second
  // cycle when the operator fixes the input and tries again.
  const createdCycle = useRef(null);
  const createdPanel = useRef(null);

  useEffect(() => {
    fetchProviderSchemes(setSchemes);
    fetchTests(setTests);
  }, []);

  useEffect(() => {
    if (!cycle.schemeId) {
      setParticipants([]);
      return;
    }
    fetchEnrollments(cycle.schemeId, (rows) =>
      setParticipants(rows.filter((row) => row.status === "Active")),
    );
  }, [cycle.schemeId]);

  const needed = aliquotsNeeded(samples, participants.length);
  const blockers = wizardBlockers({
    cycle,
    samples,
    participants,
    distributionMethod,
  });
  const scheme = schemes.find(
    (candidate) => String(candidate.id) === String(cycle.schemeId),
  );

  const updateSample = (key, field, value) =>
    setSamples((rows) =>
      rows.map((row) => (row.key === key ? { ...row, [field]: value } : row)),
    );

  const testName = (testId) => {
    const test = tests.find(
      (candidate) => String(candidate.id) === String(testId),
    );
    return test ? test.name || test.value : testId;
  };

  const fail = (id, fallback, response) => {
    setBusy(false);
    setNotification({
      kind: "error",
      message: response?.error || t(id, fallback),
    });
  };

  const openForPrep = (cycleResponse) =>
    transitionCycle(
      cycleResponse.id,
      {
        newState: "PREP_IN_PROGRESS",
        stateMachine: "PROVIDER",
        reason: t(
          "eqa.provider.wizard.transitionReason",
          "Cycle created from the provider wizard — panel prep begins",
        ),
      },
      (response) => {
        setBusy(false);
        if (failed(response)) {
          // The cycle and its panel exist; only the state move failed, so the
          // workbench is still the right place to land — it can move the state.
          setNotification({
            kind: "warning",
            message:
              response?.error ||
              t(
                "eqa.provider.wizard.transitionError",
                "The cycle and panel were created but the cycle did not move to prep. Advance it from the workbench.",
              ),
          });
        }
        history.push(`/qa/eqa/provider/cycles/${cycleResponse.id}/workbench`);
      },
    );

  const writePanel = (cycleResponse) => {
    if (createdPanel.current) {
      openForPrep(cycleResponse);
      return;
    }
    createPanel(
      panelPayload(
        { cycle, samples, prep },
        cycleResponse,
        cycle.cycleName ||
          `${scheme?.name || t("eqa.provider.scheme", "Scheme")} ${t("eqa.provider.cycle", "Cycle")} ${cycleResponse.cycleNumber}`,
      ),
      (panelResponse) => {
        if (failed(panelResponse)) {
          fail(
            "eqa.provider.wizard.panelError",
            "Could not create the panel",
            panelResponse,
          );
          return;
        }
        createdPanel.current = panelResponse;
        openForPrep(cycleResponse);
      },
    );
  };

  const create = () => {
    setBusy(true);
    setNotification(null);
    if (createdCycle.current) {
      writePanel(createdCycle.current);
      return;
    }
    createCycle(
      cyclePayload({ cycle, distributionMethod }),
      (cycleResponse) => {
        if (failed(cycleResponse)) {
          fail(
            "eqa.provider.wizard.cycleError",
            "Could not create the cycle",
            cycleResponse,
          );
          return;
        }
        createdCycle.current = cycleResponse;
        writePanel(cycleResponse);
      },
    );
  };

  const stepTitles = [
    t("eqa.provider.wizard.step1", "Cycle details"),
    t("eqa.provider.wizard.step2", "Panel samples & source"),
    t("eqa.provider.wizard.step3", "Participants"),
    t("eqa.provider.wizard.step4", "Distribution"),
    t("eqa.provider.wizard.step5", "Confirm"),
  ];

  const details = () => (
    <Tile>
      <Select
        id="cycle-scheme"
        labelText={t("eqa.provider.scheme", "Scheme")}
        value={cycle.schemeId}
        onChange={(event) =>
          setCycle({ ...cycle, schemeId: event.target.value })
        }
      >
        <SelectItem value="" text={t("label.select", "Select")} />
        {schemes.map((option) => (
          <SelectItem
            key={option.id}
            value={option.id}
            text={`${option.name} (${option.participantCount || 0})`}
          />
        ))}
      </Select>
      <TextInput
        id="cycle-name"
        labelText={t("eqa.cycle.name", "Cycle name")}
        helperText={t(
          "eqa.provider.wizard.numberHint",
          "The cycle number is the scheme's next one.",
        )}
        value={cycle.cycleName}
        onChange={(event) =>
          setCycle({ ...cycle, cycleName: event.target.value })
        }
      />
      <DatePicker
        datePickerType="single"
        dateFormat="d/m/Y"
        value={cycle.plannedStartDate}
        onChange={([picked]) =>
          setCycle({ ...cycle, plannedStartDate: toLocalIsoDate(picked) })
        }
      >
        <DatePickerInput
          id="cycle-start"
          placeholder="dd/mm/yyyy"
          labelText={t("eqa.cycle.plannedStart", "Planned start")}
        />
      </DatePicker>
      <DatePicker
        datePickerType="single"
        dateFormat="d/m/Y"
        value={cycle.plannedEndDate}
        onChange={([picked]) =>
          setCycle({ ...cycle, plannedEndDate: toLocalIsoDate(picked) })
        }
      >
        <DatePickerInput
          id="cycle-deadline"
          placeholder="dd/mm/yyyy"
          labelText={t("eqa.cycle.submissionDeadline", "Submission deadline")}
        />
      </DatePicker>
    </Tile>
  );

  const panelStep = () => (
    <Tile>
      <Table size="sm">
        <TableHead>
          <TableRow>
            <TableHeader>{t("eqa.panel.sample", "Sample")}</TableHeader>
            <TableHeader>{t("eqa.results.test", "Test")}</TableHeader>
            <TableHeader>{t("eqa.panel.target", "Target value")}</TableHeader>
            <TableHeader>{t("eqa.panel.unit", "Unit")}</TableHeader>
            <TableHeader>{t("eqa.panel.rangeLow", "Range low")}</TableHeader>
            <TableHeader>{t("eqa.panel.rangeHigh", "Range high")}</TableHeader>
          </TableRow>
        </TableHead>
        <TableBody>
          {samples.map((sample) => (
            <TableRow key={sample.key}>
              <TableCell>{sample.key}</TableCell>
              <TableCell>
                <Select
                  id={`sample-test-${sample.key}`}
                  labelText=""
                  hideLabel
                  value={sample.testId}
                  onChange={(event) =>
                    updateSample(sample.key, "testId", event.target.value)
                  }
                >
                  <SelectItem value="" text={t("label.select", "Select")} />
                  {tests.map((test) => (
                    <SelectItem
                      key={test.id}
                      value={test.id}
                      text={test.name || test.value}
                    />
                  ))}
                </Select>
              </TableCell>
              <TableCell>
                <TextInput
                  id={`sample-target-${sample.key}`}
                  labelText=""
                  hideLabel
                  value={sample.targetValue}
                  onChange={(event) =>
                    updateSample(sample.key, "targetValue", event.target.value)
                  }
                />
              </TableCell>
              <TableCell>
                <TextInput
                  id={`sample-unit-${sample.key}`}
                  labelText=""
                  hideLabel
                  value={sample.targetUnit}
                  onChange={(event) =>
                    updateSample(sample.key, "targetUnit", event.target.value)
                  }
                />
              </TableCell>
              <TableCell>
                <TextInput
                  id={`sample-low-${sample.key}`}
                  labelText=""
                  hideLabel
                  value={sample.rangeLow}
                  onChange={(event) =>
                    updateSample(sample.key, "rangeLow", event.target.value)
                  }
                />
              </TableCell>
              <TableCell>
                <TextInput
                  id={`sample-high-${sample.key}`}
                  labelText=""
                  hideLabel
                  value={sample.rangeHigh}
                  onChange={(event) =>
                    updateSample(sample.key, "rangeHigh", event.target.value)
                  }
                />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
      <Button
        kind="ghost"
        size="sm"
        onClick={() =>
          setSamples([...samples, emptySample(samples.length + 1)])
        }
      >
        {t("eqa.panel.addSample", "Add sample")}
      </Button>
      {samples.length > 1 && (
        <Button
          kind="ghost"
          size="sm"
          onClick={() => setSamples(samples.slice(0, -1))}
        >
          {t("eqa.panel.removeSample", "Remove last sample")}
        </Button>
      )}
      <p style={hintStyle}>
        {t(
          "eqa.provider.wizard.targetsSealed",
          "Target values are stored sealed: nobody sees them again until the cycle is scored.",
        )}
      </p>
      <Select
        id="panel-source"
        labelText={t("eqa.panel.sourceType", "Source")}
        value={prep.sourceType}
        onChange={(event) =>
          setPrep({ ...prep, sourceType: event.target.value })
        }
      >
        {SOURCE_TYPES.map((type) => (
          <SelectItem
            key={type}
            value={type}
            // The panel vocabulary is shared with the in-house wizard, which
            // shipped these ids first — one translation, not two.
            text={t(`eqa.inhouse.sourceType.${type}`, type.replace(/_/g, " "))}
          />
        ))}
      </Select>
      <TextInput
        id="panel-lot"
        labelText={t("eqa.panel.lotNumber", "Lot number")}
        value={prep.lotNumber}
        onChange={(event) =>
          setPrep({ ...prep, lotNumber: event.target.value })
        }
      />
      <Select
        id="panel-storage"
        labelText={t("eqa.panel.storageTemp", "Storage temperature")}
        value={prep.storageTemp}
        onChange={(event) =>
          setPrep({ ...prep, storageTemp: event.target.value })
        }
      >
        {STORAGE_TEMPS.map((temp) => (
          <SelectItem
            key={temp}
            value={temp}
            text={t(`eqa.inhouse.storageTemp.${temp}`, temp.replace(/_/g, " "))}
          />
        ))}
      </Select>
      <NumberInput
        id="panel-aliquots"
        label={t("eqa.panel.aliquotsProduced", "Aliquots produced")}
        helperText={t(
          "eqa.provider.wizard.aliquotsHint",
          "{needed} needed: one per sample per participant. Record what you actually produce here or in the prep workbench.",
          { needed },
        )}
        min={0}
        value={prep.aliquotsProduced}
        onChange={(event, { value }) =>
          setPrep({ ...prep, aliquotsProduced: Number(value) || 0 })
        }
      />
      <Checkbox
        id="panel-qc"
        labelText={t("eqa.panel.homogeneityQc", "Homogeneity QC passed")}
        checked={prep.homogeneityQcPassed}
        onChange={(event, { checked }) =>
          setPrep({ ...prep, homogeneityQcPassed: checked })
        }
      />
      <TextArea
        id="panel-qc-notes"
        labelText={t("eqa.panel.homogeneityQcNotes", "Homogeneity QC notes")}
        rows={2}
        value={prep.homogeneityQcNotes}
        onChange={(event) =>
          setPrep({ ...prep, homogeneityQcNotes: event.target.value })
        }
      />
    </Tile>
  );

  const participantStep = () => (
    <Tile>
      {participants.length === 0 ? (
        <InlineNotification
          kind="warning"
          lowContrast
          hideCloseButton
          title={t(
            "eqa.provider.wizard.noParticipants.title",
            "No enrolled participants",
          )}
          subtitle={t(
            "eqa.provider.wizard.noParticipants.body",
            "A cycle needs at least one active participant laboratory. Enrol them on EQA Participants, then start the cycle.",
          )}
        />
      ) : (
        <Table size="sm">
          <TableHead>
            <TableRow>
              <TableHeader>
                {t("eqa.participant.organization", "Laboratory")}
              </TableHeader>
              <TableHeader>{t("label.status", "Status")}</TableHeader>
              <TableHeader>
                {t("eqa.participant.enrolled", "Enrolled")}
              </TableHeader>
            </TableRow>
          </TableHead>
          <TableBody>
            {participants.map((participant) => (
              <TableRow key={participant.id}>
                <TableCell>{participant.organizationName}</TableCell>
                <TableCell>{participant.status}</TableCell>
                <TableCell>
                  {formatDateOnly(participant.enrollmentDate)}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
      <p style={hintStyle}>
        {t(
          "eqa.provider.wizard.participantsHint",
          "Every active participant of the scheme takes part in the cycle. Suspend an enrolment to leave a laboratory out.",
        )}
      </p>
    </Tile>
  );

  const distributionStep = () => (
    <Tile>
      <RadioButtonGroup
        legendText={t("eqa.cycle.distributionMethod", "Distribution method")}
        name="distribution-method"
        orientation="vertical"
        valueSelected={distributionMethod}
        onChange={setDistributionMethod}
      >
        {DISTRIBUTION_METHODS.map((method) => (
          <RadioButton
            key={method}
            id={`method-${method}`}
            value={method}
            labelText={t(
              `eqa.cycle.distributionMethod.${method.toLowerCase()}`,
              method,
            )}
          />
        ))}
      </RadioButtonGroup>
      <p style={hintStyle}>
        {t(
          "eqa.provider.wizard.methodHint",
          "How results come back and scores go out: wired FHIR, exported files, or both where participants differ.",
        )}
      </p>
    </Tile>
  );

  const confirmStep = () => (
    <Tile>
      <h5>{t("eqa.provider.wizard.summary", "Summary")}</h5>
      <ul>
        <li>
          {t("eqa.provider.scheme", "Scheme")}: {scheme?.name || "—"}
        </li>
        <li>
          {t("eqa.cycle.name", "Cycle name")}: {cycle.cycleName || "—"}
        </li>
        <li>
          {t("eqa.cycle.submissionDeadline", "Submission deadline")}:{" "}
          {formatDateOnly(cycle.plannedEndDate) || "—"}
        </li>
        <li>
          {t("eqa.panel.samples", "Samples")}:{" "}
          {samples.map((sample) => testName(sample.testId)).join(", ")}
        </li>
        <li>
          {t("eqa.prep.participants", "Participants")}: {participants.length}
        </li>
        <li>
          {t("eqa.panel.aliquotsProduced", "Aliquots produced")}:{" "}
          {prep.aliquotsProduced} / {needed}
        </li>
        <li>
          {t("eqa.cycle.distributionMethod", "Distribution method")}:{" "}
          {distributionMethod}
        </li>
      </ul>
      {blockers.length > 0 && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={t(
            "eqa.provider.wizard.blockers",
            "This cycle cannot be created yet",
          )}
          subtitle={blockers.map((id) => t(id, id)).join(" ")}
        />
      )}
      <Button disabled={blockers.length > 0 || busy} onClick={create}>
        {t("eqa.provider.wizard.create", "Create cycle & panel")}
      </Button>
      <p style={hintStyle}>
        {t(
          "eqa.provider.wizard.createHint",
          "The cycle opens in prep so the panel can be aliquoted, QC'd and packed.",
        )}
      </p>
    </Tile>
  );

  // Called, not spread into an array of elements: each step builds only when it
  // is the step in view, so one step's data cannot break the one on screen.
  const stepContent = [
    details,
    panelStep,
    participantStep,
    distributionStep,
    confirmStep,
  ];

  return (
    <>
      {busy && <Loading />}
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <h3>{t("eqa.provider.cycle.new", "New cycle")}</h3>
          {notification && (
            <InlineNotification
              kind={notification.kind}
              lowContrast
              title={notification.message}
              onCloseButtonClick={() => setNotification(null)}
            />
          )}
          <ProgressIndicator
            currentIndex={step}
            onChange={setStep}
            spaceEqually
          >
            {stepTitles.map((title) => (
              <ProgressStep key={title} label={title} />
            ))}
          </ProgressIndicator>
          <div style={{ marginTop: "1rem" }}>{stepContent[step]()}</div>
          <div style={{ marginTop: "1rem" }}>
            <Button
              kind="secondary"
              disabled={step === 0}
              onClick={() => setStep(step - 1)}
            >
              {t("back.action.button", "Back")}
            </Button>{" "}
            <Button
              disabled={step === stepTitles.length - 1}
              onClick={() => setStep(step + 1)}
            >
              {t("next.action.button", "Next")}
            </Button>
          </div>
        </Column>
      </Grid>
    </>
  );
};

export default CycleWizard;
