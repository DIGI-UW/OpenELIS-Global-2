import React from "react";
import {
  Accordion,
  AccordionItem,
  Table,
  TableHead,
  TableBody,
  TableRow,
  TableHeader,
  TableCell,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";

export default function CaseHistory({ activities }) {
  const intl = useIntl();
  const t = (id, values) => intl.formatMessage({ id }, values);
  const note = (activity) => {
    if (
      [
        "CULTURE_PURPOSE_CHANGED",
        "REPLICATES_CHANGED",
        "PROGRAM_CHANGED_REEXPORT_REQUIRED",
      ].includes(activity.activityType)
    ) {
      try {
        const data = JSON.parse(activity.structuredData || "{}");
        if (data.after !== undefined) {
          const purpose = activity.activityType === "CULTURE_PURPOSE_CHANGED";
          return t("microbiology.case.history.change", {
            before: purpose
              ? t(`microbiology.case.purpose.${data.before}`)
              : (data.before ?? t("not.available")),
            after: purpose
              ? t(`microbiology.case.purpose.${data.after}`)
              : (data.after ?? t("not.available")),
          });
        }
      } catch {
        return activity.note;
      }
    }
    return activity.note;
  };
  // i18n-keys: microbiology.case.purpose.*
  return (
    <Accordion>
      <AccordionItem title={t("microbiology.case.timeline")}>
        <Table aria-label={t("microbiology.case.timeline")}>
          <TableHead>
            <TableRow>
              {["date", "author", "note"].map((key) => (
                <TableHeader key={key}>
                  {t(`microbiology.testing.${key}`)}
                </TableHeader>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {activities.map((activity) => (
              <TableRow key={activity.id}>
                <TableCell>
                  {intl.formatDate(new Date(activity.occurredAt))}{" "}
                  {intl.formatTime(new Date(activity.occurredAt))}
                </TableCell>
                <TableCell>
                  {activity.performedByDisplay || t("not.available")}
                </TableCell>
                <TableCell>
                  {formatMicrobiologyEnum(activity.activityType, intl)}
                  {note(activity) ? `: ${note(activity)}` : ""}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </AccordionItem>
    </Accordion>
  );
}
