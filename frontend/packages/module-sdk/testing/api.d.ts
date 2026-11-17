/**
 * The `openelis_core/api` double (api.js): the shell's surface, every call
 * answering an empty success and recorded on `apiCalls`.
 */
export * from "openelis_core/api";

type CoreApi = typeof import("openelis_core/api");

/** One call through the double, by function name with its arguments. */
export interface ApiStubCall {
  name: string;
  args: unknown[];
}

/** Every call since the last `resetApiStubs()`, oldest first. */
export const apiCalls: ApiStubCall[];

/** The implementations the double's functions call; reassign or spy on one to script an answer. */
export const apiStubs: {
  [Name in Exclude<keyof CoreApi, "Roles" | "config">]: CoreApi[Name];
};

/** Clears the calls and restores every default implementation. */
export function resetApiStubs(): void;
