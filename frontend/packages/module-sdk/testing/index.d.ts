/**
 * What a module's Vite configuration needs to run its tests against the
 * doubles (index.js); the preset sets both already.
 */

/** The shared libraries, for `resolve.dedupe`: one React for the module and the doubles. */
export const openelisCoreDedupe: string[];

/** For `test.alias`: each `openelis_core/<entry>` import resolved to its double. */
export const openelisCoreAliases: Array<{ find: RegExp; replacement: string }>;
