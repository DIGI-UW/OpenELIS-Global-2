import React, { useCallback, useContext, useEffect, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { useLocation } from "react-router-dom";
import {
  Button,
  Checkbox,
  Column,
  FileUploaderDropContainer,
  Grid,
  InlineNotification,
  ListItem,
  Modal,
  RadioButton,
  RadioButtonGroup,
  RadioTile,
  Select,
  SelectItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  Tile,
  TileGroup,
  UnorderedList,
  InlineLoading,
} from "@carbon/react";
import { Download } from "@carbon/icons-react";
import { postToOpenElisServerFormDataJsonResponse } from "../../utils/Utils";
import { requestFailed, serverMessage } from "../../utils/requestOutcome";
import { LocationsContext } from "./LocationsPage";
import { getJson } from "./locationsApi";

const AREAS = ["organizations", "levels", "values"];
const AREA_LABEL = {
  organizations: "label.locations.import.area.organizations",
  levels: "label.locations.import.area.levels",
  values: "label.locations.import.area.values",
};
const OUTCOME_TAG = {
  new: "green",
  updated: "blue",
  unchanged: "gray",
  reactivated: "teal",
  decision: "purple",
  rename: "magenta",
  rejected: "red",
  skipped: "cool-gray",
  deactivated: "warm-gray",
};
const COUNT_KEYS = [
  "new",
  "updated",
  "unchanged",
  "reactivated",
  "deactivated",
  "decision",
  "rejected",
];

/** The area a file belongs to, from its name (FR-F1). */
const inferArea = (name) => {
  const lower = (name || "").toLowerCase();
  if (lower.includes("-levels")) return "levels";
  if (lower.includes("-values")) return "values";
  return "organizations";
};

const send = (path, files, areas, mode, decisions, renames) =>
  new Promise((resolve, reject) => {
    const data = new FormData();
    files.forEach((file) => data.append("files", file));
    areas.forEach((area) => data.append("areas", area));
    data.append("mode", mode);
    data.append("decisions", JSON.stringify(decisions || {}));
    data.append("renames", JSON.stringify(renames || {}));
    postToOpenElisServerFormDataJsonResponse(path, data, (response) => {
      if (requestFailed(response) || !response || !response.counts) {
        const error = new Error(
          (response && (serverMessage(response) || response.error)) ||
            "The import could not be read",
        );
        reject(error);
        return;
      }
      resolve(response);
    });
  });

/**
 * Section F and G: upload CSV files in the configuration loader's format,
 * choose Add & update or Replace, preview every outcome, decide the ambiguous
 * rows and possible renames, then apply with a confirmation that restates the
 * counts. Nothing is saved before Apply.
 */
const ImportExportView = () => {
  const intl = useIntl();
  const location = useLocation();
  const { notify } = useContext(LocationsContext);
  const presetArea = new URLSearchParams(location.search).get("area");
  const [files, setFiles] = useState([]);
  const [mode, setMode] = useState("merge");
  const [stage, setStage] = useState("setup");
  const [plan, setPlan] = useState(null);
  const [decisions, setDecisions] = useState({});
  const [renames, setRenames] = useState({});
  const [remember, setRemember] = useState({});
  const [acknowledged, setAcknowledged] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [recent, setRecent] = useState([]);

  const loadRecent = useCallback(() => {
    getJson("/rest/locations/import/recent")
      .then((runs) => setRecent(runs || []))
      .catch(() => setRecent([]));
  }, []);

  useEffect(() => {
    loadRecent();
  }, [loadRecent]);

  const addFiles = (added) => {
    const next = [...files];
    added.forEach((file) => {
      if (!next.some((entry) => entry.file.name === file.name)) {
        next.push({
          file,
          area:
            presetArea && AREAS.includes(presetArea) && !files.length
              ? presetArea
              : inferArea(file.name),
        });
      }
    });
    setFiles(next);
    setPlan(null);
    setStage("setup");
  };

  const preview = () => {
    setBusy(true);
    setError(null);
    send(
      "/rest/locations/import/preview",
      files.map((entry) => entry.file),
      files.map((entry) => entry.area),
      mode,
    )
      .then((loaded) => {
        setPlan(loaded);
        setDecisions({});
        setRenames({});
        setRemember({});
        setAcknowledged(false);
        setStage("preview");
        setBusy(false);
      })
      .catch((e) => {
        setError(e.message);
        setBusy(false);
      });
  };

  const apply = () => {
    setBusy(true);
    setError(null);
    const chosen = {};
    Object.keys(decisions).forEach((line) => {
      chosen[line] = { choice: decisions[line], remember: !!remember[line] };
    });
    send(
      "/rest/locations/import/apply",
      files.map((entry) => entry.file),
      files.map((entry) => entry.area),
      mode,
      chosen,
      renames,
    )
      .then((result) => {
        setPlan(result);
        setStage("done");
        setConfirming(false);
        setBusy(false);
        loadRecent();
        notify(intl.formatMessage({ id: "label.locations.import.complete" }));
      })
      .catch((e) => {
        setError(e.message);
        setConfirming(false);
        setBusy(false);
      });
  };

  const rows = plan ? plan.rows || [] : [];
  const decisionRows = rows.filter((row) => row.outcome === "decision");
  const renameRows = rows.filter((row) => row.outcome === "rename");
  const undecided =
    decisionRows.filter((row) => !decisions[row.line]).length +
    renameRows.filter((row) => !renames[row.line]).length;
  const counts = plan ? plan.counts || {} : {};
  const scopeText = plan && plan.scope ? plan.scope.text : null;

  const outcomeLabel = (outcome) =>
    intl.formatMessage({ id: `label.locations.import.outcome.${outcome}` });

  return (
    <Stack gap={6} className="locationsImport" data-testid="locations-import">
      {error && (
        <InlineNotification
          kind="error"
          lowContrast
          title=""
          subtitle={intl.formatMessage(
            { id: "message.locations.import.failed" },
            { reason: error },
          )}
          onClose={() => setError(null)}
        />
      )}
      {stage === "setup" && (
        <>
          <Tile>
            <h4>
              1. <FormattedMessage id="label.locations.import.files" />
            </h4>
            <FileUploaderDropContainer
              accept={[".csv"]}
              multiple
              labelText={intl.formatMessage({
                id: "label.locations.import.drop",
              })}
              onAddFiles={(event, { addedFiles }) => addFiles(addedFiles)}
            />
            {files.map((entry, index) => (
              <Stack
                key={entry.file.name}
                orientation="horizontal"
                gap={4}
                className="locationsImportFile"
              >
                <span data-testid="locations-import-file">
                  {entry.file.name}
                </span>
                <Select
                  id={`import-area-${index}`}
                  labelText={intl.formatMessage({
                    id: "label.locations.import.area",
                  })}
                  value={entry.area}
                  onChange={(e) =>
                    setFiles(
                      files.map((f, i) =>
                        i === index ? { ...f, area: e.target.value } : f,
                      ),
                    )
                  }
                >
                  {AREAS.map((area) => (
                    <SelectItem
                      key={area}
                      value={area}
                      text={intl.formatMessage({ id: AREA_LABEL[area] })}
                    />
                  ))}
                </Select>
                <Button
                  kind="ghost"
                  size="sm"
                  onClick={() => setFiles(files.filter((_, i) => i !== index))}
                >
                  <FormattedMessage id="button.remove" />
                </Button>
              </Stack>
            ))}
            <Stack orientation="horizontal" gap={3}>
              {AREAS.map((area) => (
                <Button
                  key={area}
                  kind="ghost"
                  size="sm"
                  renderIcon={Download}
                  href={`/api/OpenELIS-Global/rest/locations/import/template?area=${area}`}
                  target="_blank"
                >
                  <FormattedMessage id="button.locations.template" /> ·{" "}
                  <FormattedMessage id={AREA_LABEL[area]} />
                </Button>
              ))}
            </Stack>
          </Tile>
          <Tile>
            <h4>
              2. <FormattedMessage id="label.locations.import.mode" />
            </h4>
            <TileGroup
              name="import-mode"
              valueSelected={mode}
              onChange={setMode}
            >
              <RadioTile value="merge" id="import-mode-merge">
                <strong>
                  <FormattedMessage id="label.locations.import.mode.merge" />
                </strong>
                <p>
                  <FormattedMessage id="help.locations.import.mode.merge" />
                </p>
              </RadioTile>
              <RadioTile value="replace" id="import-mode-replace">
                <strong>
                  <FormattedMessage id="label.locations.import.mode.replace" />
                </strong>
                <p>
                  <FormattedMessage id="help.locations.import.mode.replace" />
                </p>
              </RadioTile>
            </TileGroup>
          </Tile>
          <div className="locationsImportActions">
            <Button
              disabled={!files.length || busy}
              onClick={preview}
              data-testid="locations-import-preview"
            >
              <FormattedMessage id="button.locations.import.preview" />
            </Button>
            {busy && (
              <InlineLoading
                status="active"
                description={intl.formatMessage({
                  id: "label.locations.import.working",
                })}
              />
            )}
          </div>
        </>
      )}

      {stage === "preview" && plan && (
        <>
          <InlineNotification
            kind="info"
            lowContrast
            hideCloseButton
            title=""
            subtitle={`${scopeText || intl.formatMessage({ id: mode === "replace" ? "help.locations.import.mode.replace" : "help.locations.import.mode.merge" })} ${intl.formatMessage({ id: "message.locations.import.nothingSaved" })}`}
          />
          <Grid condensed>
            {COUNT_KEYS.map((key) => (
              <Column key={key} lg={2} md={2} sm={2}>
                <Tile data-testid={`locations-import-count-${key}`}>
                  <div className="cds--type-productive-heading-05">
                    {counts[key] || 0}
                  </div>
                  <div className="cds--label">{outcomeLabel(key)}</div>
                </Tile>
              </Column>
            ))}
          </Grid>
          {renameRows.length > 0 && (
            <Tile>
              <h4>
                <FormattedMessage id="label.locations.import.rename.title" />
              </h4>
              {renameRows.map((row) => (
                <RadioButtonGroup
                  key={row.line}
                  legendText={`${row.pair ? row.pair.name : ""} → ${row.name} (${intl.formatMessage({ id: "label.locations.import.column.line" })} ${row.line})`}
                  name={`rename-${row.line}`}
                  valueSelected={renames[row.line]}
                  onChange={(value) =>
                    setRenames({ ...renames, [row.line]: value })
                  }
                >
                  <RadioButton
                    id={`rename-${row.line}-same`}
                    value="same"
                    labelText={intl.formatMessage({
                      id: "label.locations.import.rename.same",
                    })}
                  />
                  <RadioButton
                    id={`rename-${row.line}-different`}
                    value="different"
                    labelText={intl.formatMessage({
                      id: "label.locations.import.rename.different",
                    })}
                  />
                </RadioButtonGroup>
              ))}
            </Tile>
          )}
          {decisionRows.length > 0 && (
            <Tile>
              <h4>
                <FormattedMessage id="label.locations.import.outcome.decision" />
              </h4>
              {decisionRows.map((row) => (
                <Stack
                  key={row.line}
                  gap={2}
                  className="locationsImportDecision"
                >
                  <RadioButtonGroup
                    legendText={`${intl.formatMessage({ id: "label.locations.import.column.line" })} ${row.line}: ${row.type} "${row.name}"`}
                    name={`decision-${row.line}`}
                    orientation="vertical"
                    valueSelected={decisions[row.line]}
                    onChange={(value) =>
                      setDecisions({ ...decisions, [row.line]: value })
                    }
                  >
                    {(row.candidates || []).map((candidate) => (
                      <RadioButton
                        key={candidate.id}
                        id={`decision-${row.line}-${candidate.id}`}
                        value={`use:${candidate.id}`}
                        labelText={`${intl.formatMessage({ id: "label.locations.import.decision.use" })}: ${candidate.name} (${candidate.code || intl.formatMessage({ id: "label.locations.none" })}, ${candidate.parent || ""}, ${candidate.inUse} ${intl.formatMessage({ id: "label.locations.openOrders" })})`}
                      />
                    ))}
                    <RadioButton
                      id={`decision-${row.line}-new`}
                      value="new"
                      labelText={intl.formatMessage({
                        id: "label.locations.import.decision.new",
                      })}
                    />
                    <RadioButton
                      id={`decision-${row.line}-skip`}
                      value="skip"
                      labelText={intl.formatMessage({
                        id: "label.locations.import.decision.skip",
                      })}
                    />
                  </RadioButtonGroup>
                  <Checkbox
                    id={`remember-${row.line}`}
                    labelText={intl.formatMessage({
                      id: "label.locations.import.decision.remember",
                    })}
                    disabled={
                      !decisions[row.line] ||
                      !String(decisions[row.line]).startsWith("use:")
                    }
                    checked={!!remember[row.line]}
                    onChange={(_, { checked }) =>
                      setRemember({ ...remember, [row.line]: checked })
                    }
                  />
                </Stack>
              ))}
            </Tile>
          )}
          {mode === "replace" && (
            <Tile>
              <h4>
                <FormattedMessage id="label.locations.import.willDeactivate" />{" "}
                ({(plan.deactivations || []).length})
              </h4>
              <UnorderedList>
                {(plan.deactivations || []).map((item) => (
                  <ListItem key={item.id}>
                    {item.name} ({item.location || item.code || ""})
                    {item.inUse && item.inUse.open
                      ? `: ${item.inUse.open} ${intl.formatMessage({ id: "label.locations.openOrders" })}`
                      : ""}
                  </ListItem>
                ))}
              </UnorderedList>
            </Tile>
          )}
          <Table
            size="sm"
            aria-label={intl.formatMessage({
              id: "label.locations.import.rows",
            })}
          >
            <TableHead>
              <TableRow>
                {["line", "outcome", "type", "code", "name", "detail"].map(
                  (column) => (
                    <TableHeader key={column}>
                      <FormattedMessage
                        id={`label.locations.import.column.${column}`}
                      />
                    </TableHeader>
                  ),
                )}
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={`${row.file}-${row.line}`}>
                  <TableCell>{row.line}</TableCell>
                  <TableCell>
                    <Tag type={OUTCOME_TAG[row.outcome] || "gray"} size="sm">
                      {outcomeLabel(row.outcome)}
                    </Tag>
                  </TableCell>
                  <TableCell>{row.type}</TableCell>
                  <TableCell>
                    <code>{row.code || "-"}</code>
                  </TableCell>
                  <TableCell>{row.name}</TableCell>
                  <TableCell>
                    {row.reason ||
                      (row.diffs || [])
                        .map(
                          (diff) =>
                            `${diff.field}: ${diff.oldValue || "(none)"} → ${diff.newValue || "(none)"}`,
                        )
                        .join("; ")}
                    {row.registry
                      ? ` · ${intl.formatMessage({ id: "warning.locations.import.registryOverwrite" })}`
                      : ""}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <Stack orientation="horizontal" gap={3}>
            <Button
              disabled={undecided > 0 || busy}
              onClick={() => setConfirming(true)}
              data-testid="locations-import-apply"
            >
              <FormattedMessage id="button.locations.import.apply" />
            </Button>
            <Button kind="secondary" onClick={() => setStage("setup")}>
              <FormattedMessage id="button.back" />
            </Button>
          </Stack>
        </>
      )}

      {stage === "done" && plan && (
        <Tile data-testid="locations-import-done">
          <h4>
            <FormattedMessage id="label.locations.import.complete" />
          </h4>
          <p>
            {COUNT_KEYS.map(
              (key) => `${counts[key] || 0} ${outcomeLabel(key)}`,
            ).join(", ")}
            .
          </p>
          <Stack orientation="horizontal" gap={3}>
            {plan.importRunId && (
              <Button
                kind="tertiary"
                renderIcon={Download}
                href={`/api/OpenELIS-Global/rest/locations/import/runs/${plan.importRunId}/report`}
                target="_blank"
              >
                <FormattedMessage id="button.locations.import.report" />
              </Button>
            )}
            <Button
              kind="secondary"
              onClick={() => {
                setFiles([]);
                setPlan(null);
                setStage("setup");
              }}
            >
              <FormattedMessage id="button.back" />
            </Button>
          </Stack>
        </Tile>
      )}

      <Tile>
        <h4>
          <FormattedMessage id="label.locations.import.recent" />
        </h4>
        {recent.length === 0 ? (
          <p className="cds--label">
            <FormattedMessage id="label.locations.none" />
          </p>
        ) : (
          <Table
            size="sm"
            aria-label={intl.formatMessage({
              id: "label.locations.import.recent",
            })}
          >
            <TableBody>
              {recent.map((run) => (
                <TableRow key={run.id}>
                  <TableCell>{run.startedAt}</TableCell>
                  <TableCell>{run.user}</TableCell>
                  <TableCell>{run.mode}</TableCell>
                  <TableCell>{run.summary}</TableCell>
                  <TableCell>
                    <Button
                      kind="ghost"
                      size="sm"
                      renderIcon={Download}
                      href={`/api/OpenELIS-Global/rest/locations/import/runs/${run.id}/report`}
                      target="_blank"
                    >
                      <FormattedMessage id="button.locations.import.report" />
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Tile>

      <Modal
        open={confirming}
        modalHeading={intl.formatMessage({
          id: "label.locations.import.confirm.title",
        })}
        primaryButtonText={intl.formatMessage({
          id: "button.locations.import.apply",
        })}
        secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
        primaryButtonDisabled={busy || (mode === "replace" && !acknowledged)}
        onRequestClose={() => setConfirming(false)}
        onRequestSubmit={apply}
      >
        {busy && (
          <InlineLoading
            status="active"
            description={intl.formatMessage({
              id: "label.locations.import.working",
            })}
          />
        )}
        <UnorderedList>
          {COUNT_KEYS.filter(
            (key) => key !== "unchanged" && key !== "decision",
          ).map((key) => (
            <ListItem key={key}>
              {counts[key] || 0} {outcomeLabel(key)}
            </ListItem>
          ))}
        </UnorderedList>
        {mode === "replace" && (
          <Checkbox
            id="import-acknowledge"
            labelText={intl.formatMessage(
              { id: "label.locations.import.confirm.replace" },
              { count: counts.deactivated || 0 },
            )}
            checked={acknowledged}
            onChange={(_, { checked }) => setAcknowledged(checked)}
          />
        )}
      </Modal>
    </Stack>
  );
};

export default ImportExportView;
