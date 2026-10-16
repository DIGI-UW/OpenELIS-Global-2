import React, { useContext, useState, useEffect } from "react";
import AnalyserResults from "./AnalyserResults";
import { AlertDialog } from "../common/CustomNotification";
import { NotificationContext } from "../layout/Layout";
import { Button, Loading, Stack } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Redirect,
  useHistory,
  useLocation,
  useParams,
  useRouteMatch,
} from "react-router-dom";
import { importIssuesPath } from "./analyzerResultsRoutes.jsx";
import { getFromOpenElisServer } from "../utils/Utils";
import { hasServerPages, serverPageSizeOf } from "../utils/serverPaging";
import AnalyzerPageHeader from "../analyzers/AnalyzerPageHeader";
import PageBreadCrumb from "../common/PageBreadCrumb";
import CustomLabNumberInput from "../common/CustomLabNumberInput";
import ImportIssuesPanel from "./ImportIssuesPanel";
import { decisionKey, groupTestParts } from "./ResultParts";

const importIssuesBreadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "analyzer.navigation.analyzers", link: "/analyzers" },
  {
    label: "analyzer.importIssues.title",
    link: importIssuesPath,
  },
];

const testActionFields = ["isAccepted", "isRejected", "isDeleted"];

// Held for a decision the reviewer can make on the page, not for a mapping fix.
const reviewableHolds = ["awaiting_specimen", "awaiting_placement"];

const restorableResultFields = [
  "isAccepted",
  "isRejected",
  "isDeleted",
  "typeOfSampleId",
  "chosenAnalysisId",
  "redirectAccession",
  "redirectReason",
  "note",
  "result",
];

/**
 * The page title for an analyzer worklist. The URL carries the analyzer's id;
 * the name is resolved server-side, so until it arrives (or when the id matches
 * no analyzer) the bare label is shown — the id is never surfaced as a title.
 */
export const analyzerPageTitle = (label, analyzerName) =>
  analyzerName ? `${label}: ${analyzerName}` : label;

export const getAnalyzerResultsView = (search) =>
  new URLSearchParams(search).get("view") || "";

