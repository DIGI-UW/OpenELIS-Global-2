import { labTimestamp } from "./MicrobiologyTime";
import {
  MICROBIOLOGY_CASE_READY_MARK,
  markMicrobiologyReady,
} from "./MicrobiologyPerformance";
import React, { useContext, useEffect, useState, useRef } from "react";
import { useIntl } from "react-intl";
import {
  useParams,
  useLocation,
  useHistory,
  Link as RouterLink,
} from "react-router-dom";
import {
  Stack,
  Grid,
  Column,
  Tile,
  Tag,
  InlineLoading,
  InlineNotification,
  Select,
  SelectItem,
  Button,
  Accordion,
  AccordionItem,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
} from "@carbon/react";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { hasRole, Roles } from "../utils/Utils";
import "./CaseWorkspace.scss";
import "./MicrobiologyCaseView.css";
import CaseQuestionnaire from "./CaseQuestionnaire";
import OrderDetailPanel from "./OrderDetailPanel";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";
import CaseContextEditor from "./CaseContextEditor";
import CaseTestingWorkspace from "./CaseTestingWorkspace";
import serviceDefault from "./CaseWorkspaceService";
import {
  MICROBIOLOGY_CASE_PATH,
  MICROBIOLOGY_WORKLIST_PATH,
} from "./MicrobiologyRoutes";

