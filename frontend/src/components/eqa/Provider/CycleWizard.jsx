import React, { useEffect, useState } from "react";
import {
  Button,
  Column,
  DatePicker,
  DatePickerInput,
  FilterableMultiSelect,
  Grid,
  Heading,
  InlineNotification,
  Loading,
  NumberInput,
  ProgressIndicator,
  RadioButton,
  RadioButtonGroup,
  ProgressStep,
  Section,
  Select,
  SelectItem,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  TextInput,
  Tile,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { useHistory, useParams } from "react-router-dom";
import {
  getFromOpenElisServer,
  resolveApiErrorMessage,
} from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { hintStyle } from "../eqaCommon";
import { createProviderCycle } from "./Workbench/workbenchApi";
// The same test list T-21's in-house wizard picks from: the standard catalog
// narrowed to tests that carry an analyte, since a panel target is stored
// against one. One seam for both wizards rather than a second copy.
import { fetchTests } from "../eqaApi";

/** FR-V2.1-17's vocabularies, as the server spells them. */
const SOURCE_TYPES = ["IN_HOUSE_ALIQUOTED", "VENDOR_SOURCED", "MIXED"];
// Exactly EQADistributionMethod; the server refuses anything else.
const DISTRIBUTION_METHODS = ["FHIR", "CSV", "MIXED"];

const STORAGE_TEMPS = [
  "AMBIENT",
  "REFRIGERATED_2_8C",
  "FROZEN_MINUS_20C",
  "ULTRA_FROZEN_MINUS_80C",
  "DRY_ICE",
];

const emptySample = () => ({
  sampleCode: "",
  testId: "",
  targetValue: "",
  targetUnit: "",
  acceptanceRangeLow: "",
  acceptanceRangeHigh: "",
});

/**
 * Panel definition + cycle creation wizard (FR-V2.5-02): cycle details → panel
 * samples + source (with its cold chain) → participants → distribution method →
 * confirm & begin prep.
 *
 * Nothing is written until the last step. The whole cycle — cycle row, panel,
 * panel samples, participant roster — is one POST, so a refusal leaves nothing
 * behind and the operator can correct the step that caused it rather than
 * cleaning up a half-created cycle.
 */
const CycleWizard = () => {
  const intl = useIntl();
  const history = useHistory();
  const { schemeId } = useParams();
  const t = (id, defaultMessage, values) =>
    intl.formatMessage({ id, defaultMessage }, values);

  const [step, setStep] = useState(0);
  const [scheme, setScheme] = useState(null);
  const [tests, setTests] = useState([]);
  const [organizations, setOrganizations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState(null);
  const [saving, setSaving] = useState(false);

  const [cycleName, setCycleName] = useState("");
  const [cycleNumber, setCycleNumber] = useState("");
  const [plannedStartDate, setPlannedStartDate] = useState("");
  const [plannedEndDate, setPlannedEndDate] = useState("");
  const [panelName, setPanelName] = useState("");
  const [sourceType, setSourceType] = useState(SOURCE_TYPES[0]);
  const [lotNumber, setLotNumber] = useState("");
  const [vendorName, setVendorName] = useState("");
  const [vendorLot, setVendorLot] = useState("");
  const [vendorCertificateRef, setVendorCertificateRef] = useState("");
  const [samples, setSamples] = useState([emptySample()]);
  const [selectedOrgs, setSelectedOrgs] = useState([]);
  const [storageTemp, setStorageTemp] = useState("");
  const [distributionMethod, setDistributionMethod] = useState("FHIR");
  const [expirationDate, setExpirationDate] = useState("");

  useEffect(() => {
    getFromOpenElisServer(`/rest/eqa/programs/${schemeId}`, (data) => {
      setScheme(data || null);
      setLoading(false);
    });
    fetchTests(setTests);
    getFromOpenElisServer(
      `/rest/eqa/programs/${schemeId}/enrollments`,
      (data) =>
        setOrganizations(
          (data || [])
            .filter((e) => e.status === "Active" && e.organizationId != null)
            .map((e) => ({
              id: String(e.organizationId),
              name: e.organizationName || String(e.organizationId),
            })),
        ),
    );
  }, [schemeId]);

  const setSample = (index, patch) =>
    setSamples((prev) =>
      prev.map((sample, i) => (i === index ? { ...sample, ...patch } : sample)),
    );

  // A vendor-sourced panel must carry the vendor's provenance (FR-V2.1-17); the
  // server refuses it too, this only saves the operator a round trip.
  const vendorRequired = sourceType !== "IN_HOUSE_ALIQUOTED";
  const samplesComplete = samples.every(
    (sample) => sample.sampleCode.trim() && sample.testId,
  );
  // Per step, what must be filled in to move on. Step 1 is the only one the
  // server can reject outright, so it is the only one gated hard here; cycle
  // details and distribution are all optional columns.
  const canAdvance = [
    true,
    !!panelName.trim() &&
      samplesComplete &&
      (!vendorRequired || !!vendorName.trim()),
    selectedOrgs.length > 0,
    true,
    true,
  ][step];

  const handleSubmit = () => {
    setSaving(true);
    createProviderCycle(
      {
        schemeId: Number(schemeId),
        cycleNumber: cycleNumber === "" ? null : Number(cycleNumber),
        cycleName,
        plannedStartDate,
        plannedEndDate,
        panelName,
        sourceType,
        lotNumber,
        vendorName: vendorRequired ? vendorName : null,
        vendorLot: vendorRequired ? vendorLot : null,
        vendorCertificateRef: vendorRequired ? vendorCertificateRef : null,
        samples: samples.map((sample) => ({
          sampleCode: sample.sampleCode,
          testId: sample.testId,
          targetValue: sample.targetValue,
          targetUnit: sample.targetUnit,
          acceptanceRangeLow: sample.acceptanceRangeLow,
          acceptanceRangeHigh: sample.acceptanceRangeHigh,
        })),
        participantOrganizationIds: selectedOrgs.map((o) => Number(o.id)),
        storageTemp,
        expirationDate,
        distributionMethod,
      },
      ({ ok, body }) => {
        setSaving(false);
        if (ok && body?.id) {
          history.push(`/qa/eqa/provider/cycles/${body.id}/workbench`);
          return;
        }
        // The server's own refusal — a repeated sample code, a lab that is not
        // enrolled — names the field to fix, so it is shown verbatim.
        setNotice({
          kind: "error",
          text: resolveApiErrorMessage(
            intl,
            body,
            "eqa.provider.wizard.failed",
          ),
        });
      },
    );
  };

  if (loading) {
    return <Loading />;
  }

  return (
    <>
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          {
            label: "banner.menu.eqa.provider",
            link: "/qa/eqa/provider/schemes",
          },
          {
            label: "eqa.provider.wizard.title",
            link: `/qa/eqa/provider/schemes/${schemeId}/cycles/new`,
          },
        ]}
      />
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              {t("eqa.provider.wizard.forScheme", "New cycle — {name}", {
                name: scheme?.name || `#${schemeId}`,
              })}
            </Heading>
          </Section>
          {notice && (
            <InlineNotification
              kind={notice.kind}
              lowContrast
              title={notice.text}
              onCloseButtonClick={() => setNotice(null)}
            />
          )}
          <ProgressIndicator
            currentIndex={step}
            spaceEqually
            style={{ margin: "1rem 0" }}
          >
            <ProgressStep
              label={t("eqa.provider.wizard.step.cycle", "Cycle")}
            />
            <ProgressStep
              label={t("eqa.provider.wizard.step.panel", "Panel")}
            />
            <ProgressStep
              label={t("eqa.distribution.step.participants", "Participants")}
            />
            <ProgressStep
              label={t(
                "eqa.provider.wizard.step.distribution",
                "Distribution method",
              )}
            />
            <ProgressStep
              label={t("eqa.distribution.step.confirmation", "Confirmation")}
            />
          </ProgressIndicator>
        </Column>
      </Grid>

      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          {step === 0 && (
            <Grid condensed>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="cycle-name"
                  labelText={t("eqa.provider.wizard.cycleName", "Cycle name")}
                  value={cycleName}
                  onChange={(e) => setCycleName(e.target.value)}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <NumberInput
                  id="cycle-number"
                  min={1}
                  allowEmpty
                  value={cycleNumber}
                  label={t("eqa.provider.wizard.cycleNumber", "Cycle number")}
                  helperText={t(
                    "eqa.provider.wizard.cycleNumber.hint",
                    "Left blank, the next unused number for this scheme is used.",
                  )}
                  onChange={(_e, { value }) => setCycleNumber(value)}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <DatePicker
                  datePickerType="range"
                  dateFormat="d/m/Y"
                  onChange={([from, to]) => {
                    setPlannedStartDate(from ? toIsoDate(from) : "");
                    setPlannedEndDate(to ? toIsoDate(to) : "");
                  }}
                >
                  <DatePickerInput
                    id="cycle-planned-start"
                    labelText={t(
                      "eqa.provider.wizard.distributionDate",
                      "Distribution date",
                    )}
                    placeholder="dd/mm/yyyy"
                  />
                  <DatePickerInput
                    id="cycle-planned-end"
                    labelText={t(
                      "eqa.provider.wizard.submissionDeadline",
                      "Submission deadline",
                    )}
                    placeholder="dd/mm/yyyy"
                  />
                </DatePicker>
              </Column>
            </Grid>
          )}

          {step === 1 && (
            <>
              <Grid condensed>
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="panel-name"
                    labelText={t("eqa.provider.wizard.panelName", "Panel name")}
                    value={panelName}
                    onChange={(e) => setPanelName(e.target.value)}
                  />
                </Column>
                <Column lg={4} md={4} sm={4}>
                  <Select
                    id="panel-source"
                    labelText={t(
                      "eqa.provider.wizard.source",
                      "Material source",
                    )}
                    value={sourceType}
                    onChange={(e) => setSourceType(e.target.value)}
                  >
                    {SOURCE_TYPES.map((value) => (
                      <SelectItem
                        key={value}
                        value={value}
                        text={t(
                          `eqa.panel.source.${value.toLowerCase()}`,
                          value.replace(/_/g, " "),
                        )}
                      />
                    ))}
                  </Select>
                </Column>
                <Column lg={4} md={4} sm={4}>
                  <TextInput
                    id="panel-lot"
                    labelText={t("eqa.provider.wizard.lot", "Lot number")}
                    value={lotNumber}
                    onChange={(e) => setLotNumber(e.target.value)}
                  />
                </Column>
                {vendorRequired && (
                  <>
                    <Column lg={5} md={4} sm={4}>
                      <TextInput
                        id="panel-vendor"
                        labelText={t(
                          "eqa.provider.wizard.vendor",
                          "Vendor name",
                        )}
                        value={vendorName}
                        onChange={(e) => setVendorName(e.target.value)}
                      />
                    </Column>
                    <Column lg={5} md={4} sm={4}>
                      <TextInput
                        id="panel-vendor-lot"
                        labelText={t(
                          "eqa.provider.wizard.vendorLot",
                          "Vendor lot",
                        )}
                        value={vendorLot}
                        onChange={(e) => setVendorLot(e.target.value)}
                      />
                    </Column>
                    <Column lg={6} md={8} sm={4}>
                      <TextInput
                        id="panel-vendor-cert"
                        labelText={t(
                          "eqa.provider.wizard.vendorCertificate",
                          "Vendor certificate reference",
                        )}
                        value={vendorCertificateRef}
                        onChange={(e) =>
                          setVendorCertificateRef(e.target.value)
                        }
                      />
                    </Column>
                  </>
                )}
              </Grid>

              <Table size="sm" style={{ marginTop: "1rem" }}>
                <TableHead>
                  <TableRow>
                    <TableHeader>
                      {t("eqa.provider.wizard.sampleCode", "Sample code")}
                    </TableHeader>
                    <TableHeader>
                      {t("eqa.provider.wizard.test", "Test")}
                    </TableHeader>
                    <TableHeader>
                      {t("eqa.provider.wizard.target", "Target value")}
                    </TableHeader>
                    <TableHeader>
                      {t("eqa.provider.wizard.unit", "Unit")}
                    </TableHeader>
                    <TableHeader>
                      {t("eqa.provider.wizard.rangeLow", "Acceptance low")}
                    </TableHeader>
                    <TableHeader>
                      {t("eqa.provider.wizard.rangeHigh", "Acceptance high")}
                    </TableHeader>
                    <TableHeader />
                  </TableRow>
                </TableHead>
                <TableBody>
                  {samples.map((sample, index) => (
                    <TableRow key={index}>
                      <TableCell>
                        <TextInput
                          id={`sample-code-${index}`}
                          labelText={t(
                            "eqa.provider.wizard.sampleCode",
                            "Sample code",
                          )}
                          hideLabel
                          value={sample.sampleCode}
                          onChange={(e) =>
                            setSample(index, { sampleCode: e.target.value })
                          }
                        />
                      </TableCell>
                      <TableCell>
                        <Select
                          id={`sample-test-${index}`}
                          labelText={t("eqa.provider.wizard.test", "Test")}
                          hideLabel
                          value={sample.testId}
                          onChange={(e) =>
                            setSample(index, { testId: e.target.value })
                          }
                        >
                          <SelectItem
                            value=""
                            text={t(
                              "eqa.provider.wizard.test.select",
                              "Select a test",
                            )}
                          />
                          {/* /rest/test-list carries the display name in `value`
                              on some rows and `name` on others — same fallback
                              T-21's wizard uses, or the option renders blank. */}
                          {tests.map((test) => (
                            <SelectItem
                              key={test.id}
                              value={String(test.id)}
                              text={test.name || test.value}
                            />
                          ))}
                        </Select>
                      </TableCell>
                      <TableCell>
                        <TextInput
                          id={`sample-target-${index}`}
                          labelText={t(
                            "eqa.provider.wizard.target",
                            "Target value",
                          )}
                          hideLabel
                          value={sample.targetValue}
                          onChange={(e) =>
                            setSample(index, { targetValue: e.target.value })
                          }
                        />
                      </TableCell>
                      <TableCell>
                        <TextInput
                          id={`sample-unit-${index}`}
                          labelText={t("eqa.provider.wizard.unit", "Unit")}
                          hideLabel
                          value={sample.targetUnit}
                          onChange={(e) =>
                            setSample(index, { targetUnit: e.target.value })
                          }
                        />
                      </TableCell>
                      <TableCell>
                        <TextInput
                          id={`sample-low-${index}`}
                          labelText={t(
                            "eqa.provider.wizard.rangeLow",
                            "Acceptance low",
                          )}
                          hideLabel
                          value={sample.acceptanceRangeLow}
                          onChange={(e) =>
                            setSample(index, {
                              acceptanceRangeLow: e.target.value,
                            })
                          }
                        />
                      </TableCell>
                      <TableCell>
                        <TextInput
                          id={`sample-high-${index}`}
                          labelText={t(
                            "eqa.provider.wizard.rangeHigh",
                            "Acceptance high",
                          )}
                          hideLabel
                          value={sample.acceptanceRangeHigh}
                          onChange={(e) =>
                            setSample(index, {
                              acceptanceRangeHigh: e.target.value,
                            })
                          }
                        />
                      </TableCell>
                      <TableCell>
                        <Button
                          kind="ghost"
                          size="sm"
                          disabled={samples.length === 1}
                          onClick={() =>
                            setSamples(samples.filter((_s, i) => i !== index))
                          }
                        >
                          {t("eqa.provider.wizard.removeSample", "Remove")}
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <Button
                kind="tertiary"
                size="sm"
                style={{ marginTop: "0.5rem" }}
                onClick={() => setSamples([...samples, emptySample()])}
              >
                {t("eqa.provider.wizard.addSample", "Add sample")}
              </Button>
              <p style={{ ...hintStyle, marginTop: "0.5rem" }}>
                {t(
                  "eqa.provider.wizard.target.hint",
                  "A target value is sealed and encrypted at rest; leave it blank when the target is only known at scoring time.",
                )}
              </p>
              <Grid condensed>
                <Column lg={8} md={4} sm={4}>
                  <Select
                    id="panel-storage-temp"
                    labelText={t(
                      "eqa.provider.wizard.storageTemp",
                      "Storage temperature",
                    )}
                    helperText={t(
                      "eqa.provider.wizard.storageTemp.hint",
                      "Becomes each shipping box's temperature requirement.",
                    )}
                    value={storageTemp}
                    onChange={(e) => setStorageTemp(e.target.value)}
                  >
                    <SelectItem
                      value=""
                      text={t(
                        "eqa.provider.wizard.storageTemp.select",
                        "Select a temperature",
                      )}
                    />
                    {STORAGE_TEMPS.map((value) => (
                      <SelectItem
                        key={value}
                        value={value}
                        text={t(
                          `eqa.panel.storage.${value.toLowerCase()}`,
                          value.replace(/_/g, " "),
                        )}
                      />
                    ))}
                  </Select>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <DatePicker
                    datePickerType="single"
                    dateFormat="d/m/Y"
                    onChange={([date]) =>
                      setExpirationDate(date ? toIsoDate(date) : "")
                    }
                  >
                    <DatePickerInput
                      id="panel-expiration"
                      labelText={t(
                        "eqa.provider.wizard.expiration",
                        "Material expiry",
                      )}
                      placeholder="dd/mm/yyyy"
                    />
                  </DatePicker>
                </Column>
              </Grid>
            </>
          )}

          {/* FR-V2.5-02 step 4: how the cycle reaches its participants and
              returns their scores. The receipt monitor and score distribution
              read this to decide how each participant is served. */}
          {step === 3 && (
            <Tile>
              <RadioButtonGroup
                legendText={t(
                  "eqa.cycle.distributionMethod",
                  "Distribution method",
                )}
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
              <p style={{ ...hintStyle, marginTop: "0.5rem" }}>
                {t(
                  "eqa.cycle.distributionMethod.hint",
                  "Wired FHIR, exported files, or both where participants differ. The panel's cold chain is recorded with its source in step 2.",
                )}
              </p>
            </Tile>
          )}

          {step === 2 && (
            <>
              <FilterableMultiSelect
                id="cycle-participants"
                titleText={t(
                  "eqa.provider.wizard.participants",
                  "Participating laboratories",
                )}
                helperText={t(
                  "eqa.provider.wizard.participants.hint",
                  "Only labs actively enrolled in this scheme can take part, and only those chosen here are counted by the prep gate.",
                )}
                placeholder={t(
                  "eqa.distribution.participants.select",
                  "Select participants",
                )}
                items={organizations}
                itemToString={(item) => (item ? item.name || item.id : "")}
                onChange={({ selectedItems }) => setSelectedOrgs(selectedItems)}
                selectionFeedback="top-after-reopen"
                disabled={organizations.length === 0}
              />
              {organizations.length === 0 && (
                <InlineNotification
                  kind="warning"
                  lowContrast
                  hideCloseButton
                  title={t(
                    "eqa.distribution.participants.none",
                    "No laboratory is actively enrolled in this scheme yet.",
                  )}
                />
              )}
            </>
          )}

          {step === 4 && (
            <Tile>
              <Summary
                label={t("eqa.provider.wizard.cycleName", "Cycle name")}
                value={cycleName || "—"}
              />
              <Summary
                label={t("eqa.provider.wizard.panelName", "Panel name")}
                value={panelName}
              />
              <Summary
                label={t("eqa.provider.wizard.sampleCount", "Panel samples")}
                value={samples.length}
              />
              <Summary
                label={t(
                  "eqa.provider.wizard.participants",
                  "Participating laboratories",
                )}
                value={selectedOrgs.map((o) => o.name).join(", ")}
              />
              <Summary
                label={t(
                  "eqa.provider.wizard.storageTemp",
                  "Storage temperature",
                )}
                value={
                  storageTemp
                    ? t(
                        `eqa.panel.storage.${storageTemp.toLowerCase()}`,
                        storageTemp.replace(/_/g, " "),
                      )
                    : "—"
                }
              />
              <Summary
                label={t("eqa.cycle.distributionMethod", "Distribution method")}
                value={t(
                  `eqa.cycle.distributionMethod.${distributionMethod.toLowerCase()}`,
                  distributionMethod,
                )}
              />
              <p style={{ ...hintStyle, marginTop: "0.75rem" }}>
                {t(
                  "eqa.provider.wizard.confirm.hint",
                  "Creating the cycle moves it straight into prep, where aliquot counts and homogeneity QC are recorded.",
                )}
              </p>
            </Tile>
          )}
        </Column>
      </Grid>

      <Grid fullWidth>
        <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
          {step > 0 && (
            <Button
              kind="secondary"
              onClick={() => setStep(step - 1)}
              style={{ marginRight: "0.5rem" }}
            >
              {t("back.action.button", "Back")}
            </Button>
          )}
          {step < 4 ? (
            <Button disabled={!canAdvance} onClick={() => setStep(step + 1)}>
              {t("next.action.button", "Next")}
            </Button>
          ) : (
            <Button disabled={saving} onClick={handleSubmit}>
              {t("eqa.provider.wizard.create", "Create cycle and begin prep")}
            </Button>
          )}
        </Column>
      </Grid>
    </>
  );
};

const Summary = ({ label, value }) => (
  <p style={{ margin: "0.25rem 0" }}>
    <strong>{label}:</strong> {value}
  </p>
);

/** Local calendar date, not UTC — toISOString would shift it a day west. */
const toIsoDate = (date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(
    date.getDate(),
  ).padStart(2, "0")}`;

export default CycleWizard;
