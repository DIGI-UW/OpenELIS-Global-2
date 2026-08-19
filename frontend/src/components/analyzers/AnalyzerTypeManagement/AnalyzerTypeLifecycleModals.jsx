import React, { useEffect, useMemo, useState } from "react";
import {
  Checkbox,
  InlineNotification,
  Loading,
  Modal,
  Select,
  SelectItem,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  TextInput,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";

const slugify = (value) =>
  value
    .normalize("NFKD")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");

const nextAvailableIdentity = (base, types, startAtOne = false) => {
  const existing = new Set(types.map((type) => type.profileId));
  if (!startAtOne && !existing.has(base)) {
    return base;
  }
  let suffix = 1;
  while (existing.has(`${base}-${suffix}`)) {
    suffix += 1;
  }
  return `${base}-${suffix}`;
};

const nextDuplicateName = (displayName, types) => {
  const existing = new Set(types.map((type) => type.displayName));
  let suffix = 1;
  while (existing.has(`${displayName} -${suffix}`)) {
    suffix += 1;
  }
  return `${displayName} -${suffix}`;
};

const hasError = (response) => !response || Boolean(response.error);

const CreateProfileModal = ({ types, onClose, onSuccess, onError }) => {
  const intl = useIntl();
  const [displayName, setDisplayName] = useState("");
  const [manufacturer, setManufacturer] = useState("");
  const [model, setModel] = useState("");
  const [protocol, setProtocol] = useState("ASTM");
  const [dataFlow, setDataFlow] = useState("RESULTS_ONLY");
  const [connectionTest, setConnectionTest] = useState(false);
  const [affirmedNoControls, setAffirmedNoControls] = useState(false);
  const [fileFormat, setFileFormat] = useState("CSV");
  const [filePattern, setFilePattern] = useState("");
  const [sampleColumn, setSampleColumn] = useState("");
  const [testColumn, setTestColumn] = useState("");
  const [resultColumn, setResultColumn] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const normalizedName = displayName.trim();
  const duplicateName = types.some(
    (type) => type.displayName.toLowerCase() === normalizedName.toLowerCase(),
  );
  const fileColumns = [
    sampleColumn.trim(),
    testColumn.trim(),
    resultColumn.trim(),
  ];
  const fileFieldsValid =
    protocol !== "FILE" ||
    (filePattern.trim() &&
      fileColumns.every(Boolean) &&
      new Set(fileColumns).size === fileColumns.length);
  const valid =
    normalizedName &&
    manufacturer.trim() &&
    model.trim() &&
    !duplicateName &&
    affirmedNoControls &&
    fileFieldsValid;

  const submit = () => {
    if (!valid || submitting) {
      return;
    }
    setSubmitting(true);
    const profile = {
      schemaVersion: "1.0",
      profileId: nextAvailableIdentity(
        `site.${slugify(normalizedName) || "profile"}`,
        types,
      ),
      displayName: normalizedName,
      protocol,
      capabilities: {
        inboundResults: true,
        outboundOrders: dataFlow === "TWO_WAY",
        connectionTest,
      },
      identity: {
        manufacturer: manufacturer.trim(),
        model: model.trim(),
      },
      tests: [],
      controlResultRecognition: {
        mode: "NONE",
        affirmedNoControlResults: true,
      },
    };
    if (protocol === "FILE") {
      profile.file = {
        format: fileFormat,
        filePattern: filePattern.trim(),
        columnMappings: {
          [sampleColumn.trim()]: "sampleId",
          [testColumn.trim()]: "testCode",
          [resultColumn.trim()]: "result",
        },
      };
      if (fileFormat === "CSV" || fileFormat === "TSV") {
        profile.file.delimiter = fileFormat === "CSV" ? "," : "\t";
      }
    }
    postToOpenElisServerJsonResponse(
      "/rest/analyzer-types",
      JSON.stringify({ profile }),
      (response) => {
        setSubmitting(false);
        if (hasError(response)) {
          onError(response?.error);
          return;
        }
        onSuccess("create");
      },
    );
  };

  return (
    <Modal
      open
      modalHeading={intl.formatMessage({
        id: "analyzerType.button.create",
      })}
      primaryButtonText={intl.formatMessage({
        id: "analyzerType.button.create",
      })}
      secondaryButtonText={intl.formatMessage({
        id: "analyzerType.button.cancel",
      })}
      primaryButtonDisabled={!valid || submitting}
      onRequestClose={onClose}
      onSecondarySubmit={onClose}
      onRequestSubmit={submit}
      selectorPrimaryFocus="#analyzer-type-create-name"
      size="md"
    >
      <div className="analyzer-type-modal__form">
        <TextInput
          id="analyzer-type-create-name"
          labelText={intl.formatMessage({
            id: "analyzerType.field.profileName",
          })}
          value={displayName}
          invalid={duplicateName}
          invalidText={intl.formatMessage({
            id: "analyzerType.error.nameUnique",
          })}
          onChange={(event) => setDisplayName(event.target.value)}
        />
        <div className="analyzer-type-modal__two-column">
          <TextInput
            id="analyzer-type-create-manufacturer"
            labelText={intl.formatMessage({
              id: "analyzerType.field.manufacturer",
            })}
            value={manufacturer}
            onChange={(event) => setManufacturer(event.target.value)}
          />
          <TextInput
            id="analyzer-type-create-model"
            labelText={intl.formatMessage({
              id: "analyzerType.field.model",
            })}
            value={model}
            onChange={(event) => setModel(event.target.value)}
          />
        </div>
        <div className="analyzer-type-modal__two-column">
          <Select
            id="analyzer-type-create-protocol"
            aria-label={intl.formatMessage({
              id: "analyzerType.field.protocol",
            })}
            labelText={intl.formatMessage({
              id: "analyzerType.field.protocol",
            })}
            value={protocol}
            onChange={(event) => setProtocol(event.target.value)}
          >
            <SelectItem value="ASTM" text="ASTM" />
            <SelectItem value="HL7" text="HL7" />
            <SelectItem
              value="FILE"
              text={intl.formatMessage({
                id: "analyzerType.protocol.file",
              })}
            />
          </Select>
          <Select
            id="analyzer-type-create-data-flow"
            aria-label={intl.formatMessage({
              id: "analyzerType.field.dataFlow",
            })}
            labelText={intl.formatMessage({
              id: "analyzerType.field.dataFlow",
            })}
            value={dataFlow}
            onChange={(event) => setDataFlow(event.target.value)}
          >
            <SelectItem
              value="RESULTS_ONLY"
              text={intl.formatMessage({
                id: "analyzerType.dataFlow.resultsOnly",
              })}
            />
            <SelectItem
              value="TWO_WAY"
              text={intl.formatMessage({
                id: "analyzerType.dataFlow.twoWay",
              })}
            />
          </Select>
        </div>
        <Checkbox
          id="analyzer-type-create-connection-test"
          aria-label={intl.formatMessage({
            id: "analyzerType.field.connectionTest",
          })}
          labelText={intl.formatMessage({
            id: "analyzerType.field.connectionTest",
          })}
          checked={connectionTest}
          onChange={(_, state) => setConnectionTest(state.checked)}
        />
        {protocol === "FILE" && (
          <div className="analyzer-type-modal__file-fields">
            <Select
              id="analyzer-type-create-file-format"
              aria-label={intl.formatMessage({
                id: "analyzerType.field.fileFormat",
              })}
              labelText={intl.formatMessage({
                id: "analyzerType.field.fileFormat",
              })}
              value={fileFormat}
              onChange={(event) => setFileFormat(event.target.value)}
            >
              {["CSV", "TSV", "XLS", "XLSX", "ODS"].map((format) => (
                <SelectItem key={format} value={format} text={format} />
              ))}
            </Select>
            <TextInput
              id="analyzer-type-create-file-pattern"
              labelText={intl.formatMessage({
                id: "analyzerType.field.filePattern",
              })}
              value={filePattern}
              onChange={(event) => setFilePattern(event.target.value)}
            />
            <div className="analyzer-type-modal__three-column">
              <TextInput
                id="analyzer-type-create-sample-column"
                labelText={intl.formatMessage({
                  id: "analyzerType.field.sampleColumn",
                })}
                value={sampleColumn}
                onChange={(event) => setSampleColumn(event.target.value)}
              />
              <TextInput
                id="analyzer-type-create-test-column"
                labelText={intl.formatMessage({
                  id: "analyzerType.field.testColumn",
                })}
                value={testColumn}
                onChange={(event) => setTestColumn(event.target.value)}
              />
              <TextInput
                id="analyzer-type-create-result-column"
                labelText={intl.formatMessage({
                  id: "analyzerType.field.resultColumn",
                })}
                value={resultColumn}
                onChange={(event) => setResultColumn(event.target.value)}
              />
            </div>
          </div>
        )}
        <InlineNotification
          kind="warning"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "analyzerType.recognition.none.title",
          })}
          subtitle={intl.formatMessage({
            id: "analyzerType.recognition.none.subtitle",
          })}
        />
        <Checkbox
          id="analyzer-type-create-no-controls"
          aria-label={intl.formatMessage({
            id: "analyzerType.recognition.none.affirmation",
          })}
          labelText={intl.formatMessage({
            id: "analyzerType.recognition.none.affirmation",
          })}
          checked={affirmedNoControls}
          onChange={(_, state) => setAffirmedNoControls(state.checked)}
        />
      </div>
    </Modal>
  );
};

