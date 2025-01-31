import React from "react";
import { createRoot } from "react-dom/client"; // Import createRoot
import "./index.css";
import App from "./App";
import reportWebVitals from "./reportWebVitals";
import * as ServiceWorker from "./serviceWorkerRegistration";

// Register the service worker
ServiceWorker.registerServiceWorker();

// Create a root for your app
const container = document.getElementById("root");
const root = createRoot(container);

// Render your app
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();