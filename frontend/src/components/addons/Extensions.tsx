import React, { useCallback, useEffect, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Button,
  CodeSnippet,
  FileUploaderDropContainer,
  Heading,
  InlineLoading,
  InlineNotification,
  Modal,
  NumberInput,
  PasswordInput,
  Select,
  SelectItem,
  Tag,
  TextInput,
  Toggle,
} from "@carbon/react";
import PageBreadCrumb from "../common/PageBreadCrumb";
import {
  getCatalog,
  installAddon,
  invalidateExtensions,
  listAddons,
  removeAddon,
  rotateCredentials,
  saveAddonConfig,
  setAddonEnabled,
  type AddonView,
  type Catalog,
  type ConfigField,
  type Credentials,
  type InstallResult,
} from "./addonApi";
import { configToSave, initialConfig } from "./configForm";
import "./addons.scss";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
];

/**
 * Administration > Extensions: install an add-on package, see who published
 * it, review what it may see, turn it on or off, configure it, remove it.
 * Turning an add-on on is the administrator's consent to its permissions.
 */
export default function Extensions() {
  const intl = useIntl();
  const [addons, setAddons] = useState<AddonView[] | null>(null);
  const [catalog, setCatalog] = useState<Catalog | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [installing, setInstalling] = useState(false);
  const [installed, setInstalled] = useState<InstallResult | null>(null);
  const [consent, setConsent] = useState<AddonView | null>(null);
  const [removing, setRemoving] = useState<AddonView | null>(null);
  const [credentials, setCredentials] = useState<Credentials | null>(null);

  const load = useCallback(() => {
    listAddons()
      .then((r) => {
        setAddons(r.addons);
        setError(null);
      })
      .catch((e) => setError(String(e?.message || e)));
  }, []);

  useEffect(() => {
    load();
    getCatalog()
      .then(setCatalog)
      .catch(() => setCatalog(null));
  }, [load]);

  const fail = (e: unknown) => {
    const err = e as { message?: string; problems?: string[] };
    setError([err?.message || String(e), ...(err?.problems || [])].join(" · "));
  };

  const onFile = (_event: unknown, { addedFiles }: { addedFiles: File[] }) => {
    const file = addedFiles?.[0];
    if (!file) return;
    setInstalling(true);
    setInstalled(null);
    installAddon(file)
      .then((result) => {
        setInstalled(result);
        setError(null);
        load();
      })
      .catch(fail)
      .finally(() => setInstalling(false));
  };

  const switchOff = (addon: AddonView) =>
    setAddonEnabled(addon.id, false)
      .then(() => {
        invalidateExtensions();
        load();
      })
      .catch(fail);

  const switchOn = (addon: AddonView) =>
    setAddonEnabled(addon.id, true)
      .then(() => {
        setConsent(null);
        invalidateExtensions();
        load();
      })
      .catch(fail);

  const remove = (addon: AddonView) =>
    removeAddon(addon.id)
      .then(() => {
        setRemoving(null);
        invalidateExtensions();
        load();
      })
      .catch(fail);

  const rotate = (addon: AddonView) =>
    rotateCredentials(addon.id).then(setCredentials).catch(fail);

  return (
    <div className="adminPageContent" data-testid="addon-admin">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Heading>
        <FormattedMessage id="addon.admin.title" />
      </Heading>
      <p className="addon-admin__meta">
        <FormattedMessage id="addon.admin.intro" />
      </p>
      {error && (
        <InlineNotification
          kind="error"
          lowContrast
          title={intl.formatMessage({ id: "addon.admin.error" })}
          subtitle={error}
          onClose={() => setError(null)}
        />
      )}

      <div className="addon-admin__section">
        <FileUploaderDropContainer
          accept={[".zip"]}
          labelText={intl.formatMessage({ id: "addon.admin.upload" })}
          onAddFiles={onFile}
          data-testid="addon-upload"
        />
        {installing && (
          <InlineLoading
            description={intl.formatMessage({ id: "addon.admin.installing" })}
          />
        )}
      </div>

      {installed && (
        <InstallSummary
          result={installed}
          catalog={catalog}
          onDismiss={() => setInstalled(null)}
        />
      )}

      {addons && addons.length === 0 && (
        <p className="addon-admin__section" data-testid="addon-admin-empty">
          <FormattedMessage id="addon.admin.empty" />
        </p>
      )}
      {(addons || []).map((addon) => (
        <AddonCard
          key={addon.id}
          addon={addon}
          catalog={catalog}
          onToggle={(on) => (on ? setConsent(addon) : switchOff(addon))}
          onSaved={load}
          onRemove={() => setRemoving(addon)}
          onRotate={() => rotate(addon)}
        />
      ))}

      {consent && (
        <Modal
          open
          modalHeading={intl.formatMessage(
            { id: "addon.consent.title" },
            { name: consent.name },
          )}
          primaryButtonText={intl.formatMessage({ id: "addon.consent.turnOn" })}
          secondaryButtonText={intl.formatMessage({ id: "common.cancel" })}
          danger={consent.provenance === "MISMATCH"}
          onRequestSubmit={() => switchOn(consent)}
          onRequestClose={() => setConsent(null)}
        >
          <PermissionReview addon={consent} catalog={catalog} />
        </Modal>
      )}

      {removing && (
        <Modal
          open
          danger
          modalHeading={intl.formatMessage(
            { id: "addon.remove.title" },
            { name: removing.name },
          )}
          primaryButtonText={intl.formatMessage({ id: "common.remove" })}
          secondaryButtonText={intl.formatMessage({ id: "common.cancel" })}
          onRequestSubmit={() => remove(removing)}
          onRequestClose={() => setRemoving(null)}
        >
          <p>
            <FormattedMessage id="addon.remove.body" />
          </p>
        </Modal>
      )}

      {credentials && (
        <Modal
          open
          passiveModal
          modalHeading={intl.formatMessage({ id: "addon.credentials.title" })}
          onRequestClose={() => setCredentials(null)}
        >
          <CredentialsView credentials={credentials} />
        </Modal>
      )}
    </div>
  );
}

