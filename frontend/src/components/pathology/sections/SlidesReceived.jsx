import React from "react";
import {
  Heading,
  InlineNotification,
  Section,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
} from "@carbon/react";
import { useIntl } from "react-intl";
import LabelVerificationTag from "../LabelVerificationTag";
import SlideStatusTag from "../SlideStatusTag";

const HEADING_ID = "review-slides-received-heading";

/**
 * FR-12.2: the slides the pathologist has in front of them, one row each with
 * its stain and the checks the bench recorded, read-only. `rows` are
 * slidesReceived's and `labelVerifications` the case read's; `neverChecked`
 * names the slides whose label was never checked against their block, said in
 * a note above the table where the case view passes them (a note, never a
 * gate).
 */
const SlidesReceived = ({ rows, labelVerifications, neverChecked = [] }) => {
  const intl = useIntl();
  const text = (id, values) => intl.formatMessage({ id }, values);

  return (
    <Section className="pathology-case-view__slides-received">
      <Stack gap={4}>
        <Heading id={HEADING_ID} className="pathology-case-view__heading">
          {text("pathology.label.slidesReceived", { count: rows.length })}
        </Heading>
        {neverChecked.length > 0 && (
          <InlineNotification
            kind="info"
            lowContrast
            hideCloseButton
            title=""
            subtitle={text("pathology.banner.labelsNeverChecked", {
              designations: neverChecked.join(", "),
            })}
            data-testid="review-labels-never-checked"
          />
        )}
        {rows.length === 0 ? (
          <p>{text("pathology.empty.noSlidesReceived")}</p>
        ) : (
          <Table
            size="sm"
            aria-labelledby={HEADING_ID}
            data-testid="review-slides-received"
          >
            <TableHead>
              <TableRow>
                <TableHeader>{text("pathology.label.slide")}</TableHeader>
                <TableHeader>{text("pathology.label.stain")}</TableHeader>
                <TableHeader>{text("pathology.label.checks")}</TableHeader>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.id}>
                  <TableCell>{row.name}</TableCell>
                  <TableCell>{row.stain}</TableCell>
                  <TableCell>
                    <LabelVerificationTag
                      slide={row}
                      labelVerifications={labelVerifications}
                    />
                    {row.stained && (
                      <Tag size="sm" type="green">
                        {text("pathology.badge.stained")}
                      </Tag>
                    )}
                    <SlideStatusTag
                      kind="qc"
                      slide={{ coverslipQc: row.coverslipQc }}
                      named
                    />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Stack>
    </Section>
  );
};

export default SlidesReceived;
