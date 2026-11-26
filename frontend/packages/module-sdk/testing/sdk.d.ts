/**
 * The `openelis_core/sdk` double (sdk.jsx): the shell's surface, plus what a
 * test scripts the double's API and permissions with.
 */
export * from "openelis_core/sdk";

export type ApiMethod = "GET" | "POST" | "PUT" | "DELETE";

/** One request a page made through `useOpenElisApi()`; `body` is undefined for GET and DELETE. */
export interface ApiCall {
  method: ApiMethod;
  endpoint: string;
  body: unknown;
}

/** Every request since the last `resetSdkStubs()`, oldest first. */
export const apiCalls: ApiCall[];

/** A scripted answer computed per request; a throw rejects a GET. */
export type ApiAnswerFunction = (
  endpoint: string,
  body: unknown,
  method: ApiMethod,
) => unknown;

/**
 * Answers by endpoint: a value, or a function of `(endpoint, body, method)`
 * whose result is the answer. An unscripted GET, POST or PUT answers {}, a
 * DELETE answers 204; a refusal is scripted the way the shell reports one,
 * `{ statusCode, error, message, fieldErrors }`.
 */
export const apiResponses: Record<
  string,
  ApiAnswerFunction | object | string | number | boolean | null
>;

/** Permission keys `usePermission()` refuses; every other key is granted. */
export const deniedPermissions: Set<string>;

/** What `useModuleConfig(id)` answers, by module id; {} when absent. */
export const moduleConfigs: Record<string, Record<string, string>>;

/** Clears the calls, the scripted answers, the denied permissions and the module configurations. */
export function resetSdkStubs(): void;