/** Who published the add-on, as far as the signature can tell. */
export function ProvenanceLine({ addon }: { addon: AddonView }) {
  const intl = useIntl();
  if (addon.provenance === "VERIFIED" && addon.verifiedPublisher) {
    return (
      <Tag type="green" data-testid={`addon-provenance-${addon.id}`}>
        <FormattedMessage
          id="addon.provenance.verified"
          values={{ publisher: addon.verifiedPublisher }}
        />
      </Tag>
    );
  }
  if (addon.provenance === "MISMATCH") {
    return (
      <InlineNotification
        kind="error"
        lowContrast
        hideCloseButton
        data-testid={`addon-provenance-${addon.id}`}
        title={intl.formatMessage({ id: "addon.provenance.mismatch" })}
        subtitle={intl.formatMessage({ id: "addon.provenance.mismatchHelp" })}
      />
    );
  }
  return (
    <span data-testid={`addon-provenance-${addon.id}`}>
      <Tag type="gray">
        <FormattedMessage id="addon.provenance.unverified" />
      </Tag>{" "}
      <span className="addon-admin__meta">
        <FormattedMessage
          id="addon.provenance.claimed"
          values={{ publisher: addon.publisher }}
        />
      </span>
    </span>
  );
}

/** What turning the add-on on allows. */
export function PermissionReview({
  addon,
  catalog,
}: {
  addon: AddonView;
  catalog: Catalog | null;
}) {
  const intl = useIntl();
  const describe = (prefix: string, key: string, fallback?: string) =>
    // i18n-keys: addon.scope.*, addon.event.*, addon.slot.*
    intl.formatMessage({
      id: `${prefix}.${key}`,
      defaultMessage: fallback || key,
    });
  const scopeText = (key: string) =>
    describe(
      "addon.scope",
      key,
      catalog?.scopes.find((s) => s.key === key)?.description,
    );
  const eventText = (type: string) =>
    describe(
      "addon.event",
      type,
      catalog?.events.find((e) => e.type === type)?.description,
    );
  const slotText = (name: string) =>
    describe(
      "addon.slot",
      name,
      catalog?.slots.find((s) => s.name === name)?.description,
    );
  return (
    <div data-testid={`addon-permissions-${addon.id}`}>
      <ProvenanceLine addon={addon} />
      <h4 className="addon-admin__section">
        <FormattedMessage id="addon.permissions.scopes" />
      </h4>
      {addon.scopes.length ? (
        <ul>
          {addon.scopes.map((s) => (
            <li key={s}>
              {scopeText(s)} <Tag size="sm">{s}</Tag>
            </li>
          ))}
        </ul>
      ) : (
        <p>
          <FormattedMessage id="addon.permissions.none" />
        </p>
      )}
      {addon.events.length > 0 && (
        <>
          <h4 className="addon-admin__section">
            <FormattedMessage id="addon.permissions.events" />
          </h4>
          <ul>
            {addon.events.map((e) => (
              <li key={e}>
                {eventText(e)} <Tag size="sm">{e}</Tag>
              </li>
            ))}
          </ul>
        </>
      )}
      <h4 className="addon-admin__section">
        <FormattedMessage id="addon.permissions.network" />
      </h4>
      {addon.egress && addon.egress.length > 0 ? (
        <ul data-testid={`addon-egress-${addon.id}`}>
          {addon.egress.map((h) => (
            <li key={h}>
              <Tag size="sm" type="warm-gray">
                {h}
              </Tag>
            </li>
          ))}
        </ul>
      ) : (
        <p>
          <FormattedMessage id="addon.permissions.network.none" />
        </p>
      )}
      {addon.extensions.length > 0 && (
        <>
          <h4 className="addon-admin__section">
            <FormattedMessage id="addon.permissions.slots" />
          </h4>
          <ul>
            {addon.extensions.map((x) => (
              <li key={x.name}>
                {x.title}: {slotText(x.slot)}
              </li>
            ))}
          </ul>
        </>
      )}
      <p className="addon-admin__section addon-admin__meta">
        <FormattedMessage id="addon.permissions.sandbox" />
      </p>
    </div>
  );
}

