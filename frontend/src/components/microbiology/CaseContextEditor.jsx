import React, { useState, useRef, useEffect } from "react";
import { useIntl } from "react-intl";
import {
  Button,
  Grid,
  Column,
  InlineNotification,
  NumberInput,
  Select,
  SelectItem,
  Stack,
  Tile,
} from "@carbon/react";

export default function CaseContextEditor({ detail, service, onSaved }) {
  const mounted = useRef(true);
  useEffect(
    () => () => {
      mounted.current = false;
    },
    [],
  );
  const intl = useIntl();
  const t = (id, values) => intl.formatMessage({ id }, values);
  const [programId, setProgramId] = useState(detail.programId || "");
  const [purpose, setPurpose] = useState(detail.culturePurpose || "");
  const [replicates, setReplicates] = useState(detail.replicateCount ?? "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(false);
  const environmental = detail.domain === "ENVIRONMENTAL";
  const invalid =
    environmental &&
    replicates !== "" &&
    (!Number.isInteger(Number(replicates)) ||
      Number(replicates) < 1 ||
      Number(replicates) > 999);
  const save = async () => {
    setSaving(true);
    setError(false);
    try {
      const updated = await service.saveContext(detail.id, {
        culturePurpose: purpose,
        programId,
        replicateCount:
          environmental && replicates !== "" ? Number(replicates) : null,
      });
      if (mounted.current) onSaved(updated);
    } catch {
      if (mounted.current) setError(true);
    } finally {
      if (mounted.current) setSaving(false);
    }
  };
  // i18n-keys: microbiology.case.purpose.*
  return (
    <Tile>
      <Stack gap={4}>
        <Grid>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="case-program"
              labelText={t("microbiology.case.program")}
              value={programId}
              disabled={!detail.canWrite || saving}
              onChange={(e) => setProgramId(e.target.value)}
            >
              <SelectItem value="" text={t("not.available")} />
              {(detail.programs || []).map((p) => (
                <SelectItem key={p.id} value={p.id} text={p.value} />
              ))}
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="case-purpose"
              labelText={t("microbiology.case.culturePurpose")}
              value={purpose}
              disabled={!detail.canWrite || saving}
              onChange={(e) => setPurpose(e.target.value)}
            >
              <SelectItem value="" text={t("microbiology.culture.choose")} />
              {(detail.purposeOptions || []).map((value) => (
                <SelectItem
                  key={value}
                  value={value}
                  text={t(`microbiology.case.purpose.${value}`)}
                />
              ))}
            </Select>
          </Column>
        </Grid>
        {environmental && (
          <NumberInput
            id="case-replicates"
            allowEmpty
            min={1}
            max={999}
            value={replicates}
            label={t("microbiology.case.replicates")}
            helperText={t("microbiology.case.orderScope", {
              count: detail.orderCaseCount,
            })}
            invalid={invalid}
            invalidText={t("microbiology.case.replicatesInvalid")}
            disabled={!detail.canWrite || saving}
            onChange={(event, data) =>
              setReplicates(data?.value ?? event.target.value)
            }
          />
        )}
        {detail.canWrite && (
          <Button disabled={!purpose || invalid || saving} onClick={save}>
            {t("button.save")}
          </Button>
        )}
        {error && (
          <InlineNotification
            kind="error"
            title={t("microbiology.case.contextSaveError")}
            hideCloseButton
          />
        )}
      </Stack>
    </Tile>
  );
}
