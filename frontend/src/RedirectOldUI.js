import React, { useEffect } from "react";
import config from "./config.json";
import { stripBasePath } from "./components/utils/Navigation";

function RedirectOldUI() {
  useEffect(() => {
    window.location.href =
      config.serverBaseUrl + stripBasePath(window.location.pathname) + window.location.search;
  }, []);

  return <></>;
}

export default RedirectOldUI;
