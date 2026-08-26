import React, { useContext, useState, useEffect } from "react";
import AnalyserResults from "./AnalyserResults";
import { AlertDialog } from "../common/CustomNotification";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds } from "../common/CustomNotification";
import {
  Heading,
  Grid,
  Column,
  Section,
  Link,
  Button,
  Loading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { Redirect, useLocation } from "react-router-dom";
import { getFromOpenElisServer } from "../utils/Utils";
import { ArrowLeft, ArrowRight } from "@carbon/react/icons";
import PageBreadCrumb from "../common/PageBreadCrumb";
import CustomLabNumberInput from "../common/CustomLabNumberInput";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "banner.menu.results", link: "" },
  { label: "banner.menu.results.analyzer", link: "/AnalyzerResults" },
];

/**
 * The page title for an analyzer worklist. The URL carries the analyzer's id;
 * the name is resolved server-side, so until it arrives (or when the id matches
 * no analyzer) the bare label is shown — the id is never surfaced as a title.
 */
export const analyzerPageTitle = (label, analyzerName) =>
  analyzerName ? `${label}: ${analyzerName}` : label;

const Index = () => {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const [results, setResults] = useState({ resultList: [] });
  // The analyzer's display name, resolved server-side from the id in the URL.
  const [analyzerName, setAnalyzerName] = useState("");
  const [queryValue, setQueryValue] = useState("");
  const [nextPage, setNextPage] = useState(null);
  const [previousPage, setPreviousPage] = useState(null);
  const [pagination, setPagination] = useState(false);
  const [currentApiPage, setCurrentApiPage] = useState(null);
  const [totalApiPages, setTotalApiPages] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [url, setUrl] = useState("");
  const [sampleGroup, setSampleGroup] = useState([]);
  const [searchTermToPage, setSearchTermToPage] = useState([]);
  const [labNumber, setLabNumber] = useState("");
  const intl = useIntl();

  const location = useLocation();
  const selectedAnalyzerId = new URLSearchParams(location.search).get("id");

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
      getFromOpenElisServer(url, handleResults);
    }
  }, [url]);

  const extractUniqueGroups = (data) => {
    const seenGroups = new Set();
    return data.filter((item) => {
      if (!seenGroups.has(item.sampleGroupingNumber)) {
        seenGroups.add(item.sampleGroupingNumber);
        return true;
      }
      return false;
    });
  };

  const loadNextResultsPage = () => {
    setIsLoading(true);
    getFromOpenElisServer(url + "&page=" + nextPage, handleResults);
  };

  const loadPreviousResultsPage = () => {
    setIsLoading(true);
    getFromOpenElisServer(url + "&page=" + previousPage, handleResults);
  };

  const handleResults = (data) => {
    if (data) {
      setResults(data);
      setIsLoading(false);
      // the server echoes the analyzer's name in `type`, resolved from the id;
      // it comes back null for an id that matches no analyzer
      if (typeof data.type === "string" && data.type.trim()) {
        setAnalyzerName(data.type.trim());
      }
      if (data.paging) {
        var { totalPages, currentPage, searchTermToPage } = data.paging;
        setSearchTermToPage(
          Array.isArray(searchTermToPage) ? searchTermToPage : [],
        );
        if (totalPages > 1) {
          setPagination(true);
          setCurrentApiPage(currentPage);
          setTotalApiPages(totalPages);
          if (parseInt(currentPage) < parseInt(totalPages)) {
            setNextPage(parseInt(currentPage) + 1);
          } else {
            setNextPage(null);
          }
          if (parseInt(currentPage) > 1) {
            setPreviousPage(parseInt(currentPage) - 1);
          } else {
            setPreviousPage(null);
          }
        }
      }

      if (data.resultList.length == 0) {
        addNotification({
          kind: NotificationKinds.warning,
          title: intl.formatMessage({ id: "notification.title" }),
          message:
            intl.formatMessage({ id: "validation.search.noresult.analyser" }) +
            (data.type || analyzerName || queryValue),
        });
        setNotificationVisible(true);
      } else {
        setSampleGroup(extractUniqueGroups(data.resultList));
      }
    }
  };

  if (!selectedAnalyzerId) {
    return <Redirect to="/analyzers" />;
  }

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                {analyzerPageTitle(
                  intl.formatMessage({ id: "banner.menu.results.analyzer" }),
                  analyzerName,
                )}
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <div className="orderLegendBody">
        {notificationVisible === true ? <AlertDialog /> : ""}
        {isLoading && <Loading></Loading>}
        <>
          <Grid>
            <Column lg={8} md={4} sm={4}>
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
            </Column>
            <Column lg={2} md={8} sm={4}>
              <Button
                style={{ marginTop: "20px" }}
                onClick={() => {
                  const pageMapping = searchTermToPage.find(
                    (item) => item.id === labNumber,
                  );
                  if (!pageMapping) {
                    return;
                  }
                  setIsLoading(true);
                  getFromOpenElisServer(
                    url + "&page=" + pageMapping.value,
                    handleResults,
                  );
                }}
              >
                <FormattedMessage id="referral.search" />{" "}
              </Button>
            </Column>
            {pagination && (
              <>
                <Column lg={4} md={4} sm={2}></Column>
                <Column
                  lg={2}
                  style={{
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    gap: "10px",
                    width: "110%",
                  }}
                >
                  <Link>
                    {currentApiPage} / {totalApiPages}
                  </Link>
                  <div style={{ display: "flex", gap: "10px" }}>
                    <Button
                      hasIconOnly
                      id="loadpreviousresults"
                      onClick={loadPreviousResultsPage}
                      disabled={previousPage != null ? false : true}
                      renderIcon={ArrowLeft}
                      iconDescription="previous"
                    ></Button>
                    <Button
                      hasIconOnly
                      id="loadnextresults"
                      onClick={loadNextResultsPage}
                      disabled={nextPage != null ? false : true}
                      renderIcon={ArrowRight}
                      iconDescription="next"
                    ></Button>
                  </div>
                </Column>
              </>
            )}
          </Grid>
        </>
        <AnalyserResults
          analyzerId={queryValue}
          results={results}
          sampleGroup={sampleGroup}
        />
      </div>
    </>
  );
};

export default Index;
