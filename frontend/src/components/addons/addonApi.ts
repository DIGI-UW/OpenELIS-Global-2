/**
 * Calls to /rest/addons (AddonUiRestController, AddonAdminRestController).
 * Design: docs/addons/design.md.
 */
import {
  deleteFromOpenElisServer,
  getFromOpenElisServer,
  postToOpenElisServerFormDataJsonResponse,
  postToOpenElisServerJsonResponse,
  putToOpenElisServerJsonResponse,
} from "../utils/Utils";

/** A UI extension of an enabled add-on (like OpenMRS 3's assigned extension). */
export interface AddonExtension {
  addonId: string;
  addonName: string;
  version: string;
  name: string;
  slot: string;
  page: string;
  order: number;
  title: string;
  /** meta.title by locale ("en", "vi", "fr-CA"…), when the manifest gives several. */
  titles?: Record<string, string>;
  height: number;
  /** Header buttons: application | magic-wand | chat | help | idea | bot. */
  icon?: string;
}

export type Provenance = "VERIFIED" | "UNVERIFIED" | "MISMATCH";

export interface ConfigField {
  key: string;
  type: "string" | "number" | "boolean" | "select" | "secret";
  label: string;
  required: boolean;
  defaultValue: string | number | boolean | null;
  options: string[];
  min: number | null;
  max: number | null;
}

export interface AddonView {
  id: string;
  name: string;
  version: string;
  /** The name addon.json claims; verified only when verifiedPublisher is set. */
  publisher: string;
  provenance: Provenance;
  verifiedPublisher: string | null;
  description?: string;
  image: string;
  enabled: boolean;
  endpoint?: string;
  scopes: string[];
  events: string[];
  extensions: { name: string; slot: string; page: string; title: string }[];
  configSchema: ConfigField[];
  /** Plain values only; secrets are never sent back. */
  config: Record<string, unknown>;
  /** Secret fields that have a value. */
  secretsSet?: string[];
  /** Hosts outside OpenELIS the add-on's service connects to. */
  egress?: string[];
  installedAt?: string;
  deliveries: Record<string, number>;
}

export interface Credentials {
  apiKey: string;
  webhookSecret: string;
}

export interface InstallResult {
  addon: AddonView;
  upgraded: boolean;
  credentials: Credentials | null;
  composeSnippet: string | null;
}

export interface Catalog {
  scopes: { key: string; description: string }[];
  /** Scopes that let an add-on see who a patient is; shown with a warning. */
  identifyingScopes?: string[];
  events: { type: string; scope: string; description: string }[];
  slots: { name: string; description: string }[];
}

export interface IssuedToken {
  token: string;
  expiresIn: number;
  scopes: string[];
  context: Record<string, unknown>;
}

/** An answer the server refused; `code` is the server's stable code. */
export class AddonRequestError extends Error {
  status: number;
  code?: string;
  problems: string[];
  constructor(
    status: number,
    code?: string,
    message?: string,
    problems: string[] = [],
  ) {
    super(message || code || `HTTP ${status}`);
    this.status = status;
    this.code = code;
    this.problems = problems;
  }
}

type Body = Record<string, unknown> & {
  status?: number;
  code?: string;
  message?: string;
  error?: string;
  problems?: string[];
};

function settle<T>(resolve: (v: T) => void, reject: (e: Error) => void) {
  return (body?: Body) => {
    if (!body) {
      reject(new AddonRequestError(0));
    } else if (
      body.status !== undefined &&
      (body.status < 200 || body.status >= 300)
    ) {
      reject(
        new AddonRequestError(
          body.status,
          body.code,
          body.message || body.error,
          body.problems || [],
        ),
      );
    } else {
      resolve(body as unknown as T);
    }
  };
}

const post = <T>(path: string, body: unknown) =>
  new Promise<T>((resolve, reject) =>
    postToOpenElisServerJsonResponse(
      path,
      JSON.stringify(body),
      settle(resolve, reject),
    ),
  );

const put = <T>(path: string, body: unknown) =>
  new Promise<T>((resolve, reject) =>
    putToOpenElisServerJsonResponse(
      path,
      JSON.stringify(body),
      settle(resolve, reject),
    ),
  );

