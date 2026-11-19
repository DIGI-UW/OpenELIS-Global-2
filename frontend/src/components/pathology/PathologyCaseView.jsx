import React, { useContext, useState, useEffect, useRef } from "react";
import { useParams } from "react-router-dom";
import {
  Accordion,
  Button,
  Column,
  Grid,
  Heading,
  ListItem,
  Loading,
  Section,
  Stack,
  TextInput,
  UnorderedList,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  postToOpenElisServerJsonResponse,
  postToOpenElisServerForPDF,
  hasRole,
  Roles,
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
  COVERSLIPPING_SECTION_ID,
  decalcificationFlagFrozen,
  FINDINGS_SECTION_ID,
  grossExamPlace,
  deriveCaseSections,
  deriveRailItems,
  EMBEDDING_SECTION_ID,
  embeddingStatement,
  MICROTOMY_SECTION_ID,
  openRequestNames,
  openRequests,
  PROCESSING_SECTION_ID,
  railCurrentIndex,
  returnBlockedReason,
  REVIEW_SECTION_ID,
  reviewTakesRequests,
  sectionBadge,
  sectionHeader,
  sectionHoldsRecords,
  sectionIdOfStage,
  slideWorkStatement,
  slideWorkSummaries,
  stageBadgeKind,
  STAINING_SECTION_ID,
} from "./pathologySections";
import {
  blockIdentifier,
  countedRows,
  embeddingSummary,
  isEmbeddedRow,
  nextOutstandingCassette,
  objectNaming,
  slideOnBlockName,
  stripClientKeys,
} from "./pathologyRows";
import DeactivateRowDialog from "./DeactivateRowDialog";
import { NEW_REQUEST_BUTTON_ID } from "./RequestsTable";
import { focusFirstOf } from "./focus";
import CaseInformationSection from "./sections/CaseInformationSection";
import GrossingSection from "./sections/GrossingSection";
import EmbeddingSection from "./sections/EmbeddingSection";
import ProcessingSection from "./sections/ProcessingSection";
import StainingSection from "./sections/StainingSection";
import CoverslippingSection from "./sections/CoverslippingSection";
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

// The confirmation that asks before a case goes to the pathologist with
// requests still open (FR-10.11): itself, its sentence, its two buttons and
// the sentence that says why Send anyway cannot be pressed.
const SEND_CONFIRM_ID = "sendAnywayConfirm";
const SEND_CONFIRM_TEXT_ID = "sendAnywayText";
const SEND_ANYWAY_ID = "sendAnyway";
const SEND_ANYWAY_CANCEL_ID = "sendAnywayCancel";
const SEND_ANYWAY_HINT_ID = "sendAnywayHint";

// The refusal of a move onto the pathologist that names an open request the
// move did not, such as one raised after the page was read.
const OPEN_REQUESTS_REFUSAL = "pathology.error.openRequestsUnacknowledged";

const TISSUE_TYPES_URL = "/rest/dictionary/category/pathology_tissue_types";

// Where focus lands once a request change has been read back: the table's
// New request, or the Review section's header where it is not offered.
const AFTER_REQUEST_CHANGE = [
  "#" + NEW_REQUEST_BUTTON_ID,
  sectionHeader(REVIEW_SECTION_ID),
];

// The toast a request status change ends on, by the status it posts.
// i18n-keys: pathology.toast.request*
const REQUEST_STATUS_TOASTS = Object.freeze({
  COMPLETED: "pathology.toast.requestCompleted",
  CANCELLED: "pathology.toast.requestCancelled",
  OPENED: "pathology.toast.requestReopened",
});

const STAINS_URL =
  "/rest/dictionary/category/" + encodeURIComponent("Pathology Stains");

