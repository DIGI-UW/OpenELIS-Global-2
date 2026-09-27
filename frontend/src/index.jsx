import React from "react";
import ReactDOM from "react-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import "./index.css";
import App from "./App";
import { createQueryClient } from "./components/utils/queryClient";
import * as ServiceWorker from "./serviceWorkerRegistration";

ServiceWorker.registerServiceWorker();

// One cache for the app, so a screen that writes can refresh what it read
// instead of reloading the document.
const queryClient = createQueryClient();

ReactDOM.render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </React.StrictMode>,
  document.getElementById("root"),
);
