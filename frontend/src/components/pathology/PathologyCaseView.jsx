import React, { useContext, useState, useEffect, useRef } from "react";
import { useParams } from "react-router-dom";
import {
  Accordion,
  Button,
  Column,
  Grid,
  Heading,
  Loading,
  Section,
  Stack,
  TextInput,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  postToOpenElisServerJsonResponse,
  postToOpenElisServerForPDF,
  hasRole,
} from "../utils/Utils";
import { requestFailed } from "../utils/requestOutcome";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { NotificationContext, ConfigurationContext } from "../layout/contexts";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { RequiredMarker, requiredProps } from "../common/RequiredMarker";
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
  advanceBlockedReason,
  advanceLabelKey,
  decalcificationFlagFrozen,
  deriveCaseSections,
  deriveRailItems,
  EMBEDDING_SECTION_ID,
  embeddingStatement,
  MICROTOMY_SECTION_ID,
  openRequestNames,
  PROCESSING_SECTION_ID,
  railCurrentIndex,
  returnBlockedReason,
  sectionBadge,
  sectionIdOfStage,
  stageBadgeKind,
} from "./pathologySections";
import {
  blockIdentifier,
  countedRows,
  embeddingSummary,
  isEmbeddedRow,
  nextOutstandingCassette,
  objectNaming,
  stripClientKeys,
} from "./pathologyRows";
import DeactivateRowDialog from "./DeactivateRowDialog";
import { focusFirstOf } from "./focus";
import CaseInformationSection from "./sections/CaseInformationSection";
import GrossingSection from "./sections/GrossingSection";
import EmbeddingSection from "./sections/EmbeddingSection";
import ProcessingSection from "./sections/ProcessingSection";
import StagePlaceholderSection from "./sections/StagePlaceholderSection";
import MicrotomySection from "./sections/MicrotomySection";
import ReviewSection from "./sections/ReviewSection";
import FindingsSection from "./sections/FindingsSection";
import ReportsSection from "./sections/ReportsSection";

// The switch that shows a list's deactivated rows, where focus lands once
// the row it was on has left the list.
const SHOW_DEACTIVATED_SWITCH = {
  block: "#showDeactivatedCassettes",
  slide: "#showDeactivatedSlides",
};

// A section's own header, where focus lands once the case has moved to the
// stage it is worked in.
const sectionHeader = (sectionId) =>
  "#" + sectionId + " button.cds--accordion__heading";

// The embedding section's own header, where focus lands once no Embed
// button it could go to is left.
const EMBEDDING_SECTION_HEADER = sectionHeader(EMBEDDING_SECTION_ID);

const embedButton = (block) => "#embedCassette-" + block.id;

// The return control, where focus goes back to once the reason row closes,
// and the reason field it opens.
const RETURN_STAGE_BUTTON = "#returnStage";
const RETURN_REASON_ID = "returnStageReason";

// The column a return's reason is stored in.
const RETURN_REASON_MAX_LENGTH = 255;

// The ids of the sentences that say why a return control cannot be pressed,
// for the controls' aria-describedby. One case view is on a page at a time.
const RETURN_BLOCKED_HINT_ID = "returnStageBlockedReason";
const RETURN_CONFIRM_HINT_ID = "returnStageConfirmReason";

const TISSUE_TYPES_URL = "/rest/dictionary/category/pathology_tissue_types";

