import React, { useCallback, useContext, useEffect, useState } from "react";
import {
  Button,
  Checkbox,
  InlineNotification,
  ListItem,
  Loading,
  NumberInput,
  Select,
  SelectItem,
  SelectItemGroup,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  TextInput,
  UnorderedList,
} from "@carbon/react";
import { ArrowDown, ArrowUp, TrashCan } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import "../../containerTypes/ContainerTypes.css";

const UNITS = ["mL", "µL", "L"];

const toRow = (row) => ({
  containerTypeId: row.containerTypeId,
  name: row.name,
  capColourHex: row.capColourHex,
  capColourName: row.capColourName,
  active: row.active,
  yields: row.yields || [],
  role: row.role,
  count: row.count || 1,
  minVolume: row.minVolume == null ? "" : String(row.minVolume),
  volumeUnit: row.volumeUnit || "mL",
  usedAsSampleTypeId: row.usedAsSampleTypeId || "",
});

/**
 * OGC-1611 FR-G5 to G6b: the containers a test expects its sample in. One is
 * Preferred, the others Alternate, each with a count and an optional minimum
 * volume. A container yielding another sample type can be listed as an
 * Alternate used as one of the test's own sample types; the sample keeps its
 * true sample type, shown here as a secondary sample type of the test.
 */
