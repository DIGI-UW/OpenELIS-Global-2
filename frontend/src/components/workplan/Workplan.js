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
import React, { useState, useContext, useEffect } from "react";
import "../Style.css";
import "./wpStyle.css";
import { FormattedMessage, useIntl } from "react-intl";
import WorkplanSearchForm from "./WorkplanSearchForm";
import {
  postToOpenElisServerForPDF,
  convertAlphaNumLabNumForDisplay,
} from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { ConfigurationContext } from "../layout/Layout";
import PageBreadCrumb from "../common/PageBreadCrumb";

export default function Workplan(props) {
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
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(100);

  const type = props.type;
  let title = "";
  let sourceTitle = "";
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
  }, []);

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

  const handleTestsList = (tests) => {
    setTestsList(tests.workplanTests);
  };
  const handleSelectedValue = (val) => {
    setSelectedValue(val);
  };
  const handleSelectedLabel = (val) => {
    setSelectedLabel(val);
  };
  const handlePageChange = (pageInfo) => {
    if (page != pageInfo.page) {
      setPage(pageInfo.page);
    }

    if (pageSize != pageInfo.pageSize) {
      setPageSize(pageInfo.pageSize);
    }
  };

  const printWorkplan = () => {
    let form = {
      type: type,
      testTypeID: "",
      testSectionId: selectedValue,
      testName: selectedLabel,
      workplanTests: buildWorlplanTestFromTestList(testsList),
    };
    if (type === "priority") {
      delete form.testSectionId;
    }
    postToOpenElisServerForPDF(
      "/rest/PrintWorkplanReport",
      JSON.stringify(form),
      reportStatus,
    );
  };

  const buildWorlplanTestFromTestList = (rawData) => {
    let output = [];
    const propertiesToKeep = [
      "accessionNumber",
      "patientInfo",
      "receivedDate",
      "testName",
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

  const disableEnableTest = (checkbox, index) => {
    if (checkbox.checked) {
      document.querySelector("#row_" + index).style.backgroundColor = "#cccccc";
      testsList[index].notIncludedInWorkplan = true;
    } else {
      checkbox.checked = "";
      document.querySelector("#row_" + index).style.backgroundColor = "";
      testsList[index].notIncludedInWorkplan = false;
    }
  };

  let rowColorIndex = 2;
  let showAccessionNumber = false;
  let currentAccessionNumber = "";

  let breadcrumbs = [{ label: "home.label", link: "/" }];

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
              selectedValue={handleSelectedValue}
              selectedLabel={handleSelectedLabel}
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
                  id="print"
                  onClick={printWorkplan}
                >
                  <FormattedMessage id="workplan.print" />
                </Button>
              </Column>
            </Grid>
            <br />
            <Grid fullWidth={true}>
              <Column sm={4} md={8} lg={16}>
                <FormattedMessage id="label.total.tests" /> = {testsList.length}
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
                      {testsList
                        .slice((page - 1) * pageSize, page * pageSize)
                        .map((row, index) => {
                          if (
                            !(row.accessionNumber === currentAccessionNumber)
                          ) {
                            showAccessionNumber = true;
                            currentAccessionNumber = row.accessionNumber;
                            rowColorIndex++;
                          } else {
                            showAccessionNumber = false;
                          }
                          return (
                            <TableRow
                              key={index}
                              id={"row_" + index}
                              className={
                                rowColorIndex % 2 === 0 ? "evenRow" : "oddRow"
                              }
                            >
                              {!row.servingAsTestGroupIdentifier && (
                                <TableCell>
                                  <input
                                    type="checkbox"
                                    value={row.notIncludedInWorkplan}
                                    id={"includedCheck_" + index}
                                    className="includedCheck"
                                    onClick={(e) =>
                                      disableEnableTest(e.target, index)
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
                                  <Link
                                    style={{ color: "blue" }}
                                    href={
                                      `/result?type=order&doRange=false&source=${sourceTitle}&accessionNumber=` +
                                      row.accessionNumber
                                    }
                                  >
                                    <u>
                                      {convertAlphaNumLabNumForDisplay(
                                        row.accessionNumber,
                                      )}
                                    </u>
                                  </Link>
                                )}
                              </TableCell>
                              {subjectOnWorkplan?.toLowerCase() === "true" && (
                                <TableCell>
                                  {showAccessionNumber && row.patientInfo}
                                </TableCell>
                              )}
                              {nextVisitOnWorkplan?.toLowerCase() ===
                                "true" && (
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
                    onChange={handlePageChange}
                    page={page}
                    pageSize={pageSize}
                    pageSizes={[10, 20, 30, 50, 100]}
                    totalItems={testsList.length}
                    forwardText={intl.formatMessage({
                      id: "pagination.forward",
                    })}
                    backwardText={intl.formatMessage({
                      id: "pagination.backward",
                    })}
                    itemRangeText={(min, max, total) =>
                      intl.formatMessage(
                        { id: "pagination.item-range" },
                        { min: min, max: max, total: total },
                      )
                    }
                    itemsPerPageText={intl.formatMessage({
                      id: "pagination.items-per-page",
                    })}
                    itemText={(min, max) =>
                      intl.formatMessage(
                        { id: "pagination.item" },
                        { min: min, max: max },
                      )
                    }
                    pageNumberText={intl.formatMessage({
                      id: "pagination.page-number",
                    })}
                    pageRangeText={(_current, total) =>
                      intl.formatMessage(
                        { id: "pagination.page-range" },
                        { total: total },
                      )
                    }
                    pageText={(page, pagesUnknown) =>
                      intl.formatMessage(
                        { id: "pagination.page" },
                        { page: pagesUnknown ? "" : page },
                      )
                    }
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
                  id="print"
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