const STAINS_URL =
  "/rest/dictionary/category/" + encodeURIComponent("Pathology Stains");

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

  const [techniques, setTechniques] = useState([]);
  const [requests, setRequests] = useState([]);
  const [requestStatuses, setRequestStatuses] = useState([]);
  const [conclusions, setConclusions] = useState([]);
  const [immunoHistoChemistryTests, setImmunoHistoChemistryTests] = useState(
    [],
  );
  const [technicianUsers, setTechnicianUsers] = useState([]);
  const [pathologistUsers, setPathologistUsers] = useState([]);
  // Null until the list has arrived, so an empty list can only ever mean the
  // laboratory has configured none.
  const [tissueTypes, setTissueTypes] = useState(null);
  const [stains, setStains] = useState(null);
  const [loadingConclusions, setLoadingConclusions] = useState(false);
  const [loadingReport, setLoadingReport] = useState(false);
  const [nextPage, setNextPage] = useState(null);
  const [previousPage, setPreviousPage] = useState(null);
  const [pagination, setPagination] = useState(false);
  const [currentApiPage, setCurrentApiPage] = useState(null);
  const [totalApiPages, setTotalApiPages] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [postSavePrintModel, setPostSavePrintModel] = useState(null);
  const [pendingDeactivation, setPendingDeactivation] = useState(null);
  // The id of the cassette whose embedding is in flight, so no second one
  // starts before the first has been read back.
  const [embeddingId, setEmbeddingId] = useState(null);
  // True while a move of the case to another stage is unanswered. The ref is
  // the guard, so a second click in the same tick cannot start a second move
  // from a stage the first may already have left; the state is what renders
  // the form read-only meanwhile, so nothing typed can be lost to the re-read.
  const transitioningRef = useRef(false);
  const [transitioning, setTransitioning] = useState(false);
  // The inline return row: whether it is open, and the reason typed into it.
  const [returnOpen, setReturnOpen] = useState(false);
  const [returnReason, setReturnReason] = useState("");
  // The selectors, in order of preference, for the control that takes focus
  // once the case is read back, and, when set, the only controls focus may be
  // taken from (see focusFirstOf).
  const focusAfterLoad = useRef(null);
  const focusOnlyFrom = useRef(null);
  const releaseAfterLoad = useRef(null);
  // Mirrors `dirty` for a server answer that arrives after the render that
  // sent the request, so it can tell whether there is unsaved work to keep.
  const dirtyRef = useRef(false);
  const [reportParams, setReportParams] = useState({
    0: {
      submited: false,
      reportLink: "",
    },
  });

  const isPathologist = hasRole(userSessionDetails, "Pathologist");
  // The stage the case is stored at. Nothing on this screen edits it: it
  // changes only by a move, and every move reads the case back, so it is
  // always the stage the server judges a save or a move by.
  const stage = caseInfo.status ?? null;
  const decalcificationRequired = Boolean(caseInfo.decalcificationRequired);
  const flagFrozen = decalcificationFlagFrozen(stage);

  const updateCase = (patch) => {
    setCaseInfo((prev) => ({
      ...prev,
      ...(typeof patch === "function" ? patch(prev) : patch),
    }));
    dirtyRef.current = true;
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
    const release = releaseAfterLoad.current;
    releaseAfterLoad.current = null;
    if (!loaded) {
      release?.();
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
        userSessionDetails.lastName + "," + userSessionDetails.firstName;
    }
    if (!loaded.assignedTechnicianId) {
      loaded.assignedTechnicianId = userSessionDetails.userId;
      loaded.assignedTechnician =
        userSessionDetails.lastName + "," + userSessionDetails.firstName;
    }
    setCaseInfo(loaded);
    release?.();
    dirtyRef.current = false;
    setDirty(false);
    const focusTargets = focusAfterLoad.current;
    const onlyFrom = focusOnlyFrom.current;
    focusAfterLoad.current = null;
    focusOnlyFrom.current = null;
    if (focusTargets) {
      focusFirstOf(focusTargets, { onlyFrom });
    }
    setCaseLoaded(true);
    setFormVersion((version) => version + 1);
  };

  // The tissue types and stains are read again with the case because a
  // refused save can mean one was retired since the page loaded, and
  // reloading is what the bench is told to do about it. A failed read keeps
  // the list it had.
  const reloadCase = () => {
    getFromOpenElisServer(TISSUE_TYPES_URL, (list) => {
      if (componentMounted.current && Array.isArray(list)) {
        setTissueTypes(list);
      }
    });
    getFromOpenElisServer(STAINS_URL, (list) => {
      if (componentMounted.current && Array.isArray(list)) {
        setStains(list);
      }
    });
    getFromOpenElisServer(caseUrl, loadCase);
  };

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

  // The sentence for a refusal the server names by a message id, in the
  // reader's language, or null when it names none this build knows; the
  // server's own words are English and are never shown.
  // i18n-keys: pathology.error.*
  const translatedRefusal = (body) =>
    body?.messageKey && intl.messages[body.messageKey]
      ? intl.formatMessage({ id: body.messageKey }, body.messageArgs ?? {})
      : null;

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
      let messageId = "error.save.msg";
      if (body?.messageKey === "clinicalAttachment.uploadHint") {
        messageId = "clinicalAttachment.uploadHint";
      } else if (response?.status === 400) {
        messageId = "pathology.error.reloadCase";
      } else if (body?.conflict === "designation") {
        messageId = "pathology.error.designationConflict";
      }
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message:
          translatedRefusal(body) ?? intl.formatMessage({ id: messageId }),
      });
    }
  }

  // Deactivate cannot be pressed while the form is dirty or saving, so this
  // read-back can never overwrite work that was not yet saved.
  const onRowDeactivated = (pending) => {
    const naming = objectNaming(pending.kind, pending.row);
    focusAfterLoad.current = SHOW_DEACTIVATED_SWITCH[pending.kind];
    setPendingDeactivation(null);
    setNotificationVisible(true);
    addNotification({
      kind: NotificationKinds.success,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({ id: naming.toastKey }, naming.values),
    });
    reloadCase();
  };

  // Embed cannot be pressed while the form is dirty or saving, but the bench
  // can type while the request is out. The answer re-reads the case only when
  // nothing is waiting to be saved; otherwise it marks the one row as a block
  // and keeps the rest of the form as it stands. The save copies fields onto
  // the stored rows by id and reads no version from the screen, so the row's
  // older stamp does no harm. The transition is not destructive, so it asks
  // for no confirmation.
  const embedCassette = (block) => {
    if (embeddingId !== null) {
      return;
    }
    setEmbeddingId(block.id);
    const designation = blockIdentifier(block);
    const next = nextOutstandingCassette(caseInfo.blocks, block);
    postToOpenElisServerJsonResponse(
      "/rest/pathology/block/" + block.id + "/embed",
      JSON.stringify({}),
      (response) => {
        if (!componentMounted.current) {
          return;
        }
        setEmbeddingId(null);
        setNotificationVisible(true);
        if (isEmbeddedRow(response)) {
          const targets = next
            ? [embedButton(next), EMBEDDING_SECTION_HEADER]
            : EMBEDDING_SECTION_HEADER;
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage(
              { id: "pathology.toast.embedded" },
              { designation },
            ),
          });
          if (dirtyRef.current) {
            updateCase((prev) => ({
              blocks: (prev.blocks ?? []).map((row) =>
                String(row.id) === String(block.id)
                  ? { ...row, cassetteState: response.cassetteState }
                  : row,
              ),
            }));
            focusFirstOf(targets, { unlessHeld: true });
          } else {
            focusAfterLoad.current = targets;
            reloadCase();
          }
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message:
              translatedRefusal(response) ??
              intl.formatMessage(
                { id: "pathology.toast.embedFailed" },
                { designation },
              ),
          });
          // A refusal can mean another change landed on the row, so a clean
          // form is read again to show the row as it is stored.
          const targets = [embedButton(block), EMBEDDING_SECTION_HEADER];
          if (dirtyRef.current) {
            focusFirstOf(targets, { unlessHeld: true });
          } else {
            focusAfterLoad.current = targets;
            reloadCase();
          }
        }
      },
    );
  };

  // Moves the case to another stage as its own request, naming the stage the
  // page shows so a page someone else has overtaken is refused rather than
  // moving the case twice. While the request is out the form is read-only and
  // Save is held, so the form is clean when the answer lands and reading the
  // case back loses nothing typed; should it not be, the form is left as it
  // stands and the next save or discard reads the case. A refusal the server
  // names is shown in its own words, and the case is read again, since a
  // refusal can mean another change landed after the page was read.
  //
  // Focus follows the case to the section of the stage it now stands at, or
  // to the return control where that section is not this person's to open,
  // but only away from the control that started the move: a person who has
  // gone on to another control meanwhile keeps it.
  const moveCase = (direction, payload, toastKey, startedBy) => {
    if (transitioningRef.current) {
      return;
    }
    transitioningRef.current = true;
    setTransitioning(true);
    postToOpenElisServerJsonResponse(
      caseUrl + "/" + direction,
      JSON.stringify({ from: stage, ...payload }),
      (response) => {
        if (!componentMounted.current) {
          return;
        }
        const release = () => {
          transitioningRef.current = false;
          setTransitioning(false);
        };
        setNotificationVisible(true);
        const readBack = (targets) => {
          if (dirtyRef.current) {
            release();
            return;
          }
          focusAfterLoad.current = targets;
          focusOnlyFrom.current = startedBy ? [startedBy] : [];
          releaseAfterLoad.current = release;
          reloadCase();
        };
        if (!requestFailed(response)) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage(
              { id: toastKey },
              { stage: stageLabel(intl, response.status) },
            ),
          });
          setReturnOpen(false);
          setReturnReason("");
          const sectionId = sectionIdOfStage(response.status);
          readBack(
            sectionId
              ? [sectionHeader(sectionId), RETURN_STAGE_BUTTON]
              : [RETURN_STAGE_BUTTON],
          );
          return;
        }
        const conflict =
          response?.conflict === "stage" || response?.conflict === "version";
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message:
            translatedRefusal(response) ??
            intl.formatMessage({
              id: conflict ? "pathology.error.stageConflict" : "error.save.msg",
            }),
        });
        if (conflict) {
          // The stage the reason was typed against has gone, so the row is
          // closed rather than left naming a different target.
          setReturnOpen(false);
          setReturnReason("");
          readBack(direction === "return" ? [RETURN_STAGE_BUTTON] : null);
        } else if (response?.status === 400) {
          readBack(null);
        } else {
          release();
        }
      },
    );
  };

  const advanceCase = (event) =>
    moveCase(
      "advance",
      {},
      "pathology.toast.stageAdvanced",
      event?.currentTarget,
    );

  const returnCase = (event) =>
    moveCase(
      "return",
      { reason: returnReason.trim() },
      "pathology.toast.stageReturned",
      event?.currentTarget,
    );

  const openReturn = () => {
    setReturnOpen(true);
    focusFirstOf("#" + RETURN_REASON_ID);
  };

  const cancelReturn = () => {
    setReturnOpen(false);
    setReturnReason("");
    focusFirstOf(RETURN_STAGE_BUTTON);
  };

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
      blocks: stripClientKeys(caseInfo.blocks),
      slides: stripClientKeys(caseInfo.slides),
      reports: caseInfo.reports,
      grossExam: caseInfo.grossExam,
      decalcificationRequired,
      microscopyExam: caseInfo.microscopyExam,
      conclusionText: caseInfo.conclusionText,
      release: caseInfo.release != undefined ? caseInfo.release : false,
      referToImmunoHistoChemistry: caseInfo.referToImmunoHistoChemistry,
    };
    // The run reference is written once per processing entry, so it is sent
    // only while the server says it can still be recorded.
    if (caseInfo.runReferenceEditable) {
      submitValues = { ...submitValues, runReference: caseInfo.runReference };
    }
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
    reloadCase();

    return () => {
      componentMounted.current = false;
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "pathology.label.dashboard", link: "/PathologyDashboard" },
    { label: "breadcrumb.caseView", link: "" },
  ];

  const knownStage = PATHOLOGY_STAGES.includes(stage);
  const sections = deriveCaseSections({
    intl,
    status: stage,
    isPathologist,
    configurationProperties,
    decalcificationRequired,
    // Retired slides count: they are on record, and a stage switched off
    // must still show them.
    sectionHasRows: (candidate) =>
      candidate.id === MICROTOMY_SECTION_ID &&
      (caseInfo.slides ?? []).length > 0,
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
            tissueTypes={tissueTypes}
            decalcificationFlagFrozen={flagFrozen}
            dirty={dirty}
            saving={isSubmitting}
            moving={transitioning}
            onDeactivate={setPendingDeactivation}
          />
        );
      case PROCESSING_SECTION_ID:
        return (
          <ProcessingSection
            caseInfo={caseInfo}
            updateCase={updateCase}
            readOnly={readOnly}
          />
        );
      case EMBEDDING_SECTION_ID:
        return (
          <EmbeddingSection
            caseInfo={caseInfo}
            updateCase={updateCase}
            readOnly={readOnly}
            dirty={dirty}
            saving={isSubmitting}
            moving={transitioning}
            embeddingId={embeddingId}
            onEmbed={embedCassette}
          />
        );
      case MICROTOMY_SECTION_ID:
        return (
          <MicrotomySection
            caseInfo={caseInfo}
            updateCase={updateCase}
            readOnly={readOnly}
            untracked={Boolean(section.readOnlyBecauseUntracked)}
            stains={stains}
            configurationProperties={configurationProperties}
            onSlideFile={(index, file) => attachFile("slides", index, file)}
            dirty={dirty}
            saving={isSubmitting}
            moving={transitioning}
            onDeactivate={setPendingDeactivation}
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

  // Where the case can be sent on to and back to, as the server computes it
  // from the stored case with the rule its own move endpoints apply: a stage
  // the laboratory does not track, or a decalcification grossing did not
  // flag, is skipped there. The case is read again after every save and
  // move, and neither move can be pressed with unsaved work, so the stored
  // case is the one these describe.
  const nextStage = caseInfo.nextStage ?? null;
  const previousStage = caseInfo.previousStage ?? null;
  const advanceBlocked = advanceBlockedReason(intl, caseInfo, {
    storedStatus: stage,
    dirty,
    saving: isSubmitting,
    moving: transitioning,
  });
  const returnBlocked = returnBlockedReason({
    storedStatus: stage,
    previousStage,
    dirty,
    saving: isSubmitting,
    moving: transitioning,
  });
  // The confirm button's own reason: the return control's first, since a
  // reason typed into a page that cannot move the case changes nothing.
  const returnConfirmBlocked =
    returnBlocked ??
    (returnReason.trim() === ""
      ? { key: "pathology.locked.returnReasonRequired" }
      : null);
  const blockedText = (blocked) =>
    blocked ? intl.formatMessage({ id: blocked.key }, blocked.values) : null;

  // One primary whose label names where the case goes next (S-7.2). From
  // Ready for Pathologist on there is none: the pathologist's own actions
  // move the case from there. A next stage this build has no label for gets
  // no primary either, rather than a button with no words.
  const advanceKey = advanceLabelKey(nextStage);
  const primary = advanceKey
    ? {
        labelKey: advanceKey,
        disabledReasonKey: advanceBlocked?.key,
        disabledReasonValues: advanceBlocked?.values,
        onClick: advanceCase,
      }
    : undefined;

  // The stage as text, and the return beside it. A return is not
  // destructive, so its reason is asked for inline rather than in a modal,
  // and it stays out of the action group, which holds three buttons at most.
  const statusControl = (
    <div className="pathology-case-view__stage-control">
      <span id="caseStage" className="pathology-case-view__stage-readout">
        <span className="cds--visually-hidden">
          <FormattedMessage id="common.status" />{" "}
        </span>
        {knownStage ? (
          stageLabel(intl, stage)
        ) : (
          <FormattedMessage id="caseView.label.notRecorded" />
        )}
      </span>
      {!returnOpen && (
        <>
          <Button
            kind="ghost"
            size="sm"
            id="returnStage"
            disabled={Boolean(returnBlocked)}
            title={blockedText(returnBlocked) ?? undefined}
            aria-describedby={
              returnBlocked ? RETURN_BLOCKED_HINT_ID : undefined
            }
            onClick={openReturn}
          >
            <FormattedMessage id="pathology.action.returnStage" />
          </Button>
          {returnBlocked && (
            <span id={RETURN_BLOCKED_HINT_ID} className="cds--visually-hidden">
              {blockedText(returnBlocked)}
            </span>
          )}
        </>
      )}
      {returnOpen && (
        <div className="pathology-case-view__return-row">
          <TextInput
            id={RETURN_REASON_ID}
            size="sm"
            labelText={
              <>
                {intl.formatMessage({ id: "common.reason" })}
                <RequiredMarker />
              </>
            }
            {...requiredProps()}
            helperText={intl.formatMessage({
              id: "pathology.label.returnReasonHelper",
            })}
            maxLength={RETURN_REASON_MAX_LENGTH}
            value={returnReason}
            onChange={(e) => setReturnReason(e.target.value)}
          />
          <Button
            kind="secondary"
            size="sm"
            id="returnStageConfirm"
            disabled={Boolean(returnConfirmBlocked)}
            title={blockedText(returnConfirmBlocked) ?? undefined}
            aria-describedby={
              returnConfirmBlocked ? RETURN_CONFIRM_HINT_ID : undefined
            }
            onClick={returnCase}
          >
            <FormattedMessage
              id="pathology.action.returnTo"
              values={{
                stage: previousStage ? stageLabel(intl, previousStage) : "",
              }}
            />
          </Button>
          {/* Held while the return is out: closing the row then would
              drop focus on the page while the case still moves. */}
          <Button
            kind="ghost"
            size="sm"
            id="returnStageCancel"
            disabled={transitioning}
            onClick={cancelReturn}
          >
            <FormattedMessage id="common.cancel" />
          </Button>
          {returnConfirmBlocked && (
            <span
              id={RETURN_CONFIRM_HINT_ID}
              className="case-view__locked-hint"
            >
              {blockedText(returnConfirmBlocked)}
            </span>
          )}
        </div>
      )}
    </div>
  );

  const embedding = embeddingStatement(intl, {
    summary: embeddingSummary(caseInfo.blocks),
    status: stage,
    configurationProperties,
  });

  const summaryRows = [
    {
      id: "stage",
      labelKey: "pathology.label.stage",
      value: knownStage ? stageLabel(intl, stage) : null,
    },
    {
      id: "decalcification",
      labelKey: "pathology.label.decalcificationRequired",
      value: decalcificationRequired,
    },
    {
      id: "blocks",
      labelKey: "pathology.label.cassettes",
      value: countedRows(caseInfo.blocks).length,
    },
    {
      id: "embedding",
      labelKey: "pathology.stage.embedding",
      value: embedding.value,
      title: embedding.title,
    },
    {
      id: "slides",
      labelKey: "pathology.label.slides",
      value: countedRows(caseInfo.slides).length,
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
                  kind={stageBadgeKind(stage)}
                  textKey={stageDisplayKey(stage)}
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
                      status: stage,
                      reportCount: (caseInfo.reports ?? []).length,
                      openRequestNames: openNames,
                      caseInfo,
                    })}
                    currentIndex={railCurrentIndex(sections, stage)}
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
                    saving={isSubmitting || transitioning}
                    onDiscard={reloadCase}
                    onSaveDraft={save}
                    primary={primary}
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
                      {/* Read-only while a move is out, so nothing typed can
                          be lost to the read-back that follows it. */}
                      {sectionBody(
                        section,
                        section.state === SECTION_STATE.READ_ONLY ||
                          transitioning,
                      )}
                    </CaseSection>
                  ))}
                </Accordion>
              </CaseViewLayout>
              <DeactivateRowDialog
                pending={pendingDeactivation}
                onClose={() => setPendingDeactivation(null)}
                onRowDeactivated={onRowDeactivated}
              />
            </>
          )}
        </Stack>
      </Section>
    </Section>
  );
}

export default PathologyCaseView;
