import React, { useEffect, useState, useRef } from "react";
import { useIntl } from "react-intl";
import { useParams, useLocation, Link as RouterLink } from "react-router-dom";
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
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
} from "@carbon/react";
import "./CaseWorkspace.scss";
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
  const params = useParams();
  const location = useLocation();
  const id = providedId || params.caseId;
  const t = (id, values) => intl.formatMessage({ id }, values);
  const currentCase = useRef(id);
  currentCase.current = id;
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [destination, setDestination] = useState("");
  const [saving, setSaving] = useState(false);
  const [transferError, setTransferError] = useState(false);
  useEffect(() => {
    let live = true;
    setDetail(null);
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
  const transfer = () => {
    const requestedCase = id;
    setSaving(true);
    setTransferError(false);
    service
      .transferCase(id, destination)
      .then((d) => {
        if (currentCase.current !== requestedCase) return;
        setDetail(d);
        setDestination("");
      })
      .catch(() => {
        if (currentCase.current === requestedCase) setTransferError(true);
      })
      .finally(() => {
        if (currentCase.current === requestedCase) setSaving(false);
      });
  };
  // i18n-keys: microbiology.case.status.*
  return (
    <Grid fullWidth className="microbiology-case-workspace">
      <Column lg={16} md={8} sm={4}>
        <Stack gap={5}>
          <RouterLink to={`${MICROBIOLOGY_WORKLIST_PATH}${location.search}`}>
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
              <Grid>
                <Column lg={8} md={4} sm={4}>
                  <Tile>
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
                  </Tile>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Tile>
                    <h2>{t("microbiology.case.labUnit")}</h2>
                    <p>{detail.labUnit}</p>
                    <p>{detail.specimenType}</p>
                    <p>{t("microbiology.case.readOnly")}</p>
                  </Tile>
                </Column>
              </Grid>
              <h2>{t("common.samples")}</h2>
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableHeader>{t("common.labNumber")}</TableHeader>
                      <TableHeader>
                        {t("microbiology.worklist.column.specimen")}
                      </TableHeader>
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
                        <TableCell>{t("common.collected")}</TableCell>
                        <TableCell>
                          {s.collectionDate
                            ? intl.formatDate(new Date(s.collectionDate))
                            : t("not.available")}
                        </TableCell>
                      </TableRow>
                    ))}
                    {(detail.pendingSamples || []).map((s) => (
                      <TableRow key={`pending-${s.id}`}>
                        <TableCell>{t("not.available")}</TableCell>
                        <TableCell>{s.specimenType}</TableCell>
                        <TableCell>
                          {t("microbiology.case.awaitingCollection")}
                        </TableCell>
                        <TableCell>{t("not.available")}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
              <h2>{t("microbiology.case.relatedCases")}</h2>
              {(detail.relatedCases || []).length === 0 ? (
                <p>{t("microbiology.case.noRelatedCases")}</p>
              ) : (
                <ul>
                  {detail.relatedCases.map((c) => (
                    <li key={c.id}>
                      <RouterLink
                        to={`${MICROBIOLOGY_CASE_PATH}/${encodeURIComponent(c.id)}${location.search}`}
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
