import React, { useState } from "react";
import {
  Button,
  InlineLoading,
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
  Tag,
  Toggle,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
} from "../../../utils/Utils";
import {
  useInvalidateServerData,
  useServerData,
} from "../../../utils/useServerData";
import { RequiredMarker, requiredProps } from "../../../common/RequiredMarker";
import "./ValidationPolicy.scss";
import ValidationPolicyEditor, {
  policyError,
  draftPayload,
  levelRolesUrl,
  toDraft,
} from "./ValidationPolicyEditor";

export const LAB_UNIT_POLICIES_API = "/rest/validation-policy/lab-units";

const DOMAIN_TAG = {
  CLINICAL: "blue",
  ENVIRONMENTAL: "teal",
  VECTOR: "purple",
};

const refusalCode = async (response) => {
  try {
    const body = (await response?.json()) || {};
    return body.code || "saveFailed";
  } catch {
    return "saveFailed";
  }
};

/**
 * Adds an override (a lab unit with none is chosen) or edits an active one.
 * Rendered only while open, so its fields never stand beside the lab-wide
 * default's.
 */
function OverrideModal({
  editing,
  labUnits,
  overrides,
  labDefault,
  onClose,
  onSaved,
}) {
  const intl = useIntl();
  const message = (code, values) =>
    intl.formatMessage({ id: `admin.validationPolicy.${code}` }, values);
  const available = labUnits.filter(
    (unit) => !overrides.some((override) => override.testSectionId === unit.id),
  );
  const [labUnitId, setLabUnitId] = useState("");
  const [draft, setDraft] = useState(() => toDraft(editing || labDefault));
  const [saving, setSaving] = useState(false);
  const [refusal, setRefusal] = useState(null);
  const { data: roleOptions } = useServerData(
    levelRolesUrl(editing ? editing.testSectionId : labUnitId || null),
  );

  const error = policyError(draft, labDefault.maxLevels);
  const unitName = editing
    ? editing.labUnitName
    : (available.find((unit) => unit.id === labUnitId) || {}).name;
  const before = editing ? editing.trigger : labDefault.trigger;
  const turnsReviewOff =
    !error && draft.trigger === "NONE" && before !== "NONE";

  const submit = () => {
    if (!editing && !labUnitId) {
      setRefusal("labUnitRequired");
      return;
    }
    setSaving(true);
    setRefusal(null);
    const done = async (response) => {
      setSaving(false);
      if (response && response.ok) {
        onSaved(await response.json());
        return;
      }
      setRefusal(await refusalCode(response));
    };
    const policy = draftPayload(draft);
    if (editing) {
      putToOpenElisServerFullResponse(
        `${LAB_UNIT_POLICIES_API}/${editing.id}`,
        JSON.stringify(policy),
        done,
      );
    } else {
      postToOpenElisServerFullResponse(
        LAB_UNIT_POLICIES_API,
        JSON.stringify({ testSectionId: labUnitId, ...policy }),
        done,
      );
    }
  };

  return (
    <Modal
      open
      id="validation-override-modal"
      modalHeading={
        editing
          ? message("overrides.modal.editTitle", { unit: editing.labUnitName })
          : message("overrides.modal.addTitle")
      }
      primaryButtonText={message("overrides.modal.save")}
      secondaryButtonText={message("overrides.modal.cancel")}
      primaryButtonDisabled={
        saving || Boolean(error) || (!editing && available.length === 0)
      }
      onRequestSubmit={submit}
      onRequestClose={onClose}
      preventCloseOnClickOutside
    >
      <Stack gap={5}>
        {!editing &&
          (available.length === 0 ? (
            <InlineNotification
              kind="info"
              lowContrast
              hideCloseButton
              title={message("overrides.modal.noLabUnit")}
            />
          ) : (
            <Select
              id="validation-override-lab-unit"
              labelText={
                <>
                  {message("overrides.modal.labUnit")}
                  <RequiredMarker />
                </>
              }
              {...requiredProps()}
              value={labUnitId}
              onChange={(event) => {
                setLabUnitId(event.target.value);
                setRefusal(null);
              }}
            >
              <SelectItem
                value=""
                text={message("overrides.modal.labUnit.placeholder")}
              />
              {available.map((unit) => (
                <SelectItem key={unit.id} value={unit.id} text={unit.name} />
              ))}
            </Select>
          ))}
        <ValidationPolicyEditor
          idPrefix="validation-override"
          draft={draft}
          onChange={(next) => {
            setDraft(next);
            setRefusal(null);
          }}
          maxLevels={labDefault.maxLevels}
          roleOptions={roleOptions}
          labUnitName={unitName}
        />
        {turnsReviewOff && unitName && (
          <InlineNotification
            kind="warning"
            lowContrast
            hideCloseButton
            title={message("overrides.turnsReviewOff", { unit: unitName })}
          />
        )}
        {refusal && (
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            title={message(`refused.${refusal}`)}
          />
        )}
        {saving && <InlineLoading description={message("saving")} />}
      </Stack>
    </Modal>
  );
}