const Index = () => {
  const { notificationVisible } = useContext(NotificationContext);
  const [results, setResults] = useState({ resultList: [] });
  const [restoredEdits, setRestoredEdits] = useState({});
  // The analyzer's display name, resolved server-side from the id in the URL.
  const [analyzerName, setAnalyzerName] = useState("");
  const [queryValue, setQueryValue] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [url, setUrl] = useState("");
  const [searchTermToPage, setSearchTermToPage] = useState([]);
  // The rows a full server page holds, read off the responses; Carbon's items
  // per page is pinned to it so Carbon's page is the server's page.
  const [serverPageSize, setServerPageSize] = useState();
  const [labNumber, setLabNumber] = useState("");
  const location = useLocation();
  const history = useHistory();
  const params = useParams();
  const onImportIssues = Boolean(useRouteMatch({ path: importIssuesPath }));
  const selectedAnalyzerId =
    params.analyzerId || new URLSearchParams(location.search).get("id");
  const worklistDraft = location.state?.worklistDraft;
  const restoringDraft =
    worklistDraft &&
    String(worklistDraft.analyzerId) === String(selectedAnalyzerId);
  const view = onImportIssues
    ? "import-issues"
    : getAnalyzerResultsView(location.search);
  const intl = useIntl();

  useEffect(() => {
    if (!selectedAnalyzerId) {
      return;
    }
    setQueryValue(selectedAnalyzerId);
    setUrl("/rest/AnalyzerResults?id=" + selectedAnalyzerId);
    // drop the previous analyzer's name so a stale title never shows while the
    // new one is in flight
    setAnalyzerName("");
  }, [selectedAnalyzerId]);

  useEffect(() => {
    if (url) {
      setIsLoading(true);
      const page = restoringDraft ? Number(worklistDraft.page) : 1;
      getFromOpenElisServer(
        url + (Number.isInteger(page) && page > 1 ? `&page=${page}` : ""),
        handleResults,
      );
    }
  }, [url]);

  /**
   * Rereads the worklist the address bar names, after a write changes it, and
   * reopens the page the user was on when the reread worklist still has it.
   */
  const refreshResults = (pageToReopen) => {
    if (!url) {
      return;
    }
    setIsLoading(true);
    getFromOpenElisServer(url, (data) => {
      const totalPages = Number(data?.paging?.totalPages) || 1;
      if (pageToReopen > 1 && pageToReopen <= totalPages) {
        getFromOpenElisServer(url + "&page=" + pageToReopen, handleResults);
      } else {
        handleResults(data);
      }
    });
  };

  /** One server page, the same request for the arrows, the lab number search and Carbon. */
  const loadResultsPage = (pageNumber) => {
    setIsLoading(true);
    getFromOpenElisServer(url + "&page=" + pageNumber, handleResults);
  };

  // A test's action checkboxes are shown on its head row, which changes when a
  // held row is recovered; a restored action moves with it.
  const moveTestActionsToHeads = (rows, serverRows, applied) => {
    const { headIdByKey } = groupTestParts(rows);
    const serverById = new Map(serverRows.map((row) => [String(row.id), row]));
    const actionsByTest = new Map();
    rows.forEach((row) => {
      const edits = applied[String(row.id)] ?? {};
      testActionFields.forEach((field) => {
        if (!Object.prototype.hasOwnProperty.call(edits, field)) return;
        const actions = actionsByTest.get(decisionKey(row)) ?? {};
        if (!Object.prototype.hasOwnProperty.call(actions, field)) {
          actions[field] = edits[field];
        }
        actionsByTest.set(decisionKey(row), actions);
      });
    });
    return rows.map((row) => {
      const actions = actionsByTest.get(decisionKey(row));
      if (!actions) return row;
      const id = String(row.id);
      const isHead = String(headIdByKey.get(decisionKey(row))) === id;
      const moved = { ...row };
      const edits = { ...applied[id] };
      Object.entries(actions).forEach(([field, value]) => {
        if (isHead) {
          moved[field] = value;
          edits[field] = value;
        } else if (Object.prototype.hasOwnProperty.call(edits, field)) {
          moved[field] = serverById.get(id)?.[field];
          delete edits[field];
        }
      });
      if (Object.keys(edits).length) {
        applied[id] = edits;
      } else {
        delete applied[id];
      }
      return moved;
    });
  };

  const handleResults = (data) => {
    if (data) {
      const applied = {};
      const restoredList = restoringDraft
        ? data.resultList.map((row) => {
            if (
              row.importIssueReason &&
              !reviewableHolds.includes(row.importIssueReason)
            ) {
              return row;
            }
            const edits = worklistDraft.edits?.[String(row.id)];
            if (!edits) return row;
            const restored = { ...row };
            restorableResultFields.forEach((field) => {
              if (Object.prototype.hasOwnProperty.call(edits, field)) {
                restored[field] = edits[field];
                applied[String(row.id)] = {
                  ...applied[String(row.id)],
                  [field]: edits[field],
                };
              }
            });
            return restored;
          })
        : data.resultList;
      const resultList = restoringDraft
        ? moveTestActionsToHeads(restoredList, data.resultList, applied)
        : restoredList;
      setRestoredEdits(applied);
      setResults({ ...data, resultList });
      if (restoringDraft) {
        const remainingState = { ...location.state };
        delete remainingState.worklistDraft;
        history.replace({
          pathname: location.pathname,
          search: location.search,
          hash: location.hash,
          state: Object.keys(remainingState).length
            ? remainingState
            : undefined,
        });
      }
      setIsLoading(false);
      // the server echoes the analyzer's name in `type`, resolved from the id;
      // it comes back null for an id that matches no analyzer
      if (typeof data.type === "string" && data.type.trim()) {
        setAnalyzerName(data.type.trim());
      }
      setServerPageSize((previous) =>
        serverPageSizeOf(data.paging, data.resultList?.length ?? 0, previous),
      );
      setSearchTermToPage(
        Array.isArray(data.paging?.searchTermToPage)
          ? data.paging.searchTermToPage
          : [],
      );
    }
  };
  if (view === "import-issues") {
    return (
      <>
        <PageBreadCrumb breadcrumbs={importIssuesBreadcrumbs} />
        <ImportIssuesPanel />
      </>
    );
  }

  if (!selectedAnalyzerId) {
    return <Redirect to="/analyzers" />;
  }

  const pageTitle = analyzerPageTitle(
    intl.formatMessage({ id: "banner.menu.results.analyzer" }),
    analyzerName,
  );
  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "analyzer.navigation.analyzers", link: "/analyzers" },
    {
      label: analyzerName || "banner.menu.results.analyzer",
      isCurrentPage: true,
    },
  ];

  return (
    <>
      <div className="orderLegendBody analyzerResults__page">
        <AnalyzerPageHeader
          breadcrumbs={breadcrumbs}
          title={pageTitle}
          subtitle={intl.formatMessage({ id: "analyzer.results.subtitle" })}
        />
        {notificationVisible === true ? <AlertDialog /> : ""}
        {isLoading && <Loading></Loading>}
        {/* A worklist on several server pages: jump to the page holding a lab number. */}
        {hasServerPages(results.paging) && (
          <Stack
            orientation="horizontal"
            gap={4}
            className="analyzerResults__labNumberSearch"
          >
            <CustomLabNumberInput
              name="Lab Number"
              value={labNumber}
              labelText={intl.formatMessage({
                id: "input.placeholder.labNo",
                defaultMessage: "input.placeholder.labNo",
              })}
              id="Lab Number"
              onChange={(e, rawVal) => {
                setLabNumber(rawVal ? rawVal : e?.target?.value);
              }}
            />
            <Button
              kind="tertiary"
              onClick={() => {
                const pageMapping = searchTermToPage.find(
                  (item) => item.id === labNumber,
                );
                if (!pageMapping) {
                  return;
                }
                loadResultsPage(pageMapping.value);
              }}
            >
              <FormattedMessage id="referral.search" />
            </Button>
          </Stack>
        )}
        <AnalyserResults
          analyzerId={queryValue}
          results={results}
          restoredEdits={restoredEdits}
          refreshResults={refreshResults}
          serverPageSize={serverPageSize}
          loadPage={loadResultsPage}
        />
      </div>
    </>
  );
};

export default Index;
