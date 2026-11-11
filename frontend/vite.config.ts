/// <reference types="vitest" />
import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import svgr from "vite-plugin-svgr";
import { federation } from "@module-federation/vite";
import path from "path";
import moduleFederationConfig from "./module-federation.config.js";

export default defineConfig({
  // The federation plugin makes the application the host of module remotes
  // (loaded at runtime from /rest/module-registry) and a remote itself,
  // `openelis_core`, whose exposed surface modules import (docs/modules.md).
  plugins: [react(), svgr(), federation(moduleFederationConfig)],
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
    sourcemap: true,
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
