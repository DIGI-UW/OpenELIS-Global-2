import React, { useEffect, useState } from "react";
import {
  Button,
  InlineNotification,
  MultiSelect,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { resolveApiErrorMessage } from "../../../utils/Utils";
import { hintStyle } from "../../eqaCommon";
import {
  attachComments,
  detachComment,
  fetchCommentLibrary,
  fetchCycleComments,
} from "./workbenchApi";

/**
 * Interpretive comments on the printed performance report (FR OGC-934).
 *
 * The picker is the only way in: it offers the pre-approved library and posts
 * ids, so there is no free-text field to type an unreviewed judgement into. An
 * attached comment keeps the wording it was attached with, which is why
 * retiring a library entry never rewrites a report already issued.
 */
const ReportComments = ({ cycleId, onNotice }) => {
  const intl = useIntl();
  const t = (id, defaultMessage, values) =>
    intl.formatMessage({ id, defaultMessage }, values);

  const [library, setLibrary] = useState([]);
  const [attached, setAttached] = useState([]);
  const [selected, setSelected] = useState([]);
  const [busy, setBusy] = useState(null);

  useEffect(() => {
    fetchCommentLibrary(setLibrary);
    fetchCycleComments(cycleId, setAttached);
  }, [cycleId]);

  const attachedIds = attached.map((comment) => comment.libraryEntryId);
  // An attached entry drops out of the picker rather than being offered and
  // then silently ignored by the server's skip.
  const offered = library.filter((entry) => !attachedIds.includes(entry.id));

  const add = () => {
    setBusy("add");
    attachComments(
      cycleId,
      selected.map((entry) => entry.id),
      ({ ok, body }) => {
        setBusy(null);
        if (!ok) {
          onNotice({
            kind: "error",
            text: resolveApiErrorMessage(
              intl,
              body,
              "eqa.report.comments.addFailed",
            ),
          });
          return;
        }
        setSelected([]);
        fetchCycleComments(cycleId, setAttached);
        onNotice({
          kind: "success",
          text: t(
            "eqa.report.comments.added",
            "{count} comment(s) added to the report",
            { count: body?.length ?? 0 },
          ),
        });
      },
    );
  };

  const remove = (commentId) => {
    setBusy(commentId);
    detachComment(cycleId, commentId, ({ ok, body }) => {
      setBusy(null);
      if (!ok) {
        onNotice({
          kind: "error",
          text: resolveApiErrorMessage(
            intl,
            body,
            "eqa.report.comments.removeFailed",
          ),
        });
        return;
      }
      fetchCycleComments(cycleId, setAttached);
    });
  };

  return (
    <>
      <p style={hintStyle}>
        {t(
          "eqa.report.comments.hint",
          "Only pre-approved comments can be printed. Edit the wording under Administration → Dictionary Management.",
        )}
      </p>

      {library.length === 0 ? (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={t(
            "eqa.report.comments.emptyLibrary.title",
            "No approved comments configured",
          )}
          subtitle={t(
            "eqa.report.comments.emptyLibrary.body",
            "Add entries to the EQA Report Comment dictionary category to make them selectable here.",
          )}
        />
      ) : (
        <div style={{ display: "flex", gap: "1rem", alignItems: "flex-end" }}>
          <div style={{ flex: "1 1 30rem" }}>
            <MultiSelect
              id="eqa-report-comment-picker"
              titleText={t("eqa.report.comments.picker", "Approved comments")}
              label={t("eqa.report.comments.pickerLabel", "Select comments")}
              items={offered}
              itemToString={(item) => (item ? item.text : "")}
              // The library is ordered deliberately (verdict first, then
              // corrective actions); Carbon would otherwise sort it by text.
              sortItems={(items) => items}
              selectedItems={selected}
              onChange={({ selectedItems }) => setSelected(selectedItems || [])}
            />
          </div>
          <Button
            kind="tertiary"
            disabled={selected.length === 0 || busy === "add"}
            onClick={add}
          >
            {t("eqa.report.comments.add", "Add to report")}
          </Button>
        </div>
      )}

      <Table size="sm" style={{ marginTop: "1rem" }}>
        <TableHead>
          <TableRow>
            <TableHeader>
              {t("eqa.report.comments.comment", "Comment")}
            </TableHeader>
            <TableHeader>
              {t("eqa.report.comments.attribution", "Added by")}
            </TableHeader>
            <TableHeader />
          </TableRow>
        </TableHead>
        <TableBody>
          {attached.length === 0 ? (
            <TableRow>
              <TableCell colSpan={3}>
                {t(
                  "eqa.report.comments.none",
                  "No comments on this cycle's report yet.",
                )}
              </TableCell>
            </TableRow>
          ) : (
            attached.map((comment) => (
              <TableRow key={comment.id}>
                <TableCell>{comment.text}</TableCell>
                <TableCell>
                  {comment.attachedBy}
                  {comment.attachedAt ? ` — ${comment.attachedAt}` : ""}
                </TableCell>
                <TableCell>
                  <Button
                    kind="ghost"
                    size="sm"
                    disabled={busy === comment.id}
                    onClick={() => remove(comment.id)}
                  >
                    {t("eqa.report.comments.remove", "Remove")}
                  </Button>
                </TableCell>
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>
    </>
  );
};

export default ReportComments;
