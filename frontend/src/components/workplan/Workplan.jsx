import {
  Button,
  Column,
  Grid,
  Heading,
  Link,
  Section,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Pagination,
} from "@carbon/react";
import React, {
  useCallback,
  useState,
  useContext,
  useEffect,
  useRef,
} from "react";
import "../Style.css";
import "./wpStyle.css";
import { FormattedMessage, useIntl } from "react-intl";
import WorkplanSearchForm from "./WorkplanSearchForm";
import {
  getFromOpenElisServer,
  postToOpenElisServerForPDF,
  convertAlphaNumLabNumForDisplay,
} from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { ConfigurationContext } from "../layout/Layout";
import PageBreadCrumb from "../common/PageBreadCrumb";
import EQABadge from "../eqa/EQABadge";
import {
  serverPageArrowsProps,
  serverPageSizeOf,
  serverPaginationProps,
} from "../utils/serverPaging";
import ServerPageArrows from "../common/ServerPageArrows";

import useInAppNavigation from "../common/useInAppNavigation";

/**
 * A workplan row is one analysis, and its id is the same on every page. A row
 * sent without one, such as a group heading, is known by its place on its
 * server page, which holds while the workplan is loaded: a lab number and test
 * name repeat when an order has the same test on two samples.
 */
const rowKey = (row, page, index) => row.analysisId || `${page}:${index}`;

