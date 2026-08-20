import React, { useEffect, useRef, useState } from "react";
import {
  Button,
  Checkbox,
  Column,
  Grid,
  InlineNotification,
  Loading,
  MultiSelect,
  NumberInput,
  ProgressIndicator,
  ProgressStep,
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
import { useHistory } from "react-router-dom";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import {
  createCycle,
  createPanel,
  downloadLabelSheet,
  fetchAnalysts,
  fetchInHouseSchemes,
  fetchLabUsers,
  fetchTests,
  saveAnalystRoster,
  sealAndDistribute,
} from "./inHouseApi";
import {
  ASSIGNMENT_MODES,
  expandForMode,
  modeBlockers,
  prepBlockers,
} from "./blindingRules";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "banner.menu.eqa.inHouse", link: "/qa/eqa/in-house" },
  { label: "eqa.inhouse.wizard.title", link: "/qa/eqa/in-house/new" },
];

// Exactly the EQAPanelSourceType / EQAStorageTemp constants — the server
// rejects anything else, so these lists are not free text.
const SOURCE_TYPES = ["IN_HOUSE_ALIQUOTED", "VENDOR_SOURCED", "MIXED"];
const STORAGE_TEMPS = [
  "AMBIENT",
  "REFRIGERATED_2_8C",
  "FROZEN_MINUS_20C",
  "ULTRA_FROZEN_MINUS_80C",
  "DRY_ICE",
];

// The key mirrors the sample_code the server generates, so the row a user
// filled in and the row on the label sheet read the same.
const emptySample = (index) => ({
  key: `S${String(index).padStart(2, "0")}`,
  testId: "",
  targetValue: "",
  targetUnit: "",
  rangeLow: "",
  rangeHigh: "",
  analystId: null,
});

const failed = (response) =>
  !response || response.error || (response.status && response.status >= 400);

