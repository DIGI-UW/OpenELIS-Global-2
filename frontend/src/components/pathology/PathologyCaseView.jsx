import React, { useContext, useState, useEffect, useRef } from "react";
import { useParams } from "react-router-dom";
import {
  Accordion,
  Column,
  Dropdown,
  Grid,
  Heading,
  Loading,
  Section,
  Stack,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  postToOpenElisServerForPDF,
  hasRole,
} from "../utils/Utils";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { NotificationContext, ConfigurationContext } from "../layout/contexts";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { FormattedMessage, useIntl } from "react-intl";
import PatientHeader from "../common/PatientHeader";
import "./PathologyDashboard.css";
import "./pathologyCaseView.scss";
import PageBreadCrumb from "../common/PageBreadCrumb";
import PostSavePrintDialog from "../barcodeWorkflow/PostSavePrintDialog";
import CaseViewLayout from "../caseView/CaseViewLayout";
import CaseSection from "../caseView/CaseSection";
import CaseActionBar from "../caseView/CaseActionBar";
import CaseSummaryPanel from "../caseView/CaseSummaryPanel";
import ProgressRail from "../caseView/ProgressRail";
import StatusBadge from "../caseView/StatusBadge";
import { SECTION_STATE } from "../caseView/sectionState";
import {
  PATHOLOGY_STAGES,
  stageDisplayKey,
  stageLabel,
} from "./pathologyStages";
import {
  deriveCaseSections,
  deriveRailItems,
  isStageEnabled,
  openRequestNames,
  railCurrentIndex,
  sectionBadge,
  stageBadgeKind,
} from "./pathologySections";
import CaseInformationSection from "./sections/CaseInformationSection";
import GrossingSection from "./sections/GrossingSection";
import StagePlaceholderSection from "./sections/StagePlaceholderSection";
import MicrotomySection from "./sections/MicrotomySection";
import ReviewSection from "./sections/ReviewSection";
import FindingsSection from "./sections/FindingsSection";
import ReportsSection from "./sections/ReportsSection";

/**
 * The anatomic-pathology case, on the shared case-view shell.
 *
 * This screen owns the case and nothing else: it loads the case and the lists
 * the bench picks from, holds the one copy of the case being edited, and hands
 * each section the slice it works on. The sections hold no case state and read
 * no context, so what a section shows is always a function of what it was
 * given, and the save payload has exactly one place it can be built.
 *
 * Every edit goes through updateCase, which also marks the form dirty; loading
 * and re-loading set the case directly and clear the flag, so arriving at a
 * case never looks like unsaved work. updateCase accepts either a patch object
 * or a function of the previous case, because an edit to one row of a list has
 * to read the list it is replacing, and reading it from a closure would let a
 * second edit in the same tick overwrite the first.
 */
