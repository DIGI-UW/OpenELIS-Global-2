// The shape @module-federation/vite gives a built entry: an expose table whose
// loaders import the helper chunks shared by every expose and then the
// expose's own chunk, each next to the entry. "./Ghost" names a page chunk
// that is not in the bundle, for the kit's own failure test.
const exposes = {
  "./Things": async () => {
    await Promise.all([import("./assets/loadRemote-kit0003.js")]);
    return import("./assets/Things-kit0001.js");
  },
  "./Ghost": async () => {
    await Promise.all([import("./assets/loadRemote-kit0003.js")]);
    return import("./assets/Ghost-kit0002.js");
  },
};
export default exposes;