function CredentialsView({ credentials }: { credentials: Credentials }) {
  return (
    <div data-testid="addon-credentials">
      <p>
        <FormattedMessage id="addon.credentials.once" />
      </p>
      <p>OPENELIS_API_KEY</p>
      <CodeSnippet type="single">{credentials.apiKey}</CodeSnippet>
      <p>OPENELIS_WEBHOOK_SECRET</p>
      <CodeSnippet type="single">{credentials.webhookSecret}</CodeSnippet>
    </div>
  );
}

function InstallSummary({
  result,
  catalog,
  onDismiss,
}: {
  result: InstallResult;
  catalog: Catalog | null;
  onDismiss: () => void;
}) {
  const intl = useIntl();
  return (
    <div className="addon-admin__card" data-testid="addon-installed">
      <InlineNotification
        kind="success"
        lowContrast
        onClose={onDismiss}
        title={intl.formatMessage(
          {
            id: result.upgraded
              ? "addon.install.upgraded"
              : "addon.install.registered",
          },
          { name: result.addon.name, version: result.addon.version },
        )}
        subtitle={intl.formatMessage({ id: "addon.install.next" })}
      />
      <PermissionReview addon={result.addon} catalog={catalog} />
      {result.credentials && (
        <CredentialsView credentials={result.credentials} />
      )}
      {result.composeSnippet && (
        <>
          <h4 className="addon-admin__section">
            <FormattedMessage id="addon.install.compose" />
          </h4>
          <CodeSnippet type="multi" feedback="">
            {result.composeSnippet}
          </CodeSnippet>
        </>
      )}
    </div>
  );
}

function AddonCard({
  addon,
  catalog,
  onToggle,
  onSaved,
  onRemove,
  onRotate,
}: {
  addon: AddonView;
  catalog: Catalog | null;
  onToggle: (on: boolean) => void;
  onSaved: () => void;
  onRemove: () => void;
  onRotate: () => void;
}) {
  const intl = useIntl();
  const delivered = addon.deliveries || {};
  return (
    <article className="addon-admin__card" data-testid={`addon-${addon.id}`}>
      <div className="addon-admin__head">
        <div>
          <h3>{addon.name}</h3>
          <div className="addon-admin__meta">
            {addon.id} · {addon.version}
          </div>
          {addon.description && <p>{addon.description}</p>}
        </div>
        <Toggle
          id={`addon-toggle-${addon.id}`}
          labelText={intl.formatMessage({ id: "addon.toggle.label" })}
          labelA={intl.formatMessage({ id: "addon.toggle.off" })}
          labelB={intl.formatMessage({ id: "addon.toggle.on" })}
          toggled={addon.enabled}
          onToggle={onToggle}
        />
      </div>
      <PermissionReview addon={addon} catalog={catalog} />
      <p className="addon-admin__section addon-admin__meta">
        <FormattedMessage
          id="addon.deliveries"
          values={{
            delivered: delivered.DELIVERED || 0,
            pending: delivered.PENDING || 0,
            failed: delivered.FAILED || 0,
          }}
        />
      </p>
      {addon.configSchema.length > 0 && (
        <ConfigForm addon={addon} onSaved={onSaved} />
      )}
      <div className="addon-admin__section">
        <Button kind="tertiary" size="sm" onClick={onRotate}>
          <FormattedMessage id="addon.credentials.rotate" />
        </Button>{" "}
        <Button kind="danger--tertiary" size="sm" onClick={onRemove}>
          <FormattedMessage id="common.remove" />
        </Button>
      </div>
    </article>
  );
}