export default function Workplan(props) {
  const navigate = useInAppNavigation();
  const { configurationProperties } = useContext(ConfigurationContext);
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const [testsList, setTestsList] = useState([]);
  const [subjectOnWorkplan, setSubjectOnWorkplan] = useState(false);
  const [nextVisitOnWorkplan, setNextVisitOnWorkplan] = useState(false);
  const [configurationName, setConfigurationName] = useState("");
  const [selectedValue, setSelectedValue] = useState("");
  const [selectedLabel, setSelectedLabel] = useState("");
  // The server's page announcement for the list shown, and the rows a full
  // server page holds; Carbon's items per page is pinned to the latter so
  // Carbon's page is the server's page.
  const [paging, setPaging] = useState();
  const [serverPageSize, setServerPageSize] = useState();
  const pageLoader = useRef(null);
  const registerPageLoader = useCallback((run) => {
    pageLoader.current = run;
  }, []);
  const loadPage = useCallback(
    (pageNumber) => pageLoader.current?.(pageNumber),
    [],
  );
  // The address of the list's pages, so Print can take every page.
  const pageSource = useRef(null);
  const registerPageSource = useCallback((source) => {
    pageSource.current = source;
  }, []);
  const [excluded, setExcluded] = useState(() => new Set());
  const [printing, setPrinting] = useState(false);
  const [pageFailures, setPageFailures] = useState(0);

  const arrows = serverPageArrowsProps({ paging, onPageRequest: loadPage });

  const type = props.type;
  let title;
  let sourceTitle;
  switch (type) {
    case "test": {
      title = <FormattedMessage id="workplan.test.title" />;
      sourceTitle = "WorkPlanByTest";
      break;
    }
    case "panel": {
      title = <FormattedMessage id="workplan.panel.title" />;
      sourceTitle = "WorkPlanByPanel";
      break;
    }
    case "unit": {
      title = <FormattedMessage id="workplan.unit.title" />;
      sourceTitle = "WorkPlanByTestSection";
      break;
    }
    case "priority": {
      title = <FormattedMessage id="workplan.priority.title" />;
      sourceTitle = "WorkPlanByPriority";
      break;
    }
    default: {
      title = "";
      sourceTitle = "";
    }
  }

  useEffect(() => {
    setSubjectOnWorkplan(configurationProperties.SUBJECT_ON_WORKPLAN);
    setNextVisitOnWorkplan(configurationProperties.NEXT_VISIT_DATE_ON_WORKPLAN);
    setConfigurationName(configurationProperties.configurationName);
  }, [configurationProperties]);

  const reportStatus = (pdfGenerated) => {
    setNotificationVisible(true);
    if (pdfGenerated) {
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "success.report.status" }),
      });
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "error.report.status" }),
      });
    }
  };

  /**
   * The page on screen stays; the pager is rebuilt so it shows that page again
   * rather than the one asked for.
   */
  const pageFailed = () => {
    setPageFailures((failures) => failures + 1);
    setNotificationVisible(true);
    addNotification({
      kind: NotificationKinds.error,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({ id: "workplan.load.failed" }),
    });
  };

  const handleTestsList = (tests) => {
    setTestsList(tests.workplanTests);
    setPaging(tests.paging);
    setServerPageSize((previous) =>
      serverPageSizeOf(
        tests.paging,
        tests.workplanTests?.length ?? 0,
        previous,
      ),
    );
  };
  const handleSelectedValue = (val) => {
    setSelectedValue(val);
    setExcluded(new Set());
  };
  const handleSelectedLabel = (val) => {
    setSelectedLabel(val);
  };

  const fetchFrom = (address) =>
    new Promise((resolve) => {
      getFromOpenElisServer(address, (response) => resolve(response));
    });

  /**
   * Every test in the workplan, not only the page on screen: the printed bench
   * sheet listed one page of many with no notice (OGC-1647). A page without
   * paging means the session no longer holds this workplan, so the search runs
   * once more and the pages are read again.
   */
  const wholeWorkplan = async (reloaded = false) => {
    const keyed = (rows, page) =>
      rows.map((row, index) => ({ row, key: rowKey(row, page, index) }));
    let totalPages = Number(paging?.totalPages) || 1;
    if (!pageSource.current || (totalPages <= 1 && !reloaded)) {
      return keyed(testsList, currentPage);
    }
    if (reloaded) {
      const search = await fetchFrom(pageSource.current);
      if (!search?.paging) {
        return null;
      }
      totalPages = Number(search.paging.totalPages) || 1;
    }
    const rows = [];
    for (let pageNumber = 1; pageNumber <= totalPages; pageNumber++) {
      const response = await fetchFrom(
        `${pageSource.current}&page=${pageNumber}`,
      );
      if (!response?.paging) {
        return reloaded ? null : wholeWorkplan(true);
      }
      if (!Array.isArray(response.workplanTests)) {
        return null;
      }
      rows.push(...keyed(response.workplanTests, pageNumber));
    }
    return rows;
  };

  const printWorkplan = async () => {
    setPrinting(true);
    try {
      const rows = await wholeWorkplan();
      if (!rows) {
        setNotificationVisible(true);
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({ id: "workplan.print.pagesFailed" }),
        });
        return;
      }
      let form = {
        type: type,
        testTypeID: "",
        testSectionId: selectedValue,
        testName: selectedLabel,
        workplanTests: buildWorlplanTestFromTestList(
          rows.map(({ row, key }) => ({
            ...row,
            notIncludedInWorkplan: excluded.has(key),
          })),
        ),
      };
      if (type === "priority") {
        delete form.testSectionId;
      }
      postToOpenElisServerForPDF(
        "/rest/PrintWorkplanReport",
        JSON.stringify(form),
        reportStatus,
      );
    } finally {
      setPrinting(false);
    }
  };

  const buildWorlplanTestFromTestList = (rawData) => {
    let output = [];
    const propertiesToKeep = [
      "accessionNumber",
      "patientInfo",
      "receivedDate",
      "testName",
      "nextVisitDate",
      "notIncludedInWorkplan",
    ];
    rawData.forEach((item) => {
      let obj = {};
      Object.keys(item).filter((key) => {
        if (propertiesToKeep.indexOf(key) > -1) {
          Object.assign(obj, {
            [key]: item[key],
          });
        }
        return true;
      });
      output.push(obj);
    });
    return output;
  };

  const setIncluded = (key, removed) => {
    setExcluded((previous) => {
      const next = new Set(previous);
      if (removed) {
        next.add(key);
      } else {
        next.delete(key);
      }
      return next;
    });
  };

  const currentPage = Number(paging?.currentPage) || 1;
  const pagerProps = serverPaginationProps({
    paging,
    rowsOnPage: testsList.length,
    pageSize: serverPageSize,
    onPageRequest: loadPage,
    intl,
  });
  const totalTests =
    paging?.totalResults !== undefined && paging?.totalResults !== null
      ? Number(paging.totalResults)
      : testsList.filter((row) => !row.servingAsTestGroupIdentifier).length;

  let rowColorIndex = 2;
  let showAccessionNumber = false;
  let currentAccessionNumber = "";

  let breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: `workplan.${type}.title`, link: `/${sourceTitle}` },
  ];

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        {notificationVisible === true ? <AlertDialog /> : ""}
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>{title}</Heading>
            </Section>
          </Section>
        </Column>
        <br />
        <br />
      </Grid>
      <div className="orderLegendBody">
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <WorkplanSearchForm
              type={type}
              createTestsList={handleTestsList}
              onPageFailed={pageFailed}
              selectedValue={handleSelectedValue}
              selectedLabel={handleSelectedLabel}
              registerPageLoader={registerPageLoader}
              registerPageSource={registerPageSource}
            />
          </Column>
        </Grid>
        {testsList.length !== 0 && (
          <>
            <hr />
            <br />
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <Button
                  size="md"
                  type="button"
                  name="print"
                  id="print-top"
                  disabled={printing}
                  onClick={printWorkplan}
                >
                  <FormattedMessage id="workplan.print" />
                </Button>
              </Column>
            </Grid>
            <br />
            <Grid fullWidth={true}>
              <Column sm={4} md={8} lg={16}>
                <FormattedMessage id="label.total.tests" /> = {totalTests}
                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                <img
                  src={`images/nonconforming.gif`}
                  alt="nonconforming"
                /> = <FormattedMessage id="result.nonconforming.item" />
                <br />
                <br />
              </Column>
            </Grid>
            <Grid fullWidth={true}>
              <Column sm={4} md={8} lg={16}>
                <>
                  {arrows.show && <ServerPageArrows {...arrows} />}
                  <Table size={"sm"} data-cy="workplanResultsTable">
                    <TableHead>
                      <TableRow>
                        <TableHeader>
                          <FormattedMessage id="label.button.remove" />
                        </TableHeader>
                        {type === "test" && <TableHeader>&nbsp;</TableHeader>}
                        <TableHeader>
                          <FormattedMessage id="quick.entry.accession.number" />
                        </TableHeader>
                        {subjectOnWorkplan?.toLowerCase() === "true" && (
                          <TableHeader>
                            <FormattedMessage id="patient.subject.number" />
                          </TableHeader>
                        )}
                        {nextVisitOnWorkplan?.toLowerCase() === "true" && (
                          <TableHeader>
                            <FormattedMessage id="sample.entry.nextVisit.date" />
                          </TableHeader>
                        )}
                        {type !== "test" && <TableHeader>&nbsp;</TableHeader>}
                        {type !== "test" && (
                          <TableHeader>
                            {configurationName === "Haiti LNSP" ? (
                              <FormattedMessage
                                id="sample.entry.project.patient.and.testName"
                                values={{ br: <br /> }}
                              />
                            ) : (
                              <FormattedMessage id="sample.entry.project.testName" />
                            )}
                          </TableHeader>
                        )}
                        <TableHeader>
                          <FormattedMessage id="sample.receivedDate" />
                        </TableHeader>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {testsList.map((row, index) => {
                        if (!(row.accessionNumber === currentAccessionNumber)) {
                          showAccessionNumber = true;
                          currentAccessionNumber = row.accessionNumber;
                          rowColorIndex++;
                        } else {
                          showAccessionNumber = false;
                        }
                        const key = rowKey(row, currentPage, index);
                        return (
                          <TableRow
                            key={key}
                            id={"row_" + index}
                            className={
                              rowColorIndex % 2 === 0 ? "evenRow" : "oddRow"
                            }
                            style={
                              excluded.has(key)
                                ? { backgroundColor: "#cccccc" }
                                : undefined
                            }
                          >
                            {!row.servingAsTestGroupIdentifier && (
                              <TableCell>
                                <input
                                  type="checkbox"
                                  checked={excluded.has(key)}
                                  id={"includedCheck_" + index}
                                  className="includedCheck"
                                  onChange={(e) =>
                                    setIncluded(key, e.target.checked)
                                  }
                                />
                              </TableCell>
                            )}
                            {type === "test" && (
                              <TableCell>
                                {row.nonconforming && (
                                  <img
                                    src={`images/nonconforming.gif`}
                                    alt="nonconforming"
                                  />
                                )}
                              </TableCell>
                            )}
                            <TableCell>
                              {showAccessionNumber && (
                                <>
                                  <Link
                                    style={{ color: "blue" }}
                                    href={
                                      `/Results?accessionNumber=` +
                                      encodeURIComponent(row.accessionNumber)
                                    }
                                    onClick={navigate(
                                      `/Results?accessionNumber=` +
                                        encodeURIComponent(row.accessionNumber),
                                    )}
                                  >
                                    <u>
                                      {convertAlphaNumLabNumForDisplay(
                                        row.accessionNumber,
                                      )}
                                    </u>
                                  </Link>
                                  {row.eqaSample && (
                                    <EQABadge priority={row.eqaPriority} />
                                  )}
                                </>
                              )}
                            </TableCell>
                            {subjectOnWorkplan?.toLowerCase() === "true" && (
                              <TableCell>
                                {showAccessionNumber && row.patientInfo}
                              </TableCell>
                            )}
                            {nextVisitOnWorkplan?.toLowerCase() === "true" && (
                              <TableCell>
                                {showAccessionNumber && row.nextVisitDate}
                              </TableCell>
                            )}
                            {type !== "test" && (
                              <TableCell>
                                {row.nonconforming && (
                                  <img
                                    src={`images/nonconforming.gif`}
                                    alt="nonconforming"
                                  />
                                )}
                              </TableCell>
                            )}
                            {type !== "test" && (
                              <TableCell>{row.testName}</TableCell>
                            )}
                            <TableCell>{row.receivedDate}</TableCell>
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                  <Pagination
                    {...pagerProps}
                    key={`${pagerProps.key}-${pageFailures}`}
                  />
                </>
              </Column>
              <hr />
            </Grid>
            <br />
            <Grid fullWidth={true}>
              <Column sm={4} md={8} lg={16}>
                <Button
                  size="md"
                  type="button"
                  name="print"
                  id="print-bottom"
                  disabled={printing}
                  onClick={printWorkplan}
                >
                  <FormattedMessage id="workplan.print" />
                </Button>
              </Column>
            </Grid>
          </>
        )}
        {selectedValue && testsList.length === 0 && (
          <h4>
            <Grid>
              <Column sm={4} md={8} lg={16}>
                <FormattedMessage id="result.noTestsFound" />
              </Column>
            </Grid>
          </h4>
        )}
      </div>
    </>
  );
}