function PathologyCaseView() {
  const intl = useIntl();

  const componentMounted = useRef(false);

  const { pathologySampleId } = useParams();
  const caseUrl = "/rest/pathology/caseView/" + pathologySampleId;

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const configurationProperties =
    useContext(ConfigurationContext)?.configurationProperties;

  const [caseInfo, setCaseInfo] = useState({});
  const [caseLoaded, setCaseLoaded] = useState(false);
  const [dirty, setDirty] = useState(false);
  // Bumped on every load so the multiselects, which read their selection once
  // at mount, are remounted and really show what the server just returned.
  const [formVersion, setFormVersion] = useState(0);

  const [statuses, setStatuses] = useState([]);
  const [techniques, setTechniques] = useState([]);
  const [requests, setRequests] = useState([]);
  const [requestStatuses, setRequestStatuses] = useState([]);
  const [conclusions, setConclusions] = useState([]);
  const [immunoHistoChemistryTests, setImmunoHistoChemistryTests] = useState(
    [],
  );
  const [technicianUsers, setTechnicianUsers] = useState([]);
  const [pathologistUsers, setPathologistUsers] = useState([]);
  const [loadingConclusions, setLoadingConclusions] = useState(false);
  const [loadingReport, setLoadingReport] = useState(false);
  const [nextPage, setNextPage] = useState(null);
  const [previousPage, setPreviousPage] = useState(null);
  const [pagination, setPagination] = useState(false);
  const [currentApiPage, setCurrentApiPage] = useState(null);
  const [totalApiPages, setTotalApiPages] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [postSavePrintModel, setPostSavePrintModel] = useState(null);
  const [reportParams, setReportParams] = useState({
    0: {
      submited: false,
      reportLink: "",
    },
  });

  const isPathologist = hasRole(userSessionDetails, "Pathologist");

  const updateCase = (patch) => {
    setCaseInfo((prev) => ({
      ...prev,
      ...(typeof patch === "function" ? patch(prev) : patch),
    }));
    setDirty(true);
  };

  const toBase64 = (file) =>
    new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => resolve(reader.result);
      reader.onerror = reject;
    });

  const attachFile = async (listKey, index, file) => {
    const encodedFile = await toBase64(file);
    updateCase((prev) => ({
      [listKey]: (prev[listKey] ?? []).map((item, position) =>
        position === index ? { ...item, base64Image: encodedFile } : item,
      ),
    }));
  };

  const loadCase = (loaded) => {
    if (!componentMounted.current) {
      return;
    }
    // The case arrives unassigned when nobody has picked it up yet: the person
    // who opened it is the one doing the work, so record them rather than
    // asking them to say so. The pathologist half only applies once the case
    // is actually waiting on a reading.
    if (
      isPathologist &&
      !loaded.assignedPathologistId &&
      loaded.status === "READY_PATHOLOGIST"
    ) {
      loaded.assignedPathologistId = userSessionDetails.userId;
      loaded.assignedPathologist =
        userSessionDetails.lastName + " " + userSessionDetails.firstName;
    }
    if (!loaded.assignedTechnicianId) {
      loaded.assignedTechnicianId = userSessionDetails.userId;
      loaded.assignedTechnician =
        userSessionDetails.lastName + " " + userSessionDetails.firstName;
    }
    setCaseInfo(loaded);
    setDirty(false);
    setCaseLoaded(true);
    setFormVersion((version) => version + 1);
  };

  const reloadCase = () => getFromOpenElisServer(caseUrl, loadCase);

  const loadConclusionData = (res) => {
    if (res && res.displayItems && res.displayItems.length > 0) {
      setConclusions(res.displayItems);
    } else {
      setConclusions([]);
    }

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

    setLoadingConclusions(false);
  };

  const loadConclusionsPage = (page) => {
    setLoadingConclusions(true);
    getFromOpenElisServer(
      "/rest/paginatedDisplayList/PATHOLOGIST_CONCLUSIONS" + "?page=" + page,
      loadConclusionData,
    );
  };

  // A network failure never reaches this as a response at all: the post
  // helper catches it and calls back with nothing. A response whose body is
  // not the JSON this endpoint promises is no more of a save than that,
  // whatever status it carried. Either way the screen has to report the
  // failure and free the form again, because a save left in flight for ever
  // swallows every later attempt behind the in-flight guard while the button
  // goes on looking live.
  async function displayStatus(response) {
    let body = null;
    let saved = false;
    if (response) {
      try {
        body = await response.json();
        saved = response.status === 200;
      } catch {
        saved = false;
      }
    }

    setIsSubmitting(false);
    setNotificationVisible(true);
    if (saved) {
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "success.save.msg" }),
      });
      setPostSavePrintModel(body?.postSavePrintDialog || null);
      // A release moves the case to a later stage server-side, which changes
      // which sections are open and what the rail shows, so the saved case is
      // read back rather than assumed.
      reloadCase();
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
      const params = { ...reportParams };
      if (!params[index]) {
        params[index] = {};
      }
      params[index].submited = true;
      params[index].reportLink = window.URL.createObjectURL(blob, {
        type: "application/pdf",
      });
      setReportParams(params);

      const encodedFile = await toBase64(blob);
      updateCase((prev) => ({
        reports: (prev.reports ?? []).map((report, position) =>
          position === index ? { ...report, base64Image: encodedFile } : report,
        ),
      }));
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "error.report.status" }),
      });
    }
  };

  const generateReport = (index) => {
    setLoadingReport(true);
    postToOpenElisServerForPDF(
      "/rest/ReportPrint",
      JSON.stringify({
        report: "PatientPathologyReport",
        programSampleId: pathologySampleId,
      }),
      (generated, blob) => reportStatus(generated, blob, index),
    );
  };

  const save = () => {
    if (isSubmitting) {
      return;
    }
    setIsSubmitting(true);
    setPostSavePrintModel(null);
    let submitValues = {
      assignedTechnicianId: caseInfo.assignedTechnicianId,
      assignedPathologistId: caseInfo.assignedPathologistId,
      status: caseInfo.status,
      blocks: caseInfo.blocks,
      slides: caseInfo.slides,
      reports: caseInfo.reports,
      grossExam: caseInfo.grossExam,
      microscopyExam: caseInfo.microscopyExam,
      conclusionText: caseInfo.conclusionText,
      release: caseInfo.release != undefined ? caseInfo.release : false,
      referToImmunoHistoChemistry: caseInfo.referToImmunoHistoChemistry,
    };
    if (caseInfo.immunoHistoChemistryTestIds) {
      submitValues = {
        ...submitValues,
        immunoHistoChemistryTestIds: caseInfo.immunoHistoChemistryTestIds.map(
          (e) => e.id,
        ),
      };
    }
    if (caseInfo.techniques) {
      submitValues = {
        ...submitValues,
        techniques: caseInfo.techniques.map((e) => e.id),
      };
    }
    if (caseInfo.requests) {
      submitValues = {
        ...submitValues,
        requests: caseInfo.requests.map((e) => {
          return { value: e.id, status: e.status };
        }),
      };
    }
    if (caseInfo.conclusions) {
      submitValues = {
        ...submitValues,
        conclusions: caseInfo.conclusions.map((e) => e.id),
      };
    }

    postToOpenElisServerFullResponse(
      caseUrl,
      JSON.stringify(submitValues),
      displayStatus,
    );
  };

  useEffect(() => {
    componentMounted.current = true;
    setNextPage(null);
    setPreviousPage(null);
    setPagination(false);
    getFromOpenElisServer("/rest/displayList/PATHOLOGY_STATUS", setStatuses);
    getFromOpenElisServer(
      "/rest/displayList/PATHOLOGY_TECHNIQUES",
      setTechniques,
    );
    getFromOpenElisServer(
      "/rest/displayList/PATHOLOGIST_REQUESTS",
      setRequests,
    );
    getFromOpenElisServer(
      "/rest/displayList/PATHOLOGY_REQUEST_STATUS",
      setRequestStatuses,
    );
    getFromOpenElisServer(
      "/rest/displayList/IMMUNOHISTOCHEMISTRY_MARKERS_TESTS",
      setImmunoHistoChemistryTests,
    );
    getFromOpenElisServer(
      "/rest/paginatedDisplayList/PATHOLOGIST_CONCLUSIONS",
      loadConclusionData,
    );
    getFromOpenElisServer("/rest/users", setTechnicianUsers);
    getFromOpenElisServer("/rest/users/Pathologist", setPathologistUsers);
    getFromOpenElisServer(caseUrl, loadCase);

    return () => {
      componentMounted.current = false;
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "pathology.label.dashboard", link: "/PathologyDashboard" },
    { label: "breadcrumb.caseView", link: "" },
  ];

  const knownStage = PATHOLOGY_STAGES.includes(caseInfo.status);
  const sections = deriveCaseSections({
    intl,
    status: caseInfo.status,
    isPathologist,
    configurationProperties,
  });
  const openNames = openRequestNames(caseInfo.requests);

  const sectionBody = (section, readOnly) => {
    switch (section.id) {
      case "pathology-section-case-info":
        return <CaseInformationSection caseInfo={caseInfo} />;
      case "pathology-section-grossing":
        return (
          <GrossingSection
            caseInfo={caseInfo}
            updateCase={updateCase}
            readOnly={readOnly}
            technicianUsers={technicianUsers}
          />
        );
      case "pathology-section-microtomy":
        return (
          <MicrotomySection
            caseInfo={caseInfo}
            updateCase={updateCase}
            readOnly={readOnly}
            onSlideFile={(index, file) => attachFile("slides", index, file)}
          />
        );
      case "pathology-section-review":
        return (
          <ReviewSection
            caseInfo={caseInfo}
            updateCase={updateCase}
            readOnly={readOnly}
            pathologistUsers={pathologistUsers}
            requests={requests}
            requestStatuses={requestStatuses}
            formVersion={formVersion}
          />
        );
      case "pathology-section-findings":
        return (
          <FindingsSection
            caseInfo={caseInfo}
            updateCase={updateCase}
            readOnly={readOnly}
            techniques={techniques}
            conclusions={conclusions}
            immunoHistoChemistryTests={immunoHistoChemistryTests}
            pagination={pagination}
            currentApiPage={currentApiPage}
            totalApiPages={totalApiPages}
            previousPage={previousPage}
            nextPage={nextPage}
            onPreviousPage={() => loadConclusionsPage(previousPage)}
            onNextPage={() => loadConclusionsPage(nextPage)}
            formVersion={formVersion}
          />
        );
      case "pathology-section-reports":
        return (
          <ReportsSection
            caseInfo={caseInfo}
            updateCase={updateCase}
            readOnly={readOnly}
            reportParams={reportParams}
            loadingReport={loadingReport}
            onReportFile={(index, file) => attachFile("reports", index, file)}
            onGenerateReport={generateReport}
          />
        );
      default:
        return <StagePlaceholderSection />;
    }
  };

  // Every stage this laboratory tracks, plus the one the case already stands
  // at even when the laboratory has since stopped tracking it, because a case
  // must never be unable to say where it is. The list arrives from the server
  // in bench order (FR-2.1) carrying an English value; the label is localized
  // here while the item's id stays the raw enum name the save payload and the
  // backend expect.
  const stageItems = statuses.filter(
    (status) =>
      isStageEnabled(status.id, configurationProperties) ||
      status.id === caseInfo.status,
  );

  // A Carbon Dropdown rather than a Select: the bar sits at the foot of a long
  // page, where a native select opens its menu below itself and the browser
  // clips it against the window. direction="top" opens upward, in the page's
  // own markup.
  //
  // The hint is the control's own helperText rather than a span beside it, so
  // that Carbon wires it to the button with aria-describedby and it is
  // announced with the control it belongs to.
  const statusControl = (
    // Bounded, because a Carbon dropdown's wrapper is a block whose helper
    // text has no width of its own: as a flex item in the action bar the
    // group would otherwise take the width of the whole hint sentence and
    // push the buttons onto a line of their own.
    <div className="pathology-case-view__stage-control">
      <Dropdown
        id="status"
        size="sm"
        direction="top"
        // The same word the bar shows beside it, so what a sighted user reads
        // as the label is what assistive technology is told it is.
        titleText={intl.formatMessage({ id: "common.status" })}
        hideLabel
        helperText={intl.formatMessage({
          id: "pathology.banner.transitionsLater",
        })}
        label={intl.formatMessage({ id: "label.button.select.status" })}
        items={stageItems}
        itemToString={(item) =>
          item ? stageLabel(intl, item.id, item.value) : ""
        }
        selectedItem={
          stageItems.find((status) => status.id === caseInfo.status) ?? null
        }
        onChange={({ selectedItem }) =>
          selectedItem && updateCase({ status: selectedItem.id })
        }
      />
    </div>
  );

  const summaryRows = [
    {
      id: "stage",
      labelKey: "pathology.label.stage",
      value: knownStage ? stageLabel(intl, caseInfo.status) : null,
    },
    {
      id: "blocks",
      labelKey: "pathology.label.blocks",
      value: (caseInfo.blocks ?? []).length,
    },
    {
      id: "slides",
      labelKey: "pathology.label.slides",
      value: (caseInfo.slides ?? []).length,
    },
    {
      id: "requests",
      labelKey: "pathology.label.request",
      value: intl.formatMessage(
        { id: "pathology.badge.openRequestCount" },
        { count: openNames.length },
      ),
      // The count is what the row shows (FR-10.10's own layout); resting on
      // the row is what names the requests behind it.
      title:
        openNames.length > 0
          ? intl.formatMessage(
              { id: "pathology.badge.openRequests" },
              { names: openNames.join(", ") },
            )
          : undefined,
    },
    {
      id: "conclusion",
      labelKey: "pathology.label.conclusion",
      value:
        (caseInfo.conclusions ?? [])[0]?.value ||
        caseInfo.conclusionText ||
        null,
    },
    {
      id: "report",
      labelKey: "pathology.label.report",
      value:
        (caseInfo.reports ?? []).length ||
        intl.formatMessage({ id: "common.none" }),
    },
  ];

  // The title and patient band sit two Sections deep, as the two other
  // anatomic-pathology case views (immunohistochemistry, cytology) size
  // theirs; the shared PageTitle was not used because it renders the
  // breadcrumb's last label and this title carries the lab number. The rest
  // of the screen stays inside that context, so sub-headings and the summary
  // title land one level below. The Stack is the rhythm between the blocks.
  return (
    <Section>
      <Section>
        <Stack gap={6}>
          <PageBreadCrumb breadcrumbs={breadcrumbs} />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Heading>
                {caseLoaded ? (
                  <FormattedMessage
                    id="pathology.label.caseTitle"
                    values={{ labNumber: caseInfo.labNumber }}
                  />
                ) : (
                  <FormattedMessage id="pathology.label.case" />
                )}
              </Heading>
            </Column>
          </Grid>
          <PatientHeader
            id={caseInfo.patientPK}
            lastName={caseInfo.lastName}
            firstName={caseInfo.firstName}
            gender={caseInfo.sex}
            age={caseInfo.age}
            orderDate={caseInfo.requestDate}
            referringFacility={caseInfo.referringFacility}
            department={caseInfo.department}
            requester={caseInfo.requester}
            accesionNumber={caseInfo.labNumber}
            className="patient-header2"
            isOrderPage={true}
            statusTag={
              knownStage ? (
                <StatusBadge
                  kind={stageBadgeKind(caseInfo.status)}
                  textKey={stageDisplayKey(caseInfo.status)}
                />
              ) : null
            }
            assignedStaff={[
              {
                roleKey: "assigned.technician.label",
                name: caseInfo.assignedTechnician,
              },
              {
                roleKey: "assigned.pathologist.label",
                name: caseInfo.assignedPathologist,
              },
            ]}
          />
          {notificationVisible === true ? <AlertDialog /> : ""}
          {postSavePrintModel?.accessionNumber && (
            <PostSavePrintDialog
              accessionNumber={postSavePrintModel.accessionNumber}
              printableLabelTypes={postSavePrintModel.printableLabelTypes || []}
            />
          )}
          {!caseLoaded ? (
            <Loading
              description={intl.formatMessage({ id: "common.loading" })}
            />
          ) : (
            <>
              {loadingConclusions && (
                <Loading
                  description={intl.formatMessage({ id: "common.loading" })}
                />
              )}
              <CaseViewLayout
                rail={
                  <ProgressRail
                    items={deriveRailItems(sections, {
                      intl,
                      status: caseInfo.status,
                      reportCount: (caseInfo.reports ?? []).length,
                      openRequestNames: openNames,
                      caseInfo,
                    })}
                    currentIndex={railCurrentIndex(sections, caseInfo.status)}
                    onNavigate={(id) =>
                      document
                        .getElementById(id)
                        ?.scrollIntoView({ behavior: "smooth", block: "start" })
                    }
                  />
                }
                summary={<CaseSummaryPanel rows={summaryRows} />}
                actionBar={
                  <CaseActionBar
                    status={statusControl}
                    dirty={dirty}
                    saving={isSubmitting}
                    onDiscard={reloadCase}
                    onSaveDraft={save}
                  />
                }
              >
                <Accordion>
                  {sections.map((section) => (
                    <CaseSection
                      key={section.id}
                      id={section.id}
                      number={section.number}
                      titleKey={section.titleKey}
                      state={section.state}
                      lockedHintKey={section.lockedHintKey}
                      lockedHintValues={section.lockedHintValues}
                      badge={sectionBadge(section, {
                        openRequestCount: openNames.length,
                        caseInfo,
                      })}
                      defaultOpen={section.openByDefault}
                    >
                      {sectionBody(
                        section,
                        section.state === SECTION_STATE.READ_ONLY,
                      )}
                    </CaseSection>
                  ))}
                </Accordion>
              </CaseViewLayout>
            </>
          )}
        </Stack>
      </Section>
    </Section>
  );
}

export default PathologyCaseView;
