import React, { useEffect, useState } from "react";
import {
  Button,
  Form,
  InlineNotification,
  Modal,
  Select,
  SelectItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { getDstBindings, saveDstBinding } from "./api";

export default function MolecularDstPage() {
  const intl = useIntl();
  const t = (key) => intl.formatMessage({ id: `microbiology.m6.${key}` });
  const [catalog, setCatalog] = useState(null);
  const [draft, setDraft] = useState(null);
  const [error, setError] = useState(false);
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    const controller = new AbortController();
    getDstBindings(controller.signal)
      .then(setCatalog)
      .catch((e) => {
        if (e.name !== "AbortError") setError(true);
      });
    return () => controller.abort();
  }, []);
  const test = catalog?.tests.find((t) => t.id === draft?.testId);
  const component = test?.components.find((c) => c.id === draft?.componentId);
  const save = async () => {
    setBusy(true);
    setError(false);
    try {
      await saveDstBinding(draft);
      setCatalog(await getDstBindings());
      setDraft(null);
    } catch {
      setError(true);
    } finally {
      setBusy(false);
    }
  };
  const selector = (field, label, options, reset = {}) => (
    <Select
      id={`dst-binding-${field}`}
      labelText={t(label)}
      value={draft?.[field] || ""}
      onChange={(e) =>
        setDraft({ ...draft, ...reset, [field]: e.target.value })
      }
      disabled={busy}
    >
      <SelectItem value="" text={t("choose")} />
      {(options || []).map((o) => (
        <SelectItem key={o.id} value={o.id} text={o.label} />
      ))}
    </Select>
  );
  return (
    <Stack gap={5}>
      <p>{t("bindingsHelp")}</p>
      {error && (
        <InlineNotification
          kind="error"
          title={t("bindingsError")}
          hideCloseButton
        />
      )}
      <Button disabled={!catalog} onClick={() => setDraft({})}>
        {t("addBinding")}
      </Button>
      <Table aria-label={t("bindings")}>
        <TableHead>
          <TableRow>
            {[
              "bindingTest",
              "bindingComponent",
              "agent",
              "susceptibleValue",
              "resistantValue",
              "actions",
            ].map((h) => (
              <TableHeader key={h}>{t(h)}</TableHeader>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {catalog?.bindings.map((b) => {
            const test = catalog.tests.find((t) => t.id === b.testId);
            const component = test?.components.find(
              (c) => c.id === b.componentId,
            );
            const option = (value) =>
              component?.options.find((o) => o.id === value)?.label ||
              t("unavailable");
            return (
              <TableRow key={b.id}>
                <TableCell>{test?.label || t("unavailable")}</TableCell>
                <TableCell>{component?.label || t("unavailable")}</TableCell>
                <TableCell>
                  {catalog.antibiotics.find((a) => a.id === b.antibioticId)
                    ?.label || t("unavailable")}
                </TableCell>
                <TableCell>{option(b.susceptibleValue)}</TableCell>
                <TableCell>{option(b.resistantValue)}</TableCell>
                <TableCell>
                  <Button kind="ghost" onClick={() => setDraft({ ...b })}>
                    {t("editBinding")}
                  </Button>
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
      {draft && (
        <Modal
          open
          modalHeading={t("bindings")}
          primaryButtonText={t("save")}
          secondaryButtonText={t("cancel")}
          onRequestClose={() => !busy && setDraft(null)}
          onRequestSubmit={save}
          primaryButtonDisabled={
            busy ||
            !draft.testId ||
            !draft.componentId ||
            !draft.antibioticId ||
            !draft.susceptibleValue ||
            !draft.resistantValue ||
            draft.susceptibleValue === draft.resistantValue
          }
        >
          <Form>
            <Stack gap={5}>
              {selector("testId", "bindingTest", catalog.tests, {
                componentId: "",
                susceptibleValue: "",
                resistantValue: "",
              })}
              {selector("componentId", "bindingComponent", test?.components, {
                susceptibleValue: "",
                resistantValue: "",
              })}
              {selector("antibioticId", "agent", catalog.antibiotics)}
              {selector(
                "susceptibleValue",
                "susceptibleValue",
                component?.options,
              )}
              {selector("resistantValue", "resistantValue", component?.options)}
            </Stack>
          </Form>
        </Modal>
      )}
    </Stack>
  );
}
