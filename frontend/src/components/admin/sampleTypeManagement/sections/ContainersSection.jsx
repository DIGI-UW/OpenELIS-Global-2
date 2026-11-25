import React, { useEffect, useState } from "react";
import {
  Button,
  InlineNotification,
  Loading,
  Select,
  SelectItem,
  ListItem,
  Stack,
  UnorderedList,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServerJsonResponse,
} from "../../../utils/Utils";

/**
 * OGC-1611 FR-G7 and FR-G6b: the container a sample type is taken in when a
 * test names none, and the tests the sample type serves: as their own sample
 * type (primary), or through a container a test uses as its own (secondary).
 */
function ContainersSection({ sampleTypeId }) {
  const intl = useIntl();
  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [view, setView] = useState(null);
  const [choice, setChoice] = useState("");
  const [saving, setSaving] = useState(false);
  const [refusal, setRefusal] = useState(null);
  const [saved, setSaved] = useState(false);

  const apply = (loaded) => {
    setView(loaded);
    setChoice(
      loaded.defaultContainerTypeId == null
        ? ""
        : String(loaded.defaultContainerTypeId),
    );
    setRefusal(null);
  };

  useEffect(() => {
    if (!sampleTypeId) {
      return undefined;
    }
    const controller = new AbortController();
    setLoading(true);
    setLoadFailed(false);
    getFromOpenElisServer(
      `/rest/sample-type-containers/${encodeURIComponent(sampleTypeId)}`,
      (loaded) => {
        if (controller.signal.aborted) {
          return;
        }
        if (!loaded || !Array.isArray(loaded.containerChoices)) {
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
  }, [sampleTypeId]);

  const save = () => {
    setSaving(true);
    putToOpenElisServerJsonResponse(
      `/rest/sample-type-containers/${encodeURIComponent(sampleTypeId)}`,
      JSON.stringify({
        defaultContainerTypeId: choice === "" ? null : Number(choice),
      }),
      (response) => {
        setSaving(false);
        if (response && Array.isArray(response.containerChoices)) {
          apply(response);
          setSaved(true);
          return;
        }
        const code = response && response.code;
        setRefusal(
          intl.formatMessage({
            id:
              code === "notFound"
                ? "label.sampleType.containers.notFound"
                : code
                  ? `label.testCatalog.containers.error.${code}`
                  : "label.sampleType.containers.saveFailed",
            defaultMessage: intl.formatMessage({
              id: "label.sampleType.containers.saveFailed",
            }),
          }),
        );
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
          id: "label.sampleType.containers.loadFailed",
        })}
      />
    );
  }

  const unchanged =
    choice ===
    (view.defaultContainerTypeId == null
      ? ""
      : String(view.defaultContainerTypeId));

  return (
    <Stack gap={6} data-testid="sample-type-containers">
      <Select
        id="sample-type-default-container"
        labelText={intl.formatMessage({
          id: "label.sampleType.containers.default",
        })}
        helperText={intl.formatMessage({
          id: "label.sampleType.containers.defaultHelp",
        })}
        invalid={Boolean(refusal)}
        invalidText={refusal || ""}
        value={choice}
        onChange={(event) => {
          setRefusal(null);
          setSaved(false);
          setChoice(event.target.value);
        }}
      >
        <SelectItem
          value=""
          text={intl.formatMessage({
            id: "label.sampleType.containers.none",
          })}
        />
        {view.containerChoices.map((container) => (
          <SelectItem
            key={container.id}
            value={String(container.id)}
            text={
              container.active
                ? container.name
                : intl.formatMessage(
                    { id: "label.sampleType.containers.deactivatedChoice" },
                    { name: container.name },
                  )
            }
          />
        ))}
      </Select>
      {view.containerChoices.length === 0 && (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "label.sampleType.containers.noChoices",
          })}
        />
      )}
      {saved && (
        <InlineNotification
          kind="success"
          lowContrast
          onClose={() => setSaved(false)}
          title={intl.formatMessage({
            id: "label.sampleType.containers.saved",
          })}
        />
      )}
      <div>
        <Button
          kind="primary"
          size="md"
          onClick={save}
          disabled={saving || unchanged}
          data-testid="sample-type-containers-save"
        >
          <FormattedMessage id="label.button.save" />
        </Button>
      </div>

      <div data-testid="sample-type-primary-tests">
        <h4 className="cds--label">
          <FormattedMessage
            id="label.sampleType.containers.primaryFor"
            values={{ count: view.primaryForTests.length }}
          />
        </h4>
        <p>{view.primaryForTests.map((test) => test.value).join(", ")}</p>
      </div>
      <div data-testid="sample-type-secondary-tests">
        <h4 className="cds--label">
          <FormattedMessage
            id="label.sampleType.containers.secondaryFor"
            values={{ count: view.secondaryForTests.length }}
          />
        </h4>
        <UnorderedList>
          {view.secondaryForTests.map((test) => (
            <ListItem key={`${test.testId}-${test.containerName}`}>
              {intl.formatMessage(
                { id: "label.sampleType.containers.secondaryEntry" },
                {
                  test: test.testName,
                  container: test.containerName,
                  usedAs: test.usedAsSampleTypeName,
                },
              )}
            </ListItem>
          ))}
        </UnorderedList>
      </div>
    </Stack>
  );
}

export default ContainersSection;
