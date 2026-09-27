import React, { useCallback, useEffect, useState } from "react";
import {
  Button,
  InlineNotification,
  Loading,
  Stack,
  TextArea,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";

/**
 * Disposal section (OGC-296 v2.1): a single free-text "Disposal instructions"
 * field — deliberately unstructured reference guidance. The authoritative,
 * structured handling/disposal config stays per-test (TestSampleHandling) and
 * per-specimen (Sample Storage disposal). Saving sends only this field, so an
 * inactive type is never flipped back to active by a section save.
 */
function DisposalSection({ sampleTypeId }) {
  const intl = useIntl();
  const [value, setValue] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    setLoading(true);
    getFromOpenElisServer(`/rest/sample-types/${sampleTypeId}`, (response) => {
      if (response && response.success && response.data) {
        setValue(response.data.disposalInstructions || "");
      }
      setLoading(false);
    });
  }, [sampleTypeId]);

  const save = useCallback(() => {
    setSaving(true);
    setSaved(false);
    setError(null);
    putToOpenElisServer(
      `/rest/sample-types/${sampleTypeId}`,
      JSON.stringify({ disposalInstructions: value }),
      (status) => {
        setSaving(false);
        if (status === 200) {
          setSaved(true);
        } else {
          setError(
            intl.formatMessage({ id: "label.sampleType.disposal.saveError" }),
          );
        }
      },
    );
  }, [sampleTypeId, value, intl]);

  if (loading) {
    return (
      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: "var(--cds-spacing-03)",
        }}
      >
        <Loading small withOverlay={false} />
        <FormattedMessage id="label.sampleType.disposal.loading" />
      </div>
    );
  }

  return (
    <Stack gap={6} data-testid="sampleType-disposal-section">
      <p style={{ fontSize: "14px", color: "var(--cds-text-secondary)" }}>
        <FormattedMessage id="label.sampleType.disposal.intro" />
      </p>

      {saved && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({ id: "label.sampleType.disposal.saved" })}
          lowContrast
          hideCloseButton={false}
          onCloseButtonClick={() => setSaved(false)}
        />
      )}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          lowContrast
          hideCloseButton={false}
          onCloseButtonClick={() => setError(null)}
        />
      )}

      <TextArea
        id="st-disposal-instructions"
        labelText={intl.formatMessage({
          id: "label.sampleType.disposalInstructions",
        })}
        placeholder={intl.formatMessage({
          id: "label.sampleType.disposal.placeholder",
        })}
        rows={5}
        value={value}
        onChange={(e) => setValue(e.target.value)}
      />

      <div>
        <Button
          id="st-disposal-save"
          kind="primary"
          size="md"
          onClick={save}
          disabled={saving}
        >
          {saving ? (
            <FormattedMessage id="button.saving" defaultMessage="Saving..." />
          ) : (
            <FormattedMessage id="button.save" defaultMessage="Save Changes" />
          )}
        </Button>
      </div>
    </Stack>
  );
}

export default DisposalSection;