const DuplicateProfileModal = ({
  types,
  initialProfileId,
  onClose,
  onSuccess,
  onError,
}) => {
  const intl = useIntl();
  const activeTypes = useMemo(
    () => types.filter((type) => type.status === "ACTIVE"),
    [types],
  );
  const initialSource = activeTypes.find(
    (type) => type.profileId === initialProfileId,
  );
  const [sourceId, setSourceId] = useState(initialSource?.profileId || "");
  const [displayName, setDisplayName] = useState(
    initialSource ? nextDuplicateName(initialSource.displayName, types) : "",
  );
  const [submitting, setSubmitting] = useState(false);
  const source = activeTypes.find((type) => type.profileId === sourceId);
  const normalizedName = displayName.trim();
  const duplicateName = types.some(
    (type) => type.displayName.toLowerCase() === normalizedName.toLowerCase(),
  );
  const valid = source && normalizedName && !duplicateName;

  const changeSource = (profileId) => {
    const nextSource = activeTypes.find((type) => type.profileId === profileId);
    setSourceId(profileId);
    setDisplayName(
      nextSource ? nextDuplicateName(nextSource.displayName, types) : "",
    );
  };

  const submit = () => {
    if (!valid || submitting) {
      return;
    }
    setSubmitting(true);
    const targetProfileId = nextAvailableIdentity(
      source.profileId,
      types,
      true,
    );
    postToOpenElisServerJsonResponse(
      `/rest/analyzer-types/${encodeURIComponent(source.profileId)}/duplicate`,
      JSON.stringify({
        sourceRevision: source.revision,
        targetProfileId,
        displayName: normalizedName,
      }),
      (response) => {
        setSubmitting(false);
        if (hasError(response)) {
          onError(response?.error);
          return;
        }
        onSuccess("duplicate");
      },
    );
  };

  return (
    <Modal
      open
      modalHeading={intl.formatMessage({
        id: "analyzerType.button.duplicate",
      })}
      primaryButtonText={intl.formatMessage({
        id: "analyzerType.button.duplicate",
      })}
      secondaryButtonText={intl.formatMessage({
        id: "analyzerType.button.cancel",
      })}
      primaryButtonDisabled={!valid || submitting}
      onRequestClose={onClose}
      onSecondarySubmit={onClose}
      onRequestSubmit={submit}
      size="sm"
    >
      <div className="analyzer-type-modal__form">
        <Select
          id="analyzer-type-duplicate-source"
          aria-label={intl.formatMessage({
            id: "analyzerType.field.sourceProfile",
          })}
          labelText={intl.formatMessage({
            id: "analyzerType.field.sourceProfile",
          })}
          value={sourceId}
          onChange={(event) => changeSource(event.target.value)}
        >
          <SelectItem
            value=""
            text={intl.formatMessage({
              id: "analyzerType.field.sourceProfile.placeholder",
            })}
          />
          {activeTypes.map((type) => (
            <SelectItem
              key={type.profileId}
              value={type.profileId}
              text={`${type.displayName} · ${type.protocol}`}
            />
          ))}
        </Select>
        <TextInput
          id="analyzer-type-duplicate-name"
          labelText={intl.formatMessage({
            id: "analyzerType.field.newProfileName",
          })}
          value={displayName}
          invalid={Boolean(normalizedName) && duplicateName}
          invalidText={intl.formatMessage({
            id: "analyzerType.error.nameUnique",
          })}
          onChange={(event) => setDisplayName(event.target.value)}
        />
        {source && (
          <InlineNotification
            kind="info"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "analyzerType.duplicate.lineage.title",
            })}
            subtitle={intl.formatMessage(
              { id: "analyzerType.duplicate.lineage.subtitle" },
              {
                name: source.displayName,
                revision: source.revision,
              },
            )}
          />
        )}
      </div>
    </Modal>
  );
};