function ConfigForm({
  addon,
  onSaved,
}: {
  addon: AddonView;
  onSaved: () => void;
}) {
  const intl = useIntl();
  const [values, setValues] = useState<Record<string, unknown>>(() =>
    initialConfig(addon.configSchema, addon.config),
  );
  const [cleared, setCleared] = useState<Set<string>>(new Set());
  const [message, setMessage] = useState<{
    kind: "success" | "error";
    text: string;
  } | null>(null);
  const set = (key: string, value: unknown) =>
    setValues((previous) => ({ ...previous, [key]: value }));
  const save = () =>
    saveAddonConfig(addon.id, configToSave(addon.configSchema, values, cleared))
      .then(() => {
        setCleared(new Set());
        setValues((previous) => {
          const next = { ...previous };
          addon.configSchema
            .filter((f) => f.type === "secret")
            .forEach((f) => (next[f.key] = ""));
          return next;
        });
        setMessage({
          kind: "success",
          text: intl.formatMessage({ id: "addon.config.saved" }),
        });
        onSaved();
      })
      .catch(() =>
        setMessage({
          kind: "error",
          text: intl.formatMessage({ id: "addon.config.invalid" }),
        }),
      );
  const field = (f: ConfigField) => {
    const id = `addon-${addon.id}-${f.key}`;
    switch (f.type) {
      case "boolean":
        return (
          <Toggle
            key={f.key}
            id={id}
            labelText={f.label}
            toggled={values[f.key] === true}
            onToggle={(on: boolean) => set(f.key, on)}
          />
        );
      case "number":
        return (
          <NumberInput
            key={f.key}
            id={id}
            label={f.label}
            min={f.min ?? undefined}
            max={f.max ?? undefined}
            value={Number(values[f.key] ?? 0)}
            onChange={(_e: unknown, { value }: { value: number | string }) =>
              set(f.key, Number(value))
            }
          />
        );
      case "select":
        return (
          <Select
            key={f.key}
            id={id}
            labelText={f.label}
            value={String(values[f.key] ?? "")}
            onChange={(e: React.ChangeEvent<HTMLSelectElement>) =>
              set(f.key, e.target.value)
            }
          >
            {f.options.map((o) => (
              <SelectItem key={o} value={o} text={o} />
            ))}
          </Select>
        );
      case "secret": {
        const isSet =
          !cleared.has(f.key) && (addon.secretsSet || []).includes(f.key);
        return (
          <div key={f.key} className="addon-admin__secret">
            <PasswordInput
              id={id}
              labelText={f.label}
              autoComplete="new-password"
              helperText={intl.formatMessage({
                id: isSet
                  ? "addon.config.secret.set"
                  : "addon.config.secret.unset",
              })}
              value={String(values[f.key] ?? "")}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                set(f.key, e.target.value);
                setCleared((previous) => {
                  const next = new Set(previous);
                  next.delete(f.key);
                  return next;
                });
              }}
            />
            {isSet && (
              <Button
                kind="ghost"
                size="sm"
                onClick={() => {
                  set(f.key, "");
                  setCleared((previous) => new Set(previous).add(f.key));
                }}
              >
                <FormattedMessage id="addon.config.secret.clear" />
              </Button>
            )}
          </div>
        );
      }
      default:
        return (
          <TextInput
            key={f.key}
            id={id}
            labelText={f.label}
            value={String(values[f.key] ?? "")}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
              set(f.key, e.target.value)
            }
          />
        );
    }
  };
  return (
    <div className="addon-admin__section">
      <h4>
        <FormattedMessage id="addon.config.title" />
      </h4>
      <div
        className="addon-admin__form"
        data-testid={`addon-config-${addon.id}`}
      >
        {addon.configSchema.map(field)}
        <div>
          <Button size="sm" onClick={save}>
            <FormattedMessage id="common.save" />
          </Button>
        </div>
        {message && (
          <InlineNotification
            kind={message.kind}
            lowContrast
            hideCloseButton
            title={message.text}
          />
        )}
      </div>
    </div>
  );
}