const BlindingWizard = () => {
  const intl = useIntl();
  const history = useHistory();
  const [step, setStep] = useState(0);
  const [busy, setBusy] = useState(false);
  const [notification, setNotification] = useState(null);

  const [schemes, setSchemes] = useState([]);
  const [tests, setTests] = useState([]);
  const [roster, setRoster] = useState([]);
  const [labUsers, setLabUsers] = useState([]);

  const [cycle, setCycle] = useState({
    schemeId: "",
    cycleName: "",
    plannedStartDate: "",
    unblindDate: "",
  });
  const [samples, setSamples] = useState([emptySample(1)]);
  const [prep, setPrep] = useState({
    sourceType: "IN_HOUSE_ALIQUOTED",
    lotNumber: "",
    aliquotsProduced: 1,
    storageTemp: "REFRIGERATED_2_8C",
    expirationDate: "",
    homogeneityQcPassed: false,
    homogeneityQcNotes: "",
  });
  const [assignmentMode, setAssignmentMode] = useState("ROUND_ROBIN");
  const [sealed, setSealed] = useState(null);
  const lastRosterSent = useRef(null);
  // A seal that fails after the cycle is written must not write a second one
  // when the user fixes the input and tries again.
  const createdCycle = useRef(null);

  useEffect(() => {
    fetchInHouseSchemes(setSchemes);
    fetchTests(setTests);
    fetchLabUsers(setLabUsers);
  }, []);

  useEffect(() => {
    if (!cycle.schemeId) {
      setRoster([]);
      return;
    }
    fetchAnalysts(cycle.schemeId, setRoster);
  }, [cycle.schemeId]);

  const step1Ready = cycle.schemeId && cycle.unblindDate;
  // What actually gets sealed: one aliquot per row here, so the prep gate counts
  // the expansion rather than the materials the user typed.
  const assigned = expandForMode(samples, roster, assignmentMode);
  const blockers = [
    ...prepBlockers(assigned, prep),
    ...modeBlockers(roster, assignmentMode),
  ];

  const label = (id, fallback) =>
    intl.formatMessage({ id, defaultMessage: fallback });

  const updateSample = (key, field, value) =>
    setSamples((rows) =>
      rows.map((row) => (row.key === key ? { ...row, [field]: value } : row)),
    );

  // Carbon's MultiSelect fires onChange more than once per pick while it
  // reconciles a controlled selection, and two identical PUTs race each other
  // into uq_eqa_scheme_analyst_scheme_user. Sending a selection only when it
  // differs from the last one sent settles it at the source.
  const setRosterUsers = (selectedIds) => {
    const signature = [...selectedIds].map(String).sort().join(",");
    if (lastRosterSent.current === signature) {
      return;
    }
    lastRosterSent.current = signature;
    saveAnalystRoster(cycle.schemeId, selectedIds, (status) => {
      if (status >= 400 || status === 0) {
        setNotification({
          kind: "error",
          message: label(
            "eqa.inhouse.roster.saveError",
            "Could not save the analyst roster",
          ),
        });
        return;
      }
      fetchAnalysts(cycle.schemeId, setRoster);
    });
  };

  // FR-V2.4-04: cycle, panel and orders are one user action. The panel is
  // written only here, at confirm — a wizard abandoned earlier leaves nothing
  // behind to resume or clean up.
  // ponytail: no draft panels; add a resume path when a lab asks for one.
  const seal = () => {
    setBusy(true);
    setNotification(null);
    const withCycle = (cycleResponse) => {
      if (failed(cycleResponse)) {
        setBusy(false);
        setNotification({
          kind: "error",
          message:
            cycleResponse?.error ||
            label("eqa.inhouse.seal.cycleError", "Could not create the cycle"),
        });
        return;
      }
      createdCycle.current = cycleResponse;
      sealInto(cycleResponse);
    };

    if (createdCycle.current) {
      withCycle(createdCycle.current);
      return;
    }
    createCycle(
      {
        schemeId: cycle.schemeId,
        cycleName: cycle.cycleName,
        plannedStartDate: cycle.plannedStartDate || null,
        plannedEndDate: cycle.unblindDate,
      },
      withCycle,
    );
  };

  const sealInto = (cycleResponse) => {
    createPanel(
      {
        schemeId: cycle.schemeId,
        cycleId: cycleResponse.id,
        panelName:
          cycle.cycleName ||
          `${label("eqa.inhouse.panel.defaultName", "In-house panel")} ${cycleResponse.cycleNumber}`,
        panelType: "IN_HOUSE",
        unblindDate: cycle.unblindDate,
        sourceType: prep.sourceType,
        lotNumber: prep.lotNumber,
        storageTemp: prep.storageTemp,
        expirationDate: prep.expirationDate || null,
        aliquotsProduced: prep.aliquotsProduced,
        homogeneityQcPassed: prep.homogeneityQcPassed,
        homogeneityQcNotes: prep.homogeneityQcNotes,
        samples: assigned.map((sample) => ({
          testId: sample.testId,
          targetValue: sample.targetValue,
          targetUnit: sample.targetUnit,
          acceptanceRangeLow: sample.rangeLow || null,
          acceptanceRangeHigh: sample.rangeHigh || null,
        })),
      },
      (panelResponse) => {
        if (failed(panelResponse)) {
          setBusy(false);
          setNotification({
            kind: "error",
            message:
              panelResponse?.error ||
              label(
                "eqa.inhouse.seal.panelError",
                "Could not create the panel",
              ),
          });
          return;
        }
        const orders = (panelResponse.samples || []).map((created, index) => ({
          panelSampleId: created.id,
          testId: assigned[index]?.testId,
          analystId: assigned[index]?.analystId,
        }));
        sealAndDistribute(panelResponse.id, orders, (sealResponse) => {
          setBusy(false);
          if (failed(sealResponse)) {
            setNotification({
              kind: "error",
              message:
                sealResponse?.error ||
                label("eqa.inhouse.seal.error", "Could not seal the panel"),
            });
            return;
          }
          setSealed(sealResponse);
        });
      },
    );
  };

  const testName = (testId) => {
    const test = tests.find(
      (candidate) => String(candidate.id) === String(testId),
    );
    return test ? test.name || test.value : testId;
  };

  const analystName = (systemUserId) => {
    const analyst = roster.find(
      (candidate) => String(candidate.systemUserId) === String(systemUserId),
    );
    return analyst ? analyst.displayName : "—";
  };

  if (sealed) {
    return (
      <>
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <Tile>
              <h4>
                {label(
                  "eqa.inhouse.sealed.title",
                  "Panel sealed and distributed",
                )}
              </h4>
              <p>
                {label(
                  "eqa.inhouse.sealed.help",
                  "The blinded orders now carry these blind codes as their sample IDs, in result entry and in the Workplan.",
                )}
              </p>
              <ul>
                {(sealed.orderAccessionNumbers || []).map((code) => (
                  <li key={code}>{code}</li>
                ))}
              </ul>
              <Button
                kind="tertiary"
                onClick={() =>
                  downloadLabelSheet(sealed.id, () =>
                    setNotification({
                      kind: "error",
                      message: label(
                        "eqa.inhouse.labels.error",
                        "Could not generate the label sheet",
                      ),
                    }),
                  )
                }
              >
                {label("eqa.inhouse.labels.print", "Print label sheet")}
              </Button>{" "}
              <Button onClick={() => history.push("/qa/eqa/in-house")}>
                {label("eqa.inhouse.sealed.done", "Back to in-house panels")}
              </Button>
              {notification && (
                <InlineNotification
                  kind={notification.kind}
                  title={notification.message}
                  onCloseButtonClick={() => setNotification(null)}
                />
              )}
            </Tile>
          </Column>
        </Grid>
      </>
    );
  }

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      {busy && <Loading />}
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <h3>
            {label("eqa.inhouse.wizard.title", "Create in-house blinded panel")}
          </h3>
          <ProgressIndicator currentIndex={step} spaceEqually>
            <ProgressStep
              label={label("eqa.inhouse.step1", "Scheme & cycle")}
            />
            <ProgressStep
              label={label("eqa.inhouse.step2", "Samples & prep")}
            />
            <ProgressStep label={label("eqa.inhouse.step3", "Analysts")} />
            <ProgressStep
              label={label("eqa.inhouse.step4", "Confirm & seal")}
            />
          </ProgressIndicator>
        </Column>

        {notification && (
          <Column lg={16} md={8} sm={4}>
            <InlineNotification
              kind={notification.kind}
              title={notification.message}
              onCloseButtonClick={() => setNotification(null)}
            />
          </Column>
        )}

        {step === 0 && (
          <Column lg={16} md={8} sm={4}>
            <Select
              id="inhouse-scheme"
              labelText={label("eqa.inhouse.scheme", "In-house scheme")}
              value={cycle.schemeId}
              onChange={(e) => setCycle({ ...cycle, schemeId: e.target.value })}
            >
              <SelectItem value="" text="" />
              {schemes.map((scheme) => (
                <SelectItem
                  key={scheme.id}
                  value={scheme.id}
                  text={scheme.name}
                />
              ))}
            </Select>
            <TextInput
              id="inhouse-cycle-name"
              labelText={label("eqa.inhouse.cycleName", "Cycle name")}
              value={cycle.cycleName}
              onChange={(e) =>
                setCycle({ ...cycle, cycleName: e.target.value })
              }
            />
            <TextInput
              id="inhouse-start"
              type="date"
              labelText={label(
                "eqa.inhouse.plannedStart",
                "Planned start date",
              )}
              value={cycle.plannedStartDate}
              onChange={(e) =>
                setCycle({ ...cycle, plannedStartDate: e.target.value })
              }
            />
            <TextInput
              id="inhouse-unblind"
              type="date"
              labelText={label(
                "eqa.inhouse.unblindDate",
                "Unblind date (submission deadline)",
              )}
              value={cycle.unblindDate}
              onChange={(e) =>
                setCycle({ ...cycle, unblindDate: e.target.value })
              }
            />
          </Column>
        )}

        {step === 1 && (
          <Column lg={16} md={8} sm={4}>
            <p>
              {intl.formatMessage(
                {
                  id: "eqa.inhouse.samplesLeadIn",
                  defaultMessage:
                    "{count, plural, one {# sample} other {# samples}} defined. Target values are sealed when the wizard finishes; they are never stored in clear text.",
                },
                { count: samples.length },
              )}
            </p>
            <Table size="sm">
              <TableHead>
                <TableRow>
                  <TableHeader>
                    {label("eqa.inhouse.sample", "Sample")}
                  </TableHeader>
                  <TableHeader>{label("eqa.inhouse.test", "Test")}</TableHeader>
                  <TableHeader>
                    {label("eqa.inhouse.target", "Target value")}
                  </TableHeader>
                  <TableHeader>{label("eqa.inhouse.unit", "Unit")}</TableHeader>
                  <TableHeader>
                    {label("eqa.inhouse.rangeLow", "Range low")}
                  </TableHeader>
                  <TableHeader>
                    {label("eqa.inhouse.rangeHigh", "Range high")}
                  </TableHeader>
                </TableRow>
              </TableHead>
              <TableBody>
                {samples.map((sample) => (
                  <TableRow key={sample.key}>
                    <TableCell>{sample.key}</TableCell>
                    <TableCell>
                      <Select
                        id={`test-${sample.key}`}
                        labelText=""
                        hideLabel
                        value={sample.testId}
                        onChange={(e) =>
                          updateSample(sample.key, "testId", e.target.value)
                        }
                      >
                        <SelectItem value="" text="" />
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
                        id={`target-${sample.key}`}
                        labelText=""
                        hideLabel
                        value={sample.targetValue}
                        onChange={(e) =>
                          updateSample(
                            sample.key,
                            "targetValue",
                            e.target.value,
                          )
                        }
                      />
                    </TableCell>
                    <TableCell>
                      <TextInput
                        id={`unit-${sample.key}`}
                        labelText=""
                        hideLabel
                        value={sample.targetUnit}
                        onChange={(e) =>
                          updateSample(sample.key, "targetUnit", e.target.value)
                        }
                      />
                    </TableCell>
                    <TableCell>
                      <TextInput
                        id={`low-${sample.key}`}
                        labelText=""
                        hideLabel
                        value={sample.rangeLow}
                        onChange={(e) =>
                          updateSample(sample.key, "rangeLow", e.target.value)
                        }
                      />
                    </TableCell>
                    <TableCell>
                      <TextInput
                        id={`high-${sample.key}`}
                        labelText=""
                        hideLabel
                        value={sample.rangeHigh}
                        onChange={(e) =>
                          updateSample(sample.key, "rangeHigh", e.target.value)
                        }
                      />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            <Button
              kind="ghost"
              onClick={() =>
                setSamples((rows) => [...rows, emptySample(rows.length + 1)])
              }
            >
              {label("eqa.inhouse.addSample", "Add sample")}
            </Button>

            <h5 style={{ marginTop: "1.5rem" }}>
              {label("eqa.inhouse.prep.title", "Prep & aliquoting details")}
            </h5>
            <p>
              {label(
                "eqa.inhouse.prep.subtitle",
                "The same panel-inventory fields the provider side records (FR-V2.1-17), so in-house and provider prep read alike.",
              )}
            </p>
            {/* The requirement, stated before the user trips it. One aliquot per
                blinded sample: the analyst multiplier the FRS writes is already
                in the row count, since distribution creates one order per
                sample. */}
            <p>
              {intl.formatMessage(
                {
                  id: "eqa.inhouse.prep.required",
                  defaultMessage:
                    "Required: {samples, plural, one {# sample} other {# samples}} = {samples, plural, one {# aliquot} other {# aliquots}} minimum",
                },
                { samples: samples.length },
              )}
            </p>

            <Select
              id="inhouse-source"
              labelText={label("eqa.inhouse.prep.sourceType", "Source")}
              value={prep.sourceType}
              onChange={(e) => setPrep({ ...prep, sourceType: e.target.value })}
            >
              {SOURCE_TYPES.map((type) => (
                <SelectItem
                  key={type}
                  value={type}
                  // eqa.panel.* is the shared panel vocabulary the provider
                  // wizard renders too (T-24); lower-cased like every other
                  // enum-derived key in this module.
                  text={label(
                    `eqa.panel.source.${type.toLowerCase()}`,
                    type.replace(/_/g, " "),
                  )}
                />
              ))}
            </Select>
            <TextInput
              id="inhouse-lot"
              labelText={label("eqa.inhouse.prep.lotNumber", "Lot number")}
              value={prep.lotNumber}
              onChange={(e) => setPrep({ ...prep, lotNumber: e.target.value })}
            />
            <NumberInput
              id="inhouse-aliquots"
              min={0}
              label={label("eqa.inhouse.prep.aliquots", "Aliquots produced")}
              value={prep.aliquotsProduced}
              onChange={(e, { value }) =>
                setPrep({ ...prep, aliquotsProduced: value })
              }
            />
            <Select
              id="inhouse-storage"
              labelText={label(
                "eqa.inhouse.prep.storageTemp",
                "Storage temperature",
              )}
              value={prep.storageTemp}
              onChange={(e) =>
                setPrep({ ...prep, storageTemp: e.target.value })
              }
            >
              {STORAGE_TEMPS.map((temp) => (
                <SelectItem
                  key={temp}
                  value={temp}
                  text={label(
                    `eqa.panel.storage.${temp.toLowerCase()}`,
                    temp.replace(/_/g, " "),
                  )}
                />
              ))}
            </Select>
            <TextInput
              id="inhouse-expiry"
              type="date"
              labelText={label(
                "eqa.inhouse.prep.expiration",
                "Expiration date",
              )}
              value={prep.expirationDate}
              onChange={(e) =>
                setPrep({ ...prep, expirationDate: e.target.value })
              }
            />
            <Checkbox
              id="inhouse-homogeneity"
              labelText={label(
                "eqa.inhouse.prep.homogeneity",
                "Homogeneity QC passed",
              )}
              checked={prep.homogeneityQcPassed}
              onChange={(e, { checked }) =>
                setPrep({ ...prep, homogeneityQcPassed: checked })
              }
            />
            <TextArea
              id="inhouse-homogeneity-notes"
              labelText={label(
                "eqa.inhouse.prep.homogeneityNotes",
                "Homogeneity QC notes",
              )}
              value={prep.homogeneityQcNotes}
              onChange={(e) =>
                setPrep({ ...prep, homogeneityQcNotes: e.target.value })
              }
            />

            {blockers.map((blocker) => (
              <InlineNotification
                key={blocker}
                kind="warning"
                lowContrast
                hideCloseButton
                title={label(blocker, blocker)}
                subtitle={
                  blocker === "eqa.inhouse.gate.aliquots"
                    ? `${samples.length} / ${prep.aliquotsProduced || 0}`
                    : ""
                }
              />
            ))}
          </Column>
        )}

        {step === 2 && (
          <Column lg={16} md={8} sm={4}>
            <MultiSelect
              id="inhouse-roster"
              titleText={label("eqa.inhouse.roster", "Scheme analysts")}
              label={label("eqa.inhouse.roster.pick", "Select analysts")}
              items={labUsers}
              itemToString={(user) =>
                user ? user.displayName || user.loginName : ""
              }
              // Controlled, not initialSelectedItems: the roster arrives after
              // mount, and a stale-empty selection would make the next pick a
              // full replace that silently drops the analysts already on the
              // scheme.
              selectedItems={labUsers.filter((user) =>
                roster.some(
                  (analyst) => String(analyst.systemUserId) === String(user.id),
                ),
              )}
              onChange={({ selectedItems }) =>
                setRosterUsers((selectedItems || []).map((user) => user.id))
              }
            />
            <Select
              id="inhouse-assignment-mode"
              labelText={label("eqa.inhouse.assign.mode", "Assignment mode")}
              value={assignmentMode}
              onChange={(e) => {
                // Manual starts from the deal the user was just looking at,
                // rather than emptying every row the moment they take control.
                if (e.target.value === "MANUAL") {
                  setSamples(expandForMode(samples, roster, "ROUND_ROBIN"));
                }
                setAssignmentMode(e.target.value);
              }}
            >
              {ASSIGNMENT_MODES.map((mode) => (
                <SelectItem
                  key={mode}
                  value={mode}
                  text={label(`eqa.inhouse.assign.${mode}`, mode)}
                />
              ))}
            </Select>
            {assignmentMode === "IDENTICAL" && (
              <p>
                {intl.formatMessage(
                  {
                    id: "eqa.inhouse.assign.identicalNote",
                    defaultMessage:
                      "Every analyst runs every sample: {materials, plural, one {# sample} other {# samples}} × {analysts, plural, one {# analyst} other {# analysts}} = {aliquots, plural, one {# blinded aliquot} other {# blinded aliquots}}, each with its own blind code.",
                  },
                  {
                    materials: samples.length,
                    analysts: roster.length,
                    aliquots: assigned.length,
                  },
                )}
              </p>
            )}
            <p>
              {label(
                "eqa.inhouse.assign.footnote",
                "Each assignment is recorded on the blinded order it creates, and follows that order through result entry.",
              )}
            </p>
            <Table size="sm">
              <TableHead>
                <TableRow>
                  <TableHeader>
                    {label("eqa.inhouse.sample", "Sample")}
                  </TableHeader>
                  <TableHeader>{label("eqa.inhouse.test", "Test")}</TableHeader>
                  <TableHeader>
                    {label("eqa.inhouse.analyst", "Analyst")}
                  </TableHeader>
                </TableRow>
              </TableHead>
              <TableBody>
                {assigned.map((sample) => (
                  <TableRow key={sample.key}>
                    {/* The material the aliquot came from. The blind code the
                        analyst will see is generated server-side at seal, so the
                        internal expansion key stays out of the UI. */}
                    <TableCell>{sample.materialKey || sample.key}</TableCell>
                    <TableCell>{testName(sample.testId)}</TableCell>
                    <TableCell>
                      {assignmentMode === "MANUAL" ? (
                        <Select
                          id={`analyst-${sample.key}`}
                          labelText=""
                          hideLabel
                          value={sample.analystId || ""}
                          onChange={(e) =>
                            updateSample(
                              sample.key,
                              "analystId",
                              e.target.value,
                            )
                          }
                        >
                          <SelectItem value="" text="" />
                          {roster.map((analyst) => (
                            <SelectItem
                              key={analyst.systemUserId}
                              value={analyst.systemUserId}
                              text={analyst.displayName}
                            />
                          ))}
                        </Select>
                      ) : (
                        analystName(sample.analystId)
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Column>
        )}

        {step === 3 && (
          <Column lg={16} md={8} sm={4}>
            {/* Stated before the irreversible click, and only what is true of
                this implementation: there is no post-seal edit path. */}
            <InlineNotification
              kind="warning"
              lowContrast
              hideCloseButton
              title={label(
                "eqa.inhouse.confirm.sealWarning",
                "Sealing encrypts the target values",
              )}
              subtitle={label(
                "eqa.inhouse.confirm.sealWarningBody",
                "After sealing, revealing a target takes the unblind privilege and is recorded in the audit trail. The panel cannot be edited afterwards.",
              )}
            />
            <Tile>
              <p>
                {label("eqa.inhouse.scheme", "In-house scheme")}:{" "}
                {schemes.find(
                  (scheme) => String(scheme.id) === String(cycle.schemeId),
                )?.name || "—"}
              </p>
              <p>
                {label("eqa.inhouse.cycle", "Cycle")}: {cycle.cycleName || "—"}
              </p>
              <p>
                {label("eqa.inhouse.confirm.samples", "Samples")}:{" "}
                {samples.length}
              </p>
              <p>
                {label("eqa.inhouse.confirm.aliquotRows", "Blinded aliquots")}:{" "}
                {assigned.length}
              </p>
              <p>
                {label("eqa.inhouse.prep.aliquots", "Aliquots produced")}:{" "}
                {prep.aliquotsProduced}
              </p>
              <p>
                {label("eqa.inhouse.prep.lotNumber", "Lot number")}:{" "}
                {prep.lotNumber || "—"}
              </p>
              <p>
                {label(
                  "eqa.inhouse.unblindDate",
                  "Unblind date (submission deadline)",
                )}
                : {cycle.unblindDate}
              </p>
              <p>
                {label("eqa.inhouse.prep.homogeneity", "Homogeneity QC passed")}
                : {prep.homogeneityQcPassed ? "✓" : prep.homogeneityQcNotes}
              </p>
              <p>
                {label(
                  "eqa.inhouse.confirm.labelNote",
                  "Label sheets carry the blind code, cycle and analyte only — never a target value.",
                )}
              </p>
              <p>
                {label(
                  "eqa.inhouse.confirm.afterSeal",
                  "Sealing creates one blinded order per aliquot. They appear in result entry and the Workplan under their blind codes, like any other order.",
                )}
              </p>
            </Tile>
          </Column>
        )}

        <Column lg={16} md={8} sm={4}>
          {step > 0 && (
            <Button kind="secondary" onClick={() => setStep(step - 1)}>
              {label("eqa.inhouse.back", "Back")}
            </Button>
          )}{" "}
          {step < 3 && (
            <Button
              disabled={
                (step === 0 && !step1Ready) ||
                (step === 1 && blockers.length > 0)
              }
              onClick={() => setStep(step + 1)}
            >
              {label("eqa.inhouse.next", "Next")}
            </Button>
          )}
          {step === 3 && (
            <Button disabled={busy || blockers.length > 0} onClick={seal}>
              {label("eqa.inhouse.seal", "Seal panel & distribute")}
            </Button>
          )}{" "}
          <Button kind="ghost" onClick={() => history.push("/qa/eqa/in-house")}>
            {label("eqa.inhouse.cancel", "Cancel")}
          </Button>
        </Column>
      </Grid>
    </>
  );
};

export default BlindingWizard;