const LifecycleConfirmationModal = ({
  action,
  profile,
  onClose,
  onSuccess,
  onError,
}) => {
  const intl = useIntl();
  const [submitting, setSubmitting] = useState(false);
  const title = intl.formatMessage(
    {
      id:
        action === "deactivate"
          ? "analyzerType.modal.deactivate.title"
          : "analyzerType.modal.reactivate.title",
    },
    { name: profile.displayName },
  );

  const submit = () => {
    if (submitting) {
      return;
    }
    setSubmitting(true);
    postToOpenElisServerJsonResponse(
      `/rest/analyzer-types/${encodeURIComponent(profile.profileId)}/${action}`,
      "{}",
      (response) => {
        setSubmitting(false);
        if (hasError(response)) {
          onError(response?.error);
          return;
        }
        onSuccess(action);
      },
    );
  };

  return (
    <Modal
      open
      danger={action === "deactivate"}
      modalHeading={title}
      primaryButtonText={intl.formatMessage({
        id:
          action === "deactivate"
            ? "analyzerType.action.deactivate"
            : "analyzerType.action.reactivate",
      })}
      secondaryButtonText={intl.formatMessage({
        id: "analyzerType.button.cancel",
      })}
      primaryButtonDisabled={submitting}
      onRequestClose={onClose}
      onSecondarySubmit={onClose}
      onRequestSubmit={submit}
      size="sm"
    >
      <p>
        {intl.formatMessage({
          id:
            action === "deactivate"
              ? "analyzerType.modal.deactivate.subtitle"
              : "analyzerType.modal.reactivate.subtitle",
        })}
      </p>
    </Modal>
  );
};