/**
 * The lab unit overrides on Result Configuration: a unit with an active
 * override follows it instead of the lab-wide default.
 */
export default function LabUnitOverrides({ policies, failed, labDefault }) {
  const intl = useIntl();
  const invalidate = useInvalidateServerData();
  const message = (code, values) =>
    intl.formatMessage({ id: `admin.validationPolicy.${code}` }, values);
  const [showDeactivated, setShowDeactivated] = useState(false);
  const [modal, setModal] = useState(null);
  const [notice, setNotice] = useState(null);
  const [busyId, setBusyId] = useState(null);

  const overrides = (policies && policies.overrides) || [];
  const shown = overrides.filter(
    (override) => showDeactivated || override.active,
  );

  const saved = (override) => {
    setModal(null);
    invalidate(LAB_UNIT_POLICIES_API);
    setNotice({
      kind: "success",
      text: message("overrides.saved", { unit: override.labUnitName }),
    });
  };

  const setActive = (override, active) => {
    setBusyId(override.id);
    setNotice(null);
    putToOpenElisServerFullResponse(
      `${LAB_UNIT_POLICIES_API}/${override.id}/active`,
      JSON.stringify({ active }),
      async (response) => {
        setBusyId(null);
        if (response && response.ok) {
          invalidate(LAB_UNIT_POLICIES_API);
          setNotice({
            kind: "success",
            text: message(
              active ? "overrides.reactivated" : "overrides.deactivated",
              { unit: override.labUnitName },
            ),
          });
          return;
        }
        const code = await refusalCode(response);
        setNotice({ kind: "error", text: message(`refused.${code}`) });
      },
    );
  };

  const rolesOf = (override) =>
    override.levels === 0
      ? message("overrides.roles.none")
      : override.levelRoles.map((level) => level.roleName).join(" → ");

  return (
    <div data-testid="validation-overrides">
      <Stack gap={5}>
        <div className="validationOverridesHeader">
          <div>
            <h5>
              <FormattedMessage id="admin.validationPolicy.overrides.title" />
            </h5>
            <p className="cds--label">
              <FormattedMessage id="admin.validationPolicy.overrides.help" />
            </p>
          </div>
          <div className="validationOverridesActions">
            <Toggle
              id="validation-overrides-show-deactivated"
              size="sm"
              labelText={message("overrides.showDeactivated")}
              labelA={message("overrides.showDeactivated")}
              labelB={message("overrides.showDeactivated")}
              toggled={showDeactivated}
              onToggle={(toggled) => setShowDeactivated(toggled)}
              hideLabel
            />
            <Button
              id="validation-overrides-add"
              kind="tertiary"
              size="sm"
              disabled={!policies}
              onClick={() => {
                setNotice(null);
                setModal({ editing: null });
              }}
            >
              <FormattedMessage id="admin.validationPolicy.overrides.add" />
            </Button>
          </div>
        </div>
        {failed && !policies && (
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            title={message("overrides.loadFailed")}
          />
        )}
        {!failed && !policies && (
          <InlineLoading description={message("overrides.loading")} />
        )}
        {policies && shown.length === 0 && (
          <p data-testid="validation-overrides-empty">
            <FormattedMessage
              id={
                overrides.length === 0
                  ? "admin.validationPolicy.overrides.empty"
                  : "admin.validationPolicy.overrides.noneActive"
              }
            />
          </p>
        )}
        {shown.length > 0 && (
          <Table size="sm" useZebraStyles={false}>
            <TableHead>
              <TableRow>
                {[
                  "labUnit",
                  "domain",
                  "trigger",
                  "levels",
                  "roles",
                  "status",
                  "actions",
                ].map((column) => (
                  <TableHeader key={column}>
                    <FormattedMessage
                      id={`admin.validationPolicy.overrides.column.${column}`}
                    />
                  </TableHeader>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {shown.map((override) => (
                <TableRow
                  key={override.id}
                  data-testid={`validation-override-${override.testSectionId}`}
                >
                  <TableCell>{override.labUnitName}</TableCell>
                  <TableCell>
                    {override.domain && (
                      <Tag
                        size="sm"
                        type={DOMAIN_TAG[override.domain] || "gray"}
                      >
                        <FormattedMessage
                          id={`label.domain.${override.domain}`}
                          defaultMessage={override.domain}
                        />
                      </Tag>
                    )}
                  </TableCell>
                  <TableCell>
                    <FormattedMessage
                      id={`admin.validationPolicy.trigger.${override.trigger}`}
                    />
                  </TableCell>
                  <TableCell>{override.levels}</TableCell>
                  <TableCell>{rolesOf(override)}</TableCell>
                  <TableCell>
                    <Tag size="sm" type={override.active ? "green" : "gray"}>
                      <FormattedMessage
                        id={
                          override.active
                            ? "admin.validationPolicy.overrides.status.active"
                            : "admin.validationPolicy.overrides.status.deactivated"
                        }
                      />
                    </Tag>
                  </TableCell>
                  <TableCell>
                    {busyId === override.id ? (
                      <InlineLoading description={message("saving")} />
                    ) : (
                      <>
                        {override.active && (
                          <Button
                            kind="ghost"
                            size="sm"
                            data-testid={`validation-override-edit-${override.testSectionId}`}
                            onClick={() => {
                              setNotice(null);
                              setModal({ editing: override });
                            }}
                          >
                            <FormattedMessage id="admin.validationPolicy.overrides.edit" />
                          </Button>
                        )}
                        <Button
                          kind="ghost"
                          size="sm"
                          data-testid={`validation-override-${
                            override.active ? "deactivate" : "reactivate"
                          }-${override.testSectionId}`}
                          onClick={() => setActive(override, !override.active)}
                        >
                          <FormattedMessage
                            id={
                              override.active
                                ? "admin.validationPolicy.overrides.deactivate"
                                : "admin.validationPolicy.overrides.reactivate"
                            }
                          />
                        </Button>
                      </>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
        {notice && (
          <InlineNotification
            kind={notice.kind}
            lowContrast
            title={notice.text}
            onClose={() => setNotice(null)}
          />
        )}
        <p className="cds--label">
          <FormattedMessage id="admin.validationPolicy.overrides.appliesAfter" />
        </p>
      </Stack>
      {modal && policies && (
        <OverrideModal
          editing={modal.editing}
          labUnits={policies.labUnits}
          overrides={overrides}
          labDefault={labDefault}
          onClose={() => setModal(null)}
          onSaved={saved}
        />
      )}
    </div>
  );
}
