import React, { useEffect, useState } from "react";
import { useIntl } from "react-intl";
import {
  Button,
  Select,
  SelectItem,
  NumberInput,
  Stack,
  InlineNotification,
} from "@carbon/react";
export default function ContainerLabelButton({ detail, kind, rowId, service }) {
  const intl = useIntl(),
    t = (id) => intl.formatMessage({ id });
  const [open, setOpen] = useState(false),
    [presets, setPresets] = useState([]),
    [preset, setPreset] = useState(""),
    [quantity, setQuantity] = useState(0),
    [url, setUrl] = useState(null),
    [error, setError] = useState(false);
  useEffect(() => {
    if (!open) return undefined;
    let live = true;
    service
      .getContainerPresets()
      .then((data) => {
        if (live) setPresets(data);
      })
      .catch(() => {
        if (live) setError(true);
      });
    return () => {
      live = false;
    };
  }, [service, open]);
  useEffect(
    () => () => {
      if (url) URL.revokeObjectURL(url);
    },
    [url],
  );
  if (!detail.canWrite) return null;
  return (
    <Stack gap={3}>
      <Button kind="ghost" size="sm" onClick={() => setOpen((value) => !value)}>
        {t("microbiology.labels.print")}
      </Button>
      {open && (
        <>
          <Select
            id={`label-preset-${rowId}`}
            labelText={t("microbiology.labels.preset")}
            value={preset}
            onChange={(e) => {
              setPreset(e.target.value);
              setQuantity(
                presets.find((p) => String(p.id) === e.target.value)
                  ?.defaultQuantity || 0,
              );
            }}
          >
            <SelectItem value="" text={t("common.select")} />
            {presets.map((p) => (
              <SelectItem key={p.id} value={String(p.id)} text={p.name} />
            ))}
          </Select>
          <NumberInput
            id={`label-count-${rowId}`}
            label={t("microbiology.labels.quantity")}
            min={0}
            max={presets.find((p) => String(p.id) === preset)?.maxQuantity || 0}
            value={quantity}
            onChange={(_, { value }) => setQuantity(Number(value))}
          />
          <Button
            disabled={!preset || !quantity || navigator.onLine === false}
            onClick={async () => {
              try {
                const blob = await service.printContainer({
                  kind,
                  caseId: detail.id,
                  rowId,
                  presetId: Number(preset),
                  quantity,
                });
                setUrl(URL.createObjectURL(blob));
                setError(false);
              } catch {
                setError(true);
              }
            }}
          >
            {t("microbiology.labels.prepare")}
          </Button>
          {url && (
            <a href={url} target="_blank" rel="noreferrer">
              {t("microbiology.labels.open")}
            </a>
          )}
          {error && (
            <InlineNotification
              kind="error"
              title={t("microbiology.labels.error")}
              hideCloseButton
            />
          )}
        </>
      )}
    </Stack>
  );
}
