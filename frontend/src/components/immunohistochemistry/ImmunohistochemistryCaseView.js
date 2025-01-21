import React, { useContext, useState, useEffect, useRef } from "react";
import { useParams } from "react-router-dom";
import config from "../../config.json";
import {
  IconButton,
  Heading,
  TextInput,
  Select,
  SelectItem,
  Button,
  Grid,
  Column,
  Checkbox,
  FileUploader,
  Tag,
  Section,
  Stack,
  Loading,
  InlineLoading,
  Toggle,
  TextArea,
  FilterableMultiSelect,
  Link,
} from "@carbon/react";
import { Launch, Subtract, ArrowLeft, ArrowRight } from "@carbon/react/icons";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  postToOpenElisServerForPDF,
  hasRole,
} from "../utils/Utils";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { SearchResults } from "../resultPage/SearchResultForm";
import { FormattedMessage, useIntl } from "react-intl";
import PatientHeader from "../common/PatientHeader";
import QuestionnaireResponse from "../common/QuestionnaireResponse";
import "./../pathology/PathologyDashboard.css";
import PageBreadCrumb from "../common/PageBreadCrumb";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  {
    label: "immunohistochemistry.label.dashboard",
    link: "/ImmunohistochemistryDashboard",
  },
];

function ImmunohistochemistryCaseView() {
  const componentMounted = useRef(false);

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const { immunohistochemistrySampleId } = useParams();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const [immunohistochemistrySampleInfo, setImmunohistochemistrySampleInfo] =
    useState({ labNumber: "" });

  const [conclusions, setConclusions] = useState([]);

  const [statuses, setStatuses] = useState([]);
  const [reportTypes, setReportTypes] = useState([]);
  const [technicianUsers, setTechnicianUsers] = useState([]);
  const [pathologistUsers, setPathologistUsers] = useState([]);
  const [results, setResults] = useState({ testResult: [] });
  const [loading, setLoading] = useState(true);
  const [resultsLoading, setResultsLoading] = useState(true);
  const [loadingReport, setLoadingReport] = useState(false);
  const [intensityList, setIntensityList] = useState([]);
  const [cerbB2PatternList, setCerbB2PatternList] = useState([]);
  const [molecularSubTypeList, setMolecularSubTypeList] = useState([]);
  const [nextPage, setNextPage] = useState(null);
  const [previousPage, setPreviousPage] = useState(null);
  const [pagination, setPagination] = useState(false);
  const [currentApiPage, setCurrentApiPage] = useState(null);
  const [totalApiPages, setTotalApiPages] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [reportParams, setReportParams] = useState({
    0: {
      erPercent: "",
      erIntensity: "",
      erScore: "",
      prPercent: "",
      prIntensity: "",
      prScore: "",
      mib: "",
      pattern: "",
      herAssesment: "",
      herScore: "",
      diagnosis: "",
      molecularSubType: "",
      conclusion: "",
      codedConclusions: [],
      ihcScore: "",
      ihcRatio: "",
      averageChrom: "",
      averageHer2: "",
      numberOfcancerNuclei: "",
      toggled: false,
      submited: false,
      reportLink: "",
    },
  });

  async function displayStatus(response) {
    var body = await response.json();
    console.debug(body);
    setIsSubmitting(false);
    var status = response.status;
    setNotificationVisible(true);
    if (status == "200") {
      const save1 = document.getElementById("pathology_save");
      const save2 = document.getElementById("pathology_save2");
      save1.disabled = true;
      save2.disabled = true;
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "save.success" }),
      });
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "error.save.msg" }),
      });
    }
  }

  const reportStatus = async (pdfGenerated, blob, index) => {
    setNotificationVisible(true);
    setLoadingReport(false);
    if (pdfGenerated) {
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "success.report.status" }),
      });
      var params = { ...reportParams };
      if (!params[index]) {
        params[index] = {};
      }
      params[index].submited = true;
      params[index].toggled = false;
      params[index].reportLink = window.URL.createObjectURL(blob, {
        type: "application/pdf",
      });
      setReportParams(params);

      var newReports = [...immunohistochemistrySampleInfo.reports];
      let encodedFile = await toBase64(blob);
      newReports[index].base64Image = encodedFile;

      setImmunohistochemistrySampleInfo({
        ...immunohistochemistrySampleInfo,
        reports: newReports,
      });
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "error.report.status" }),
      });
    }
  };

  const getReportName = (reportType) => {
    switch (reportType) {
      case "DUAL_IN_SITU_HYBRIDISATION":
        return "DualInSituHybridizationReport";
      case "BREAST_CANCER_HORMONE_RECEPTOR":
        return "BreastCancerHormoneReceptorReport";
      case "IMMUNOHISTOCHEMISTRY":
        return "PatientImmunoChemistryReport";
    }
  };

  const toggleReportParam = (e, index) => {
    const params = { ...reportParams };
    if (!params[index]) {
      params[index] = {};
    }
    params[index]["toggled"] = e;
    setReportParams(params);
  };

  useEffect(() => {
    componentMounted.current = true;

    setNextPage(null);
    setPreviousPage(null);
    setPagination(false);
    getFromOpenElisServer(
      "/rest/paginatedDisplayList/PATHOLOGIST_CONCLUSIONS",
      loadConclusionData,
    );

    return () => {
      componentMounted.current = false;
    };
  }, []);

  const loadNextCOnclusionsPage = () => {
    setLoading(true);
    getFromOpenElisServer(
      "/rest/paginatedDisplayList/PATHOLOGIST_CONCLUSIONS" +
        "?page=" +
        nextPage,
      loadConclusionData,
    );
  };

  const loadPreviousConclusionsPage = () => {
    setLoading(true);
    getFromOpenElisServer(
      "/rest/paginatedDisplayList/PATHOLOGIST_CONCLUSIONS" +
        "?page=" +
        previousPage,
      loadConclusionData,
    );
  };

  const loadConclusionData = (res) => {
    // If the response object is not null and has displayItems array with length greater than 0 then set it as data.
    if (res && res.displayItems && res.displayItems.length > 0) {
      setConclusions(res.displayItems);
    } else {
      setConclusions([]);
    }

    // Sets next and previous page numbers based on the total pages and current page number.
    if (res && res.paging) {
      const { totalPages, currentPage } = res.paging;
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

    setLoading(false);
  };

  const createReportParams = (reportType, index) => {
    switch (reportType) {
      case "BREAST_CANCER_HORMONE_RECEPTOR":
        return (
          <>
            {" "}
            <Column lg={16} md={8} sm={4}>
              <Grid fullWidth={true} className="gridBoundary">
                <Column lg={2} md={8} sm={4}>
                  ER
                </Column>
                <Column lg={2} md={8} sm={4}>
                  <TextInput
                    id={"erPercent_" + index}
                    labelText=""
                    hideLabel={true}
                    type="number"
                    value={reportParams[index]?.erPercent}
                    onChange={(e) => {
                      var params = { ...reportParams };
                      if (!params[index]) {
                        params[index] = {};
                      }
                      params[index].erPercent = e.target.value;
                      setReportParams(params);
                    }}
                  />
                </Column>
                <Column lg={3} md={8} sm={4}>
                  <FormattedMessage id="immunohistochemistry.label.cellPercent" />
                </Column>
                <Column lg={2} md={8} sm={4}>
                  <Select
                    id={"erIntensity_" + index}
                    name="status"
                    labelText=""
                    value={reportParams[index]?.erIntensity}
                    onChange={(e) => {
                      var params = { ...reportParams };
                      if (!params[index]) {
                        params[index] = {};
                      }
                      params[index].erIntensity = e.target.value;
                      setReportParams(params);
                    }}
                  >
                    <SelectItem disabled value="placeholder" text="Intensity" />
                    <SelectItem value="" text="" />
                    {intensityList.map((status, index) => {
                      return (
                        <SelectItem
                          key={index}
                          text={status.value}
                          value={status.value}
                        />
                      );
                    })}
                  </Select>
                </Column>
                <Column lg={3} md={8} sm={4}>
                  <FormattedMessage id="immunohistochemistry.label.cellIntensity" />
                </Column>
                <Column lg={2} md={8} sm={4}>
                  <TextInput
                    id={"erScore_" + index}
                    labelText=""
                    hideLabel={true}
                    type="number"
                    value={reportParams[index]?.erScore}
                    onChange={(e) => {
                      var params = { ...reportParams };
                      if (!params[index]) {
                        params[index] = {};
                      }
                      params[index].erScore = e.target.value;
                      setReportParams(params);
                    }}
                  />
                </Column>
                <Column lg={2} md={8} sm={4}>
                  <FormattedMessage id="immunohistochemistry.label.outOf8" />
                </Column>

                <Column lg={16} md={8} sm={4}>
                  <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
                </Column>
                <Column lg={2} md={8} sm={4}>
                  PR
                </Column>
                <Column lg={2} md={8} sm={4}>
                  <TextInput
                    id={"prPercent_" + index}
                    labelText=""
                    hideLabel={true}
                    type="number"
                    value={reportParams[index]?.prPercent}
                    onChange={(e) => {
                      var params = { ...reportParams };
                      if (!params[index]) {
                        params[index] = {};
                      }
                      params[index].prPercent = e.target.value;
                      setReportParams(params);
                    }}
                  />
                </Column>
                <Column lg={3} md={8} sm={4}>
                  <FormattedMessage id="immunohistochemistry.label.cellPercent" />
                </Column>
                <Column lg={2} md={8} sm={4}>
                  <Select
                    id={"prIntensity_" + index}
                    name="prIntensity"
                    labelText=""
                    value={reportParams[index]?.prIntensity}
                    onChange={(e) => {
                      var params = { ...reportParams };
                      if (!params[index]) {
                        params[index] = {};
                      }
                      params[index].prIntensity = e.target.value;
                      setReportParams(params);
                    }}
                  >
                    <SelectItem disabled value="placeholder" text="Intensity" />
                    <SelectItem value="" text="" />
                    {intensityList.map((status, index) => {
                      return (
                        <SelectItem
                          key={index}
                          text={status.value}
                          value={status.value}
                        />
                      );
                    })}
                  </Select>
                </Column>
                <Column lg={3} md={8} sm={4}>
                  <FormattedMessage id="immunohistochemistry.label.cellIntensity" />
                </Column>
                <Column lg={2} md={8} sm={4}>
                  <TextInput
                    id={"erScore_" + index}
                    labelText=""
                    hideLabel={true}
                    type="number"
                    value={reportParams[index]?.prScore}
                    onChange={(e) => {
                      var params = { ...reportParams };
                      if (!params[index]) {
                        params[index] = {};
                      }
                      params[index].prScore = e.target.value;
                      setReportParams(params);
                    }}
                  />
                </Column>
                <Column lg={2} md={8} sm={4}>
                  <FormattedMessage id="immunohistochemistry.label.outOf8" />
                </Column>

                <Column lg={16} md={8} sm={4}>
                  <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
                </Column>

                <Column lg={3} md={8} sm={4}>
                  <FormattedMessage id="immunohistochemistry.label.mibName" />
                </Column>
                <Column lg={8} md={8} sm={4}>
                  <TextInput
                    id={"mib_" + index}
                    labelText=""
                    hideLabel={true}
                    //type="number"
                    value={reportParams[index]?.mib}
                    onChange={(e) => {
                      var params = { ...reportParams };
                      if (!params[index]) {
                        params[index] = {};
                      }
                      params[index].mib = e.target.value;
                      setReportParams(params);
                    }}
                  />
                </Column>
                <Column lg={5} md={8} sm={4}>
                  <FormattedMessage id="immunohistochemistry.label.tumorCells" />
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
                </Column>

                <Column lg={3} md={8} sm={4}>
                  <FormattedMessage id="immunohistochemistry.label.her2Pattern" />
                </Column>
                <Column lg={13} md={8} sm={4}>
                  {/* <Select
                    id={"pattern_" + index}
                    name="pattern"
                    labelText=""
                    value={reportParams[index]?.pattern}
                    onChange={(e) => {
                      var params = { ...reportParams };
                      if (!params[index]) {
                        params[index] = {};
                      }
                      params[index].pattern = e.target.value;
                      setReportParams(params);
                    }}
                  >
                    <SelectItem disabled value="placeholder" text="" />
                    <SelectItem value="" text="" />
                    {cerbB2PatternList.map((status, index) => {
                      return (
                        <SelectItem
                          key={index}
                          text={status.value}
                          value={status.value}
                        />
                      );
                    })}
                  </Select> */}
                  <TextInput
                    id={"pattern_" + index}
                    labelText=""
                    hideLabel={true}
                    //type="number"
                    value={reportParams[index]?.pattern}
                    onChange={(e) => {
                      var params = { ...reportParams };
                      if (!params[index]) {
                        params[index] = {};
                      }
                      params[index].pattern = e.target.value;
                      setReportParams(params);
                    }}
                  />
                </Column>

                <Column lg={16} md={8} sm={4}>
                  <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
                </Column>

                <Column lg={4} md={8} sm={4}>
                  <FormattedMessage id="immunohistochemistry.label.her2Assesment" />
                </Column>
                <Column lg={4} md={8} sm={4}>
                  <TextInput
                    id={"herAssesment_" + index}
                    labelText=""
                    hideLabel={true}
                    value={reportParams[index]?.herAssesment}
                    onChange={(e) => {
                      var params = { ...reportParams };
                      if (!params[index]) {
                        params[index] = {};
                      }
                      params[index].herAssesment = e.target.value;
                      setReportParams(params);
                    }}
                  />
                </Column>
                <Column lg={4} md={8} sm={4}>
                  <FormattedMessage id="immunohistochemistry.label.her2ScoreOf" />
                </Column>
                <Column lg={4} md={8} sm={4}>
                  <TextInput
                    id={"herScore_" + index}
                    labelText=""
                    hideLabel={true}
                    type="number"
                    value={reportParams[index]?.herScore}
                    onChange={(e) => {
                      var params = { ...reportParams };
                      if (!params[index]) {
                        params[index] = {};
                      }
                      params[index].herScore = e.target.value;
                      setReportParams(params);
                    }}
                  />
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
                </Column>
                <Column lg={3} md={8} sm={4}>
                  <FormattedMessage id="immunohistochemistry.label.historicalDiagnosis" />
                </Column>
                <Column lg={13} md={8} sm={4}>
                  <TextArea
                    id={"diagnosis_" + index}
                    labelText=""
                    hideLabel={true}
                    value={reportParams[index]?.diagnosis}
                    onChange={(e) => {
                      var params = { ...reportParams };
                      if (!params[index]) {
                        params[index] = {};
                      }
                      params[index].diagnosis = e.target.value;
                      setReportParams(params);
                    }}
                  />
                </Column>

                <Column lg={16} md={8} sm={4}>
                  <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
                </Column>
                <Column lg={3} md={8} sm={4}>
                  <FormattedMessage id="immunohistochemistry.label.molecularType" />
                </Column>
                <Column lg={13} md={8} sm={4}>
                  <Select
                    id={"molecularSubType_" + index}
                    name="molecularSubType"
                    labelText=""
                    value={reportParams[index]?.molecularSubType}
                    onChange={(e) => {
                      var params = { ...reportParams };
                      if (!params[index]) {
                        params[index] = {};
                      }
                      params[index].molecularSubType = e.target.value;
                      setReportParams(params);
                    }}
                  >
                    <SelectItem disabled value="placeholder" text="" />
                    <SelectItem value="" text="" />
                    {molecularSubTypeList.map((status, index) => {
                      return (
                        <SelectItem
                          key={index}
                          text={status.value}
                          value={status.value}
                        />
                      );
                    })}
                  </Select>
                </Column>
              </Grid>
            </Column>
          </>
        );
      case "DUAL_IN_SITU_HYBRIDISATION":
        return (
          <>
            <Column lg={16} md={8} sm={4}>
              <Grid fullWidth={true} className="gridBoundary">
                <Column lg={3} md={8} sm={4}>
                  <FormattedMessage id="immunohistochemistry.label.numberOfCancer" />
                </Column>
                <Column lg={5} md={8} sm={4}>
                  <TextInput
                    id={"nuclei_" + index}
                    labelText=""
                    hideLabel={true}
                    type="number"
                    value={reportParams[index]?.numberOfcancerNuclei}
                    onChange={(e) => {
                      var params = { ...reportParams };
                      if (!params[index]) {
                        params[index] = {};
                      }
                      params[index].numberOfcancerNuclei = e.target.value;
                      setReportParams(params);
                    }}
                  />
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
                </Column>
                <Column lg={3} md={8} sm={4}>
                  <FormattedMessage id="immunohistochemistry.label.averageHer2" />
                </Column>
                <Column lg={5} md={8} sm={4}>
                  <TextInput
                    id={"her_" + index}
                    labelText=""
                    hideLabel={true}
                    type="number"
                    value={reportParams[index]?.averageHer2}
                    onChange={(e) => {
                      var params = { ...reportParams };
                      if (!params[index]) {
                        params[index] = {};
                      }
                      params[index].averageHer2 = e.target.value;
                      var her2 = e.target.value;
                      var chrom = params[index].averageChrom;
                      if (chrom) {
                        var ratio = her2 / chrom;
                        params[index].ihcRatio = ratio.toFixed(2);
                        if (ratio >= 2.0) {
                          params[index].ihcScore = "AMPLIFICATION";
                        } else {
                          params[index].ihcScore = "NO_AMPLIFICATION";
                        }
                      }
                      setReportParams(params);
                    }}
                  />
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
                </Column>
                <Column lg={3} md={8} sm={4}>
                  <FormattedMessage id="immunohistochemistry.label.averageChrom" />
                </Column>
                <Column lg={5} md={8} sm={4}>
                  <TextInput
                    id={"her_" + index}
                    labelText=""
                    hideLabel={true}
                    type="number"
                    value={reportParams[index]?.averageChrom}
                    onChange={(e) => {
                      var params = { ...reportParams };
                      if (!params[index]) {
                        params[index] = {};
                      }
                      params[index].averageChrom = e.target.value;
                      var her2 = params[index].averageHer2;
                      var chrom = e.target.value;
                      if (her2) {
                        var ratio = her2 / chrom;
                        params[index].ihcRatio = ratio.toFixed(2);
                        if (ratio >= 2.0) {
                          params[index].ihcScore = "AMPLIFICATION";
                        } else {
                          params[index].ihcScore = "NO_AMPLIFICATION";
                        }
                      }
                      setReportParams(params);
                    }}
                  />
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
                </Column>
                <Column lg={3} md={8} sm={4}>
                  <FormattedMessage id="immunohistochemistry.label.ihcRatio" />
                </Column>
                <Column lg={5} md={8} sm={4}>
                  <TextInput
                    id={"her_" + index}
                    labelText=""
                    hideLabel={true}
                    disabled={true}
                    value={reportParams[index]?.ihcRatio}
                  />
                </Column>

                <Column lg={16} md={8} sm={4}>
                  <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
                </Column>
                <Column lg={3} md={8} sm={4}>
                  <FormattedMessage id="immunohistochemistry.label.ihcScore" />
                </Column>
                <Column lg={5} md={8} sm={4}>
                  <TextInput
                    id={"her_" + index}
                    labelText=""
                    hideLabel={true}
                    disabled={true}
                    value={reportParams[index]?.ihcScore}
                  />
                </Column>
              </Grid>
            </Column>
          </>
        );
      case "IMMUNOHISTOCHEMISTRY":
        return (
          <>
            <Column lg={16} md={8} sm={4}>
              <Grid fullWidth={true} className="gridBoundary">
                <Column lg={16} md={8} sm={4}>
                  <Grid fullWidth={true} className="gridBoundary">
                    {pagination && (
                      <Column lg={16} md={8} sm={4}>
                        <Link>
                          {currentApiPage} / {totalApiPages}
                        </Link>
                        <div style={{ display: "flex", gap: "10px" }}>
                          <Button
                            hasIconOnly
                            iconDescription="previous"
                            disabled={previousPage != null ? false : true}
                            onClick={loadPreviousConclusionsPage}
                            renderIcon={ArrowLeft}
                            size="sm"
                          />
                          <Button
                            hasIconOnly
                            iconDescription="next"
                            disabled={nextPage != null ? false : true}
                            renderIcon={ArrowRight}
                            onClick={loadNextCOnclusionsPage}
                            size="sm"
                          />
                        </div>
                      </Column>
                    )}
                    <Column lg={16}>
                      <br />
                      <br />
                    </Column>
                    <Column lg={3} md={8} sm={4}>
                      <FormattedMessage id="pathology.label.conclusion" />
                    </Column>
                    <Column lg={4} md={8} sm={4}>
                      <FilterableMultiSelect
                        id="conclusion"
                        titleText=""
                        items={conclusions}
                        itemToString={(item) => (item ? item.value : "")}
                        initialSelectedItems={
                          reportParams[index]?.codedConclusions
                        }
                        onChange={(changes) => {
                          var params = { ...reportParams };
                          if (!params[index]) {
                            params[index] = {};
                          }
                          params[index].codedConclusions =
                            changes.selectedItems;
                          setReportParams(params);
                        }}
                        selectionFeedback="top-after-reopen"
                      />
                    </Column>
                    <Column lg={8} md={8} sm={4}>
                      {reportParams[index] &&
                        reportParams[index]?.codedConclusions.map(
                          (conclusion, conclusionIndex) => (
                            <Tag
                              key={conclusionIndex}
                              filter
                              onClose={() => {
                                var params = { ...reportParams };
                                params[index]["codedConclusions"].splice(
                                  conclusionIndex,
                                  1,
                                );
                                setReportParams(params);
                              }}
                            >
                              {conclusion.value}
                            </Tag>
                          ),
                        )}
                    </Column>
                  </Grid>
                </Column>
                <Column lg={3} md={8} sm={4}>
                  <FormattedMessage id="pathology.label.textconclusion" />
                </Column>
                <Column lg={13} md={8} sm={4}>
                  <TextArea
                    id={"conclusion_" + index}
                    labelText=""
                    hideLabel={true}
                    value={reportParams[index]?.conclusion}
                    onChange={(e) => {
                      var params = { ...reportParams };
                      if (!params[index]) {
                        params[index] = {};
                      }
                      params[index].conclusion = e.target.value;
                      setReportParams(params);
                    }}
                  />
                </Column>
              </Grid>
            </Column>
          </>
        );
    }
  };

  const setResultsWithId = (results) => {
    if (results) {
      var i = 0;
      if (results.testResult) {
        results.testResult.forEach((item) => (item.id = "" + i++));
      }
      setResults(results);
    } else {
      setResults({ testResult: [] });
    }
    setResultsLoading(false);
  };

  const getResults = () => {
    setResults({ testResult: [] });
    var searchEndPoint =
      "/rest/ReactLogbookResultsByRange?" +
      "labNumber=" +
      immunohistochemistrySampleInfo.labNumber +
      "&doRange=" +
      false +
      "&finished=" +
      true +
      "&patientPK=" +
      "&collectionDate=" +
      "&recievedDate=" +
      "&selectedTest=" +
      "&selectedSampleStatus=" +
      "&selectedAnalysisStatus=";
    getFromOpenElisServer(searchEndPoint, setResultsWithId);
  };

  const toBase64 = (file) =>
    new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => resolve(reader.result);
      reader.onerror = reject;
    });

  const save = () => {
    if (isSubmitting) {
      return;
    }
    setIsSubmitting(true);
    let submitValues = {
      assignedTechnicianId: immunohistochemistrySampleInfo.assignedTechnicianId,
      assignedPathologistId:
        immunohistochemistrySampleInfo.assignedPathologistId,
      status: immunohistochemistrySampleInfo.status,
      reports: immunohistochemistrySampleInfo.reports,
      release:
        immunohistochemistrySampleInfo.release != undefined
          ? immunohistochemistrySampleInfo.release
          : false,
    };

    postToOpenElisServerFullResponse(
      "/rest/immunohistochemistry/caseView/" + immunohistochemistrySampleId,
      JSON.stringify(submitValues),
      displayStatus,
    );
  };

  const setReportTypeList = (reportTypes) => {
    if (componentMounted.current) {
      setReportTypes(reportTypes);
    }
  };

  const setInitialImmunohistochemistrySampleInfo = (e) => {
    if (
      hasRole(userSessionDetails, "Pathologist") &&
      !e.assignedPathologistId &&
      e.status === "READY_PATHOLOGIST"
    ) {
      e.assignedPathologistId = userSessionDetails.userId;
      e.assignedPathologist =
        userSessionDetails.lastName + " " + userSessionDetails.firstName;
    }
    if (!e.assignedTechnicianId) {
      e.assignedTechnicianId = userSessionDetails.userId;
      e.assignedTechnician =
        userSessionDetails.lastName + " " + userSessionDetails.firstName;
    }
    setImmunohistochemistrySampleInfo(e);
    setLoading(false);
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer(
      "/rest/displayList/IMMUNOHISTOCHEMISTRY_STATUS",
      setStatuses,
    );
    getFromOpenElisServer(
      "/rest/displayList/IMMUNOHISTOCHEMISTRY_REPORT_TYPES",
      setReportTypeList,
    );
    getFromOpenElisServer(
      "/rest/displayList/IHC_BREAST_CANCER_REPORT_INTENSITY",
      setIntensityList,
    );
    getFromOpenElisServer(
      "/rest/displayList/IHC_BREAST_CANCER_REPORT_CERBB2_PATTERN",
      setCerbB2PatternList,
    );
    getFromOpenElisServer(
      "/rest/displayList/IHC_BREAST_CANCER_REPORT_MOLE_SUBTYPE",
      setMolecularSubTypeList,
    );
    //TODO make conclusions list instead of reusing pathrequest
    getFromOpenElisServer("/rest/users", setTechnicianUsers);
    getFromOpenElisServer("/rest/users/Pathologist", setPathologistUsers);
    getFromOpenElisServer(
      "/rest/immunohistochemistry/caseView/" + immunohistochemistrySampleId,
      setInitialImmunohistochemistrySampleInfo,
    );

    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    componentMounted.current = true;
    getResults();
    return () => {
      componentMounted.current = false;
    };
  }, [immunohistochemistrySampleInfo.labNumber]);

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="immunohistochemistry.label.title" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <PatientHeader
                id={immunohistochemistrySampleInfo.labNumber}
                lastName={immunohistochemistrySampleInfo.lastName}
                firstName={immunohistochemistrySampleInfo.firstName}
                gender={immunohistochemistrySampleInfo.sex}
                age={immunohistochemistrySampleInfo.age}
                orderDate={immunohistochemistrySampleInfo.requestDate}
                referringFacility={
                  immunohistochemistrySampleInfo.referringFacility
                }
                department={immunohistochemistrySampleInfo.department}
                requester={immunohistochemistrySampleInfo.requester}
                accesionNumber={immunohistochemistrySampleInfo.labNumber}
                className="patient-header2"
                isOrderPage={true}
              >
                {" "}
              </PatientHeader>
            </Section>
          </Section>
          <Section>
            <Section>
              <div className="patient-header2">
                <QuestionnaireResponse
                  questionnaireResponse={
                    immunohistochemistrySampleInfo.programQuestionnaireResponse
                  }
                />
              </div>
            </Section>
          </Section>
        </Column>
      </Grid>
      <Stack gap={4}>
        <Grid fullWidth={true} className="orderLegendBody">
          {notificationVisible === true ? <AlertDialog /> : ""}
          {(loading || resultsLoading) && (
            <Loading description="Loading Dasboard..." />
          )}

          <Column lg={16} md={8} sm={4}>
            <Button
              id="pathology_save"
              disabled={isSubmitting}
              onClick={(e) => {
                e.preventDefault();
                save(e);
              }}
            >
              <FormattedMessage id="label.button.save" />
            </Button>
          </Column>
          <Column lg={16} md={8} sm={4}>
            <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
          </Column>
          <Column lg={4} md={2} sm={2}>
            <Select
              id="status"
              name="status"
              labelText={intl.formatMessage({
                id: "label.button.select.status",
              })}
              value={immunohistochemistrySampleInfo.status}
              onChange={(event) => {
                setImmunohistochemistrySampleInfo({
                  ...immunohistochemistrySampleInfo,
                  status: event.target.value,
                });
              }}
            >
              <SelectItem disabled value="placeholder" text="Status" />

              {statuses.map((status, index) => {
                return (
                  <SelectItem
                    key={index}
                    text={status.value}
                    value={status.id}
                  />
                );
              })}
            </Select>
          </Column>
          <Column lg={4} md={2} sm={2}>
            <Select
              id="assignedTechnician"
              name="assignedTechnician"
              labelText={
                <FormattedMessage id="label.button.select.technician" />
              }
              value={immunohistochemistrySampleInfo.assignedTechnicianId}
              onChange={(event) => {
                setImmunohistochemistrySampleInfo({
                  ...immunohistochemistrySampleInfo,
                  assignedTechnicianId: event.target.value,
                });
              }}
            >
              <SelectItem />
              {technicianUsers.map((user, index) => {
                return (
                  <SelectItem key={index} text={user.value} value={user.id} />
                );
              })}
            </Select>
          </Column>
          <Column lg={4} md={2} sm={2}>
            <Select
              id="assignedPathologist"
              name="assignedPathologist"
              labelText={
                <FormattedMessage id="label.button.select.pathologist" />
              }
              value={immunohistochemistrySampleInfo.assignedPathologistId}
              onChange={(e) => {
                setImmunohistochemistrySampleInfo({
                  ...immunohistochemistrySampleInfo,
                  assignedPathologistId: e.target.value,
                });
              }}
            >
              <SelectItem />
              {pathologistUsers.map((user, index) => {
                return (
                  <SelectItem key={index} text={user.value} value={user.id} />
                );
              })}
            </Select>
          </Column>
          <Column lg={16} md={8} sm={4}>
            <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
          </Column>
          <Column lg={16} md={8} sm={4}>
            <Grid fullWidth={true} className="gridBoundary">
              <Column lg={16} md={8} sm={4}>
                <h5>
                  <FormattedMessage id="sidenav.label.results" />
                </h5>
              </Column>
              <Column lg={16} md={8} sm={4}>
                <SearchResults
                  results={results}
                  setResultForm={setResults}
                  refreshOnSubmit={false}
                />
              </Column>
            </Grid>
          </Column>

          <Column lg={16} md={8} sm={4}>
            <Grid fullWidth={true} className="gridBoundary">
              <Column lg={4} md={2} sm={2}>
                <Select
                  id="report"
                  name="report"
                  labelText={intl.formatMessage({
                    id: "immunohistochemistry.label.addreport",
                  })}
                  onChange={(event) => {
                    setImmunohistochemistrySampleInfo({
                      ...immunohistochemistrySampleInfo,
                      reports: [
                        ...(immunohistochemistrySampleInfo.reports || []),
                        { id: "", reportType: event.target.value },
                      ],
                    });
                  }}
                >
                  <SelectItem
                    disabled
                    value="ADD"
                    text={intl.formatMessage({
                      id: "immunohistochemistry.label.addreport",
                    })}
                  />
                  {reportTypes.map((report, index) => {
                    return (
                      <SelectItem
                        key={index}
                        text={report.value}
                        value={report.id}
                      />
                    );
                  })}
                </Select>
              </Column>
              <Column lg={12} md={2} sm={2}></Column>
              <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
              <Column lg={16} md={8} sm={4}>
                <h5>
                  {" "}
                  <FormattedMessage id="immunohistochemistry.label.reports" />
                </h5>
                {loadingReport && <InlineLoading />}
              </Column>
              <Column lg={16} md={8} sm={4}>
                <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
              </Column>
              {immunohistochemistrySampleInfo.reports &&
                immunohistochemistrySampleInfo.reports.map((report, index) => {
                  return (
                    <>
                      <Column lg={2} md={8} sm={4}>
                        <IconButton
                          label={intl.formatMessage({
                            id: "label.button.remove.report",
                          })}
                          onClick={() => {
                            var info = { ...immunohistochemistrySampleInfo };
                            info["reports"].splice(index, 1);
                            setImmunohistochemistrySampleInfo(info);
                          }}
                          kind="tertiary"
                          size="sm"
                        >
                          <Subtract size={18} />{" "}
                          <FormattedMessage id="immunohistochemistry.label.report" />
                        </IconButton>
                      </Column>

                      <Column lg={3} md={2} sm={2}>
                        <FileUploader
                          style={{ marginTop: "-20px" }}
                          buttonLabel={
                            <FormattedMessage id="label.button.uploadfile" />
                          }
                          iconDescription="file upload"
                          multiple={false}
                          accept={[
                            "image/jpeg",
                            "image/png",
                            "application/pdf",
                          ]}
                          disabled={reportParams[index]?.submited}
                          name=""
                          buttonKind="primary"
                          size="lg"
                          filenameStatus="edit"
                          onChange={async (e) => {
                            e.preventDefault();
                            let file = e.target.files[0];
                            var newReports = [
                              ...immunohistochemistrySampleInfo.reports,
                            ];
                            let encodedFile = await toBase64(file);
                            newReports[index].base64Image = encodedFile;
                            setImmunohistochemistrySampleInfo({
                              ...immunohistochemistrySampleInfo,
                              reports: newReports,
                            });
                          }}
                          onClick={function noRefCheck() {}}
                          onDelete={(e) => {
                            e.preventDefault();
                          }}
                        />
                      </Column>
                      <Column lg={4} md={2} sm={2}>
                        <h6>
                          {
                            reportTypes.filter(
                              (type) => type.id === report.reportType,
                            )[0]?.value
                          }
                        </h6>
                      </Column>

                      {immunohistochemistrySampleInfo.reports[index].image && (
                        <>
                          {!reportParams[index]?.submited && (
                            <Column lg={2} md={2} sm={2}>
                              <Button
                                onClick={() => {
                                  var win = window.open();
                                  win.document.write(
                                    '<iframe src="' +
                                      report.fileType +
                                      ";base64," +
                                      report.image +
                                      '" frameborder="0" style="border:0; top:0px; left:0px; bottom:0px; right:0px; width:100%; height:100%;" allowfullscreen></iframe>',
                                  );
                                }}
                              >
                                <Launch />{" "}
                                <FormattedMessage id="pathology.label.view" />
                              </Button>
                            </Column>
                          )}
                        </>
                      )}
                      {reportParams[index]?.submited && (
                        <Column lg={2} md={1} sm={2}>
                          <Button
                            onClick={() => {
                              window.open(
                                reportParams[index]?.reportLink,
                                "_blank",
                              );
                            }}
                          >
                            <Launch />{" "}
                            <FormattedMessage id="pathology.label.view" />
                          </Button>
                        </Column>
                      )}

                      <Column lg={3} md={2} sm={2}>
                        <Button
                          disabled={reportParams[index]?.submited}
                          onClick={(e) => {
                            setLoadingReport(true);
                            const form = {
                              report: getReportName(report.reportType),
                              programSampleId: immunohistochemistrySampleId,
                              erPercent: reportParams[index]?.erPercent,
                              erIntensity: reportParams[index]?.erIntensity,
                              erScore: reportParams[index]?.erScore,
                              prPercent: reportParams[index]?.prPercent,
                              prIntensity: reportParams[index]?.prIntensity,
                              prScore: reportParams[index]?.prScore,
                              mib: reportParams[index]?.mib,
                              pattern: reportParams[index]?.pattern,
                              herAssesment: reportParams[index]?.herAssesment,
                              herScore: reportParams[index]?.herScore,
                              diagnosis: reportParams[index]?.diagnosis,
                              molecularSubType:
                                reportParams[index]?.molecularSubType,
                              conclusion: reportParams[index]?.conclusion,
                              ihcScore: reportParams[index]?.ihcScore,
                              ihcRatio: reportParams[index]?.ihcRatio,
                              averageChrom: reportParams[index]?.averageChrom,
                              averageHer2: reportParams[index]?.averageHer2,
                              numberOfcancerNuclei:
                                reportParams[index]?.numberOfcancerNuclei,
                              codedConclusions: reportParams[
                                index
                              ]?.codedConclusions.map(
                                (conclusion) => conclusion.id,
                              ),
                            };
                            postToOpenElisServerForPDF(
                              "/rest/ReportPrint",
                              JSON.stringify(form),
                              (e, blob) => reportStatus(e, blob, index),
                            );
                          }}
                        >
                          {" "}
                          <FormattedMessage id="button.label.genarateReport" />
                        </Button>
                      </Column>
                      <Column lg={2} md={2} sm={2}>
                        <Toggle
                          toggled={reportParams[index]?.toggled}
                          disabled={reportParams[index]?.submited}
                          aria-label="toggle button"
                          id={index + "_toggle"}
                          labelText={intl.formatMessage({
                            id: "button.label.showHidePram",
                          })}
                          onToggle={(e) => toggleReportParam(e, index)}
                        />
                      </Column>
                      {/* <Column lg={1} md={2} sm={2}/> */}
                      {reportParams[index]?.toggled &&
                        createReportParams(report.reportType, index)}
                      <Column lg={16} md={8} sm={4}>
                        <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
                      </Column>
                    </>
                  );
                })}
            </Grid>
          </Column>

          {immunohistochemistrySampleInfo.reffered && (
            <>
              <Column lg={16} md={8} sm={4}>
                <Grid fullWidth={true} className="gridBoundary">
                  <Column lg={16} md={8} sm={4}>
                    <h5>
                      {" "}
                      <FormattedMessage id="pathology.label.blocks" />
                    </h5>
                  </Column>
                  <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
                  <Column lg={16} md={8} sm={4} />
                  {immunohistochemistrySampleInfo.blocks &&
                    immunohistochemistrySampleInfo.blocks.map(
                      (block, index) => {
                        return (
                          <>
                            <Column lg={2} md={2} sm={1} key={index}>
                              <TextInput
                                id="blockNumber"
                                labelText={intl.formatMessage({
                                  id: "pathology.label.block.number",
                                })}
                                hideLabel={true}
                                placeholder={intl.formatMessage({
                                  id: "pathology.label.block.number",
                                })}
                                value={block.blockNumber}
                                disabled={true}
                              />
                            </Column>
                            <Column lg={2} md={2} sm={1}>
                              <TextInput
                                id="location"
                                labelText={intl.formatMessage({
                                  id: "pathology.label.location",
                                })}
                                hideLabel={true}
                                placeholder={intl.formatMessage({
                                  id: "pathology.label.location",
                                })}
                                value={block.location}
                                disabled={true}
                              />
                            </Column>
                            <Column lg={2} md={2} sm={2}>
                              <Button
                                onClick={() => {
                                  window.open(
                                    config.serverBaseUrl +
                                      "/LabelMakerServlet?labelType=block&code=" +
                                      block.blockNumber,
                                    "_blank",
                                  );
                                }}
                              >
                                {" "}
                                <FormattedMessage id="pathology.label.printlabel" />
                              </Button>
                            </Column>
                            <Column lg={10} md={2} sm={0} />
                            <Column lg={16} md={8} sm={4}>
                              <div>
                                {" "}
                                &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;
                              </div>
                            </Column>
                          </>
                        );
                      },
                    )}
                </Grid>
              </Column>

              <Column lg={16} md={8} sm={4}>
                <Grid fullWidth={true} className="gridBoundary">
                  <Column lg={16} md={8} sm={4}>
                    <h5>
                      {" "}
                      <FormattedMessage id="pathology.label.slides" />
                    </h5>
                    <div> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;</div>
                  </Column>

                  <Column lg={16} md={8} sm={4} />
                  {immunohistochemistrySampleInfo.slides &&
                    immunohistochemistrySampleInfo.slides.map(
                      (slide, index) => {
                        return (
                          <>
                            <Column lg={2} md={2} sm={1} key={index}>
                              <TextInput
                                id="slideNumber"
                                labelText={intl.formatMessage({
                                  id: "pathology.label.slide.number",
                                })}
                                hideLabel={true}
                                disabled={true}
                                placeholder={intl.formatMessage({
                                  id: "pathology.label.slide.number",
                                })}
                                value={slide.slideNumber}
                              />
                            </Column>
                            <Column lg={2} md={2} sm={1}>
                              <TextInput
                                id="location"
                                labelText={intl.formatMessage({
                                  id: "pathology.label.location",
                                })}
                                hideLabel={true}
                                placeholder={intl.formatMessage({
                                  id: "pathology.label.location",
                                })}
                                value={slide.location}
                                disabled={true}
                              />
                            </Column>
                            <Column lg={2} md={1} sm={2}>
                              {immunohistochemistrySampleInfo.slides[index]
                                .image && (
                                <>
                                  <Button
                                    onClick={() => {
                                      var win = window.open();
                                      win.document.write(
                                        '<iframe src="' +
                                          slide.fileType +
                                          ";base64," +
                                          slide.image +
                                          '" frameborder="0" style="border:0; top:0px; left:0px; bottom:0px; right:0px; width:100%; height:100%;" allowfullscreen></iframe>',
                                      );
                                    }}
                                  >
                                    <Launch />{" "}
                                    <FormattedMessage id="pathology.label.view" />
                                  </Button>
                                </>
                              )}
                            </Column>
                            <Column lg={2} md={1} sm={2}>
                              <Button
                                onClick={() => {
                                  window.open(
                                    config.serverBaseUrl +
                                      "/LabelMakerServlet?labelType=slide&code=" +
                                      slide.slideNumber,
                                    "_blank",
                                  );
                                }}
                              >
                                {" "}
                                <FormattedMessage id="pathology.label.printlabel" />
                              </Button>
                            </Column>
                            <Column lg={8} md={1} sm={2} />
                            <Column lg={16} md={8} sm={4}>
                              <div>
                                {" "}
                                &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;
                              </div>
                            </Column>
                          </>
                        );
                      },
                    )}
                </Grid>
              </Column>

              {hasRole(userSessionDetails, "Pathologist") && (
                <Column lg={16} md={8} sm={4}>
                  <Grid fullWidth={true} className="gridBoundary">
                    <>
                      <Column lg={2} md={4} sm={2}>
                        <h6>
                          <FormattedMessage id="pathology.label.techniques" />:{" "}
                        </h6>
                      </Column>
                      <Column lg={14} md={4} sm={2}>
                        {immunohistochemistrySampleInfo.techniques &&
                          immunohistochemistrySampleInfo.techniques.map(
                            (technique, index) => (
                              <>
                                <Tag key={index}>{technique.value} </Tag>
                              </>
                            ),
                          )}
                      </Column>
                      <Column lg={2} md={4} sm={2}>
                        <h6>
                          <FormattedMessage id="pathology.label.request" />:{" "}
                        </h6>
                      </Column>
                      <Column lg={14} md={4} sm={2}>
                        {immunohistochemistrySampleInfo.requests &&
                          immunohistochemistrySampleInfo.requests.map(
                            (technique, index) => (
                              <>
                                <Tag key={index}> {technique.value} </Tag>
                              </>
                            ),
                          )}
                      </Column>
                      <Column lg={2} md={4} sm={2}>
                        <h6>
                          {" "}
                          <FormattedMessage id="pathology.label.grossexam" /> :
                        </h6>
                      </Column>
                      <Column lg={14} md={4} sm={2}>
                        <Tag>{immunohistochemistrySampleInfo.grossExam} </Tag>
                      </Column>
                      <Column lg={2} md={4} sm={2}>
                        <h6>
                          {" "}
                          <FormattedMessage id="pathology.label.microexam" /> :
                        </h6>
                      </Column>
                      <Column lg={14} md={4} sm={2}>
                        <Tag>
                          {immunohistochemistrySampleInfo.microscopyExam}{" "}
                        </Tag>
                      </Column>
                      <Column lg={2} md={4} sm={2}>
                        <h6>
                          {" "}
                          <FormattedMessage id="pathology.label.conclusion" /> :
                        </h6>
                      </Column>
                      <Column lg={14} md={4} sm={2}>
                        {immunohistochemistrySampleInfo.conclusions &&
                          immunohistochemistrySampleInfo.conclusions.map(
                            (conclusion, index) => (
                              <>
                                <Tag key={index}> {conclusion.value} </Tag>
                              </>
                            ),
                          )}
                      </Column>
                      <Column lg={2} md={4} sm={2}>
                        <h6>
                          {" "}
                          <FormattedMessage id="pathology.label.textconclusion" />{" "}
                          :
                        </h6>
                      </Column>
                      <Column lg={14} md={4} sm={2}>
                        <Tag>
                          {immunohistochemistrySampleInfo.conclusionText}
                        </Tag>
                      </Column>
                    </>
                  </Grid>
                </Column>
              )}
            </>
          )}
          {immunohistochemistrySampleInfo.assignedPathologistId &&
            immunohistochemistrySampleInfo.assignedTechnicianId && (
              <Column lg={16}>
                <Checkbox
                  labelText={intl.formatMessage({
                    id: "pathology.label.release",
                  })}
                  id="release"
                  onChange={() => {
                    setImmunohistochemistrySampleInfo({
                      ...immunohistochemistrySampleInfo,
                      release: !immunohistochemistrySampleInfo.release,
                    });
                  }}
                />
              </Column>
            )}

          <Column>
            <Button
              id="pathology_save2"
              disabled={isSubmitting}
              onClick={(e) => {
                e.preventDefault();
                save(e);
              }}
            >
              <FormattedMessage id="label.button.save" />
            </Button>
          </Column>
        </Grid>
      </Stack>
    </>
  );
}

export default ImmunohistochemistryCaseView;