/** GET as a promise; getFromOpenElisServer reports any failure as undefined. */
const get = <T>(path: string) =>
  new Promise<T>((resolve, reject) =>
    getFromOpenElisServer(path, (body: T | undefined) =>
      body === undefined ? reject(new AddonRequestError(0)) : resolve(body),
    ),
  );

const base = "/rest/addons";
const id = (addonId: string) => `${base}/${encodeURIComponent(addonId)}`;

/** Address of a page of an add-on's UI, served sandboxed by the backend. */
export const uiUrl = (serverBaseUrl: string, e: AddonExtension) =>
  `${serverBaseUrl}${id(e.addonId)}/ui/${e.page}`;

/** The add-on's own API, reached through the reverse proxy (see the design doc). */
export const apiBase = (addonId: string) =>
  `/addons/${encodeURIComponent(addonId)}/api/`;

// ----- slots (one load per page session; reloaded when an add-on is switched)

let extensions: Promise<AddonExtension[]> | null = null;
export const EXTENSIONS_CHANGED = "openelis-addons-changed";

export function loadExtensions(): Promise<AddonExtension[]> {
  if (!extensions) {
    extensions = get<{ extensions: AddonExtension[] }>(`${base}/extensions`)
      .then((r) => r.extensions || [])
      .catch(() => {
        extensions = null; // try again next time; slots stay empty meanwhile
        return [];
      });
  }
  return extensions;
}

export function invalidateExtensions() {
  extensions = null;
  window.dispatchEvent(new Event(EXTENSIONS_CHANGED));
}

export const requestToken = (
  addonId: string,
  slot: string,
  context: Record<string, unknown>,
) => post<IssuedToken>(`${id(addonId)}/token`, { slot, context });

// ----- administration

export const getCatalog = () => get<Catalog>(`${base}/catalog`);

export const listAddons = () =>
  get<{ addons: AddonView[]; trustedPublishers: string[] }>(base);

export const installAddon = (file: File) => {
  const form = new FormData();
  form.append("package", file);
  return new Promise<InstallResult>((resolve, reject) =>
    postToOpenElisServerFormDataJsonResponse(
      base,
      form,
      settle(resolve, reject),
    ),
  );
};

export const setAddonEnabled = (addonId: string, enabled: boolean) =>
  put<AddonView>(`${id(addonId)}/enabled`, { enabled });

export const saveAddonConfig = (
  addonId: string,
  values: Record<string, unknown>,
) =>
  put<{ config: Record<string, unknown> }>(`${id(addonId)}/config`, { values });

export const rotateCredentials = (addonId: string) =>
  post<Credentials>(`${id(addonId)}/credentials`, {});

/** One request an add-on made to the add-on API (AddonAccessLogInterceptor). */
export interface AccessLogEntry {
  at: string;
  addonId: string;
  credential: "service" | "user";
  userId?: string | null;
  /** As reported by the add-on (X-Addon-Actor). */
  actor?: string | null;
  /** As reported by the add-on (X-Addon-Purpose). */
  purpose?: string | null;
  method: string;
  endpoint: string;
  subjectType: "order" | "patient" | "query" | "none";
  subject?: string | null;
  status: number;
  rows?: number | null;
}

export const accessLogPath = (
  addonId: string,
  params: { actor?: string; subject?: string; limit?: number; format?: string },
) => {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== "") query.set(k, String(v));
  });
  return `${id(addonId)}/access-log?${query.toString()}`;
};

export const getAccessLog = (
  addonId: string,
  params: { actor?: string; subject?: string; limit?: number } = {},
) =>
  get<{ addon: string; retentionDays: number; entries: AccessLogEntry[] }>(
    accessLogPath(addonId, { limit: 200, ...params }),
  );

export const removeAddon = (addonId: string) =>
  new Promise<void>((resolve, reject) =>
    deleteFromOpenElisServer(id(addonId), (status: number) =>
      status >= 200 && status < 300
        ? resolve()
        : reject(new AddonRequestError(status)),
    ),
  );
