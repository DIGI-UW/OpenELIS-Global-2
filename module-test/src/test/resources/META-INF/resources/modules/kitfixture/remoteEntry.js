// The shape @module-federation/vite gives a built entry: an expose table whose
// values import the expose's chunk next to the entry. "./Ghost" names a chunk
// that is not in the bundle, for the kit's own failure test.
const exposes = {
  "./Things": async () => import("./assets/Things-kit0001.js"),
  "./Ghost": async () => import("./assets/Ghost-kit0002.js"),
};
export default exposes;
