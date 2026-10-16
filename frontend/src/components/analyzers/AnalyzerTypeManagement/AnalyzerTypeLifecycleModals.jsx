import React, { useEffect, useMemo, useState } from "react";
import {
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
import {
  createAnalyzerTypeDraft,
  duplicateAnalyzerType,
  getAnalyzerTypeRevision,
  updateSharedAnalyzerType,
} from "../../../services/analyzerService";
import AffectedAnalyzerList from "./AffectedAnalyzerList";

const nextDuplicateName = (displayName, types) => {
  const existing = new Set(types.map((type) => type.displayName));
  let suffix = 1;
  while (existing.has(`${displayName} -${suffix}`)) {
    suffix += 1;
  }
  return `${displayName} -${suffix}`;
};

const hasError = (response) => !response || Boolean(response.error);

const isExactUpdateDraft = (draft, profile) =>
  draft?.kind === "UPDATE" &&
  draft.baseProfileId === profile.profileId &&
  draft.baseRevision === profile.revision;

// Names a new type; its draft then opens on its own page.
const CreateProfileModal = ({ types, onClose, onError, onDraftCreated }) => {
  const intl = useIntl();
  const [displayName, setDisplayName] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const normalizedName = displayName.trim();
  const duplicateName = types.some(
    (type) => type.displayName.toLowerCase() === normalizedName.toLowerCase(),
  );
  const valid = Boolean(normalizedName) && !duplicateName;

  const submit = () => {
    if (!valid || submitting) {
      return;
    }
    setSubmitting(true);
    createAnalyzerTypeDraft(normalizedName, (response) => {
      setSubmitting(false);
      if (hasError(response) || !response.draftId) {
        onError(response?.error);
        return;
      }
      onDraftCreated(response.draftId);
    });
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
      size="sm"
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
      </div>
    </Modal>
  );
};

// Chooses the type to copy and names the copy; its draft then opens on its
// own page.
const DuplicateProfileModal = ({
  types,
  initialProfileId,
  initialRevision,
  onClose,
  onError,
  onDraftCreated,
}) => {
  const intl = useIntl();
  const activeTypes = useMemo(
    () => types.filter((type) => type.status === "ACTIVE"),
    [types],
  );
  const initialSource = activeTypes.find(
    (type) => type.profileId === initialProfileId,
  );
  const initialPinnedSource =
    initialSource && initialSource.revision === initialRevision
      ? initialSource
      : null;
  const [sourceId, setSourceId] = useState(initialSource?.profileId || "");
  const [displayName, setDisplayName] = useState(
    initialSource ? nextDuplicateName(initialSource.displayName, types) : "",
  );
  const [pinnedSource, setPinnedSource] = useState(initialPinnedSource);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!initialProfileId || !initialRevision || initialPinnedSource) {
      return;
    }
    getAnalyzerTypeRevision(initialProfileId, initialRevision, (response) => {
      if (hasError(response)) {
        onError(response?.error);
        return;
      }
      setPinnedSource(response);
      setSourceId(response.profileId);
      setDisplayName(nextDuplicateName(response.displayName, types));
    });
  }, [initialPinnedSource, initialProfileId, initialRevision, onError, types]);

  const exactInitialRevisionRequested =
    initialProfileId === sourceId && Boolean(initialRevision);
  const source = exactInitialRevisionRequested
    ? pinnedSource?.profileId === sourceId
      ? pinnedSource
      : null
    : activeTypes.find((type) => type.profileId === sourceId);
  const normalizedName = displayName.trim();
  const duplicateName = types.some(
    (type) => type.displayName.toLowerCase() === normalizedName.toLowerCase(),
  );
  const valid = Boolean(source) && Boolean(normalizedName) && !duplicateName;

  const changeSource = (profileId) => {
    const nextSource = activeTypes.find((type) => type.profileId === profileId);
    setPinnedSource(null);
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
    duplicateAnalyzerType(
      source.profileId,
      source.revision,
      normalizedName,
      (response) => {
        setSubmitting(false);
        if (hasError(response) || !response.draftId) {
          onError(response?.error);
          return;
        }
        onDraftCreated(response.draftId);
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

// Says what a new revision of a shared type does; its draft then opens on
// its own page.
const UpdateSharedProfileModal = ({
  profile,
  onClose,
  onError,
  onDraftCreated,
}) => {
  const intl = useIntl();
  const [submitting, setSubmitting] = useState(false);
  const title = intl.formatMessage(
    { id: "analyzerType.modal.update.title" },
    { name: profile.displayName },
  );

  const submit = () => {
    if (submitting) {
      return;
    }
    setSubmitting(true);
    updateSharedAnalyzerType(
      profile.profileId,
      profile.revision,
      (response) => {
        setSubmitting(false);
        if (
          hasError(response) ||
          !response.draftId ||
          !isExactUpdateDraft(response, profile)
        ) {
          onError(response?.error);
          return;
        }
        onDraftCreated(response.draftId);
      },
    );
  };

  return (
    <Modal
      open
      modalHeading={title}
      primaryButtonText={intl.formatMessage({
        id: "analyzerType.button.startUpdate",
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
        {intl.formatMessage(
          { id: "analyzerType.modal.update.subtitle" },
          { count: profile.usedBy, revision: profile.revision },
        )}
      </p>
      <AffectedAnalyzerList
        analyzers={profile.affectedAnalyzers}
        revision={profile.revision}
      />
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
            {(history || []).map((revision) => {
              const revisionCatalog = revision.profile.catalog;
              return (
                <TableRow
                  key={`${revision.profile.profileMeta.id}:${revisionCatalog.revision}`}
                >
                  <TableCell>
                    {intl.formatMessage(
                      { id: "analyzerType.history.revisionValue" },
                      { revision: revisionCatalog.revision },
                    )}
                  </TableCell>
                  <TableCell>
                    {intl.formatMessage({
                      id:
                        revisionCatalog.status === "ACTIVE"
                          ? "analyzerType.status.active"
                          : "analyzerType.status.inactive",
                    })}
                  </TableCell>
                  <TableCell>
                    {actionLabel(revision.publication.action)}
                  </TableCell>
                  <TableCell>{revision.publication.actor}</TableCell>
                  <TableCell>
                    {intl.formatDate(new Date(revision.publication.markedAt), {
                      year: "numeric",
                      month: "short",
                      day: "numeric",
                      hour: "numeric",
                      minute: "2-digit",
                    })}
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      )}
    </Modal>
  );
};

const AnalyzerTypeLifecycleModals = ({
  action,
  profileId,
  revision,
  types,
  onClose,
  onSuccess,
  onError,
  onDraftCreated,
}) => {
  const profile = types.find((type) => type.profileId === profileId);

  if (action === "create") {
    return (
      <CreateProfileModal
        types={types}
        onClose={onClose}
        onError={onError}
        onDraftCreated={onDraftCreated}
      />
    );
  }
  if (action === "duplicate") {
    return (
      <DuplicateProfileModal
        types={types}
        initialProfileId={profileId}
        initialRevision={revision}
        onClose={onClose}
        onError={onError}
        onDraftCreated={onDraftCreated}
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
    action === "update" &&
    profile?.source === "SITE" &&
    profile.status === "ACTIVE"
  ) {
    return (
      <UpdateSharedProfileModal
        profile={profile}
        onClose={onClose}
        onError={onError}
        onDraftCreated={onDraftCreated}
      />
    );
  }
  if ((action === "deactivate" || action === "reactivate") && profile) {
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