export default function CaseViewShell({
  service = serviceDefault,
  caseId: providedId,
}) {
  const intl = useIntl();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const canCorrectSamples = hasRole(userSessionDetails, Roles.RECEPTION);
  const [infoOpen, setInfoOpen] = useState(true);
  const params = useParams();
  const location = useLocation();
  const history = useHistory();
  const selectScope = (scope, parameters = {}) => {
    const next = new URLSearchParams(location.search);
    [
      "view",
      "section",
      "action",
      "row",
      "astView",
      "astRunId",
      "astIsolateId",
      "targetType",
      "targetId",
    ].forEach((key) => next.delete(key));
    next.set("scope", scope);
    Object.entries(parameters).forEach(([key, value]) => next.set(key, value));
    history.push({ pathname: location.pathname, search: next.toString() });
  };
  const worklistSearch = new URLSearchParams(location.search);
  [
    "view",
    "section",
    "scope",
    "row",
    "action",
    "targetType",
    "targetId",
    "astRunId",
    "astIsolateId",
    "astView",
  ].forEach((key) => worklistSearch.delete(key));
  const id = providedId || params.caseId;
  const t = (id, values) => intl.formatMessage({ id }, values);
  const currentCase = useRef(id);
  const visitGeneration = useRef(0);
  if (currentCase.current !== id) visitGeneration.current += 1;
  currentCase.current = id;
  useEffect(
    () => () => {
      visitGeneration.current += 1;
      currentCase.current = null;
    },
    [],
  );
  const [detail, setDetail] = useState(null);
  const [incomingCount, setIncomingCount] = useState(0);
  const [incomingError, setIncomingError] = useState(false);
  useEffect(() => {
    let live = true;
    if (!detail || !service.getIncoming) return undefined;
    service
      .getIncoming(detail.id)
      .then((rows) => {
        if (live) {
          setIncomingCount(
            rows.filter((row) => row.state === "WAITING").length,
          );
          setIncomingError(false);
        }
      })
      .catch(() => {
        if (live) setIncomingError(true);
      });
    return () => {
      live = false;
    };
  }, [detail, service]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [destination, setDestination] = useState("");
  const [saving, setSaving] = useState(false);
  const [transferError, setTransferError] = useState(false);
  useEffect(() => {
    let live = true;
    setDetail(null);
    setIncomingCount(0);
    setIncomingError(false);
    setLoading(true);
    setError(false);
    setDestination("");
    setSaving(false);
    setTransferError(false);
    service
      .getCase(id)
      .then((d) => {
        if (live) setDetail(d);
      })
      .catch(() => {
        if (live) setError(true);
      })
      .finally(() => {
        if (live) setLoading(false);
      });
    return () => {
      live = false;
    };
  }, [id, service]);
  useEffect(() => {
    if (!loading && !error && detail?.id === id) {
      markMicrobiologyReady(MICROBIOLOGY_CASE_READY_MARK);
    }
  }, [loading, error, detail, id]);
  const transfer = () => {
    const requestedVisit = visitGeneration.current;
    setSaving(true);
    setTransferError(false);
    service
      .transferCase(id, destination)
      .then((d) => {
        if (visitGeneration.current !== requestedVisit) return;
        setDetail(d);
        setDestination("");
      })
      .catch(() => {
        if (visitGeneration.current === requestedVisit) setTransferError(true);
      })
      .finally(() => {
        if (visitGeneration.current === requestedVisit) setSaving(false);
      });
  };
  // i18n-keys: microbiology.case.status.*
  return (
    <Grid
      fullWidth
      className="microbiology-case-workspace"
      data-testid="microbiology-case-view"
    >
      <Column lg={16} md={8} sm={4}>
        <Stack gap={5}>
          <RouterLink
            to={`${MICROBIOLOGY_WORKLIST_PATH}${worklistSearch.toString() ? `?${worklistSearch}` : ""}`}
          >
            {t("microbiology.worklist.title")}
          </RouterLink>
          {loading && <InlineLoading description={t("common.loading")} />}
          {error && (
            <InlineNotification
              kind="error"
              title={t("microbiology.case.loadError")}
              hideCloseButton
            />
          )}
          {detail && (
            <>
              <h1>
                {t("microbiology.case.heading", {
                  accession: detail.accessionNumber || detail.id,
                })}
              </h1>
              <Tag type="blue">
                {t(`microbiology.case.status.${detail.status}`)}
              </Tag>
              <p>
                {t("microbiology.case.workStage")}:{" "}
                {formatMicrobiologyEnum(detail.stage, intl)} ·{" "}
                {t("microbiology.case.releaseState")}:{" "}
                {formatMicrobiologyEnum(detail.finalReleaseState, intl)} ·{" "}
                {t("microbiology.case.priority")}:{" "}
                {formatMicrobiologyEnum(detail.priority, intl)}
              </p>
              {incomingCount > 0 && (
                <Tile>
                  <p role="status">
                    {t("microbiology.incoming.waitingCount", {
                      count: incomingCount,
                    })}
                  </p>
                  <Button kind="ghost" onClick={() => selectScope("INCOMING")}>
                    {t("microbiology.incoming.title")}
                  </Button>
                </Tile>
              )}
              {incomingError && (
                <InlineNotification
                  kind="warning"
                  hideCloseButton
                  title={t("microbiology.incoming.loadError")}
                />
              )}
              <Grid>
                <Column lg={8} md={4} sm={4}>
                  <Tile>
                    {detail.domain === "ENVIRONMENTAL" ? (
                      <>
                        <Tag type="teal">
                          {t("microbiology.case.environmental")}
                        </Tag>
                        <p>{t("microbiology.case.patientRulesSkipped")}</p>
                        <h2>{t("microbiology.case.site")}</h2>
                        <p>
                          {detail.siteName} · {detail.siteCode}
                        </p>
                        <p>{detail.siteLocation}</p>
                        {detail.latitude && detail.longitude && (
                          <p>
                            {t("microbiology.case.coordinates", {
                              latitude: detail.latitude,
                              longitude: detail.longitude,
                            })}
                          </p>
                        )}
                        <p>
                          {t("microbiology.case.requester")}:{" "}
                          {detail.requesterName || t("not.available")}{" "}
                          {detail.requesterPhone}
                        </p>
                      </>
                    ) : (
                      <>
                        <h2>{t("common.patient")}</h2>
                        <p>{detail.patientName || t("not.available")}</p>
                        <p>
                          {t("microbiology.case.birthDate")}:{" "}
                          {detail.birthDate
                            ? intl.formatDate(new Date(detail.birthDate))
                            : t("not.available")}
                        </p>
                        <p>
                          {t("microbiology.case.sex")}:{" "}
                          {detail.gender || t("not.available")}
                        </p>
                      </>
                    )}
                  </Tile>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Tile>
                    <h2>{t("microbiology.case.labUnit")}</h2>
                    <p>{detail.labUnit}</p>
                    {detail.reportingTrack && (
                      <Tag type="teal">{detail.reportingTrack}</Tag>
                    )}
                    <p>{detail.specimenType}</p>
                    {detail.referred && (
                      <Tag type="purple">
                        {t("label.results.history.event.REFERRAL")}
                      </Tag>
                    )}
                    {!detail.canWrite && (
                      <p>
                        {t(
                          detail.finalReleaseState === "FINAL_RELEASED"
                            ? "microbiology.case.finalLocked.title"
                            : "microbiology.case.readOnly",
                        )}
                      </p>
                    )}
                  </Tile>
                </Column>
              </Grid>
              <div>
                {detail.canWrite && (
                  <>
                    <Button
                      kind="tertiary"
                      onClick={() =>
                        selectScope("CRITICAL", {
                          action: "log-critical",
                          targetType: "CASE",
                          targetId: id,
                        })
                      }
                    >
                      {t("microbiology.critical.title")}
                    </Button>
                    <Button
                      kind="tertiary"
                      onClick={() => selectScope("NONCONFORMANCE")}
                    >
                      {t("microbiology.nce.sectionTitle")}
                    </Button>
                    <Button
                      kind="tertiary"
                      onClick={() =>
                        selectScope("NONCONFORMANCE", { action: "mark-lost" })
                      }
                    >
                      {t("microbiology.nce.markLost")}
                    </Button>
                  </>
                )}
                {detail.canAmend &&
                  detail.finalReleaseState === "FINAL_RELEASED" && (
                    <Button
                      kind="tertiary"
                      onClick={() => selectScope("AMENDMENT")}
                    >
                      {t("microbiology.amendment.open")}
                    </Button>
                  )}
              </div>
              <section
                id="case-information"
                onFocus={(event) => {
                  if (event.target === event.currentTarget) setInfoOpen(true);
                }}
                tabIndex={-1}
                aria-label={t("microbiology.progress.caseInfo")}
              >
                <Accordion>
                  <AccordionItem
                    title={t("microbiology.progress.caseInfo")}
                    open={infoOpen}
                    onHeadingClick={({ isOpen }) => setInfoOpen(isOpen)}
                  >
                    {(detail.culturePurpose || detail.canWrite) && (
                      <CaseContextEditor
                        key={`${id}-${detail.culturePurpose}-${detail.replicateCount}-${detail.programId}`}
                        detail={detail}
                        service={service}
                        onSaved={(next) => {
                          if (currentCase.current === id) setDetail(next);
                        }}
                      />
                    )}
                    {detail.domain !== "ENVIRONMENTAL" &&
                      detail.orderDetail && (
                        <OrderDetailPanel
                          key={`${id}-${JSON.stringify(detail.orderDetail)}`}
                          caseId={id}
                          orderDetail={detail.orderDetail}
                          service={service}
                          showCulturePurpose={false}
                          requiredFields={detail.requiredClinicalFields}
                          isReadOnly={!detail.canWrite}
                          onSaved={() =>
                            service
                              .getCase(id)
                              .then((next) => {
                                if (currentCase.current === id) setDetail(next);
                              })
                              .catch(() => setError(true))
                          }
                        />
                      )}
                    {detail.domain !== "ENVIRONMENTAL" &&
                      detail.programId &&
                      service.getQuestionnaire && (
                        <CaseQuestionnaire
                          key={`${id}-${detail.programId}`}
                          detail={detail}
                          service={service}
                          onSaved={() =>
                            service.getCase(id).then((next) => {
                              if (currentCase.current === id) setDetail(next);
                            })
                          }
                        />
                      )}
                  </AccordionItem>
                </Accordion>
              </section>
              <h2>{t("common.samples")}</h2>
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableHeader>{t("common.labNumber")}</TableHeader>
                      <TableHeader>
                        {t("microbiology.worklist.column.specimen")}
                      </TableHeader>
                      {detail.domain === "ENVIRONMENTAL" && (
                        <TableHeader>
                          {t("microbiology.case.samplingPoint")}
                        </TableHeader>
                      )}
                      <TableHeader>
                        {t("microbiology.sets.bodySite")}
                      </TableHeader>
                      <TableHeader>
                        {t("microbiology.sets.container")}
                      </TableHeader>
                      <TableHeader>{t("collector")}</TableHeader>
                      <TableHeader>{t("common.status")}</TableHeader>
                      <TableHeader>
                        {t("microbiology.case.collectionDate")}
                      </TableHeader>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {(detail.samples || []).map((s) => (
                      <TableRow key={s.id}>
                        <TableCell>{s.label}</TableCell>
                        <TableCell>{s.specimenType}</TableCell>
                        {detail.domain === "ENVIRONMENTAL" && (
                          <TableCell>
                            {s.samplingPoint || t("not.available")}
                          </TableCell>
                        )}
                        <TableCell>
                          {s.bodySite || t("not.available")}
                        </TableCell>
                        <TableCell>
                          {s.containerType || t("not.available")}
                        </TableCell>
                        <TableCell>
                          {s.collector || t("not.available")}
                        </TableCell>
                        <TableCell>{t("common.collected")}</TableCell>
                        <TableCell>
                          {s.collectionDate
                            ? labTimestamp(intl, s.collectionDate)
                            : t("not.available")}
                        </TableCell>
                      </TableRow>
                    ))}
                    {(detail.pendingSamples || []).map((s) => (
                      <TableRow key={`pending-${s.id}`}>
                        <TableCell>{t("not.available")}</TableCell>
                        <TableCell>{s.specimenType}</TableCell>
                        {detail.domain === "ENVIRONMENTAL" && (
                          <TableCell>{t("not.available")}</TableCell>
                        )}
                        <TableCell colSpan={3}>{t("not.available")}</TableCell>
                        <TableCell>
                          {t("microbiology.case.awaitingCollection")}
                        </TableCell>
                        <TableCell>{t("not.available")}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
              {detail.canWrite && canCorrectSamples && detail.orderId && (
                <RouterLink
                  to={`/ModifyOrder?accessionNumber=${encodeURIComponent(detail.accessionNumber)}`}
                >
                  {t("microbiology.case.correctSample")}
                </RouterLink>
              )}
              <CaseTestingWorkspace
                onScopeChange={selectScope}
                scanSearch={location.search}
                key={id}
                detail={detail}
                service={service}
                onCaseChanged={() =>
                  service
                    .getCase(id)
                    .then((next) => {
                      if (currentCase.current === id) setDetail(next);
                    })
                    .catch(() => setError(true))
                }
              />
              <h2>{t("microbiology.case.relatedCases")}</h2>
              {(detail.relatedCases || []).length === 0 ? (
                <p>{t("microbiology.case.noRelatedCases")}</p>
              ) : (
                <ul>
                  {detail.relatedCases.map((c) => (
                    <li key={c.id}>
                      <RouterLink
                        to={`${MICROBIOLOGY_CASE_PATH}/${encodeURIComponent(c.id)}${worklistSearch.toString() ? `?${worklistSearch}` : ""}`}
                      >
                        {c.accessionNumber} · {c.labUnit} · {c.specimenType}
                      </RouterLink>
                    </li>
                  ))}
                </ul>
              )}
              {detail.canWrite && detail.transferLabUnits?.length > 0 && (
                <Tile>
                  <h2>{t("microbiology.case.transfer")}</h2>
                  <Select
                    id="transfer-unit"
                    labelText={t("microbiology.case.destination")}
                    value={destination}
                    onChange={(e) => setDestination(e.target.value)}
                    disabled={saving}
                  >
                    <SelectItem
                      value=""
                      text={t("microbiology.case.chooseUnit")}
                    />
                    {detail.transferLabUnits.map((u) => (
                      <SelectItem key={u.id} value={u.id} text={u.value} />
                    ))}
                  </Select>
                  <Button disabled={!destination || saving} onClick={transfer}>
                    {t("microbiology.case.transfer")}
                  </Button>
                  {saving && (
                    <InlineLoading description={t("common.loading")} />
                  )}
                </Tile>
              )}
              {transferError && (
                <InlineNotification
                  kind="error"
                  title={t("microbiology.case.transferError")}
                  hideCloseButton
                />
              )}
            </>
          )}
        </Stack>
      </Column>
    </Grid>
  );
}
