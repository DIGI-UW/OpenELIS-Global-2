/// <reference types="vitest" />
import { defineConfig } from "vitest/config";
import type { PluginOption } from "vite";
import react from "@vitejs/plugin-react";
import svgr from "vite-plugin-svgr";
import path from "path";

/**
 * @carbon/react lists its own barrel in package.json `sideEffects`, so a bundler
 * must keep all ~9 MB of it however few components a file names. The entries in
 * that list that matter are the stylesheets; the JS barrel is pure. Declaring it
 * so lets the named imports across the app shake down to what they actually use.
 */
const carbonBarrelIsPure = (): PluginOption => ({
  name: "carbon-barrel-is-pure",
  enforce: "pre",
  async resolveId(source, importer, options) {
    if (source !== "@carbon/react" && source !== "@carbon/react/") {
      return null;
    }
    const resolved = await this.resolve(source, importer, {
      ...options,
      skipSelf: true,
    });
    return resolved ? { ...resolved, moduleSideEffects: false } : null;
  },
});

export default defineConfig({
  plugins: [carbonBarrelIsPure(), react(), svgr()],
  resolve: {
    extensions: [".mjs", ".js", ".ts", ".jsx", ".tsx", ".json"],
    alias: [
      { find: /^~/, replacement: "" },
      // react-data-table-component@7.5.3's `main` is a CJS bundle that does NOT
      // set `exports.__esModule = true`. Vite's CJS-to-ESM interop refuses to
      // unwrap `.default` without that marker, so `import DataTable from "..."`
      // resolves to the whole exports object (`{ default, STOP_PROP_TAG, ... }`)
      // instead of the component. Force Vite to use the ESM bundle directly
      // (its `module` field already points here) so the default export works.
      // CRA's webpack hid this because it had more aggressive default-unwrap.
      {
        find: /^react-data-table-component$/,
        replacement: "react-data-table-component/dist/index.es.js",
      },
    ],
  },
  css: {
    preprocessorOptions: {
      scss: {
        quietDeps: true,
      },
    },
  },
  build: {
    outDir: "dist",
    // Maps are 40 MB against 22 MB of JS and ship readable source in the image.
    sourcemap: false,
  },
  test: {
    alias: [
      {
        find: "flatpickr",
        replacement: path.resolve(__dirname, "./src/__mocks__/flatpickr.js"),
      },
      {
        find: /.*config\.json$/,
        replacement: path.resolve(__dirname, "./src/__mocks__/config.mock.js"),
      },
    ],
    globals: true,
    environment: "jsdom",
    setupFiles: "./src/setupTests.js",
    server: {
      deps: {
        inline: ["@carbon/react", "flatpickr"],
      },
    },
    exclude: [
      "**/node_modules/**",
      "**/dist/**",
      "**/cypress/**",
      "**/playwright/**",
    ],
  },
  server: {
    port: 80,
    host: true,
    https: !!process.env.HTTPS,
    open: false,
  },
});
