import React, { useEffect, useState } from "react";
import {
  ActionableNotification,
  Column,
  Grid,
  InlineNotification,
  Loading,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { useHistory, useLocation, useParams } from "react-router-dom";
import AnalyzerPageHeader from "../AnalyzerPageHeader";
import { safeInternalPath } from "../../utils/UrlUtils";
import {
  getAnalyzerTypeDraft,
  publishAnalyzerTypeDraft,
} from "../../../services/analyzerService";
import ProfileDraftEditor from "./ProfileDraftEditor";
// The editors' shared form and recognition styles.
import "./AnalyzerTypeManagement.scss";
import "./AnalyzerTypeDraftPage.scss";

// What publishing a draft of each kind did, as the types page names it.
const PUBLISHED = {
  CREATE: "create",
  DUPLICATE: "duplicate",
  UPDATE: "update",
};

const failed = (response) => !response || Boolean(response.error);

/**
 * A type's draft, edited on its own page: what the instrument is and how it
 * talks, its tests, its connection fields and control recognition, then one
 * bar to publish it or leave it saved for later.
 */
// A type started from analyzer setup goes back to setup with it selected.
const setupReturn = (returnTo, published) => {
  const [pathname, search = ""] = returnTo.split("?");
  const params = new URLSearchParams(search);
  const profileId = published?.profile?.profileMeta?.id;
  const revision = published?.profile?.catalog?.revision;
  if (!params.get("setup") || !profileId || !revision) {
    return null;
  }
  params.set("profile", profileId);
  params.set("revision", String(revision));
  return { pathname, search: `?${params.toString()}` };
};

const AnalyzerTypeDraftPage = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const { draftId } = useParams();
  const returnTo = safeInternalPath(
    new URLSearchParams(location.search).get("returnTo"),
    "/analyzers/types",
  );
  const [draft, setDraft] = useState(null);
  const [loadFailed, setLoadFailed] = useState(false);
  const [editorState, setEditorState] = useState({
    publishable: false,
    dirty: false,
  });
  const [publishing, setPublishing] = useState(false);
  const [publishError, setPublishError] = useState(null);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    getAnalyzerTypeDraft(draftId, (response) => {
      if (failed(response) || response.draftId !== draftId) {
        setLoadFailed(true);
        return;
      }
      setLoadFailed(false);
      setDraft(response);
    });
  }, [draftId, attempt]);

  const publish = () => {
    if (!editorState.publishable || publishing) {
      return;
    }
    setPublishing(true);
    setPublishError(null);
    publishAnalyzerTypeDraft(draftId, (response) => {
      setPublishing(false);
      if (failed(response)) {
        setPublishError(
          response?.error ||
            intl.formatMessage({
              id: "analyzerType.notification.action.error.subtitle",
            }),
        );
        return;
      }
      history.push(
        setupReturn(returnTo, response) || {
          pathname: "/analyzers/types",
          state: { analyzerTypeNotice: PUBLISHED[draft.kind] || "create" },
        },
      );
    });
  };

  const name = draft?.profile?.profileMeta?.displayName || "";
  const title = draft
    ? intl.formatMessage(
        {
          id:
            draft.kind === "UPDATE"
              ? "analyzerType.draftPage.title.update"
              : "analyzerType.draftPage.title.new",
        },
        { name },
      )
    : intl.formatMessage({ id: "analyzerType.draft.loading" });
  const subtitle = !draft
    ? null
    : draft.kind === "UPDATE"
      ? intl.formatMessage({ id: "analyzerType.draft.update.subtitle" })
      : draft.kind === "DUPLICATE"
        ? `${intl.formatMessage(
            { id: "analyzerType.lineage" },
            { profileId: draft.baseProfileId, revision: draft.baseRevision },
          )}. ${intl.formatMessage({ id: "analyzerType.draft.created.subtitle" })}`
        : intl.formatMessage({ id: "analyzerType.draft.created.subtitle" });

  return (
    <Grid
      fullWidth
      className="analyzer-type-draft-page"
      data-testid="analyzer-type-draft-page"
    >
      <Column lg={16} md={8} sm={4}>
        <AnalyzerPageHeader
          breadcrumbs={[
            { label: "home.label", link: "/" },
            { label: "analyzer.page.hierarchy.root", link: "/analyzers" },
            { label: "analyzerType.page.title", link: "/analyzers/types" },
            { label: title, isCurrentPage: true },
          ]}
          title={title}
          subtitle={subtitle}
          testId="analyzer-type-draft-header"
        />
        {loadFailed ? (
          <ActionableNotification
            inline
            kind="error"
            lowContrast
            title={intl.formatMessage({
              id: "analyzerType.draftPage.loadError",
            })}
            actionButtonLabel={intl.formatMessage({ id: "common.retry" })}
            onActionButtonClick={() => setAttempt((count) => count + 1)}
          />
        ) : !draft ? (
          <div className="analyzer-type-draft-page__loading">
            <Loading
              withOverlay={false}
              description={intl.formatMessage({
                id: "analyzerType.draft.loading",
              })}
            />
          </div>
        ) : (
          <ProfileDraftEditor
            key={draft.draftId}
            draft={draft}
            onStateChange={setEditorState}
            closeAction={{
              label: intl.formatMessage({ id: "analyzerType.draftPage.close" }),
              onClick: () => history.push(returnTo),
            }}
            publishAction={{
              label: intl.formatMessage({ id: "analyzerType.button.publish" }),
              onClick: publish,
              busy: publishing,
            }}
            notice={
              publishError && (
                <InlineNotification
                  kind="error"
                  lowContrast
                  className="analyzer-type-draft-page__bar-notice"
                  title={intl.formatMessage({
                    id: "analyzerType.notification.action.error",
                  })}
                  subtitle={publishError}
                  onCloseButtonClick={() => setPublishError(null)}
                />
              )
            }
          />
        )}
      </Column>
    </Grid>
  );
};

export default AnalyzerTypeDraftPage;
