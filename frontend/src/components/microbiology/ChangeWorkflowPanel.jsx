import React, { useEffect, useState } from "react";
import {
  Button,
  Checkbox,
  InlineNotification,
  Select,
  SelectItem,
  Stack,
  TextArea,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";

const WORKFLOW_OPTIONS = ["BACTERIOLOGY", "MYCOBACTERIOLOGY_TB"];

const ChangeWorkflowPanel = ({
  caseId,
  workflowType,
  cultureMethodId,
  requiresConfirmation,
  service,
  onChanged,
}) => {
  const intl = useIntl();
  const [targetWorkflow, setTargetWorkflow] = useState(
    workflowType === "UNASSIGNED" ? "" : workflowType,
  );
  const [targetMethodId, setTargetMethodId] = useState(cultureMethodId || "");
  const [methods, setMethods] = useState([]);
  const [reason, setReason] = useState("");
  const [preserveConfirmed, setPreserveConfirmed] = useState(false);
  const [loadingMethods, setLoadingMethods] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    if (!targetWorkflow) {
      setMethods([]);
      setTargetMethodId("");
      return () => {
        active = false;
      };
    }

    setLoadingMethods(true);
    service.getCultureMethods(targetWorkflow).then((options) => {
      if (!active) {
        return;
      }
      const nextMethods = Array.isArray(options) ? options : [];
      setMethods(nextMethods);
      setTargetMethodId((currentMethodId) =>
        nextMethods.some((method) => method.id === currentMethodId)
          ? currentMethodId
          : "",
      );
      setLoadingMethods(false);
    });

    return () => {
      active = false;
    };
  }, [service, targetWorkflow]);

  const applyWorkflow = () => {
    setSaving(true);
    setError("");
    service
      .changeCaseWorkflow(caseId, {
        workflowType: targetWorkflow,
        cultureMethodId: targetMethodId,
        reason: reason.trim(),
        preserveExistingWorkConfirmed: preserveConfirmed,
      })
      .then((detail) => {
        if (!detail || detail.error || detail.status >= 400) {
          throw new Error(detail?.message || detail?.error || "UNKNOWN_ERROR");
        }
        onChanged(detail);
      })
      .catch((workflowError) => {
        setError(formatMicrobiologyEnum(workflowError.message, intl));
      })
      .finally(() => setSaving(false));
  };

  const valid =
    targetWorkflow &&
    targetMethodId &&
    reason.trim() &&
    (!requiresConfirmation || preserveConfirmed);

  return (
    <section aria-labelledby="microbiology-change-workflow-title">
      <Stack gap={5}>
        <div>
          <h3 id="microbiology-change-workflow-title">
            {intl.formatMessage({ id: "microbiology.workflowChange.title" })}
          </h3>
          <p>
            {intl.formatMessage({
              id:
                workflowType === "UNASSIGNED"
                  ? "microbiology.workflowChange.unassignedHelp"
                  : "microbiology.workflowChange.help",
            })}
          </p>
        </div>
        {error && (
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "microbiology.workflowChange.error",
            })}
            subtitle={error}
          />
        )}
        <Select
          id="microbiology-workflow-target"
          labelText={intl.formatMessage({
            id: "microbiology.workflowChange.workflow",
          })}
          value={targetWorkflow}
          onChange={(event) => {
            setTargetWorkflow(event.target.value);
            setPreserveConfirmed(false);
          }}
        >
          <SelectItem
            value=""
            text={intl.formatMessage({
              id: "microbiology.workflowChange.workflowPlaceholder",
            })}
          />
          {WORKFLOW_OPTIONS.map((option) => (
            <SelectItem
              key={option}
              value={option}
              text={formatMicrobiologyEnum(option, intl)}
            />
          ))}
        </Select>
        <Select
          id="microbiology-workflow-method"
          labelText={intl.formatMessage({
            id: "microbiology.orderDetail.cultureMethod",
          })}
          value={targetMethodId}
          disabled={!targetWorkflow || loadingMethods}
          onChange={(event) => setTargetMethodId(event.target.value)}
        >
          <SelectItem
            value=""
            text={intl.formatMessage({
              id: "microbiology.workflowChange.methodPlaceholder",
            })}
          />
          {methods.map((method) => (
            <SelectItem key={method.id} value={method.id} text={method.label} />
          ))}
        </Select>
        <TextArea
          id="microbiology-workflow-reason"
          labelText={intl.formatMessage({
            id: "microbiology.workflowChange.reason",
          })}
          value={reason}
          onChange={(event) => setReason(event.target.value)}
        />
        {requiresConfirmation && (
          <>
            <InlineNotification
              kind="warning"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({
                id: "microbiology.workflowChange.preserveWarningTitle",
              })}
              subtitle={intl.formatMessage({
                id: "microbiology.workflowChange.preserveWarning",
              })}
            />
            <Checkbox
              id="microbiology-workflow-preserve-confirmation"
              labelText={intl.formatMessage({
                id: "microbiology.workflowChange.preserveConfirmation",
              })}
              checked={preserveConfirmed}
              onChange={(_, state) => setPreserveConfirmed(state.checked)}
            />
          </>
        )}
        <Button disabled={!valid || saving} onClick={applyWorkflow}>
          {intl.formatMessage({ id: "microbiology.workflowChange.apply" })}
        </Button>
      </Stack>
    </section>
  );
};

export default ChangeWorkflowPanel;
