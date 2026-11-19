import React, { useContext, useEffect, useRef, useState } from "react";
import {
  Button,
  Column,
  Grid,
  Heading,
  Link,
  Section,
  SkeletonText,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { getFromOpenElisServer, Roles } from "../utils/Utils";
import useSiteDateFormat from "./useSiteDateFormat";
import "./priorResultsPanel.scss";

/**
 * The patient's earlier anatomic-pathology cases, read under the patient band
 * of the pathology, immunohistochemistry and cytology case views. It reads the
 * list once per case and never after a save: the cases it lists are other
 * cases, and a save on this one does not change them.
 */

// The lab unit each bench's case view is routed under, and where it opens.
const BENCHES = {
  PATHOLOGY: {
    unit: "Pathology",
    route: "/PathologyCaseView/",
    titleKey: "pathology.label.title",
  },
  IMMUNOHISTOCHEMISTRY: {
    unit: "Immunohistochemistry",
    route: "/ImmunohistochemistryCaseView/",
    titleKey: "immunohistochemistry.label.title",
  },
  CYTOLOGY: {
    unit: "Cytology",
    route: "/CytologyCaseView/",
    titleKey: "cytology.label.title",
  },
};

const CONCLUSION_KEYS = {
  NOT_REPORTED: "caseView.priorResults.notReported",
  NONE_RECORDED: "caseView.priorResults.noConclusion",
  IN_REPORT: "caseView.priorResults.inReport",
};

const benchOf = (bench) => BENCHES[String(bench ?? "").toUpperCase()];

/**
 * Whether the user may open a bench's case view: the rule the route itself
 * applies, so a link the route would refuse is never offered.
 */
export const canOpenBench = (userSessionDetails, bench) => {
  const map = userSessionDetails?.userLabRolesMap;
  const unit = benchOf(bench)?.unit;
  if (!map || !unit) {
    return false;
  }
  const roles = map.AllLabUnits ? map.AllLabUnits : map[unit] || [];
  return roles.includes(Roles.RESULTS);
};

// Anything but {rows: [...], total: n} is a read that failed: the case-view
// tests, and an error body, answer with something else entirely. The answer
// also says whether the user's lab units limit the list; an answer without
// that flag is still good and reads as not limited.
const isAnswer = (answer) =>
  answer !== null &&
  typeof answer === "object" &&
  Array.isArray(answer.rows) &&
  typeof answer.total === "number";

const isRestricted = (answer) => answer?.restricted === true;

const PriorResultsBody = ({ url }) => {
  const intl = useIntl();
  const { formatIsoDate } = useSiteDateFormat();
  const { userSessionDetails } = useContext(UserSessionDetailsContext) || {};
  const [capped, setCapped] = useState(null);
  const [loadFailed, setLoadFailed] = useState(false);
  const [everyRow, setEveryRow] = useState(null);
  const [showingAll, setShowingAll] = useState(false);
  const [loadingAll, setLoadingAll] = useState(false);
  const [allFailed, setAllFailed] = useState(false);
  const allController = useRef(null);
  const toggleRef = useRef(null);
  const refocusToggle = useRef(false);

  useEffect(() => {
    const controller = new AbortController();
    getFromOpenElisServer(
      url,
      (answer) => {
        if (controller.signal.aborted) {
          return;
        }
        if (isAnswer(answer)) {
          setCapped(answer);
        } else {
          setLoadFailed(true);
        }
      },
      controller.signal,
    );
    return () => {
      controller.abort();
      allController.current?.abort();
      allController.current = null;
    };
  }, [url]);

  // The button is disabled while every row is read, and a browser drops focus
  // from a disabled control to the page. It is the control the user just
  // pressed, so focus goes back to it, unless the user has moved on meanwhile.
  useEffect(() => {
    if (!loadingAll && refocusToggle.current) {
      refocusToggle.current = false;
      const focused = document.activeElement;
      if (!focused || focused === document.body) {
        toggleRef.current?.focus();
      }
    }
  }, [loadingAll]);

  const toggle = () => {
    if (showingAll) {
      setShowingAll(false);
      return;
    }
    if (everyRow) {
      setShowingAll(true);
      return;
    }
    const controller = new AbortController();
    allController.current = controller;
    refocusToggle.current = document.activeElement === toggleRef.current;
    setAllFailed(false);
    setLoadingAll(true);
    getFromOpenElisServer(
      `${url}?all=true`,
      (answer) => {
        if (controller.signal.aborted) {
          return;
        }
        if (isAnswer(answer)) {
          setEveryRow(answer);
          setShowingAll(true);
        } else {
          setAllFailed(true);
        }
        setLoadingAll(false);
      },
      controller.signal,
    );
  };

  const benchName = (rowBench) => {
    const known = benchOf(rowBench);
    return known ? intl.formatMessage({ id: known.titleKey }) : rowBench;
  };

  const labNumberCell = (row) => {
    const known = benchOf(row.bench);
    if (!known || !canOpenBench(userSessionDetails, row.bench)) {
      return row.labNumber;
    }
    return (
      <Link
        href={known.route + row.caseId}
        target="_blank"
        rel="noopener noreferrer"
        aria-label={intl.formatMessage(
          { id: "caseView.priorResults.openCase" },
          { bench: benchName(row.bench), labNumber: row.labNumber },
        )}
      >
        {row.labNumber}
      </Link>
    );
  };

  const conclusionOf = (row) => {
    if (row.conclusionState === "REPORTED") {
      return row.conclusion;
    }
    const key = CONCLUSION_KEYS[row.conclusionState];
    return key ? intl.formatMessage({ id: key }) : "";
  };

  const specimenOf = (row) =>
    String(row.specimen ?? "").trim()
      ? row.specimen
      : intl.formatMessage({ id: "common.notRecorded" });

  const renderBody = () => {
    if (loadFailed) {
      return null;
    }
    if (!capped) {
      return (
        <div data-testid="prior-results-loading">
          <SkeletonText paragraph lineCount={2} />
        </div>
      );
    }
    if (capped.total === 0) {
      return (
        <p className="prior-results__message">
          <FormattedMessage
            id={
              isRestricted(capped)
                ? "caseView.priorResults.noneInYourUnits"
                : "pathology.empty.noPriorResults"
            }
          />
        </p>
      );
    }
    const rows = showingAll && everyRow ? everyRow.rows : capped.rows;
    return (
      <>
        <Table size="sm" aria-labelledby="prior-results-title">
          <TableHead>
            <TableRow>
              <TableHeader>
                <FormattedMessage id="sample.label.labnumber" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="sample.requestDate" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="caseView.label.specimen" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="pathology.label.conclusion" />
              </TableHeader>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((row) => (
              <TableRow
                key={`${row.bench}-${row.caseId}`}
                data-testid={`prior-result-${row.bench}-${row.caseId}`}
              >
                <TableCell className="prior-results__nowrap">
                  {labNumberCell(row)}
                  <div className="prior-results__bench">
                    {benchName(row.bench)}
                  </div>
                </TableCell>
                <TableCell className="prior-results__nowrap">
                  {formatIsoDate(row.requestDate)}
                </TableCell>
                <TableCell>{specimenOf(row)}</TableCell>
                <TableCell className="prior-results__conclusion">
                  {conclusionOf(row)}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        {isRestricted(capped) && (
          <p className="prior-results__message">
            <FormattedMessage id="caseView.priorResults.otherUnitsNotListed" />
          </p>
        )}
      </>
    );
  };

  // A screen reader announces a status element's new text only when the
  // element was already on the page, so it stays mounted and only its text
  // comes and goes.
  const failed = loadFailed || allFailed;

  return (
    <Grid fullWidth={true} className="prior-results">
      <Column lg={16} md={8} sm={4}>
        <Section
          level={4}
          data-testid="prior-results"
          aria-busy={!capped && !loadFailed}
        >
          <Heading id="prior-results-title">
            <FormattedMessage id="caseView.label.priorResults" />
          </Heading>
          {renderBody()}
          <div role="status">
            {failed && (
              <p className="prior-results__message">
                <FormattedMessage id="caseView.priorResults.loadFailed" />
              </p>
            )}
          </div>
          {capped && capped.total > capped.rows.length && (
            <Button
              ref={toggleRef}
              kind="ghost"
              size="sm"
              className="prior-results__toggle"
              data-testid="prior-results-toggle"
              aria-expanded={showingAll}
              disabled={loadingAll}
              onClick={toggle}
            >
              {showingAll
                ? intl.formatMessage({ id: "caseView.priorResults.showFewer" })
                : intl.formatMessage(
                    { id: "caseView.priorResults.showAll" },
                    { total: capped.total },
                  )}
            </Button>
          )}
        </Section>
      </Column>
    </Grid>
  );
};

// A new case mounts a fresh panel, so nothing read for the case that was left
// carries over.
const PriorResultsPanel = ({ bench, caseId }) => {
  const url = `/rest/${bench}/caseView/${caseId}/priorResults`;
  return <PriorResultsBody key={url} url={url} />;
};

export default PriorResultsPanel;
