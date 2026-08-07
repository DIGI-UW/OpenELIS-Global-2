import React, { useEffect, useMemo, useRef, useState } from "react";
import { Add } from "@carbon/icons-react";
import {
  Button,
  DataTable,
  InlineNotification,
  Select,
  SelectItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  TextInput,
} from "@carbon/react";
import { useIntl } from "react-intl";
import ReagentLotPicker from "./ReagentLotPicker";
import ReagentUsageHistory from "./ReagentUsageHistory";

const CaseInoculationPanel = ({
  inoculations = [],
  onRecord,
  saving = false,
  reagentRequirements = [],
  reagentUsages = [],
  readOnly = false,
}) => {
  const intl = useIntl();
  const [mode, setMode] = useState("");
  const [sourceInoculationId, setSourceInoculationId] = useState("");
  const [containerIdentifier, setContainerIdentifier] = useState("");
  const [media, setMedia] = useState("");
  const [incubation, setIncubation] = useState("");
  const [atmosphere, setAtmosphere] = useState("");
  const [selectedLots, setSelectedLots] = useState({});
  const primaryTriggerRef = useRef(null);
  const subcultureTriggerRef = useRef(null);
  const activeTriggerRef = useRef(null);
  const sourceFieldRef = useRef(null);
  const identifierFieldRef = useRef(null);
  const previousModeRef = useRef("");

  const byId = useMemo(
    () =>
      new Map(inoculations.map((inoculation) => [inoculation.id, inoculation])),
    [inoculations],
  );
  const rows = inoculations.map((inoculation) => ({
    id: inoculation.id,
    identifier: inoculation.containerIdentifier,
    media: inoculation.media,
    source: inoculation.sourceInoculationId
      ? byId.get(inoculation.sourceInoculationId)?.containerIdentifier ||
        inoculation.sourceInoculationId
      : "",
    incubation: inoculation.incubation || "-",
  }));
  const headers = [
    {
      key: "identifier",
      header: intl.formatMessage({ id: "microbiology.inoculation.identifier" }),
    },
    {
      key: "media",
      header: intl.formatMessage({ id: "microbiology.case.media" }),
    },
    {
      key: "source",
      header: intl.formatMessage({ id: "microbiology.inoculation.source" }),
    },
    {
      key: "incubation",
      header: intl.formatMessage({ id: "microbiology.case.incubation" }),
    },
  ];

  const clearForm = () => {
    setSourceInoculationId("");
    setContainerIdentifier("");
    setMedia("");
    setIncubation("");
    setAtmosphere("");
    setSelectedLots({});
  };

  const openForm = (nextMode, triggerRef) => {
    clearForm();
    activeTriggerRef.current = triggerRef.current;
    setMode(nextMode);
  };

  const closeForm = () => {
    clearForm();
    setMode("");
  };

  useEffect(() => {
    if (mode === "subculture") {
      sourceFieldRef.current?.focus();
    } else if (mode === "primary") {
      identifierFieldRef.current?.focus();
    } else if (previousModeRef.current) {
      activeTriggerRef.current?.focus();
    }
    previousModeRef.current = mode;
  }, [mode]);

  const valid =
    containerIdentifier.trim() &&
    media.trim() &&
    (mode !== "subculture" || sourceInoculationId);

  const submit = () => {
    const payload = {
      sourceInoculationId:
        mode === "subculture" ? sourceInoculationId : undefined,
      containerIdentifier: containerIdentifier.trim(),
      media: media.trim(),
      incubation: incubation.trim(),
      atmosphere: atmosphere.trim(),
      lotSelections: Object.values(selectedLots),
    };
    Promise.resolve(onRecord(payload))
      .then(closeForm)
      .catch(() => undefined);
  };

  return (
    <section aria-labelledby="microbiology-inoculation-title">
      <Stack gap={5}>
        <div>
          <h3 id="microbiology-inoculation-title">
            {intl.formatMessage({ id: "microbiology.case.setup" })}
          </h3>
          <p>{intl.formatMessage({ id: "microbiology.inoculation.hint" })}</p>
        </div>

        <div className="microbiology-inline-actions">
          <Button
            ref={primaryTriggerRef}
            kind="tertiary"
            renderIcon={Add}
            disabled={saving || readOnly}
            onClick={() => openForm("primary", primaryTriggerRef)}
          >
            {intl.formatMessage({ id: "microbiology.inoculation.start" })}
          </Button>
          <Button
            ref={subcultureTriggerRef}
            kind="tertiary"
            renderIcon={Add}
            disabled={saving || readOnly || inoculations.length === 0}
            onClick={() => openForm("subculture", subcultureTriggerRef)}
          >
            {intl.formatMessage({
              id: "microbiology.inoculation.addSubculture",
            })}
          </Button>
        </div>

        {inoculations.length === 0 ? (
          <InlineNotification
            kind="info"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({ id: "microbiology.inoculation.empty" })}
          />
        ) : (
          <DataTable rows={rows} headers={headers}>
            {({
              rows: tableRows,
              headers: tableHeaders,
              getTableProps,
              getHeaderProps,
              getRowProps,
            }) => (
              <TableContainer
                title={intl.formatMessage({
                  id: "microbiology.inoculation.recorded",
                })}
              >
                <Table {...getTableProps()} tabIndex={0}>
                  <TableHead>
                    <TableRow>
                      {tableHeaders.map((header) => (
                        <TableHeader
                          {...getHeaderProps({ header })}
                          key={header.key}
                        >
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {tableRows.map((row) => (
                      <TableRow {...getRowProps({ row })} key={row.id}>
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>
                            {cell.info.header === "source" ? (
                              cell.value ? (
                                <span>
                                  <Tag type="purple">
                                    {intl.formatMessage({
                                      id: "microbiology.inoculation.subculture",
                                    })}
                                  </Tag>{" "}
                                  {cell.value}
                                </span>
                              ) : (
                                <Tag type="blue">
                                  {intl.formatMessage({
                                    id: "microbiology.inoculation.primary",
                                  })}
                                </Tag>
                              )
                            ) : (
                              cell.value
                            )}
                          </TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
        )}

        {mode && (
          <section
            aria-labelledby="microbiology-inoculation-form-title"
            onKeyDown={(event) => {
              if (event.key === "Escape" && !saving) {
                event.preventDefault();
                closeForm();
              }
            }}
          >
            <Stack gap={5}>
              <h4 id="microbiology-inoculation-form-title">
                {intl.formatMessage({
                  id:
                    mode === "subculture"
                      ? "microbiology.inoculation.addSubculture"
                      : "microbiology.inoculation.start",
                })}
              </h4>
              {mode === "subculture" && (
                <Select
                  ref={sourceFieldRef}
                  id="microbiology-inoculation-source"
                  labelText={intl.formatMessage({
                    id: "microbiology.inoculation.parent",
                  })}
                  value={sourceInoculationId}
                  onChange={(event) =>
                    setSourceInoculationId(event.target.value)
                  }
                >
                  <SelectItem
                    value=""
                    text={intl.formatMessage({
                      id: "microbiology.inoculation.parentPlaceholder",
                    })}
                  />
                  {inoculations.map((inoculation) => (
                    <SelectItem
                      key={inoculation.id}
                      value={inoculation.id}
                      text={`${inoculation.containerIdentifier} - ${inoculation.media}`}
                    />
                  ))}
                </Select>
              )}
              <div className="microbiology-form-grid">
                <TextInput
                  ref={identifierFieldRef}
                  id="microbiology-inoculation-identifier"
                  labelText={intl.formatMessage({
                    id: "microbiology.inoculation.identifier",
                  })}
                  helperText={intl.formatMessage({
                    id: "microbiology.inoculation.identifierHint",
                  })}
                  value={containerIdentifier}
                  onChange={(event) =>
                    setContainerIdentifier(event.target.value)
                  }
                />
                <TextInput
                  id="microbiology-inoculation-media"
                  labelText={intl.formatMessage({
                    id: "microbiology.case.media",
                  })}
                  value={media}
                  onChange={(event) => setMedia(event.target.value)}
                />
                <TextInput
                  id="microbiology-inoculation-incubation"
                  labelText={intl.formatMessage({
                    id: "microbiology.case.incubation",
                  })}
                  value={incubation}
                  onChange={(event) => setIncubation(event.target.value)}
                />
                <TextInput
                  id="microbiology-inoculation-atmosphere"
                  labelText={intl.formatMessage({
                    id: "microbiology.case.atmosphere",
                  })}
                  value={atmosphere}
                  onChange={(event) => setAtmosphere(event.target.value)}
                />
              </div>
              <ReagentLotPicker
                id="microbiology-culture-lots"
                requirements={reagentRequirements}
                selectedLots={selectedLots}
                onChange={(selection) => {
                  const key = `${selection.analysisId}:${selection.testReagentLinkId}`;
                  setSelectedLots((current) => ({
                    ...current,
                    [key]: selection,
                  }));
                }}
                disabled={saving}
              />
              <div className="microbiology-inline-actions">
                <Button onClick={submit} disabled={!valid || saving}>
                  {intl.formatMessage({ id: "microbiology.inoculation.save" })}
                </Button>
                <Button kind="secondary" onClick={closeForm} disabled={saving}>
                  {intl.formatMessage({ id: "button.cancel" })}
                </Button>
              </div>
            </Stack>
          </section>
        )}

        <div className="cds--visually-hidden" role="status" aria-live="polite">
          {mode
            ? intl.formatMessage({
                id:
                  mode === "subculture"
                    ? "microbiology.inoculation.subcultureExpanded"
                    : "microbiology.inoculation.primaryExpanded",
              })
            : ""}
        </div>

        <ReagentUsageHistory
          usages={reagentUsages.filter(
            (usage) => usage.usageContext === "CULTURE_SETUP",
          )}
        />
      </Stack>
    </section>
  );
};

export default CaseInoculationPanel;
