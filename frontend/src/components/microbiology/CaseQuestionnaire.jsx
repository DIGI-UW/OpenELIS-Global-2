import React, { useEffect, useState } from "react";
import {
  Button,
  InlineLoading,
  InlineNotification,
  MultiSelect,
  Select,
  SelectItem,
  Stack,
  TextArea,
  TextInput,
  Tile,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { labDateTimeInput, labInputInstant } from "./MicrobiologyTime";

const valueOf = (answer) =>
  Object.entries(answer || {}).find(([key]) => key.startsWith("value"))?.[1];
const equal = (a, b) =>
  typeof a === "object" || typeof b === "object"
    ? JSON.stringify(a) === JSON.stringify(b)
    : a === b;
export function questionEnabled(item, answers) {
  if (!item.enableWhen?.length) return true;
  const matches = item.enableWhen.map((condition) => {
    const values = (answers[condition.question] || []).map(valueOf);
    const expected = Object.entries(condition).find(([key]) =>
      key.startsWith("answer"),
    )?.[1];
    if (condition.operator === "exists")
      return Boolean(values.length) === expected;
    return values.some(
      (value) =>
        ({
          "=": equal(value, expected),
          "!=": !equal(value, expected),
          ">": value > expected,
          "<": value < expected,
          ">=": value >= expected,
          "<=": value <= expected,
        })[condition.operator],
    );
  });
  return item.enableBehavior === "any"
    ? matches.some(Boolean)
    : matches.every(Boolean);
}
const answersFrom = (items, answers = {}) => {
  (items || []).forEach((item) => {
    answers[item.linkId] = item.answer || [];
    answersFrom(item.item, answers);
    (item.answer || []).forEach((answer) => answersFrom(answer.item, answers));
  });
  return answers;
};
export default function CaseQuestionnaire({ detail, service, onSaved }) {
  const intl = useIntl(),
    t = (id) => intl.formatMessage({ id });
  const [data, setData] = useState(null),
    [answers, setAnswers] = useState({}),
    [error, setError] = useState(false),
    [saving, setSaving] = useState(false);
  useEffect(() => {
    let live = true;
    setData(null);
    setError(false);
    service
      .getQuestionnaire(detail.id)
      .then((result) => {
        if (!live) return;
        const questionnaire = result.questionnaireJson
          ? JSON.parse(result.questionnaireJson)
          : null;
        const response = result.responseJson
          ? JSON.parse(result.responseJson)
          : {};
        setData({ ...result, questionnaire, response });
        setAnswers(answersFrom(response.item));
      })
      .catch(() => {
        if (live) setError(true);
      });
    return () => {
      live = false;
    };
  }, [detail.id, detail.programId, service]);
  const change = (id, next) =>
    setAnswers((previous) => ({ ...previous, [id]: next }));
  const save = async () => {
    setSaving(true);
    setError(false);
    const items = (questions) =>
      questions
        .filter((q) => questionEnabled(q, answers))
        .map((q) => ({
          linkId: q.linkId,
          text: q.text,
          ...(q.type === "group"
            ? { item: items(q.item || []) }
            : {
                answer: (answers[q.linkId] || [])
                  .filter(
                    (answer) =>
                      q.type !== "open-choice" ||
                      typeof answer.valueString !== "string" ||
                      answer.valueString.trim() !== "",
                  )
                  .map((answer) => ({
                    ...answer,
                    ...(q.item?.length ? { item: items(q.item) } : {}),
                  })),
              }),
        }));
    try {
      const saved = await service.saveQuestionnaire(detail.id, {
        programId: data.programId,
        version: data.version,
        responseJson: JSON.stringify({
          resourceType: "QuestionnaireResponse",
          status: "in-progress",
          item: items(data.questionnaire.item || []),
        }),
      });
      setData((previous) => ({ ...previous, version: saved.version }));
      onSaved?.();
    } catch {
      setError(true);
    } finally {
      setSaving(false);
    }
  };
  const renderItem = (item) => {
    if (!questionEnabled(item, answers)) return null;
    const id = `case-question-${item.linkId}`,
      value = valueOf(answers[item.linkId]?.[0]) ?? "";
    const label = item.required
      ? intl.formatMessage(
          { id: "microbiology.case.requiredQuestion" },
          { label: item.text || item.linkId },
        )
      : item.text || item.linkId;
    const common = {
      id,
      labelText: label,
      disabled: !detail.canWrite || item.readOnly || saving,
    };
    const options = (item.answerOption || []).map((option, index) => ({
      id: String(index),
      label: option.valueCoding?.display || String(valueOf(option)),
      answer: Object.fromEntries(
        Object.entries(option).filter(([key]) => key.startsWith("value")),
      ),
    }));
    const customAnswers = (answers[item.linkId] || []).filter(
      (answer) =>
        typeof answer.valueString === "string" &&
        !options.some((option) =>
          equal(valueOf(option.answer), valueOf(answer)),
        ),
    );
    let field;
    if (["group", "display"].includes(item.type)) field = <h4>{label}</h4>;
    else if (
      item.type === "open-choice" ||
      (item.type === "choice" && options.length)
    ) {
      field = item.repeats ? (
        <MultiSelect
          {...common}
          titleText={label}
          label={t("common.select")}
          items={options}
          itemToString={(o) => o?.label || ""}
          selectedItems={options.filter((o) =>
            (answers[item.linkId] || []).some((a) =>
              equal(valueOf(a), valueOf(o.answer)),
            ),
          )}
          onChange={({ selectedItems }) =>
            change(
              item.linkId,
              selectedItems
                .map((o) => o.answer)
                .concat(item.type === "open-choice" ? customAnswers : []),
            )
          }
        />
      ) : (
        <Select
          {...common}
          value={
            options.find((o) => equal(valueOf(o.answer), value))?.id ??
            (customAnswers.length ? "custom" : "")
          }
          onChange={(e) =>
            change(
              item.linkId,
              e.target.value === ""
                ? []
                : e.target.value === "custom"
                  ? [{ valueString: "" }]
                  : [options[Number(e.target.value)].answer],
            )
          }
        >
          <SelectItem value="" text={t("common.select")} />
          {options.map((o) => (
            <SelectItem key={o.id} value={o.id} text={o.label} />
          ))}
          {item.type === "open-choice" && (
            <SelectItem
              value="custom"
              text={t("microbiology.case.otherAnswer")}
            />
          )}
        </Select>
      );
      if (
        item.type === "open-choice" &&
        (item.repeats || !options.length || customAnswers.length)
      ) {
        const TextField = item.repeats ? TextArea : TextInput;
        field = (
          <Stack gap={3}>
            {options.length > 0 && field}
            <TextField
              {...common}
              id={`${id}-custom`}
              labelText={
                options.length
                  ? t(
                      item.repeats
                        ? "microbiology.case.otherAnswers"
                        : "microbiology.case.otherAnswer",
                    )
                  : label
              }
              value={customAnswers
                .map((answer) => answer.valueString)
                .join("\n")}
              onChange={(event) =>
                change(
                  item.linkId,
                  item.repeats
                    ? (answers[item.linkId] || [])
                        .filter((answer) => !customAnswers.includes(answer))
                        .concat(
                          event.target.value
                            .split("\n")
                            .map((text) => ({ valueString: text })),
                        )
                    : [{ valueString: event.target.value }],
                )
              }
            />
          </Stack>
        );
      }
    } else if (item.type === "boolean")
      field = (
        <Select
          {...common}
          value={String(value)}
          onChange={(e) =>
            change(
              item.linkId,
              e.target.value === ""
                ? []
                : [{ valueBoolean: e.target.value === "true" }],
            )
          }
        >
          <SelectItem value="" text={t("common.select")} />
          <SelectItem value="true" text={t("label.yes")} />
          <SelectItem value="false" text={t("label.no")} />
        </Select>
      );
    else if (
      [
        "string",
        "text",
        "integer",
        "decimal",
        "date",
        "dateTime",
        "time",
        "url",
      ].includes(item.type) &&
      !item.repeats
    ) {
      const type =
        {
          integer: "number",
          decimal: "number",
          date: "date",
          dateTime: "datetime-local",
          time: "time",
          url: "url",
        }[item.type] || "text";
      const key = {
        text: "valueString",
        string: "valueString",
        integer: "valueInteger",
        decimal: "valueDecimal",
        date: "valueDate",
        dateTime: "valueDateTime",
        time: "valueTime",
        url: "valueUri",
      }[item.type];
      const props = {
        ...common,
        value:
          item.type === "dateTime" && value ? labDateTimeInput(value) : value,
        onChange: (e) =>
          change(
            item.linkId,
            e.target.value === ""
              ? []
              : [
                  {
                    [key]: ["integer", "decimal"].includes(item.type)
                      ? Number(e.target.value)
                      : item.type === "dateTime"
                        ? labInputInstant(e.target.value)
                        : e.target.value,
                  },
                ],
          ),
      };
      field =
        item.type === "text" ? (
          <TextArea {...props} />
        ) : (
          <TextInput
            {...props}
            type={type}
            step={item.type === "integer" ? "1" : "any"}
          />
        );
    } else
      field = (
        <InlineNotification
          kind="warning"
          hideCloseButton
          title={label}
          subtitle={t("microbiology.case.questionUnsupported")}
        />
      );
    return (
      <Stack gap={3} key={item.linkId}>
        {field}
        {(item.item || []).map(renderItem)}
      </Stack>
    );
  };
  const hasRepeatedGroups = (items) =>
    (items || []).some(
      (item) =>
        (item.repeats && (item.type === "group" || item.item?.length)) ||
        hasRepeatedGroups(item.item),
    );
  const repeatedGroups = hasRepeatedGroups(data?.questionnaire?.item);
  if (!detail.programId) return null;
  return (
    <Tile>
      <Stack gap={4}>
        <h3>{t("notebook.label.questionnaire")}</h3>
        {error && (
          <InlineNotification
            kind="error"
            hideCloseButton
            title={t("microbiology.case.questionSaveError")}
          />
        )}
        {!data && !error && <InlineLoading description={t("common.loading")} />}
        {data?.questionnaire && (
          <>
            {repeatedGroups && (
              <InlineNotification
                kind="warning"
                hideCloseButton
                title={t("microbiology.case.questionUnsupported")}
              />
            )}
            {(data.questionnaire.item || []).map(renderItem)}
            {detail.canWrite && (
              <Button disabled={saving || repeatedGroups} onClick={save}>
                {t("microbiology.case.saveQuestions")}
              </Button>
            )}
          </>
        )}
      </Stack>
    </Tile>
  );
}
