import React, { useState } from "react";
import {
  Button,
  Checkbox,
  Modal,
  Stack,
  Tag,
  TextArea,
  InlineNotification,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { labTimestamp } from "./MicrobiologyTime";
import { MODAL_FIRST_FIELD, ReturnFocus } from "./returnFocus";
import OfflineNotice, { useOnline } from "./OfflineNotice";

export default function CaseAttachmentNotes({
  detail,
  service,
  timeline = [],
  targetType,
  targetId,
  onSaved,
}) {
  const intl = useIntl(),
    t = (id, values) => intl.formatMessage({ id }, values);
  const online = useOnline();
  const [open, setOpen] = useState(false),
    [text, setText] = useState(""),
    [external, setExternal] = useState(false),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(false);
  const notes = timeline.filter(
    (n) =>
      n.activityType === "MANUAL_NOTE" &&
      n.targetType === targetType &&
      n.targetId === targetId,
  );
  const save = async () => {
    setBusy(true);
    setError(false);
    try {
      await service.addNote(detail.id, text, external, targetType, targetId);
      setText("");
      await onSaved?.();
    } catch {
      setError(true);
    } finally {
      setBusy(false);
    }
  };
  return (
    <>
      <Button
        kind="ghost"
        size="sm"
        onClick={() => {
          setOpen(true);
          setError(false);
        }}
      >
        {t("microbiology.testing.notesCount", { count: notes.length })}
      </Button>
      {open && <ReturnFocus />}
      {open && (
        <Modal
          open
          passiveModal={!detail.canWrite}
          selectorPrimaryFocus={MODAL_FIRST_FIELD}
          modalHeading={t("microbiology.testing.notes")}
          closeButtonLabel={t("label.button.close")}
          primaryButtonText={t("microbiology.testing.saveNote")}
          secondaryButtonText={t("label.button.close")}
          primaryButtonDisabled={busy || !text.trim() || !online}
          onRequestSubmit={save}
          onRequestClose={() => setOpen(false)}
          onSecondarySubmit={() => setOpen(false)}
        >
          <Stack gap={4}>
            <OfflineNotice />
            {notes.map((note) => (
              <div key={note.id}>
                <Tag>
                  {t(
                    note.structuredData === "SEND_RESULT"
                      ? "microbiology.m6.sendWithResult"
                      : "microbiology.testing.internalNote",
                  )}
                </Tag>
                <p>{note.note}</p>
                <p>
                  {note.performedByDisplay} ·{" "}
                  {labTimestamp(intl, note.occurredAt)}
                </p>
              </div>
            ))}
            {detail.canWrite && (
              <>
                <TextArea
                  id={`note-${targetType}-${targetId}`}
                  labelText={t("microbiology.testing.newNote")}
                  value={text}
                  maxLength={4000}
                  disabled={busy}
                  onChange={(e) => setText(e.target.value)}
                />
                <Checkbox
                  id={`external-${targetType}-${targetId}`}
                  labelText={t("microbiology.m6.sendWithResult")}
                  checked={external}
                  onChange={(_, { checked }) => setExternal(checked)}
                />
              </>
            )}
            {error && (
              <InlineNotification
                kind="error"
                hideCloseButton
                title={t("microbiology.testing.saveError")}
              />
            )}
          </Stack>
        </Modal>
      )}
    </>
  );
}
