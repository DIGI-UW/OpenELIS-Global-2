import React from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";

const ReagentUsageHistory = ({ usages = [] }) => {
  const intl = useIntl();
  const title = intl.formatMessage({ id: "microbiology.reagentLots.history" });

  if (usages.length === 0) {
    return null;
  }

  return (
    <TableContainer title={title} className="microbiology-reagent-usage">
      <Table size="sm" aria-label={title}>
        <TableHead>
          <TableRow>
            <TableHeader>
              {intl.formatMessage({ id: "microbiology.reagentLots.reagent" })}
            </TableHeader>
            <TableHeader>
              {intl.formatMessage({ id: "microbiology.reagentLots.lot" })}
            </TableHeader>
            <TableHeader>
              {intl.formatMessage({ id: "microbiology.reagentLots.context" })}
            </TableHeader>
            <TableHeader>
              {intl.formatMessage({ id: "microbiology.reagentLots.used" })}
            </TableHeader>
            <TableHeader>
              {intl.formatMessage({ id: "microbiology.reagentLots.status" })}
            </TableHeader>
            <TableHeader>
              {intl.formatMessage({ id: "microbiology.reagentLots.recorded" })}
            </TableHeader>
          </TableRow>
        </TableHead>
        <TableBody>
          {usages.map((usage) => (
            <TableRow key={usage.id}>
              <TableCell>{usage.reagentName || "-"}</TableCell>
              <TableCell>{usage.lotNumber || "-"}</TableCell>
              <TableCell>
                {intl.formatMessage({
                  id: `microbiology.reagentLots.context.${usage.usageContext}`,
                  defaultMessage: formatMicrobiologyEnum(usage.usageContext),
                })}
              </TableCell>
              <TableCell>
                {intl.formatMessage(
                  { id: "microbiology.reagentLots.quantity" },
                  {
                    quantity: usage.quantityUsed,
                    unit: usage.quantityUnit || "",
                  },
                )}
              </TableCell>
              <TableCell>
                <Tag
                  type={usage.currentLotStatus === "CONSUMED" ? "gray" : "blue"}
                >
                  {intl.formatMessage({
                    id: `microbiology.reagentLots.status.${usage.currentLotStatus}`,
                    defaultMessage: formatMicrobiologyEnum(
                      usage.currentLotStatus,
                    ),
                  })}
                </Tag>
              </TableCell>
              <TableCell>
                {usage.usageDate
                  ? intl.formatDate(usage.usageDate, {
                      dateStyle: "medium",
                      timeStyle: "short",
                    })
                  : "-"}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
};

export default ReagentUsageHistory;