// What each slide mark at staining or coverslipping posts, the field the
// value goes in, the row field it is compared with, and the toast that names
// the slides once it lands.
// i18n-keys: pathology.toast.*
const SLIDE_WORK = Object.freeze({
  STAINED: {
    path: "/slides/stain-status",
    field: "status",
    rowField: "stainStatus",
    toastKey: "pathology.toast.slidesStained",
    toastKind: NotificationKinds.success,
  },
  UNSATISFACTORY: {
    path: "/slides/stain-status",
    field: "status",
    rowField: "stainStatus",
    toastKey: "pathology.toast.slidesUnsatisfactory",
    toastKind: NotificationKinds.info,
  },
  PASS: {
    path: "/slides/coverslip-qc",
    field: "result",
    rowField: "coverslipQc",
    toastKey: "pathology.toast.qcPassed",
    toastKind: NotificationKinds.success,
  },
  FAIL: {
    path: "/slides/coverslip-qc",
    field: "result",
    rowField: "coverslipQc",
    toastKey: "pathology.toast.qcFailed",
    toastKind: NotificationKinds.info,
  },
});

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
  const [immunoHistoChemistryTests, setImmunoHistoChemistryTests] = useState(
    [],
  );
  const [technicianUsers, setTechnicianUsers] = useState([]);
  const [pathologistUsers, setPathologistUsers] = useState([]);
  // Null until the list has arrived, so an empty list can only ever mean the
  // laboratory has configured none.
  const [tissueTypes, setTissueTypes] = useState(null);
  const [stains, setStains] = useState(null);
  const [loadingReport, setLoadingReport] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [postSavePrintModel, setPostSavePrintModel] = useState(null);
  const [pendingDeactivation, setPendingDeactivation] = useState(null);
  // The id of the cassette whose embedding is in flight, so no second one
  // starts before the first has been read back.
  const [embeddingId, setEmbeddingId] = useState(null);
  // True while a move of the case or a slide mark is unanswered or the case
  // is still being read back after it (see sendHeld). The ref is the guard,
  // so a second click in the same tick cannot post a second change from a
  // case the first may already have changed; the state is what renders the
  // form read-only meanwhile, so nothing typed can be lost to the re-read.
  const heldRef = useRef(false);
  const [held, setHeld] = useState(false);
  // The inline return row: whether it is open, and the reason typed into it.
  const [returnOpen, setReturnOpen] = useState(false);
  const [returnReason, setReturnReason] = useState("");
  // The confirmation before sending the case to the pathologist with open
  // requests, which shares the status slot with the return row, one at a
  // time, and the primary that opened it, where Cancel puts focus back.
  const [sendConfirmOpen, setSendConfirmOpen] = useState(false);
  const primaryButton = useRef(null);
  // A request started from a Microtomy row: its kind and target, and a nonce
  // so each press opens the new request again (see RequestsTable).
  const [requestPrefill, setRequestPrefill] = useState(null);
  // Counts the moves to Under Pathologist Review, each of which opens
  // Findings & Conclusion (see CaseSection's openRequest).
  const [findingsOpenRequest, setFindingsOpenRequest] = useState(0);
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

  // The sentence for a refusal the server names by a message id, in the
  // reader's language, or null when it names none this build knows; the
  // server's own words are English and are never shown.
  // i18n-keys: pathology.error.*
  const translatedRefusal = (body) =>
    body?.messageKey && intl.messages[body.messageKey]
      ? intl.formatMessage({ id: body.messageKey }, body.messageArgs ?? {})
      : null;

  // What a refused change says: the server's sentence where it names one,
  // else, for a version check lost to another change, the save's generic
  // "another save changed this case", else the caller's own fallback.
  const refusalMessage = (body, fallbackKey) =>
    translatedRefusal(body) ??
    intl.formatMessage({
      id:
        body?.conflict === "version"
          ? "pathology.error.designationConflict"
          : fallbackKey,
    });

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
        message: refusalMessage(body, messageId),
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

  // Checks one slide's label against its block (FR-7.3 to FR-7.5) and
  // resolves what came of it: `{ ok: true, verification }`, `{ ok: false,
  // mismatch: { slideBarcode, blockBarcode } }` when the label belongs to
  // another block, or `{ ok: false }` for any other refusal, which is told
  // here. A check changes nothing the save posts, so a verified slide is
  // written into the case without marking the form dirty and without reading
  // the case again, and nothing typed meanwhile is lost. Where focus goes
  // next, and whether a check is in flight, the section keeps for itself.
  // Answered after the case view has gone, it resolves nothing, so nothing
  // is written into a section that went with it.
  const verifySlideLabel = (slideId, block, slideLabel) =>
    new Promise((resolve) => {
      const slide = (caseInfo.slides ?? []).find(
        (row) => String(row.id) === String(slideId),
      );
      const name = slide ? slideOnBlockName(slide, caseInfo.blocks) : "";
      postToOpenElisServerJsonResponse(
        "/rest/pathology/slide/" + slideId + "/verify-label",
        JSON.stringify({ block, slide: slideLabel }),
        (response) => {
          if (!componentMounted.current) {
            return;
          }
          if (!requestFailed(response)) {
            setCaseInfo((prev) => ({
              ...prev,
              labelVerifications: {
                ...(prev.labelVerifications ?? {}),
                [String(slideId)]: response.labelVerification,
              },
            }));
            setNotificationVisible(true);
            addNotification({
              kind: NotificationKinds.success,
              title: intl.formatMessage({ id: "notification.title" }),
              message: intl.formatMessage(
                { id: "pathology.toast.labelVerified" },
                { slide: name },
              ),
            });
            resolve({ ok: true, verification: response.labelVerification });
            return;
          }
          if (response?.conflict === "label") {
            resolve({
              ok: false,
              mismatch: {
                slideBarcode: response.messageArgs?.slideBarcode,
                blockBarcode: response.messageArgs?.blockBarcode,
              },
            });
            return;
          }
          setNotificationVisible(true);
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message:
              translatedRefusal(response) ??
              intl.formatMessage({ id: "pathology.toast.verifyFailed" }),
          });
          // A refusal can mean another change landed on the slide, so a
          // clean form is read again to show the slide as it is stored.
          if (!dirtyRef.current) {
            reloadCase();
          }
          resolve({ ok: false });
        },
      );
    });

  // Posts a change to the stored case that the page has to read back: a move
  // to another stage or a slide mark. It is held from the press until the
  // case has been read again, and the form is read-only meanwhile, so a
  // second press in that window posts nothing and nothing typed is lost to
  // the read. Should the form be dirty all the same, it is left as it
  // stands, and the next save or discard reads the case.
  //
  // `onAnswer(response, ok)` tells the person what happened and returns where
  // focus goes once the case is read back. A success or a refusal (400, 409)
  // reads the case again, since a refusal can mean another change landed
  // after the page was read; any other failure lets go at once. Focus moves
  // only away from the control that started the change, so a person who has
  // gone on to another control meanwhile keeps it. Resolves `{ ok }` once
  // the hold is let go, or `{ ok: false }` at once for a second press.
  const sendHeld = ({ path, body, startedBy, onAnswer }) =>
    new Promise((resolve) => {
      if (heldRef.current) {
        resolve({ ok: false });
        return;
      }
      heldRef.current = true;
      setHeld(true);
      postToOpenElisServerJsonResponse(
        caseUrl + path,
        JSON.stringify(body),
        (response) => {
          if (!componentMounted.current) {
            return;
          }
          const ok = !requestFailed(response);
          const release = () => {
            heldRef.current = false;
            setHeld(false);
            resolve({ ok });
          };
          setNotificationVisible(true);
          const targets = onAnswer(response, ok);
          const refused = response?.status === 400 || response?.status === 409;
          if ((ok || refused) && !dirtyRef.current) {
            focusAfterLoad.current = targets;
            focusOnlyFrom.current = startedBy ? [startedBy] : [];
            releaseAfterLoad.current = release;
            reloadCase();
          } else {
            release();
          }
        },
      );
    });

  // Moves the case to another stage as its own request, naming the stage the
  // page shows so a page someone else has overtaken is refused rather than
  // moving the case twice. A refusal the server names is shown in its own
  // words.
  //
  // Focus follows the case to the section of the stage it now stands at, or
  // to the return control where that section is not this person's to open.
  const moveCase = (direction, payload, toastKey, startedBy) =>
    sendHeld({
      path: "/" + direction,
      body: { from: stage, ...payload },
      startedBy,
      onAnswer: (response, ok) => {
        if (ok) {
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
          setSendConfirmOpen(false);
          // Begin review opens Findings & Conclusion and lands in the reading
          // itself, not on its header: the pathologist pressed it to write the
          // microscopic description, the macroscopic one being already
          // written.
          if (response.status === "UNDER_REVIEW") {
            if (direction === "advance") {
              setFindingsOpenRequest((count) => count + 1);
            }
            return [
              "#microscopyExam",
              "#grossExam",
              sectionHeader(FINDINGS_SECTION_ID),
              RETURN_STAGE_BUTTON,
            ];
          }
          const sectionId = sectionIdOfStage(response.status);
          return sectionId
            ? [sectionHeader(sectionId), RETURN_STAGE_BUTTON]
            : [RETURN_STAGE_BUTTON];
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
          setSendConfirmOpen(false);
          return direction === "return" ? [RETURN_STAGE_BUTTON] : null;
        }
        // A request opened behind the page: the confirmation is shown, or
        // stays, listing what the case read after this refusal holds open.
        if (response?.messageKey === OPEN_REQUESTS_REFUSAL) {
          setReturnOpen(false);
          setReturnReason("");
          setSendConfirmOpen(true);
          return ["#" + SEND_ANYWAY_ID];
        }
        return null;
      },
    });

  // Records a stain status or a coverslip QC result on one slide or a batch
  // (FR-8.2). The toast names only the slides the mark changed, since the
  // server leaves a slide that already carries the value as it is. A mark
  // that holds a slide back (unsatisfactory, failed) is told as information,
  // not as a success. After a refusal focus goes back where the mark started.
  const recordSlideWork = ({
    value,
    slides,
    startedBy,
    focusTargets,
    refusalFocusTargets,
  }) => {
    const work = SLIDE_WORK[value];
    const changed = slides.filter((slide) => slide[work.rowField] !== value);
    const names = (list) =>
      list.map((slide) => slideOnBlockName(slide, caseInfo.blocks)).join(", ");
    return sendHeld({
      path: work.path,
      body: {
        slideIds: slides.map((slide) => slide.id),
        [work.field]: value,
      },
      startedBy,
      onAnswer: (response, ok) => {
        if (ok) {
          addNotification({
            kind: work.toastKind,
            title: intl.formatMessage({ id: "notification.title" }),
            message:
              changed.length > 0
                ? intl.formatMessage(
                    { id: work.toastKey },
                    { slides: names(changed) },
                  )
                : intl.formatMessage(
                    { id: "pathology.toast.slidesUnchanged" },
                    { slides: names(slides) },
                  ),
          });
          return focusTargets;
        }
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message: refusalMessage(response, "pathology.toast.slideWorkFailed"),
        });
        return refusalFocusTargets;
      },
    });
  };

  // A bench request change (FR-10): raising one, or completing, cancelling or
  // reopening one, each its own request that reads the case again, as a
  // slide mark does. A refusal keeps the row the change came from as it was
  // typed, and focus goes back to the control that sent it, also when the
  // read after it fails.
  const sendRequestChange = ({ path, body, toastKey, startedBy }) => {
    const refusalTargets = [
      ...(startedBy?.id ? ["#" + startedBy.id] : []),
      ...AFTER_REQUEST_CHANGE,
    ];
    return sendHeld({
      path,
      body,
      startedBy,
      onAnswer: (response, ok) => {
        if (ok) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({ id: toastKey }),
          });
          return AFTER_REQUEST_CHANGE;
        }
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message: refusalMessage(
            response,
            response?.conflict === "request"
              ? "pathology.error.requestConflict"
              : "error.save.msg",
          ),
        });
        return refusalTargets;
      },
    }).then((outcome) => {
      if (!outcome.ok && startedBy) {
        focusFirstOf(refusalTargets, { onlyFrom: [startedBy] });
      }
      return outcome;
    });
  };

  const raiseRequest = (body, startedBy) =>
    sendRequestChange({
      path: "/requests",
      body,
      toastKey: "pathology.toast.requestRaised",
      startedBy,
    });

  const changeRequestStatus = (request, body, startedBy) =>
    sendRequestChange({
      path: "/requests/" + request.id + "/status",
      body,
      toastKey: REQUEST_STATUS_TOASTS[body.status],
      startedBy,
    });

  const openRequestRows = openRequests(caseInfo.requests);
  const openNames = openRequestNames(caseInfo.requests);

  // FR-10.11: a case that would land on the pathologist with requests open
  // asks first, inline, since nothing is destroyed; every other move goes at
  // once. The return row closes, as the status slot holds one row at a time.
  const advanceCase = (event) => {
    primaryButton.current = event?.currentTarget ?? null;
    if (
      caseInfo.nextStage === "READY_PATHOLOGIST" &&
      openRequestRows.length > 0
    ) {
      setReturnOpen(false);
      setReturnReason("");
      setSendConfirmOpen(true);
      focusFirstOf("#" + SEND_ANYWAY_ID);
      return;
    }
    moveCase(
      "advance",
      {},
      "pathology.toast.stageAdvanced",
      event?.currentTarget,
    );
  };

  // The move names the requests the confirmation listed, so one opened
  // behind the page is refused and shown before it is overridden (AC-20).
  const sendAnyway = (event) =>
    moveCase(
      "advance",
      {
        acknowledgedRequestIds: openRequestRows.map((request) => request.id),
      },
      "pathology.toast.sentWithOpenRequests",
      event?.currentTarget,
    );

  const cancelSend = () => {
    setSendConfirmOpen(false);
    const primary = primaryButton.current;
    setTimeout(() => primary?.isConnected && primary.focus());
  };

  // Opens a section whose header is collapsed. CaseSection seeds its open
  // state once, so its own header is pressed.
  const openSection = (sectionId) => {
    const header = document.querySelector(sectionHeader(sectionId));
    if (header?.getAttribute("aria-expanded") === "false") {
      header.click();
    }
  };

  // A request started from a block or slide in Microtomy (FR-10.2): the
  // review opens and its new request takes the kind and the target, with
  // focus on the instruction.
  const startRequest = (kind, targetId) => {
    openSection(REVIEW_SECTION_ID);
    setRequestPrefill((previous) => ({
      kind,
      targetId,
      nonce: (previous?.nonce ?? 0) + 1,
    }));
  };

  const returnCase = (event) =>
    moveCase(
      "return",
      { reason: returnReason.trim() },
      "pathology.toast.stageReturned",
      event?.currentTarget,
    );

  const openReturn = () => {
    setSendConfirmOpen(false);
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
    // The ids lists are sent even when empty, so clearing the last
    // technique or diagnosis is saved as cleared. A case already referred
    // stays referred: the box is shown ticked and cannot be unticked.
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
      reviewNotes: caseInfo.reviewNotes ?? null,
      techniques: (caseInfo.techniques ?? []).map((e) => e.id),
      conclusions: (caseInfo.conclusions ?? []).map((e) => e.id),
      release: caseInfo.release != undefined ? caseInfo.release : false,
      referToImmunoHistoChemistry: Boolean(
        caseInfo.referredToIhc || caseInfo.referToImmunoHistoChemistry,
      ),
    };
    // The run reference is written once per processing entry, so it is sent
    // only while the server says it can still be recorded.
    if (caseInfo.runReferenceEditable) {
      submitValues = { ...submitValues, runReference: caseInfo.runReference };
    }
    if (caseInfo.coverslipRunReferenceEditable) {
      submitValues = {
        ...submitValues,
        coverslipRunReference: caseInfo.coverslipRunReference,
      };
    }
    if (caseInfo.immunoHistoChemistryTestIds) {
      submitValues = {
        ...submitValues,
        immunoHistoChemistryTestIds: caseInfo.immunoHistoChemistryTestIds.map(
          (e) => e.id,
        ),
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
    getFromOpenElisServer(
      "/rest/displayList/PATHOLOGY_TECHNIQUES",
      setTechniques,
    );
    getFromOpenElisServer(
      "/rest/displayList/IMMUNOHISTOCHEMISTRY_MARKERS_TESTS",
      setImmunoHistoChemistryTests,
    );
    getFromOpenElisServer("/rest/users", setTechnicianUsers);
    getFromOpenElisServer("/rest/users/Pathologist", setPathologistUsers);
    reloadCase();

    return () => {
      componentMounted.current = false;
    };
  }, []);

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "pathology.label.dashboard", link: "/PathologyDashboard" },
    { label: "breadcrumb.caseView", link: "" },
  ];

  const knownStage = PATHOLOGY_STAGES.includes(stage);
  // Staining and coverslipping, counted once for the sections and the
  // summary below.
  const slideWork = slideWorkSummaries(caseInfo, configurationProperties);
  const sections = deriveCaseSections({
    intl,
    status: stage,
    isPathologist,
    configurationProperties,
    decalcificationRequired,
    sectionHasRows: (candidate) => sectionHoldsRecords(candidate.id, caseInfo),
    requestCount: (caseInfo.requests ?? []).length,
    readingStage: caseInfo.readingStage,
    findingsOpen: caseInfo.findingsOpen,
  });
  const reviewOpenForRequests = reviewTakesRequests(
    sections.find((section) => section.id === REVIEW_SECTION_ID),
  );

  const sectionBody = (section, readOnly) => {
    switch (section.id) {
      case "pathology-section-case-info":
        return <CaseInformationSection caseInfo={caseInfo} />;
      case "pathology-section-grossing":
        return (
          <GrossingSection
            grossExamHere={grossExamPlace(stage) === "grossing"}
            caseInfo={caseInfo}
            updateCase={updateCase}
            readOnly={readOnly}
            technicianUsers={technicianUsers}
            tissueTypes={tissueTypes}
            decalcificationFlagFrozen={flagFrozen}
            dirty={dirty}
            saving={isSubmitting}
            moving={held}
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
            moving={held}
            embeddingId={embeddingId}
            onEmbed={embedCassette}
          />
        );
      // These two take the section's own read-only state rather than the
      // one every sibling takes, which also covers a request in flight: a
      // read-only table drops its selection and buttons, and focus with
      // them, so a request out holds them (`held`) instead.
      case STAINING_SECTION_ID:
        return (
          <StainingSection
            caseInfo={caseInfo}
            updateCase={updateCase}
            readOnly={section.state === SECTION_STATE.READ_ONLY}
            held={held}
            untracked={Boolean(section.readOnlyBecauseUntracked)}
            stains={stains}
            dirty={dirty}
            saving={isSubmitting}
            slideWork={slideWork}
            onSlideWork={recordSlideWork}
          />
        );
      case COVERSLIPPING_SECTION_ID:
        return (
          <CoverslippingSection
            caseInfo={caseInfo}
            updateCase={updateCase}
            readOnly={section.state === SECTION_STATE.READ_ONLY}
            held={held}
            untracked={Boolean(section.readOnlyBecauseUntracked)}
            dirty={dirty}
            saving={isSubmitting}
            slideWork={slideWork}
            onSlideWork={recordSlideWork}
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
            moving={held}
            onDeactivate={setPendingDeactivation}
            onVerifyLabel={verifySlideLabel}
            onRequestWork={reviewOpenForRequests ? startRequest : undefined}
          />
        );
      // As Staining: the section's own state and the change in flight apart,
      // so a held request keeps its row and what was typed. The fields follow
      // the section's state; the requests table offers New request and its
      // actions only on a review that takes requests, so a signed-out case
      // leaves its requests with no action.
      case REVIEW_SECTION_ID:
        return (
          <ReviewSection
            caseInfo={caseInfo}
            updateCase={updateCase}
            readOnly={section.state === SECTION_STATE.READ_ONLY}
            held={held}
            takesRequests={reviewTakesRequests(section)}
            benchRequests={Boolean(section.benchRequests)}
            pathologistUsers={pathologistUsers}
            stains={stains}
            dirty={dirty}
            saving={isSubmitting}
            requestPrefill={requestPrefill}
            onRaiseRequest={raiseRequest}
            onChangeRequestStatus={changeRequestStatus}
          />
        );
      // A locked Findings renders no body: the accordion still mounts a
      // disabled item's children, and the macroscopic field must exist in
      // one place at a time (AC-23), Grossing's on the bench, none in the
      // queue. Nothing in a locked section can be read or changed anyway.
      case FINDINGS_SECTION_ID:
        if (section.state === SECTION_STATE.DISABLED) {
          return null;
        }
        return (
          <FindingsSection
            caseInfo={caseInfo}
            updateCase={updateCase}
            readOnly={readOnly}
            techniques={techniques}
            immunoHistoChemistryTests={immunoHistoChemistryTests}
            canAddDiagnosis={hasRole(userSessionDetails, Roles.GLOBAL_ADMIN)}
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
  // A slide mark in flight holds both moves as a move would, since the case
  // is read back after it.
  const advanceBlocked = advanceBlockedReason(intl, caseInfo, {
    storedStatus: stage,
    dirty,
    saving: isSubmitting,
    moving: held,
    configurationProperties,
  });
  const returnBlocked = returnBlockedReason({
    storedStatus: stage,
    previousStage,
    dirty,
    saving: isSubmitting,
    moving: held,
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
  // Ready for Pathologist it is Begin review, the pathologist's own, where
  // the laboratory tracks Under Pathologist Review (D1); from there on the
  // pathologist's own actions move the case. A next stage this build has no
  // label for gets no primary either, rather than a button with no words.
  const advanceKey =
    nextStage === "UNDER_REVIEW" && !isPathologist
      ? null
      : advanceLabelKey(nextStage);
  const primary = advanceKey
    ? {
        labelKey: advanceKey,
        disabledReasonKey: advanceBlocked?.key,
        disabledReasonValues: advanceBlocked?.values,
        onClick: advanceCase,
      }
    : undefined;

  // FR-10.11: the case goes to the pathologist with requests open only once
  // the person has seen each one. Inline in the status slot, not a modal, as
  // nothing is destroyed; Send anyway is held as the primary is.
  const sendConfirmShown = sendConfirmOpen && openRequestRows.length > 0;
  const sendConfirmation = (
    <div
      id={SEND_CONFIRM_ID}
      role="group"
      aria-labelledby={SEND_CONFIRM_TEXT_ID}
    >
      <Stack gap={3}>
        <p id={SEND_CONFIRM_TEXT_ID}>
          <FormattedMessage
            id="pathology.banner.openRequests"
            values={{ count: openRequestRows.length }}
          />
        </p>
        <UnorderedList>
          {openRequestRows.map((request) => (
            <ListItem key={request.id}>
              {request.targetName
                ? intl.formatMessage(
                    { id: "pathology.label.requestOnTarget" },
                    {
                      instruction: request.instruction,
                      target: request.targetName,
                    },
                  )
                : request.instruction}
            </ListItem>
          ))}
        </UnorderedList>
        <div className="pathology-case-view__stage-control">
          <Button
            kind="secondary"
            size="sm"
            id={SEND_ANYWAY_ID}
            disabled={Boolean(advanceBlocked)}
            title={blockedText(advanceBlocked) ?? undefined}
            aria-label={intl.formatMessage(
              { id: "pathology.action.sendAnywayOf" },
              { name: openNames.join(", ") },
            )}
            aria-describedby={advanceBlocked ? SEND_ANYWAY_HINT_ID : undefined}
            onClick={sendAnyway}
          >
            <FormattedMessage id="pathology.action.sendAnyway" />
          </Button>
          {/* Held while the move is out, as the return row's Cancel is. */}
          <Button
            kind="ghost"
            size="sm"
            id={SEND_ANYWAY_CANCEL_ID}
            disabled={held}
            onClick={cancelSend}
          >
            <FormattedMessage id="common.cancel" />
          </Button>
          {advanceBlocked && (
            <span id={SEND_ANYWAY_HINT_ID} className="case-view__locked-hint">
              {blockedText(advanceBlocked)}
            </span>
          )}
        </div>
      </Stack>
    </div>
  );

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
      {sendConfirmShown && sendConfirmation}
      {!sendConfirmShown && !returnOpen && (
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
            disabled={held}
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

  const staining = slideWorkStatement(intl, {
    stage: "STAINING",
    summary: slideWork.STAINING,
    status: stage,
    configurationProperties,
  });
  const coverslipping = slideWorkStatement(intl, {
    stage: "COVERSLIPPING",
    summary: slideWork.COVERSLIPPING,
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
      id: "staining",
      labelKey: "pathology.stage.staining",
      value: staining.value,
      title: staining.title,
    },
    {
      id: "coverslipping",
      labelKey: "pathology.stage.coverslipping",
      value: coverslipping.value,
      title: coverslipping.title,
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
              <CaseViewLayout
                rail={
                  <ProgressRail
                    items={deriveRailItems(sections, {
                      intl,
                      status: stage,
                      reportCount: (caseInfo.reports ?? []).length,
                      openRequestNames: openNames,
                      caseInfo,
                      configurationProperties,
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
                    saving={isSubmitting || held}
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
                        configurationProperties,
                      })}
                      defaultOpen={section.openByDefault}
                      openRequest={
                        section.id === FINDINGS_SECTION_ID
                          ? findingsOpenRequest
                          : undefined
                      }
                    >
                      {/* Read-only while a move or a slide mark is out, so
                          nothing typed can be lost to the read-back that
                          follows it. */}
                      {sectionBody(
                        section,
                        section.state === SECTION_STATE.READ_ONLY || held,
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