const ContainersSection = ({ testId }) => {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [view, setView] = useState(null);
  const [rows, setRows] = useState([]);
  const [needsOwnContainer, setNeedsOwnContainer] = useState(false);
  const [adding, setAdding] = useState("");
  const [saving, setSaving] = useState(false);
  const [refusal, setRefusal] = useState(null);

  const apply = useCallback((loaded) => {
    setView(loaded);
    setRows((loaded.containers || []).map(toRow));
    setNeedsOwnContainer(Boolean(loaded.needsOwnContainer));
    setAdding("");
    setRefusal(null);
  }, []);

  useEffect(() => {
    if (!testId) {
      return undefined;
    }
    const controller = new AbortController();
    setLoading(true);
    setLoadFailed(false);
    getFromOpenElisServer(
      `/rest/test-catalog/tests/${testId}/containers`,
      (loaded) => {
        if (controller.signal.aborted) {
          return;
        }
        if (!loaded || !Array.isArray(loaded.containers)) {
          setLoadFailed(true);
          setLoading(false);
          return;
        }
        apply(loaded);
        setLoading(false);
      },
      controller.signal,
    );
    return () => controller.abort();
  }, [testId, apply]);

  const primaryTypes = view?.primarySampleTypes || [];
  const listed = new Set(rows.map((row) => row.containerTypeId));
  const yielding = (view?.yieldingChoices || []).filter(
    (choice) => !listed.has(choice.id),
  );
  const other = (view?.otherChoices || []).filter(
    (choice) => !listed.has(choice.id),
  );
  const isOther = (row) =>
    !row.yields.some((type) =>
      primaryTypes.some((primary) => primary.id === type.id),
    );
  const hasPreferred = rows.some((row) => row.role === "PREFERRED");

  const update = (index, changes) => {
    setRefusal(null);
    setRows((current) =>
      current.map((row, position) => {
        if (position === index) {
          return { ...row, ...changes };
        }
        if (changes.role === "PREFERRED" && row.role === "PREFERRED") {
          return { ...row, role: "ALTERNATE" };
        }
        return row;
      }),
    );
  };

  const move = (index, step) => {
    setRows((current) => {
      const next = [...current];
      const [moved] = next.splice(index, 1);
      next.splice(index + step, 0, moved);
      return next;
    });
  };

  const add = (choiceId) => {
    const choice = [...yielding, ...other].find(
      (candidate) => String(candidate.id) === choiceId,
    );
    if (!choice) {
      return;
    }
    const usedAs = other.some((candidate) => candidate.id === choice.id);
    setRefusal(null);
    setRows((current) => [
      ...current,
      toRow({
        containerTypeId: choice.id,
        name: choice.name,
        capColourHex: choice.capColourHex,
        capColourName: choice.capColourName,
        active: choice.active,
        yields: choice.sampleTypes,
        role:
          !usedAs && !current.some((row) => row.role === "PREFERRED")
            ? "PREFERRED"
            : "ALTERNATE",
        count: 1,
        usedAsSampleTypeId: usedAs ? primaryTypes[0]?.id : "",
      }),
    ]);
    setAdding("");
  };

  const save = () => {
    setSaving(true);
    setRefusal(null);
    const body = {
      needsOwnContainer,
      containers: rows.map((row) => ({
        containerTypeId: row.containerTypeId,
        role: row.role,
        count: Number(row.count) || 1,
        minVolume: row.minVolume === "" ? null : Number(row.minVolume),
        volumeUnit: row.minVolume === "" ? null : row.volumeUnit,
        usedAsSampleTypeId: row.usedAsSampleTypeId || null,
      })),
    };
    putToOpenElisServerJsonResponse(
      `/rest/test-catalog/tests/${testId}/containers`,
      JSON.stringify(body),
      (response) => {
        setSaving(false);
        setNotificationVisible(true);
        if (response && Array.isArray(response.containers)) {
          apply(response);
          addNotification({
            kind: "success",
            title: intl.formatMessage({
              id: "label.testCatalog.section.containers",
            }),
            message: intl.formatMessage({
              id: "label.testCatalog.containers.saved",
            }),
          });
          return;
        }
        const known = response && response.code;
        const message = intl.formatMessage({
          id: known
            ? `label.testCatalog.containers.error.${response.code}`
            : "label.testCatalog.containers.error.saveFailed",
          defaultMessage: intl.formatMessage({
            id: "label.testCatalog.containers.error.saveFailed",
          }),
        });
        setRefusal({ row: response?.row ?? null, message });
        addNotification({
          kind: "error",
          title: intl.formatMessage({
            id: "label.testCatalog.section.containers",
          }),
          message,
        });
      },
    );
  };

  if (loading || (!loadFailed && !view)) {
    return (
      <Loading
        description={intl.formatMessage({ id: "label.loading" })}
        withOverlay={false}
      />
    );
  }

  if (loadFailed) {
    return (
      <InlineNotification
        kind="error"
        lowContrast
        hideCloseButton
        title={intl.formatMessage({ id: "error.title" })}
        subtitle={intl.formatMessage({
          id: "label.testCatalog.containers.loadFailed",
        })}
      />
    );
  }

  const secondary = view.secondarySampleTypes || [];

  return (
    <Stack gap={6} data-testid="containers-section">
      <p className="cds--label-description">
        <FormattedMessage id="label.testCatalog.containers.intro" />
      </p>
      <div data-testid="containers-sample-types">
        <div className="cds--label">
          <FormattedMessage id="label.testCatalog.containers.sampleTypes" />
        </div>
        <div>
          <FormattedMessage id="label.testCatalog.containers.primary" />{" "}
          {primaryTypes.length === 0 ? (
            <FormattedMessage id="label.testCatalog.containers.noSampleType" />
          ) : (
            primaryTypes.map((type) => (
              <Tag key={type.id} size="sm" type="blue">
                {type.value}
              </Tag>
            ))
          )}
        </div>
        <div data-testid="containers-secondary">
          <FormattedMessage id="label.testCatalog.containers.secondary" />{" "}
          {secondary.length === 0 ? (
            <FormattedMessage id="label.testCatalog.containers.noSecondary" />
          ) : (
            <UnorderedList>
              {secondary.map((type) => (
                <ListItem key={`${type.sampleTypeId}-${type.containerName}`}>
                  {intl.formatMessage(
                    { id: "label.testCatalog.containers.secondaryEntry" },
                    {
                      sampleType: type.sampleTypeName,
                      container: type.containerName,
                      usedAs: type.usedAsSampleTypeName,
                    },
                  )}
                </ListItem>
              ))}
            </UnorderedList>
          )}
        </div>
      </div>

      <Checkbox
        id="containers-needs-own"
        labelText={intl.formatMessage({
          id: "label.testCatalog.containers.needsOwn",
        })}
        helperText={intl.formatMessage({
          id: "label.testCatalog.containers.needsOwnHelp",
        })}
        checked={needsOwnContainer}
        onChange={(_, { checked }) => setNeedsOwnContainer(checked)}
      />

      {rows.length === 0 ? (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "label.testCatalog.containers.empty",
          })}
        />
      ) : (
        <div className="testContainerTable">
          <Table
            size="lg"
            aria-label={intl.formatMessage({
              id: "label.testCatalog.section.containers",
            })}
            data-testid="containers-table"
          >
            <TableHead>
              <TableRow>
                <TableHeader>
                  <FormattedMessage id="label.testCatalog.containers.col.container" />
                </TableHeader>
                <TableHeader>
                  <FormattedMessage id="label.testCatalog.containers.col.role" />
                </TableHeader>
                <TableHeader>
                  <FormattedMessage id="label.testCatalog.containers.col.count" />
                </TableHeader>
                <TableHeader>
                  <FormattedMessage id="label.testCatalog.containers.col.minVolume" />
                </TableHeader>
                <TableHeader>
                  <span className="cds--visually-hidden">
                    <FormattedMessage id="label.testCatalog.containers.col.actions" />
                  </span>
                </TableHeader>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row, index) => {
                const usedAs = isOther(row);
                const refused = refusal && refusal.row === index;
                return (
                  <TableRow
                    key={row.containerTypeId}
                    data-testid={`container-row-${row.containerTypeId}`}
                  >
                    <TableCell className="testContainerName">
                      <span className="containerTypeCap">
                        {row.capColourHex && (
                          <span
                            className="containerTypeSwatch"
                            style={{ backgroundColor: row.capColourHex }}
                            aria-hidden="true"
                          />
                        )}
                        {row.name}
                      </span>
                      {!row.active && (
                        <Tag size="sm" type="gray">
                          <FormattedMessage id="label.testCatalog.containers.deactivated" />
                        </Tag>
                      )}
                      {usedAs ? (
                        <div className="testContainerUsedAsField">
                          <Select
                            id={`container-used-as-${row.containerTypeId}`}
                            labelText={intl.formatMessage(
                              { id: "label.testCatalog.containers.usedAsFor" },
                              {
                                container: row.name,
                                yields: row.yields
                                  .map((type) => type.value)
                                  .join(", "),
                              },
                            )}
                            size="sm"
                            helperText={intl.formatMessage(
                              { id: "label.testCatalog.containers.yields" },
                              {
                                sampleTypes: row.yields
                                  .map((type) => type.value)
                                  .join(", "),
                              },
                            )}
                            hideLabel
                            value={row.usedAsSampleTypeId}
                            onChange={(event) =>
                              update(index, {
                                usedAsSampleTypeId: event.target.value,
                              })
                            }
                          >
                            {primaryTypes.map((type) => (
                              <SelectItem
                                key={type.id}
                                value={type.id}
                                text={intl.formatMessage(
                                  {
                                    id: "label.testCatalog.containers.canBeUsedAs",
                                  },
                                  { sampleType: type.value },
                                )}
                              />
                            ))}
                          </Select>
                        </div>
                      ) : (
                        <div>
                          {row.yields.map((type) => (
                            <Tag key={type.id} size="sm" type="blue">
                              {type.value}
                            </Tag>
                          ))}
                        </div>
                      )}
                      {refused && (
                        <div
                          className="cds--form-requirement"
                          style={{ display: "block" }}
                          data-testid={`container-row-error-${row.containerTypeId}`}
                        >
                          {refusal.message}
                        </div>
                      )}
                    </TableCell>
                    <TableCell>
                      <div className="testContainerRoleField">
                        <Select
                          id={`container-role-${row.containerTypeId}`}
                          labelText={intl.formatMessage(
                            { id: "label.testCatalog.containers.roleFor" },
                            { container: row.name },
                          )}
                          hideLabel
                          size="sm"
                          value={row.role}
                          onChange={(event) =>
                            update(index, { role: event.target.value })
                          }
                        >
                          <SelectItem
                            value="PREFERRED"
                            disabled={usedAs}
                            text={intl.formatMessage({
                              id: "label.testCatalog.containers.role.PREFERRED",
                            })}
                          />
                          <SelectItem
                            value="ALTERNATE"
                            text={intl.formatMessage({
                              id: "label.testCatalog.containers.role.ALTERNATE",
                            })}
                          />
                        </Select>
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="testContainerCountField">
                        <NumberInput
                          id={`container-count-${row.containerTypeId}`}
                          label={intl.formatMessage(
                            { id: "label.testCatalog.containers.countFor" },
                            { container: row.name },
                          )}
                          hideLabel
                          size="sm"
                          min={1}
                          max={99}
                          value={row.count}
                          invalidText={intl.formatMessage({
                            id: "label.testCatalog.containers.error.countInvalid",
                          })}
                          onChange={(event, { value }) =>
                            update(index, { count: value })
                          }
                        />
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="testContainerVolume">
                        <div className="testContainerVolumeField">
                          <TextInput
                            id={`container-volume-${row.containerTypeId}`}
                            labelText={intl.formatMessage(
                              {
                                id: "label.testCatalog.containers.minVolumeFor",
                              },
                              { container: row.name },
                            )}
                            hideLabel
                            size="sm"
                            inputMode="decimal"
                            placeholder={intl.formatMessage({
                              id: "label.testCatalog.containers.optional",
                            })}
                            value={row.minVolume}
                            onChange={(event) =>
                              update(index, { minVolume: event.target.value })
                            }
                          />
                        </div>
                        <div className="testContainerUnitField">
                          <Select
                            id={`container-unit-${row.containerTypeId}`}
                            labelText={intl.formatMessage(
                              { id: "label.testCatalog.containers.unitFor" },
                              { container: row.name },
                            )}
                            hideLabel
                            size="sm"
                            value={row.volumeUnit}
                            onChange={(event) =>
                              update(index, { volumeUnit: event.target.value })
                            }
                          >
                            {UNITS.map((unit) => (
                              <SelectItem key={unit} value={unit} text={unit} />
                            ))}
                          </Select>
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="testContainerActions">
                        <Button
                          kind="ghost"
                          size="sm"
                          hasIconOnly
                          renderIcon={ArrowUp}
                          iconDescription={intl.formatMessage(
                            { id: "label.testCatalog.containers.moveUp" },
                            { container: row.name },
                          )}
                          disabled={index === 0}
                          onClick={() => move(index, -1)}
                        />
                        <Button
                          kind="ghost"
                          size="sm"
                          hasIconOnly
                          renderIcon={ArrowDown}
                          iconDescription={intl.formatMessage(
                            { id: "label.testCatalog.containers.moveDown" },
                            { container: row.name },
                          )}
                          disabled={index === rows.length - 1}
                          onClick={() => move(index, 1)}
                        />
                        <Button
                          kind="ghost"
                          size="sm"
                          hasIconOnly
                          renderIcon={TrashCan}
                          iconDescription={intl.formatMessage(
                            { id: "label.testCatalog.containers.remove" },
                            { container: row.name },
                          )}
                          data-testid={`container-remove-${row.containerTypeId}`}
                          onClick={() =>
                            setRows((current) =>
                              current.filter(
                                (_, position) => position !== index,
                              ),
                            )
                          }
                        />
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </div>
      )}

      {rows.length > 0 && !hasPreferred && (
        <InlineNotification
          kind="warning"
          lowContrast
          hideCloseButton
          data-testid="containers-preferred-required"
          title={intl.formatMessage({
            id: "label.testCatalog.containers.error.preferredRequired",
          })}
        />
      )}
      {refusal && refusal.row == null && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          data-testid="containers-refusal"
          title={refusal.message}
        />
      )}

      <Select
        id="containers-add"
        labelText={intl.formatMessage({
          id: "label.testCatalog.containers.add",
        })}
        value={adding}
        onChange={(event) => add(event.target.value)}
      >
        <SelectItem
          value=""
          text={intl.formatMessage({
            id: "label.testCatalog.containers.addPlaceholder",
          })}
        />
        <SelectItemGroup
          label={intl.formatMessage(
            { id: "label.testCatalog.containers.group.yielding" },
            {
              sampleTypes: primaryTypes.map((type) => type.value).join(", "),
            },
          )}
        >
          {yielding.map((choice) => (
            <SelectItem
              key={choice.id}
              value={String(choice.id)}
              text={choice.name}
            />
          ))}
        </SelectItemGroup>
        <SelectItemGroup
          label={intl.formatMessage({
            id: "label.testCatalog.containers.group.other",
          })}
          disabled={primaryTypes.length === 0}
        >
          {other.map((choice) => (
            <SelectItem
              key={choice.id}
              value={String(choice.id)}
              text={`${choice.name} (${choice.sampleTypes
                .map((type) => type.value)
                .join(", ")})`}
            />
          ))}
        </SelectItemGroup>
      </Select>

      <div>
        <Button
          kind="primary"
          onClick={save}
          disabled={saving || (rows.length > 0 && !hasPreferred)}
          data-testid="containers-save"
        >
          <FormattedMessage id="label.button.save" />
        </Button>
      </div>
    </Stack>
  );
};

export default ContainersSection;