const ProfileHistoryModal = ({ profile, onClose, onError }) => {
  const intl = useIntl();
  const [history, setHistory] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getFromOpenElisServer(
      `/rest/analyzer-types/${encodeURIComponent(profile.profileId)}/history`,
      (response) => {
        setLoading(false);
        if (!Array.isArray(response)) {
          onError(response?.error);
          return;
        }
        setHistory(response);
      },
    );
  }, [onError, profile.profileId]);

  const actionLabel = (action) =>
    intl.formatMessage({
      id: `analyzerType.history.action.${String(action).toLowerCase()}`,
      defaultMessage: action,
    });

  return (
    <Modal
      open
      passiveModal
      modalHeading={intl.formatMessage(
        { id: "analyzerType.modal.history.title" },
        { name: profile.displayName },
      )}
      onRequestClose={onClose}
      size="md"
    >
      {loading ? (
        <div className="analyzer-type-modal__loading">
          <Loading
            withOverlay={false}
            description={intl.formatMessage({
              id: "analyzerType.history.loading",
            })}
          />
        </div>
      ) : (
        <Table size="sm">
          <TableHead>
            <TableRow>
              <TableHeader>
                {intl.formatMessage({
                  id: "analyzerType.history.revision",
                })}
              </TableHeader>
              <TableHeader>
                {intl.formatMessage({ id: "analyzerType.history.status" })}
              </TableHeader>
              <TableHeader>
                {intl.formatMessage({ id: "analyzerType.history.change" })}
              </TableHeader>
              <TableHeader>
                {intl.formatMessage({ id: "analyzerType.history.actor" })}
              </TableHeader>
              <TableHeader>
                {intl.formatMessage({ id: "analyzerType.history.time" })}
              </TableHeader>
            </TableRow>
          </TableHead>
          <TableBody>
            {(history || []).map((revision) => (
              <TableRow key={revision.profile.revision}>
                <TableCell>
                  {intl.formatMessage(
                    { id: "analyzerType.history.revisionValue" },
                    { revision: revision.profile.revision },
                  )}
                </TableCell>
                <TableCell>{revision.profile.status}</TableCell>
                <TableCell>
                  {actionLabel(revision.publication.action)}
                </TableCell>
                <TableCell>{revision.publication.actor}</TableCell>
                <TableCell>
                  {intl.formatDate(new Date(revision.publication.publishedAt), {
                    year: "numeric",
                    month: "short",
                    day: "numeric",
                    hour: "numeric",
                    minute: "2-digit",
                  })}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </Modal>
  );
};

const AnalyzerTypeLifecycleModals = ({
  action,
  profileId,
  types,
  onClose,
  onSuccess,
  onError,
}) => {
  const profile = types.find((type) => type.profileId === profileId);

  if (action === "create") {
    return (
      <CreateProfileModal
        types={types}
        onClose={onClose}
        onSuccess={onSuccess}
        onError={onError}
      />
    );
  }
  if (action === "duplicate") {
    return (
      <DuplicateProfileModal
        types={types}
        initialProfileId={profileId}
        onClose={onClose}
        onSuccess={onSuccess}
        onError={onError}
      />
    );
  }
  if (action === "history" && profile) {
    return (
      <ProfileHistoryModal
        profile={profile}
        onClose={onClose}
        onError={onError}
      />
    );
  }
  if (
    (action === "deactivate" || action === "reactivate") &&
    profile?.source === "SITE"
  ) {
    return (
      <LifecycleConfirmationModal
        action={action}
        profile={profile}
        onClose={onClose}
        onSuccess={onSuccess}
        onError={onError}
      />
    );
  }
  return null;
};

export default AnalyzerTypeLifecycleModals;
