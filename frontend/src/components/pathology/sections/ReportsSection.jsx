import ClinicalAttachmentPreview from "../../common/ClinicalAttachment/ClinicalAttachmentPreview";
import React from "react";
import { Button, FileUploader, InlineLoading, Stack } from "@carbon/react";
import { Launch, Subtract } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import "../pathologyCaseView.scss";
import "../../caseView/caseView.scss";

/**
 * FR-15: the report versions the case carries, each either generated here or
 * uploaded from outside, and opened one click away.
 */
const ReportsSection = ({
  caseInfo,
  updateCase,
  readOnly,
  reportParams,
  loadingReport,
  onReportFile,
  onGenerateReport,
}) => {
  const intl = useIntl();

  const reports = caseInfo.reports ?? [];

  const removeReport = (index) =>
    updateCase((prev) => ({
      reports: (prev.reports ?? []).filter((_, position) => position !== index),
    }));

  const addReport = () =>
    updateCase((prev) => ({
      reports: [...(prev.reports ?? []), { id: "", reportType: "PATHOLOGY" }],
    }));

  return (
    <Stack gap={6}>
      {/* No heading of its own: the accordion header above already names
          this section, and repeating the word under it says nothing. */}
      {loadingReport && <InlineLoading />}
      <div>
        {reports.length === 0 && (
          <p className="case-view__locked-hint">
            <FormattedMessage id="pathology.empty.noReports" />
          </p>
        )}
        {reports.map((report, index) => (
          <div className="pathology-case-view__row" key={index}>
            <span className="pathology-case-view__row-label">
              <FormattedMessage id="pathology.label.report" />
            </span>
            <div className="pathology-case-view__row-actions">
              <FileUploader
                labelDescription={intl.formatMessage({
                  id: "clinicalAttachment.uploadHint",
                })}
                buttonLabel={intl.formatMessage({
                  id: "label.button.uploadfile",
                })}
                iconDescription={intl.formatMessage({
                  id: "label.button.uploadfile",
                })}
                multiple={false}
                accept={["image/jpeg", "image/png", "application/pdf"]}
                disabled={readOnly || reportParams[index]?.submited}
                name=""
                buttonKind="tertiary"
                size="md"
                filenameStatus="edit"
                onChange={(e) => {
                  e.preventDefault();
                  onReportFile(index, e.target.files[0]);
                }}
                onClick={function noRefCheck() {}}
                onDelete={(e) => {
                  e.preventDefault();
                }}
              />
              {report.image && !reportParams[index]?.submited && (
                <ClinicalAttachmentPreview
                  kind="tertiary"
                  size="md"
                  attachment={report}
                />
              )}
              {reportParams[index]?.submited && (
                <Button
                  kind="tertiary"
                  size="md"
                  renderIcon={Launch}
                  onClick={() =>
                    window.open(reportParams[index]?.reportLink, "_blank")
                  }
                >
                  <FormattedMessage id="pathology.label.view" />
                </Button>
              )}
              <Button
                kind="tertiary"
                size="md"
                disabled={readOnly || reportParams[index]?.submited}
                onClick={() => onGenerateReport(index)}
              >
                <FormattedMessage id="button.label.genarateReport" />
              </Button>
              {/* See the same button in GrossingSection: an IconButton clips
                  the word it was given beside its glyph. */}
              <Button
                kind="ghost"
                size="md"
                renderIcon={Subtract}
                disabled={readOnly}
                onClick={() => removeReport(index)}
              >
                <FormattedMessage id="label.button.remove.report" />
              </Button>
            </div>
          </div>
        ))}
        <div className="pathology-case-view__add-row">
          <Button size="md" disabled={readOnly} onClick={addReport}>
            <FormattedMessage id="immunohistochemistry.label.addreport" />
          </Button>
        </div>
      </div>
    </Stack>
  );
};

export default ReportsSection;
